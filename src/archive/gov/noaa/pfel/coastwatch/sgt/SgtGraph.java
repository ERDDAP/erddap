public class SgtGraph {
    /**
   * This uses SGT to make an image with a horizontal legend for one GraphDataLayer.
   *
   * @param xAxisTitle null = none
   * @param yAxisTitle null = none
   * @param legendPosition must be SgtUtil.LEGEND_BELOW (SgtUtil.LEGEND_RIGHT currently not
   *     supported)
   * @param legendTitle1 the first line of the legend
   * @param legendTitle2 the second line of the legend
   * @param imageDir the directory with the logo file
   * @param logoImageFile the logo image file in the imageDir (should be square image) (currently,
   *     must be png, gif, jpg, or bmp) (currently noaa-simple-40.gif for lowRes), or null for none.
   * @param graphDataLayer
   * @param g2 the graphics2D object to be used (the background need not already have been drawn)
   * @param imageWidthPixels defines the image width, in pixels
   * @param imageHeightPixels defines the image height, in pixels
   * @param fontScale relative to 1=normalHeight
   * @throws Exception
   */
  public void makeLegend(
    String xAxisTitle,
    String yAxisTitle,
    int legendPosition,
    String legendTitle1,
    String legendTitle2,
    String imageDir,
    String logoImageFile,
    GraphDataLayer gdl,
    Graphics2D g2,
    int imageWidthPixels,
    int imageHeightPixels,
    double fontScale)
    throws Exception {

  // Coordinates in SGT:
  //   Graph - 'U'ser coordinates      (graph's axes' coordinates)
  //   Layer - 'P'hysical coordinates  (e.g., pseudo-inches, 0,0 is lower left)
  //   JPane - 'D'evice coordinates    (pixels, 0,0 is upper left)

  if (legendTitle1 == null) legendTitle1 = "";
  if (legendTitle2 == null) legendTitle2 = "";

  // set the clip region
  g2.setClip(0, 0, imageWidthPixels, imageHeightPixels);
  try {
    if (reallyVerbose) String2.log("\n{{ SgtGraph.makeLegend "); // + Math2.memoryString());
    long startTime = System.currentTimeMillis();

    double axisLabelHeight = fontScale * defaultAxisLabelHeight;
    double labelHeight = fontScale * defaultLabelHeight;

    // define sizes
    double dpi = 100; // dots per inch
    double imageWidthInches = imageWidthPixels / dpi;
    double imageHeightInches = imageHeightPixels / dpi;
    int labelHeightPixels = Math2.roundToInt(labelHeight * dpi);
    double betweenGraphAndColorBar = fontScale * .25;

    // set legend location and size (in pixels)
    // standard length of vector (and other samples) in user units (e.g., inches)
    double legendSampleSizeInches =
        0.22; // Don't change this (unless make other changes re vector length on graph)
    int legendSampleSize = Math2.roundToInt(legendSampleSizeInches * dpi);
    int legendInsideBorder = Math2.roundToInt(fontScale * 0.1 * dpi);
    int maxCharsPerLine =
        SgtUtil.maxCharsPerLine(
            imageWidthPixels - (legendSampleSize + 3 * legendInsideBorder), fontScale);
    int maxBoldCharsPerLine = SgtUtil.maxBoldCharsPerLine(maxCharsPerLine);

    // String2.log("legendLineCount=" + legendLineCount);
    int legendBoxULX = 0;
    int legendBoxULY = 0;

    // legendTextX and Y
    int legendTextX = legendBoxULX + legendSampleSize + 2 * legendInsideBorder;
    int legendTextY = legendBoxULY + legendInsideBorder + labelHeightPixels;
    // String2.log("SgtGraph baseULXPixel=" + baseULXPixel +  " baseULYPixel=" + baseULYPixel +
    //    "  imageWidth=" + imageWidthPixels + " imageHeight=" + imageHeightPixels +
    //    "\n  legend boxULX=" + legendBoxULX + " boxULY=" + legendBoxULY +
    //    "\n  textX=" + legendTextX + " textY=" + legendTextY +
    //    " insideBorder=" + legendInsideBorder + " labelHeightPixels=" + labelHeightPixels);

    // create the label font
    Font labelFont = new Font(fontFamily, Font.PLAIN, 10); // Font.ITALIC

    // draw legend basics
    if (true) {
      // box for legend
      g2.setColor(new Color(0xFFFFCC));
      g2.fillRect(legendBoxULX, legendBoxULY, imageWidthPixels - 1, imageHeightPixels - 1);
      g2.setColor(Color.black);
      g2.drawRect(legendBoxULX, legendBoxULY, imageWidthPixels - 1, imageHeightPixels - 1);

      // legend titles
      if (String2.isSomething(legendTitle1 + legendTitle2)) {
        if (legendPosition == SgtUtil.LEGEND_BELOW) {
          // draw LEGEND_BELOW
          legendTextY =
              SgtUtil.drawHtmlText(
                  g2,
                  legendTextX,
                  legendTextY,
                  0,
                  fontFamily,
                  labelHeightPixels * 3 / 2,
                  false,
                  "<strong><color=#2600aa>"
                      + SgtUtil.encodeAsHtml(legendTitle1 + " " + legendTitle2)
                      + "</color></strong>");
          legendTextY += labelHeightPixels / 2;
        } else {
          // draw LEGEND_RIGHT
          int tx = legendBoxULX + legendInsideBorder;
          if (legendTitle1.length() > 0)
            legendTextY =
                SgtUtil.drawHtmlText(
                    g2,
                    tx,
                    legendTextY,
                    0,
                    fontFamily,
                    labelHeightPixels * 5 / 4,
                    false,
                    "<strong><color=#2600aa>"
                        + SgtUtil.encodeAsHtml(legendTitle1)
                        + "</color></strong>");
          if (legendTitle2.length() > 0)
            legendTextY =
                SgtUtil.drawHtmlText(
                    g2,
                    tx,
                    legendTextY,
                    0,
                    fontFamily,
                    labelHeightPixels * 5 / 4,
                    false,
                    "<strong><color=#2600aa>"
                        + SgtUtil.encodeAsHtml(legendTitle2)
                        + "</color></strong>");
          legendTextY += labelHeightPixels * 3 / 2;
        }

        // draw the logo
        if (logoImageFile != null && File2.isFile(imageDir + logoImageFile)) {
          BufferedImage bi2 = ImageIO.read(new File(imageDir + logoImageFile));

          // g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          //                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
          //                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
          //                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
          int ulx = legendBoxULX + legendSampleSize / 2;
          int uly = legendBoxULY + legendInsideBorder / 2;
          int tSize = (int) (fontScale * 20);
          g2.drawImage(bi2, ulx, uly, tSize, tSize, null); // null=ImageObserver
          // if (verbose) String2.log("  draw logo time=" +
          //    (System.currentTimeMillis() - logoTime) + "ms");
        }
      }
    }

    // done?
    if (gdl.boldTitle == null) return;

    // create the pane
    JPane jPane = new JPane("", new java.awt.Dimension(imageWidthPixels, imageHeightPixels));
    jPane.setLayout(new StackedLayout());
    StringArray layerNames = new StringArray();
    Dimension2D layerDimension2D = new Dimension2D(imageWidthInches, imageHeightInches);

    // prepare to plot the data
    int tMarkerSize = rint(gdl.markerSize * fontScale);
    boolean drawMarkers = gdl.draw == GraphDataLayer.DRAW_MARKERS;
    boolean drawLines = gdl.draw == GraphDataLayer.DRAW_LINES;
    boolean drawMarkersAndLines = gdl.draw == GraphDataLayer.DRAW_MARKERS_AND_LINES;
    boolean drawSticks = gdl.draw == GraphDataLayer.DRAW_STICKS;
    boolean drawColoredSurface =
        gdl.draw == GraphDataLayer.DRAW_COLORED_SURFACE
            || gdl.draw == GraphDataLayer.DRAW_COLORED_SURFACE_AND_CONTOUR_LINES;

    // useGridData
    if (drawColoredSurface) {
      if (reallyVerbose) String2.log("  drawColoredSurface: " + gdl);
      CompoundColorMap colorMap = (CompoundColorMap) gdl.colorMap;
      Layer layer = new Layer("coloredSurface", layerDimension2D);
      layerNames.add(layer.getId());
      jPane.add(layer); // calls layer.setPane(this);

      // add a horizontal colorBar
      legendTextY += labelHeightPixels;
      CompoundColorMapLayerChild ccmLayerChild = new CompoundColorMapLayerChild("", colorMap);
      ccmLayerChild.setRectangle( // leftX,upperY(when rotated),width,height
          layer.getXDtoP(legendTextX),
          layer.getYDtoP(legendTextY),
          imageWidthInches
              - (2 * legendInsideBorder + legendSampleSize) / dpi
              - betweenGraphAndColorBar,
          fontScale * 0.15);
      ccmLayerChild.setLabelFont(labelFont);
      ccmLayerChild.setLabelHeightP(axisLabelHeight);
      ccmLayerChild.setTicLength(fontScale * 0.02);
      layer.addChild(ccmLayerChild);
      legendTextY += 3 * labelHeightPixels;
    }

    // draw colorMap (do first since colorMap shifts other things down)
    if ((drawMarkers || drawMarkersAndLines) && gdl.colorMap != null) {
      // draw the color bar
      Layer layer = new Layer("colorbar", layerDimension2D);
      layerNames.add(layer.getId());
      jPane.add(layer); // calls layer.setPane(this);

      legendTextY += labelHeightPixels;
      CompoundColorMapLayerChild lc =
          new CompoundColorMapLayerChild("", (CompoundColorMap) gdl.colorMap);
      lc.setRectangle( // leftX,upperY(when rotated),width,height
          layer.getXDtoP(legendTextX),
          layer.getYDtoP(legendTextY),
          imageWidthInches
              - (2 * legendInsideBorder + legendSampleSize) / dpi
              - betweenGraphAndColorBar,
          fontScale * 0.15);
      lc.setLabelFont(labelFont);
      lc.setLabelHeightP(axisLabelHeight);
      lc.setTicLength(fontScale * 0.02);
      layer.addChild(lc);
      legendTextY += 3 * labelHeightPixels;
    }

    // draw a line
    // don't draw line if gdl.draw = DRAW_COLORED_SURFACE_AND_CONTOUR_LINES
    if (gdl.draw == GraphDataLayer.DRAW_CONTOUR_LINES
        || drawLines
        || drawMarkersAndLines
        || drawSticks) {
      g2.setColor(gdl.lineColor);
      g2.drawLine(
          legendTextX - legendSampleSize - legendInsideBorder,
          legendTextY - labelHeightPixels / 2,
          legendTextX - legendInsideBorder,
          legendTextY - labelHeightPixels / 2);
    }

    // draw marker
    if (drawMarkers || drawMarkersAndLines) {
      int tx = legendTextX - legendInsideBorder - legendSampleSize / 2;
      int ty = legendTextY - labelHeightPixels / 2;
      g2.setColor(gdl.lineColor);
      drawMarker(
          g2,
          gdl.markerType,
          tMarkerSize,
          tx,
          ty,
          gdl.colorMap == null
              ? gdl.lineColor
              : gdl.colorMap.getColor(
                  (gdl.colorMap.getRange().start + gdl.colorMap.getRange().end) / 2),
          gdl.lineColor);
    }

    // draw legend text
    g2.setColor(gdl.lineColor);
    SgtUtil.belowLegendText(
        g2,
        legendTextX,
        legendTextY,
        fontFamily,
        labelHeightPixels,
        SgtUtil.makeShortLines(maxBoldCharsPerLine, gdl.boldTitle, null, null),
        SgtUtil.makeShortLines(maxCharsPerLine, gdl.title2, gdl.title3, gdl.title4));

    // actually draw the graph
    jPane.draw(g2); // comment out for memory leak tests

    // deconstruct jPane
    SgtMap.deconstructJPane("SgtMap.makeLegend", jPane, layerNames);

    // return antialiasing to original
    // if (originalAntialiasing != null)
    //    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
    //        originalAntialiasing);

    // display time to makeGraph
    if (verbose)
      String2.log(
          "}} SgtGraph.makeLegend done. TOTAL TIME="
              + (System.currentTimeMillis() - startTime)
              + "ms\n");
  } finally {
    g2.setClip(null);
  }
}
}
