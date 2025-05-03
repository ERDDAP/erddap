package gov.noaa.pfel.erddap.filetypes;

public abstract class FileTypeInterface {

  public abstract void writeTableToStream(DapRequestInfo requestInfo) throws Throwable;

  public abstract void writeGridToStream(DapRequestInfo requestInfo) throws Throwable;

  public abstract String getHelpText(int language);

  public String getTableHelpText(int language) {
    return getHelpText(language);
  }

  public String getGridHelpText(int language) {
    return getHelpText(language);
  }
}
