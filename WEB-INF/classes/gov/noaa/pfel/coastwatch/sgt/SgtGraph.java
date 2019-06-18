/* 
 * SgtGraph Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.DoubleObject;
import com.cohort.util.File2;
import com.cohort.util.Image2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.Grid;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.AttributedString2;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pmel.sgt.*;
import gov.noaa.pmel.sgt.demo.*;
import gov.noaa.pmel.sgt.dm.*;
import gov.noaa.pmel.util.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints; 
import java.awt.Shape;
import java.io.File;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import javax.imageio.ImageIO;


/**
 * This class holds a SGT graph.
 * A note about coordinates:
 *   <ul>
 *   <li> Graph - uses "user" coordinates (e.g., lat and lon).
 *   <li> Layer - uses "physical" coordinates (doubles, 0,0 at lower left).
 *   <li> JPane - uses "device" coordinates (ints, 0,0 at upper left).
 *   </ul>
 */
public class SgtGraph  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /**
     * Set this to true (by calling reallyVerbose=true in your program, not by changing the code here)
     * if you want lots and lots of diagnostic messages sent to String2.log.
     */
    public static boolean reallyVerbose = false;

    private String fontFamily;
    public double defaultAxisLabelHeight = SgtUtil.DEFAULT_AXIS_LABEL_HEIGHT; 
    public double defaultLabelHeight = SgtUtil.DEFAULT_LABEL_HEIGHT; 
    public double majorLabelRatio = 1.25; //axisTitleHeight/axisLabelHeight; 1.25 matches SGT

    public static Color DefaultBackgroundColor = new Color(0xCCCCFF); //just the RGB part (no A)
    public int widenOnePoint = 1;  //pixels
    private final static String testImageExtension = ".png"; //was/could be ".gif"

    /**
     * Constructor.
     * This throws exception if trouble
     *
     * @param fontFamily the name of the font family (a good choice is "DejaVu Sans"; a safe choice is "SansSerif")
     */
    public SgtGraph(String fontFamily) {

        this.fontFamily = fontFamily;

        //create the font  (ensure the fontFamily is available)
        SgtUtil.getFont(fontFamily);

    }
    
    private static int rint(double d) {return Math2.roundToInt(d); }
     

    /**
     * This uses SGT to plot data on a graph.
     * Strings should be "" if not needed.
     * If there is no data, the graph and legend are still drawn, but
     * the graph has "No Data" in the center.
     *
     * @param transparent if true, the graph fills the 
     *   baseULX/YPixel and imageWidth/HeightPixels area 
     *   and no legend or axis labels/ticks/lines/titles will be drawn.
     *   The image's background color and graph color will not be changed (or actively drawn).
     * @param xAxisTitle  null = none
     * @param yAxisTitle  null = none
     * @param legendPosition must be SgtUtil.LEGEND_BELOW (SgtUtil.LEGEND_RIGHT currently not supported)
     * @param legendTitle1 the first line of the legend
     * @param legendTitle2 the second line of the legend
     * @param imageDir the directory with the logo file 
     * @param logoImageFile the logo image file in the imageDir (should be square image) 
     *    (currently, must be png, gif, jpg, or bmp)
     *    (currently noaa-simple-40.gif for lowRes),
     *    or null for none.
     * @param minX the min value for the X axis. Use Double.NaN to tell makeGraph to set it.
     *    If xIsTimeAxis, specify minX in epochSeconds.
     *    Here (and inside this method), minX &lt; maxX.
     * @param maxX the max value for the X axis. Use Double.NaN to tell makeGraph to set it.
     *    If xIsTimeAxis, specify minX in epochSeconds.
     * @param xAscending if true, the axis will be drawn low to high; else it will be flipped.
     *    The current minX maxX order is irrelevant.
     * @param xIsTimeAxis   note that SGT doesn't allow x and y to be time axes
     * @param xIsLogAxis   This states a preference. 
     *    If xIsTimeAxis or minX&lt;=0 or this is a Sticks graph, this is ignored and treated as false.
     * @param minY the min value for the Y axis. Use Double.NaN to tell makeGraph to set it.
     *    If yIsTimeAxis, specify minY in epochSeconds. 
     *    Here (and inside this method), minY &lt; maxY.
     * @param maxY the max value for the Y axis. Use Double.NaN to tell makeGraph to set it.
     *    If yIsTimeAxis, specify minY in epochSeconds. 
     * @param yAscending if true, the axis will be drawn low to high; else it will be flipped.
     *    The current minY maxY order is irrelevant.
     * @param yIsTimeAxis
     * @param yIsLogAxis This states a preference. 
     *    If yIsTimeAxis or minY&lt;=0 or this is a Sticks graph, this is ignored and treated as false.
     * @param graphDataLayers an ArrayList of GraphDataLayers with the data to be plotted; 
     *    the first column in the table is treated as the X data, 
     *    the second column as the y data.
     *    If you want variable-color filled markers, specify a colorMap
     *    and choose a "filled" marker type.
     * @param g2 the graphics2D object to be used (the image background color should
     *    already have been drawn).
     * @param baseULXPixel defines area to be used, in pixels 
     * @param baseULYPixel defines area to be used, in pixels 
     * @param imageWidthPixels defines the area to be used, in pixels 
     * @param imageHeightPixels defines the area to be used, in pixels 
     * @param graphWidthOverHeight the desired shape of the graph 
     *     (or NaN or 0 to fill the available space)
     * @param fontScale relative to 1=normalHeight
     * @return ArrayList with info about where the GraphDataLayer markers were plotted 
     *   (for generating the user map on the image:
     *     0=IntArray minX, 1=IntArray maxX, 2=IntArray minY, 3=IntArray maxY,
     *     4=IntArray rowNumber 5=IntArray whichPointScreen(0,1,2,...)),
     *   pixel location of graph 6=IntArray originX,endX,originY,endY,
     *   XY double MinMax graph 7=DoubleArray originX,endX,originY,endY.
     *   (The returnAL isn't extensively tested yet.)
     * @throws Exception
     */
    public ArrayList makeGraph(boolean transparent,
        String xAxisTitle, String yAxisTitle,
        int legendPosition, String legendTitle1, String legendTitle2,
        String imageDir, String logoImageFile,
        double minX, double maxX, boolean xAscending, boolean xIsTimeAxis, boolean xIsLogAxis,
        double minY, double maxY, boolean yAscending, boolean yIsTimeAxis, boolean yIsLogAxis,
        ArrayList graphDataLayers,
        Graphics2D g2,
        int baseULXPixel, int baseULYPixel,
        int imageWidthPixels, int imageHeightPixels,
        double graphWidthOverHeight,
        Color backgroundColor, double fontScale
        ) throws Exception {

        //Coordinates in SGT:
        //   Graph - 'U'ser coordinates      (graph's axes' coordinates)
        //   Layer - 'P'hysical coordinates  (e.g., pseudo-inches, 0,0 is lower left)
        //   JPane - 'D'evice coordinates    (pixels, 0,0 is upper left)

        if (legendTitle1 == null) 
            legendTitle1 = "";
        if (legendTitle2 == null) 
            legendTitle2 = "";

        if (xIsTimeAxis) xIsLogAxis = false;
        if (yIsTimeAxis) yIsLogAxis = false;

        //make the empty returnAL
        IntArray returnMinX = new IntArray();
        IntArray returnMaxX = new IntArray();
        IntArray returnMinY = new IntArray();
        IntArray returnMaxY = new IntArray();
        IntArray returnRowNumber = new IntArray();
        IntArray returnPointScreen = new IntArray();
        IntArray returnIntWESN = new IntArray();
        DoubleArray returnDoubleWESN = new DoubleArray();
        ArrayList returnAL = new ArrayList();
        returnAL.add(returnMinX);
        returnAL.add(returnMaxX);
        returnAL.add(returnMinY);
        returnAL.add(returnMaxY);
        returnAL.add(returnRowNumber);
        returnAL.add(returnPointScreen);
        returnAL.add(returnIntWESN);
        returnAL.add(returnDoubleWESN);

        //set the clip region
        g2.setClip(baseULXPixel, baseULYPixel, imageWidthPixels, imageHeightPixels);
        try {
            if (reallyVerbose) String2.log("\n{{ SgtGraph.makeGraph "); // + Math2.memoryString());
            long startTime = System.currentTimeMillis();
            long setupTime = System.currentTimeMillis();

            if (xIsTimeAxis && yIsTimeAxis) {
                //Test.error(String2.ERROR + " in SgtGraph.makeGraph: SGT doesn't allow x and y axes to be time axes.");
                //kludge: just make y not a time axis
                yIsTimeAxis = false;
            }
            double axisLabelHeight = fontScale * defaultAxisLabelHeight;
            double labelHeight     = Math.max(1, fontScale) * defaultLabelHeight; //never smaller than default

            //read the data from the files
            Table tables[] = new Table[graphDataLayers.size()];
            Grid  grids[]  = new Grid[graphDataLayers.size()];
            boolean useTableData[] = new boolean[graphDataLayers.size()];
            boolean someUseGridData = false;
            for (int i = 0; i < graphDataLayers.size(); i++) {
                GraphDataLayer gdl = (GraphDataLayer)graphDataLayers.get(i);
                if (reallyVerbose) String2.log("  graphDataLayer" + i + "=" + gdl);
                tables[i] = gdl.table;
                grids[i] = gdl.grid1;
                useTableData[i] = tables[i] != null;
                if (!useTableData[i]) 
                    someUseGridData = true;
                //String2.log("SgtGraph table" + i + "=" + tables[i].toString("row", 20));
            }
            
            //if minX maxX not specified, calculate x axis ranges
            if (Double.isFinite(minX) && Double.isFinite(maxX)) {
                //ensure minX < maxX
                if (minX > maxX) {double d = minX; minX = maxX; maxX = d;}
            } else {
                double tMinX = Double.MAX_VALUE;
                double tMaxX = -Double.MAX_VALUE;
                for (int i = 0; i < tables.length; i++) {
                    GraphDataLayer gdl = (GraphDataLayer)graphDataLayers.get(i);
                    double xStats[] = useTableData[i]?
                        tables[i].getColumn(gdl.v1).calculateStats() :
                        (new DoubleArray(grids[i].lon)).calculateStats();
                    if (reallyVerbose) String2.log("  table=" + i + 
                        " tMinX=" + xStats[PrimitiveArray.STATS_MIN] + 
                        " tMaxX=" + xStats[PrimitiveArray.STATS_MAX]);
                    if (xStats[PrimitiveArray.STATS_N] > 0) {
                        tMinX = Math.min(tMinX, xStats[PrimitiveArray.STATS_MIN]);
                        tMaxX = Math.max(tMaxX, xStats[PrimitiveArray.STATS_MAX]);
                    }
                }
                //ensure tMinX,tMaxX are finite
                if (tMinX == Double.MAX_VALUE) {
                    //no data was found
                    tMinX = Double.isFinite(minX)? minX : Double.isFinite(maxX)? maxX : 0; 
                    tMaxX = Double.isFinite(maxX)? maxX : Double.isFinite(minX)? minX : 0;
                }

                //set minX and maxX
                if (someUseGridData) {
                    //use exact minX maxX
                    minX = Double.isFinite(minX)? minX : tMinX;
                    maxX = Double.isFinite(maxX)? maxX : tMaxX;
                    if (minX > maxX) {double d = minX; minX = maxX; maxX = d;}
                } else if (xIsTimeAxis) {
                    //add little buffer zone
                    minX = Double.isFinite(minX)? minX : tMinX;
                    maxX = Double.isFinite(maxX)? maxX : tMaxX;
                    if (minX > maxX) {double d = minX; minX = maxX; maxX = d;}
                    double r20 = (maxX - minX) / 20;
                    minX -= r20;
                    maxX += r20;                    
                } else {
                    //get suggestedLowHigh for x
                    tMinX = Double.isFinite(minX)? minX : tMinX;  //work on t values first
                    tMaxX = Double.isFinite(maxX)? maxX : tMaxX;
                    if (tMinX > tMaxX) {double d = tMinX; tMinX = tMaxX; tMaxX = d;}
                    double xLowHigh[] = xIsLogAxis && tMinX > 0?
                        new double[]{tMinX*0.8, tMaxX*1.2} :
                        Math2.suggestLowHigh(tMinX, tMaxX);
                    //then set minX, maxX
                    minX = Double.isFinite(minX)? minX : xLowHigh[0];
                    maxX = Double.isFinite(maxX)? maxX : xLowHigh[1];
                    if (minX > maxX) {double d = minX; minX = maxX; maxX = d;}
                }
            }
            if (minX == maxX) { 
                //no data variation
                if (xIsTimeAxis) { //do +/-an hour
                    minX -= Calendar2.SECONDS_PER_HOUR;
                    maxX += Calendar2.SECONDS_PER_HOUR;
                } else {
                    double xLowHigh[] = Math2.suggestLowHigh(minX, maxX);
                    minX = xLowHigh[0];
                    maxX = xLowHigh[1];
                }
            }

            //are any of the gdl's vector STICKS?  
            boolean sticksGraph = false;
            int sticksGraphNRows = -1;
            double minVData = Double.MAX_VALUE;
            double maxVData = -Double.MAX_VALUE;
            for (int spli = 0; spli < graphDataLayers.size(); spli++) {
                GraphDataLayer gdl = (GraphDataLayer)graphDataLayers.get(spli);
                if (gdl.draw == GraphDataLayer.DRAW_STICKS) {
                    sticksGraph = true;
                    sticksGraphNRows = tables[spli].nRows();

                    //calculate v variable's stats
                    double vStats[] = tables[spli].getColumn(gdl.v3).calculateStats();
                    if (reallyVerbose) String2.log("  table=" + spli + 
                        " minData=" + vStats[PrimitiveArray.STATS_MIN] + 
                        " maxData=" + vStats[PrimitiveArray.STATS_MAX]);
                    if (vStats[PrimitiveArray.STATS_N] > 0) {
                        minVData = Math.min(minVData, vStats[PrimitiveArray.STATS_MIN]);
                        maxVData = Math.max(maxVData, vStats[PrimitiveArray.STATS_MAX]);
                    }
                }
            }

            //if minY maxY not specified, calculate y axis ranges
            if (Double.isFinite(minY) && Double.isFinite(maxY)) {
                //ensure minY < maxY
                if (minY > maxY) {double d = minY; minY = maxY; maxY = d;}
            } else {
                double tMinY = Double.MAX_VALUE;
                double tMaxY = -Double.MAX_VALUE;
                for (int i = 0; i < tables.length; i++) {
                    GraphDataLayer gdl = (GraphDataLayer)graphDataLayers.get(i);
                    double yStats[] = useTableData[i]?
                        tables[i].getColumn(gdl.v2).calculateStats() :
                        (new DoubleArray(grids[i].lat)).calculateStats();
                    if (reallyVerbose) String2.log("  table=" + i + " minY=" + yStats[1] + " maxY=" + yStats[2]);
                    if (yStats[PrimitiveArray.STATS_N] > 0) {
                        tMinY = Math.min(tMinY, yStats[PrimitiveArray.STATS_MIN]);
                        tMaxY = Math.max(tMaxY, yStats[PrimitiveArray.STATS_MAX]);
                    }
                }

                //ensure tMinY,tMaxY are finite
                if (tMinY == Double.MAX_VALUE) {
                    //no data was found
                    tMinY = Double.isFinite(minY)? minY : Double.isFinite(maxY)? maxY : 0; 
                    tMaxY = Double.isFinite(maxY)? maxY : Double.isFinite(minY)? minY : 0;
                }

                //set minY and maxY
                if (someUseGridData) {
                    //use exact minY maxY
                    minY = Double.isFinite(minY)? minY : tMinY;
                    maxY = Double.isFinite(maxY)? maxY : tMaxY;
                    if (minY > maxY) {double d = minY; minY = maxY; maxY = d;}
                } else if (yIsTimeAxis) {
                    //add little buffer zone
                    minY = Double.isFinite(minY)? minY : tMinY;
                    maxY = Double.isFinite(maxY)? maxY : tMaxY;
                    if (minY > maxY) {double d = minY; minY = maxY; maxY = d;}
                    double r20 = (maxY - minY) / 20;
                    minY -= r20;
                    maxY += r20;                    
                } else {
                    //get suggestedLowHigh for y
                    tMinY = Double.isFinite(minY)? minY : tMinY;  //work on t values first
                    tMaxY = Double.isFinite(maxY)? maxY : tMaxY;
                    if (tMinY > tMaxY) {double d = tMinY; tMinY = tMaxY; tMaxY = d;}
                    double yLowHigh[] = yIsLogAxis && tMinY > 0?
                        new double[]{tMinY*0.5, tMaxY*1.2} :
                        Math2.suggestLowHigh(tMinY, tMaxY);
                    //then set minY, maxY
                    minY = Double.isFinite(minY)? minY : yLowHigh[0];
                    maxY = Double.isFinite(maxY)? maxY : yLowHigh[1];
                    if (minY > maxY) {double d = minY; minY = maxY; maxY = d;}
                }


                //if any gdl's are vector STICKS, include minVData,maxVData and make y range symmetrical
                //(only need longest u or v component, not longest stick)
                //(technically, just need to encompass longest v component,
                //  but there may be stick and non-stick layers)
                if (sticksGraph) {
                    maxY = Math.max(Math.abs(minY), Math.abs(maxY));  //u component
                    if (maxY == 0) 
                        maxY = 0.1;
                    if (minVData != Double.MAX_VALUE) {  //v component
                        maxY = Math.max(maxY, Math.abs(minVData));
                        maxY = Math.max(maxY, Math.abs(maxVData));
                    }
                    minY = -maxY;
                }
            }
            if (minY == maxY) { 
                double yLowHigh[] = Math2.suggestLowHigh(minY, maxY);
                minY = yLowHigh[0];
                maxY = yLowHigh[1];
            }

            //whether yMin yMax predefined or not, if sticksGraph  ensure maxY=-minY 
            //rendering algorithm below depends on it
            if (sticksGraph) {
                //make room for sticks at end points
                //  just less than 1 month; any more looks like no data for those months
                //--nice idea, but it looks like there is no data for that section of time
                //just do it for 1month data
                double xRange = maxX - minX;
                if (sticksGraphNRows >= 2 && 
                    xRange / (sticksGraphNRows - 1) > 29 * Calendar2.SECONDS_PER_DAY) { //x is in seconds
                    minX -= 29 * Calendar2.SECONDS_PER_DAY;  //almost a month
                    maxX += 29 * Calendar2.SECONDS_PER_DAY;
                }

                //special calculation of minY maxY
                maxY = Math.max(Math.abs(minY), Math.abs(maxY));  //u component
                minY = -maxY;
            }
            double scaleXIfTime = xIsTimeAxis? 1000 : 1; //s to ms
            double scaleYIfTime = yIsTimeAxis? 1000 : 1; //s to ms
            if (reallyVerbose) String2.log("  sticksGraph=" + sticksGraph + 
                "\n    minX=" + (xIsTimeAxis? Calendar2.epochSecondsToIsoStringTZ(minX) : "" + minX) + 
                     " maxX=" + (xIsTimeAxis? Calendar2.epochSecondsToIsoStringTZ(maxX) : "" + maxX) + 
                "\n    minY=" + (yIsTimeAxis? Calendar2.epochSecondsToIsoStringTZ(minY) : "" + minY) + 
                     " maxY=" + (yIsTimeAxis? Calendar2.epochSecondsToIsoStringTZ(maxY) : "" + maxY)); 


            //figure out the params needed to make the graph
            String error = "";
            //make doubly sure min < max
            if (minX > maxX) {double d = minX; minX = maxX; maxX = d;}
            if (minY > maxY) {double d = minY; minY = maxY; maxY = d;}
            double xRange = maxX - minX; 
            double yRange = maxY - minY; 
            double beginX = xAscending? minX: maxX;
            double endX   = xAscending? maxX: minX;
            double beginY = yAscending? minY: maxY;
            double endY   = yAscending? maxY: minY;

            //Perhaps set xIsLogAxis and yIsLogAxis to false since pertinent info is now known.
            //Sticks: because sticks calls CartesianProjection.graphToDeviceYDistance 
            //  which doesn't support log axes.
            //  Also, the y range of sticks graph is -yMax to +yMax, 
            //  so it fails the &lt;=0 test, too.
            if (xIsLogAxis) {
                String reason = null;
                if      (minX <= 0)   reason = "minX<=0";
                else if (xIsTimeAxis) reason = "xIsTimeAxis";
                else if (sticksGraph) reason = "sticksGraph";
                else if (Math2.intExponent(minX) == Math2.intExponent(maxX) &&  //within 1 decade
                        Math.floor(Math2.mantissa(maxX)) - Math.floor(Math2.mantissa(minX)) <= 1) //only 0 or 1 labels visible
                    reason = "narrow range " + minX + " to " + maxX;
                if (reason != null) {
                    if (verbose) String2.log("  ! xIsLogAxis=false because " + reason);
                    xIsLogAxis = false;
                }
            }
            if (yIsLogAxis) {
                String reason = null;
                if      (minY <= 0)   reason = "minY<=0";
                else if (yIsTimeAxis) reason = "yIsTimeAxis";
                else if (sticksGraph) reason = "sticksGraph";
                else if (Math2.intExponent(minY) == Math2.intExponent(maxY) && //within 1 decade
                        Math.floor(Math2.mantissa(maxY)) - Math.floor(Math2.mantissa(minY)) <= 1) //only 0 or 1 labels visible
                    reason = "narrow range " + minY + " to " + maxY;
                if (reason != null) {
                    if (verbose) String2.log("  ! yIsLogAxis=false because " + reason);
                    yIsLogAxis = false;
                }
            }

            double rangeScaler = 1;  //bigger -> fewer labels
            if (imageWidthPixels <= 180) {
                rangeScaler *= 2; //adjust for tiny graphs, 
            } else if (imageWidthPixels <= 300) {
                rangeScaler *= 2; //adjust for small graphs,
            } else if (imageWidthPixels >= 2400) {
                rangeScaler /= 6; //adjust for huge graphs 
            } else if (imageWidthPixels >= 1200) {
                rangeScaler /= 3; //adjust for big graphs e.g., 1400
            } else if (imageWidthPixels >= 600) {
                rangeScaler /= 1.5; //adjust for big graphs e.g., 700
            }
            if      (fontScale >= 3)    rangeScaler *= 2;   //aim at fontScale 4 
            else if (fontScale >= 1.5)  rangeScaler *= 1.5; //aim at fontScale 2
            else if (fontScale >= 0.75) ;                   //aim at fontScale 1
            else if (fontScale >= 0.37) rangeScaler /= 1.5; //aim at fontScale 0.5
            else                        rangeScaler /= 2;   //aim at fontScale 0.25
            double xDivisions[] = Math2.suggestDivisions(xRange * rangeScaler);
            double yDivisions[] = Math2.suggestDivisions(yRange * rangeScaler); 

            //define sizes
            double dpi = 100; //dots per inch
            double imageWidthInches  = imageWidthPixels  / dpi;  
            double imageHeightInches = imageHeightPixels / dpi;
            double betweenGraphAndLegend = fontScale * .25;
            int labelHeightPixels = Math2.roundToInt(labelHeight * dpi);
            double betweenGraphAndColorBar  = fontScale * .25;
            if (imageWidthPixels < 300) {
                betweenGraphAndLegend /= 4;
                betweenGraphAndColorBar /= 4;
            }

            //set legend location and size (in pixels)   for LEGEND_RIGHT
            //standard length of vector (and other samples) in user units (e.g., inches)
            double legendSampleSizeInches = 0.22; //Don't change this (unless make other changes re vector length on graph)
            int legendSampleSize = Math2.roundToInt(legendSampleSizeInches * dpi); 
            int legendBoxWidth  = Math2.roundToInt(fontScale * 1.4 * dpi); //1.4inches 
            int legendBoxHeight = imageHeightPixels;  
            int legendBoxULX = baseULXPixel + imageWidthPixels - legendBoxWidth; 
            int legendBoxULY = baseULYPixel;
            int legendInsideBorder = Math2.roundToInt(fontScale * 0.1 * dpi); 
            int maxCharsPerLine = SgtUtil.maxCharsPerLine(
                legendBoxWidth - (legendSampleSize + 3 * legendInsideBorder), fontScale); 
            int maxBoldCharsPerLine = SgtUtil.maxBoldCharsPerLine(maxCharsPerLine);

            //deal with LEGEND_BELOW   
            if (legendPosition == SgtUtil.LEGEND_BELOW) {
                maxCharsPerLine = SgtUtil.maxCharsPerLine(
                    imageWidthPixels - (legendSampleSize + 3 * legendInsideBorder), fontScale); 
                maxBoldCharsPerLine = SgtUtil.maxBoldCharsPerLine(maxCharsPerLine);
                double legendLineCount = 
                    String2.isSomething(legendTitle1 + legendTitle2)? 1 : -1; //for legend title   //???needs adjustment for larger font size
                for (int i = 0; i < graphDataLayers.size(); i++) 
                    legendLineCount += ((GraphDataLayer)graphDataLayers.get(i)).legendLineCount(maxCharsPerLine);
                //String2.log("legendLineCount=" + legendLineCount);
                legendBoxWidth = imageWidthPixels;  
                legendBoxHeight = (int)(legendLineCount * labelHeightPixels) + //safe
                    2 * legendInsideBorder;  
                legendBoxULX = baseULXPixel;
                legendBoxULY = baseULYPixel + imageHeightPixels - legendBoxHeight;
            }
            //determine appropriate axis lengths to best fill available space
            double graphULX = fontScale * 0.37;   //relative to baseULXYPixel
            double graphULY = fontScale * 0.2;
            double graphBottomY = fontScale * 0.37;
            if (imageWidthPixels < 300) 
                graphULY /= 2;
            double graphWidth = imageWidthInches - graphULX - legendBoxWidth/dpi -
                betweenGraphAndLegend;
            double graphHeight = imageHeightInches - graphBottomY - graphULY;
            if (legendPosition == SgtUtil.LEGEND_BELOW) {
                graphWidth = imageWidthInches - graphULX - betweenGraphAndLegend;
                graphHeight = imageHeightInches - graphBottomY - graphULY - legendBoxHeight/dpi;
            }

            //adjust graph width or height
            if (Double.isNaN(graphWidthOverHeight) || graphWidthOverHeight < 0) {
                //leave it (as big as possible)
            } else if (graphWidth / graphHeight < graphWidthOverHeight) {
                //graph is too tall -- reduce the height
                double newGraphHeight = graphWidth / graphWidthOverHeight;
                //String2.log("graph is too tall  oldHt=" + graphHeight + " newHt=" + newGraphHeight);
                double diff = graphHeight - newGraphHeight; //a positive number
                graphHeight = newGraphHeight;
                if (legendPosition == SgtUtil.LEGEND_BELOW) {
                    graphBottomY += diff;
                    legendBoxULY -= Math2.roundToInt(diff * dpi);
                } else {
                    graphBottomY += diff / 2;
                    graphULY += diff / 2;
                }
            } else { 
                //graph is too wide -- reduce the width
                double newGraphWidth = graphWidthOverHeight * graphHeight;
                //String2.log("graph is too wide  oldWidth=" + graphWidth + " newWidth=" + newGraphWidth);
                double diff = graphWidth - newGraphWidth; //a positive number
                graphWidth = newGraphWidth;
                if (legendPosition == SgtUtil.LEGEND_BELOW) {
                    graphULX += diff / 2;
                } else {
                    graphULX += diff;
                    legendBoxULX -= Math2.roundToInt(diff * dpi);
                }
            } 
            //String2.log("graphULX=" + graphULX + " ULY=" + graphULY + " width=" + graphWidth +
            //    " height=" + graphHeight + " bottomY=" + graphBottomY + 
            //    "\n  widthPixels=" + graphWidthPixels + " heightPixels=" + graphHeightPixels);

            //legendTextX and Y
            int legendTextX = legendPosition == SgtUtil.LEGEND_BELOW?
                legendBoxULX + legendSampleSize + 2 * legendInsideBorder :  //leftX    
                legendBoxULX + legendBoxWidth / 2; //centerX 
            int legendTextY = legendBoxULY + legendInsideBorder + labelHeightPixels;   
            //String2.log("SgtGraph baseULXPixel=" + baseULXPixel +  " baseULYPixel=" + baseULYPixel +
            //    "  imageWidth=" + imageWidthPixels + " imageHeight=" + imageHeightPixels +
            //    "\n  legend boxULX=" + legendBoxULX + " boxULY=" + legendBoxULY + 
            //    " boxWidth=" + legendBoxWidth + " boxHeight=" + legendBoxHeight +
            //    "\n  textX=" + legendTextX + " textY=" + legendTextY +
            //    " insideBorder=" + legendInsideBorder + " labelHeightPixels=" + labelHeightPixels);

            //but if transparent, reset the graph position and ignore legend position
            if (transparent) {
                graphULX = 0;   //relative to baseULXYPixel
                graphULY = 0;
                graphBottomY = 0;
                graphWidth = imageWidthInches;
                graphHeight = imageHeightInches;
            }

            //create the label font
            Font labelFont = new Font(fontFamily, Font.PLAIN, 10); //size is adjusted below

            //SgtUtil.drawHtmlText needs non-text antialiasing ON
            //but if transparent, turn antialiasing OFF (fuzzy pixels make a halo around things)
            Object originalAntialiasing = 
                g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING); 
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, transparent?
                RenderingHints.VALUE_ANTIALIAS_OFF :
                RenderingHints.VALUE_ANTIALIAS_ON); 

            //draw legend basics
            if (!transparent) {
                //box for legend    
                g2.setColor(new Color(0xFFFFCC));       
                g2.fillRect(legendBoxULX, legendBoxULY, legendBoxWidth - 1, legendBoxHeight - 1);
                g2.setColor(Color.black);
                g2.drawRect(legendBoxULX, legendBoxULY, legendBoxWidth - 1, legendBoxHeight - 1);

                //legend titles
                if (String2.isSomething(legendTitle1 + legendTitle2)) {
                    if (legendPosition == SgtUtil.LEGEND_BELOW) {
                        //draw LEGEND_BELOW
                        legendTextY = SgtUtil.drawHtmlText(g2, legendTextX, legendTextY, 
                            0, fontFamily, labelHeightPixels * 3 / 2, false, 
                            "<strong><color=#2600aa>" + SgtUtil.encodeAsHtml(legendTitle1 + " " + 
                                legendTitle2) + "</color></strong>");
                        legendTextY += labelHeightPixels / 2;
                    } else {
                        //draw LEGEND_RIGHT
                        int tx = legendBoxULX + legendInsideBorder;
                        if (legendTitle1.length() > 0)
                            legendTextY = SgtUtil.drawHtmlText(g2, tx, legendTextY, 
                                0, fontFamily, labelHeightPixels * 5 / 4, false, 
                                "<strong><color=#2600aa>" + SgtUtil.encodeAsHtml(legendTitle1) + "</color></strong>");
                        if (legendTitle2.length() > 0)
                            legendTextY = SgtUtil.drawHtmlText(g2, tx, legendTextY, 
                                0, fontFamily, labelHeightPixels * 5 / 4, false, 
                                "<strong><color=#2600aa>" + SgtUtil.encodeAsHtml(legendTitle2) + "</color></strong>");
                        legendTextY += labelHeightPixels * 3 / 2;
                    }

                    //draw the logo
                    if (logoImageFile != null && File2.isFile(imageDir + logoImageFile)) {
                        long logoTime = System.currentTimeMillis();
                        BufferedImage bi2 = ImageIO.read(new File(imageDir + logoImageFile));

                        //g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        //                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        //                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        //                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                        //draw LEGEND_RIGHT
                        int ulx = baseULXPixel + (int)((imageWidthInches - fontScale * 0.45)*dpi);
                        int uly = baseULYPixel + (int)(fontScale * 0.05 * dpi);
                        int tSize = (int)(fontScale * 40);
                        if (legendPosition == SgtUtil.LEGEND_BELOW) {
                            //draw LEGEND_BELOW
                            ulx = legendBoxULX + legendSampleSize / 2;
                            uly = legendBoxULY + legendInsideBorder / 2;
                            tSize = (int)(fontScale * 20);
                        }
                        g2.drawImage(bi2, ulx, uly, tSize, tSize, null); //null=ImageObserver 
                        //if (verbose) String2.log("  draw logo time=" + 
                        //    (System.currentTimeMillis() - logoTime) + "ms");
                    }
                }
            }


            //set up the Projection Cartesian  
            int graphX1 = baseULXPixel + rint(graphULX * dpi);
            int graphY2 = baseULYPixel + rint(graphULY * dpi);
            int graphX2 = graphX1 + rint(graphWidth * dpi); 
            int graphY1 = graphY2 + rint(graphHeight * dpi);
            int graphWidthPixels  = graphX2 - graphX1;
            int graphHeightPixels = graphY1 - graphY2;
            //String2.log("    graphX1=" + graphX1 + " X2=" + graphX2 + 
            //    " graphY1=" + graphY1 + " Y2=" + graphY2);
            double narrowXGraph = (imageWidthPixels / 360.0) / fontScale;
            double narrowYGraph = imageHeightPixels / 440.0; 
            if (reallyVerbose) String2.log(
                "  graphWidth=" + graphWidthPixels + " narrowX=" + narrowXGraph +
                " graphHeight=" + graphHeightPixels + " narrowY=" + narrowYGraph);
            //This is where xIsTimeAxis, yIsTimeAxis, xIsLogAxis, yIsLogAxis first actually gets used.
            CartesianProjection cp = new CartesianProjection(
                beginX * scaleXIfTime, endX * scaleXIfTime, 
                beginY * scaleYIfTime, endY * scaleYIfTime, 
                graphX1, graphX2, graphY1, graphY2,
                xIsLogAxis, yIsLogAxis);
            DoubleObject dox1 = new DoubleObject(0);
            DoubleObject doy1 = new DoubleObject(0);
            DoubleObject dox2 = new DoubleObject(0);
            DoubleObject doy2 = new DoubleObject(0);

            returnIntWESN.add(graphX1);
            returnIntWESN.add(graphX2);
            returnIntWESN.add(graphY1);
            returnIntWESN.add(graphY2);

            returnDoubleWESN.add(beginX);
            returnDoubleWESN.add(endX);
            returnDoubleWESN.add(beginY);
            returnDoubleWESN.add(endY);

            if (!transparent) {
                //draw the graph background color 
                g2.setColor(backgroundColor);
                g2.fillRect(graphX1, graphY2, graphWidthPixels + 1, graphHeightPixels + 1); //+1 since SGT sometimes draws larger rect
            }

             //create the pane
            JPane jPane = new JPane("", new java.awt.Dimension(
                baseULXPixel + imageWidthPixels, 
                baseULYPixel + imageHeightPixels));
            jPane.setLayout(new StackedLayout());
            StringArray layerNames = new StringArray();

            //create the common graph parts
            //graph's physical location (start, end, delta); delta is ignored
            Range2D xPhysRange = new Range2D(
                baseULXPixel/dpi + graphULX, 
                baseULXPixel/dpi + graphULX + graphWidth,  1); 
            Range2D yPhysRange = new Range2D(
                (transparent? 0:
                    legendPosition == SgtUtil.LEGEND_BELOW? legendBoxHeight/dpi : 0) + 
                    graphBottomY, 
                imageHeightInches - graphULY, 1); 
            Dimension2D layerDimension2D = new Dimension2D(
                baseULXPixel/dpi + imageWidthInches, 
                baseULYPixel/dpi + imageHeightInches);

            //redefine some graph parts for this graph with SoT objects -- minX vs beginX
            SoTRange xUserRange = xIsTimeAxis?
                (SoTRange)new SoTRange.Time((long)(beginX * scaleXIfTime), (long)(endX * scaleXIfTime)):
                (SoTRange)new SoTRange.Double(beginX, endX, (xAscending? 1 : -1) * xDivisions[0]);
            SoTRange yUserRange = yIsTimeAxis?
                (SoTRange)new SoTRange.Time((long)(beginY * scaleYIfTime), (long)(endY * scaleYIfTime)):
                (SoTRange)new SoTRange.Double(beginY, endY, (yAscending? 1 : -1) * yDivisions[0]);
            gov.noaa.pmel.sgt.AxisTransform xt = xIsLogAxis?
                new gov.noaa.pmel.sgt.LogTransform(   xPhysRange, xUserRange) :
                new gov.noaa.pmel.sgt.LinearTransform(xPhysRange, xUserRange);
            gov.noaa.pmel.sgt.AxisTransform yt = yIsLogAxis?
                new gov.noaa.pmel.sgt.LogTransform(   yPhysRange, yUserRange) :
                new gov.noaa.pmel.sgt.LinearTransform(yPhysRange, yUserRange);
            SoTPoint origin2 = new SoTPoint( //where are axes drawn?
                xIsTimeAxis? (SoTValue)new SoTValue.Time((long)(beginX * scaleXIfTime)) : (SoTValue)new SoTValue.Double(beginX), 
                yIsTimeAxis? (SoTValue)new SoTValue.Time((long)(beginY * scaleYIfTime)) : (SoTValue)new SoTValue.Double(beginY));

            //draw the point layers 
            int nTotalValid = 0;
            for (int gdli = 0; gdli < graphDataLayers.size(); gdli++) {

                long gdlTime = System.currentTimeMillis();
             
                //prepare to plot the data              
                GraphDataLayer gdl = (GraphDataLayer)graphDataLayers.get(gdli);              
                int tMarkerSize = rint(gdl.markerSize * fontScale); 
                boolean drawMarkers = gdl.draw == GraphDataLayer.DRAW_MARKERS;
                boolean drawLines = gdl.draw == GraphDataLayer.DRAW_LINES;
                boolean drawMarkersAndLines = gdl.draw == GraphDataLayer.DRAW_MARKERS_AND_LINES;
                boolean drawSticks = gdl.draw == GraphDataLayer.DRAW_STICKS;               
                boolean drawColoredSurface = gdl.draw == GraphDataLayer.DRAW_COLORED_SURFACE ||
                    gdl.draw == GraphDataLayer.DRAW_COLORED_SURFACE_AND_CONTOUR_LINES;
                boolean drawContourLines = gdl.draw == GraphDataLayer.DRAW_CONTOUR_LINES ||
                    gdl.draw == GraphDataLayer.DRAW_COLORED_SURFACE_AND_CONTOUR_LINES;

                //clip to graph region
                g2.setClip(graphX1, graphY2, graphWidthPixels, graphHeightPixels);                        

                //plot tableData
                if (useTableData[gdli]) {
                    PrimitiveArray xPA = tables[gdli].getColumn(gdl.v1);
                    PrimitiveArray yPA = tables[gdli].getColumn(gdl.v2);
                    PrimitiveArray vPA = gdl.v3 >= 0? 
                        tables[gdli].getColumn(gdl.v3) : null;
                    //String2.log("vPA is null? " + (vPA==null) + " colorMap=" + gdl.colorMap);
                    //first single pt is wide. all others a dot.
                    int tWidenOnePoint = drawLines? widenOnePoint : 0; //0 if markers involved
                    tMarkerSize = rint(gdl.markerSize * fontScale); 
                    int n = Math.min(xPA.size(), yPA.size());
                    int gpState = 0; //0=needsMoveTo (0 pts), 1=needsLineTo (1pt), 2=last was lineTo (2+ pts)
                    int nValid = 0;
                    int itx = 0, ity = 0;
                    //String2.log("SgtGraph: draw point layer " + gdli);

                    //draw the mean line first (so underneath data) 
                    if (gdl.regressionType == GraphDataLayer.REGRESS_MEAN) {
                    
                        //calculate the mean of y
                        double[] stats = yPA.calculateStats();
                        double statsN = stats[PrimitiveArray.STATS_N]; 
                        if (statsN > 0) {
                            double mean = stats[PrimitiveArray.STATS_SUM] / statsN;
                            if (drawSticks) {
                                //for sticks, mean is "component mean"
                                //It is calculated from uMean and vMean (not from mean of vector lengths).
                                //This matches method used to plot "average" vector on the graph.
                                //And this matches how NDBC calculates the "average" for a given hour
                                // (see https://www.ndbc.noaa.gov/measdes.shtml#stdmet).
                                stats = vPA.calculateStats();
                                statsN = stats[PrimitiveArray.STATS_N]; 
                                if (statsN > 0) {
                                    double vMean = stats[PrimitiveArray.STATS_SUM] / statsN;
                                    mean = Math.sqrt(mean*mean + vMean*vMean);
                                }
                            }
                            if (reallyVerbose) String2.log("  drawing mean=" + mean + 
                                " n=" + stats[PrimitiveArray.STATS_N] + 
                                " min=" + stats[PrimitiveArray.STATS_MIN] + 
                                " max=" + stats[PrimitiveArray.STATS_MAX]);
                            if (statsN > 0 && //check again, it may have changed
                                mean > minY && mean < maxY && 
                                //don't let mean line obscure no variability of data 
                                stats[PrimitiveArray.STATS_MIN] != stats[PrimitiveArray.STATS_MAX]) {
                                    
                                //draw the line
                                g2.setColor(SgtUtil.whiter(SgtUtil.whiter(SgtUtil.whiter(gdl.lineColor))));
                                cp.graphToDevice(minX * scaleXIfTime, mean * scaleYIfTime, dox1, doy1);
                                cp.graphToDevice(maxX * scaleXIfTime, mean * scaleYIfTime, dox2, doy2);
                                g2.drawLine(rint(dox1.d), rint(doy1.d),
                                    rint(dox2.d), rint(doy2.d));
                            }
                        }
                    }


                    //drawSticks
                    if (drawSticks) {
                        g2.setColor(gdl.lineColor);
                        for (int ni = 0; ni < n; ni++) {
                            double dtx = xPA.getDouble(ni); 
                            double dty = yPA.getDouble(ni);

                            //for sticksGraph, maxY=-minY (set above), 
                            double dtv = vPA.getDouble(ni);
                            if (Double.isNaN(dtx) || Double.isNaN(dty) || Double.isNaN(dtv))
                                continue;
                            nValid++;

                            //draw line from base to tip
                            cp.graphToDevice(dtx * scaleXIfTime, 0, dox1, doy1);
                            int cx1 = rint(dox1.d);
                            int cy1 = rint(doy1.d);
                            int cx2 = rint(dox1.d + cp.graphToDeviceYDistance(dty)); //u component
                            int cy2 = rint(doy1.d - cp.graphToDeviceYDistance(dtv)); //v component  "-" to move up 
                            g2.drawLine(cx1, cy1, cx2, cy2);

                            //return box around tip point
                            returnMinX.add(cx2 - 3);
                            returnMaxX.add(cx2 + 3);
                            returnMinY.add(cy2 - 3);
                            returnMaxY.add(cy2 + 3);
                            returnRowNumber.add(ni);
                            returnPointScreen.add(gdl.sourceID);

                        }
                    }

                    //drawMarkers || drawLines || drawMarkersAndLines
                    if (drawMarkers || drawLines || drawMarkersAndLines) {
                        g2.setColor(gdl.lineColor);
                        IntArray markerXs = new IntArray();
                        IntArray markerYs = new IntArray();
                        ArrayList markerInteriorColors = new ArrayList();
                        if (reallyVerbose) String2.log("  draw=" + GraphDataLayer.DRAW_NAMES[gdl.draw] + " n=" + n);
                        long accumTime = System.currentTimeMillis();
                        int oitx, oity;
                        for (int ni = 0; ni < n; ni++) {
                            double dtx = xPA.getDouble(ni);  
                            double dty = yPA.getDouble(ni);
                            double dtv = vPA == null? Double.NaN : vPA.getDouble(ni);

                            if (Double.isNaN(dtx) || Double.isNaN(dty)
                                //I think valid dtx dty with invalid dtv is hollow point
                                //|| (gpl.colorMap != null && Double.isNaN(dtv))
                                ) {
                                if (gpState == 1) { //gpState==1 only occurs if drawing lines
                                    //just one point (moveTo left hanging)? draw a mini horizontal line to right and left
                                    //itx, ity will still have last valid point
                                    g2.drawLine(itx - tWidenOnePoint, ity, itx + tWidenOnePoint, ity);
                                    tWidenOnePoint = 0;
                                }
                                gpState = 0;
                                continue;
                            }

                            //point is valid.  get int coordinates of point
                            nValid++;
                            cp.graphToDevice(dtx * scaleXIfTime, dty * scaleYIfTime, dox1, doy1);
                            oitx = itx;
                            oity = ity;
                            itx = rint(dox1.d);
                            ity = rint(doy1.d);

                            //draw line
                            if (drawLines || drawMarkersAndLines) {
                                if (gpState == 0) {
                                    gpState = 1;
                                } else {
                                    g2.drawLine(oitx, oity, itx, ity);
                                    gpState = 2;
                                }
                            }

                            //drawing markers? store location
                            if (drawMarkers || drawMarkersAndLines) {
                                markerXs.add(itx);
                                markerYs.add(ity);
                                markerInteriorColors.add(
                                    gdl.colorMap == null? gdl.lineColor : gdl.colorMap.getColor(dtv));
                            }

                            //return box around point
                            returnMinX.add(itx - 3);
                            returnMaxX.add(itx + 3);
                            returnMinY.add(ity - 3);
                            returnMaxY.add(ity + 3);
                            returnRowNumber.add(ni);
                            returnPointScreen.add(gdl.sourceID);

                        }
                        if (reallyVerbose) String2.log("  accum time=" + (System.currentTimeMillis() - accumTime) + "ms");

                        if (gpState == 1) { //gpState==1 only occurs if drawing lines
                            //just one point (moveTo left hanging)? draw a mini horizontal line to right and left
                            g2.drawLine(itx - tWidenOnePoint, ity, itx + tWidenOnePoint, ity);
                            tWidenOnePoint = 0;
                            gpState = 0;
                            //if (verbose) String2.log("  gpState=1! drawing final point");
                        }

                        //draw markers on top of lines
                        int nMarkerXs = markerXs.size();
                        if (nMarkerXs > 0) {
                            long markerTime = System.currentTimeMillis();
                            int nMarkerXs1 = nMarkerXs - 1;
                            int xarray[] = markerXs.array;
                            int yarray[] = markerYs.array;
                            for (int i = 0; i < nMarkerXs1; i++) {
                                //simple optimization:
                                //if next marker at same x,y, skip this one 
                                //(since other params constant or irrelevant)
                                if (xarray[i] != xarray[i+1] || yarray[i] != yarray[i+1]) {
                                    drawMarker(g2, gdl.markerType, tMarkerSize, 
                                        xarray[i], yarray[i],
                                        (Color)markerInteriorColors.get(i), gdl.lineColor);
                                }
                            }
                            //always draw last marker
                            drawMarker(g2, gdl.markerType, tMarkerSize, 
                                xarray[nMarkerXs1], yarray[nMarkerXs1],
                                (Color)markerInteriorColors.get(nMarkerXs1), gdl.lineColor);
                            if (reallyVerbose) String2.log("  draw markers n=" + nMarkerXs + 
                                " time=" + (System.currentTimeMillis() - markerTime) + "ms");
                        }

                    }

                    //update nTotalValid
                    nTotalValid += nValid;

                } else { //use grid data

                    /*if (scaleXIfTime != 1) {
                        double ar[] = gdl.grid1.lon;
                        int tn = ar.length;
                        for (int i = 0; i < tn; i++) 
                            ar[i] *= scaleXIfTime;
                    }
                    if (scaleYIfTime != 1) {
                        double ar[] = gdl.grid1.lat;
                        int tn = ar.length;
                        for (int i = 0; i < tn; i++) 
                            ar[i] *= scaleYIfTime;
                    }*/

                    if (drawColoredSurface) {
                        if (reallyVerbose) String2.log("  drawColoredSurface: " + gdl);
                        CompoundColorMap colorMap = (CompoundColorMap)gdl.colorMap;
                        CartesianGraph graph = new CartesianGraph("", xt, yt);
                        Layer layer = new Layer("coloredSurface", layerDimension2D);
                        layerNames.add(layer.getId());
                        jPane.add(layer);      //calls layer.setPane(this);
                        layer.setGraph(graph); //calls graph.setLayer(this);

                        if (xIsTimeAxis) {
                            graph.setClip(
                                new GeoDate(Math2.roundToLong(minX * scaleXIfTime)), 
                                new GeoDate(Math2.roundToLong(maxX * scaleXIfTime)),
                                minY * scaleYIfTime, maxY * scaleYIfTime);
                        } else if (yIsTimeAxis) {
                            graph.setClip( //it applies GeoDates to Y time axis
                                new GeoDate(Math2.roundToLong(minY * scaleYIfTime)),
                                new GeoDate(Math2.roundToLong(maxY * scaleYIfTime)),
                                minX * scaleXIfTime, maxX * scaleXIfTime);
                        } else {
                            graph.setClip(minX, maxX, minY, maxY);
                        }
                        graph.setClipping(true);

                        //get the Grid
                        SimpleGrid simpleGrid;
                        if (xIsTimeAxis) {
                            double ar[] = gdl.grid1.lon;
                            int n = ar.length;
                            GeoDate gDate[] = new GeoDate[n];
                            for (int i = 0; i < n; i++)
                                gDate[i] = new GeoDate((long)(ar[i] * scaleXIfTime));
                            simpleGrid = new SimpleGrid(gdl.grid1.data, 
                                gDate, gdl.grid1.lat, ""); //title
                            //String2.log(">>gdl.grid1.lat.length=" + gdl.grid1.lat.length);
                        } else if (yIsTimeAxis) {
                            double ar[] = gdl.grid1.lat;
                            int n = ar.length;
                            GeoDate gDate[] = new GeoDate[n];
                            for (int i = 0; i < n; i++)
                                gDate[i] = new GeoDate((long)(ar[i] * scaleYIfTime));
                            simpleGrid = new SimpleGrid(gdl.grid1.data, 
                                gdl.grid1.lon, gDate, ""); //title
                        } else {
                            simpleGrid = new SimpleGrid(gdl.grid1.data, 
                                gdl.grid1.lon, gdl.grid1.lat, ""); //title
                        }

                        //temp
                        if (reallyVerbose) {
                            gdl.grid1.calculateStats();
                            String2.log("  grid data min=" + gdl.grid1.minData + 
                                "  max=" + gdl.grid1.maxData + 
                                "  n=" + gdl.grid1.nValidPoints +
                                "\n  grid x min=" + gdl.grid1.lon[0] + " max=" + gdl.grid1.lon[gdl.grid1.lon.length - 1] +
                                "\n  grid y min=" + gdl.grid1.lat[0] + " max=" + gdl.grid1.lat[gdl.grid1.lat.length - 1]);
                        }

                        //assign the data 
                        graph.setData(simpleGrid, new GridAttribute(GridAttribute.RASTER, colorMap)); 

                    }
                    if (drawContourLines) {
                        CartesianGraph graph = new CartesianGraph("", xt, yt);
                        Layer layer = new Layer("contourLines", layerDimension2D);
                        layerNames.add(layer.getId());
                        jPane.add(layer);      //calls layer.setPane(this);
                        layer.setGraph(graph); //calls graph.setLayer(this);
                        //graph.setClip(minX * scaleXIfTime, maxX * scaleXIfTime,
                        //              minY * scaleXIfTime, maxY * scaleYIfTime);
                        //graph.setClipping(true);

                        //get the Grid
                        SimpleGrid simpleGrid = new SimpleGrid(gdl.grid1.data, 
                            gdl.grid1.lon, gdl.grid1.lat, ""); //title
                        gdl.grid1.calculateStats(); //so grid.minData maxData is correct
                        double gridMinData = gdl.grid1.minData;
                        double gridMaxData = gdl.grid1.maxData;
                        if (reallyVerbose)
                            String2.log("  contour minData=" + String2.genEFormat6(gridMinData) + 
                                " maxData=" + String2.genEFormat10(gridMaxData));

                        //assign the data 
                        //get color levels from colorMap
                        CompoundColorMap colorMap = (CompoundColorMap)gdl.colorMap;
                        int nLevels = colorMap.rangeLow.length + 1;
                        double levels[] = new double[nLevels];
                        System.arraycopy(colorMap.rangeLow, 0, levels, 0, nLevels - 1);
                        levels[nLevels - 1] = colorMap.rangeHigh[nLevels - 1];
                        DecimalFormat format = new DecimalFormat("#0.######");
                        ContourLevels contourLevels = new ContourLevels();
                        for (int i = 0; i < levels.length; i++) {
                            ContourLineAttribute contourLineAttribute = new ContourLineAttribute();
                            contourLineAttribute.setColor(gdl.lineColor);
                            contourLineAttribute.setLabelColor(gdl.lineColor);
                            contourLineAttribute.setLabelHeightP(fontScale * 0.15);
                            contourLineAttribute.setLabelFormat("%g"); //this seems to be active
                            contourLineAttribute.setLabelText(format.format(levels[i])); //this seems to be ignored
                            contourLevels.addLevel(levels[i], contourLineAttribute);
                        }
                        graph.setData(simpleGrid, new GridAttribute(contourLevels));
                        if (reallyVerbose) 
                            String2.log("  contour levels = " + String2.toCSSVString(levels));
                    }
                } //end use of grid data

                //change clip back to full area
                g2.setClip(baseULXPixel, baseULYPixel, imageWidthPixels, imageHeightPixels);

                //draw legend for this GraphDataLayer
                if (transparent || gdl.boldTitle == null) {
                    //no legend entry for this gdl

                } else if (legendPosition == SgtUtil.LEGEND_BELOW) {

                    //draw LEGEND_BELOW
                    g2.setColor(gdl.lineColor);

                    //draw colorMap (do first since colorMap shifts other things down)
                    if ((drawMarkers || drawMarkersAndLines || drawColoredSurface) && gdl.colorMap != null) {
                        //draw the color bar
                        CartesianGraph graph = new CartesianGraph("colorbar" + gdli, xt, yt);
                        Layer layer = new Layer("colorbar" + gdli, layerDimension2D);
                        layerNames.add(layer.getId());
                        jPane.add(layer);      //calls layer.setPane(this);
                        layer.setGraph(graph); //calls graph.setLayer(this);

                        legendTextY += labelHeightPixels; 
                        CompoundColorMapLayerChild lc = 
                            new CompoundColorMapLayerChild("", (CompoundColorMap)gdl.colorMap);
                        lc.setRectangle( //leftX,upperY(when rotated),width,height
                            layer.getXDtoP(legendTextX), layer.getYDtoP(legendTextY), 
                            imageWidthInches - (2 * legendInsideBorder + legendSampleSize)/dpi - 
                                betweenGraphAndColorBar, 
                            fontScale * 0.15); 
                        lc.setLabelFont(labelFont);
                        lc.setLabelHeightP(axisLabelHeight);
                        lc.setTicLength(fontScale * 0.02);
                        layer.addChild(lc);
                        legendTextY += 3 * labelHeightPixels; 
                    }
                        
                    //draw a line
                    if (drawLines || drawMarkersAndLines || drawSticks) {  
                        g2.drawLine(
                            legendTextX - legendSampleSize - legendInsideBorder, 
                            legendTextY - labelHeightPixels/2, 
                            legendTextX - legendInsideBorder, 
                            legendTextY - labelHeightPixels/2);
                    }

                    //then draw marker
                    if (drawMarkers || drawMarkersAndLines) {
                        int tx = legendTextX - legendInsideBorder - legendSampleSize / 2;
                        int ty = legendTextY - labelHeightPixels/2;
                        drawMarker(g2, gdl.markerType, tMarkerSize, tx, ty, 
                            gdl.colorMap == null? gdl.lineColor : 
                                gdl.colorMap.getColor((gdl.colorMap.getRange().start + gdl.colorMap.getRange().end) / 2), 
                            gdl.lineColor);
                    } 
                    //draw legend text
                    legendTextY = SgtUtil.belowLegendText(g2, legendTextX, legendTextY,
                        fontFamily, labelHeightPixels, 
                        SgtUtil.makeShortLines(maxBoldCharsPerLine, gdl.boldTitle, null, null),
                        SgtUtil.makeShortLines(maxCharsPerLine, gdl.title2, gdl.title3, gdl.title4));

                } else {  //draw LEGEND_RIGHT
                    g2.setColor(gdl.lineColor);

                    //draw a line
                    if (drawLines || drawMarkersAndLines || drawSticks) {
                        g2.drawLine(
                            legendTextX - legendSampleSize/2, 
                            legendTextY - labelHeightPixels*7/8, 
                            legendTextX + legendSampleSize/2, 
                            legendTextY - labelHeightPixels*7/8);
                        legendTextY += labelHeightPixels / 2; //for demo line
                    }

                    //draw a marker
                    if (drawMarkers || drawMarkersAndLines) {
                        int tx = legendTextX;
                        int ty = legendTextY - labelHeightPixels; 
                        drawMarker(g2, gdl.markerType, tMarkerSize, tx, ty, 
                            gdl.lineColor, gdl.lineColor);
                        legendTextY += labelHeightPixels;
                    } 
                    
                    //point legend text
                    legendTextY = SgtUtil.drawHtmlText(g2, legendTextX, legendTextY, 
                        1, fontFamily, labelHeightPixels, false, 
                        "<strong>" + SgtUtil.encodeAsHtml(gdl.boldTitle) + "</strong>");
                    legendTextY = SgtUtil.drawHtmlText(g2, legendTextX, legendTextY, 
                        1, fontFamily, labelHeightPixels, false, 
                        SgtUtil.encodeAsHtml(gdl.title2));
                    legendTextY = SgtUtil.drawHtmlText(g2, legendTextX, legendTextY, 
                        1, fontFamily, labelHeightPixels, true, 
                        SgtUtil.encodeAsHtml(gdl.title3));
                    legendTextY = SgtUtil.drawHtmlText(g2, legendTextX, legendTextY, 
                        1, fontFamily, labelHeightPixels, true, 
                        SgtUtil.encodeAsHtml(gdl.title4));
                }
                if (reallyVerbose)
                    String2.log("  graphDataLayer" + gdli + " time=" +
                        (System.currentTimeMillis() - gdlTime) + "ms");
            }

            if (reallyVerbose) String2.log("  set up the graph time=" + 
                (System.currentTimeMillis() - setupTime) + "ms");

            //*** set up graph with the AXIS LINES 
            //Drawing once avoids anti-aliasing problems when axis labels drawn 2+ times.
            //Draw this last, so axis lines drawn over data at the edges.
            //String2.log("SgtGraph.makeGraph before jPane.draw: " + Math2.memoryString());
            long drawGraphTime = System.currentTimeMillis();

            if (!transparent) {
                CartesianGraph graph = new CartesianGraph("", xt, yt);
                Layer layer = new Layer("axis", layerDimension2D);
                layerNames.add(layer.getId());
                jPane.add(layer);      //calls layer.setPane(this);
                layer.setGraph(graph); //calls graph.setLayer(this);
                //no clipping needed
                //DegreeMinuteFormatter dmf = new DegreeMinuteFormatter(); 

                //create the x axis
                Axis xAxis;
                if (xIsTimeAxis) {
                    //TimeAxis
                    TimeAxis timeAxis = new TimeAxis(TimeAxis.AUTO);
                    xAxis = timeAxis;
                } else if (xIsLogAxis) {
                    xAxis = new LogAxis("X");  //id
                } else {
                    //plainAxis
                    PlainAxis2 plainAxis = new PlainAxis2(new GenEFormatter());
                    xAxis = plainAxis;
                    //plainAxis.setRangeU(xUserRange);
                    //plainAxis.setLocationU(origin);
                    int nXSmallTics = Math2.roundToInt(xDivisions[0] / xDivisions[1]) - 1;
                    plainAxis.setNumberSmallTics(nXSmallTics); 
                    plainAxis.setLabelInterval(1);
                    //plainAxis.setLabelFormat("%g");
                    if (xAxisTitle != null && xAxisTitle.length() > 0) {
                        SGLabel xTitle = new SGLabel("", xAxisTitle, new Point2D.Double(0, 0));
                        xTitle.setHeightP(majorLabelRatio * axisLabelHeight);
                        xTitle.setFont(labelFont);
                        plainAxis.setTitle(xTitle);
                    }
                }
                xAxis.setRangeU(xUserRange);
                xAxis.setLocationU(origin2); //exception thrown here if x and y are time axes
                xAxis.setLabelFont(labelFont); 
                xAxis.setLabelHeightP(axisLabelHeight);
                xAxis.setSmallTicHeightP(fontScale * .02);
                xAxis.setLargeTicHeightP(fontScale * .05);
                graph.addXAxis(xAxis);
                if (xIsTimeAxis) {
                    TimeAxis timeAxis = (TimeAxis)xAxis;
                    if (reallyVerbose) String2.log(timeAxis.toString());
                    int minorInterval = timeAxis.getMinorLabelInterval();

                    if (narrowXGraph < 0.6) {
                        timeAxis.setMinorLabelInterval(minorInterval * 2);
                        if (reallyVerbose) String2.log("  x timeAxis minor interval (now doubled) was " + minorInterval);
                    } else if (narrowXGraph > 1.8) {
                        timeAxis.setMinorLabelInterval(Math.max(1, minorInterval / 2));
                        if (reallyVerbose) String2.log("  x timeAxis minor interval (now halved) was " + minorInterval);
                    } else {
                        if (reallyVerbose) String2.log("  x timeAxis minor interval is " + minorInterval);
                    }
                }

                //create the y axes
                Axis yAxis;
                if (yIsTimeAxis) {
                    //TimeAxis
                    TimeAxis timeAxis = new TimeAxis(TimeAxis.AUTO);
                    yAxis = timeAxis;
                    //timeAxis.setRangeU(yUserRange);
                    //timeAxis.setLocationU(origin);
                } else if (yIsLogAxis) {
                    yAxis = new LogAxis("Y");  //id
//                    yAxis.setLabelInterval(1);
                } else {
                    //plainAxis
                    PlainAxis2 plainAxis = new PlainAxis2(new GenEFormatter());
                    yAxis = plainAxis;
                    //plainAxis.setRangeU(yUserRange);
                    //plainAxis.setLocationU(origin);
                    int nYSmallTics = Math2.roundToInt(yDivisions[0] / yDivisions[1]) - 1;
                    plainAxis.setNumberSmallTics(nYSmallTics); 
                    plainAxis.setLabelInterval(1);
                    //plainAxis.setLabelFormat("%g");
                }
                if (!yIsTimeAxis && yAxisTitle != null && yAxisTitle.length() > 0) {
                    SGLabel yTitle = new SGLabel("", yAxisTitle, new Point2D.Double(0, 0));
                    yTitle.setHeightP(majorLabelRatio * axisLabelHeight);
                    yTitle.setFont(labelFont);
                    yAxis.setTitle(yTitle);
                }
                yAxis.setRangeU(yUserRange);
                yAxis.setLocationU(origin2);
                yAxis.setLabelFont(labelFont); 
                yAxis.setLabelHeightP(axisLabelHeight);
                yAxis.setSmallTicHeightP(fontScale * .02);
                yAxis.setLargeTicHeightP(fontScale * .05);
                graph.addYAxis(yAxis);
                if (yIsTimeAxis) {
                    TimeAxis timeAxis = (TimeAxis)yAxis;
                    int interval = timeAxis.getMinorLabelInterval();
                    if (reallyVerbose) String2.log("  y timeAxis interval (now doubled) was " + interval);
                    if (narrowYGraph < 0.6)
                        timeAxis.setMinorLabelInterval(interval * 2);
                    else if (narrowYGraph > 1.8)
                        timeAxis.setMinorLabelInterval(Math.max(1, interval / 2));
                }
            }

            //actually draw the graph
            jPane.draw(g2);  //comment out for memory leak tests

            //deconstruct jpane
            SgtMap.deconstructJPane("SgtGraph.makeGraph", jPane, layerNames);

            if (reallyVerbose) {
                String2.log("  draw the graph time=" + 
                    (System.currentTimeMillis() - drawGraphTime) + "ms");
                //String2.log("SgtGraph.makeGraph after jPane.draw: " + Math2.memoryString());
                //Math2.gcAndWait(); //a diagnostic in development.  outside of timing system.
                //String2.log("SgtGraph.makeGraph after gc: " + Math2.memoryString());
            }

            if (!transparent) {
                //draw the topX and rightY axis lines  (and main X Y if just lines)
                g2.setColor(Color.black);
                g2.drawLine(graphX1, graphY2, graphX2, graphY2);
                g2.drawLine(graphX2, graphY2, graphX2, graphY1);
                //if (!drawAxes) {
                //    g2.drawLine(graphX1, graphY1, graphX2, graphY1);
                //    g2.drawLine(graphX1, graphY1, graphX1, graphY2);
                //}
                if (!someUseGridData && nTotalValid == 0) {
                    g2.setFont(fontScale == 1? labelFont : labelFont.deriveFont(labelFont.getSize2D() * (float)fontScale));
                    g2.drawString("No Data", 
                        (graphX1 + graphX2) / 2 - Math2.roundToInt(17 * fontScale), 
                        (graphY1 + graphY2) / 2);
                }
            }

            //return to original RenderingHints           
            if (originalAntialiasing != null)
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAntialiasing); 

            //display time to makeGraph
            if (verbose) String2.log("}} SgtGraph.makeGraph done. TOTAL TIME=" + 
                (System.currentTimeMillis() - startTime) + "ms\n");
        } finally {
            g2.setClip(null);
        }

        return returnAL;
    }

    /**
     * This draws the requested marker at the requested position. 
     * g2.setColor must have been already used.
     * 
     * @param g2d 
     * @param markerType
     * @param markerSize2 the marker size in pixels (actually Graphics2D units)
     * @param x the center x
     * @param y the center y
     * @param interiorColor  use null if inactive, or to get hollow "filled" marker.
     *    For non-filled markers, this color is used as the line color. 
     * @param lineColor
     */
    public static void drawMarker(Graphics2D g2d, int markerType, int markerSize, 
        int x, int y, Color interiorColor, Color lineColor) {

        if (markerType <= GraphDataLayer.MARKER_TYPE_NONE) 
            return;

        //allow markers of any size (simple use of /2 forces to act like only even sizes)
        int m2 = markerSize / 2;
        int ulx = x - m2;
        int uly = y - m2;
        
        if (markerType == GraphDataLayer.MARKER_TYPE_SQUARE) {
            int xa[] = {ulx, ulx, ulx + markerSize, ulx + markerSize};
            int ya[] = {uly, uly + markerSize, uly + markerSize, uly};
            g2d.setColor(interiorColor == null? lineColor : interiorColor);
            g2d.drawPolygon(xa, ya, 4);
            return;
        }
        if (markerType == GraphDataLayer.MARKER_TYPE_FILLED_SQUARE) {
            int xa[] = {ulx, ulx, ulx + markerSize, ulx + markerSize};
            int ya[] = {uly, uly + markerSize, uly + markerSize, uly};
            if (interiorColor != null) {
                g2d.setColor(interiorColor);
                g2d.fillPolygon(xa, ya, 4);
            }
            g2d.setColor(lineColor);
            g2d.drawPolygon(xa, ya, 4);
            return;
        }
        if (markerType == GraphDataLayer.MARKER_TYPE_CIRCLE) {
            g2d.setColor(interiorColor == null? lineColor : interiorColor);
            g2d.drawOval(ulx, uly, markerSize, markerSize);
            return;
        }
        if (markerType == GraphDataLayer.MARKER_TYPE_FILLED_CIRCLE) {
            if (interiorColor != null) {
                g2d.setColor(interiorColor);
                g2d.fillOval(ulx, uly, markerSize, markerSize); 
            }
            g2d.setColor(lineColor);
            g2d.drawOval(ulx, uly, markerSize, markerSize); 
            return;
        }
        if (markerType == GraphDataLayer.MARKER_TYPE_UP_TRIANGLE) {
            int m21 = m2 + 1; //to make the size look same as others
            int xa[] = {x - m21, x,       x + m21}; //ensure symmetrical
            int ya[] = {y + m21, y - m21, y + m21};
            g2d.setColor(interiorColor == null? lineColor : interiorColor);
            g2d.drawPolygon(xa, ya, 3);
            return;
        }
        if (markerType == GraphDataLayer.MARKER_TYPE_FILLED_UP_TRIANGLE) {
            int m21 = m2 + 1; //to make the size look same as others
            int xa[] = {x - m21, x,       x + m21}; //ensure symmetrical
            int ya[] = {y + m21, y - m21, y + m21};
            if (interiorColor != null) {
                g2d.setColor(interiorColor);
                g2d.fillPolygon(xa, ya, 3);
            }
            g2d.setColor(lineColor);
            g2d.drawPolygon(xa, ya, 3);
            return;
        }
        if (markerType == GraphDataLayer.MARKER_TYPE_PLUS) {
            g2d.setColor(interiorColor == null? lineColor : interiorColor);
            g2d.drawLine(ulx, y, x + m2, y); //ensure symmetrical
            g2d.drawLine(x, uly, x, y + m2);
            return;
        }
        if (markerType == GraphDataLayer.MARKER_TYPE_X) {
            m2 = (markerSize - 1) / 2;  //-1 to make similar wt
            ulx = x - m2;
            uly = y - m2;
            g2d.setColor(interiorColor == null? lineColor : interiorColor);
            g2d.drawLine(ulx, uly, ulx + markerSize, uly + markerSize);
            g2d.drawLine(ulx, uly + markerSize, ulx + markerSize, uly);
            return;
        }
        if (markerType == GraphDataLayer.MARKER_TYPE_DOT) {
            g2d.setColor(interiorColor == null? lineColor : interiorColor);
            g2d.drawLine(x, y, x+1, y);
            return;
        }
    }


    /**
     * This uses SGT to make an image with a horizontal legend for one GraphDataLayer.
     *
     * @param xAxisTitle  null = none
     * @param yAxisTitle  null = none
     * @param legendPosition must be SgtUtil.LEGEND_BELOW (SgtUtil.LEGEND_RIGHT currently not supported)
     * @param legendTitle1 the first line of the legend
     * @param legendTitle2 the second line of the legend
     * @param imageDir the directory with the logo file 
     * @param logoImageFile the logo image file in the imageDir (should be square image) 
     *    (currently, must be png, gif, jpg, or bmp)
     *    (currently noaa-simple-40.gif for lowRes),
     *    or null for none.
     * @param graphDataLayer
     * @param g2 the graphics2D object to be used (the background need not 
     *    already have been drawn)
     * @param imageWidthPixels defines the image width, in pixels 
     * @param imageHeightPixels defines the image height, in pixels 
     * @param fontScale relative to 1=normalHeight
     * @throws Exception
     */
    public void makeLegend(String xAxisTitle, String yAxisTitle,
        int legendPosition, String legendTitle1, String legendTitle2,
        String imageDir, String logoImageFile,
        GraphDataLayer gdl,
        Graphics2D g2,
        int imageWidthPixels, int imageHeightPixels,
        double fontScale
        ) throws Exception {

        //Coordinates in SGT:
        //   Graph - 'U'ser coordinates      (graph's axes' coordinates)
        //   Layer - 'P'hysical coordinates  (e.g., pseudo-inches, 0,0 is lower left)
        //   JPane - 'D'evice coordinates    (pixels, 0,0 is upper left)

        if (legendTitle1 == null) 
            legendTitle1 = "";
        if (legendTitle2 == null) 
            legendTitle2 = "";

        //set the clip region
        g2.setClip(0, 0, imageWidthPixels, imageHeightPixels);
        try {
            if (reallyVerbose) String2.log("\n{{ SgtGraph.makeLegend "); // + Math2.memoryString());
            long startTime = System.currentTimeMillis();
            long setupTime = System.currentTimeMillis();

            double axisLabelHeight = fontScale * defaultAxisLabelHeight;
            double labelHeight     = fontScale * defaultLabelHeight;

            boolean sticksGraph = gdl.draw == GraphDataLayer.DRAW_STICKS;

            String error = "";

            //define sizes
            double dpi = 100; //dots per inch
            double imageWidthInches  = imageWidthPixels  / dpi;  
            double imageHeightInches = imageHeightPixels / dpi;
            int labelHeightPixels = Math2.roundToInt(labelHeight * dpi);
            double betweenGraphAndColorBar  = fontScale * .25;

            //set legend location and size (in pixels)  
            //standard length of vector (and other samples) in user units (e.g., inches)
            double legendSampleSizeInches = 0.22; //Don't change this (unless make other changes re vector length on graph)
            int legendSampleSize = Math2.roundToInt(legendSampleSizeInches * dpi); 
            int legendInsideBorder = Math2.roundToInt(fontScale * 0.1 * dpi); 
            int maxCharsPerLine = SgtUtil.maxCharsPerLine(
                imageWidthPixels - (legendSampleSize + 3 * legendInsideBorder), fontScale); 
            int maxBoldCharsPerLine = SgtUtil.maxBoldCharsPerLine(maxCharsPerLine);

            double legendLineCount = 
                String2.isSomething(legendTitle1 + legendTitle2)? 1 : -1; //for legend title   //???needs adjustment for larger font size
            legendLineCount += gdl.legendLineCount(maxCharsPerLine);
                //String2.log("legendLineCount=" + legendLineCount);
            int legendBoxULX = 0;
            int legendBoxULY = 0;

            //legendTextX and Y
            int legendTextX = legendBoxULX + legendSampleSize + 2 * legendInsideBorder; 
            int legendTextY = legendBoxULY + legendInsideBorder + labelHeightPixels;   
            //String2.log("SgtGraph baseULXPixel=" + baseULXPixel +  " baseULYPixel=" + baseULYPixel +
            //    "  imageWidth=" + imageWidthPixels + " imageHeight=" + imageHeightPixels +
            //    "\n  legend boxULX=" + legendBoxULX + " boxULY=" + legendBoxULY + 
            //    "\n  textX=" + legendTextX + " textY=" + legendTextY +
            //    " insideBorder=" + legendInsideBorder + " labelHeightPixels=" + labelHeightPixels);

            //create the label font
            Font labelFont = new Font(fontFamily, Font.PLAIN, 10); //Font.ITALIC

            //draw legend basics
            if (true) {
                //box for legend    
                g2.setColor(new Color(0xFFFFCC));       
                g2.fillRect(legendBoxULX, legendBoxULY, imageWidthPixels - 1, imageHeightPixels - 1);
                g2.setColor(Color.black);
                g2.drawRect(legendBoxULX, legendBoxULY, imageWidthPixels - 1, imageHeightPixels - 1);

                //legend titles
                if (String2.isSomething(legendTitle1 + legendTitle2)) {
                    if (legendPosition == SgtUtil.LEGEND_BELOW) {
                        //draw LEGEND_BELOW
                        legendTextY = SgtUtil.drawHtmlText(g2, legendTextX, legendTextY, 
                            0, fontFamily, labelHeightPixels * 3 / 2, false, 
                            "<strong><color=#2600aa>" + SgtUtil.encodeAsHtml(legendTitle1 + " " + 
                                legendTitle2) + "</color></strong>");
                        legendTextY += labelHeightPixels / 2;
                    } else {
                        //draw LEGEND_RIGHT
                        int tx = legendBoxULX + legendInsideBorder;
                        if (legendTitle1.length() > 0)
                            legendTextY = SgtUtil.drawHtmlText(g2, tx, legendTextY, 
                                0, fontFamily, labelHeightPixels * 5 / 4, false, 
                                "<strong><color=#2600aa>" + SgtUtil.encodeAsHtml(legendTitle1) + "</color></strong>");
                        if (legendTitle2.length() > 0)
                            legendTextY = SgtUtil.drawHtmlText(g2, tx, legendTextY, 
                                0, fontFamily, labelHeightPixels * 5 / 4, false, 
                                "<strong><color=#2600aa>" + SgtUtil.encodeAsHtml(legendTitle2) + "</color></strong>");
                        legendTextY += labelHeightPixels * 3 / 2;
                    }

                    //draw the logo
                    if (logoImageFile != null && File2.isFile(imageDir + logoImageFile)) {
                        long logoTime = System.currentTimeMillis();
                        BufferedImage bi2 = ImageIO.read(new File(imageDir + logoImageFile));

                        //g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        //                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        //                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        //                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                        int ulx = legendBoxULX + legendSampleSize / 2;
                        int uly = legendBoxULY + legendInsideBorder / 2;
                        int tSize = (int)(fontScale * 20);
                        g2.drawImage(bi2, ulx, uly, tSize, tSize, null); //null=ImageObserver 
                        //if (verbose) String2.log("  draw logo time=" + 
                        //    (System.currentTimeMillis() - logoTime) + "ms");
                    }
                }
            }

            //done?
            if (gdl.boldTitle == null) 
                return; 

             //create the pane
            JPane jPane = new JPane("", new java.awt.Dimension(
                imageWidthPixels, imageHeightPixels));
            jPane.setLayout(new StackedLayout());
            StringArray layerNames = new StringArray();
            Dimension2D layerDimension2D = new Dimension2D(imageWidthInches, imageHeightInches);

            
            //prepare to plot the data              
            int tMarkerSize = rint(gdl.markerSize * fontScale); 
            boolean drawMarkers = gdl.draw == GraphDataLayer.DRAW_MARKERS;
            boolean drawLines = gdl.draw == GraphDataLayer.DRAW_LINES;
            boolean drawMarkersAndLines = gdl.draw == GraphDataLayer.DRAW_MARKERS_AND_LINES;
            boolean drawSticks = gdl.draw == GraphDataLayer.DRAW_STICKS;               
            boolean drawColoredSurface = gdl.draw == GraphDataLayer.DRAW_COLORED_SURFACE ||
                gdl.draw == GraphDataLayer.DRAW_COLORED_SURFACE_AND_CONTOUR_LINES;
            boolean drawContourLines = gdl.draw == GraphDataLayer.DRAW_CONTOUR_LINES ||
                gdl.draw == GraphDataLayer.DRAW_COLORED_SURFACE_AND_CONTOUR_LINES;

            //useGridData
            if (drawColoredSurface) {
                if (reallyVerbose) String2.log("  drawColoredSurface: " + gdl);
                CompoundColorMap colorMap = (CompoundColorMap)gdl.colorMap;
                Layer layer = new Layer("coloredSurface", layerDimension2D);
                layerNames.add(layer.getId());
                jPane.add(layer);      //calls layer.setPane(this);

                //add a horizontal colorBar
                legendTextY += labelHeightPixels; 
                CompoundColorMapLayerChild ccmLayerChild = 
                    new CompoundColorMapLayerChild("", colorMap);
                ccmLayerChild.setRectangle( //leftX,upperY(when rotated),width,height
                    layer.getXDtoP(legendTextX), layer.getYDtoP(legendTextY), 
                    imageWidthInches - (2 * legendInsideBorder + legendSampleSize)/dpi - 
                        betweenGraphAndColorBar, 
                    fontScale * 0.15); 
                ccmLayerChild.setLabelFont(labelFont);
                ccmLayerChild.setLabelHeightP(axisLabelHeight);
                ccmLayerChild.setTicLength(fontScale * 0.02);
                layer.addChild(ccmLayerChild);
                legendTextY += 3 * labelHeightPixels; 
            }

            //draw colorMap (do first since colorMap shifts other things down)
            if ((drawMarkers || drawMarkersAndLines) && gdl.colorMap != null) {
                //draw the color bar
                Layer layer = new Layer("colorbar", layerDimension2D);
                layerNames.add(layer.getId());
                jPane.add(layer);      //calls layer.setPane(this);

                legendTextY += labelHeightPixels; 
                CompoundColorMapLayerChild lc = 
                    new CompoundColorMapLayerChild("", (CompoundColorMap)gdl.colorMap);
                lc.setRectangle( //leftX,upperY(when rotated),width,height
                    layer.getXDtoP(legendTextX), layer.getYDtoP(legendTextY), 
                    imageWidthInches - (2 * legendInsideBorder + legendSampleSize)/dpi - 
                        betweenGraphAndColorBar, 
                    fontScale * 0.15); 
                lc.setLabelFont(labelFont);
                lc.setLabelHeightP(axisLabelHeight);
                lc.setTicLength(fontScale * 0.02);
                layer.addChild(lc);
                legendTextY += 3 * labelHeightPixels; 
            }
                
            //draw a line
            //don't draw line if gdl.draw = DRAW_COLORED_SURFACE_AND_CONTOUR_LINES
            if (gdl.draw == GraphDataLayer.DRAW_CONTOUR_LINES || 
                drawLines || drawMarkersAndLines || drawSticks) {  
                g2.setColor(gdl.lineColor);
                g2.drawLine(
                    legendTextX - legendSampleSize - legendInsideBorder, 
                    legendTextY - labelHeightPixels/2, 
                    legendTextX - legendInsideBorder, 
                    legendTextY - labelHeightPixels/2);
            }

            //draw marker
            if (drawMarkers || drawMarkersAndLines) {
                int tx = legendTextX - legendInsideBorder - legendSampleSize / 2;
                int ty = legendTextY - labelHeightPixels/2;
                g2.setColor(gdl.lineColor);
                drawMarker(g2, gdl.markerType, tMarkerSize, tx, ty, 
                    gdl.colorMap == null? gdl.lineColor : 
                        gdl.colorMap.getColor((gdl.colorMap.getRange().start + gdl.colorMap.getRange().end) / 2), 
                    gdl.lineColor);
            } 

            //draw legend text
            g2.setColor(gdl.lineColor);
            legendTextY = SgtUtil.belowLegendText(g2, legendTextX, legendTextY,
                fontFamily, labelHeightPixels, 
                SgtUtil.makeShortLines(maxBoldCharsPerLine, gdl.boldTitle, null, null),
                SgtUtil.makeShortLines(maxCharsPerLine, gdl.title2, gdl.title3, gdl.title4));

            //actually draw the graph
            jPane.draw(g2);  //comment out for memory leak tests

            //deconstruct jPane
            SgtMap.deconstructJPane("SgtMap.makeLegend", jPane, layerNames);

            //return antialiasing to original
            //if (originalAntialiasing != null)
            //    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
            //        originalAntialiasing); 

            //display time to makeGraph
            if (verbose) String2.log("}} SgtGraph.makeLegend done. TOTAL TIME=" + 
                (System.currentTimeMillis() - startTime) + "ms\n");
        } finally {
            g2.setClip(null);
        }
    }


    

    /** This makes and displays lots of graph types. */
    public static void testDiverseGraphs(boolean testAllAndDisplay, boolean xIsLogAxis, boolean yIsLogAxis) throws Exception {
        verbose = true;
        reallyVerbose = true;
        PathCartesianRenderer.verbose = true;
        PathCartesianRenderer.reallyVerbose = true;
        //AttributedString2.verbose = true;
        long time = System.currentTimeMillis();
        String tempDir = SSR.getTempDirectory();
        SgtGraph sgtGraph = new SgtGraph("DejaVu Sans");  //"DejaVu Sans" "Bitstream Vera Sans"); //"SansSerif" is safe choice
        String imageDir = SSR.getContextDirectory() + //with / separator and / at the end
            "images/";

        int width = 800;  //2 graphs wide 
        int height = 1200;  //3 graphs high 

        //graph 1: make a data file with data
        PrimitiveArray xCol1 = PrimitiveArray.factory(new double[]{1.500e9, 1.501e9, 1.502e9, 1.503e9});
        PrimitiveArray yCol1 = PrimitiveArray.factory(new double[]{1, 29, Double.NaN, 23});
        Table table1 = new Table();
        table1.addColumn("X", xCol1);
        table1.addColumn("Y", yCol1);

        //graph 2: make a data file with data
        PrimitiveArray xCol2 = PrimitiveArray.factory(new double[]{0,1,2,3}); //comment out for another test
        //PrimitiveArray xCol2 = PrimitiveArray.factory(new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN}); //comment out for another test
        PrimitiveArray yCol2 = PrimitiveArray.factory(new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN}); //comment out for another test
        Table table2 = new Table();
        table2.addColumn("X", xCol2);
        table2.addColumn("Y", yCol2);

        //graph 3: make a data file with data
        PrimitiveArray xCol3 = PrimitiveArray.factory(new double[]{1.500e9, 1.501e9, 1.502e9, 1.503e9, 1.504e9, 1.505e9, 1.506e9});
        PrimitiveArray uCol3 = PrimitiveArray.factory(new double[]{1,2,3,4,5,6,7}); 
        PrimitiveArray vCol3 = PrimitiveArray.factory(new double[]{-3,-2,-1,0,1,2,3}); 
        Table table3 = new Table();
        table3.addColumn("X", xCol3);
        table3.addColumn("U", uCol3);
        table3.addColumn("V", vCol3);
        
        //graph 5: make a data file with two time columns
        PrimitiveArray xCol5 = PrimitiveArray.factory(new double[]{1.500e9, 1.501e9, 1.502e9, 1.503e9, 1.504e9, 1.505e9, 1.506e9});
        PrimitiveArray yCol5 = PrimitiveArray.factory(new double[]{1.500e9, 1.504e9, 1.501e9, 1.500e9, 1.503e9, 1.502e9, 1.506e9});
        Table table5 = new Table();
        table5.addColumn("X", xCol5);
        table5.addColumn("Y", yCol5);
        
        //graph 1: make a graphDataLayer with data for a time series line
        GraphDataLayer graphDataLayer = new GraphDataLayer(
            -1, //which pointScreen
            0, 1, -1, -1, -1, //x,y,  others unused
            GraphDataLayer.DRAW_MARKERS_AND_LINES, true, false,
            "Time", "Y Axis Title", //x,yAxisTitle  for now, always std units 
            "This is the really, really, extra long and very nice bold title.",             
            "This is a really, really, extra long and very nice title2.", 
            "This is a really, really, extra long and very nice title3.",
            "This is title4.",
            table1, null, null,
            null, new java.awt.Color(0x0099FF),
            GraphDataLayer.MARKER_TYPE_PLUS, GraphDataLayer.MARKER_SIZE_SMALL,
            0, //vectorStandard
            GraphDataLayer.REGRESS_MEAN);
        ArrayList graphDataLayers1 = new ArrayList();
        graphDataLayers1.add(graphDataLayer);

        //graph 1: plus 10 random points of each marker type
        int lastMarker = 9;
        for (int i = 1; i <= lastMarker; i++) {
            //make a data file with data
            PrimitiveArray txCol = new DoubleArray();
            PrimitiveArray tyCol = new DoubleArray();
            for (int j = 0; j < 10; j++) {
                txCol.addDouble(1.500e9 + Math.random() * .0025e9);
                tyCol.addDouble(15 + 10* Math.random());
            }
            Table ttable = new Table();
            ttable.addColumn("X", txCol);
            ttable.addColumn("Y", tyCol);

            graphDataLayer = new GraphDataLayer(
                -1, //which pointScreen
                0, 1, -1, -1, -1, //x,y,  others unused
                GraphDataLayer.DRAW_MARKERS, 
                true, false, //x,yIsTimeAxis
                "Time", "Y Axis Title", //x,yAxisTitle for now, always std units 
                null, //so not in legend
                "title2", "title3", "title4", //will be ignored
                ttable, null, null,
                null, new java.awt.Color(0x0099FF),
                i, GraphDataLayer.MARKER_SIZE_SMALL,
                0, //vectorStandard
                GraphDataLayer.REGRESS_NONE); //else mean line for each marker type
            graphDataLayers1.add(graphDataLayer);
        }
        
        //graph 2: make a graphDataLayer with no data
        graphDataLayer = new GraphDataLayer(
            -1, //which pointScreen
            0, 1, -1, -1, -1, //x,y, others unused
            GraphDataLayer.DRAW_MARKERS, 
            false, false, //x,yIsTimeAxis
            "X Axis Title", "Y Axis Title", //for now, always std units
            "This is the really, really, extra long and very nice bold title.",             
            "This is a really, really, extra long and very nice title2.", 
            "This is a really, really, extra long and very nice title3.",
            "title4",
            table2, null, null, 
            null, new java.awt.Color(0x0099FF),
            GraphDataLayer.MARKER_TYPE_FILLED_CIRCLE, GraphDataLayer.MARKER_SIZE_MEDIUM,
            0, //vectorStandard
            GraphDataLayer.REGRESS_MEAN);
        ArrayList graphDataLayers2 = new ArrayList();
        graphDataLayers2.add(graphDataLayer);

        //graph 3: make a graphDataLayer with data for a sticks graph
        graphDataLayer = new GraphDataLayer(
            -1, //which pointScreen
            0, 1, 2, -1, -1, //x,u,v
            GraphDataLayer.DRAW_STICKS, 
            true, false, //x,yIsTimeAxis
            "Time", "Y Axis Title", //for now, always std units
            "This is the really, really, extra long and very nice bold title.",             
            "This is a really, really, extra long and very nice title2.", 
            "This is a really, really, extra long and very nice title3.",
            "title4",
            table3, null, null,
            null, new java.awt.Color(0xFF9900),
            GraphDataLayer.MARKER_TYPE_NONE, 0,
            0, //vectorStandard
            GraphDataLayer.REGRESS_MEAN);
        ArrayList graphDataLayers3 = new ArrayList();
        graphDataLayers3.add(graphDataLayer);

        //graph 4: make a graphDataLayer with data for a time series line
        graphDataLayer = new GraphDataLayer(
            -1, //which pointScreen
            0, 1, -1, -1, -1, //x,y,  others unused
            GraphDataLayer.DRAW_MARKERS_AND_LINES, 
            true, false, //x,yIsTimeAxis
            "Time", "Y Axis Title", //for now, always std units
            "This is the  extra long and very nice bold title (REGRESS=NONE).",             
            "This is a really, really, extra long and very nice title2.", 
            "This is a really, really, extra long and very nice title3.",
            "title4",
            table1, null, null,
            null, new java.awt.Color(0x0099FF),
            GraphDataLayer.MARKER_TYPE_PLUS, GraphDataLayer.MARKER_SIZE_MEDIUM,
            0, //vectorStandard
            GraphDataLayer.REGRESS_NONE);
        ArrayList graphDataLayers4 = new ArrayList();
        graphDataLayers4.add(graphDataLayer);

        //graph 4: plus 10 random points of each marker type
        for (int i = 1; i <= lastMarker; i++) {
            //make a data file with data
            PrimitiveArray txCol = new DoubleArray();
            PrimitiveArray tyCol = new DoubleArray();
            for (int j = 0; j < 10; j++) {
                txCol.addDouble(1.500e9 + Math.random() * .0025e9);
                tyCol.addDouble(1.5 + Math.random());
            }
            Table ttable = new Table();
            ttable.addColumn("X", txCol);
            ttable.addColumn("Y", tyCol);

            graphDataLayer = new GraphDataLayer(
                -1, //which pointScreen
                0, 1, -1, -1, -1, //x,y,  others unused
                GraphDataLayer.DRAW_MARKERS, 
                true, false,//x,yIsTimeAxis
                "Time", "Y Axis Title", //for now, always std units
                null, //so no legend entry
                "title2", "title3", "title4", //will be ignored
                ttable, null, null,
                null, new java.awt.Color(0x0099FF),
                i, GraphDataLayer.MARKER_SIZE_MEDIUM,
                0, //vectorStandard
                GraphDataLayer.REGRESS_NONE);  //if MEAN, you get a line for each marker type
            graphDataLayers4.add(graphDataLayer);
        }

        //graph 5: make a graphDataLayer with data for a time:time graph
        graphDataLayer = new GraphDataLayer(
            -1, //which pointScreen
            0, 1, -1, -1, -1, //x,y
            GraphDataLayer.DRAW_MARKERS, 
            true, false,  //x,yIsTimeAxis
            "Time", "Y Axis Title", //for now, always std units
            "This is the long and very nice bold title.  REGRESS=NONE",             
            "This is a really, really, extra long and very nice title2.", 
            "This is a really, really, extra long and very nice title3.",
            "title4",
            table5, null, null,
            null, new java.awt.Color(0xFF0000),
            GraphDataLayer.MARKER_TYPE_FILLED_SQUARE, GraphDataLayer.MARKER_SIZE_SMALL,
            0, //vectorStandard
            GraphDataLayer.REGRESS_NONE);
        ArrayList graphDataLayers5 = new ArrayList();
        graphDataLayers5.add(graphDataLayer);

        //graph 6: make a graphDataLayer with data for a x=data, y=time line
        graphDataLayer = new GraphDataLayer(
            -1, //which pointScreen
            1, 0, -1, -1, -1, //x,y,  others unused         //x and y swapped from graph 1
            GraphDataLayer.DRAW_MARKERS_AND_LINES, false, true,
            "Time", "Y Axis Title", //x,yAxisTitle  for now, always std units 
            "This is the really, really, extra long and very nice bold title.",             
            "This is a really, really, extra long and very nice title2.", 
            "This is a really, really, extra long and very nice title3.",
            "title4",
            table1, null, null,
            null, new java.awt.Color(0x0099FF),
            GraphDataLayer.MARKER_TYPE_PLUS, GraphDataLayer.MARKER_SIZE_SMALL,
            0, //vectorStandard
            GraphDataLayer.REGRESS_MEAN);
        ArrayList graphDataLayers6 = new ArrayList();
        graphDataLayers6.add(graphDataLayer);

        //draw the graph with data
        BufferedImage bufferedImage  = SgtUtil.getBufferedImage(width+20, height+20);
        BufferedImage bufferedImage1 = SgtUtil.getBufferedImage(300, 300);
        BufferedImage bufferedImage2 = SgtUtil.getBufferedImage(300, 300);
        BufferedImage bufferedImage3 = SgtUtil.getBufferedImage(300, 300);
        BufferedImage bufferedImage4 = SgtUtil.getBufferedImage(300, 300);
        BufferedImage bufferedImage5 = SgtUtil.getBufferedImage(300, 300);
        BufferedImage bufferedImage6 = SgtUtil.getBufferedImage(300, 300);
        Graphics2D g2  = (Graphics2D)bufferedImage.getGraphics();
        Graphics2D g21 = (Graphics2D)bufferedImage1.getGraphics();
        Graphics2D g22 = (Graphics2D)bufferedImage2.getGraphics();
        Graphics2D g23 = (Graphics2D)bufferedImage3.getGraphics();
        Graphics2D g24 = (Graphics2D)bufferedImage4.getGraphics();
        Graphics2D g25 = (Graphics2D)bufferedImage5.getGraphics();
        Graphics2D g26 = (Graphics2D)bufferedImage6.getGraphics();

        //graph 1
        String2.log("Graph 1");
        sgtGraph.makeGraph(true, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Graph 1,", "x is TimeAxis",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, true, true,  xIsLogAxis, //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, true, false, yIsLogAxis, 
            graphDataLayers1,
            g21, 0, 0, 300, 300, 2, //graph width/height
            DefaultBackgroundColor, 1); //fontScale
        sgtGraph.makeGraph(false, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Graph 1,", "x is TimeAxis",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, true, true,  xIsLogAxis, //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, true, false, yIsLogAxis, 
            graphDataLayers1,
            g2, 0, 0, //upperLeft
            width/2, height/3, 2, //graph width/height
            DefaultBackgroundColor, 1); //fontScale
        
        //graph 2
        String2.log("Graph 2");
        sgtGraph.makeGraph(true, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Graph 2,", "no time, no data.",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, true, false, xIsLogAxis, //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, true, false, yIsLogAxis, 
            graphDataLayers2,
            g22, 0, 0, 300, 300, 2, //graph width/height
            DefaultBackgroundColor, 1.5); //fontScale
        sgtGraph.makeGraph(false, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Graph 2,", "no time, no data.",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, true, false, xIsLogAxis,  //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, true, false, yIsLogAxis,
            graphDataLayers2,
            g2, width/2 + 10, 0, //upper Right
            width/2, height/3, 2, //graph width/height
            DefaultBackgroundColor, 1.5); //fontScale

        //graph 3
        String2.log("Graph 3");
        sgtGraph.makeGraph(true, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Graph 3,", "stick graph",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, true, true,  xIsLogAxis, //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, true, false, yIsLogAxis,
            graphDataLayers3,
            g23, 0, 0, 300, 300, 2, //graph width/height
            DefaultBackgroundColor, 1); //fontScale
        sgtGraph.makeGraph(false, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Graph 3,", "stick graph",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, true, true,  xIsLogAxis, //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, true, false, yIsLogAxis, 
            graphDataLayers3,
            g2, 0, height/3, //mid Left
            width/2, height/3, 2, //graph width/height
            DefaultBackgroundColor, 1); //fontScale

        //graph 4
        String2.log("Graph 4");
        sgtGraph.makeGraph(true, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Graph 4,", "x is TimeAxis",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, true, true,  xIsLogAxis, //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, true, false, yIsLogAxis, 
            graphDataLayers4,
            g24, 0, 0, 300, 300, 2, //graph width/height
            DefaultBackgroundColor, 1); //fontScale
        sgtGraph.makeGraph(false, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Graph 4,", "x is TimeAxis",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, true, true,  xIsLogAxis, //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, true, false, yIsLogAxis,
            graphDataLayers4,
            g2, width/2 + 10, height/3, //mid Right
            width/2, height/3, 2, //graph width/height
            DefaultBackgroundColor, 1); //fontScale

        //graph 5
        String2.log("Graph 5");
        sgtGraph.makeGraph(true, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Graph 5,", "2 time axis! y->not",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, true, true, xIsLogAxis,  //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, true, true, yIsLogAxis,
            graphDataLayers5,
            g25, 0, 0, 300, 300, 2, //graph width/height
            DefaultBackgroundColor, 1); //fontScale
        sgtGraph.makeGraph(false, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Graph 5,", "2 time axis! y->not",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, true, true, xIsLogAxis,  //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, true, true, yIsLogAxis,
            graphDataLayers5,
            g2, 0, height*2/3, //low left
            width/2, height/3, 2, //graph width/height
            DefaultBackgroundColor, 1); //fontScale

        //graph 6
        String2.log("Graph 6");
        sgtGraph.makeGraph(true, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Graph 6,", "y is TimeAxis",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, true, false, xIsLogAxis,  //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, true, true,  yIsLogAxis,
            graphDataLayers6,
            g26, 0, 0, 300, 300, 2, //graph width/height
            DefaultBackgroundColor, 1); //fontScale
        sgtGraph.makeGraph(false, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Graph 6,", "y is TimeAxis",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, true, false, xIsLogAxis,  //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, true, true,  yIsLogAxis,
            graphDataLayers6,
            g2, width/2 + 10, height*2/3, //low right
            width/2, height/3, 2, //graph width/height
            DefaultBackgroundColor, 1); //fontScale

        //make sure old files are deleted
        String fileName = tempDir + "SgtGraphTest";
        File2.delete(fileName + ".png");
        File2.delete(fileName + "1.png");
        File2.delete(fileName + "2.png");
        File2.delete(fileName + "3.png");
        File2.delete(fileName + "4.png");
        File2.delete(fileName + "5.png");
        File2.delete(fileName + "6.png");

        //save image
        SgtUtil.saveImage(bufferedImage, fileName + ".png");
        if (testAllAndDisplay) {
            SgtUtil.saveImage(bufferedImage1, fileName + "1.png");
            SgtUtil.saveImage(bufferedImage2, fileName + "2.png");
            SgtUtil.saveImage(bufferedImage3, fileName + "3.png");
            SgtUtil.saveImage(bufferedImage4, fileName + "4.png");
            SgtUtil.saveImage(bufferedImage5, fileName + "5.png");
            SgtUtil.saveImage(bufferedImage6, fileName + "6.png");
        } else {
            String2.log("fileName=" + fileName + ".png");
        }

        //view it
        SSR.displayInBrowser("file://" + fileName + ".png");  Math2.sleep(2000);
        if (testAllAndDisplay) {
            SSR.displayInBrowser("file://" + fileName + "1.png"); Math2.sleep(400);
            SSR.displayInBrowser("file://" + fileName + "2.png"); Math2.sleep(400);
            SSR.displayInBrowser("file://" + fileName + "3.png"); Math2.sleep(400);
            SSR.displayInBrowser("file://" + fileName + "4.png"); Math2.sleep(400);
            SSR.displayInBrowser("file://" + fileName + "5.png"); Math2.sleep(400);
            SSR.displayInBrowser("file://" + fileName + "6.png"); Math2.sleep(400);
        }

        //done
        Math2.gcAndWait(); Math2.gcAndWait(); //part of a test.  Ensure all are garbage collected.
        String2.log("time=" + (System.currentTimeMillis() - time) + "ms\n" +
            Math2.memoryString());
    } 



    /**
     * Test for memory leak.
     * Make a series of images and .pdf's: 
     *   SSR.getTempDirectory() + temp* + testImageExtension and  temp*.pdf
     *
     */
    public static void testForMemoryLeak() throws Exception {

        verbose = true;
        reallyVerbose = true;
        PathCartesianRenderer.verbose = true;
        PathCartesianRenderer.reallyVerbose = true;
        boolean xIsLogAxis = false;
        boolean yIsLogAxis = false;
        //AttributedString2.verbose = true;
        long time = System.currentTimeMillis();
        String tempDir = SSR.getTempDirectory();
        SgtGraph sgtGraph = new SgtGraph("DejaVu Sans");  //"DejaVu Sans" "Bitstream Vera Sans"); //"SansSerif" is safe choice
        String imageDir = SSR.getContextDirectory() + //with / separator and / at the end
            "images/";

        int width = 400;  
        int height = 600; 

        //graph 1: make a data file with data
        PrimitiveArray xCol1 = PrimitiveArray.factory(new double[]{1.500e9, 1.501e9, 1.502e9, 1.503e9});
        PrimitiveArray yCol1 = PrimitiveArray.factory(new double[]{1.1, 2.9, Double.NaN, 2.3});
        Table table1 = new Table();
        table1.addColumn("X", xCol1);
        table1.addColumn("Y", yCol1);

        //graph 1: make a graphDataLayer with data for a time series line
        GraphDataLayer graphDataLayer = new GraphDataLayer(
            -1, //which pointScreen
            0, 1, -1, -1, -1, //x,y,  others unused
            GraphDataLayer.DRAW_MARKERS_AND_LINES, true, false,
            "Time", "Y Axis Title", //x,yAxisTitle  for now, always std units 
            "This is the really, really, extra long and very nice bold title.",             
            "This is a really, really, extra long and very nice title2.", 
            "This is a really, really, extra long and very nice title3.",
            "This is title4.",
            table1, null, null,
            null, new java.awt.Color(0x0099FF),
            GraphDataLayer.MARKER_TYPE_PLUS, GraphDataLayer.MARKER_SIZE_SMALL,
            0, //vectorStandard
            GraphDataLayer.REGRESS_MEAN);
        ArrayList graphDataLayers1 = new ArrayList();
        graphDataLayers1.add(graphDataLayer);

        //graph 1: plus 100 random points of each marker type
        int lastMarker = 9;
        for (int i = 1; i <= lastMarker; i++) {
            //make a data file with data
            PrimitiveArray txCol = new DoubleArray();
            PrimitiveArray tyCol = new DoubleArray();
            for (int j = 0; j < 100; j++) {
                txCol.addDouble(1.500e9 + Math.random() * .0025e9);
                tyCol.addDouble(1.5 + Math.random());
            }
            Table ttable = new Table();
            ttable.addColumn("X", txCol);
            ttable.addColumn("Y", tyCol);

            graphDataLayer = new GraphDataLayer(
                -1, //which pointScreen
                0, 1, -1, -1, -1, //x,y,  others unused
                GraphDataLayer.DRAW_MARKERS, 
                true, false, //x,yIsTimeAxis
                "Time", "Y Axis Title", //x,yAxisTitle for now, always std units 
                null, //so not in legend
                "title2", "title3", "title4", //will be ignored
                ttable, null, null,
                null, new java.awt.Color(0x0099FF),
                i, GraphDataLayer.MARKER_SIZE_SMALL,
                0, //vectorStandard
                GraphDataLayer.REGRESS_NONE); //else mean line for each marker type
            graphDataLayers1.add(graphDataLayer);
        }

        //draw lots of graphs       
        long baseMemory = 0;
        int nReps = 20;
        for (int rep = 0; rep < nReps; rep++) {   //do more reps for harder memory test

            //draw the graph with data
            BufferedImage bufferedImage1 = SgtUtil.getBufferedImage(width, height);
            Graphics2D g21 = (Graphics2D)bufferedImage1.getGraphics();
            sgtGraph.makeGraph(false, "xAxisTitle", "yAxisTitle",
                SgtUtil.LEGEND_BELOW, "Graph 1,", "x is TimeAxis",
                imageDir, "noaa20.gif",
                Double.NaN, Double.NaN, true, true,  xIsLogAxis, //isAscending, isTimeAxis, isLog
                Double.NaN, Double.NaN, true, false, yIsLogAxis, 
                graphDataLayers1,
                g21, 0, 0, //upperLeft
                width, height, 1, //graph width/height
                DefaultBackgroundColor, 1); //fontScale
            String fileName = tempDir + "SgtGraphMemoryTest" + rep + ".png";
            SgtUtil.saveImage(bufferedImage1, fileName);

            //view it in browser?
            if (rep == 0) 
                SSR.displayInBrowser("file://" + fileName);
        }

        //check memory usage
        for (int rep = 0; rep < nReps; rep++)
            File2.delete(tempDir + "SgtGraphMemoryTest" + rep + ".png");
        Math2.gcAndWait(); Math2.gcAndWait(); //in a test, before getMemoryInUse().  Ensure all garbage collected.
        long using = Math2.getMemoryInUse();
        if (baseMemory == 0) baseMemory = using;
        long lpr = (using - baseMemory)/nReps;
        String2.log("\n*** SgtGraph test for memory leak: nReps=" + nReps + 
                " memoryUsing=" + using + " leak/rep=" + lpr +
            "\nPress CtrlBreak in console window to generate hprof heap info.");
        if (lpr > 0)
            String2.pressEnterToContinue(); 
    }

    public static void testSurface(boolean xIsLogAxis, boolean yIsLogAxis) throws Exception {
        verbose = true;
        reallyVerbose = true;
        PathCartesianRenderer.verbose = true;
        PathCartesianRenderer.reallyVerbose = true;
        //AttributedString2.verbose = true;
        Math2.gcAndWait(); Math2.gcAndWait(); //in a test, before getMemoryInUse().  Ensure all garbage collected.
        long time = System.currentTimeMillis();
        long memoryInUse = Math2.getMemoryInUse();

        String tempDir = SSR.getTempDirectory();
        SgtGraph sgtGraph = new SgtGraph("DejaVu Sans");  //"DejaVu Sans" "Bitstream Vera Sans"); //"SansSerif" is safe choice
        String imageDir = SSR.getContextDirectory() + //with / separator and / at the end
            "images/";
       
        int width = 400;  
        int height = 300; 

        //make a grid
        Grid grid = new Grid();
        grid.lon = new double[]{1,2,3,4};
        grid.lat = new double[]{10,20,30,40,50};
        grid.data = new double[]{  //mv's are NaNs
            1,2,3,4, //leftmost column, going up
            5,6,7,8,
            9,10,11,12,
            13,14,15,16,
            17,18,19,20}; //rightmost column, going up
        grid.setLonLatSpacing();
        grid.calculateStats();

        CompoundColorMap cColorMap = new CompoundColorMap(
            //String baseDir, String palette, String scale, double minData, 
            //double maxData, int nSections, boolean continuous, String resultDir)
            SSR.getContextDirectory() + "WEB-INF/cptfiles/",
            "Rainbow", "linear", 0, 20, 5, true, 
            SSR.getTempDirectory());

        //graph 1: make a graphDataLayer with data for a time series line
        GraphDataLayer graphDataLayer = new GraphDataLayer(
            -1, //which pointScreen
            0, 1, -1, -1, -1, //x,y,  others unused
            GraphDataLayer.DRAW_COLORED_SURFACE, true, false,
            "X Axis", "Y Axis Title", //x,yAxisTitle  for now, always std units 
            "This is the really, really, extra long and very nice bold title.",             
            "This is a really, really, extra long and very nice title2.", 
            "This is a really, really, extra long and very nice title3.",
            "This is title4.",
            null, grid, null,
            cColorMap, new java.awt.Color(0x0099FF),
            GraphDataLayer.MARKER_TYPE_PLUS, GraphDataLayer.MARKER_SIZE_SMALL,
            0, //vectorStandard
            GraphDataLayer.REGRESS_NONE);
        ArrayList graphDataLayers1 = new ArrayList();
        graphDataLayers1.add(graphDataLayer);

        //draw the graph with data
        BufferedImage bufferedImage = SgtUtil.getBufferedImage(width*2, height*2);
        Graphics2D g2 = (Graphics2D)bufferedImage.getGraphics();

        sgtGraph.makeGraph(false, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Standard,", "some text",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, true, false, xIsLogAxis,  //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, true, false, yIsLogAxis,
            graphDataLayers1,
            g2, 0, 0, //upperLeft
            width, height, 2, //graph width/height
            new Color(0x808080), 1); //gray, fontScale
        
        sgtGraph.makeGraph(false, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Flip X,", "some text",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, false, false, xIsLogAxis,  //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, true,  false, yIsLogAxis,
            graphDataLayers1,
            g2, width, 0, //upperLeft
            width, height, 2, //graph width/height
            new Color(0x808080), 1); //gray, fontScale

        sgtGraph.makeGraph(false, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Flip Y", "some text",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, true,  false, xIsLogAxis,  //isAscending, isTimeAxis, isLog
            Double.NaN, Double.NaN, false, false, yIsLogAxis,
            graphDataLayers1,
            g2, 0, height, //upperLeft
            width, height, 2, //graph width/height
            new Color(0x808080), 1); //gray, fontScale

        sgtGraph.makeGraph(false, "xAxisTitle", "yAxisTitle",
            SgtUtil.LEGEND_BELOW, "Flip X & Y,", "some text",
            imageDir, "noaa20.gif",
            Double.NaN, Double.NaN, false, false, xIsLogAxis,  //isAscending, isTimeAxis false,
            Double.NaN, Double.NaN, false, false, yIsLogAxis,
            graphDataLayers1,
            g2, width, height, //upperLeft
            width, height, 2, //graph width/height
            new Color(0x808080), 1); //gray, fontScale

        //save image
        String fileName = tempDir + "SgtGraphTestSurface.png";
        SgtUtil.saveImage(bufferedImage, fileName);

        //view it
        SSR.displayInBrowser("file://" + fileName);
        Math2.sleep(2000);
        //String2.pressEnterToContinue(); 
        
        //delete files        
        File2.delete(fileName);

        //done
        Math2.gcAndWait(); Math2.gcAndWait(); //in a test, before getMemoryInUse().  Ensure all garbage collected.
        String2.log("time=" + (System.currentTimeMillis() - time) + "ms " +
            "changeInMemoryInUse=" + (Math2.getMemoryInUse() - memoryInUse));
    } 


    /** This tests SgtGraph. */ 
    public static void test() throws Exception {
        testDiverseGraphs(true, false, false); //testAllAndDisplay, xIsLogAxis, yIsLogAxis
        testDiverseGraphs(true, false, true);  
        testDiverseGraphs(true, true,  true);  
        testForMemoryLeak();
        testSurface(false, false); //xIsLogAxis, yIsLogAxis
        testSurface(false, true); 
        testSurface(true, true); 
    }

}
