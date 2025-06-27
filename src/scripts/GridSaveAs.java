/*
 * GridSaveAs Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

/**
 * This class is designed to be a stand-alone program to convert from one type of Grid data file to
 * another, or all of the files in one directory into files of another type in another directory.
 * See Grid.davesSaveAs() for details.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-11-04
 */
public class GridSaveAs {

  /**
   * This reads a GMT .grd file and populates the public variables, EXCEPT data which is not
   * touched. NOT YET TESTED.
   *
   * @param fullFileName
   * @throws Exception if trouble
   */
  public void readGrdInfo(String fullFileName) throws Exception {

    if (verbose) String2.log("Grid.readGrdInfo");
    long time = System.currentTimeMillis();

    nValidPoints = Integer.MIN_VALUE;
    minData = Double.NaN;
    maxData = Double.NaN;

    // open the file (before 'try'); if it fails, no temp file to delete
    NetcdfFile grdFile = NcHelper.openFile(fullFileName);
    try {
      // if (verbose) String2.log(NcHelper.ncdump(fullFileName, "-h"));

      // get fileLonSpacing and fileLatSpacing
      Variable variable = grdFile.findVariable("spacing");
      DoubleArray da = NcHelper.getNiceDoubleArray(variable, 0, -1);
      double fileLonSpacing = da.get(0);
      double fileLatSpacing = da.get(1);
      if (Double.isNaN(fileLonSpacing)) // happens if only 1 value
      fileLonSpacing = 1;
      if (Double.isNaN(fileLatSpacing)) // happens if only 1 value
      fileLatSpacing = 1;
      if (verbose)
        String2.log("  fileLonSpacing=" + fileLonSpacing + " fileLatSpacing=" + fileLatSpacing);

      // get nLon and nLat
      variable = grdFile.findVariable("dimension");
      ArrayInt.D1 aid1 = (ArrayInt.D1) variable.read();
      int fileNLon = aid1.get(0);
      int fileNLat = aid1.get(1);
      if (verbose) String2.log("  fileNLon=" + fileNLon + " fileNLat=" + fileNLat);

      // get fileMinLon
      variable = grdFile.findVariable("x_range");
      da = NcHelper.getNiceDoubleArray(variable, 0, -1);
      double fileMinLon = da.get(0);
      double fileMaxLon = da.get(1);
      if (verbose) String2.log("  fileMinLon=" + fileMinLon + " fileMaxLon=" + fileMaxLon);

      // get fileMinLat
      variable = grdFile.findVariable("y_range");
      da = NcHelper.getNiceDoubleArray(variable, 0, -1);
      double fileMinLat = da.get(0);
      double fileMaxLat = da.get(1);
      if (verbose) String2.log("  fileMinLat=" + fileMinLat + " fileMaxLat=" + fileMaxLat);

      // get min/maxData
      variable = grdFile.findVariable("z_range");
      da = NcHelper.getNiceDoubleArray(variable, 0, -1);
      minData = da.get(0);
      maxData = da.get(1);
      if (verbose) String2.log("  minData=" + minData + " maxData=" + maxData);

      if (verbose)
        String2.log(
            "  "
                + lonInfoString()
                + "\n  "
                + latInfoString()
                + "\n  Grid.readGrdInfo done. TIME="
                + (System.currentTimeMillis() - time)
                + "\n");

    } finally {
      try {
        if (grdFile != null) grdFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * This reads the data from an HDF file. This is not a very generalized reader, the data must be
   * in the standard CoastWatch format: with the first three HdfScientificData tags containing data,
   * lon, and lat, each with double or float data. This doesn't set minData, maxData, or
   * nValidObservations; use calculateStats to get these values.
   *
   * <p>This always reads all of the data and doesn't change it (e.g., makeLonPM180). Use
   * makeLonPM180 or subset afterwards to do those things.
   *
   * <p>This could/should set globalAttributes, latAttributes, lonAttributes, and dataAttributes,
   * but it currently doesn't.
   *
   * @param fullFileName
   * @param dfntType an HdfConstants dfntType (currently supported: DFNT_FLOAT32, DFNT_FLOAT64).
   *     Ideally, this procedure could detect this.
   * @throws Exception if trouble
   */
  public void readHDF(String fullFileName, int dfntType) throws Exception {
    if (verbose) String2.log("grid.readHDF(" + fullFileName + ")");

    // read the raw data
    SdsReader sdsReader = new SdsReader(fullFileName);

    // read backwards to get lat and lon before grid
    for (int count = 2; count >= 0; count--) {
      HdfScientificData sd = (HdfScientificData) sdsReader.sdList.get(count);
      double dar[];
      double tMissingValue = DataHelper.FAKE_MISSING_VALUE;
      if (dfntType == HdfConstants.DFNT_FLOAT32) {
        float tempValue = (float) tMissingValue;
        tMissingValue = tempValue; // to bruise it like bruised in far
        float[] far = SdsReader.toFloatArray(sd.data);
        int n = far.length;
        dar = new double[n];
        if (count > 0) {
          // don't make data nice
          for (int i = 0; i < n; i++) dar[i] = far[i];
        } else {
          // do make lat and lon nice
          for (int i = 0; i < n; i++) dar[i] = Math2.floatToDouble(far[i]);
        }
      } else if (dfntType == HdfConstants.DFNT_FLOAT64) {
        dar = SdsReader.toDoubleArray(sd.data);
      } else {
        throw new Exception("Grid.readHDF: Unsupported dfntType: " + dfntType + ".");
      }

      // String2.log("  count=" + count + " dar=" +
      // String2.noLongerThanDots(String2.toCSSVString(dar), 300));

      if (count == 0) {
        // save dar as data
        // look for otherMissingValue
        int otherMissingValue = -999;
        int n = dar.length;
        for (int i = 0; i < n; i++) {
          if (dar[i] == tMissingValue || dar[i] == otherMissingValue) dar[i] = Double.NaN;
        }

        // change the order of the data
        data = new double[dar.length];
        int nLat = lat.length;
        int nLon = lon.length;
        int po = 0;
        for (int tLat = nLat - 1; tLat >= 0; tLat--)
          for (int tLon = 0; tLon < nLon; tLon++) setData(tLon, tLat, dar[po++]);
      } else if (count == 1) {
        // save dar as lon
        lon = dar;
      } else if (count == 2) {
        // save dar as lat
        lat = dar;
      }
    }

    setLonLatSpacing();
  }

  /**
   * This is an alias for saveAsXYZ(directory, name, true, "NaN");
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".xyz" will be
   *     added.
   * @throws Exception
   */
  public void saveAsXYZ(String directory, String name) throws Exception {
    saveAsXYZ(
        directory, name, false, // was true=fromTop; I think to mimic what GMT did
        "NaN");
  }

  /**
   * Save this grid data as a tab-separated XYZ ASCII file. This writes the lon values as they are
   * currently in this grid (e.g., +-180 or 0..360). Note the GMT seems to want the values as
   * 0..360. This overwrites any existing file of the specified name. This makes an effort not to
   * create a partial file if there is an error. If no exception is thrown, the file was
   * successfully created. **Currently, the lat, lon, and data values are written as floats.
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".xyz" will be
   *     added.
   * @param fromTop write the data in rows, starting at the highest lat (vs the lowest lat).
   * @param NaNString is the String to write for NaN's.
   * @throws Exception
   */
  public void saveAsXYZ(String directory, String name, boolean fromTop, String NaNString)
      throws Exception {

    if (verbose) String2.log("Grid.saveAsXYZ " + name);
    long time = System.currentTimeMillis();

    // delete any existing file
    String ext = SAVE_AS_EXTENSIONS.get(SAVE_AS_XYZ);
    File2.delete(directory + name);

    // make sure there is data
    ensureThereIsData();

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // open the temp file
    // (I tried with Buffer/FileOutputStream. No faster.)
    BufferedWriter bufferedWriter = File2.getBufferedFileWriter88591(directory + randomInt);
    try {

      // write the data
      int nLat = lat.length;
      int nLon = lon.length;
      if (fromTop) {
        // write values from row to row, top to bottom
        for (int tLat = nLat - 1; tLat >= 0; tLat--) {
          for (int tLon = 0; tLon < nLon; tLon++) {
            float f = (float) getData(tLon, tLat);
            bufferedWriter.write(
                String2.genEFormat10(lon[tLon])
                    + "\t"
                    + String2.genEFormat10(lat[tLat])
                    + "\t"
                    + (Float.isNaN(f) ? NaNString + '\n' : f + "\n"));
          }
        }
      } else {
        // write values from row to row, bottom to top
        for (int tLat = 0; tLat < nLat; tLat++) {
          for (int tLon = 0; tLon < nLon; tLon++) {
            float f = (float) getData(tLon, tLat);
            bufferedWriter.write(
                String2.genEFormat10(lon[tLon])
                    + "\t"
                    + String2.genEFormat10(lat[tLat])
                    + "\t"
                    + (Float.isNaN(f) ? NaNString + '\n' : f + "\n"));
          }
        }
      }
    } finally {
      bufferedWriter.close();
    }

    // rename the file to the specified name
    File2.rename(directory, randomInt + "", name + ext);

    // diagnostic
    /*
    if (verbose) {
        String[] rff = File2.readFromFile(directory + name + ext);
        if (rff[0].length() > 0)
            throw new Exception(String2.ERROR + ":\n" + rff[0]);
        String2.log("grid.saveAsXYZ: " + directory + name + ext + " contains:\n" +
            String2.annotatedString(
                rff[1].substring(0, Math.min(rff[1].length(), 200))));
    }
    */

    if (verbose)
      String2.log("  Grid.saveAsXYZ done. TIME=" + (System.currentTimeMillis() - time) + "\n");
  }

   /**
   * Save this grid data as an ArcInfo ASCII grid format file that can be imported ArcGIS programs
   * -- lon values are forced to be within -180 to 180. This doesn't adjust attributes because .asc
   * doesn't store attributes.
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".asc" will be
   *     added.
   * @throws Exception if trouble, notably if lonSpacing!=latSpacing
   */
  public void saveAsEsriASCII(String directory, String name) throws Exception {

    // for lon values to pm180
    makeLonPM180(true);

    // save as ascii
    saveAsASCII(
        directory,
        name,
        true,
        "" + DataHelper.FAKE_MISSING_VALUE); // esri likes fromTop=true and -9999 for mv
  }

  /**
   * This is an alias for saveAsAscii(directory, name, true, "-9999999");
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".asc" will be
   *     added.
   * @throws Exception if trouble, notably if lonSpacing!=latSpacing
   */
  public void saveAsASCII(String directory, String name) throws Exception {
    saveAsASCII(directory, name, true, "" + DataHelper.FAKE_MISSING_VALUE); // 12/2006 was "-9999"
  }

  /**
   * Save this grid data as an ASCII grid format file, with lon values as is. This writes the lon
   * values as they are currently in this grid (e.g., +-180 or 0..360). This overwrites any existing
   * file of the specified name. This makes an effort not to create a partial file if there is an
   * error. If no exception is thrown, the file was successfully created. See
   * https://en.wikipedia.org/wiki/Esri_grid for info about this format. **Currently, the values are
   * written as floats.
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".asc" will be
   *     added.
   * @param fromTop write the data in rows, starting at the highest lat (vs the lowest lat). ArcInfo
   *     likes fromTop = true;
   * @param NaNString is the String to write for NaN's. ArcInfo doesn't like "NaN", and uses "-9999"
   *     as the suggested value.
   * @throws Exception if trouble, notably if lonSpacing!=latSpacing
   */
  public void saveAsASCII(String directory, String name, boolean fromTop, String NaNString)
      throws Exception {

    if (verbose) String2.log("Grid.saveAsASCII " + name);
    long time = System.currentTimeMillis();

    // delete any existing file
    String ext = SAVE_AS_EXTENSIONS.get(SAVE_AS_ASCII); // fortunately same as ESRI_ASCII
    File2.delete(directory + name + ext);

    // make sure there is data
    ensureThereIsData();

    // ensure lat and lon values are evenly spaced
    // (they may not be and this format doesn't store values, just first and spacing)
    DataHelper.ensureEvenlySpaced(
        lon,
        String2.ERROR
            + " in Grid.saveAsASCII:\n"
            + "The longitude values aren't evenly spaced (as required by ESRI's .asc format):\n");
    DataHelper.ensureEvenlySpaced(
        lat,
        String2.ERROR
            + " in Grid.saveAsASCII:\n"
            + "The latitude values aren't evenly spaced (as required by ESRI's .asc format):\n");

    // ensure latSpacing=lonSpacing since ESRI .asc requires it
    // (only 1 "cellsize" parameter in header)
    setLonLatSpacing(); // double check that they are set nicely

    // for almostEqual(3, lonSpacing, latSpacing) DON'T GO BELOW 3!!!
    // For example: PHssta has 4096 lon points so spacing is ~.0878
    // But .0878 * 4096 = 359.6
    // and .0879 * 4096 = 360.0    (just beyond extreme test of 3 digit match)
    // That is unacceptable. So 2 would be abominable.  Even 3 is stretching the limits.
    if (!Math2.almostEqual(3, lonSpacing, latSpacing))
      Test.ensureEqual(
          lonSpacing,
          latSpacing,
          String2.ERROR
              + " in Grid.saveAsASCII:\n"
              + "ESRI's .asc format requires that the longitude spacing equal the latitude spacing.");

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // open the temp file
    BufferedWriter bufferedWriter = File2.getBufferedFileWriter88591(directory + randomInt);
    try {
      // write the data
      bufferedWriter.write("ncols " + lon.length + "\n");
      bufferedWriter.write("nrows " + lat.length + "\n");
      bufferedWriter.write(
          "xllcenter "
              + lon[0]
              + "\n"); // as precisely as possible; was xllcorner, but xllcenter is correct
      bufferedWriter.write("yllcenter " + lat[0] + "\n");
      // ArcGIS forces cellsize to be square; see test above
      bufferedWriter.write("cellsize " + latSpacing + "\n");
      bufferedWriter.write("nodata_value " + NaNString + "\n");
      if (fromTop) {
        // write values from row to row, top to bottom
        int last = lon.length - 1;
        for (int tLat = lat.length - 1; tLat >= 0; tLat--) {
          float f;
          for (int tLon = 0; tLon < last; tLon++) {
            f = (float) getData(tLon, tLat);
            bufferedWriter.write(Float.isNaN(f) ? NaNString + ' ' : f + " ");
          }
          f = (float) getData(last, tLat);
          bufferedWriter.write(Float.isNaN(f) ? NaNString + '\n' : f + "\n");
        }
      } else {
        // write values from row to row, bottom to top
        int nLat = lat.length;
        int last = lon.length - 1;
        for (int tLat = 0; tLat < nLat; tLat++) {
          float f;
          for (int tLon = 0; tLon < last; tLon++) {
            f = (float) getData(tLon, tLat);
            bufferedWriter.write(Float.isNaN(f) ? NaNString + ' ' : f + " ");
          }
          f = (float) getData(last, tLat);
          bufferedWriter.write(Float.isNaN(f) ? NaNString + '\n' : f + "\n");
        }
      }
    } finally {
      bufferedWriter.close();
    }

    // rename the file to the specified name
    File2.rename(directory, randomInt + "", name + ext);

    // diagnostic
    if (false) {
      String[] rff = File2.readFromFile88591(directory + name + ext);
      if (rff[0].length() > 0) throw new Exception(String2.ERROR + ":\n" + rff[0]);
      String2.log(
          "grid.saveAsASCII: "
              + directory
              + name
              + ext
              + " contains:\n"
              + String2.annotatedString(rff[1].substring(0, Math.min(rff[1].length(), 1000))));
    }

    if (verbose)
      String2.log("  Grid.saveAsASCII done. TIME=" + (System.currentTimeMillis() - time) + "\n");
  }

  /**
   * This saves the current grid (which must have .nc-compatible attributes) as a grayscale GeoTIFF
   * file. This calls makeLonPM180 to ensure the data lons are +/-180 (which the GeotiffWriter
   * requires because ESRI only accepts that range).
   *
   * <p>Grayscale GeoTIFFs may not be very colorful, but they have an advantage over color GeoTIFFs:
   * the clear correspondence of the gray level of each pixel (0 - 255) to the original data allows
   * programs to reconstruct the original data values, something that is not possible with color
   * GeoTIFFS.
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".tif" will be
   *     added to create the output file name.
   * @param dataName The name for the data variable (e.g., ATssta).
   * @throws Exception
   */
  public void saveAsGeotiff(String directory, String name, String dataName) throws Exception {

    if (verbose) String2.log("Grid.saveAsGeotiff " + name);
    long time = System.currentTimeMillis();

    // foce to be pm180
    makeLonPM180(true);

    // save as .nc file first
    saveAsNetCDF(directory, name, dataName);

    // attempt via java netcdf libraries
    GeotiffWriter writer = new GeotiffWriter(directory + name + ".tif");
    try {
      // 2013-08-28 new code to deal with GeotiffWritter in netcdf-java 4.3+
      GridDataset gridDataset = GridDataset.open(directory + name + ".nc");
      java.util.List<GridDatatype> grids = gridDataset.getGrids();
      // if (grids.size() == 0) ...
      GeoGrid geoGrid = (GeoGrid) grids.get(0);
      Array dataArray = geoGrid.readDataSlice(-1, -1, -1, -1); // get all
      writer.writeGrid(gridDataset, geoGrid, dataArray, true); // true=grayscale

      // 2013-08-28 pre 4.3.16, it was
      // LatLonRect latLonRect = new LatLonRect(
      //    new LatLonPointImpl(lat[0], lon[0]),
      //    new LatLonPointImpl(lat[lat.length - 1], lon[lon.length - 1]));
      // writer.writeGrid(directory + name + ".nc", dataName, 0, 0,
      //    true, //true=grayscale   color didn't work for me. and see javadocs above.
      //    latLonRect);
    } finally {
      writer.close();
    }

    if (verbose)
      String2.log("  Grid.saveAsGeotiff done. TIME=" + (System.currentTimeMillis() - time) + "\n");
  }

  /*  //attempt based on gdal
  public void saveAsGeoTIFF(String directory, String name) throws Exception {
      //POLICY: because this procedure may be used in more than one thread,
      //do work on unique temp files names using randomInt, then rename to proper file name.
      //If procedure fails half way through, there won't be a half-finished file.
      int randomInt = Math2.random(Integer.MAX_VALUE));

      //delete any existing file
      String ext = SAVE_AS_EXTENSIONS.get(SAVE_AS_GEOTIFF);
      File2.delete(directory + name + ext);

      //make sure there is data
      ensureThereIsData();

      //save as grd first
      //saveAsGrd(directory, name);
      saveAsASCII(directory, name);

      //rename it as .nc
      //File2.rename(directory, name + ".grd", name + ".nc");

      //make the temp tiff file   (gdal is part of FWTools)
      //SSR.dosOrCShell("gdal_translate -ot Float32 -of GTiff " + directory + name + ".grd " +
      SSR.dosOrCShell("gdal_translate -of GTiff " + directory + name + ".asc " +
          directory + randomInt + ext, 60);

      //delete the grd file
      File2.delete(directory + name + ".grd");   //*** or .asc?

      //and rename to <name>.tif
      File2.rename(directory, randomInt + ext, name + ext);
  } */

  /**
   * Save this grid data as a Matlab .mat file. This writes the lon values as they are currently in
   * this grid (e.g., +-180 or 0..360). This overwrites any existing file of the specified name.
   * This makes an effort not to create a partial file if there is an error. If no exception is
   * thrown, the file was successfully created.
   *
   * @param directory with a slash at the end
   * @param name The file name without the extension (e.g., myFile). The extension ".mat" will be
   *     added.
   * @param varName the name to use for the variable (e.g., ux10). If it isn't variableNameSafe, it
   *     will be made so.
   * @throws Exception
   */
  public void saveAsMatlab(String directory, String name, String varName) throws Exception {

    if (verbose) String2.log("Grid.saveAsMatlab " + name);
    long time = System.currentTimeMillis();
    varName = String2.modifyToBeVariableNameSafe(varName);

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // delete any existing file
    String ext = SAVE_AS_EXTENSIONS.get(SAVE_AS_MATLAB);
    File2.delete(directory + name + ext);

    // make sure there is data
    ensureThereIsData();

    // open a dataOutputStream
    DataOutputStream dos = DataStream.getDataOutputStream(directory + randomInt);
    try {
      // write the header
      Matlab.writeMatlabHeader(dos);

      // first: write the lon array
      Matlab.writeDoubleArray(dos, "lon", lon);

      // second: make the lat array
      Matlab.writeDoubleArray(dos, "lat", lat);

      // make an array of the data[row][col]
      int nLat = lat.length;
      int nLon = lon.length;
      double ar[][] = new double[nLat][nLon];
      for (int row = 0; row < nLat; row++)
        for (int col = 0; col < nLon; col++) ar[row][col] = getData(col, row);
      Matlab.write2DDoubleArray(dos, varName, ar);

      // this doesn't write attributes because .mat files don't store attributes
      // setStatsAttributes(true); //true = double
      // write the attributes...

    } finally {
      dos.close();
    }

    // rename the file to the specified name
    File2.rename(directory, randomInt + "", name + ext);

    // Old way relies on script which calls Matlab.
    // This relies on a proprietary program, so good to remove it.
    // cShell("/u00/chump/grdtomatlab " + fullGrdName + " " +
    //    fullResultName + randomInt + ".mat " + varName);

    if (verbose)
      String2.log("  Grid.saveAsMatlab done. TIME=" + (System.currentTimeMillis() - time) + "\n");
  }

  /**
   * This saves the data in a Grid as an HDF file with whatever attributes are currently set (use
   * GridDataSet.setAttributes) This method is self-contained; it does not require external lookup
   * tables with data, but instead uses fileNameUtility to get information from the .grd file name.
   *
   * <p>The data is stored in the hdf file as doubles. Missing values are stored as
   * DataHelper.FAKE_MISSING_VALUE, not NaN.
   *
   * <p>This calls setStatsAttributes(true).
   *
   * <p>This relies on a pure Java implemtation, not NCSA's libraries. See comments in
   * hdf/SdsWriter.java for reasoning.
   *
   * <p>See documentation that comes with CoastWatch Utilities e.g.,
   * c:/programs/cwutilities/doc/Metadata.html.
   *
   * <p>This method is vastly faster (9 s web page time for SST 1km) than Dave's script (69 s). And
   * I think the Matlab approach is odd/flawed because it makes 2D lat and lon arrays, which appear
   * as datasets in CDAT.
   *
   * <p>The grid needs to hold gridded data with longitude +-180. (If not +-180, the data won't be
   * in the right place for CDAT to display coastline and political lines.) The full range and
   * resolution of the grid data will be saved in the hdf file.
   *
   * @param hdfFileName the full name for the new hdf file, but without the .hdf extension (it will
   *     be added by this method). Because fileNameUtility is used to generate the metadata based on
   *     the file name, the file name must follow the CWBrowser file naming convention (see
   *     FileNameUtility).
   * @param variableName the name to assign to the variable, usually the 6Name
   * @throws Exception if trouble. If there is trouble, there should be no partially created .hdf
   *     file.
   */
  public void saveAsHDF(String hdfFileName, String variableName) throws Exception {

    if (verbose) String2.log("Grid.saveAsHDF " + hdfFileName);
    long time = System.currentTimeMillis();
    File2.delete(hdfFileName + ".hdf");

    // make sure there is data
    ensureThereIsData();

    try {
      // gather the data
      // A new array is needed because of different order and different type.
      int nLon = lon.length;
      int nLat = lat.length;
      int nData = data.length;
      double tData[] = new double[nData];
      int po = 0;
      for (int tLat = 0; tLat < nLat; tLat++) {
        for (int tLon = 0; tLon < nLon; tLon++) {
          double d = getData(tLon, nLat - 1 - tLat); // up side down
          tData[po++] = Double.isNaN(d) ? DataHelper.FAKE_MISSING_VALUE : d;
        }
      }
      String2.log("globalAttributes=" + globalAttributes.toString());
      String2.log("et_affine=" + globalAttributes.get("et_affine"));

      // set the attributes
      setStatsAttributes(true); // save as double

      // create the file
      SdsWriter.create(
          hdfFileName + ".hdf", // must be the correct name, since it is stored in the file
          new double[][] {
            lon, lat
          }, // order is important and tied to desired order of data in array
          new String[] {"Longitude", "Latitude"},
          new Attributes[] {lonAttributes, latAttributes},
          tData,
          variableName,
          dataAttributes,
          globalAttributes);
    } catch (Exception e) {
      File2.delete(hdfFileName + ".hdf");
      throw e;
    }

    if (verbose)
      String2.log("  Grid.saveAsHDF done. TIME=" + (System.currentTimeMillis() - time) + "\n");
  }

  /**
   * This tests saveAsHDF.
   *
   * @throws Exception if trouble
   */
  // public static void miniTestSaveAsHDF() {
  // test hdf    test in CoastWatch Utilities CDAT (it has the required metadata)
  // also use NCSA HDSView 2.2 to view the file
  // 7/14/05 this file was tested by (now) BobSimons2.00@gmail.com in CDAT
  //    String2.log("\n***** Grid.main miniJavaTestSaveAsHDF");
  //    time = System.currentTimeMillis();
  // miniJavaTestSaveAsHDF();  //different test than other files since more complicated to set up
  //    String2.log("Grid.miniJavaTestSaveAsHDF time=" + (System.currentTimeMillis() - time));
  // }

  /**
   * THIS WORKS BUT IS NOT CURRENTLY USED BECAUSE saveAsHDF IS BETTER SINCE IT DOESN'T NEED LINUX,
   * SHELL SCRIPTS, OR MATLAB. This saves the current grid data as a CoastWatch Utilities-compatible
   * HDF ver 4 file. Currently, this relies on several files in "c:/u00/chump/"
   * ("chump_make_grd2hdf", "lookup_data_id.m", "lookup_data_source.m") which rely on Matlab.
   *
   * @param directory with a slash at the end
   * @param name The file name with out the extension (e.g., myFile). The extension ".hdf" will be
   *     added.
   * @param varName the name to use for the variable (e.g., ux10).
   * @throws Exception
   */
  public void saveAsHDFViaMatlab(String directory, String name, String varName) throws Exception {
    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // delete any existing file
    String ext = SAVE_AS_EXTENSIONS.get(SAVE_AS_HDF);
    File2.delete(directory + name + ext);

    // make sure there is data
    ensureThereIsData();

    // /* //old way
    // save as grd first
    saveAsGrd(directory, name);

    // MakeHdf via Matlab script
    // see copies of the files in <context>WEB-INF/ref/
    SSR.cShell(
        "/u00/chump/grd2hdf "
            + directory
            + name
            + ".grd "
            + directory
            + randomInt
            + ext
            + " "
            + varName,
        240);

    // delete the grd file
    File2.delete(directory + name + ".grd");

    // rename to final name
    File2.rename(directory, randomInt + ext, name + ext);
    // */

    /* //new way
    //save as xyz first
    saveAsXYZ(directory, name);

    //create matlab control script
    StringBuilder matlabControl = new StringBuilder();
    String binDir = "/u00/chump";
    matlabControl.append("path(path,'" + binDir + "')\n");
    matlabControl.append("cd " + directory + "\n");
    matlabControl.append("chump_make_grd2hdf(" +
        "'" + directory + name + ".xyz'," +
        "'" + directory + name + "_" + randomInt + ".hdf'," +
        "'" + varName + "'," + lonSpacing + "," + latSpacing + ")\n");
    matlabControl.append("quit\n");
    String error = File2.writeToFile88591(directory + randomInt + ".control", matlabControl.toString());
    if (error.length() > 0)
        throw new RuntimeException(error);

    //execute matlabControl
    String2.log("HDF matlabControl:\n" + matlabControl);
    SSR.runMatlab(directory + randomInt + ".control", directory + randomInt + ".out");
    String[] results = File2.readFromFile(directory + randomInt + ".out");
    String2.log("Grid.saveAsHDF matlabOutput:\n" + results[1] + "\n[end]");
    //File2.delete(directory + randomInt + ".control");
    //File2.delete(directory + randomInt + ".out");

    //delete the xyz file
    File2.delete(directory + name + ".xyz");

    //rename to final name
    File2.rename(directory, name + "_" + randomInt + ".hdf", name + ".hdf");
    // */

  }

  // private static Object saveAsHDFLock = Calendar2.newGCalendarLocal();

  /**
   * THIS METHOD WORKS ON COMPUTERS WHERE THE HDFLIBRARIES ARE INSTALLED AND ARE COMPATIBLE THE THE
   * CURRENT VERSION OF JAVA. (WHAT A PAIN!!!) USE saveAsHDF (A PURE JAVA VERSION) INSTEAD. My
   * mother said, "if you don't have anything nice to say, don't say anything at all".
   *
   * <p>This saves the data in a Grid as an HDF file with the metadata required by CDAT (in
   * CoastWatch Utilities). This method is self-contained; it does not require external lookup
   * tables with data, nor does it get information from the .grd file name.
   *
   * <p>This uses jhdf.jar which must be in the File2.webInfParentDirectory()+"WEB-INF\lib"
   * directory (for Tomcat) and on the javac's and java's classpath (for compiling and running
   * outside of Tomcat).
   *
   * <p>A native HDFLibrary must be installed for ncsa.hdf.hdflib.HDFLibrary to function within this
   * program. The easiest way to install it is to download and install HDFView from:
   * http://hdf.ncsa.uiuc.edu/hdf-java-html/hdfview/. See the JavaDocs for the HDFLibrary at
   * http://hdf.ncsa.uiuc.edu/hdf-java-html/javadocs/ncsa/hdf/hdflib/HDFLibrary.html.
   *
   * <p>See documentation that comes with CoastWatch Utilities e.g.,
   * c:/programs/cwutilities/doc/Metadata.html.
   *
   * <p>This is modified from Dave Foley's Matlab script chump_make_grd2hdf.m 13 Jan 2003, which was
   * modified 26 May 2005 by Luke Spence. This method is vastly faster (9 s web page time for SST
   * 1km) than Dave's script (69 s).
   *
   * <p>See also the Matlab (programming) book: "Exporting MATLAB Data to an HDF4 File", pg 6-94,
   * which had clues about how to make this work.
   *
   * <p>The grid needs to hold gridded data with longitude +-180. (If not +-180, the data won't be
   * in the right place for CDAT to display coastline and political lines.) The full range and
   * resolution of the grid data will be saved in the hdf file.
   *
   * @param hdfFileName the full name for the new hdf file
   * @param varName the name of the data variable (e.g., "sea surface temperature")
   * @param satellite the name of the satellite (e.g., "NOAA GOES spacecraft")
   * @param sensor the name of the sensor (e.g., "GOES SST")
   * @param origin the origin of the data (e.g., "NOAA/NESDIS")
   * @param startDate the start date/time (time should be 00:00:00 for composites). Note that this
   *     is a simple minded start date: where y-m-dTh:m:s are right even though a local time zone
   *     may be used. This procedure will interpret the day,hour as UTC time.
   * @param centeredTime the centered time (e.g., 2006-06-04 12:00:00 for composites). Note that
   *     this is a simple minded end date: where y-m-dTh:m:s are right even though a local time zone
   *     may be used. This procedure will interpret the day,hour as UTC time.
   * @param passType is "day" (e.g., for "sstd"), "night" (e.g., for "sstn"), or "day/night" (for
   *     all others)
   * @param dataUnits (e.g., "degrees Celsius")
   * @param dataFractionDigits digits to right of decimal place, for formatting data (e.g., 3)
   * @param latLonFractionDigits digits to right of decimal place, for formatting data (e.g., 3)
   * @throws Exception if trouble
   */
  /*    public void saveAsHDFViaNCSA(String hdfFileName,
              String varName, String satellite, String sensor, String origin,
              GregorianCalendar startDate, GregorianCalendar centeredTime,
              String passType, String dataUnits,
              int dataFractionDigits, int latLonFractionDigits)
              throws Exception {

          //This is synchronized because all calls to HDFLibrary are static.
          //So I need to ensure that only one thread uses it at once.
          synchronized(saveAsHDFLock) {
              if (verbose) String2.log("Grid.saveAsHDF " + hdfFileName);
              String errorIn = String2.ERROR + " in HdfWriter.grdToHdf: ";

              // create HDF file
              File2.delete(hdfFileName);

              //make sure there is data
              ensureThereIsData();

              int fileID = HDFLibrary.SDstart(hdfFileName, HDFConstants.DFACC_CREATE); //fails if file already exists
              if (verbose) String2.log("  create hdf fileID = " + fileID);

              // create SDS 0
              int sdsID = HDFLibrary.SDcreate(fileID, varName, HDFConstants.DFNT_FLOAT64, 2,
                  new int[]{lat.length, lon.length});
              if (verbose) String2.log("  sdsID create 0 = " + sdsID);
              int nLon = lon.length;
              int nLat = lat.length;
              int nData = data.length;
              if (verbose) String2.log("  nLon=" + nLon + " nLat=" + nLat);
              Test.ensureEqual(nLat * nLon, nData, "nLon(" + nLon + ") * nLat(" + nLat + ") != nData(" + nData + ")");
              double tData[] = new double[nData];
              int po = 0;
              for (int tLat = nLat - 1; tLat >= 0; tLat--) {
                  for (int tLon = 0; tLon < nLon; tLon++) {
                      tData[po] = getData(tLon, tLat);
                      if (Double.isNaN(tData[po]))
                          tData[po] = hdfMissingValue;
                      po++;
                  }
              }
              Test.ensureTrue(
                  HDFLibrary.SDwritedata(sdsID, new int[]{0,0}, new int[]{1,1}, //ds_start, ds_stride,
                      new int[]{nLat, nLon}, tData),   //ds_edges, var); //nLon,nLat works; I want nLat,nLon
                  errorIn + "SDwritedata for SDS 0.");
              tData = null; //allow garbage collection
              Test.ensureTrue(
                  HDFLibrary.SDendaccess(sdsID),
                  errorIn + "SDendaccess for SDS 0.");

              // create SDS 1
              //If I write lon and lat as 1D variables, CDAT doesn't ask if you
              //  want to import them (which it does for files created by Dave's script,
              //  because Matlab treats everything as a 2D (or more) array).
              //I think 1D is more appropriate. 2D (and importing) seems to imply they are data.
              sdsID = HDFLibrary.SDcreate(fileID, "Longitude", HDFConstants.DFNT_FLOAT64, 1, new int[]{nLon});
              if (verbose) String2.log("  sdsID create 1 = " + sdsID);
              Test.ensureTrue(
                  HDFLibrary.SDwritedata(sdsID, new int[]{0}, new int[]{1}, new int[]{nLon}, lon),
                  errorIn + "SDwritedata for SDS 1.");
              Test.ensureTrue(
                  HDFLibrary.SDendaccess(sdsID),
                  errorIn + "SDendaccess for SDS 1.");

              // create SDS 2
              sdsID = HDFLibrary.SDcreate(fileID, "Latitude", HDFConstants.DFNT_FLOAT64, 1, new int[]{nLat});
              if (verbose) String2.log("  sdsID create 2 = " + sdsID);
              Test.ensureTrue(
                  HDFLibrary.SDwritedata(sdsID, new int[]{0}, new int[]{1}, new int[]{nLat}, lat),
                  errorIn + "SDwritedata for SDS 2.");
              Test.ensureTrue(
                  HDFLibrary.SDendaccess(sdsID),
                  errorIn + "SDendaccess for SDS 2.");

              //------------------------------------------------------
              //write metadata for HDF files using CoastWatch Metadata Specifications

              //write Global Attributes
              setAttribute(fileID, "satellite", satellite);              //string
              setAttribute(fileID, "sensor", sensor);                    //string
              setAttribute(fileID, "origin", origin);                    //string
              setAttribute(fileID, "history", "unknown");                //string
              setAttribute(fileID, "cwhdf_version", "3.2");              //string
              setAttribute(fileID, "composite", startDate.equals(centeredTime)? "false" : "true"); //string

              //pass_date is days since Jan 1, 1970, e.g., {12806, 12807, 12808} for composite
              //start_time is seconds since midnight, e.g., {0, 0, 86399} for composite
              //int division truncates result
              long startMillis = Calendar2.utcToMillis(startDate);
              long endMillis   = Calendar2.utcToMillis(centeredTime);
              int startDays = (int)(startMillis / Calendar2.MILLIS_PER_DAY); //safe
              int endDays   = (int)(endMillis   / Calendar2.MILLIS_PER_DAY); //safe
              int nDays = endDays - startDays + 1;
              int pass_date[]     = new int[nDays];
              double start_time[] = new double[nDays]; //initially filled with 0's
              for (int i = 0; i < nDays; i++)
                  pass_date[i] = startDays + i;
              //int division 1000 truncates to second; % discards nDays
              start_time[nDays - 1] = (endMillis / 1000) % Calendar2.SECONDS_PER_DAY;
              setAttribute(fileID, "pass_date", pass_date);              //int32[nDays]
              setAttribute(fileID, "start_time", start_time);            //float64[nDays]

              //write map projection data
              setAttribute(fileID, "projection_type", "mapped");         //string
              setAttribute(fileID, "projection", "geographic");          //string
              setAttribute(fileID, "gctp_sys", new int[]{0});            //int32
              setAttribute(fileID, "gctp_zone", new int[]{0});           //int32
              setAttribute(fileID, "gctp_parm", new double[15]);         //float64[15 0's]
              setAttribute(fileID, "gctp_datum", new int[]{12});         //int32 12=WGS84

              //determine et_affine transformation
              // long = a*row + c*col + e
              // lat = b*row + d*col + f
              double matrix[] = {0, -latSpacing, lonSpacing,
                  0, lon[0], lat[lat.length-1]};
              setAttribute(fileID, "et_affine", matrix);                 //float64[] {a, b, c, d, e, f}

              //write row and column attributes
              setAttribute(fileID, "rows", new int[]{nLat});             //int32 number of rows
              setAttribute(fileID, "cols", new int[]{nLon});             //int32 number of columns

              //polygon attributes would be written here if needed

              //metadata for variable 0
              sdsID = HDFLibrary.SDselect(fileID, 0);
              if (verbose) String2.log("  sdsID select 0 = " + sdsID);
              setAttribute(sdsID, "long_name", varName);               //string e.g., "sea surface temperature"
              setAttribute(sdsID, "units", dataUnits);                 //string e.g., "degrees celsius"
              setAttribute(sdsID, "coordsys", "geographic");           //string
              setAttribute(sdsID, "_FillValue", new double[]{hdfMissingValue});   //same type as data as data
              setAttribute(sdsID, "missing_value", new double[]{hdfMissingValue});//same type as data as data
              setAttribute(sdsID, "scale_factor", new double[]{1});    //float64
              setAttribute(sdsID, "scale_factor_err", new double[]{0});//float64
              setAttribute(sdsID, "add_offset", new double[]{0});      //float64 calibration offset
              setAttribute(sdsID, "add_offset_err", new double[]{0});  //float64 calibration error
              setAttribute(sdsID, "calibrated_nt", new int[]{0});      //int32 hdf data type code for uncalibrated data
              setAttribute(sdsID, "fraction_digits", new int[]{dataFractionDigits}); //int32
              Test.ensureTrue(
                  HDFLibrary.SDendaccess(sdsID),
                  errorIn + "SDendaccess for variable 0.");

              //metadata for variable 1
              sdsID = HDFLibrary.SDselect(fileID, 1);
              if (verbose) String2.log("  sdsID select 1 = " + sdsID);
              setAttribute(sdsID, "long_name", "Longitude");           //string e.g., "sea surface temperature"
              setAttribute(sdsID, "units", "degrees");                 //string e.g., "degrees celsius"
              setAttribute(sdsID, "coordsys", "geographic");           //string
              setAttribute(sdsID, "_FillValue", new double[]{hdfMissingValue});   //same type as data as data
              setAttribute(sdsID, "missing_value", new double[]{hdfMissingValue});//same type as data as data
              setAttribute(sdsID, "scale_factor", new double[]{1});    //float64
              setAttribute(sdsID, "scale_factor_err", new double[]{0});//float64
              setAttribute(sdsID, "add_offset", new double[]{0});      //float64 calibration offset
              setAttribute(sdsID, "add_offset_err", new double[]{0});  //float64 calibration error
              setAttribute(sdsID, "calibrated_nt", new int[]{0});      //int32 hdf data type code for uncalibrated data
              setAttribute(sdsID, "fraction_digits", new int[]{latLonFractionDigits}); //int32
              Test.ensureTrue(
                  HDFLibrary.SDendaccess(sdsID),
                  errorIn + "SDendaccess for variable 1.");

              //metadata for variable 2
              sdsID = HDFLibrary.SDselect(fileID, 2);
              if (verbose) String2.log("  sdsID select 2 = " + sdsID);
              setAttribute(sdsID, "long_name", "Latitude");            //string e.g., "sea surface temperature"
              setAttribute(sdsID, "units", "degrees");                 //string e.g., "degrees celsius"
              setAttribute(sdsID, "coordsys", "geographic");           //string
              setAttribute(sdsID, "_FillValue", new double[]{hdfMissingValue});   //same type as data as data
              setAttribute(sdsID, "missing_value", new double[]{hdfMissingValue});//same type as data as data
              setAttribute(sdsID, "scale_factor", new double[]{1});    //float64
              setAttribute(sdsID, "scale_factor_err", new double[]{0});//float64
              setAttribute(sdsID, "add_offset", new double[]{0});      //float64 calibration offset
              setAttribute(sdsID, "add_offset_err", new double[]{0});  //float64 calibration error
              setAttribute(sdsID, "calibrated_nt", new int[]{0});      //int32 hdf data type code for uncalibrated data
              setAttribute(sdsID, "fraction_digits", new int[]{latLonFractionDigits}); //int32
              Test.ensureTrue(
                  HDFLibrary.SDendaccess(sdsID),
                  errorIn + "SDendaccess for variable 2.");

              //Close all open datasets and files and saves changes
              //Dave: hdfml("closeall");   //SDend is from Matlab book
              Test.ensureTrue(
                  HDFLibrary.SDend(fileID),
                  errorIn + "SDend.");
          }
      }
  */

  /**
   * Sets an HDF attribute value. This is PRIVATE because it isn't thread safe and should only be
   * used by saveAsHDF which is synchronized internally.
   *
   * @param id the HDF scientific dataset ID.
   * @param name the attribute name.
   * @param array an array of some primitive type or a String
   * @throws HDFException if an error occurred in an HDF routine.
   * @throws ClassNotFoundException if the HDF attribute type is unknown.
   */
  /*    private static void setAttribute(int id, String name, Object array)
          throws HDFException, ClassNotFoundException {

          boolean result;
          if (array instanceof String sa) {
              byte bar[] = sa.getBytes();
              result = HDFLibrary.SDsetattr(id, name, HDFConstants.DFNT_CHAR8, bar.length, bar);
          } else if (array instanceof byte[] ba)
              result = HDFLibrary.SDsetattr(id, name, HDFConstants.DFNT_INT8, ba.length, array);
          else if (array instanceof short[] sa)
              result = HDFLibrary.SDsetattr(id, name, HDFConstants.DFNT_INT16, sa.length, array);
          else if (array instanceof int[] ia)
              result = HDFLibrary.SDsetattr(id, name, HDFConstants.DFNT_INT32, ia.length, array);
          else if (array instanceof long[] la)
              result = HDFLibrary.SDsetattr(id, name, HDFConstants.DFNT_INT64, la.length, array);
          else if (array instanceof float[] fa)
              result = HDFLibrary.SDsetattr(id, name, HDFConstants.DFNT_FLOAT32, fa.length, array);
          else if (array instanceof double[] da)
              result = HDFLibrary.SDsetattr(id, name, HDFConstants.DFNT_FLOAT64, da.length, array);
          else throw new ClassNotFoundException ("Unsupported signed type class for: " + name);

          if (!result)
              throw new HDFException ("Cannot set attribute value for '" + name + "'.");
      }
  */

  /* INACTIVE.  If reactivated, it needs to correctly deal with makeLonPM180.
   *
   * This just determines if a GMT .grd file has any data in the specified
   * region.
   *
   * @param fullFileName
   * @param makeLonPM180 If true, the lon values will be forced to be +-180.
   *     If false, the lon values will be forced to be 0..360.
   *     See makeLonPM180().
   * @param desiredMinLon the minimum desired longitude (use Double.NaN if no restriction)
   * @param desiredMaxLon the maximum desired longitude (use Double.NaN if no restriction)
   * @param desiredMinLat the minimum desired latitude (use Double.NaN if no restriction)
   * @param desiredMaxLat the maximum desired latitude (use Double.NaN if no restriction)
   * @return true if there is data for the specified region.
   *   Returns false for any other reason (including file doesn't exist).
   * @throws Exception if file not found
   */
  /*public static boolean grdHasData(String fullFileName, boolean makeLonPM180,
              double desiredMinLon, double desiredMaxLon,
              double desiredMinLat, double desiredMaxLat)
              throws Exception {

          //*** what is in the .grd netcdf file?
          if (verbose) String2.log("Grid.grdHasData: " + fullFileName);

          //open the file (before 'try'); if it fails, no temp file to delete
          NetcdfFile grdFile = NetcdfFiles.open(fullFileName);
          try {
              long time = System.currentTimeMillis();

              //get min/maxLon
              Variable variable = grdFile.findVariable("x_range");
              ArrayDouble.D1 add1 = (ArrayDouble.D1)variable.read();
              double minLon = add1.get(0);
              double maxLon = add1.get(1);

              //calculate minLon maxLon
              if ((minLon < 0   && maxLon > 0) ||
                  (minLon < 180 && maxLon > 180)) {
                  //do nothing; some points are in eastern hemisphere; some are in western.
              } else if (makeLonPM180 && maxLon > 180) {
                  minLon = Math2.looserAnglePM180(minLon);
                  maxLon = Math2.looserAnglePM180(maxLon);
              } else if (!makeLonPM180 && minLon < 0) {
                  minLon = Math2.looserAngle0360(minLon);
                  maxLon = Math2.looserAngle0360(maxLon);
              }

              //do lon test
              if (verbose) String2.log("  minLon=" + minLon + " maxLon=" + maxLon);
              if (minLon > desiredMaxLon || maxLon < desiredMinLon) {
                  grdFile.close();
                  return false;
              }

              //get min/maxLat
              variable = grdFile.findVariable("y_range");
              add1 = (ArrayDouble.D1)variable.read();
              double minLat = add1.get(0);
              double maxLat = add1.get(1);
              if (verbose) String2.log("  minLat=" + minLat + " maxLat=" + maxLat);
              if (minLat > desiredMaxLat || maxLat < desiredMinLat) {
                  grdFile.close();
                  return false;
              }

              //makeLonPM180
              makeLonPM180(makeLonPM180);
  switch to finally clause
              grdFile.close();
              if (lon[0] > desiredMaxLon || lon[lon.length - 1] < desiredMinLon ||
                  lat[0] > desiredMaxLat || lat[lat.length - 1] < desiredMinLat) return false;
              return true;
          } catch (Exception e) {
              if (verbose) String2.log(MustBe.throwable(String2.ERROR + " in Grid.grdHasData: " + fullFileName, e));
              if (grdFile != null)
                  grdFile.close();
              return false;
          }
      } */

  /**
   * This prints a header in a format that pretty closely mimics the C version of ncdump (starting
   * with the "{") and acts as if the file will be stored as an .nc file.
   *
   * @param variableName (usually from FileNameUtility.get6CharName(fileName))
   * @param centeredTimeGC the centered time (e.g., 2006-02-17T12:00:00, usually from
   *     FileNameUtility.getCenteredTimeGC(fileName)). If it is null, secondsSinceEpoch will appear
   *     as 0.
   * @return a string representation of this grid
   */
  public String getNCHeader(String variableName, GregorianCalendar centeredTimeGC) {
    int nLon = lon.length;
    int nLat = lat.length;
    double secondsSinceEpoch =
        centeredTimeGC == null ? 0 : centeredTimeGC.getTimeInMillis() / 1000.0;
    StringBuilder sb =
        new StringBuilder(
            // this pretty closely mimics the C version of ncdump
            // (number formats are a little different)
            // and acts as if the file will be stored as an .nc file
            "{\n"
                + "dimensions:\n"
                + "\ttime = 1 ;\n"
                + "\taltitude = 1 ;\n"
                + "\tlat = "
                + nLat
                + " ;\n"
                + "\tlon = "
                + nLon
                + " ;\n"
                + "variables:\n"
                + "\tdouble time(time) ;\n"
                + "\t\ttime:actual_range = "
                + secondsSinceEpoch
                + ", "
                + secondsSinceEpoch
                + ";\n"
                + "\t\ttime:long_name = \"Centered Time\" ;\n"
                + "\t\ttime:standard_name = \"time\" ;\n"
                + "\t\ttime:units = \""
                + Calendar2.SECONDS_SINCE_1970
                + "\" ;\n"
                + "\tdouble altitude(altitude) ;\n"
                + "\t\taltitude:actual_range = 0.0, 0.0;\n"
                + "\t\taltitude:long_name = \"Altitude\" ;\n"
                + "\t\taltitude:standard_name = \"altitude\" ;\n"
                + "\t\taltitude:units = \"m\" ;\n"
                + "\tdouble lat(lat) ;\n");
    sb.append(latAttributes.toNcString("\t\tlat:", " ;"));
    sb.append("\tdouble lon(lon) ;\n");
    sb.append(lonAttributes.toNcString("\t\tlon:", " ;"));
    sb.append("\tfloat " + variableName + "(time, altitude, lat, lon) ;\n");
    sb.append(dataAttributes.toNcString("\t\t" + variableName + ":", " ;"));
    sb.append("\n" + "// global attributes:\n");
    sb.append(globalAttributes.toNcString("\t\t:", " ;"));
    sb.append("}\n");

    return sb.toString();
  }

  /**
   * This gets a string representation of this grid.
   *
   * @param printAllData If false, it prints just the 4 corners.
   * @return a string representation of this grid
   */
  public String toString(boolean printAllData) {
    calculateStats();
    StringBuilder sb =
        new StringBuilder(getNCHeader("data", null)); // dummy info is sufficient for here
    sb.append(
        lonInfoString()
            + "\n"
            + latInfoString()
            + "\n"
            + "minData="
            + minData
            + ", maxData="
            + maxData
            + ", nValidPoints="
            + nValidPoints
            + "\n");
    if (printAllData) sb.append("gridData=\n" + String2.toCSSVString(data));
    sb.append("\n");

    return sb.toString();
  }

  /**
   * This returns a string with the values of the 4 corners of the grid.
   *
   * @return a string with the values of the 4 corners of the grid.
   */
  public String fourCorners() {
    StringBuilder sb = new StringBuilder();
    // for (int lati=0; lati<3; lati++)
    //    for (int loni=0; loni<3; loni++)
    //        sb.append("  lati=" + lati + " loni=" + loni + " data=" + getData(loni, lati) + "\n");
    int nLon = lon.length;
    int nLat = lat.length;
    if (nLon == 0 || nLat == 0) return "nLon=" + nLon + ", nLat=" + nLat;
    return sb.toString()
        + "lowerLeftData="
        + getData(0, 0)
        + ", upperLeftData="
        + getData(0, nLat - 1)
        + ", lowerRightData="
        + getData(nLon - 1, 0)
        + ", upperRightData="
        + getData(nLon - 1, nLat - 1);
  }

  /**
   * For each point in lon1, this finds the index of the closest point in lon2. If there is no
   * nearest lon2, the index is set to -1. If isLon, this also tries lon2+360 and lon2-360.
   *
   * @param lon1 a ascending sorted list of lat or lon values.
   * @param lon2 a ascending sorted list of lat or lon values.
   * @param isLon use true if these are indeed lon values; or false if lat values.
   * @return an array the same length as lon1, with the indexes of the closest lon2 values (or -1 if
   *     out of range).
   */
  public static int[] binaryFindClosestIndexes(double lon1[], double lon2[], boolean isLon) {

    // for each lon1 value, find the closest lon2 value
    int lon3[] = new int[lon1.length];
    int nLon2 = lon2.length;
    double lon2Spacing = (lon2[nLon2 - 1] - lon2[0]) / (nLon2 - 1);
    double minLon2 = lon2[0] - lon2Spacing / 2;
    double maxLon2 = lon2[nLon2 - 1] + lon2Spacing / 2;
    for (int i = 0; i < lon1.length; i++) {
      double tLon = lon1[i];
      if (tLon >= minLon2 && tLon <= maxLon2) lon3[i] = Math2.binaryFindClosest(lon2, tLon);
      else if (isLon && tLon + 360 >= minLon2 && tLon + 360 <= maxLon2)
        lon3[i] = Math2.binaryFindClosest(lon2, tLon + 360);
      else if (isLon && tLon - 360 >= minLon2 && tLon - 360 <= maxLon2)
        lon3[i] = Math2.binaryFindClosest(lon2, tLon - 360);
      else lon3[i] = -1;
      // String2.log("loni=" + i + " tLon=" + tLon +
      //    " minLon2="  + minLon2 +
      //    " maxLon2=" + maxLon2 +
      //    " closest=" + lon3[i]);
    }
    return lon3;
  }

  /**
   * For each point in the current grid, this subtracts the data from the nearest point in the
   * climatology grid. If there is no nearby point, the datum is set to NaN. !!!This blanks the
   * statistics information.
   *
   * @param climatology the grid which will be subtracted from the current grid. The climatology may
   *     have a different +/-180 range than this grid.
   */
  public void subtract(Grid climatology) {
    // blank the statistics information
    nValidPoints = Integer.MIN_VALUE;
    minData = Double.NaN;
    maxData = Double.NaN;

    // for each local lat value, find the closest climatology lat value
    int closestLat[] = binaryFindClosestIndexes(lat, climatology.lat, false);
    int closestLon[] = binaryFindClosestIndexes(lon, climatology.lon, true);

    // for each point in the current grid, subtract the data from the nearest
    // point in the climatology grid
    for (int loni = 0; loni < lon.length; loni++) {
      int closestLoni = closestLon[loni];
      for (int lati = 0; lati < lat.length; lati++) {
        int closestLati = closestLat[lati];
        // String2.log("loni=" + loni + " lati=" + lati + " closestLoni=" + closestLoni + "
        // closestLati=" + closestLati);
        if (closestLoni < 0 || closestLati < 0) {
          setData(loni, lati, Double.NaN);
          continue;
        }
        // subtract   (if either is NaN, result will be NaN)
        setData(loni, lati, getData(loni, lati) - climatology.getData(closestLoni, closestLati));
      }
    }
  }

  /**
   * Given grid2, with the same lat and lon values, this changes each of the pixels in the current
   * grid to be 'fraction' of the way between this grid and grid2. ???If just one of the grid or
   * grid2 pixels is NaN, this just uses the non-NaN value. !!!This blanks the statistics
   * information.
   *
   * @param grid2 a grid with the same lat lon values as this grid
   * @param fraction is the fraction of the way between this grid and grid2 to be interpolated
   *     (e.g., 0 will just use this grid's values; 1 will just use grid2's values).
   * @throws Exception if trouble
   */
  public void interpolate(Grid grid2, double fraction) {
    // blank the statistics information
    nValidPoints = Integer.MIN_VALUE;
    minData = Double.NaN;
    maxData = Double.NaN;

    // ensure the grid2 lat and lon values match this grid's lat lon values
    String errorInMethod = String2.ERROR + " in Grid.interpolate:\n";
    Test.ensureEqual(lat, grid2.lat, errorInMethod + "lat values not equal.");
    Test.ensureEqual(lon, grid2.lon, errorInMethod + "lat values not equal.");

    // for each point, interpolate
    int n = data.length;
    for (int i = 0; i < n; i++) {
      double d1 = data[i];
      double d2 = grid2.data[i];
      if (Double.isNaN(d1)) {
        data[i] = d2;
      } else if (Double.isNaN(d2)) {
        // leave data[i] unchanged
      } else {
        // interpolate
        data[i] = d1 + fraction * (d2 - d1);
      }
    }
  }

  /**
   * CURRENTLY, THIS DOES NOTHING SINCE THE NATIVE HDFLIBRARY ISN'T BEING USED. This loads the
   * native library that is used by ncsa.hdf.hdflib.HDFLibrary, which is imported above. Currently,
   * this works with Windows and Linux only. For other OS's, this won't fail, but saveAsHDF will
   * fail because no library will have been loaded. Also, the jhdf.jar file referred to in the class
   * path must be the appropriate version (from the Windows or Linux distributions of HDFView).
   *
   * @param dir the directory with the jhdf.dll and libjhdf.so files, with a slash at the end
   */
  /*public static void initialize(String dir) {
      if (String2.OSIsWindows)
          System.load(dir + "jhdf.dll");
      else if (String2.OSIsLinux)
          System.load(dir + "libjhdf.so");
      else String2.log("Grid.initialize currently only works with Windows and Linux.");
  }
  */

  /**
   * This method converts from one type of Grid data file to another, based on their file names. See
   * msg below for details.
   *
   * @param args must contain two parameters:
   *     <ol>
   *       <li>The full name of an existing Grid file with a Dave-style file name, with an optional
   *           .zip or .gz suffix.
   *       <li>The full name of the desired Grid file with a Dave-style file name, with an optional
   *           .zip or .gz suffix.
   *     </ol>
   *     Or,
   *     <ol>
   *       <li>The full name of an existing <directory>/.<extension> describing a directory with
   *           grid files with Dave-style file names.
   *       <li>The full name of an existing <directory>/.<extension> describing a directory to be
   *           filled with grid files with Dave-style file names.
   *     </ol>
   *
   * @param fileNameUtility is used to generate all of the metadata based on the file name
   * @return trouble StringArray with full names of files that failed to convert
   * @throws Exception if trouble in set up, but error while converting a file just displays error
   *     message.
   */
  public static StringArray davesSaveAs(String args[], FileNameUtility fileNameUtility)
      throws Exception {
    // note: I wanted to add -r recursive switch. But there is no way
    // to specify the different in and out directories (unless I make things
    // dependent on coastwatch directory structure).
    String methodName = "Grid.davesSaveAs";
    String msg =
        "This program loads a grid from one data file and saves it in another\n"
            + "data file. The input and output names must be Dave-style names, e.g.,\n"
            + "  \"<dir>/AH2001067_2001067_sstd_westus.grd\" for composites, or\n"
            + "  \"<dir>/AH2005060_044800h_sstd_westus.grd\" for single passes.\n"
            + "It is ok if the names don't have \"_<regionName>\".\n"
            + "\n"
            + "usage: java gov.noaa.pfel.coastwatch.griddata.GridSaveAs <in> <out>\n"
            + "'in' and 'out' must be the complete directory + name + extension.\n"
            + "For whole directories, don't supply the name part of the 'in' and 'out'.\n"
            + "'in' and 'out' may also have a .zip or .gz extension.\n"
            + "\n"
            + "The file extensions determine what type of file is read and written:\n"
            + "  * .asc (write) an ESRI ASCII grid data file (plain .asc is not selectable)\n"
            + "  * .grd (read and write) a GMT-style NetCDF file\n"
            + "  * .hdf (read and write) a Hierarchal Data Format SDS (NASA EOSDIS) file\n"
            + "  * .mat (write) a Matlab binary file\n"
            + "  * .nc  (read and write) a NetCDF binary file\n"
            + "  * .tif (write) a georeferenced tiff (GeoTIFF) file\n"
            + "  * .xyz (write) an ASCII file with 3 columns: longitude, latitude,\n"
            + "       and data value (missing value = \"NaN\").\n"
            + "New .nc and .hdf files will have COARDS, CF, ADCC, and CWHDF compliant\n"
            + "metadata.  See MetaMetadata.txt for metadata details.  Metadata can only\n"
            + "be generated for the datasets pre-defined in\n"
            + "gov/noaa/pfel/coastwatch/DataSet.properties.  So .hdf and .nc files\n"
            + "can only be generated for those datasets.\n"
            + "The lon values in the incoming file can be either +/-180 or 0 - 360\n"
            + "and will not be altered.\n"
            + "This program is picky about the input files having exactly the right format.\n"
            + "\n"
            + "Have a pleasant day.";

    long time = System.currentTimeMillis();
    // ensure args.length = 2
    if (args == null || args.length != 2) {
      throw new Exception(
          String2.ERROR
              + ": Incorrect number of input arguments.\nargs="
              + String2.toCSSVString(args)
              + "\n\n"
              + msg);
    }

    // pick apart the file names
    String inDirName = args[0];
    String outDirName = args[1];
    String2.log(methodName + "\n  in =" + inDirName + "\n  out=" + outDirName);
    String inDir = File2.getDirectory(inDirName);
    String outDir = File2.getDirectory(outDirName);
    String inDaveNameExt = File2.getNameAndExtension(inDirName);
    String outDaveNameExt = File2.getNameAndExtension(outDirName);
    boolean inZip = inDaveNameExt.toLowerCase().endsWith(".zip");
    boolean inGz = inDaveNameExt.toLowerCase().endsWith(".gz");
    if (inZip) inDaveNameExt = inDaveNameExt.substring(0, inDaveNameExt.length() - 4);
    if (inGz) inDaveNameExt = inDaveNameExt.substring(0, inDaveNameExt.length() - 3);
    boolean outZip = outDaveNameExt.toLowerCase().endsWith(".zip");
    boolean outGz = outDaveNameExt.toLowerCase().endsWith(".gz");
    if (outZip) outDaveNameExt = outDaveNameExt.substring(0, outDaveNameExt.length() - 4);
    if (outGz) outDaveNameExt = outDaveNameExt.substring(0, outDaveNameExt.length() - 3);
    String inExt = File2.getExtension(inDaveNameExt).toLowerCase(); // e.g., ".grd"
    String outExt = File2.getExtension(outDaveNameExt).toLowerCase(); // e.g., ".nc"
    String inDaveName =
        inDaveNameExt.substring(
            0, inDaveNameExt.length() - inExt.length()); // now just a name, no ext
    String outDaveName = outDaveNameExt.substring(0, outDaveNameExt.length() - outExt.length());
    Test.ensureEqual(
        inDaveName,
        outDaveName,
        String2.ERROR
            + ": The file names (not counting directories or extensions) must be the same.\n\n"
            + msg);

    // gather the file names
    String nameList[];
    if (inDaveName.equals("")) {
      // just get dave-style names: AH2001067_2001067_sstd_westus.grd //_westus not required
      String regex =
          "[A-Z0-9]{2}[0-9]{7}_[0-9]{6}[0-9h]_.{4}.*\\"
              + inExt
              + (inZip ? "\\.[Zz][Ii][Pp]" : "")
              + (inGz ? "\\.[Gg][Zz]" : "");
      nameList = RegexFilenameFilter.list(inDir, regex);
    } else nameList = new String[] {File2.getNameAndExtension(inDirName)};

    // process the files
    StringArray trouble = new StringArray();
    for (int i = 0; i < nameList.length; i++) {
      try {
        String tDaveNameExt = nameList[i];
        String2.log("  converting " + tDaveNameExt);

        // unzip
        if (inZip) {
          SSR.unzip(inDir + tDaveNameExt, inDir, true, 10, null); // 10=seconds time out
          tDaveNameExt = tDaveNameExt.substring(0, tDaveNameExt.length() - 4);
        }
        if (inGz) {
          SSR.unGzip(inDir + tDaveNameExt, inDir, true, 10); // 10=seconds time out
          tDaveNameExt = tDaveNameExt.substring(0, tDaveNameExt.length() - 3);
        }

        String tDaveName =
            tDaveNameExt.substring(
                0, tDaveNameExt.length() - inExt.length()); // now just a name, no ext
        String tInCWName = null;
        try {
          tInCWName = FileNameUtility.convertDaveNameToCWBrowserName(tDaveName);
        } catch (Exception e) {
          String2.log(
              MustBe.throwable(
                  String2.ERROR
                      + ": Unable to convert DaveName ("
                      + tDaveName
                      + ") to CWBrowserName.",
                  e));
        }
        String tOutCWName = tInCWName;
        if (verbose) String2.log("tInCWName=" + tInCWName);

        // ensure dataset info is valid
        fileNameUtility.ensureValidDataSetProperties(
            FileNameUtility.get7CharName(tInCWName),
            true); // ensure is very important here, since this is often the conversion to
        // thredds-format data

        // read the input file  (always get the full range, file's min/max lon/lat
        Grid grid = new Grid();
        // verbose = true;
        if (inExt.equals(".grd")) {
          grid.readGrd(inDir + tDaveName + inExt);
        } else if (inExt.equals(".hdf")) {
          grid.readHDF(inDir + tDaveName + inExt, HdfConstants.DFNT_FLOAT64);
        } else if (inExt.equals(".nc")) {
          grid.readNetCDF(
              inDir + tDaveName + inExt,
              FileNameUtility.get6CharName(tInCWName),
              Double.NaN,
              Double.NaN,
              Double.NaN,
              Double.NaN,
              Integer.MAX_VALUE,
              Integer.MAX_VALUE);
        } else
          throw new Exception(
              String2.ERROR + ": Input type \"" + inExt + "\" not supported.\n\n" + msg);

        if (inZip || inGz) File2.delete(inDir + tDaveName + inExt);

        // add the metadata
        if (tOutCWName != null) grid.setAttributes(tOutCWName, fileNameUtility, false);

        // prove it has clean lon and lat values
        String2.log(
            "  lon min="
                + grid.lon[0]
                + " max="
                + grid.lon[grid.lon.length - 1]
                + " inc="
                + grid.lonSpacing
                + " n="
                + grid.lon.length);
        String2.log(
            "  lat min="
                + grid.lat[0]
                + " max="
                + grid.lat[grid.lat.length - 1]
                + " inc="
                + grid.latSpacing
                + " n="
                + grid.lat.length);

        // write the output file (use outCWName since saveAsHDF requires that)
        int saveAsType = SAVE_AS_EXTENSIONS.indexOf(outExt);
        grid.saveAs(
            outDir,
            tOutCWName,
            FileNameUtility.get6CharName(tOutCWName),
            saveAsType,
            false); // don't zip here; rename first
        File2.rename(outDir, tOutCWName + outExt, tDaveName + outExt);

        // outZip
        if (outZip) {
          SSR.zip(
              outDir + tDaveName + outExt + ".zip",
              new String[] {outDir + tDaveName + outExt},
              10); // 10 = seconds time out
          File2.delete(outDir + tDaveName + outExt);
          // String2.log("GridSaveAs deleted: " + outDir + tDaveName + outExt);
        }
        if (outGz) {
          SSR.gzip(outDir + tDaveName + outExt + ".gz", new String[] {outDir + tDaveName + outExt});
          File2.delete(outDir + tDaveName + outExt);
        }

      } catch (Exception e) {
        String2.log(String2.ERROR + ": " + MustBe.throwableToString(e));
        trouble.add(inDir + nameList[i]);
      }
    }

    // done
    String2.log(
        "GridSaveAs finished. "
            + (nameList.length - trouble.size())
            + " out of "
            + nameList.length
            + " file(s) were converted in "
            + (System.currentTimeMillis() - time)
            + " ms.");
    if (trouble.size() > 0)
      String2.log("The unsuccessful files were:\n" + trouble.toNewlineString());
    return trouble;
  }

  /**
   * This saves the current grid in some type of file. If the file already exists, it is touched,
   * and nothing else is done. This does not add attributes (other than calling addStatsAttributes
   * ()).
   *
   * @param directory the directory for the resulting file (with a slash at the end)
   * @param fileName the name for the resulting file (without any extension)
   * @param variableName the name for the variable, usually FileNameUtility.get6CharName(fileName)
   * @param saveAsType one of the SAVE_AS constants
   * @param zipIt If true, creates a .zip file and deletes the intermediate file (e.g., .asc). If
   *     false, the specified saveAsType is created.
   * @throws Exception if trouble
   */
  public void saveAs(
      String directory, String fileName, String variableName, int saveAsType, boolean zipIt)
      throws Exception {

    if (verbose) String2.log("Grid.saveAs(name=" + fileName + " type=" + saveAsType + ")");
    if (saveAsType != SAVE_AS_ASCII
        && saveAsType != SAVE_AS_ESRI_ASCII
        && saveAsType != SAVE_AS_GEOTIFF
        && saveAsType != SAVE_AS_GRD
        && saveAsType != SAVE_AS_HDF
        && saveAsType != SAVE_AS_MATLAB
        && saveAsType != SAVE_AS_NETCDF
        && saveAsType != SAVE_AS_XYZ)
      throw new RuntimeException(
          String2.ERROR + " in Grid.saveAs: invalid saveAsType=" + saveAsType);

    String ext = SAVE_AS_EXTENSIONS.get(saveAsType);

    // does the file already exist?
    String finalName = directory + fileName + ext + (zipIt ? ".zip" : "");
    if (File2.touch(finalName)) {
      if (verbose) String2.log("  Grid.saveAs done. reusing " + finalName);
      return;
    }

    // save as ...
    if (saveAsType == SAVE_AS_ASCII) saveAsASCII(directory, fileName);
    else if (saveAsType == SAVE_AS_ESRI_ASCII) saveAsEsriASCII(directory, fileName);
    else if (saveAsType == SAVE_AS_GEOTIFF) saveAsGeotiff(directory, fileName, variableName);
    else if (saveAsType == SAVE_AS_GRD) saveAsGrd(directory, fileName);
    else if (saveAsType == SAVE_AS_HDF) saveAsHDF(directory + fileName, variableName);
    else if (saveAsType == SAVE_AS_MATLAB) saveAsMatlab(directory, fileName, variableName);
    else if (saveAsType == SAVE_AS_NETCDF) saveAsNetCDF(directory, fileName, variableName);
    else if (saveAsType == SAVE_AS_XYZ) saveAsXYZ(directory, fileName);

    if (zipIt) {
      // zip to a temporary zip file, -j: don't include dir info
      SSR.zip(
          directory + fileName + ext + ".temp.zip", new String[] {directory + fileName + ext}, 20);

      // delete the file that was zipped
      File2.delete(directory + fileName + ext);

      // if all successful, rename to final name
      File2.rename(directory, fileName + ext + ".temp.zip", fileName + ext + ".zip");
    }
  }

  /**
   * Convert a .grd file with a Dave-style name (e.g., "<dir>/AH2001067_2001069_sstd_westus.grd" for
   * composites, or "<dir>/AH2005060_044800h_sstd_westus.grd" for single passes) into a .nc file
   * with the specified name (with any style name) and lots of metadata (see setAttributes()).
   *
   * @param fullDaveGrdFileName the complete Dave-style file name of the extant .grd input file
   * @param fullNetCDFFileName the complete Dave-style file name of the extant .grd input file
   * @param fileNameUtility is used to generate all of the metadata based on the file name
   * @param makeLonPM180 If true, the lon values will be forced to be +-180. If false, the lon
   *     values will be forced to be 0..360. However, if some points are in the western hemisphere
   *     and some are in the eastern hemisphere, this parameter is disregarded and the lon values
   *     are not modified.
   * @throws Exception
   */
  /*    public static void convertDaveGrdToNetCDF(String fullDaveGrdFileName,
          String fullNetCDFFileName,
          FileNameUtility fileNameUtility, boolean makeLonPM180) throws Exception {

          //generate the CWBrowser version of the grd name
          String daveName = File2.getNameAndExtension(fullDaveGrdFileName);
          Test.ensureTrue(daveName.toLowerCase().endsWith(".grd"),
              String2.ERROR + "in Grid.convertDaveGrdToNetCDF: the input file name (" +
              fullDaveGrdFileName + ") must end in \".grd\".");
          daveName = daveName.substring(0, daveName.length() - 4);
          String cwBrowserName = FileNameUtility.convertDaveNameToCWBrowserName(daveName);

          //split the netCDF file name
          String ncDir = File2.getDirectory(fullNetCDFFileName);
          String ncName = File2.getNameAndExtension(fullNetCDFFileName);
          Test.ensureTrue(ncName.toLowerCase().endsWith(".nc"),
              String2.ERROR + "in Grid.convertDaveGrdToNetCDF: the ouput file name (" +
              fullNetCDFFileName + ") must end in \".nc\".");
          ncName = ncName.substring(0, ncName.length() - 3);

          //load the .grd file
          Grid grid = new Grid();
          grid.readGrd(fullDaveGrdFileName, makeLonPM180);

          //add the metadata
          grid.setAttributes(cwBrowserName, fileNameUtility, false);

          //generate the .nc file
          grid.saveAsNetCDF(ncDir, ncName, FileNameUtility.get6CharName(cwBrowserName));

      }
  */

  /**
   * Convert a .nc file with a Dave-style name (e.g., "<dir>/AH2001067_2001069_sstd_westus.grd" for
   * composites, or "<dir>/AH2005060_044800h_sstd_westus.grd" for single passes) into the specified
   * .grd file (with any style name).
   *
   * @param fullDaveNcFileName the complete Dave-style file name of the extant .nc input file
   * @param fullGrdFileName the complete name (any style) for the .grd file.
   * @param makeLonPM180 If true, the lon values will be forced to be +-180. If false, the lon
   *     values will be forced to be 0..360. However, if some points are in the western hemisphere
   *     and some are in the eastern hemisphere, this parameter is disregarded and the lon values
   *     are not modified.
   * @throws Exception
   */
  /*  public static void convertDaveNetCDFToGrd(String fullDaveNcFileName,
          String fullGrdFileName, boolean makeLonPM180) throws Exception {

          //generate the CWBrowser version of the ncName
          String daveName = File2.getNameAndExtension(fullDaveNcFileName);
          Test.ensureTrue(daveName.toLowerCase().endsWith(".nc"),
              String2.ERROR + " in Grid.convertDaveNetCDFToGrd: the input file name (" +
              fullDaveNcFileName + ") must end in \".nc\".");
          daveName = daveName.substring(0, daveName.length() - 3);
          String cwBrowserName = FileNameUtility.convertDaveNameToCWBrowserName(daveName);

          //split the grd file name
          String grdDir = File2.getDirectory(fullGrdFileName);
          String grdName = File2.getNameAndExtension(fullGrdFileName);
          Test.ensureTrue(grdName.toLowerCase().endsWith(".grd"),
              String2.ERROR + "in Grid.convertDaveNetCDFToGrd: the ouput file name (" +
              fullGrdFileName + ") must end in \".grd\".");
          grdName = grdName.substring(0, grdName.length() - 4);

          //load the netCDF file
          Grid grid = new Grid();
          grid.readNetCDF(fullDaveNcFileName,
              FileNameUtility.get6CharName(cwBrowserName), makeLonPM180);

          //generate the .grd file
          grid.saveAsGrd(grdDir, grdName);

      }
  */
  /**
   * This tests convertGrdToNc.main and convertNcToGrd.main and the local methods
   * convertDaveGrdToDaveNetCDF and convertDaveNetCDFToDaveGrd.
   *
   * @param fileNameUtility
   * @throws Exception if trouble
   */
  /*    public static void testConvertGrdToFromNetCDF() throws Exception {

          String2.log("\n*** Grid.testConvertGrdToNc");
          String errorIn = String2.ERROR + " in Grid.testConvertGrdToNc: ";

          //copy the .grd file to a Dave-style name
          String cwName = testName;
          FileNameUtility fnu = new FileNameUtility("gov.noaa.pfel.coastwatch.CWBrowser");
          String daveName = fnu.convertCWBrowserNameToDaveName(cwName);
          Test.ensureTrue(
              File2.copy(testDir + cwName + ".grd",  testDir + daveName + ".grd"),
              errorIn + "copy " + testDir + cwName + ".grd.");

          //read the .grd file
          Grid grid1 = new Grid();
          grid1.readGrd(testDir + daveName + ".grd", false); //false=not pm180

          //*** make the .nc file and delete the grd file
          File2.verbose = true;
          ConvertGrdToNc.main(new String[]{
              testDir + daveName + ".grd",
              testDir + daveName + ".nc"});
          Test.ensureTrue(File2.delete(testDir + daveName + ".grd"),
              errorIn + " while deleting " + testDir + daveName + ".grd");

          //read .nc file
          Grid grid2 = new Grid();
          grid2.readNetCDF(testDir + daveName + ".nc",
              FileNameUtility.get6CharName(cwName), false); //false=not pm180

          //are they the same?
          Test.ensureTrue(grid1.equals(grid2), errorIn);

          //*** make the .grd file and delete the .nc file
          ConvertNcToGrd.main(new String[]{
              testDir + daveName + ".nc",
              testDir + daveName + ".grd"});
          Test.ensureTrue(File2.delete(testDir + daveName + ".nc"),
              errorIn + " while deleting " + testDir + daveName + ".nc");

          //read .grd file
          Grid grid3 = new Grid();
          grid3.readGrd(testDir + daveName + ".grd", false); //false=not pm180

          //are they the same?
          Test.ensureTrue(grid1.equals(grid3), errorIn);

          //delete the .grd file
  //        Test.ensureTrue(File2.delete(testDir + daveName + ".grd"),
  //            errorIn + " while deleting " + testDir + daveName + ".grd");

          //FUTURE: check metadata
      }
  */

  /**
   * This converts a .nc file with a 3D grid data(time, lat, lon) (integers) into an .xyz file.
   * Missing data values will be written as "NaN".
   *
   * @param inName the full name of the input .nc file, e.g., "c:/temp/temp/sst.mnmean.nc".
   * @param outName the full name of the output .xyz file, e.g., "c:/temp/temp/sst.mnmean.xyz".
   * @param xName the name of the x variable in the .nc file, e.g., "lon".
   * @param yName the name of the y variable in the .nc file, e.g., "lat".
   * @param tName the name of the time variable in the .nc file, e.g., "time".
   * @param dataName the name of the data variable in the .nc file, e.g., "sst".
   * @throws Exception if touble
   */
  public static void ncXYTtoAsciiXYT(
      String inName, String outName, String xName, String yName, String tName, String dataName)
      throws Exception {

    String newline = String2.lineSeparator;

    // open the netcdf file
    String2.log("ncXYTtoAsciiXYT: " + inName + " to " + outName);

    NetcdfFile ncFile = NcHelper.openFile(inName);
    try {
      // read the dimensions
      double[] xArray = NcHelper.getNiceDoubleArray(ncFile.findVariable(xName), 0, -1).toArray();
      double[] yArray = NcHelper.getNiceDoubleArray(ncFile.findVariable(yName), 0, -1).toArray();
      double[] tArray = NcHelper.getNiceDoubleArray(ncFile.findVariable(tName), 0, -1).toArray();
      String2.log("xArray=" + xArray[0] + " to " + xArray[xArray.length - 1]);
      String2.log("yArray=" + yArray[0] + " to " + yArray[yArray.length - 1]);
      String2.log("tArray=" + tArray[0] + " to " + tArray[tArray.length - 1]);

      // convert t ("days since 1800-1-1 00:00:00") to iso
      String tStrings[] = new String[tArray.length];
      for (int t = 0; t < tArray.length; t++) {
        GregorianCalendar base = Calendar2.newGCalendarZulu(1800, 1, 1);
        base.add(Calendar2.DATE, Math2.roundToInt(tArray[t]));
        tStrings[t] = Calendar2.formatAsISODate(base);
      }
      String2.log("tStrings=" + tStrings[0] + " to " + tStrings[tStrings.length - 1]);

      // read the data   sst(time,lat,lon)
      Variable dataVariable = ncFile.findVariable(dataName);
      ArrayShort.D3 data = (ArrayShort.D3) dataVariable.read();

      // get the scaleFactor
      Attribute attribute = dataVariable.findAttribute("scale_factor");
      double scaleFactor = attribute == null ? 1 : NcHelper.getNiceDouble(attribute);

      // get the addOffset
      attribute = dataVariable.findAttribute("add_offset");
      double addOffset = attribute == null ? 0 : NcHelper.getNiceDouble(attribute);

      // get the missingValue
      attribute = dataVariable.findAttribute("missing_value");
      int missingValue =
          attribute == null
              ? Integer.MAX_VALUE
              : Math2.roundToInt(NcHelper.getNiceDouble(attribute));

      String2.log(
          "missingValue="
              + scaleFactor
              + " addOffset="
              + addOffset
              + " missingValue="
              + missingValue);

      // write to ascii
      Writer ascii = File2.getBufferedFileWriter88591(outName);
      try {
        String s = xName + "\t" + yName + "\t" + tName + "\t" + dataName + newline;
        ascii.write(s, 0, s.length());
        for (int t = 0; t < tArray.length; t++) {
          if ((t % 10) == 0) String2.log("writing t=" + t + " of " + tArray.length);
          for (int y = 0; y < yArray.length; y++) {
            for (int x = 0; x < xArray.length; x++) {
              int datum = data.get(t, y, x);
              s =
                  xArray[x]
                      + "\t"
                      + yArray[y]
                      + "\t"
                      + tStrings[t]
                      + "\t"
                      + (datum == missingValue
                          ? "NaN"
                          :
                          // treat like float; don't let scaling add excess digits
                          String2.genEFormat6(datum * scaleFactor + addOffset))
                      + newline;
              ascii.write(s, 0, s.length());
            }
          }
        }
      } finally {
        try {
          ascii.close();
        } catch (Exception e) {
        }
      }
    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * Given an xytd land mask ascii file (0=land 1=sea) and an xytd ascii file with the same grid
   * (although the ascii file can be a subset), this replaces the data values in the ascii files
   * with NaN where there is land.
   *
   * @param landMaskFileName An ISO-8859-1 file, perhaps externally compressed.
   * @param dataFileName An ISO-8859-1 file, perhaps externally compressed.
   * @param newFileName
   */
  public static void maskAsciiXYT(String landMaskFileName, String dataFileName, String newFileName)
      throws Exception {

    String s;
    String newline = String2.lineSeparator;

    // read the land mask into a hash table
    HashSet<String> hashSet = new HashSet();
    BufferedReader maskFile = File2.getDecompressedBufferedFileReader88591(landMaskFileName);
    try {
      s = maskFile.readLine(); // skip col names
      s = maskFile.readLine();
      while (s != null) {
        // store land bits to the hashSet
        String fields[] = String2.split(s, '\t'); // x,y,t,d
        if (fields[3].equals("0")) // land
        hashSet.add(fields[0] + fields[1]);

        // read the next line
        s = maskFile.readLine();
      }
    } finally {
      maskFile.close();
    }

    // read the asciiFile and write new newFile
    BufferedReader in = File2.getDecompressedBufferedFileReader88591(dataFileName);
    try {
      BufferedWriter out = File2.getBufferedFileWriter88591(newFileName);
      try {
        s = in.readLine(); // skip col names
        s = in.readLine();
        while (s != null) {
          // replace land data with NaN
          String fields[] = String2.split(s, '\t'); // x,y,t,d
          if (hashSet.contains(fields[0] + fields[1])) fields[3] = "NaN";
          s = String2.toSVString(fields, "\t", false) + newline;
          out.write(s, 0, s.length());

          // read the next line
          s = in.readLine();
        }
      } finally {
        out.close();
      }
    } finally {
      in.close();
    }
  }

  /**
   * This sets globalAttributes, latAttributes, lonAttributes, and dataAttributes so that the
   * attributes have COARDS, CF, THREDDS ACDD, and CWHDF-compliant metadata attributes. This also
   * calls calculateStats, so that information will be up-to-date. See MetaMetadata.txt for more
   * information.
   *
   * <p>This is used in some situations. In other situations, gridDataSet.setAttributes is used.
   *
   * @param name A CWBrowser-style file name (without directory info) so that fileNameUtility can
   *     generate the information.
   * @param fileNameUtility is used to generate all of the metadata based on the file name
   * @param saveMVAsDouble If true, _FillValue and missing_value are saved as doubles, else floats.
   */
  public void setAttributes(String name, FileNameUtility fileNameUtility, boolean saveMVAsDouble)
      throws Exception {
    Test.ensureNotNull(fileNameUtility, "fileNameUtility is null.");
    // should this clear existing attributes?

    // calculateStats
    calculateStats();

    // assemble the global metadata attributes
    int nLat = lat.length;
    int nLon = lon.length;
    globalAttributes.set("Conventions", FileNameUtility.getConventions());
    globalAttributes.set("title", fileNameUtility.getBoldTitle(name));
    globalAttributes.set("summary", fileNameUtility.getAbstract(name));
    globalAttributes.set("keywords", fileNameUtility.getKeywords(name));
    globalAttributes.set("id", FileNameUtility.getID(name));
    globalAttributes.set("naming_authority", FileNameUtility.getNamingAuthority());
    globalAttributes.set("keywords_vocabulary", FileNameUtility.getKeywordsVocabulary());
    globalAttributes.set("cdm_data_type", FileNameUtility.getCDMDataType());
    globalAttributes.set("date_created", FileNameUtility.getDateCreated());
    globalAttributes.set("creator_name", FileNameUtility.getCreatorName());
    globalAttributes.set("creator_url", FileNameUtility.getCreatorURL());
    globalAttributes.set("creator_email", FileNameUtility.getCreatorEmail());
    globalAttributes.set("institution", fileNameUtility.getInstitution(name));
    globalAttributes.set("project", FileNameUtility.getProject());
    globalAttributes.set("processing_level", FileNameUtility.getProcessingLevel());
    globalAttributes.set("acknowledgement", FileNameUtility.getAcknowledgement());
    globalAttributes.set(
        "geospatial_vertical_min", 0.0); // currently depth always 0.0 (not 0, which is int)
    globalAttributes.set("geospatial_vertical_max", 0.0);
    globalAttributes.set("geospatial_lat_min", Math.min(lat[0], lat[nLat - 1]));
    globalAttributes.set("geospatial_lat_max", Math.max(lat[0], lat[nLat - 1]));
    globalAttributes.set("geospatial_lon_min", Math.min(lon[0], lon[nLon - 1]));
    globalAttributes.set("geospatial_lon_max", Math.max(lon[0], lon[nLon - 1]));
    globalAttributes.set("geospatial_vertical_units", "m");
    globalAttributes.set("geospatial_vertical_positive", "up");
    globalAttributes.set("geospatial_lat_units", FileNameUtility.getLatUnits());
    globalAttributes.set("geospatial_lat_resolution", Math.abs(latSpacing));
    globalAttributes.set("geospatial_lon_units", FileNameUtility.getLonUnits());
    globalAttributes.set("geospatial_lon_resolution", Math.abs(lonSpacing));
    globalAttributes.set(
        "time_coverage_start",
        Calendar2.formatAsISODateTimeTZ(FileNameUtility.getStartCalendar(name)));
    globalAttributes.set(
        "time_coverage_end", Calendar2.formatAsISODateTimeTZ(FileNameUtility.getEndCalendar(name)));
    // globalAttributes.set("time_coverage_resolution", "P12H"));
    globalAttributes.set("standard_name_vocabulary", FileNameUtility.getStandardNameVocabulary());
    globalAttributes.set("license", FileNameUtility.getLicense());
    if (globalAttributes.get("contributor_name") == null) {
      globalAttributes.set("contributor_name", fileNameUtility.getContributorName(name));
      globalAttributes.set("contributor_role", FileNameUtility.getContributorRole());
    }
    globalAttributes.set("date_issued", FileNameUtility.getDateCreated());
    globalAttributes.set("references", fileNameUtility.getReferences(name));
    globalAttributes.set("source", fileNameUtility.getSource(name));
    // attributes for Google Earth
    globalAttributes.set("Southernmost_Northing", Math.min(lat[0], lat[nLat - 1]));
    globalAttributes.set("Northernmost_Northing", Math.max(lat[0], lat[nLat - 1]));
    globalAttributes.set("Westernmost_Easting", Math.min(lon[0], lon[nLon - 1]));
    globalAttributes.set("Easternmost_Easting", Math.max(lon[0], lon[nLon - 1]));

    // globalAttributes for HDF files using CoastWatch Metadata Specifications
    // required unless noted otherwise
    globalAttributes.set("cwhdf_version", "3.4"); // string
    String satellite = fileNameUtility.getSatellite(name);
    if (satellite.length() > 0) {
      globalAttributes.set("satellite", fileNameUtility.getSatellite(name)); // string
      globalAttributes.set("sensor", fileNameUtility.getSensor(name)); // string
    } else {
      globalAttributes.set("data_source", fileNameUtility.getSensor(name)); // string
    }
    globalAttributes.set("composite", FileNameUtility.getComposite(name)); // string (not required)

    globalAttributes.set(
        "pass_date", new IntArray(FileNameUtility.getPassDate(name))); // int32[nDays]
    globalAttributes.set(
        "start_time", new DoubleArray(FileNameUtility.getStartTime(name))); // float64[nDays]
    globalAttributes.set("origin", fileNameUtility.getCourtesy(name)); // string
    globalAttributes.set("history", fileNameUtility.getHistory(name)); // string

    // write map projection data
    globalAttributes.set("projection_type", "mapped"); // string
    globalAttributes.set("projection", "geographic"); // string
    globalAttributes.set("gctp_sys", 0); // int32
    globalAttributes.set("gctp_zone", 0); // int32
    globalAttributes.set("gctp_parm", new DoubleArray(new double[15])); // float64[15 0's]
    globalAttributes.set("gctp_datum", 12); // int32 12=WGS84

    // determine et_affine transformation
    // lon = a*row + c*col + e
    // lat = b*row + d*col + f
    double matrix[] = {0, -latSpacing, lonSpacing, 0, lon[0], lat[lat.length - 1]}; // up side down
    globalAttributes.set("et_affine", new DoubleArray(matrix)); // float64[] {a, b, c, d, e, f}

    // write row and column attributes
    globalAttributes.set("rows", nLat); // int32 number of rows
    globalAttributes.set("cols", nLon); // int32 number of columns
    globalAttributes.set(
        "polygon_latitude",
        new DoubleArray(
            new double[] { // not required
              lat[0], lat[nLat - 1], lat[nLat - 1], lat[0], lat[0]
            }));
    globalAttributes.set(
        "polygon_longitude",
        new DoubleArray(
            new double[] { // not required
              lon[0], lon[0], lon[nLon - 1], lon[nLon - 1], lon[0]
            }));

    // COARDS, CF, ACDD metadata attributes for latitude
    latAttributes.set("_CoordinateAxisType", "Lat");
    latAttributes.set("long_name", "Latitude");
    latAttributes.set("standard_name", "latitude");
    latAttributes.set("units", FileNameUtility.getLatUnits());

    // Lynn's metadata attributes
    latAttributes.set("point_spacing", "even");
    latAttributes.set("actual_range", new DoubleArray(new double[] {lat[0], lat[nLat - 1]}));

    // CWHDF metadata attributes for Latitude
    // latAttributes.set("long_name",             "Latitude")); //string
    // latAttributes.set("units",                 fileNameUtility.getLatUnits(name))); //string
    latAttributes.set("coordsys", "geographic"); // string
    latAttributes.set("fraction_digits", fileNameUtility.getLatLonFractionDigits(name)); // int32

    // COARDS, CF, ACDD metadata attributes for longitude
    lonAttributes.set("_CoordinateAxisType", "Lon");
    lonAttributes.set("long_name", "Longitude");
    lonAttributes.set("standard_name", "longitude");
    lonAttributes.set("units", FileNameUtility.getLonUnits());

    // Lynn's metadata attributes
    lonAttributes.set("point_spacing", "even");
    lonAttributes.set("actual_range", new DoubleArray(new double[] {lon[0], lon[nLon - 1]}));

    // CWHDF metadata attributes for Longitude
    // lonAttributes.set("long_name",             "Longitude"); //string
    // lonAttributes.set("units",                 fileNameUtility.getLonUnits(name));  //string
    lonAttributes.set("coordsys", "geographic"); // string
    lonAttributes.set("fraction_digits", fileNameUtility.getLatLonFractionDigits(name)); // int32

    // COARDS, CF, ACDD metadata attributes for data
    dataAttributes.set("long_name", fileNameUtility.getBoldTitle(name));
    String sn = fileNameUtility.getStandardName(name);
    if (sn.length() > 0) {
      dataAttributes.set("standard_name", sn);
    }
    dataAttributes.set("units", fileNameUtility.getUdUnits(name));
    PrimitiveArray mvAr;
    PrimitiveArray rangeAr;
    if (saveMVAsDouble) {
      mvAr = new DoubleArray(new double[] {(double) DataHelper.FAKE_MISSING_VALUE});
      rangeAr = new DoubleArray(new double[] {minData, maxData});
    } else {
      mvAr = new FloatArray(new float[] {(float) DataHelper.FAKE_MISSING_VALUE});
      rangeAr = new FloatArray(new float[] {(float) minData, (float) maxData});
    }
    dataAttributes.set("_FillValue", mvAr); // must be same type as data
    dataAttributes.set("missing_value", mvAr); // must be same type as data
    dataAttributes.set("numberOfObservations", nValidPoints);
    dataAttributes.set("actual_range", rangeAr);

    // CWHDF metadata attributes for the data: varName
    // dataAttributes.set("long_name",            fileNameUtility.getTitle(name))); //string
    // dataAttributes.set("units",                fileNameUtility.getUDUnits(name))); //string
    dataAttributes.set("coordsys", "geographic"); // string
    dataAttributes.set("fraction_digits", fileNameUtility.getDataFractionDigits(name)); // int32
  }

  /**
   * This class is designed to be a stand-alone program to convert from one type of Grid data file
   * to another. See msg below for details.
   *
   * <p>This is tested in Grid.
   *
   * @param args must contain two parameters: <in> <out> . 'in' and 'out' must be the complete
   *     directory + name + extension. To convert whole directories, don't supply the name part of
   *     the 'in' and 'out'. 'in' and 'out' may also have a .zip or .gz extension.
   */
  public static void main(String args[]) throws Exception {
    // Grid.verbose = true;
    davesSaveAs(args, new FileNameUtility("gov.noaa.pfel.coastwatch.TimePeriods"));
  }

  /**
   * This is a unit test for saveAsHDF and readHDF; it makes
   * .../coastwatch/griddata/<testName>Test.hdf.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testHDF() throws Exception {
    String2.log("\n*** Grid.testHDF");
    // load the data
    Grid grid1 = new Grid();
    grid1.readGrd(
        testDir + testName + ".grd", true); // +-180; put longitude into Atlanticentric reference

    // set attributes
    // if (testResult) {
    // set just a few attributes, because that's all the source has
    grid1.globalAttributes().set("title", "Wind, QuikSCAT Seawinds, Composite, Zonal");
    grid1.latAttributes().set("units", FileNameUtility.getLatUnits());
    grid1.lonAttributes().set("units", FileNameUtility.getLonUnits());
    grid1.dataAttributes().set("units", "m s-1");
    // } else {
    // //set all atts
    // grid1.setAttributes(testName, fileNameUtility, false); //data saved not as
    // doubles
    // }

    // save the data
    grid1.saveAsHDF(testDir + testName + "Test", "QNux10");

    // if (testResult) {
    // now try to read it
    Grid grid2 = new Grid();
    grid2.readHDF(testDir + testName + "Test.hdf", HdfConstants.DFNT_FLOAT64);
    // makeLonPM180(true);

    // look for differences
    Test.ensureTrue(grid1.equals(grid2), "testHDF");

    // attributes
    Test.ensureEqual(
        grid1.globalAttributes().getString("title"),
        "Wind, QuikSCAT Seawinds, Composite, Zonal",
        "");
    Test.ensureEqual(grid1.latAttributes().getString("units"), FileNameUtility.getLatUnits(), "");
    Test.ensureEqual(grid1.lonAttributes().getString("units"), FileNameUtility.getLonUnits(), "");
    Test.ensureEqual(grid1.dataAttributes().getString("units"), "m s-1", "");

    // delete the test file
    Test.ensureTrue(
        File2.delete(testDir + testName + "Test.hdf"),
        String2.ERROR + " in Grid.testHDF: unable to delete " + testDir + testName + "Test.hdf");
    // }
  }

  /**
   * This tests GridSaveAs.main and the local method saveAs.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testSaveAs() throws Exception {

    String2.log("\n*** Grid.testSaveAs");
    String errorIn = String2.ERROR + " in Grid.testSaveAs: ";
    File2.verbose = true;
    String cwName = testName;

    // read a .grd file (for reference)
    Grid grid1 = new Grid();
    grid1.readGrd(testDir + cwName + ".grd", false); // false=not pm180

    // ************* create the write-only file types
    // make a Dave-style .grd file from the cwName'd file
    FileNameUtility fnu = new FileNameUtility("gov.noaa.pfel.coastwatch.TimePeriods");
    String daveName = fnu.convertCWBrowserNameToDaveName(cwName);

    // make sure the files don't exist
    File2.delete(testDir + daveName + ".asc");
    File2.delete(testDir + daveName + ".grd");
    File2.delete(testDir + daveName + ".hdf");
    File2.delete(testDir + daveName + ".mat");
    File2.delete(testDir + daveName + ".nc");
    File2.delete(testDir + daveName + ".xyz");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".asc"), ".asc");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".grd"), ".grd");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".hdf"), ".hdf");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".mat"), ".mat");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".nc"), ".nc");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".xyz"), ".xyz");

    // starting point: make a daveName.grd file by copying from cwName.grd file
    Test.ensureTrue(
        File2.copy(testDir + cwName + ".grd", testDir + daveName + ".grd"),
        errorIn + "copy " + testDir + cwName + ".grd.");

    // test GridSaveAs.main: make the .asc file from the .grd file
    GridSaveAs.main(new String[] {testDir + daveName + ".grd", testDir + daveName + ".asc"});

    // test GridSaveAs.main: make the .mat file from the .grd file
    GridSaveAs.main(new String[] {testDir + daveName + ".grd", testDir + daveName + ".mat"});

    // test GridSaveAs.main: make the .xyz file from the .grd file
    GridSaveAs.main(new String[] {testDir + daveName + ".grd", testDir + daveName + ".xyz"});

    // ensure asc was saved as pm180
    if (true) { // so sar is garbage collected
      String sar[] = File2.readFromFile88591(testDir + daveName + ".asc");
      Test.ensureEqual(sar[0], "", "");
      // String2.log(sar[1].substring(1, 200));
      String t =
          "ncols 121\n"
              + "nrows 113\n"
              + "xllcenter -135.0\n"
              + "yllcenter 22.0\n"
              + "cellsize 0.25\n"
              + "nodata_value -9999999\n"
              + "4.2154 4.20402 4.39077";
      Test.ensureEqual(sar[1].substring(0, t.length()), t, "");
    }

    // ************ Test .grd to .nc to .grd
    // make a Dave-style .grd file from the cwName'd file
    Test.ensureTrue(
        File2.copy(testDir + cwName + ".grd", testDir + daveName + ".grd"),
        errorIn + "copy " + testDir + cwName + ".grd.");

    // test GridSaveAs.main: make the .nc file from the .grd file
    GridSaveAs.main(new String[] {testDir + daveName + ".grd", testDir + daveName + ".nc"});

    // delete the Dave-style grd file
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".grd"),
        errorIn + " while deleting " + testDir + daveName + ".grd");

    // read .nc file
    Grid grid2 = new Grid();
    grid2.readNetCDF(
        testDir + daveName + ".nc", FileNameUtility.get6CharName(cwName), false); // false=not pm180

    // are they the same?
    Test.ensureTrue(grid1.equals(grid2), errorIn);

    // test GridSaveAs.main: make a .grd file from the .nc file
    GridSaveAs.main(new String[] {testDir + daveName + ".nc", testDir + daveName + ".grd"});

    // read .grd file
    Grid grid3 = new Grid();
    grid3.readGrd(testDir + daveName + ".grd", false); // false=not pm180

    // are they the same?
    Test.ensureTrue(grid1.equals(grid3), errorIn);

    // delete the .grd file
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".grd"),
        errorIn + " while deleting " + testDir + daveName + ".grd");

    // ******************* Test .grd to .hdf to .grd
    // make a Dave-style .grd file from the cwName'd file
    Test.ensureTrue(
        File2.copy(testDir + cwName + ".grd", testDir + daveName + ".grd"),
        errorIn + "copy " + testDir + cwName + ".grd.");

    // test GridSaveAs.main: make the .hdf file from the .grd file
    GridSaveAs.main(new String[] {testDir + daveName + ".grd", testDir + daveName + ".hdf"});

    // delete the Dave-style grd file
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".grd"),
        errorIn + " while deleting " + testDir + daveName + ".grd");

    // read .hdf file
    grid2 = new Grid();
    grid2.readHDF(testDir + daveName + ".hdf", HdfConstants.DFNT_FLOAT64); // I write FLOAT64 files
    grid2.makeLonPM180(false);

    // are they the same?
    Test.ensureTrue(grid1.equals(grid2), errorIn);

    // test GridSaveAs.main: make a .grd file from the .hdf file
    GridSaveAs.main(new String[] {testDir + daveName + ".hdf", testDir + daveName + ".grd"});

    // delete the .hdf file
    // Test.ensureTrue(File2.delete(testDir + daveName + ".hdf"),
    // errorIn + " while deleting " + testDir + daveName + ".hdf");

    // read .grd file
    grid3 = new Grid();
    grid3.readGrd(testDir + daveName + ".grd", false); // false=not pm180

    // are they the same?
    Test.ensureTrue(grid1.equals(grid3), errorIn);

    // that leaves a created .grd file extant

    // ***test whole directory .grd to .hdf.zip
    // make sure the files don't exist
    File2.delete(testDir + daveName + ".hdf");
    File2.delete(testDir + daveName + ".hdf.zip");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".hdf"), ".hdf");
    Test.ensureTrue(!File2.isFile(testDir + daveName + ".hdf.zip"), ".hdf.zip");

    // test GridSaveAs.main: make the .hdf file from the .grd file
    GridSaveAs.main(
        new String[] {
          testDir + ".grd", // no "daveName+" because doing whole directory
          testDir + ".hdf.zip"
        }); // no "daveName+" because doing whole directory

    // ensure that the temp .hdf (without the .zip) was deleted
    Test.ensureEqual(
        File2.isFile(testDir + daveName + ".hdf"),
        false,
        ".hdf (no .zip) not deleted: " + testDir + daveName + ".hdf");

    // unzip the .hdf file
    SSR.unzipRename(testDir, daveName + ".hdf.zip", testDir, daveName + ".hdf", 10);

    // read the hdf file
    Grid grid4 = new Grid();
    grid4.readHDF(testDir + daveName + ".hdf", HdfConstants.DFNT_FLOAT64);
    grid4.makeLonPM180(false);

    // are they the same?
    Test.ensureTrue(grid4.equals(grid3), errorIn);

    // ***test whole directory .grd to .hdf
    // make sure the files don't exist
    File2.delete(testDir + daveName + ".hdf");
    File2.delete(testDir + daveName + ".hdf.zip");

    // test GridSaveAs.main: make the .hdf file from the .grd file
    GridSaveAs.main(new String[] {testDir + ".grd", testDir + ".hdf"});

    // read the hdf file
    Grid grid5 = new Grid();
    grid5.readHDF(testDir + daveName + ".hdf", HdfConstants.DFNT_FLOAT64);
    grid5.makeLonPM180(false);

    // are they the same?
    Test.ensureTrue(grid5.equals(grid3), errorIn);

    // ******* done
    // ??? better: automate by looking at the first 300 bytes of hexDump?
    // but I've re-read the files and checked them above

    // delete the files (or leave them for inspection by hand)
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".asc"),
        errorIn + " while deleting " + testDir + daveName + ".asc");
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".grd"),
        errorIn + " while deleting " + testDir + daveName + ".grd");
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".hdf"),
        errorIn + " while deleting " + testDir + daveName + ".hdf");
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".mat"),
        errorIn + " while deleting " + testDir + daveName + ".mat");
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".nc"),
        errorIn + " while deleting " + testDir + daveName + ".nc");
    Test.ensureTrue(
        File2.delete(testDir + daveName + ".xyz"),
        errorIn + " while deleting " + testDir + daveName + ".xyz");

    // FUTURE: check metadata
  }

  /** This tests the 'subtract' method. */
  @org.junit.jupiter.api.Test
  void testSubtract() {
    String2.log("Grid.testSubtract");
    double mv = Double.NaN;

    Grid grid1 = new Grid();
    grid1.lat = new double[] {0, 1, 2, 3, 4};
    grid1.lon = new double[] {10, 12, 14, 16};
    grid1.latSpacing = 1;
    grid1.lonSpacing = 2;
    // A 1D array, column by column, from the lower left (the way SGT wants it).
    grid1.data =
        new double[] {
          0, 10, 20, 30, 40, // a column's data
          100, mv, 120, 130, 140,
          200, 210, 220, 230, 240,
          300, 310, 320, 330, 340
        };

    // make grid 2 cover a smaller area, and closest values are .1*grid1 values
    Grid grid2 = new Grid();
    grid2.lat = new double[] {1.1, 2.1, 3.1}; // match 1, 2, 3
    grid2.lon = new double[] {12.1, 14.1}; // mateh 12, 14
    grid2.latSpacing = 1;
    grid2.lonSpacing = 2;
    // A 1D array, column by column, from the lower left (the way SGT wants it).
    grid2.data =
        new double[] {
          11, 12, 13, // a column's data
          21, mv, 23
        };

    // subtract grid1 from grid2
    grid1.subtract(grid2);
    Test.ensureEqual(
        grid1.data,
        new double[] {
          mv, mv, mv, mv, mv, // a column's data
          mv, mv, 120 - 12, 130 - 13, mv, mv, 210 - 21, mv, 230 - 23, mv, mv, mv, mv, mv, mv
        },
        "result not as expected: ");
  }

  /** This tests the 'interpolate' method. */
  @org.junit.jupiter.api.Test
  void testInterpolate() {
    String2.log("Grid.testInterpolate");
    double mv = Double.NaN;

    double lat[] = {0, 1, 2, 3, 4};
    double lon[] = {0};
    double latSpacing = 1;
    double lonSpacing = 2;
    double data1[] = {0, 10, mv, 30, 40};
    double data2[] = {0, mv, 20, 40, 80};

    Grid grid1 = new Grid();
    grid1.lat = lat;
    grid1.lon = lon;
    grid1.latSpacing = latSpacing;
    grid1.lonSpacing = lonSpacing;
    grid1.data = data1;

    Grid grid2 = new Grid();
    grid2.lat = lat;
    grid2.lon = lon;
    grid2.latSpacing = latSpacing;
    grid2.lonSpacing = lonSpacing;
    grid2.data = data2;

    grid1.data = data1;
    grid1.interpolate(grid2, 0);
    Test.ensureEqual(grid1.data, new double[] {0, 10, 20, 30, 40}, "");

    grid1.data = data1;
    grid1.interpolate(grid2, .75);
    Test.ensureEqual(grid1.data, new double[] {0, 10, 20, 37.5, 70}, "");

    grid1.data = data1;
    grid1.interpolate(grid2, 1);
    Test.ensureEqual(grid1.data, new double[] {0, 10, 20, 40, 80}, "");
  }

  
  /**
   * This tests saveAsEsriASCII.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testSaveAsEsriASCII() throws Exception {

    String2.log("\n***** Grid.testSaveAsEsriASCII");
    Grid grid = new Grid();
    grid.readGrd(testDir + testName + ".grd", true);
    grid.makeLonPM180(false); // so saveAsEsriASCII will have to change it
    grid.saveAsEsriASCII(testDir, "temp");
    String result[] = File2.readFromFile88591(testDir + "temp.asc");
    Test.ensureEqual(result[0], "", "");
    String reference =
        "ncols 121\n"
            + "nrows 113\n"
            + "xllcenter -135.0\n"
            + "yllcenter 22.0\n"
            + "cellsize 0.25\n"
            + "nodata_value -9999999\n"
            + "4.2154 4.20402 4.39077 4.3403 4.31166 4.06579 4.0913";
    Test.ensureEqual(result[1].substring(0, reference.length()), reference, "");

    File2.delete(testDir + "temp.asc");
  }

  /**
   * This tests saveAsASCII.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testSaveAsASCII() throws Exception {

    // 7/14/05 this file was tested by rich.cosgrove@noaa.gov -- fine, see email
    String2.log("\n***** Grid.testSaveAsASCII");
    Grid grid = new Grid();
    grid.readGrd(testDir + testName + ".grd", true);
    grid.saveAsASCII(testDir, "temp");
    String result[] = File2.readFromFile88591(testDir + "temp.asc");
    Test.ensureEqual(result[0], "", "");
    String reference =
        "ncols 121\n"
            + "nrows 113\n"
            + "xllcenter -135.0\n"
            + "yllcenter 22.0\n"
            + "cellsize 0.25\n"
            + "nodata_value -9999999\n"
            + "4.2154 4.20402 4.39077 4.3403 4.31166 4.06579 4.0913";
    Test.ensureEqual(result[1].substring(0, reference.length()), reference, "");
    File2.delete(testDir + "temp.asc");
  }

  /**
   * Test saveAsXYZ
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testSaveAsXYZ() throws Exception {
    String2.log("\n***** Grid.testSaveAsXYZ");
    Grid grid = new Grid();
    grid.readGrd(testDir + testName + ".grd", true);
    for (int i = 0; i < 3; i++) {
      grid.saveAsXYZ(testDir, "temp");
    }
    String sa[] = File2.readFromFile88591(testDir + "temp.xyz");
    Test.ensureEqual(sa[0], "", "");
    sa[1] = String2.annotatedString(sa[1].substring(0, 300));
    String2.log(sa[1]);
    Test.ensureEqual(
        sa[1], // note it is now bottom up
        "-135[9]22[9]-5.72424[10]\n"
            + "-134.75[9]22[9]-6.14737[10]\n"
            + "-134.5[9]22[9]-6.21717[10]\n"
            + "-134.25[9]22[9]-5.81695[10]\n"
            + "-134[9]22[9]-5.90187[10]\n"
            + "-133.75[9]22[9]-5.80732[10]\n"
            + "-133.5[9]22[9]-5.92826[10]\n"
            + "-133.25[9]22[9]-5.83655[10]\n"
            + "-133[9]22[9]-4.88194[10]\n"
            + "-132.75[9]22[9]-2.69262[10]\n"
            + "-132.5[9]22[9]-2.63237[10]\n"
            + "-132.25[9]22[9]-2.61885[10]\n"
            + "-132[9]22[9]-2.5787[10]\n"
            + "-131.75[9]22[9]-2.56497[10]\n"
            + "-131.5[9]22[9]NaN[10]\n"
            + "-131.25[9]22[9]NaN[10]\n"
            + "-131[9]22[end]",
        "");
    File2.delete(testDir + "temp.xyz");
  }

  /**
   * This tests saveAsMatlab().
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void testSaveAsMatlab() throws Exception {

    // 7/14/05 this file was tested by luke.spence@noaa.gov in Matlab
    String2.log("\n***** Grid.testSaveAsMatlab");
    Grid grid = new Grid();
    grid.readGrd(testDir + testName + ".grd", true);
    grid.saveAsMatlab(testDir, "temp", "ssta");
    String mhd = File2.hexDump(testDir + "temp.mat", 300);
    Test.ensureEqual(
        mhd.substring(0, 71 * 4) + mhd.substring(71 * 7), // remove the creation dateTime
        "4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n"
            + "69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n"
            + "20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n"
            + "6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n"
            + "20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n"
            + "00 00 00 0e 00 00 03 f8   00 00 00 06 00 00 00 08                    |\n"
            + "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 01 00 00 00 79   00 03 00 01 6c 6f 6e 00          y    lon  |\n"
            + "00 00 00 09 00 00 03 c8   c0 60 e0 00 00 00 00 00            `       |\n"
            + "c0 60 d8 00 00 00 00 00   c0 60 d0 00 00 00 00 00    `       `       |\n"
            + "c0 60 c8 00 00 00 00 00   c0 60 c0 00 00 00 00 00    `       `       |\n"
            + "c0 60 b8 00 00 00 00 00   c0 60 b0 00 00 00 00 00    `       `       |\n"
            + "c0 60 a8 00 00 00 00 00   c0 60 a0 00 00 00 00 00    `       `       |\n"
            + "c0 60 98 00 00 00 00 00   c0 60 90 00 00 00 00 00    `       `       |\n"
            + "c0 60 88 00 00 00 00 00   c0 60 80 00 00 00 00 00    `       `       |\n"
            + "c0 60 78 00 00 00 00 00   c0 60 70 00  `x      `p                    |\n",
        "File2.hexDump(testDir + \"temp.mat\", 300)");
    File2.delete(testDir + "temp.mat");
  }

  /** This reads a grid and save it as geotiff. */
  @org.junit.jupiter.api.Test
  void testSaveAsGeotiff() throws Exception {
    String2.log("\n***** Grid.testSaveAsGeotiff");
    FileNameUtility fileNameUtility = new FileNameUtility("gov.noaa.pfel.coastwatch.TimePeriods");
    Grid grid = new Grid();
    grid.readGrd(
        testDir + testName + ".grd", -180, 180, 22, 50, Integer.MAX_VALUE, Integer.MAX_VALUE);
    grid.setAttributes(testName, fileNameUtility, false); // data saved not as doubles
    grid.saveAsGeotiff(testDir, testName, "ATssta");
    // do some test of it?
    File2.delete(testDir + testName + ".tif");
  }

  /**
   * This is a test of readNetCDF and saveAsNetCDF.
   *
   * @param fileNameUtility is used to generate all of the metadata based on the file name
   * @throws Exception of trouble
   */
  @org.junit.jupiter.api.Test
  void testNetCDF() throws Exception {
    String2.log("\n*** Grid.testNetCDF");
    FileNameUtility fileNameUtility = new FileNameUtility("gov.noaa.pfel.coastwatch.TimePeriods");
    // ***** test composite *******************************************************

    Grid grid1 = new Grid();
    grid1.latSpacing = 0.5;
    grid1.lonSpacing = 0.25;
    grid1.minData = 1;
    grid1.maxData = 12;
    grid1.nValidPoints = Integer.MIN_VALUE;
    grid1.lat = new double[] {22, 22.5, 23};
    grid1.lon = new double[] {-135, -134.75, -134.5, -134.25};
    grid1.data = new double[] {1, 2, 3, 4, Double.NaN, 6, 7, 8, 9, 10, 11, 12};

    // add the metadata attributes
    String fileName =
        "LATsstaS1day_20030304120000_x-135_X-134.25_y22_Y23."; // a CWBrowser compatible name
    grid1.setAttributes(fileName, fileNameUtility, false);

    // save the file
    grid1.saveAsNetCDF(testDir, fileName + "Test", "ATssta");

    // read the file
    Grid grid2 = new Grid();
    grid2.readNetCDF(testDir + fileName + "Test.nc", "ATssta", true);
    // String2.log("\ngrid.readNetCDF read: " + grid2.toString(false));

    // see if it has the expected values
    int nLat = grid2.lat.length;
    int nLon = grid2.lon.length;
    Test.ensureTrue(grid1.equals(grid2), "TestNetCDF"); // tests lat, lon, data
    Test.ensureEqual(
        grid2.globalAttributes().get("Conventions"),
        new StringArray(new String[] {"COARDS, CF-1.6, ACDD-1.3, CWHDF"}),
        "Conventions");
    Test.ensureEqual(
        grid2.globalAttributes().get("title"),
        new StringArray(
            new String[] {"SST, NOAA POES AVHRR, LAC, 0.0125 degrees, West US, Day and Night"}),
        "title");
    Test.ensureEqual(
        grid2.globalAttributes().get("summary"),
        new StringArray(
            new String[] {
              "NOAA CoastWatch provides sea surface temperature (SST) products derived from NOAA's Polar Operational Environmental Satellites (POES).  This data is provided at high resolution (0.0125 degrees) for the North Pacific Ocean.  Measurements are gathered by the Advanced Very High Resolution Radiometer (AVHRR) instrument, a multiband radiance sensor carried aboard the NOAA POES satellites."
            }),
        "summary");
    Test.ensureEqual(
        grid2.globalAttributes().get("keywords"),
        new StringArray(
            new String[] {"EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature"}),
        "keywords");
    Test.ensureEqual(
        grid2.globalAttributes().get("id"), new StringArray(new String[] {"LATsstaS1day"}), "id");
    Test.ensureEqual(
        grid2.globalAttributes().get("naming_authority"),
        new StringArray(new String[] {"gov.noaa.pfeg.coastwatch"}),
        "naming_authority");
    Test.ensureEqual(
        grid2.globalAttributes().get("keywords_vocabulary"),
        new StringArray(new String[] {"GCMD Science Keywords"}),
        "keywords_vocabulary");
    Test.ensureEqual(
        grid2.globalAttributes().get("cdm_data_type"),
        new StringArray(new String[] {"Grid"}),
        "cdm_data_typ");
    String history = grid2.globalAttributes().getString("history");
    String2.log("history=" + history);
    Test.ensureTrue(history.startsWith("NOAA NWS Monterey and NOAA CoastWatch\n20"), "getHistory");
    Test.ensureTrue(
        history.endsWith("NOAA CoastWatch (West Coast Node) and NOAA SWFSC ERD"), "getHistory");
    Test.ensureEqual(
        grid2.globalAttributes().get("date_created"),
        new StringArray(new String[] {Calendar2.formatAsISODate(Calendar2.newGCalendarZulu())}),
        "date_created");
    Test.ensureEqual(
        grid2.globalAttributes().get("creator_name"),
        new StringArray(new String[] {"NOAA CoastWatch, West Coast Node"}),
        "creator_name");
    Test.ensureEqual(
        grid2.globalAttributes().get("creator_url"),
        new StringArray(new String[] {"https://coastwatch.pfeg.noaa.gov"}),
        "creator_url");
    Test.ensureEqual(
        grid2.globalAttributes().get("creator_email"),
        new StringArray(new String[] {"erd.data@noaa.gov"}),
        "creator_email");
    Test.ensureEqual(
        grid2.globalAttributes().get("institution"),
        new StringArray(new String[] {"NOAA CoastWatch, West Coast Node"}),
        "institution");
    Test.ensureEqual(
        grid2.globalAttributes().get("project"),
        new StringArray(new String[] {"CoastWatch (https://coastwatch.noaa.gov/)"}),
        "project");
    Test.ensureEqual(
        grid2.globalAttributes().get("processing_level"),
        new StringArray(new String[] {"3 (projected)"}),
        "processing_level");
    Test.ensureEqual(
        grid2.globalAttributes().get("acknowledgement"),
        new StringArray(new String[] {"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD"}),
        "acknowledgement");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lat_min"),
        new DoubleArray(new double[] {22}),
        "geospatial_lat_min");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lat_max"),
        new DoubleArray(new double[] {23}),
        "geospatial_lat_max");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lon_min"),
        new DoubleArray(new double[] {-135}),
        "geospatial_lon_min");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lon_max"),
        new DoubleArray(new double[] {-134.25}),
        "geospatial_lon_max");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lat_units"),
        new StringArray(new String[] {"degrees_north"}),
        "geospatial_lat_units");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lat_resolution"),
        new DoubleArray(new double[] {0.5}),
        "geospatial_lat_resolution");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lon_units"),
        new StringArray(new String[] {"degrees_east"}),
        "geospatial_lon_units");
    Test.ensureEqual(
        grid2.globalAttributes().get("geospatial_lon_resolution"),
        new DoubleArray(new double[] {0.25}),
        "geospatial_lon_resolution");
    Test.ensureEqual(
        grid2.globalAttributes().get("time_coverage_start"),
        new StringArray(new String[] {"2003-03-04T00:00:00Z"}),
        "time_coverage_start");
    Test.ensureEqual(
        grid2.globalAttributes().get("time_coverage_end"),
        new StringArray(new String[] {"2003-03-05T00:00:00Z"}),
        "time_coverage_end");
    // Test.ensureEqual(grid2.globalAttributes().get("time_coverage_resolution", new
    // StringArray(new String[]{""}), "time_coverage_resolution");
    Test.ensureEqual(
        grid2.globalAttributes().get("standard_name_vocabulary"),
        new StringArray(new String[] {"CF Standard Name Table v70"}),
        "standard_name_vocabulary");
    Test.ensureEqual(
        grid2.globalAttributes().get("license"),
        new StringArray(
            new String[] {
              "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information."
            }),
        "license");
    Test.ensureEqual(
        grid2.globalAttributes().get("contributor_name"),
        new StringArray(new String[] {"NOAA NWS Monterey and NOAA CoastWatch"}),
        "contributor_name");
    Test.ensureEqual(
        grid2.globalAttributes().get("contributor_role"),
        new StringArray(new String[] {"Source of level 2 data."}),
        "contributor_role");
    Test.ensureEqual(
        grid2.globalAttributes().get("date_issued"),
        new StringArray(new String[] {Calendar2.formatAsISODate(Calendar2.newGCalendarZulu())}),
        "date_issued");
    Test.ensureEqual(
        grid2.globalAttributes().getString("references"),
        "NOAA POES satellites information: https://coastwatch.noaa.gov/poes_sst_overview.html . Processing information: https://www.ospo.noaa.gov/Operations/POES/index.html . Processing reference: Walton C. C., W. G. Pichel, J. F. Sapper, D. A. May. The development and operational application of nonlinear algorithms for the measurement of sea surface temperatures with the NOAA polar-orbiting environmental satellites. J.G.R., 103: (C12) 27999-28012, 1998. Cloudmask reference: Stowe, L. L., P. A. Davis, and E. P. McClain.  Scientific basis and initial evaluation of the CLAVR-1 global clear/cloud classification algorithm for the advanced very high resolution radiometer. J. Atmos. Oceanic Technol., 16, 656-681. 1999. Calibration and Validation: Li, X., W. Pichel, E. Maturi, P. Clemente-Colon, and J. Sapper. Deriving the operational nonlinear multi-channel sea surface temperature algorithm coefficients for NOAA-15 AVHRR/3. International Journal of Remote Sensing, Volume 22, No. 4, 699 - 704, March 2001a. Calibration and Validation: Li, X, W. Pichel, P. Clemente-Colon, V. Krasnopolsky, and J. Sapper. Validation of coastal sea and lake surface temperature measurements derived from NOAA/AVHRR Data. International Journal of Remote Sensing, Vol. 22, No. 7, 1285-1303, 2001b.",
        "references");
    Test.ensureEqual(
        grid2.globalAttributes().getString("source"),
        "satellite observation: POES, AVHRR HRPT",
        "source");
    // Google Earth
    Test.ensureEqual(
        grid2.globalAttributes().get("Southernmost_Northing"),
        new DoubleArray(new double[] {22}),
        "southernmost");
    Test.ensureEqual(
        grid2.globalAttributes().get("Northernmost_Northing"),
        new DoubleArray(new double[] {23}),
        "northernmost");
    Test.ensureEqual(
        grid2.globalAttributes().get("Westernmost_Easting"),
        new DoubleArray(new double[] {-135}),
        "westernmost");
    Test.ensureEqual(
        grid2.globalAttributes().get("Easternmost_Easting"),
        new DoubleArray(new double[] {-134.25}),
        "easternmost");

    // cwhdf attributes
    Test.ensureEqual(
        grid2.globalAttributes().get("cwhdf_version"),
        new StringArray(new String[] {"3.4"}),
        "cwhdf_version"); // string
    Test.ensureEqual(
        grid2.globalAttributes().get("satellite"),
        new StringArray(new String[] {"POES"}),
        "satellite"); // string
    Test.ensureEqual(
        grid2.globalAttributes().get("sensor"),
        new StringArray(new String[] {"AVHRR HRPT"}),
        "sensor"); // string
    Test.ensureEqual(
        grid2.globalAttributes().get("composite"),
        new StringArray(new String[] {"true"}),
        "composite"); // string

    Test.ensureEqual(
        grid2.globalAttributes().get("pass_date"),
        new IntArray(new int[] {12115}),
        "pass_date"); // int32[nDays]
    Test.ensureEqual(
        grid2.globalAttributes().get("start_time"),
        new DoubleArray(new double[] {0}),
        "start_time"); // float64[nDays]
    Test.ensureEqual(
        grid2.globalAttributes().get("origin"),
        new StringArray(new String[] {"NOAA NWS Monterey and NOAA CoastWatch"}),
        "origin"); // string
    // Test.ensureEqual(grid2.globalAttributes().get("history"), new StringArray(new
    // String[]{"unknown"}), "history"); //string

    Test.ensureEqual(
        grid2.globalAttributes().get("projection_type"),
        new StringArray(new String[] {"mapped"}),
        "projection_type"); // string
    Test.ensureEqual(
        grid2.globalAttributes().get("projection"),
        new StringArray(new String[] {"geographic"}),
        "projection"); // string
    Test.ensureEqual(
        grid2.globalAttributes().get("gctp_sys"), new IntArray(new int[] {0}), "gctp_sys"); // int32
    Test.ensureEqual(
        grid2.globalAttributes().get("gctp_zone"),
        new IntArray(new int[] {0}),
        "gctp_zone"); // int32
    Test.ensureEqual(
        grid2.globalAttributes().get("gctp_parm"),
        new DoubleArray(new double[15]),
        "gctp_parm"); // float64[15
    // 0's]
    Test.ensureEqual(
        grid2.globalAttributes().get("gctp_datum"),
        new IntArray(new int[] {12}),
        "gctp_datum"); // int32
    // 12=WGS84

    Test.ensureEqual(
        grid2.globalAttributes().get("et_affine"),
        new DoubleArray(
            new double[] {0, grid1.latSpacing, grid1.lonSpacing, 0, grid1.lon[0], grid1.lat[0]}),
        "et_affine"); // right side up

    Test.ensureEqual(
        grid2.globalAttributes().get("rows"),
        new IntArray(new int[] {grid1.lat.length}),
        "rows"); // int32
    // number
    // of
    // rows
    Test.ensureEqual(
        grid2.globalAttributes().get("cols"),
        new IntArray(new int[] {grid1.lon.length}),
        "cols"); // int32
    // number
    // of
    // columns
    Test.ensureEqual(
        grid2.globalAttributes().get("polygon_latitude"),
        new DoubleArray(
            new double[] {
              grid1.lat[0], grid1.lat[nLat - 1], grid1.lat[nLat - 1], grid1.lat[0], grid1.lat[0]
            }),
        "polygon_latitude");
    Test.ensureEqual(
        grid2.globalAttributes().get("polygon_longitude"),
        new DoubleArray(
            new double[] {
              grid1.lon[0], grid1.lon[0], grid1.lon[nLon - 1], grid1.lon[nLon - 1], grid1.lon[0]
            }),
        "polygon_longitude");

    // lat attributes
    Test.ensureEqual(
        grid2.latAttributes().get("long_name"),
        new StringArray(new String[] {"Latitude"}),
        "lat long_name");
    Test.ensureEqual(
        grid2.latAttributes().get("standard_name"),
        new StringArray(new String[] {"latitude"}),
        "lat standard_name");
    Test.ensureEqual(
        grid2.latAttributes().get("units"),
        new StringArray(new String[] {"degrees_north"}),
        "lat units");
    Test.ensureEqual(
        grid2.latAttributes().get("point_spacing"),
        new StringArray(new String[] {"even"}),
        "lat point_spacing");
    Test.ensureEqual(
        grid2.latAttributes().get("actual_range"),
        new DoubleArray(new double[] {22, 23}),
        "lat actual_range");

    // CWHDF metadata/attributes for Latitude
    Test.ensureEqual(
        grid2.latAttributes().get("coordsys"),
        new StringArray(new String[] {"geographic"}),
        "coordsys"); // string
    Test.ensureEqual(
        grid2.latAttributes().get("fraction_digits"),
        new IntArray(new int[] {4}),
        "fraction_digits"); // int32

    // lon attributes
    Test.ensureEqual(
        grid2.lonAttributes().get("long_name"),
        new StringArray(new String[] {"Longitude"}),
        "lon long_name");
    Test.ensureEqual(
        grid2.lonAttributes().get("standard_name"),
        new StringArray(new String[] {"longitude"}),
        "lon standard_name");
    Test.ensureEqual(
        grid2.lonAttributes().get("units"),
        new StringArray(new String[] {"degrees_east"}),
        "lon units");
    Test.ensureEqual(
        grid2.lonAttributes().get("point_spacing"),
        new StringArray(new String[] {"even"}),
        "lon point_spacing");
    Test.ensureEqual(
        grid2.lonAttributes().get("actual_range"),
        new DoubleArray(new double[] {-135, -134.25}),
        "lon actual_range");

    // CWHDF metadata/attributes for Longitude
    Test.ensureEqual(
        grid2.lonAttributes().get("coordsys"),
        new StringArray(new String[] {"geographic"}),
        "coordsys"); // string
    Test.ensureEqual(
        grid2.lonAttributes().get("fraction_digits"),
        new IntArray(new int[] {4}),
        "fraction_digits"); // int32

    // data attributes
    Test.ensureEqual(
        grid2.dataAttributes().get("long_name"),
        new StringArray(
            new String[] {"SST, NOAA POES AVHRR, LAC, 0.0125 degrees, West US, Day and Night"}),
        "data long_name");
    Test.ensureEqual(
        grid2.dataAttributes().get("standard_name"),
        new StringArray(new String[] {"sea_surface_temperature"}),
        "data standard_name");
    Test.ensureEqual(
        grid2.dataAttributes().get("units"),
        new StringArray(new String[] {"degree_C"}),
        "data units");
    Test.ensureEqual(
        grid2.dataAttributes().get("_FillValue"),
        new FloatArray(new float[] {-9999999}),
        "data _FillValue");
    Test.ensureEqual(
        grid2.dataAttributes().get("missing_value"),
        new FloatArray(new float[] {-9999999}),
        "data missing_value");
    Test.ensureEqual(
        grid2.dataAttributes().get("numberOfObservations"),
        new IntArray(new int[] {11}),
        "data numberOfObservations");
    Test.ensureEqual(
        grid2.dataAttributes().get("percentCoverage"),
        new DoubleArray(new double[] {0.9166666666666666}),
        "data percentCoverage");

    // CWHDF metadata/attributes for the data: varName
    Test.ensureEqual(
        grid2.dataAttributes().get("coordsys"),
        new StringArray(new String[] {"geographic"}),
        "coordsys"); // string
    Test.ensureEqual(
        grid2.dataAttributes().get("fraction_digits"),
        new IntArray(new int[] {1}),
        "fraction_digits"); // int32

    // test that it has correct centeredTime value
    NetcdfFile netcdfFile = NcHelper.openFile(testDir + fileName + "Test.nc");
    try {
      Variable timeVariable = NcHelper.findVariable(netcdfFile, "time");
      PrimitiveArray pa = NcHelper.getPrimitiveArray(timeVariable);
      Test.ensureEqual(pa.size(), 1, "");
      Test.ensureEqual(
          pa.getDouble(0), Calendar2.isoStringToEpochSeconds("2003-03-04T12:00:00"), "");
    } finally {
      try {
        if (netcdfFile != null) netcdfFile.close();
      } catch (Exception e9) {
      }
    }

    // delete the file
    File2.delete(testDir + fileName + "Test.nc");

    // ****** test hday *****************************************************
    // add the metadata attributes
    fileName = "LATsstaSpass_20030304170000_x-135_X-134.25_y22_Y23."; // a CWBrowser compatible name
    grid1.setAttributes(fileName, fileNameUtility, false);

    // save the file
    grid1.saveAsNetCDF(testDir, fileName + "Test", "ATssta");

    // read the file
    grid2.clear(); // extra insurance
    grid2.readNetCDF(testDir + fileName + "Test.nc", "ATssta", true);
    // String2.log("\ngrid.readNetCDF read: " + grid2.toString(false));

    // see if it has the expected values
    nLat = grid2.lat.length;
    nLon = grid2.lon.length;
    Test.ensureTrue(grid1.equals(grid2), "TestNetCDF"); // tests lat, lon, data
    Test.ensureEqual(
        grid2.globalAttributes().get("date_created"),
        new StringArray(new String[] {Calendar2.formatAsISODate(Calendar2.newGCalendarZulu())}),
        "date_created");
    Test.ensureEqual(
        grid2.globalAttributes().get("time_coverage_start"),
        new StringArray(new String[] {"2003-03-04T17:00:00Z"}),
        "time_coverage_start");
    Test.ensureEqual(
        grid2.globalAttributes().get("time_coverage_end"),
        new StringArray(new String[] {"2003-03-04T17:00:00Z"}),
        "time_coverage_end");
    // Test.ensureEqual(grid2.globalAttributes().get("time_coverage_resolution", new
    // StringArray(new String[]{""}), "time_coverage_resolution");
    Test.ensureEqual(
        grid2.globalAttributes().get("date_issued"),
        new StringArray(new String[] {Calendar2.formatAsISODate(Calendar2.newGCalendarZulu())}),
        "date_issued");

    Test.ensureEqual(
        grid2.globalAttributes().get("pass_date"),
        new IntArray(new int[] {12115}),
        "pass_date"); // int32[nDays]
    Test.ensureEqual(
        grid2.globalAttributes().get("start_time"),
        new DoubleArray(new double[] {17 * Calendar2.SECONDS_PER_HOUR}),
        "start_time"); // float64[nDays]

    // test that it has correct centeredTime value
    netcdfFile = NcHelper.openFile(testDir + fileName + "Test.nc");
    try {
      Variable timeVariable = NcHelper.findVariable(netcdfFile, "time");
      PrimitiveArray pa = NcHelper.getPrimitiveArray(timeVariable);
      Test.ensureEqual(pa.size(), 1, "");
      Test.ensureEqual(
          pa.getDouble(0), Calendar2.isoStringToEpochSeconds("2003-03-04T17:00:00"), "");
    } finally {
      try {
        if (netcdfFile != null) netcdfFile.close();
      } catch (Exception e9) {
      }
    }

    // delete the file
    File2.delete(testDir + fileName + "Test.nc");

    // ****** test climatology *****************************************************
    // add the metadata attributes
    fileName = "LATsstaS1day_00010304120000_x-135_X-134.25_y22_Y23."; // a CWBrowser compatible name
    grid1.setAttributes(fileName, fileNameUtility, false);

    // save the file
    grid1.saveAsNetCDF(testDir, fileName + "Test", "ATssta");

    // read the file
    grid2.clear(); // extra insurance
    grid2.readNetCDF(testDir + fileName + "Test.nc", "ATssta", true);
    // String2.log("\ngrid.readNetCDF read: " + grid2.toString(false));

    // see if it has the expected values
    nLat = grid2.lat.length;
    nLon = grid2.lon.length;
    Test.ensureTrue(grid1.equals(grid2), "TestNetCDF"); // tests lat, lon, data
    Test.ensureEqual(
        grid2.globalAttributes().get("date_created"),
        new StringArray(new String[] {Calendar2.formatAsISODate(Calendar2.newGCalendarZulu())}),
        "date_created");
    Test.ensureEqual(
        grid2.globalAttributes().get("time_coverage_start"),
        new StringArray(new String[] {"0001-03-04T00:00:00Z"}),
        "time_coverage_start");
    Test.ensureEqual(
        grid2.globalAttributes().get("time_coverage_end"),
        new StringArray(new String[] {"0001-03-05T00:00:00Z"}),
        "time_coverage_end");
    // Test.ensureEqual(grid2.globalAttributes().get("time_coverage_resolution", new
    // StringArray(new String[]{""}), "time_coverage_resolution");
    Test.ensureEqual(
        grid2.globalAttributes().get("date_issued"),
        new StringArray(new String[] {Calendar2.formatAsISODate(Calendar2.newGCalendarZulu())}),
        "date_issued");

    Test.ensureEqual(
        grid2.globalAttributes().get("pass_date"),
        new IntArray(new int[] {-719101}),
        "pass_date"); // int32[nDays]
    Test.ensureEqual(
        grid2.globalAttributes().get("start_time"),
        new DoubleArray(new double[] {0}),
        "start_time"); // float64[nDays]

    // test that it has correct centeredTime value
    netcdfFile = NcHelper.openFile(testDir + fileName + "Test.nc");
    try {
      Variable timeVariable = NcHelper.findVariable(netcdfFile, "time");
      PrimitiveArray pa = NcHelper.getPrimitiveArray(timeVariable);
      Test.ensureEqual(pa.size(), 1, "");
      Test.ensureEqual(
          pa.getDouble(0), Calendar2.isoStringToEpochSeconds("0001-03-04T12:00:00"), "");
    } finally {
      try {
        if (netcdfFile != null) netcdfFile.close();
      } catch (Exception e9) {
      }
    }

    // delete the file
    File2.delete(testDir + fileName + "Test.nc");
  }
}
