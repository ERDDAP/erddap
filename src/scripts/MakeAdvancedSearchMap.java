/**
 * This is likely dead code from SgtMap. The largest chunk is the code used to make the advanced search map,
 * hence the name.
 */
public class MakeAdvancedSearchMap {

    /**
     * This sets globalAttributes, latAttributes, lonAttributes, and dataAttributes
     * for bathymetry
     * data so that the attributes have COARDS, CF, THREDDS ACDD, and
     * CWHDF-compliant metadata
     * attributes. This also calls calculateStats, so that information will be
     * up-to-date. See
     * MetaMetadata.txt for more information.
     *
     * @param grid
     * @param saveMVAsDouble If true, _FillValue and missing_value are saved as
     *                       doubles, else floats.
     */
    public static void setBathymetryAttributes(Grid grid, boolean saveMVAsDouble) throws Exception {
        // should this clear existing attributes?

        // aliases
        double lat[] = grid.lat;
        double lon[] = grid.lon;
        Attributes globalAttributes = grid.globalAttributes();
        Attributes lonAttributes = grid.lonAttributes();
        Attributes latAttributes = grid.latAttributes();
        Attributes dataAttributes = grid.dataAttributes();

        // calculateStats
        grid.calculateStats();

        // assemble the global metadata attributes
        int nLat = lat.length;
        int nLon = lon.length;
        globalAttributes.set("Conventions", FileNameUtility.getConventions());
        globalAttributes.set("title", BATHYMETRY_BOLD_TITLE);
        globalAttributes.set("summary", BATHYMETRY_SUMMARY);
        globalAttributes.set("keywords", "Oceans > Bathymetry/Seafloor Topography > Bathymetry");
        globalAttributes.set("id", "ETOPO"); // 2019-05-07 was "SampledFrom" + etopoFileName);
        globalAttributes.set("naming_authority", FileNameUtility.getNamingAuthority());
        globalAttributes.set("keywords_vocabulary", FileNameUtility.getKeywordsVocabulary());
        globalAttributes.set("cdm_data_type", FileNameUtility.getCDMDataType());
        globalAttributes.set("date_created", FileNameUtility.getDateCreated());
        globalAttributes.set("creator_name", FileNameUtility.getCreatorName());
        globalAttributes.set("creator_url", FileNameUtility.getCreatorURL());
        globalAttributes.set("creator_email", FileNameUtility.getCreatorEmail());
        globalAttributes.set("institution", BATHYMETRY_COURTESY);
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
        globalAttributes.set("geospatial_lat_resolution", Math.abs(grid.latSpacing));
        globalAttributes.set("geospatial_lon_units", FileNameUtility.getLonUnits());
        globalAttributes.set("geospatial_lon_resolution", Math.abs(grid.lonSpacing));
        // globalAttributes.set("time_coverage_start",
        // Calendar2.formatAsISODateTimeTZ(FileNameUtility.getStartCalendar(name)));
        // globalAttributes.set("time_coverage_end",
        // Calendar2.formatAsISODateTimeTZ(FileNameUtility.getEndCalendar(name)));
        // globalAttributes.set("time_coverage_resolution", "P12H"));
        globalAttributes.set("standard_name_vocabulary", FileNameUtility.getStandardNameVocabulary());
        globalAttributes.set("license", FileNameUtility.getLicense());
        globalAttributes.set("contributor_name", BATHYMETRY_COURTESY);
        globalAttributes.set("contributor_role", "Source of level 3 data.");
        globalAttributes.set("date_issued", FileNameUtility.getDateCreated());
        globalAttributes.set("references", BATHYMETRY_CITE);
        globalAttributes.set("source", BATHYMETRY_SOURCE_URL);
        // attributes for Google Earth
        globalAttributes.set("Southernmost_Northing", Math.min(lat[0], lat[nLat - 1]));
        globalAttributes.set("Northernmost_Northing", Math.max(lat[0], lat[nLat - 1]));
        globalAttributes.set("Westernmost_Easting", Math.min(lon[0], lon[nLon - 1]));
        globalAttributes.set("Easternmost_Easting", Math.max(lon[0], lon[nLon - 1]));

        // globalAttributes for HDF files using CoastWatch Metadata Specifications
        // required unless noted otherwise
        globalAttributes.set("cwhdf_version", "3.4"); // string
        // String satellite = fileNameUtility.getSatellite(name);
        // if (satellite.length() > 0) {
        // globalAttributes.set("satellite", fileNameUtility.getSatellite(name));
        // //string
        // globalAttributes.set("sensor", fileNameUtility.getSensor(name)); //string
        // } else {
        globalAttributes.set("data_source", BATHYMETRY_COURTESY); // string
        // }
        // globalAttributes.set("composite", FileNameUtility.getComposite(name));
        // //string (not
        // required)

        // globalAttributes.set("pass_date", new
        // IntArray(fileNameUtility.getPassDate(name)));
        // //int32[nDays]
        // globalAttributes.set("start_time", new
        // DoubleArray(fileNameUtility.getStartTime(name))); //float64[nDays]
        globalAttributes.set("origin", BATHYMETRY_COURTESY); // string
        globalAttributes.set("history", DataHelper.addBrowserToHistory(BATHYMETRY_COURTESY)); // string

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
        double matrix[] = {
                0, -grid.latSpacing, grid.lonSpacing, 0, lon[0], lat[lat.length - 1]
        }; // up side down
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
        latAttributes.set("actual_range", new DoubleArray(new double[] { lat[0], lat[nLat - 1] }));

        // CWHDF metadata attributes for Latitude
        // latAttributes.set("long_name", "Latitude")); //string
        // latAttributes.set("units", fileNameUtility.getLatUnits(name))); //string
        latAttributes.set("coordsys", "geographic"); // string
        latAttributes.set("fraction_digits", 6); // int32 because often .033333

        // COARDS, CF, ACDD metadata attributes for longitude
        lonAttributes.set("_CoordinateAxisType", "Lon");
        lonAttributes.set("long_name", "Longitude");
        lonAttributes.set("standard_name", "longitude");
        lonAttributes.set("units", FileNameUtility.getLonUnits());

        // Lynn's metadata attributes
        lonAttributes.set("point_spacing", "even");
        lonAttributes.set("actual_range", new DoubleArray(new double[] { lon[0], lon[nLon - 1] }));

        // CWHDF metadata attributes for Longitude
        // lonAttributes.set("long_name", "Longitude"); //string
        // lonAttributes.set("units", fileNameUtility.getLonUnits(name)); //string
        lonAttributes.set("coordsys", "geographic"); // string
        lonAttributes.set("fraction_digits", 6); // int32

        // COARDS, CF, ACDD metadata attributes for data
        dataAttributes.set("long_name", BATHYMETRY_BOLD_TITLE);
        dataAttributes.set("standard_name", BATHYMETRY_STANDARD_NAME);
        dataAttributes.set("units", BATHYMETRY_UNITS);
        PrimitiveArray mvAr;
        PrimitiveArray rangeAr;
        if (saveMVAsDouble) {
            mvAr = new DoubleArray(new double[] { (double) DataHelper.FAKE_MISSING_VALUE });
            rangeAr = new DoubleArray(new double[] { grid.minData, grid.maxData });
        } else {
            mvAr = new FloatArray(new float[] { (float) DataHelper.FAKE_MISSING_VALUE });
            rangeAr = new FloatArray(new float[] { (float) grid.minData, (float) grid.maxData });
        }
        dataAttributes.set("_FillValue", mvAr); // must be same type as data
        dataAttributes.set("missing_value", mvAr); // must be same type as data
        dataAttributes.set("numberOfObservations", grid.nValidPoints);
        dataAttributes.set("actual_range", rangeAr);

        // CWHDF metadata attributes for the data: varName
        // dataAttributes.set("long_name", fileNameUtility.getTitle(name))); //string
        // dataAttributes.set("units", fileNameUtility.getUDUnits(name))); //string
        dataAttributes.set("coordsys", "geographic"); // string
        dataAttributes.set("fraction_digits", 0); // int32 bathymetry is to nearest meter
    }

    /**
     * Make a matlab file with the specified topography grid. This is a custom
     * method to help Luke.
     *
     * @param spacing e.g., 0.25 degrees
     */
    public static void createTopographyMatlabFile(
            double minX, double maxX, double minY, double maxY, double spacing, String dir)
            throws Exception {
        String name = "Bathymetry_W"
                + String2.genEFormat10(minX)
                + "_E"
                + String2.genEFormat10(maxX)
                + "_S"
                + String2.genEFormat10(minY)
                + "_N"
                + String2.genEFormat10(maxY)
                + "_R"
                + String2.genEFormat10(spacing);
        if (verbose)
            String2.log("SgtMap.createTopographMatlabFile: " + dir + name + ".mat");
        double graphWidth = (maxX - minX) / spacing;
        double graphHeight = (maxY - minY) / spacing;
        int graphWidthPixels = Math2.roundToInt(graphWidth);
        int graphHeightPixels = Math2.roundToInt(graphHeight);
        Test.ensureEqual(
                graphWidth, graphWidthPixels, name + ": graphWidth=" + graphWidth + " isn't an integer.");
        Test.ensureEqual(
                graphHeight,
                graphHeightPixels,
                name + ": graphHeight=" + graphHeight + " isn't an integer.");
        Grid grid = createTopographyGrid(
                null, minX, maxX, minY, maxY, graphWidthPixels + 1, graphHeightPixels + 1);
        grid.saveAsMatlab(dir, name, "Elevation");
    }

    public static void makeAdvSearchMapBig() throws Exception {
        // 2019-10-17 was makeAdvSearchMap(303, 285);
        makeAdvSearchMap(572, 350);
    }

    public static void makeAdvSearchMap() throws Exception {
        makeAdvSearchMap(255, 190);
    }

    /**
     * This was used one time to make a +-180 and a 0-360 map for ERDDAP's Advanced
     * Search. To make
     * -180 to 360, combine them in CoPlot.
     *
     * @param w image width in pixels (the true width)
     * @param h image height in pixels (make it too big, so room for legend)
     */
    public static void makeAdvSearchMap(int w, int h) throws Exception {
        double fontScale = 1; // was 0.9

        landMaskStrokeColor = new Color(0, 0, 0, 0);

        String cptName = CompoundColorMap.makeCPT(
                File2.getWebInfParentDirectory()
                        + // with / separator and / at the end
                        "WEB-INF/cptfiles/",
                "Topography",
                "Linear",
                -8000,
                8000,
                -1,
                true, // continuous,
                SSR.getTempDirectory());

        Grid grid = createTopographyGrid(SSR.getTempDirectory(), -180, 180, -90, 90, w - 15, (w - 15) / 2);
        BufferedImage image = SgtUtil.getBufferedImage(w, h);
        makeMap(
                SgtUtil.LEGEND_BELOW,
                "",
                "",
                "",
                "",
                -180,
                180,
                -90,
                90,
                "under",
                true,
                grid,
                1,
                1,
                0,
                cptName,
                "",
                "",
                "",
                "",
                NO_LAKES_AND_RIVERS,
                (Graphics2D) image.getGraphics(),
                0,
                0,
                w,
                h,
                0,
                fontScale);
        String fileName = "C:/data/AdvancedSearch/tWorldPm180.png";
        SgtUtil.saveImage(image, fileName);
        Test.displayInBrowser("file://" + fileName);

        grid = createTopographyGrid(SSR.getTempDirectory(), 0, 360, -90, 90, w - 15, (w - 15) / 2);
        image = SgtUtil.getBufferedImage(w, h);
        makeMap(
                SgtUtil.LEGEND_BELOW,
                "",
                "",
                "",
                "",
                0,
                360,
                -90,
                90,
                "under",
                true,
                grid,
                1,
                1,
                0,
                cptName,
                "",
                "",
                "",
                "",
                NO_LAKES_AND_RIVERS,
                (Graphics2D) image.getGraphics(),
                0,
                0,
                w,
                h,
                0,
                fontScale);
        fileName = "C:/data/AdvancedSearch/tWorld0360.png";
        SgtUtil.saveImage(image, fileName);
        Test.displayInBrowser("file://" + fileName);
    }

    /**
     * This makes a map of a .nc, .grd, ... gridded data file. The file can be
     * zipped, but the name of
     * the .zip file must be the name of the data file + ".zip".
     *
     * @param args args[0] must be the name of the input file.
     * @throws Exception if trouble and does an ncdump of the file
     */
    public static void main(String args[]) throws Exception {
        verbose = true;
        reallyVerbose = true;
        Grid.verbose = true;

        try {
            // set a bunch of "constants"
            long time = System.currentTimeMillis();
            // args = new String[1];
            // args[0] = "c:/temp/AG2006001_2006001_ssta_westus.grd.zip";
            String tempDir = SSR.getTempDirectory();
            int imageWidth = 480;
            int imageHeight = 640;
            int legendPosition = SgtUtil.LEGEND_BELOW;

            // get the grid file name
            if (args == null || args.length < 1) {
                String2.log("  args[0] must be the name of a grid data file.");
                Math2.sleep(5000);
                System.exit(1);
            }
            String args0 = args[0];
            String gridDir = File2.getDirectory(args0);
            String gridName = File2.getNameAndExtension(args0);
            String gridExt = File2.getExtension(args0).toLowerCase();

            // if zipped, unzip it
            if (gridExt.equals(".zip")) {
                SSR.unzipRename(
                        gridDir, gridName, tempDir, gridName.substring(0, gridName.length() - 4), 10);
                gridDir = tempDir;
                gridName = gridName.substring(0, gridName.length() - 4);
                gridExt = File2.getExtension(gridName).toLowerCase();
            }

            // read the data
            Grid grid = new Grid();
            if (gridExt.equals(".hdf")) {
                grid.readHDF(gridDir + gridName, HdfConstants.DFNT_FLOAT32); // or FLOAT64
            } else if (gridExt.equals(".nc")) {
                grid.readNetCDF(
                        gridDir + gridName,
                        null,
                        Double.NaN,
                        Double.NaN,
                        Double.NaN,
                        Double.NaN,
                        imageWidth,
                        imageHeight);
            } else if (gridExt.equals(".grd")) {
                grid.readGrd(
                        gridDir + gridName,
                        Double.NaN,
                        Double.NaN,
                        Double.NaN,
                        Double.NaN,
                        imageWidth,
                        imageHeight);
            } else {
                String2.log("Unrecognized grid extension: " + gridExt);
                Math2.sleep(5000);
                System.exit(1);
            }

            // get min/max/x/y
            double minX = grid.lon[0];
            double maxX = grid.lon[grid.lon.length - 1];
            double minY = grid.lat[0];
            double maxY = grid.lat[grid.lat.length - 1];
            String2.log(
                    "  file lon min="
                            + minX
                            + " max="
                            + maxX
                            + "\n    spacing="
                            + grid.lonSpacing
                            + "\n  file lat min="
                            + minY
                            + " max="
                            + maxY
                            + "\n    spacing="
                            + grid.latSpacing);
            // String2.log(" file lon=" + String2.toCSSVString(grid.lon) +
            // "\n file lat=" + String2.toCSSVString(grid.lat));

            // make the bufferedImage
            BufferedImage image = SgtUtil.getBufferedImage(imageWidth, imageHeight);

            // make the cpt file
            String contextDir = File2.getWebInfParentDirectory(); // with / separator and / at the end
            DoubleArray dataDA = new DoubleArray(grid.data);
            double stats[] = dataDA.calculateStats();
            double minData = stats[PrimitiveArray.STATS_MIN];
            double maxData = stats[PrimitiveArray.STATS_MAX];
            double[] lowHighData = Math2.suggestLowHigh(minData, maxData);
            String2.log(
                    "  minData="
                            + String2.genEFormat6(minData)
                            + " maxData="
                            + String2.genEFormat6(maxData)
                            + " sugLow="
                            + String2.genEFormat10(lowHighData[0])
                            + " sugHigh="
                            + String2.genEFormat6(lowHighData[1]));
            // String cptName = tempDir + "Rainbow_Linear_" + lowHighData[0] + "_" +
            // lowHighData[1] +
            // ".cpt";
            String cptName = CompoundColorMap.makeCPT(
                    contextDir + "WEB-INF/cptfiles/",
                    "Rainbow",
                    "Linear",
                    lowHighData[0],
                    lowHighData[1],
                    -1,
                    true, // continuous,
                    tempDir);

            // encourage for garbage collection
            dataDA = null;

            // make a map
            makeMap(
                    false,
                    legendPosition,
                    "NOAA",
                    "CoastWatch",
                    contextDir + "images/", // imageDir
                    "noaa_simple.gif", // logoImageFile
                    minX,
                    maxX,
                    minY,
                    maxY,
                    "over",
                    true, // plotGridData,
                    grid,
                    1,
                    1,
                    0, // double gridScaleFactor, gridAltScaleFactor, gridAltOffset,
                    cptName,
                    gridName, // boldTitle
                    "minData=" + (float) minData + " maxData=" + (float) maxData, // title2
                    "",
                    "",
                    SgtMap.NO_LAKES_AND_RIVERS,
                    false, // plotContourData,
                    null, // contourGrid,
                    1,
                    1,
                    0, // double contourScaleFactor, contourAltScaleFactor, contourAltOffset,
                    "10, 14", // contourDrawLinesAt,
                    new Color(0x990099), // contourColor
                    "Contour Bold Title",
                    "cUnits",
                    "Contour Title2 and more text",
                    "2004-01-05 to 2004-01-07", // contourDateTime,
                    "Data courtesy of blah blah blah", // Contour data
                    new ArrayList(), // graphDataLayers
                    (Graphics2D) image.getGraphics(),
                    0,
                    0,
                    imageWidth,
                    imageHeight,
                    0, // boundaryResAdjust,
                    1 // fontScale
            );

            // save as image
            long imageTime = System.currentTimeMillis();
            String imageName = gridName.substring(0, gridName.length() - gridExt.length()) + "png";
            SgtUtil.saveImage(image, gridDir + imageName);
            // Image2.saveAsJpeg(image, gridDir + imageName, 1f);
            // Image2.saveAsGif216(image, gridDir + imageName, false); //use dithering
            imageTime = System.currentTimeMillis() - imageTime;
            String2.log("  imageTime=" + imageTime + "ms\n  imageName=" + gridDir + imageName);

            // make an html file with image and NcCDL info (if .nc or .grd)
            StringBuilder sb = new StringBuilder();
            sb.append(
                    "<html>\n"
                            + "<head>\n"
                            + "  <title>"
                            + gridDir
                            + gridName
                            + "</title>\n"
                            + "</head>\n"
                            + "<body style=\"background-color:white; color:black;\">\n"
                            + "  <img src=\"file://"
                            + gridDir
                            + imageName
                            + "\">\n");
            if (gridExt.equals(".nc") || gridExt.equals(".grd"))
                sb.append("<p><pre>\n" + NcHelper.readCDL(gridDir + gridName) + "\n</pre>\n");
            sb.append("</body>\n</html>\n");
            File2.writeToFileUtf8(gridDir + gridName + ".html", sb.toString());

            // view it
            // ImageViewer.display("SgtMap", image);
            Test.displayInBrowser("file://" + gridDir + gridName + ".html");

            // delete temp files
            File2.delete(cptName);
            if (!(gridDir + gridName).equals(args0))
                File2.delete(gridDir + gridName);

            String2.log(
                    "  SgtMap.main is finished. time="
                            + (System.currentTimeMillis() - time)
                            + "ms\n"
                            + Math2.memoryString());
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            if (args != null && args.length > 0)
                String2.log(NcHelper.ncdump(args[0], ""));
        }
        String2.pressEnterToContinue();
    }

}
