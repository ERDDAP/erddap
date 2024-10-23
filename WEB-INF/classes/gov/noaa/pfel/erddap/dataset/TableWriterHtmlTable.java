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
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.HtmlWidgets;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.BufferedWriter;

/**
 * TableWriterHtmlTable provides a way to write a table to an HTML or XHTML Table outputStream in
 * chunks so that the whole table doesn't have to be in memory at one time. This is used by
 * EDDTable. The outputStream isn't obtained until the first call to writeSome().
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-08-23
 */
public class TableWriterHtmlTable extends TableWriter {

  /**
   * This prevents naive requests for large amounts of data in an HtmlTable from crashing the user's
   * browser. It specifies the maximum number of MegaBytes that will ever be written to an HTML
   * table (since large tables often crash browsers). It is not final so EDstatic can change it from
   * htmTableMaxMB in messages.xml. (Note that XHTML tables have no limit since they are intended
   * for processing by a program, usually other than a browser.) 50 here causes Firefox 13.0.1 on
   * Windows XP to crash. 25 here causes Google Chrome 19.0 on Windows XP to crash.
   */
  // FUTURE: can browsers handle more (and faster?) if I use .css and class= to properties are
  // shared?
  public static int htmlTableMaxMB = 15;

  // set by constructor
  protected String loggedInAs,
      endOfRequest,
      queryString,
      fileNameNoExt,
      preTableHtml,
      postTableHtml,
      tErddapUrl,
      externalLinkHtml,
      questionMarkImageUrl;
  protected boolean writeHeadAndBodyTags, xhtmlMode, encode, writeUnits;
  protected int totalRows = 0, rowsShown = 0;
  protected int showFirstNRows; // perhaps modified by htmlTableMaxMB in "do firstTime stuff"
  protected String noWrap;

  // set firstTime
  protected volatile boolean isCharOrString[];
  protected volatile boolean isTimeStamp[];
  protected volatile String time_precision[];
  protected volatile String fileAccessBaseUrl[];
  protected volatile String fileAccessSuffix[];
  protected volatile BufferedWriter writer;

  // set later
  public volatile boolean isMBLimited = false; // ie, did htmlTableMaxMB reduce showFirstNRows?
  public volatile boolean allDataDisplayed = true;

  /**
   * The constructor.
   *
   * @param language the index of the selected language
   * @param tOutputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered. The ouputStream is not procured until there is data to be written.
   * @param tWriteHeadAndBodyTags If true, this writes an entire document. If false, it just writes
   *     the table.
   * @param tFileNameNoExt is the fileName without dir or extension (used for the web page title).
   * @param tXhtmlMode if true, the table is stored as an XHTML table. If false, it is stored as an
   *     HTML table. HTML tables are limited by htmlTableMaxMB. HTML and XHTML tables are limited by
   *     tShowFirstNRows.
   * @param tPreTableHtml is valid html (or xhtml) text to be inserted at the start of the body of
   *     the document, before the table tag (or "" if none).
   * @param tPostTableXhtml is valid html (or xhtml) text to be inserted at the end of the body of
   *     the document, after the table tag (or "" if none).
   * @param tEncode if true, strings are encoded as XML/HTML (depending on tXhtmlMode) (e.g.,
   *     lessThan, greaterThan, doubleQuotes, etc. are converted to character entities). Otherwise,
   *     they are stored as is (presumably already XML/HTML).
   * @param tWriteUnits if true, the second row of the table will have units.
   * @param tShowFirstNRows if &gt;= 0, this only shows the specified number of rows, then ignores
   *     the remaining rows.
   */
  public TableWriterHtmlTable(
      int tLanguage,
      EDD tEdd,
      String tNewHistory,
      String tLoggedInAs,
      String tEndOfRequest,
      String tQueryString,
      OutputStreamSource tOutputStreamSource,
      boolean tWriteHeadAndBodyTags,
      String tFileNameNoExt,
      boolean tXhtmlMode,
      String tPreTableHtml,
      String tPostTableHtml,
      boolean tEncode,
      boolean tWriteUnits,
      int tShowFirstNRows,
      String tQuestionMarkImageUrl) {

    super(tLanguage, tEdd, tNewHistory, tOutputStreamSource);
    loggedInAs = tLoggedInAs;
    endOfRequest = tEndOfRequest;
    queryString = tQueryString;
    writeHeadAndBodyTags = tWriteHeadAndBodyTags;
    fileNameNoExt = tFileNameNoExt;
    xhtmlMode = tXhtmlMode;
    preTableHtml = tPreTableHtml;
    postTableHtml = tPostTableHtml;
    encode = tEncode;
    writeUnits = tWriteUnits;
    showFirstNRows = tShowFirstNRows >= 0 ? tShowFirstNRows : Integer.MAX_VALUE;
    tErddapUrl = EDStatic.erddapUrl(loggedInAs, language);
    externalLinkHtml = EDStatic.externalLinkHtml(language, tErddapUrl);
    questionMarkImageUrl = tQuestionMarkImageUrl;
  }

  /** This encodes s as XML or HTML */
  String encode(String s) {
    // encodingSpaces for HTML ensures spacing info won't be lost when rendered in a browser.
    // encodingSpaces for XML is debatable.
    // Thankfully, only leading, trailing, and consecutive spaces (all rare) are encoded.
    if (xhtmlMode) {
      s = XML.encodeAsXML(s);
    } else {
      s = String2.toJson(s, 65536);
      s = XML.encodeAsHTML(s.substring(1, s.length() - 1));
    }
    return XML.minimalEncodeSpaces(s);
  }

  /**
   * This adds the current contents of table (a chunk of data) to the OutputStream. <br>
   * This calls ensureCompatible each time it is called. <br>
   * If this is the first time this is called, this does first time things (e.g., call
   * OutputStreamSource.outputStream() and write file header). <br>
   * The number of columns, the column names, and the types of columns must be the same each time
   * this is called.
   *
   * <p>TimeStamp columns are detected by units=EDV.TIME_UNITS ("seconds since
   * 1970-01-01T00:00:00Z") or EDV.TIME_UCUM_UNITS ("s{since 1970-01-01T00:00:00Z}"). If a timeStamp
   * column has a time_precision attribute, it is used to format the times.
   *
   * @param table with destinationValues. The table should have missing values stored as
   *     destinationMissingValues or destinationFillValues. This implementation converts them to
   *     NaNs.
   * @throws Throwable if trouble
   */
  @Override
  public void writeSome(Table table) throws Throwable {
    // Future: use try/catch and EDStatic.htmlForException(language, Exception e) ???

    if (table.nRows() == 0) return;
    if (rowsShown >= showFirstNRows) {
      noMoreDataPlease = true;
      return;
    }

    // ensure the table's structure is the same as before
    boolean firstTime = columnNames == null;
    boolean somethingWritten;
    ensureCompatible(table);

    int nColumns = table.nColumns();
    PrimitiveArray colPA[] = new PrimitiveArray[nColumns];
    for (int col = 0; col < nColumns; col++) colPA[col] = table.getColumn(col);

    // do firstTime stuff
    if (firstTime) {
      isTimeStamp = new boolean[nColumns];
      time_precision = new String[nColumns];
      fileAccessBaseUrl = new String[nColumns];
      fileAccessSuffix = new String[nColumns];
      int bytesPerRow =
          (xhtmlMode ? 10 : 5)
              + // e.g., <tr> </tr> \n
              nColumns * (xhtmlMode ? 9 : 4); // e.g., <td> </td>
      for (int col = 0; col < nColumns; col++) {
        Attributes catts = table.columnAttributes(col);
        fileAccessBaseUrl[col] = catts.getString("fileAccessBaseUrl"); // null if none
        fileAccessSuffix[col] = catts.getString("fileAccessSuffix"); // null if none
        if (!String2.isSomething(fileAccessBaseUrl[col])) fileAccessBaseUrl[col] = "";
        if (!String2.isSomething(fileAccessSuffix[col])) fileAccessSuffix[col] = "";
        String u = catts.getString("units");
        isTimeStamp[col] = u != null && (u.equals(EDV.TIME_UNITS) || u.equals(EDV.TIME_UCUM_UNITS));
        if (isTimeStamp[col]) {
          // for xhtmlMode, just keep time_precision if it includes fractional seconds
          String tp = catts.getString(EDV.TIME_PRECISION);
          if (xhtmlMode && tp != null && !tp.startsWith("1970-01-01T00:00:00.0"))
            tp = null; // default
          time_precision[col] = tp;
        }

        if (isTimeStamp[col]) {
          bytesPerRow += 20;
        } else {
          if (colPA[col] instanceof StringArray) {
            bytesPerRow += 10 + Math.max(10, ((StringArray) colPA[col]).maxStringLength());
          } else {
            bytesPerRow +=
                (3 * colPA[col].elementSize()) / 2
                    + 14; // 1.5*nBytes->~nChar.  14: text-align:right
          }
        }
      }

      // adjust showFirstNRows
      if (!xhtmlMode) {
        int tMaxRows = Math2.roundToInt((htmlTableMaxMB * (double) Math2.BytesPerMB) / bytesPerRow);
        if (reallyVerbose)
          String2.log(
              "htmlTableMaxMB="
                  + htmlTableMaxMB
                  + " bytesPerRow="
                  + bytesPerRow
                  + " tMaxRows="
                  + tMaxRows
                  + " oShowFirstNRows="
                  + showFirstNRows);
        if (tMaxRows < showFirstNRows) {
          isMBLimited = true;
          showFirstNRows = tMaxRows;
        }
      }

      // write the header
      writer = File2.getBufferedWriterUtf8(outputStreamSource.outputStream(File2.UTF_8));
      if (writeHeadAndBodyTags) {
        if (xhtmlMode)
          writer.write(
              HtmlWidgets.DOCTYPE_XHTML_TRANSITIONAL
                  + // specifies UTF-8
                  "  <title>"
                  + XML.encodeAsXML(fileNameNoExt)
                  + "</title>\n"
                  + "  <link rel=\"stylesheet\" type=\"text/css\" href=\""
                  + EDStatic.imageDirUrl(loggedInAs, language)
                  + "erddap2.css\" />\n"
                  + // xhtml has closing /
                  "</head>\n"
                  + "<body>\n");
        else {
          writer.write(EDStatic.startHeadHtml(language, tErddapUrl, fileNameNoExt));
          writer.write("\n</head>\n");
          writer.write(
              EDStatic.startBodyHtml(
                  language,
                  loggedInAs,
                  edd == null
                      ? "index.html"
                      : // this happens when .htmlTable is used for non-dataset data. Fall back to
                      // index.html
                      edd.dapProtocol()
                          + "/"
                          + edd.datasetID()
                          + ".html", // was endOfRequest. Now should be .htmlTable, but since no
                  // params that would request entire dataset, so go back to
                  // .html form
                  queryString));
          // writer.write(HtmlWidgets.BACK_BUTTON);
          writer.write("&nbsp;<br>");
        }
      }

      writer.write(preTableHtml);
      writer.write("\n&nbsp;\n"); // necessary for the blank line before table (not <p>)
      writer.write(
          // Opera doesn't seem to use class/style info in xhtml  (see xhtmlMode use above, too)
          // keep XHTML plain (no style, no bgcolor), since admin can't set text color (above)
          "<table class=\"erd commonBGColor nowrap\">\n");

      // write the column names   and gather isCharOrString
      isCharOrString = new boolean[nColumns];
      writer.write("<tr>\n");
      somethingWritten = false;
      for (int col = 0; col < nColumns; col++) {
        String s = table.getColumnName(col);
        if (encode) s = encode(s);

        if (somethingWritten) {
        } else if (s.trim().length() > 0) {
          somethingWritten = true;
        } else if (col == nColumns - 1) {
          s = xhtmlMode ? "&#160;" : "&nbsp;";
        }

        writer.write(
            "<th>"
                + s
                + (xhtmlMode ? "</th>" : "")
                + // HTML doesn't require it, so save bandwidth
                "\n");
        isCharOrString[col] = colPA[col] instanceof CharArray || colPA[col] instanceof StringArray;
      }
      writer.write("</tr>\n");

      // write the units
      if (writeUnits) {
        writer.write("<tr>\n");
        somethingWritten = false;
        for (int col = 0; col < nColumns; col++) {
          String tUnits = table.columnAttributes(col).getString("units");
          if (isTimeStamp[col]) tUnits = "UTC"; // no longer true: "seconds since 1970-01-01..."
          if (tUnits == null) tUnits = "";
          if (encode) tUnits = encode(tUnits);

          if (somethingWritten) {
          } else if (tUnits.trim().length() > 0) {
            somethingWritten = true;
          } else if (col == nColumns - 1) {
            tUnits = xhtmlMode ? "&#160;" : "&nbsp;";
          }
          writer.write(
              "<th>"
                  + tUnits
                  + (xhtmlMode ? "</th>" : "")
                  + // HTML doesn't require it, so save bandwidth
                  "\n");
        }
        writer.write("</tr>\n");
      }
    }

    // *** do everyTime stuff
    table.convertToStandardMissingValues(); // to NaNs

    // write how many rows?
    int nRows = table.nRows();
    int showNRows = nRows;
    if (rowsShown + nRows > showFirstNRows) { // there are excess rows
      showNRows = showFirstNRows - rowsShown; // may be negative
      allDataDisplayed = false;
      noMoreDataPlease = true;
    }

    // write the data
    boolean baseHttpsUrlIsSomething = String2.isSomething(EDStatic.baseHttpsUrl);
    for (int row = 0; row < showNRows; row++) {
      writer.write("<tr>\n");
      somethingWritten = false;
      for (int col = 0; col < nColumns; col++) {
        String s;
        if (isTimeStamp[col]) {
          double d = colPA[col].getDouble(row);
          writer.write("<td>");
          if (Double.isNaN(d)) s = "";
          else s = Calendar2.epochSecondsToLimitedIsoStringT(time_precision[col], d, "");
          writer.write(s);
        } else {
          s = colPA[col].getString(row);
          if (s == null || s.length() == 0) {
            writer.write("<td>");

          } else if (isCharOrString[col]) {
            if (encode) {
              if (xhtmlMode) {
                s = encode(s);
              } else {
                // if html, display urls and email addresses as links
                String url = null;
                if (fileAccessBaseUrl[col].length() > 0 || fileAccessSuffix[col].length() > 0) {
                  // display as a link
                  url = fileAccessBaseUrl[col] + s + fileAccessSuffix[col];
                  boolean isLocal =
                      url.startsWith(EDStatic.baseUrl)
                          || (baseHttpsUrlIsSomething && url.startsWith(EDStatic.baseHttpsUrl));
                  s =
                      "<a href=\""
                          + XML.encodeAsHTMLAttribute(url)
                          + "\">"
                          + encode(s)
                          + // just the fileName
                          (isLocal ? "" : externalLinkHtml)
                          + "</a>";
                } else if (String2.isUrl(s)) {
                  // display as a link
                  url = s;
                  boolean isLocal =
                      url.startsWith(EDStatic.baseUrl)
                          || (baseHttpsUrlIsSomething && url.startsWith(EDStatic.baseHttpsUrl));
                  s =
                      "<a href=\""
                          + XML.encodeAsHTMLAttribute(url)
                          + "\">"
                          + encode(s)
                          + (isLocal ? "" : externalLinkHtml)
                          + "</a>";
                } else if (String2.isEmailAddress(s)) {
                  // to improve security, convert "@" to " at "
                  s = XML.encodeAsHTMLAttribute(String2.replaceAll(s, "@", " at "));
                } else if (s.startsWith("data:image/png;base64,")) {
                  url = s;
                } else {
                  s = encode(s);
                }

                if (url != null) {
                  // if media url, show '?' with viewer
                  // very similar code in Table.directoryListing and TableWriterHtmlTable.
                  int whichIcon = File2.whichIcon(url);
                  String iconAlt = File2.ICON_ALT.get(whichIcon); // always 3 characters

                  // make HTML for a viewer?
                  String viewer = "";
                  if (iconAlt.equals("SND")) {
                    viewer = HtmlWidgets.cssTooltipAudio(questionMarkImageUrl, "?", "", url);

                  } else if (iconAlt.equals("IMG")) {
                    // this system doesn't need to know the size ahead of time
                    viewer =
                        HtmlWidgets.cssTooltipImage(
                            questionMarkImageUrl, "?", "", url, "img" + (totalRows + row));

                  } else if (iconAlt.equals("MOV")) {
                    viewer = HtmlWidgets.cssTooltipVideo(questionMarkImageUrl, "?", "", url);

                  } else if (iconAlt.equals("UNK") && url.startsWith("data:image/png;base64,")) {
                    viewer =
                        HtmlWidgets.cssTooltipImageBase64(
                            questionMarkImageUrl,
                            "?",
                            "",
                            url,
                            "img" + (totalRows + row),
                            language);
                    s = "";
                  }
                  if (viewer.length() > 0) s = viewer + "&nbsp;" + s;
                }
              }
            }
            writer.write("<td>" + s);
          } else { // a non-MV number
            writer.write("<td class=\"R\">" + s);
          }
        }
        // ensure something written on each row (else row is very narrow)
        if (somethingWritten) {
        } else if (s.trim().length() > 0) {
          somethingWritten = true;
        } else if (col == nColumns - 1) {
          writer.write(xhtmlMode ? "&#160;" : "&nbsp;");
        }
        if (xhtmlMode) writer.write("</td>"); // HTML doesn't require it, so save bandwidth
        writer.write("\n");
      }
      writer.write("</tr>\n");
      rowsShown++;
    }
    boolean flushAfterward = totalRows == 0; // flush initial chunk so info gets to user quickly
    totalRows += nRows;

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
    if (writer == null) throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");

    // close the table
    writer.write("</table>\n");

    //  isMBLimited and !allDataDisplayed
    if (isMBLimited && !allDataDisplayed)
      writer.write(
          "<span class=\"warningColor\">"
              + EDStatic.htmlTableMaxMessageAr[language]
              + "</span>"
              + (xhtmlMode ? "<br />" : "<br>")
              + "\n");

    // close the document
    writer.write(postTableHtml);
    if (writeHeadAndBodyTags)
      if (xhtmlMode) writer.write("</body>\n" + "</html>\n");
      else
        writer.write(
            EDStatic.endBodyHtml(language, EDStatic.erddapUrl(loggedInAs, language), loggedInAs)
                + "\n</html>\n");

    writer.flush(); // essential

    // diagnostic
    if (verbose)
      String2.log(
          "TableWriterHtmlTable done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }

  /** This returns the total number of rows of data received so far. */
  public int totalRows() {
    return totalRows;
  }

  /** This returns the total number of rows of data shown so far. */
  public int rowsShown() {
    return rowsShown;
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
      String loggedInAs,
      Table table,
      String endOfRequest,
      String queryString,
      OutputStreamSource outputStreamSource,
      boolean writeHeadAndBodyTags,
      String fileNameNoExt,
      boolean xhtmlMode,
      String preTableHtml,
      String postTableHtml,
      boolean encode,
      boolean writeUnits,
      int tShowFirstNRows)
      throws Throwable {

    TableWriterHtmlTable tw =
        new TableWriterHtmlTable(
            language,
            tEdd,
            tNewHistory,
            loggedInAs,
            endOfRequest,
            queryString,
            outputStreamSource,
            writeHeadAndBodyTags,
            fileNameNoExt,
            xhtmlMode,
            preTableHtml,
            postTableHtml,
            encode,
            writeUnits,
            tShowFirstNRows,
            EDStatic.imageDirUrl(loggedInAs, language) + EDStatic.questionMarkImageFile);
    tw.writeAllAndFinish(table);
  }
}
