package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterGeoJson;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".json",
    fileTypeName = ".geoJson",
    infoUrl = "http://wiki.geojson.org/Main_Page",
    versionAdded = "1.82.0",
    availableGrid = false)
public class GeoJsonFiles extends TableWriterFileType {

  @Override
  public TableWriter generateTableWriter(DapRequestInfo requestInfo) throws Exception {
    return new TableWriterGeoJson(
        requestInfo.language(),
        requestInfo.edd(),
        requestInfo.newHistory(),
        requestInfo.outputStream(),
        EDStatic.getJsonpFromQuery(requestInfo.language(), requestInfo.userDapQuery()));
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_geoJsonAr[language];
  }
}
