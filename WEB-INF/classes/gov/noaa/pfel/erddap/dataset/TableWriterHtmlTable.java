/* 
 * TableWriterHtmlTable Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.CharArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
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

    /** 
     * This prevents naive requests for large amounts of data in an HtmlTable
     * from crashing the user's browser.     
     * It specifies the maximum number of MegaBytes that will ever be written to 
     * an HTML table (since large tables often crash browsers).
     * It is not final so EDstatic can change it from htmTableMaxMB in messages.xml.
     * (Note that XHTML tables have no limit since they are intended for 
     * processing by a program, usually other than a browser.)
     * 50 here causes Firefox 13.0.1 on Windows XP to crash.
     * 25 here causes Google Chrome 19.0 on Windows XP to crash.
     */
//FUTURE: can browsers handle more (and faster?) if I use .css and class= to properties are shared?
    public static int htmlTableMaxMB = 15;

    //set by constructor
    protected String loggedInAs, fileNameNoExt, preTableHtml, postTableHtml, 
        tErddapUrl, externalLinkHtml;
    protected boolean writeHeadAndBodyTags, xhtmlMode, encode, writeUnits;
    protected int totalRows = 0, rowsShown = 0;
    protected int showFirstNRows;  //perhaps modified by htmlTableMaxMB in "do firstTime stuff"
    protected String noWrap;

    //set firstTime
    protected boolean isCharOrString[];
    protected boolean isTimeStamp[];
    protected String time_precision[];
    protected String fileAccessBaseUrl[];
    protected BufferedWriter writer;

    //set later
    public boolean isMBLimited = false; //ie, did htmlTableMaxMB reduce showFirstNRows?
    public boolean allDataDisplayed = true;

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
     *   HTML tables are limited by htmlTableMaxMB.  
     *   HTML and XHTML tables are limited by tShowFirstNRows.
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
     * @param tShowFirstNRows if &gt;= 0, this only shows the specified number of rows,
     *   then ignores the remaining rows.
     */
    public TableWriterHtmlTable(EDD tEdd, String tNewHistory, String tLoggedInAs, 
        OutputStreamSource tOutputStreamSource,        
        boolean tWriteHeadAndBodyTags, String tFileNameNoExt, boolean tXhtmlMode,         
        String tPreTableHtml, String tPostTableHtml,
        boolean tEncode, boolean tWriteUnits, int tShowFirstNRows) {

        super(tEdd, tNewHistory, tOutputStreamSource);
        loggedInAs = tLoggedInAs;
        writeHeadAndBodyTags = tWriteHeadAndBodyTags;
        fileNameNoExt = tFileNameNoExt;
        xhtmlMode = tXhtmlMode;
        preTableHtml = tPreTableHtml;
        postTableHtml = tPostTableHtml;
        encode = tEncode;
        writeUnits = tWriteUnits;
        showFirstNRows = tShowFirstNRows >= 0? tShowFirstNRows : Integer.MAX_VALUE;
        noWrap = xhtmlMode? " nowrap=\"nowrap\"" : " nowrap"; 
        tErddapUrl = EDStatic.erddapUrl(loggedInAs);
        externalLinkHtml = EDStatic.externalLinkHtml(tErddapUrl);
    }

    /** This encodes s as XML or HTML */
    String encode(String s) {
        //encodingSpaces for HTML ensures spacing info won't be lost when rendered in a browser.
        //encodingSpaces for XML is debatable. 
        //Thankfully, only leading, trailing, and consecutive spaces (all rare) are encoded.
        if (xhtmlMode) {
            s = XML.encodeAsXML(s);
        } else {
            s = String2.toJson(s, 65536);
            s = XML.encodeAsHTML(s.substring(1, s.length() - 1));
        }
        return XML.minimalEncodeSpaces(s);    
    }

    /**
     * This adds the current contents of table (a chunk of data) to the OutputStream.
     * <br>This calls ensureCompatible each time it is called.
     * <br>If this is the first time this is called, this does first time things
     *   (e.g., call OutputStreamSource.outputStream() and write file header).
     * <br>The number of columns, the column names, and the types of columns 
     *   must be the same each time this is called.
     *
     * <p>TimeStamp columns are detected by 
     *   units=EDV.TIME_UNITS ("seconds since 1970-01-01T00:00:00Z") or
     *   EDV.TIME_UCUM_UNITS ("s{since 1970-01-01T00:00:00Z}").
     *   If a timeStamp column has a time_precision attribute, it is used
     *   to format the times.
     *
     * <p>The table should have missing values stored as destinationMissingValues
     * or destinationFillValues.
     * This implementation converts them to NaNs.
     *
     * @param table with destinationValues
     * @throws Throwable if trouble
     */
    public void writeSome(Table table) throws Throwable {
//Future: use try/catch and EDStatic.htmlForException(Exception e) ???

        if (table.nRows() == 0) 
            return;
        if (rowsShown >= showFirstNRows) {
            noMoreDataPlease = true;
            return;
        }

        //ensure the table's structure is the same as before
        boolean firstTime = columnNames == null;
        //xhtml: safari objects to &nbsp;   
        //   ie doesn't show cells with "" or space.
        //most html ignore cells with ""; "&nbsp;" is common and universally ok
        String emptyCell = xhtmlMode? "" : "&nbsp;"; 
        ensureCompatible(table);

        int nColumns = table.nColumns();
        PrimitiveArray colPA[] = new PrimitiveArray[nColumns];
        for (int col = 0; col < nColumns; col++) 
            colPA[col] = table.getColumn(col);

        //do firstTime stuff
        if (firstTime) {
            isTimeStamp = new boolean[nColumns];
            time_precision = new String[nColumns];
            fileAccessBaseUrl = new String[nColumns];
            int bytesPerRow = (xhtmlMode? 10 : 5) +  //e.g., <tr> </tr> \n
                nColumns * (xhtmlMode? 9 : 4) ;      //e.g., <td> </td>
            for (int col = 0; col < nColumns; col++) {
                Attributes catts = table.columnAttributes(col);
                fileAccessBaseUrl[col] = catts.getString("fileAccessBaseUrl"); //null if none
                String u = catts.getString("units");
                isTimeStamp[col] = u != null && 
                    (u.equals(EDV.TIME_UNITS) || u.equals(EDV.TIME_UCUM_UNITS));
                if (isTimeStamp[col]) {
                    //for xhtmlMode, just keep time_precision if it includes fractional seconds 
                    String tp = catts.getString(EDV.TIME_PRECISION);
                    if (xhtmlMode && tp != null && !tp.startsWith("1970-01-01T00:00:00.0")) 
                        tp = null; //default
                    time_precision[col] = tp;
                }

                if (isTimeStamp[col]) {
                    bytesPerRow += 20 + noWrap.length();
                } else {
                    if (colPA[col] instanceof StringArray) {
                        bytesPerRow += noWrap.length() +
                            Math.max(10, ((StringArray)colPA[col]).maxStringLength());
                    } else {
                        bytesPerRow += (3 * colPA[col].elementSize()) / 2 + 14;  //1.5*nBytes->~nChar.  14: align="right"
                    }
                }
            }

            //adjust showFirstNRows
            if (!xhtmlMode) {
                int tMaxRows = Math2.roundToInt(
                    (htmlTableMaxMB * (double)Math2.BytesPerMB) / bytesPerRow);
                if (reallyVerbose) String2.log("htmlTableMaxMB=" + htmlTableMaxMB +
                    " bytesPerRow=" + bytesPerRow + " tMaxRows=" + tMaxRows +
                    " oShowFirstNRows=" + showFirstNRows);
                if (tMaxRows < showFirstNRows) {
                    isMBLimited = true;
                    showFirstNRows = tMaxRows;
                }
            }

            //write the header
            writer = new BufferedWriter(new OutputStreamWriter(
                outputStreamSource.outputStream(String2.UTF_8), String2.UTF_8)); 
            if (writeHeadAndBodyTags) {
                if (xhtmlMode)
                    writer.write(
                        HtmlWidgets.DOCTYPE_XHTML_TRANSITIONAL + //specifies UTF-8
                        "  <title>" + XML.encodeAsXML(fileNameNoExt) + "</title>\n" +
                        "</head>\n" +
                        "<body style=\"color:black; background:white; " + HtmlWidgets.SANS_SERIF_STYLE + "\">\n");
                else {
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

            //write the column names   and gather isCharOrString 
            isCharOrString = new boolean[nColumns];
            writer.write("<tr>\n");
            for (int col = 0; col < nColumns; col++) {
                String s = table.getColumnName(col);
                writer.write("<th>" +                     
                    (encode? encode(s) : s) + 
                    (xhtmlMode? "</th>" : "") + //HTML doesn't require it, so save bandwidth
                    "\n");
                isCharOrString[col] = colPA[col] instanceof CharArray ||
                                      colPA[col] instanceof StringArray;
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

        //write how many rows?
        int nRows = table.nRows();
        int showNRows = nRows;
        if (rowsShown + nRows > showFirstNRows) {    //there are excess rows
            showNRows = showFirstNRows - rowsShown;  //may be negative        
            allDataDisplayed = false;
            noMoreDataPlease = true;
        }

        //write the data
        for (int row = 0; row < showNRows; row++) {
            writer.write("<tr>\n");
            for (int col = 0; col < nColumns; col++) {
                if (isTimeStamp[col]) {
                    double d = colPA[col].getDouble(row);
                    if (Double.isNaN(d))
                         writer.write("<td>" + emptyCell);
                    else  
                        writer.write("<td" + noWrap + ">" + 
                            Calendar2.epochSecondsToLimitedIsoStringT(
                                time_precision[col], d, ""));
                } else {
                    String s = colPA[col].getString(row);
                    if (s.length() == 0) {
                         writer.write("<td>" + emptyCell); 
                    } else if (isCharOrString[col]) {
                        if (encode) {
                            if (xhtmlMode) {
                                s = encode(s);
                            } else {
                                //if html, display urls and email addresses as links
                                if (fileAccessBaseUrl[col] != null) {
                                    //display as a link
                                    boolean isLocal = fileAccessBaseUrl[col].startsWith(EDStatic.baseUrl);
                                    s = "<a href=\"" + 
                                        XML.encodeAsHTMLAttribute(fileAccessBaseUrl[col] + s) + 
                                        "\">" + XML.encodeAsHTMLAttribute(s) + //just the fileName
                                       (isLocal? "" : externalLinkHtml) + "</a>";
                                } else if (String2.isUrl(s)) {
                                    //display as a link
                                    boolean isLocal = s.startsWith(EDStatic.baseUrl);
                                    s = XML.encodeAsHTMLAttribute(s);
                                    s = "<a href=\"" + s + "\">" + s + 
                                       (isLocal? "" : externalLinkHtml) + "</a>";
                                } else if (String2.isEmailAddress(s)) {
                                    //display as a mailTo link
                                    s = XML.encodeAsHTMLAttribute(s);
                                    s = "<a href=\"mailto:" + s + "\">" + s + "</a>";
                                } else {
                                    s = encode(s);
                                }
                            }
                        }
                        writer.write("<td" + noWrap + ">" + s); 
                    } else if (s.indexOf('-') >= 0) { //it's a numeric column  //nowrap because can break at minus sign
                        writer.write("<td" + noWrap + " align=\"right\">" + s); 
                    } else {
                        writer.write("<td align=\"right\">" + s); 
                    }
                }
                writer.write(
                    (xhtmlMode? "</td>" : "") + //HTML doesn't require it, so save bandwidth
                    "\n");
            }
            writer.write("</tr>\n");
            rowsShown++;
        }       
        totalRows += nRows;

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

        //close the table
        writer.write(
            "</table>\n");

        //  isMBLimited and !allDataDisplayed
        if (isMBLimited &&  !allDataDisplayed) 
            writer.write("<font class=\"warningColor\">" + 
                EDStatic.htmlTableMaxMessage + "</font>" +
                (xhtmlMode? "<br />" : "<br>") +
                "\n");  

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

    /** This returns the total number of rows of data received so far. */
    public int totalRows() {return totalRows;}

    /** This returns the total number of rows of data shown so far. */
    public int rowsShown() {return rowsShown;}


    /**
     * This is a convenience method to write an entire table in one step.
     *
     * @throws Throwable if trouble  (no columns is trouble; no rows is not trouble)
     */
    public static void writeAllAndFinish(EDD tEdd, String tNewHistory, 
        String loggedInAs,
        Table table, OutputStreamSource outputStreamSource,
        boolean writeHeadAndBodyTags, String fileNameNoExt, boolean xhtmlMode, 
        String preTableHtml, String postTableHtml,
        boolean encode, boolean writeUnits, int tShowFirstNRows) throws Throwable {

        TableWriterHtmlTable tw = new TableWriterHtmlTable(tEdd, tNewHistory, 
            loggedInAs, outputStreamSource,  
            writeHeadAndBodyTags, fileNameNoExt, xhtmlMode, preTableHtml, postTableHtml, 
            encode, writeUnits, tShowFirstNRows);
        tw.writeAllAndFinish(table);
    }

}



