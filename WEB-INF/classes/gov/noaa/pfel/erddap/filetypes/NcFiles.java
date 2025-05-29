package gov.noaa.pfel.erddap.filetypes;

import com.cohort.util.File2;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.TableWriterAllWithMetadata;
import gov.noaa.pfel.erddap.util.EDStatic;
import ucar.nc2.write.NetcdfFileFormat;

@FileTypeClass(
    fileTypeExtension = ".nc",
    fileTypeName = ".nc",
    infoUrl = "https://www.unidata.ucar.edu/software/netcdf/",
    versionAdded = "1.0.0",
    contentType = "application/x-netcdf")
public class NcFiles extends CacheLockFiles {

  public NcFiles() {
    super(false);
  }

  protected NcFiles(boolean headerOverride) {
    super(headerOverride);
  }

  @Override
  protected void generateTableFile(DapRequestInfo requestInfo, String cacheFullName)
      throws Throwable {
    EDDTable edd = requestInfo.getEDDTable();
    // if .ncHeader, make sure the .nc file exists
    // (and it is the better file to cache)
    TableWriterAllWithMetadata twawm =
        edd.getTwawmForDapQuery(
            requestInfo.language(),
            requestInfo.loggedInAs(),
            requestInfo.requestUrl(),
            requestInfo.userDapQuery());

    edd.saveAsFlatNc(
        requestInfo.language(),
        NetcdfFileFormat.NETCDF3,
        cacheFullName,
        twawm); // internally, it writes to temp file, then renames to cacheFullName

    File2.isFile(
        cacheFullName,
        5); // for possible waiting thread, wait till file is visible via operating system
    twawm.close();
  }

  @Override
  protected void generateGridFile(DapRequestInfo requestInfo, String cacheFullName)
      throws Throwable {
    EDDGrid edd = requestInfo.getEDDGrid();
    // if .ncHeader, make sure the .nc file exists (and it is the better file to cache)
    edd.saveAsNc(
        requestInfo.language(),
        NetcdfFileFormat.NETCDF3,
        requestInfo.ipAddress(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        cacheFullName,
        true,
        0); // it saves to temp random file first
    File2.isFile(
        cacheFullName,
        5); // for possible waiting thread, wait till file is visible via operating system
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelpTable_nc3Ar[language];
  }

  @Override
  public String getGridHelpText(int language) {
    return EDStatic.messages.fileHelpGrid_nc3Ar[language];
  }
}
