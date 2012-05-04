/* 
 * TableWriterHtmlTable Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.HtmlWidgets;
import gov.noaa.pfel.erddap.variable.EDV;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

/**
 * TableWriterHtmlTable provides a way to write a table to an HTML or XHTML Table 
 * outputStream in chunks so that the whole table doesn't have to be in memory 
 * at one time.
 * This is used by EDDTable.
 * The outputStream isn't obtained until the first call to writeSome().
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-08-23
 */
public class TableWriterHtmlTable extends TableWriter {

    //set by constructor
    protected String loggedInAs, fileNameNoExt, preTableHtml, postTableHtml;
    protected boolean writeHeadAndBodyTags, xhtmlMode, encode, writeUnits;

    //set firstTime
    protected boolean isTimeStamp[];
    protected boolean isString[];
    protected BufferedWriter writer;

    /**
     * The constructor.
     *
     * @param tOutputStreamSource  the source of an outputStream that receives the 
     *     results, usually already buffered.
     *     The ouputStream is not procured until there is data to be written.
     * @param tWriteHeadAndBodyTags  If true, this writes an entire document.
     *     If false, it just writes the table.
     * @param tFileNameNoExt is the fileName without dir or extension (used for the web page title).
     * @param tXhtmlMode if true, the table is stored as an XHTML table.
     *   If false, it is stored as an HTML table.
     * @param tPreTableHtml is valid html (or xhtml) text to be inserted at the 
     *   start of the body of the document, before the table tag
     *   (or "" if none).
     * @param tPostTableXhtml is valid html (or xhtml) text to be inserted at 
     *   the end of the body of the document, after the table tag
     *   (or "" if none).
     * @param tEncode if true, strings are encoded as XML/HTML (depending on tXhtmlMode)
     *   (e.g., lessThan, greaterThan, doubleQuotes, etc. are converted
     *   to character entities).
     *   Otherwise, they are stored as is (presumably already XML/HTML).
     * @param tWriteUnits if true, the second row of the table will have units.
     */
    public TableWriterHtmlTable(String tLoggedInAs, OutputStreamSource tOutputStreamSource,        
        boolean tWriteHeadAndBodyTags, String tFileNameNoExt, boolean tXhtmlMode,         
        String tPreTableHtml, String tPostTableHtml,
        boolean tEncode, boolean tWriteUnits) {

        super(tOutputStreamSource);
        loggedInAs = tLoggedInAs;
        writeHeadAndBodyTags = tWriteHeadAndBodyTags;
        fileNameNoExt = tFileNameNoExt;
        xhtmlMode = tXhtmlMode;
        preTableHtml = tPreTableHtml;
        postTableHtml = tPostTableHtml;
        encode = tEncode;
        writeUnits = tWriteUnits;
    }

    /** This encodes s as XML or HTML */
    String encode(String s) {
        return xhtmlMode? XML.encodeAsXML(s) : XML.encodeAsHTML(s);
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
     * This implementation converts them to NaNs.
     *
     * @param table with destinationValues
     * @throws Throwable if trouble
     */
    public void writeSome(Table table) throws Throwable {
        if (table.nRows() == 0) 
            return;

        //ensure the table's structure is the same as before
        boolean firstTime = columnNames == null;
        //xhtml: safari objects to &nbsp;   
        //   ie doesn't show cells with "" or space.
        //most html ignore cells with ""; "&nbsp;" is common and universally ok
        String emptyCell = xhtmlMode? "" : "&nbsp;"; 
        ensureCompatible(table);

        //do firstTime stuff
        int nColumns = table.nColumns();
        if (firstTime) {
            isTimeStamp = new boolean[nColumns];
            for (int col = 0; col < nColumns; col++) {
                String u = table.columnAttributes(col).getString("units");
                isTimeStamp[col] = u != null && 
                    (u.equals(EDV.TIME_UNITS) || u.equals(EDV.TIME_UCUM_UNITS));
            }

            //write the header
            writer = new BufferedWriter(new OutputStreamWriter(
                outputStreamSource.outputStream("UTF-8"), "UTF-8")); 
//Future: use try/catch and EDStatic.htmlForException(Exception e) ???
            if (writeHeadAndBodyTags) {
                if (xhtmlMode)
                    writer.write(
                        HtmlWidgets.DOCTYPE_XHTML_TRANSITIONAL + //specifies UTF-8
                        "  <title>" + XML.encodeAsXML(fileNameNoExt) + "</title>\n" +
                        "</head>\n" +
                        "<body style=\"color:black; background:white; " + HtmlWidgets.SANS_SERIF_STYLE + "\">\n");
                else {
                    String tErddapUrl = EDStatic.erddapUrl(loggedInAs);
                    writer.write(EDStatic.startHeadHtml(tErddapUrl, fileNameNoExt));
                    writer.write("\n</head>\n");
                    writer.write(EDStatic.startBodyHtml(loggedInAs));
                    writer.write("\n" +
                        "&nbsp;\n" + //necessary for the blank line before form (not <p>)
                        HtmlWidgets.BACK_BUTTON);
                }
            }

            writer.write(preTableHtml);
            writer.write("\n&nbsp;\n"); //necessary for the blank line before table (not <p>)
            writer.write(xhtmlMode?
                //Opera doesn't seem to use class/style info in xhtml  (see xhtmlMode use above, too)
                //keep XHTML plain (no style, no bgcolor), since admin can't set text color (above)
                "<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" : 
                "<table class=\"erd commonBGColor\" cellspacing=\"0\">\n");

            //write the column names   and gather isString
            isString = new boolean[nColumns];
            writer.write("<tr>\n");
            for (int col = 0; col < nColumns; col++) {
                String s = table.getColumnName(col);
                writer.write("<th>" +                     
                    (encode? encode(s) : s) + 
                    (xhtmlMode? "</th>" : "") + //HTML doesn't require it, so save bandwidth
                    "\n");
                isString[col] = table.getColumn(col) instanceof StringArray;
            }
            writer.write("</tr>\n");

            //write the units   
            if (writeUnits) {
                writer.write("<tr>\n");
                for (int col = 0; col < nColumns; col++) {
                    String tUnits = table.columnAttributes(col).getString("units");
                    if (isTimeStamp[col])  tUnits = "UTC"; //no longer true: "seconds since 1970-01-01..."
                    if (tUnits == null)    tUnits = "";
                    if (encode)            tUnits = encode(tUnits);
                    writer.write("<th>" + 
                        (tUnits.length() == 0? emptyCell : tUnits) + 
                        (xhtmlMode? "</th>" : "") + //HTML doesn't require it, so save bandwidth
                        "\n");
                }
                writer.write("</tr>\n");
            }

        }

        //*** do everyTime stuff
        convertToStandardMissingValues(table);  //NaNs; not the method in Table, so metadata is unchanged

        //write the data
        String noWrap = xhtmlMode? " nowrap=\"nowrap\"" : " nowrap"; //if xhtml, need: nowrap="nowrap"
        int nRows = table.nRows();
        for (int row = 0; row < nRows; row++) {
            writer.write("<tr>\n");
            for (int col = 0; col < nColumns; col++) {
                if (isTimeStamp[col]) {
                    double d = table.getDoubleData(col, row);
                    if (Double.isNaN(d))
                         writer.write("<td>" + emptyCell);
                    else  
                        writer.write("<td" + noWrap + ">" + Calendar2.epochSecondsToIsoStringT(d) + "Z");
                } else {
                    String s = table.getStringData(col, row);
                    if (s.length() == 0)
                         writer.write("<td>" + emptyCell); 
                    else if (isString[col])
                        writer.write("<td" + noWrap + ">" + (encode? encode(s) : s)); 
                    else if (s.indexOf('-') >= 0) //it's a numeric column  //nowrap because can break at minus sign
                        writer.write("<td" + noWrap + " align=\"right\">" + s); 
                    else 
                        writer.write("<td align=\"right\">" + s); 
                }
                writer.write(
                    (xhtmlMode? "</td>" : "") + //HTML doesn't require it, so save bandwidth
                    "\n");
            }
            writer.write("</tr>\n");
        }       

        //ensure it gets to user right away
        if (nRows > 1) //some callers work one row at a time; avoid excessive flushing
            writer.flush(); 
    }

    
    /**
     * This writes any end-of-file info to the stream and flushes the stream.
     *
     * @throws Throwable if trouble (e.g., EDStatic.THERE_IS_NO_DATA if there is no data)
     */
    public void finish() throws Throwable {
        //check for EDStatic.THERE_IS_NO_DATA
        if (writer == null)
            throw new SimpleException(EDStatic.THERE_IS_NO_DATA);

        //close the table
        writer.write(
            "</table>\n");

        //close the document
        writer.write(postTableHtml);
        if (writeHeadAndBodyTags)
            if (xhtmlMode)
                writer.write(
                    "</body>\n" +
                    "</html>\n");
            else writer.write(
                EDStatic.endBodyHtml(EDStatic.erddapUrl(loggedInAs)) +
                "\n</html>\n");

        writer.flush(); //essential

        //diagnostic
        if (verbose)
            String2.log("TableWriterHtmlTable done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");

    }

    /**
     * This is a convenience method to write an entire table in one step.
     *
     * @throws Throwable if trouble  (no columns is trouble; no rows is not trouble)
     */
    public static void writeAllAndFinish(String loggedInAs,
        Table table, OutputStreamSource outputStreamSource,
        boolean writeHeadAndBodyTags, String fileNameNoExt, boolean xhtmlMode, 
        String preTableHtml, String postTableHtml,
        boolean encode, boolean writeUnits) throws Throwable {

        TableWriterHtmlTable tw = new TableWriterHtmlTable(loggedInAs,
            outputStreamSource,  
            writeHeadAndBodyTags, fileNameNoExt, xhtmlMode, preTableHtml, postTableHtml, 
            encode, writeUnits);
        tw.writeAllAndFinish(table);
    }

}



