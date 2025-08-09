package gov.noaa.pfel.erddap.jte;

import com.cohort.array.Attributes;
import com.cohort.util.Calendar2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import java.util.List;

public class TableOptions {

  private final String otherClasses;
  private final String bgColor;
  private final int border;
  private final boolean writeUnits;
  private final int timeColumn;
  private final boolean needEncodingAsHtml;
  private final boolean allowWrap;
  private final Table table;
  private boolean somethingWritten = false;
  private final String[] fileAccessBaseUrl;
  private final String[] fileAccessSuffix;

  private TableOptions(TableOptionsBuilder builder) {
    this.otherClasses = builder.otherClasses;
    this.bgColor = builder.bgColor;
    this.border = builder.border;
    this.writeUnits = builder.writeUnits;
    this.timeColumn = builder.timeColumn;
    this.needEncodingAsHtml = builder.needEncodingAsHtml;
    this.allowWrap = builder.allowWrap;
    this.table = builder.table;
    int nCols = table.nColumns();
    this.fileAccessBaseUrl = new String[nCols];
    this.fileAccessSuffix = new String[nCols];
  }

  public static class TableOptionsBuilder {
    private String otherClasses = "";
    private String bgColor = null;
    private int border = 0;
    private boolean writeUnits = false;
    private int timeColumn = -1;
    private boolean needEncodingAsHtml = false;
    private boolean allowWrap = true;
    private Table table;

    public TableOptionsBuilder(Table table) {
      this.table = table;
    }

    public TableOptionsBuilder otherClasses(String otherClasses) {
      this.otherClasses = otherClasses;
      return this;
    }

    public TableOptionsBuilder bgColor(String bgColor) {
      this.bgColor = bgColor;
      return this;
    }

    public TableOptionsBuilder border(int border) {
      this.border = border;
      return this;
    }

    public TableOptionsBuilder writeUnits(boolean writeUnits) {
      this.writeUnits = writeUnits;
      return this;
    }

    public TableOptionsBuilder timeColumn(int timeColumn) {
      this.timeColumn = timeColumn;
      return this;
    }

    public TableOptionsBuilder needEncodingAsHtml(boolean needEncodingAsHtml) {
      this.needEncodingAsHtml = needEncodingAsHtml;
      return this;
    }

    public TableOptionsBuilder allowWrap(boolean allowWrap) {
      this.allowWrap = allowWrap;
      return this;
    }

    public TableOptions build() {
      return new TableOptions(this);
    }
  }

  public String getNoData() {
    return MustBe.THERE_IS_NO_DATA;
  }

  public String getWrap() {
    return allowWrap ? "" : " nowrap";
  }

  public String getOtherClasses() {
    return (otherClasses == null || otherClasses.length() == 0 ? "" : " " + otherClasses);
  }

  public String getBgColor() {
    return bgColor == null ? "false" : bgColor;
  }

  public int getNColumns() {
    return table.nColumns();
  }

  public int getNRows() {
    return table.nRows();
  }

  public String insideForLoophead(int col) {
    String s = table.getColumnName(col);
    if (needEncodingAsHtml) {
      s = XML.encodeAsHTML(s);
    }
    if (!somethingWritten && s.trim().length() > 0) {
      somethingWritten = true;
    } else if (col == getNColumns() - 1 && s.trim().length() == 0) {
      s = "&nbsp;";
    }

    Attributes catts = table.columnAttributes(col);
    fileAccessBaseUrl[col] = catts.getString("fileAccessBaseUrl");
    fileAccessSuffix[col] = catts.getString("fileAccessSuffix");
    if (!String2.isSomething(fileAccessBaseUrl[col])) fileAccessBaseUrl[col] = "";
    if (!String2.isSomething(fileAccessSuffix[col])) fileAccessSuffix[col] = "";

    return s;
  }

  public boolean getWriteUnits() {
    return writeUnits;
  }

  public String insideForLoopUnits(int col) {
    String tUnits = table.columnAttributes(col).getString("units");
    if (col == timeColumn) tUnits = "UTC";
    if (tUnits == null) tUnits = "";
    if (needEncodingAsHtml) tUnits = XML.encodeAsHTML(tUnits);
    if (!somethingWritten && tUnits.trim().length() > 0) {
      somethingWritten = true;
    } else if (col == getNColumns() - 1 && tUnits.trim().length() == 0) {
      tUnits = "&nbsp;";
    }
    return tUnits;
  }

  public int getTimeColumn() {
    return timeColumn;
  }

  public String getDoubleDataConverted(int col, int row) {
    return Calendar2.safeEpochSecondsToIsoStringTZ(table.getDoubleData(col, row), "");
  }

  public String getStringData(int col, int row) {
    return table.getStringData(col, row);
  }

  public boolean checkLengthFile(int col) {
    return (fileAccessBaseUrl[col].length() > 0 || fileAccessSuffix[col].length() > 0);
  }

  public String getTs(String s) {
    return needEncodingAsHtml ? s : XML.decodeEntities(s);
  }

  public String getUrl(int col, String s) {
    return fileAccessBaseUrl[col] + getTs(s) + fileAccessSuffix[col];
  }

  public String getEncodeashtml(String s) {
    return needEncodingAsHtml ? XML.encodeAsHTML(s) : s;
  }

  public boolean checkcontainsurl(String s) {
    return needEncodingAsHtml && String2.containsUrl(s);
  }

  public boolean checkmouseover(String s) {
    return !needEncodingAsHtml
        && !s.contains("href=")
        && !s.contains("onmouseover")
        && String2.containsUrl(XML.decodeEntities(s));
  }

  public List<String> getExtractUrls(String s) {
    return String2.extractUrls(s);
  }

  public boolean getContainsUrl(String s) {
    return String2.containsUrl(s);
  }

  public String encodeaddhttps(String s) {
    return XML.encodeAsHTMLAttribute(String2.addHttpsForWWW(s));
  }

  public boolean checkDecodeEntities(String s) {
    return !needEncodingAsHtml && String2.isUrl(XML.decodeEntities(s));
  }

  public String getEncodeashtmlattribute(String s) {
    return needEncodingAsHtml ? XML.encodeAsHTMLAttribute(s) : s;
  }

  public boolean checkIsEmail(String s) {
    return String2.isEmailAddress(s);
  }

  public String getEmailData(String input) {
    String decoded = needEncodingAsHtml ? input : XML.decodeEntities(input);
    return XML.encodeAsHTML(String2.replaceAll(decoded, "@", " at "));
  }

  public boolean checkEncodingAsHtml() {
    return needEncodingAsHtml;
  }
}
