package gov.noaa.pfel.erddap.filetypes;

import com.cohort.array.PrimitiveArray;
import com.cohort.util.File2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.dataset.OutputStreamSourceSimple;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.List;
import ucar.ma2.Array;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.geotiff.GeotiffWriter;
import ucar.nc2.write.NetcdfFileFormat;

@FileTypeClass(
    fileTypeExtension = ".tif",
    fileTypeName = ".geotif",
    infoUrl = "https://trac.osgeo.org/geotiff/",
    versionAdded = "1.0.0",
    availableTable = false,
    isImage = true)
public class GeotifFiles extends ImageTypes {

  @Override
  protected boolean tableToImage(DapRequestInfo requestInfo, OutputStreamSourceSimple osss)
      throws Throwable {
    throw new UnsupportedOperationException();
  }

  @Override
  protected boolean gridToImage(DapRequestInfo requestInfo, OutputStreamSourceSimple osss)
      throws Throwable {
    return saveAsGeotiff(
        requestInfo.language(),
        requestInfo.ipAddress(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        osss,
        requestInfo.dir(),
        requestInfo.fileName(),
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelp_geotifAr[language];
  }

  /**
   * This saves the requested data (must be lat- lon-based data only) as a grayscale GeoTIFF file.
   * For .geotiff, dataVariable queries can specify multiple longitude and latitude values, but just
   * one value for other dimensions. GeotiffWriter requires that lons are +/-180 (because ESRI only
   * accepts that range). Currently, the lons in the request can't be below and above 180 (below is
   * fine; above is automatically shifted down). [future: do more extensive fixup].
   *
   * <p>javaDoc for the netcdf GeotiffWriter class isn't in standard javaDocs. Try
   * https://www.unidata.ucar.edu/software/netcdf-java/v4.3/javadocAll/ucar/nc2/geotiff/GeotiffWriter.html
   * or search Google for GeotiffWriter.
   *
   * <p>Grayscale GeoTIFFs may not be very colorful, but they have an advantage over color GeoTIFFs:
   * the clear correspondence of the gray level of each pixel (0 - 255) to the original data allows
   * programs to reconstruct (crudely) the original data values, something that is not possible with
   * color GeoTIFFS.
   *
   * @param language the index of the selected language
   * @param ipAddress The IP address of the user (for statistics).
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @param directory with a slash at the end in which to cache the file
   * @param fileName The file name with out the extension (e.g., myFile). The extension ".tif" will
   *     be added to create the output file name.
   * @return true of written ok; false if exception occurred (and written on image)
   * @throws Throwable
   */
  private boolean saveAsGeotiff(
      int language,
      String ipAddress,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      String directory,
      String fileName,
      EDDGrid eddGrid)
      throws Throwable {

    if (EDDGrid.reallyVerbose) String2.log("Grid.saveAsGeotiff " + fileName);
    long time = System.currentTimeMillis();

    // Can GeotiffWriter handle this dataset?
    // GeotiffWriter just throws non-helpful error messages if these requirements aren't met.

    // Has Lon and Lat?
    if (eddGrid.lonIndex() < 0 || eddGrid.latIndex() < 0)
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.queryErrorAr[0]
                  + MessageFormat.format(
                      EDStatic.messages.noXxxBecause2Ar[0],
                      ".geotif",
                      EDStatic.messages.noXxxNoLLAr[0]),
              EDStatic.messages.queryErrorAr[language]
                  + MessageFormat.format(
                      EDStatic.messages.noXxxBecause2Ar[language],
                      ".geotif",
                      EDStatic.messages.noXxxNoLLAr[language])));

    // Lon and Lat are evenly spaced?
    // See 2nd test in EDDGridFromDap.testDescendingAxisGeotif()
    if (!eddGrid.axisVariables()[eddGrid.latIndex()].isEvenlySpaced()
        || !eddGrid.axisVariables()[eddGrid.lonIndex()].isEvenlySpaced())
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.queryErrorAr[0]
                  + MessageFormat.format(
                      EDStatic.messages.noXxxBecause2Ar[0],
                      ".geotif",
                      EDStatic.messages.noXxxNoLLEvenlySpacedAr[0]),
              EDStatic.messages.queryErrorAr[language]
                  + MessageFormat.format(
                      EDStatic.messages.noXxxBecause2Ar[language],
                      ".geotif",
                      EDStatic.messages.noXxxNoLLEvenlySpacedAr[language])));

    // 2013-10-21 NO LONGER A LIMITATION: lon and lat are ascending?
    //  GeotiffWriter now deals with descending.
    //  see test in EDDGridFromDap.testDescendingAxisGeotif
    // if (axisVariables[eddGrid.latIndex()].averageSpacing() <= 0 ||
    //    axisVariables[eddGrid.lonIndex()].averageSpacing() <= 0)
    //    throw new SimpleException(EDStatic.bilingual(language,
    //    EDStatic.messages.queryErrorAr[0]        +
    // MessageFormat.format(EDStatic.messages.queryErrorAscending,
    // ".geotif"),
    //    EDStatic.messages.queryErrorAr[language] +
    // MessageFormat.format(EDStatic.messages.queryErrorAscending,
    // ".geotif")));

    // can't handle axis request
    if (eddGrid.isAxisDapQuery(userDapQuery))
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.queryErrorAr[0]
                  + MessageFormat.format(EDStatic.messages.queryErrorNotAxisAr[0], ".geotif"),
              EDStatic.messages.queryErrorAr[language]
                  + MessageFormat.format(
                      EDStatic.messages.queryErrorNotAxisAr[language], ".geotif")));

    // parse the userDapQuery and get the GridDataAccessor
    // this also tests for error when parsing query
    double lonAdjust = 0;
    try (GridDataAccessor gridDataAccessor =
        new GridDataAccessor(
            language, eddGrid, requestUrl, userDapQuery, true, true)) { // rowMajor, convertToNaN
      if (gridDataAccessor.dataVariables().length > 1)
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.queryErrorAr[0]
                    + MessageFormat.format(EDStatic.messages.queryError1VarAr[0], ".geotif"),
                EDStatic.messages.queryErrorAr[language]
                    + MessageFormat.format(
                        EDStatic.messages.queryError1VarAr[language], ".geotif")));

      PrimitiveArray lonPa = null, latPa = null;
      double minX = Double.NaN, maxX = Double.NaN, minY = Double.NaN, maxY = Double.NaN;
      for (int av = 0; av < eddGrid.axisVariables().length; av++) {
        PrimitiveArray avpa = gridDataAccessor.axisValues(av);
        if (av == eddGrid.lonIndex()) {
          lonPa = avpa;
          minX = lonPa.getNiceDouble(0);
          maxX = lonPa.getNiceDouble(lonPa.size() - 1);
          if (minX > maxX) { // then deal with descending axis values
            double d = minX;
            minX = maxX;
            maxX = d;
          }
          if (minX < 180 && maxX > 180)
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + MessageFormat.format(EDStatic.messages.queryError180Ar[0], ".geotif"),
                    EDStatic.messages.queryErrorAr[language]
                        + MessageFormat.format(
                            EDStatic.messages.queryError180Ar[language], ".geotif")));
          if (minX >= 180) lonAdjust = -360;
          minX += lonAdjust;
          maxX += lonAdjust;
          if (minX < -180 || maxX > 180)
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorAdjustedAr[0],
                            ".geotif",
                            "" + minX,
                            "" + maxX),
                    EDStatic.messages.queryErrorAr[language]
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorAdjustedAr[language],
                            ".geotif",
                            "" + minX,
                            "" + maxX)));
        } else if (av == eddGrid.latIndex()) {
          latPa = avpa;
          minY = latPa.getNiceDouble(0);
          maxY = latPa.getNiceDouble(latPa.size() - 1);
          if (minY > maxY) { // then deal with descending axis values
            double d = minY;
            minY = maxY;
            maxY = d;
          }
        } else {
          if (avpa.size() > 1)
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + MessageFormat.format(
                            EDStatic.messages.queryError1ValueAr[0],
                            ".geotif",
                            eddGrid.axisVariables()[av].destinationName()),
                    EDStatic.messages.queryErrorAr[language]
                        + MessageFormat.format(
                            EDStatic.messages.queryError1ValueAr[language],
                            ".geotif",
                            eddGrid.axisVariables()[av].destinationName())));
        }
      }
    }

    // The request is ok and compatible with geotiffWriter!

    /*
    //was &.size=width|height specified?
    String ampParts[] = Table.getDapQueryParts(userDapQuery); //decoded.  always at least 1 part (may be "")
    int imageWidth = -1, imageHeight = -1;
    for (int ap = 0; ap < ampParts.length; ap++) {
        String ampPart = ampParts[ap];

        //.colorBar ignored
        //.color    ignored
        //.draw     ignored
        //.font     ignored
        //.land     ignored
        //.legend   ignored
        //.marker   ignored

        //.size
        if (ampPart.startsWith(".size=")) {
            customSize = true;
            String pParts[] = String2.split(ampPart.substring(6), '|'); //subparts may be ""; won't be null
            if (pParts == null) pParts = new String[0];
            if (pParts.length > 0) {
                int w = String2.parseInt(pParts[0]);
                if (w > 0 && w < EDD.WMS_MAX_WIDTH) imageWidth = w;
            }
            if (pParts.length > 1) {
                int h = String2.parseInt(pParts[1]);
                if (h > 0 && h < EDD.WMS_MAX_WIDTH) imageHeight = h;
            }
            if (reallyVerbose)
                String2.log(".size=  imageWidth=" + imageWidth + " imageHeight=" + imageHeight);
        }

        //.trim   ignored
        //.vars   ignored
        //.vec    ignored
        //.xRange ignored
        //.yRange ignored
    }

    //recalculate stride?
    if (imageWidth > 0 && imageHeight > 0) {

        //pull apart userDapQuery
        ...

        //lat
        int have = maxXIndex - minXIndex + 1;
        int stride = constraints.get(xAxisIndex * 3 + 1);
        imageWidth = DataHelper.strideWillFind(have, stride);
        //protect against huge .png (and huge amount of data in memory)
        if (imageWidth > 3601) {
            stride = DataHelper.findStride(have, 3601);
            imageWidth = DataHelper.strideWillFind(have, stride);
            constraints.set(xAxisIndex * 3 + 1, stride);
            if (reallyVerbose) String2.log("  xStride reduced to stride=" + stride);
        }

        //lon
        ...

        //recreate userDapQuery
        ...
    }
    */

    // save the data in a .nc file
    // ???I'm pretty sure the axis order can be lon,lat or lat,lon, but not certain.
    // ???The GeotiffWriter seems to be detecting lat and lon and reacting accordingly.
    String ncFullName =
        directory + fileName + "_tiff.nc"; // _tiff is needed because unused axes aren't saved
    if (!File2.isFile(ncFullName))
      eddGrid.saveAsNc(
          language,
          NetcdfFileFormat.NETCDF3,
          ipAddress,
          requestUrl,
          userDapQuery,
          directory,
          ncFullName,
          false, // keepUnusedAxes=false  this is necessary
          lonAdjust);
    // String2.log(NcHelper.ncdump(ncFullName, "-h"));

    // attempt to create geotif via java netcdf libraries
    try (GeotiffWriter writer = new GeotiffWriter(directory + fileName + ".tif")) {

      // 2013-08-28 new code to deal with GeotiffWritter in netcdf-java 4.3+
      GridDataset gridDataset = GridDataset.open(ncFullName);
      List<GridDatatype> grids = gridDataset.getGrids();
      // if (grids.size() == 0) ...
      GeoGrid geoGrid = (GeoGrid) grids.getFirst();
      Array dataArray = geoGrid.readDataSlice(-1, -1, -1, -1); // get all
      writer.writeGrid(gridDataset, geoGrid, dataArray, true); // true=grayscale

      // old code for netcdf-java <4.3
      // LatLonRect latLonRect = new LatLonRect(
      //    new LatLonPointImpl(minY, minX),
      //    new LatLonPointImpl(maxY, maxX));
      // writer.writeGrid(ncFullName, dataName, 0, 0,
      //    true, //true=grayscale   color didn't work for me. and see javadocs above.
      //    latLonRect);

    }

    // copy to outputStream
    try (OutputStream out = outputStreamSource.outputStream("")) {
      if (!File2.copy(directory + fileName + ".tif", out)) {
        // outputStream contentType already set,
        // so I can't go back to html and display error message
        // note than the message is thrown if user cancels the transmission; so don't email to me
        throw new SimpleException(String2.ERROR + " while transmitting file.");
      }
    }
    // downloads of e.g., erddap2.css don't work right if not closed. (just if gzip'd?)

    if (EDDGrid.reallyVerbose)
      String2.log(
          "  Grid.saveAsGeotiff done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
    return true;
  }
}
