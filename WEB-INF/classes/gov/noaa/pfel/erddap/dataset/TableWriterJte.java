/*
 * TableWriterJte: a TableWriter that uses JTE templates.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.CharArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.XML;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.WriterOutput;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.HtmlWidgets;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableWriterJte extends TableWriter {

  protected final HttpServletRequest request;
  protected final String loggedInAs;
  protected final String endOfRequest;
  protected final String queryString;
  protected final String fileNameNoExt;
  protected final boolean writeHeadAndBodyTags;
  protected final boolean xhtmlMode;
  protected final boolean encode;
  protected final boolean writeUnits;
  protected final String preTableHtml;
  protected final String postTableHtml;
  protected int totalRows = 0, rowsShown = 0;
  protected int showFirstNRows; // may be modified by MB limit

  // set firstTime
  protected volatile boolean isTimeStamp[];
  protected volatile java.time.format.DateTimeFormatter[] time_precision;
  protected volatile String fileAccessBaseUrl[];
  protected volatile String fileAccessSuffix[];
  protected volatile BufferedWriter writer;

  // set later
  public volatile boolean isMBLimited = false;
  public volatile boolean allDataDisplayed = true;

  // Buffer all rows until finish() so JTE can render a complete, balanced document
  protected List<String[]> allRows = new ArrayList<>();
  protected boolean[] rightAlignCol;
  protected String[] displayColumnNames;
  protected String[] displayUnits;

  private final String questionMarkImageUrl;

  public TableWriterJte(
      HttpServletRequest tRequest,
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
    request = tRequest;
    loggedInAs = tLoggedInAs;
    endOfRequest = tEndOfRequest;
    queryString = tQueryString;
    writeHeadAndBodyTags = tWriteHeadAndBodyTags;
    fileNameNoExt = tFileNameNoExt;
    xhtmlMode = tXhtmlMode;
    encode = tEncode;
    writeUnits = tWriteUnits;
    preTableHtml = tPreTableHtml == null ? "" : tPreTableHtml;
    postTableHtml = tPostTableHtml == null ? "" : tPostTableHtml;
    showFirstNRows = tShowFirstNRows >= 0 ? tShowFirstNRows : Integer.MAX_VALUE;
    questionMarkImageUrl = tQuestionMarkImageUrl;
  }

  private String encode(String s) {
    if (s == null) return "";
    if (xhtmlMode) {
      s = XML.encodeAsXML(s);
    } else {
      s = String2.toJson(s, 65536);
      s = XML.encodeAsHTML(s.substring(1, s.length() - 1));
    }
    return XML.minimalEncodeSpaces(s);
  }

  @Override
  public void writeSome(Table table) throws Throwable {
    if (table.nRows() == 0) return;
    if (rowsShown >= showFirstNRows) {
      noMoreDataPlease = true;
      return;
    }

    boolean firstTime = columnNames == null;
    ensureCompatible(table);

    int nColumns = columnNames.length;

    if (firstTime) {
      isTimeStamp = new boolean[nColumns];
      time_precision = new java.time.format.DateTimeFormatter[nColumns];
      fileAccessBaseUrl = new String[nColumns];
      fileAccessSuffix = new String[nColumns];
      int bytesPerRow = (xhtmlMode ? 10 : 5) + nColumns * (xhtmlMode ? 9 : 4);

      for (int col = 0; col < nColumns; col++) {
        Attributes catts = columnAttributes[col];
        fileAccessBaseUrl[col] = catts.getString("fileAccessBaseUrl");
        fileAccessSuffix[col] = catts.getString("fileAccessSuffix");
        if (!String2.isSomething(fileAccessBaseUrl[col])) fileAccessBaseUrl[col] = "";
        if (!String2.isSomething(fileAccessSuffix[col])) fileAccessSuffix[col] = "";
        String u = catts.getString("units");
        isTimeStamp[col] = u != null && (u.equals(EDV.TIME_UNITS) || u.equals(EDV.TIME_UCUM_UNITS));
        if (isTimeStamp[col]) {
          String tp = catts.getString(EDV.TIME_PRECISION);
          if (xhtmlMode && tp != null && !tp.startsWith("1970-01-01T00:00:00.0")) tp = null;
          time_precision[col] = Calendar2.timePrecisionToDateTimeFormatter(tp);
          bytesPerRow += 20;
        } else {
          PrimitiveArray pa = table.getColumn(col);
          if (pa instanceof StringArray) {
            bytesPerRow += 10 + Math.max(10, ((StringArray) pa).maxStringLength());
          } else {
            bytesPerRow += (3 * pa.elementSize()) / 2 + 14;
          }
        }
      }

      if (!xhtmlMode) {
        int tMaxRows =
            Math2.roundToInt(
                (TableWriterHtmlTable.htmlTableMaxMB * (double) Math2.BytesPerMB) / bytesPerRow);
        if (true)
          String2.log(
              "TableWriterJte: htmlTableMaxMB="
                  + TableWriterHtmlTable.htmlTableMaxMB
                  + " bytesPerRow="
                  + bytesPerRow
                  + " tMaxRows="
                  + tMaxRows
                  + " showFirstNRows="
                  + showFirstNRows);
        if (tMaxRows < showFirstNRows) {
          isMBLimited = true;
          showFirstNRows = tMaxRows;
        }
      }

      writer = File2.getBufferedWriterUtf8(outputStreamSource.outputStream(File2.UTF_8));

      // On first time, prepare column metadata for finish()
      displayColumnNames = new String[nColumns];
      displayUnits = new String[nColumns];
      rightAlignCol = new boolean[nColumns];
      for (int col = 0; col < nColumns; col++) {
        String s = columnNames[col];
        displayColumnNames[col] = encode ? encode(s) : s;
        String u = columnAttributes[col].getString("units");
        if (isTimeStamp[col]) u = "UTC";
        if (u == null) u = "";
        displayUnits[col] = encode ? encode(u) : u;

        rightAlignCol[col] =
            !isTimeStamp[col]
                && !(table.getColumn(col) instanceof CharArray
                    || table.getColumn(col) instanceof StringArray);
      }
    }

    table.convertToStandardMissingValues();
    int nRows = table.nRows();
    int showNRows = nRows;
    if (rowsShown + nRows > showFirstNRows) {
      showNRows = showFirstNRows - rowsShown;
      allDataDisplayed = false;
      noMoreDataPlease = true;
    }

    boolean baseHttpsUrlIsSomething = String2.isSomething(EDStatic.config.baseHttpsUrl);

    for (int row = 0; row < showNRows; row++) {
      String[] outRow = new String[nColumns];
      boolean somethingWritten = false;
      for (int col = 0; col < nColumns; col++) {
        String s;
        PrimitiveArray pa = table.getColumn(col);
        if (isTimeStamp[col]) {
          double d = pa.getDouble(row);
          if (Double.isNaN(d)) s = "";
          else s = Calendar2.epochSecondsToLimitedIsoStringT(time_precision[col], d, "");
        } else {
          s = pa.getString(row);
          if (s == null || s.length() == 0) {
            s = "";
          } else if (pa instanceof CharArray || pa instanceof StringArray) {
            if (encode) {
              if (xhtmlMode) {
                s = encode(s);
              } else {
                String url = null;
                if (fileAccessBaseUrl[col].length() > 0 || fileAccessSuffix[col].length() > 0) {
                  url = fileAccessBaseUrl[col] + s + fileAccessSuffix[col];
                  boolean isLocal =
                      url.startsWith(EDStatic.config.baseUrl)
                          || (baseHttpsUrlIsSomething
                              && url.startsWith(EDStatic.config.baseHttpsUrl));
                  s =
                      "<a href=\""
                          + XML.encodeAsHTMLAttribute(url)
                          + "\">"
                          + encode(s)
                          + (isLocal
                              ? ""
                              : EDStatic.messages.externalLinkHtml(
                                  language, EDStatic.erddapUrl(request, loggedInAs, language)))
                          + "</a>";
                } else if (String2.containsUrl(s)) {
                  List<String> separatedText = String2.extractUrls(s);
                  StringBuilder output = new StringBuilder();
                  for (String text : separatedText) {
                    if (String2.containsUrl(text)) {
                      url = text;
                      boolean isLocal =
                          url.startsWith(EDStatic.config.baseUrl)
                              || (baseHttpsUrlIsSomething
                                  && url.startsWith(EDStatic.config.baseHttpsUrl));
                      output.append(
                          "<a href=\""
                              + XML.encodeAsHTMLAttribute(String2.addHttpsForWWW(url))
                              + "\">"
                              + encode(text)
                              + (isLocal
                                  ? ""
                                  : EDStatic.messages.externalLinkHtml(
                                      language, EDStatic.erddapUrl(request, loggedInAs, language)))
                              + "</a>");
                    } else {
                      output.append(encode(text));
                    }
                  }
                  s = output.toString();
                } else if (String2.isEmailAddress(s)) {
                  s = XML.encodeAsHTMLAttribute(String2.replaceAll(s, "@", " at "));
                } else if (s.startsWith("data:image/png;base64,")) {
                  url = s;
                } else {
                  s = encode(s);
                }

                if (url != null) {
                  int whichIcon = File2.whichIcon(url);
                  String iconAlt = File2.ICON_ALT.get(whichIcon);
                  String viewer = "";
                  if (iconAlt.equals("SND")) {
                    viewer = HtmlWidgets.cssTooltipAudio(questionMarkImageUrl, "?", "", url);
                  } else if (iconAlt.equals("IMG")) {
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
          }
        }
        if (somethingWritten) {
        } else if (s.trim().length() > 0) {
          somethingWritten = true;
        } else if (col == nColumns - 1) {
          s = xhtmlMode ? "&#160;" : "&nbsp;";
        }
        outRow[col] = s;
      }
      allRows.add(outRow);
      rowsShown++;
    }

    totalRows += nRows;
  }

  @Override
  public void finish() throws Throwable {
    if (ignoreFinish) return;
    if (writer == null) {
      // No data was written at all; create writer and render empty table
      writer = File2.getBufferedWriterUtf8(outputStreamSource.outputStream(File2.UTF_8));
    }

    TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);
    Map<String, Object> params = new HashMap<>();

    if (writeHeadAndBodyTags) {
      params.put("title", fileNameNoExt);
      params.put("cssHref", EDStatic.imageDirUrl(request, loggedInAs, language));
      params.put("preTableHtml", preTableHtml);
      params.put("postTableHtml", postTableHtml);
      params.put("isMBLimited", isMBLimited && !allDataDisplayed);
      params.put(
          "htmlTableMaxMessage", EDStatic.messages.get(Message.HTML_TABLE_MAX_MESSAGE, language));
      params.put("xhtmlMode", xhtmlMode);
      params.put("writeHeader", !allRows.isEmpty()); // write header only if we have data
      params.put("columnNames", displayColumnNames);
      params.put("rightAlignCol", rightAlignCol);
      params.put("units", displayUnits);
      params.put("rows", allRows);

      // Generate head and body HTML using EDStatic methods for proper ERDDAP styling/navigation
      String headHtml;
      String bodyHtml;
      String endBodyHtml;
      if (xhtmlMode) {
        headHtml =
            "  <title>"
                + XML.encodeAsXML(fileNameNoExt)
                + "</title>\n"
                + "  <link rel=\"stylesheet\" type=\"text/css\" href=\""
                + EDStatic.imageDirUrl(request, loggedInAs, language)
                + "erddap2.css\" />\n";
        bodyHtml = "";
        endBodyHtml = "";
      } else {
        headHtml =
            EDStatic.startHeadHtml(
                    language, EDStatic.erddapUrl(request, loggedInAs, language), fileNameNoExt)
                + "\n</head>";
        bodyHtml =
            EDStatic.startBodyHtml(
                request,
                language,
                loggedInAs,
                edd == null ? "index.html" : edd.dapProtocol() + "/" + edd.datasetID() + ".html",
                queryString);
        endBodyHtml =
            EDStatic.endBodyHtml(
                    request,
                    language,
                    EDStatic.erddapUrl(request, loggedInAs, language),
                    loggedInAs)
                + "\n</html>";
      }

      params.put("headHtml", headHtml);
      params.put("bodyHtml", bodyHtml);
      params.put("endBodyHtml", endBodyHtml);

      engine.render("table/htmlTable_complete.jte", params, new WriterOutput(writer));
    } else {
      // Just render the table element without head/body tags
      params.put("writeHeader", !allRows.isEmpty());
      params.put("columnNames", displayColumnNames);
      params.put("rightAlignCol", rightAlignCol);
      params.put("units", displayUnits);
      params.put("rows", allRows);
      params.put("xhtmlMode", xhtmlMode);
      params.put("isMBLimited", isMBLimited && !allDataDisplayed);
      params.put(
          "htmlTableMaxMessage", EDStatic.messages.get(Message.HTML_TABLE_MAX_MESSAGE, language));

      engine.render("table/htmlTable_rows.jte", params, new WriterOutput(writer));
    }

    writer.flush();
  }

  @Override
  public void close() throws IOException {
    if (writer != null) {
      writer.close();
    }
  }
}
