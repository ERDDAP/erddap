package gov.noaa.pfel.erddap.filetypes;

import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.Grid;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.CompoundColorMap;
import gov.noaa.pfel.coastwatch.sgt.GraphDataLayer;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.GridDataAccessor;
import gov.noaa.pfel.erddap.dataset.OutputStreamSource;
import gov.noaa.pfel.erddap.dataset.OutputStreamSourceSimple;
import gov.noaa.pfel.erddap.dataset.TableWriterAllWithMetadata;
import gov.noaa.pfel.erddap.dataset.WaitThenTryAgainException;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import gov.noaa.pfel.erddap.variable.EDVGridAxis;
import gov.noaa.pfel.erddap.variable.EDVLatGridAxis;
import gov.noaa.pfel.erddap.variable.EDVLonGridAxis;
import gov.noaa.pfel.erddap.variable.EDVTimeStamp;
import gov.noaa.pfel.erddap.variable.EDVTimeStampGridAxis;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ImageFiles extends ImageTypes {

  @Override
  protected boolean tableToImage(DapRequestInfo requestInfo, OutputStreamSourceSimple osss)
      throws Throwable {
    return saveAsImage(
        requestInfo.language(),
        requestInfo.loggedInAs(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        osss,
        requestInfo.fileTypeName(),
        requestInfo.getEDDTable());
  }

  @Override
  protected boolean gridToImage(DapRequestInfo requestInfo, OutputStreamSourceSimple osss)
      throws Throwable {
    return saveAsImage(
        requestInfo.language(),
        requestInfo.loggedInAs(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.dir(),
        requestInfo.fileName(),
        osss,
        requestInfo.fileTypeName(),
        requestInfo.getEDDGrid());
  }

  /**
   * This saves the data in the table to the outputStream as an image. If
   * table.getColumnName(0)=LON_NAME and table.getColumnName(0)=LAT_NAME, this plots the data on a
   * map. Otherwise, this makes a graph with x=col(0) and y=col(1).
   *
   * @param language the index of the selected language
   * @param loggedInAs the user's login name if logged in (or null if not logged in).
   * @param requestUrl I think it's currently just used to add to "history" metadata.
   * @param userDapQuery the part after the '?', still percentEncoded (shouldn't be null).
   * @param outputStreamSource
   * @param fileTypeName
   * @return true of written ok; false if exception occurred (and written on image)
   * @throws Throwable if trouble
   */
  private boolean saveAsImage(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      String fileTypeName,
      EDDTable eddTable)
      throws Throwable {

    if (EDDTable.reallyVerbose) String2.log("  EDDTable.saveAsImage query=" + userDapQuery);
    long time = System.currentTimeMillis();
    if (EDDTable.debugMode) String2.log("saveAsImage 1");
    // imageFileTypes
    // determine the size
    int sizeIndex =
        fileTypeName.startsWith(".small")
            ? 0
            : fileTypeName.startsWith(".medium") ? 1 : fileTypeName.startsWith(".large") ? 2 : 1;
    boolean pdf = fileTypeName.toLowerCase().endsWith("pdf");
    boolean png = fileTypeName.toLowerCase().endsWith("png");
    boolean transparentPng = fileTypeName.equals(".transparentPng");
    if (!pdf && !png)
      throw new SimpleException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
              + "Unexpected image type="
              + fileTypeName);
    Object pdfInfo[] = null;
    BufferedImage bufferedImage = null;
    Graphics2D g2 = null;
    int imageWidth, imageHeight;
    if (pdf) {
      imageWidth = EDStatic.messages.pdfWidths[sizeIndex];
      imageHeight = EDStatic.messages.pdfHeights[sizeIndex];
    } else if (transparentPng) {
      imageWidth = EDStatic.messages.imageWidths[sizeIndex];
      imageHeight = imageWidth;
    } else {
      imageWidth = EDStatic.messages.imageWidths[sizeIndex];
      imageHeight = EDStatic.messages.imageHeights[sizeIndex];
    }

    Color transparentColor =
        transparentPng ? Color.white : null; // getBufferedImage returns white background
    String drawLegend = EDDTable.LEGEND_BOTTOM;
    int trim = Integer.MAX_VALUE;
    boolean ok = true;

    try {
      // get the user-specified resultsVariables
      StringArray resultsVariables = new StringArray();
      StringArray constraintVariables = new StringArray();
      StringArray constraintOps = new StringArray();
      StringArray constraintValues = new StringArray();
      eddTable.parseUserDapQuery(
          language,
          userDapQuery,
          resultsVariables,
          constraintVariables,
          constraintOps,
          constraintValues, // non-regex EDVTimeStamp conValues will be ""+epochSeconds
          false);
      if (EDDTable.debugMode) String2.log("saveAsImage 2");

      if (resultsVariables.size() < 2)
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.get(Message.QUERY_ERROR, 0)
                    + MessageFormat.format(
                        EDStatic.messages.get(Message.QUERY_ERROR_2_VAR, 0), fileTypeName),
                EDStatic.messages.get(Message.QUERY_ERROR, language)
                    + MessageFormat.format(
                        EDStatic.messages.get(Message.QUERY_ERROR_2_VAR, language), fileTypeName)));
      // xVar,yVar are 1st and 2nd request variables
      EDV xVar = eddTable.findVariableByDestinationName(resultsVariables.get(0));
      EDV yVar = eddTable.findVariableByDestinationName(resultsVariables.get(1));
      EDV zVar = null, tVar = null;
      String xUnits = xVar.units(); // may be null
      String yUnits = yVar.units();
      boolean isMap = false;
      if (String2.caseInsensitiveIndexOf(EDV.LON_UNITS_VARIANTS, xUnits) >= 0
          && String2.caseInsensitiveIndexOf(EDV.LAT_UNITS_VARIANTS, yUnits) >= 0) {
        isMap = true;
      } else if (String2.caseInsensitiveIndexOf(EDV.LON_UNITS_VARIANTS, yUnits) >= 0
          && String2.caseInsensitiveIndexOf(EDV.LAT_UNITS_VARIANTS, xUnits) >= 0) {
        isMap = true;
        // force x=lon, y=lat
        EDV e = xVar;
        xVar = yVar;
        yVar = e;
        String s = xUnits;
        xUnits = yUnits;
        yUnits = s;
      }
      if (resultsVariables.size() >= 3)
        zVar = eddTable.findVariableByDestinationName(resultsVariables.get(2));
      if (resultsVariables.size() >= 4)
        tVar = eddTable.findVariableByDestinationName(resultsVariables.get(3));

      // get the table with all the data
      // errors here will be caught below
      // drawLegend=Only: Since data is needed early on, no way to not get data if legend doesn't
      // need it
      TableWriterAllWithMetadata twawm =
          eddTable.getTwawmForDapQuery(language, loggedInAs, requestUrl, userDapQuery);
      Table table = twawm.cumulativeTable();
      twawm.releaseResources();
      twawm.close();
      table.convertToStandardMissingValues();
      if (EDDTable.debugMode) String2.log("saveAsImage 3");

      // units
      int xColN = table.findColumnNumber(xVar.destinationName());
      int yColN = table.findColumnNumber(yVar.destinationName());
      int zColN = zVar == null ? -1 : table.findColumnNumber(zVar.destinationName());
      int tColN = tVar == null ? -1 : table.findColumnNumber(tVar.destinationName());
      if (xVar instanceof EDVTimeStamp) xUnits = "UTC";
      if (yVar instanceof EDVTimeStamp) yUnits = "UTC";
      String zUnits = zVar == null ? null : zVar instanceof EDVTimeStamp ? "UTC" : zVar.units();
      String tUnits = tVar == null ? null : tVar instanceof EDVTimeStamp ? "UTC" : tVar.units();
      xUnits = xUnits == null ? "" : " (" + xUnits + ")";
      yUnits = yUnits == null ? "" : " (" + yUnits + ")";
      zUnits = zUnits == null ? "" : " (" + zUnits + ")";
      tUnits = tUnits == null ? "" : " (" + tUnits + ")";

      // extract optional .graphicsSettings from userDapQuery
      //  xRange, yRange, color and colorbar information
      //  title2 -- a prettified constraint string
      boolean drawLines = false;
      boolean drawLinesAndMarkers = false;
      boolean drawMarkers = true;
      boolean drawSticks = false;
      boolean drawVectors = false;
      int markerType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE;
      int markerSize = GraphDataLayer.MARKER_SIZE_SMALL;
      Color color = Color.black;

      // set colorBar defaults via zVar attributes
      String ts;
      ts = zVar == null ? null : zVar.combinedAttributes().getString(language, "colorBarPalette");
      String palette = ts == null ? "" : ts;
      ts = zVar == null ? null : zVar.combinedAttributes().getString(language, "colorBarScale");
      String scale = ts == null ? "Linear" : ts;
      double paletteMin =
          zVar == null
              ? Double.NaN
              : zVar.combinedAttributes().getDouble(language, "colorBarMinimum");
      double paletteMax =
          zVar == null
              ? Double.NaN
              : zVar.combinedAttributes().getDouble(language, "colorBarMaximum");
      int nSections =
          zVar == null ? -1 : zVar.combinedAttributes().getInt(language, "colorBarNSections");
      if (nSections < 0 || nSections >= 100) nSections = -1;
      ts =
          zVar == null ? null : zVar.combinedAttributes().getString(language, "colorBarContinuous");
      boolean continuous = String2.parseBoolean(ts); // defaults to true

      // x/yMin < x/yMax
      double xMin = Double.NaN, xMax = Double.NaN, yMin = Double.NaN, yMax = Double.NaN;
      boolean xAscending = true, yAscending = true; // this is what controls flipping of the axes
      String xScale = "", yScale = ""; // (default) or Linear or Log
      double fontScale = 1, vectorStandard = Double.NaN;
      String currentDrawLandMask = null; // not yet set
      StringBuilder title2 = new StringBuilder();
      Color bgColor = EDStatic.config.graphBackgroundColor;
      String ampParts[] =
          Table.getDapQueryParts(userDapQuery); // decoded.  always at least 1 part (may be "")
      for (int ap = 0; ap < ampParts.length; ap++) {
        String ampPart = ampParts[ap];
        if (EDDTable.debugMode) String2.log("saveAsImage 4 " + ap);

        // .bgColor
        if (ampPart.startsWith(".bgColor=")) {
          String pParts[] = String2.split(ampPart.substring(9), '|');
          if (pParts.length > 0 && pParts[0].length() > 0)
            bgColor = new Color(String2.parseInt(pParts[0]), true); // hasAlpha

          // .colorBar defaults: palette=""|continuous=C|scale=Linear|min=NaN|max=NaN|nSections=-1
        } else if (ampPart.startsWith(".colorBar=")) {
          String pParts[] = String2.split(ampPart.substring(10), '|');
          if (pParts == null) pParts = new String[0];
          if (pParts.length > 0 && pParts[0].length() > 0) palette = pParts[0];
          if (pParts.length > 1 && pParts[1].length() > 0)
            continuous = !pParts[1].toLowerCase().startsWith("d");
          if (pParts.length > 2 && pParts[2].length() > 0) scale = pParts[2];
          if (pParts.length > 3 && pParts[3].length() > 0)
            paletteMin = String2.parseDouble(pParts[3]);
          if (pParts.length > 4 && pParts[4].length() > 0)
            paletteMax = String2.parseDouble(pParts[4]);
          if (pParts.length > 5 && pParts[5].length() > 0) nSections = String2.parseInt(pParts[5]);
          if (String2.indexOf(EDStatic.messages.palettes, palette) < 0) palette = "";
          if (EDV.VALID_SCALES.indexOf(scale) < 0) scale = "Linear";
          if (nSections < 0 || nSections >= 100) nSections = -1;
          if (EDDTable.reallyVerbose)
            String2.log(
                ".colorBar palette="
                    + palette
                    + " continuous="
                    + continuous
                    + " scale="
                    + scale
                    + " min="
                    + paletteMin
                    + " max="
                    + paletteMax
                    + " nSections="
                    + nSections);

          // .color
        } else if (ampPart.startsWith(".color=")) {
          int iColor = String2.parseInt(ampPart.substring(7));
          if (iColor < Integer.MAX_VALUE) {
            color = new Color(iColor);
            if (EDDTable.reallyVerbose) String2.log(".color=0x" + Integer.toHexString(iColor));
          }

          // .draw
        } else if (ampPart.startsWith(".draw=")) {
          String tDraw = ampPart.substring(6);
          // make all false
          drawLines = false;
          drawLinesAndMarkers = false;
          drawMarkers = false;
          drawSticks = false;
          drawVectors = false;
          // set one option to true
          if (tDraw.equals("sticks") && zVar != null) {
            drawSticks = true;
            isMap = false;
          } else if (isMap && tDraw.equals("vectors") && zVar != null && tVar != null)
            drawVectors = true;
          else if (tDraw.equals("lines")) drawLines = true;
          else if (tDraw.equals("linesAndMarkers")) drawLinesAndMarkers = true;
          else drawMarkers = true; // default

          // .font
        } else if (ampPart.startsWith(".font=")) {
          String pParts[] =
              String2.split(ampPart.substring(6), '|'); // subparts may be ""; won't be null
          if (pParts == null) pParts = new String[0];
          if (pParts.length > 0) fontScale = String2.parseDouble(pParts[0]);
          fontScale =
              Double.isNaN(fontScale) ? 1 : fontScale < 0.1 ? 0.1 : fontScale > 10 ? 10 : fontScale;
          if (EDDTable.reallyVerbose) String2.log(".font= scale=" + fontScale);

          // .land
        } else if (ampPart.startsWith(".land=")) {
          String gt = ampPart.substring(6);
          int which = SgtMap.drawLandMask_OPTIONS.indexOf(gt);
          if (which >= 1) currentDrawLandMask = gt;
          if (EDDTable.reallyVerbose)
            String2.log(".land= currentDrawLandMask=" + currentDrawLandMask);

          // .legend
        } else if (ampPart.startsWith(".legend=")) {
          drawLegend = ampPart.substring(8);
          if (!drawLegend.equals(EDDTable.LEGEND_OFF) && !drawLegend.equals(EDDTable.LEGEND_ONLY))
            drawLegend = EDDTable.LEGEND_BOTTOM;
          if (drawLegend.equals(EDDTable.LEGEND_ONLY)) {
            transparentPng = false; // if it was transparent, it was already png=true, size=1
            transparentColor = null;
          }

          // .marker
        } else if (ampPart.startsWith(".marker=")) {
          String pParts[] =
              String2.split(ampPart.substring(8), '|'); // subparts may be ""; won't be null
          if (pParts == null) pParts = new String[0];
          if (pParts.length > 0) markerType = String2.parseInt(pParts[0]);
          if (pParts.length > 1) markerSize = String2.parseInt(pParts[1]);
          if (markerType < 0 || markerType >= GraphDataLayer.MARKER_TYPES.size())
            markerType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE;
          if (markerSize < 1 || markerSize > 50) markerSize = GraphDataLayer.MARKER_SIZE_SMALL;
          if (EDDTable.reallyVerbose)
            String2.log(".marker= type=" + markerType + " size=" + markerSize);

          // .size
        } else if (ampPart.startsWith(".size=")) {
          // customSize = true; not used in EDDTable
          String pParts[] =
              String2.split(ampPart.substring(6), '|'); // subparts may be ""; won't be null
          if (pParts == null) pParts = new String[0];
          if (pParts.length > 0) {
            int w = String2.parseInt(pParts[0]);
            if (w > 0 && w <= EDD.WMS_MAX_WIDTH) imageWidth = w;
          }
          if (pParts.length > 1) {
            int h = String2.parseInt(pParts[1]);
            if (h > 0 && h <= EDD.WMS_MAX_HEIGHT) imageHeight = h;
          }
          if (EDDTable.reallyVerbose)
            String2.log(".size=  imageWidth=" + imageWidth + " imageHeight=" + imageHeight);

          // .trim
        } else if (ampPart.startsWith(".trim=")) {
          trim = String2.parseInt(ampPart.substring(6));
          if (EDDTable.reallyVerbose) String2.log(".trim " + trim);

          // .vec
        } else if (ampPart.startsWith(".vec=")) {
          vectorStandard = String2.parseDouble(ampPart.substring(5));
          if (EDDTable.reallyVerbose) String2.log(".vec " + vectorStandard);

          // .xRange   (supported, but currently not created by the Make A Graph form)
          //  this is more powerful than xVar constraints
        } else if (ampPart.startsWith(".xRange=")) {
          String pParts[] = String2.split(ampPart.substring(8), '|');
          if (pParts.length > 0) xMin = String2.parseDouble(pParts[0]);
          if (pParts.length > 1) xMax = String2.parseDouble(pParts[1]);
          if (pParts.length > 2)
            // if the param slot is there, the param determines Ascending
            xAscending = String2.parseBoolean(pParts[2]); // "" -> true(the default)
          if (pParts.length > 3) {
            // if the param slot is there, the param determines Scale
            xScale = String2.toTitleCase(pParts[3].trim()); // "" -> (the default)
            if (!xScale.equals("Log") && !xScale.equals("Linear"))
              xScale = ""; // "" -> (the default)
          }
          if (EDDTable.reallyVerbose)
            String2.log(
                ".xRange min="
                    + xMin
                    + " max="
                    + xMax
                    + " ascending="
                    + xAscending
                    + " xScale="
                    + xScale);

          // .yRange
          //  this is more powerful than yVar constraints
        } else if (ampPart.startsWith(".yRange=")) {
          String pParts[] = String2.split(ampPart.substring(8), '|');
          if (pParts.length > 0) yMin = String2.parseDouble(pParts[0]);
          if (pParts.length > 1) yMax = String2.parseDouble(pParts[1]);
          if (pParts.length > 2)
            // if the param slot is there, the param determines Ascending
            yAscending = String2.parseBoolean(pParts[2]); // "" -> true(the default)
          if (pParts.length > 3) {
            // if the param slot is there, the param determines Scale
            yScale = String2.toTitleCase(pParts[3].trim()); // "" -> (the default)
            if (!yScale.equals("Log") && !yScale.equals("Linear"))
              yScale = ""; // "" -> (the default)
          }
          if (EDDTable.reallyVerbose)
            String2.log(
                ".yRange min="
                    + yMin
                    + " max="
                    + yMax
                    + " ascending="
                    + yAscending
                    + " scale="
                    + yScale);

          // ignore any unrecognized .something
        } else if (ampPart.startsWith(".")) {

          // don't do anything with list of vars
        } else if (ap == 0) {

          // x and yVar constraints
        } else if (ampPart.startsWith(xVar.destinationName())
            || // x,y axis ranges indicate x,y var constraints
            ampPart.startsWith(yVar.destinationName())) {
          // don't include in title2  (better to leave in, but space often limited)

          // constraints on other vars
        } else {
          // add to title2
          if (title2.length() > 0) title2.append(", ");
          title2.append(ampPart);
        }
      }
      if (title2.length() > 0) {
        title2.insert(0, "(");
        title2.append(")");
      }
      if (xScale.length() == 0) {
        // use the default from colorBarScale
        xScale = xVar.combinedAttributes().getString(language, "colorBarScale");
        xScale = xScale == null ? "" : String2.toTitleCase(xScale.trim());
        if (!xScale.equals("Log")) xScale = "Linear"; // apply "" default -> Linear
      }
      boolean xIsLogAxis = !(xVar instanceof EDVTimeStamp) && xScale.equals("Log");
      if (yScale.length() == 0) {
        // use the default from colorBarScale
        yScale = yVar.combinedAttributes().getString(language, "colorBarScale");
        yScale = yScale == null ? "" : String2.toTitleCase(yScale.trim());
        if (!yScale.equals("Log")) yScale = "Linear"; // apply "" default -> Linear
      }
      boolean yIsLogAxis = !(yVar instanceof EDVTimeStamp) && yScale.equals("Log");

      if (EDDTable.debugMode) String2.log("saveAsImage 5");

      // make colorMap if needed
      CompoundColorMap colorMap = null;
      // if (drawLines || drawSticks || drawVectors)
      //    colorMap = null;
      // if ((drawLinesAndMarkers || drawMarkers) && zVar == null)
      //    colorMap = null;
      if (drawVectors && zVar != null && Double.isNaN(vectorStandard)) {
        double zStats[] = table.getColumn(zColN).calculateStats();
        if (zStats[PrimitiveArray.STATS_N] == 0) {
          vectorStandard = 1;
        } else {
          double minMax[] =
              Math2.suggestLowHigh(
                  0,
                  Math.max(
                      Math.abs(zStats[PrimitiveArray.STATS_MIN]),
                      Math.abs(zStats[PrimitiveArray.STATS_MAX])));
          vectorStandard = minMax[1];
        }
      }
      if ((drawLinesAndMarkers || drawMarkers) && zVar != null && colorMap == null) {
        if ((palette.length() == 0 || Double.isNaN(paletteMin) || Double.isNaN(paletteMax))
            && zColN >= 0) {
          // set missing items based on z data
          double zStats[] = table.getColumn(zColN).calculateStats();
          if (zStats[PrimitiveArray.STATS_N] > 0) {
            double minMax[];
            if (zVar instanceof EDVTimeStamp) {
              // ???I think this is too crude. Smarter code elsewhere? Or handled by
              // compoundColorMap?
              double r20 =
                  (zStats[PrimitiveArray.STATS_MAX] - zStats[PrimitiveArray.STATS_MIN]) / 20;
              minMax =
                  new double[] {
                    zStats[PrimitiveArray.STATS_MIN] - r20, zStats[PrimitiveArray.STATS_MAX] + r20
                  };
            } else {
              minMax =
                  Math2.suggestLowHigh(
                      zStats[PrimitiveArray.STATS_MIN], zStats[PrimitiveArray.STATS_MAX]);
            }

            if (palette.length() == 0) {
              if (minMax[1] >= minMax[0] / -2 && minMax[1] <= minMax[0] * -2) {
                double td = Math.max(minMax[1], -minMax[0]);
                minMax[0] = -td;
                minMax[1] = td;
                palette = "BlueWhiteRed";
              } else {
                palette = "Rainbow";
              }
            }
            if (Double.isNaN(paletteMin)) paletteMin = minMax[0];
            if (Double.isNaN(paletteMax)) paletteMax = minMax[1];
          }
        }
        if (palette.length() == 0 || Double.isNaN(paletteMin) || Double.isNaN(paletteMax)) {
          String2.log(
              "Warning in EDDTable.saveAsImage: NaNs not allowed (zVar has no numeric data):"
                  + " palette="
                  + palette
                  + " paletteMin="
                  + paletteMin
                  + " paletteMax="
                  + paletteMax);
        } else {
          if (EDDTable.reallyVerbose)
            String2.log(
                "create colorBar palette="
                    + palette
                    + " continuous="
                    + continuous
                    + " scale="
                    + scale
                    + " min="
                    + paletteMin
                    + " max="
                    + paletteMax
                    + " nSections="
                    + nSections);
          if (zVar instanceof EDVTimeStamp)
            colorMap =
                new CompoundColorMap(
                    EDStatic.config.fullPaletteDirectory,
                    palette,
                    false, // false= data is seconds
                    paletteMin,
                    paletteMax,
                    nSections,
                    continuous,
                    EDStatic.config.fullCptCacheDirectory);
          else
            colorMap =
                new CompoundColorMap(
                    EDStatic.config.fullPaletteDirectory,
                    palette,
                    scale,
                    paletteMin,
                    paletteMax,
                    nSections,
                    continuous,
                    EDStatic.config.fullCptCacheDirectory);
        }
      }

      // x|yVar > >= < <= constraints are relevant to x|yMin|Max (if not already set)
      StringBuilder constraintTitle = new StringBuilder();
      for (int con = 0; con < constraintVariables.size(); con++) {
        String conVar = constraintVariables.get(con);
        String conOp = constraintOps.get(con);
        String conVal = constraintValues.get(con);
        double conValD = String2.parseDouble(conVal); // times are epochSeconds
        boolean isX = conVar.equals(xVar.destinationName());
        boolean isY = conVar.equals(yVar.destinationName());
        if (isX || isY) {
          boolean isG = conOp.startsWith(">");
          boolean isL = conOp.startsWith("<");
          if (isG || isL) {
            if (isX && isG && Double.isNaN(xMin)) xMin = conValD;
            else if (isX && isL && Double.isNaN(xMax)) xMax = conValD;
            else if (isY && isG && Double.isNaN(yMin)) yMin = conValD;
            else if (isY && isL && Double.isNaN(yMax)) yMax = conValD;
          }

          // isX and isY constraints not written to legend:
          //  axis range implies variable constraint  (e.g., lat, lon, time)

        } else {

          // build constraintTitle for legend
          if (constraintTitle.length() > 0) constraintTitle.append(", ");
          EDV edv = eddTable.findDataVariableByDestinationName(conVar);
          constraintTitle.append(
              conVar
                  + conOp
                  + ((conOp.equals(PrimitiveArray.REGEX_OP)
                          || edv.destinationDataPAType() == PAType.STRING)
                      ? String2.toJson(conVal)
                      : !Double.isFinite(conValD)
                          ? "NaN"
                          : edv instanceof EDVTimeStamp
                              ?
                              // not time_precision, since query may be more precise
                              Calendar2.epochSecondsToIsoStringTZ(conValD)
                              : conVal));
        }
      }
      if (constraintTitle.length() > 0) {
        constraintTitle.insert(0, '(');
        constraintTitle.append(')');
      }
      if (EDDTable.debugMode) String2.log("saveAsImage 6");

      String varTitle = "";
      if (drawLines) {
        varTitle = "";
      } else if (drawLinesAndMarkers || drawMarkers) {
        varTitle = zVar == null || colorMap == null ? "" : zVar.longName() + zUnits;
      } else if (drawSticks) {
        varTitle =
            "x="
                + yVar.destinationName()
                + (yUnits.equals(zUnits) ? "" : yUnits)
                + ", y="
                + zVar.destinationName()
                + zUnits;
      } else if (drawVectors) {
        varTitle =
            "x="
                + zVar.destinationName()
                + (zUnits.equals(tUnits) ? "" : zUnits)
                + ", y="
                + tVar.destinationName()
                + tUnits
                + ", standard="
                + (float) vectorStandard;
      }
      String yLabel = drawSticks ? varTitle : yVar.longName() + yUnits;

      // make a graphDataLayer
      GraphDataLayer graphDataLayer =
          new GraphDataLayer(
              -1, // which pointScreen
              xColN,
              yColN,
              zColN,
              tColN,
              zColN, // x,y,z1,z2,z3 column numbers
              drawSticks
                  ? GraphDataLayer.DRAW_STICKS
                  : drawVectors
                      ? GraphDataLayer.DRAW_POINT_VECTORS
                      : drawLines
                          ? GraphDataLayer.DRAW_LINES
                          : drawLinesAndMarkers
                              ? GraphDataLayer.DRAW_MARKERS_AND_LINES
                              : GraphDataLayer.DRAW_MARKERS, // default
              true,
              false,
              xVar.longName() + xUnits, // x,yAxisTitle  for now, always std units
              yVar.longName() + yUnits,
              varTitle.length() > 0 ? varTitle : eddTable.title(language),
              varTitle.length() > 0 ? eddTable.title(language) : "",
              constraintTitle.toString(), // title2.toString(),
              MessageFormat.format(
                  EDStatic.messages.get(Message.IMAGE_DATA_COURTESY_OF, language),
                  eddTable.institution(language)),
              table,
              null,
              null,
              colorMap,
              color,
              markerType,
              markerSize,
              vectorStandard,
              GraphDataLayer.REGRESS_NONE);
      ArrayList<GraphDataLayer> graphDataLayers = new ArrayList<>();
      graphDataLayers.add(graphDataLayer);

      // setup graphics2D
      String logoImageFile;
      if (pdf) {
        logoImageFile = EDStatic.config.highResLogoImageFile;
        fontScale *= 1.4; // SgtMap.PDF_FONTSCALE=1.5 is too big
        // getting the outputStream was delayed as long as possible to allow errors
        // to be detected and handled before committing to sending results to client
        pdfInfo =
            SgtUtil.createPdf(
                SgtUtil.PDFPageSize.LETTER_PORTRAIT,
                imageWidth,
                imageHeight,
                outputStreamSource.outputStream(File2.UTF_8));
        g2 = (Graphics2D) pdfInfo[0];
      } else {
        logoImageFile =
            sizeIndex <= 1
                ? EDStatic.config.lowResLogoImageFile
                : EDStatic.config.highResLogoImageFile;
        fontScale *= imageWidth < 500 ? 1 : 1.25;
        bufferedImage = SgtUtil.getBufferedImage(imageWidth, imageHeight);
        g2 = (Graphics2D) bufferedImage.getGraphics();
      }
      if (EDDTable.reallyVerbose)
        String2.log(
            "  sizeIndex="
                + sizeIndex
                + " pdf="
                + pdf
                + " imageWidth="
                + imageWidth
                + " imageHeight="
                + imageHeight);
      if (EDDTable.debugMode) String2.log("saveAsImage 7");

      if (isMap) {
        // create a map

        if (Double.isNaN(xMin) || Double.isNaN(xMax) || Double.isNaN(yMin) || Double.isNaN(yMax)) {

          // calculate the xy axis ranges (this should be in make map!)
          double xStats[] = table.getColumn(xColN).calculateStats();
          double yStats[] = table.getColumn(yColN).calculateStats();
          if (xStats[PrimitiveArray.STATS_N] == 0 || yStats[PrimitiveArray.STATS_N] == 0)
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.get(Message.QUERY_ERROR, 0)
                        + EDStatic.messages.get(Message.NO_DATA_NO_LL, 0),
                    EDStatic.messages.get(Message.QUERY_ERROR, language)
                        + EDStatic.messages.get(Message.NO_DATA_NO_LL, language)));

          // old way  (too tied to big round numbers like 100, 200, 300)
          // often had big gap on one side
          // double xLH[] = Math2.suggestLowHigh(
          //    xStats[PrimitiveArray.STATS_MIN],
          //    xStats[PrimitiveArray.STATS_MAX]);
          // double yLH[] = Math2.suggestLowHigh(
          //    yStats[PrimitiveArray.STATS_MIN],
          //    yStats[PrimitiveArray.STATS_MAX]);

          // new way
          double xLH[] = {xStats[PrimitiveArray.STATS_MIN], xStats[PrimitiveArray.STATS_MAX]};
          double[] sd = Math2.suggestDivisions(xLH[1] - xLH[0]);
          xLH[0] -= sd[1]; // tight range
          xLH[1] += sd[1];
          if (xStats[PrimitiveArray.STATS_N] == 0) {
            xLH[0] = xVar.destinationMinDouble();
            xLH[1] = xVar.destinationMaxDouble();
          }

          double yLH[] = {yStats[PrimitiveArray.STATS_MIN], yStats[PrimitiveArray.STATS_MAX]};
          sd = Math2.suggestDivisions(yLH[1] - yLH[0]);
          yLH[0] -= sd[1]; // tight range
          yLH[1] += sd[1];
          if (yStats[PrimitiveArray.STATS_N] == 0) {
            yLH[0] = yVar.destinationMinDouble();
            yLH[1] = yVar.destinationMaxDouble();
          }

          // ensure default range at least 1x1 degree
          double expandBy = (1 - (xLH[1] - xLH[0])) / 2;
          if (expandBy > 0) {
            xLH[0] -= expandBy;
            xLH[1] += expandBy;
          }
          expandBy = (1 - (yLH[1] - yLH[0])) / 2;
          if (expandBy > 0) {
            yLH[0] -= expandBy;
            yLH[1] += expandBy;
          }

          // ensure reasonable for a map
          if (xLH[0] < -180) xLH[0] = -180;
          // deal with odd cases like pmelArgoAll: x<0 and >180
          if (-xLH[0] > xLH[1] - 180) { // i.e., if minX is farther below 0, then maxX is >180
            if (xLH[1] > 180) xLH[1] = 180;
          } else {
            if (xLH[0] < 0) xLH[0] = 0;
            if (xLH[1] > 360) xLH[1] = 360;
          }
          if (yLH[0] < -90) yLH[0] = -90;
          if (yLH[1] > 90) yLH[1] = 90;

          // make square
          if (true) {
            double xRange = xLH[1] - xLH[0];
            double yRange = yLH[1] - yLH[0];
            if (xRange > yRange) {
              double diff2 = (xRange - yRange) / 2;
              yLH[0] = Math.max(-90, yLH[0] - diff2);
              yLH[1] = Math.min(90, yLH[1] + diff2);
            } else {
              double diff2 = (yRange - xRange) / 2;
              // deal with odd cases like pmelArgoAll: x<0 and >180
              if (-xLH[0] > xLH[1] - 180) { // i.e., if minX is farther below 0, than maxX is >180
                xLH[0] = Math.max(-180, xLH[0] - diff2);
                xLH[1] = Math.min(180, xLH[1] + diff2);
              } else {
                xLH[0] = Math.max(0, xLH[0] - diff2);
                xLH[1] = Math.min(360, xLH[1] + diff2);
              }
            }
          }

          // set xyMin/Max
          if (Double.isNaN(xMin) || Double.isNaN(xMax)) {
            xMin = xLH[0];
            xMax = xLH[1];
          }
          if (Double.isNaN(yMin) || Double.isNaN(yMax)) {
            yMin = yLH[0];
            yMax = yLH[1];
          }
        }

        // unlike graphs, maps still force xMin < xMax, and yMin < yMax
        if (xMin > xMax) {
          double d = xMin;
          xMin = xMax;
          xMax = d;
        }
        if (yMin > yMax) {
          double d = yMin;
          yMin = yMax;
          yMax = d;
        }

        int predicted[] =
            SgtMap.predictGraphSize(1, imageWidth, imageHeight, xMin, xMax, yMin, yMax);
        Grid bath =
            transparentPng
                    || "outline".equals(currentDrawLandMask)
                    || "off".equals(currentDrawLandMask)
                    || table.nRows() == 0
                ? null
                : SgtMap.createTopographyGrid(
                    EDStatic.config.fullSgtMapTopographyCacheDirectory,
                    xMin,
                    xMax,
                    yMin,
                    yMax,
                    predicted[0],
                    predicted[1]);

        if (currentDrawLandMask == null) {
          EDV edv = zVar == null ? yVar : zVar;
          currentDrawLandMask = edv.drawLandMask(eddTable.defaultDrawLandMask(language));
        }

        if (transparentPng) {
          // fill with unusual color --> later convert to transparent
          // Not a great approach to the problem.
          transparentColor = new Color(0, 3, 1); // not common, not in grayscale, not white
          g2.setColor(transparentColor);
          g2.fillRect(0, 0, imageWidth, imageHeight);
        }

        URL bathyResourceFile =
            "under".equals(currentDrawLandMask)
                ? SgtMap.topographyCptFullName
                : SgtMap.bathymetryCptFullName; // "over": deals better with elevation ~= 0
        String bathymetryCptFullPath = File2.accessResourceFile(bathyResourceFile.toString());
        List<PrimitiveArray> mmal =
            SgtMap.makeMap(
                transparentPng,
                SgtUtil.LEGEND_BELOW,
                EDStatic.messages.legendTitle1,
                EDStatic.messages.legendTitle2,
                EDStatic.config.imageDir,
                logoImageFile,
                xMin,
                xMax,
                yMin,
                yMax, // predefined min/maxX/Y
                currentDrawLandMask,
                bath != null, // plotGridData (bathymetry)
                bath,
                1,
                1,
                0, // double gridScaleFactor, gridAltScaleFactor, gridAltOffset,
                bathymetryCptFullPath,
                null, // SgtMap.TOPOGRAPHY_BOLD_TITLE + " (" + SgtMap.TOPOGRAPHY_UNITS + ")",
                "",
                "",
                "", // MessageFormat.format(EDStatic.imageDataCourtesyOf,
                // SgtMap.TOPOGRAPHY_COURTESY)
                "off".equals(currentDrawLandMask)
                    ? SgtMap.NO_LAKES_AND_RIVERS
                    : SgtMap.FILL_LAKES_AND_RIVERS,
                false,
                null,
                1,
                1,
                1,
                "",
                null,
                "",
                "",
                "",
                "",
                "", // plot contour
                graphDataLayers,
                g2,
                0,
                0,
                imageWidth,
                imageHeight,
                0, // no boundaryResAdjust,
                fontScale);

        eddTable.writePngInfo(loggedInAs, userDapQuery, fileTypeName, mmal);

      } else {
        // create a graph

        if (transparentPng) {
          // fill with unusual color --> later convert to transparent
          // Not a great approach to the problem.
          transparentColor = new Color(0, 3, 1); // not common, not in grayscale, not white
          g2.setColor(transparentColor);
          g2.fillRect(0, 0, imageWidth, imageHeight);
        }

        List<PrimitiveArray> mmal =
            EDStatic.sgtGraph.makeGraph(
                transparentPng,
                xVar.longName() + xUnits, // x,yAxisTitle  for now, always std units
                png && drawLegend.equals(EDDTable.LEGEND_ONLY)
                    ? "."
                    : yLabel, // avoid running into legend
                SgtUtil.LEGEND_BELOW,
                EDStatic.messages.legendTitle1,
                EDStatic.messages.legendTitle2,
                EDStatic.config.imageDir,
                logoImageFile,
                xMin,
                xMax,
                xAscending,
                xVar instanceof EDVTimeStamp,
                xIsLogAxis,
                yMin,
                yMax,
                yAscending,
                yVar instanceof EDVTimeStamp,
                yIsLogAxis,
                graphDataLayers,
                g2,
                0,
                0,
                imageWidth,
                imageHeight,
                Double.NaN, // graph imageWidth/imageHeight
                bgColor,
                fontScale);

        eddTable.writePngInfo(loggedInAs, userDapQuery, fileTypeName, mmal);
      }
      if (EDDTable.debugMode) String2.log("saveAsImage 8");

      // deal with .legend and trim
      if (png && !transparentPng) {
        if (drawLegend.equals(EDDTable.LEGEND_OFF))
          bufferedImage = SgtUtil.removeLegend(bufferedImage);
        else if (drawLegend.equals(EDDTable.LEGEND_ONLY))
          bufferedImage = SgtUtil.extractLegend(bufferedImage);

        // do after removeLegend
        bufferedImage = SgtUtil.trimBottom(bufferedImage, trim);
      }
      if (EDDTable.debugMode) String2.log("saveAsImage 9");

    } catch (WaitThenTryAgainException wttae) {
      throw wttae;

    } catch (Throwable t) {
      EDStatic.rethrowClientAbortException(t); // first thing in catch{}
      ok = false;
      try {
        String msg = MustBe.getShortErrorMessage(t);
        String2.log(MustBe.throwableToString(t)); // log full message with stack trace

        if (png && drawLegend.equals(EDDTable.LEGEND_ONLY)) {
          // return a transparent 1x1 pixel image
          bufferedImage = SgtUtil.getBufferedImage(1, 1); // has white background
          transparentColor = Color.white;

        } else {
          // write exception info on image
          double tFontScale = pdf ? 1.25 : 1;
          int tHeight = Math2.roundToInt(tFontScale * 12);

          if (pdf) {
            if (pdfInfo == null)
              pdfInfo =
                  SgtUtil.createPdf(
                      SgtUtil.PDFPageSize.LETTER_PORTRAIT,
                      imageWidth,
                      imageHeight,
                      outputStreamSource.outputStream(File2.UTF_8));
            if (g2 == null) g2 = (Graphics2D) pdfInfo[0];
          } else {
            // make a new image (I don't think pdf can work this way -- sent as created)
            bufferedImage = SgtUtil.getBufferedImage(imageWidth, imageHeight);
            g2 = (Graphics2D) bufferedImage.getGraphics();
          }
          if (transparentPng) {
            // don't write the message
            // The "right" thing to do is different in different situations.
            // But e.g., No Data, should just be a transparent image.
            transparentColor = Color.white;
          } else {
            g2.setClip(0, 0, imageWidth, imageHeight); // unset in case set by sgtGraph
            msg = String2.noLongLines(msg, (imageWidth * 10 / 6) / tHeight, "    ");
            String lines[] = msg.split("\\n"); // not String2.split which trims
            g2.setColor(Color.black);
            g2.setFont(new Font(EDStatic.config.fontFamily, Font.PLAIN, tHeight));
            int ty = tHeight * 2;
            for (String line : lines) {
              g2.drawString(line, tHeight, ty);
              ty += tHeight + 2;
            }
          }
        }
      } catch (Throwable t2) {
        EDStatic.rethrowClientAbortException(t2); // first thing in catch{}
        String2.log(
            String2.ERROR + "2 while creating error image:\n" + MustBe.throwableToString(t2));
        if (pdf) {
          if (pdfInfo == null) throw t;
        } else {
          if (bufferedImage == null) throw t;
        }
        // else fall through to close/save image below
      }
    }
    if (EDDTable.debugMode) String2.log("saveAsImage 9");

    // save image
    if (pdf) {
      SgtUtil.closePdf(pdfInfo);
    } else {

      // getting the outputStream was delayed as long as possible to allow errors
      // to be detected and handled before committing to sending results to client
      SgtUtil.saveAsTransparentPng(
          bufferedImage, transparentColor, outputStreamSource.outputStream(""));
    }

    outputStreamSource.outputStream("").flush(); // safety

    if (EDDTable.reallyVerbose)
      String2.log(
          "  EDDTable.saveAsImage done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
    return ok;
  }

  /**
   * This writes the data to various types of images. This requires a dataDapQuery (not an
   * axisDapQuery) where just 1 or 2 of the dimensions be size &gt; 1. One active dimension results
   * in a graph. Two active dimensions results in a map (one active data variable results in colored
   * graph, two results in vector plot).
   *
   * <p>Note that for transparentPng maps, GoogleEarth assumes requested image will be isotropic
   * (but presumably that is what it will request).
   *
   * <p>This is public so it can be called from Erddap:doGeoServicesRest. Ideally that would use the
   * standard code path and not need to call this directly.
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param dir the directory (on this computer's hard drive) to use for temporary/cache files
   * @param fileName the name for the 'file' (no dir, no extension), which is used to write the
   *     suggested name for the file to the response header and is also used to write the
   *     [fileTypeName]Info (e.g., .pngInfo) file.
   * @param outputStreamSource
   * @param fileTypeName
   * @return true of written ok; false if exception occurred (and written on image)
   * @throws Throwable if trouble.
   */
  public boolean saveAsImage(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      String dir,
      String fileName,
      OutputStreamSource outputStreamSource,
      String fileTypeName,
      EDDGrid eddGrid)
      throws Throwable {
    if (EDDGrid.reallyVerbose) String2.log("  EDDGrid.saveAsImage query=" + userDapQuery);
    long time = System.currentTimeMillis();

    // determine the image size
    int sizeIndex =
        fileTypeName.startsWith(".small")
            ? 0
            : fileTypeName.startsWith(".medium") ? 1 : fileTypeName.startsWith(".large") ? 2 : 1;
    boolean pdf = fileTypeName.toLowerCase().endsWith("pdf");
    boolean png = fileTypeName.toLowerCase().endsWith("png");
    boolean transparentPng = fileTypeName.equals(".transparentPng");
    if (!pdf && !png)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "Unexpected image type="
              + fileTypeName);
    int imageWidth, imageHeight;
    if (pdf) {
      imageWidth = EDStatic.messages.pdfWidths[sizeIndex];
      imageHeight = EDStatic.messages.pdfHeights[sizeIndex];
    } else if (transparentPng) {
      imageWidth = EDStatic.messages.imageWidths[sizeIndex];
      imageHeight = imageWidth;
    } else {
      imageWidth = EDStatic.messages.imageWidths[sizeIndex];
      imageHeight = EDStatic.messages.imageHeights[sizeIndex];
    }
    if (EDDGrid.reallyVerbose)
      String2.log(
          "  sizeIndex="
              + sizeIndex
              + " pdf="
              + pdf
              + " imageWidth="
              + imageWidth
              + " imageHeight="
              + imageHeight);
    Object pdfInfo[] = null;
    BufferedImage bufferedImage = null;
    Graphics2D g2 = null;
    Color transparentColor =
        transparentPng ? Color.white : null; // getBufferedImage returns white background
    String drawLegend = EDDGrid.LEGEND_BOTTOM;
    int trim = Integer.MAX_VALUE;
    boolean ok = true;

    try {
      // can't handle axis request
      if (eddGrid.isAxisDapQuery(userDapQuery))
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.get(Message.QUERY_ERROR, 0)
                    + MessageFormat.format(
                        EDStatic.messages.get(Message.QUERY_ERROR_NOT_AXIS, 0), fileTypeName),
                EDStatic.messages.get(Message.QUERY_ERROR, language)
                    + MessageFormat.format(
                        EDStatic.messages.get(Message.QUERY_ERROR_NOT_AXIS, language),
                        fileTypeName)));

      // modify the query to get no more data than needed
      StringArray reqDataNames = new StringArray();
      IntArray constraints = new IntArray();
      DoubleArray inputValues = new DoubleArray();

      // TransparentPng repairs input ranges during parsing and stores raw input values in the
      // inputValues array.
      eddGrid.parseDataDapQuery(
          language,
          userDapQuery,
          reqDataNames,
          constraints,
          transparentPng /* repair */,
          inputValues);

      // for now, just plot first 1 or 2 data variables
      int nDv = reqDataNames.size();
      EDV reqDataVars[] = new EDV[nDv];
      for (int dv = 0; dv < nDv; dv++)
        reqDataVars[dv] = eddGrid.findDataVariableByDestinationName(reqDataNames.get(dv));

      // extract optional .graphicsSettings from userDapQuery
      //  xRange, yRange, color and colorbar information
      //  title2 -- a prettified constraint string
      boolean drawLines = false,
          drawLinesAndMarkers = false,
          drawMarkers = false,
          drawSticks = false,
          drawSurface = false,
          drawVectors = false;
      Color color = Color.black;

      // for now, palette values are unset.
      String palette = "";
      String scale = "";
      double paletteMin = Double.NaN;
      double paletteMax = Double.NaN;
      String continuousS = "";
      int nSections = Integer.MAX_VALUE;

      // minX/Y < maxX/Y
      double minX = Double.NaN, maxX = Double.NaN, minY = Double.NaN, maxY = Double.NaN;
      boolean xAscending = true, yAscending = true; // this is what controls flipping of the axes
      String xScale = "", yScale = ""; // (default) or Linear or Log
      int nVars = 4;
      EDV vars[] = null; // set by .vars or lower
      int axisVarI[] = null, dataVarI[] = null; // set by .vars or lower
      String ampParts[] =
          Table.getDapQueryParts(userDapQuery); // decoded.  always at least 1 part (may be "")
      boolean customSize = false;
      int markerType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE;
      int markerSize = GraphDataLayer.MARKER_SIZE_SMALL;
      double fontScale = 1, vectorStandard = Double.NaN;
      String currentDrawLandMask = null; // null = not yet set
      Color bgColor = EDStatic.config.graphBackgroundColor;
      for (String ampPart : ampParts) {
        // .bgColor
        if (ampPart.startsWith(".bgColor=")) {
          String pParts[] = String2.split(ampPart.substring(9), '|');
          if (pParts.length > 0 && pParts[0].length() > 0)
            bgColor = new Color(String2.parseInt(pParts[0]), true); // hasAlpha

          // .colorBar defaults: palette=""|continuous=C|scale=Linear|min=NaN|max=NaN|nSections=-1
        } else if (ampPart.startsWith(".colorBar=")) {
          String pParts[] =
              String2.split(ampPart.substring(10), '|'); // subparts may be ""; won't be null
          if (pParts == null) pParts = new String[0];
          if (pParts.length > 0 && pParts[0].length() > 0) palette = pParts[0];
          if (pParts.length > 1 && pParts[1].length() > 0) continuousS = pParts[1].toLowerCase();
          if (pParts.length > 2 && pParts[2].length() > 0) scale = pParts[2];
          if (pParts.length > 3 && pParts[3].length() > 0)
            paletteMin = String2.parseDouble(pParts[3]);
          if (pParts.length > 4 && pParts[4].length() > 0)
            paletteMax = String2.parseDouble(pParts[4]);
          if (pParts.length > 5 && pParts[5].length() > 0) nSections = String2.parseInt(pParts[5]);
          if (EDDGrid.reallyVerbose)
            String2.log(
                ".colorBar palette="
                    + palette
                    + " continuousS="
                    + continuousS
                    + " scale="
                    + scale
                    + " min="
                    + paletteMin
                    + " max="
                    + paletteMax
                    + " nSections="
                    + nSections);

          // .color
        } else if (ampPart.startsWith(".color=")) {
          int iColor = String2.parseInt(ampPart.substring(7));
          if (iColor < Integer.MAX_VALUE) {
            color = new Color(iColor);
            if (EDDGrid.reallyVerbose) String2.log(".color=" + String2.to0xHexString(iColor, 0));
          }

          // .draw
        } else if (ampPart.startsWith(".draw=")) {
          String gt = ampPart.substring(6);
          // try to set an option to true
          // ensure others are false in case of multiple .draw
          drawLines = gt.equals("lines");
          drawLinesAndMarkers = gt.equals("linesAndMarkers");
          drawMarkers = gt.equals("markers");
          drawSticks = gt.equals("sticks");
          drawSurface = gt.equals("surface");
          drawVectors = gt.equals("vectors");

          // .font
        } else if (ampPart.startsWith(".font=")) {
          String pParts[] =
              String2.split(ampPart.substring(6), '|'); // subparts may be ""; won't be null
          if (pParts == null) pParts = new String[0];
          if (pParts.length > 0) fontScale = String2.parseDouble(pParts[0]);
          fontScale =
              Double.isNaN(fontScale) ? 1 : fontScale < 0.1 ? 0.1 : fontScale > 10 ? 10 : fontScale;
          if (EDDGrid.reallyVerbose) String2.log(".font= scale=" + fontScale);

          // .land
        } else if (ampPart.startsWith(".land=")) {
          String gt = ampPart.substring(6);
          int which = SgtMap.drawLandMask_OPTIONS.indexOf(gt);
          if (which >= 1) currentDrawLandMask = gt;
          if (EDDGrid.reallyVerbose)
            String2.log(".land= currentDrawLandMask=" + currentDrawLandMask);

          // .legend
        } else if (ampPart.startsWith(".legend=")) {
          drawLegend = ampPart.substring(8);
          if (!drawLegend.equals(EDDGrid.LEGEND_OFF) && !drawLegend.equals(EDDGrid.LEGEND_ONLY))
            drawLegend = EDDGrid.LEGEND_BOTTOM;
          if (drawLegend.equals(EDDGrid.LEGEND_ONLY)) {
            transparentPng = false; // if it was transparent, it was already png=true, size=1
            transparentColor = null;
          }

          // .marker
        } else if (ampPart.startsWith(".marker=")) {
          String pParts[] =
              String2.split(ampPart.substring(8), '|'); // subparts may be ""; won't be null
          if (pParts == null) pParts = new String[0];
          if (pParts.length > 0) markerType = String2.parseInt(pParts[0]);
          if (pParts.length > 1) markerSize = String2.parseInt(pParts[1]);
          if (markerType < 0 || markerType >= GraphDataLayer.MARKER_TYPES.size())
            markerType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE;
          if (markerSize < 1 || markerSize > 50) markerSize = GraphDataLayer.MARKER_SIZE_SMALL;
          if (EDDGrid.reallyVerbose)
            String2.log(".marker= type=" + markerType + " size=" + markerSize);

          // .size
        } else if (ampPart.startsWith(".size=")) {
          customSize = true;
          String pParts[] =
              String2.split(ampPart.substring(6), '|'); // subparts may be ""; won't be null
          if (pParts == null) pParts = new String[0];
          if (pParts.length > 0) {
            int w = String2.parseInt(pParts[0]);
            if (w > 0 && w <= EDD.WMS_MAX_WIDTH) imageWidth = w;
          }
          if (pParts.length > 1) {
            int h = String2.parseInt(pParts[1]);
            if (h > 0 && h <= EDD.WMS_MAX_HEIGHT) imageHeight = h;
          }
          if (EDDGrid.reallyVerbose)
            String2.log(".size=  imageWidth=" + imageWidth + " imageHeight=" + imageHeight);

          // .trim
        } else if (ampPart.startsWith(".trim=")) {
          trim = String2.parseInt(ampPart.substring(6));
          if (EDDGrid.reallyVerbose) String2.log(".trim " + trim);

          // .vars    request should use this with values or don't use this; no defaults
        } else if (ampPart.startsWith(".vars=")) {
          vars = new EDV[nVars];
          axisVarI = new int[nVars];
          Arrays.fill(axisVarI, -1);
          dataVarI = new int[nVars];
          Arrays.fill(dataVarI, -1);
          String pParts[] = String2.split(ampPart.substring(6), '|');
          for (int p = 0; p < nVars; p++) {
            if (pParts.length > p && pParts[p].length() > 0) {
              int ti = String2.indexOf(eddGrid.axisVariableDestinationNames(), pParts[p]);
              if (ti >= 0) {
                vars[p] = eddGrid.axisVariables()[ti];
                axisVarI[p] = ti;
              } else if (reqDataNames.indexOf(pParts[p]) >= 0) {
                ti = String2.indexOf(eddGrid.dataVariableDestinationNames(), pParts[p]);
                vars[p] = eddGrid.dataVariables()[ti];
                dataVarI[p] = ti;
              } else {
                throw new SimpleException(
                    EDStatic.bilingual(
                        language,
                        EDStatic.messages.get(Message.QUERY_ERROR, 0)
                            + MessageFormat.format(
                                EDStatic.messages.get(Message.QUERY_ERROR_UNKNOWN_VARIABLE, 0),
                                pParts[p]),
                        EDStatic.messages.get(Message.QUERY_ERROR, language)
                            + MessageFormat.format(
                                EDStatic.messages.get(
                                    Message.QUERY_ERROR_UNKNOWN_VARIABLE, language),
                                pParts[p])));
              }
            }
          }

          // .vec
        } else if (ampPart.startsWith(".vec=")) {
          vectorStandard = String2.parseDouble(ampPart.substring(5));
          if (EDDGrid.reallyVerbose) String2.log(".vec " + vectorStandard);

          // .xRange   (supported, but currently not created by the Make A Graph form)
          //  prefer set via xVar constraints
        } else if (ampPart.startsWith(".xRange=")) {
          String pParts[] = String2.split(ampPart.substring(8), '|');
          if (pParts.length > 0) minX = String2.parseDouble(pParts[0]);
          if (pParts.length > 1) maxX = String2.parseDouble(pParts[1]);
          if (pParts.length > 2)
            // if the param slot is there, the param determines Ascending
            xAscending = String2.parseBoolean(pParts[2]); // "" -> true(the default)
          if (pParts.length > 3) {
            // if the param slot is there, the param determines Scale
            xScale = String2.toTitleCase(pParts[3].trim()); // "" -> (the default)
            if (!xScale.equals("Log") && !xScale.equals("Linear"))
              xScale = ""; // "" -> (the default)
          }
          if (EDDGrid.reallyVerbose)
            String2.log(
                ".xRange min="
                    + minX
                    + " max="
                    + maxX
                    + " ascending="
                    + xAscending
                    + " scale="
                    + xScale);

          // .yRange   (supported, as of 2010-10-22 it's on the Make A Graph form)
          //  prefer set via yVar constraints
        } else if (ampPart.startsWith(".yRange=")) {
          String pParts[] = String2.split(ampPart.substring(8), '|');
          if (pParts.length > 0) minY = String2.parseDouble(pParts[0]);
          if (pParts.length > 1) maxY = String2.parseDouble(pParts[1]);
          if (pParts.length > 2)
            // if the param slot is there, the param determines Ascending
            yAscending = String2.parseBoolean(pParts[2]); // "" -> true(the default)
          if (pParts.length > 3) {
            // if the param slot is there, the param determines Scale
            yScale = String2.toTitleCase(pParts[3].trim()); // "" -> (the default)
            if (!yScale.equals("Log") && !yScale.equals("Linear"))
              yScale = ""; // "" -> (the default)
          }
          if (EDDGrid.reallyVerbose)
            String2.log(
                ".yRange min="
                    + minY
                    + " max="
                    + maxY
                    + " ascending="
                    + yAscending
                    + " scale="
                    + yScale);

          // just to be clear: ignore any unrecognized .something
        }
      }
      boolean reallySmall = imageWidth < 260; // .smallPng is 240

      // figure out which axes are active (>1 value)
      IntArray activeAxes = new IntArray();
      for (int av = 0; av < eddGrid.axisVariables().length; av++)
        if (constraints.get(av * 3) < constraints.get(av * 3 + 2)) activeAxes.add(av);
      int nAAv = activeAxes.size();
      if (nAAv < 1 || nAAv > 2)
        throw new SimpleException(
            EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                + "To draw a graph, either 1 or 2 axes must be active and have 2 or more values.");

      // figure out / validate graph set up
      // if .draw= was provided...
      int cAxisI = 0, cDataI = 0; // use them up as needed
      if (drawLines) {
        if (vars == null) { // use default var selection
          vars = new EDV[nVars];
          for (int v = 0; v < 2; v++) { // get 2 vars
            if (nAAv > cAxisI) vars[v] = eddGrid.axisVariables()[activeAxes.get(cAxisI++)];
            else if (nDv > cDataI) vars[v] = reqDataVars[cDataI++];
            else
              throw new SimpleException(
                  EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                      + "Too few active axes and/or data variables for .draw=lines.");
          }
        } else {
          // vars 0,1 must be valid (any type)
          if (vars[0] == null)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For .draw=lines, .var #0 is required.");
          if (vars[1] == null)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For .draw=lines, .var #1 is required.");
        }
        vars[2] = null;
        vars[3] = null;
      } else if (drawLinesAndMarkers || drawMarkers) {
        String what = drawLinesAndMarkers ? "linesAndMarkers" : "markers";
        if (vars == null) { // use default var selection
          vars = new EDV[nVars];
          for (int v = 0; v < 3; v++) { // get 2 or 3 vars
            if (nAAv > cAxisI) vars[v] = eddGrid.axisVariables()[activeAxes.get(cAxisI++)];
            else if (nDv > cDataI) vars[v] = reqDataVars[cDataI++];
            else if (v < 2)
              throw new SimpleException(
                  EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                      + "Too few active axes and/or data variables for .draw="
                      + what
                      + ".");
          }
        } else {
          // vars 0,1 must be valid (any type)
          if (vars[0] == null)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For .draw="
                    + what
                    + ", .var #0 is required.");
          if (vars[1] == null)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For .draw="
                    + what
                    + ", .var #1 is required.");
        }
        vars[3] = null;
      } else if (drawSticks) {
        if (vars == null) { // use default var selection
          vars = new EDV[nVars];
          // var0 must be axis
          if (nAAv > 0) vars[0] = eddGrid.axisVariables()[activeAxes.get(cAxisI++)];
          else
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + ".draw=sticks requires an active axis variable.");
          // var 1,2 must be data
          for (int v = 1; v <= 2; v++) {
            if (nDv > cDataI) vars[v] = reqDataVars[cDataI++];
            else
              throw new SimpleException(
                  EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                      + "Too few data variables to .draw=sticks.");
          }
        } else {
          // vars 0 must be axis, 1,2 must be data
          if (axisVarI[0] < 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For .draw=sticks, .var #0 must be an axis variable.");
          if (dataVarI[1] < 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For .draw=sticks, .var #1 must be a data variable.");
          if (dataVarI[2] < 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For .draw=sticks, .var #2 must be a data variable.");
        }
        vars[3] = null;
      } else if (drawSurface) {
        if (vars == null) { // use default var selection
          vars = new EDV[nVars];
          // var0,1 must be axis, prefer lon,lat
          if (activeAxes.indexOf("" + eddGrid.lonIndex()) >= 0
              && activeAxes.indexOf("" + eddGrid.latIndex()) >= 0) {
            vars[0] = eddGrid.axisVariables()[eddGrid.lonIndex()];
            vars[1] = eddGrid.axisVariables()[eddGrid.latIndex()];
          } else if (nAAv < 2) {
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + ".draw=surface requires 2 axes with >1 value.");
          } else {
            // prefer last 2 axes (e.g., if [time][altitude][y][x]
            vars[0] = eddGrid.axisVariables()[activeAxes.get(nAAv - 1)];
            vars[1] = eddGrid.axisVariables()[activeAxes.get(nAAv - 2)];
          }
          // var 2 must be data
          vars[2] = reqDataVars[cDataI++]; // at least one is valid
        } else {
          // vars 0,1 must be axis, 2 must be data
          if (axisVarI[0] < 0 || axisVarI[1] < 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For .draw=surface, .var #0 and #1 must be axis variables.");
          if (axisVarI[0] == axisVarI[1])
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For .draw=surface, .var #0 and #1 must be different axis variables.");
          if (dataVarI[2] < 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For .draw=surface, .var #2 must be a data variable.");
        }
        vars[3] = null;
      } else if (drawVectors) {
        if (vars == null) { // use default var selection
          vars = new EDV[nVars];
          // var0,1 must be axes
          if (nAAv == 2) {
            vars[0] = eddGrid.axisVariables()[activeAxes.get(0)];
            vars[1] = eddGrid.axisVariables()[activeAxes.get(1)];
          } else
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + ".draw=vectors requires 2 active axis variables.");
          // var2,3 must be data
          if (nDv == 2) {
            vars[2] = reqDataVars[0];
            vars[3] = reqDataVars[1];
          } else
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + ".draw=vectors requires 2 data variables.");
        } else {
          // vars 0,1 must be axes, 2,3 must be data
          if (axisVarI[0] < 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For .draw=vectors, .var #0 must be an axis variable.");
          if (axisVarI[1] < 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For .draw=vectors, .var #1 must be an axis variable.");
          if (dataVarI[2] < 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For .draw=vectors, .var #2 must be a data variable.");
          if (dataVarI[3] < 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For .draw=vectors, .var #3 must be a data variable.");
        }

      } else if (vars == null) {
        // neither .vars nor .draw were provided
        // detect from OPeNDAP request
        vars = new EDV[nVars];
        if (nAAv == 0) {
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "At least 1 axis variable must be active and have a range of values.");
        } else if (nAAv == 1) { // favor linesAndMarkers
          drawLinesAndMarkers = true;
          vars[0] = eddGrid.axisVariables()[activeAxes.get(0)];
          vars[1] = reqDataVars[0];
          if (nDv > 1) vars[2] = reqDataVars[1];
        } else if (nAAv == 2) { // favor Surface
          // if lon lat in dataset
          if (eddGrid.lonIndex() >= 0
              && eddGrid.latIndex() >= 0
              && activeAxes.indexOf(eddGrid.lonIndex()) >= 0
              && activeAxes.indexOf(eddGrid.latIndex()) >= 0) {
            vars[0] = eddGrid.axisVariables()[eddGrid.lonIndex()];
            vars[1] = eddGrid.axisVariables()[eddGrid.latIndex()];
            vars[2] = reqDataVars[0];
            if (reqDataVars.length >= 2) {
              // draw vectors
              drawVectors = true;
              vars[3] = reqDataVars[1];
            } else {
              // draw surface
              drawSurface = true;
            }

          } else {
            // use last 2 axis vars (e.g., when time,alt,y,x)
            drawSurface = true;
            vars[0] = eddGrid.axisVariables()[activeAxes.get(nAAv - 1)];
            vars[1] = eddGrid.axisVariables()[activeAxes.get(nAAv - 2)];
            vars[2] = reqDataVars[0];
          }
        } else {
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "Either 1 or 2 axes must be active and have a range of values.");
        }
      } else {
        // .vars was provided, .draw wasn't
        // look for drawVector (2 axisVars + 2 dataVars)
        if (axisVarI[0] >= 0 && axisVarI[1] >= 0 && dataVarI[2] >= 0 && dataVarI[3] >= 0) {
          // ??? require map (lat lon), not graph???
          drawVectors = true;

          // look for drawSurface (2 axisVars + 1 dataVar)
        } else if (axisVarI[0] >= 0 && axisVarI[1] >= 0 && dataVarI[2] >= 0 && dataVarI[3] < 0) {
          drawSurface = true;

          // drawMarker
        } else {
          // ensure marker compatible
          if (axisVarI[0] < 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + ".var #0 must be an axis variable.");
          if (axisVarI[1] < 0 && dataVarI[1] < 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + ".var #1 must be an axis or a data variable.");
          axisVarI[1] = -1;
          // var2 may be a dataVar or ""
          vars[3] = null;
          drawLinesAndMarkers = true;
        }
      }

      boolean isMap = vars[0] instanceof EDVLonGridAxis && vars[1] instanceof EDVLatGridAxis;
      boolean xIsTimeAxis =
          vars[0] instanceof EDVTimeStampGridAxis || vars[0] instanceof EDVTimeStamp;
      boolean yIsTimeAxis =
          vars[1] instanceof EDVTimeStampGridAxis || vars[1] instanceof EDVTimeStamp;
      if (xScale.length() == 0) {
        // use the default from colorBarScale
        xScale = vars[0].combinedAttributes().getString(language, "colorBarScale");
        xScale = xScale == null ? "" : String2.toTitleCase(xScale.trim());
        if (!xScale.equals("Log")) xScale = "Linear"; // apply "" default -> Linear
      }
      boolean xIsLogAxis = !xIsTimeAxis && xScale.equals("Log");
      if (yScale.length() == 0) {
        // use the default from colorBarScale
        yScale = vars[1].combinedAttributes().getString(language, "colorBarScale");
        yScale = yScale == null ? "" : String2.toTitleCase(yScale.trim());
        if (!yScale.equals("Log")) yScale = "Linear"; // apply "" default -> Linear
      }
      boolean yIsLogAxis = !yIsTimeAxis && yScale.equals("Log");

      int xAxisIndex =
          String2.indexOf(eddGrid.axisVariableDestinationNames(), vars[0].destinationName());
      int yAxisIndex =
          String2.indexOf(eddGrid.axisVariableDestinationNames(), vars[1].destinationName());

      // if map or coloredSurface, modify the constraints so as to get only minimal amount of data
      // if 1D graph, no restriction
      EDVGridAxis xAxisVar = null;
      int minXIndex = -1, maxXIndex = -1;
      if (xAxisIndex < 0)
        // It is probably possible to not require x be an axis var,
        // but it is currently an assumption and what drives the creation of all graphs.
        // And the GUI always sets it up this way.
        throw new SimpleException(
            EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                + "The variable assigned to the x axis ("
                + vars[0].destinationName()
                + ") must be an axis variable.");
      if (xAxisIndex >= 0) {
        xAxisVar = eddGrid.axisVariables()[xAxisIndex];
        minXIndex = constraints.get(xAxisIndex * 3);
        maxXIndex = constraints.get(xAxisIndex * 3 + 2);
        if (Double.isNaN(minX)) minX = xAxisVar.destinationValue(minXIndex).getNiceDouble(0);
        if (Double.isNaN(maxX)) maxX = xAxisVar.destinationValue(maxXIndex).getNiceDouble(0);
      }
      if (minX > maxX) { // can only be true if both are finite
        double d = minX;
        minX = maxX;
        maxX = d;
      }

      int minYIndex = -1, maxYIndex = -1;
      EDVGridAxis yAxisVar = yAxisIndex >= 0 ? eddGrid.axisVariables()[yAxisIndex] : null;
      double minData = Double.NaN, maxData = Double.NaN;

      if (drawSurface || drawVectors) {
        if (yAxisVar == null) // because yAxisIndex < 0
        throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "The variable assigned to the y axis ("
                  + vars[0].destinationName()
                  + ") must be an axis variable.");
        minYIndex = constraints.get(yAxisIndex * 3);
        maxYIndex = constraints.get(yAxisIndex * 3 + 2);
        if (Double.isNaN(minY)) minY = yAxisVar.destinationValue(minYIndex).getNiceDouble(0);
        if (Double.isNaN(maxY)) maxY = yAxisVar.destinationValue(maxYIndex).getNiceDouble(0);
        if (minY > maxY) {
          double d = minY;
          minY = maxY;
          maxY = d;
        }

        if (transparentPng && drawSurface && !customSize) {

          // This is the one situation to change imageWidth/Height to ~1 pixel/lon or lat
          int have = maxXIndex - minXIndex + 1;
          int stride = constraints.get(xAxisIndex * 3 + 1);
          imageWidth = DataHelper.strideWillFind(have, stride);
          // protect against huge .png (and huge amount of data in memory)
          if (imageWidth > 3601) {
            stride = DataHelper.findStride(have, 3601);
            imageWidth = DataHelper.strideWillFind(have, stride);
            constraints.set(xAxisIndex * 3 + 1, stride);
            if (EDDGrid.reallyVerbose) String2.log("  xStride reduced to stride=" + stride);
          }
          have = maxYIndex - minYIndex + 1;
          stride = constraints.get(yAxisIndex * 3 + 1);
          imageHeight = DataHelper.strideWillFind(have, stride);
          if (imageHeight > 1801) {
            stride = DataHelper.findStride(have, 1801);
            imageHeight = DataHelper.strideWillFind(have, stride);
            constraints.set(yAxisIndex * 3 + 1, stride);
            if (EDDGrid.reallyVerbose) String2.log("  yStride reduced to stride=" + stride);
          }

        } else {
          // calculate/fix up stride so as to get enough data (but not too much)
          // find size of map or graph
          int activeWidth = imageWidth - 50; // decent guess for drawSurface
          int activeHeight = imageHeight - 75;

          if (drawVectors) {

            double maxXY = Math.max(maxX - minX, maxY - minY);
            double vecInc =
                SgtMap.suggestVectorIncrement( // e.g. 2 degrees
                    maxXY, Math.max(imageWidth, imageHeight), fontScale);

            activeWidth =
                Math.max(5, Math2.roundToInt((maxX - minX) / vecInc)); // e.g., 20 deg / 2 deg -> 10
            activeHeight = Math.max(5, Math2.roundToInt((maxY - minY) / vecInc));

          } else { // drawSurface;

            if (transparentPng) {
              activeWidth = imageWidth;
              activeHeight = imageHeight;

            } else if (isMap) {
              int wh[] =
                  SgtMap.predictGraphSize(
                      fontScale, imageWidth, imageHeight, minX, maxX, minY, maxY);
              activeWidth = wh[0];
              activeHeight = wh[1];

            } else {
              activeWidth = imageWidth;
              activeHeight = imageHeight;
            }
          }

          // calculate/fix up stride so as to get enough data (but not too much)
          int have = maxXIndex - minXIndex + 1;
          int stride = DataHelper.findStride(have, activeWidth);
          constraints.set(xAxisIndex * 3 + 1, stride);
          if (EDDGrid.reallyVerbose)
            String2.log(
                "  xStride="
                    + stride
                    + " activeHeight="
                    + activeHeight
                    + " strideWillFind="
                    + DataHelper.strideWillFind(have, stride));

          have = maxYIndex - minYIndex + 1;
          stride = DataHelper.findStride(have, activeHeight);
          constraints.set(yAxisIndex * 3 + 1, stride);
          if (EDDGrid.reallyVerbose)
            String2.log(
                "  yStride="
                    + stride
                    + " activeHeight="
                    + activeHeight
                    + " strideWillFind="
                    + DataHelper.strideWillFind(have, stride));
        }
      }

      // units
      String xUnits = vars[0].units();
      String yUnits = vars[1].units();
      String zUnits = vars[2] == null ? null : vars[2].units();
      String tUnits = vars[3] == null ? null : vars[3].units();
      xUnits = xUnits == null ? "" : " (" + xUnits + ")";
      yUnits = yUnits == null ? "" : " (" + yUnits + ")";
      zUnits = zUnits == null ? "" : " (" + zUnits + ")";
      tUnits = tUnits == null ? "" : " (" + tUnits + ")";

      // get the desctiptive info for the other axes (the ones with 1 value)
      StringBuilder otherInfo = new StringBuilder();
      for (int av = 0; av < eddGrid.axisVariables().length; av++) {
        if (av != xAxisIndex && av != yAxisIndex) {
          int ttIndex = constraints.get(av * 3);
          EDVGridAxis axisVar = eddGrid.axisVariables()[av];
          if (otherInfo.length() > 0) otherInfo.append(", ");
          double td = axisVar.destinationValue(ttIndex).getNiceDouble(0);
          if (av == eddGrid.lonIndex()) otherInfo.append(td + "°E");
          else if (av == eddGrid.latIndex()) otherInfo.append(td + "°N");
          else if (axisVar instanceof EDVTimeStampGridAxis)
            otherInfo.append(
                Calendar2.epochSecondsToLimitedIsoStringT(
                    axisVar.combinedAttributes().getString(language, EDV.TIME_PRECISION),
                    td,
                    "NaN"));
          else {
            String avDN = axisVar.destinationName();
            String avLN = axisVar.longName();
            String avUnits = axisVar.units();
            otherInfo.append(
                (avLN.length() <= 12 || avLN.length() <= avDN.length() ? avLN : avDN)
                    + "="
                    + td
                    + (avUnits == null ? "" : " " + avUnits));
          }
        }
      }
      if (otherInfo.length() > 0) {
        otherInfo.insert(0, "(");
        otherInfo.append(")");
      }

      // prepare to get the data
      StringArray newReqDataNames = new StringArray();
      int nBytesPerElement = 0;
      for (int v = 0; v < nVars; v++) {
        if (vars[v] != null && !(vars[v] instanceof EDVGridAxis)) {
          newReqDataNames.add(vars[v].destinationName());
          nBytesPerElement +=
              drawSurface
                  ? 8
                  : // grid always stores data in double[]
                  vars[v].destinationBytesPerElement();
        }
      }
      String newQuery = EDDGrid.buildDapQuery(newReqDataNames, constraints);
      if (EDDGrid.reallyVerbose) String2.log("  newQuery=" + newQuery);

      Grid grid = null;
      Table table = null;
      GraphDataLayer graphDataLayer = null;
      ArrayList<GraphDataLayer> graphDataLayers = new ArrayList<>();
      String cptFullName = null;
      try ( // Table needs row-major order
      GridDataAccessor gda =
          new GridDataAccessor(
              language,
              eddGrid,
              requestUrl,
              newQuery,
              yAxisVar == null
                  || yAxisIndex
                      > xAxisIndex, // Grid needs column-major order (so depends on axis order)
              // //??? what if xAxisIndex < 0???
              true)) { // convertToNaN
        long requestNL = gda.totalIndex().size();
        Math2.ensureArraySizeOkay(requestNL, "EDDGrid.saveAsImage");
        Math2.ensureMemoryAvailable(requestNL * nBytesPerElement, "EDDGrid.saveAsImage");
        int requestN = (int) requestNL; // safe since checked above

        if (drawVectors) {
          // put the data in a Table   0=xAxisVar 1=yAxisVar 2=dataVar1 3=dataVar2
          if (yAxisVar == null) // because yAxisIndex < 0      //redundant, since tested above
          throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "The variable assigned to the y axis ("
                    + vars[0].destinationName()
                    + ") must be an axis variable.");
          table = new Table();
          PrimitiveArray xpa =
              PrimitiveArray.factory(vars[0].destinationDataPAType(), requestN, false);
          PrimitiveArray ypa =
              PrimitiveArray.factory(vars[1].destinationDataPAType(), requestN, false);
          PrimitiveArray zpa =
              PrimitiveArray.factory(vars[2].destinationDataPAType(), requestN, false);
          PrimitiveArray tpa =
              PrimitiveArray.factory(vars[3].destinationDataPAType(), requestN, false);
          table.addColumn(vars[0].destinationName(), xpa);
          table.addColumn(vars[1].destinationName(), ypa);
          table.addColumn(vars[2].destinationName(), zpa);
          table.addColumn(vars[3].destinationName(), tpa);
          PAOne xPAOne = new PAOne(xpa);
          PAOne yPAOne = new PAOne(ypa);
          PAOne zPAOne = new PAOne(zpa);
          PAOne tPAOne = new PAOne(tpa);
          while (gda.increment()) {
            gda.getAxisValueAsPAOne(xAxisIndex, xPAOne).addTo(xpa);
            gda.getAxisValueAsPAOne(yAxisIndex, yPAOne).addTo(ypa);
            gda.getDataValueAsPAOne(0, zPAOne).addTo(zpa);
            gda.getDataValueAsPAOne(1, tPAOne).addTo(tpa);
          }
          if (Double.isNaN(vectorStandard)) {
            double stats1[] = zpa.calculateStats();
            double stats2[] = tpa.calculateStats();
            double lh[] =
                Math2.suggestLowHigh(
                    0,
                    Math.max( // suggestLowHigh handles NaNs
                        Math.abs(stats1[PrimitiveArray.STATS_MAX]),
                        Math.abs(stats2[PrimitiveArray.STATS_MAX])));
            vectorStandard = lh[1];
          }

          String varInfo =
              vars[2].longName()
                  + (zUnits.equals(tUnits) ? "" : zUnits)
                  + ", "
                  + vars[3].longName()
                  + " ("
                  + (float) vectorStandard
                  + (tUnits.length() == 0 ? "" : " " + vars[3].units())
                  + ")";

          // make a graphDataLayer with data  time series line
          graphDataLayer =
              new GraphDataLayer(
                  -1, // which pointScreen
                  0,
                  1,
                  2,
                  3,
                  1, // x,y,z1,z2,z3 column numbers
                  GraphDataLayer.DRAW_POINT_VECTORS,
                  xIsTimeAxis,
                  yIsTimeAxis,
                  vars[0].longName() + xUnits,
                  vars[1].longName() + yUnits,
                  varInfo,
                  eddGrid.title(language),
                  otherInfo.toString(),
                  MessageFormat.format(
                      EDStatic.messages.get(Message.IMAGE_DATA_COURTESY_OF, language),
                      eddGrid.institution(language)),
                  table,
                  null,
                  null,
                  null,
                  color,
                  GraphDataLayer.MARKER_TYPE_NONE,
                  0,
                  vectorStandard,
                  GraphDataLayer.REGRESS_NONE);
          graphDataLayers.add(graphDataLayer);

        } else if (drawSticks) {
          // put the data in a Table   0=xAxisVar 1=uDataVar 2=vDataVar
          table = new Table();
          PrimitiveArray xpa =
              PrimitiveArray.factory(vars[0].destinationDataPAType(), requestN, false);
          PrimitiveArray ypa =
              PrimitiveArray.factory(vars[1].destinationDataPAType(), requestN, false);
          PrimitiveArray zpa =
              PrimitiveArray.factory(vars[2].destinationDataPAType(), requestN, false);
          table.addColumn(vars[0].destinationName(), xpa);
          table.addColumn(vars[1].destinationName(), ypa);
          table.addColumn(vars[2].destinationName(), zpa);
          PAOne xPAOne = new PAOne(xpa);
          PAOne yPAOne = new PAOne(ypa);
          PAOne zPAOne = new PAOne(zpa);
          while (gda.increment()) {
            gda.getAxisValueAsPAOne(xAxisIndex, xPAOne).addTo(xpa);
            gda.getDataValueAsPAOne(0, yPAOne).addTo(ypa);
            gda.getDataValueAsPAOne(1, zPAOne).addTo(zpa);
          }

          String varInfo =
              vars[1].longName()
                  + (yUnits.equals(zUnits) ? "" : yUnits)
                  + ", "
                  + vars[2].longName()
                  + (zUnits.length() == 0 ? "" : zUnits);

          // make a graphDataLayer with data  time series line
          graphDataLayer =
              new GraphDataLayer(
                  -1, // which pointScreen
                  0,
                  1,
                  2,
                  1,
                  1, // x,y,z1,z2,z3 column numbers
                  GraphDataLayer.DRAW_STICKS,
                  xIsTimeAxis,
                  yIsTimeAxis,
                  vars[0].longName() + xUnits,
                  varInfo,
                  eddGrid.title(language),
                  otherInfo.toString(),
                  "",
                  MessageFormat.format(
                      EDStatic.messages.get(Message.IMAGE_DATA_COURTESY_OF, language),
                      eddGrid.institution(language)),
                  table,
                  null,
                  null,
                  null,
                  color,
                  GraphDataLayer.MARKER_TYPE_NONE,
                  0,
                  1,
                  GraphDataLayer.REGRESS_NONE);
          graphDataLayers.add(graphDataLayer);

        } else if (isMap || drawSurface) {
          // if .colorBar info didn't provide info, try to get defaults from vars[2] colorBarXxx
          // attributes
          if (yAxisVar == null) // because yAxisIndex < 0
          throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "The variable assigned to the y axis ("
                    + vars[0].destinationName()
                    + ") must be an axis variable.");
          if (vars[2] != null) { // it shouldn't be
            LocalizedAttributes colorVarAtts = vars[2].combinedAttributes();
            if (palette.length() == 0)
              palette = colorVarAtts.getString(language, "colorBarPalette");
            if (scale.length() == 0) scale = colorVarAtts.getString(language, "colorBarScale");
            if (nSections == Integer.MAX_VALUE)
              nSections = colorVarAtts.getInt(language, "colorBarNSections");
            if (Double.isNaN(paletteMin))
              paletteMin = colorVarAtts.getDouble(language, "colorBarMinimum");
            if (Double.isNaN(paletteMax))
              paletteMax = colorVarAtts.getDouble(language, "colorBarMaximum");
            String ts = colorVarAtts.getString(language, "colorBarContinuous");
            if (continuousS.length() == 0 && ts != null)
              continuousS = String2.parseBoolean(ts) ? "c" : "d"; // defaults to true
          }

          if (String2.indexOf(EDStatic.messages.palettes, palette) < 0) palette = "";
          if (EDV.VALID_SCALES.indexOf(scale) < 0) scale = "Linear";
          if (nSections < 0 || nSections >= 100) nSections = -1;
          boolean continuous = !continuousS.startsWith("d");

          // put the data in a Grid, data in column-major order
          grid = new Grid();
          grid.data = new double[requestN];
          if (png
              && drawLegend.equals(EDDGrid.LEGEND_ONLY)
              && palette.length() > 0
              && !Double.isNaN(paletteMin)
              && !Double.isNaN(paletteMax)) {

            // legend=Only and palette range is known, so don't need to get the data
            if (EDDGrid.reallyVerbose) String2.log("***LEGEND ONLY: SO NOT GETTING THE DATA");
            Arrays.fill(grid.data, Double.NaN); // safe for all situations

          } else {

            // get the data
            int po = 0;
            while (gda.increment()) grid.data[po++] = gda.getDataValueAsDouble(0);
          }
          // get the x axis "lon" values
          PrimitiveArray tpa = gda.axisValues(xAxisIndex);
          int tn = tpa.size();
          grid.lon = new double[tn];
          for (int i = 0; i < tn; i++) grid.lon[i] = tpa.getDouble(i);
          grid.lonSpacing = (grid.lon[tn - 1] - grid.lon[0]) / Math.max(1, tn - 1);

          // get the y axis "lat" values
          tpa = gda.axisValues(yAxisIndex);
          tn = tpa.size();
          // String2.log(">>gdaYsize=" + tn);
          grid.lat = new double[tn];
          for (int i = 0; i < tn; i++) grid.lat[i] = tpa.getDouble(i);
          grid.latSpacing = (grid.lat[tn - 1] - grid.lat[0]) / Math.max(1, tn - 1);

          // cptFullName
          if (Double.isNaN(paletteMin) || Double.isNaN(paletteMax)) {
            // if not specified, I have the right to change
            DoubleArray da = new DoubleArray(grid.data);
            double stats[] = da.calculateStats();
            minData = stats[PrimitiveArray.STATS_MIN];
            maxData = stats[PrimitiveArray.STATS_MAX];
            if (maxData >= minData / -2 && maxData <= minData * -2) {
              double td = Math.max(maxData, -minData);
              minData = -td;
              maxData = td;
            }
            double tRange[] = Math2.suggestLowHigh(minData, maxData);
            minData = tRange[0];
            maxData = tRange[1];
            if (maxData >= minData / -2 && maxData <= minData * -2) {
              double td = Math.max(maxData, -minData);
              minData = -td;
              maxData = td;
            }
            if (Double.isNaN(paletteMin)) paletteMin = minData;
            if (Double.isNaN(paletteMax)) paletteMax = maxData;
          }
          if (paletteMin > paletteMax) {
            double d = paletteMin;
            paletteMin = paletteMax;
            paletteMax = d;
          }
          if (paletteMin == paletteMax) {
            double tRange[] = Math2.suggestLowHigh(paletteMin, paletteMax);
            paletteMin = tRange[0];
            paletteMax = tRange[1];
          }
          if (palette.length() == 0)
            palette = Math2.almostEqual(3, -paletteMin, paletteMax) ? "BlueWhiteRed" : "Rainbow";
          if (scale.length() == 0) scale = "Linear";
          cptFullName =
              CompoundColorMap.makeCPT(
                  EDStatic.config.fullPaletteDirectory,
                  palette,
                  scale,
                  paletteMin,
                  paletteMax,
                  nSections,
                  continuous,
                  EDStatic.config.fullCptCacheDirectory);

          // make a graphDataLayer with coloredSurface setup
          graphDataLayer =
              new GraphDataLayer(
                  -1, // which pointScreen
                  0,
                  1,
                  1,
                  1,
                  1, // x,y,z1,z2,z3 column numbers    irrelevant
                  GraphDataLayer.DRAW_COLORED_SURFACE, // AND_CONTOUR_LINE?
                  xIsTimeAxis,
                  yIsTimeAxis,
                  (reallySmall ? vars[0].destinationName() : vars[0].longName())
                      + xUnits, // x,yAxisTitle  for now, always std units
                  (reallySmall ? vars[1].destinationName() : vars[1].longName()) + yUnits,
                  (reallySmall ? vars[2].destinationName() : vars[2].longName())
                      + zUnits, // boldTitle
                  eddGrid.title(language),
                  otherInfo.toString(),
                  MessageFormat.format(
                      EDStatic.messages.get(Message.IMAGE_DATA_COURTESY_OF, language),
                      eddGrid.institution(language)),
                  null,
                  grid,
                  null,
                  new CompoundColorMap(cptFullName),
                  color, // color is irrelevant
                  -1,
                  -1, // marker type, size
                  0, // vectorStandard
                  GraphDataLayer.REGRESS_NONE);
          graphDataLayers.add(graphDataLayer);

        } else { // make graph with lines, linesAndMarkers, or markers
          // put the data in a Table   x,y,(z)
          table = new Table();
          PrimitiveArray xpa =
              PrimitiveArray.factory(vars[0].destinationDataPAType(), requestN, false);
          PrimitiveArray ypa =
              PrimitiveArray.factory(vars[1].destinationDataPAType(), requestN, false);
          PrimitiveArray zpa =
              vars[2] == null
                  ? null
                  : PrimitiveArray.factory(vars[2].destinationDataPAType(), requestN, false);
          table.addColumn(vars[0].destinationName(), xpa);
          table.addColumn(vars[1].destinationName(), ypa);

          if (vars[2] != null) {
            table.addColumn(vars[2].destinationName(), zpa);

            // if .colorBar info didn't provide info, try to get defaults from vars[2] colorBarXxx
            // attributes
            LocalizedAttributes colorVarAtts = vars[2].combinedAttributes();
            if (palette.length() == 0)
              palette = colorVarAtts.getString(language, "colorBarPalette");
            if (scale.length() == 0) scale = colorVarAtts.getString(language, "colorBarScale");
            if (nSections == Integer.MAX_VALUE)
              nSections = colorVarAtts.getInt(language, "colorBarNSections");
            if (Double.isNaN(paletteMin))
              paletteMin = colorVarAtts.getDouble(language, "colorBarMinimum");
            if (Double.isNaN(paletteMax))
              paletteMax = colorVarAtts.getDouble(language, "colorBarMaximum");
            String ts = colorVarAtts.getString(language, "colorBarContinuous");
            if (continuousS.length() == 0 && ts != null)
              continuousS = String2.parseBoolean(ts) ? "c" : "d"; // defaults to true

            if (String2.indexOf(EDStatic.messages.palettes, palette) < 0) palette = "";
            if (EDV.VALID_SCALES.indexOf(scale) < 0) scale = "Linear";
            if (nSections < 0 || nSections >= 100) nSections = -1;
          }

          PAOne xpaPAOne = new PAOne(xpa);
          PAOne ypaPAOne = new PAOne(ypa);
          PAOne zpaPAOne = zpa == null ? null : new PAOne(zpa);
          if (png
              && drawLegend.equals(EDDGrid.LEGEND_ONLY)
              && (vars[2] == null
                  || (palette.length() > 0
                      && !Double.isNaN(paletteMin)
                      && !Double.isNaN(paletteMax)))) {

            // legend=Only and (no color var or palette range is known), so don't need to get the
            // data
            if (EDDGrid.reallyVerbose) String2.log("***LEGEND ONLY: SO NOT GETTING THE DATA");
            xpaPAOne.setDouble(Double.NaN).addTo(xpa);
            ypaPAOne.setDouble(Double.NaN).addTo(ypa);
            if (vars[2] != null) zpaPAOne.setDouble(Double.NaN).addTo(zpa);

          } else {
            // need to get the data
            while (gda.increment()) {
              gda.getAxisValueAsPAOne(xAxisIndex, xpaPAOne).addTo(xpa);
              if (yAxisIndex >= 0) gda.getAxisValueAsPAOne(yAxisIndex, ypaPAOne).addTo(ypa);
              else gda.getDataValueAsPAOne(0, ypaPAOne).addTo(ypa);
              if (vars[2] != null)
                gda.getDataValueAsPAOne(yAxisIndex >= 0 ? 0 : 1, zpaPAOne)
                    .addTo(zpa); // yAxisIndex>=0 is true if y is axisVariable
            }
          }

          // make the colorbar
          CompoundColorMap colorMap = null;
          if (vars[2] != null) {
            boolean continuous = !continuousS.startsWith("d");

            if (palette.length() == 0 || Double.isNaN(paletteMin) || Double.isNaN(paletteMax)) {
              // set missing items based on z data
              double zStats[] = table.getColumn(2).calculateStats();
              if (zStats[PrimitiveArray.STATS_N] > 0) {
                double minMax[];
                if (vars[2] instanceof EDVTimeStamp) {
                  // ???I think this is too crude. Smarter code elsewhere? Or handled by
                  // compoundColorMap?
                  double r20 =
                      (zStats[PrimitiveArray.STATS_MAX] - zStats[PrimitiveArray.STATS_MIN]) / 20;
                  minMax =
                      new double[] {
                        zStats[PrimitiveArray.STATS_MIN] - r20,
                        zStats[PrimitiveArray.STATS_MAX] + r20
                      };
                } else {
                  minMax =
                      Math2.suggestLowHigh(
                          zStats[PrimitiveArray.STATS_MIN], zStats[PrimitiveArray.STATS_MAX]);
                }

                if (palette.length() == 0) {
                  if (minMax[1] >= minMax[0] / -2 && minMax[1] <= minMax[0] * -2) {
                    double td = Math.max(minMax[1], -minMax[0]);
                    minMax[0] = -td;
                    minMax[1] = td;
                    palette = "BlueWhiteRed";
                    // } else if (minMax[0] >= 0 && minMax[0] < minMax[1] / 5) {
                    //    palette = "WhiteRedBlack";
                  } else {
                    palette = "Rainbow";
                  }
                }
                if (Double.isNaN(paletteMin)) paletteMin = minMax[0];
                if (Double.isNaN(paletteMax)) paletteMax = minMax[1];
              }
            }
            if (palette.length() == 0 || Double.isNaN(paletteMin) || Double.isNaN(paletteMax)) {
              // don't create a colorMap
              String2.log(
                  "Warning in EDDTable.saveAsImage: NaNs not allowed (zVar has no numeric data):"
                      + " palette="
                      + palette
                      + " paletteMin="
                      + paletteMin
                      + " paletteMax="
                      + paletteMax);
            } else {
              if (EDDGrid.reallyVerbose)
                String2.log(
                    "create colorBar palette="
                        + palette
                        + " continuous="
                        + continuous
                        + " scale="
                        + scale
                        + " min="
                        + paletteMin
                        + " max="
                        + paletteMax
                        + " nSections="
                        + nSections);
              if (vars[2] instanceof EDVTimeStamp)
                colorMap =
                    new CompoundColorMap(
                        EDStatic.config.fullPaletteDirectory,
                        palette,
                        false, // false= data is seconds
                        paletteMin,
                        paletteMax,
                        nSections,
                        continuous,
                        EDStatic.config.fullCptCacheDirectory);
              else
                colorMap =
                    new CompoundColorMap(
                        EDStatic.config.fullPaletteDirectory,
                        palette,
                        scale,
                        paletteMin,
                        paletteMax,
                        nSections,
                        continuous,
                        EDStatic.config.fullCptCacheDirectory);
            }
          }

          // make a graphDataLayer with data  time series line
          graphDataLayer =
              new GraphDataLayer(
                  -1, // which pointScreen
                  0,
                  1,
                  vars[2] == null ? 1 : 2,
                  1,
                  1, // x,y,z1,z2,z3 column numbers
                  drawLines
                      ? GraphDataLayer.DRAW_LINES
                      : drawMarkers
                          ? GraphDataLayer.DRAW_MARKERS
                          : GraphDataLayer.DRAW_MARKERS_AND_LINES,
                  xIsTimeAxis,
                  yIsTimeAxis,
                  (reallySmall ? vars[0].destinationName() : vars[0].longName())
                      + xUnits, // x,yAxisTitle  for now, always std units
                  (reallySmall ? vars[1].destinationName() : vars[1].longName()) + yUnits,
                  vars[2] == null
                      ? eddGrid.title(language)
                      : (reallySmall ? vars[2].destinationName() : vars[2].longName()) + zUnits,
                  vars[2] == null ? "" : eddGrid.title(language),
                  otherInfo.toString(),
                  MessageFormat.format(
                      EDStatic.messages.get(Message.IMAGE_DATA_COURTESY_OF, language),
                      eddGrid.institution(language)),
                  table,
                  null,
                  null,
                  colorMap,
                  color,
                  markerType,
                  markerSize,
                  0, // vectorStandard
                  GraphDataLayer.REGRESS_NONE);
          graphDataLayers.add(graphDataLayer);
        }
      }

      // setup graphics2D
      String logoImageFile;
      // transparentPng will revise this below
      if (pdf) {
        fontScale *= 1.4 * fontScale; // SgtMap.PDF_FONTSCALE=1.5 is too big
        logoImageFile = EDStatic.config.highResLogoImageFile;
        pdfInfo =
            SgtUtil.createPdf(
                SgtUtil.PDFPageSize.LETTER_PORTRAIT,
                imageWidth,
                imageHeight,
                outputStreamSource.outputStream(File2.UTF_8));
        g2 = (Graphics2D) pdfInfo[0];
      } else {
        fontScale *= imageWidth < 500 ? 1 : 1.25;
        logoImageFile =
            sizeIndex <= 1
                ? EDStatic.config.lowResLogoImageFile
                : EDStatic.config.highResLogoImageFile;

        // transparentPng supports returning requests outside of data
        // range to enable tiles that partially contain data. This
        // section adjusts the map output to match the requested
        // inputs.

        if (transparentPng && isMap) {
          // transparentPng supports returning requests outside of data range
          // to enable tiles that partially contain data. This section
          // validates there is data to return before continuing.
          // Get the X input values.
          double inputMinX = inputValues.get(eddGrid.lonIndex() * 2);
          double inputMaxX = inputValues.get(eddGrid.lonIndex() * 2 + 1);
          if (inputMinX > inputMaxX) {
            double d = inputMinX;
            inputMinX = inputMaxX;
            inputMaxX = d;
          }
          // Get the Y input values.
          double inputMinY = inputValues.get(eddGrid.latIndex() * 2);
          double inputMaxY = inputValues.get(eddGrid.latIndex() * 2 + 1);
          if (inputMinY > inputMaxY) {
            double d = inputMinY;
            inputMinY = inputMaxY;
            inputMaxY = d;
          }
          eddGrid.validateLatLon(language, inputMinX, inputMaxX, inputMinY, inputMaxY);
          // end moved section

          double diffAllowance = 1;
          double minXDiff = Math.abs(minX - inputMinX);
          double maxXDiff = Math.abs(inputMaxX - maxX);
          double minYDiff = Math.abs(minY - inputMinY);
          double maxYDiff = Math.abs(inputMaxY - maxY);
          // Use the inputParams compared to the repaired axis to
          // determine if we need to adjust
          // the image width or height.
          if (minXDiff < diffAllowance && maxXDiff < diffAllowance) {
            // xAxis good
          } else {
            double repairedWidth = maxX - minX;
            double inputWidth = inputMaxX - inputMinX;
            if (!customSize) imageWidth = (int) (imageWidth * inputWidth / repairedWidth);
            minX = inputMinX;
            maxX = inputMaxX;
          }
          if (minYDiff < diffAllowance && maxYDiff < diffAllowance) {
            // yAxis good
          } else {
            double repairedHeight = maxY - minY;
            double inputHeight = inputMaxY - inputMinY;
            if (!customSize) imageHeight = (int) (imageHeight * inputHeight / repairedHeight);
            minY = inputMinY;
            maxY = inputMaxY;
          }
        }

        bufferedImage = SgtUtil.getBufferedImage(imageWidth, imageHeight);
        g2 = (Graphics2D) bufferedImage.getGraphics();
      }

      if (transparentPng) {
        // fill with unusual color --> later convert to transparent
        // Not a great approach to the problem.
        transparentColor = new Color(0, 3, 1); // not common, not in grayscale, not white
        g2.setColor(transparentColor);
        g2.fillRect(0, 0, imageWidth, imageHeight);
      }

      if (isMap) {
        // for maps, ignore xAscending and yAscending
        // ensure minX < maxX and minY < maxY
        if (minX > maxX) {
          double d = minX;
          minX = maxX;
          maxX = d;
        }
        if (minY > maxY) {
          double d = minY;
          minY = maxY;
          maxY = d;
        }
      }

      if (drawSurface && isMap) {

        // draw the map
        if (transparentPng) {
          SgtMap.makeCleanMap(
              minX,
              maxX,
              minY,
              maxY,
              false,
              grid,
              1,
              1,
              0, // double gridScaleFactor, gridAltScaleFactor, gridAltOffset,
              cptFullName,
              false,
              false,
              SgtMap.NO_LAKES_AND_RIVERS,
              false,
              false,
              g2,
              imageWidth,
              imageHeight,
              0,
              0,
              imageWidth,
              imageHeight);
        } else {
          if (currentDrawLandMask == null)
            currentDrawLandMask = vars[2].drawLandMask(eddGrid.defaultDrawLandMask(language));

          List<PrimitiveArray> mmal =
              SgtMap.makeMap(
                  false,
                  SgtUtil.LEGEND_BELOW,
                  EDStatic.messages.legendTitle1,
                  EDStatic.messages.legendTitle2,
                  EDStatic.config.imageDir,
                  logoImageFile,
                  minX,
                  maxX,
                  minY,
                  maxY,
                  currentDrawLandMask,
                  true, // plotGridData
                  grid,
                  1,
                  1,
                  0, // double gridScaleFactor, gridAltScaleFactor, gridAltOffset,
                  cptFullName,
                  vars[2].longName() + zUnits,
                  eddGrid.title(language),
                  otherInfo.toString(),
                  MessageFormat.format(
                      EDStatic.messages.get(Message.IMAGE_DATA_COURTESY_OF, language),
                      eddGrid.institution(language)),
                  "off".equals(currentDrawLandMask)
                      ? SgtMap.NO_LAKES_AND_RIVERS
                      : palette.equals("Ocean") || palette.equals("Topography")
                          ? SgtMap.FILL_LAKES_AND_RIVERS
                          : SgtMap.STROKE_LAKES_AND_RIVERS,
                  false,
                  null,
                  1,
                  1,
                  1,
                  "",
                  null,
                  "",
                  "",
                  "",
                  "",
                  "", // plot contour
                  new ArrayList<>(),
                  g2,
                  0,
                  0,
                  imageWidth,
                  imageHeight,
                  0, // no boundaryResAdjust,
                  fontScale);

          eddGrid.writePngInfo(loggedInAs, userDapQuery, fileTypeName, mmal);
        }

      } else if (drawVectors
          || drawLines
          || drawLinesAndMarkers
          || drawMarkers
          || drawSticks
          || (drawSurface && !isMap)) {
        if (currentDrawLandMask == null) {
          EDV edv = vars[2] == null ? vars[1] : vars[2];
          currentDrawLandMask = edv.drawLandMask(eddGrid.defaultDrawLandMask(language));
        }

        List<PrimitiveArray> mmal =
            isMap
                ? SgtMap.makeMap(
                    transparentPng,
                    SgtUtil.LEGEND_BELOW,
                    EDStatic.messages.legendTitle1,
                    EDStatic.messages.legendTitle2,
                    EDStatic.config.imageDir,
                    logoImageFile,
                    minX,
                    maxX,
                    minY,
                    maxY,
                    currentDrawLandMask,
                    false, // plotGridData
                    null,
                    1,
                    1,
                    0,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "off".equals(currentDrawLandMask)
                        ? SgtMap.NO_LAKES_AND_RIVERS
                        : SgtMap.FILL_LAKES_AND_RIVERS,
                    false,
                    null,
                    1,
                    1,
                    1,
                    "",
                    null,
                    "",
                    "",
                    "",
                    "",
                    "", // plot contour
                    graphDataLayers,
                    g2,
                    0,
                    0,
                    imageWidth,
                    imageHeight,
                    0, // no boundaryResAdjust,
                    fontScale)
                : EDStatic.sgtGraph.makeGraph(
                    transparentPng,
                    graphDataLayer.xAxisTitle,
                    png && drawLegend.equals(EDDGrid.LEGEND_ONLY)
                        ? "."
                        : graphDataLayer.yAxisTitle, // avoid running into legend
                    SgtUtil.LEGEND_BELOW,
                    EDStatic.messages.legendTitle1,
                    EDStatic.messages.legendTitle2,
                    EDStatic.config.imageDir,
                    logoImageFile,
                    minX,
                    maxX,
                    xAscending,
                    xIsTimeAxis,
                    xIsLogAxis,
                    minY,
                    maxY,
                    yAscending,
                    yIsTimeAxis,
                    yIsLogAxis,
                    graphDataLayers,
                    g2,
                    0,
                    0,
                    imageWidth,
                    imageHeight,
                    Double.NaN, // graph imageWidth/imageHeight
                    drawSurface
                        ? (!bgColor.equals(EDStatic.config.graphBackgroundColor)
                            ? bgColor
                            : palette.equals("BlackWhite") || palette.equals("WhiteBlack")
                                ? new Color(0xccccff)
                                : // opaque light blue
                                new Color(0x808080))
                        : // opaque gray
                        bgColor,
                    fontScale);

        eddGrid.writePngInfo(loggedInAs, userDapQuery, fileTypeName, mmal);
      }

      // .legend
      if (png && !transparentPng) {
        if (drawLegend.equals(EDDGrid.LEGEND_OFF))
          bufferedImage = SgtUtil.removeLegend(bufferedImage);
        else if (drawLegend.equals(EDDGrid.LEGEND_ONLY))
          bufferedImage = SgtUtil.extractLegend(bufferedImage);

        // do after removeLegend
        bufferedImage = SgtUtil.trimBottom(bufferedImage, trim);
      }

    } catch (WaitThenTryAgainException wttae) {
      throw wttae;

    } catch (Throwable t) {
      EDStatic.rethrowClientAbortException(t); // first thing in catch{}
      ok = false;
      try {
        String msg = MustBe.getShortErrorMessage(t);
        String fullMsg = MustBe.throwableToString(t);
        String2.log(fullMsg); // log full message with stack trace

        if (png && drawLegend.equals(EDDGrid.LEGEND_ONLY)) {
          // return a transparent 1x1 pixel image
          bufferedImage = SgtUtil.getBufferedImage(1, 1); // has white background
          transparentColor = Color.white;

        } else {
          // write exception info on image
          double tFontScale = pdf ? 1.25 : 1;
          int tHeight = Math2.roundToInt(tFontScale * 12);

          if (pdf) {
            if (pdfInfo == null)
              pdfInfo =
                  SgtUtil.createPdf(
                      SgtUtil.PDFPageSize.LETTER_PORTRAIT,
                      imageWidth,
                      imageHeight,
                      outputStreamSource.outputStream(File2.UTF_8));
            if (g2 == null) g2 = (Graphics2D) pdfInfo[0];
          } else { // png
            // make a new image (I don't think pdf can work this way -- sent as created)
            bufferedImage = SgtUtil.getBufferedImage(imageWidth, imageHeight);
            g2 = (Graphics2D) bufferedImage.getGraphics();
          }
          if (transparentPng) {
            // don't write the message
            // The "right" thing to do is different in different situations.
            // But e.g., No Data, should just be a transparent image.
            transparentColor = Color.white;
          } else {

            g2.setClip(0, 0, imageWidth, imageHeight); // unset in case set by sgtGraph
            msg = String2.noLongLines(msg, (imageWidth * 10 / 6) / tHeight, "    ");
            String lines[] = msg.split("\\n"); // not String2.split which trims
            g2.setColor(Color.black);
            g2.setFont(new Font(EDStatic.config.fontFamily, Font.PLAIN, tHeight));
            int ty = tHeight * 2;
            for (String line : lines) {
              g2.drawString(line, tHeight, ty);
              ty += tHeight + 2;
            }
          }
        }
      } catch (Throwable t2) {
        EDStatic.rethrowClientAbortException(t2); // first thing in catch{}
        String2.log("ERROR2 while creating error image:\n" + MustBe.throwableToString(t2));
        if (pdf) {
          if (pdfInfo == null) throw t;
        } else {
          if (bufferedImage == null) throw t;
        }
        // else fall through to close/save image below
      }
    }

    // save image
    if (pdf) {
      SgtUtil.closePdf(pdfInfo);
    } else {
      SgtUtil.saveAsTransparentPng(
          bufferedImage, transparentColor, outputStreamSource.outputStream(""));
    }

    OutputStream out = outputStreamSource.existingOutputStream();
    if (out != null) out.flush(); // safety

    if (EDDGrid.reallyVerbose)
      String2.log(
          "  EDDGrid.saveAsImage done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
    return ok;
  }
}
