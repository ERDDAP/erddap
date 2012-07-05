/* 
 * Table Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.util.*;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.FileNameUtility;
import gov.noaa.pfel.coastwatch.griddata.Matlab;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.util.DataStream;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.Tally;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/*import javax.xml.xpath.XPath;   //requires java 1.5
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
*/
import org.xml.sax.XMLReader;
//import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
//import org.xml.sax.helpers.DefaultHandler;

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

/** The Java DAP classes.  */
import dods.dap.*;

/**
 * This class holds tabular data as 
 *   <ul>
 *   <li>globalAttributes (a list of (String)name=(PrimitiveArray)value)
 *   <li>columnAttributes - an ArrayList of Attributes, one for each column
 *      (each is a list of (String)name=(PrimitiveArray)value).
 *   <li>columnNames - a StringArray
 *   <li>the columns of data - an ArrayList of PrimitiveArray
 *   </ul>
 * This class is used by CWBrowser as a way to read tabular data from 
 * ASCII and .nc files and Opendap, store the data in a standard 
 * in-memory format, and write the data to several types of files 
 * (ASCII, MatLab, .nc, ...).
 * Since there is often a lot of data, these objects are usually short-lived.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2005-12-05
 */
public class Table  {

    /**
     * Set this to true (by calling verbose=true in your program, not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /**
     * Set this to true (by calling debug=true in your program, not but changing the code here)
     * if you want lots and lots of diagnostic messages sent to String2.log.
     */
    public static boolean debug = false;    


    /**
     * Set this to true (by calling reallyVerbose=true in your program, 
     * not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean reallyVerbose = false; 

    /**
     * If true, readASCII allows data lines to have varying numbers of 
     * values and assumes that the missing values are from the end columns.
     */
    public boolean allowRaggedRightInReadASCII = false;

    /**
     * If true, readOpendap requestes compressed data. 
     * I think this should always be true.
     */
    public boolean opendapAcceptDeflate = true;

    /** Since users use these numbers (not names) from the command line,
     * the value for a given option shouldn't ever change.
     */
    public final static int READ_ASCII = 0, READ_FLAT_NC = 1, READ_OPENDAP_SEQUENCE = 2, READ_4D_NC = 3;

    //the values here won't change as new file types are supported
    public static final int SAVE_AS_TABBED_ASCII = 0;
    public static final int SAVE_AS_FLAT_NC = 1;
    public static final int SAVE_AS_4D_NC = 2;
    public static final int SAVE_AS_MATLAB = 3;
    //public static final int SAVE_AS_HDF = 4;
    public static final String SAVE_AS_EXTENSIONS[] = {
        ".asc", ".nc", ".nc", ".mat"};

    public static String BGCOLOR = "#ffffcc"; 

    //Bob: see identical css in ERDDAP's setup.xml <startHeadHtml>
    public static String ERD_TABLE_CSS =
        "<style type=\"text/CSS\">\n" +
        "<!--\n" +
        "  table.erd {border-collapse:collapse; border:1px solid gray; }\n" +
        "  table.erd th, table.erd td {padding:2px; border:1px solid gray; }\n" +
        "-->" +
        "</style>\n"; 

    /** An arrayList to hold 0 or more PrimitiveArray's with data.  */
    private ArrayList columns = new ArrayList();  

    /** An arrayList to hold the column names. */
    private StringArray columnNames = new StringArray();


    /**
     * This holds the global attributes ((String)name = (PrimitiveArray)value).
     * Although a HashTable is more appropriate for name=value pairs,
     * this uses ArrayList to preserve the order of the attributes.
     * This may be null if not in use.
     */
    private Attributes globalAttributes = new Attributes();

    /**
     * This holds the column Attributes ((String)name = (PrimitiveArray)value)
     * in an arrayList.
     * Although a HashTable is more appropriate for name=value pairs,
     * this uses ArrayList to preserve the order of the attributes.
     * This may be null if not in use.
     */
    private ArrayList columnAttributes = new ArrayList();


    /** testDir is used for tests. */
    public static String testDir = 
        String2.getClassPath() + "gov/noaa/pfel/coastwatch/pointdata/";

    /** The one known valid url for readIobis. */
    public final static String IOBIS_URL = "http://www.iobis.org/OBISWEB/ObisControllerServlet"; 

    /**
     * This clears everything.
     */
    public void clear() {
        columns.clear();  
        columnNames.clear(); 

        globalAttributes.clear();
        columnAttributes.clear();
    }

    /**
     * This makes a deep clone of the current table (data and attributes).
     *
     * @param startRow
     * @param stride
     * @param endRow (inclusive)  e.g., nRows()-1 
     * @return a new Table.
     */
    public Table subset(int startRow, int stride, int endRow) {
        Table tTable = new Table();

        int n = columns.size();
        for (int i = 0; i < n; i++)
            tTable.columns.add(getColumn(i).subset(startRow, stride, endRow));

        tTable.columnNames = (StringArray)columnNames.clone();

        tTable.globalAttributes = (Attributes)globalAttributes.clone(); 

        for (int col = 0; col < columnAttributes.size(); col++) 
            tTable.columnAttributes.add(((Attributes)columnAttributes.get(col)).clone()); 

        return tTable;
    }

    /**
     * This makes a deep clone of the entire current table (data and attributes).
     *
     * @return a new Table.
     */
    public Object clone() {
        return subset(0, 1, nRows() - 1);
    }

    /**
     * This converts a string with a double value or a date/time 
     * (matching at least the start of a date time formatted as 
     * 1970-01-01T00:00:00, where 'T' can be any non-digit)
     * into a double.
     *
     * @param s
     * @return a double value (or NaN if trouble)
     */
/*
//what is this method and why is it in Table???
//who calls it??? no one
    public static double parse(String s) {
        //future: handle degrees? see gmt appendix B
        s = s.trim();

        //is it a date?
        int po = s.indexOf('-');
        if (po > 0 &&   //not a negative number, e.g., -1
//is this a good test for sci notation?
            Character.toLowerCase(s.charAt(po - 1)) == 'e') {  //not sci notation, e.g., 1.5e-12
            return Calendar2.isoStringToEpochSeconds(s);
        }

        //regular number
        return String2.parseDouble(s);
    }
*/

    /**
     * This returns the current number of columns.
     *
     * @return the current number of columns
     */
    public int nColumns() {
        return columns.size();
    }

    /**
     * This returns the PrimitiveArray for a specific column.
     *
     * @param col (0..)
     * @return the corresponding PrimitiveArray
     * @throws Exception if col is invalid
     */
    public PrimitiveArray getColumn(int col) {
        if (col < 0 || col >= columns.size())
            throw new IllegalArgumentException(
                String2.ERROR + " in Table.getColumn: col=" + col + " must be 0 ... " + (columns.size()-1) + "."); 
        return (PrimitiveArray)columns.get(col);
    }

    /**
     * This returns the PrimitiveArray for a specific column.
     *
     * @param columnName
     * @return the corresponding PrimitiveArray
     * @throws IllegalArgumentException if columnName is invalid
     */
    public PrimitiveArray getColumn(String columnName) {
        int col = findColumnNumber(columnName);
        if (col < 0)
            throw new IllegalArgumentException(
                String2.ERROR + " in Table.getColumn: columnName=" + columnName + " not found."); 
        return (PrimitiveArray)columns.get(col);
    }

    /**
     * This sets the PrimitiveArray for a specific column.
     *
     * @param col (0.. size-1)
     * @param pa the corresponding PrimitiveArray
     * @throws Exception if col is invalid
     */
    public void setColumn(int col, PrimitiveArray pa) {
        if (col >= columns.size())
            throw new IllegalArgumentException(
                String2.ERROR + " in Table.setColumn: col (" + col + ") is >= size (" + columns.size() + ")."); 
        if (pa == null)
            throw new IllegalArgumentException(
                String2.ERROR + " in Table.setColumn(col=" + col + ", pa): pa is null."); 
        columns.set(col, pa);
    }

    /**
     * This returns the PrimitiveArrays for all of the columns.
     *
     * @return the PrimitiveArray[]
     * @throws Exception if col is invalid
     */
    public PrimitiveArray[] getColumns() {
        PrimitiveArray pa[] = new PrimitiveArray[columns.size()];
        for (int i = 0; i < columns.size(); i++)
            pa[i] = getColumn(i);
        return pa;
    }

    /**
     * This simplifies (changes the datatype to the simplest possible type)
     * a column.
     *
     * @param col the column to be simplified, 0...
     */
    public void simplify(int col) {
        columns.set(col, getColumn(col).simplify());
    }

    /**
     * This simplifies (changes the datatype to the simplest possible type)
     * all columns.
     *
     */
    public void simplify() {
        int nColumns = columns.size();
        for (int col = 0; col < nColumns; col++) 
            simplify(col);
    }

    /**
     * This copies the values from one row to another (without affecting
     * any other rows).
     *
     * @param from the 'from' row
     * @param to the 'to' row
     */
    public void copyRow(int from, int to) {
        PrimitiveArray.copyRow(columns, from, to);
    }

    /** 
     * This writes a row to a DataOutputStream.
     * @param row
     * @param dos
     */
    public void writeRowToDOS(int row, DataOutputStream dos) throws Exception {
        int nColumns = columns.size();
        for (int col = 0; col < nColumns; col++) 
            ((PrimitiveArray)columns.get(col)).writeDos(dos, row);
    }

    /** 
     * This reads/appends a row from a DataInputStream.
     * @param dis
     */
    public void readRowFromDIS(DataInputStream dis) throws Exception {
        int nColumns = columns.size();
        for (int col = 0; col < nColumns; col++) 
            ((PrimitiveArray)columns.get(col)).readDis(dis, 1); //read 1 value
    }

    /**
     * This returns an estimate of the number of bytes per row
     * (assuming 20 per String column, which is probably low).
     */
    public int estimatedBytesPerRow() {
        int sum = 0;
        for (int col = 0; col < columnAttributes.size(); col++) 
            sum += getColumn(col).elementSize();
        return sum;
    }


    /**
     * This inserts a column in the table.
     *
     * @param position  0..
     * @param name the name for the column
     *    If name is null, the name will be "Column<#>" (0..)
     * @param pa the data for the column.
     *    The data is not copied. 
     *    This primitiveArray will continue to be used to store the data. 
     *    This doesn't ensure that it has the correct size().
     * @param attributes the attributes for the column
     * @return the column's number
     */
    public int addColumn(int position, String name, PrimitiveArray pa, Attributes attributes) {
        if (pa == null)
            throw new SimpleException(String2.ERROR + " in Table.addColumn: pa is null.");
        if (attributes == null)
            throw new SimpleException(String2.ERROR + " in Table.addColumn: attributes is null.");
        int size = columns.size();
        if (position > size) 
            throw new IllegalArgumentException(
                String2.ERROR + " in Table.addColumn: position (" + position + 
                ") is beyond size (" + size  + ").");
        if (name == null) 
            name = "Column" + position;
        columnNames.add(position, name);
        columns.add(position, pa);
        columnAttributes.add(position, attributes);
        return columns.size() - 1;
    }

    /**
     * This inserts a column in the table.
     * The column's will initially have 0 attributes.
     *
     * @param position  0..
     * @param name the name for the column
     *    If name is null, the name will be "Column<#>" (0..)
     * @param pa the data for the column.
     *    The data is not copied. This primitiveArray will continue to be 
     *    used to store the data.
     * @return the column's number
     */
    public int addColumn(int position, String name, PrimitiveArray pa) {
        return addColumn(position, name, pa, new Attributes());
    }

    /**
     * This adds a column to the table and the end.
     * The column's will initially have 0 attributes.
     *
     * @param name the name for the column
     *    If name is null, the name will be "Column<#>" (0..)
     * @param pa the data for the column.
     *    The data is not copied. This primitiveArray will continue to be 
     *    used to store the data.
     * @return the column's number
     */
    public int addColumn(String name, PrimitiveArray pa) {
        return addColumn(columnNames.size(), name, pa);
    }

    /**
     * This removes a column (and any associated columnName and attributes).
     *
     * @param col (0..)
     * @throws Exception if col is invalid
     */
    public void removeColumn(int col) {
        columnNames.remove(col);
        columns.remove(col);
        columnAttributes.remove(col);
    }

    /**
     * This removes a column (and any associated columnName and attributes).
     *
     * @param columnName
     * @throws IllegalArgumentException if columnName is invalid
     */
    public void removeColumn(String columnName) {
        int col = findColumnNumber(columnName);
        if (col < 0)
            throw new IllegalArgumentException(
                String2.ERROR + " in Table.removeColumn: columnName=" + columnName + " not found."); 
        removeColumn(col);
    }



    /**
     * This removes a range of columns (and any associated columnName and attributes).
     *
     * @param from (0..)
     * @param to   exclusive (0..)
     * @throws Exception if col is invalid
     */
    public void removeColumns(int from, int to) {
        for (int col = to - 1; col >= from; col--)
            removeColumn(col);
    }

    /**
     * This removes all columns.
     *
     */
    public void removeAllColumns() {
        removeColumns(0, nColumns());
    }

    /**
     * This moves a column.
     *
     * @param from
     * @param to
     * @throws Exception if 'from' or 'to' is not valid.
     */
    public void moveColumn(int from, int to) {
        if (from == to)
            return;
        addColumn(to, getColumnName(from), getColumn(from), columnAttributes(from));

        //the 'from' column may now be in a different position
        if (from > to) 
            from++;
        removeColumn(from);
    }

    /**
     * This returns the columnName for a specific column.
     *
     * @param col (0..)
     * @return the corresponding column name (or "Column#<col>", where col is 0..).
     * @throws Exception if col not valid.
     */
    public String getColumnName(int col) {
        if (col < 0 || col >= nColumns())
            throw new IllegalArgumentException(
                String2.ERROR + " in Table.getColumnName: col " + col + " is invalid.");
        return columnNames.get(col);
    }

    /**
     * This returns a String[] with all of the column names.
     *
     * @return a String[] with all of the column names.
     */
    public String[] getColumnNames() {
        return columnNames.toArray();
    }

    /**
     * This returns the number of the column named columnName.
     *
     * @param columnName
     * @return the corresponding column number (or -1 if not found).
     */
    public int findColumnNumber(String columnName) {
        return columnNames.indexOf(columnName, 0);
    }

    /**
     * This returns the column named columnName.
     *
     * @param columnName
     * @return the corresponding column
     * @throws IllegalArgumentException if not found
     */
    public PrimitiveArray findColumn(String columnName) {
        int col = findColumnNumber(columnName);
        if (col < 0) 
            throw new IllegalArgumentException(String2.ERROR + " in Table.findColumn: columnName=" + 
                columnName + " not found.");
        return getColumn(col);
    }

    /**
     * This sets the columnName for a specific column.
     *
     * @param col (0..)
     * @param newName the new name for the column
     * @throws Exception if columnNames is null, or col not valid.
     */
    public void setColumnName(int col, String newName) {
        if (col < 0 || col >= nColumns())
            throw new IllegalArgumentException(
                String2.ERROR + " in Table.setColumnName: col " + col + " is invalid.");
        columnNames.set(col, newName);
    }

    /**
     * This returns the columnName for a specific column
     * with internal spaces replaced with '_'s.
     *
     * @param col (0..)
     * @return the corresponding column name (or "Column_" + col, 
     *   or "" if col is invalid)
     */
    public String getColumnNameWithoutSpaces(int col) {
        return String2.replaceAll(getColumnName(col), " ", "_");
    }

    /**
     * Test of speed and memory efficiency of ucar.ma package.
     *
     */
    /*public static void testMA() {
        String2.log("testMA");
        Math2.incgc(200);
        Math2.incgc(200);
        int n = 10000000;
        int j;
        double d;

        //test ArrayInt.D1
        long oMemory = Math2.getUsingMemory();
        ArrayInt.D1 arrayInt = new ArrayInt.D1(n);
        String2.log("ArrayInt.D1 bytes/int  = " + ((Math2.getUsingMemory() - oMemory) / (double)n));
        long time = System.currentTimeMillis();
        Index1D index = new Index1D(new int[]{n});
        for (int i = 0; i < n; i++)
            d = arrayInt.getDouble(index.set(i));
        String2.log("ArrayInt.D1 read time=" + (System.currentTimeMillis() - time)); //157

        //test int[]
        oMemory = Math2.getUsingMemory();
        int iar[] = new int[n];
        String2.log("int[] bytes/int  = " + ((Math2.getUsingMemory() - oMemory) / (double)n));
        time = System.currentTimeMillis();
        for (int i = 0; i < n; i++)
            d = iar[i];
        String2.log("int[] read time=" + (System.currentTimeMillis() - time)); //32

        //test int[] as object
        Object o = iar;
        time = System.currentTimeMillis();
        for (int i = 0; i < n; i++) 
            d = getDouble(o, i);
        String2.log("(int[])o read time=" + (System.currentTimeMillis() - time)); //172

        //test Integer[]
        oMemory = Math2.getUsingMemory();
        Number nar[] = new Integer[n];
        String2.log("Integer[] bytes/int  = " + ((Math2.getUsingMemory() - oMemory) / (double)n));
        time = System.currentTimeMillis();
        for (int i = 0; i < n; i++)
            nar[i] = new Integer(i);
        String2.log("Integer[] create time=" + (System.currentTimeMillis() - time)); //2271!
        time = System.currentTimeMillis();
        for (int i = 0; i < n; i++)
            d = nar[i].doubleValue();
        String2.log("Integer[] read time=" + (System.currentTimeMillis() - time)); //110

        Math2.sleep(30000);

    } */

    /**
     * This returns the current number of rows.
     *
     * @return the current number of rows
     */
    public int nRows() {
        if (nColumns() == 0)
            return 0;
        return getColumn(0).size();
    }

    /**
     * This inserts a blank row.
     *
     * @param index 0..size
     * @throws Exception if trouble
     */
    public void insertBlankRow(int index) throws Exception {
        if (index < 0 || index > nRows())
            throw new Exception("index=" + index + " must be between 0 and " + nRows() + ".");
        int nCols = nColumns();
        for (int col = 0; col < nCols; col++) 
            getColumn(col).addString(index, "");
    }

    /**
     * This removes a range of rows.
     * This does the best it can to not throw an exception 
     * (e.g., to help clean up a damaged table with columns with different numbers
     * of rows).
     *
     * @param from the first element to be removed, 0 ... size
     * @param to one after the last element to be removed, from ... size
     *    (or use Integer.MAX_VALUE to remove to the end).
     * @throws Exception if trouble
     */
    public void removeRows(int from, int to) {
        int nCols = nColumns();
        for (int col = 0; col < nCols; col++) {
            PrimitiveArray pa = getColumn(col);
            int nRows = pa.size();
            if (from < nRows)
                pa.removeRange(from, Math.min(nRows, to));
        }
    }

    /**
     * This removes all rows of data, but leaves the columns intact.
     *
     */
    public void removeAllRows() {
        int nCols = nColumns();
        for (int col = 0; col < nCols; col++) 
            getColumn(col).clear();
    }

    /**
     * This returns the value of one datum as a String.
     *
     * @param col the column number (0 ... nColumns-1 )
     * @param row the row number (0 ... nRows-1 )
     * @return the value of one datum as a String.
     * @throws Exception if trouble (e.g., row or col out of range)
     */
    public String getStringData(int col, int row) {
        return getColumn(col).getString(row);
    }

    /**
     * This returns the value of one datum as a float.
     *
     * @param col the column number (0 ... nColumns-1 )
     * @param row the row number (0 ... nRows-1 )
     * @return the value of one datum as a float.
     * @throws Exception if trouble (e.g., row or col out of range)
     *   Strings return the parsed value of the string 
     *    (Double.NaN if not a number).
     */
    public float getFloatData(int col, int row) {
        return getColumn(col).getFloat(row);
    }

    /**
     * This returns the value of one datum as a double.
     *
     * @param col the column number (0 ... nColumns-1 )
     * @param row the row number (0 ... nRows-1 )
     * @return the value of one datum as a double.
     * @throws Exception if trouble (e.g., row or col out of range)
     *   Strings return the parsed value of the string 
     *    (Double.NaN if not a number).
     */
    public double getDoubleData(int col, int row) {
        return getColumn(col).getDouble(row);
    }

    /**
     * This returns the value of one datum as a double.
     *
     * @param col the column number (0 ... nColumns-1 )
     * @param row the row number (0 ... nRows-1 )
     * @return the value of one datum as a nice double 
     *    (rounded to 7 significant digits, so not bruised).
     * @throws Exception if trouble (e.g., row or col out of range)
     *   Strings return the parsed value of the string 
     *    (Double.NaN if not a number).
     */
    public double getNiceDoubleData(int col, int row) {
        return getColumn(col).getNiceDouble(row);
    }

    /**
     * This returns the value of one datum as a long.
     *
     * @param col the column number (0 ... nColumns-1 )
     * @param row the row number (0 ... nRows-1 )
     * @return the value of one datum as a long.
     * @throws Exception if trouble (e.g., row or col out of range)
     *   Strings return the parsed value of the string 
     *    (Long.MAX_VALUE if not a number).
     */
    public long getLongData(int col, int row) {
        return getColumn(col).getLong(row);
    }

    /**
     * This returns the value of one datum as an int.
     *
     * @param col the column number (0 ... nColumns-1 )
     * @param row the row number (0 ... nRows-1 )
     * @return the value of one datum as an int.
     * @throws Exception if trouble (e.g., row or col out of range)
     *   Strings return the parsed value of the string 
     *    (Integer.MAX_VALUE if not a number).
     */
    public int getIntData(int col, int row) {
        return getColumn(col).getInt(row);
    }

    /**
     * This sets the value of one datum as a String.
     *
     * @param col the column number (0 ... nColumns-1 )
     * @param row the row number (0 ... nRows-1 )
     * @param s the value of one datum as a String.
     * @throws Exception if trouble (e.g., row or col out of range)
     */
    public void setStringData(int col, int row, String s) {
        getColumn(col).setString(row, s);
    }

    /**
     * This sets the value of one datum as a float.
     *
     * @param col the column number (0 ... nColumns-1 )
     * @param row the row number (0 ... nRows-1 )
     * @param d the value of one datum as a float.
     * @throws Exception if trouble (e.g., row or col out of range)
     */
    public void setFloatData(int col, int row, float d) {
        getColumn(col).setFloat(row, d);
    }

    /**
     * This sets the value of one datum as a double.
     *
     * @param col the column number (0 ... nColumns-1 )
     * @param row the row number (0 ... nRows-1 )
     * @param d the value of one datum as a double.
     * @throws Exception if trouble (e.g., row or col out of range)
     */
    public void setDoubleData(int col, int row, double d) {
        getColumn(col).setDouble(row, d);
    }

    /**
     * This sets the value of one datum as an int.
     *
     * @param col the column number (0 ... nColumns-1 )
     * @param row the row number (0 ... nRows-1 )
     * @param i the value of one datum as an int.
     * @throws Exception if trouble (e.g., row or col out of range)
     */
    public void setIntData(int col, int row, int i) {
        getColumn(col).setInt(row, i);
    }

    /**
     * This add the value of one datum as a String to one of the columns,
     * thereby increasing the number of rows in that column.
     *
     * @param col the column number (0 ... nColumns-1 )
     * @param s the value of one datum as a String.
     * @throws Exception if trouble (e.g., row or col out of range)
     */
    public void addStringData(int col, String s) {
        getColumn(col).addString(s);
    }

    /**
     * This add the value of one datum as a float to one of the columns,
     * thereby increasing the number of rows in that column.
     *
     * @param col the column number (0 ... nColumns-1 )
     * @param d the value of one datum as a float.
     * @throws Exception if trouble (e.g., row or col out of range)
     */
    public void addFloatData(int col, float d) {
        getColumn(col).addFloat(d);
    }

    /**
     * This add the value of one datum as a double to one of the columns,
     * thereby increasing the number of rows in that column.
     *
     * @param col the column number (0 ... nColumns-1 )
     * @param d the value of one datum as a double.
     * @throws Exception if trouble (e.g., row or col out of range)
     */
    public void addDoubleData(int col, double d) {
        getColumn(col).addDouble(d);
    }

    /**
     * This add the value of one datum as an int to one of the columns,
     * thereby increasing the number of rows in that column.
     *
     * @param col the column number (0 ... nColumns-1 )
     * @param d the value of one datum as an int.
     * @throws Exception if trouble (e.g., row or col out of range)
     */
    public void addIntData(int col, int d) {
        getColumn(col).addInt(d);
    }

    /** This tests that the value in a column is as expected.
     * @throws Exception if trouble
     */
    public void test1(String columnName, int row, String expected) {
        String observed = findColumn(columnName).getString(row);
        //ensureEqual deals with nulls
        Test.ensureEqual(observed, expected, "colName=" + columnName + " row=" + row);
    }

    /** This tests that the value in a column is as expected.
     * @throws Exception if trouble
     */
    public void test1(String columnName, int row, int expected) {
        int observed = findColumn(columnName).getInt(row);
        if (observed != expected)
            throw new RuntimeException("colName=" + columnName + " row=" + row);
    }

    /** This tests that the value in a column is as expected.
     * @throws Exception if trouble
     */
    public void test1(String columnName, int row, float expected) {
        float observed = findColumn(columnName).getFloat(row);
        //ensureEqual does fuzzy test
        Test.ensureEqual(observed, expected, "colName=" + columnName + " row=" + row);
    }

    /** This tests that the value in a column is as expected.
     * @throws Exception if trouble
     */
    public void test1(String columnName, int row, double expected) {
        double observed = findColumn(columnName).getDouble(row);
        //ensureEqual does fuzzy test
        Test.ensureEqual(observed, expected, "colName=" + columnName + " row=" + row);
    }

    /** This tests that the value in a column is as expected.
     * The table's column should have epoch seconds values.
     * 
     * @param expected value formatted with 
     *     Calendar2.safeEpochSecondsToIsoStringTZ(seconds, "")
     * @throws Exception if trouble
     */
    public void test1Time(String columnName, int row, String expected) {
        double seconds = findColumn(columnName).getDouble(row);
        String observed = Calendar2.safeEpochSecondsToIsoStringTZ(seconds, "");
        if (!observed.equals(expected))
            throw new RuntimeException("colName=" + columnName + " row=" + row);
    }


    /** This checks that the value in a column is as expected and prints PASS/FAIL and the test. */
    public String check1(String columnName, int row, String expected) {
        String observed = findColumn(columnName).getString(row);
        //Test.equal deals with nulls
        return (Test.equal(observed, expected)? "PASS" : "FAIL") +
            ": col=" + String2.left(columnName, 15) + 
             " row=" + String2.left("" + row, 2) + 
            " observed=" + observed + " expected=" + expected;
    }

    /** This checks that the value in a column is as expected and prints PASS/FAIL and the test. */
    public String check1(String columnName, int row, int expected) {
        int observed = findColumn(columnName).getInt(row);
        return (Test.equal(observed, expected)? "PASS" : "FAIL") +
            ": col=" + String2.left(columnName, 15) + 
             " row=" + String2.left("" + row, 2) + 
            " observed=" + observed + " expected=" + expected;
    }

    /** This checks that the value in a column is as expected and prints PASS/FAIL and the test. */
    public String check1(String columnName, int row, float expected) {
        float observed = findColumn(columnName).getFloat(row);
        return (Test.equal(observed, expected)? "PASS" : "FAIL") +
            ": col=" + String2.left(columnName, 15) + 
             " row=" + String2.left("" + row, 2) + 
            " observed=" + observed + " expected=" + expected;
    }

    /** This checks that the value in a column is as expected and prints PASS/FAIL and the test. */
    public String check1(String columnName, int row, double expected) {
        double observed = findColumn(columnName).getDouble(row);
        return (Test.equal(observed, expected)? "PASS" : "FAIL") +
            ": col=" + String2.left(columnName, 15) + 
             " row=" + String2.left("" + row, 2) + 
            " observed=" + observed + " expected=" + expected;
    }

    /** This checks that the value in a column is as expected and prints PASS/FAIL and the test. 
     * The table's column should have epoch seconds values.
     * 
     * @param expected value formatted with 
     *     Calendar2.safeEpochSecondsToIsoStringTZ(seconds, "")
     */
    public String check1Time(String columnName, int row, String expected) {
        double seconds = findColumn(columnName).getDouble(row);
        String observed = Calendar2.safeEpochSecondsToIsoStringTZ(seconds, "");
        return (Test.equal(observed, expected)? "PASS" : "FAIL") +
            ": col=" + String2.left(columnName, 15) + 
             " row=" + String2.left("" + row, 2) + 
            " observed=" + observed + " expected=" + expected;
    }

    /**
     * This prints a header in a format that pretty closely mimics the C version of ncdump
     * (starting with the "{") and acts as if the file will be stored as an .nc file.
     *
     * @param dimensionName the name for the rows (e.g., "time", "row", "station", "observation")
     * @return a string representation of this grid
     */
    public String getNCHeader(String dimensionName) {
        //dimensions
        StringBuilder sb = new StringBuilder(
            //this pretty closely mimics the C version of ncdump 
            //(number formats are a little different)
            //and acts as if the file will be stored as an .nc file
            "{\n" +
            "dimensions:\n" +
                "\t" + dimensionName + " = " + nRows() + " ;\n");
        int nColumns = nColumns();
        for (int col = 0; col < nColumns; col++) {
            PrimitiveArray pa = getColumn(col);
            if (pa instanceof StringArray) {
                StringArray sa = (StringArray)pa;
                sb.append("\t" + getColumnName(col) + NcHelper.StringLengthSuffix + 
                    " = " + sa.maxStringLength() + " ;\n");
            }
        }

        //variables
        sb.append("variables:\n");
        for (int col = 0; col < nColumns; col++) {
            PrimitiveArray pa = getColumn(col);
            String columnName = getColumnName(col);
            if (pa instanceof StringArray) {
                StringArray sa = (StringArray)pa;
                sb.append("\tchar " + columnName + "(" + dimensionName + ", " +
                    columnName + NcHelper.StringLengthSuffix + ") ;\n");
            } else {
                sb.append("\t" + pa.elementClassString() + 
                    " " + columnName + "(" + dimensionName + ") ;\n");
            }
            sb.append(columnAttributes(col).toNcString("\t\t" + columnName + ":", " ;"));
            }
        sb.append("\n// global attributes:\n");
        sb.append(globalAttributes.toNcString("\t\t:", " ;"));
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * This makes a string representation of this data.
     *
     * @param dimensionName the name for the rows (e.g., "time", "row", "station", "observation")
     * @param showFirstNRows  use Integer.MAX_VALUE for all rows.
     * @return a string representation of this point data
     */
    public String toString(String dimensionName, int showFirstNRows) {
        ensureValid(); //throws Exception if not
        String result = getNCHeader(dimensionName);
        
        /*
        int nRows = nRows();
        int nColumns = nColumns();
        StringBuilder sb = new StringBuilder(
            "Table[nRows=" + nRows + " nColumns=" + nColumns + "\n");

        //print global attributes
        sb.append("\tglobal attributes:\n");
        sb.append(globalAttributes.toNcString("\t\t", " ;")

        //print data attributes
        for (int col = 0; col < nColumns; col++) {
            sb.append("  Column " + col + " = " + getColumnName(col) + " (" + 
                getColumn(col).elementClassString() + ")\n");
            sb.append(columnAttributes(col).toNcString("\t\t" + getColumnName(col) + ":", " ;"));
            }
        }
*/
        
        return result + dataToString(showFirstNRows);
    }

    /**
     * This prints the data to a crude table.
     * 
     * @param showFirstNRows  use Integer.MAX_VALUE for all rows.
     */
    public String dataToString(int showFirstNRows) {
        if (showFirstNRows <= 0) 
            return "";
        StringBuilder sb = new StringBuilder();
        showFirstNRows = Math.min(showFirstNRows, nRows());
        sb.append("    Row " + getColumnarColumnNamesString());
        for (int row = 0; row < showFirstNRows; row++)
            sb.append("\n" + String2.right("" + row, 7) + " " + 
                getRowToColumnarString(row));
        sb.append('\n');
        return sb.toString();
    }

    /**
     * This prints the metadata and the data to a CSV table.
     * 
     */
    public String toCSVString() {
        return toCSVString(Integer.MAX_VALUE);
    }

    /**
     * This prints the metadata and the data to a CSV table.
     * 
     * @param showFirstNRows  use Integer.MAX_VALUE for all rows.
     */
    public String toCSVString(int showFirstNRows) {
        if (showFirstNRows < 0) 
            return "";
        return getNCHeader("row") + dataToCSVString(showFirstNRows);
    }

    /**
     * This is convenience for dataToCSVString(showAllRows, don't showRowNumbers).
     */
    public String dataToCSVString() {
        return dataToCSVString(Integer.MAX_VALUE, false);
    }

    /**
     * This is convenience for dataToCSVString(int showFirstNRows, showRowNumber=true).
     */
    public String dataToCSVString(int showFirstNRows) {
        return dataToCSVString(showFirstNRows, true);
    }

    /**
     * This prints the data to a CSV table.
     * 
     * @param showFirstNRows  use Integer.MAX_VALUE for all rows.
     */
    public String dataToCSVString(int showFirstNRows, boolean showRowNumber) {
        if (showFirstNRows < 0) 
            return "";
        StringBuilder sb = new StringBuilder();
        int nCols = nColumns();
        showFirstNRows = Math.min(showFirstNRows, nRows());
        sb.append((showRowNumber? "row," : "") + 
            getColumnNamesCSVString() + "\n");
        for (int row = 0; row < showFirstNRows; row++) {
            if (showRowNumber)
                sb.append(row + ",");
            for (int col = 0; col < nCols; col++) {
                String s = getStringData(col, row);
                if (s.indexOf(',')  >= 0 || s.indexOf('"')  >= 0 || 
                    s.indexOf('\n') >= 0 || s.indexOf('\t') >= 0)
                    s = String2.toJson(s);
                sb.append(s);
                if (col == nCols - 1)
                    sb.append('\n');
                else 
                    sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * This makes a string representation (toString(true)) of this point data.
     *
     * @return a string representation of this table
     */
    public String toString() {
        String observationDimension = globalAttributes.getString("observationDimension");
        if (observationDimension == null)
            observationDimension = "row";
        return toString(observationDimension, Integer.MAX_VALUE);
    }

    /**
     * For diagnostic purposes: this returns a string with the names of the
     * columns.
     * Note that columns names will be truncated at 14 characters.
     *
     * @return a string with the column names.
     */
    public String getColumnarColumnNamesString() {
        StringBuilder sb = new StringBuilder();
        for (int col = 0; col < nColumns(); col++) {
            sb.append(' ' + String2.right(String2.noLongerThan(
                columnNames.get(col), 14), 14));
        }
        return sb.toString();
    }

    /**
     * This returns a Comma Separated Value (CSV) string with the names of the columns.
     *
     * @return a csv string with the column names.
     */
    public String getColumnNamesCSVString() {
        return columnNames.toCSVString();
    }

    /**
     * This returns a Comma Space Separated Value (CSSV) string with the names of the columns.
     *
     * @return a csv string with the column names.
     */
    public String getColumnNamesCSSVString() {
        return columnNames.toString();
    }

    /**
     * For diagnostic purposes: this returns a string with the values for one
     * row in a columnar format String.
     * Note that strings values will be truncated at 14 characters.
     *
     * @param row the index of the desired row (0..length-1)
     * @return a string with the data values.
     */
    public String getRowToColumnarString(int row) {
        StringBuilder sb = new StringBuilder();
        for (int col = 0; col < nColumns(); col++) {
            PrimitiveArray array = getColumn(col);
            sb.append(' ');
            if (array instanceof StringArray) {
                sb.append(String2.right(String2.noLongerThan(array.getString(row), 14), 14));
            } else {
                double d = array.getDouble(row);
                long tl = Math.round(d);
                if (d == tl && Math.abs(d) < 1e14)
                     sb.append(String2.right("" + tl, 14));
                else sb.append(String2.right(String2.genEFormat6(d), 14));
            }
        }
        return sb.toString();
    }

    /**
     * This returns the Attributes with the global attributes.
     * 
     * @return the Attributes with global attributes.
     */
    public Attributes globalAttributes() {
        return globalAttributes;
    }

    /**
     * This gets the Attributes for a given column.
     * Use this with care; this is the actual data structure, not a clone.
     * 
     * @param column
     * @return the ArrayList with the attributes for the column
     */
    public Attributes columnAttributes(int column) {
        return (Attributes)columnAttributes.get(column);
    }

    /**
     * This gets the Attributes for a given column.
     * Use this with care; this is the actual data structure, not a clone.
     * 
     * @param columnName
     * @return the ArrayList with the attributes for the column
     * @throws IllegalArgumentException if columnName not found
     */
    public Attributes columnAttributes(String columnName) {
        int col = findColumnNumber(columnName);
        if (col < 0)
            throw new IllegalArgumentException(
                String2.ERROR + " in Table.getColumn: columnName=" + columnName + " not found."); 
        return columnAttributes(col);
    }

    /**
     * This adds the standard attributes, including calling setActualRangeAndBoundingBox
     * (which sets the coordinate variables' axis, long_name, standard_name, and units).
     * If a string parameter is null, nothing is done; the attribute is not set and not cleared.
     * If a string parameter is "", that attributes is cleared.
     *
     * <p>This does not call convertToFakeMissingValues(), nor should that
     * (generally speaking) be called before or after calling this.
     * This procedure expects NaN's as the missing values.
     * convertToFakeMissingValues and convertToStandardMissingValues are called
     * internally by the saveAsXxx methods to temporarily switch to fakeMissingValue.
     *
     * <p>This does not set the data columns' attributes, notably:
     *   "long_name" (boldTitle?), "standard_name", "units" (UDUnits).
     *
     * <p>This sets most of the metadata needed to comply with 
     *  Unidata Observation Dataset Conventions 
     * (http://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html). 
     * To fully comply, you may need to add the global attribute 
     * observationDimension (see saveAsFlatNc).
     *
     * @param lonIndex identifies the longitude column (or -1 if none)
     * @param latIndex identifies the latitude column (or -1 if none)
     * @param depthIndex identifies the depth column (or -1 if none)
     * @param timeIndex identifies the time column (or -1 if none)
     * @param boldTitle  a descriptive title for this data
     * @param cdmDataType   "Grid", "Image", "Station", "Point", "Trajectory", or "Radial"
     * @param creatorEmail usually already set, e.g., DataHelper.CS_CREATOR_EMAIL, "dave.foley@noaa.gov"
     * @param creatorName usually already set, e.g., DataHelper.CS_CREATOR_NAME, "NOAA CoastWatch, West Coast Node"
     * @param creatorUrl usually already set, e.g., DataHelper.CS_CREATOR_URL, "http://coastwatch.pfeg.noaa.gov"
     * @param project usually already set, e.g., DataHelper.CS_PROJECT
     * @param id a unique string identifying this table
     * @param keywordsVocabulary e.g., "GCMD Science Keywords"
     * @param keywords  e.g., a keyword string from 
     *    http://gcmd.gsfc.nasa.gov/Resources/valids/gcmd_parameters.html
     *    e.g., "Oceans > Ocean Temperature > Sea Surface Temperature"
     * @param references
     * @param summary a longer description of this data
     * @param courtesy   e.g., "Channel Islands National Park (David Kushner)"
     * @param timeLongName "Time" (the default if this is null), 
     *    or a better description of time, e.g., "Centered Time",
     *    "Centered Time of 1 Day Averages"
     */
    public void setAttributes(int lonIndex, int latIndex, int depthIndex, int timeIndex,
        String boldTitle, String cdmDataType, 
        String creatorEmail, String creatorName, String creatorUrl, String project,
        String id, String keywordsVocabulary,
        String keywords, String references, String summary, 
        String courtesy, String timeLongName) {

        String currentDateTimeZ = Calendar2.getCurrentISODateTimeStringZulu() + "Z";
        trySet(globalAttributes, "acknowledgement", "NOAA NESDIS COASTWATCH, NOAA SWFSC ERD");
        trySet(globalAttributes, "cdm_data_type", cdmDataType);
        trySet(globalAttributes, "contributor_name", courtesy);
        trySet(globalAttributes, "contributor_role", "Source of data."); 
        //Unidata Observation Dataset v1.0?
        trySet(globalAttributes, "Conventions",          "COARDS, CF-1.6, Unidata Dataset Discovery v1.0"); //unidata-related
        trySet(globalAttributes, "Metadata_Conventions", "COARDS, CF-1.6, Unidata Dataset Discovery v1.0"); //unidata-related
        trySet(globalAttributes, "creator_email", creatorEmail);
        trySet(globalAttributes, "creator_name", creatorName);
        trySet(globalAttributes, "creator_url", creatorUrl);
        trySet(globalAttributes, "date_created", currentDateTimeZ);
        trySet(globalAttributes, "date_issued",  currentDateTimeZ);
        String oldHistory = globalAttributes.getString("history");
        if (oldHistory == null)
            oldHistory = courtesy;
        trySet(globalAttributes, "history", DataHelper.addBrowserToHistory(oldHistory));
        //String2.log("Table.setAttributes new history=" + globalAttributes.getString("history") +
        //    "\nstack=" + MustBe.stackTrace());
        trySet(globalAttributes, "id", id);
        trySet(globalAttributes, "institution", creatorName);
        if (keywords != null && keywords.length() > 0) {
            trySet(globalAttributes, "keywords_vocabulary", keywordsVocabulary);
            trySet(globalAttributes, "keywords", keywords);
        }
        trySet(globalAttributes, "license", "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.");
        trySet(globalAttributes, "naming_authority", "gov.noaa.pfel.coastwatch");  //for generating id
        //trySet(globalAttributes, "processing_level", "3"); //appropriate for satellite data
        trySet(globalAttributes, "project", project);
        trySet(globalAttributes, "references", references);
        trySet(globalAttributes, "standard_name_vocabulary", FileNameUtility.getStandardNameVocabulary());
        trySet(globalAttributes, "summary", summary);
        trySet(globalAttributes, "title", boldTitle);  //Time series from ... ?

        //remove some commonly set, but no longer relevant, attributes
        globalAttributes.remove(CacheOpendapStation.OPENDAP_TIME_DIMENSION_SIZE);
        globalAttributes.remove("Unlimited_Dimension"); //e.g., MBARI has this

        //setActualRangeAndBoundingBox
        setActualRangeAndBoundingBox(lonIndex, latIndex, depthIndex, -1, timeIndex, timeLongName);

    }


    /**
     * This is called by setAttributes to calculate and 
     * set each column's "actual_range" attributes
     * and set the THREDDS ACDD-style
     * geospatial_lat_min and max, ... and time_coverage_start and end
     * global attributes, and the Google Earth-style 
     *   Southernmost_Northing, ... Easternmost_Easting.
     * This also sets column attributes for the lon, lat, depth, and time variables 
     * (if the index isn't -1).
     * For Unidata Observation Dataset Conventions (e.g., _Coordinate), 
     * see http://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html .
     *
     * @param lonIndex identifies the longitude column (or -1 if none)
     * @param latIndex identifies the latitude column (or -1 if none)
     * @param depthIndex identifies the depth column (or -1 if none)
     * @param altIndex identifies the altitude column (or -1 if none).
     *   There shouldn't be both a depth and an altitude column.
     * @param timeIndex identifies the time column (or -1 if none)
     * @param timeLongName "Time" (the default, if this is null), 
     *    or a better description of time, e.g., "Centered Time",
     *    "Centered Time of 1 Day Averages"
     */
    public void setActualRangeAndBoundingBox(
        int lonIndex, int latIndex, int depthIndex, int altIndex, int timeIndex, String timeLongName) {
        //set actual_range
        for (int col = 0; col < nColumns(); col++) {
            setActualRange(col);

            //set acdd-style and google-style bounding box
            PrimitiveArray range = columnAttributes(col).get("actual_range");
            if (col == lonIndex) {
                columnAttributes(col).set("_CoordinateAxisType", "Lon");  //unidata-related
                columnAttributes(col).set("axis",                "X");
                columnAttributes(col).set("long_name",           "Longitude");
                columnAttributes(col).set("standard_name",       "longitude");
                columnAttributes(col).set("units",               "degrees_east");

                globalAttributes.set("geospatial_lon_units", "degrees_east");
                if (range == null) {
                    globalAttributes.remove("geospatial_lon_min");
                    globalAttributes.remove("geospatial_lon_max");
                    globalAttributes.remove("Westernmost_Easting");
                    globalAttributes.remove("Easternmost_Easting");
                } else if (range instanceof FloatArray) {
                    globalAttributes.set("geospatial_lon_min",  range.getFloat(0));
                    globalAttributes.set("geospatial_lon_max",  range.getFloat(1));
                    globalAttributes.set("Westernmost_Easting", range.getFloat(0));
                    globalAttributes.set("Easternmost_Easting", range.getFloat(1));
                } else {
                    globalAttributes.set("geospatial_lon_min",  range.getDouble(0));
                    globalAttributes.set("geospatial_lon_max",  range.getDouble(1));
                    globalAttributes.set("Westernmost_Easting", range.getDouble(0));
                    globalAttributes.set("Easternmost_Easting", range.getDouble(1));
                }
            } else if (col == latIndex) {
                columnAttributes(col).set("_CoordinateAxisType", "Lat"); //unidata-related
                columnAttributes(col).set("axis",                "Y");
                columnAttributes(col).set("long_name",           "Latitude");
                columnAttributes(col).set("standard_name",       "latitude");
                columnAttributes(col).set("units",               "degrees_north");

                globalAttributes.set("geospatial_lat_units", "degrees_north");
                if (range == null) {
                    globalAttributes.remove("geospatial_lat_min");
                    globalAttributes.remove("geospatial_lat_max");
                    globalAttributes.remove("Southernmost_Northing");
                    globalAttributes.remove("Northernmost_Northing");
                } else if (range instanceof FloatArray) {
                    globalAttributes.set("geospatial_lat_min",    range.getFloat(0)); //unidata-related
                    globalAttributes.set("geospatial_lat_max",    range.getFloat(1));
                    globalAttributes.set("Southernmost_Northing", range.getFloat(0));
                    globalAttributes.set("Northernmost_Northing", range.getFloat(1));
                } else {
                    globalAttributes.set("geospatial_lat_min",    range.getDouble(0)); //unidata-related
                    globalAttributes.set("geospatial_lat_max",    range.getDouble(1));
                    globalAttributes.set("Southernmost_Northing", range.getDouble(0));
                    globalAttributes.set("Northernmost_Northing", range.getDouble(1));
                }
            } else if (col == depthIndex) {
                columnAttributes(col).set("_CoordinateAxisType", "Height");   //unidata
                columnAttributes(col).set("_CoordinateZisPositive", "down");  //unidata
                columnAttributes(col).set("axis",                "Z");
                columnAttributes(col).set("long_name",           "Depth");  //this is a commitment to Depth
                columnAttributes(col).set("positive",            "down");   //this is a commitment to Depth, //unidata-related
                columnAttributes(col).set("standard_name",       "depth");  //this is a commitment to Depth
                columnAttributes(col).set("units",               "m");  //CF standard names says canonical units are "m"

                if (range == null) {
                    globalAttributes.remove("geospatial_vertical_min"); //unidata-related
                    globalAttributes.remove("geospatial_vertical_max");
                } else if (range instanceof DoubleArray) {
                    globalAttributes.set("geospatial_vertical_min", range.getDouble(0));
                    globalAttributes.set("geospatial_vertical_max", range.getDouble(1));
                } else if (range instanceof FloatArray) {
                    globalAttributes.set("geospatial_vertical_min", range.getFloat(0));
                    globalAttributes.set("geospatial_vertical_max", range.getFloat(1));
                } else {
                    globalAttributes.set("geospatial_vertical_min", range.getInt(0));
                    globalAttributes.set("geospatial_vertical_max", range.getInt(1));
                }
                globalAttributes.set("geospatial_vertical_units", "m");
                globalAttributes.set("geospatial_vertical_positive", "down"); //this is a commitment to Depth
            } else if (col == altIndex) {
                columnAttributes(col).set("_CoordinateAxisType", "Height");   //unidata
                columnAttributes(col).set("_CoordinateZisPositive", "up");  //unidata
                columnAttributes(col).set("axis",                "Z");
                columnAttributes(col).set("long_name",           "Altitude");  //this is a commitment to Altitude
                columnAttributes(col).set("positive",            "up");        //this is a commitment to Altitude, //unidata-related
                columnAttributes(col).set("standard_name",       "altitude");  //this is a commitment to Altitude
                columnAttributes(col).set("units",               "m");  //CF standard names says canonical units are "m"

                if (range == null) {
                    globalAttributes.remove("geospatial_vertical_min"); //unidata-related
                    globalAttributes.remove("geospatial_vertical_max");
                } else if (range instanceof DoubleArray) {
                    globalAttributes.set("geospatial_vertical_min", range.getDouble(0));
                    globalAttributes.set("geospatial_vertical_max", range.getDouble(1));
                } else if (range instanceof FloatArray) {
                    globalAttributes.set("geospatial_vertical_min", range.getFloat(0));
                    globalAttributes.set("geospatial_vertical_max", range.getFloat(1));
                } else {
                    globalAttributes.set("geospatial_vertical_min", range.getInt(0));
                    globalAttributes.set("geospatial_vertical_max", range.getInt(1));
                }
                globalAttributes.set("geospatial_vertical_units", "m");
                globalAttributes.set("geospatial_vertical_positive", "up"); //this is a commitment to Altitude
            } else if (col == timeIndex) {
                columnAttributes(col).set("_CoordinateAxisType", "Time"); //unidata-related
                columnAttributes(col).set("axis",                "T");
                if (timeLongName == null || timeLongName.length() == 0)
                    timeLongName = "Time";
                columnAttributes(col).set("long_name",           timeLongName); 
                columnAttributes(col).set("standard_name",       "time");
                //LAS Intermediate files wants time_origin "01-Jan-1970 00:00:00" ;
                //http://ferret.wrc.noaa.gov/LASdoc/serve/cache/90.html
                columnAttributes(col).set("time_origin",         "01-JAN-1970 00:00:00");
                columnAttributes(col).set("units",               Calendar2.SECONDS_SINCE_1970);

                //this range is a little misleading for averaged data
                if (range == null) {
                    globalAttributes.remove("time_coverage_start");   //unidata-related
                    globalAttributes.remove("time_coverage_end");
                } else {
                    globalAttributes.set("time_coverage_start", Calendar2.epochSecondsToIsoStringT(range.getDouble(0)) + "Z");
                    globalAttributes.set("time_coverage_end",   Calendar2.epochSecondsToIsoStringT(range.getDouble(1)) + "Z");
                }
                //this doesn't set tableGlobalAttributes.set("time_coverage_resolution", "P1H");
            } 
        }

    }

    /**
     * Call this to remove file-specific attributes 
     * (which are wrong if applied to an aggregated dataset):
     * each column's "actual_range" attributes and the THREDDS ACDD-style
     * geospatial_lat_min and max, ... and time_coverage_start and end
     * global attributes, and the Google Earth-style 
     *   Southernmost_Northing, ... Easternmost_Easting.
     * For Unidata Observation Dataset Conventions (e.g., _Coordinate), 
     * see http://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html .
     *
     */
    public void unsetActualRangeAndBoundingBox() {

        //remove acdd-style and google-style bounding box
        globalAttributes.remove("geospatial_lon_min");
        globalAttributes.remove("geospatial_lon_max");
        globalAttributes.remove("Westernmost_Easting");
        globalAttributes.remove("Easternmost_Easting");
        globalAttributes.remove("geospatial_lat_min");
        globalAttributes.remove("geospatial_lat_max");
        globalAttributes.remove("Southernmost_Northing");
        globalAttributes.remove("Northernmost_Northing");
        globalAttributes.remove("geospatial_vertical_min"); //unidata-related
        globalAttributes.remove("geospatial_vertical_max");
        globalAttributes.remove("time_coverage_start");   //unidata-related
        globalAttributes.remove("time_coverage_end");
     
        //remove actual_range
        for (int col = 0; col < nColumns(); col++) {
            Attributes atts = columnAttributes(col);
            atts.remove("actual_range");
            atts.remove("data_min");
            atts.remove("data_max");
        }

    }

    /**
     * This is called by setActualRangeAndBoundingBox to set (or revise) the actual_range metadata.
     * This works on the current (unpacked; not scaled) data. So call this when the
     * data is unpacked.
     * If the column is a String column, nothing will be done.
     *
     * @param column
     */
    public void setActualRange(int column) {
        PrimitiveArray pa = getColumn(column);
        if (pa instanceof StringArray) 
            return;
        double stats[] = pa.calculateStats();
        double min = stats[PrimitiveArray.STATS_MIN];
        double max = stats[PrimitiveArray.STATS_MAX];

        //if no data, don't specify range
        if (Double.isNaN(min)) {
            columnAttributes(column).remove("actual_range");
            return;
        }

        PrimitiveArray minMax = PrimitiveArray.factory(pa.elementClass(), 2, false);
        minMax.addDouble(min);
        minMax.addDouble(max);
        columnAttributes(column).set("actual_range", minMax);
        return;
    }

    /**
     * If attValue is null, this does nothing.
     * If attValue is "", this removes the attribute (if present).'
     * Otherwise, this sets the attribute.
     */
    private void trySet(Attributes attributes, String attName, String attValue) {
        if (attValue == null)
            return;
        if (attValue.length() == 0)
            attributes.remove(attName);
        attributes.set(attName, attValue);
    }

    /** 
     * This tests if this object is valid (e.g., each column in 'data' has 
     * the same number of rows).
     * This also checks the validity of globalAttribute or columnAttribute.
     *
     * @throws Exception if table not valid 
     */
    public void ensureValid() {

        //check that all columns have the same nRows
        int nRows = nRows();
        for (int col = 0; col < nColumns(); col++)
            if (getColumn(col).size() != nRows)
                throw new SimpleException("getColumn(" + col + ").size");

    }

    /**
     * This adds missingValues (NaN) to columns as needed so all columns have the same number of rows.
     *
     */
    public void makeColumnsSameSize() {

        //columns may have different numbers of rows (some may not have had data)
        //find maxNRows
        int maxNRows = 0;
        int tNCol = nColumns();
        for (int col = 0; col < tNCol; col++) 
            maxNRows = Math.max(getColumn(col).size(), maxNRows);

        //ensure all columns have correct maxNRows 
        if (maxNRows > 0) {
            for (int col = 0; col < tNCol; col++) {
                PrimitiveArray pa = getColumn(col);
                pa.addNDoubles(maxNRows - pa.size(), Double.NaN);
            }
        }
    }

    /**
     * This replicates the last value as needed so all columns have the same number of rows.
     *
     */
    public void ensureColumnsAreSameSize_LastValue() {

        //columns may have different numbers of rows (some may not have had data)
        //find maxNRows
        int maxNRows = 0;
        int tNCol = nColumns();
        for (int col = 0; col < tNCol; col++) 
            maxNRows = Math.max(getColumn(col).size(), maxNRows);

        //ensure all columns have correct maxNRows 
        if (maxNRows > 0) {
            for (int col = 0; col < tNCol; col++) {
                PrimitiveArray pa = getColumn(col);
                String s = pa.size() == 0? "" : pa.getString(pa.size() - 1);
                pa.addNStrings(maxNRows - pa.size(), s);
            }
        }
    }

    /** 
     * This tests if o is a Table with the same data and columnNames as this table. 
     * (Currently) This doesn't test globalAttribute or columnAttribute.
     * This requires that the column types be identical.
     *
     * @param o (usually) a Table object
     * @return true if o is a Table object and has column types and data values 
     *   that equal this Table object.
     */
    public boolean equals(Object o) {
        return equals(o, true);
    }

    /** 
     * This tests if o is a Table with the same data and columnNames as this table. 
     * (Currently) This doesn't test globalAttribute or columnAttribute.
     *
     * @param o (usually) a Table object
     * @param ensureColumnTypesEqual
     * @return true if o is a Table object and has column types and data values 
     *   that equal this Table object.
     */
    public boolean equals(Object o, boolean ensureColumnTypesEqual) {

        String errorInMethod = String2.ERROR + " in Table.equals while testing ";
        try {

            Table table2 = (Table)o;
            ensureValid(); //throws Exception if not
            table2.ensureValid();

            int nRows = nRows();
            int nColumns = nColumns();
            Test.ensureEqual(nRows, table2.nRows(), 
                errorInMethod + "nRows");
            Test.ensureEqual(nColumns(), table2.nColumns(), 
                errorInMethod + "nColumns\nthis table: " + columnNames.toString() +
                  "\nother table: " + table2.columnNames.toString());

            for (int col = 0; col < nColumns; col++) {
                Test.ensureEqual(getColumnName(col), table2.getColumnName(col),
                    errorInMethod + "column=" + col + " names.");
                PrimitiveArray array1 = getColumn(col);
                PrimitiveArray array2 = table2.getColumn(col);
                if (ensureColumnTypesEqual) 
                    Test.ensureEqual(array1.elementClassString(), array2.elementClassString(),
                        errorInMethod + "column=" + col + " types.");
                boolean stringTest = array1 instanceof StringArray ||
                    array2 instanceof StringArray;
                for (int row = 0; row < nRows; row++) {
                    if (stringTest) { 
                        //avoid generating error strings unless needed
                        if (!array1.getString(row).equals(array2.getString(row))) {
                            Test.ensureEqual(
                                array1.getString(row), array2.getString(row),
                                errorInMethod + "data(col=" + col + ", row=" + row + ").");
                        }
                    } else {
                        if (array1.getDouble(row) != array2.getDouble(row))
                            //avoid generating error strings unless needed
                            Test.ensureEqual(array1.getDouble(row), array2.getDouble(row), 
                                errorInMethod + "data(col=" + col + ", row=" + row + ").");
                    }

                }
            }

            return true;
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            return false;
        }
    }

    /** This makes a table with 2 String columns with the keys (sorted) and values
     * from the map.
     *
     * @param map  if it needs to be thread-safe, use ConcurrentHashMap
     * @param keysName
     * @param valuesName
     */
    public void readMap(Map map, String keysName, String valuesName) {
        //create the empty table
        clear();
        StringArray keys = new StringArray();
        StringArray values = new StringArray();
        addColumn(keysName, keys);
        addColumn(valuesName, values);

        //get the keys and values
        Set entrySet = map.entrySet();
        Iterator it = entrySet.iterator();
        while (it.hasNext()) {
            Map.Entry me = (Map.Entry)it.next();
            keys.add(me.getKey().toString());
            values.add(me.getValue().toString());
        }
        leftToRightSort(1);
    }


    /**
     * This reads data from an ASCII file.
     * <br>The lineSeparator can be \n, \r\n, or \r.
     * <br>See readASCII(lines, nHeaderLines) for other details.
     * <br>This does simplify the columns.
     *
     * @param fullFileName
     * @param columnNamesLine (0.., or -1 if no names)
     * @param dataStartLine (0..)
     * @param testColumns the names of the columns to be tested (null = no tests).
     *   All of the test columns must use the same, one, dimension that the
     *   loadColumns use.
     *   Ideally, the first tests will greatly restrict the range of valid rows.
     * @param testMin the minimum allowed value for each testColumn (null = no tests)
     * @param testMax the maximum allowed value for each testColumn (null = no tests)
     * @param loadColumns the names of the columns to be loaded
     *     (perhaps in different order than in the file).
     *     If loadColumns is null, this will read all of the data columns.
     * @param simplify
     * @throws Exception if trouble
     */
    public void readASCII(String fullFileName, int columnNamesLine, int dataStartLine,
        String testColumns[], double testMin[], double testMax[], 
        String loadColumns[], boolean simplify) {

        String sar[] = String2.readFromFile(fullFileName, null, 2);
        Test.ensureEqual(sar[0].length(), 0, sar[0]); //check that there was no error
        //String2.log(String2.annotatedString(sar[1]));
        readASCII(fullFileName, String2.splitNoTrim(sar[1], '\n'), columnNamesLine, dataStartLine,
            testColumns, testMin, testMax, loadColumns, simplify); 
    }
 
    /** Another variant for compatibility with older code. 
     * This uses simplify=true.
     * 
     * @throws Exception if trouble
     */
    public void readASCII(String fullFileName, int columnNamesLine, int dataStartLine,
        String testColumns[], double testMin[], double testMax[], 
        String loadColumns[]) {

        readASCII(fullFileName, columnNamesLine, dataStartLine,
            testColumns, testMin, testMax, loadColumns, true);
    }

    /** Another variant. 
     * This uses simplify=true.
     * 
     * @throws Exception if trouble
     */
    public void readASCII(String fullFileName, int columnNamesLine, int dataStartLine) {

        readASCII(fullFileName, columnNamesLine, dataStartLine,
            null, null, null, null, true);
    }

    /** Another variant. 
     * This uses columnNamesLine=0, dataStartLine=1, simplify=true.
     * 
     * @throws Exception if trouble
     */
    public void readASCII(String fullFileName) {

        readASCII(fullFileName, 0, 1, null, null, null, null, true);
    }

    /**
     * This reads data from an array of tab, comma, or space-separated ASCII Strings.
     * <ul>
     * <li> If no exception is thrown, the file was successfully read.
     * <li> The item separator on each line can be tab, comma, or 1 or more spaces.
     * <li> Missing values for tab- and comma-separated files can be "" or "." or "NaN".
     * <li> Missing values for space-separated files can be "." or "NaN".
     * <li> Normally, all data rows must have the same number of data items. 
     *    However, you can set the instance's allowRaggedRightInReadASCII to true
     *    to allow missing values at the end of a row.
     * </ul>
     *
     * @param fileName for diagnostic messages only
     * @param lines the array of ASCII strings with the info from the file
     * @param columnNamesLine (0.., or -1 if no names).
     *    If there are no columnNames, names in the form "Column#<col>" 
     *    (where col is 0 .. nColumns) will be created.
     * @param dataStartLine (0..)
     * @param testColumns the names of the columns to be tested (null = no tests).
     *   All of the test columns must use the same, one, dimension that the
     *   loadColumns use.
     *   Ideally, the first tests will greatly restrict the range of valid rows.
     * @param testMin the minimum allowed value for each testColumn (null = no tests)
     * @param testMax the maximum allowed value for each testColumn (null = no tests)
     * @param loadColumns the names of the columns to be loaded 
     *     (perhaps in different order than in the file).
     *     If null, this will read all variables.
     * @param simplify 
     *    The data is initially read as Strings. 
     *    If this is set to 'true', the columns are simplified to their simplest type
     *      (e.g., to doubles, ... or bytes) so they store the data compactly.
     *       Date strings are left as strings. 
     *    If this is set to 'false', the columns are left as strings.
     * @throws Exception if trouble  (e.g., no data)
     */
    public void readASCII(String fileName, String lines[], int columnNamesLine, int dataStartLine,
        String testColumns[], double testMin[], double testMax[], 
        String loadColumns[], boolean simplify) {

        //validate parameters
        if (verbose) String2.log("Table.readASCII " + fileName); 
        long time = System.currentTimeMillis();
        String errorInMethod = String2.ERROR + " in Table.readASCII(" + fileName + "):\n";
        if (testColumns == null)
            testColumns = new String[0];
        else {
            Test.ensureEqual(testColumns.length, testMin.length, 
                errorInMethod + "testColumns.length != testMin.length.");
            Test.ensureEqual(testColumns.length, testMax.length, 
                errorInMethod + "testColumns.length != testMax.length.");
        }

        //clear everything
        clear();

        //determine column separator
        String oneLine = lines[dataStartLine];
        char colSeparator = '\t';
        int tpo = oneLine.indexOf('\t');
        if (tpo < 0) {
            colSeparator = ',';
            tpo = oneLine.indexOf(',');
            if (tpo < 0) {
                colSeparator = ' ';
                tpo = oneLine.indexOf(' '); 
                if (tpo < 0)
                    //if tpo is still -1, only one datum per line; colSeparator irrelevant        
                    colSeparator = '\u0000';
            }
        } 

        //read the file's column names
        StringArray fileColumnNames = new StringArray();
        if (columnNamesLine >= 0) {
            oneLine = lines[columnNamesLine];
            if (oneLine.endsWith("\r"))
                oneLine = oneLine.substring(0, oneLine.length() - 1);
            oneLine = oneLine.trim();
            //break the lines into items
            String items[];
            if (colSeparator == '\u0000')
                items = new String[]{oneLine.trim()};
            else if (colSeparator == ' ')
                items = oneLine.split(" +"); //regex for one or more spaces
            else if (colSeparator == ',')
                items = StringArray.arrayFromCSV(oneLine);  //does handle "'d phrases
            else items = String2.split(oneLine, colSeparator);
            //store the fileColumnNames
            for (int col = 0; col < items.length; col++) {
                fileColumnNames.add(items[col]);
            }
            //if (verbose) String2.log("fileColumnNames=" + fileColumnNames);
        }

        //remove empty rows at end
        int nRows = lines.length - dataStartLine;
        while (nRows > 0) {
            String ts = lines[dataStartLine + nRows - 1].trim();
            if (ts.length() == 0 || ts.equals("\r"))
                nRows--;
            else break;
        }        

        //get the data
        int testColumnNumbers[] = null;
        int loadColumnNumbers[] = null;
        StringArray loadColumnSA[] = null;
        boolean missingItemNoted = false;
        int expectedNItems = -1;
        for (int row = 0; row < nRows; row++) {
            oneLine = lines[dataStartLine + row];
            if (oneLine.endsWith("\r"))
                oneLine = oneLine.substring(0, oneLine.length() - 1);

            //break the lines into items
            String items[];
            if (colSeparator == '\u0000')
                items = new String[]{oneLine.trim()};
            else if (colSeparator == ' ')
                items = oneLine.split("[ ]+"); //regex for one or more spaces  //!!!Doesn't handle "'d phrases
            else if (colSeparator == ',')
                items = StringArray.arrayFromCSV(oneLine);  //does handle "'d phrases
            else items = String2.split(oneLine, colSeparator);
            //if (verbose) String2.log("row=" + row + " nItems=" + items.length + 
            //    "\nitems=" + String2.toCSSVString(items));

            //one time things 
            if (row == 0) {
                expectedNItems = items.length;

                //make column names (if not done already) 
                for (int col = fileColumnNames.size(); col < items.length; col++) {
                    fileColumnNames.add("Column#" + col);
                }

                //identify the testColumnNumbers
                testColumnNumbers = new int[testColumns.length];
                for (int col = 0; col < testColumns.length; col++) {
                    int po = fileColumnNames.indexOf(testColumns[col], 0);
                    if (po < 0)
                       throw new IllegalArgumentException(errorInMethod + 
                           "testColumn '" + testColumns[col] + "' not found.");
                    testColumnNumbers[col] = po;
                }

                //identify the loadColumnNumbers
                if (loadColumns == null) {
                    //load all
                    loadColumnNumbers = new int[fileColumnNames.size()];
                    for (int col = 0; col < fileColumnNames.size(); col++)
                        loadColumnNumbers[col] = col;
                } else {
                    loadColumnNumbers = new int[loadColumns.length];
                    for (int col = 0; col < loadColumns.length; col++) {
                        int po = fileColumnNames.indexOf(loadColumns[col], 0);
                        if (po < 0)
                            throw new IllegalArgumentException(errorInMethod + 
                               "loadColumn '" + loadColumns[col] + "' not found.");
                        loadColumnNumbers[col] = po;
                    }
                }
                //if (verbose) String2.log("loadColumnNumbers=" + String2.toCSSVString(loadColumnNumbers));

                //generate the Table's columnNames which will be loaded
                //and create the primitiveArrays in data
                loadColumnSA = new StringArray[loadColumnNumbers.length];
                for (int col = 0; col < loadColumnNumbers.length; col++) {
                    loadColumnSA[col] = new StringArray(nRows, false); //hard to know type 
                    addColumn(fileColumnNames.get(loadColumnNumbers[col]), loadColumnSA[col]); 
                }
            }

            //ensure nItems is correct
            int nItems = items.length;
            if (!allowRaggedRightInReadASCII && expectedNItems != nItems) 
                throw new RuntimeException(errorInMethod + 
                    "unexpected number of items on line #" + 
                       (dataStartLine + row + 1) + " (observed=" + nItems + 
                       ", expected=" + expectedNItems + ").");

            //do the tests
            boolean ok = true;
            for (int test = 0; test < testColumnNumbers.length; test++) {
                double d = String2.parseDouble(items[testColumnNumbers[test]]);
                if (d >= testMin[test] && d <= testMax[test]) //NaN will fail this test
                    continue;
                else {ok = false; break; }
            }
            if (!ok) continue;
           
            //store the data items
            for (int col = 0; col < loadColumnNumbers.length; col++) {
                int itemNumber = loadColumnNumbers[col];
                if (itemNumber < nItems) {
                    loadColumnSA[col].addNotCanonical(items[itemNumber]);
                } else if (allowRaggedRightInReadASCII) {  
                    //it is a bad idea to allow this (who knows which value is missing?), 
                    //but some buoy files clearly lack the last value,
                    //see NdbcMeteorologicalStation.java
                    if (!missingItemNoted) {
                        String2.log("nonfatal " + errorInMethod + "itemNumber " + 
                            itemNumber + " not present starting on row " + (dataStartLine + row) + ".\n" +
                            "fileColNames=" + fileColumnNames + "\n" +
                            "loadColumnNumbers=" + String2.toCSSVString(loadColumnNumbers) + "\n" +
                            "line=" + String2.annotatedString(oneLine) + "\n" +
                            "items=" + String2.toCSSVString(items));
                        missingItemNoted = true;
                    }
                    loadColumnSA[col].addNotCanonical(""); //missing value
                } else {
                    throw new IllegalArgumentException(
                        errorInMethod + "itemNumber " + 
                        itemNumber + " not present starting on row " + (dataStartLine + row) + ".\n" +
                        "fileColNames=" + fileColumnNames + "\n" +
                        "loadColumnNumbers=" + String2.toCSSVString(loadColumnNumbers) + "\n" +
                        "line=" + String2.annotatedString(oneLine) + "\n" +
                        "items=" + String2.toCSSVString(items));
                }
            }
        }

        //no data?
        if (loadColumnNumbers == null)
            throw new SimpleException(MustBe.THERE_IS_NO_DATA);

        //simplify the columns
        if (simplify) 
            simplify();

        loadColumnSA = null; //get data PA's from getColumn from now on

        //canonicalize the string columns
        for (int col = 0; col < loadColumnNumbers.length; col++) {
            PrimitiveArray pa = getColumn(col);
            if (pa instanceof StringArray)
                ((StringArray)pa).makeCanonical();
        }

        if (verbose) String2.log("  Table.readASCII done. nColumns=" + nColumns() +
            " nRows=" + nRows() + " TIME=" + (System.currentTimeMillis() - time));

    }

    /**
     * This is like the other readStandardTabbedASCII, but this one actually reads the 
     * data from the file.
     *
     * @throws Exception if trouble
     */
    public void readStandardTabbedASCII(String fullFileName, String loadColumns[], boolean simplify) {

        String sar[] = String2.readFromFile(fullFileName, null, 2);
        Test.ensureEqual(sar[0].length(), 0, sar[0]); //check that there was no error
        //String2.log(String2.annotatedString(sar[1]));
        readStandardTabbedASCII(fullFileName, String2.splitNoTrim(sar[1], '\n'), 
            loadColumns, simplify); 
    }

    /**
     * This reads data from an array of tab-separated ASCII Strings.
     * This differs from readASCII in that a given field (but not the last field)
     * can have internal newlines and thus span multiple lines in the file.
     * Also, column names *must* all be on the first line and
     *  data *must* start on the second line. (There is no units line.)
     * <ul>
     * <li> If no exception is thrown, the file was successfully read.
     * <li> Missing values can be "" or "." or "NaN".
     * <li> All data rows must have the same number of data items. 
     * </ul>
     *
     * @param fileName for diagnostic messages only
     * @param lines the array of ASCII strings with the info from the file
     * @param loadColumns the names of the columns to be loaded 
     *     (perhaps in different order than in the file).
     *     If null, this will read all variables.
     * @param simplify 
     *    The data is initially read as Strings. 
     *    If this is set to 'true', the columns are simplified to their simplest type
     *      (e.g., to doubles, ... or bytes) so they store the data compactly.
     *       Date strings are left as strings. 
     *    If this is set to 'false', the columns are left as strings.
     * @throws Exception if trouble  (e.g., no data)
     */
    public void readStandardTabbedASCII(String fileName, String lines[],
        String loadColumns[], boolean simplify) {

        if (verbose) String2.log("Table.readStandardTabbedASCII " + fileName); 
        long time = System.currentTimeMillis();
        String errorInMethod = String2.ERROR + " in Table.readStandardTabbedASCII(" + fileName + "):\n";

        char colSeparator = '\t';
        int columnNamesLine = 0;
        int dataStartLine = 1;

        //clear the table
        clear();

        //read the file's column names (must be on one line)
        StringArray fileColumnNames = new StringArray();
        String oneLine = lines[columnNamesLine];
        if (oneLine.endsWith("\r"))
            oneLine = oneLine.substring(0, oneLine.length() - 1);
        oneLine = oneLine.trim();
        //break the lines into items
        String items[] = String2.split(oneLine, colSeparator);
        int nItems = items.length;
        int expectedNItems = nItems;
        for (int col = 0; col < nItems; col++) 
            fileColumnNames.add(items[col]);
        //if (verbose) String2.log("fileColumnNames=" + fileColumnNames);

        //identify the loadColumnNumbers
        int loadColumnNumbers[];
        if (loadColumns == null) {
            //load all
            loadColumnNumbers = new int[fileColumnNames.size()];
            for (int col = 0; col < fileColumnNames.size(); col++)
                loadColumnNumbers[col] = col;
        } else {
            loadColumnNumbers = new int[loadColumns.length];
            for (int col = 0; col < loadColumns.length; col++) {
                int po = fileColumnNames.indexOf(loadColumns[col], 0);
                if (po < 0)
                    throw new IllegalArgumentException(errorInMethod + 
                       "loadColumn '" + loadColumns[col] + "' not found.");
                loadColumnNumbers[col] = po;
            }
            //if (verbose) String2.log("loadColumnNumbers=" + String2.toCSSVString(loadColumnNumbers));
        }

        //remove empty rows at end
        int nRows = lines.length - dataStartLine;
        while (nRows > 0) {
            String ts = lines[dataStartLine + nRows - 1].trim();
            if (ts.length() == 0 || ts.equals("\r"))
                nRows--;
            else break;
        }        

        //generate the Table's columns which will be loaded
        //and create the primitiveArrays for loaded data
        StringArray loadColumnSA[] = new StringArray[loadColumnNumbers.length];
        for (int col = 0; col < loadColumnNumbers.length; col++) {
            loadColumnSA[col] = new StringArray(nRows, false); 
            addColumn(fileColumnNames.get(loadColumnNumbers[col]), loadColumnSA[col]); 
        }

        //get the data
        int row = 0;
        if (debug) String2.log("expectedNItems=" + expectedNItems);
        while (row < nRows) {
        if (debug) String2.log("row=" + row);
            items = null;
            nItems = 0;
            while (nItems < expectedNItems) {
                oneLine = lines[dataStartLine + row++];  //row incremented
                if (oneLine.endsWith("\r"))
                    oneLine = oneLine.substring(0, oneLine.length() - 1);

                //break the lines into items
                String tItems[] = String2.split(oneLine, colSeparator);
                int ntItems = tItems.length;
                if (debug) String2.log("row=" + (row-1) + " ntItems=" + ntItems + " tItems=" + String2.toCSSVString(tItems));
                
                if (items == null) {
                    items = tItems; 
                } else {
                    //append next line 
                    String ttItems[] = new String[nItems + ntItems - 1];
                    System.arraycopy(items, 0, ttItems, 0, nItems);
                    //combine last field old line + newline + first field new line
                    ttItems[nItems - 1] += "\n" + tItems[0]; 
                    if (ntItems > 1) 
                        System.arraycopy(tItems, 1, ttItems, nItems, ntItems - 1);
                    items = ttItems;
                }
                nItems = items.length;
            }

            //too many items?
            if (nItems > expectedNItems)
                throw new RuntimeException(errorInMethod + 
                    "unexpected number of items ending on line #" + 
                       (dataStartLine + row + 1) + " (observed=" + nItems + 
                    ", expected=" + expectedNItems + ").");            
          
            //store the data items
            for (int col = 0; col < loadColumnNumbers.length; col++) 
                loadColumnSA[col].addNotCanonical(items[loadColumnNumbers[col]]);
        }

        //simplify the columns
        if (simplify) 
            simplify();

        loadColumnSA = null; //get data PA's from getColumn from now on

        //canonicalize the string columns
        for (int col = 0; col < loadColumnNumbers.length; col++) {
            PrimitiveArray pa = getColumn(col);
            if (pa instanceof StringArray)
                ((StringArray)pa).makeCanonical();
        }

        if (verbose) String2.log("  Table.readStandardTabbedASCII done. nColumns=" + nColumns() +
            " nRows=" + nRows() + " TIME=" + (System.currentTimeMillis() - time));

    }

    /**
     * This gets data from the IOBIS web site (http://www.iobis.org)
     * by mimicing the Advanced Search form 
     * (http://www.iobis.org/OBISWEB/ObisControllerServlet)
     * which has access to a cached version
     * of all the data from all of the obis data providers/resources.
     * So it lets you get results from all data providers/resources with one request.
     * This calls setObisAttributes().
     *
     * @param url I think the only valid url is IOBIS_URL.
     * @param genus  case sensitive, use null or "" for no preference 
     * @param species case sensitive, use null or "" for no preference
     * @param west the west boundary specified in degrees east, -180 to 180; use null or "" for no preference; 
     *     west must be &lt;= east.
     * @param east the east boundary specified in degrees east, -180 to 180; use null or "" for no preference; 
     * @param south the south boundary specified in degrees north; use null or "" for no preference; 
     *     south must be &lt;= north.
     * @param north the north boundary specified in degrees north; use null or "" for no preference;  
     * @param minDepth in meters; use null or "" for no preference; 
     *     positive equals down;
     *     minDepth must be &lt;= maxDepth.
     *     Depth has few values, so generally good not to specify minDepth or maxDepth.
     * @param maxDepth in meters; use null or "" for no preference; 
     * @param startDate an ISO date string (optional time, space or T connected), use null or "" for no preference; 
     *     startDate must be &lt;= endDate.
     *     The time part of the string is used.
     * @param endDate 
     * @param loadColumns which will follow LON,LAT,DEPTH,TIME,ID (which are always loaded) 
     *    (use null to load all).
     *    <br>DEPTH is from Minimumdepth
     *    <br>TIME is from Yearcollected|Monthcollected|Daycollected|Timeofday.
     *    <br>ID is Institutioncode:Collectioncode:Catalognumber.
     *    <br>The available column names are slightly different from obis schema 
     *    (and without namespace prefixes)!
     *   <p>The loadColumns storing data as Strings are:
     *   "Res_name", "Scientificname", "Institutioncode", 
     *   "Catalognumber", "Collectioncode", "Datelastmodified", 
     *   "Basisofrecord", "Genus", "Species", "Class", "Kingdom", "Ordername", "Phylum", 
     *   "Family", "Citation", "Source", "Scientificnameauthor", "Recordurl", 
     *   "Collector", "Locality", "Country", 
     *   "Fieldnumber", "Notes", 
     *   "Ocean", "Timezone", "State", 
     *   "County", "Collectornumber", 
     *   "Identifiedby", "Lifestage", "Depthrange", "Preparationtype", "Subspecies", 
     *   "Typestatus", "Sex", "Subgenus", 
     *   "Relatedcatalogitem", "Relationshiptype", 
     *   "Previouscatalognumber", "Samplesize".
     *
     *   <p>The loadColumns storing data as doubles are 
     *   "Latitude", "Longitude", "Minimumdepth", "Maximumdepth", "Slatitude",
     *   "Starttimecollected", "Endtimecollected", "Timeofday", "Slongitude", 
     *   "Coordinateprecision", "Seprecision",
     *   "Observedweight", "Elatitude", "Elongitude", "Temperature", 
     *   "Starttimeofday", "Endtimeofday".
     *
     *   <p>The loadColumns storing data as ints are
     *   "Yearcollected", "Monthcollected", "Daycollected", 
     *   "Startyearcollected", "Startmonthcollected", "Startdaycollected",
     *   "Julianday", "Startjulianday", 
     *   "Endyearcollected", "Endmonthcollected", "Enddaycollected",
     *   "Yearidentified", "Monthidentified", "Dayidentified",
     *   "Endjulianday", "Individualcount", "Observedindividualcount".
     * @throws Exception if trouble
     */
    public void readIobis(String url, String genus, String species, 
        String west, String east, String south, String north,
        String minDepth, String maxDepth,
        String startDate, String endDate, 
        String loadColumns[]) throws Exception {

        String errorInMethod = String2.ERROR + " in Table.readIobis: ";
        clear();
        if (genus     == null) genus = "";   
        if (species   == null) species = "";
        if (startDate == null) startDate = "";
        if (endDate   == null) endDate = "";
        if (minDepth  == null) minDepth = "";
        if (maxDepth  == null) maxDepth = "";
        if (south     == null) south = "";
        if (north     == null) north = "";
        if (west      == null) west = "";
        if (east      == null) east = "";
        //The web site javascript tests that the constraints are supplied in pairs
        //(and NESW must be all or none).
        //I verified: if e.g., leave off north value, south is ignored.
        //So fill in missing values where needed.
        if (west.length() == 0) west = "-180";
        if (east.length() == 0) east = "180";
        if (south.length() == 0) south = "-90";
        if (north.length() == 0) north = "90";
        if (minDepth.length() >  0 && maxDepth.length() == 0) maxDepth = "10000";
        if (minDepth.length() == 0 && maxDepth.length() >  0) minDepth = "0";
        if (startDate.length() >  0 && endDate.length() == 0) endDate   = "2100-01-01";
        if (startDate.length() == 0 && endDate.length() >  0) startDate = "1500-01-01";
        //I HAVE NEVER GOTTEN STARTDATE ENDDATE REQUESTS TO WORK,
        //SO I DO THE TEST MANUALLY (BELOW) AFTER GETTING THE DATA.
        //convert iso date time format to their format e.g., 1983/12/31
        //if (startDate.length() > 10) startDate = startDate.substring(0, 10);
        //if (endDate.length()   > 10) endDate   = endDate.substring(0, 10);
        //startDate = String2.replaceAll(startDate, "-", "/"); 
        //endDate   = String2.replaceAll(endDate,   "-", "/");

        //submit the request
        String userQuery = //the part specified by the user
            "&genus="   + genus +   
            "&species=" + species +
            "&date1="   + "" + //startDate + 
            "&date2="   + "" + //endDate +       
            "&depth1="  + minDepth +     
            "&depth2="  + maxDepth +
            "&south="   + south + "&ss=N" +
            "&north="   + north + "&nn=N" + 
            "&west="    + west + "&ww=E" +   
            "&east="    + east + "&ee=E";  
        String glue = "?site=null&sbox=null&searchCategory=/AdvancedSearchServlet"; 
        if (verbose) String2.log("url=" + url + glue + userQuery);
        String response = SSR.getUrlResponseString(url + glue + userQuery);
        //String2.log(response);

        //in the returned web page, search for .txt link, read into tTable
        int po2 = response.indexOf(".txt'); return false;\">.TXT</a>"); //changed just before 2007-09-10
        int po1 = po2 == -1? -1 : response.lastIndexOf("http://", po2);
        if (po1 < 0) {
            String2.log(errorInMethod + ".txt link not found in OBIS response; " +
                "probably because no data was found.\nresponse=" + response);
            return;
        }
        String url2 = response.substring(po1, po2 + 4);
        if (verbose) String2.log("url2=" + url2);

        //get the .txt file
        String dataLines[] = SSR.getUrlResponse(url2);
        int nLines = dataLines.length;
        for (int line = 0; line < nLines; line++) 
            dataLines[line] = String2.replaceAll(dataLines[line], '|', '\t');

        //read the data into a temporary table
        Table tTable = new Table();
        tTable.readASCII(url2, dataLines, 0, 1, //columnNamesLine, int dataStartLine,
            null, null, null, //constraints
            null, false); //just load all the columns, and don't simplify

        //immediatly remove 'index'
        if (tTable.getColumnName(0).equals("index"))
            tTable.removeColumn(0);

        //convert double columns to doubles, int columns to ints
        int nRows = tTable.nRows();
        int nCols = tTable.nColumns();
        //I wasn't super careful with assigning to Double or Int
        String doubleColumns[] = {"Latitude", "Longitude", "Minimumdepth", "Maximumdepth", "Slatitude",
            "Starttimecollected", "Endtimecollected", "Timeofday", "Slongitude", 
            "Coordinateprecision", "Seprecision",
            "Observedweight", "Elatitude", "Elongitude", "Temperature", 
            "Starttimeofday", "Endtimeofday"}; 
        String intColumns[] = {"Yearcollected", "Monthcollected", "Daycollected", 
            "Startyearcollected", "Startmonthcollected", "Startdaycollected",
            "Julianday", "Startjulianday", 
            "Endyearcollected", "Endmonthcollected", "Enddaycollected",
            "Yearidentified", "Monthidentified", "Dayidentified",
            "Endjulianday", "Individualcount", "Observedindividualcount"};
        for (int col = 0; col < nCols; col++) {
            String s = tTable.getColumnName(col);
            if (String2.indexOf(doubleColumns, s) >= 0)
                tTable.setColumn(col, new DoubleArray(tTable.getColumn(col)));
            if (String2.indexOf(intColumns, s) >= 0)
                tTable.setColumn(col, new IntArray(tTable.getColumn(col)));
        }

        //create and add x,y,z,t,id columns    (numeric cols forced to be doubles)
        addColumn(DataHelper.TABLE_VARIABLE_NAMES[0], new DoubleArray(tTable.findColumn("Longitude")));
        addColumn(DataHelper.TABLE_VARIABLE_NAMES[1], new DoubleArray(tTable.findColumn("Latitude")));
        addColumn(DataHelper.TABLE_VARIABLE_NAMES[2], new DoubleArray(tTable.findColumn("Minimumdepth")));
        DoubleArray tPA = new DoubleArray(nRows, false);
        addColumn(DataHelper.TABLE_VARIABLE_NAMES[3], tPA);
        StringArray idPA = new StringArray(nRows, false);
        addColumn(DataHelper.TABLE_VARIABLE_NAMES[4], idPA);
        PrimitiveArray yearPA      = tTable.findColumn("Yearcollected");
        PrimitiveArray monthPA     = tTable.findColumn("Monthcollected");
        PrimitiveArray dayPA       = tTable.findColumn("Daycollected");
        PrimitiveArray timeOfDayPA = tTable.findColumn("Timeofday");
//        PrimitiveArray timeZonePA = findColumn("Timezone"); //deal with ???
        //obis schema says to construct id as
        //"URN:catalog:[InstitutionCode]:[CollectionCode]:[CatalogNumber]"  but their example is more terse than values I see
        PrimitiveArray insPA       = tTable.findColumn("Institutioncode");
        PrimitiveArray colPA       = tTable.findColumn("Collectioncode");
        PrimitiveArray catPA       = tTable.findColumn("Catalognumber");
        for (int row = 0; row < nRows; row++) {
            //make the t value
            double seconds = Double.NaN;
            StringBuilder sb = new StringBuilder(yearPA.getString(row));
            if (sb.length() > 0) {
                String tMonth = monthPA.getString(row);
                if (tMonth.length() > 0) {
                    sb.append("-" + tMonth);  //month is 01 - 12 
                    String tDay = dayPA.getString(row);
                    if (tDay.length() > 0) {
                        sb.append("-" + tDay);
                    }
                }
                try { 
                    seconds = Calendar2.isoStringToEpochSeconds(sb.toString()); 
                    String tTime = timeOfDayPA.getString(row);  //decimal hours since midnight
                    int tSeconds = Math2.roundToInt(
                        String2.parseDouble(tTime) * Calendar2.SECONDS_PER_HOUR);
                    if (tSeconds < Integer.MAX_VALUE)
                        seconds += tSeconds;
                } catch (Exception e) {
                    if (verbose) String2.log("Table.readObis unable to parse date=" + sb);
                }
            }
            tPA.add(seconds);

            //make the id value
            idPA.add(insPA.getString(row) + ":" + colPA.getString(row) + ":" + catPA.getString(row));

        }

        //if loadColumns == null, make loadColumns with all the original column names ('index' already removed)
        if (loadColumns == null) 
            loadColumns = tTable.getColumnNames();   

        //add the loadColumns
        for (int col = 0; col < loadColumns.length; col++) 
            addColumn(loadColumns[col], tTable.findColumn(loadColumns[col]));

        //no more need for tTable
        tTable = null;

        //do startDate endDate test (if one is specified, both are)
        if (startDate.length() > 0) 
            subset(new int[]{3}, 
                new double[]{Calendar2.isoStringToEpochSeconds(startDate)}, 
                new double[]{Calendar2.isoStringToEpochSeconds(endDate)});

        //remove time=NaN rows? no, it gets rid of otherwise intesting data rows


        //setAttributes  (this sets the coordinate variables' axis, long_name, standard_name, and units)
//add column attributes from DigirDarwin.properties and DigirObis.properties?
        setObisAttributes(0,1,2,3, url, new String[]{"AdvancedQuery"}, userQuery);
   
    }

    /**
     * This sets obis attributes to this table.
     * Currently, this is more geared to the http://www.iobis.org/ portal
     * than it should be -- but I'm don't see how to generalize
     * for all possible DiGIR/Darwin/OBIS providers without a bigger
     * infrastructure for metadata info.
     * This correctly deals with attributes that have already been set.
     *
     * @param lonColumn or -1 if none
     * @param latColumn or -1 if none
     * @param depthColumn or -1 if none
     * @param timeColumn or -1 if none
     * @param url the url that was queried
     * @param resources the resources that were queried
     * @param querySummary a summary of the query
     */
    public void setObisAttributes(int lonColumn, int latColumn, int depthColumn,
        int timeColumn, String url, String resources[], String querySummary) {

        String courtesy = "OBIS, and the Darwin and OBIS Data Providers (" + 
            url + " : " + String2.toCSSVString(resources) + ")";
        String disCit = 
            "users acknowledge the OBIS disclaimer (http://www.iobis.org/data/policy/disclaimer/) " +
            "and agree to follow the OBIS citation policy (http://www.iobis.org/data/policy/citation/).";
        String agree = 
            //not appropriate if a non-iobis site is the source
            //but the intent and basic info is always appropriate
            "  By using data accessed from " + courtesy + ", " + disCit;
        setAttributes(lonColumn, latColumn, depthColumn, timeColumn,
            "Ocean Biogeographic Information System",  //boldTitle
            "Point", //cdmDataType 
            DataHelper.ERD_CREATOR_EMAIL, 
            DataHelper.ERD_CREATOR_NAME,
            DataHelper.ERD_CREATOR_URL,
            DataHelper.ERD_PROJECT,
            "OBIS_" + String2.md5Hex12(url + String2.toCSSVString(resources) + querySummary), 
            "GCMD Science Keywords",
            "Oceans > Marine Biology", //not correct if a darwin provider searched for non-oceanography data
            "http://www.iobis.org/ and " + url + " (" + String2.toCSSVString(resources) + ")", //references,   
            //summary  from http://www.iobis.org/about/     //not appropriate if non-obis
            "The Ocean Biogeographic Information System (OBIS) is the information " +
            "component of the Census of Marine Life (CoML), a growing network of " +
            "more than 1000 researchers in 73 nations engaged in a 10-year initiative " +
            "to assess and explain the diversity, distribution, and abundance of " +
            "life in the oceans - past, present, and future.  OBIS is a web-based " +
            "provider of global geo-referenced information on marine species. " +
            "We contain expert species level and habitat level databases and " +
            "provide a variety of spatial query tools for visualizing relationships " +
            "among species and their environment. OBIS strives to assess and " +
            "integrate biological, physical, and chemical oceanographic data from " +
            "multiple sources. Users of OBIS, including researchers, students, " +
            "and environmental managers, will gain a dynamic view of the " +
            "multi-dimensional oceanic world. You can explore this constantly " +
            "expanding and developing facility through the OBIS Portal (http://www.iobis.org/).", 

            courtesy, 
            null); //timeLongName

        //customize a little more
        globalAttributes.set("history", 
            DataHelper.addBrowserToHistory(
                Calendar2.getCurrentISODateStringZulu() + " " + 
                courtesy + " (" + querySummary + ")"));
      
        String license = globalAttributes.getString("license");
        if (license.indexOf(disCit) < 0)  //agree may change but disCit ending is constant
            globalAttributes.set("license", license + agree);
      
        String ack = globalAttributes.getString("acknowledgement");
        if (ack.indexOf(courtesy) < 0) 
            globalAttributes.set("acknowledgement", ack + ", " + courtesy);

        String lasConvention = "LAS Intermediate netCDF File";
        String con = globalAttributes.getString("Conventions");
        if (con.indexOf(lasConvention) < 0) 
            globalAttributes.set("Conventions", con + ", " + lasConvention);

    }
    


    /**
     * This tests readIObis.
     */
    public static void testIobis() throws Exception {
        verbose = true;
        reallyVerbose = true;
        String2.log("\n*** testIobis");
        String testName = "c:/programs/digir/Macrocyctis.nc";
        Table table = new Table();
        if (true) {
            table.readIobis(IOBIS_URL, "Macrocystis", "", //String genus, String species, 
                "" , "", "53", "54", //String west, String east, String south, String north,
                "", "", //String minDepth, String maxDepth,
                "1970-01-01", "",//String iso startDate, String iso endDate, 
                new String[]{"Institutioncode", "Collectioncode", "Scientificname", "Temperature"}); //String loadColumns[])

//            table.saveAsFlatNc(testName, "row");
        } else {
            table.readFlatNc(testName, null, 1);
        }
        String2.log(table.toString());
        table.testObis5354Table();

    }

    /**
     * This tests that the values in this table are the expected results from 
     * the typical obis "Macrocystis", time 1970+, lat 53.. 54 request.
     */
    public void testObis5354Table() {
        String2.log("\nTable.testObis5354Table...");
        leftToRightSort(5);

        Test.ensureTrue(nRows() >= 30, "nRows=" + nRows());
        Test.ensureEqual(nColumns(), 9, "");
        if (String2.toCSSVString(getColumnNames()).equals(
            "LON, LAT, DEPTH, TIME, ID, " +
            "darwin:InstitutionCode, darwin:CollectionCode, " +
            "darwin:ScientificName, obis:Temperature")) {
        } else if (String2.toCSSVString(getColumnNames()).equals(
            "LON, LAT, DEPTH, TIME, ID, " +
            "Institutioncode, Collectioncode, " +
            "Scientificname, Temperature")) {
        } else throw new RuntimeException(
            "Unexpected col names: " + String2.toCSSVString(getColumnNames()));

        //!!!note that from GHMP request, rows of data are in pairs of almost duplicates
        //and CollectionCode includes 2 sources -- 1 I requested and another one (both served by GHMP?)
        //and Lat and Lon can be slightly different (e.g., row 60/61 lat)
        DoubleArray latCol = (DoubleArray)getColumn(1);
        double stats[] = latCol.calculateStats();
        Test.ensureTrue(stats[PrimitiveArray.STATS_MIN] >= 53, "min=" + stats[PrimitiveArray.STATS_MIN]);
        Test.ensureTrue(stats[PrimitiveArray.STATS_MAX] <= 54, "max=" + stats[PrimitiveArray.STATS_MAX]);
        Test.ensureEqual(stats[PrimitiveArray.STATS_N], nRows(), "");

        //test time > 0  (1970-01-01)
        DoubleArray timeCol = (DoubleArray)getColumn(3);
        stats = timeCol.calculateStats();
        Test.ensureTrue(stats[PrimitiveArray.STATS_MIN] >= 0, "min=" + stats[PrimitiveArray.STATS_MIN]);
        Test.ensureEqual(stats[PrimitiveArray.STATS_N], nRows(), "");

        DoubleArray lonCol = (DoubleArray)getColumn(0); //==0
        int row = lonCol.indexOf("-132.4223");  
        Test.ensureEqual(getDoubleData(0, row), -132.4223, "");
        Test.ensureEqual(getDoubleData(1, row), 53.292, "");
        Test.ensureEqual(getDoubleData(2, row), Double.NaN, "");
        Test.ensureEqual(getDoubleData(3, row), 347155200, "");
        Test.ensureEqual(getStringData(4, row), 
            "BIO:GHMP:10036-MACRINT", "");
        Test.ensureEqual(getStringData(5, row), "BIO", "");
        Test.ensureEqual(getStringData(6, row), "GHMP", "");
        Test.ensureEqual(getStringData(7, row), "Macrocystis integrifolia", "");
        Test.ensureEqual(getDoubleData(8, row), Double.NaN, "");
/* duplicates (described above) disappeared 2007-09-04
        row++;  
        Test.ensureEqual(getDoubleData(0, row), -132.4223, "");
        Test.ensureEqual(getDoubleData(1, row), 53.292, "");
        Test.ensureEqual(getDoubleData(2, row), Double.NaN, "");
        Test.ensureEqual(getDoubleData(3, row), 347155200, "");
        Test.ensureEqual(getStringData(4, row), 
            "Marine Fish Division, Fisheries and Oceans Canada:Gwaii Haanas Marine Algae:10036-MACRINT", "");
        Test.ensureEqual(getStringData(5, row), "Marine Fish Division, Fisheries and Oceans Canada", "");
        Test.ensureEqual(getStringData(6, row), "Gwaii Haanas Marine Algae", "");
        Test.ensureEqual(getStringData(7, row), "Macrocystis integrifolia", "");
        Test.ensureEqual(getDoubleData(8, row), Double.NaN, "");
*/
        row = lonCol.indexOf("-132.08171"); 
        Test.ensureEqual(getDoubleData(0, row), -132.08171, "");
        Test.ensureEqual(getDoubleData(1, row), 53.22519, "");
        Test.ensureEqual(getDoubleData(2, row), Double.NaN, "");
        Test.ensureEqual(getDoubleData(3, row), 63072000, "");
        Test.ensureEqual(getStringData(4, row), 
            "BIO:GHMP:198-MACRINT", "");
        Test.ensureEqual(getStringData(5, row), "BIO", "");
        Test.ensureEqual(getStringData(6, row), "GHMP", "");
        Test.ensureEqual(getStringData(7, row), "Macrocystis integrifolia", "");
        Test.ensureEqual(getDoubleData(8, row), Double.NaN, "");
/*
        row++; 
        Test.ensureEqual(getDoubleData(0, row), -132.08171, "");
        Test.ensureEqual(getDoubleData(1, row), 53.225193, "");
        Test.ensureEqual(getDoubleData(2, row), Double.NaN, "");
        Test.ensureEqual(getDoubleData(3, row), 63072000, "");
        Test.ensureEqual(getStringData(4, row), 
            "Marine Fish Division, Fisheries and Oceans Canada:Gwaii Haanas Marine Algae:198-MACRINT", "");
        Test.ensureEqual(getStringData(5, row), "Marine Fish Division, Fisheries and Oceans Canada", "");
        Test.ensureEqual(getStringData(6, row), "Gwaii Haanas Marine Algae", "");
        Test.ensureEqual(getStringData(7, row), "Macrocystis integrifolia", "");
        Test.ensureEqual(getDoubleData(8, row), Double.NaN, "");
*/

        String2.log("Table.testObis5354Table finished successfully.");
    }



    /**
     * This reads (and flattens) an xml document and populates this table.
     * See TableXmlHandler for details.
     *
     * @param xml the BufferedReader with access to the xml information
     * @param validate indicates if the XML parser should validate the xml
     *    against the .dtd specified by DOCTYPE in the file
     *    (see http://www.w3.org/TR/REC-xml#proc-types).
     *    true or false, the XMLReader always insists that the document be well formed.
     *    true or false, the XMLReader doesn't validate against a schema.
     *    The validate parameter will be ignored if the XMLReader doesn't support
     *    validation. (It is a good sign that, on Windows, the XMLReader that 
     *    comes with Java seems to support validation, or at least doesn't
     *    object to being told to validate the xml.)
     * @param rowElementXPath the element (XPath style) identifying a row, 
     *    e.g., /response/content/record.
     * @param rowElementAttributes are the attributes of the row element (e.g., "name")
     *    which will be noted and stored in columns of the table.
     *    May be null if none.
     *    (Other element's attributes are ignored.)
     *    E.g., <row name="Nate"> will cause a column called name to be
     *    created (with value "Nate" for this example row).
     * @param simplify 'true' simplifies the columns to their simplest type
     *    (without regard for what any schema says);
     *    'false' leaves the columns as StringArrays.
     * @throws Exception if trouble
     */
    public void readXml(Reader xml, boolean validate, String rowElementXPath, 
        String rowElementAttributes[], boolean simplify) throws Exception {

        //I had written an JDom + XPath version of readXml. It took 22 s! for a large file.
        //This SAX-based version takes 140 ms!
        //And this version requires a fraction of the memory
        //  (JDom version holds entire doc in place and with zillions of objects 
        //  pointing to parts).

        long time = System.currentTimeMillis();
        clear();  
        
        //get the XMLReader
        XMLReader xr = TableXmlHandler.getXmlReader(this, validate, rowElementXPath,
            rowElementAttributes);
        xr.parse(new InputSource(xml));

        //simplify the columns
        if (simplify) 
            simplify();

        if (verbose) String2.log("  Table.readXml done. nColumns=" + nColumns() +
            " nRows=" + nRows() + 
            " TIME=" + (System.currentTimeMillis() - time));
    }

    /**
     * This tests readXml.
     */
    public static void testXml() throws Exception {
        verbose = true;
        reallyVerbose = true;

//Lines are commented out to test some aspects of readXml.
String xml = 
"<?xml version='1.0' encoding='utf-8' ?>\n" +
"<response\n" +
"  xmlns=\"http://digir.net/schema/protocol/2003/1.0\" \n" +
"  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" \n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
"  xmlns:darwin=\"http://digir.net/schema/conceptual/darwin/2003/1.0\" \n" +
"  xmlns:obis=\"http://www.iobis.org/obis\" >\n" +
"<header>\n" +
"<version>$Revision: 1.12 $</version>\n" +
"</header>\n" + 
"<content><record>\n" +
//"<darwin:InstitutionCode>Marine Fish Division, Fisheries and Oceans Canada</darwin:InstitutionCode>\n" +
"<darwin:CollectionCode>Gwaii Haanas Marine Algae</darwin:CollectionCode>\n" +
"<darwin:CatalogNumber>100-MACRINT</darwin:CatalogNumber>\n" +
"<darwin:ScientificName>Macrocystis integrifolia</darwin:ScientificName>\n" +
"<darwin:ScientificName>sciName2</darwin:ScientificName>\n" +
"<darwin:Latitude>52.65172</darwin:Latitude>\n" +
"<darwin:Longitude>-131.66368</darwin:Longitude>\n" +
"<obis:Temperature xsi:nil='true'/>\n" +
"<parent>\n" +
"  <child1>child1data</child1>\n" +
"  <child2>child2data</child2>\n" +
"  <child2>child22data</child2>\n" +
"</parent>\n" +
"</record><record><darwin:InstitutionCode>BIO</darwin:InstitutionCode>\n" +
"<darwin:CollectionCode>GHMP</darwin:CollectionCode>\n" +
"<darwin:CatalogNumber>100-MACRINT</darwin:CatalogNumber>\n" +
"<darwin:ScientificName>Macrocystis integrifolia</darwin:ScientificName>\n" +
"<darwin:Latitude>52.65172</darwin:Latitude>\n" +
"<darwin:Longitude>-131.66368</darwin:Longitude>\n" +
"<obis:Temperature xsi:nil='true'/>\n" +
"</record><record>\n" +
//"<darwin:InstitutionCode>Marine Fish Division, Fisheries and Oceans Canada</darwin:InstitutionCode>\n" +
"<darwin:CollectionCode>Gwaii Haanas Marine Algae</darwin:CollectionCode>\n" +
"<darwin:CatalogNumber>10036-MACRINT</darwin:CatalogNumber>\n" +
"<darwin:ScientificName>Macrocystis integrifolia</darwin:ScientificName>\n" +
"<darwin:Latitude>53.292</darwin:Latitude>\n" +
"<darwin:Longitude>-132.4223</darwin:Longitude>\n" +
"<obis:Temperature xsi:nil='true'/>\n" +
"</record></content>\n" +
"<diagnostics>\n" +
"</diagnostics></response>\n";

//this doesn't test heirarchical elements
        Table table = new Table();
        table.readXml(new BufferedReader(new StringReader(xml)), 
            false, //no validate since no .dtd
            "/response/content/record", null, true);
        table.ensureValid(); //throws Exception if not
        Test.ensureEqual(table.nRows(), 3, "");
        Test.ensureEqual(table.nColumns(), 10, "");
        Test.ensureEqual(table.getColumnName(0), "darwin:CollectionCode", "");
        Test.ensureEqual(table.getColumnName(1), "darwin:CatalogNumber", "");
        Test.ensureEqual(table.getColumnName(2), "darwin:ScientificName", "");
        Test.ensureEqual(table.getColumnName(3), "darwin:ScientificName2", "");
        Test.ensureEqual(table.getColumnName(4), "darwin:Latitude", "");
        Test.ensureEqual(table.getColumnName(5), "darwin:Longitude", "");
        //Test.ensureEqual(table.getColumnName(5), "obis:Temperature", ""); //no data, so no column
        Test.ensureEqual(table.getColumnName(6), "parent/child1", ""); 
        Test.ensureEqual(table.getColumnName(7), "parent/child2", ""); 
        Test.ensureEqual(table.getColumnName(8), "parent/child22", ""); 
        Test.ensureEqual(table.getColumnName(9), "darwin:InstitutionCode", "");
        Test.ensureEqual(table.getColumn(0).elementClassString(), "String", "");
        Test.ensureEqual(table.getColumn(1).elementClassString(), "String", "");
        Test.ensureEqual(table.getColumn(2).elementClassString(), "String", "");
        Test.ensureEqual(table.getColumn(3).elementClassString(), "String", "");
        Test.ensureEqual(table.getColumn(4).elementClassString(), "float", "");
        Test.ensureEqual(table.getColumn(5).elementClassString(), "double", "");
        Test.ensureEqual(table.getColumn(6).elementClassString(), "String", "");
        Test.ensureEqual(table.getColumn(7).elementClassString(), "String", "");
        Test.ensureEqual(table.getColumn(8).elementClassString(), "String", "");
        Test.ensureEqual(table.getColumn(9).elementClassString(), "String", "");
        Test.ensureEqual(table.getStringData(0, 2), "Gwaii Haanas Marine Algae", "");
        Test.ensureEqual(table.getStringData(1, 2), "10036-MACRINT", "");
        Test.ensureEqual(table.getStringData(2, 2), "Macrocystis integrifolia", "");
        Test.ensureEqual(table.getStringData(3, 0), "sciName2", "");
        Test.ensureEqual(table.getFloatData(4, 2), 53.292f, "");
        Test.ensureEqual(table.getFloatData(5, 2), -132.4223f, "");
        Test.ensureEqual(table.getStringData(6, 0), "child1data", "");
        Test.ensureEqual(table.getStringData(7, 0), "child2data", "");
        Test.ensureEqual(table.getStringData(8, 0), "child22data", "");
        Test.ensureEqual(table.getStringData(9, 0), "", "");
        Test.ensureEqual(table.getStringData(9, 1), "BIO", "");
        Test.ensureEqual(table.getStringData(9, 2), "", "");

//a subset of http://opendap.co-ops.nos.noaa.gov/stations/stationsXML.jsp
String stationsXml = 
"<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
"<stations xmlns=\"http://opendap.co-ops.nos.noaa.gov/stations/\" \n" +
"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
"xsi:schemaLocation=\"http://opendap.co-ops.nos.noaa.gov/stations/   xml_schemas/stations.xsd\"> \n" +
"<station name=\"DART BUOY 46419\" ID=\"1600013\" >\n" +
"<metadata>\n" +
"<location>\n" +
"<lat> 48 28.7 N </lat>\n" +
"<long> 129 21.5 W </long>\n" +
"<state>   </state>\n" +
"</location>\n" +
"<date_established> 2003-01-01 </date_established>\n" +
"</metadata>\n" +
"</station>\n" +
"<station name=\"DART BUOY 46410\" ID=\"1600014\" >\n" +
"<metadata>\n" +
"<location>\n" +
"<lat> 57 29.9 N </lat>\n" +
"<long> 144 0.06 W </long>\n" +
"<state>   </state>\n" +
"</location>\n" +
"<date_established> 2001-01-01 </date_established>\n" +
"</metadata>\n" +
"</station>\n" +
"<station name=\"Magueyes Island\" ID=\"9759110\" >\n" +
"<metadata>\n" +
"<location>\n" +
"<lat> 17 58.3 N </lat>\n" +
"<long> 67 2.8 W </long>\n" +
"<state> PR </state>\n" +
"</location>\n" +
"<date_established> 1954-12-01 </date_established>\n" +
"</metadata>\n" +
"<parameter name=\"Water Level\" sensorID=\"A1\" DCP=\"1\" status=\"1\" />\n" +
"<parameter name=\"Winds\" sensorID=\"C1\" DCP=\"1\" status=\"1\" />\n" +
"<parameter name=\"Air Temp\" sensorID=\"D1\" DCP=\"1\" status=\"1\" />\n" +
"<parameter name=\"Water Temp\" sensorID=\"E1\" DCP=\"1\" status=\"1\" />\n" +
"<parameter name=\"Air Pressure\" sensorID=\"F1\" DCP=\"1\" status=\"1\" />\n" +
"</station>\n" +
"</stations>\n";
        table.clear();
        table.readXml(new BufferedReader(new StringReader(stationsXml)), 
            false, //no validate since no .dtd
            "/stations/station", new String[]{"name", "ID"}, true);
        table.ensureValid(); //throws Exception if not
        String2.log(table.toString());
//    Row            name             ID metadata/locat metadata/locat metadata/date_ metadata/locat
//      0  DART BUOY 4641        1600013      48 28.7 N     129 21.5 W     2003-01-01
//      1  DART BUOY 4641        1600014      57 29.9 N     144 0.06 W     2001-01-01
//      2  Magueyes Islan        9759110      17 58.3 N       67 2.8 W     1954-12-01             PR
        Test.ensureEqual(table.nRows(), 3, "");
        Test.ensureEqual(table.nColumns(), 6, "");
        Test.ensureEqual(table.getColumnName(0), "name", "");
        Test.ensureEqual(table.getColumnName(1), "ID", "");
        Test.ensureEqual(table.getColumnName(2), "metadata/location/lat", "");
        Test.ensureEqual(table.getColumnName(3), "metadata/location/long", "");
        Test.ensureEqual(table.getColumnName(4), "metadata/date_established", "");
        Test.ensureEqual(table.getColumnName(5), "metadata/location/state", "");
        Test.ensureEqual(table.getColumn(0).elementClassString(), "String", "");
        Test.ensureEqual(table.getColumn(1).elementClassString(), "int", "");
        Test.ensureEqual(table.getColumn(2).elementClassString(), "String", "");
        Test.ensureEqual(table.getColumn(3).elementClassString(), "String", "");
        Test.ensureEqual(table.getColumn(4).elementClassString(), "String", "");
        Test.ensureEqual(table.getColumn(5).elementClassString(), "String", "");
        Test.ensureEqual(table.getStringData(0, 0), "DART BUOY 46419", "");
        Test.ensureEqual(table.getStringData(1, 0), "1600013", "");
        Test.ensureEqual(table.getStringData(2, 0), "48 28.7 N", "");
        Test.ensureEqual(table.getStringData(3, 0), "129 21.5 W", "");
        Test.ensureEqual(table.getStringData(4, 0), "2003-01-01", "");
        Test.ensureEqual(table.getStringData(5, 0), "", "");
        Test.ensureEqual(table.getStringData(0, 2), "Magueyes Island", "");
        Test.ensureEqual(table.getStringData(1, 2), "9759110", "");
        Test.ensureEqual(table.getStringData(2, 2), "17 58.3 N", "");
        Test.ensureEqual(table.getStringData(3, 2), "67 2.8 W", "");
        Test.ensureEqual(table.getStringData(4, 2), "1954-12-01", "");
        Test.ensureEqual(table.getStringData(5, 2), "PR", "");
 
    }


    /**
     * This writes the table's data attributes (as if it were a DODS Sequence) 
     * to the outputStream as an DODS DAS (see www.opendap.org, DAP 2.0, 7.2.1).
     * Note that the table does needs columns (and their attributes),
     * but it doesn't need any rows of data.
     * See writeDAS.
     *
     * @param outputStream the outputStream to receive the results (will be encoded as ISO-8859-1).
     *    Afterwards, it is flushed, not closed.
     * @param sequenceName  e.g., "bottle_data_2002"
     * @throws Exception  if trouble. 
     */
    public void saveAsDAS(OutputStream outputStream, String sequenceName) throws Exception {

        if (verbose) String2.log("  Table.saveAsDAS"); 
        long time = System.currentTimeMillis();
        Writer writer = new OutputStreamWriter(
            //DAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common 8bit
            outputStream, "ISO-8859-1"); 
        writeDAS(writer, sequenceName, false);

        //diagnostic
        if (verbose)
            String2.log("  Table.saveAsDAS done. TIME=" + 
                (System.currentTimeMillis() - time));
    }

    /**
     * This writes the table's data attributes (as if it were a DODS Sequence) 
     * to the outputStream as an DODS DAS (see www.opendap.org, DAP 2.0, 7.2.1).
     * Note that the table does needs columns (and their attributes),
     * but it doesn't need any rows of data.
     * E.g. from http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle.das
<pre>
Attributes {
    bottle_data_2002 {
        date {
            String long_name "Date";
        }
        ship {
            String long_name "Ship";
        }
        day {
            String long_name "Day";
        }
        year {
            String long_name "Year";
        }
        lat {
            String long_name "Latitude";
        }
        lon {
            String long_name "Longitude";
        }
        chl_a_total {
            String long_name "Chlorophyll-a";
        }
    }
}
</pre> 
     *
     * @param writer the Writer to receive the results.
     *    Afterwards, it is flushed, not closed.
     * @param sequenceName  e.g., "bottle_data_2002"
     * @param encodeAsHtml if true, characters like &lt; are converted to their 
     *    character entities and lines wrapped with \n if greater than 78 chars.
     * @throws Exception  if trouble. 
     */
    public void writeDAS(Writer writer, String sequenceName, boolean encodeAsHtml) throws Exception {

        writer.write("Attributes {" + OpendapHelper.EOL); //see EOL definition for comments
        writer.write(" " + XML.encodeAsHTML(sequenceName, encodeAsHtml) + " {" + OpendapHelper.EOL); //see EOL definition for comments
        for (int v = 0; v < nColumns(); v++) 
            OpendapHelper.writeToDAS(getColumnName(v),
                columnAttributes(v), writer, encodeAsHtml);
        writer.write(" }" + OpendapHelper.EOL); //see EOL definition for comments 

        //how do global attributes fit into opendap view of attributes?
        OpendapHelper.writeToDAS(
            "NC_GLOBAL", //DAP 2.0 spec doesn't talk about global attributes, was "GLOBAL"; ncBrowse and netcdf-java treat NC_GLOBAL as special case
            globalAttributes, writer, encodeAsHtml);
        writer.write("}" + OpendapHelper.EOL); //see EOL definition for comments 
        writer.flush(); //essential

    }


    /**
     * This writes the table's data structure (as if it were a DODS Sequence) 
     * to the outputStream as an DODS DDS (see www.opendap.org, DAP 2.0, 7.2.2).
     * Note that the table does needs columns (and their attributes),
     * but it doesn't need any rows of data.
     * E.g. from http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle.dds
     <pre>
Dataset {
    Sequence {
        String date;
        String ship;
        Byte month;
        Byte day;
        Int16 year;
        Int16 time;
        Float64 lat;
        Float64 lon;
        Float64 chl_a_total;
    } bottle_data_2002;
} bottle_data_2002; </pre>
     * 
     * @param outputStream the outputStream to receive the results.
     *    Afterwards, it is flushed, not closed.
     * @param sequenceName  e.g., "bottle_data_2002"
     * @throws Exception  if trouble. 
     */
    public void saveAsDDS(OutputStream outputStream, String sequenceName) throws Exception {

        if (verbose) String2.log("  Table.saveAsDDS"); 
        long time = System.currentTimeMillis();
        Writer writer = new OutputStreamWriter(outputStream);

        int nColumns = nColumns();
        writer.write("Dataset {" + OpendapHelper.EOL); //see EOL definition for comments
        writer.write("  Sequence {" + OpendapHelper.EOL); //see EOL definition for comments
        for (int v = 0; v < nColumns; v++) {
            PrimitiveArray pa = getColumn(v);
            writer.write("    " + OpendapHelper.getAtomicType(pa.elementClass()) +
                " " + getColumnName(v) + ";" + OpendapHelper.EOL); //see EOL definition for comments
        }
        writer.write("  } " + sequenceName + ";" + OpendapHelper.EOL); //see EOL definition for comments 
        writer.write("} " + sequenceName + ";" + OpendapHelper.EOL); //see EOL definition for comments 
        writer.flush(); //essential

        if (verbose)
            String2.log("  Table.saveAsDDS done. TIME=" + 
                (System.currentTimeMillis() - time));
    }


    /**
     * This writes the table's data structure (as if it were a DODS Sequence) 
     * to the outputStream as DODS ASCII data (which is not defined in DAP 2.0,
     * but which is very close to saveAsDODS below).
     * This mimics http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle.asc?lon,ship,cast,t0,NO3&lon<-125.7
     * 
     * <p>This sends missing values as is.
     * This doesn't call convertToFakeMissingValues. Do it beforehand if you need to.
     * 
     * @param outputStream the outputStream to receive the results.
     *    Afterwards, it is flushed, not closed.
     * @param sequenceName  e.g., "erd_opendap_globec_bottle"
     * @throws Exception  if trouble. 
     */
    public void saveAsDodsAscii(OutputStream outputStream, String sequenceName) throws Exception {

        if (verbose) String2.log("  Table.saveAsDodsAscii"); 
        long time = System.currentTimeMillis();
        
        //write the dds    //DAP 2.0, 7.2.3
        saveAsDDS(outputStream, sequenceName);  

        //write the connector  
        Writer writer = new OutputStreamWriter(outputStream); 
        writer.write("---------------------------------------------" + OpendapHelper.EOL); //see EOL definition for comments

        //write the column names
        int nColumns = nColumns();
        int nRows = nRows();
        boolean isStringCol[] = new boolean[nColumns];
        for (int col = 0; col < nColumns; col++) {
            isStringCol[col] = getColumn(col).elementClass() == String.class;
            writer.write(getColumnName(col) +
                (col == nColumns - 1? OpendapHelper.EOL : ", "));
        }

        //write the data  //DAP 2.0, 7.3.2.3
        //write elements of the sequence, in dds order
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nColumns; col++) {
                String s = getColumn(col).getString(row);
                if (isStringCol[col]) //see DODS Appendix A, quoted-string
                    s = "\"" + String2.replaceAll(s, "\"", "\\\"") + "\"";
                writer.write(s + (col == nColumns - 1? OpendapHelper.EOL : ", "));
            }
        }

        writer.flush(); //essential

        if (verbose)
            String2.log("  Table.saveAsDodsAscii done. TIME=" + 
                (System.currentTimeMillis() - time));
    }


    /**
     * This writes the table's data structure (as if it were a DODS Sequence) 
     * to the outputStream as an DODS DataDDS (see www.opendap.org, DAP 2.0, 7.2.3).
     *
     * <p>This sends missing values as is.
     * This doesn't call convertToFakeMissingValues. Do it beforehand if you need to.
     * 
     * @param outputStream the outputStream to receive the results.
     *    Afterwards, it is flushed, not closed.
     * @param sequenceName  e.g., "erd_opendap_globec_bottle"
     * @throws Exception  if trouble. 
     */
    public void saveAsDODS(OutputStream outputStream, String sequenceName) throws Exception {
        if (verbose) String2.log("  Table.saveAsDODS"); 
        long time = System.currentTimeMillis();

        //write the dds    //DAP 2.0, 7.2.3
        saveAsDDS(outputStream, sequenceName);  

        //write the connector  //DAP 2.0, 7.2.3
        //see EOL definition for comments
        outputStream.write((OpendapHelper.EOL + "Data:" + OpendapHelper.EOL).getBytes()); 

        //write the data  //DAP 2.0, 7.3.2.3
        //write elements of the sequence, in dds order
        int nColumns = nColumns();
        int nRows = nRows();
        DataOutputStream dos = new DataOutputStream(outputStream);
        for (int row = 0; row < nRows; row++) {
            dos.writeInt(0x5A << 24); //start of instance
            for (int col = 0; col < nColumns; col++) 
                getColumn(col).externalizeForDODS(dos, row);
        }
        dos.writeInt(0xA5 << 24); //end of sequence; so if nRows=0, this is all that is sent

        dos.flush(); //essential

        if (verbose)
            String2.log("  Table.saveAsDDS done. TIME=" + 
                (System.currentTimeMillis() - time));
    }


    /**
     * This is like the other saveAsHtml, but saves to a file.
     *
     * @param fullFileName the complete file name (including directory and
     *    extension, usually ".htm" or ".html").
     */
    public void saveAsHtml(String fullFileName, 
        String preTableHtml, String postTableHtml, 
        String otherClasses, String bgColor, int border, boolean writeUnits, int timeColumn, 
        boolean needEncodingAsXml, boolean allowWrap) throws Exception {

        if (verbose) String2.log("Table.saveAsHtml " + fullFileName); 
        long time = System.currentTimeMillis();

        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(
            fullFileName + randomInt));

        try {
            //saveAsHtml(outputStream, ...)
            saveAsHtml(bos, 
                File2.getNameNoExtension(fullFileName),
                preTableHtml, postTableHtml, 
                otherClasses, bgColor, border, writeUnits, timeColumn, 
                needEncodingAsXml, allowWrap);
            bos.close();

            //rename the file to the specified name, instantly replacing the original file
            File2.rename(fullFileName + randomInt, fullFileName);

        } catch (Exception e) {
            try {bos.close();
            } catch (Exception e2) {
            }
            File2.delete(fullFileName + randomInt);
            File2.delete(fullFileName);
            throw e;
        }


        //diagnostic
        if (verbose)
            String2.log("Table.saveAsHtml done. TIME=" + 
                (System.currentTimeMillis() - time));

    }
    
    /**
     * Write this data as a table to an outputStream.
     * See saveAsHtmlTable (which this calls) for some of the details.
     * 
     * @param outputStream There is no need for it to be already buffered.
     *    Afterwards, it is flushed, not closed.
     * @param fileNameNoExt is the fileName without dir or extension (only used for the document title).
     * @param preTableHtml is html text to be inserted at the start of the 
     *   body of the document, before the table tag
     *   (or "" if none).
     * @param postTableHtml is html text to be inserted at the end of the 
     *   body of the document, after the table tag
     *   (or "" if none).
     * @param otherClasses a space separated list of other (HTML style) classes (or null or "")
     * @param bgColor the backgroundColor, e.g., BGCOLOR, "#f1ecd8" or null (for none defined)
     * @param border the line width of the cell border lines (e.g., 0, 1, 2)
     * @param writeUnits if true, the table's second row will be units (from columnAttributes "units") 
     * @param timeColumn the column with epoch seconds which should be written
     *    as ISO formatted date times; if <0, this is ignored.
     * @param needEncodingAsXml if true, the cell contents will be encodedAsXml (i.e., they contain plain text);
     *    otherwise, they are written as is (i.e., they already contain html-encoded text).
     * @param allowWrap if true, data may be broken into different lines
     *    so the table is only as wide as the screen.
     * @throws Exception  if trouble. But if no data, it makes a simple html file.
     */
    public void saveAsHtml(OutputStream outputStream, String fileNameNoExt, 
        String preTableHtml, String postTableHtml, 
        String otherClasses, String bgColor, int border, boolean writeUnits, 
        int timeColumn, boolean needEncodingAsXml, boolean allowWrap) throws Exception {

        if (verbose) String2.log("Table.saveAsHtml"); 
        long time = System.currentTimeMillis();

        //write the header
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        writer.write(
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" +
            "  \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
            "<html>\n" +
            "<head>\n" +
            "  <title>" + fileNameNoExt + "</title>\n" +
            Table.ERD_TABLE_CSS +
            "</head>\n" +
            "<body bgcolor=\"white\" text=\"black\"\n" +
            "  style=\"font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n");

        writer.write(preTableHtml);
        //write the actual table
        saveAsHtmlTable(writer, otherClasses, bgColor, border, writeUnits, timeColumn, 
            needEncodingAsXml, allowWrap);

        //close the document
        writer.write(postTableHtml);
        writer.write(
            "</body>\n" +
            "</html>\n");

        writer.flush(); //essential

        //diagnostic
        if (verbose)
            String2.log("Table.saveAsHtml done. TIME=" + 
                (System.currentTimeMillis() - time));

    }


    /**
     * Save this data as an html table (not a complete html document.
     * <br>This writes the lon values as they are currently in this table
     *    (e.g., +-180 or 0..360). 
     * <br>If no exception is thrown, the table was successfully written.
     * <br>Currently, all values are written as double.
     * <br>NaN's are written as "NaN".
     * <br>The table will be assigned class="erd".
     * <br>This is an HTML table, not quite a valid XHTML table.
     * 
     * @param writer usually already buffered
     * @param otherClasses a space separated list of other (HTML style) classes (or null or "")
     * @param bgColor the backgroundColor, e.g., BGCOLOR, "#f1ecd8" or null (for none defined)
     * @param border the line width of the cell border lines (e.g., 0, 1, 2)
     * @param writeUnits if true, the table's second row will be units (from columnAttributes "units") 
     * @param timeColumn the column with epoch seconds which should be written
     *    as ISO formatted date times; if <0, this is ignored.
     * @param needEncodingAsXml if true, the cell contents will be encodedAsXml (i.e., they contain plain text);
     *    otherwise, they are written as is (i.e., they already contain html-encoded text).
     * @param allowWrap if true, data may be broken into different lines
     *    so the table is only as wide as the screen.
     * @throws Exception  if trouble. But if no data, it makes a simple html file.
     */
    public void saveAsHtmlTable(Writer writer, String otherClasses, String bgColor, int border, 
        boolean writeUnits, int timeColumn, boolean needEncodingAsXml,
        boolean allowWrap) throws Exception {

        if (verbose) String2.log("  Table.saveAsHtmlTable"); 
        long time = System.currentTimeMillis();

        //no data?
        if (nRows() == 0) {
            writer.write(MustBe.THERE_IS_NO_DATA);
        } else {

            writer.write(
                "<table class=\"erd" +
                (otherClasses == null || otherClasses.length() == 0? "" : " " + otherClasses) + "\" " +
                (bgColor == null? "" : "bgcolor=\"" + bgColor + "\" ") +
                "cellspacing=\"0\">\n"); //css border-spacing is not supported in all browsers

            //write the column names   
            writer.write("<tr>\n");
            int nColumns = nColumns();
            if (columnNames != null && columnNames.size() == nColumns) {
                for (int col = 0; col < nColumns; col++) {
                    String s = getColumnName(col);
                    if (needEncodingAsXml) 
                        s = XML.encodeAsHTML(s);
                    writer.write("<th>" + s + 
                        //"</th>" + //HTML doesn't require it, so save bandwidth
                        "\n");
                }
            }
            writer.write("</tr>\n");

            //write the units   
            if (writeUnits) {
                writer.write("<tr>\n");
                if (columnNames != null && columnNames.size() == nColumns) {
                    for (int col = 0; col < nColumns; col++) {
                        String tUnits = columnAttributes(col).getString("units");
                        if (col == timeColumn)
                            tUnits = "UTC"; //no longer true: "seconds since 1970-01-01..."
                        if (tUnits == null)
                            tUnits = "";
                        if (needEncodingAsXml) 
                            tUnits = XML.encodeAsHTML(tUnits);
                        writer.write("<th>" + 
                            (tUnits.length() == 0? "&nbsp;" : tUnits) + 
                            //"</th>" + //HTML doesn't require it, so save bandwidth
                            "\n");
                    }
                }
                writer.write("</tr>\n");
            }

            //write the data
            int nRows = nRows();
            for (int row = 0; row < nRows; row++) {
                writer.write("<tr>\n"); 
                for (int col = 0; col < nColumns; col++) {
                    writer.write(allowWrap? "<td>" : "<td nowrap>"); //no 6 spaces to left: make document smaller
                    if (col == timeColumn) {
                        double d = getDoubleData(col, row);
                        writer.write(Calendar2.safeEpochSecondsToIsoStringT(d, "&nbsp;"));
                    } else {
                        String s = getStringData(col, row);
                        if (needEncodingAsXml) 
                            s = XML.encodeAsHTML(s);
                        writer.write(s.length() == 0? "&nbsp;" : s); 
                    }
                    writer.write(
                        //"</td>" + //HTML doesn't require it, so save bandwidth
                        "\n");
                }
                writer.write("</tr>\n");
            }

            //close the table
            writer.write(
                "</table>\n");
        }

        //diagnostic
        if (verbose)
            String2.log("    Table.saveAsHtmlTable done. TIME=" + 
                (System.currentTimeMillis() - time));
    }

    /** This actually reads the file, then reads the HTML table in the file. */
    public void readHtml(String fullFileName, int skipNTables, 
        boolean secondRowHasUnits, boolean simplify) throws Exception {
        
        String sar[] = String2.readFromFile(fullFileName, null, 2);
        Test.ensureEqual(sar[0].length(), 0, sar[0]); //check that there was no error
        //String2.log(String2.annotatedString(sar[1]));
        readHtml(fullFileName, sar[1], skipNTables, secondRowHasUnits, simplify); 
    }

    /**
     * This reads a standard HTML table of data from an HTML file.
     * <ul>
     * <li>Currently, this isn't very precise about HTML syntax.
     * <li>Currently, this doesn't deal with table-like tags in CDATA. They remain as is.
     * <li>And it doesn't deal with colspan or rowspan.
     * <li>Column Names are taken from first row.
     * <li>If secondRowHasUnits, data should start on 3rd row (else 2nd row).
     * <li>It is okay if a row has fewer columns or more columns than expected.
     *    If table.debug = true, a message will be logged about this.
     * <li>nRows=0 and nColumns=0 is not an error.
     * <li>XML.decodeEntities is applied to data in String columns,
     *   so there may be HTML tags if the data is HTML.
     *   Common entities (&amp;amp; &amp;lt; &amp;gt; &amp;quot;) are converted
     *   to the original characters.
     *   &amp;nbsp; is converted to a regular space.   
     * </ul>
     *
     * @param fullFileName just for diagnostics
     * @param html the html text
     * @param skipNTables the number of start &lt;table@gt; tags to be skipped.
     *    The tables may be separate or nested.
     * @param secondRowHasUnits  (ERDDAP tables do; others usually don't)
     * @param simplify if the columns should be simplified
     * @throws Exception
     */
    public void readHtml(String fullFileName, String html, int skipNTables, 
        boolean secondRowHasUnits, boolean simplify) 
        throws Exception {

        if (verbose)
            String2.log("Table.readHtml " + fullFileName);
        long time = System.currentTimeMillis();
        String startError = "Error while reading HTML table from " + fullFileName + 
            "\nInvalid HTML: ";

        clear();

        //skipNTables
        int po = 0; //next po to look at
        for (int skip = 0; skip < skipNTables; skip++) {
            po = String2.indexOfIgnoreCase(html, "<table", po);
            if (po < 0) 
                throw new RuntimeException(startError + "unable to skip " + skipNTables + " <table>'s.");
            po += 6;
        }

        //find main table
        po = String2.indexOfIgnoreCase(html, "<table", po);
        if (po < 0) 
            throw new RuntimeException(startError + "missing main <table>.");
        po += 6;
        int endTable = String2.indexOfIgnoreCase(html, "</table", po);
        if (endTable < 0) 
            throw new RuntimeException(startError + "missing </table>.");

        //read a row
        int nRows = 0;
        int nCols = 0;
        while (po < endTable) {
            int potr = String2.indexOfIgnoreCase(html, "<tr", po);
            if (potr < 0 || potr > endTable) 
                break;
            //</tr> isn't required, so look for next <tr
            int endRow = String2.indexOfIgnoreCase(html, "<tr", potr + 3);
            if (endRow < 0)
                endRow = endTable;
            String row = html.substring(potr, endRow);            

            //process the row
            int poInRow = 2;
            int tCol = 0;
            while (true) {
                int poth = String2.indexOfIgnoreCase(row, "<th", poInRow);
                int potd = String2.indexOfIgnoreCase(row, "<td", poInRow);
                //ensure at least one was found
                if (poth < 0 && potd < 0) 
                    break;
                if (poth < 0) poth = row.length();
                if (potd < 0) potd = row.length();
                int gtPo = row.indexOf('>', Math.min(poth, potd) + 3);
                if (gtPo < 0) 
                    throw new RuntimeException(startError + "missing '>' after final " +
                        (poth < potd? "<th" : "<td") + 
                        " on table row#" + nRows + 
                        " on or before line #" + (1 + String2.countAll(html.substring(0, endRow), "\n")) + 
                        ".");
                //</th> and </td> aren't required, so look for next tag's <
                int endTagPo = row.indexOf('<', gtPo + 1);
                if (endTagPo < 0)
                    endTagPo = row.length();
                String datum = XML.decodeEntities(  
                    row.substring(gtPo + 1, endTagPo).trim());

                if (nRows == 0) {
                    //if first row, add a column
                    addColumn(datum, new StringArray());
                    nCols++;
                } else if (tCol < nCols) {
                    if (secondRowHasUnits && nRows == 1) {
                        //store the units
                        columnAttributes(tCol).add("units", datum);
                    } else {
                        //append the data
                        ((StringArray)getColumn(tCol)).addNotCanonical(datum);
                    }
                } else {
                    //ignore this and subsequent columns on this row
                    if (debug) String2.log("!!!Extra columns were found on row=" + nRows);
                    break;
                }

                poInRow = endTagPo; //"endTag" might be next <th or <td
                tCol++;
            }

            //add blanks so all columns are same length
            if (tCol < nCols) {
                if (debug) String2.log("!!!Too few columns were found on row=" + nRows);
                makeColumnsSameSize();
            }
            nRows++;
            po = endRow;

        }

        //simplify the columns
        if (simplify) 
            simplify();

        //canonicalize the string columns
        for (int col = 0; col < nCols; col++) {
            PrimitiveArray pa = getColumn(col);
            if (pa instanceof StringArray)
                ((StringArray)pa).makeCanonical();
        }

        //diagnostic
        if (verbose)
            String2.log("    Table.readHtml done. nRows=" + nRows + 
                " nCols=" + nCols + " TIME=" + 
                (System.currentTimeMillis() - time));
    }


    /**
     * This reads all rows of all of the specified columns in a flat .nc file
     * or an http:<ncFile> (several 1D variables (columns), all referencing the
     * same dimension).
     * This also reads global and variable attributes.
     * This converts fakeMissingValues to standard PrimitiveArray mv's.
     *
     * <p>If the fullName is an http address, the name needs to start with "http:\\" 
     * (upper or lower case) and the server needs to support "byte ranges"
     * (see ucar.nc2.NetcdfFile documentation).
     * 
     * @param fullName This may be a local file name, an "http:" address of a
     *    .nc file, or an opendap url.
     * @param loadColumns if null, this searches for the (pseudo)structure variables
     * @param unpack If 0, no variables will be unpacked and the
     *    tests will be done with unpacked values. If 1 or 2, 
     *    the testColumns will be unpacked before
     *    testing and the resulting loadColumns will be unpacked.
     *    Variables are only unpacked only if a variable has an attribute 
     *    scale_factor!=1 or add_offset!=0.
     *    If unpack == 1, variables are unpacked to floats or doubles,
     *    depending on the data type of scale_factor and add_offset.
     *    If unpack == 2, variables are always unpacked to DoubleArrays.
     * @throws Exception if trouble
     */
    public void readFlatNc(String fullName, String loadColumns[], int unpack) throws Exception {
        lowReadFlatNc(fullName, loadColumns, unpack, -1);
    }

    /**
     * This is like readFlatNc, but just reads the first row of data and all the metadata
     * (useful to get the file info).
     */
    public void readFlatNcInfo(String fullName, String loadColumns[], int unpack) throws Exception {
        lowReadFlatNc(fullName, loadColumns, unpack, 0);
    }

    /**
     * The low level workhorse for readFlatNc and readFlatNcInfo.
     *
     * @param lastRow the last row to be read (inclusive).  
     *    If lastRow = -1, the entire var is read.
     */
    public void lowReadFlatNc(String fullName, String loadColumns[], int unpack, 
        int lastRow) throws Exception {

        //get information
        if (verbose) String2.log("Table.readFlatNc " + fullName); 
        long time = System.currentTimeMillis();
        NetcdfFile netcdfFile = NcHelper.openFile(fullName);
        try {
            Variable loadVariables[] = NcHelper.findVariables(netcdfFile, loadColumns);

            //fill the table
            clear();
            appendNcRows(loadVariables, 0, lastRow);
            NcHelper.getGlobalAttributes(netcdfFile, globalAttributes());
            for (int col = 0; col < loadVariables.length; col++)
                NcHelper.getVariableAttributes(loadVariables[col], columnAttributes(col));

            //I care about this exception
            netcdfFile.close();

        } catch (Exception e) {
            //make sure it is explicitly closed
            try {
                netcdfFile.close(); 
            } catch (Exception e2) {
                //don't care
            }
            throw e;
        }

        //unpack 
        int nCol = nColumns();
        if (unpack == 1 || unpack == 2) {
            for (int col = 0; col < nCol; col++)
                tryToUnpack(col, unpack);
        }

        //convert to standard MissingValues
        convertToStandardMissingValues();
        if (verbose) String2.log("  Table.readFlatNc done. nColumns=" + nColumns() +
            " nRows=" + nRows() + " TIME=" + (System.currentTimeMillis() - time));
    }

    /**
     * This reads the 1D variables from a .nc file *and* the scalar (0D) values 
     * (duplicated to have the same number of rows).
     *
     * <p>This always converts to standard missing values (MAX_INT, ..., NaN).
     *
     * @param fullName This may be a local file name, an "http:" address of a
     *    .nc file, or an opendap url.
     * @param loadColumns The 1D variables to be loaded. 
     *    If null, this searches for the (pseudo)structure variables.
     *    (The scalar variables are always loaded.)
     * @param unpack If 0, no variables will be unpacked and the
     *    tests will be done with unpacked values. If 1 or 2, 
     *    the testColumns will be unpacked before
     *    testing and the resulting loadColumns will be unpacked.
     *    Variables are only unpacked only if a variable has an attribute 
     *    scale_factor!=1 or add_offset!=0.
     *    If unpack == 1, variables are unpacked to floats or doubles,
     *    depending on the data type of scale_factor and add_offset.
     *    If unpack == 2, variables are always unpacked to DoubleArrays.
     * @param lastRow the last row to be read (inclusive).  
     *    If lastRow = -1, the entire var is read.
     * @throws Exception if trouble
     */
    public void readFlat0Nc(String fullName, String loadColumns[], int unpack, 
        int lastRow) throws Exception {

        //read the 1D variables
        if (verbose) String2.log("Table.readFlat0Nc " + fullName); 
        long time = System.currentTimeMillis();
        lowReadFlatNc(fullName, loadColumns, unpack, lastRow);
        int tnRows = nRows();

        //read the scalar variables
        NetcdfFile netcdfFile = NcHelper.openFile(fullName);
        int insertAt = 0;
        try {
            Group rootGroup = netcdfFile.getRootGroup();
            List rootGroupVariables = rootGroup.getVariables(); 
            int nv = rootGroupVariables.size();
            for (int v = 0; v < nv; v++) {
                Variable var = (Variable)rootGroupVariables.get(v);
                boolean isChar = var.getDataType() == DataType.CHAR;
                if (var.getRank() + (isChar? -1 : 0) == 0) {
                    PrimitiveArray pa = NcHelper.getPrimitiveArray(var.read());                    
                    //technically, shouldn't trim. 
                    //But more likely problem is source meant to trim but didn't.
                    if (pa instanceof StringArray) 
                        pa.setString(0, pa.getString(0).trim());
                    if (tnRows > 1) {
                        if (pa instanceof StringArray) 
                             pa.addNStrings(tnRows-1, pa.getString(0));
                        else pa.addNDoubles(tnRows-1, pa.getDouble(0));
                    }

                    Attributes atts = new Attributes();
                    NcHelper.getVariableAttributes(var, atts);
                    addColumn(insertAt++, var.getShortName(), pa, atts);
                }
            }

            //I care about this exception
            netcdfFile.close();

        } catch (Exception e) {
            //make sure it is explicitly closed
            try {
                netcdfFile.close(); 
            } catch (Exception e2) {
                //don't care
            }
            throw e;
        }

        //unpack 
        int nCol = nColumns();
        if (unpack == 1 || unpack == 2) {
            for (int col = 0; col < nCol; col++)
                tryToUnpack(col, unpack);
        }

        //convert to standard MissingValues
        convertToStandardMissingValues();
        if (verbose) String2.log("  Table.readFlat0Nc done. nColumns=" + nColumns() +
            " nRows=" + nRows() + " TIME=" + (System.currentTimeMillis() - time));
    }
    
    /**
     * This reads all specified 4D numeric (or 5D char) variables into a flat table with
     * columns x,y,z,t (with type matching the type in the file) + loadColumns.
     * This works with true 4D numeric (or 5D char) variables (not just length=1 x,y,z axes).
     * It does not check for metadata standards compliance or ensure
     *    that axes correspond to lon, lat, depth, time.
     * This also reads global and variable attributes.
     *
     * <p>If the fullName is an http address, the name needs to start with "http:\\" 
     * (upper or lower case) and the server needs to support "byte ranges"
     * (see ucar.nc2.NetcdfFile documentation).
     * 
     * <p>This supports an optional stringVariable which is read
     * from the file as a 1D char array, but used to fill a String column.   
     * Dapper/DChart prefers this to a 4D array for the ID info.
     * 
     * @param fullName This may be a local file name, an "http:" address of a
     *    .nc file, or an opendap url.
     * @param loadColumns if null, this searches for a 4D numeric (or 5D char) variable
     *    and loads all variables using the same 4 axes.
     *    If no variables are found, the table will have 0 rows and columns.
     *    All dimension columns are automatically loaded.
     *    Dimension names in loadColumns are ignored (since they will be loaded automatically).
     * @param unpack If 0, no variables will be unpacked and the
     *    tests will be done with unpacked values. If 1 or 2, 
     *    the testColumns will be unpacked before
     *    testing and the resulting loadColumns will be unpacked.
     *    Variables are only unpacked only if a variable has an attribute 
     *    scale_factor!=1 or add_offset!=0.
     *    If unpack == 1, variables are unpacked to floats or doubles,
     *    depending on the data type of scale_factor and add_offset.
     *    If unpack == 2, variables are always unpacked to DoubleArrays.
     * @param stringVariableName the name of the stringVariable (or null if not used)
     * @param stringVariableColumn the columnNumber for the stringVariable (or -1 for the end)
     * @throws Exception if trouble
     */
    public void read4DNc(String fullName, String loadColumns[], int unpack, 
        String stringVariableName, int stringVariableColumn) throws Exception {

        if (verbose) String2.log("Table.read4DNc " + fullName); 
        long time = System.currentTimeMillis();
        String errorInMethod = String2.ERROR + " in Table.read4DNc " + fullName + ":\n";
        //get information
        NetcdfFile ncFile = NcHelper.openFile(fullName);
        try {
            Variable loadVariables[] = NcHelper.find4DVariables(ncFile, loadColumns);

            //clear the table
            clear();

            //load the variables
            Dimension dimensions[] = new Dimension[4];  //all the 4D arrays are 0=t,1=z,2=y,3=x
            String dimensionNames[] = new String[4];
            Variable axisVariables[] = new Variable[4];
            PrimitiveArray axisPA[] = new PrimitiveArray[4];
            int xLength, yLength, zLength, tLength;
            boolean needToSetUpAxes = true;
            for (int v = 0; v < loadVariables.length; v++) {
                Variable variable = loadVariables[v];
                if (variable.getRank() < 2)
                    continue;

                //if first variable, set up axis columns
                if (needToSetUpAxes) {
                    //get axes
                    needToSetUpAxes = false;
                    List dimList = variable.getDimensions();
                    if (variable.getDataType() != DataType.CHAR && dimList.size() != 4)
                        throw new SimpleException(errorInMethod + 
                            "nDimensions not 4 for numeric variable: " + variable.getName());

                    if (variable.getDataType() == DataType.CHAR && dimList.size() != 5)
                        throw new SimpleException(errorInMethod + 
                            "nDimensions not 5 for char variable: " + variable.getName());
                        
                    for (int i = 0; i < 4; i++) {
                        dimensions[i] = (Dimension)dimList.get(i);
                        dimensionNames[i] = dimensions[i].getName();
                        axisVariables[i] = ncFile.findVariable(dimensionNames[i]);
                        if (axisVariables[i] == null)
                            throw new SimpleException( 
                                errorInMethod + "dimension variable not found: " + dimensionNames[i]);
                        axisPA[i] = NcHelper.getPrimitiveArray(axisVariables[i]); 
                    }    

                    //make axes columns (x,y,z,t) to hold flattened axis values
                    xLength = axisPA[3].size();
                    yLength = axisPA[2].size();
                    zLength = axisPA[1].size();
                    tLength = axisPA[0].size();
                    int totalNValues = tLength * zLength * yLength * xLength;
                    PrimitiveArray xColumn = PrimitiveArray.factory(NcHelper.getElementClass(axisVariables[3].getDataType()), totalNValues, false);
                    PrimitiveArray yColumn = PrimitiveArray.factory(NcHelper.getElementClass(axisVariables[2].getDataType()), totalNValues, false);
                    PrimitiveArray zColumn = PrimitiveArray.factory(NcHelper.getElementClass(axisVariables[1].getDataType()), totalNValues, false);
                    PrimitiveArray tColumn = PrimitiveArray.factory(NcHelper.getElementClass(axisVariables[0].getDataType()), totalNValues, false);
                    addColumn(dimensionNames[3], xColumn); 
                    addColumn(dimensionNames[2], yColumn); 
                    addColumn(dimensionNames[1], zColumn); 
                    addColumn(dimensionNames[0], tColumn); 

                    //get the axis attributes
                    for (int col = 0; col < 4; col++) 
                        NcHelper.getVariableAttributes(axisVariables[3 - col], columnAttributes(col));

                    //populate the columns
                    for (int t = 0; t < tLength; t++) {
                        double tValue = axisPA[0].getDouble(t);
                        for (int z = 0; z < zLength; z++) {
                            double zValue = axisPA[1].getDouble(z);
                            for (int y = 0; y < yLength; y++) {
                                double yValue = axisPA[2].getDouble(y);
                                for (int x = 0; x < xLength; x++) {
                                    xColumn.addDouble(axisPA[3].getDouble(x));
                                    yColumn.addDouble(yValue);
                                    zColumn.addDouble(zValue);
                                    tColumn.addDouble(tValue);                                    
                                }
                            }
                        }
                    }
                }

                //get data
                Array array = variable.read(); 
                PrimitiveArray pa = PrimitiveArray.factory(NcHelper.getArray(array)); 
                //String2.log("Table.read4DNc v=" + v + ": " + pa);

                //store data
                addColumn(variable.getName(), pa);
                NcHelper.getVariableAttributes(variable, columnAttributes(nColumns() - 1));
            }

            //load the stringVariable
            if (stringVariableName != null) {
                Variable variable = ncFile.findVariable(stringVariableName);
                Array array = variable.read(); 
                PrimitiveArray pa = PrimitiveArray.factory(NcHelper.getArray(array)); 
                if (verbose) String2.log("  stringVariableName values=" + pa);

                //store data
                if (stringVariableColumn < 0 || stringVariableColumn > nColumns())
                    stringVariableColumn = nColumns();
                String sar[] = new String[nRows()];
                Arrays.fill(sar, pa.getString(0));
                addColumn(stringVariableColumn, stringVariableName, new StringArray(sar));
                NcHelper.getVariableAttributes(variable, columnAttributes(stringVariableColumn));
            }

            //load the global metadata
            NcHelper.getGlobalAttributes(ncFile, globalAttributes());

            //I do care if this throws exception
            ncFile.close(); 

        } catch (Exception e) {
            //make sure ncFile is explicitly closed
            try {
                ncFile.close(); 
            } catch (Exception e2) {
                //don't care
            }

            throw e;
        }

        //unpack 
        int nCol = nColumns();
        if (unpack == 1 || unpack == 2) {
            for (int col = 0; col < nCol; col++)
                tryToUnpack(col, unpack);
        }

        //convert to standard MissingValues
        convertToStandardMissingValues();
        if (verbose) String2.log("  Table.read4DNc done. nColumns=" + nColumns() +
            " nRows=" + nRows() + " TIME=" + (System.currentTimeMillis() - time));
    }

    /**
     * This reads and flattens all specified nDimensional (1 or more) variables 
     * (which must have shared dimensions) into a table.
     * <br>This does not unpack the values or convert to standardMissingValues.
     * <br>Axis vars can't be String variables.
     * <br>Axis vars can be just dimensions (not actual variables).
     *
     * <p>If the fullName is an http address, the name needs to start with "http:\\" 
     * (upper or lower case) and the server needs to support "byte ranges"
     * (see ucar.nc2.NetcdfFile documentation).
     * 
     * @param fullName This may be a local file name, an "http:" address of a
     *    .nc file, or an opendap url.
     * @param loadVariableNames if null or length 0, all vars are read.
     *    <br>If a specified var isn't in the file, there won't be a column in the results file for it.
     *    <br>All dimension vars are automatically loaded.
     *    <br>0D and 1D vars in loadVariables are ignored (since they will be loaded automatically).
     * @param constraintAxisVarName  or null if none.
     *    <br>This works if constraintAxisVar is sorted ascending (it is tested here); 
     *      otherwise the constraint is ignored.
     *    <br>This uses precision of 5 digits, so the constraint is very crude (but valid) 
     *      for time (e.g., seconds since 1970-01-01).
     * @param constraintMin  if variable is packed, this is packed. 
     * @param constraintMax  if variable is packed, this is packed.
     * @param getMetadata  if true, this gets global and variable metadata.
     * @throws Exception if trouble
     */
    public void readNDNc(String fullName, String loadVariableNames[], 
        String constraintAxisVarName, double constraintMin, double constraintMax, 
        boolean getMetadata) throws Exception {

        if (loadVariableNames == null)
            loadVariableNames = new String[0];
        if (verbose) String2.log("Table.readNDNc " + fullName);
        if (reallyVerbose) String2.log(
            "  loadVars:" + String2.toCSSVString(loadVariableNames) +
            (constraintAxisVarName == null? "" :
                "\n  constrain:" + constraintAxisVarName + " >=" + constraintMin + " <=" + constraintMax)); 
        long time = System.currentTimeMillis();
        String errorInMethod = String2.ERROR + " in Table.readNDNc " + fullName + ":\n";
        //get information
        NetcdfFile ncFile = NcHelper.openFile(fullName);
        try {
            //clear the table
            clear();

            //load the variables
            Variable loadVariables[] = null;
            if (loadVariableNames.length == 0) {
                loadVariables = NcHelper.findMaxDVariables(ncFile);
            } else {
                ArrayList list = new ArrayList();
                for (int i = 0; i < loadVariableNames.length; i++) {
                    Variable variable = ncFile.findVariable(loadVariableNames[i]);
                    if (variable == null) {
                        if (verbose) String2.log("  var=" + loadVariableNames[i] + " not found");
                    } else {
                        list.add(variable);
                    }
                }
                loadVariables = NcHelper.variableListToArray(list);
            }
            Test.ensureTrue(loadVariables != null && loadVariables.length > 0, 
                errorInMethod + "The file has no variables with dimensions.");

            //go through the variables
            int nAxes = -1;  //not set up yet
            int readOrigin[] = null;
            int axisLengths[] = null;
            for (int v = 0; v < loadVariables.length; v++) {
                Variable variable = loadVariables[v];
                boolean isChar = variable.getDataType() == DataType.CHAR;
                if (debug) 
                    String2.log("var#" + v + "=" + variable.getName());

                //is it a 0D variable?    
                if (variable.getRank() + (isChar? -1 : 0) == 0) { 
                    if (debug) String2.log("  skipping 0D var");
                    continue;
                }

                //is it an axis variable?    
                if (!isChar && variable.getRank() == 1 &&
                    variable.getDimension(0).getName().equals(variable.getName())) { //varName = dimName
                    if (debug) String2.log("  skipping axisVariable");
                    continue;
                }

                //if first non-axis variable, set up axis columns
                if (nAxes < 0) {

                    //set up dim variables                    
                    nAxes = variable.getRank() - (isChar? 1 : 0);
                    PrimitiveArray axisPAs[] = new PrimitiveArray[nAxes];
                    PrimitiveArray columnPAs[] = new PrimitiveArray[nAxes];
                    axisLengths = new int[nAxes];

                    List axisList = variable.getDimensions();                        
                    for (int a = 0; a < nAxes; a++) {
                        Dimension dimension = (Dimension)axisList.get(a);
                        String axisName = dimension.getName();
                        axisLengths[a] = dimension.getLength();
                        if (debug) String2.log("  found axisName=" + axisName + " size=" + axisLengths[a]);
                        Attributes atts = new Attributes();
                        Variable axisVariable = ncFile.findVariable(axisName);
                        if (axisVariable == null) {
                            //that's ok; set up dummy 0,1,2,3...
                            axisPAs[a] = 
                                axisLengths[a] < Byte.MAX_VALUE - 1?  new ByteArray( 0, axisLengths[a]-1) :
                                axisLengths[a] < Short.MAX_VALUE - 1? new ShortArray(0, axisLengths[a]-1) :
                                                                      new IntArray(  0, axisLengths[a]-1);
                        } else {
                            axisPAs[a] = NcHelper.getPrimitiveArray(axisVariable); 
                            if (getMetadata)
                                NcHelper.getVariableAttributes(axisVariable, atts);
                        }
                        columnPAs[a] = PrimitiveArray.factory(axisPAs[a].elementClass(), 1, false);
                        addColumn(a, axisName, columnPAs[a], atts);
                    }    
                    readOrigin = new int[nAxes]; //all 0's
                    //readShape = axisLengths

                    //deal with constraintAxisVarName
                    int constraintCol = constraintAxisVarName == null? -1 : findColumnNumber(constraintAxisVarName);
                    int constraintFirst = -1;
                    int constraintLast = -1; 
                    if (constraintCol >= 0 && 
                        !Double.isNaN(constraintMin) && !Double.isNaN(constraintMax)) {
                        PrimitiveArray cpa = axisPAs[constraintCol];
                        String asc = cpa.isAscending();
                        if (asc.length() == 0) {
                            constraintFirst = cpa.binaryFindFirstGAE5(0, cpa.size() - 1, constraintMin);
                            if (constraintFirst >= cpa.size())
                                constraintFirst = -1;
                            else constraintLast  = cpa.binaryFindLastLAE5(constraintFirst, 
                                cpa.size() - 1, constraintMax);
                            if (debug) String2.log("  constraintAxisVar=" + constraintAxisVarName + 
                                " is ascending.  first=" + constraintFirst + 
                                " last(inclusive)=" + constraintLast);
                            if (constraintFirst >= 0 && constraintLast >= constraintFirst) {
                                //ok, use it
                                readOrigin[ constraintCol] = constraintFirst;
                                axisLengths[constraintCol] = constraintLast - constraintFirst + 1;
                                cpa.removeRange(constraintLast + 1, cpa.size());  
                                cpa.removeRange(0, constraintFirst);
                            }
                        } else {
                            if (debug) String2.log("  constraintAxisVar=" + constraintAxisVarName + 
                                " isn't ascending: " + asc);
                        }
                    }

                    //populate the axes columns
                    NDimensionalIndex ndi = new NDimensionalIndex(axisLengths);
                    Math2.ensureArraySizeOkay(ndi.size(), "Table.readNDNc");
                    int nRows = (int)ndi.size(); //safe since checked above
                    int current[] = ndi.getCurrent();
                    for (int a = 0; a < nAxes; a++) 
                        columnPAs[a].ensureCapacity(nRows);
                    while (ndi.increment()) {
                        for (int a = 0; a < nAxes; a++) {
                            //String2.log("  a=" + a + " current[a]=" + current[a] + 
                            //    " axisPAs[a].size=" + axisPAs[a].size());
                            //getDouble not getString since axisVars aren't strings, double is faster
                            columnPAs[a].addDouble(axisPAs[a].getDouble(current[a])); 
                        }
                    }
                }

                //ensure axes are as expected
                if (isChar) {
                    Test.ensureEqual(variable.getRank(), nAxes + 1,
                        errorInMethod + "Unexpected nDimensions for String variable: " + 
                        variable.getName());
                } else {
                    Test.ensureEqual(variable.getRank(), nAxes,
                        errorInMethod + "Unexpected nDimensions for numeric variable: " + 
                        variable.getName());
                }
                for (int a = 0; a < nAxes; a++) 
                    Test.ensureEqual(variable.getDimension(a).getName(), getColumnName(a),
                        errorInMethod + "Unexpected axis#" + a + 
                        " for variable=" + variable.getName());

                //get the data
                int tReadOrigin[] = readOrigin;
                int tReadShape[] = axisLengths;
                if (isChar && variable.getRank() == nAxes + 1) {
                    tReadOrigin = new int[nAxes + 1]; //all 0's
                    tReadShape  = new int[nAxes + 1];
                    System.arraycopy(readOrigin,  0, tReadOrigin, 0, nAxes);
                    System.arraycopy(axisLengths, 0, tReadShape, 0, nAxes);
                    tReadOrigin[nAxes] = 0;
                    tReadShape[nAxes] = variable.getDimension(nAxes).getLength();
                }
                Array array = variable.read(tReadOrigin, tReadShape); 
                PrimitiveArray pa = PrimitiveArray.factory(NcHelper.getArray(array)); 
                Test.ensureEqual(pa.size(), nRows(),
                    errorInMethod + "Unexpected nRows for " + variable.getName() + ".");

                //store data
                addColumn(variable.getName(), pa);
                if (getMetadata)
                    NcHelper.getVariableAttributes(variable, columnAttributes(nColumns() - 1));
            }

            //if the request is only for axis variables, set up axis columns
            //Note that request must have been for specific vars, since all vars would have found non-axis vars.
            if (nAxes < 0) { //no non-axis vars were requested; all vars are axis vars
                //This isn't quite right.  There may be other axes which weren't requested.
                //But in that case, this will at least return all distinct combinations 
                //  of the requested axis vars.

                //ensure names are available dimensions 
                //  !!they could be nDimensional vars that aren't in this file
                ArrayList dimensions = new ArrayList();
                for (int v = 0; v < loadVariableNames.length; v++) {
                    String axisName = loadVariableNames[v];
                    Dimension dimension = ncFile.findDimension(axisName);
                    if (dimension != null) 
                        dimensions.add(dimension);
                }

                if (dimensions.size() > 0) {

                    //set up dim variables                    
                    nAxes = dimensions.size();
                    PrimitiveArray axisPAs[] = new PrimitiveArray[nAxes];
                    PrimitiveArray columnPAs[] = new PrimitiveArray[nAxes];
                    axisLengths = new int[nAxes];

                    for (int a = 0; a < nAxes; a++) {
                        Dimension dimension = (Dimension)dimensions.get(a);
                        String axisName = dimension.getName();
                        Attributes atts = new Attributes();
                        axisLengths[a] = dimension.getLength();
                        if (debug) String2.log("  found axisName=" + axisName + " size=" + axisLengths[a]);
                        Variable axisVariable = ncFile.findVariable(axisName);
                        if (axisVariable == null) {
                            //that's ok; set up dummy 0,1,2,3...
                            axisPAs[a] = 
                                axisLengths[a] < Byte.MAX_VALUE - 1?  new ByteArray( 0, axisLengths[a]-1) :
                                axisLengths[a] < Short.MAX_VALUE - 1? new ShortArray(0, axisLengths[a]-1) :
                                                                      new IntArray(  0, axisLengths[a]-1);
                        } else {
                            axisPAs[a] = NcHelper.getPrimitiveArray(axisVariable); 
                            if (getMetadata)
                                NcHelper.getVariableAttributes(axisVariable, atts);
                        }
                        columnPAs[a] = PrimitiveArray.factory(axisPAs[a].elementClass(), 1, false);
                        addColumn(a, axisName, columnPAs[a], atts);
                    }    
                    readOrigin = new int[nAxes]; //all 0's
                    //readShape = axisLengths

                    //deal with constraintAxisVarName
                    int constraintCol = constraintAxisVarName == null? -1 : findColumnNumber(constraintAxisVarName);
                    int constraintFirst = -1;
                    int constraintLast = -1; 
                    if (constraintCol >= 0 && 
                        !Double.isNaN(constraintMin) && !Double.isNaN(constraintMax)) {
                        PrimitiveArray cpa = axisPAs[constraintCol];
                        String asc = cpa.isAscending();
                        if (asc.length() == 0) {
                            constraintFirst = cpa.binaryFindFirstGAE5(0, cpa.size() - 1, constraintMin);
                            if (constraintFirst >= cpa.size())
                                constraintFirst = -1;
                            else constraintLast  = cpa.binaryFindLastLAE5(constraintFirst, 
                                cpa.size() - 1, constraintMax);
                            if (debug) String2.log("  constraintAxisVar=" + constraintAxisVarName + 
                                " is ascending.  first=" + constraintFirst + 
                                " last(inclusive)=" + constraintLast);
                            if (constraintFirst >= 0 && constraintLast >= constraintFirst) {
                                //ok, use it
                                readOrigin[ constraintCol] = constraintFirst;
                                axisLengths[constraintCol] = constraintLast - constraintFirst + 1;
                                cpa.removeRange(constraintLast + 1, cpa.size());  
                                cpa.removeRange(0, constraintFirst);
                            }
                        } else {
                            if (debug) String2.log("  constraintAxisVar=" + constraintAxisVarName + 
                                " isn't ascending: " + asc);
                        }
                    }

                    //populate the axes columns
                    NDimensionalIndex ndi = new NDimensionalIndex(axisLengths);
                    Math2.ensureArraySizeOkay(ndi.size(), "Table.readNDNc"); 
                    int nRows = (int)ndi.size(); //safe since checked above
                    int current[] = ndi.getCurrent();
                    for (int a = 0; a < nAxes; a++) 
                        columnPAs[a].ensureCapacity(nRows);
                    while (ndi.increment()) {
                        for (int a = 0; a < nAxes; a++) {
                            //String2.log("  a=" + a + " current[a]=" + current[a] + 
                            //    " axisPAs[a].size=" + axisPAs[a].size());
                            //getDouble not getString since axisVars aren't strings, double is faster
                            columnPAs[a].addDouble(axisPAs[a].getDouble(current[a])); 
                        }
                    }
                }
            }

            //load the 0D variables
            Group rootGroup = ncFile.getRootGroup();
            List rootGroupVariables = rootGroup.getVariables(); 
            int tnRows = nRows();
            for (int v = 0; v < rootGroupVariables.size(); v++) {
                Variable var = (Variable)rootGroupVariables.get(v);
                boolean isChar = var.getDataType() == DataType.CHAR;
                if (var.getRank() + (isChar? -1 : 0) == 0) {
                    //if loadVariableNames specified, skip var because not explicitly requested?
                    if (loadVariableNames.length > 0 && 
                        String2.indexOf(loadVariableNames, var.getShortName()) < 0)
                        continue;

                    //read it
                    PrimitiveArray pa = NcHelper.getPrimitiveArray(var.read());                    
                    //technically, shouldn't trim. 
                    //But more likely problem is source meant to trim but didn't.
                    if (pa instanceof StringArray) 
                        pa.setString(0, pa.getString(0).trim());
                    if (tnRows > 1) {
                        if (pa instanceof StringArray) 
                             pa.addNStrings(tnRows-1, pa.getString(0));
                        else pa.addNDoubles(tnRows-1, pa.getDouble(0));
                    }
    
                    Attributes atts = new Attributes();
                    if (getMetadata)
                        NcHelper.getVariableAttributes(var, atts);
                    addColumn(nColumns(), var.getShortName(), pa, atts);
                }
            }
            
            //load the global metadata
            if (getMetadata)
                NcHelper.getGlobalAttributes(ncFile, globalAttributes());

            //I do care if this throws exception
            ncFile.close(); 

        } catch (Exception e) {
            //make sure ncFile is explicitly closed
            try {
                ncFile.close(); 
            } catch (Exception e2) {
                //don't care
            }

            String2.log(MustBe.throwableToString(e));
        }
        if (verbose) String2.log("  readNDNC finished. nRows=" + nRows() + 
            " nCols=" + nColumns() + " time=" + (System.currentTimeMillis() - time));
    }

    /** This tests readNDNC. */
    public static void testReadNDNc() throws Exception {
        verbose = true;
        reallyVerbose = true;
        String2.log("\n*** Table.testReadNDNc");
        Table table = new Table();
        String results, expected;
        try {

        //test  no vars specified,  4D,  only 2nd dim has >1 value,  getMetadata
        String fiName = "c:/u00/data/points/erdCalcofiSubsurface/1950/subsurface_19500106_69_144.nc";
        table.readNDNc(fiName, null,    null, 0, 0, true);
        results = table.toCSVString();
        expected = 
"{\n" +
"dimensions:\n" +
"\trow = 16 ;\n" +
"variables:\n" +
"\tdouble time(row) ;\n" +
"\t\ttime:long_name = \"time\" ;\n" +
"\t\ttime:units = \"seconds since 1948-1-1\" ;\n" +
"\tfloat depth(row) ;\n" +
"\t\tdepth:long_name = \"depth index\" ;\n" +
"\tfloat lat(row) ;\n" +
"\t\tlat:_FillValue = -999.0f ;\n" +
"\t\tlat:long_name = \"latitude\" ;\n" +
"\t\tlat:missing_value = -999.0f ;\n" +
"\t\tlat:units = \"degrees_north\" ;\n" +
"\tfloat lon(row) ;\n" +
"\t\tlon:_FillValue = -999.0f ;\n" +
"\t\tlon:long_name = \"longitude\" ;\n" +
"\t\tlon:missing_value = -999.0f ;\n" +
"\t\tlon:units = \"degrees_east\" ;\n" +
"\tint stationyear(row) ;\n" +
"\t\tstationyear:_FillValue = -9999 ;\n" +
"\t\tstationyear:long_name = \"Station Year\" ;\n" +
"\t\tstationyear:missing_value = -9999 ;\n" +
"\t\tstationyear:units = \"years\" ;\n" +
"\tint stationmonth(row) ;\n" +
"\t\tstationmonth:_FillValue = -9999 ;\n" +
"\t\tstationmonth:long_name = \"Station Month\" ;\n" +
"\t\tstationmonth:missing_value = -9999 ;\n" +
"\t\tstationmonth:units = \"months\" ;\n" +
"\tint stationday(row) ;\n" +
"\t\tstationday:_FillValue = -9999 ;\n" +
"\t\tstationday:long_name = \"Station Day\" ;\n" +
"\t\tstationday:missing_value = -9999 ;\n" +
"\t\tstationday:units = \"days\" ;\n" +
"\tint stime(row) ;\n" +
"\t\tstime:_FillValue = -9999 ;\n" +
"\t\tstime:long_name = \"Cast Time (GMT)\" ;\n" +
"\t\tstime:missing_value = -9999 ;\n" +
"\t\tstime:units = \"GMT\" ;\n" +
"\tfloat stationline(row) ;\n" +
"\t\tstationline:_FillValue = -999.0f ;\n" +
"\t\tstationline:long_name = \"CALCOFI Line Number\" ;\n" +
"\t\tstationline:missing_value = -999.0f ;\n" +
"\t\tstationline:units = \"number\" ;\n" +
"\tfloat stationnum(row) ;\n" +
"\t\tstationnum:_FillValue = -999.0f ;\n" +
"\t\tstationnum:long_name = \"CALCOFI Station Number\" ;\n" +
"\t\tstationnum:missing_value = -999.0f ;\n" +
"\t\tstationnum:units = \"number\" ;\n" +
"\tfloat temperature(row) ;\n" +
"\t\ttemperature:_FillValue = -999.0f ;\n" +
"\t\ttemperature:has_data = 1 ;\n" +
"\t\ttemperature:long_name = \"Temperature\" ;\n" +
"\t\ttemperature:missing_value = -999.0f ;\n" +
"\t\ttemperature:units = \"degC\" ;\n" +
"\tfloat salinity(row) ;\n" +
"\t\tsalinity:_FillValue = -999.0f ;\n" +
"\t\tsalinity:has_data = 1 ;\n" +
"\t\tsalinity:long_name = \"Salinity\" ;\n" +
"\t\tsalinity:missing_value = -999.0f ;\n" +
"\t\tsalinity:units = \"PSU\" ;\n" +
"\tfloat pressure(row) ;\n" +
"\t\tpressure:_FillValue = -999.0f ;\n" +
"\t\tpressure:has_data = 0 ;\n" +
"\t\tpressure:long_name = \"Pressure\" ;\n" +
"\t\tpressure:missing_value = -999.0f ;\n" +
"\t\tpressure:units = \"decibars\" ;\n" +
"\tfloat oxygen(row) ;\n" +
"\t\toxygen:_FillValue = -999.0f ;\n" +
"\t\toxygen:has_data = 1 ;\n" +
"\t\toxygen:long_name = \"Oxygen\" ;\n" +
"\t\toxygen:missing_value = -999.0f ;\n" +
"\t\toxygen:units = \"milliliters/liter\" ;\n" +
"\tfloat po4(row) ;\n" +
"\t\tpo4:_FillValue = -999.0f ;\n" +
"\t\tpo4:has_data = 1 ;\n" +
"\t\tpo4:long_name = \"Phosphate\" ;\n" +
"\t\tpo4:missing_value = -999.0f ;\n" +
"\t\tpo4:units = \"ugram-atoms/liter\" ;\n" +
"\tfloat silicate(row) ;\n" +
"\t\tsilicate:_FillValue = -999.0f ;\n" +
"\t\tsilicate:has_data = 0 ;\n" +
"\t\tsilicate:long_name = \"Silicate\" ;\n" +
"\t\tsilicate:missing_value = -999.0f ;\n" +
"\t\tsilicate:units = \"ugram-atoms/liter\" ;\n" +
"\tfloat no2(row) ;\n" +
"\t\tno2:_FillValue = -999.0f ;\n" +
"\t\tno2:has_data = 0 ;\n" +
"\t\tno2:long_name = \"Nitrite\" ;\n" +
"\t\tno2:missing_value = -999.0f ;\n" +
"\t\tno2:units = \"ugram-atoms/liter\" ;\n" +
"\tfloat no3(row) ;\n" +
"\t\tno3:_FillValue = -999.0f ;\n" +
"\t\tno3:has_data = 0 ;\n" +
"\t\tno3:long_name = \"Nitrate\" ;\n" +
"\t\tno3:missing_value = -999.0f ;\n" +
"\t\tno3:units = \"ugram-atoms/liter\" ;\n" +
"\tfloat nh3(row) ;\n" +
"\t\tnh3:_FillValue = -999.0f ;\n" +
"\t\tnh3:has_data = 0 ;\n" +
"\t\tnh3:long_name = \"Ammonia\" ;\n" +
"\t\tnh3:missing_value = -999.0f ;\n" +
"\t\tnh3:units = \"ugram-atoms/liter\" ;\n" +
"\tfloat chl(row) ;\n" +
"\t\tchl:_FillValue = -999.0f ;\n" +
"\t\tchl:has_data = 0 ;\n" +
"\t\tchl:long_name = \"Chlorophyll-a\" ;\n" +
"\t\tchl:missing_value = -999.0f ;\n" +
"\t\tchl:units = \"milligrams/meter**3\" ;\n" +
"\tfloat dark(row) ;\n" +
"\t\tdark:_FillValue = -999.0f ;\n" +
"\t\tdark:has_data = 0 ;\n" +
"\t\tdark:long_name = \"Dark Bottle C14 Assimilation\" ;\n" +
"\t\tdark:missing_value = -999.0f ;\n" +
"\t\tdark:units = \"milligrams/meter**3/experiment\" ;\n" +
"\tfloat primprod(row) ;\n" +
"\t\tprimprod:_FillValue = -999.0f ;\n" +
"\t\tprimprod:has_data = 0 ;\n" +
"\t\tprimprod:long_name = \"Mean Primary Production (C14 Assimilation)\" ;\n" +
"\t\tprimprod:missing_value = -999.0f ;\n" +
"\t\tprimprod:units = \"milligrams/meter**3/experiment\" ;\n" +
"\tfloat lightpercent(row) ;\n" +
"\t\tlightpercent:_FillValue = -999.0f ;\n" +
"\t\tlightpercent:has_data = 0 ;\n" +
"\t\tlightpercent:long_name = \"Percent Light (for incubations)\" ;\n" +
"\t\tlightpercent:missing_value = -999.0f ;\n" +
"\t\tlightpercent:units = \"milligrams/meter**3/experiment\" ;\n" +
"\n" +
"// global attributes:\n" +
"\t\t:history = \"created by ERD from Matlab database created by Andrew Leising  from the CalCOFI Physical data\" ;\n" +
"\t\t:title = \"CalCOFI Physical Observations, 1949-2001\" ;\n" +
"}\n" +
"row,time,depth,lat,lon,stationyear,stationmonth,stationday,stime,stationline,stationnum,temperature,salinity,pressure,oxygen,po4,silicate,no2,no3,nh3,chl,dark,primprod,lightpercent\n" +
"0,6.3612E7,0.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,16.19,33.6,-999.0,5.3,0.42,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"1,6.3612E7,22.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,16.18,33.6,-999.0,5.26,0.38,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"2,6.3612E7,49.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,16.2,33.6,-999.0,5.3,0.36,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"3,6.3612E7,72.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,14.95,33.58,-999.0,5.51,0.37,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"4,6.3612E7,98.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,13.02,33.35,-999.0,5.35,0.45,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"5,6.3612E7,147.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,11.45,33.36,-999.0,4.99,0.81,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"6,6.3612E7,194.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,9.32,33.55,-999.0,4.47,1.19,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"7,6.3612E7,241.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,8.51,33.85,-999.0,4.02,1.51,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"8,6.3612E7,287.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,7.74,33.95,-999.0,3.48,1.76,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"9,6.3612E7,384.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,6.42,33.97,-999.0,2.55,2.15,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"10,6.3612E7,477.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,5.35,34.04,-999.0,1.29,2.48,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"11,6.3612E7,576.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,4.83,34.14,-999.0,0.73,2.73,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"12,6.3612E7,673.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,4.44,34.22,-999.0,0.48,2.9,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"13,6.3612E7,768.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,4.15,34.31,-999.0,0.37,2.87,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"14,6.3612E7,969.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,3.67,34.43,-999.0,0.49,2.8,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"15,6.3612E7,1167.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,3.3,34.49,-999.0,0.66,2.7,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test same but !getMetadata
        table.readNDNc(fiName, null,    null, 0, 0, false);
        results = table.toCSVString();
        expected = 
"{\n" +
"dimensions:\n" +
"\trow = 16 ;\n" +
"variables:\n" +
"\tdouble time(row) ;\n" +
"\tfloat depth(row) ;\n" +
"\tfloat lat(row) ;\n" +
"\tfloat lon(row) ;\n" +
"\tint stationyear(row) ;\n" +
"\tint stationmonth(row) ;\n" +
"\tint stationday(row) ;\n" +
"\tint stime(row) ;\n" +
"\tfloat stationline(row) ;\n" +
"\tfloat stationnum(row) ;\n" +
"\tfloat temperature(row) ;\n" +
"\tfloat salinity(row) ;\n" +
"\tfloat pressure(row) ;\n" +
"\tfloat oxygen(row) ;\n" +
"\tfloat po4(row) ;\n" +
"\tfloat silicate(row) ;\n" +
"\tfloat no2(row) ;\n" +
"\tfloat no3(row) ;\n" +
"\tfloat nh3(row) ;\n" +
"\tfloat chl(row) ;\n" +
"\tfloat dark(row) ;\n" +
"\tfloat primprod(row) ;\n" +
"\tfloat lightpercent(row) ;\n" +
"\n" +
"// global attributes:\n" +
"}\n" +
"row,time,depth,lat,lon,stationyear,stationmonth,stationday,stime,stationline,stationnum,temperature,salinity,pressure,oxygen,po4,silicate,no2,no3,nh3,chl,dark,primprod,lightpercent\n" +
"0,6.3612E7,0.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,16.19,33.6,-999.0,5.3,0.42,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"1,6.3612E7,22.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,16.18,33.6,-999.0,5.26,0.38,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"2,6.3612E7,49.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,16.2,33.6,-999.0,5.3,0.36,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"3,6.3612E7,72.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,14.95,33.58,-999.0,5.51,0.37,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"4,6.3612E7,98.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,13.02,33.35,-999.0,5.35,0.45,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"5,6.3612E7,147.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,11.45,33.36,-999.0,4.99,0.81,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"6,6.3612E7,194.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,9.32,33.55,-999.0,4.47,1.19,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"7,6.3612E7,241.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,8.51,33.85,-999.0,4.02,1.51,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"8,6.3612E7,287.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,7.74,33.95,-999.0,3.48,1.76,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"9,6.3612E7,384.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,6.42,33.97,-999.0,2.55,2.15,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"10,6.3612E7,477.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,5.35,34.04,-999.0,1.29,2.48,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"11,6.3612E7,576.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,4.83,34.14,-999.0,0.73,2.73,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"12,6.3612E7,673.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,4.44,34.22,-999.0,0.48,2.9,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"13,6.3612E7,768.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,4.15,34.31,-999.0,0.37,2.87,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"14,6.3612E7,969.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,3.67,34.43,-999.0,0.49,2.8,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n" +
"15,6.3612E7,1167.0,33.31667,-128.53333,1950,1,6,600,69.0,144.0,3.3,34.49,-999.0,0.66,2.7,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0,-999.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test specify vars (including out-of-order axis var, and nonsense var), !getMetadata
        table.readNDNc(fiName, new String[]{"temperature", "lat", "salinity", "junk"},  "depth", 100, 200, false);
        results = table.dataToCSVString(Integer.MAX_VALUE);
        expected = 
"row,time,depth,lat,lon,temperature,salinity\n" +
"0,6.3612E7,147.0,33.31667,-128.53333,11.45,33.36\n" +
"1,6.3612E7,194.0,33.31667,-128.53333,9.32,33.55\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test String vars 
        fiName = "f:/u00/cwatch/erddap2/copy/cPostDet3/BARBARAx20BLOCK/LAMNAx20DITROPIS/Nx2fA/52038_A69-1303_1059305.nc";
        table.readNDNc(fiName, null,  null, 0, 0, false);
        results = table.dataToCSVString(4);
        expected = 
"row,row,unique_tag_id,PI,longitude,latitude,time,bottom_depth,common_name,date_public,line,position_on_subarray,project,riser_height,role,scientific_name,serial_number,stock,surgery_time,surgery_location,tagger\n" +
"0,0,52038_A69-1303_1059305,BARBARA BLOCK,-146.1137,60.7172,1.2192849E9,,SALMON SHARK,1.273271649385E9,,,HOPKINS MARINE STATION,,BLOCK_BARBARA_LAMNA_DITROPIS_N/A,LAMNA DITROPIS,1059305,N/A,1.2192156E9,\"PORT GRAVINA, PRINCE WILLIAM SOUND\",\n" +
"1,1,52038_A69-1303_1059305,BARBARA BLOCK,-146.32355,60.66713,1.233325298E9,127.743902439024,SALMON SHARK,1.273271649385E9,PORT GRAVINA,6,HOPKINS MARINE STATION,,BLOCK_BARBARA_LAMNA_DITROPIS_N/A,LAMNA DITROPIS,1059305,N/A,1.2192156E9,\"PORT GRAVINA, PRINCE WILLIAM SOUND\",\n" +
"2,2,52038_A69-1303_1059305,BARBARA BLOCK,-146.32355,60.66713,1.233325733E9,127.743902439024,SALMON SHARK,1.273271649385E9,PORT GRAVINA,6,HOPKINS MARINE STATION,,BLOCK_BARBARA_LAMNA_DITROPIS_N/A,LAMNA DITROPIS,1059305,N/A,1.2192156E9,\"PORT GRAVINA, PRINCE WILLIAM SOUND\",\n" +
"3,3,52038_A69-1303_1059305,BARBARA BLOCK,-146.32355,60.66713,1.233325998E9,127.743902439024,SALMON SHARK,1.273271649385E9,PORT GRAVINA,6,HOPKINS MARINE STATION,,BLOCK_BARBARA_LAMNA_DITROPIS_N/A,LAMNA DITROPIS,1059305,N/A,1.2192156E9,\"PORT GRAVINA, PRINCE WILLIAM SOUND\",\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test 4D but request axis vars only, with constraints
        fiName = "/u00/data/points/ndbcMet/NDBC_51001_met.nc"; //implied c:
        table.readNDNc(fiName, new String[]{"LON", "LAT", "TIME"},  "TIME", 1.2051936e9, 1.20528e9, false);
        results = table.dataToCSVString(4);
        expected = 
//"row, LON, LAT, TIME\n" +   //pre 2011-07-28
//"0, -162.21, 23.43, 1.2051828E9\n" +
//"1, -162.21, 23.43, 1.2051864E9\n" +
//"2, -162.21, 23.43, 1.20519E9\n" +
//"3, -162.21, 23.43, 1.2051936E9\n";
"row,LON,LAT,TIME\n" +
"0,-162.279,23.445,1.2051828E9\n" +
"1,-162.279,23.445,1.2051864E9\n" +
"2,-162.279,23.445,1.20519E9\n" +
"3,-162.279,23.445,1.2051936E9\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml." + 
                "\nPress ^C to stop or Enter to continue..."); 
        }
        
    }

        
    /** This tests readNDNC. */
    public static void testReadNDNc2() throws Exception {
        verbose = true;
        reallyVerbose = true;
        String2.log("\n*** Table.testReadNDNc2");
        String fiName = "F:/data/wod/monthly/APB/199804-199804/wod_008015632O.nc";
        Table table = new Table();
        String results, expected;

        //test  no vars specified
        table.readNDNc(fiName, null, null, 0, 0, true);
        results = table.toCSVString();
        expected = 
"{\n" +
"dimensions:\n" +
"\trow = 11 ;\n" +
"\tWOD_cruise_identifier_strlen = 8 ;\n" +
"\tProject_strlen = 49 ;\n" +
"\tdataset_strlen = 14 ;\n" +
"variables:\n" +
"\tfloat z(row) ;\n" +
"\t\tz:long_name = \"depth_below_sea_level\" ;\n" +
"\t\tz:positive = \"down\" ;\n" +
"\t\tz:standard_name = \"altitude\" ;\n" +
"\t\tz:units = \"m\" ;\n" +
"\tfloat Temperature(row) ;\n" +
"\t\tTemperature:coordinates = \"time lat lon z\" ;\n" +
"\t\tTemperature:flag_definitions = \"WODfp\" ;\n" +
"\t\tTemperature:grid_mapping = \"crs\" ;\n" +
"\t\tTemperature:Instrument_(WOD_code) = \"UNDERWAY: MK3 data recording tag (Wildlife Computers) mounted on elephant seal\" ;\n" +
"\t\tTemperature:long_name = \"Temperature\" ;\n" +
"\t\tTemperature:standard_name = \"sea_water_temperature\" ;\n" +
"\t\tTemperature:units = \"degree_C\" ;\n" +
"\t\tTemperature:WODprofile_flag = 9 ;\n" +
"\tint Temperature_sigfigs(row) ;\n" +
"\tint Temperature_WODflag(row) ;\n" +
"\t\tTemperature_WODflag:flag_definitions = \"WODf\" ;\n" +
"\tchar WOD_cruise_identifier(row, WOD_cruise_identifier_strlen) ;\n" +
"\t\tWOD_cruise_identifier:comment = \"two byte country code + WOD cruise number (unique to country code)\" ;\n" +
"\t\tWOD_cruise_identifier:country = \"UNITED STATES\" ;\n" +
"\t\tWOD_cruise_identifier:long_name = \"WOD_cruise_identifier\" ;\n" +
"\tint wod_unique_cast(row) ;\n" +
"\tfloat lat(row) ;\n" +
"\t\tlat:axis = \"Y\" ;\n" +
"\t\tlat:long_name = \"latitude\" ;\n" +
"\t\tlat:standard_name = \"latitude\" ;\n" +
"\t\tlat:units = \"degrees_north\" ;\n" +
"\tfloat lon(row) ;\n" +
"\t\tlon:axis = \"X\" ;\n" +
"\t\tlon:long_name = \"longitude\" ;\n" +
"\t\tlon:standard_name = \"longitude\" ;\n" +
"\t\tlon:units = \"degrees_east\" ;\n" +
"\tdouble time(row) ;\n" +
"\t\ttime:axis = \"T\" ;\n" +
"\t\ttime:long_name = \"time\" ;\n" +
"\t\ttime:standard_name = \"time\" ;\n" +
"\t\ttime:units = \"days since 1770-01-01 00:00:00\" ;\n" +
"\tint date(row) ;\n" +
"\t\tdate:comment = \"YYYYMMDD\" ;\n" +
"\t\tdate:long_name = \"date\" ;\n" +
"\tfloat GMT_time(row) ;\n" +
"\t\tGMT_time:long_name = \"GMT_time\" ;\n" +
"\tint Access_no(row) ;\n" +
"\t\tAccess_no:comment = \"used to find original data at NODC\" ;\n" +
"\t\tAccess_no:long_name = \"NODC_accession_number\" ;\n" +
"\t\tAccess_no:units = \"NODC_code\" ;\n" +
"\tchar Project(row, Project_strlen) ;\n" +
"\t\tProject:comment = \"name or acronym of project under which data were measured\" ;\n" +
"\t\tProject:long_name = \"Project_name\" ;\n" +
"\tchar dataset(row, dataset_strlen) ;\n" +
"\t\tdataset:long_name = \"WOD_dataset\" ;\n" +
"\tfloat ARGOS_last_fix(row) ;\n" +
"\t\tARGOS_last_fix:units = \"hours\" ;\n" +
"\tfloat ARGOS_next_fix(row) ;\n" +
"\t\tARGOS_next_fix:units = \"hours\" ;\n" +
"\tint crs(row) ;\n" +
"\t\tcrs:epsg_code = \"EPSG:4326\" ;\n" +
"\t\tcrs:grid_mapping_name = \"latitude_longitude\" ;\n" +
"\t\tcrs:inverse_flattening = 298.25723f ;\n" +
"\t\tcrs:longitude_of_prime_meridian = 0.0f ;\n" +
"\t\tcrs:semi_major_axis = 6378137.0f ;\n" +
"\tint profile(row) ;\n" +
"\tint WODf(row) ;\n" +
"\t\tWODf:flag_meanings = \"accepted range_out inversion gradient anomaly gradient+inversion range+inversion range+gradient range+anomaly range+inversion+gradient\" ;\n" +
"\t\tWODf:flag_values = \"0 1 2 3 4 5 6 7 8 9\" ;\n" +
"\t\tWODf:long_name = \"WOD_observation_flag\" ;\n" +
"\tint WODfp(row) ;\n" +
"\t\tWODfp:flag_meanings = \"accepted annual_sd_out density_inversion cruise seasonal_sd_out monthly_sd_out annual+seasonal_sd_out anomaly_or_annual+monthly_sd_out seasonal+monthly_sd_out annual+seasonal+monthly_sd_out\" ;\n" +
"\t\tWODfp:flag_values = \"0 1 2 3 4 5 6 7 8 9\" ;\n" +
"\t\tWODfp:long_name = \"WOD_profile_flag\" ;\n" +
"\tint WODfd(row) ;\n" +
"\t\tWODfd:flag_meanings = \"accepted duplicate_or_inversion density_inversion\" ;\n" +
"\t\tWODfd:flag_values = \"0 1 2\" ;\n" +
"\t\tWODfd:long_name = \"WOD_depth_level_\" ;\n" +
"\n" +
"// global attributes:\n" +
"\t\t:cdm_data_type = \"Profile\" ;\n" +
"\t\t:cf_role = \"profile_id\" ;\n" +
"\t\t:Conventions = \"CF-1.5\" ;\n" +
"\t\t:featureType = \"profile\" ;\n" +
"\t\t:Metadata_Conventions = \"Unidata Dataset Discovery v1.0\" ;\n" +
"\t\t:standard_name_vocabulary = \"CF-1.5\" ;\n" +
"}\n" +
"row,z,Temperature,Temperature_sigfigs,Temperature_WODflag,WOD_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Project,dataset,ARGOS_last_fix,ARGOS_next_fix,crs,profile,WODf,WODfp,WODfd\n" +
"0,0.0,7.9,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"1,10.0,7.9,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"2,42.0,7.8,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"3,76.0,7.8,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"4,120.0,7.8,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"5,166.0,7.5,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"6,212.0,7.0,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"7,260.0,6.5,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"8,308.0,5.8,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"9,354.0,5.2,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"10,402.0,4.9,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test same but !getMetadata
        table.readNDNc(fiName, null, null, 0, 0, false);
        results = table.toCSVString();
        expected = 
"{\n" +
"dimensions:\n" +
"\trow = 11 ;\n" +
"\tWOD_cruise_identifier_strlen = 8 ;\n" +
"\tProject_strlen = 49 ;\n" +
"\tdataset_strlen = 14 ;\n" +
"variables:\n" +
"\tfloat z(row) ;\n" +
"\tfloat Temperature(row) ;\n" +
"\tint Temperature_sigfigs(row) ;\n" +
"\tint Temperature_WODflag(row) ;\n" +
"\tchar WOD_cruise_identifier(row, WOD_cruise_identifier_strlen) ;\n" +
"\tint wod_unique_cast(row) ;\n" +
"\tfloat lat(row) ;\n" +
"\tfloat lon(row) ;\n" +
"\tdouble time(row) ;\n" +
"\tint date(row) ;\n" +
"\tfloat GMT_time(row) ;\n" +
"\tint Access_no(row) ;\n" +
"\tchar Project(row, Project_strlen) ;\n" +
"\tchar dataset(row, dataset_strlen) ;\n" +
"\tfloat ARGOS_last_fix(row) ;\n" +
"\tfloat ARGOS_next_fix(row) ;\n" +
"\tint crs(row) ;\n" +
"\tint profile(row) ;\n" +
"\tint WODf(row) ;\n" +
"\tint WODfp(row) ;\n" +
"\tint WODfd(row) ;\n" +
"\n" +
"// global attributes:\n" +
"}\n" +
"row,z,Temperature,Temperature_sigfigs,Temperature_WODflag,WOD_cruise_identifier,wod_unique_cast,lat,lon,time,date,GMT_time,Access_no,Project,dataset,ARGOS_last_fix,ARGOS_next_fix,crs,profile,WODf,WODfp,WODfd\n" +
"0,0.0,7.9,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"1,10.0,7.9,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"2,42.0,7.8,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"3,76.0,7.8,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"4,120.0,7.8,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"5,166.0,7.5,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"6,212.0,7.0,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"7,260.0,6.5,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"8,308.0,5.8,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"9,354.0,5.2,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n" +
"10,402.0,4.9,2,0,US025547,8015632,45.28,-142.24,83369.90625,19980403,21.81665,573,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES),animal mounted,4.836731,14.149658,-2147483647,-2147483647,-2147483647,-2147483647,-2147483647\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test specify 0D and 1D data vars (out-of-order, implied axis var, and nonsense var), !getMetadata
        table.readNDNc(fiName, new String[]{
            "lon", "lat", "time", "Temperature", "WOD_cruise_identifier", "junk"},  "z", 100, 200, false);
        results = table.dataToCSVString(Integer.MAX_VALUE);
        expected = 
"row,z,Temperature,WOD_cruise_identifier,lat,lon,time\n" +
"0,120.0,7.8,US025547,45.28,-142.24,83369.90625\n" +
"1,166.0,7.5,US025547,45.28,-142.24,83369.90625\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //request axis vars only, with constraints
        table.readNDNc(fiName, new String[]{"z"},  "z", 100, 200, false);
        results = table.dataToCSVString(4);
        expected = 
"row,z\n" +
"0,120.0\n" +
"1,166.0\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
        
        //request 0D vars only, with constraints (ignored)
        table.readNDNc(fiName, new String[]{
            "WOD_cruise_identifier", "Project", "junk", "lon", "lat"},  "z", 100, 200, false);
        results = table.dataToCSVString(4);
        expected = 
"row,WOD_cruise_identifier,lat,lon,Project\n" +
"0,US025547,45.28,-142.24,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES)\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
        
        //request axis var and 0D vars only, with constraints
        table.readNDNc(fiName, new String[]{
            "WOD_cruise_identifier", "Project", "z", "junk", "lon", "lat"},  "z", 100, 200, false);
        results = table.dataToCSVString(4);
        expected = 
"row,z,WOD_cruise_identifier,lat,lon,Project\n" +
"0,120.0,US025547,45.28,-142.24,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES)\n" +
"1,166.0,US025547,45.28,-142.24,AUTONOMOUS PINNIPED ENVIRONMENTAL SAMPLERS (APES)\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
        
    }



    /**
     * This is a minimalist readNetcdf which just reads specified rows
     * from specified variables from an .nc file and appends them
     * to the current data columns.
     * This doesn't read global attributes or variable attributes.
     * This doesn't unpack packed variables.
     * If the table initially has no columns, this creates columns
     * and reads column names; otherwise it doesn't read column names.
     *
     * @param loadVariables the variables in the .nc file which correspond
     *     to the columns in this table.
     *     They must all be ArrayXxx.D1 or ArrayChar.D2 variables and use the 
     *     same, one, dimension as the first dimension.
     *     If this is null or empty, nothing is done.
     * @param firstRow  the first row to be appended.
     * @param lastRow    the last row to be appended (inclusive).
     *   If lastRow is -1, firstRow is ignored and all of the data will be appended.
     * @throws Exception if trouble
     */
    public void appendNcRows(Variable loadVariables[], int firstRow, int lastRow) throws Exception {

        //if (verbose) String2.log("Table.appendNcRows firstRow=" + firstRow + 
        //    " lastRow=" + lastRow);
        if (loadVariables == null || loadVariables.length == 0) {
            if (verbose) String2.log("Table.appendNcRows: nVariables = 0 so nothing done");
            return;
        }

        boolean needToAddColumns = nColumns() == 0;
        for (int col = 0; col < loadVariables.length; col++) {

            //get the data 
            Variable variable = loadVariables[col];
            PrimitiveArray pa = lastRow == -1?
                NcHelper.getPrimitiveArray(variable) :
                NcHelper.getPrimitiveArray(variable, firstRow, lastRow);
            if (needToAddColumns) {
                addColumn(variable.getShortName(), pa);
            } else getColumn(col).append(pa);
        }
    }


    /**
     * This appends the okRows from a .nc file.
     * This doesn't read global attributes or variable attributes.
     * This doesn't unpack packed variables.
     * If the table initially has no columns, this creates columns
     * and reads column names; otherwise it doesn't read column names.
     *
     * @param loadVariables the variables to be loaded.
     *     They must all be ArrayXxx.D1 or ArrayChar.D2 variables and use the 
     *     same, one, dimension as the first dimension.
     *     This must not be null or empty.
     *     If you have variable names, use ncFile.findVariable(name);
     * @param okRows  
     * @throws Exception if trouble
     */
    public void appendNcRows(Variable loadVariables[], BitSet okRows) throws Exception {
        //this is tested in PointSubset

        String errorInMethod = String2.ERROR + " in appendNcRows: ";
        long time = System.currentTimeMillis();

        //get the desired rows   (first call adds pa's to data and adds columnNames)
        int n = okRows.size();
        int firstRow = okRows.nextSetBit(0);
        int nAppendCalls = 0;
        while (firstRow >= 0) {
            //find end of sequence of okRows
            int endRow = firstRow == n-1? n : okRows.nextClearBit(firstRow + 1);
            if (endRow == -1)
                endRow = n;

            //get the data
            appendNcRows(loadVariables, firstRow, endRow - 1);
            nAppendCalls++;
            
            //first start of next sequence of okRows
            if (endRow >= n - 1)
                firstRow = -1;
            else firstRow = okRows.nextSetBit(endRow + 1); 
        }
        String2.log("Table.appendNcRows done. nAppendCalls=" + nAppendCalls +
            " nRows=" + nRows() + 
            " TIME=" + (System.currentTimeMillis() - time));

    }



    /**
     * This appends okRows rows from a .nc file by doing one big read
     * and then removing unwanted rows -- thus it may be more suited
     * for opendap since it may avoid huge numbers of separate reads.
     * This doesn't read global attributes or variable attributes.
     * This doesn't unpack packed variables.
     * If the table initially has no columns, this creates columns
     * and reads column names; otherwise it doesn't read column names.
     *
     * @param loadVariables the variables to be loaded.
     *     They must all be ArrayXxx.D1 or ArrayChar.D2 variables and use the 
     *     same, one, dimension as the first dimension.
     *     This must not be null or empty.
     * @param okRows
     * @throws Exception if trouble
     */
    public void blockAppendNcRows(Variable loadVariables[], BitSet okRows) throws Exception {
        //this is tested in PointSubset

        String errorInMethod = String2.ERROR + " in blockAppendNcRows: ";
        long time = System.currentTimeMillis();
      
        //!!****THIS HASN'T BEEN MODIFIED TO DO BLOCK READ YET

        //get the desired rows   (first call adds pa's to data and adds columnNames)
        int n = okRows.size();
        int firstRow = okRows.nextSetBit(0);
        int nAppendCalls = 0;
        while (firstRow >= 0) {
            //find end of sequence of okRows
            int endRow = firstRow == n-1? n : okRows.nextClearBit(firstRow + 1);
            if (endRow == -1)
                endRow = n;

            //get the data
            appendNcRows(loadVariables, firstRow, endRow - 1);
            nAppendCalls++;
            
            //first start of next sequence of okRows
            if (endRow >= n - 1)
                firstRow = -1;
            else firstRow = okRows.nextSetBit(endRow + 1); 
        }
        String2.log("Table.blockAppendNcRows done. nAppendCalls=" + nAppendCalls +
            " nRows=" + nRows() + 
            " TIME=" + (System.currentTimeMillis() - time));

    }

    /**
     * This calls convertToStandardMissingValues for all columns.
     *
     * <p>!!!This is used inside the readXxx methods. 
     * And this is used inside saveAsXxx methods to convert fake missing values
     * back to NaNs. It is rarely called elsewhere.
     *
     */
    public void convertToStandardMissingValues() {
        int nColumns = nColumns();
        for (int col = 0; col < nColumns; col++)
            convertToStandardMissingValues(col);
    }

    /**
     * If this column has _FillValue and/or missing_value attributes,
     * those values are converted to PrimitiveArray-style missing values
     * and the attributes are removed.
     *
     * <p>!!!This is used inside the readXxx methods. 
     * And this is used inside saveAsXxx methods to convert fake missing values
     * back to NaNs. It is rarely called elsewhere.
     *
     * @param column
     */
    public void convertToStandardMissingValues(int column) {
        Attributes colAtt = columnAttributes(column);
        //double mv = colAtt.getDouble("missing_value");
        int nSwitched = getColumn(column).convertToStandardMissingValues( 
            colAtt.getDouble("_FillValue"),
            colAtt.getDouble("missing_value"));
        //if (!Double.isNaN(mv)) String2.log("  convertToStandardMissingValues mv=" + mv + " n=" + nSwitched);

        //remove current attributes
        colAtt.remove("missing_value");
        colAtt.remove("_FillValue");
    }


    /**
     * This calls convertToFakeMissingValues for all columns.
     * !!!This is used inside the saveAsXxx methods to temporarily 
     * convert to fake missing values. It is rarely called elsewhere.
     *
     */
    public void convertToFakeMissingValues() {
        int nColumns = nColumns();
        for (int col = 0; col < nColumns; col++)
            convertToFakeMissingValues(col);
    }

    /**
     * This sets (or revises) the missing_value and _FillValue metadata
     * to DataHelper.FAKE_MISSING_VALUE for FloatArray and DoubleArray 
     * and the standard missing value (e.g., Xxx.MAX_VALUE) for other PrimitiveArrays.
     * This works on the current (possibly packed) data. So call this when the
     * data is packed.
     * If the column is a String column, nothing will be done.
     *
     * <p>!!!This is used inside the saveAsXxx methods to temporarily 
     * convert to fake missing values. It is rarely called elsewhere.
     *
     * @param column
     */
    public void convertToFakeMissingValues(int column) {
        //String2.log("Table.convertToFakeMissingValues column=" + column);
        PrimitiveArray pa = getColumn(column);
        if (pa instanceof StringArray) 
            return;
        //boolean removeMVF = false;  //commented out 2010-10-26 so NDBC files have consistent _FillValue
        if      (pa instanceof ByteArray) {
            columnAttributes(column).set("missing_value", Byte.MAX_VALUE);
            columnAttributes(column).set("_FillValue",    Byte.MAX_VALUE);
            //removeMVF = ((ByteArray)pa).indexOf(Byte.MAX_VALUE, 0) < 0;
        } else if (pa instanceof CharArray) {
            columnAttributes(column).set("missing_value", Character.MAX_VALUE);
            columnAttributes(column).set("_FillValue",    Character.MAX_VALUE);
            //removeMVF = ((CharArray)pa).indexOf(Character.MAX_VALUE, 0) < 0;
        } else if (pa instanceof ShortArray) {
            columnAttributes(column).set("missing_value", Short.MAX_VALUE);
            columnAttributes(column).set("_FillValue",    Short.MAX_VALUE);
            //removeMVF = ((ShortArray)pa).indexOf(Short.MAX_VALUE, 0) < 0;
        } else if (pa instanceof IntArray) {
            columnAttributes(column).set("missing_value", Integer.MAX_VALUE);
            columnAttributes(column).set("_FillValue",    Integer.MAX_VALUE);
            //removeMVF = ((IntArray)pa).indexOf(Integer.MAX_VALUE, 0) < 0;
        } else if (pa instanceof LongArray) {
            columnAttributes(column).set("missing_value", Long.MAX_VALUE);
            columnAttributes(column).set("_FillValue",    Long.MAX_VALUE);
            //removeMVF = ((LongArray)pa).indexOf(Long.MAX_VALUE, 0) < 0;
        } else if (pa instanceof FloatArray) {
            columnAttributes(column).set("missing_value", (float)DataHelper.FAKE_MISSING_VALUE);
            columnAttributes(column).set("_FillValue",    (float)DataHelper.FAKE_MISSING_VALUE);
            pa.switchFromTo("", "" + DataHelper.FAKE_MISSING_VALUE);
            //removeMVF = ((FloatArray)pa).indexOf((float)DataHelper.FAKE_MISSING_VALUE, 0) < 0; //safer to search, in case values were already fake mv
        } else if (pa instanceof DoubleArray) {
            columnAttributes(column).set("missing_value", DataHelper.FAKE_MISSING_VALUE);
            columnAttributes(column).set("_FillValue",    DataHelper.FAKE_MISSING_VALUE);
            pa.switchFromTo("", "" + DataHelper.FAKE_MISSING_VALUE);
            //removeMVF = ((DoubleArray)pa).indexOf(DataHelper.FAKE_MISSING_VALUE, 0) < 0; //safer to search, in case values were already fake mv
        } else return; //do nothing if e.g., StringArray

        //if there are no mv's, remove missing_value and _FillValue attributes.
        //For example, coordinate columns (lon, lat, depth, time) should have
        //no missing values and hence no missing_value information.
        //Also, there is trouble with new FAKE_MISSING_VALUE (-9999999) being used for 
        //climatology time column: it is a valid time.
        //if (removeMVF) {
        //    columnAttributes(column).remove("missing_value");
        //    columnAttributes(column).remove("_FillValue");
        //}

    }

    /**
     * This unpacks a column if unpack>0 and the attributes scale_factor and 
     * add_offset are specified.
     * This revises the metadata: scale_factor and add_offset are removed;
     *   _FillValue and missing_value are modified to the unpacked values.    
     *
     * @param column
     * @param unpack 0=don't 1=to float 
     *     '1' and '2' only work if the attributes scale_factor and add_offset are specified.
     */
    public void tryToUnpack(int column, int unpack) {
        String errorInMethod = String2.ERROR + " in Table.tryToUnpack(" + column + "):\n";
        Test.ensureBetween(unpack, 0, 2, errorInMethod + "unpack");
        if (unpack == 0)
            return; //do nothing
        PrimitiveArray pa = getColumn(column);
        PrimitiveArray scaleFactorPA = columnAttributes(column).remove("scale_factor"); 
        PrimitiveArray addOffsetPA = columnAttributes(column).remove("add_offset");

        //get the scaleFactor
        double scaleFactor = 1;
        if (scaleFactorPA != null) {
            Test.ensureEqual(scaleFactorPA.size(), 1, errorInMethod + "ScaleFactorPA.size");
            scaleFactor = scaleFactorPA.getDouble(0);
        }

        //get addOffset
        double addOffset = 0;
        if (addOffsetPA != null) {
            Test.ensureEqual(addOffsetPA.size(),   1, errorInMethod + "addOffsetPA.size");
            addOffset = addOffsetPA.getDouble(0);
        }

        //if (verbose) String2.log("Table.tryToUnpack col=" + column + " unpack=" + unpack + 
        //    " scaleFactor=" + scaleFactor + " addOffset=" + addOffset);
        if (scaleFactor == 1 && addOffset == 0)
            return; //do nothing

        //unpack
        int n = pa.size();
        PrimitiveArray newPA;
        if (scaleFactorPA instanceof FloatArray && unpack == 1) {
            //unpack as floats
            newPA = new FloatArray(pa);
            columnAttributes(column).set("_FillValue", new FloatArray(new float[]{Float.NaN})); 
            columnAttributes(column).set("missing_value", new FloatArray(new float[]{Float.NaN}));
        } else { 
            //unpack as doubles
            if (scaleFactorPA.elementClass() == float.class)
                scaleFactor = Math2.floatToDouble(scaleFactor);
            if (addOffsetPA.elementClass() == float.class)
                addOffset = Math2.floatToDouble(addOffset);
            newPA = new DoubleArray(pa);
            columnAttributes(column).set("_FillValue", new DoubleArray(new double[]{Double.NaN})); 
            columnAttributes(column).set("missing_value", new DoubleArray(new double[]{Double.NaN}));
        }
        //String2.log("Table.tryToUnPack scale=" + scaleFactor + " offset=" + addOffset); 
        newPA.scaleAddOffset(scaleFactor, addOffset);

        //put newPA in data
        columns.set(column, newPA);

    }

    /**
     * This does the equivalent of a left outer join 
     * (http://en.wikipedia.org/wiki/Join_%28SQL%29#Left_outer_join) 
     * -- by matching a keyColumn(s) 
     * in this table and the first column(s) (the key(s)) in the lookUpTable, 
     * the other columns in the lookUpTable are inserted right after keyColumn(s)
     * in this table with the values from the matching row with the matching key(s).
     *
     * <p>The names and attributes of this table's key columns are not changed.
     *
     * <p>The keys are matched via their string representation, so they can have
     * different elementClass as long as their strings match.
     * E.g., byte, short, int, long, String are usually compatible.
     * Warning: But double = int will probably fail because "1.0" != "1".
     *
     * <p>If the keyCol value(s) isn't found in the lookUpTable, the
     * missing_value attribute (or _FillValue, or "") for the new columns provides the mv.
     *
     * <p>If you don't want to keep the original keyCol, just removeColumn(keyCol) afterwards.
     *
     * <p>If reallyVerbose, this tallies the not matched items and displays the top 50 to String2.log.
     *
     * @param nKeys  1 or more
     * @param keyCol the first key column in this table.
     *   If nKeys&gt;1, the others must immediately follow it.
     * @param mvKey if the found key is "", this key is used (or "" or null if not needed).
     *   E.g., if key="" is found, you might want to treat it as if key="0".
     *   If nKeys&gt;1, the parts are tab separated.
     * @param lookUpTable with its keyCol(s) as column 0+, in the same order
     *   as in this table (but not necessarily with identical columnNames). 
     *   This method won't change the lookUpTable.
     * @return the number of key values in this column which were matched in the lookUpTable
     * @throws RuntimeException if trouble (e.g., nKeys < 1, keyCol is invalid,
     *   or lookUpTable is null
     */
    public int join(int nKeys, int keyCol, String mvKey, Table lookUpTable) {
        if (nKeys < 1) 
            throw new RuntimeException(String2.ERROR + " in Table.join: nKeys=" +
                nKeys + " must be at least 1.");
        if (keyCol < 0 || keyCol + nKeys > nColumns())
            throw new RuntimeException(String2.ERROR + " in Table.join: keyCol=" + 
                keyCol + " is invalid.");
        if (mvKey == null)
            mvKey = "";
        long time = System.currentTimeMillis();
        Tally tally = null;
        if (reallyVerbose)
            tally = new Tally();

        //gather keyPA's
        PrimitiveArray keyPA[]  = new PrimitiveArray[nKeys];
        PrimitiveArray lutKeyPA[] = new PrimitiveArray[nKeys];
        for (int key = 0; key < nKeys; key++) {
            keyPA[key]    = getColumn(keyCol + key);
            lutKeyPA[key] = lookUpTable.getColumn(key);
        }

        //make hashtable of keys->new Integer(row#) in lookUpTable
        //so join is fast with any number of rows in lookUpTable
        int lutNRows = lutKeyPA[0].size();
        HashMap hashMap = new HashMap(Math2.roundToInt(1.4 * lutNRows));
        for (int row = 0; row < lutNRows; row++) {
            StringBuilder sb = new StringBuilder(lutKeyPA[0].getString(row));
            for (int key = 1; key < nKeys; key++) 
                sb.append("\t" + lutKeyPA[key].getString(row));
            hashMap.put(sb.toString(), new Integer(row));
        }
        
        //insert columns to be filled
        int nRows = nRows();
        int lutNCols = lookUpTable.nColumns();
        PrimitiveArray lutPA[] = new PrimitiveArray[lutNCols]; //from the lut
        PrimitiveArray newPA[] = new PrimitiveArray[lutNCols]; //paralleling the lutPA
        String mvString[]      = new String[lutNCols];
        for (int lutCol = nKeys; lutCol < lutNCols; lutCol++) {
            lutPA[lutCol] = lookUpTable.getColumn(lutCol);
            newPA[lutCol] = PrimitiveArray.factory(lutPA[lutCol].elementClass(), nRows, false);
            Attributes lutAtts = lookUpTable.columnAttributes(lutCol);
            addColumn(keyCol + lutCol, 
                lookUpTable.getColumnName(lutCol), 
                newPA[lutCol],
                (Attributes)(lutAtts.clone()));
            mvString[lutCol] = lutAtts.getString("missing_value"); //preferred
            if (mvString[lutCol] == null) {
                mvString[lutCol] = lutAtts.getString("_FillValue"); //second choice
                if (mvString[lutCol] == null) {
                    mvString[lutCol] = ""; //default
                }
            }
        }

        //fill the new columns by matching the looking up the key value
        int nMatched = 0;
        for (int row = 0; row < nRows; row++) {
            StringBuilder sb = new StringBuilder(keyPA[0].getString(row));
            for (int key = 1; key < nKeys; key++) 
                sb.append("\t" + keyPA[key].getString(row));
            String s = sb.toString();
            if (s.length() == nKeys-1) //just tabs separating ""
                s = mvKey;
            Object obj = hashMap.get(s);
            if (obj == null) {
                //add missing values
                if (reallyVerbose) tally.add("NotMatched", s);
                for (int lutCol = nKeys; lutCol < lutNCols; lutCol++) 
                    newPA[lutCol].addString(mvString[lutCol]);
            } else {
                //copy values from lutPA's to newPA's
                nMatched++;
                int fRow = ((Integer)obj).intValue();
                for (int lutCol = nKeys; lutCol < lutNCols; lutCol++) 
                    newPA[lutCol].addFromPA(lutPA[lutCol], fRow);
            }
        }
        if (verbose) String2.log("Table.join(nKeys=" + nKeys + " keyCol=" + keyCol + 
            "=" + getColumnName(keyCol) + " lutInsertCol=" + lookUpTable.getColumnName(nKeys) +
            ") nMatched=" + nMatched + " nNotMatched=" + (nRows - nMatched) + 
            " time=" + (System.currentTimeMillis() - time));
        if (reallyVerbose) 
            String2.log(tally.toString(50));
        return nMatched;
    }
    
    /**
     * This updates the data in this table with better data from otherTable
     * by matching rows based on the values in key columns which are in both tables
     * (like a batch version of SQL's UPDATE http://www.w3schools.com/sql/sql_update.asp).
     * Afterwards, this table will have rows for *all* of the values of the key columns
     * from both tables.  This is very fast and efficient, but may need lots of memory.
     *
     * <p>Values are grabbed from the other table by matching column names,
     * so otherTable's values have precedence. 
     * The columns in the two tables need not be the same, nor in the same order.
     * If otherTable doesn't have a matching column, 
     *   the current value (if any) isn't changed, or the new value will be
     *   the missing_value (or _FillValue) for the column (or "" if none).
     *
     * <p>The names and attributes of this table's columns won't be changed.
     * The initial rows in thisTable will be in the same order. 
     * New rows (for new key values) will be at the end (in their order
     * from otherTable).  
     *
     * <p>The key values are matched via their string representation, so they can have
     * different elementClasses as long as their strings match.
     * E.g., byte, short, int, long, String are usually compatible.
     * Warning: But double = int will probably fail because "1.0" != "1".
     *
     * @param keyNames  these columns must be present in this table and otherTable
     * @param otherTable 
     * @return the number of existing rows that were matched.
     *    The number of new rows = otherTable.nRows() - nMatched.
     * @throws RuntimeException if trouble (e.g., keyCols not found
     *   or lookUpTable is null)
     */
    public int update(String keyNames[], Table otherTable) {
        String msg = String2.ERROR + " in Table.update: ";
        long time = System.currentTimeMillis();
        int nRows = nRows();
        int nOtherRows = otherTable.nRows();
        int nCols = nColumns();
        int nKeyCols = keyNames.length;
        if (nKeyCols < 1) 
            throw new RuntimeException(msg + "nKeys=" +
                nKeyCols + " must be at least 1.");
        int keyCols[]      = new int[nKeyCols];
        int otherKeyCols[] = new int[nKeyCols];
        PrimitiveArray keyPAs[]      = new PrimitiveArray[nKeyCols];
        PrimitiveArray otherKeyPAs[] = new PrimitiveArray[nKeyCols];
        for (int key = 0; key < nKeyCols; key++) {
            keyCols[     key] =            findColumnNumber(keyNames[key]);
            otherKeyCols[key] = otherTable.findColumnNumber(keyNames[key]);
            if (keyCols[key] < 0)
                throw new RuntimeException(msg + "keyName=" + keyNames[key] + 
                    " not found in this table.");
            if (otherKeyCols[key] < 0)
                throw new RuntimeException(msg + "keyName=" + keyNames[key] + 
                    " not found in otherTable.");
            keyPAs[     key] =            getColumn(     keyCols[key]);
            otherKeyPAs[key] = otherTable.getColumn(otherKeyCols[key]);
        }

        //make hashmap of this table's key values to row#
        HashMap rowHash = new HashMap(Math2.roundToInt(1.4 * nRows));
        for (int row = 0; row < nRows; row++) {
            StringBuilder sb = new StringBuilder();
            for (int key = 0; key < nKeyCols; key++) 
                sb.append(keyPAs[key].getString(row) + "\n");
            rowHash.put(sb.toString(), new Integer(row));
        }

        //find columns in otherTable which correspond to the columns in this table
        int otherCols[] = new int[nCols];
        PrimitiveArray otherPAs[] = new PrimitiveArray[nCols];
        String missingValues[] = new String[nCols];
        Arrays.fill(missingValues, "");
        int nColsMatched = 0;
        StringArray colsNotMatched = new StringArray();
        for (int col = 0; col < nCols; col++) {
            otherCols[col] = otherTable.findColumnNumber(getColumnName(col));
            if (otherCols[col] >= 0) {
                nColsMatched++;
                otherPAs[col] = otherTable.getColumn(otherCols[col]);
            } else {
                colsNotMatched.add(getColumnName(col));
            }

            //collect missing values
            Attributes atts = columnAttributes(col);
            String mv = atts.getString("missing_value");
            if (mv == null)
                mv = atts.getString("_FillValue");
            if (mv != null)
                missingValues[col] = mv;
        }

        //go through rows of otherTable
        int nNewRows = 0;
        int nRowsMatched = 0;
        for (int otherRow = 0; otherRow < nOtherRows; otherRow++) {
            //for each, find the matching row in this table (or not)
            StringBuilder sb = new StringBuilder();
            for (int key = 0; key < nKeyCols; key++) 
                sb.append(otherKeyPAs[key].getString(otherRow) + "\n");
            String sbString = sb.toString();
            Object thisRowI = rowHash.get(sbString);
            if (thisRowI == null) {
                //add blank row at end
                nNewRows++;
                rowHash.put(sbString, new Integer(nRows++));
                for (int col = 0; col < nCols; col++) {
                    if (otherPAs[col] == null)
                         getColumn(col).addString(missingValues[col]);
                    else getColumn(col).addFromPA(otherPAs[col], otherRow);
                }
            } else {
                //replace current values
                nRowsMatched++;
                for (int col = 0; col < nCols; col++) {
                    //if otherTable doesn't have matching column, current value isn't changed
                    if (otherPAs[col] != null) 
                        getColumn(col).setFromPA(((Integer)thisRowI).intValue(), 
                            otherPAs[col], otherRow);
                }
            }
        }
        if (verbose) String2.log("Table.update finished." +
            " nColsMatched=" + nColsMatched + " of " + nCols + 
              (nColsMatched == nCols? "" : " (missing: " + colsNotMatched.toString() + ")") +
            ", nRowsMatched=" + nRowsMatched +
            ", nNewRows=" + nNewRows + 
            ", time=" + (System.currentTimeMillis() - time) + "ms");
        return nRowsMatched;
    }
    

    /* *  THIS IS INACTIVE.
     * This reads a NetCDF file with no structure or groups, but with
     * at least one dimension and at least 1 1D array variable that uses that 
     * dimension, and populates the public variables.
     * Suitable files include LAS Intermediate NetCDF files.
     *
     * <p>If there is a time variable with attribute "units"="seconds",
     *   and storing seconds since 1970-01-01T00:00:00Z. 
     *   See [COARDS] "Time or date dimension".
     * <p>The file may have a lat variable with attribute "units"="degrees_north" 
     *   to identify the latitude variable. See [COARDS] "Latitude Dimension".
     *   It can be of any numeric data type.
     * <p>The file may have a lon variable with attribute "units"="degrees_east" 
     *   to identify the longitude variable. See [COARDS] "Longitude Dimension".
     *   It can be of any numeric data type.
     *
     * <p>netcdf files use the ucar.nc2.dods classes are read with code in
     * netcdf-X.X.XX.jar which is part of the
     * <a href="http://www.unidata.ucar.edu/software/netcdf-java/index.htm">NetCDF Java Library</a>
     * from <a href="http://my.unidata.ucar.edu/>Unidata</a> renamed as netcdf-latest.jar.
     * Get slf4j-jdk14.jar from 
     * ftp://ftp.unidata.ucar.edu/pub/netcdf-java/slf4j-jdk14.jar
     * and copy it to <context>/WEB-INF/lib.
     * Put both of these .jar files in the classpath for the compiler and for Java.
     *
     * <p>This sets globalAttributes and columnAttributes.
     *
     * @param fullFileName the full name of the file, for diagnostic messages.
     * @param ncFile an open ncFile
     * @param unpackVariables If 0, no variables will be unpacked and the
     *    tests will be done with unpacked values. If 1 or 2, 
     *    the testColumns will be unpacked before
     *    testing and the resulting loadColumns will be unpacked.
     *    Variables are only unpacked only if a variable has an attribute 
     *    scale_factor!=1 or add_offset!=0.
     *    If unpackVariables == 1, variables are unpacked to floats or doubles,
     *    depending on the data type of scale_factor and add_offset.
     *    If unpackVariables == 2, variables are always unpacked to DoubleArrays.
     * @param okRows indicates which rows should be kept.
     *   This is used as the starting point for the tests (the tests may reject
     *   rows which are initially ok) or can be used without tests. It may be null.
     * @param testColumns the names of the columns to be tested (null = no tests).
     *   All of the test columns must use the same, one, dimension that the
     *   loadColumns use.
     *   Ideally, the first tests will greatly restrict the range of valid rows.
     * @param testMin the minimum allowed value for each testColumn (null = no tests)
     * @param testMax the maximum allowed value for each testColumn (null = no tests)
     * @param loadColumns the names of the columns to be loaded.
     *     They must all be ArrayXxx.D1 or ArrayChar.D2 variables and use the 
     *     same, one, dimension as the first dimension.
     *     If loadColumns is null, this will read all of the variables in the
     *     main group which use the biggest rootGroup dimension as their 
     *     one and only dimension.
     * @throws Exception if trouble
     */
/*    public void readNetCDF(String fullFileName, NetcdfFile ncFile, 
        int unpackVariables, BitSet okRows,
        String testColumns[], double testMin[], double testMax[], 
        String loadColumns[]) throws Exception {

        //if (verbose) String2.log(File2.hexDump(fullFileName, 300));
        if (verbose) String2.log("Table.readNetCDF" +
            "\n  testColumns=" + String2.toCSSVString(testColumns) + 
            "\n  testMin=" + String2.toCSSVString(testMin) + 
            "\n  testMax=" + String2.toCSSVString(testMax) + 
            "\n  loadColumns=" + String2.toCSSVString(loadColumns)); 
        
        //setup
        long time = System.currentTimeMillis();
        clear();
        String errorInMethod = String2.ERROR + " in Table.readNetCDF(" + fullFileName + "):\n"; 

        //*** ncdump  //this is very slow for big files
        //if (verbose) DataHelper.ncDump("Start of Table.readNetCDF", fullFileName, false);

        //read the globalAttributes
        if (verbose) String2.log("  read the globalAttributes");
        globalAttributes = new ArrayList();
        List globalAttList = ncFile.globalAttributes();
        for (int att = 0; att < globalAttList.size(); att++) {
            Attribute gAtt = (Attribute)globalAttList.get(att);
            globalAttributes.add(gAtt.getName());
            globalAttributes.add(PrimitiveArray.factory(
                DataHelper.getArray(gAtt.getValues())));
        }

        //find the mainDimension
        Dimension mainDimension = null;
        if (loadColumns == null) {
            //assume mainDimension is the biggest dimension
            //future: better to look for 1d arrays and find the largest?
            //   Not really, because lat and lon could have same number 
            //   but they are different dimension.
            List dimensions = ncFile.getDimensions(); //next nc version: rootGroup.getDimensions();
            if (dimensions.size() == 0)
                throw new SimpleException(errorInMethod + "the file has no dimensions.");
            mainDimension = (Dimension)dimensions.get(0);
            if (!mainDimension.isUnlimited()) {
                for (int i = 1; i < dimensions.size(); i++) {
                    if (verbose) String2.log("  look for biggest dimension, check " + i);
                    Dimension tDimension = (Dimension)dimensions.get(i);
                    if (tDimension.isUnlimited()) {
                        mainDimension = tDimension;
                        break;
                    }
                    if (tDimension.getLength() > mainDimension.getLength())
                        mainDimension = tDimension;
                }
            }
        } else {
            //if loadColumns was specified, get mainDimension from loadColumns[0]
            if (verbose) String2.log("  get mainDimension from loadColumns[0]");
            Variable v = ncFile.findVariable(loadColumns[0]);
            mainDimension = v.getDimension(0);
        } 


        //make a list of the needed variables (loadColumns and testColumns)
        ArrayList allVariables = new ArrayList();
        if (loadColumns == null) {
            //get a list of all variables which use just mainDimension
            List variableList = ncFile.getVariables();
            for (int i = 0; i < variableList.size(); i++) {
                if (verbose) String2.log("  get all variables which use mainDimension, check " + i);
                Variable tVariable = (Variable)variableList.get(i);
                List tDimensions = tVariable.getDimensions();
                int nDimensions = tDimensions.size();
                if (verbose) String2.log("i=" + i + " name=" + tVariable.getName() + 
                    " type=" + tVariable.getDataType());
                if ((nDimensions == 1 && tDimensions.get(0).equals(mainDimension)) ||
                    (nDimensions == 2 && tDimensions.get(0).equals(mainDimension) && 
                         tVariable.getDataType() == DataType.CHAR)) {
                        allVariables.add(tVariable);
                }
            }
        } else {
            //make the list from the loadColumns and testColumns
            for (int i = 0; i < loadColumns.length; i++) {
                if (verbose) String2.log("  getLoadColumns " + i);
                allVariables.add(ncFile.findVariable(loadColumns[i]));
            }
            if (testColumns != null) {
                for (int i = 0; i < testColumns.length; i++) {
                    if (String2.indexOf(loadColumns, testColumns[i]) < 0) {
                        if (verbose) String2.log("  getTestColumns " + i);
                        allVariables.add(ncFile.findVariable(testColumns[i]));
                    }
                }
            }
        }
        if (verbose) String2.log("  got AllVariables " + allVariables.size());

        //get the data
        getNetcdfSubset(errorInMethod, allVariables, unpackVariables, okRows,
            testColumns, testMin, testMax, loadColumns);

        if (verbose)
            String2.log("Table.readNetCDF nColumns=" + nColumns() +
                " nRows=" + nRows() + " time=" + (System.currentTimeMillis() - time));
     
    }


    /**  THIS IS NOT YET FINISHED.
     * This reads all rows of all of the specified columns from an opendap
     * dataset.
     * This also reads global and variable attributes.
     * The data is always unpacked.
     *
     * <p>If the fullName is an http address, the name needs to start with "http:\\" 
     * (upper or lower case) and the server needs to support "byte ranges"
     * (see ucar.nc2.NetcdfFile documentation).
     * 
     * @param fullName This may be a local file name, an "http:" address of a
     *    .nc file, or an opendap url.
     * @param loadColumns if null, this searches for the (pseudo)structure variables
     * @throws Exception if trouble
     */
    public void readOpendap(String fullName, String loadColumns[]) throws Exception {

        //get information
        if (verbose) String2.log("Table.readOpendap " + fullName); 
        long time = System.currentTimeMillis();
        NetcdfFile netcdfFile = NcHelper.openFile(fullName);
        try {
            Variable loadVariables[] = NcHelper.findVariables(netcdfFile, loadColumns);

            //fill the table
            clear();
            appendNcRows(loadVariables, 0, -1);
            NcHelper.getGlobalAttributes(netcdfFile, globalAttributes());
            for (int col = 0; col < loadVariables.length; col++)
                NcHelper.getVariableAttributes(loadVariables[col], columnAttributes(col));

            //I care about this exception
            netcdfFile.close();

        } catch (Exception e) {
            try {
                netcdfFile.close(); //make sure it is explicitly closed
            } catch (Exception e2) {
                //don't care
            }
            throw e;
        }
        if (verbose) String2.log("  Table.readOpendap done. nColumns=" + nColumns() +
            " nRows=" + nRows() + " TIME=" + (System.currentTimeMillis() - time));

    }
    

    /**
     * For compatibility with older programs, this calls readOpendapSequence(url, false);
     * @param url  the url, already SSR.percentEncoded as needed
     */
    public void readOpendapSequence(String url) throws Exception {
        readOpendapSequence(url, false);
    }

    /**
     * This populates the table from an opendap one level or two-level (Dapper-style) sequence response.
     * See Opendap info: http://www.opendap.org/pdf/dap_2_data_model.pdf .
     * See Dapper Conventions: http://www.epic.noaa.gov/epic/software/dapper/dapperdocs/conventions/ .
     *
     * <p>A typical dapper-style two-level nested sequence is:
     * <pre>
      Dataset {
          Sequence {
              Float32 lat;
              Float64 time;
              Float32 lon;
              Int32 _id;
              Sequence {
                  Float32 depth;
                  Float32 temp;
                  Float32 salinity;
                  Float32 pressure;
              } profile;
          } location;
      } northAtlantic;
     * </pre>
     * The resulting flat table created by this method has a column
     * for each variable (lat, time, lon, _id, depth, temp, salinity, pressure)
     * and a row for each measurement. 
     *
     * <p>Following the JDAP getValue return type, DUInt16 is converted to a Java short 
     * and DUInt32 is converted to a Java int.
     * 
     * <p>!!! Unlike the DAPPER convention, this method only supports one inner sequence;
     * otherwise, the response couldn't be represented as a simple table.
     *
     * <p>!!! Two-level sequences from DAPPER always seems to have 
     * data from different inner sequences separated by a row of NaNs 
     * (in the inner sequence variables)
     * to mark the end of the upper inner sequence's info.
     * I believe Dapper is doing this to mark the transition from one 
     * inner sequence to the next.
     *
     * <p>!!! Dapper doesn't return variables in the order that you requested 
     * them. It is unclear (to me) what determines the order.
     * 
     * <p>Remember that Dapper doesn't support constraints on non-axis 
     * variables! 
     *
     * <p>This clears the table before starting.
     * This sets the global and data attributes (see javadocs for DataHelper.getAttributesFromOpendap).
     *
     * <p>!!!I don't know if the opendap server auto-unpacks (scale, offset) the variables.
     * I have no test cases. This method just passes through what it gets.
     *
     * @param url This may include a constraint expression at the end
     *    e.g., ?latitude,longitude,time,WTMP&time>=1124463600.
     *    The url (pre "?") should have no extension, e.g., .dods, at the end.
     *    The query should already be SSR.percentEncoded as needed.
     * @param skipDapperSpacerRows if true, this skips the last row of each 
     *     innerSequence other than the last innerSequence (because Dapper
     *     puts NaNs in the row to act as a spacer).
     * @throws Exception if trouble
     */
    public void readOpendapSequence(String url, boolean skipDapperSpacerRows) throws Exception {

        if (verbose) String2.log("Table.readOpendapSequence url=\n" + url);
        String errorInMethod = String2.ERROR + " in Table.readOpendapSequence(" + url + "):\n";
        long time = System.currentTimeMillis();
        clear();
        DConnect dConnect = new DConnect(url, opendapAcceptDeflate, 1, 1);
        DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
        OpendapHelper.getAttributes(das, "GLOBAL", globalAttributes());

        //get the outerSequence information
        DataDDS dataDds = dConnect.getData(null); //null = no statusUI
        if (verbose)
            String2.log("  dConnect.getData time=" + (System.currentTimeMillis() - time));
        BaseType firstVariable = (BaseType)dataDds.getVariables().nextElement();
        if (!(firstVariable instanceof DSequence)) 
            throw new Exception(errorInMethod + "firstVariable not a DSequence: name=" + 
                firstVariable.getName() + " type=" + firstVariable.getTypeName());
        DSequence outerSequence = (DSequence)firstVariable;
        int nOuterRows = outerSequence.getRowCount();
        int nOuterColumns = outerSequence.elementCount();
        AttributeTable outerAttributeTable = das.getAttributeTable(
            outerSequence.getLongName()); //I think getLongName == getName() here
        //String2.log("outerAttributeTable=" + outerAttributeTable);

        //create the columns
        int innerSequenceColumn = -1; //the outerCol with the inner sequence (or -1 if none)
        int nInnerColumns = 0;  //0 important if no innerSequenceColumn
        for (int outerCol = 0; outerCol < nOuterColumns; outerCol++) {
            //create the columns
            BaseType obt = outerSequence.getVar(outerCol); //this doesn't have data, just description of obt
            if      (obt instanceof DByte)    addColumn(obt.getName(), new ByteArray());
            else if (obt instanceof DFloat32) addColumn(obt.getName(), new FloatArray());
            else if (obt instanceof DFloat64) addColumn(obt.getName(), new DoubleArray());
            else if (obt instanceof DInt16)   addColumn(obt.getName(), new ShortArray());
            else if (obt instanceof DUInt16)  addColumn(obt.getName(), new ShortArray());
            else if (obt instanceof DInt32)   addColumn(obt.getName(), new IntArray());
            else if (obt instanceof DUInt32)  addColumn(obt.getName(), new IntArray());
            else if (obt instanceof DBoolean) addColumn(obt.getName(), new ByteArray()); //.nc doesn't support booleans, so store byte=0|1
            else if (obt instanceof DString)  addColumn(obt.getName(), new StringArray());
            else if (obt instanceof DSequence) {
                //*** Start Dealing With InnerSequence
                //Ensure this is the first innerSequence.
                //If there are two, the response can't be represented as a simple table.
                if (innerSequenceColumn != -1)
                    throw new Exception(errorInMethod + 
                        "The response has more than one inner sequence: " +
                        getColumnName(innerSequenceColumn) + " and " + obt.getName() + ".");
                innerSequenceColumn = outerCol;
                if (verbose) String2.log("  innerSequenceColumn=" + innerSequenceColumn);

                //deal with the inner sequence
                DSequence innerSequence = (DSequence)obt;
                nInnerColumns = innerSequence.elementCount();
                AttributeTable innerAttributeTable = das.getAttributeTable(innerSequence.getName());
                //String2.log("innerAttributeTable=" + innerAttributeTable);
                for (int innerCol = 0; innerCol < nInnerColumns; innerCol++) {

                    //create the columns
                    BaseType ibt = innerSequence.getVar(innerCol); //this doesn't have data, just description of ibt
                    if      (ibt instanceof DByte)    addColumn(ibt.getName(), new ByteArray());
                    else if (ibt instanceof DFloat32) addColumn(ibt.getName(), new FloatArray());
                    else if (ibt instanceof DFloat64) addColumn(ibt.getName(), new DoubleArray());
                    else if (ibt instanceof DInt16)   addColumn(ibt.getName(), new ShortArray());
                    else if (ibt instanceof DUInt16)  addColumn(ibt.getName(), new ShortArray());
                    else if (ibt instanceof DInt32)   addColumn(ibt.getName(), new IntArray());
                    else if (ibt instanceof DUInt32)  addColumn(ibt.getName(), new IntArray());
                    else if (ibt instanceof DBoolean) addColumn(ibt.getName(), new ByteArray()); //.nc doesn't support booleans, so store byte=0|1
                    else if (ibt instanceof DString)  addColumn(ibt.getName(), new StringArray());
                    else throw new Exception(errorInMethod + "Unexpected inner variable type=" + 
                        ibt.getTypeName() + " for name=" + ibt.getName());

                    //get the ibt attributes  
                    //(some servers return innerAttributeTable, some don't -- see test cases)
                    if (innerAttributeTable == null) {
                        //Dapper needs this approach
                        //note use of getLongName here
                        Attributes tAtt = columnAttributes(nColumns() - 1);
                        OpendapHelper.getAttributes(das, ibt.getLongName(), tAtt);
                        if (tAtt.size() == 0)
                            OpendapHelper.getAttributes(das, ibt.getName(), tAtt);
                    } else {
                        //note use of getName in this section
                        int tCol = nColumns() - 1; //the table column just created
                        //String2.log("try getting attributes for inner " + getColumnName(col));
                        dods.dap.Attribute attribute = innerAttributeTable.getAttribute(ibt.getName());
                        //it should be a container with the attributes for this column
                        if (attribute == null) {
                            String2.log(errorInMethod + "Unexpected: no attribute for innerVar=" + 
                                ibt.getName() + ".");
                        } else if (attribute.isContainer()) { 
                            OpendapHelper.getAttributes(attribute.getContainer(), 
                                columnAttributes(tCol));
                        } else {
                            String2.log(errorInMethod + "Unexpected: attribute for innerVar=" + 
                                ibt.getName() + " not a container: " + 
                                attribute.getName() + "=" + attribute.getValueAt(0));
                        }
                    }
                }
                //*** End Dealing With InnerSequence

            } else throw new Exception(errorInMethod + "Unexpected outer variable type=" + 
                obt.getTypeName() + " for name=" + obt.getName());

            //get the obt attributes 
            //(some servers return outerAttributeTable, some don't -- see test cases)
            if (obt instanceof DSequence) {
                //it is the innerSequence, so attributes already read
            } else if (outerAttributeTable == null) {
                //Dapper needs this approach
                //note use of getLongName here
                Attributes tAtt = columnAttributes(nColumns() - 1);
                OpendapHelper.getAttributes(das, obt.getLongName(), tAtt);
                //drds needs this approach
                if (tAtt.size() == 0)
                    OpendapHelper.getAttributes(das, obt.getName(), tAtt);
            } else {            
                //note use of getName in this section
                int tCol = nColumns() - 1; //the table column just created
                //String2.log("try getting attributes for outer " + getColumnName(col));
                dods.dap.Attribute attribute = outerAttributeTable.getAttribute(obt.getName());
                //it should be a container with the attributes for this column
                if (attribute == null) {
                    String2.log(errorInMethod + 
                        "Unexpected: no attribute for outerVar=" + obt.getName() + ".");
                } else if (attribute.isContainer()) { 
                    OpendapHelper.getAttributes(attribute.getContainer(), 
                        columnAttributes(tCol));
                } else {
                    String2.log(errorInMethod + "Unexpected: attribute for outerVar=" + 
                        obt.getName() + " not a container: " + 
                        attribute.getName() + "=" + attribute.getValueAt(0));
                }
            }
        }
        //if (reallyVerbose) String2.log("  columns were created.");

        //Don't ensure that an innerSequence was found
        //so that this method can be used for 1 or 2 level sequences.
        //String2.log("nOuterRows=" + nOuterRows);
        //String2.log(toString());

        //*** read the data (row-by-row, as it wants)
        for (int outerRow = 0; outerRow < nOuterRows; outerRow++) {
            Vector outerVector = outerSequence.getRow(outerRow);
            int col;

            //get data from innerSequence first (so nInnerRows is known)
            int nInnerRows = 1; //1 is important if no innerSequence
            if (innerSequenceColumn >= 0) {
                DSequence innerSequence = (DSequence)outerVector.get(innerSequenceColumn);
                nInnerRows = innerSequence.getRowCount();
                if (skipDapperSpacerRows && outerRow < nOuterRows -1)
                    nInnerRows--;
                //if (verbose) String2.log("  nInnerRows=" + nInnerRows + " nInnerCols=" + nInnerColumns);
                Test.ensureEqual(nInnerColumns, innerSequence.elementCount(),
                    errorInMethod + "Unexpected nInnerColumns for outer row #" + outerRow);
                col = innerSequenceColumn;
                for (int innerRow = 0; innerRow < nInnerRows; innerRow++) {
                    Vector innerVector = innerSequence.getRow(innerRow);
                    for (int innerCol = 0; innerCol < nInnerColumns; innerCol++) {
                        //if (reallyVerbose) String2.log("  OR=" + outerRow + " OC=" + col + " IR=" + innerRow + " IC=" + innerCol);
                        BaseType ibt = (BaseType)innerVector.get(innerCol); 
                        if      (ibt instanceof DByte)    (  (ByteArray)columns.get(col + innerCol)).add(((DByte)ibt).getValue());
                        else if (ibt instanceof DFloat32) ( (FloatArray)columns.get(col + innerCol)).add(((DFloat32)ibt).getValue());
                        else if (ibt instanceof DFloat64) ((DoubleArray)columns.get(col + innerCol)).add(((DFloat64)ibt).getValue());
                        else if (ibt instanceof DInt16)   ( (ShortArray)columns.get(col + innerCol)).add(((DInt16)ibt).getValue());
                        else if (ibt instanceof DUInt16)  ( (ShortArray)columns.get(col + innerCol)).add(((DUInt16)ibt).getValue());
                        else if (ibt instanceof DInt32)   (   (IntArray)columns.get(col + innerCol)).add(((DInt32)ibt).getValue());
                        else if (ibt instanceof DUInt32)  (   (IntArray)columns.get(col + innerCol)).add(((DUInt32)ibt).getValue());
                        else if (ibt instanceof DBoolean) (  (ByteArray)columns.get(col + innerCol)).add((byte)(((DBoolean)ibt).getValue()? 1 : 0)); //.nc doesn't support booleans, so store byte=0|1
                        else if (ibt instanceof DString)  ((StringArray)columns.get(col + innerCol)).add(((DString)ibt).getValue());
                        else throw new Exception(errorInMethod + "Unexpected inner variable type=" + 
                            ibt.getTypeName() + " for name=" + ibt.getName());
                    }
                }
            }

            //process the other outerCol
            //if (reallyVerbose) String2.log("  process the other outer col");
            col = 0; //restart at 0
            for (int outerCol = 0; outerCol < nOuterColumns; outerCol++) {
                //innerSequenceColumn already processed above
                if (outerCol == innerSequenceColumn) {
                    col += nInnerColumns;
                    continue; 
                }

                //note addN (not add)
                //I tried storing type of column to avoid instanceof, but no faster.
                BaseType obt = (BaseType)outerVector.get(outerCol);
                if      (obt instanceof DByte)    (  (ByteArray)columns.get(col++)).addN(nInnerRows, ((DByte)obt).getValue());
                else if (obt instanceof DFloat32) ( (FloatArray)columns.get(col++)).addN(nInnerRows, ((DFloat32)obt).getValue());
                else if (obt instanceof DFloat64) ((DoubleArray)columns.get(col++)).addN(nInnerRows, ((DFloat64)obt).getValue());
                else if (obt instanceof DInt16)   ( (ShortArray)columns.get(col++)).addN(nInnerRows, ((DInt16)obt).getValue());
                else if (obt instanceof DUInt16)  ( (ShortArray)columns.get(col++)).addN(nInnerRows, ((DUInt16)obt).getValue());
                else if (obt instanceof DInt32)   (   (IntArray)columns.get(col++)).addN(nInnerRows, ((DInt32)obt).getValue());
                else if (obt instanceof DUInt32)  (   (IntArray)columns.get(col++)).addN(nInnerRows, ((DUInt32)obt).getValue());
                else if (obt instanceof DBoolean) (  (ByteArray)columns.get(col++)).addN(nInnerRows, (byte)(((DBoolean)obt).getValue()? 1 : 0)); //.nc doesn't support booleans, so store byte=0|1
                else if (obt instanceof DString)  ((StringArray)columns.get(col++)).addN(nInnerRows, ((DString)obt).getValue());
                else throw new Exception(errorInMethod + "Unexpected outer variable type=" +
                    obt.getTypeName() + " for name=" + obt.getName());
            }
        }

        //since addition of data to different columns is helter-skelter,
        //ensure resulting table is valid (it's a fast test)
        ensureValid(); //throws Exception if not

        if (verbose) String2.log("  Table.readOpendapSequence done. nColumns=" + nColumns() +
            " nRows=" + nRows() + " TOTAL TIME=" + (System.currentTimeMillis() - time));
    }

    /**
     * This populates the table from an opendap sequence.
     * The test information and loadColumns will be used generate
     * a new url with a constraint expression. Opendap sequences
     * support constraint expressions; many other opendap datasets do not.
     * The testColumns and loadColumns must be in the same sequence.
     *
     * <p>!!!I don't know if this auto-unpacks (scale, offset) the variables.
     * I have no test cases.
     *
     * @param url The url may not include a constraint expression,
     *    since them method generates one to do the tests
     * @param testColumns the names of the columns to be tested. 
     *   If testColumns is null, no tests will be done.
     * @param testMin the minimum allowed value for each testColumn.
     *    Ignored if testColumns is null.
     * @param testMax the maximum allowed value for each testColumn.
     *    Ignored if testColumns is null.
     * @param loadColumns The columns to be downloaded for the rows were
     *    the test result is 'true'. If null, all columns will be loaded.
     * @param skipDapperSpacerRows if true, this skips the last row of each 
     *     innerSequence (other than the last innerSequence (because Dapper
     *     puts NaNs in the row to act as a spacer).
     * @throws Exception if trouble
     */
    public void readOpendapSequence(String url, 
        String testColumns[], double testMin[], double testMax[],
        String loadColumns[], boolean skipDapperSpacerRows) throws Exception {

        if (verbose) String2.log("Table.readOpendapSequence with restrictions...");
        StringBuilder qSB = new StringBuilder();
        if (loadColumns != null && loadColumns.length > 0) {
            qSB.append(loadColumns[0]);
            for (int col = 1; col < loadColumns.length; col++) 
                qSB.append("," + loadColumns[col]);
        }
        if (testColumns != null) {
            for (int col = 0; col < testColumns.length; col++) {
                qSB.append("&" + testColumns[col] + ">=" + testMin[col]);
                qSB.append("&" + testColumns[col] + "<=" + testMax[col]);
            }
        }
        readOpendapSequence(url + "?" + qSB.toString(), skipDapperSpacerRows);
    }


    /**
     * This populates the table from an opendap response.
     *
     * @param url This may include a constraint expression
     *    e.g., ?latitude,longitude,time,WTMP&time>==1124463600
     * @param loadColumns The columns from the response to be saved in the
     *    table (use null for have the methods search for all variables
     *    in a (pseudo)structure). These columns are not appended to the
     *    url.
     * @throws Exception if trouble
     */
/*    public void readOpendap(String url, String loadColumns[]) throws Exception {

        //get information
        NetcdfFile netcdfFile = NetcdfDataset.openDataset(url);
        try {
            List loadVariables = findNcVariables(netcdfFile, loadColumns);

            //fill the table
            clear();
            globalAttributes = getNcGlobalAttributes(netcdfFile);
            columnAttributes = getNcVariableAttributes(loadVariables);
            appendNcRows(loadVariables, 0, -1);
        } finally {
            try {
                netcdfFile.close(); //make sure it is explicitly closed
            } catch (Exception e) {
            }
        }

    }
*/


    /**
     * This forces the values in lonAr to be +/-180 or 0..360.
     * THIS ONLY WORKS IF MINLON AND MAXLON ARE BOTH WESTERN OR EASTERN HEMISPHERE.
     *
     * @param lonArray 
     * @param pm180 If true, lon values are forced to be +/-180. 
     *     If false, lon values are forced to be 0..360.
     */
    public static void forceLonPM180(PrimitiveArray lonArray, boolean pm180) {
        double stats[] = lonArray.calculateStats();
        int nRows = lonArray.size();
        String2.log("forceLon stats=" + String2.toCSSVString(stats));
        if (pm180 && stats[PrimitiveArray.STATS_MAX] > 180) {
            String2.log("  force >");
            for (int row = 0; row < nRows; row++) {
                lonArray.setDouble(row, Math2.looserAnglePM180(lonArray.getDouble(row))); 
            }
        } else if (!pm180 && stats[PrimitiveArray.STATS_MIN] < 0) {
            String2.log("  force <");
            for (int row = 0; row < nRows; row++) {
                lonArray.setDouble(row, Math2.looserAngle0360(lonArray.getDouble(row)));
            }
        }
    }


    /**
     * Make a subset of this data by retaining only the rows which 
     * pass the test.
     * For each column number in testColumns, there is a corresponding
     * min and max.
     * Rows of data will be kept if, for the value in each testColumns,
     * the value isn't NaN and is &gt;= min  and &lt;= max.
     * This doesn't touch xxxAttributes.
     * 
     * @param testColumnNumbers the column numbers to be tested
     * @param min the corresponding min allowed value
     * @param max the corresponding max allowed value
     */
    public void subset(int testColumnNumbers[], double min[], double max[]) {

        int nRows = nRows();
        int nColumns = nColumns();
        int nTestColumns = testColumnNumbers.length;
        PrimitiveArray testColumns[] = new PrimitiveArray[nTestColumns];
        for (int col = 0; col < nTestColumns; col++) {
            testColumns[col] = getColumn(testColumnNumbers[col]);
        }

        //go through the data looking for valid rows
        //copy successes to position nGood
        int nGood = 0;
        for (int row = 0; row < nRows; row++) {
            boolean ok = true;
            for (int testCol = 0; testCol < nTestColumns; testCol++) {
                double d = testColumns[testCol].getDouble(row);
                if (Math2.isFinite(d) && d >= min[testCol] && d <= max[testCol]) {
                } else {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                for (int col = 0; col < nColumns; col++)
                    getColumn(col).copy(row, nGood);
                nGood++;
            }
        }

        //remove excess at end of column
        for (int col = 0; col < nColumns; col++)
            getColumn(col).removeRange(nGood, getColumn(col).size());
    }

    /**
     * This tries to clear bits of keep (each representing a row), based on a constraint.
     * The table won't be changed.
     *
     * @param idCol  For reallyVerbose only: rejected rows will log the value 
     *    in this column (e.g., stationID) (or row= if idCol < 0). 
     * @param conVar perhaps a column name in the table.   If not, this constraint is ignored.
     *    This correctly deals with float, double, and other data types.
     * @param conOp    an EDDTable-operator, e.g., "="
     * @param conVal  
     * @param keep   only the rows with set bits are tested.
     *     If a test fails, the bit for that row is cleared.
     * @return keep.cardinality()
     */
    public int tryToApplyConstraint(int idCol,
        String conVar, String conOp, String conVal, BitSet keep) {

        return lowApplyConstraint(false, idCol, conVar, conOp, conVal, keep);
    }


    /**
     * This is like tryToApplyConstraint, but requires that the constraintVariable exist
     * in the table.
     *
     * @return keep.cardinality()
     */
    public int applyConstraint(int idCol,
        String conVar, String conOp, String conVal, BitSet keep) {

        return lowApplyConstraint(true, idCol, conVar, conOp, conVal, keep);
    }

    /** The low level method for tryToApplyConstraint and applyConstraint */
    public int lowApplyConstraint(boolean requireConVar, int idCol,
        String conVar, String conOp, String conVal, BitSet keep) {

        int row = keep.nextSetBit(0);
        if (row < 0)
            return 0;

        if (debug) String2.log("\ntryToApplyConstraint " + conVar + conOp + conVal);

        //PrimitiveArray idPa = idCol >= 0? getColumn(idCol) : null;

        //is conVar in the table?
        int conVarCol = findColumnNumber(conVar);
        if (conVarCol < 0) {
            String msg = "Table.applyConstraint: constraintVariable=" + conVar + 
                         " isn't in the table.";
            if (requireConVar)
                throw new RuntimeException(msg);
            if (reallyVerbose) String2.log(msg);
            return keep.cardinality(); //unfortunate that time is perhaps wasted to calculate this
        }

        //test the keep=true rows for this constraint
        PrimitiveArray conPa = getColumn(conVarCol);
        int nKeep = conPa.applyConstraint(keep, conOp, conVal);

        if (reallyVerbose || debug) 
            String2.log("  applyConstraint: after " + conVar + conOp + "\"" + conVal + "\", " +
                nKeep + " rows remain");
        return nKeep;
    }


    /**
     * This is like ApplyConstraint, but actually removes the rows that don't match
     * the constraint.
     *
     * @return table.nRows()
     */
    public int oneStepApplyConstraint(int idCol,
        String conVar, String conOp, String conVal) {

        BitSet keep = new BitSet();
        keep.set(0, nRows());
        lowApplyConstraint(true, idCol, conVar, conOp, conVal, keep);
        justKeep(keep);
        return nRows();
    }




    /**
     * Make a subset of this data by retaining only the keep(row)==true rows.
     * 
     * @param keep
     */
    public void justKeep(BitSet keep) {
        int nColumns = nColumns();
        for (int col = 0; col < nColumns; col++)
            getColumn(col).justKeep(keep);
    }

    /**
     * If two or more adjacent rows are duplicates, this removes
     * the duplicates (leaving the original row).
     * Presumably, the file is sorted in a way to make identical rows adjacent.
     *
     * @return the number of duplicates removed
     */
    public int removeDuplicates() {
        return PrimitiveArray.removeDuplicates(columns);
    }

    /**
     * This moves the specified columns into place and removes other columns.
     *
     * @param colNames
     * @param extraErrorMessage (may be "")
     * @throws IllegalArgumentException if a colName not found.
     */
    public void justKeepColumns(String colNames[], String extraErrorMessage) {
        for (int col = 0; col < colNames.length; col++) {
            int tCol = findColumnNumber(colNames[col]); 
            if (tCol < 0)
                throw new IllegalArgumentException("column name=" + colNames[col] + " wasn't found." +
                    extraErrorMessage);
            moveColumn(tCol, col);
        }

        removeColumns(colNames.length, nColumns());
    }

    /**
     * This moves the specified columns into place and removes other columns.
     * This variant creates columns (with missing values) if they didn't exist.
     *
     * @param colNames  the desired columnNames
     * @param classes the class for each desired columnName
     */
    public void justKeepColumns(String colNames[], Class classes[]) {
        for (int col = 0; col < colNames.length; col++) {
            int tCol = findColumnNumber(colNames[col]); 
            if (tCol >= 0) {
                moveColumn(tCol, col);
                setColumn(col, PrimitiveArray.factory(classes[tCol], getColumn(col)));
            } else {
                addColumn(col, colNames[col], 
                    PrimitiveArray.factory(classes[col], nRows(), ""),
                    new Attributes());
            }
        }
        int tnCols = nColumns();
        if (tnCols > colNames.length) {
            if (verbose) {
                StringArray sa = new StringArray();
                for (int col = colNames.length; col < tnCols; col++)
                    sa.add(getColumnName(col));
                String2.log("Table.justKeepColumns removing excess columns: " + 
                    sa.toString());
            }
            removeColumns(colNames.length, tnCols);
        }
    }



    /**
     * This removes rows in which the value in 'column' is less than
     * the value in the previous row.
     * Rows with values of NaN or bigger than 1e300 are also removed.
     * !!!Trouble: one erroneous big value will cause all subsequent valid values to be tossed.
     *
     * @param column the column which should be ascending
     * @return the number of rows removed
     */
    public int ensureAscending(int column) {
        return PrimitiveArray.ensureAscending(columns, column);
    }

    /**
     * The adds the data from each column in other to the end of each column
     * in this.
     * If old column is simpler than new column, old column is upgraded.
     * This column's metadata is unchanged.
     *
     * @param other another table with columns with the same meanings as this table
     */
    public void append(Table other) {
        int n = Math.min(nColumns(), other.nColumns());
        for (int col = 0; col < n; col++) {

            //beware current column is simpler than new column
            if (      getColumn(col).elementClassIndex() <
                other.getColumn(col).elementClassIndex()) {
                PrimitiveArray newPA = PrimitiveArray.factory(
                    other.getColumn(col).elementClass(), 1, false);
                newPA.append(getColumn(col));
                setColumn(col, newPA);
            }
   
            //append it
            getColumn(col).append(other.getColumn(col));
        }
    }

    /**
     * This ranks the rows of data in the table by some key columns
     * (each of which can be sorted ascending or descending).
     *
     * @param keyColumns the numbers of the key columns (first is most important)
     * @param ascending try if a given key column should be sorted ascending
     * @return an int[] with values (0 ... size-1) 
     *   which points to the row number for a row with a specific 
     *   rank (e.g., result[0].intValue() is the row number of the first item 
     *   in the sorted list, result[1].intValue() is the row number of the
     *   second item in the sorted list, ...).
     */
    public int[] rank(int keyColumns[], boolean ascending[]) {
        return PrimitiveArray.rank(columns, keyColumns, ascending);
    }

    /**
     * Like rank, but StringArrays are ranked in a case-insensitive way.
     *
     * @param keyColumns the numbers of the key columns (first is most important)
     * @param ascending try if a given key column should be ranked ascending
     */
    public int[] rankIgnoreCase(int keyColumns[], boolean ascending[]) {
        return PrimitiveArray.rankIgnoreCase(columns, keyColumns, ascending);
    }

    /**
     * This sorts the rows of data in the table by some key columns
     * (each of which can be sorted ascending or descending).
     *
     * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
     *
     * @param keyColumns the numbers of the key columns (first is most important)
     * @param ascending try if a given key column should be sorted ascending
     */
    public void sort(int keyColumns[], boolean ascending[]) {
        PrimitiveArray.sort(columns, keyColumns, ascending);
    }

    public void sort(String keyNames[], boolean ascending[]) {
        int keyColumns[] = new int[keyNames.length];
        for (int k = 0; k < keyNames.length; k++)
            keyColumns[k] = findColumnNumber(keyNames[k]);
        PrimitiveArray.sort(columns, keyColumns, ascending);
    }

    /**
     * Like sort, but StringArrays are sorted in a case-insensitive way.
     *
     * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
     *
     * @param keyColumns the numbers of the key columns (first is most important)
     * @param ascending try if a given key column should be sorted ascending
     */
    public void sortIgnoreCase(int keyColumns[], boolean ascending[]) {
        PrimitiveArray.sortIgnoreCase(columns, keyColumns, ascending);
    }

    /** Like sortIgnoreCase, but based on key column's names. */
    public void sortIgnoreCase(String keyNames[], boolean ascending[]) {
        int keyColumns[] = new int[keyNames.length];
        for (int k = 0; k < keyNames.length; k++)
            keyColumns[k] = findColumnNumber(keyNames[k]);
        PrimitiveArray.sortIgnoreCase(columns, keyColumns, ascending);
    }

    /**
     * This sorts the rows of data in the table by some key columns
     * (each of which is sorted ascending).
     *
     * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
     *
     * @param keyColumns the numbers of the key columns (first is most important)
     */
    public void ascendingSort(int keyColumns[]) {
        boolean ascending[] = new boolean[keyColumns.length];
        Arrays.fill(ascending, true);
        PrimitiveArray.sort(columns, keyColumns, ascending);
    }

    /** Like ascendingSort, but based on key column's names. */
    public void ascendingSort(String keyNames[]) {
        int keyColumns[] = new int[keyNames.length];
        for (int k = 0; k < keyNames.length; k++)
            keyColumns[k] = findColumnNumber(keyNames[k]);
        boolean ascending[] = new boolean[keyColumns.length];
        Arrays.fill(ascending, true);
        PrimitiveArray.sort(columns, keyColumns, ascending);
    }

    /**
     * Like ascendingSort, but StringArrays are sorted in a case-insensitive way.
     *
     * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
     *
     * @param keyColumns the numbers of the key columns (first is most important)
     */
    public void ascendingSortIgnoreCase(int keyColumns[]) {
        boolean ascending[] = new boolean[keyColumns.length];
        Arrays.fill(ascending, true);
        PrimitiveArray.sortIgnoreCase(columns, keyColumns, ascending);
    }

    /** Like ascendingSortIgnoreCase, but based on key column's names. */
    public void ascendingSortIgnoreCase(String keyNames[]) {
        int keyColumns[] = new int[keyNames.length];
        for (int k = 0; k < keyNames.length; k++)
            keyColumns[k] = findColumnNumber(keyNames[k]);
        boolean ascending[] = new boolean[keyColumns.length];
        Arrays.fill(ascending, true);
        PrimitiveArray.sortIgnoreCase(columns, keyColumns, ascending);
    }

    /**
     * This sorts based on the leftmost nSortColumns (leftmost is most important, all ascending).
     *
     * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
     *
     * @param nSortColumns (leftmost is most important)
     */
    public void leftToRightSort(int nSortColumns) {
        int keyColumns[] = (new IntArray(0, nSortColumns-1)).toArray();
        boolean ascending[] = new boolean[nSortColumns];
        Arrays.fill(ascending, true);
        PrimitiveArray.sort(columns, keyColumns, ascending);
    }

    /**
     * Like leftToRightSort, but StringArrays are sorted in a case-insensitive way.
     *
     * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
     *
     * @param nSortColumns (leftmost is most important)
     */
    public void leftToRightSortIgnoreCase(int nSortColumns) {
        int keyColumns[] = (new IntArray(0, nSortColumns-1)).toArray();
        boolean ascending[] = new boolean[nSortColumns];
        Arrays.fill(ascending, true);
        PrimitiveArray.sortIgnoreCase(columns, keyColumns, ascending);
    }

    /**
     * This sorts the table by the sortColumns. Then, it replaces
     * rows where the values in the sortColumns are equal, by
     * one row with their average.
     * If the number of rows is reduced to 1/2 or less, this
     * calls trimToSize on each of the PrimitiveArrays.
     * If there are no non-NaN values to average, the average is NaN.
     * 
     * @param keyColumns the numbers of the key columns (first is most important)
     */
    public void average(int keyColumns[]) {
        int nRows = nRows();
        if (nRows == 0)
            return;

        //sort
        int nSortColumns = keyColumns.length;
        boolean ascending[] = new boolean[nSortColumns];
        Arrays.fill(ascending, true);
        PrimitiveArray.sort(columns, keyColumns, ascending);

        averageAdjacentRows(keyColumns);
    }


    /**
     * This combines (averages) adjacent rows where the values of the keyColumns are equal.
     * If the number of rows is reduced to 1/2 or less, this
     * calls trimToSize on each of the PrimitiveArrays.
     * If there are no non-NaN values to average, the average is NaN.
     * 
     * @param keyColumns the numbers of the key columns
     */
    public void averageAdjacentRows(int keyColumns[]) {
        int nRows = nRows();
        if (nRows == 0)
            return;

        //make a bitset of sortColumnNumbers
        BitSet isSortColumn = (new IntArray(keyColumns)).toBitSet();

        //gather sort columns
        int nSortColumns = keyColumns.length;
        PrimitiveArray sortColumns[] = new PrimitiveArray[nSortColumns];
        for (int col = 0; col < nSortColumns; col++) 
            sortColumns[col] = getColumn(keyColumns[col]);

        //go through the data looking for groups of rows with constant values in the sortColumnNumbers
        int nColumns = nColumns();
        int nGood = 0;
        int firstRowInGroup = 0;
        while (firstRowInGroup < nRows) {
            //find lastRowInGroup
            int lastRowInGroup = firstRowInGroup;
            ROW_LOOP:
            for (int row = lastRowInGroup + 1; row < nRows; row++) {
                for (int col = 0; col < nSortColumns; col++) {
                    if (sortColumns[col].compare(firstRowInGroup, row) != 0)
                        break ROW_LOOP;
                }
                lastRowInGroup = row;
            }
            //if (verbose) String2.log("Table.average: first=" + firstRowInGroup +
            //    " last=" + lastRowInGroup);

            //average values in group and store in row nGood
            if (nGood != lastRowInGroup) { //so, the group is not one row, already in place
                for (int col = 0; col < nColumns; col++) {
                    PrimitiveArray pa = getColumn(col);
                    if (firstRowInGroup == lastRowInGroup || //only one row in group
                        isSortColumn.get(col)) {             //values in sortColumnNumbers in a group are all the same
                        pa.copy(firstRowInGroup, nGood);
                    } else {
                        double sum = 0;
                        int count = 0;
                        for (int row = firstRowInGroup; row <= lastRowInGroup; row++) {
                            double d = pa.getDouble(row);
                            if (!Double.isNaN(d)) {
                                count++;
                                sum += pa.getDouble(row);
                            }
                        }
                        pa.setDouble(nGood, count == 0? Double.NaN : sum / count);
                    }
                }
            }

            firstRowInGroup = lastRowInGroup + 1;
            nGood++;
        }

        //remove excess at end of column
        for (int col = 0; col < nColumns; col++)
            getColumn(col).removeRange(nGood, getColumn(col).size());

        //trimToSize
        if (nGood <= nRows / 2) {
            for (int col = 0; col < nColumns; col++)
                getColumn(col).trimToSize();
        }
        if (verbose) String2.log("Table.averageAdjacentRows done. old nRows=" + nRows +
            " new nRows=" + nGood);
    }


    /**
     * This is like the other saveAsMatlab, but writes to a file.
     *
     * @param fullName The full file name (dir + name + ext (usually .mat))
     */
    public void saveAsMatlab(String fullName, String varName) throws Exception {
        if (verbose) String2.log("Table.saveAsMatlab " + fullName); 
        long time = System.currentTimeMillis();

        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);


        //write to dataOutputStream 
        BufferedOutputStream bos = new BufferedOutputStream(
            new FileOutputStream(fullName + randomInt));

        try {
            saveAsMatlab(bos, varName);
            bos.close();

            //rename the file to the specified name, instantly replacing the original file     
            File2.rename(fullName + randomInt, fullName);

        } catch (Exception e) {
            //try to close the file
            try {
                bos.close(); //it calls flush()
            } catch (Exception e2) {
                //don't care
            }

            //delete the partial file
            File2.delete(fullName + randomInt);
            //delete any existing file
            File2.delete(fullName);

            throw e;
        }

        //Old way relies on script which calls Matlab.
        //This relies on a proprietary program, so good to remove it.
        //cShell("/u00/chump/grdtomatlab " + fullGrdName + " " + 
        //    fullResultName + randomInt + ".mat " + varName); 

        if (verbose) String2.log("  Table.saveAsMatlab done. TIME=" + 
            (System.currentTimeMillis() - time));
    }


    /**
     * Save this table data as a Matlab .mat file.
     * This writes the lon values as they are currently in this table
     *    (e.g., +-180 or 0..360).
     * This overwrites any existing file of the specified name.
     * This makes an effort not to create a partial file if there is an error.
     * If no exception is thrown, the file was successfully created.
     * 
     * @param outputStream usually already buffered.
     *    Afterwards, it is flushed, not closed.
     * @param varName the name to use for the variable which holds all of the data, 
     *    usually the dataset's internal name.
     * @throws Exception 
     */
/* commented out 2008-02-07
    public void saveAsMatlab(OutputStream outputStream, String varName) 
        throws Exception {
        if (verbose) String2.log("Table.saveAsMatlab outputStream"); 
        long time = System.currentTimeMillis();

        String errorInMethod = String2.ERROR + " in Table.saveAsMatlab:\n";

        //make sure there is data
        if (nRows() == 0)
            throw new SimpleException(errorInMethod + MustBe.THERE_IS_NO_DATA);

        //open a dataOutputStream 
        DataOutputStream dos = new DataOutputStream(outputStream);

        //write the header
        Matlab.writeMatlabHeader(dos);

        //make an array of the data[row][col]
        int tnRows = nRows();
        int tnCols = nColumns();
        double ar[][] = new double[tnRows][tnCols];
        for (int col = 0; col < tnCols; col++) {
            PrimitiveArray pa = getColumn(col);
            if (pa.elementClass() == String.class) {
                for (int row = 0; row < tnRows; row++) {
                    ar[row][col] = Double.NaN; //can't store strings in a double array                  
                }                
            } else {
                for (int row = 0; row < tnRows; row++)
                    ar[row][col] = pa.getNiceDouble(row);
            }
        }
        Matlab.write2DDoubleArray(dos, varName, ar);

        //this doesn't write attributes because .mat files don't store attributes
        //setStatsAttributes(true); //true = double
        //write the attributes...

        dos.flush(); //essential

        if (verbose) String2.log("  Table.saveAsMatlab done. TIME=" + 
            (System.currentTimeMillis() - time));
    }
*/
    /**
     * Save this table data as a Matlab .mat file.
     * This writes the lon values as they are currently in this table
     *    (e.g., +-180 or 0..360).
     * This overwrites any existing file of the specified name.
     * This makes an effort not to create a partial file if there is an error.
     * If no exception is thrown, the file was successfully created.
     * This maintains the data types (Strings become char[][]).
     * Missing values should already be stored as NaNs (perhaps via convertToStandardMissingValues()).
     *
     * <p>If the columns have different lengths, they will be stored that way in the file(!).
     * 
     * @param outputStream usually already buffered.
     *    Afterwards, it is flushed, not closed.
     * @param structureName the name to use for the Matlab structure which holds all of the data, 
     *    usually the dataset's internal name.
     * @throws Exception 
     */
    public void saveAsMatlab(OutputStream outputStream, String structureName) 
        throws Exception {
        if (verbose) String2.log("Table.saveAsMatlab outputStream"); 
        long time = System.currentTimeMillis();

        String errorInMethod = String2.ERROR + " in Table.saveAsMatlab:\n";

        //make sure there is data
        if (nRows() == 0)
            throw new SimpleException(errorInMethod + MustBe.THERE_IS_NO_DATA);

        //calculate the size of the structure
        int nCols = nColumns();
        byte structureNameInfo[] = Matlab.nameInfo(structureName); 
        NDimensionalIndex ndIndex[] = new NDimensionalIndex[nCols];
        long cumSize = //see 1-32
            16 + //for array flags
            16 + //my structure array is always 2 dimensions 
            structureNameInfo.length +
            8 + //field name length (for all fields)
            8 + nCols * 32; //field names
        for (int col = 0; col < nCols; col++) {
            //String ndIndex takes time to make; so make it and store it for use below
            ndIndex[col] = Matlab.make2DNDIndex(getColumn(col)); 
            //if (verbose) String2.log("  " + getColumnName(col) + " " + ndIndex[col]);
            //add size of each cell
            cumSize += 8 + //type and size
                Matlab.sizeOfNDimensionalArray(  //throws exception if too big for Matlab
                    "", //without column names (they're stored separately)
                    getColumn(col), ndIndex[col]);
        }
        if (cumSize >= Integer.MAX_VALUE - 1000)
            throw new RuntimeException("Too much data: Matlab structures must be < Integer.MAX_VALUE bytes.");

        //open a dataOutputStream 
        DataOutputStream stream = new DataOutputStream(outputStream);

        //******** Matlab Structure
        //*** THIS CODE MIMICS EDDTable.saveAsMatlab. If make changes here, make them there, too.
        //    The code in EDDGrid.saveAsMatlab is similar, too.
        //write the header
        Matlab.writeMatlabHeader(stream);

        //*** write Matlab Structure  see 1-32
        //write the miMatrix dataType and nBytes
        stream.writeInt(Matlab.miMATRIX);        //dataType
        stream.writeInt((int)cumSize); //safe since checked above

        //write array flags 
        stream.writeInt(Matlab.miUINT32); //dataType
        stream.writeInt(8);  //fixed nBytes of data
        stream.writeInt(Matlab.mxSTRUCT_CLASS); //array flags  
        stream.writeInt(0); //reserved; ends on 8 byte boundary

        //write structure's dimension array 
        stream.writeInt(Matlab.miINT32); //dataType
        stream.writeInt(2 * 4);  //nBytes
        //matlab docs have 2,1, octave has 1,1. 
        //Think of structure as one row of a table, where elements are entire arrays:  e.g., sst.lon sst.lat sst.sst.
        //Having multidimensions (e.g., 2 here) lets you have additional rows, e.g., sst(2).lon sst(2).lat sst(2).sst.
        //So 1,1 makes sense.
        stream.writeInt(1);  
        stream.writeInt(1);
         
        //write structure name 
        stream.write(structureNameInfo, 0, structureNameInfo.length);

        //write length for all field names (always 32)  (short form)
        stream.writeShort(4);                //nBytes
        stream.writeShort(Matlab.miINT32);    //dataType
        stream.writeInt(32);                 //nBytes per field name

        //write the field names (each 32 bytes)
        stream.writeInt(Matlab.miINT8);    //dataType
        stream.writeInt(nCols * 32);      //nBytes per field name
        String nulls = String2.makeString('\u0000', 32);
        for (int col = 0; col < nCols; col++) 
            stream.write(String2.toByteArray(
                String2.noLongerThan(getColumnName(col), 31) + nulls), 0, 32);

        //write the structure's elements (one for each col)
        for (int col = 0; col < nCols; col++) 
            Matlab.writeNDimensionalArray(stream, "", //without column names (they're stored separately)
                getColumn(col), ndIndex[col]);

        //this doesn't write attributes because .mat files don't store attributes

        stream.flush(); //essential

        if (verbose) String2.log("  Table.saveAsMatlab done. TIME=" + 
            (System.currentTimeMillis() - time));

    }

    /**
     * This tests saveAsMatlab().
     *
     * @throws Exception if trouble
     */
    public static void testSaveAsMatlab() throws Exception {
        verbose = true;
        reallyVerbose = true;
        //see gov.noaa.pfel.coastwatch.griddata.Matlab class for summary of Matlab commands

        String2.log("\n***** Table.testSaveAsMatlab");
        Table table = new Table();
        table.addColumn("ints", new IntArray(new int[]{1,2,3}));
        table.addColumn("floats", new FloatArray(new float[]{1.1f, 2.2f, 3.3f}));
        table.addColumn("Strings", new StringArray(new String[]{"a", "bb", "ccc"}));
        table.addColumn("doubles", new DoubleArray(new double[]{1.111, 2.222, 3.333}));
        String dir = "c:/temp/";
        File2.delete(dir + "temp.mat");
        table.saveAsMatlab(dir + "temp.mat", "sst"); //names of length 3,4,5 were a challenge
        String mhd = File2.hexDump(dir + "temp.mat", 1000);
        String2.log(mhd);
        //String2.log("\nsst.mat=\n" + File2.hexDump(dir + "sst.mat", 1000));
        Test.ensureEqual(
            mhd.substring(0, 71 * 4) + mhd.substring(71 * 7), //remove the creation dateTime
"4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n" +
"69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n" +
"20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n" +
"6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n" +
//"2c 20 43 72 65 61 74 65   64 20 6f 6e 3a 20 4d 6f   , Created on: Mo |\n" +
//"6e 20 46 65 62 20 31 31   20 30 39 3a 31 31 3a 30   n Feb 11 09:11:0 |\n" +
//"30 20 32 30 30 38 20 20   20 20 20 20 20 20 20 20   0 2008           |\n" +
"20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n" +
"00 00 00 0e 00 00 01 e8   00 00 00 06 00 00 00 08                    |\n" +
"00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 01 00 00 00 01   00 03 00 01 73 73 74 00               sst  |\n" +
"00 04 00 05 00 00 00 20   00 00 00 01 00 00 00 80                    |\n" +
"69 6e 74 73 00 00 00 00   00 00 00 00 00 00 00 00   ints             |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"66 6c 6f 61 74 73 00 00   00 00 00 00 00 00 00 00   floats           |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"53 74 72 69 6e 67 73 00   00 00 00 00 00 00 00 00   Strings          |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"64 6f 75 62 6c 65 73 00   00 00 00 00 00 00 00 00   doubles          |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"00 00 00 0e 00 00 00 40   00 00 00 06 00 00 00 08          @         |\n" +
"00 00 00 0c 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 03 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 05 00 00 00 0c   00 00 00 01 00 00 00 02                    |\n" +
"00 00 00 03 00 00 00 00   00 00 00 0e 00 00 00 40                  @ |\n" +
"00 00 00 06 00 00 00 08   00 00 00 07 00 00 00 00                    |\n" +
"00 00 00 05 00 00 00 08   00 00 00 03 00 00 00 01                    |\n" +
"00 00 00 01 00 00 00 00   00 00 00 07 00 00 00 0c                    |\n" +
"3f 8c cc cd 40 0c cc cd   40 53 33 33 00 00 00 00   ?   @   @S33     |\n" +
"00 00 00 0e 00 00 00 48   00 00 00 06 00 00 00 08          H         |\n" +
"00 00 00 04 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 03 00 00 00 03   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 04 00 00 00 12   00 61 00 62 00 63 00 20            a b c   |\n" +
"00 62 00 63 00 20 00 20   00 63 00 00 00 00 00 00    b c     c       |\n" +
"00 00 00 0e 00 00 00 48   00 00 00 06 00 00 00 08          H         |\n" +
"00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 03 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 09 00 00 00 18   3f f1 c6 a7 ef 9d b2 2d           ?      - |\n" +
"40 01 c6 a7 ef 9d b2 2d   40 0a a9 fb e7 6c 8b 44   @      -@    l D |\n",
"mhd=" + mhd);
        //File2.delete(dir + "temp.mat");
    }


    /**
     * Save this table of data as a flat netCDF .nc file (a column for each 
     * variable, all referencing one dimension) using the currently
     * available attributes.
     * <br>The data are written as separate variables, sharing a common dimension
     *   "observation", not as a Structure.
     * <br>The data values are written as their current data type 
     *   (e.g., float or int).
     * <br>This writes the lon values as they are currently in this table
     *   (e.g., +-180 or 0..360).
     * <br>This overwrites any existing file of the specified name.
     * <br>This makes an effort not to create a partial file if there is an error.
     * <br>If no exception is thrown, the file was successfully created.
     * <br>!!!The file must have at least one row, or an Exception will be thrown
     *   (nc dimensions can't be 0 length).
     * <br>!!!The table should initially have missing values stored as NaNs.
     *    NaN's are converted to DataHelper.FAKE_MISSING_VALUE temporarily.
     * 
     * @param fullName The full file name (dir + name + ext (usually .nc))
     * @param dimensionName the name for the rows dimension, 
     *    e.g., usually "time", "station", "observation", "trajectory", "row", or ...?
     *    To conform to the Unidata Observation Dataset Conventions
     *    (http://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html):
     *    This sets the global attribute observationDimension={dimensionName}.
     * @throws Exception 
     */
    public void saveAsFlatNc(String fullName, String dimensionName) throws Exception {
        saveAsFlatNc(fullName, dimensionName, true);
    }

    /**
     * Save this table of data as a flat netCDF .nc file (a column for each 
     * variable, all referencing one dimension) using the currently
     * available attributes.
     * <br>The data are written as separate variables, sharing a common dimension
     *   "observation", not as a Structure.
     * <br>The data values are written as their current data type 
     *   (e.g., float or int).
     * <br>This writes the lon values as they are currently in this table
     *   (e.g., +-180 or 0..360).
     * <br>This overwrites any existing file of the specified name.
     * <br>This makes an effort not to create a partial file if there is an error.
     * <br>If no exception is thrown, the file was successfully created.
     * <br>!!!The file must have at least one row, or an Exception will be thrown
     *   (nc dimensions can't be 0 length).
     * 
     * @param fullName The full file name (dir + name + ext (usually .nc))
     * @param dimensionName the name for the rows dimension, 
     *    e.g., usually "time", "station", "observation", "trajectory", "row", or ...?
     *    To conform to the Unidata Observation Dataset Conventions
     *    (http://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html):
     *    This sets the global attribute observationDimension={dimensionName}.
     * @param convertToFakeMissingValues if true, 
     *    NaN's are converted to DataHelper.FAKE_MISSING_VALUE temporarily.
     * @throws Exception 
     */
    public void saveAsFlatNc(String fullName, String dimensionName, 
            boolean convertToFakeMissingValues) throws Exception {
        if (verbose) String2.log("Table.saveAsFlatNc " + fullName); 
        long time = System.currentTimeMillis();


        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        //open the file (before 'try'); if it fails, no temp file to delete
        NetcdfFileWriteable nc = NetcdfFileWriteable.createNew(fullName + randomInt, false);
        try {
            //items determined by looking at a .nc file; items written in that order 

            int nRows = nRows();
            int nColumns = nColumns();
            if (nRows == 0) {
                throw new Exception(String2.ERROR + " in Table.saveAsFlatNc:\n" + 
                    MustBe.THERE_IS_NO_DATA);
            }

            //define the dimensions
            Dimension dimension  = nc.addDimension(dimensionName, nRows);
//javadoc says: if there is an unlimited dimension, all variables that use it are in a structure
//Dimension rowDimension  = nc.addDimension("row", nRows, true, true, false); //isShared, isUnlimited, isUnknown
//String2.log("unlimitied dimension exists: " + (nc.getUnlimitedDimension() != null));

            //add the variables
            for (int col = 0; col < nColumns; col++) {
                PrimitiveArray pa = getColumn(col);
                Class type = pa.elementClass();
                String tColName = getColumnNameWithoutSpaces(col);
                if (type == String.class) {
                    int max = Math.max(1, ((StringArray)pa).maxStringLength()); //nc libs want at least 1; 0 happens if no data
                    Dimension lengthDimension = nc.addDimension(
                        tColName + NcHelper.StringLengthSuffix, max);
                    nc.addVariable(tColName, DataType.CHAR, 
                        new Dimension[]{dimension, lengthDimension}); 
                } else {
                    nc.addVariable(tColName, DataType.getType(type), new Dimension[]{dimension}); 
                }
//nc.addMemberVariable(recordStructure, nc.findVariable(tColName));
            }

//boolean bool = nc.addRecordStructure(); //creates a structure variable called "record"         
//String2.log("addRecordStructure: " + bool);
//Structure recordStructure = (Structure)nc.findVariable("record");

            //set id attribute = file name
            //currently, only the .nc saveAs types use attributes!
            if (globalAttributes.get("id") == null)
                globalAttributes.set("id", File2.getNameNoExtension(fullName));

            globalAttributes.set("observationDimension", dimensionName);
            //set the globalAttributes
            NcHelper.setAttributes(nc, "NC_GLOBAL", globalAttributes);

            for (int col = 0; col < nColumns; col++) {
                //convert to fake MissingValues   (in time to write attributes)
                if (convertToFakeMissingValues)
                    convertToFakeMissingValues(col);

                NcHelper.setAttributes(nc, getColumnNameWithoutSpaces(col), columnAttributes(col));
            }

            //leave "define" mode
            nc.create();

            //write the data
            for (int col = 0; col < nColumns; col++) {
                PrimitiveArray pa = getColumn(col);
                nc.write(getColumnNameWithoutSpaces(col), 
                    NcHelper.get1DArray(pa.toObjectArray()));

                //convert back to standard MissingValues
                if (convertToFakeMissingValues)
                    convertToStandardMissingValues(col);
            }

            //if close throws exception, it is trouble
            nc.close(); //it calls flush() and doesn't like flush called separately

            //rename the file to the specified name, instantly replacing the original file
            File2.rename(fullName + randomInt, fullName);

            //diagnostic
            if (verbose) String2.log("  Table.saveAsFlatNc done. TIME=" + 
                (System.currentTimeMillis() - time));
            //ncDump("End of Table.saveAsFlatNc", directory + name + ext, false);

        } catch (Exception e) {
            //try to close the file
            try {
                nc.close(); //it calls flush() and doesn't like flush called separately
            } catch (Exception e2) {
                //don't care
            }

            //delete the partial file
            File2.delete(fullName + randomInt);

            //delete any existing file
            File2.delete(fullName);

            throw e;
        }

    }

    /** 
     * This is like saveAs4DNc but with no StringVariable option.
     */
    public void saveAs4DNc(String fullName, int xColumn, int yColumn, 
            int zColumn, int tColumn) throws Exception {
 
        saveAs4DNc(fullName, xColumn, yColumn, 
            zColumn, tColumn, null, null, null);
    }

    /** 
     * This is like saveAs4DNc but removes stringVariableColumn (often column 4 = "ID", which
     * must have just 1 value, repeated)
     * and saves it as a stringVariable in the 4DNc file, 
     * and then reinserts the stringVariableColumn.
     * For files with just 1 station's data, Dapper and DChart like this format.
     *
     * @param stringVariableColumn is the column (
     */
    public void saveAs4DNcWithStringVariable(String fullName, int xColumn, int yColumn, 
            int zColumn, int tColumn, int stringVariableColumn) throws Exception {
 
        //remove ID column
        String tName = getColumnName(stringVariableColumn);
        Attributes tIdAtt = columnAttributes(stringVariableColumn);
        PrimitiveArray tIdPa = getColumn(stringVariableColumn);
        removeColumn(stringVariableColumn);

        //save as 4DNc
        saveAs4DNc(fullName, xColumn, yColumn, 
            zColumn, tColumn, tName, tIdPa.getString(0), tIdAtt);

        //reinsert ID column
        addColumn(stringVariableColumn, tName, tIdPa, tIdAtt);
    }

        

    /**
     * Save this table of data as a 4D netCDF .nc file using the currently
     * available attributes.
     * This method uses the terminology x,y,z,t, but does require
     *   that the data represent lon,lat,alt,time.
     * All columns other than the 4 dimension related columns are stored
     * as 4D arrays.
     * This will sort the values t (primary key), then z, then y, then x (least important key).
     * The data values are written as their current data type 
     * (e.g., float or int).
     * This writes the lon values as they are currently in this table
     * (e.g., +-180 or 0..360).
     * This overwrites any existing file of the specified name.
     * This makes an effort not to create a partial file if there is an error.
     * If no exception is thrown, the file was successfully created.
     * !!!The file must have at least one row, or an Exception will be thrown
     * (nc dimensions can't be 0 length).
     * This tries to look like it works instantaneously:
     *   it writes to a temp file then renames it to correct name.
     *
     * <p>This supports an optional stringVariable which is written
     * to the file as a 1D char array.   Dapper/DChart prefers this
     * to a 4D array for the ID info.
     * 
     * @param fullName The full file name (dir + name + ext (usually .nc))
     * @param xColumn the column with lon info.
     * @param yColumn the column with lat info.
     * @param zColumn the column with alt info.
     * @param tColumn the column with time info.
     * @param stringVariableName the name for the optional 1D String variable 
     *    (or null to not use this feature)
     * @param stringVariableValue the value for the optional 1D String variable
     *    (must be non-null non-"" if stringVariableName isn't null)
     * @param stringVariableAttributes the attributes for the optional 1D String variable
     *    (must be non-null if stringVariableName isn't null)
     * @throws Exception 
     */
    public void saveAs4DNc(String fullName, int xColumn, int yColumn, 
            int zColumn, int tColumn, String stringVariableName, 
            String stringVariableValue, Attributes stringVariableAttributes) throws Exception {
        if (verbose) String2.log("Table.saveAs4DNc " + fullName); 
        long time = System.currentTimeMillis();


        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);


        //ensure there is data
        String errorInMethod = String2.ERROR + " in Table.saveAs4DNc:\n";
        if (stringVariableName != null) {
            Test.ensureNotEqual(stringVariableName.length(), 0, errorInMethod + "stringVariableName is \"\".");
            if (stringVariableValue == null)
                throw new SimpleException(errorInMethod + "stringVariableValue is null.");
            Test.ensureNotEqual(stringVariableValue.length(), 0, errorInMethod + "stringVariableValue is \"\".");
        }
        if (nRows() == 0) {
            throw new Exception(errorInMethod + MustBe.THERE_IS_NO_DATA);
        }

        //open the file (before 'try'); if it fails, no temp file to delete
        NetcdfFileWriteable nc = NetcdfFileWriteable.createNew(fullName + randomInt, false);
        long make4IndicesTime = -1;
        
        try {
            //if (verbose) {
            //    String2.log("Table.saveAs4DNc" +
            //        "\n     raw X " + getColumn(xColumn).statsString() + 
            //        "\n     raw Y " + getColumn(yColumn).statsString() + 
            //        "\n     raw Z " + getColumn(zColumn).statsString() + 
            //        "\n     raw T " + getColumn(tColumn).statsString()); 
            //}

            //sort
            PrimitiveArray.sort(columns, 
                new int[]{tColumn, zColumn, yColumn, xColumn},
                new boolean[]{true, true, true, true});


            //add axis attributes
            columnAttributes(xColumn).set("axis", "X");
            columnAttributes(yColumn).set("axis", "Y");
            columnAttributes(zColumn).set("axis", "Z");
            columnAttributes(tColumn).set("axis", "T");

            //make the indices
            make4IndicesTime = System.currentTimeMillis();
            IntArray xIndices = new IntArray();
            IntArray yIndices = new IntArray();
            IntArray zIndices = new IntArray();
            IntArray tIndices = new IntArray();
            PrimitiveArray uniqueX = getColumn(xColumn).makeIndices(xIndices);
            PrimitiveArray uniqueY = getColumn(yColumn).makeIndices(yIndices);
            PrimitiveArray uniqueZ = getColumn(zColumn).makeIndices(zIndices);
            PrimitiveArray uniqueT = getColumn(tColumn).makeIndices(tIndices);
            make4IndicesTime = System.currentTimeMillis() - make4IndicesTime;
            //if (verbose) {
            //    String2.log("Table.saveAs4DNc" +
            //        "\n  unique X " + uniqueX.statsString() + 
            //        "\n  unique Y " + uniqueY.statsString() + 
            //        "\n  unique Z " + uniqueZ.statsString() + 
            //        "\n  unique T " + uniqueT.statsString()); 
            //    //String2.getStringFromSystemIn("continue?");
            //}

            int nX = uniqueX.size();
            int nY = uniqueY.size();
            int nZ = uniqueZ.size();
            int nT = uniqueT.size();

            //items determined by looking at a .nc file; items written in that order 
            int nRows = nRows();
            int nColumns = nColumns();
            if (nRows == 0) {
                throw new Exception(String2.ERROR + " in Table.saveAs4DNc:\nThe table has no data.");
            }
            int stringLength[] = new int[nColumns];

            //define the dimensions
            Dimension xDimension  = nc.addDimension(getColumnNameWithoutSpaces(xColumn), nX);
            Dimension yDimension  = nc.addDimension(getColumnNameWithoutSpaces(yColumn), nY);
            Dimension zDimension  = nc.addDimension(getColumnNameWithoutSpaces(zColumn), nZ);
            Dimension tDimension  = nc.addDimension(getColumnNameWithoutSpaces(tColumn), nT);
//javadoc says: if there is an unlimited dimension, all variables that use it are in a structure
//Dimension rowDimension  = nc.addDimension("row", nRows, true, true, false); //isShared, isUnlimited, isUnknown
//String2.log("unlimitied dimension exists: " + (nc.getUnlimitedDimension() != null));

            //add the variables
            for (int col = 0; col < nColumns; col++) {

                //for x/y/z/t make a 1D variable
                PrimitiveArray pa = null;
                if  (col == xColumn || col == yColumn || 
                     col == zColumn || col == tColumn) {
                    Dimension aDimension = null;
                    if      (col == xColumn) {pa = uniqueX; aDimension = xDimension;}
                    else if (col == yColumn) {pa = uniqueY; aDimension = yDimension;}
                    else if (col == zColumn) {pa = uniqueZ; aDimension = zDimension;}
                    else if (col == tColumn) {pa = uniqueT; aDimension = tDimension;}
                    Class type = pa.elementClass();
                    String tColName = getColumnNameWithoutSpaces(col);
                    if (type == String.class) {
                        int max = Math.max(1, ((StringArray)pa).maxStringLength()); //nc libs want at least 1; 0 happens if no data
                        stringLength[col] = max;
                        Dimension lengthDimension = nc.addDimension(
                            tColName + NcHelper.StringLengthSuffix, max);
                        nc.addVariable(tColName, DataType.CHAR, 
                            new Dimension[]{aDimension, lengthDimension}); 
                    } else {
                        nc.addVariable(tColName, DataType.getType(type), 
                            new Dimension[]{aDimension}); 
                    }
                } else {

                    //for other columns, make a 4D array
                    pa = getColumn(col);
                    Class type = pa.elementClass();
                    String tColName = getColumnNameWithoutSpaces(col);
                    if (type == String.class) {
                        int max = Math.max(1, ((StringArray)pa).maxStringLength()); //nc libs want at least 1; 0 happens if no data
                        stringLength[col] = max;
                        Dimension lengthDimension  = nc.addDimension(
                            tColName + NcHelper.StringLengthSuffix, max);
                        nc.addVariable(tColName, DataType.CHAR, 
                            new Dimension[]{tDimension, zDimension, yDimension, xDimension, lengthDimension}); 
                    } else {
                        nc.addVariable(tColName, DataType.getType(type), 
                            new Dimension[]{tDimension, zDimension, yDimension, xDimension}); 
                    }

                    //convert to fake MissingValues
                    convertToFakeMissingValues(col);

                }
            }
//nc.addMemberVariable(recordStructure, nc.findVariable(tColName));

//boolean bool = nc.addRecordStructure(); //creates a structure variable called "record"         
//String2.log("addRecordStructure: " + bool);
//Structure recordStructure = (Structure)nc.findVariable("record");

            //set id attribute = file name
            //currently, only the .nc saveAs types use attributes!
            globalAttributes.set("id", File2.getNameNoExtension(fullName));

            //write Attributes   (after adding variables since mv's and related attributes adjusted)
            NcHelper.setAttributes(nc, "NC_GLOBAL", globalAttributes);
            for (int col = 0; col < nColumns; col++) 
                NcHelper.setAttributes(nc, getColumnNameWithoutSpaces(col), columnAttributes(col));

            //create the stringVariable
            if (stringVariableName != null) {
                stringVariableName = String2.replaceAll(stringVariableName, " ", "_");

                Dimension lengthDimension = nc.addDimension(
                    stringVariableName + NcHelper.StringLengthSuffix, 
                    Math.max(1, stringVariableValue.length())); //nclib wants at least 1
                nc.addVariable(stringVariableName, DataType.CHAR, 
                    new Dimension[]{lengthDimension}); 

                //save the attributes
                NcHelper.setAttributes(nc, stringVariableName, stringVariableAttributes);
            }

            //leave "define" mode
            nc.create();

            //write the data
            for (int col = 0; col < nColumns; col++) {
                //String2.log("  writing col=" + col);
                Array ar = null;
                PrimitiveArray pa = getColumn(col);

                if         (col == xColumn) { ar = NcHelper.get1DArray(uniqueX.toObjectArray()); 
                } else if  (col == yColumn) { ar = NcHelper.get1DArray(uniqueY.toObjectArray());
                } else if  (col == zColumn) { ar = NcHelper.get1DArray(uniqueZ.toObjectArray());
                } else if  (col == tColumn) { ar = NcHelper.get1DArray(uniqueT.toObjectArray());
                } else {
                    //other columns are 4D arrays
                    if (pa instanceof DoubleArray) {
                        double par[] = ((DoubleArray)pa).array;
                        ArrayDouble.D4 tar = new ArrayDouble.D4(nT, nZ, nY, nX);
                        for (int row = 0; row < nRows; row++)
                            tar.set(tIndices.array[row], zIndices.array[row],
                                    yIndices.array[row], xIndices.array[row], par[row]);
                        ar = tar;
                    } else if (pa instanceof FloatArray) {
                        float par[] = ((FloatArray)pa).array;
                        ArrayFloat.D4 tar = new ArrayFloat.D4(nT, nZ, nY, nX);
                        for (int row = 0; row < nRows; row++)
                            tar.set(tIndices.array[row], zIndices.array[row],
                                    yIndices.array[row], xIndices.array[row], par[row]);
                        ar = tar;
                    } else if (pa instanceof LongArray) {
                        long par[] = ((LongArray)pa).array;
                        ArrayLong.D4 tar = new ArrayLong.D4(nT, nZ, nY, nX);
                        for (int row = 0; row < nRows; row++)
                            tar.set(tIndices.array[row], zIndices.array[row],
                                    yIndices.array[row], xIndices.array[row], par[row]);
                        ar = tar;
                    } else if (pa instanceof IntArray) {
                        int par[] = ((IntArray)pa).array;
                        ArrayInt.D4 tar = new ArrayInt.D4(nT, nZ, nY, nX);
                        for (int row = 0; row < nRows; row++)
                            tar.set(tIndices.array[row], zIndices.array[row],
                                    yIndices.array[row], xIndices.array[row], par[row]);
                        ar = tar;
                    } else if (pa instanceof ShortArray) {
                        short par[] = ((ShortArray)pa).array;
                        ArrayShort.D4 tar = new ArrayShort.D4(nT, nZ, nY, nX);
                        for (int row = 0; row < nRows; row++)
                            tar.set(tIndices.array[row], zIndices.array[row],
                                    yIndices.array[row], xIndices.array[row], par[row]);
                        ar = tar;
                    } else if (pa instanceof ByteArray) {
                        byte par[] = ((ByteArray)pa).array;
                        ArrayByte.D4 tar = new ArrayByte.D4(nT, nZ, nY, nX);
                        for (int row = 0; row < nRows; row++)
                            tar.set(tIndices.array[row], zIndices.array[row],
                                    yIndices.array[row], xIndices.array[row], par[row]);
                        ar = tar;
                    } else if (pa instanceof StringArray) {
                        String par[] = ((StringArray)pa).array;
                        ArrayChar.D5 tar = new ArrayChar.D5(nT, nZ, nY, nX, stringLength[col]);
                        ucar.ma2.Index index = tar.getIndex();
                        for (int row = 0; row < nRows; row++) 
                            tar.setString(index.set(tIndices.array[row], zIndices.array[row],
                                    yIndices.array[row], xIndices.array[row], 0), par[row]);
                        /*
                        for (int row = 0; row < nRows; row++) {
                            String s = par[row];
                            int sLength = s.length();
                            int tt = tIndices.array[row];
                            int tz = zIndices.array[row];
                            int ty = yIndices.array[row]; 
                            int tx = xIndices.array[row];
                            for (int po = 0; po < sLength; po++) 
                                tar.set(tt, tz, ty, tx, po, s.charAt(po));
                        }*/
                        ar = tar;
                    } else throw new SimpleException(
                        String2.ERROR + " in Table.saveAs4DNc: unexpected object type: " + 
                        pa.elementClass().toString());

                }

                //write the data
                nc.write(getColumnNameWithoutSpaces(col), ar);

                //undo fakeMissingValue
                if  (col != xColumn && col != yColumn && 
                     col != zColumn && col != tColumn) {

                    //convert back to standard MissingValues
                    convertToStandardMissingValues(col);
                }
            }

            //write the stringVariable
            if (stringVariableName != null) {
                //ArrayChar.D1 ar = new ArrayChar.D1(stringVariableValue.length());
                //ar.setString(stringVariableValue);
                nc.write(stringVariableName, 
                    NcHelper.get1DArray(stringVariableValue));
            }


            //if close throws exception, it is trouble
            nc.close(); //it calls flush() and doesn't like flush called separately

            //rename the file to the specified name, instantly replacing the original file
            File2.rename(fullName + randomInt, fullName);

            //diagnostic
            if (verbose)
                String2.log("  Table.saveAs4DNc done. " + 
                    " make4IndicesTime=" + make4IndicesTime +
                    " total TIME=" + (System.currentTimeMillis() - time));
            //ncDump("End of Table.saveAs4DNc", fullName, false);

        } catch (Exception e) {
            //try to close the file
            try {
                nc.close(); //it calls flush() and doesn't like flush called separately
            } catch (Exception e2) {
                //don't care
            }

            //delete the partial file
            File2.delete(fullName + randomInt);

            //delete the original file
            File2.delete(fullName);

            throw e;
        }


    }


    /**
     * This populates the table with the data from a sql resultsSet.
     * See readSql.
     *
     * @param rs
     * @throws Exception if trouble
     */
    public void readSqlResultSet(ResultSet rs) throws Exception {

        if (verbose) String2.log("  Table.readSqlResultSet");
        long time = System.currentTimeMillis();
        clear();

        //set up columns in table
        ResultSetMetaData metadata = rs.getMetaData();
        int nCol = metadata.getColumnCount();
        PrimitiveArray paArray[] = new PrimitiveArray[nCol];
        boolean getString[]  = new boolean[nCol]; 
        boolean getInt[]     = new boolean[nCol];
        boolean getLong[]    = new boolean[nCol];
        boolean getDouble[]  = new boolean[nCol];
        boolean getDate[]    = new boolean[nCol]; //read as String, then convert to seconds since epoch
        for (int col = 0; col < nCol; col++) {
            //note that sql counts 1...
            int colType = metadata.getColumnType(1 + col);
            paArray[col] = PrimitiveArray.sqlFactory(colType);
            Class tClass = paArray[col].elementClass();
            if      (tClass == String.class) getString[col] = true;
            else if (colType == Types.DATE ||
                     colType == Types.TIMESTAMP) getDate[col] = true;
            else if (tClass == double.class) getDouble[col] = true;
            else if (tClass == long.class)   getLong[col] = true;
            else getInt[col] = true;

            //actually add the column
            String colName = metadata.getColumnName(1 + col); //getColumnName == getColumnLabel
            addColumn(colName, paArray[col]);
            //if (verbose) String2.log("    col=" + col + " name=" + metadata.getColumnName(1 + col));
        }

        //process the rows of data
        while (rs.next()) {
            for (int col = 0; col < nCol; col++) {
                if      (getString[col])  {
                    String ts = rs.getString(1 + col); 
                    paArray[col].addString(ts == null? "" : ts); 
                } else if (getInt[col])     paArray[col].addInt(rs.getInt(1 + col)); 
                else if (getLong[col])    ((LongArray)paArray[col]).add(rs.getLong(1 + col)); 
                else if (getDouble[col])  paArray[col].addDouble(rs.getDouble(1 + col)); 
                //date string is always in form yyyy-mm-dd
                //timestamp string is always in form yyyy-mm-dd hh:mm:ss.fffffffff 
                else if (getDate[col]) {
                    //convert timestamp and date to epochSeconds
                    Timestamp ts = rs.getTimestamp(1 + col);
                    paArray[col].addDouble(ts == null? Double.NaN : ts.getTime() / 1000.0);                    
                } else throw new SimpleException(
                    String2.ERROR + " in Table.readSqlResultSet: process unknown column(" + 
                    col + ") type."); 
            }
        }

        if (verbose) String2.log("    Table.readSqlResultSet done. nColumns=" + nColumns() +
            " nRows=" + nRows() + " TIME=" + (System.currentTimeMillis() - time));
    }

    /**
     * This reads data from the resultsSet from an sql query using jdbc.
     * !!!WARNING - THIS APPROACH OFFERS NO PROTECTION FROM SQL INJECTION.
     * ONLY USE THIS IF YOU, NOT SOME POSSIBLY MALICIOUS USER, SPECIFIED
     * THE QUERY.
     *
     * <p>Examples of things done to prepare to use this method:
     * <ul>
     * <li>Class.forName("org.postgresql.Driver");
     * <li>String url = "jdbc:postgresql://otter.pfeg.noaa.gov/posttest";  //database name
     * <li>String user = "postadmin";
     * <li>String password = String2.getPasswordFromSystemIn("Password for '" + user + "'? ");
     * <li>Connection con = DriverManager.getConnection(url, user, password);
     * </ul>
     *
     * @param con a Connection (these are sometimes pooled to save time)
     * @param query e.g., "SELECT * FROM names WHERE id = 3"
     * @throws Exception if trouble
     */
    public void readSql(Connection con, String query) throws Exception {

        if (verbose) String2.log("Table.readSql");
        long time = System.currentTimeMillis();
        clear();

        //create the statement and execute the query
        Statement statement = con.createStatement();
        readSqlResultSet(statement.executeQuery(query));

        //close the statement
        statement.close();

        if (verbose) String2.log("  Table.readSql done. nColumns=" + nColumns() +
            " nRows=" + nRows() + " TIME=" + (System.currentTimeMillis() - time));
    }


    /**
     * This inserts the rows of data in this table to a sql table, using jdbc.
     * <ul>
     * <li> The column names in this table must match the column names in the database table.
     * <li> If createTable is true, the column names won't be changed (e.g., 
     *   spaces in column names will be retained.)
     * <li> If createTable is false, the column names in this table don't have to 
     *   be all of the column names in the database table, or in the same order.
     * <li> This assumes all columns (except primary key) accept nulls or have
     *   defaults defined.  If a value in this table is missing, 
     *   the default value will be put in the database table.
     * <li> The database timezone (in pgsql/data/postgresql.conf) should be set to -0 (UTC)
     *   so that dates and times are interpreted as being in UTC time zone.
     * </ul>
     *
     * <p>Examples of things done to prepare to use this method:
     * <ul>
     * <li>Class.forName("org.postgresql.Driver"); //to load the jdbc driver
     * <li>String url = "jdbc:postgresql://otter.pfeg.noaa.gov/posttest";  //database name
     * <li>String user = "postadmin";
     * <li>String password = String2.getPasswordFromSystemIn("Password for '" + user + "'? ");
     * <li>Connection con = DriverManager.getConnection(url, user, password);
     * </ul>
     *
     * @param con a Connection (these are sometimes pooled to save time)
     * @param createTable if createTable is true, a new table will be created 
     *   with all columns (except primaryKeyCol) allowing nulls.
     *   If you need more flexibility when creating the table, create it
     *   separately, then use this method to insert data into it.
     *   If createTable is true and a table by the same name exists, it will be deleted.
     *   If createTable is false, it must already exist.
     * @param tableName  the database's name for the table that this table's data
     *    will be inserted into, 
     *    e.g., "myTable" (equivalent in postgres to "public.myTable")
     *    or "mySchema.myTable".
     * @param primaryKeyCol is the primary key column (0..., or -1 if none).
     *    This is ignored if createTable is false.
     * @param dateCols a list of columns (0..) with dates, stored
     *    as seconds since epoch in DoubleArrays.
     * @param timestampCols a list of columns (0..) with timestamps (date + time), stored
     *    as seconds since epoch in DoubleArrays.
     *    Here, timestamp precision (decimal digits for seconds value) is always 0.
     *    If createTable is true, these columns show up (in postgresql) as
     *    "timestamp without time zone".
     * @param timeCols a list of columns (0..) with times (without dates), stored
     *    as strings in StringArrays (with format "hh:mm:ss", e.g., "23:59:59" 
     *    with implied time zone of UTC), 
     *    which will be stored as sql TIME values.
     *    Here, time precision (decimal digits for seconds value) is always 0.
     *    Missing values can be stored as "" or null.
     *    Improperly formatted time values throw an exception.
     *    If createTable is true, these columns show up (in postgresql) as
     *    "time without time zone".
     * @param stringLengthFactor for StringArrays, this is the factor (typically 1.5)  
     *   to be multiplied by the current max string length (then rounded up to 
     *   a multiple of 10) to estimate the varchar length.  
     * @throws Exception if trouble.
     *   If exception thrown, table may or may not have been created, but no
     *      data rows have been inserted.
     *   If no exception thrown, table was created (if requested) and all data 
     *       was inserted.
     */
    public void saveAsSql(Connection con, boolean createTable, String tableName,
        int primaryKeyCol, int dateCols[], int timestampCols[], int timeCols[],
        double stringLengthFactor) throws Exception {

//    * @param timeZoneOffset this identifies the time zone associated with the 
//    *    time columns.  (The Date and Timestamp columns are already UTC.)

        if (verbose) String2.log("Table.saveAsSql(" + tableName + ")");
        String errorInMethod = String2.ERROR + " in Table.saveAsSql(" + tableName + "):\n";
        long elapsedTime = System.currentTimeMillis();
        if (dateCols == null) dateCols = new int[0];
        if (timestampCols == null) timestampCols = new int[0];
        if (timeCols == null) timeCols = new int[0];
        int nCols = nColumns();       
        int nRows = nRows();       

        //make a local 'table' for faster access
        PrimitiveArray paArray[] = new PrimitiveArray[nCols];
        String sqlType[] = new String[nCols];
        for (int col = 0; col < nCols; col++) {
            paArray[col] = getColumn(col);
            sqlType[col] = paArray[col].getSqlTypeString(stringLengthFactor);
        }

        //swap in the dateCols
        boolean isDateCol[] = new boolean[nCols];
        for (int col = 0; col < dateCols.length; col++) {
            int tCol = dateCols[col];
            isDateCol[tCol] = true;
            sqlType[tCol] = "date";
        }

        //swap in the timestampCols
        boolean isTimestampCol[] = new boolean[nCols];
        for (int col = 0; col < timestampCols.length; col++) {
            int tCol = timestampCols[col];
            isTimestampCol[tCol] = true;
            sqlType[tCol] = "timestamp";
        }

        //identify timeCols
        boolean isTimeCol[] = new boolean[nCols];
        for (int col = 0; col < timeCols.length; col++) {
            int tCol = timeCols[col];
            isTimeCol[tCol] = true;
            sqlType[tCol] = "time";
        }

        //*** create the table   (in postgres, default for columns is: allow null)
        if (createTable) {
            //delete the table (if it exists)
            dropSqlTable(con, tableName, true);

            Statement statement = con.createStatement();
            StringBuilder create = new StringBuilder(
                "CREATE TABLE " + tableName + " ( \"" + 
                getColumnName(0) + "\" " + sqlType[0] + 
                    (primaryKeyCol == 0? " PRIMARY KEY" : ""));
            for (int col = 1; col < nCols; col++) 
                create.append(", \"" + getColumnName(col) + "\" " + sqlType[col] + 
                    (primaryKeyCol == col? " PRIMARY KEY" : ""));
            create.append(" )");
            if (verbose) String2.log("  create=" + create);
            statement.executeUpdate(create.toString());

            //close statement
            statement.close();
        }

        //*** insert the rows of data into the table
        //There may be no improved efficiency from statement.executeBatch
        //as postgres may still be doing commands one at a time
        //(http://archives.free.net.ph/message/20070115.122431.93092975.en.html#pgsql-jdbc)
        //and it is more memory efficient to just do one at a time.
        //BUT batch is more efficient on other databases and
        //most important, it allows us to rollback.
        //See batch info:
        //http://www.jguru.com/faq/view.jsp?EID=5079
        //and  http://www.onjava.com/pub/a/onjava/excerpt/javaentnut_2/index3.html?page=2 (no error checking/rollback).

        //timezones
        //jdbc setTime setDate setTimestamp normally works with local time only.
        //To specify UTC timezone, you need to call setDate, setTime, setTimestamp
        //   with Calendar object which has the time zone used to interpret the date/time.
        //see http://www.idssoftware.com/faq-j.html  see J15
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        //make the prepared statement
        StringBuilder prep = new StringBuilder();
        prep.append("INSERT INTO " + tableName + " ( \"" + getColumnName(0) + "\"");
        for (int col = 1; col < nCols; col++) 
            prep.append(", \"" + getColumnName(col) + "\"");
        prep.append(") VALUES (?");
        for (int col = 1; col < nCols; col++) 
            prep.append(", ?");
        prep.append(")");
        if (verbose) String2.log("  prep=" + prep);
        PreparedStatement pStatement = con.prepareStatement(prep.toString());

        //add each row's data to the prepared statement 
        for (int row = 0; row < nRows; row++) {
            //clear parameters, to ensure previous row's data is cleared and new values are set
            pStatement.clearParameters(); 

            //add this row's values
            for (int col = 0; col < nCols; col++) {
                PrimitiveArray pa = paArray[col];
                Class et = pa.elementClass();
                //col+1 because sql counts columns as 1...
                //check for date, timestamp, time columns before double and String
                if (isDateCol[col]) {    
                    double d = pa.getDouble(row);
                    if (Math2.isFinite(d)) {
                        Date date = new Date(Math.round(d * 1000)); //set via UTC millis
                        pStatement.setDate(col + 1, date, cal); //cal specifies the UTC timezone
                    } else pStatement.setDate(col + 1, null);
                } else if (isTimestampCol[col]) {    
                    double d = pa.getDouble(row);
                    if (Math2.isFinite(d)) {
                        Timestamp timestamp = new Timestamp(Math.round(d * 1000)); //set via UTC millis
                        pStatement.setTimestamp(col + 1, timestamp, cal); //cal specifies the UTC timezone
                    } else pStatement.setTimestamp(col + 1, null);
                } else if (isTimeCol[col]) {    
                    //data already a time string
                    String s = pa.getString(row);  
                    if (s == null || s.length() == 0) {
                        pStatement.setTime(col + 1, null);
                    } else if ( //ensure that format is HH:MM:SS
                        s.length() == 8 &&
                        String2.isDigit(s.charAt(0)) && String2.isDigit(s.charAt(1)) && s.charAt(2) == ':' && 
                        String2.isDigit(s.charAt(3)) && String2.isDigit(s.charAt(4)) && s.charAt(5) == ':' && 
                        String2.isDigit(s.charAt(6)) && String2.isDigit(s.charAt(7))) {
                        //Time documentation says Time is java Date object with date set to 1970-01-01
                        try {
                            double d = Calendar2.isoStringToEpochSeconds("1970-01-01T" + s);  //throws exception if trouble
                            //String2.log("date=" + s + " -> " + Calendar2.epochSecondsToIsoStringT(d));
                            Time time = new Time(Math.round(d * 1000)); 
                            pStatement.setTime(col + 1, time, cal); //cal specifies the UTC timezone
                        } catch (Exception e) {
                            pStatement.setTime(col + 1, null);
                        }
                    } else {
                        throw new SimpleException(errorInMethod + "Time format must be HH:MM:SS. Bad value=" +
                            s + " in row=" + row + " col=" + col);
                    }
                //for integer types, there seems to be no true null, so keep my missing value, e.g., Byte.MAX_VALUE
                } else if (et == byte.class) {  pStatement.setByte(  col + 1, ((ByteArray)pa).get(row)); 
                } else if (et == short.class) { pStatement.setShort( col + 1, ((ShortArray)pa).get(row)); 
                } else if (et == int.class) {   pStatement.setInt(   col + 1, pa.getInt(row)); 
                } else if (et == long.class) {  pStatement.setLong(  col + 1, pa.getLong(row)); 
                //for double and float, NaN is fine
                } else if (et == float.class) { pStatement.setFloat( col + 1, pa.getFloat(row));  
                } else if (et == double.class) {pStatement.setDouble(col + 1, pa.getDouble(row)); 
                } else if (et == String.class || et == char.class) {
                    pStatement.setString(col + 1, pa.getString(row));  //null is ok
                } else throw new SimpleException(errorInMethod + "Process column(" + 
                    col + ") unknown type=" + pa.elementClassString()); 
            }

            //add this row's data to batch
            pStatement.addBatch();
        }

        //try to executeBatch 
        //setAutoCommit(false) seems to perform an implicit postgresql 
        //BEGIN command to start a transaction.
        //(Do this after all preparation in case exception thrown there.)
        con.setAutoCommit(false); //must be false for rollback to work
        int[] updateCounts = null;
        Exception caughtException = null;
        try {
            //NOTE that I don't try/catch the stuff above.
            //  If exception in preparation, no need to roll back
            //  (and don't want to roll back previous statement).
            //  But exception in executeBatch needs to be rolled back.

            //process the batch
            updateCounts = pStatement.executeBatch();

            //got here without exception? save the changes
            con.commit();

        } catch (Exception e) {          
            //get the caught exception
            caughtException = e; 
            try {
                if (e instanceof BatchUpdateException) {
                    //If e was BatchUpdateException there is additional information. 
                    //Try to combine the e exception (identifies bad row's data) 
                    //and bue.getNextException (says what the problem was).
                    Exception bue2 = ((BatchUpdateException)e).getNextException(); 
                    caughtException = new Exception(
                        errorInMethod +
                        "[BU ERROR] " + MustBe.throwableToString(e) + 
                        "[BU ERROR2] " + bue2.toString());
                } else {
                }
            } catch (Exception e2) {
                //oh well, e is best I can get
            }

            //since there was a failure, try to rollback the whole transaction.
            try {
                con.rollback();
            } catch (Exception rbe) {
                //hopefully won't happen
                caughtException = new Exception(
                    errorInMethod +
                    "[C ERROR] " + MustBe.throwableToString(caughtException) + 
                    "[RB ERROR] " + rbe.toString());
                String2.log(String2.ERROR + " in Table.saveAsSql during rollback:\n" + 
                    MustBe.throwableToString(caughtException));
            }
        } 
        
        //other clean up
        try {
            //pStatement.close(); //not necessary?
            //go back to autoCommit; this signals end of transaction
            con.setAutoCommit(true); 
        } catch (Exception e) {
            //small potatoes
            String2.log(errorInMethod + "During cleanup:\n" + 
                MustBe.throwableToString(e));
        }

        //rethrow the big exception, so caller knows there was trouble
        if (caughtException != null)
            throw caughtException;

        //all is well, print diagnostics
        if (verbose) {
            IntArray failedRows = new IntArray();
            for (int i = 0; i < updateCounts.length; i++) {
                if (updateCounts[i] != 1) {
                    failedRows.add(i);
                }
            }
            if (failedRows.size() > 0) 
                String2.log("  failedRows(0..)=" + failedRows.toString());
            String2.log(
                "  Table.saveAsSql done. nColumns=" + nColumns() +
                " nRowsSucceed=" + (nRows - failedRows.size()) + 
                " nRowsFailed=" + (failedRows.size()) + 
                " TIME=" + (System.currentTimeMillis() - elapsedTime));
        }
    }


    /**
     * This returns a list of schemas (subdirectories of this database) e.g., "public" 
     *
     * @return a list of schemas (subdirectories of this database) e.g., "public".
     *   Postgres always returns all lowercase names.
     * @throws Exception if trouble
     */
    public static StringArray getSqlSchemas(Connection con) throws Exception {

        DatabaseMetaData dm = con.getMetaData();
        Table schemas = new Table();
        schemas.readSqlResultSet(dm.getSchemas());
        return (StringArray)schemas.getColumn(0);
    }

    /**
     * This returns a list of tables of a certain type or types.
     *
     * @param con
     * @param schema a specific schema (a subdirectory of the database) e.g., "public" (not null)
     * @param types  null (for any) or String[] of one or more of 
     *   "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
     * @return a StringArray of matching table names.
     *   Postgres always returns all lowercase names.
     * @throws Exception if trouble
     */
    public static StringArray getSqlTableNames(Connection con, String schema,
        String types[]) throws Exception {

        if (schema == null)
            throw new SimpleException(String2.ERROR + " in Table.getSqlTableList: schema is null.");

        //getTables(catalogPattern, schemaPattern, tableNamePattern, String[] types)
        //"%" means match any substring of 0 or more characters, and 
        //"_" means match any one character. 
        //If a search pattern argument is set to null, that argument's criterion will be dropped from the search.
        DatabaseMetaData dm = con.getMetaData();
        Table tables = new Table();   //works with "posttest", "public", "names", null
        tables.readSqlResultSet(dm.getTables(null, schema.toLowerCase(), null, types));
        return (StringArray)(tables.getColumn(2)); //table name is always col (0..) 2
    }

    /**
     * Determines the type of a table (or if the table exists).
     *
     * @param schema a specific schema (a subdirectory of the database) e.g., "public" (not null)
     * @param tableName  a specific tableName (can't be null)
     * @return the table type:  
     *   "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM",
     *   or null if the table doesn't exist.
     * @throws Exception if trouble
     */
    public static String getSqlTableType(Connection con, String schema, 
        String tableName) throws Exception {

        if (schema == null)
            throw new SimpleException(String2.ERROR + " in Table.getSqlTableType: schema is null.");
        if (tableName == null)
            throw new SimpleException(String2.ERROR + " in Table.getSqlTableType: tableName is null.");

        //getTables(catalogPattern, schemaPattern, tableNamePattern, String[] types)
        //"%" means match any substring of 0 or more characters, and 
        //"_" means match any one character. 
        //If a search pattern argument is set to null, that argument's criterion will be dropped from the search.
        DatabaseMetaData dm = con.getMetaData();
        Table tables = new Table();   //works with "posttest", "public", "names", null
        tables.readSqlResultSet(dm.getTables(null, schema.toLowerCase(), 
            tableName.toLowerCase(), null));
        if (tables.nRows() == 0)
            return null;
        return tables.getStringData(3, 0); //table type is always col (0..) 3
    }

    /**
     * Drops (deletes) an sql table (if it exists).
     *
     * @param tableName a specific table name (not null),
     *    e.g., "myTable" (equivalent in postgres to "public.myTable")
     *    or "myShema.myTable".
     * @param cascade  This is only relevant if tableName is referenced by a view.
     *   If so, and cascade == true, the table will be deleted.
     *   If so, and cascade == false, the table will not be deleted.
     * @throws Exception if trouble (e.g., the table existed but couldn't be deleted,
     *    perhaps because connection's user doesn't have permission)
     */
    public static void dropSqlTable(Connection con, String tableName, boolean cascade) throws Exception {

        //create the statement and execute the query
        //DROP TABLE [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]
        Statement statement = con.createStatement();
        statement.executeUpdate("DROP TABLE IF EXISTS " + tableName + //case doesn't matter here
            (cascade? " CASCADE": "")); 
        statement.close();

    }

    /**
     * THIS IS NOT YET FINISHED.
     * This converts the specified String column with 
     * date (with e.g., "2006-01-02"), 
     * time (with e.g., "23:59:59"), 
     * or timestamp values (with e.g., "2006-01-02 23:59:59" with any character
     * between the date and time) 
     * into a double column with secondSinceEpoch (1970-01-01 00:00:00 UTC time zone).
     * No metadata is changed by this method.
     *
     * @param col the number of the column (0..) with the date, time, or timestamp strings.
     * @param type indicates the type of data in the column: 
     *    0=date, 1=time, 2=timestamp.
     * @param timeZoneOffset this identifies the time zone associated with  
     *    col (e.g., 0 if already UTC, -7 for California in summer (DST), 
     *    and -8 for California in winter, so that the data can be converted
     *    to UTC timezone. 
     * @param strict If true, this throws an exception if a value is
     *    improperly formatted. (Missing values of "" or null are allowed.)
     *    If false, improperly formatted values are silently converted to 
     *    missing values (Double.NaN).
     *    Regardless of 'strict', the method rolls components as needed,
     *    for example, Jan 32 becomes Feb 1.
     * @return the number of valid values.
     * @throws Exception if trouble (and no changes will have been made)
     */
    public int isoStringToEpochSeconds(int col, int type, 
        int timeZoneOffset, boolean strict) throws Exception {
       
        String errorInMethod = String2.ERROR + " in Table.isoStringToEpochSeconds(col=" + col + "):\n";
        Test.ensureTrue(type >= 0 && type <= 2, errorInMethod + "type=" + type +
            " must be between 0 and 2.");
        String isoDatePattern = "[1-2][0-9]{3}\\-[0-1][0-9]\\-[0-3][0-9]";
        String isoTimePattern = "[0-2][0-9]\\:[0-5][0-9]\\:[0-5][0-9]";
        String stringPattern = type == 0? isoDatePattern : 
            type == 1? isoTimePattern : isoDatePattern + "." + isoTimePattern;          
        Pattern pattern = Pattern.compile(stringPattern);
        StringArray sa = (StringArray)getColumn(col);
        int n = sa.size();
        DoubleArray da = new DoubleArray(n, true);
        int nGood = 0;
        int adjust = timeZoneOffset * Calendar2.SECONDS_PER_HOUR;
        for (int row = 0; row < n; row++) {
            String s = sa.get(row);

            //catch allowed missing values
            if (s == null || s.length() == 0) {
                da.array[row] = Double.NaN;            
                continue;
            }

            //catch improperly formatted values (stricter than Calendar2.isoStringToEpochSeconds below)
            if (strict && !pattern.matcher(s).matches()) 
                throw new SimpleException(errorInMethod + "value=" + s + " on row=" + row +
                    " is improperly formatted.");

            //parse the string
            if (type == 1) 
                s = "1970-01-01 " + s;
            double d = Calendar2.isoStringToEpochSeconds(s); //throws exception
            if (!Double.isNaN(d)) {
                nGood++;
                d -= adjust;
            }
            da.array[row] = d;            
        }
        setColumn(col, da);
        return nGood;
    }

/*   *
     * THIS IS NOT YET FINISHED.
     * This converts the specified double column with secondSinceEpoch 
     * (1970-01-01T00:00:00 UTC)
     * into a String column with 
     * date (with e.g., "2006-01-02"), 
     * time (with e.g., "23:59:59"), 
     * or timestamp values (with e.g., "2006-01-02 23:59:59").
     * No metadata is changed by this method.
     *
     * @param col the number of the column (0..) with the 
     *    secondsSinceEpoch double values.
     *    Double.NaN values are converted to missingValueString.
     * @param type indicates the desired type of data: 
     *    0=date, 1=time, 2=timestamp.
     * @param timeZoneOffset this identifies the time zone for the  
     *    new strings (e.g., 0 if already UTC, -7 for California in summer (DST), 
     *    and -8 for California in winter. 
     * @param missingValueString the string (usually null or "") 
     *    which will replace missing values (Double.NaN).
     * @return the number of valid values.
     * @throws Exception if trouble (and no changes will have been made)
     */
/*    public int epochSecondsToIsoString(int col, int type, int timeZoneOffset,
        String missingValueString) throws Exception {
       
        String errorInMethod = String2.ERROR + " in Table.epochSecondsToIsoString(col=" + col + "):\n";
        Test.ensureTrue(type >= 0 && type <= 2, errorInMethod + "type=" + type +
            " must be between 0 and 2.");
        DoubleArray da = (DoubleArray)getColumn(col);
        int n = da.size();
        StringArray sa = new StringArray(n, true);
        int nGood = 0;
        int adjust = timeZoneOffset * Calendar2.SECONDS_PER_HOUR;
        for (int row = 0; row < n; row++) {
            double d = da.get(row);

            //catch missing values
//            if (!Math2.isFinite(s == null) || s.length() == 0) {
//                sa.array[row] = missingValueString;            
//                continue;
//            }

            //format d
//            String s = d.array[row] = Calendar2.epochSecondsToIsoStringSpace(d);
//            if (type == 0) {
//                d.array[row] = 
//            } else if (type == 1) {
//                d.array[row] = 
//            } else if (type == 1) {
//            }
        }
        setColumn(col, da);
        return nGood;

    }
*/
    /**
     * This tests the readSql and saveAsSql methods.
     * @throws Exception if trouble
     */
    public static void testSql() throws Exception {
        String2.log("\n*** testSql");
        verbose = true;
        reallyVerbose = true;

        //load the sql driver  (the actual driver .jar must be in the classpath)
        Class.forName("org.postgresql.Driver");

        //set up connection and query
        String url = "jdbc:postgresql://otter.pfeg.noaa.gov/posttest";  //database name
        String user = "postadmin";
        String password = String2.getPasswordFromSystemIn("Password for '" + user + "'? ");
        if (password.length() == 0) {
            String2.log("No password, so skipping the test.");
            return;
        }
        long tTime = System.currentTimeMillis();
        Connection con = DriverManager.getConnection(url, user, password);
        String2.log("getConnection time=" + (System.currentTimeMillis() - tTime)); //often 9s !

        DatabaseMetaData dm = con.getMetaData();
        String2.log("getMaxRowSize=" + dm.getMaxRowSize());  //1GB

        //get catalog info  -- has one col with name(s) of databases for this user
        //...

        //test getSqlSchemas
        StringArray schemas = getSqlSchemas(con);
        Test.ensureTrue(schemas.indexOf("public") >= 0, "schemas=" + schemas.toString());

        //sometimes: make names table
        if (false) {
            Table namesTable = new Table();
            namesTable.addColumn("id",         PrimitiveArray.factory(new int[]{1,2,3}));
            namesTable.addColumn("first_name", PrimitiveArray.factory(new String[]{"Bob", "Nate", "Nancy"}));
            namesTable.addColumn("last_name",  PrimitiveArray.factory(new String[]{"Smith", "Smith", "Jones"}));
            namesTable.saveAsSql( 
                con, true, //'true' tests dropSqlTable, too
                "names", 0, null, null, null, 2);       
        }

        //test getSqlTableNames
        StringArray tableNames = getSqlTableNames(con, "public", new String[]{"TABLE"});
        String2.log("tableNames=" + tableNames);
        Test.ensureTrue(tableNames.indexOf("names") >= 0, "tableNames=" + tableNames.toString()); 
        Test.ensureTrue(tableNames.indexOf("zztop") <  0, "tableNames=" + tableNames.toString()); //doesn't exist

        //test getSqlTableType
        Test.ensureEqual(getSqlTableType(con, "public", "names"), "TABLE", "");
        Test.ensureEqual(getSqlTableType(con, "public", "zztop"), null, ""); //doesn't exist


        //*** test saveAsSql  (create a table)    (this tests dropSqlTable, too)
        if (true) {
            String tempTableName = "TempTest";
            String dates[] = {"1960-01-02", "1971-01-02", null, "2020-12-31"};
            double dateDoubles[] = new double[4];
            String timestamps[] = {"1960-01-02 01:02:03", "1971-01-02 07:08:09", null, "2020-12-31 23:59:59"};
            double timestampDoubles[] = new double[4];
            String times[] = {"01:02:03", "07:08:09", null, "23:59:59"};
            for (int i = 0; i < 4; i++) {
                dateDoubles[i]      = dates[i]      == null? Double.NaN : Calendar2.isoStringToEpochSeconds(dates[i]);
                timestampDoubles[i] = timestamps[i] == null? Double.NaN : Calendar2.isoStringToEpochSeconds(timestamps[i]);
            }
            Table tempTable = new Table();
            tempTable.addColumn("uid",       PrimitiveArray.factory(new int[]{1,2,3,4}));
            tempTable.addColumn("short",     PrimitiveArray.factory(new short[]{-10, 0, Short.MAX_VALUE, 10}));
            //Math2.random makes this test different every time. ensures old table is dropped and new one created.
            tempTable.addColumn("int",       PrimitiveArray.factory(new int[]{Math2.random(1000), 0, Integer.MAX_VALUE, 20}));
            tempTable.addColumn("long",      PrimitiveArray.factory(new long[]{-30, 0, Long.MAX_VALUE, 30}));
            tempTable.addColumn("float",     PrimitiveArray.factory(new float[]{-44.4f, 0f, Float.NaN, 44.4f}));
            tempTable.addColumn("double",    PrimitiveArray.factory(new double[]{-55.5, 0, Double.NaN, 55.5}));
            tempTable.addColumn("string",    PrimitiveArray.factory(new String[]{"ab", "", null, "longer"}));
            tempTable.addColumn("date",      PrimitiveArray.factory(dateDoubles));
            tempTable.addColumn("timestamp", PrimitiveArray.factory(timestampDoubles));
            tempTable.addColumn("time",      PrimitiveArray.factory(times));
            tempTable.saveAsSql( 
                con, true, //'true' tests dropSqlTable, too
                tempTableName, 0, new int[]{7}, new int[]{8}, new int[]{9}, 1.5);       

            //test readSql  (read a table)
            Table tempTable2 = new Table();
            tempTable2.readSql(con, "SELECT * FROM " + tempTableName);
            Test.ensureEqual(tempTable, tempTable2, "");


            //*** test rollback: add data that causes database to throw exception
            tempTable2.setIntData(0, 0, 5); //ok
            tempTable2.setIntData(0, 1, 6); //ok
            tempTable2.setIntData(0, 2, 7); //ok
            tempTable2.setIntData(0, 3, 1); //not ok because not unique
            try { //try to add new tempTable2 to database table
                tempTable2.saveAsSql( 
                    con, false, //false, so added to previous data
                    tempTableName, 0, new int[]{7}, new int[]{8}, new int[]{9}, 1.5); 
                String2.log("Shouldn't get here."); Math2.sleep(60000);
            } catch (Exception e) {
                //this error is expected
                //make sure it has both parts of the error message
                String2.log("\nEXPECTED " + String2.ERROR + ":\n" + MustBe.throwableToString(e));
                Test.ensureTrue(e.toString().indexOf(
                    "PSQLException: " + String2.ERROR + 
                    ": duplicate key violates unique constraint \"temptest_pkey\"") >= 0, 
                    "(A) The error was: " + e.toString());
                Test.ensureTrue(e.toString().indexOf(
                    "java.sql.BatchUpdateException: Batch entry 3 INSERT INTO TempTest (") >= 0, 
                    "(B)The error was: " + e.toString());
            }

            //and ensure database was rolled back to previous state
            tempTable2.readSql(con, "SELECT * FROM " + tempTableName);
            Test.ensureEqual(tempTable, tempTable2, "");


            //*** test pre-execute errors: add data that causes saveAsSql to throw exception
            tempTable2.setIntData(0, 0, 5); //ok, so new rows can be added
            tempTable2.setIntData(0, 1, 6); //ok
            tempTable2.setIntData(0, 2, 7); //ok
            tempTable2.setIntData(0, 3, 8); //ok
            int timeCol = tempTable2.findColumnNumber("time");
            //invalid date will be caught before statement is fully prepared
            tempTable2.setStringData(timeCol, 3, "20.1/30"); //first 3 rows succeed, this should fail
            try { //try to add new tempTable2 to database table
                tempTable2.saveAsSql(
                    con, false, //false, so added to previous data
                    tempTableName, 0, new int[]{7}, new int[]{8}, new int[]{9}, 1.5); 
                String2.log("Shouldn't get here."); Math2.sleep(60000);
            } catch (Exception e) {
                //this error is expected
                //make sure it is the right error
                String2.log("\nEXPECTED " + String2.ERROR + ":\n" + MustBe.throwableToString(e));
                Test.ensureTrue(e.toString().indexOf(
                    "java.lang.RuntimeException: ERROR in Table.saveAsSql(TempTest):\n" +
                        "Time format must be " +
                        "HH:MM:SS. Bad value=20.1/30 in row=3 col=9") >= 0, 
                    "error=" + e.toString());
            }

            //and ensure it rolls back to previous state
            tempTable2.readSql(con, "SELECT * FROM " + tempTableName);
            Test.ensureEqual(tempTable, tempTable2, "");


            //*** test successfully add data (and ensure previous rollbacks worked
            //assign row numbers so new rows can be added (and so different from possibly added 5,6,7,8)
            tempTable2.setIntData(0, 0, 9); //ok, 
            tempTable2.setIntData(0, 1, 10); //ok
            tempTable2.setIntData(0, 2, 11); //ok
            tempTable2.setIntData(0, 3, 12); //ok
            tempTable2.saveAsSql(
                con, false, //false, so added to previous data
                tempTableName, 0, new int[]{7}, new int[]{8}, new int[]{9}, 1.5); 

            //and ensure result has 8 rows
            tempTable2.readSql(con, "SELECT uid, string FROM " + tempTableName); 
            Test.ensureEqual(tempTable2.getColumn(0).toString(), "1, 2, 3, 4, 9, 10, 11, 12", "");
            Test.ensureEqual(tempTable2.getColumn(1).toString(), "ab, , [null], longer, ab, , [null], longer", "");
        }

        //don't drop the table, so I can view it in phpPgAdmin

//how read just column names and types?   query that returns no rows???

    }


    /**
     * This is like the other saveAsTabbedASCII, but writes to a file.
     * The second line has units.
     *
     * @param fullFileName the complete file name (including directory and
     *    extension, usually ".asc").
     *    An existing file with this name will be overwritten.
     * @throws Exception 
     */
    public void saveAsTabbedASCII(String fullFileName) throws Exception {
        if (verbose) String2.log("Table.saveAsTabbedASCII " + fullFileName); 
        long time = System.currentTimeMillis();

        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        //open the file (before 'try'); if it fails, no temp file to delete
        OutputStream os = new FileOutputStream(fullFileName + randomInt);

        try {
            saveAsTabbedASCII(os);
            os.close();

            //rename the file to the specified name, instantly replacing the original file
            File2.rename(fullFileName + randomInt, fullFileName);

        } catch (Exception e) {
            os.close();
            File2.delete(fullFileName + randomInt);
            //delete any existing file
            File2.delete(fullFileName);


            throw new Exception(e);
        }

    }

    /**
     * Save this data as a tab-separated ASCII outputStream.
     * The second line has units.
     * This writes the lon values as they are currently in this table
     *    (e.g., +-180 or 0..360). 
     * If no exception is thrown, the data was successfully written.
     * NaN's are written as "NaN".
     *
     * @param outputStream There is no need for it to be buffered.
     *    Afterwards, it is flushed, not closed.
     * @throws Exception 
     */
    public void saveAsTabbedASCII(OutputStream outputStream) throws Exception {
        saveAsSeparatedAscii(outputStream, "\t", false);
    }

    /**
     * This is like the other saveAsCsvASCII, but writes to a file.
     *
     * @param fullFileName the complete file name (including directory and
     *    extension, usually ".csv"). 
     *    An existing file with this name will be overwritten.
     * @throws Exception 
     */
    public void saveAsCsvASCII(String fullFileName) throws Exception {
        if (verbose) String2.log("Table.saveAsCsvASCII " + fullFileName); 
        long time = System.currentTimeMillis();


        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        //open the file (before 'try'); if it fails, no temp file to delete
        OutputStream os = new FileOutputStream(fullFileName + randomInt);

        try {
            saveAsCsvASCII(os);
            os.close();

            //rename the file to the specified name, instantly replacing the original file
            File2.rename(fullFileName + randomInt, fullFileName);

        } catch (Exception e) {
            os.close();
            File2.delete(fullFileName + randomInt);
            //delete any existing file
            File2.delete(fullFileName);

            throw new Exception(e);
        }

    }

    /**
     * This returns the table as a CSV String.
     * The second line has units.
     * This is generally used for diagnostics, as the String will be large
     * if the table is large.
     */
    public String saveAsCsvASCIIString() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        saveAsCsvASCII(baos);
        return baos.toString();
    }

    /**
     * Save this data as a comma-separated ASCII file.
     * The second line has units.
     * This writes the lon values as they are currently in this table
     *    (e.g., +-180 or 0..360). 
     * If no exception is thrown, the data was successfully written.
     * NaN's are written as "NaN".
     *
     * @param outputStream There is no need for it to be buffered.
     *    Afterwards, it is flushed, not closed.
     * @throws Exception 
     */
    public void saveAsCsvASCII(OutputStream outputStream) throws Exception {
        saveAsSeparatedAscii(outputStream, ",", true);
    }

    /**
     * Save this data as a separated value ASCII outputStream.
     * The second line has units.
     * This writes the lon values as they are currently in this table
     *    (e.g., +-180 or 0..360). 
     * If no exception is thrown, the data was successfully written.
     * NaN's are written as "NaN".
     *
     * @param outputStream There is no need for it to be buffered.
     *    Afterwards, it is flushed, not closed.
     * @param separator  usually a tab or a comma
     * @param quoted if true, strings will be quoted if needed (see String2.quote).
     * @throws Exception 
     */
    public void saveAsSeparatedAscii(OutputStream outputStream, String separator,
        boolean quoted) throws Exception {

        //ensure there is data
        if (nRows() == 0) {
            throw new Exception(String2.ERROR + " in Table.saveAsSeparatedAscii:\n" + 
                MustBe.THERE_IS_NO_DATA);
        }

        long time = System.currentTimeMillis();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

        //write the column names   
        int nColumns = nColumns();
        if (columnNames != null && columnNames.size() == nColumns) { //isn't this always true???
            for (int col = 0; col < nColumns; col++) {
                //quoteIfNeeded converts carriageReturns/newlines to (char)166 //'¦'  (#166)
                String s = String2.quoteIfNeeded(quoted, getColumnName(col));
                writer.write(s);
                writer.write(col == nColumns -1? "\n" : separator);
            }
        }

        //write the units
        for (int col = 0; col < nColumns; col++) {
            String s = columnAttributes(col).getString("units");
            if (s == null) s = "";
            //quoteIfNeeded converts carriageReturns/newlines to (char)166) //'¦'  (#166)
            writer.write(String2.quoteIfNeeded(quoted, s));
            writer.write(col == nColumns -1? "\n" : separator);
        }

        //get columnTypes
        boolean isString[] = new boolean[nColumns];
        for (int col = 0; col < nColumns; col++) {
            isString[col] = getColumn(col).elementClass() == String.class;
        }

        //write the data
        int nRows = nRows();
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nColumns; col++) {
                String s = getColumn(col).getString(row);
                if (isString[col]) {
                    if (s == null)
                        s = "null";
                    //quoteIfNeeded converts carriageReturns/newlines to (char)166; //'¦'  (#166)
                    writer.write(String2.quoteIfNeeded(quoted, s));
                } else {
                    writer.write(s.length() == 0? "NaN" : s);
                }
                writer.write(col == nColumns -1? "\n" : separator);
            }
        }

        writer.flush(); //essential

        //diagnostic
        if (verbose)
            String2.log("  Table.saveAsSeparatedASCII done. TIME=" + 
                (System.currentTimeMillis() - time));

    }


    /**
     * Save this data in this table as a json file.
     * <br>Missing values should already be stored as NaNs (perhaps via convertToStandardMissingValues()).
     *
     * @param fileName the full file name
     * @param timeColumn the column number of the column that has 
     *    seconds since 1970-01-01 double data that should be saved as 
     *    an ISO string (or -1 if none)
     *    An existing file with this name will be overwritten.
     * @param writeUnits if true, columnUnits will be written with
     *    the "units" attribute for each column
     *    Note that timeColumn will have units "UTC".
     * @throws Exception (no error if there is no data)
     */
    public void saveAsJson(String fileName, int timeColumn, 
        boolean writeUnits) throws Exception {
        FileOutputStream fos = new FileOutputStream(fileName);
        saveAsJson(fos, timeColumn, writeUnits);
        fos.close();
    }

    /**
     * Save this file as a json string.
     * <br>This is usually just used for diagnostics, since the string might be very large.
     * <br>Missing values should already be stored as NaNs (perhaps via convertToStandardMissingValues()).
     * 
     * @param timeColumn
     * @param writeUnits
     */
    public String saveAsJsonString(int timeColumn, boolean writeUnits) throws Exception {
        StringWriter sw = new StringWriter();
        saveAsJson(sw, timeColumn, writeUnits);
        return sw.toString();
    }

    /**
     * Save this data in this table as a json outputStream.
     * <br>There is no standard way to do this (that I am aware of),
     *   so I just made one up.
     * <br>This writes the lon values as they are currently in this table
     *    (e.g., +-180 or 0..360). 
     * <br>If no exception is thrown, the data was successfully written.
     * <br>Missing values should already be stored as NaNs (perhaps via convertToStandardMissingValues()).
     * <br>NaN's are written as "null" (to match the json library standard).
     *
     * @param outputStream There is no need for it to be buffered.
     *    A UTF-8 OutputStreamWriter is generated from it temporarily.
     *    Afterwards, it is flushed, not closed.
     * @param timeColumn the column number of the column that has 
     *    seconds since 1970-01-01 double data that should be saved as 
     *    an ISO string (or -1 if none)
     * @param writeUnits if true, columnUnits will be written with
     *    the "units" attribute for each column
     *    Note that timeColumn will have units "UTC".
     * @throws Exception (no error if there is no data)
     */
    public void saveAsJson(OutputStream outputStream, int timeColumn, 
        boolean writeUnits) throws Exception {

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
        saveAsJson(writer, timeColumn, writeUnits);
    }

    /**
     * Save this table as a json writer.
     * <br>The writer is flushed, but not closed at the end.
     * <br>nRows and nColumns may be 0.
     * <br>Missing values should already be stored as NaNs (perhaps via convertToStandardMissingValues()).
     * <br>NaN's are written as "null" (to match the json library standard).
     */
    public void saveAsJson(Writer writer, int timeColumn, 
        boolean writeUnits) throws Exception {

        long time = System.currentTimeMillis();

        //write the column names   
        int nColumns = nColumns();
        int nRows = nRows();
        boolean isString[] = new boolean[nColumns];
        writer.write(
            "{\n" +
            "  \"table\": {\n" + //begin main structure
            "    \"columnNames\": [");
        for (int col = 0; col < nColumns; col++) {
            isString[col] = getColumn(col).elementClass() == String.class;
            writer.write(String2.toJson(getColumnName(col)));
            writer.write(col == nColumns - 1? "],\n" : ", ");
        }

        //write the types   
        writer.write("    \"columnTypes\": [");
        for (int col = 0; col < nColumns; col++) {
            String s = getColumn(col).elementClassString();
            if (col == timeColumn)
                s = "String"; //not "double"
            writer.write(String2.toJson(s));  //nulls written as: null
            writer.write(col == nColumns - 1? "],\n" : ", ");
        }

        //write the units   
        if (writeUnits) {
            writer.write("    \"columnUnits\": [");
            for (int col = 0; col < nColumns; col++) {
                String s = columnAttributes(col).getString("units");
                if (col == timeColumn)
                    s = "UTC"; //not "seconds since 1970-01-01..."
                writer.write(String2.toJson(s));  //nulls written as: null
                writer.write(col == nColumns - 1? "],\n" : ", ");
            }
        }

        //write the data
        writer.write("    \"rows\": [\n");
        for (int row = 0; row < nRows; row++) {
            writer.write("      ["); //beginRow
            for (int col = 0; col < nColumns; col++) {
                if (col == timeColumn) {
                    double d = getDoubleData(col, row);
                    String s = Double.isNaN(d)? "null" : 
                        "\"" + Calendar2.epochSecondsToIsoStringT(d) + "Z\"";
                    writer.write(s);
                } else if (isString[col]) {
                    String s = getStringData(col, row);
                    writer.write(String2.toJson(s));
                } else {
                    String s = getStringData(col, row);
                    //represent NaN as null? yes, that is what json library does
                    writer.write(s.length() == 0? "null" : s); 
                }
                if (col < nColumns - 1) writer.write(", "); 
            }
            writer.write(row < nRows - 1? "],\n" : "]"); //endRow
        }       

        //end of big array
        writer.write(
            "\n" +
            "    ]\n" + //end of rows array
            "  }\n" + //end of table
            "}\n"); //end of main structure
        writer.flush(); 

        if (verbose) String2.log("Table.saveAsJson done. time=" + 
            (System.currentTimeMillis() - time));
    }

    /**
     * This reads data from json table (of the type written by saveAsJson).
     * <ul>
     * <li> If no exception is thrown, the file was successfully read.
     * <li> If there is a String column with units="UTC", the ISO 8601 values
     *    in the column are converted to doubles (seconds since 1970-01-01).   
     * </ul>
     *
     * @param fileName the full file name
     * @throws Exception if trouble
     */
    public void readJson(String fileName) throws Exception {
        String results[] = String2.readFromFile(fileName, "UTF-8", 2);
        if (results[0].length() > 0)
            throw new Exception(results[0]);
        readJson(fileName, results[1]);
    }


    /**
     * This reads data from json table (of the type written by saveAsJson).
     * This is a little stricter about the format that a JSON file has to be
     *  (e.g., columnNames/columnTypes/columnUnits/rowOfData, if present, must be on one line).
     * <ul>
     * <li> If no exception is thrown, the file was successfully read.
     * <li> If there is a String column with units="UTC", the ISO 8601 values
     *    in the column are converted to doubles (seconds since 1970-01-01).   
     * </ul>
     *
     * @param fileName for diagnostic messages only
     * @param source the json info
     * @throws Exception if trouble
     */
    public void readJson(String fileName, String source) throws Exception {
        if (verbose) String2.log("Table.readJson " + fileName); 
        long time = System.currentTimeMillis();
        String note = "In Table.readJson(" + fileName + "): ";
        String errorInMethod = String2.ERROR + " in Table.readJson(" + fileName + "):\n";

        //clear everything
        clear();
        JSONTokener tokener = new JSONTokener(source);
        char ch;
        String s, s2;
        String[] cNames = null, cTypes = null, cUnits = null;
        StringArray sa;
        int nCol = 0;
        PrimitiveArray pas[] = null;

//{
//  "table": {
//    "columnNames": ["longitude", "latitude", "time", "sea_surface_temperature"],
//    "columnTypes": ["float", "float", "String", "float"],
//    "columnUnits": ["degrees_east", "degrees_north", "UTC", "degree_C"],
//    "rows": [
//      [180.099, 0.032, "2007-10-04T12:00:00Z", 27.66],
//      [180.099, 0.032, null, null],
//      [189.971, -7.98, "2007-10-04T12:00:00Z", 29.08]
//    ]
//}
        if ((ch = tokener.nextClean()) != '{')     throw new IOException(errorInMethod + "Initial '{' not found (" + ch + ").");
        if ((ch = tokener.nextClean()) != '\"')    throw new IOException(errorInMethod + "\"table\" not found (" + ch + ").");
        if (!(s  = tokener.nextString('\"')).equals("table")) 
                                                   throw new IOException(errorInMethod + "\"table\" not found (" + s + ").");
        if ((ch = tokener.nextClean()) != ':')     throw new IOException(errorInMethod + "':' after \"table\" not found (" + ch + ").");
        if ((ch = tokener.nextClean()) != '{')     throw new IOException(errorInMethod + "'{' after \"table\": not found (" + ch + ").");
        ch = tokener.nextClean();
        while (ch == '\"') {
            s = tokener.nextString('\"');
            if ((ch = tokener.nextClean()) != ':') throw new IOException(errorInMethod + "':' after \"" + s + "\" not found (" + ch + ").");
            if ((ch = tokener.nextClean()) != '[') throw new IOException(errorInMethod + "'{' after \"" + s + "\": not found (" + ch + ").");
            if (s.equals("columnNames")) {
                s2 = tokener.nextTo('\n'); //assumes all content is on this line!
                if (!s2.endsWith("],")) throw new IOException(errorInMethod + "columnNames line should end with '],' (" + s2 + ").");
                cNames = StringArray.arrayFromCSV(s2.substring(0, s2.length() - 2));

            } else if (s.equals("columnTypes")) {
                s2 = tokener.nextTo('\n'); //assumes all content is on this line!
                if (!s2.endsWith("],")) throw new IOException(errorInMethod + "columnTypes line should end with '],' (" + s2 + ").");
                cTypes = StringArray.arrayFromCSV(s2.substring(0, s2.length() - 2));

            } else if (s.equals("columnUnits")) {
                s2 = tokener.nextTo('\n'); //assumes all content is on this line!
                if (!s2.endsWith("],")) throw new IOException(errorInMethod + "columnUnits line should end with '],' (" + s2 + ").");
                cUnits = StringArray.arrayFromCSV(s2.substring(0, s2.length() - 2));
                for (int i = 0; i < cUnits.length; i++)
                    if ("null".equals(cUnits[i]))
                        cUnits[i] = null;

            } else if (s.equals("rows")) {
                //build the table
                nCol = cNames.length;
                if (cTypes != null && cTypes.length != nCol) throw new IOException(errorInMethod + "columnTypes size=" + cTypes.length + " should be " + nCol + ".");
                if (cUnits != null && cUnits.length != nCol) throw new IOException(errorInMethod + "columnUnits size=" + cUnits.length + " should be " + nCol + ".");
                pas = new PrimitiveArray[nCol];
                //need isString since null in numeric col is NaN, but null in String col is the word null.
                boolean isString[] = new boolean[nCol]; //all false  (includes UTC times -- initially Strings)
                boolean isUTC[] = new boolean[nCol]; //all false
                for (int col = 0; col < nCol; col++) {
                    Class elementClass = cTypes == null? String.class : PrimitiveArray.elementStringToClass(cTypes[col]);
                    isString[col] = elementClass == String.class;
                    Attributes atts = new Attributes();
                    if (cUnits != null) {
                        isUTC[col] = isString[col] && "UTC".equals(cUnits[col]);
                        if (isUTC[col]) {
                            elementClass = double.class;
                            cUnits[col] = Calendar2.SECONDS_SINCE_1970;
                        }
                        atts.add("units", cUnits[col]);
                    }
                    pas[col] = PrimitiveArray.factory(elementClass, 128, false);
                    addColumn(col, cNames[col], pas[col], atts);
                }

                //read the rows of data
                //I can use StringArray.arrayFromCSV to process a row of data 
                //  even though it doesn't distinguish a null String from the word "null" in a String column
                //  because ERDDAP never has null Strings (closest thing is "").
                //  If this becomes a problem, switch to grabbing the nCol items separately
                //     utilizing the specifics of the json syntax.
                while ((ch = tokener.nextClean()) == '[') {
                    s2 = tokener.nextTo('\n'); //assumes all content is on this line!
                    if (s2.endsWith("],"))
                        s2 = s2.substring(0, s2.length() - 2);
                    else if (s2.endsWith("]"))
                        s2 = s2.substring(0, s2.length() - 1);
                    else throw new IOException(errorInMethod + "JSON syntax error (missing final ']'?) on data row #" + pas[0].size() + ".");
                    String sar[] = StringArray.arrayFromCSV(s2);
                    if (sar.length != nCol)
                        throw new IOException( errorInMethod + "JSON syntax error (incorrect number of data values?) on data row #" + pas[0].size() + ".");
                    for (int col = 0; col < nCol; col++) {
                        String ts = sar[col];
                        if (isUTC[col]) 
                            pas[col].addDouble(Calendar2.safeIsoStringToEpochSeconds(ts)); //returns NaN if trouble

                        //For both null and "null, arrayFromCSV returns "null"!
                        //String columns will treat null (shouldn't be any) and "null" as the word "null", 
                        //  numeric columns will treat null as NaN.  So all is well.
                        else pas[col].addString(ts); 
                    }
                }

                //after last data row, should be ]
                if (ch != ']') throw new IOException(errorInMethod + "']' not found after all rows of data (" + ch + ").");

            } else {
                String2.log(note + "Unexpected \"" + s + "\".");
                //it better all be on this line.
            }
            ch = tokener.nextClean();
        }

        //current ch should be final }, but don't insist on it.

        //simplify
        if (cTypes == null)
            simplify();

        /*
        //convert times to epoch seconds  (after simplify, so dates are still Strings)
        int tnRows = nRows();
        if (cUnits != null) {
            for (int col = 0; col < nCol; col++) {
                String ttUnits = cUnits[col];
                if ((pas[col] instanceof StringArray) && 
                    ttUnits != null && ttUnits.equals("UTC")) {
                    sa = (StringArray)pas[col];
                    DoubleArray da = new DoubleArray(tnRows, false);
                    for (int row = 0; row < tnRows; row++) {
                        String iso = sa.get(row);
                        da.add((iso == null || iso.length() == 0)? 
                            Double.NaN : 
                            Calendar2.isoStringToEpochSeconds(iso));
                    }
                    setColumn(col, da);
                    columnAttributes(col).set("units", Calendar2.SECONDS_SINCE_1970);
                }
            }
        }*/

        //String2.log(" place3 nColumns=" + nColumns() + " nRows=" + nRows() + " nCells=" + (nColumns() * nRows()));
        //String2.log(toString("row", 10));
        if (verbose) String2.log("  Table.readJson done. nColumns=" + nColumns() +
            " nRows=" + nRows() + " TIME=" + (System.currentTimeMillis() - time));
    }

    /* *
     * This reads data from json table (of the type written by saveAsJson).
     * <ul>
     * <li> If no exception is thrown, the file was successfully read.
     * <li> If "columnTypes" is not supplied, the columns are simplified.
     * <li> If there is a String column with units="UTC", the ISO 8601 values
     *    in the column are converted to doubles (seconds since 1970-01-01).   
     * </ul>
     *
     * @param fileName for diagnostic messages only
     * @param source the json info
     * @throws Exception if trouble
     */
/* //This retired 2010-10-13.  It works, but new version above is faster
  public void readJson(String fileName, String source) throws Exception {

        //validate parameters
        if (verbose) String2.log("Table.readJson " + fileName); 
        long time = System.currentTimeMillis();
        String errorInMethod = String2.ERROR + " in Table.readJson(" + fileName + "):\n";

        //clear everything
        clear();

        //for now, do it the simple but memory-expensive way
        //There will be 3 copies of data in memory: source String, json objects, Table!
        //Math2.gc(500); String2.log("readJson start " + Math2.memoryString());
        //long tTime = System.currentTimeMillis();
        JSONObject mainObject = new JSONObject(source);
        //String2.log("  json main time=" + (System.currentTimeMillis() - tTime));  //all the JSON time is here: ~8000ms
        //Math2.gc(500); String2.log("  place1 " + Math2.memoryString()); //max memory usage reached here
        //tTime = System.currentTimeMillis();
        JSONObject tableObject = mainObject.getJSONObject("table");
        //String2.log("  json table time=" + (System.currentTimeMillis() - tTime));
        //Math2.gc(500); String2.log("  place2 " + Math2.memoryString());
    
        //read the parts of the table
        JSONArray tNames = tableObject.getJSONArray("columnNames");
        int nColumns = tNames.length();
        JSONArray tTypes = tableObject.optJSONArray("columnTypes");
        JSONArray tUnits = tableObject.optJSONArray("columnUnits");
        //tTime = System.currentTimeMillis();
        JSONArray tData = tableObject.getJSONArray("rows");
        //String2.log("  json rows time=" + (System.currentTimeMillis() - tTime));
        int nRows = tData.length();

        //create the table
        boolean isStringCol[] = new boolean[nColumns];
        //Math2.gc(500); String2.log(" place3 " + Math2.memoryString());
        for (int col = 0; col < nColumns; col++) {
            addColumn(tNames.getString(col), 
                tTypes == null? new StringArray(nRows, false) : 
                PrimitiveArray.factory(PrimitiveArray.elementStringToClass(tTypes.getString(col)), nRows, false));
            isStringCol[col] = getColumn(col) instanceof StringArray;
            if (tUnits != null) {
                String ttUnits = tUnits.isNull(col)? null : tUnits.getString(col);
                if (ttUnits != null && ttUnits.length() > 0)
                    columnAttributes(col).add("units", ttUnits);
            }
        }

        //read the data
        //long rTime = System.currentTimeMillis();
        for (int row = 0; row < nRows; row++) {
            JSONArray rowData = tData.getJSONArray(row);
            //if (row % 1000 == 0) {
            //    String2.log("row=" + row + " time=" + (System.currentTimeMillis() - rTime) + " ms");
            //    rTime = System.currentTimeMillis();
            //}
    
            for (int col = 0; col < nColumns; col++) {
                if (rowData.isNull(col)) { //apparently, you have to ask or you get String value "null"
                    if (isStringCol[col])
                         getColumn(col).addString("");
                    else getColumn(col).addDouble(Double.NaN);
                } else {
                    if (isStringCol[col])
                         getColumn(col).addString(rowData.getString(col));
                    else getColumn(col).addDouble(rowData.getDouble(col));
                }
            }
        }
        //Math2.gc(500); String2.log(" place4 " + Math2.memoryString());

        //simplify
        if (tTypes == null)
            simplify();

        //convert times to epoch seconds  (after simplify, so dates are still Strings)
        for (int col = 0; col < nColumns; col++) {
            String ttUnits = columnAttributes(col).getString("units");
            if ((getColumn(col) instanceof StringArray) && 
                ttUnits != null && ttUnits.equals("UTC")) {
                StringArray sa = (StringArray)getColumn(col);
                DoubleArray da = new DoubleArray(nRows, false);
                for (int row = 0; row < nRows; row++) {
                    String iso = sa.get(row);
                    da.add((iso == null || iso.length() == 0)? 
                        Double.NaN : 
                        Calendar2.isoStringToEpochSeconds(iso));
                }
                setColumn(col, da);
                columnAttributes(col).set("units", Calendar2.SECONDS_SINCE_1970);
            }
        }
        //String2.log(" place3 nColumns=" + nColumns() + " nRows=" + nRows() + " nCells=" + (nColumns() * nRows()));
        //String2.log(toString("row", 10));
        if (verbose) String2.log("  Table.readJson done. nColumns=" + nColumns() +
            " nRows=" + nRows() + " TIME=" + (System.currentTimeMillis() - time));

    }
*/
    /* *
     * This reads data from json table (of the type written by saveAsJson).
     * <ul>
     * <li> If no exception is thrown, the table was successfully read.
     * <li> If "columnTypes" is not supplied, the columns are simplified.
     * <li> If there is a String column with units="UTC", the ISO 8601 values
     *    in the column are converted to doubles (seconds since 1970-01-01).   
     * </ul>
     *
     * @param fileName for diagnostic messages only
     * @param in the json info
     * @throws Exception if trouble
     */
/*    public void readJson(String fileName, Reader in) throws Exception {
/ *
{
  "table": {
    "columnNames": ["longitude", "latitude", "time", "sea_surface_temperature"],
    "columnTypes": ["float", "float", "String", "float"],
    "columnUnits": ["degrees_east", "degrees_north", "UTC", "degree_C"],
    "rows": [
      [180.099, 0.032, "2007-10-04T12:00:00Z", 27.66],
      [180.099, 0.032, null, null],
      [189.971, -7.98, "2007-10-04T12:00:00Z", 29.08]
    ]
  }
}
* /
        //validate parameters
        if (verbose) String2.log("Table.readJson " + fileName); 
        long time = System.currentTimeMillis();
        String errorInMethod = String2.ERROR + " in Table.readJson(" + fileName + "):\n";

        //clear everything
        clear();
        boolean isStringCol[];
        boolean isIntCol[];
        int nColumns = -1;

        //
        ParseJSON parseJson = new ParseJSON(in);
        parseJson.readExpected('{');
        parseJson.readExpected('"');
        Test.ensureEqual(parseJson.readString('"'),  "table", errorInMethod + "Missing \"table\"." + parseJson.onLine());
        parseJson.readExpected(':');
        parseJson.readExpected('{');
not finished
touble: because table is JsonObject, info may not be in expected order
  //it will for tables I write, but technically, not all valid tables
        int chi = parseJson.readNonWhiteChar();
        while (chi == '"') {
            String what = parseJson.readString('"');
            if (what.equals("columnNames")) {
                parseJson.readExpected(':');
                ArrayList tNames = parseJson.readPrimitiveArray();
                for (int col = 0; col < tNames.size(); col++) {
                    addColumn(tNames.get(col).toString(), new StringArray());
                }
                isStringCol = new boolean[tNames.size()];
                Arrays.fill(isStringCol, true);

            } else if (what.equals("columnTypes")) {
                parseJson.readExpected(':');
                ArrayList tTypes = parseJson.readPrimitiveArray();
                for (int col = 0; col < nColumns; col++) {
                    addColumn(tNames.get(col).toString(), 
                        PrimitiveArray.factory(PrimitiveArray.elementStringToClass(tTypes.get(col).toString()), 8, false));
                }
                isStringCol[col] = getColumn(col) instanceof StringArray;

            } else if (what.equals("columnUnits")) {
                parseJson.readExpected(':');
                ArrayList al = parseJson.readPrimitiveArray();
                for (int col = 0; col < nColumns; col++) {
                    if (sar[col] != null && sar[col].length() > 0) {
                        columnAttributes(col).add("units", sar[col]);
                    }
                }
            } else if (what.equals("rows")) {
                Test.ensureEqual(parseJson.readNonWhiteChar(), '[', 
                    errorInMethod + "Missing '[' after \"rows\"" + parseJson.onLine());
                chi = parseJson.readNonWhiteChar();
                while (chi == '[') {
                    //process a row

                    //next char is ',' or ']'
                    chi = parseJson.readNonWhiteChar();
                    if (chi != ',' && chi != ']')
                        throw new Exception("',' or ']' expected" + parseJson.onLine());

                } 
                Test.ensureEqual(chi, ']', errorInMethod + "Missing ']' after last row of \"rows\".");

            } else { //skip
            }

            chi = parseJson.readNonWhiteChar();
        }
        Test.ensureEqual(chi,                          '}', errorInMethod + "Missing '}' to close \"table\":{ ." + parseJson.onLine());
        Test.ensureEqual(parseJson.readNonWhiteChar(), '}', errorInMethod + "Missing final '}'."                 + parseJson.onLine());

        //read the data
        for (int row = 0; row < nRows; row++) {
            JSONArray rowData = tData.getJSONArray(row);
            for (int col = 0; col < nColumns; col++) {
                if (rowData.isNull(col)) { //apparently, you have to ask or you get String value "null"
                    if (isStringCol[col])
                         getColumn(col).addString("");
                    else getColumn(col).addDouble(Double.NaN);
                } else {
                    if (isStringCol[col])
                         getColumn(col).addString(rowData.getString(col));
                    else getColumn(col).addDouble(rowData.optDouble(col, Double.NaN));
                }
            }
        }

        //simplify
        if (tTypes == null)
            simplify();

        //convert times to epoch seconds  (after simplify, so dates are still Strings)
        for (int col = 0; col < nColumns; col++) {
            String ttUnits = columnAttributes(col).getString("units");
            if ((getColumn(col) instanceof StringArray) && 
                ttUnits != null && ttUnits.equals("UTC")) {
                StringArray sa = (StringArray)getColumn(col);
                DoubleArray da = new DoubleArray(nRows, false);
                for (int row = 0; row < nRows; row++) {
                    String iso = sa.get(row);
                    da.add((iso == null || iso.length() == 0)? 
                        Double.NaN : 
                        Calendar2.isoStringToEpochSeconds(iso));
                }
                setColumn(col, da);
                columnAttributes(col).set("units", Calendar2.SECONDS_SINCE_1970);
            }
        }
        if (verbose) String2.log("  Table.readJson done. nColumns=" + nColumns() +
            " nRows=" + nRows() + " TIME=" + (System.currentTimeMillis() - time));

    }
*/
    /**
     * This reads a table structure (everything but data) from an ERDDAP info url
     * for a TableDap dataset.
     * <ul>
     * <li> If no exception is thrown, the file was successfully read.
     * </ul>
     *
     * @param url e.g., http://127.0.0.1:8080/cwexperimental/info/pmelTaoDySst/index.json .
     *    It MUST be already percentEncoded as needed.
     * @throws Exception if trouble
     */
    public void readErddapInfo(String url) throws Exception {

        if (verbose) String2.log("Table.readJson " +url); 
        long time = System.currentTimeMillis();
        String errorInMethod = String2.ERROR + " in Table.readJson(" + url + "):\n";

        //clear everything
        clear();

        //read the json source
        //"columnNames": ["Row Type", "Variable Name", "Attribute Name", "Java Type", "Value"],
        Table infoTable = new Table();
        infoTable.readJson(url, SSR.getUrlResponseString(url));
        String tColNames = " column not found in colNames=" + String2.toCSSVString(infoTable.getColumnNames());
        int nRows = infoTable.nRows();
        int rowTypeCol = infoTable.findColumnNumber("Row Type");
        int variableNameCol = infoTable.findColumnNumber("Variable Name");
        int attributeNameCol = infoTable.findColumnNumber("Attribute Name");
        int javaTypeCol = infoTable.findColumnNumber("Data Type");
        int valueCol = infoTable.findColumnNumber("Value");
        Test.ensureTrue(rowTypeCol       != -1, errorInMethod + "'Row Type'" + tColNames);
        Test.ensureTrue(variableNameCol  != -1, errorInMethod + "'Variable Name'" + tColNames);
        Test.ensureTrue(attributeNameCol != -1, errorInMethod + "'Attribute Name'" + tColNames);
        Test.ensureTrue(javaTypeCol      != -1, errorInMethod + "'Data Type'" + tColNames);
        Test.ensureTrue(valueCol         != -1, errorInMethod + "'Value'" + tColNames);
        for (int row = 0; row < nRows; row++) {
            String rowType = infoTable.getStringData(rowTypeCol, row);
            String variableName = infoTable.getStringData(variableNameCol, row);
            String javaType = infoTable.getStringData(javaTypeCol, row);
            Class tClass = PrimitiveArray.elementStringToClass(javaType);
            if (rowType.equals("attribute")) {
                String attributeName = infoTable.getStringData(attributeNameCol, row);
                String value = infoTable.getStringData(valueCol, row);
                PrimitiveArray pa = tClass == String.class? 
                    new StringArray(String2.splitNoTrim(value, '\n')) : 
                    PrimitiveArray.csvFactory(tClass, value);
                if (variableName.equals("GLOBAL") || variableName.equals("NC_GLOBAL")) 
                    globalAttributes.add(attributeName, pa);
                else columnAttributes(variableName).add(attributeName, pa);
            } else if (rowType.equals("variable")) {
                addColumn(variableName, PrimitiveArray.factory(tClass, 1, false));
            } else throw new Exception("Unexpected rowType=" + rowType);
        }
    }

    /**
     * This mimics the a simple directory listing web page created by Apache.
     * <br>It mimics http://www.ngdc.noaa.gov/metadata/published/NOAA/NESDIS/NGDC/MGG/Hazard_Photos/fgdc/xml/
     * stored on Bob's computer as f:/programs/apache/listing.html
     * <br>The URL for this page MUST be a directoryURL ending in '/', or the links don't work!
     * <br>This just writes the part inside the 'body' tag.
     * <br>If there is a parentDirectory, its link will be at the top of the list.
     * <br>The table need not be sorted initially. This method handles sorting.
     * <br>The table should have 4 columns: "Name" (String), "Last modified" (long), 
     *    "Size" (long), and "Description" (String)
     * <br>The displayed Last Modified time will be some Zulu timezone.
     * <br>The displayed size will be some number of bytes, or truncated to some
     *    number of K (1024), M (1024^2), G (1024^3), or T (1024^4), 
     *
     * @param showUrlDir the part of the URL directory name to be displayed (with trailing slash).
     *    This is for display only.
     * @param userQuery  may be null. Still percent encoded.  
     *   <br>The parameter options are listed at 
     *    http://httpd.apache.org/docs/2.0/mod/mod_autoindex.html
     *   <br>C=N sorts the directory by file name
     *   <br>C=M sorts the directory by last-modified date, then file name
     *   <br>C=S sorts the directory by size, then file name
     *   <br>C=D sorts the directory by description, then file name
     *   <br>O=A sorts the listing in Ascending Order
     *   <br>O=D sorts the listing in Descending Order
     *   <br>F=0 formats the listing as a simple list (not FancyIndexed)
     *   <br>F=1 formats the listing as a FancyIndexed list
     *   <br>F=2 formats the listing as an HTMLTable FancyIndexed list
     *   <br>V=0 disables version sorting
     *   <br>V=1 enables version sorting
     *   <br>P=pattern lists only files matching the given pattern
     *   <br>The default is "C=N;O=A".
     *   <br>Currently, only C= and O= parameters are supported.
     * @param iconUrlDir the public URL directory (with slash at end) with the icon files
     * @param addParentDir if true, this shows a link to the parent directory
     * @param dirNames is the list of subdirectories in the directory (without trailing '/'). 
     *   It will be sorted within directoryListing.
     */
    public String directoryListing(String showUrlDir, String userQuery,
        String iconUrlDir, boolean addParentDir, 
        StringArray dirNames) throws Exception {

        int nameSpaces = 51; //with " " after it to ensure separation
        int dateSpaces = 17; //with " " after it to ensure separation
        int sizeSpaces = 5; //e.g., 1003, 1003K, 1003M, 1003G, 1003T, with "  "(!) after it

        //String2.log("Table.directoryListing("showUrlDir=" + showUrlDir +
        //    "\n  userQuery=" + userQuery + 
        //    "\n  iconUrlDir=" + iconUrlDir +
        //    "\n  nDirNames=" + dirNames.size() + " table.nRows=" + nRows());

        String xmlShowUrlDir = XML.encodeAsXML(showUrlDir);

        //ensure column names are as expected
        String ncssv = getColumnNamesCSSVString();
        String ncssvMust = "Name, Last modified, Size, Description";
        if (!ncssvMust.equals(ncssv))
             throw new SimpleException(
                String2.ERROR + " in directoryListing(), the table's column\n" +
                "names must be \"" + ncssvMust + "\".\n" +
                "The names are \"" + ncssv + "\".");
        PrimitiveArray namePA        = getColumn(0);
        PrimitiveArray modifiedPA    = getColumn(1);
        PrimitiveArray sizePA        = getColumn(2);
        PrimitiveArray descriptionPA = getColumn(3);

        //ensure column types are as expected
        String tcssv = getColumn(0).elementClassString() + ", " +
                       getColumn(1).elementClassString() + ", " +
                       getColumn(2).elementClassString() + ", " +
                       getColumn(3).elementClassString();
        String tcssvMust = "String, long, long, String";
        if (!tcssvMust.equals(tcssv))
             throw new SimpleException(
                String2.ERROR + " in directoryListing(), the table's column\n" +
                "types must be \"" + tcssvMust + "\".\n" +
                "The types are \"" + tcssv + "\".");

        //parse userQuery (e.g., C=N;O=A)  
        userQuery = userQuery == null? "" : 
            ";" + SSR.percentDecode(userQuery) + ";";  // ";" make it easy to search below
        int keyColumns[] =  //see definitions in javadocs above
            userQuery.indexOf(";C=N;") >= 0? new int[]{0, 1} : //2nd not needed, but be consistent
            userQuery.indexOf(";C=M;") >= 0? new int[]{1, 0} : 
            userQuery.indexOf(";C=S;") >= 0? new int[]{2, 0} : 
            userQuery.indexOf(";C=D;") >= 0? new int[]{3, 0} : 
                                             new int[]{0, 1};  
        boolean ascending[] =  //see definitions in javadocs above
            userQuery.indexOf(";O=D;") >= 0? new boolean[]{false, false} :
                                             new boolean[]{true, true};  
        //Order=A|D in column links will be 'A', 
        //  except currently selected column will offer !currentAscending
        char linkAD[] = {'A','A','A','A'};
        linkAD[keyColumns[0]] = ascending[0]? 'D' : 'A'; // !currentAscending
        

        //and sort the table (while lastModified and size are still the raw values) 
        sortIgnoreCase(keyColumns, ascending);


        //convert LastModified to string  (after sorting)
        int tnRows = nRows();
        StringArray newModifiedPA = new StringArray(tnRows, false);
        for (int row = 0; row < tnRows; row++) {
            String newMod = "";
            try {
                long tl = modifiedPA.getLong(row);
                newMod = tl == Long.MAX_VALUE? "" : 
                    Calendar2.formatAsDDMonYYYY(Calendar2.newGCalendarZulu(tl)).substring(0, dateSpaces); 
            } catch (Throwable t) {
            }
            newModifiedPA.add(newMod);
        }
        modifiedPA = newModifiedPA;

        //convert sizes
        StringArray newSizePA = new StringArray(tnRows, false);
        for (int row = 0; row < tnRows; row++) {
            String newSize = "";
            try {
                //lim ensures the displayed number will be lim ... lim*1000-1
               //(values of 1...lim-1 are not as precise)
                int lim = 6;
                long tl = sizePA.getLong(row);
                newSize = tl == Long.MAX_VALUE? "" :  
                    (tl >= lim * Math2.BytesPerPB? (tl / Math2.BytesPerPB) + "P" :
                     tl >= lim * Math2.BytesPerTB? (tl / Math2.BytesPerTB) + "T" :
                     tl >= lim * Math2.BytesPerGB? (tl / Math2.BytesPerGB) + "G" :
                     tl >= lim * Math2.BytesPerMB? (tl / Math2.BytesPerMB) + "M" :
                     tl >= lim * Math2.BytesPerKB? (tl / Math2.BytesPerKB) + "K" : tl + "");
            } catch (Throwable t) {
            }
            newSizePA.add(newSize);
        }
        sizePA = newSizePA;


//<pre>
//<img src="/icons/blank.gif" alt="Icon "> <a href="?C=N;O=D">Name</a>                                                <a href="?C=M;O=A">Last modified</a>      <a href="?C=S;O=A">Size</a>  <a href="?C=D;O=A">Description</a><hr><img src="/icons/back.gif" alt="[DIR]"> <a href="/published/NOAA/NESDIS/NGDC/MGG/Hazard_Photos/fgdc/">Parent Directory</a>                                                         -   
//<img src="/icons/text.gif" alt="[TXT]"> <a href="G01194.xml">G01194.xml</a>                                          05-Aug-2011 15:41   20K  

        //write showUrlDir
        StringBuilder sb = new StringBuilder();
        sb.append(
            "<h1>Index of " + XML.encodeAsXML(showUrlDir) + "</h1>\n");

        //write column names
        sb.append(
            "<pre><img src=\"" + iconUrlDir + "blank.gif\" alt=\"Icon \"> " + 
            "<a href=\"?C=N;O=" + linkAD[0] + "\">Name</a>"          + String2.makeString(' ', nameSpaces - 4)  + " " +
            "<a href=\"?C=M;O=" + linkAD[1] + "\">Last modified</a>" + String2.makeString(' ', dateSpaces - 13) + "  " +
            "<a href=\"?C=S;O=" + linkAD[2] + "\">Size</a>"          +                                            "  " +
            "<a href=\"?C=D;O=" + linkAD[3] + "\">Description</a>"   +
            "\n" + 
            "<hr>"); //listings I looked at didn't have \n here


        //display the directories
        dirNames.sortIgnoreCase();
        if (keyColumns[0] == 0 && !ascending[0])  //if sorted by Names, descending order
            dirNames.reverse();
        //if shown, parentDir always at top
        if (addParentDir && dirNames.indexOf("..") < 0) 
            dirNames.add(0, "..");
        int nDir = dirNames.size();
        for (int row = 0; row < nDir; row++) {
            try {
                String dirName = dirNames.get(row); 
                String showDirName = dirName;
                String xmlDirName = XML.encodeAsXML(dirName + 
                    (dirName.equals("..")? "" : "/"));
                String iconFile = "dir.gif"; //default
                String iconAlt  = "DIR";  //always 3 characters
                if (dirName.equals("..")) {
                    showDirName = "Parent Directory";
                    iconFile    = "back.gif";
                }
                sb.append(
                    "<img src=\"" + iconUrlDir + iconFile + "\" alt=\"[" + iconAlt + "]\" " +
                        "align=\"absbottom\"> " +
                    "<a href=\"" + xmlDirName + "\">" + XML.encodeAsXML(showDirName) + "</a>" +
                    String2.makeString(' ',  nameSpaces - showDirName.length()) + " " +
                    String2.left("", dateSpaces) + " " +
                    String2.right("- ", sizeSpaces) + "  \n"); 
            } catch (Throwable t) {
                String2.log(String2.ERROR + " for directoryListing(" +
                    showUrlDir + ")\n" +
                    MustBe.throwableToString(t));
            }
        }

        //define the file types  (should be in messages.xml?)
        //compressed and image ext from wikipedia
        //many ext from http://www.fileinfo.com/filetypes/common
        String binaryExt[] = {".accdb", ".bin", ".cab", ".cer", ".class", ".cpi", ".csr",
            ".db", ".dbf", ".dll", ".dmp", ".drv", ".dwg", ".dxf", ".fnt", ".fon", 
            ".ini", ".keychain", ".lnk", ".mat", ".mdb", ".mim", ".nc", 
            ".otf", ".pdb", ".prf", ".sys", ".ttf"};
        String compressedExt[] = {".7z", ".a", ".ace", ".afa", ".alz", ".apk", 
            ".ar", ".arc", ".arj", ".ba", ".bak", ".bh", ".bz2", 
            ".cab", ".cfs", ".cpio", ".dar", ".dd", ".deb", ".dgc", ".dmg", ".f",
            ".gca", ".gho", ".gz", 
            ".gzip", ".ha", ".hki", ".hqx", ".infl", ".iso", 
            ".j", ".jar", ".kgb", ".kmz", 
            ".lbr", ".lha", ".lz", ".lzh", ".lzma", ".lzo", ".lzx", 
            ".mar", ".msi", ".partimg", ".paq6", ".paq7", ".paq8", ".pea", ".pim", ".pit",
            ".pkg", ".qda", ".rar", ".rk", ".rpm", ".rz", 
            ".s7z", ".sda", ".sea", ".sen", ".sfark", ".sfx", ".shar", ".sit", ".sitx", ".sqx",
            ".tar", ".tbz2", ".tgz", ".tlz", ".toast", ".torrent",
            ".uca", ".uha", ".uue", ".vcd", ".war", ".wim", ".xar", ".xp3", ".xz", ".yz1", 
            ".z", ".zip", ".zipx", ".zoo"};
        String imageExt[] = {".ai", ".bmp", ".cgm", ".draw", ".drw", ".gif", 
            ".ico", ".jfif", ".jpeg", ".jpg", 
            ".pbm", ".pgm", ".png", ".pnm", ".ppm", ".pspimage", 
            ".raw", ".svg", ".thm", ".tif", ".tiff", ".webp", ".yuv"};
        String layoutExt[] = {".doc", ".docx", ".indd", ".key", ".pct",
            ".pps", ".ppt", ".pptx",
            ".psd", ".qxd", ".qxp", ".rels", ".rtf", ".wpd", ".wps",
            ".xlr", ".xls", ".xlsx"};  
        String movieExt[] = {".3g2", ".3gp", ".asf", ".asx", ".avi", ".fla", ".flv", 
            ".mov", ".mp4", ".mpg", ".rm", ".swf", ".vob", ".wmv"};
        String pdfExt[] = {".pdf"};
        String psExt[] = {".eps", ".ps"};
        String scriptExt[] = {  //or executable  
            ".app", ".asp", ".bat", ".cgi", ".com", ".csh", ".exe", ".gadget", ".js", ".jsp", 
            ".ksh", ".php", ".pif", ".pl", ".py", ".sh", ".tcsh", ".vb", ".wsf"};
        String soundExt[] = {".aif", ".iff", ".m3u", ".m4a", ".mid", 
            ".mp3", ".mpa", ".wav", ".wma"};
        String textExt[] = {".asc", ".c", ".cpp", ".cs", ".csv", ".das", ".dat", ".dds", 
            ".java", ".json", ".log", ".m", 
            ".sdf", ".sql", ".tsv", ".txt", ".vcf"};
        String worldExt[] = {".css", ".htm", ".html", ".xhtml"};
        String xmlExt[] = {".dtd", ".gpx", ".kml", ".xml", ".rss"};
        
        //display the files
        for (int row = 0; row < tnRows; row++) {
            try {
                String fileName = namePA.getString(row);
                String fileNameLC = fileName.toLowerCase();
                String xmlFileName = XML.encodeAsXML(fileName);

                String iconFile = "generic.gif"; //default
                String iconAlt  = "UNK";  //always 3 characters  (unknown)
                String extLC = File2.getExtension(fileName).toLowerCase();
                if (fileNameLC.equals("index.html") ||
                    fileNameLC.equals("index.htm")) {            
                    iconFile = "index.gif"; iconAlt = "IDX";
                } else if (String2.indexOf(binaryExt, extLC) >= 0) {
                    iconFile = "binary.gif"; iconAlt = "BIN";
                } else if (String2.indexOf(compressedExt, extLC) >= 0) {
                    iconFile = "compressed.gif"; iconAlt = "ZIP";
                } else if (String2.indexOf(imageExt, extLC) >= 0) {
                    iconFile = "image2.gif"; iconAlt = "IMG";
                } else if (String2.indexOf(layoutExt, extLC) >= 0) {
                    iconFile = "layout.gif"; iconAlt = "DOC";
                } else if (String2.indexOf(movieExt, extLC) >= 0) {
                    iconFile = "movie.gif"; iconAlt = "MOV";
                } else if (String2.indexOf(pdfExt, extLC) >= 0) {
                    iconFile = "pdf.gif"; iconAlt = "PDF";
                } else if (String2.indexOf(psExt, extLC) >= 0) {
                    iconFile = "ps.gif"; iconAlt = "PS ";
                } else if (String2.indexOf(scriptExt, extLC) >= 0) {
                    iconFile = "script.gif"; iconAlt = "EXE";
                } else if (String2.indexOf(soundExt, extLC) >= 0) {
                    iconFile = "sound.gif"; iconAlt = "SND";
                } else if (String2.indexOf(textExt, extLC) >= 0) {
                    iconFile = "text.gif"; iconAlt = "TXT";
                } else if (String2.indexOf(worldExt, extLC) >= 0) {
                    iconFile = "world1.gif"; iconAlt = "WWW";
                } else if (String2.indexOf(xmlExt, extLC) >= 0) {
                    iconFile = "xml.gif"; iconAlt = "XML";
                }

                sb.append(
                    "<img src=\"" + iconUrlDir + iconFile + "\" alt=\"[" + iconAlt + "]\" " +
                        "align=\"absbottom\"> " +
                    "<a href=\"" + xmlFileName + "\">" + xmlFileName + "</a>" +
                    String2.makeString(' ',  nameSpaces - fileName.length()) + " " +
                    String2.left(modifiedPA.getString(row), dateSpaces) + " " +
                    String2.right(sizePA.getString(row), sizeSpaces) + "  " +
                    XML.encodeAsXML(descriptionPA.getString(row)) + "\n"); 
            } catch (Throwable t) {
                String2.log(String2.ERROR + " for directoryListing(" +
                    showUrlDir + ")\n" +
                    MustBe.throwableToString(t));
            }
        }

        sb.append(
            "<hr></pre>\n" +
            nDir   + (nDir   == 1? " directory, " : " directories, ") + 
            tnRows + (tnRows == 1? " file "       : " files") + 
            "\n\n");
        return sb.toString();
    } 

    /**
     * Test saveAsJson and readJson.
     *
     * @throws Exception if trouble
     */
    public static void testJson() throws Exception {
        String2.log("\n***** Table.testJson");
        verbose = true;
        reallyVerbose = true;

        //generate some data    
        Table table = getTestTable(true, true);

        //write it to a file
        String fileName = testDir + "tempTable.json";
        table.saveAsJson(fileName, 0, true);
        //String2.log(fileName + "=\n" + String2.readFromFile(fileName)[1]);
        //SSR.displayInBrowser("file://" + fileName);

        //read it from the file
        String results[] = String2.readFromFile(fileName);
        Test.ensureEqual(results[0], "", "");
        Test.ensureEqual(results[1], 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"Time\", \"Longitude\", \"Latitude\", \"Double Data\", \"Long Data\", \"Int Data\", \"Short Data\", \"Byte Data\", \"String Data\"],\n" +
"    \"columnTypes\": [\"String\", \"int\", \"float\", \"double\", \"long\", \"int\", \"short\", \"byte\", \"String\"],\n" +
"    \"columnUnits\": [\"UTC\", \"degrees_east\", \"degrees_north\", \"doubles\", \"longs\", \"ints\", \"shorts\", \"bytes\", \"Strings\"],\n" +
"    \"rows\": [\n" +
"      [\"1970-01-01T00:00:00Z\", -3, 1.0, -1.0E300, -2000000000000000, -2000000000, -32000, -120, \"a\"],\n" +
"      [\"2005-08-31T16:01:02Z\", -2, 1.5, 3.123, 2, 2, 7, 8, \"bb\"],\n" +
"      [\"2005-11-02T18:04:09Z\", -1, 2.0, 1.0E300, 2000000000000000, 2000000000, 32000, 120, \"ccc\"]\n" +
"    ]\n" +
"  }\n" +
"}\n", 
        results[1]);

        //read it
        Table table2 = new Table();
        table2.readJson(fileName, String2.readFromFile(fileName)[1]);
        Test.ensureTrue(table.equals(table2), "");

        //finally 
        File2.delete(fileName);

        //******************* test readErddapInfo
        //String tUrl = "http://coastwatch.pfeg.noaa.gov/erddap2";
        //http://127.0.0.1:8080/cwexperimental/info/pmelTaoDySst/index.json
        String tUrl = "http://127.0.0.1:8080/cwexperimental";
        try {
            table.readErddapInfo(tUrl + "/info/pmelTaoDySst/index.json");
            String ncHeader = table.getNCHeader("row");
            Test.ensureEqual(table.globalAttributes.getString("cdm_data_type"), "TimeSeries", ncHeader);
            Test.ensureEqual(table.globalAttributes.getString("title"), 
                "TAO/TRITON, RAMA, and PIRATA Buoys, Daily, Sea Surface Temperature", 
                ncHeader);
            Test.ensureEqual(table.globalAttributes.get("history").size(), 2,  ncHeader);
            Test.ensureEqual(table.globalAttributes.get("history").getString(0), 
                "2012-06-04 Most recent downloading and reformatting of all " + //changes monthly
                "cdf/sites/... files from PMEL TAO's FTP site by bob.simons at noaa.gov.", 
                ncHeader);
            Test.ensureEqual(table.globalAttributes.get("history").getString(1), 
                "Since then, recent data has been updated every day.", 
                ncHeader);
            Test.ensureEqual(table.nColumns(), 8, ncHeader);
            Test.ensureEqual(table.findColumnNumber("longitude"), 1, ncHeader);
            Test.ensureEqual(table.columnAttributes(1).getString("units"), "degrees_east", ncHeader);
            int t25Col = table.findColumnNumber("T_25");
            Test.ensureTrue(t25Col > 0, ncHeader);
            Test.ensureEqual(table.columnAttributes(t25Col).getString("ioos_category"), "Temperature", ncHeader);
            Test.ensureEqual(table.columnAttributes(t25Col).getString("units"), "degree_C", ncHeader);

        } catch (Exception e) {
            String2.getStringFromSystemIn(
                MustBe.throwableToString(e) +
                "\nERROR while testing " + tUrl + ".  Press ^C to stop or Enter to continue..."); 

        }
    }

    /**
     * This saves the current table in some type of file.
     * If the file already exists, it is touched, and nothing else is done.
     *
     * @param fullFileName including directory and extension (e.g., ".asc"),
     *   but not including ".zip" if you want it zipped.
     * @param saveAsType one of the SAVE_AS constants.
     *   If SAVE_AS_4D_NC, it is assumed that column 1=lon, 2=lat, 3=depth, 4=time.
     * @param dimensionName usually "row", but e.g., may be "time", 
     *    "station", or "observation", or ...?
     * @param zipIt If true, creates a .zip file and deletes the
     *    intermediate file (e.g., .asc). If false, the specified
     *    saveAsType is created.
     * @throws Exception if trouble
     */
    public void saveAs(String fullFileName, 
            int saveAsType, String dimensionName, boolean zipIt) throws Exception {

        if (verbose) String2.log("Table.saveAs(name=" + fullFileName + " type=" + saveAsType + ")"); 
        if (saveAsType != SAVE_AS_TABBED_ASCII &&
            saveAsType != SAVE_AS_FLAT_NC &&
            saveAsType != SAVE_AS_4D_NC &&
            saveAsType != SAVE_AS_MATLAB
            )
            throw new RuntimeException(String2.ERROR + " in Table.saveAs: invalid saveAsType=" + saveAsType);
       
        String ext = SAVE_AS_EXTENSIONS[saveAsType];

        //does the file already exist?
        String finalName = fullFileName + (zipIt? ".zip" : "");
        if (File2.touch(finalName)) { 
            String2.log("Table.saveAs reusing " + finalName);
            return;
        }
     
        //save as ...
        long time = System.currentTimeMillis();
        if      (saveAsType == SAVE_AS_TABBED_ASCII) saveAsTabbedASCII(fullFileName);
        else if (saveAsType == SAVE_AS_FLAT_NC)      saveAsFlatNc(fullFileName, dimensionName);
        else if (saveAsType == SAVE_AS_4D_NC)        saveAs4DNc(fullFileName, 0, 1, 2, 3);
        else if (saveAsType == SAVE_AS_MATLAB)       saveAsMatlab(fullFileName, getColumnName(nColumns() - 1));

        if (zipIt) {
            //zip to a temporary zip file, -j: don't include dir info
            SSR.zip(         fullFileName + ".temp.zip",
                new String[]{fullFileName}, 20); 

            //delete the file that was zipped
            File2.delete(fullFileName); 

            //if all successful, rename to final name
            File2.rename(fullFileName + ".temp.zip", fullFileName + ".zip");
        }
    }

    /**
     * This reads an input table file (or 1- or 2-level opendap sequence)
     * and saves it in a file (optionally zipped).
     * A test which reads data from an opendap 1-level sequence and writes it to an .nc file: 
     * convert("http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?t0,oxygen&month=\"5\"", 2, testDir + "convert.nc", 1, false);
     *
     * @param inFullName  the full name of the file (with the extension .zip
     *    if it is zipped) or opendap sequence url (optionally with a query). 
     *    <ul>
     *    <li> If it is zipped, the data file should be the only file in the .zip file
     *      and the data file's name should be inFullName minus the directory 
     *      and the ".zip" at the end.
     *    <li> All of the data in the file will be read.
     *    <li> If the data is packed (e.g., scale_factor, add_offset), this will not unpack it.
     *    <li>ASCII files must have column names on the first line and data 
     *      starting on the second line.
     *       <ul>
     *       <li>The item separator on each line can be tab, comma, or 1 or more spaces.
     *       <li>Missing values for tab- and comma-separated files can be "" or "." or "NaN".
     *       <li>Missing values for space-separated files can be "." or "NaN".
     *       <li> All data rows must have the same number of data items. 
     *       <li> The data is initially read as Strings. Then columns are simplified
     *          (e.g., to doubles, ... or bytes) so they store the data compactly.
     *       <li> Currently, date strings are left as strings. 
     *       </ul>
     *    <li>For opendap sequence details, see readOpendapSequence  (query must already SSR.percentEncoded as needed).
     *    </ul>
     * @param inType the type of input file: READ_FLAT_NC, READ_ASCII, READ_OPENDAP_SEQUENCE
     * @param outFullName the full name of the output file (but without
     *    the .zip if you want it zipped).
     * @param outType the type of output file: SAVE_AS_MAT, SAVE_AS_TABBED_ASCII, 
     *    SAVE_AS_FLAT_NC, or SAVE_AS_4D_NC.
     * @param dimensionName usually "row", but, e.g., maybe "time",
     *     "station", or "observation", or ...?
     * @param zipIt true if you want the file to be zipped
     * @throws Exception if trouble
     */
    public static void convert(String inFullName, int inType,
        String outFullName, int outType, String dimensionName, boolean zipIt) throws Exception {

        if (verbose) 
            String2.log("Table.convert in=" + inFullName + " inType=" + inType +
            "  out=" + outFullName + " outType=" + outType + " zipIt=" + zipIt);

        //unzip inFullName
        boolean unzipped = inFullName.toLowerCase().endsWith(".zip"); 
        if (unzipped) {     
            String tempDir = File2.getSystemTempDirectory();
            if (verbose) String2.log("unzipping to systemTempDir = " + tempDir);
 
            SSR.unzip(inFullName, tempDir, true, 10000);
            inFullName = tempDir + 
                File2.getNameAndExtension(inFullName).substring(0, inFullName.length() - 4); //without .zip at end          
        }

        //read the file
        Table table = new Table();
        if (inType == READ_ASCII)
            table.readASCII(inFullName);
        else if (inType == READ_FLAT_NC)
            table.readFlatNc(inFullName, null, 0); //don't unpack
        else if (inType == READ_OPENDAP_SEQUENCE)
            table.readOpendapSequence(inFullName, false);
        else throw new Exception(String2.ERROR + " in Table.convert: unrecognized inType: " + inType);

        //if input file was unzipped, delete the unzipped file
        if (unzipped)
            File2.delete(inFullName);

        //save the file (and zipIt?)
        table.saveAs(outFullName, outType, dimensionName, zipIt);
    }


    /**
     * Test convert.
     */
    public static void testConvert() throws Exception {
        verbose = true;
        reallyVerbose = true;
        String url, fileName;
        Table table = new Table();

// /*
        //the original test from Roy
        //This is used as an example in various documentation. 
        //If url changes, do search and replace to change all references to it.
        url = "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?t0,oxygen&month=\"5\"";
        String2.log("\ntesting Table.convert \n url=" + url);
        fileName = testDir + "convertOriginal.nc";
        convert(url, READ_OPENDAP_SEQUENCE, fileName, SAVE_AS_FLAT_NC, "row", false);
        table.readFlatNc(fileName, null, 0); //should be already unpacked
        String2.log(table.toString("row", 3));
        Test.ensureEqual(table.nColumns(), 2, "");
        Test.ensureEqual(table.nRows(), 190, "");
        Test.ensureEqual(table.getColumnName(0), "t0", "");
        Test.ensureEqual(table.getColumnName(1), "oxygen", "");
        Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Temperature T0", "");
        Test.ensureEqual(table.columnAttributes(1).getString("long_name"), "Oxygen", "");
        Test.ensureEqual(table.getFloatData(0, 0), 12.1185f, "");
        Test.ensureEqual(table.getFloatData(0, 1), 12.1977f, "");
        Test.ensureEqual(table.getFloatData(1, 0), 6.56105f, "");
        Test.ensureEqual(table.getFloatData(1, 1), 6.95252f, "");
        File2.delete(fileName);
// */

// /*
        //The 8/16/06 test from osu
        //Test values below from html ascii request  (same as url below, but with .ascii before "?")
        //adcp95.yearday, adcp95.depth, adcp95.x, adcp95.y, adcp95.eastv
        //row0: 184.45120239257812, 22, -123.8656005859375, 48.287899017333984, -0.31200000643730164
        //last row: 185.99949645996094, 22, -125.98069763183594, 42.844200134277344, -0.15399999916553497
        url = "http://nwioos.coas.oregonstate.edu:8080/dods/drds/1995%20Hake%20Survey%20ADCP?ADCP95.yearday,ADCP95.Z,ADCP95.x,ADCP95.y,ADCP95.EV&ADCP95.yearday<186&ADCP95.Z<25";
        String2.log("\ntesting Table.convert \n url=" + url);
        fileName = testDir + "convertOSU.nc";
        convert(url, READ_OPENDAP_SEQUENCE, fileName, SAVE_AS_FLAT_NC, "row", false);
        table.readFlatNc(fileName, null, 0); //should be already unpacked
        String2.log(table.toString("row", 3));
        Test.ensureEqual(table.nColumns(), 5, "");
        Test.ensureEqual(table.nRows(), 446, "");
        Test.ensureEqual(table.getColumnName(0), "yearday", "");
        Test.ensureEqual(table.getColumnName(1), "Z", "");
        Test.ensureEqual(table.getColumnName(2), "x", "");
        Test.ensureEqual(table.getColumnName(3), "y", "");
        Test.ensureEqual(table.getColumnName(4), "EV", "");
        //no attributes
        Test.ensureEqual(table.getDoubleData(0, 0), 184.45120239257812, "");
        Test.ensureEqual(table.getDoubleData(1, 0), 22, "");
        Test.ensureEqual(table.getDoubleData(2, 0), -123.8656005859375, "");
        Test.ensureEqual(table.getDoubleData(3, 0), 48.287899017333984, "");
        Test.ensureEqual(table.getDoubleData(4, 0), -0.31200000643730164, "");
        Test.ensureEqual(table.getDoubleData(0, 445), 185.99949645996094, "");
        Test.ensureEqual(table.getDoubleData(1, 445), 22, "");
        Test.ensureEqual(table.getDoubleData(2, 445), -125.98069763183594, "");
        Test.ensureEqual(table.getDoubleData(3, 445), 42.844200134277344, "");
        Test.ensureEqual(table.getDoubleData(4, 445), -0.15399999916553497, "");
        File2.delete(fileName);
// */

// /*
        //The 8/17/06 test from cimt
        //Test values below from html ascii request  (same as url below, but with .ascii before "?")
        //vCTD.latitude, vCTD.longitude, vCTD.station, vCTD.depth, vCTD.salinity
        //first: 36.895, -122.082, "T101", 1.0, 33.9202
        //last: 36.609, -121.989, "T702", 4.0, 33.4914
        url = "http://cimt.dyndns.org:8080/dods/drds/vCTD?vCTD.latitude,vCTD.longitude,vCTD.station,vCTD.depth,vCTD.salinity&vCTD.depth<5";
        String2.log("\ntesting Table.convert \n url=" + url);
        fileName = testDir + "convertCIMT.nc";
        convert(url, READ_OPENDAP_SEQUENCE, fileName, SAVE_AS_FLAT_NC, "row", false);
        table.readFlatNc(fileName, null, 0); //should be already unpacked
        String2.log(table.toString("row", 3));
        Test.ensureEqual(table.nColumns(), 5, "");
        //Test.ensureEqual(table.nRows(), 1407, "");  //this changes; file is growing
        Test.ensureEqual(table.getColumnName(0), "latitude", "");
        Test.ensureEqual(table.getColumnName(1), "longitude", "");
        Test.ensureEqual(table.getColumnName(2), "station", "");
        Test.ensureEqual(table.getColumnName(3), "depth", "");
        Test.ensureEqual(table.getColumnName(4), "salinity", "");
        //no attributes
        Test.ensureEqual(table.getFloatData(0, 0), 36.895f, "");
        Test.ensureEqual(table.getFloatData(1, 0), -122.082f, "");
        Test.ensureEqual(table.getStringData(2, 0), "T101", "");
        Test.ensureEqual(table.getFloatData(3, 0), 1.0f, "");
        Test.ensureEqual(table.getFloatData(4, 0), 33.9202f, "");
        Test.ensureEqual(table.getFloatData(0, 1406), 36.609f, "");
        Test.ensureEqual(table.getFloatData(1, 1406), -121.989f, "");
        Test.ensureEqual(table.getStringData(2, 1406), "T702", "");
        Test.ensureEqual(table.getFloatData(3, 1406), 4.0f, "");
        Test.ensureEqual(table.getFloatData(4, 1406), 33.4914f, "");
        File2.delete(fileName);
// */
    }

    /**
     * This rearranges the columns to be by case-insensitive alphabetical colum name.
     *
     */
    public void sortColumnsByName() {
        //make lowercase lcColumnNames
        StringArray lcColumnNames = new StringArray();
        int n = nColumns();
        for (int i = 0; i < n; i++)
            lcColumnNames.add(columnNames.get(i).toLowerCase());

        //rank it
        ArrayList al = new ArrayList();
        al.add(lcColumnNames);
        int rank[] = PrimitiveArray.rank(al, new int[]{0}, new boolean[]{true});
        //String2.log("old colNames=" + columnNames);
        //String2.log("ranks=" + String2.toCSSVString(rank));

        //reorder
        ArrayList newColumns = new ArrayList();
        ArrayList newAttributes = new ArrayList();
        for (int i = 0; i < n; i++) {
            newColumns.add(getColumn(rank[i]));
            newAttributes.add(columnAttributes(rank[i]));
        }
        columnNames.reorder(rank);
        columns = newColumns;
        columnAttributes = newAttributes;
        //String2.log("new colNames=" + columnNames);
    }

    /** 
     * This tests sortColumnsByName.
     */
    public static void testSortColumnsByName() {
        verbose = true;
        reallyVerbose = true;
        String2.log("\n***** Table.testSortColumnsByName");
        Table table = getTestTable(true, true);
        table.setColumnName(2, "latitude"); //to test case-insensitive

        table.sortColumnsByName();

        //byte
        Test.ensureEqual(table.getColumnName(0), "Byte Data", "");
        Test.ensureEqual(table.columnAttributes(0).getString("units"), "bytes", "");

        //double
        Test.ensureEqual(table.getColumnName(1), "Double Data", "");
        Test.ensureEqual(table.columnAttributes(1).getString("units"), "doubles", "");

        //int
        Test.ensureEqual(table.getColumnName(2), "Int Data", "");
        Test.ensureEqual(table.columnAttributes(2).getString("units"), "ints", "");

        //Lat
        Test.ensureEqual(table.getColumnName(3), "latitude", "");
        Test.ensureEqual(table.columnAttributes(3).getString("units"), "degrees_north", "");

        //long  
        Test.ensureEqual(table.getColumnName(4), "Long Data", "");
        Test.ensureEqual(table.columnAttributes(4).getString("units"), "longs", "");

        //Lon
        Test.ensureEqual(table.getColumnName(5), "Longitude", "");
        Test.ensureEqual(table.columnAttributes(5).getString("units"), "degrees_east", "");

        //short
        Test.ensureEqual(table.getColumnName(6), "Short Data", "");
        Test.ensureEqual(table.columnAttributes(6).getString("units"), "shorts", "");

        //String
        Test.ensureEqual(table.getColumnName(7), "String Data", "");
        Test.ensureEqual(table.columnAttributes(7).getString("units"), "Strings", "");

        //Time
        Test.ensureEqual(table.getColumnName(8), "Time", "");
        Test.ensureEqual(table.columnAttributes(8).getString("units"), Calendar2.SECONDS_SINCE_1970, "");


    }

    /**
     * Make a test Table.
     *
     * @param includeLongs set this to true if you want a column with longs
     * @param includeStrings set this to true if you want a column with Strings
     * @return Table
     */
    public static Table getTestTable(boolean includeLongs, boolean includeStrings) {

        String testTimes[] = {"1970-01-01T00:00:00", "2005-08-31T16:01:02", "2005-11-02T18:04:09"};
        Table table = new Table();
        int nRows = testTimes.length;

        //global attributes
        table.globalAttributes().set("global_att1", "a string");
        table.globalAttributes().set("global_att2", new IntArray(new int[]{1,100}));

        //add the data variables (and their attributes)
        ArrayList variableAttributes;

        //0=seconds
        double[] ad = new double[nRows];
        for (int i = 0; i < nRows; i++)
            ad[i] = Calendar2.isoStringToEpochSeconds(testTimes[i]);
        int col = table.addColumn("Time", new DoubleArray(ad));
        table.columnAttributes(col).set("units", Calendar2.SECONDS_SINCE_1970);
        table.columnAttributes(col).set("time_att2", new DoubleArray(new double[]{-1e300, 1e300}));

        //1=lon
        int[] ai = {-3, -2, -1};
        col = table.addColumn("Longitude", new IntArray(ai));
        table.columnAttributes(col).set("units", "degrees_east");
        table.columnAttributes(col).set("lon_att2", new IntArray(new int[]{-2000000000, 2000000000}));

        //2=lat
        float[] af = {1, 1.5f, 2};
        col = table.addColumn("Latitude", new FloatArray(af));
        table.columnAttributes(col).set("units", "degrees_north");
        table.columnAttributes(col).set("lat_att2", new FloatArray(new float[]{-1e30f, 1e30f}));

        //3=double
        ad = new double[]{-1e300, 3.123, 1e300};
        col = table.addColumn("Double Data", new DoubleArray(ad));
        table.columnAttributes(col).set("units", "doubles");
        table.columnAttributes(col).set("double_att2", new DoubleArray(new double[]{-1e300, 1e300}));

        //4=long  
        if (includeLongs) {
            long[] al = {-2000000000000000L, 2, 2000000000000000L};
            col = table.addColumn("Long Data", new LongArray(al));
            table.columnAttributes(col).set("units", new StringArray(new String[]{"longs"}));
            table.columnAttributes(col).set("long_att2", new LongArray(new long[]{-2000000000000000L, 2000000000000000L}));
        }

        //5=int
        ai = new int[]{-2000000000, 2, 2000000000};
        col = table.addColumn("Int Data", new IntArray(ai));
        table.columnAttributes(col).set("units", new StringArray(new String[]{"ints"}));
        table.columnAttributes(col).set("int_att2", new IntArray(new int[]{-2000000000, 2000000000}));

        //6=short
        short[] as = {(short)-32000, (short)7, (short)32000};
        col = table.addColumn("Short Data", new ShortArray(as));
        table.columnAttributes(col).set("units", new StringArray(new String[]{"shorts"}));
        table.columnAttributes(col).set("short_att2", new ShortArray(new short[]{(short)-30000, (short)30000}));

        //7=byte
        byte[] ab = {(byte)-120, (byte)8, (byte)120};
        col = table.addColumn("Byte Data", new ByteArray(ab));
        table.columnAttributes(col).set("units", "bytes");
        table.columnAttributes(col).set("byte_att2", new ByteArray(new byte[]{(byte)-120, (byte)120}));

        //8=String
        if (includeStrings) {
            String[] aS = {"a", "bb", "ccc"};
            col = table.addColumn("String Data", new StringArray(aS));
            table.columnAttributes(col).set("units", "Strings");
            table.columnAttributes(col).set("String_att2", new StringArray(new String[]{"a string"}));
        }

        table.ensureValid(); //throws Exception if not
        return table;
    }     

    /**
     * Test the readASCII and saveAsASCII.
     *
     * @throws Exception if trouble
     */
    public static void testASCII() throws Exception {
        //*** test read all
        String2.log("\n***** Table.testASCII  read all");
        verbose = true;
        reallyVerbose = true;

        //generate some data    
        Table table = getTestTable(true, true);

        //write it to a file
        String fileName = testDir + "tempTable.asc";
        table.saveAsTabbedASCII(fileName);
        //String2.log(fileName + "=\n" + String2.readFromFile(fileName)[1]);

        //read it from the file
        Table table2 = new Table();
        table2.readASCII(fileName);

        //check units on 1st data row
        Test.ensureEqual(table2.getStringData(1, 0), "degrees_east", "");
        Test.ensureEqual(table2.getStringData(2, 0), "degrees_north", "");

        //remove units row
        table2.removeRows(0, 1);
        table2.simplify();

        //are they the same (but column types may be different)?
        Test.ensureTrue(table.equals(table2, false), 
            "\ntable=" + table + "\ntable2=" + table2);

        //test simplification: see if column types are the same as original table
        int n = table.nColumns();
        for (int col = 2; col < n; col++) //skip first 2 columns which are intentionally initially stored in bigger type
            Test.ensureEqual(table.columns.get(col).getClass(),
                table2.getColumn(col).getClass(), "test type of col#" + col);
        
        //*** test read subset
        String2.log("\n***** Table.testASCII  read subset");

        //read 2nd row from the file
        table2 = new Table();
        table2.readASCII(fileName, 0, 1, 
            new String[]{"Int Data"}, new double[]{0}, new double[]{4}, 
            new String[]{"Short Data", "String Data"});
        Test.ensureEqual(table2.nColumns(), 2, "");
        Test.ensureEqual(table2.nRows(), 1, "");
        Test.ensureEqual(table2.getColumnName(0), "Short Data", "");
        Test.ensureEqual(table2.getColumnName(1), "String Data", "");
        Test.ensureEqual(table2.getDoubleData(0, 0), 7, "");
        Test.ensureEqual(table2.getStringData(1, 0), "bb", "");


        //*** test read subset with no column names (otherwise same as test above)
        String2.log("\n***** Table.testASCII  read subset with no column names");
        //read 3rd row from the file
        table2 = new Table();
        table2.readASCII(fileName, -1, 1,  //-1=no column names
            new String[]{"Column#5"}, new double[]{0}, new double[]{4}, 
            new String[]{"Column#6", "Column#8"});
        Test.ensureEqual(table2.nColumns(), 2, "");
        Test.ensureEqual(table2.nRows(), 1, "");
        Test.ensureEqual(table2.getColumnName(0), "Column#6", "");
        Test.ensureEqual(table2.getColumnName(1), "Column#8", "");
        Test.ensureEqual(table2.getDoubleData(0, 0), 7, "");
        Test.ensureEqual(table2.getStringData(1, 0), "bb", "");
        
        //** finally 
        File2.delete(fileName);

    }

    /**
     * Test readStandardTabbedASCII.
     *
     * @throws Exception if trouble
     */
    public static void testReadStandardTabbedASCII() throws Exception {
        //*** test read all
        String2.log("\n***** Table.testReadStandardTabbedASCII");
        verbose = true;
        reallyVerbose = true;
       
        //generate some data    
        String lines[] = {
            "colA\tcolB\tcolC",
            "1a\t1b\t1c",
            "2",
             "a\t2",
                 "b\t2c",
            "3a\t3b\t3c"};
        Table table = new Table();

        //read it from lines
        table.readStandardTabbedASCII("tFileName", lines, null, true);
        String2.log("nRows=" + table.nRows() + " nCols=" + table.nColumns());
        Test.ensureEqual(table.dataToCSVString(), 
            "colA,colB,colC\n" +
            "1a,1b,1c\n" +
            "\"2\\na\",\"2\\nb\",2c\n" +
            "3a,3b,3c\n",
            "tFileName toCSVString=\n" + table.dataToCSVString());

        //write it to a file
        String fileName = testDir + "tempTable.asc";
        String2.writeToFile(fileName, String2.toNewlineString(lines));

        //read all columns from the file
        Table table2 = new Table();
        table2.readStandardTabbedASCII(fileName, null, true);
        String2.log("nRows=" + table2.nRows() + " nCols=" + table2.nColumns());
        Test.ensureEqual(table2.dataToCSVString(), 
            "colA,colB,colC\n" +
            "1a,1b,1c\n" +
            "\"2\\na\",\"2\\nb\",2c\n" +
            "3a,3b,3c\n",
            "table2 toCSVString=\n" + table2.dataToCSVString());

        //just read cols B and C from the file
        table2 = new Table();
        table2.readStandardTabbedASCII(fileName, new String[]{"colB", "colC"}, true);
        String2.log("nRows=" + table2.nRows() + " nCols=" + table2.nColumns());
        Test.ensureEqual(table2.dataToCSVString(), 
            "colB,colC\n" +
            "1b,1c\n" +
            "\"2\\nb\",2c\n" +
            "3b,3c\n",
            "table2 toCSVString=\n" + table2.dataToCSVString());

        //** finally 
        File2.delete(fileName);

    }

    /**
     * Test the saveAsHtml.
     *
     * @throws Exception if trouble
     */
    public static void testHtml() throws Exception {
        String2.log("\n***** Table.testHtml");
        verbose = true;
        reallyVerbose = true;

        //generate some data    
        Table table = getTestTable(true, true);

        //write it to a file
        String fileName = testDir + "tempTable.html";
        table.saveAsHtml(fileName, "preTextHtml\n<br>\n", "postTextHtml\n<br>", 
            null, BGCOLOR, 1, true, 0, true, false);
        //String2.log(fileName + "=\n" + String2.readFromFile(fileName)[1]);
        SSR.displayInBrowser("file://" + fileName);

        //read it from the file
        String results[] = String2.readFromFile(fileName);
        Test.ensureEqual(results[0], "", "");
        Test.ensureEqual(results[1], 
"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" +
"  \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
"<html>\n" +
"<head>\n" +
"  <title>tempTable</title>\n" +
"<style type=\"text/CSS\">\n" +
"<!--\n" +
"  table.erd {border-collapse:collapse; border:1px solid gray; }\n" +
"  table.erd th, table.erd td {padding:2px; border:1px solid gray; }\n" +
"--></style>\n" +
"</head>\n" +
"<body bgcolor=\"white\" text=\"black\"\n" +
"  style=\"font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n" +
"preTextHtml\n" +
"<br>\n" +
"<table class=\"erd\" bgcolor=\"#ffffcc\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>Time\n" +
"<th>Longitude\n" +
"<th>Latitude\n" +
"<th>Double Data\n" +
"<th>Long Data\n" +
"<th>Int Data\n" +
"<th>Short Data\n" +
"<th>Byte Data\n" +
"<th>String Data\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC\n" +
"<th>degrees_east\n" +
"<th>degrees_north\n" +
"<th>doubles\n" +
"<th>longs\n" +
"<th>ints\n" +
"<th>shorts\n" +
"<th>bytes\n" +
"<th>Strings\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1970-01-01T00:00:00\n" +
"<td nowrap>-3\n" +
"<td nowrap>1.0\n" +
"<td nowrap>-1.0E300\n" +
"<td nowrap>-2000000000000000\n" +
"<td nowrap>-2000000000\n" +
"<td nowrap>-32000\n" +
"<td nowrap>-120\n" +
"<td nowrap>a\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>2005-08-31T16:01:02\n" +
"<td nowrap>-2\n" +
"<td nowrap>1.5\n" +
"<td nowrap>3.123\n" +
"<td nowrap>2\n" +
"<td nowrap>2\n" +
"<td nowrap>7\n" +
"<td nowrap>8\n" +
"<td nowrap>bb\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>2005-11-02T18:04:09\n" +
"<td nowrap>-1\n" +
"<td nowrap>2.0\n" +
"<td nowrap>1.0E300\n" +
"<td nowrap>2000000000000000\n" +
"<td nowrap>2000000000\n" +
"<td nowrap>32000\n" +
"<td nowrap>120\n" +
"<td nowrap>ccc\n" +
"</tr>\n" +
"</table>\n" +
"postTextHtml\n" +
"<br></body>\n" +
"</html>\n", 
            results[1]);

        //test readHtml - treat 2nd row as data
        Table table2 = new Table();
        table2.readHtml(fileName, results[1], 0, false, true);
        String csv = table2.dataToCSVString();
        Test.ensureEqual(csv, 
"Time,Longitude,Latitude,Double Data,Long Data,Int Data,Short Data,Byte Data,String Data\n" +
"UTC,degrees_east,degrees_north,doubles,longs,ints,shorts,bytes,Strings\n" +
"1970-01-01T00:00:00,-3,1.0,-1.0E300,-2000000000000000,-2000000000,-32000,-120,a\n" +
"2005-08-31T16:01:02,-2,1.5,3.123,2,2,7,8,bb\n" +
"2005-11-02T18:04:09,-1,2.0,1.0E300,2000000000000000,2000000000,32000,120,ccc\n",
            csv);

        //test readHtml - treat 2nd row as units
        table2 = new Table();
        table2.readHtml(fileName, results[1], 0, true, true);
        csv = table2.dataToCSVString();
        Test.ensureEqual(csv, 
"Time,Longitude,Latitude,Double Data,Long Data,Int Data,Short Data,Byte Data,String Data\n" +
"1970-01-01T00:00:00,-3,1.0,-1.0E300,-2000000000000000,-2000000000,-32000,-120,a\n" +
"2005-08-31T16:01:02,-2,1.5,3.123,2,2,7,8,bb\n" +
"2005-11-02T18:04:09,-1,2.0,1.0E300,2000000000000000,2000000000,32000,120,ccc\n",
            csv);
        Test.ensureEqual(table2.columnAttributes(0).getString("units"), "UTC", "");
        Test.ensureEqual(table2.columnAttributes(1).getString("units"), "degrees_east", "");
        Test.ensureEqual(table2.columnAttributes(8).getString("units"), "Strings", "");

        //** finally 
        Math2.gc(3000); //do something useful while browser gets going to display it 
        File2.delete(fileName);
    }


    /**
     * This is a test of readFlatNc and saveAsFlatNc.
     *
     * @throws Exception of trouble
     */
    public static void testFlatNc() throws Exception {

        //********** test reading all data 
        String2.log("\n*** Table.testFlatNc write and then read all");
        verbose = true;
        reallyVerbose = true;

        //generate some data
        Table table = getTestTable(false, true); //falses=.nc doesn't seem to take longs
        String2.log("*******table=" + table.toString("row", Integer.MAX_VALUE));

        //write it to a file
        String fileName = testDir + "tempTable.nc";
        table.saveAsFlatNc(fileName, "time");

        //read it from the file
        Table table2 = new Table();
        table2.readFlatNc(fileName, null, 0);  
        String2.log("*********table2=" + table2.toString("row", Integer.MAX_VALUE));

        //replace ' ' with '_' in column names
        for (int i = 0; i < table.columnNames.size(); i++) 
            table.columnNames.set(i, String2.replaceAll(table.columnNames.get(i), " ", "_"));

        //do the test that the tables are equal
        String2.log("testFlatNc table.nColAtt=" + table.columnAttributes.size() +
                         " table2.nColAtt=" + table2.columnAttributes.size());
        Test.ensureTrue(table.equals(table2), "Test table equality");
        
        //test if data types are the same
        int n = table.nColumns();
        for (int col = 0; col < n; col++) 
            Test.ensureEqual(table.columns.get(col).getClass(),
                table2.columns.get(col).getClass(), "test type of col#" + col);

        //clean up
        table2.clear();
        File2.delete(fileName);


        //***test unpack options     (and global and variable attributes)
        String2.log("\n*** Table.testFlatNc test unpack");
        //row of data from 41015h1993.txt
        //YY MM DD hh WD   WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS
        //93 05 24 11 194 02.5 02.8 00.70 04.20 04.90 185 1021.2  17.3  16.4 999.0 18.5   
        double seconds = Calendar2.isoStringToEpochSeconds("1993-05-24T11");
        String2.log("seconds=" + seconds);
        int[] testColumns = {0};
        double testMin[] = {seconds};
        double testMax[] = {seconds};

        //unpack 0   don't
        int unpack = 0;
        table.readFlatNc(testDir + "41015.nc", new String[]{"time", "BAR"}, unpack); 
        //String2.log(table.toString(100));
        table.subset(testColumns, testMin, testMax);
        Test.ensureEqual(table.nColumns(), 2, "");
        Test.ensureEqual(table.nRows(), 1, "");
        Test.ensureEqual(table.getColumnName(0), "time", "");
        Test.ensureEqual(table.getColumnName(1), "BAR", "");
        Test.ensureEqual(table.getColumn(1).elementClass(), short.class, ""); //short
        Test.ensureEqual(table.getDoubleData(0, 0), seconds, "");
        Test.ensureEqual(table.getDoubleData(1, 0), 10212, "");

        //test global and variable attributes 
        Test.ensureEqual(table.globalAttributes().getString("creator_name"), "NOAA National Data Buoy Center", "");
        Test.ensureEqual(table.columnAttributes(1).getString("long_name"), "Sea Level Pressure", "");

        //unpack 1  to float if that is recommended
        unpack = 1;
        table.readFlatNc(testDir + "41015.nc", new String[]{"time", "BAR"}, unpack);
        table.subset(testColumns, testMin, testMax);
        Test.ensureEqual(table.nColumns(), 2, "");
        Test.ensureEqual(table.nRows(), 1, "");
        Test.ensureEqual(table.getColumnName(0), "time", "");
        Test.ensureEqual(table.getColumnName(1), "BAR", "");
        Test.ensureEqual(table.getColumn(1).elementClass(), float.class, ""); //float
        Test.ensureEqual(table.getDoubleData(0, 0), seconds, "");
        Test.ensureEqual(table.getFloatData(1, 0), 1021.2f, "");

        //unpack 2  to double
        unpack = 2;
        table.readFlatNc(testDir + "41015.nc", new String[]{"time", "BAR"}, unpack);
        table.subset(testColumns, testMin, testMax);
        Test.ensureEqual(table.nColumns(), 2, "");
        Test.ensureEqual(table.nRows(), 1, "");
        Test.ensureEqual(table.getColumnName(0), "time", "");
        Test.ensureEqual(table.getColumnName(1), "BAR", "");
        Test.ensureEqual(table.getColumn(1).elementClass(), double.class, ""); //double
        Test.ensureEqual(table.getDoubleData(0, 0), seconds, "");
        Test.ensureEqual(table.getDoubleData(1, 0), 1021.2, "");


        //********** test reading subset of data  via bitset (which uses read via firstrow/lastrow)
        String2.log("\n*** Table.testFlatNc read subset");
        table.clear();
        NetcdfFile netcdfFile = NcHelper.openFile(testDir + "41015.nc");
        try {
            Variable loadVariables[] = NcHelper.findVariables(netcdfFile, new String[]{"time", "BAR"});
            Variable testVariables[] = NcHelper.findVariables(netcdfFile, new String[]{"time"});
            BitSet okRows = NcHelper.testRows(testVariables, testMin, testMax); 
            table.appendNcRows(loadVariables, okRows);
            Test.ensureEqual(okRows.cardinality(), 1, "");
        } finally {
            try {
                netcdfFile.close(); //make sure it is explicitly closed
            } catch (Exception e) {
            }
        }

        Test.ensureEqual(table.nColumns(), 2, "");
        Test.ensureEqual(table.nRows(), 1, "");
        Test.ensureEqual(table.getColumnName(0), "time", "");
        Test.ensureEqual(table.getColumnName(1), "BAR", "");
        Test.ensureEqual(table.getColumn(1).elementClass(), short.class, ""); //short
        Test.ensureEqual(table.getDoubleData(0, 0), seconds, "");
        Test.ensureEqual(table.getDoubleData(1, 0), 10212, ""); //still packed

    }

    /**
     * This is a test of read4DNc and saveAs4DNc.
     *
     * @throws Exception of trouble
     */
    public static void test4DNc() throws Exception {

        //********** test reading all data 
        String2.log("\n*** Table.test4DNc write and then read all");
        verbose = true;
        reallyVerbose = true;

        //generate some data
        Table table = new Table();
        DoubleArray xCol = new DoubleArray();
        DoubleArray yCol = new DoubleArray();
        DoubleArray zCol = new DoubleArray();
        DoubleArray tCol = new DoubleArray();
        IntArray    data1Col = new IntArray();
        DoubleArray data2Col = new DoubleArray();
        StringArray data3Col = new StringArray();
        table.addColumn("X", xCol);
        table.addColumn("Y", yCol);
        table.addColumn("Z", zCol);
        table.addColumn("T", tCol);
        table.addColumn("data1", data1Col);
        table.addColumn("data2", data2Col);
        table.addColumn("data3", data3Col);
        for (int t = 0; t < 2; t++) {
            for (int z = 0; z < 3; z++) {
                for (int y = 0; y < 3; y++) {
                    for (int x = 0; x < 4; x++) {
                        xCol.add(x+1);
                        yCol.add(y+1);
                        zCol.add(z+1);
                        tCol.add(t+1);
                        int fac = (x+1)*(y+1)*(z+1)*(t+1);
                        data1Col.add(fac);
                        data2Col.add(100+ (x+1)*(y+1)*(z+1)*(t+1));
                        data3Col.add("" + fac);
                    }
                }
            }
        }
        table.ensureValid(); //throws Exception if not
        //String2.log(table.toString("obs", 10));

        //write it to a file
        String fileName = testDir + "temp4DTable.nc";
        Attributes idAttributes = new Attributes();
        idAttributes.set("long_name", "The station's name.");
        idAttributes.set("units", DataHelper.UNITLESS);
        String stringVariableValue = "My Id Value";
        table.saveAs4DNc(fileName, 0,1,2,3, "ID", stringVariableValue, idAttributes);

        //then insert col 4 filled with "My Id Value"
        String sar[] = new String[table.nRows()];
        Arrays.fill(sar, stringVariableValue);
        table.addColumn(4, "ID", new StringArray(sar), idAttributes);

        //get the header
        //String2.log("table=" + DataHelper.ncDumpString(fileName, true));

        //read from file
        Table table2 = new Table();
        table2.read4DNc(fileName, null, 1, "ID", 4);
        //String2.log("col6=" + table2.getColumn(6));
        //String2.log("\ntable2 after read4DNc: " + table2.toString("obs", 1000000));
         
        //test equality
        Test.ensureTrue(table.equals(table2), "test4DNc tables not equal!");

        File2.delete(fileName);

    }


    /**
     * This is a test of readOpendapSequence.
     * Test cases from Roy:
     * GLOBEC VPT:
     * stn_id=loaddods('http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt?stn_id&unique()');
     * abund=loaddods('-F','http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt?abund_m3&stn_id="NH05"');
     * GLOBEC Bottle:
     * month=loaddods('-F','http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?month&unique()');
     * [oxy temp]=loaddods('-F','http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?t0,oxygen&month="5"');
     * GLOBEC MOC1:
     * [abund,lon,lat]=loaddods('-F','http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1?abund_m3,lat,long');
     * [abund1,lon1,lat1]=loaddods('-F','http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1?abund_m3,lat,long&program="MESO_1"');
     * I note that loaddods documentation is at http://www.opendap.org/user/mgui-html/mgui_36.html
     * and -F says to convert all strings to floats.
     * "unique()" seems to just return unique values.
     *
     * @throws Exception of trouble
     */
    public static void testOpendapSequence() throws Exception {

        String2.log("\n*** Table.testOpendapSequence");
        verbose = true;
        reallyVerbose = true;
        Table table = new Table();
        int nRows;
        String url;
        float lon, lat;
/* */
        //************* SINGLE SEQUENCE *****************************
        try {
            //read data from opendap
            table.readOpendapSequence(
                //via ascii test:  date is ok, but requesting 'year' always fails
                //http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1.asc?abund_m3,lat,lon,date,time_local,min_sample_depth
                //via ascii: "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1.asc?abund_m3,lat,lon", null);
                //~2010-01-15, now lon, was long
                //~2010-01-17 back to long
                "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1?abund_m3,lat,long", false);
            String results = table.toString("row", 5);
            String2.log(results);
//               this changed 2008-10-27
//MOC1.lat, MOC1.long, MOC1.abund_m3
//44.6517, -124.65, 0.003961965
//44.0, -124.4, 0.153550868
//44.0, -124.8, 0.012982798
//44.6517, -124.65, 0.46807087
//44.6517, -124.4117, 0.854271346

            nRows = 3779;
            Test.ensureEqual(table.nColumns(), 3, "");
            Test.ensureEqual(table.nRows(), nRows, "");

            String expected = 
"{\n" +
"dimensions:\n" +
"\trow = 3779 ;\n" +
"variables:\n" +
"\tfloat lat(row) ;\n" +
"\t\tlat:long_name = \"Latitude\" ;\n" +
"\tfloat long(row) ;\n" +
"\t\tlong:long_name = \"Longitude\" ;\n" +
"\tfloat abund_m3(row) ;\n" +
"\t\tabund_m3:long_name = \"Abundance m3\" ;\n" +
"\n" +
"// global attributes:\n" +
"}\n" +
"    Row             lat           long       abund_m3\n" +
"      0       44.651699    -124.650002     3.69822E-3\n" +
"      1       44.651699    -124.650002     7.26257E-2\n" +
"      2       42.504601    -125.011002     1.10023E-3\n" +
"      3       42.501801    -124.706001     7.88955E-2\n" +
"      4         42.5033    -124.845001        3.41646\n";

            Test.ensureEqual(results, expected, "");
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from opendapSequence failure? Press 'Enter' to continue or ^C to stop...");
        }


        try {    
            url = "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt?stn_id&unique()"; 
            table.readOpendapSequence(url, false);
            String2.log(table.toString("row", 3));
            //source has no global metadata 
            Test.ensureEqual(table.nColumns(), 1, "");
            Test.ensureEqual(table.nRows(), 77, "");
            Test.ensureEqual(table.getColumnName(0), "stn_id", "");
            Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Station ID", "");
            Test.ensureEqual(table.getStringData(0, 0), "BO01", "");
            Test.ensureEqual(table.getStringData(0, 1), "BO02", "");
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from opendapSequence failure? Press 'Enter' to continue or ^C to stop...");
        }


        try {
            url = "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt?abund_m3&stn_id=\"NH05\""; 
            table.readOpendapSequence(url, false);
            String2.log(table.toString("row", 3));
            //source has no global metadata 
            Test.ensureEqual(table.nColumns(), 1, "");
            Test.ensureEqual(table.nRows(), 2400, "");
            Test.ensureEqual(table.getColumnName(0), "abund_m3", "");
            Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Abundance m3", "");
            Test.ensureEqual(table.getFloatData(0, 0), 11.49f, "");
            Test.ensureEqual(table.getFloatData(0, 1), 74.720001f, "");
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from opendapSequence failure? Press 'Enter' to continue or ^C to stop...");
        }


        try {
            url = "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?month&unique()"; 
            table.readOpendapSequence(url, false);
            String2.log(table.toString("row", 3));
            //source has no global metadata 
            Test.ensureEqual(table.nColumns(), 1, "");
            Test.ensureEqual(table.nRows(), 4, "");
            Test.ensureEqual(table.getColumnName(0), "month", "");
            Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Month", "");
            Test.ensureEqual(table.getDoubleData(0, 0), 5, "");
            Test.ensureEqual(table.getDoubleData(0, 1), 6, "");
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from opendapSequence failure? Press 'Enter' to continue or ^C to stop...");
        }


        try {
            url = "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?t0,oxygen&month=\"5\"";
            table.readOpendapSequence(url, false);
            String2.log(table.toString("row", 3));
            //source has no global metadata 
            Test.ensureEqual(table.nColumns(), 2, "");
            Test.ensureEqual(table.nRows(), 190, "");
            Test.ensureEqual(table.getColumnName(0), "t0", "");
            Test.ensureEqual(table.getColumnName(1), "oxygen", "");
            Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Temperature T0", "");
            Test.ensureEqual(table.columnAttributes(1).getString("long_name"), "Oxygen", "");
            Test.ensureEqual(table.getFloatData(0, 0), 12.1185f, "");
            Test.ensureEqual(table.getFloatData(0, 1), 12.1977f, "");
            Test.ensureEqual(table.getFloatData(1, 0), 6.56105f, "");
            Test.ensureEqual(table.getFloatData(1, 1), 6.95252f, "");
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from opendapSequence failure? Press 'Enter' to continue or ^C to stop...");
        }

        try {
            url = "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1?abund_m3,lat,long&program=\"MESO_1\"";
            table.readOpendapSequence(url, false);
            String results = table.toString("row", 3);
            String2.log(results);
            //source has no global metadata 
            String expected =
"{\n" +
"dimensions:\n" +
"\trow = 328 ;\n" +
"variables:\n" +
"\tfloat lat(row) ;\n" +
"\t\tlat:long_name = \"Latitude\" ;\n" +
"\tfloat long(row) ;\n" +
"\t\tlong:long_name = \"Longitude\" ;\n" +
"\tfloat abund_m3(row) ;\n" +
"\t\tabund_m3:long_name = \"Abundance m3\" ;\n" +
"\n" +
"// global attributes:\n" +
"}\n" +
"    Row             lat           long       abund_m3\n" +
"      0       44.651699    -124.650002        10.7463\n" +
"      1       44.651699    -124.650002     1.40056E-2\n" +
"      2       44.651699    -124.650002       0.252101\n";
            Test.ensureEqual(table.nColumns(), 3, "");
            Test.ensureEqual(table.nRows(), 328, "");
            Test.ensureEqual(results, expected, "");
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from opendapSequence failure? Press 'Enter' to continue or ^C to stop...");
        }

        try {
            //test getting all data
            //modified from above so it returns lots of data 
            //nRows=16507 nColumns=28  readTime=5219 ms  processTime=94 ms
            url = "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt";
            table.readOpendapSequence(url, false);
            String2.log(table.toString("row", 5));
            //source has no global metadata 
//    Row        datetime   datetime_utc datetime_utc_e           year        program      cruise_id        cast_no         stn_id
//lat           long        lat1000        lon1000    water_depth      sample_id min_sample_dep max_sample_dep    month_local      day_local
//   time_local       d_n_flag      gear_type   gear_area_m2      gear_mesh       vol_filt     counter_id       comments   perc_counted     lo
//cal_code      nodc_code  genus_species     life_stage       abund_m3
//      0  2001-04-25 21: 2001-04-25 22:      988261731           2001             NH       EL010403              2           NH05      44.650
//002    -124.169998          44650        -124170             60              1              0             55              4             25
//        -9999          -9999            VPT        0.19635          0.202          14.46            WTP          -9999            1.1    611
//8010204#     6118010204 CALANUS_MARSHA        3;_CIII          11.49
            Test.ensureEqual(table.nColumns(), 32, "");
            Test.ensureEqual(table.nRows(), 16507, "");
            Test.ensureEqual(table.getColumnName(2), "datetime_utc_epoch", "");
            Test.ensureEqual(table.getColumnName(3), "year", "");
            Test.ensureEqual(table.getColumnName(31),"abund_m3", "");
            Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Date", "");
            Test.ensureEqual(table.columnAttributes(1).getString("long_name"), "Date UTC", "");
            Test.ensureEqual(table.columnAttributes(2).getString("long_name"), "Date UTC (sec since 1970)", "");
            Test.ensureEqual(table.columnAttributes(31).getString("long_name"),"Abundance m3", "");
            Test.ensureEqual(table.getStringData(1, 0), "2001-04-25 22:08:51.0", "");
            Test.ensureEqual(table.getDoubleData(2, 0), 9.88261731E8, "");
            Test.ensureEqual(table.getStringData(4, 0), "NH", "");
            Test.ensureEqual(table.getStringData(5, 0), "EL010403", "");
            Test.ensureEqual(table.getFloatData(31, 0), 11.49f, "");
            Test.ensureEqual(table.getFloatData(31, 1), 74.72f, "");
            Test.ensureEqual(table.getFloatData(31, 2), 57.48f, "");
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from opendapSequence failure? Press 'Enter' to continue or ^C to stop...");
        }


        try {

            //*************
            String2.log("\n*** Table.testOpendapSequence subset data via tests");

            //read data from opendap
            table = new Table();
            table.readOpendapSequence(
                //resulting url (for asc) is: 
                // http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1.asc?abund_m3,lat,long&abund_m3>=0.248962651&abund_m3<=0.248962653
                //  Opera browser changes > to %3E and < to %3C
                "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1",
                new String[]{"abund_m3"}, 
                new double[]{0.24896}, //new double[]{0.248962651}, //the last 2 rows only
                new double[]{0.24897}, //new double[]{0.248962653}, 
                new String[]{"abund_m3","lat","long"},
                false); 

            String2.log(table.toString("row", 5));

            nRows = 2; //from rows 3774 and 3778
            Test.ensureEqual(table.nColumns(), 3, "");
            Test.ensureEqual(table.nRows(), nRows, "");

            Test.ensureEqual(table.getColumnName(0), "lat", "");
            Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Latitude", "");
            Test.ensureEqual(table.getFloatData(0, 0), 44.6517f, "");
            Test.ensureEqual(table.getFloatData(0, nRows-1), 44.6517f, "");

            Test.ensureEqual(table.getColumnName(1), "long", "");
            Test.ensureEqual(table.columnAttributes(1).getString("long_name"), "Longitude", "");
            Test.ensureEqual(table.getFloatData(1, 0), -124.65f, "");
            Test.ensureEqual(table.getFloatData(1, nRows-1), -124.65f, "");

            Test.ensureEqual(table.getColumnName(2), "abund_m3", "");
            Test.ensureEqual(table.columnAttributes(2).getString("long_name"), "Abundance m3", "");
            Test.ensureEqual(table.getFloatData(2, 0), 0.248962652f, "");
            Test.ensureEqual(table.getFloatData(2, nRows-1), 0.248962652f, "");
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from opendapSequence failure? Press 'Enter' to continue or ^C to stop...");
        }


        //************* TWO-LEVEL SEQUENCE *****************************

        try {

            //note that I was UNABLE to get .asc responses for these dapper urls while poking around.
            //but straight dods request (without ".dods") works.
            //I'm testing Lynn's old ndbc data because I can independently generate test info
            //  from my ndbc files.
            //Starting url from Roy: http://las.pfeg.noaa.gov/dods/
            //See in DChart: http://las.pfeg.noaa.gov/dchart
            //Test info from my cached ndbc file c:/observation/ndbcMetHistoricalTxt/46022h2004.txt
            //YYYY MM DD hh  WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
            //2004 01 01 00 270  2.0  3.1  3.11 16.67  9.80 999 1010.7 999.0 999.0 999.0 99.0 99.00
            //2004 01 01 01 120  5.9  7.3  3.29 16.67 10.12 999 1010.4 999.0 999.0 999.0 99.0 99.00
            //test attributes are from "http://las.pfeg.noaa.gov/dods/ndbc/all_noaa_time_series.cdp.das"
            //http://las.pfeg.noaa.gov/dods/ndbc/all_noaa_time_series.cdp?location.LON,location.LAT,location.DEPTH,location.profile.TIME,location.profile.WSPD1,location.profile.BAR1&location.LON>=235.3&location.LON<=235.5&location.LAT>=40.7&location.LAT<=40.8&location.profile.TIME>=1072915200000&location.profile.TIME<=1072920000000
            lon = 235.460007f; //exact values from just get LON and LAT values available
            lat = 40.779999f;
            long time1 = Calendar2.newGCalendarZulu(2004, 1, 1).getTimeInMillis();
            long time2 = time1 + Calendar2.MILLIS_PER_HOUR;
            //was http://las.pfeg.noaa.gov/dods/ndbc/all_noaa_time_series.cdp 
            url = "http://las.pfeg.noaa.gov/dods/ndbcMet/ndbcMet_time_series.cdp?" +
                "location.LON,location.LAT,location.DEPTH,location.profile.TIME,location.profile.WSPD,location.profile.BAR" +
                "&location.LON>=" + (lon - .01f) + "&location.LON<=" + (lon + .01f) + //I couldn't catch lon with "="
                "&location.LAT>=" + (lat - .01f) + "&location.LAT<=" + (lat + .01f) + //I couldn't catch lat with "="
                "&location.profile.TIME>=" + (time1 - 1) + 
                "&location.profile.TIME<=" + (time2 + 1);
            String2.log("url=" + url);
            table.readOpendapSequence(url, false);
            String2.log(table.toString());
            Test.ensureEqual(table.nColumns(), 6, "");
            Test.ensureEqual(table.nRows(), 2, "");
            int latCol = table.findColumnNumber("LAT");
            int lonCol = table.findColumnNumber("LON");
            Test.ensureTrue(latCol >= 0 && latCol < 2, "latCol=" + latCol); 
            Test.ensureTrue(lonCol >= 0 && lonCol < 2, "lonCol=" + lonCol);  
            Test.ensureEqual(table.getColumnName(2), "DEPTH", "");
            Test.ensureEqual(table.getColumnName(3), "TIME", "");
            int barCol = table.findColumnNumber("BAR");
            int wspdCol = table.findColumnNumber("WSPD");
            Test.ensureEqual(table.getColumn(latCol).elementClassString(), "float", "");
            Test.ensureEqual(table.getColumn(3).elementClassString(), "double", "");
            Test.ensureEqual(table.getColumn(wspdCol).elementClassString(), "float", "");
            Test.ensureEqual(table.globalAttributes().getString("Conventions"), "epic-insitu-1.0", "");
            Test.ensureEqual(table.globalAttributes().get("lat_range").toString(), "-27.7000007629395, 70.4000015258789", "");
            Test.ensureEqual(table.getFloatData(latCol, 0), lat, "");
            Test.ensureEqual(table.getFloatData(latCol, 1), lat, "");
            Test.ensureEqual(table.getFloatData(lonCol, 0), lon, ""); 
            Test.ensureEqual(table.getFloatData(lonCol, 1), lon, "");
            //outer attributes...
            Test.ensureEqual(table.columnAttributes(latCol).getString("units"), "degrees_north", ""); 
            Test.ensureEqual(table.columnAttributes(latCol).getString("long_name"), "latitude", "");
            Test.ensureEqual(table.columnAttributes(latCol).getDouble("missing_value"), Double.NaN, "");
            Test.ensureEqual(table.columnAttributes(latCol).getString("axis"), "Y", "");
            Test.ensureEqual(table.getDoubleData(3, 0), time1, "");
            Test.ensureEqual(table.getDoubleData(3, 1), time2, "");
            //inner attributes...
            Test.ensureEqual(table.columnAttributes(3).getString("units"), "msec since 1970-01-01 00:00:00 GMT", "");
            Test.ensureEqual(table.columnAttributes(3).getString("long_name"), "time", "");
            Test.ensureEqual(table.columnAttributes(3).getDouble("missing_value"), Double.NaN, "");
            Test.ensureEqual(table.columnAttributes(3).getString("axis"), "T", "");
            Test.ensureEqual(table.getFloatData(barCol, 0), 1010.7f, ""); //bar
            Test.ensureEqual(table.getFloatData(barCol, 1), 1010.4f, "");
            Test.ensureEqual(table.getFloatData(wspdCol, 0), 2.0f, ""); //wspd
            Test.ensureEqual(table.getFloatData(wspdCol, 1), 5.9f, "");

        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from opendapSequence failure? Press 'Enter' to continue or ^C to stop...");
        }    


        try {
            //This Calcofi test simply verifies that the results now are as they were when
            //  I wrote the test (circular logic).
            //I hope this test is better than ndbc test above,
            //  since hopefully longer lived (since ndbc data may not be around forever).
            //target data
            //    Row            time            lat            lon          depth English_sole_LarvaeCount
            //     10    947320140000      32.341667     241.445007     203.800003        NaN
            //     11    947320140000      32.341667     241.445007            NaN        NaN
            lat = 32.341667f;
            lon = 241.445f;
            long time = 947320140000L;
            //Starting url from roy: http://las.pfeg.noaa.gov/dods/
            //see info via url without query, but plus .dds or .das
            url = "http://las.pfeg.noaa.gov/dods/CalCOFI/Biological.cdp?" +
                "location.lon,location.lat,location.time,location.profile.depth,location.profile.Line,location.profile.Disintegrated_fish_larvae_LarvaeCount" +
                "&location.lon>=" + (lon - .01f) + "&location.lon<=" + (lon + .01f) + //I couldn't catch lon with "="
                "&location.lat>=" + (lat - .01f) + "&location.lat<=" + (lat + .01f) + //I couldn't catch lat with "="
                "&location.time>=" + (time - 1);
            table.readOpendapSequence(url, false);

            String2.log(table.toString());
            int latCol = table.findColumnNumber("lat");
            int lonCol = table.findColumnNumber("lon");
            Test.ensureTrue(latCol >= 0, "latCol=" + latCol); 
            Test.ensureTrue(lonCol >= 0, "lonCol=" + lonCol);  
            Test.ensureEqual(table.nColumns(), 6, "");
            Test.ensureEqual(table.nRows(), 31, "");
            Test.ensureEqual(table.getColumnName(0), "time", ""); //not in order I requested!   they are in dataset order
            Test.ensureEqual(table.getColumnName(latCol), "lat", "");  
            Test.ensureEqual(table.getColumnName(lonCol), "lon", "");  
            Test.ensureEqual(table.getColumnName(3), "Line", "");
            Test.ensureEqual(table.getColumnName(4), "Disintegrated_fish_larvae_LarvaeCount", "");
            Test.ensureEqual(table.getColumnName(5), "depth", "");
            Test.ensureEqual(table.getColumn(0).elementClassString(), "double", "");
            Test.ensureEqual(table.getColumn(latCol).elementClassString(), "float", "");
            Test.ensureEqual(table.getColumn(lonCol).elementClassString(), "float", "");
            Test.ensureEqual(table.getColumn(3).elementClassString(), "float", "");
            Test.ensureEqual(table.getColumn(4).elementClassString(), "float", "");
            Test.ensureEqual(table.getColumn(5).elementClassString(), "float", "");
            //global attributes
            Test.ensureEqual(table.globalAttributes().getString("Conventions"), "epic-insitu-1.0", "");
            Test.ensureEqual(table.globalAttributes().getInt("total_profiles_in_dataset"),  6407, "");
            //test of outer attributes
            Test.ensureEqual(table.columnAttributes(0).getString("units"), "msec since 1970-01-01 00:00:00 GMT", "");
            Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "time", "");
            Test.ensureEqual(table.columnAttributes(0).getDouble("missing_value"), Double.NaN, "");
            Test.ensureEqual(table.columnAttributes(0).getString("axis"), "T", "");
            //test of inner attributes
            Test.ensureEqual(table.columnAttributes(4).getString("long_name"), "disintegrated fish larvae larvae count", "");
            Test.ensureEqual(table.columnAttributes(4).getFloat("missing_value"), Float.NaN, "");
            Test.ensureEqual(table.columnAttributes(4).getString("units"), "number of larvae", "");

            //test of results
            //NOTE that data from different inner sequences is always separated by a row of NaNs 
            //  in the ending inner sequence's info.
            //  I believe Dapper is doing this. See more comments below.
            //    Row            time            lat            lon Disintegrated_           Line          depth
            //      0    947320140000      32.341667     241.445007            NaN      93.300003     203.800003
            //      1    947320140000      32.341667     241.445007            NaN            NaN            NaN
            //      2    955184100000      32.348331     241.448334            NaN      93.300003     215.800003
            //     30   1099482480000      32.345001     241.445007            NaN      93.300003     198.899994
             Test.ensureEqual(table.getDoubleData(0, 0), 947320140000L, "");
            Test.ensureEqual(table.getFloatData(latCol, 0), 32.341667f, "");
            Test.ensureEqual(table.getFloatData(lonCol, 0), 241.445007f, "");
            Test.ensureEqual(table.getFloatData(3, 0), 93.300003f, "");
            Test.ensureEqual(table.getFloatData(4, 0), Float.NaN, "");
            Test.ensureEqual(table.getFloatData(5, 0), 203.800003f, "");

            Test.ensureEqual(table.getDoubleData(0, 1), 947320140000L, "");
            Test.ensureEqual(table.getFloatData(latCol, 1), 32.341667f, "");
            Test.ensureEqual(table.getFloatData(lonCol, 1), 241.445007f, "");
            Test.ensureEqual(table.getFloatData(3, 1), Float.NaN, "");
            Test.ensureEqual(table.getFloatData(4, 1), Float.NaN, "");
            Test.ensureEqual(table.getFloatData(5, 1), Float.NaN, "");

            Test.ensureEqual(table.getDoubleData(0, 30), 1099482480000L, "");
            Test.ensureEqual(table.getFloatData(latCol, 30), 32.345001f, "");
            Test.ensureEqual(table.getFloatData(lonCol, 30), 241.445007f, "");
            Test.ensureEqual(table.getFloatData(3, 30), 93.300003f, "");
            Test.ensureEqual(table.getFloatData(4, 30), Float.NaN, "");
            Test.ensureEqual(table.getFloatData(5, 30), 198.899994f, "");
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from opendapSequence failure? Press 'Enter' to continue or ^C to stop...");
        }


        try {
            //*** visual test: is dapper returning the NAN row at the end of every innerSequence (true)
            //  or is that the way it is in the files?  (false)
            lon = 235.460007f; //exact values from just get LON and LAT values available
            lat = 40.779999f;
            long time1 = Calendar2.newGCalendarZulu(2004, 1, 1).getTimeInMillis();
            long time2 = time1 + Calendar2.MILLIS_PER_HOUR;
            //was http://las.pfeg.noaa.gov/dods/ndbc/all_noaa_time_series.cdp
            url = "http://las.pfeg.noaa.gov/dods/ndbcMet/ndbcMet_time_series.cdp?" +
                "location.LON,location.LAT,location.DEPTH,location.profile.TIME,location.profile.WSPD,location.profile.BAR" +
                "&location.LON>=" + (lon - 5f) + "&location.LON<=" + (lon + 5f) + 
                "&location.LAT>=" + (lat - 5f) + "&location.LAT<=" + (lat + 5f) + 
                "&location.profile.TIME>=" + (time1 - 1) + 
                "&location.profile.TIME<=" + (time2 + 1);
            table.readOpendapSequence(url, false);
            String2.log(table.toString());
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nStarted failing 2009-07-21 too much data, timeout.\n" +
                "Recover from opendapSequence failure? Press 'Enter' to continue or ^C to stop...");
        }

    /*
        try{
            //THIS WORKS, BUT TAKES ~40 SECONDS!!!  so don't do all the time
            //see questions below.
            //This gets all the valid English_sole_LarvaeCount data.
            //UNFORTUNATELY, you can't put constraint on non-axis variable,
            //  so I have to get all data and then filter the results.
            //This test simply verifies that the results now are as they were when
            //  I wrote the test (circular logic).
            //I had hoped this test would be better than ndbc test above,
            //  since hopefully longer lived (since ndbc data may not be around forever).
            //Starting url from roy: http://las.pfeg.noaa.gov/dods/
            url = "http://las.pfeg.noaa.gov/dods/CalCOFI/Biological.cdp?" +
                "location.lon,location.lat,location.time,location.profile.depth,location.profile.English_sole_LarvaeCount";
            table.readOpendapSequence(url);
            String2.log("raw results nRows=" + table.nRows());
            //just keep rows with larvaeCounts >= 0
            table.subset(new int[]{4}, new double[]{0}, new double[]{1e300}); 

            String2.log(table.toString());
            Test.ensureEqual(table.nColumns(), 5, "");
            Test.ensureEqual(table.nRows(), 98, "");
            Test.ensureEqual(table.getColumnName(0), "time", ""); //not in order I requested!   they are in dataset order
            Test.ensureEqual(table.getColumnName(1), "lat", "");  
            Test.ensureEqual(table.getColumnName(2), "lon", "");  
            Test.ensureEqual(table.getColumnName(3), "depth", "");
            Test.ensureEqual(table.getColumnName(4), "English_sole_LarvaeCount", "");
            Test.ensureEqual(table.getColumn(0).elementClassString(), "double", "");
            Test.ensureEqual(table.getColumn(1).elementClassString(), "float", "");
            Test.ensureEqual(table.getColumn(2).elementClassString(), "float", "");
            Test.ensureEqual(table.getColumn(3).elementClassString(), "float", "");
            Test.ensureEqual(table.getColumn(4).elementClassString(), "float", "");
            //global attributes
            Test.ensureEqual(table.globalAttributes().getString("Conventions"), "epic-insitu-1.0", "");
            Test.ensureEqual(table.globalAttributes().getInt("total_profiles_in_dataset"),  6407, "");
            //test of outer attributes
            Test.ensureEqual(table.columnAttributes(0).getString("units"), "msec since 1970-01-01 00:00:00 GMT", "");
            Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "time", "");
            Test.ensureEqual(table.columnAttributes(0).getDouble("missing_value"), Double.NaN, "");
            Test.ensureEqual(table.columnAttributes(0).getString("axis"), "T", "");
            //test of inner attributes
            Test.ensureEqual(table.columnAttributes(4).getString("long_name"), "parophrys vetulus larvae count", "");
            Test.ensureEqual(table.columnAttributes(4).getFloat("missing_value"), Float.NaN, "");
            Test.ensureEqual(table.columnAttributes(4).getString("units"), "number of larvae", "");

            //test of results
            Test.ensureEqual(table.getDoubleData(0, 0), 955657380000L, "");
            Test.ensureEqual(table.getFloatData(1, 0), 33.485001f, "");
            Test.ensureEqual(table.getFloatData(2, 0), 242.231659f, "");
            Test.ensureEqual(table.getFloatData(3, 0), 210.800003f, "");
            Test.ensureEqual(table.getFloatData(4, 0), 1, "");

            Test.ensureEqual(table.getDoubleData(0, 1), 955691700000L, "");
            Test.ensureEqual(table.getFloatData(1, 1), 33.825001f, "");
            Test.ensureEqual(table.getFloatData(2, 1), 241.366669f, "");
            Test.ensureEqual(table.getFloatData(3, 1), 208.5f, "");
            Test.ensureEqual(table.getFloatData(4, 1), 6, "");

            Test.ensureEqual(table.getDoubleData(0, 97), 923900040000L, "");
            Test.ensureEqual(table.getFloatData(1, 97), 34.976665f, "");
            Test.ensureEqual(table.getFloatData(2, 97), 237.334991f, "");
            Test.ensureEqual(table.getFloatData(3, 97), 24.1f, "");
            Test.ensureEqual(table.getFloatData(4, 97), 4, "");
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from opendapSequence failure? Press 'Enter' to continue or ^C to stop...");
        }

/* */
/**
[Bob talked to Lynn about this. Conclusions below.]
1)   If I just get all lon,lat,time,depth, and English_sole_LarvaeCount
        url = "http://las.pfeg.noaa.gov/dods/CalCOFI/Biological.cdp?" +
            "location.lon,location.lat,location.time,location.profile.depth,location.profile.English_sole_LarvaeCount";
    it looks like each time,lat,lon combo has a data row and a NaN row.
    ???Is this a real NaN row, or a mistake in my code (e.g., end of sequence beginning of next).
    [I believe it is real and added by Dapper.]
    Note that some sole counts below are non-NaN.

    Row            time            lat            lon          depth English_sole_LarvaeCount
      0    947255160000      32.955002     242.695007           71.5        NaN
      1    947255160000      32.955002     242.695007            NaN        NaN
      2    947264520000      32.913334     242.606659     207.600006        NaN
      3    947264520000      32.913334     242.606659            NaN        NaN
      4    947275680000      32.848335     242.471664     211.699997        NaN
      5    947275680000      32.848335     242.471664            NaN        NaN
      6    947290920000          32.68     242.126663     195.899994        NaN
      7    947290920000          32.68     242.126663            NaN        NaN
      8    947306040000      32.513332     241.790009     208.100006        NaN
      9    947306040000      32.513332     241.790009            NaN        NaN
     10    947320140000      32.341667     241.445007     203.800003        NaN
     11    947320140000      32.341667     241.445007            NaN        NaN
     12    947343360000          32.18     241.110001          209.5        NaN
     13    947343360000          32.18     241.110001            NaN        NaN
     14    947359140000      32.006668     240.764999          215.5        NaN
     15    947359140000      32.006668     240.764999            NaN        NaN
2) do all time,lat,lon combo's just have one depth?
   If so, then why set up this way?  
   Just to match dapper convention (must have z or t outside and t or z inside)?
   [I believe so.]

3) Since it appears that the 150(?) variables were only measured rarely,
   it seems hugely wasteful to allocate space for them.
   And worse, since a query use constraints on non-axis variables,
   one can't simply ask for  ... &English_sole_LarvaeCount>=0
   to find time,lat,lon,depth where there are valid values of English_sole_LarvaeCount.
   And requesting all data rows (so I can then filtering on my end) takes ~40 seconds
   for 98 rows of data.
   [Wasteful, but I believe Roy did it this way to conform to Dapper Conventions so
   data easily served by Dapper/DChart, see http://las.pfeg.noaa.gov/dchart.]

4) There are no 0 values for English_sole_LarvaeCount.
   So how can one tell if people looked for English_sole_Larvae but didn't find any?
   Are NaN's to be treated as 0 for this data set?
   [It looks like 0 values are lumped in with NaNs.]

5) Why is number of larvae (units="number of larvae") a float and not an int?
   [Because all variables are floats for simplicity (maybe for matlab or fortran).]

*/
    }

    /** Test the speed of readASCII */
    public static void testReadASCIISpeed() throws Exception {

        try {
            //warmup
            String2.log("\n*** Table.testReadASCIISpeed\n");
            String fileName = "F:/data/ndbc/ndbcMetHistoricalTxt/41009h1990.txt"; 
            Table table = new Table();
            table.readASCII(fileName);

            //time it
            long fileLength = File2.length(fileName); //was 1335204
            Test.ensureTrue(fileLength > 1335000, "fileName=" + fileName + " length=" + fileLength); 
            long time = System.currentTimeMillis();
            table = new Table();
            table.readASCII(fileName);
            time = System.currentTimeMillis() - time;

            String results = table.dataToCSVString(3);
            String expected =
"row,YY,MM,DD,hh,WD,WSPD,GST,WVHT,DPD,APD,MWD,BAR,ATMP,WTMP,DEWP,VIS\n" +
"0,90,1,1,0,161,8.6,10.7,1.5,5.0,4.8,999,1017.2,22.7,22.0,999.0,99.0\n" +
"1,90,1,1,1,163,9.3,11.3,1.5,5.0,4.9,999,1017.3,22.7,22.0,999.0,99.0\n" +
"2,90,1,1,1,164,9.2,10.6,1.6,4.8,4.9,999,1017.3,22.7,22.0,999.0,99.0\n";
            Test.ensureEqual(results, expected, "results=\n" + expected);
            Test.ensureEqual(table.nColumns(), 16, "nColumns=" + table.nColumns()); 
            Test.ensureEqual(table.nRows(), 17117, "nRows=" + table.nRows()); 

            String2.log("********** Done. cells/ms=" + 
                (table.nColumns() * table.nRows()/time) + " (usual=648)" +
                " time=" + time + " ms (usual=422,  java 1.5 was 719)"); 
            if (time > 700)
                throw new SimpleException("readASCII took too long.");
            Math2.sleep(5000);
        } catch (Exception e) {
            String2.getStringFromSystemIn(
                MustBe.throwableToString(e) +
                "\nUnexpected " + String2.ERROR + ".  Press ^C to stop or Enter to continue..."); 
        }
    }


    /** Test the speed of readJson */
    public static void testReadJsonSpeed() throws Exception {

        try {
            //warmup
            String2.log("\n*** Table.testReadJsonSpeed\n");
            String fileName = "F:/u00/cwatch/testData/cPostDet3.files.json"; 
            Table table=new Table();
            table.readJson(fileName, String2.readFromFile(fileName)[1]);

            //time it
            long time = System.currentTimeMillis();
            long fileLength = File2.length(fileName); //was 10,166KB
            Test.ensureTrue(fileLength > 9000000, "fileName=" + fileName + " length=" + fileLength); 
            table=new Table();
            table.readJson(fileName, String2.readFromFile(fileName)[1]);

            String results = table.dataToCSVString(3);
            String2.log("results=\n" + results);
//row,dirIndex,fileName,lastMod,sortedSpacing,unique_tag_id_min_,unique_tag_id_max_,PI_min_,PI_max_,longitude_min_,longitude_max_,l
//atitude_min_,latitude_max_,time_min_,time_max_,bottom_depth_min_,bottom_depth_max_,common_name_min_,common_name_max_,date_public_min
//_,date_public_max_,line_min_,line_max_,position_on_subarray_min_,position_on_subarray_max_,project_min_,project_max_,riser_height_mi
//n_,riser_height_max_,role_min_,role_max_,scientific_name_min_,scientific_name_max_,serial_number_min_,serial_number_max_,stock_min_,
// stock_max_,surgery_time_min_,surgery_time_max_,surgery_location_min_,surgery_location_max_,tagger_min_,tagger_max_
            Test.ensureTrue(results.indexOf("unique_tag_id_max") > 0, "test 1");
            Test.ensureTrue(results.indexOf("surgery_time_min") > 0,  "test 2");
            Test.ensureTrue(table.nColumns() > 40, "nColumns=" + table.nColumns()); //was 42
            Test.ensureTrue(table.nRows() > 15000, "nRows=" + table.nRows()); //was 15024

            time = System.currentTimeMillis() - time;
            String2.log("********* Done. cells/ms=" + 
                (table.nColumns() * table.nRows()/time) + " (usual=747)" +
                " time=" + time + " ms (usual=844;   java 1.5 was 1687)"); 
            if (time > 1200)
                throw new SimpleException("readJson took too long.");
            Math2.sleep(5000);
        } catch (Exception e) {
            String2.getStringFromSystemIn(
                MustBe.throwableToString(e) +
                "\nUnexpected ERROR.  Press ^C to stop or Enter to continue..."); 
        }
    }


    /** Test the speed of readNDNc */
    public static void testReadNDNcSpeed() throws Exception {

        try {
            //warmup
            String2.log("\n*** Table.testReadNDNcSpeed\n");
            String fileName = "c:/u00/data/points/ndbcMet/NDBC_41002_met.nc"; 
            Table table = new Table();
            table.readNDNc(fileName, null, null, 0, 0, true);

            //time it
            long time = System.currentTimeMillis();

            long fileLength = File2.length(fileName); //was 20580000
            Test.ensureTrue(fileLength > 20570000, "fileName=" + fileName + " length=" + fileLength); 
            table = new Table();
            table.readNDNc(fileName, null, null, 0, 0, true);

            String results = table.dataToCSVString(3);
            String expected =  //before 2011-06-14 was 32.31, -75.35
"row,TIME,DEPTH,LAT,LON,WD,WSPD,GST,WVHT,DPD,APD,MWD,BAR,ATMP,WTMP,DEWP,VIS,PTDY,TIDE,WSPU,WSPV,ID\n" +
"0,1.235556E8,0.0,32.309,-75.483,149,1.5,-9999999.0,-9999999.0,-9999999.0,-9999999.0,,1031.0,15.5,-9999999.0,5.4,-9999999.0,-9999999.0,-9999999.0,-0.8,1.3,41002\n" +
"1,1.235592E8,0.0,32.309,-75.483,145,0.3,-9999999.0,-9999999.0,-9999999.0,-9999999.0,,1031.0,13.9,-9999999.0,7.3,-9999999.0,-9999999.0,-9999999.0,-0.2,0.2,41002\n" +
"2,1.235628E8,0.0,32.309,-75.483,315,1.4,-9999999.0,-9999999.0,-9999999.0,-9999999.0,,1031.0,11.4,-9999999.0,6.5,-9999999.0,-9999999.0,-9999999.0,1.0,-1.0,41002\n";
            Test.ensureEqual(results, expected, "results=\n" + results);
            Test.ensureEqual(table.nColumns(), 21, "nColumns=" + table.nColumns()); 
            Test.ensureTrue(table.nRows() >= 309736, "nRows=" + table.nRows()); 

            time = System.currentTimeMillis() - time;
            String2.log("********** Done. cells/ms=" + 
                (table.nColumns() * table.nRows()/time) + " (usual=9679)" +
                " time=" + time + " ms (usual=640,  java 1.5 was 828, but varies a lot)"); 
            if (time > 1000)
                throw new SimpleException("readNDNc took too long.");
            Math2.sleep(5000);
        } catch (Exception e) {
            String2.getStringFromSystemIn(
                MustBe.throwableToString(e) +
                "\nUnexpected ERROR.  Press ^C to stop or Enter to continue..."); 
        }
    }


    /** Test the speed of readOpendapSequence */
    public static void testReadOpendapSequenceSpeed() throws Exception {

        try {
            //warm up
            String2.log("\n*** Table.testReadOpendapSequenceSpeed\n");
            String url = 
                "http://coastwatch.pfeg.noaa.gov/erddap/tabledap/cwwcNDBCMet?&time>=1999-01-01&time<=1999-04-01&station=%2241009%22";
            Table table = new Table();
            table.readOpendapSequence(url);

            //time it
            long time = System.currentTimeMillis();
            table = new Table();
            table.readOpendapSequence(url);
            String results = table.dataToCSVString(3);            
            String expected = //before 2011-06-14 was -80.17, 28.5
"row,station,longitude,latitude,time,wd,wspd,gst,wvht,dpd,apd,mwd,bar,atmp,wtmp,dewp,vis,ptdy,tide,wspu,wspv\n" +
"0,41009,-80.166,28.519,9.151488E8,0,1.9,2.7,1.02,11.11,6.49,,1021.0,20.4,24.2,-9999999.0,-9999999.0,-9999999.0,-9999999.0,0.0,-1.9\n" +
"1,41009,-80.166,28.519,9.151524E8,53,1.5,2.8,0.99,11.11,6.67,,1021.0,20.6,24.5,-9999999.0,-9999999.0,-9999999.0,-9999999.0,-1.2,-0.9\n" +
"2,41009,-80.166,28.519,9.15156E8,154,1.0,2.2,1.06,11.11,6.86,,1021.2,20.6,24.6,-9999999.0,-9999999.0,-9999999.0,-9999999.0,-0.4,0.9\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            Test.ensureTrue(table.nRows() > 2100, "nRows=" + table.nRows());
            time = System.currentTimeMillis() - time;
            String2.log("********** Done. cells/ms=" + 
                (table.nColumns() * table.nRows()/time) + " (usual=106)" +
                " time=" + time + " ms (usual=406,  java 1.5 was 562)");  
            if (time > 700)
                throw new SimpleException("readOpendapSequence took too long.");
            Math2.sleep(5000);
        } catch (Exception e) {
            String2.getStringFromSystemIn(
                MustBe.throwableToString(e) +
                "\nUnexpected ERROR.  Press ^C to stop or Enter to continue..."); 
        }
    }


    /** Test the speed of saveAs speed */
    public static void testSaveAsSpeed() throws Exception {

        try {
            //warmup
            String2.log("\n*** Table.testSaveAsSpeed\n");
            String sourceName = "F:/data/ndbc/ndbcMetHistoricalTxt/41009h1990.txt"; 
            String destName = File2.getSystemTempDirectory() + "testSaveAsSpeed";
            Table table = new Table();
            table.readASCII(sourceName);
            Test.ensureEqual(table.nColumns(), 16, "nColumns=" + table.nColumns()); 
            Test.ensureEqual(table.nRows(), 17117, "nRows=" + table.nRows()); 
            table.saveAsCsvASCII(destName + ".csv");
            table.saveAsJson(destName + ".json", table.findColumnNumber("time"), true); //writeUnits
            table.saveAsFlatNc(destName + ".nc", "row");

            //time it
            String2.log("\ntime it\n");

            //saveAsCsvASCII
            long time = System.currentTimeMillis();
            table.saveAsCsvASCII(destName + ".csv");
            time = System.currentTimeMillis() - time;
            String2.log("saveAsCsvASCII done. cells/ms=" + (table.nColumns() * table.nRows() / time) + //796
                " time=" + time + " ms  (expected=344)"); 
            File2.delete(destName + ".csv");
            if (time > 550)
                throw new SimpleException("saveAsCsvASCII took too long. Expected=~532 for 17117 rows.");

            //saveAsJson
            time = System.currentTimeMillis();
            table.saveAsJson(destName + ".json", table.findColumnNumber("time"), true); //writeUnits
            time = System.currentTimeMillis() - time;
            String2.log("saveAsJson done. cells/ms=" + (table.nColumns() * table.nRows() / time) +   //974
                " time=" + time + " ms  (expect=281)"); 
            File2.delete(destName + ".json");
            if (time > 400)
                throw new SimpleException("saveAsJson took too long. Expected=~515 for 17117 rows.");

            //saveAsFlatNc
            time = System.currentTimeMillis();
            table.saveAsFlatNc(destName + ".nc", "row");
            time = System.currentTimeMillis() - time;
            String2.log("saveAsFlatNc done. cells/ms=" + (table.nColumns() * table.nRows() / time) + //2190
                " time=" + time + " ms  (expected=125)"); 
            File2.delete(destName + ".nc");
            if (time > 200)
                throw new SimpleException("saveAsFlatNc took too long. Expected=~172 for 17117 rows.");
            

        } catch (Exception e) {
            String2.getStringFromSystemIn(
                MustBe.throwableToString(e) +
                "\nUnexpected ERROR.  Press ^C to stop or Enter to continue..."); 
        }
    }



    /**
     * This is a test of readOpendap.
     *
     * @throws Exception of trouble
     */
    public static void testOpendap() throws Exception {
        //*************
        String2.log("\n*** Table.testOpendap");
        verbose = true;
        reallyVerbose = true;

        //opendap, even sequence data, can be read via .nc
        //  but constraints are not supported
        Table table = new Table();
        int nRows = 3779;
        table.readFlatNc(
            //read all via ascii: "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1.asc?abund_m3,lat,long", null);
            //or                  "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1.asc?MOC1.abund_m3,MOC1.lat,MOC1.long", null);
            "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1", 
            new String[]{"MOC1.abund_m3", "MOC1.lat", "MOC1.long"},  //but "MOC1." is required here
            2); //2=unpack to doubles
        String2.log(table.toString("row", 5));

        Test.ensureEqual(table.nColumns(), 3, "");
        Test.ensureEqual(table.nRows(), nRows, "");

        Test.ensureEqual(table.getColumnName(0), "abund_m3", "");
        Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Abundance m3", "");
        Test.ensureEqual(table.getDoubleData(0, 0), 0.242688983, "");
        Test.ensureEqual(table.getDoubleData(0, nRows-1), 0.248962652, "");

        Test.ensureEqual(table.getColumnName(1), "lat", "");
        Test.ensureEqual(table.columnAttributes(1).getString("long_name"), "Latitude", "");
        Test.ensureEqual(table.getDoubleData(1, 0), 44.6517, "");
        Test.ensureEqual(table.getDoubleData(1, nRows-1), 44.6517, "");

        Test.ensureEqual(table.getColumnName(2), "long", "");
        Test.ensureEqual(table.columnAttributes(2).getString("long_name"), "Longitude", "");
        Test.ensureEqual(table.getDoubleData(2, 0), -124.175, "");
        Test.ensureEqual(table.getDoubleData(2, nRows-1), -124.65, "");

        //can it read with list of variables?
        table.readFlatNc(
            "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1?abund_m3,lat,long",
            null, //read all variables
            2); //2=unpack to doubles
        String2.log(table.toString("row", 5));
        Test.ensureEqual(table.nColumns(), 3, "");
        Test.ensureEqual(table.nRows(), nRows, "");

        //!!!HEY, the results are an unexpected order!!!
        Test.ensureEqual(table.getColumnName(0), "lat", "");
        Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Latitude", "");
        Test.ensureEqual(table.getDoubleData(0, 0), 44.6517, "");
        Test.ensureEqual(table.getDoubleData(0, nRows-1), 44.6517, "");

        Test.ensureEqual(table.getColumnName(1), "long", "");
        Test.ensureEqual(table.columnAttributes(1).getString("long_name"), "Longitude", "");
        Test.ensureEqual(table.getDoubleData(1, 0), -124.175, "");
        Test.ensureEqual(table.getDoubleData(1, nRows-1), -124.65, "");
 
        Test.ensureEqual(table.getColumnName(2), "abund_m3", "");
        Test.ensureEqual(table.columnAttributes(2).getString("long_name"), "Abundance m3", "");
        Test.ensureEqual(table.getDoubleData(2, 0), 0.242688983, "");
        Test.ensureEqual(table.getDoubleData(2, nRows-1), 0.248962652, "");

    }

    /**
     * This tests the little methods.
     */
    public static void testLittleMethods() {
        String2.log("\n*** testLittleMethods...");
        verbose = true;
        reallyVerbose = true;

        //isValid   and findColumnNumber   and subset
        Table table = getTestTable(true, true);
        table.ensureValid(); //throws Exception if not
        Test.ensureEqual(table.findColumnNumber("Time"), 0, "");
        Test.ensureEqual(table.findColumnNumber("String Data"), 8, "");
        Test.ensureEqual(table.findColumnNumber("zz"), -1, "");

      
        //toString
        table = getTestTable(true, true);
        String2.log("toString: " + table.toString("row", Integer.MAX_VALUE));

        //ensureEqual
        Table table2 = getTestTable(true, true);
        Test.ensureTrue(table.equals(table2), "test equals a");

        String2.log("intentional error:\n");
        table2.getColumn(2).setDouble(1, 100);
        Test.ensureEqual(table.equals(table2), false, "intentional notEqual b");

        String2.log("intentional error:\n");
        table2 = getTestTable(true, true);
        table2.getColumn(0).setDouble(2, 55);
        Test.ensureEqual(table.equals(table2), false, "intentional notEqual c");

        //getSubset
        /*table = getTestTable();
        table.getSubset(new int[]{table.secondsColumn},
            new double[]{stringToSeconds("2005-08-31T16:01:01")}, 
            new double[]{stringToSeconds("2005-08-31T16:01:03")});
        Test.ensureEqual(table.nRows(), 1, "getSubset a");
        Test.ensureEqual(table.nColumns(), 5, "getSubset b");
        Test.ensureEqual(table.getColumn(table.secondsColumn), new double[]{stringToSeconds("2005-08-31T16:01:02")}, "getSubset c"); 
        Test.ensureEqual(table.getColumn(table.lonColumn),     new double[]{-2}, "getSubset d"); 
        Test.ensureEqual(table.getColumn(table.latColumn),     new double[]{1.5}, "getSubset e"); 
        Test.ensureEqual(table.getColumn(3)[0], 7, "getSubset f"); 
        Test.ensureEqual(table.getColumn(4)[0], 8, "getSubset g"); 

        table = getTestTable();
        table.getSubset(new int[]{table.latColumn},
            new double[]{1.9},
            new double[]{2.1});
        Test.ensureEqual(table.nRows(), 1, "getSubset b");
        Test.ensureEqual(table.getColumn(table.latColumn), new double[]{2}, "getSubset j"); 
        */

        //calculateStats   look at array via constants and as array
        table = getTestTable(true, true);
        Test.ensureEqual(table.getColumnName(1), "Longitude", "columnNames a");
        table.setColumnName(1, "Test");
        Test.ensureEqual(table.getColumnName(1), "Test",      "columnNames b");
        table.setColumnName(1, "Longitude");
        Test.ensureEqual(table.getColumnName(1), "Longitude", "columnNames c");

        double stats[] = table.getColumn(1).calculateStats();
        Test.ensureEqual(stats[PrimitiveArray.STATS_N],    3, "calculateStats n"); 
        Test.ensureEqual(stats[PrimitiveArray.STATS_MIN], -3, "calculateStats min"); 
        Test.ensureEqual(stats[PrimitiveArray.STATS_MAX], -1, "calculateStats max"); 

        //forceLonPM180(boolean pm180)
        table = getTestTable(true, true);
        PrimitiveArray lonAr = table.getColumn(1);
        forceLonPM180(lonAr, false);
        Test.ensureEqual(lonAr.toString(), "357, 358, 359", "forceLonPM180f"); 
        Table.forceLonPM180(lonAr, true);
        Test.ensureEqual(lonAr.toString(), "-3, -2, -1", "forceLonPM180t"); 

        //clear
        table = getTestTable(true, true);
        table.clear();
        Test.ensureEqual(table.nRows(), 0, "clear a");
        Test.ensureEqual(table.nColumns(), 0, "clear b");

        //getXxxAttribute
        table = getTestTable(true, true);
        int col = table.findColumnNumber("Double Data");
        Test.ensureEqual(col, 3, "");
        Test.ensureEqual(table.globalAttributes().getString("global_att1"), "a string", "");
        Test.ensureEqual(table.globalAttributes().getString("test"), null, "");
        table.globalAttributes().set("global_att1", "new");
        Test.ensureEqual(table.globalAttributes().getString("global_att1"), "new", "");
        table.globalAttributes().remove("global_att1");
        Test.ensureEqual(table.globalAttributes().getString("global_att1"), null, "");

        Test.ensureEqual(table.columnAttributes(3).getString("units"), "doubles", "");
        table.columnAttributes(3).set("units", "new");
        Test.ensureEqual(table.columnAttributes(3).getString("units"), "new", "");
        table.columnAttributes(3).remove("units");
        Test.ensureEqual(table.columnAttributes(3).getString("units"), null, "");
        Test.ensureEqual(table.getDoubleData(3,1), 3.123, "");
        Test.ensureEqual(table.getStringData(3,1), "3.123", "");
        table.setColumnName(3, "new3");
        Test.ensureEqual(table.getColumnName(3), "new3", "");

        //sort
        table.sort(new int[]{3}, new boolean[]{false});
        Test.ensureEqual(table.getColumn(2).toString(), "2.0, 1.5, 1.0", "");
        Test.ensureEqual(table.getColumn(3).toString(), "1.0E300, 3.123, -1.0E300", "");

        //removeColumn
        table.removeColumn(3);
        Test.ensureEqual(table.getColumn(3).toString(), "2000000000000000, 2, -2000000000000000", "");
        Test.ensureEqual(table.getColumnName(3), "Long Data", "");
        Test.ensureEqual(table.columnAttributes(3).getString("units"), "longs", "");

        //addColumn
        table.addColumn(3, "test3", new IntArray(new int[]{10,20,30}));
        Test.ensureEqual(table.getColumn(3).toString(), "10, 20, 30", "");
        Test.ensureEqual(table.getColumnName(3), "test3", "");        
        Test.ensureEqual(table.getColumn(4).toString(), "2000000000000000, 2, -2000000000000000", "");
        Test.ensureEqual(table.getColumnName(4), "Long Data", "");
        Test.ensureEqual(table.columnAttributes(4).getString("units"), "longs", "");

        //append
        table.append(table);
        Test.ensureEqual(table.getColumn(4).toString(), 
            "2000000000000000, 2, -2000000000000000, 2000000000000000, 2, -2000000000000000", "");
        Test.ensureEqual(table.getColumnName(4), "Long Data", "");
        Test.ensureEqual(table.columnAttributes(4).getString("units"), "longs", "");

        //average
        table = new Table();
        DoubleArray da = new DoubleArray(new double[]{10,20,30,40,50});
        table.addColumn("a", da);
        da = new DoubleArray(new double[]{0,0,1,2,2});
        table.addColumn("b", da);
        table.average(new int[]{1});
        Test.ensureEqual(table.getColumn(0).toString(), "15.0, 30.0, 45.0", "");
        Test.ensureEqual(table.getColumn(1).toString(), "0.0, 1.0, 2.0", "");

    } 

    /** Test join(). **/
    public static void testJoin() {

        //*** testJoin 1
        String2.log("\n*** testJoin 1 column");
        Table table = new Table();
        table.addColumn("zero", PrimitiveArray.csvFactory(String.class, "a,b,c,d,,e"));
        table.addColumn("one",  PrimitiveArray.csvFactory(int.class, "40,10,12,30,,20"));
        table.addColumn("two",  PrimitiveArray.csvFactory(String.class, "aa,bb,cc,dd,,ee"));
        table.columnAttributes(0).add("long_name", "hey zero");
        table.columnAttributes(1).add("missing_value", -99999);
        table.columnAttributes(2).add("long_name", "hey two");
        
        Table lut = new Table();
        lut.addColumn("aa", PrimitiveArray.csvFactory(int.class, "10,20,30,40"));
        lut.addColumn("bb", PrimitiveArray.csvFactory(String.class, "11,22,33,44"));
        lut.addColumn("cc", PrimitiveArray.csvFactory(long.class, "111,222,333,444"));
        lut.columnAttributes(0).add("missing_value", -99999);
        lut.columnAttributes(1).add("long_name", "hey bb");
        lut.columnAttributes(2).add("missing_value", -9999999L);

        //test lut before join
        String results = lut.toCSVString();
        String expectedLut = 
"{\n" +
"dimensions:\n" +
"\trow = 4 ;\n" +
"\tbb_strlen = 2 ;\n" +
"variables:\n" +
"\tint aa(row) ;\n" +
"\t\taa:missing_value = -99999 ;\n" +
"\tchar bb(row, bb_strlen) ;\n" +
"\t\tbb:long_name = \"hey bb\" ;\n" +
"\tlong cc(row) ;\n" +
"\t\tcc:missing_value = -9999999 ;\n" +
"\n" +
"// global attributes:\n" +
"}\n" +
"row,aa,bb,cc\n" +
"0,10,11,111\n" +
"1,20,22,222\n" +
"2,30,33,333\n" +
"3,40,44,444\n";
        Test.ensureEqual(results, expectedLut, "lut results=\n" + results);

        //do the join
        table.join(1, 1, "10", lut);

        results = table.toCSVString();
        String expected = 
"{\n" +
"dimensions:\n" +
"\trow = 6 ;\n" +
"\tzero_strlen = 1 ;\n" +
"\tbb_strlen = 2 ;\n" +
"\ttwo_strlen = 2 ;\n" +
"variables:\n" +
"\tchar zero(row, zero_strlen) ;\n" +
"\t\tzero:long_name = \"hey zero\" ;\n" +
"\tint one(row) ;\n" +
"\t\tone:missing_value = -99999 ;\n" +
"\tchar bb(row, bb_strlen) ;\n" +
"\t\tbb:long_name = \"hey bb\" ;\n" +
"\tlong cc(row) ;\n" +
"\t\tcc:missing_value = -9999999 ;\n" +
"\tchar two(row, two_strlen) ;\n" +
"\t\ttwo:long_name = \"hey two\" ;\n" +
"\n" +
"// global attributes:\n" +
"}\n" +
"row,zero,one,bb,cc,two\n" +
"0,a,40,44,444,aa\n" +
"1,b,10,11,111,bb\n" +
"2,c,12,,-9999999,cc\n" +
"3,d,30,33,333,dd\n" +
"4,,,11,111,\n" +
"5,e,20,22,222,ee\n";
        Test.ensureEqual(results, expected, "join 1 results=\n" + results);

        //ensure lut unchanged
        results = lut.toCSVString();
        Test.ensureEqual(results, expectedLut, "lut 1 results=\n" + results);


        //*** testJoin 2 columns
        String2.log("\n*** testJoin 2 columns");
        table = new Table();
        table.addColumn("zero", PrimitiveArray.csvFactory(String.class, "a,b,c,d,,e"));
        table.addColumn("one",  PrimitiveArray.csvFactory(int.class, "40,10,12,30,,20"));
        table.addColumn("two",  PrimitiveArray.csvFactory(String.class, "44,bad,1212,33,,22"));
        table.addColumn("three",PrimitiveArray.csvFactory(String.class, "aaa,bbb,ccc,ddd,,eee"));
        table.columnAttributes(0).add("long_name", "hey zero");
        table.columnAttributes(1).add("missing_value", -99999);
        table.columnAttributes(2).add("long_name", "hey two");
        table.columnAttributes(3).add("long_name", "hey three");

        //do the join
        table.join(2, 1, "10\t11", lut);

        results = table.toCSVString();
        expected = 
"{\n" +
"dimensions:\n" +
"\trow = 6 ;\n" +
"\tzero_strlen = 1 ;\n" +
"\ttwo_strlen = 4 ;\n" +
"\tthree_strlen = 3 ;\n" +
"variables:\n" +
"\tchar zero(row, zero_strlen) ;\n" +
"\t\tzero:long_name = \"hey zero\" ;\n" +
"\tint one(row) ;\n" +
"\t\tone:missing_value = -99999 ;\n" +
"\tchar two(row, two_strlen) ;\n" +
"\t\ttwo:long_name = \"hey two\" ;\n" +
"\tlong cc(row) ;\n" +
"\t\tcc:missing_value = -9999999 ;\n" +
"\tchar three(row, three_strlen) ;\n" +
"\t\tthree:long_name = \"hey three\" ;\n" +
"\n" +
"// global attributes:\n" +
"}\n" +
"row,zero,one,two,cc,three\n" +
"0,a,40,44,444,aaa\n" +
"1,b,10,bad,-9999999,bbb\n" +
"2,c,12,1212,-9999999,ccc\n" +
"3,d,30,33,333,ddd\n" +
"4,,,,111,\n" + 
"5,e,20,22,222,eee\n";
        Test.ensureEqual(results, expected, "join 2 results=\n" + results);

        //ensure lut unchanged
        results = lut.toCSVString();
        Test.ensureEqual(results, expectedLut, "lut 2 results=\n" + results);
    }

    /** test update() */
    public static void testUpdate() throws Exception {
        Table table = new Table();
        table.addColumn("zero", PrimitiveArray.csvFactory(String.class, "a,    b,  c,  d,   ,  e"));
        table.addColumn("one",  PrimitiveArray.csvFactory(int.class,    "10,  20, 30, 40,   , 50"));
        table.addColumn("two",  PrimitiveArray.csvFactory(int.class,    "111,222,333,444,-99,555"));
        table.addColumn("three",PrimitiveArray.csvFactory(double.class, "1.1,2.2,3.3,4.4,4.6,5.5"));
        table.columnAttributes(2).add("missing_value", -99);

        //otherTable rows: matches, matches, partial match, new
        //otherTable cols: keys, matches (but different type), doesn't match
        Table otherTable = new Table(); 
        otherTable.addColumn("one",  PrimitiveArray.csvFactory(int.class,    " 50,   , 11,  5"));
        otherTable.addColumn("zero", PrimitiveArray.csvFactory(String.class, "  e,   ,  a,  f"));
        otherTable.addColumn("three",PrimitiveArray.csvFactory(int.class,    " 11, 22, 33, 44"));
        otherTable.addColumn("five", PrimitiveArray.csvFactory(int.class,    "  1,  2,  3,  4"));

        int nMatched = table.update(new String[]{"zero", "one"}, otherTable);
        String results = table.dataToCSVString();
        String expected = 
"zero,one,two,three\n" +
"a,10,111,1.1\n" +
"b,20,222,2.2\n" +
"c,30,333,3.3\n" +
"d,40,444,4.4\n" +
",,-99,22.0\n" +  
"e,50,555,11.0\n" +
"a,11,-99,33.0\n" + //-99 is from missing_value
"f,5,-99,44.0\n";   //-99 is from missing_value
        Test.ensureEqual(results, expected, "update results=\n" + results);
        Test.ensureEqual(nMatched, 2, "nMatched");

    }



    /**
     * A main method -- used to test the methods in this class.
     *
     * @param args is ignored  (use null)
     * @throws Exception if trouble
     */
    public static void main(String args[]) throws Exception {

        verbose = true;
        reallyVerbose = true;

        /* */
        testLittleMethods();
        testSortColumnsByName();
        
        //readWrite tests
        testASCII();
        testHtml();
        testJson();
        testFlatNc();
        test4DNc();
        testSaveAsMatlab();
        testOpendapSequence(); 
        //testOpendap(); //not done yet, see opendapSequence
        testReadNDNc();
        testReadNDNc2();
        testJoin();
        testReadStandardTabbedASCII();

        testReadASCIISpeed();
        testReadJsonSpeed();
        testReadNDNcSpeed();
        testReadOpendapSequenceSpeed();
        testSaveAsSpeed();
        testUpdate();

        try {
            testConvert();
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from cimt failure? Press 'Enter' to continue or ^C to stop...");
        }

        /* not active
        try {
            testSql();
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from testSql failure? Press 'Enter' to continue or ^C to stop...");
        }
        */
        testXml();

        /*not active -- it needs work to deal with sessions
        try {
            testIobis();
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn(
                "\nRecover from testIobis failure (1007-09-10: it needs work to deal with sessions)?\n" +
                "Press 'Enter' to continue or ^C to stop...");
        }*/

        //done
        String2.log("\n***** Table.main finished successfully");

    }


}
