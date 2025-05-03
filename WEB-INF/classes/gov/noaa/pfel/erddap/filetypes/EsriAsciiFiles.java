package gov.noaa.pfel.erddap.filetypes;

import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.GridDataRandomAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Arrays;

@FileTypeClass(
    fileTypeExtension = ".asc",
    fileTypeName = ".esriAscii",
    infoUrl = "https://en.wikipedia.org/wiki/Esri_grid",
    versionAdded = "1.0.0",
    availableTable = false)
public class EsriAsciiFiles extends FileTypeInterface {

  @Override
  public void writeTableToStream(DapRequestInfo requestInfo) throws Throwable {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsEsriAscii(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        requestInfo.getEDDGrid());
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.fileHelpGrid_esriAsciiAr[language];
  }

  /**
   * This gets the data for the userDapQuery and writes the grid data to the outputStream in the
   * ESRI ASCII data format. For .esriAsci, dataVariable queries can specify multiple longitude and
   * latitude values, but just one value for other dimensions. Currently, the requested lon values
   * can't be below and above 180 (below is fine; above is automatically shifted down). [future: do
   * more extensive fixup].
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param outputStreamSource the source of an outputStream (usually already buffered) to receive
   *     the results. At the end of this method the outputStream is flushed, not closed.
   * @throws Throwable if trouble.
   */
  private void saveAsEsriAscii(
      int language,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      EDDGrid eddGrid)
      throws Throwable {

    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsEsriAscii");
    long time = System.currentTimeMillis();
    int lonIndex = eddGrid.lonIndex();
    int latIndex = eddGrid.latIndex();

    // does the dataset support .esriAscii?
    if (lonIndex < 0 || latIndex < 0)
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.queryErrorAr[0]
                  + MessageFormat.format(
                      EDStatic.messages.noXxxBecause2Ar[0],
                      ".esriAscii",
                      EDStatic.messages.noXxxNoLLAr[0]),
              EDStatic.messages.queryErrorAr[language]
                  + MessageFormat.format(
                      EDStatic.messages.noXxxBecause2Ar[language],
                      ".esriAscii",
                      EDStatic.messages.noXxxNoLLAr[language])));

    if (!eddGrid.axisVariables()[latIndex].isEvenlySpaced()
        || !eddGrid.axisVariables()[lonIndex].isEvenlySpaced())
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.queryErrorAr[0]
                  + MessageFormat.format(
                      EDStatic.messages.noXxxBecause2Ar[0],
                      ".esriAscii",
                      EDStatic.messages.noXxxNoLLEvenlySpacedAr[0]),
              EDStatic.messages.queryErrorAr[language]
                  + MessageFormat.format(
                      EDStatic.messages.noXxxBecause2Ar[language],
                      ".esriAscii",
                      EDStatic.messages.noXxxNoLLEvenlySpacedAr[language])));

    // can't handle axis request
    if (eddGrid.isAxisDapQuery(userDapQuery))
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.queryErrorAr[0]
                  + MessageFormat.format(EDStatic.messages.queryErrorNotAxisAr[0], ".esriAscii"),
              EDStatic.messages.queryErrorAr[language]
                  + MessageFormat.format(
                      EDStatic.messages.queryErrorNotAxisAr[language], ".esriAscii")));

    // parse the userDapQuery and get the GridDataAccessor
    // this also tests for error when parsing query
    try (GridDataAccessor gridDataAccessor =
        new GridDataAccessor(
            language, eddGrid, requestUrl, userDapQuery, true, true)) { // rowMajor, convertToNaN
      if (gridDataAccessor.dataVariables().length > 1)
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.queryErrorAr[0]
                    + MessageFormat.format(EDStatic.messages.queryError1VarAr[0], ".esriAscii"),
                EDStatic.messages.queryErrorAr[language]
                    + MessageFormat.format(
                        EDStatic.messages.queryError1VarAr[language], ".esriAscii")));
      EDV edv = gridDataAccessor.dataVariables()[0];
      PAType edvPAType = edv.destinationDataPAType();
      PAOne edvPAOne = new PAOne(edvPAType);

      // check that request meets ESRI restrictions
      PrimitiveArray lonPa = null, latPa = null;
      for (int av = 0; av < eddGrid.axisVariables().length; av++) {
        PrimitiveArray avpa = gridDataAccessor.axisValues(av);
        if (av == lonIndex) {
          lonPa = avpa;
        } else if (av == latIndex) {
          latPa = avpa;
        } else {
          if (avpa.size() > 1)
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + MessageFormat.format(
                            EDStatic.messages.queryError1ValueAr[0],
                            ".esriAscii",
                            eddGrid.axisVariables()[av].destinationName()),
                    EDStatic.messages.queryErrorAr[language]
                        + MessageFormat.format(
                            EDStatic.messages.queryError1ValueAr[language],
                            ".esriAscii",
                            eddGrid.axisVariables()[av].destinationName())));
        }
      }

      int nLon = lonPa.size();
      int nLat = latPa.size();
      double minX = lonPa.getDouble(0);
      double minY = latPa.getDouble(0);
      double maxX = lonPa.getDouble(nLon - 1);
      double maxY = latPa.getDouble(nLat - 1);
      boolean flipX = false;
      boolean flipY = false;
      if (minX > maxX) {
        flipX = true;
        double d = minX;
        minX = maxX;
        maxX = d;
      }
      if (minY > maxY) {
        flipY = true;
        double d = minY;
        minY = maxY;
        maxY = d;
      }
      double lonSpacing = lonPa.size() <= 1 ? Double.NaN : (maxX - minX) / (nLon - 1);
      double latSpacing = latPa.size() <= 1 ? Double.NaN : (maxY - minY) / (nLat - 1);
      if (Double.isNaN(lonSpacing) && !Double.isNaN(latSpacing)) lonSpacing = latSpacing;
      if (!Double.isNaN(lonSpacing) && Double.isNaN(latSpacing)) latSpacing = lonSpacing;
      if (Double.isNaN(lonSpacing) && Double.isNaN(latSpacing))
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.queryErrorAr[0]
                    + MessageFormat.format(EDStatic.messages.queryErrorLLGt1Ar[0], ".esriAscii"),
                EDStatic.messages.queryErrorAr[language]
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorLLGt1Ar[language], ".esriAscii")));

      // for almostEqual(3, lonSpacing, latSpacing) DON'T GO BELOW 3!!!
      // For example: PHssta has 4096 lon points so spacing is ~.0878
      // But .0878 * 4096 = 359.6
      // and .0879 * 4096 = 360.0    (just beyond extreme test of 3 digit match)
      // That is unacceptable. So 2 would be abominable.  Even 3 is stretching the limits.
      if (!Math2.almostEqual(3, lonSpacing, latSpacing))
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.queryErrorAr[0]
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorEqualSpacingAr[0],
                        ".esriAscii",
                        "" + lonSpacing,
                        "" + latSpacing),
                EDStatic.messages.queryErrorAr[language]
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorEqualSpacingAr[language],
                        ".esriAscii",
                        "" + lonSpacing,
                        "" + latSpacing)));
      if (minX < 180 && maxX > 180)
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.queryErrorAr[0]
                    + MessageFormat.format(EDStatic.messages.queryError180Ar[0], ".esriAscii"),
                EDStatic.messages.queryErrorAr[language]
                    + MessageFormat.format(
                        EDStatic.messages.queryError180Ar[language], ".esriAscii")));
      double lonAdjust = lonPa.getDouble(0) >= 180 ? -360 : 0;
      if (minX + lonAdjust < -180 || maxX + lonAdjust > 180)
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.queryErrorAr[0]
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorAdjustedAr[0],
                        ".esriAscii",
                        "" + (minX + lonAdjust),
                        "" + (maxX + lonAdjust)),
                EDStatic.messages.queryErrorAr[language]
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorAdjustedAr[language],
                        ".esriAscii",
                        "" + (minX + lonAdjust),
                        "" + (maxX + lonAdjust))));

      // request is ok and compatible with ESRI .asc!

      // complications:
      // * lonIndex and latIndex can be in any position in axisVariables.
      // * ESRI .asc wants latMajor (that might be rowMajor or columnMajor),
      //   and TOP row first!
      // The simplest solution is to save all data to temp file,
      // then read values as needed from file and write to writer.

      // make the GridDataRandomAccessor
      try (GridDataRandomAccessor gdra = new GridDataRandomAccessor(gridDataAccessor)) {
        int current[] =
            gridDataAccessor.totalIndex().getCurrent(); // the internal object that changes

        // then get the writer
        // ???!!! ISO-8859-1 is a guess. I found no specification.
        try (Writer writer =
            File2.getBufferedWriter88591(outputStreamSource.outputStream(File2.ISO_8859_1))) {
          // ESRI .asc doesn't like NaN
          double dmv = edv.safeDestinationMissingValue();
          String NaNString =
              Double.isNaN(dmv)
                  ? "-9999999"
                  : // good for int and floating data types
                  dmv == Math2.roundToLong(dmv) ? "" + Math2.roundToLong(dmv) : "" + dmv;

          // write the data
          writer.write("ncols " + nLon + "\n");
          writer.write("nrows " + nLat + "\n");
          // ???!!! ERD always uses centered, but others might need was xllcorner yllcorner
          writer.write("xllcenter " + (minX + lonAdjust) + "\n");
          writer.write("yllcenter " + minY + "\n");
          // ArcGIS forces cellsize to be square; see test above
          writer.write("cellsize " + latSpacing + "\n");
          writer.write("nodata_value " + NaNString + "\n");

          // write values from row to row, top to bottom
          Arrays.fill(current, 0); // manipulate indices in current[]

          for (int tLat = 0; tLat < nLat; tLat++) {
            current[latIndex] = flipY ? tLat : nLat - tLat - 1;
            for (int tLon = 0; tLon < nLon; tLon++) {
              current[lonIndex] = flipX ? nLon - tLon - 1 : tLon;
              gdra.getDataValueAsPAOne(current, 0, edvPAOne);
              String s = edvPAOne.toString();
              writer.write(s.isEmpty() || s.equals("NaN") ? NaNString : s);
              writer.write(tLon == nLon - 1 ? '\n' : ' ');
            }
          }

          writer.flush(); // essential
        }
      }
    }

    // diagnostic
    if (EDDGrid.reallyVerbose)
      String2.log(
          "  EDDGrid.saveAsEsriAscii done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }
}
