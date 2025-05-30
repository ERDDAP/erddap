package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".nc",
    fileTypeName = ".ncCFMA",
    infoUrl = "https://www.ncei.noaa.gov/netcdf-templates",
    versionAdded = "1.40.0",
    availableGrid = false,
    contentType = "application/x-netcdf")
public class NcCFMAFiles extends NcCFFiles {
  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_ncCFMAAr[language];
  }
}
