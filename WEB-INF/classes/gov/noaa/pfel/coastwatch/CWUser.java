/* 
 * CWUser Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.array.ByteArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.ema.*;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import com.lowagie.text.PageSize;

import gov.noaa.pfel.coastwatch.griddata.*;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.*;
import gov.noaa.pfel.coastwatch.util.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * This is a collection of the things unique to a user's CWBrowser or CWBrowserSA session.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-09-12
 */
public class CWUser extends User  {


    private EmaClass emaClass;
    private MapScreen mapScreen;
    private GridScreen gridScreen;
    private ContourScreen contourScreen;
    private VectorScreen vectorScreen;
    private PointVectorScreen pointVectorScreen;
    private PointScreen pointScreen[];
    private TrajectoryScreen trajectoryScreen[];
    private EmaSelect  edit;
    private EmaButton  submitForm, resetAll, back;
    private boolean lonPM180;
    private static final int imageGap = 10; //gap, if there is a map and a graph
    private int nPointScreens;
    private int nTrajectoryScreens;


    /** 
     * The constructor for User. 
     */
    public CWUser(OneOf oneOf, Shared shared, HttpSession session, boolean doTally) {
        super(oneOf, shared, session, doTally);

        //my order of creation must match Edit options order (so editOption assigned is correct)
        emaClass = new EmaClass(oneOf.fullClassName(), oneOf.emaRB2(), oneOf.classRB2());
        int editOption = 0;
        mapScreen = new MapScreen(editOption++, oneOf, shared, emaClass, doTally);
        gridScreen = new GridScreen(editOption++, oneOf, shared, emaClass, doTally);
        contourScreen = new ContourScreen(editOption++, oneOf, shared, emaClass, doTally);
        vectorScreen = new VectorScreen(editOption++, oneOf, shared, emaClass, doTally);
        pointVectorScreen = new PointVectorScreen(editOption++, oneOf, shared, emaClass, doTally);
        nPointScreens = oneOf.nPointScreens();
        pointScreen = new PointScreen[nPointScreens];
        for (int ps = 0; ps < nPointScreens; ps++)
            pointScreen[ps] = new PointScreen(editOption++, ps + 1, oneOf, shared, emaClass, doTally);
        nTrajectoryScreens = oneOf.nTrajectoryScreens();
        trajectoryScreen = new TrajectoryScreen[nTrajectoryScreens];
        for (int ts = 0; ts < nTrajectoryScreens; ts++)
            trajectoryScreen[ts] = new TrajectoryScreen(editOption++, ts + 1, oneOf, shared, emaClass, doTally);

        //addAttribute(new EmaLabel(this, "instructions"));
        emaClass.addAttribute(edit        = new EmaSelect(emaClass, "edit"));
        emaClass.addAttribute(submitForm  = new EmaButton(emaClass, "submitForm"));
        emaClass.addAttribute(resetAll    = new EmaButton(emaClass, "resetAll"));
        emaClass.addAttribute(back        = new EmaButton(emaClass, "back"));

        Test.ensureNotNull(edit.getLabel(),       "edit.label is null.");
        Test.ensureNotNull(submitForm.getLabel(), "submitForm.label is null.");
        Test.ensureNotNull(resetAll.getLabel(),   "resetAll.label is null.");
        Test.ensureNotNull(back.getLabel(),       "back.label is null.");

        //addDefaults
        emaClass.addDefaultsToSession(session);

        lonPM180 = oneOf.lonPM180();
    }


    /**
     * This resets the Shared info for this user.
     * Because this is handled by one method (with one value passed in), 
     * it has the effect of synchronizing everything done within it.
     *
     * @param shared the new Shared object
     */
    public void setShared(Shared shared) {
        this.shared = shared;
        mapScreen.setShared(shared);
        gridScreen.setShared(shared);
        contourScreen.setShared(shared);
        vectorScreen.setShared(shared);
        pointVectorScreen.setShared(shared);
        for (int ps = 0; ps < nPointScreens; ps++)
          pointScreen[ps].setShared(shared);
        for (int ts = 0; ts < nTrajectoryScreens; ts++)
          trajectoryScreen[ts].setShared(shared);
    }


    /** This returns the user's emaClass object. */
    public EmaClass emaClass() {return emaClass;}

    /** This returns the user's mapScreen object. */
    public MapScreen mapScreen() {return mapScreen;}

    /** This returns the user's gridScreen object. */
    public GridScreen gridScreen() {return gridScreen;}

    /** This returns the user's contourScreen object. */
    public ContourScreen contourScreen() {return contourScreen;}

    /** This returns the user's vectorScreen object. */
    public VectorScreen vectorScreen() {return vectorScreen;}

    /** This returns the user's pointVectorScreen object. */
    public PointVectorScreen pointVectorScreen() {return pointVectorScreen;}

    /** This returns one of the user's pointScreen objects. */
    public PointScreen pointScreen(int which) {return pointScreen[which];}

    /** This returns one of the user's trajectoryScreen objects. */
    public TrajectoryScreen trajectoryScreen(int which) {return trajectoryScreen[which];}

    /** This returns the user's edit object. */
    public EmaSelect edit() {return edit;}

    /** This returns the user's submitForm object. */
    public EmaButton submitForm() {return submitForm;}

    /** This returns the user's emaButton object. */
    public EmaButton resetAll() {return resetAll;}

    /** This returns the user's back object. */
    public EmaButton back() {return back;}


    /**
     * This returns the number of requests made in this user session.
     *
     * @param session usually created with request.getSession()
     * @return the number of requests made in this user session
     *    (or -1 if defaults haven't even been set)
     */
    public int getNRequestsThisSession(HttpSession session) {
        return emaClass.getNRequestsThisSession(session);
    }

    /**
     * This handles a "request" from a user, storing incoming attributes
     * as session values.
     * This updates totalNRequests, totalProcessingTime, maxProcessingTime.
     *
     * @param request 
     * @return true if all the values on the form are valid
     */
    public boolean processRequest(HttpServletRequest request) throws Exception {   
        resetLastAccessTime();

        //before processRequest, note some previous values (from user session)
        String previousRegionValue = mapScreen().region.getValue(session);
        String previousDataSet = gridScreen().dataSet.getValue(session);
        String previousViewAnomaly = gridScreen().view.getValue(session);
        String previousContour = contourScreen().dataSet.getValue(session);
        String previousUnits = gridScreen().units.getValue(session);
        String previousContourUnits = contourScreen().units.getValue(session);
        String previousPointDataSet[] = new String[nPointScreens];
        String previousPointUnits[] = new String[nPointScreens];
        for (int ps = 0; ps < nPointScreens; ps++) {
          previousPointDataSet[ps] = pointScreen(ps).dataSet.getValue(session);
          previousPointUnits[ps] = pointScreen(ps).units.getValue(session);
        }
        //trajectoryScreen's DataSets don't require this type of adjustment

        //call standard processRequest
        boolean result = emaClass().processRequest(request);

        //was submitter a request to reset all settings for this client?
        String submitter = emaClass().getSubmitterButtonName(request);
        if (submitter.equals(resetAll().getName())) {
            emaClass().addDefaultsToSession(session);
        }

        //if grid dataset or view anomaly has changed ...
        String newGridDataSetValue = gridScreen().dataSet.getValue(session);
        if (oneOf.verbose())
            String2.log("dataSetValue=" + newGridDataSetValue + " previous=" + previousDataSet +
                "\nviewAnomaly new=" + gridScreen().view.getValue(session) + " previous=" + previousViewAnomaly);
        if ((!previousDataSet.equals(newGridDataSetValue) &&
                !shared.dataSetsAreSimilar(previousDataSet, newGridDataSetValue))) {
            //set invalid; they'll be set to default for new dataset later
            gridScreen().units.setValue(session, "");  //I considered: let it try to stay same  (e.g., SST -> SST Anom)
            gridScreen().palette.setValue(session, ""); 
            gridScreen().paletteScale.setValue(session, ""); 
            gridScreen().paletteMin.setValue(session, "");
            gridScreen().paletteMax.setValue(session, "");
        }
        if (!previousViewAnomaly.equals(gridScreen().view.getValue(session))) {
            //set invalid; they'll be set to default for new dataset later
            gridScreen().palette.setValue(session, ""); 
            gridScreen().paletteScale.setValue(session, ""); 
            gridScreen().paletteMin.setValue(session, "");
            gridScreen().paletteMax.setValue(session, "");
        }

        //if units changed, ...
        if (oneOf.verbose())
            String2.log("unitsValue=" + gridScreen().units.getValue(session) + 
                " previousUnits=" + previousUnits);
        if (!previousUnits.equals(gridScreen().units.getValue(session))) {
            //set invalid; they'll be set to default for new dataset later
            gridScreen().paletteMin.setValue(session, "");
            gridScreen().paletteMax.setValue(session, "");
        }

        //if contourDataset changed, ...
        String newContourDataSetValue = contourScreen().dataSet.getValue(session);
        if (!previousContour.equals(newContourDataSetValue) &&
            !shared.dataSetsAreSimilar(previousContour, newContourDataSetValue)) {
            //set invalid; they'll be set to default for new dataset later
            contourScreen().units.setValue(session, "");  //I considered: let it try to stay same  (e.g., SST -> SST Anom)
            contourScreen().drawLinesAt.setValue(session, ""); 
        }

        //if contourUnits changed, ...
        if (!previousContourUnits.equals(contourScreen().units.getValue(session))) {
            //set invalid; it'll be set to default for new dataset later
            contourScreen().drawLinesAt.setValue(session, ""); 
        }

        for (int ps = 0; ps < nPointScreens; ps++) {
            //if pointDataset changed ...
            if (oneOf.verbose())
                String2.log("pointScreen" + (ps + 1) + 
                    " dataSetValue=" + pointScreen(ps).dataSet.getValue(session) + 
                    " previousDataSetValue=" + previousPointDataSet[ps]);
            if (!previousPointDataSet[ps].equals(
                  pointScreen(ps).dataSet.getValue(session))) {
                pointScreen(ps).units.setValue(session, "");  
                pointScreen(ps).palette.setValue(session, ""); 
                pointScreen(ps).paletteScale.setValue(session, ""); 
            }

            //if pointUnits changed ...
            if (oneOf.verbose())
                String2.log("pointScreen" + (ps + 1) + 
                    " unitsValue=" + pointScreen(ps).units.getValue(session) + 
                    " previousUnits=" + previousPointUnits[ps]);
            if (!previousPointUnits[ps].equals(pointScreen(ps).units.getValue(session))) {
                //set invalid; they'll be fixed below
                pointScreen(ps).paletteMin.setValue(session, "");
                pointScreen(ps).paletteMax.setValue(session, "");
            }
        }

        //trajectoryScreens don't require that type of adjustment 

        //then... ensure min/max/X/Y are set to valid values for lonPM180 setting
        double MIN_RANGE = 0.01;   //inc is MIN_RANGE/10=0.01 
        double tMinX = mapScreen().minX.getDouble(session);
        double tMaxX = mapScreen().maxX.getDouble(session);
        double tMinY = mapScreen().minY.getDouble(session);
        double tMaxY = mapScreen().maxY.getDouble(session);
        if (!Double.isFinite(tMinX)) tMinX = String2.parseDouble(mapScreen().minX.getDefaultValue());
        if (!Double.isFinite(tMaxX)) tMaxX = String2.parseDouble(mapScreen().maxX.getDefaultValue());
        if (!Double.isFinite(tMinY)) tMinY = String2.parseDouble(mapScreen().minY.getDefaultValue());
        if (!Double.isFinite(tMaxY)) tMaxY = String2.parseDouble(mapScreen().maxY.getDefaultValue());
        if (lonPM180) {
            tMinX = Math2.looserAnglePM180(tMinX);
            tMaxX = Math2.looserAnglePM180(tMaxX);
        } else {
            tMinX = Math2.looserAngle0360(tMinX);
            tMaxX = Math2.looserAngle0360(tMaxX);
        }
        if (tMinX > tMaxX) {double d = tMinX; tMinX = tMaxX; tMaxX = d;}
        if (tMinY > tMaxY) {double d = tMinY; tMinY = tMaxY; tMaxY = d;}
        double xZoomInc = Math.max((tMaxX - tMinX) / 10, MIN_RANGE / 10); 
        double yZoomInc = Math.max((tMaxY - tMinY) / 10, MIN_RANGE / 10); 
        if (xZoomInc >= 1) xZoomInc = Math.rint(xZoomInc);
        if (yZoomInc >= 1) yZoomInc = Math.rint(yZoomInc);
        String2.log("xZoomInc=" + xZoomInc + " yZoomInc=" + yZoomInc);

        //deal with zoomIn 
        if (oneOf.verbose()) 
            String2.log(Math2.memoryString() + "\nsubmitter = " + submitter);
        if (submitter.equals(mapScreen().zoomIn.getName())) {
            //move min/max/X/Y inward by zoomInc 
            if (tMaxX - tMinX > MIN_RANGE) {tMinX += xZoomInc; tMaxX -= xZoomInc;}
            if (tMaxY - tMinY > MIN_RANGE) {tMinY += yZoomInc; tMaxY -= yZoomInc;
            }
        }

        //deal with zoomOut 
        if (submitter.equals(mapScreen().zoomOut.getName())) {
            tMinX -= xZoomInc; tMaxX += xZoomInc;
            tMinY -= yZoomInc; tMaxY += yZoomInc;
        }

        //deal with up 
        if (submitter.equals(mapScreen().moveNorth.getName()) && tMaxY < oneOf.regionMaxY()) {
            tMinY += yZoomInc; tMaxY += yZoomInc;
        }

        //deal with down 
        if (submitter.equals(mapScreen().moveSouth.getName()) && tMinY > oneOf.regionMinY()) {
            tMinY -= yZoomInc; tMaxY -= yZoomInc;
        }

        //deal with left
        if (submitter.equals(mapScreen().moveWest.getName()) && tMinX > oneOf.regionMinX()) {
            tMinX -= xZoomInc; tMaxX -= xZoomInc;
        }

        //deal with right 
        if (submitter.equals(mapScreen().moveEast.getName()) && tMaxX < oneOf.regionMaxX()) {
            tMinX += xZoomInc; tMaxX += xZoomInc;
        }

        //final validity check
        //find absolute min and max (they may be reversed to match pixels min/max)
        tMinX = Math2.minMax(oneOf.regionMinX(), oneOf.regionMaxX(), tMinX);
        tMaxX = Math2.minMax(oneOf.regionMinX(), oneOf.regionMaxX(), tMaxX);
        tMinY = Math2.minMax(oneOf.regionMinY(), oneOf.regionMaxY(), tMinY);
        tMaxY = Math2.minMax(oneOf.regionMinY(), oneOf.regionMaxY(), tMaxY);
        if (tMaxX - tMinX < MIN_RANGE) {
            double average = (tMinX + tMaxX) / 2;
            tMinX = Math2.minMax(oneOf.regionMinX(), oneOf.regionMaxX(), average - MIN_RANGE / 2);
            tMaxX = Math2.minMax(oneOf.regionMinX(), oneOf.regionMaxX(), average + MIN_RANGE / 2);
        }
        if (tMaxY - tMinY < MIN_RANGE) {
            double average = (tMinY + tMaxY) / 2;
            tMinY = Math2.minMax(oneOf.regionMinY(), oneOf.regionMaxY(), average - MIN_RANGE / 2);
            tMaxY = Math2.minMax(oneOf.regionMinY(), oneOf.regionMaxY(), average + MIN_RANGE / 2);
        }

        //did region value change?
        //[the logic of this section requires deep thinking]
        String regionValue = mapScreen().region.getValue(session);
        if (oneOf.verbose()) 
            String2.log("regionValue=" + regionValue + " previousRegion=" + previousRegionValue);
        if (regionValue.equals(previousRegionValue)) { 
            //form was submitted by user changing some other component
            //set region to a standard region?
            mapScreen().region.setValue(session, ""); //or unset if no match
            String tMinXString = String2.genEFormat6(tMinX); 
            String tMaxXString = String2.genEFormat6(tMaxX);
            String tMinYString = String2.genEFormat6(tMinY);
            String tMaxYString = String2.genEFormat6(tMaxY);
            for (int tRegion = 0; tRegion < oneOf.regionInfo().length; tRegion++) {
                if (tMinXString.equals(oneOf.regionInfo()[tRegion][1]) && 
                    tMaxXString.equals(oneOf.regionInfo()[tRegion][2]) && 
                    tMinYString.equals(oneOf.regionInfo()[tRegion][3]) && 
                    tMaxYString.equals(oneOf.regionInfo()[tRegion][4])) {
                    if (oneOf.verbose()) 
                        String2.log("match tRegion=" + tRegion);
                    mapScreen().region.setValue(session, mapScreen().region.getOption(tRegion));
                    break;
                }
            }
        } else { //form was submitted by user choosing a different region
            int tRegion = mapScreen().region.indexOf(regionValue);
            if (tRegion >= 0) { //it should be
                tMinX = String2.parseDouble(oneOf.regionInfo()[tRegion][1]);
                tMaxX = String2.parseDouble(oneOf.regionInfo()[tRegion][2]);
                tMinY = String2.parseDouble(oneOf.regionInfo()[tRegion][3]);
                tMaxY = String2.parseDouble(oneOf.regionInfo()[tRegion][4]);
            }
        }
        
        //save the changes
        mapScreen().minX.setValue(session, String2.genEFormat6(tMinX)); 
        mapScreen().maxX.setValue(session, String2.genEFormat6(tMaxX));
        mapScreen().minY.setValue(session, String2.genEFormat6(tMinY));
        mapScreen().maxY.setValue(session, String2.genEFormat6(tMaxY));

        return result;
    }

    /** 
     * This does most of the work (validate each screen, generate html,
     * and create the files).
     *
     * @param request is a request from a user
     * @param htmlSB 
     * @return true if successful (no exceptions thrown)
     */
    public boolean getHTMLForm(HttpServletRequest request, StringBuilder htmlSB, long startTime) {
        boolean succeeded = true;

        try {
            if (oneOf.verbose()) 
                String2.log("\n************ getHTMLForm");
                
            IntObject step      = new IntObject(1); //the next step number for the user
            IntObject rowNumber = new IntObject(0);

            //do some preliminary work on tRegion since it affects customRegionsImage
            int tRegion = mapScreen().region.indexOf(mapScreen.region.getValue(session));
            if (oneOf.verbose()) 
                String2.log("tRegion=" + tRegion + "=" + mapScreen.region.getValue(session));
            String customRegionsImage = createCustomRegionsImage(session); //used a few lines below
            boolean displayErrorMessages = false;//an EMA setting; always false
         
            //validate the 'edit' widget
            int editIndex = edit().getSelectedIndex(session);
            if (editIndex < 0) {
                editIndex = 0;
                edit().setValue(session, edit().getOption(0));
            }

            //show (show = editIndex, but equals -1 if a getXxx button pressed)
            //  Thus, show reflects which screen is shown to the user.
            int show = editIndex;
            String submitter = emaClass().getSubmitterButtonName(request);
            if (submitter.length() > 0) { 
                if (mapScreen.submitterIsAGetButton(submitter) || 
                    gridScreen.submitterIsAGetButton(submitter) || 
                    contourScreen.submitterIsAGetButton(submitter) || 
                    vectorScreen.submitterIsAGetButton(submitter) ||
                    pointVectorScreen.submitterIsAGetButton(submitter)) 
                    show = -1; 
                for (int ps = 0; ps < nPointScreens; ps++) 
                    if (pointScreen(ps).submitterIsAGetButton(submitter))
                        show = -1;
                for (int ts = 0; ts < nTrajectoryScreens; ts++) 
                    if (trajectoryScreen(ts).submitterIsAGetButton(submitter))
                        show = -1;
            }

            //display the 'edit' row on the form
            if (show >= 0) {
                //the start of the HTML form and the HTML Table
                htmlSB.append(emaClass().getStartOfHTMLForm());

                //the 'edit' row
                htmlSB.append(
                    "    <tr style=\"background-color:#" + oneOf.backgroundColor(0) + ";\">\n" + 
                    "      <td>" + edit.getLabel() + "&nbsp;</td>\n" +
                    "      <td style=\"width:90%;\">" + edit.getControl(edit.getValue(session)) + "</td>\n");
            }

            //did user click on the map?    (after 'show' is known)
            //get x,y from query
            String query = request.getQueryString();
            String2.log("Query: " + query);
            if (query != null && query.matches("\\d+,\\d+")) { //digits,digits
                String lastImageFileName = (String)session.getAttribute("lastImageFileName");
                if (lastImageFileName != null) {
                    IntArray graphLocation = OneOf.getGraphLocation(lastImageFileName);
                    if (graphLocation != null) {
                        double ttMinX = mapScreen.minX.getDouble(session);
                        double ttMaxX = mapScreen.maxX.getDouble(session);
                        double ttMinY = mapScreen.minY.getDouble(session);
                        double ttMaxY = mapScreen.maxY.getDouble(session);
                
                        int po = query.indexOf(',');
                        int pixelX = String2.parseInt(query.substring(0, po));
                        int pixelY = String2.parseInt(query.substring(po + 1));

                        int originXPixel = graphLocation.get(0);
                        int endXPixel = graphLocation.get(1);
                        int originYPixel = graphLocation.get(2);
                        int endYPixel = graphLocation.get(3);
                        double lon = ttMinX + ((ttMaxX - ttMinX) * (pixelX - originXPixel)) / (endXPixel - originXPixel);
                        double lat = ttMinY + ((ttMaxY - ttMinY) * (pixelY - originYPixel)) / (endYPixel - originYPixel);
                        lon = Math2.roundTo(lon, 3);
                        lat = Math2.roundTo(lat, 3);
                        String2.log("Edit==Grid? " + (show == gridScreen.editOption()) +
                            " clickX=" + pixelX + " y=" + pixelY + " -> lon=" + lon + " lat=" + lat +
                            "\n ttMinX=" + ttMinX + " ttMaxX=" + ttMaxX +
                            " ttMinY=" + ttMinY + " ttMaxY=" + ttMaxY +
                            "\n originXPixel=" + originXPixel + " endXPixel=" + endXPixel +
                            " originYPixel=" + originYPixel + " endYPixel=" + endYPixel);
                        boolean queryOk = lon >= ttMinX && lon <= ttMaxX && 
                                          lat >= ttMinY && lat <= ttMaxY;
                        if (show == gridScreen.editOption()) {
                            gridScreen.timeSeriesLon.setValue(session, queryOk? String2.genEFormat6(lon) : "");
                            gridScreen.timeSeriesLat.setValue(session, queryOk? String2.genEFormat6(lat) : "");
                        } // else if ...
                    } 
                }
            }

            if (show == mapScreen.editOption()) {//map only visible mapScreen because it works on min/max/X/Y HTML components
                int mapScreenRows = 8; 
                htmlSB.append(
                "      <td rowspan=\"" + mapScreenRows + "\" style=\"background-color:#FFFFFF\">\n" +
                "        <a href=\"" + emaClass.getUrl() + "\"" +
                //onclick was here
                ">" + //no \n or space gap before next thing

                //Don't use ismap=\"ismap\" to call server side processing if client side isn't working.
                //Now, isMap is used for user clicking on the map in the main image.
                "<img src=\"" + customRegionsImage + "\" id=\"regionsImage\" \n" + 
                "            alt=\"" + oneOf.regionsImageAlt() + "\" title=\"" + oneOf.regionsImageTitle() + "\"\n" +
                "            width=\"" + oneOf.regionsImageWidth() + "\" height=\"" + oneOf.regionsImageHeight() + "\"\n" + 
                /*
                //CLICK = CHANGE THE CENTER
                "        onclick=\"javascript:\n" +
                "          var form = document." + emaClass.getFormName() + ";\n" + 
                //get eventX/Y where user clicked on the regionsImage (0,0 at upper left)
                //the problem is that IE and Netscape do it differently
                //this solution is adapted from 
                //  http://www.faqts.com/knowledge_base/view.phtml/aid/9386/fid/122
                //default = center of image (no change)
                "          eventX = " + ((regionsImageMinXPixel + regionsImageMaxXPixel)/2) + ";\n" +
                "          eventY = " + ((regionsImageMinYPixel + regionsImageMaxYPixel)/2) + ";\n" +
                "          if (window.event) {\n" +
                "            eventX = window.event.offsetX;\n" +
                "            eventY = window.event.offsetY;\n" +
                //IE and Opera use this code
                //"            alert('upper handler: x=' + eventX + ' y=' + eventY);\n" +
                "          } else if (event.target) {\n" +
                "            eventX = event.clientX;\n" +
                "            eventY = event.clientY;\n" +
                "            var el = event.target;\n" +
                "            do {\n" +
                "              eventX -= el.offsetLeft;\n" +
                "              eventY -= el.offsetTop;\n" +
                "            } while ((el = el.offsetParent));\n" +
                //FireFox uses this code            
                //"            alert('lower handler: x=' + eventX + ' y=' + eventY);\n" +
                "          }\n" +
                "          var minXEl = form.minX;\n" +
                "          var maxXEl = form.maxX;\n" +
                "          var minYEl = form.minY;\n" +
                "          var maxYEl = form.maxY;\n" +
                "          if (!isFinite(minXEl.value)) minXEl.value = " + regionsImageMinXDegrees + ";\n" +
                "          if (!isFinite(maxXEl.value)) maxXEl.value = " + regionsImageMaxXDegrees + ";\n" +
                "          if (!isFinite(minYEl.value)) minYEl.value = " + regionsImageMaxYDegrees + ";\n" + //yes, reversed
                "          if (!isFinite(maxYEl.value)) maxYEl.value = " + regionsImageMinYDegrees + ";\n" +
                "          var xRange2 = Math.abs(maxXEl.value - minXEl.value) / 2;\n" +
                "          var yRange2 = Math.abs(maxYEl.value - minYEl.value) / 2;\n" +
                "          if (xRange2 == 0) xRange2 = 10;\n" +
                "          if (yRange2 == 0) yRange2 = 10;\n" +
                "          var centerX = " + regionsImageMinXDegrees + 
                    " + ((eventX - " + regionsImageMinXPixel + ")/" +
                    (regionsImageMaxXPixel - regionsImageMinXPixel) + 
                    ") * " + (regionsImageMaxXDegrees - regionsImageMinXDegrees) + ";\n" +
                "          var centerY = " + regionsImageMinYDegrees + 
                    " + ((eventY - " + regionsImageMinYPixel + ")/" +
                    (regionsImageMaxYPixel - regionsImageMinYPixel) + 
                    ") * " + (regionsImageMaxYDegrees - regionsImageMinYDegrees) + ";\n" +
                //Round to the nearest 0.5 (0.25 would probably also work well)   
                //GMT doesn't handle < 0.25
                //I wanted round to nearest .1, but *10 /10 leads to bruised numbers
                "          minXEl.value = Math.round((centerX - xRange2)*2) / 2;\n" +
                "          maxXEl.value = Math.round((centerX + xRange2)*2) / 2;\n" +
                "          minYEl.value = Math.round((centerY - yRange2)*2) / 2;\n" +
                "          maxYEl.value = Math.round((centerY + yRange2)*2) / 2;\n" +
                "          form.submit();\"\n" +
                */

                //end of image tag        end of 'a' tag
                //CLICK = A REGION       
                "            usemap=\"#regionCoordinates\"></a>\n" +

                oneOf.regionCoordinatesMap(emaClass.getFormName()) +
                "        <br><div style=\"text-align:center;\"><small>" + oneOf.regionsImageLabel() + "</small></div></td>\n");
               
            }

            if (show >= 0) {
                htmlSB.append(
                    "    </tr>\n");
                rowNumber.i++;
            }

            //if animation requested, set synchronizeTimes to true
            if (submitter.equals(mapScreen.animationViewIt.getName())) 
                mapScreen.synchronizeTimes.setValue(session, "true");

            //validate mapScreen first (region info need by other screens) and add 'show' screen to htmlSB  
            mapScreen.validate(session, show, step, rowNumber, htmlSB);
            double tMinX = mapScreen.minX.getDouble(session);
            double tMaxX = mapScreen.maxX.getDouble(session);
            double tMinY = mapScreen.minY.getDouble(session);
            double tMaxY = mapScreen.maxY.getDouble(session);
            String cWESNString = FileNameUtility.makeWESNString(tMinX, tMaxX, tMinY, tMaxY);  

            //validate the editIndex screen first (so timePeriod and centeredTime are known for synchronizing dates)
            String synchTimePeriod = null;
            String synchCenteredTime = null;
            String synchBeginDate = null;
            if (editIndex == gridScreen.editOption()) {
                gridScreen.validate(session, show, step, rowNumber, htmlSB,
                    tMinX, tMaxX, tMinY, tMaxY, cWESNString, mapScreen.imageSizeIndex);
                synchTimePeriod = gridScreen.timePeriod.getValue(session);
                synchCenteredTime = gridScreen.centeredTime.getValue(session);
                synchBeginDate = gridScreen.beginTime.getValue(session);
            } else if (editIndex == contourScreen.editOption()) {
                contourScreen.validate(   session, show, step, rowNumber, htmlSB,
                    tMinX, tMaxX, tMinY, tMaxY, cWESNString, mapScreen.imageSizeIndex);
                synchTimePeriod = contourScreen.timePeriod.getValue(session);
                synchCenteredTime = contourScreen.centeredTime.getValue(session);
            } else if (editIndex == vectorScreen.editOption()) {
                vectorScreen.validate(    session, show, step, rowNumber, htmlSB, 
                    tMinX, tMaxX, tMinY, tMaxY, cWESNString, mapScreen.imageSizeIndex);
                synchTimePeriod = vectorScreen.timePeriod.getValue(session);
                synchCenteredTime = vectorScreen.centeredTime.getValue(session);
            } else if (editIndex == pointVectorScreen.editOption()) {
                pointVectorScreen.validate(    session, show, step, rowNumber, htmlSB, 
                    tMinX, tMaxX, tMinY, tMaxY, cWESNString, mapScreen.imageSizeIndex);
                synchTimePeriod = pointVectorScreen.timePeriod.getValue(session);
                synchCenteredTime = pointVectorScreen.centeredTime.getValue(session);
                synchBeginDate  = pointVectorScreen.beginTime.getValue(session);
            } else {
                for (int ps = 0; ps < nPointScreens; ps++) {
                    PointScreen tps = pointScreen[ps];
                    if (editIndex == tps.editOption()) {
                        tps.validate(session, show, step, rowNumber, htmlSB, 
                            tMinX, tMaxX, tMinY, tMaxY, cWESNString);
                        synchTimePeriod =   tps.timePeriod.getValue(session);
                        synchCenteredTime = tps.centeredTime.getValue(session);
                        synchBeginDate  =   tps.beginTime.getValue(session);
                    }
                }
                for (int ts = 0; ts < nTrajectoryScreens; ts++) {
                    TrajectoryScreen tts = trajectoryScreen[ts];
                    if (editIndex == tts.editOption()) {
                        tts.validate(session, show, step, rowNumber, htmlSB, 
                            tMinX, tMaxX, tMinY, tMaxY, cWESNString);
                        //synchTimePeriod   = tts.timePeriod.getValue(session);
                        //synchCenteredTime = tts.endTime.getValue(session);
                        //synchBeginDate    = tts.startTime.getValue(session);
                        //String2.log("synchTimePeriod=" + synchTimePeriod + " synchCenteredTime=" + synchCenteredTime);
                    }
                }
            }

            //synchronize the other timePeriods
            if (mapScreen.synchronizeTimesValue && synchTimePeriod != null) {
                if (editIndex != gridScreen.editOption())    gridScreen.timePeriod.setValue(session, synchTimePeriod);
                //bathymetry has no date
                if (editIndex != contourScreen.editOption()) contourScreen.timePeriod.setValue(session, synchTimePeriod);
                if (editIndex != vectorScreen.editOption())  vectorScreen.timePeriod.setValue(session, synchTimePeriod);
                if (editIndex != pointVectorScreen.editOption())  pointVectorScreen.timePeriod.setValue(session, synchTimePeriod);
                for (int ps = 0; ps < nPointScreens; ps++) 
                    if (editIndex != pointScreen(ps).editOption())
                        pointScreen(ps).timePeriod.setValue(session, synchTimePeriod);
                //trajectoryScreen currently not involved 
            }
            //synchronize the other centeredTimes
            if (mapScreen.synchronizeTimesValue && synchCenteredTime != null) {
                if (editIndex != gridScreen.editOption())    
                    gridScreen.centeredTime.setValue(session, 
                        synchedTime(gridScreen.centeredTime.getValue(session), synchCenteredTime));
                //bathymetry has no centeredTime
                if (editIndex != contourScreen.editOption()) 
                    contourScreen.centeredTime.setValue(session, 
                        synchedTime(contourScreen.centeredTime.getValue(session), synchCenteredTime));
                if (editIndex != vectorScreen.editOption())  
                    vectorScreen.centeredTime.setValue(session, 
                        synchedTime(vectorScreen.centeredTime.getValue(session), synchCenteredTime));
                if (editIndex != pointVectorScreen.editOption())  
                    pointVectorScreen.centeredTime.setValue(session, 
                        synchedTime(pointVectorScreen.centeredTime.getValue(session), synchCenteredTime));
                for (int ps = 0; ps < nPointScreens; ps++) 
                    if (editIndex != pointScreen(ps).editOption())
                        pointScreen(ps).centeredTime.setValue(session, 
                            synchedTime(pointScreen(ps).centeredTime.getValue(session), synchCenteredTime));
                //trajectoryScreen currently not involved
            }
            //synchronize the other beginTimes
            if (mapScreen.synchronizeTimesValue && synchBeginDate != null) {
                if (editIndex != gridScreen.editOption()) gridScreen.beginTime.setValue(session, synchBeginDate);
                //bathymetry has no beginTime
                //if (editIndex != contourScreen.editOption()) contourScreen.beginTime.setValue(session, synchBeginDate);
                //if (editIndex != vectorScreen.editOption())  vectorScreen.beginTime.setValue(session, synchBeginDate);
                if (editIndex != pointVectorScreen.editOption())  pointVectorScreen.beginTime.setValue(session, synchBeginDate);
                for (int ps = 0; ps < nPointScreens; ps++) 
                    if (editIndex != pointScreen(ps).editOption())
                        pointScreen(ps).beginTime.setValue(session, synchBeginDate);
                //trajectoryScreen currently not involved
            }

            //validate all the screens and add 'show' screen to htmlSB  
            if (editIndex != gridScreen.editOption()) 
                gridScreen.validate(      session, show, step, rowNumber, htmlSB,
                    tMinX, tMaxX, tMinY, tMaxY, cWESNString, mapScreen.imageSizeIndex);
            if (editIndex != contourScreen.editOption()) 
                contourScreen.validate(   session, show, step, rowNumber, htmlSB,
                    tMinX, tMaxX, tMinY, tMaxY, cWESNString, mapScreen.imageSizeIndex);
            if (editIndex != vectorScreen.editOption()) 
                vectorScreen.validate(    session, show, step, rowNumber, htmlSB, 
                    tMinX, tMaxX, tMinY, tMaxY, cWESNString, mapScreen.imageSizeIndex);
            if (editIndex != pointVectorScreen.editOption()) 
                pointVectorScreen.validate(    session, show, step, rowNumber, htmlSB, 
                    tMinX, tMaxX, tMinY, tMaxY, cWESNString, mapScreen.imageSizeIndex);
            for (int ps = 0; ps < nPointScreens; ps++) 
                if (editIndex != pointScreen(ps).editOption())
                    pointScreen(ps).validate( session, show, step, rowNumber, htmlSB,
                        tMinX, tMaxX, tMinY, tMaxY, cWESNString);
            for (int ts = 0; ts < nTrajectoryScreens; ts++) 
                if (editIndex != trajectoryScreen(ts).editOption())
                    trajectoryScreen(ts).validate( session, show, step, rowNumber, htmlSB,
                        tMinX, tMaxX, tMinY, tMaxY, cWESNString);

            //make the image;  this sets several instance variables
            ArrayList imageReturn = makeImage(synchCenteredTime);
            String imageFileName     = (String)imageReturn.get(0);
            Table sgtMapResultsTable = (Table)imageReturn.get(1);
            int imageWidth2          = ((Integer)imageReturn.get(2)).intValue();
            ArrayList mapGDLs        = (ArrayList)imageReturn.get(3);
            ArrayList graphGDLs      = (ArrayList)imageReturn.get(4);
            String warning           = (String)imageReturn.get(5);

            //store the imageFileName as the lastImageFileName for this user
            session.setAttribute("lastImageFileName", imageFileName);
         
            //end of table, end of form; display the image 
            if (show >= 0) {

                //display the map's Get options
                //This is a special case, since imageFileName isn't known until other screens are done.
                if (show == mapScreen.editOption()) 
                    mapScreen.displayGetOptions(session, show, step, rowNumber, htmlSB, imageFileName);

                //display submitForm button
                //This isn't beginning of row, just set the color.
                emaClass().setBeginRow("<tr style=\"background-color:#" + oneOf.backgroundColor(3) + "\">");
                //old way
                //but 'noscript' must be between td /td tags to be HTML compliant. 
                htmlSB.append("    <noscript>\n" + 
                    submitForm.getTableEntry(submitForm.getValue(session), displayErrorMessages) +
                    "    </noscript>\n");
                //new way
                //This causes thin empty row in table.
                //htmlSB.append(
                //    "    " + emaClass.getBeginRow() + "\n" + 
                //    "      <td><noscript>" + submitForm.getLabel() + "&nbsp;</noscript></td>\n" +
                //    "      <td><noscript>" + submitForm.getControl(submitForm().getValue(session)) + "</noscript></td>\n" +
                //    "    " + emaClass.getEndRow() + "\n");

                //end of form
                htmlSB.append(emaClass().getEndOfHTMLForm(startTime, 
                    mapScreen.hiddenInputStillPending));

                //generate the animated gif?  
                if (submitter.equals(mapScreen.animationViewIt.getName())) { 
                    String tName = createAnimatedGif(session, htmlSB);  
                    if (tName != null) 
                        imageFileName = tName;  
                }

                //display the map image (using isMap so gridScreen etc can lead to timeSeries)
                if (show == gridScreen.editOption()) {
                    htmlSB.append( //href so isMap knows where to send results
                        "<a href=\"" + emaClass.getUrl() + "\">" +
                        "  <img src=\"" + OneOf.PUBLIC_DIRECTORY + imageFileName + "\"\n" + 
                        "      title=\"" + oneOf.clickOnMapToSeeTimeSeries() + "\"\n" +
                        "      ismap=\"ismap\" alt=\"" + oneOf.clickOnMapToSeeTimeSeries() + "\"\n" + 
                        "      onclick=\"pleaseWait();\"\n" + 
                        "      width=\"" + imageWidth2 + "\" height=\"" + mapScreen.imageHeight + "\"></a>\n\n");
                } else {

                    //create stationsUseMap innards if there is info (but not first or last line) 
                    StringBuilder suMap = new StringBuilder();
                    if (sgtMapResultsTable != null && sgtMapResultsTable.nRows() > 0) {
                        //String2.log("sgtMapResultsTable=" + sgtMapResultsTable);
                        //sgtMapResultsTable.isValid(); // for debugging purposes only
                        IntArray minXIA        = (IntArray)sgtMapResultsTable.getColumn(0);
                        IntArray maxXIA        = (IntArray)sgtMapResultsTable.getColumn(1);
                        IntArray minYIA        = (IntArray)sgtMapResultsTable.getColumn(2);
                        IntArray maxYIA        = (IntArray)sgtMapResultsTable.getColumn(3);
                        IntArray rowNumberIA   = (IntArray)sgtMapResultsTable.getColumn(4);
                        IntArray sourceIA      = (IntArray)sgtMapResultsTable.getColumn(5);

                        //combine if duplicate min/max/X/Y
                        //first match found is used -- that is hard to take advantage of that here
                        //note that only markers for current pointScreen are put in <map>
                        //  If it were all pointScreens, it would be unclear which pointScreen to change.
                        //  Or, if change all, then no way to change for just one PointScreen. 
                        //  (And there are other complications about components for other PointScreens
                        //   not being on the html form, so how change.)
                        int tn = minXIA.size();
                        for (int i = 0; i < tn; i++) {
                            //# Pixel 0,0 at upper left
                            //# IE 5.2.3 for Mac OS X insists minX<maxX and minY<maxY
                            int sourceEditOption = sourceIA.array[i];
                            if (sourceEditOption != show) {
                                //not interested if sourceEditOption not visible screen 
                                continue;
                            }
                            if (sourceEditOption == pointVectorScreen.editOption()) {
                                if (pointVectorScreen.plotData) { 
                                    //String2.log("show=" + show + " i=" + i + " minXIA=" + minXIA.array[i] + " rowNumberIA=" + rowNumberIA.array[i]);
                                    suMap.append(
                                        "  <area shape=\"rect\" coords=\"" + 
                                            minXIA.array[i] + "," + minYIA.array[i] + "," +         
                                            maxXIA.array[i] + "," + maxYIA.array[i] + "\"\n" +
                                        "    title=\"Click to plot the time series for " + 
                                            (pointVectorScreen.timeSeriesOptions[
                                                rowNumberIA.array[i] + 1]) + //+1 since option 0 is ""
                                            "\"\n" + 
                                        "    alt=\"Click to plot the time series for " + 
                                            (pointVectorScreen.timeSeriesOptions[
                                                rowNumberIA.array[i] + 1]) + //+1 since option 0 is ""
                                            "\"\n" +                                         
                                        "    href=\"#\" " +   // was href=\"javascript:
                                            "onClick=\"" + 
                                            "document." + emaClass.getFormName() + ".pointVectorTimeSeries.selectedIndex=" + 
                                            (rowNumberIA.array[i] + 1) + //+1 since option 0 is ""
                                            ";  document." + emaClass.getFormName() + ".submit();\">\n");
                                }
                            } else {
                                for (int ps = 0; ps < nPointScreens; ps++) {
                                    if (sourceEditOption == pointScreen(ps).editOption() &&
                                        pointScreen(ps).plotData) { 
                                        suMap.append(
                                            "  <area shape=\"rect\" coords=\"" + 
                                                minXIA.array[i] + "," + minYIA.array[i] + "," +         
                                                maxXIA.array[i] + "," + maxYIA.array[i] + "\"\n" +
                                            "    title=\"Click to plot the time series for " + 
                                                (pointScreen(ps).timeSeriesOptions[
                                                    rowNumberIA.array[i] + 1]) + //+1 since option 0 is ""
                                                "\"\n" + 
                                            "    alt=\"Click to plot the time series for " + 
                                                (pointScreen(ps).timeSeriesOptions[
                                                    rowNumberIA.array[i] + 1]) + //+1 since option 0 is ""
                                                "\"\n" +                                             
                                            "    href=\"#\" " +   // was href=\"javascript:
                                                "onClick=\"" + 
                                                "document." + emaClass.getFormName() + ".pointTimeSeries" + 
                                                (ps + 1) + ".selectedIndex=" + 
                                                (rowNumberIA.array[i] + 1) + //index is +1 since #0 is ""
                                                ";  document." + emaClass.getFormName() + ".submit();\">\n");
                                        break; //we're done looking through pointScreens
                                    }
                                }
                            }
                        }
                    }
                    
                    htmlSB.append(
                        "<img src=\"" + OneOf.PUBLIC_DIRECTORY + imageFileName + "\"\n" + 
                        //"      title=\"" + oneOf.hereIsAlt() + "\"\n" +
                        "      alt=\"" + oneOf.hereIsAlt() + "\"" +
                            (suMap.length() == 0? "" : " usemap=\"#stationCoordinates\"") +
                            "\n" + 
                        "      width=\"" + imageWidth2 + "\" height=\"" + mapScreen.imageHeight + "\">\n\n");
                    if (suMap.length() > 0) {
                        htmlSB.append("<map name=\"stationCoordinates\">\n");
                        htmlSB.append(suMap.toString());
                        htmlSB.append("</map>\n");
                    }

                }

            }

           //deal with show < 0
           if (show < 0) {
                //backButtonForm is useful in several situations.
                //The link to go back to CWBrowser.jsp often lead to jumble of previous values. [why?]
                //String oBack = "o back to editing the map.";
                //htmlSB.append("<p>Then, <a href=\"CWBrowser.jsp\" title=\"G" + oBack + "\">g" + oBack + "</a>\n");
                //(The browser's Back button goes to the previous page which is fine.)
                //So use a Back button:
                String backButtonForm =
                    emaClass.getStartOfHTMLForm() +
                    "<tr>\n" +
                    "  <td>" +
                        back.getLabel() + "\n" +
                        back.getControl(back.getValue(session)) + "\n" +
                    "  </td>\n" +
                    "  <td>&nbsp;</td>\n" + //to take up horizontal space
                    "</tr>\n" +
                    emaClass.getEndOfHTMLForm(startTime, 
                        mapScreen.hiddenInputStillPending) + "\n";

                //deal with 'get' requests
                boolean submitterHandled = true;
                if (submitter.equals(mapScreen.getPdf.getName())) { 
                    //getPDF
                    String pdfFileName = File2.getNameNoExtension(imageFileName) + ".pdf";
                    if (File2.touch(oneOf.fullPublicDirectory() + pdfFileName)) {
                        if (oneOf.verbose()) 
                            String2.log("reuse pdf file: " + pdfFileName);
                    } else {
                        //POLICY: because this procedure may be used in more than one thread,
                        //do work on unique temp files names using randomInt, then rename to proper file name.
                        //If procedure fails half way through, there won't be a half-finished file.
                        int randomInt = Math2.random(Integer.MAX_VALUE);

                        int pdfWidth1 = graphGDLs.size() > 0?
                            oneOf.pdfLandscapeWidths()[mapScreen.imageSizeIndex]:
                            oneOf.pdfPortraitWidths()[mapScreen.imageSizeIndex];
                        int pdfHeight = graphGDLs.size() > 0?
                            oneOf.pdfLandscapeHeights()[mapScreen.imageSizeIndex]:
                            oneOf.pdfPortraitHeights()[mapScreen.imageSizeIndex];
                        int pdfWidth2 = pdfWidth1;
                        int pdfGap = imageGap * 2;
                        if (graphGDLs.size() > 0) 
                            pdfWidth2 = pdfWidth2 * 2 + pdfGap;
                        double fontScale = SgtMap.PDF_FONTSCALE;
                            //was 1 + mapScreen.imageSizeIndex/(graphGDLs.size() > 0? 4.0 : 2.0); 

                        //??? The pdf's created here have odd colors,
                        //but the pdf's created in SgtMap.test are fine.
                        //What's the difference???
                        Object oar[] = SgtUtil.createPdf(
                            graphGDLs.size() > 0? PageSize.LETTER.rotate() : PageSize.LETTER, 
                            pdfWidth2, pdfHeight, 
                            oneOf.fullPublicDirectory() + randomInt + ".pdf");
                        Graphics2D g2d = (Graphics2D)oar[0];

                        //makeMap
                        makeMap(
                            oneOf.highResLogoImageFile(), //note highRes      GMT: noaa-420.ras
                            g2d, 0, 0, pdfWidth1, pdfHeight,
                            fontScale, mapGDLs);  

                        //make graphs
                        if (graphGDLs.size() > 0) 
                            makeGraphs(
                                oneOf.highResLogoImageFile(), 
                                g2d, pdfWidth1 + pdfGap, 0, pdfWidth1, pdfHeight,
                                fontScale, graphGDLs);

                        //warning
                        if (warning != null) 
                            AttributedString2.drawHtmlText(g2d, warning, 
                                pdfWidth2 / 2, fontScale * 10, //x,y
                                oneOf.fontFamily(), fontScale * 10, Color.red, 1);

                        //finish the pdf
                        SgtUtil.closePdf(oar);
                        File2.rename(oneOf.fullPublicDirectory(), randomInt + ".pdf", pdfFileName);

                    } 

                    //tell the user it is available
                    htmlSB.append(
                        "<p>Click on this link to view the .pdf file, or right click to download the file:\n" + 
                        "<a href=\"" + OneOf.PUBLIC_DIRECTORY + pdfFileName + "\"\n" +  
                            "      title=\"" + pdfFileName + "\">" + 
                            pdfFileName + "</a>\n" +
                        "\n" +
                        "<p>.pdf files can be viewed and printed with\n" + 
                        "<a href=\"https://get.adobe.com/reader/\">Acrobat Reader</a>,\n" +   
                        "a free program from Adobe.\n");
                    
                    //The link to go back to CWBrowser.jsp often lead to jumble of previous values. [why?]
                    //String oBack = "o back to editing the map.";
                    //htmlSB.append("<p>Then, <a href=\"CWBrowser.jsp\" title=\"G" + oBack + "\">g" + oBack + "</a>\n");
                    //(The browser's Back button goes to the previous page which is fine.)
                    //So use a Back button:
                    htmlSB.append(backButtonForm);

                } else if (gridScreen.submitterIsAGetButton(submitter)) {
                    gridScreen.respondToSubmitter(submitter, htmlSB, backButtonForm);
                } else if (contourScreen.submitterIsAGetButton(submitter)) {
                    contourScreen.respondToSubmitter(submitter, htmlSB, backButtonForm);
                } else if (vectorScreen.submitterIsAGetButton(submitter)) {
                    vectorScreen.respondToSubmitter(submitter, htmlSB, backButtonForm);
                } else if (pointVectorScreen.submitterIsAGetButton(submitter)) {
                    pointVectorScreen.respondToSubmitter(submitter, htmlSB, backButtonForm);
                } else submitterHandled = false;

                if (!submitterHandled) 
                    throw new Exception("Unexpected \"submitter\": " + submitter);
            } //end of show < 0


        } catch (Exception e) {
            succeeded = false;

            //display the error message to the user and print to log 
            String tError = MustBe.throwableToString(e);

            htmlSB.setLength(0);
            htmlSB.append( 
                oneOf.errorMessage1() + "\n" +
                e.toString() + "\n" +
                oneOf.errorMessage2() + "\n" +
                emaClass.getStartOfHTMLForm() +
                  "<tr>\n" +
                  "  <td>" +
                      //The link to go back to CWBrowser.jsp often lead to jumble of previous values. [why?]
                      //String GoBack = "Go back to editing the map.";
                      //"<a href=\"CWBrowser.jsp\" title=\"" + GoBack + "\">" + GoBack + "</a>" +
                      //(The browser's Back button goes to the previous page which is fine.)
                      //So use a Back button:
                      back.getLabel() + "\n" +
                      back.getControl(back.getValue(session)) + "\n" +
                      "<br>(" + resetAll.getLabel() + "\n" +
                      resetAll.getControl(resetAll.getValue(session)) + 
                          "&nbsp;)</td>\n" +
                  "  <td>&nbsp;</td>\n" + //to take up horizontal space
                  "</tr>\n" +
                emaClass.getEndOfHTMLForm(startTime, 
                    mapScreen.hiddenInputStillPending) + "\n" +
                "<p>&nbsp;<hr>\n" +
                "<pre>Details:\n" +
                "\n" + 
                tError +
                "</pre>\n"); 

            //send email
            oneOf.email(oneOf.emailEverythingTo(), 
                String2.ERROR + " in " + oneOf.shortClassName(), htmlSB.toString());

        } //end of 'catch'

        if (oneOf.verbose()) String2.log("************ getHTMLForm done. TOTAL TIME=" + 
            (System.currentTimeMillis() - startTime));

        return succeeded;

    }

    /**
     * This synchronizes an oldTime (regular time or climatology time (year=0001))
     * based on a new synchTime (regular time or climatology time (year=0001)).
     *
     * @return the revised time (or oldTime if trouble)
     */
    private static String synchedTime(String oldTime, String synchTime) {
        if (synchTime == null || synchTime.length() < 4 ||
            oldTime == null || oldTime.length() < 4)
            return oldTime;
        if (synchTime.substring(0, 4).equals("0001")) {
            //synchTime is climatology
            if (oldTime.substring(0, 4).equals("0001")) {
                //oldTime is climatology
                return synchTime; 
            } else {
                //oldTime is not climatology -- use the old year + synch month, date, time
                return oldTime.substring(0, 4) + synchTime.substring(4);
            }
        } else {
            //synchTime is not climatology
            if (oldTime.substring(0, 4).equals("0001")) {
                //oldTime is climatology  -- use the old year + synch month, date, time
                return oldTime.substring(0, 4) + synchTime.substring(4);
            } else {
                //oldTime is not climatology
                return synchTime; 
            }
        }
    }


    /** 
     * This makes the image based on the current settings.
     *
     * @param synchCenteredTime  the iso date time the data is supposed to be synchronized on
     * @return an ArrayList with:
     *    <br>0=String imageFileName  without dir (implied public directory); with extension
     *    <br>1=Table sgtMapResultsTable
     *    <br>2=Integer imageWidth2
     *    <br>3=ArrayList mapGDLs
     *    <br>4=ArrayList graphGDLs
     *    <br>5=String warning
     * @throws Exception if trouble
     */
    private ArrayList makeImage(String synchCenteredTime) throws Exception {

        //get the warning string
        String warning = getWarning(synchCenteredTime); //an instance variable

        //make the imageFileName
        String imageFileName = makeImageFileName();

        //update requestedGridFilesMap
        if (doTally) 
            oneOf.tally().add("Most Requested " + OneOf.imageExtension + " Files:", imageFileName);

        //gather mapGDLs  (for SgtMap)
        //if user selected it, plot it; even if no data, it will appear in legend  
        ArrayList mapGDLs = new ArrayList();
        GraphDataLayer gdl;
        for (int i = 0; i < nPointScreens; i++) {
            gdl = pointScreen(i).getMapGDL(); 
            if (gdl != null) 
                mapGDLs.add(gdl);
        }
        for (int i = 0; i < nTrajectoryScreens; i++) {
            gdl = trajectoryScreen(i).getMapGDL(); 
            if (gdl != null) 
                mapGDLs.add(gdl);
        }

        //gather graphGDLs (GraphDataLayers for SgtGraph)
        //if user selected it, plot it; even if no rows of data, it will appear in legend  
        ArrayList graphGDLs = new ArrayList(); //an instance variable
        gdl = gridScreen.getGraphGDL(); 
        if (gdl != null) 
            graphGDLs.add(gdl);
        gdl = pointVectorScreen.getGraphGDL(); 
        if (gdl != null) 
            graphGDLs.add(gdl);
        for (int i = 0; i < nPointScreens; i++) {
            gdl = pointScreen(i).getGraphGDL();
            if (gdl != null)
                graphGDLs.add(gdl);
        }
        for (int i = 0; i < nTrajectoryScreens; i++) {
            gdl = trajectoryScreen(i).getGraphGDL();
            if (gdl != null)
                graphGDLs.add(gdl);
        }

        int imageWidth1 = mapScreen.imageWidth;
        int imageWidth2 = imageWidth1;  //an instance variable
        if (graphGDLs.size() > 0) 
            imageWidth2 = imageWidth2 * 2 + imageGap;
          
        //use SGT to make the image (if it doesn't already exist)
        Table sgtMapResultsTable = null;  //an instance variable
        String sgtMapResultTableNcName = 
            oneOf.fullPrivateDirectory() + imageFileName + "_Results.nc";
        if (File2.touch(oneOf.fullPublicDirectory() + imageFileName)) {
            if (oneOf.verbose()) 
                String2.log("\nCWUser reusing " + OneOf.imageExtension + ": " + 
                    imageFileName);

            if (File2.touch(sgtMapResultTableNcName)) {
                sgtMapResultsTable = new Table();
                sgtMapResultsTable.readFlatNc(sgtMapResultTableNcName, null, 1); //standardizeWhat=1
            }
        } else {
            if (oneOf.verbose()) 
                String2.log("makeMap new imageFileName=" + imageFileName);

            //make the image
            BufferedImage bufferedImage = 
                SgtUtil.getBufferedImage(imageWidth2, mapScreen.imageHeight);
            Graphics2D g2d = (Graphics2D)bufferedImage.getGraphics();

            //makeMap
            ArrayList sgtMapResults = makeMap(
                oneOf.lowResLogoImageFile(), g2d,  
                0, 0, imageWidth1, mapScreen.imageHeight,
                1, mapGDLs);    //standard font size

            //store the results in a table
            sgtMapResultsTable = new Table();
            sgtMapResultsTable.addColumn("minX",          (PrimitiveArray)sgtMapResults.get(0));
            sgtMapResultsTable.addColumn("maxX",          (PrimitiveArray)sgtMapResults.get(1));
            sgtMapResultsTable.addColumn("minY",          (PrimitiveArray)sgtMapResults.get(2));
            sgtMapResultsTable.addColumn("maxY",          (PrimitiveArray)sgtMapResults.get(3));
            sgtMapResultsTable.addColumn("rowNumber",     (PrimitiveArray)sgtMapResults.get(4));
            sgtMapResultsTable.addColumn("pointLayer",    (PrimitiveArray)sgtMapResults.get(5));

            //Store the graphLocation information in graphLocationHashtable.
            //graphLocation is an IntArray: originX,endX,originY,endY.
            OneOf.putGraphLocation(imageFileName, (IntArray)sgtMapResults.get(6));

            //store sgtMapResultsTable
            if (sgtMapResultsTable.nRows() > 0) 
                sgtMapResultsTable.saveAsFlatNc(sgtMapResultTableNcName, "row"); //not "time"

            //makeGraphs
            String2.log("CWUser graphGDLs.size()=" + 
                graphGDLs.size());
            if (graphGDLs.size() > 0) 
                makeGraphs(
                    oneOf.lowResLogoImageFile(), g2d,  
                    imageWidth1 + imageGap, 0, imageWidth1, mapScreen.imageHeight,
                    1, graphGDLs);

            //warning
            if (warning != null) 
                AttributedString2.drawHtmlText(g2d, warning, imageWidth2 / 2, 10, 
                    oneOf.fontFamily(), 10, Color.red, 1);

            //save image
            SgtUtil.saveImage(bufferedImage, oneOf.fullPublicDirectory() + imageFileName);

        }

        //create the arrayList for the return
        ArrayList imageReturn = new ArrayList();
        imageReturn.add(imageFileName);
        imageReturn.add(sgtMapResultsTable);
        imageReturn.add(Integer.valueOf(imageWidth2));
        imageReturn.add(mapGDLs);
        imageReturn.add(graphGDLs);
        imageReturn.add(warning);
        return imageReturn;

    }

    /**
     * This generates the warning string (if dates not perfectly synchronized).
     *
     * @param synchCenteredTime  just for diagnostic purposes
     * @return the warning string (or null if no warning needed)
     */
    private String getWarning(String synchCenteredTime) {

        //determine level of timePeriod sychronization
        long startEndCount[] = {Long.MIN_VALUE, Long.MAX_VALUE, 0, Long.MAX_VALUE, Long.MIN_VALUE};
        boolean timePeriodsPerfectlySynchronized = true;
        //EEEEK!!! isClimatology not dealt with. Deal with below after real time period calculated???
        if (gridScreen.plotData && !gridScreen.isClimatology) {
            timePeriodsPerfectlySynchronized = adjustStartEndCount(
                startEndCount, 
                gridScreen.startCalendar.getTimeInMillis(),
                gridScreen.endCalendar.getTimeInMillis(),
                timePeriodsPerfectlySynchronized);
        }
        if (contourScreen.plotData && !contourScreen.isClimatology) {
            timePeriodsPerfectlySynchronized = adjustStartEndCount(
                startEndCount, 
                contourScreen.startCalendar.getTimeInMillis(),
                contourScreen.endCalendar.getTimeInMillis(),
                timePeriodsPerfectlySynchronized);
        }
        if (vectorScreen.plotData && !vectorScreen.xGridDataSet.isClimatology) {
            timePeriodsPerfectlySynchronized = adjustStartEndCount(
                startEndCount, 
                vectorScreen.startCalendar.getTimeInMillis(),
                vectorScreen.endCalendar.getTimeInMillis(),
                timePeriodsPerfectlySynchronized);
        }
        if (pointVectorScreen.plotData) {
            timePeriodsPerfectlySynchronized = adjustStartEndCount(
                startEndCount, 
                pointVectorScreen.startCalendar.getTimeInMillis(),
                pointVectorScreen.endCalendar.getTimeInMillis(),
                timePeriodsPerfectlySynchronized);
        }
        for (int i = 0; i < nPointScreens; i++) {
            if (pointScreen(i).plotData) {
                timePeriodsPerfectlySynchronized = adjustStartEndCount(
                    startEndCount, 
                    pointScreen(i).startCalendar.getTimeInMillis(),
                    pointScreen(i).endCalendar.getTimeInMillis(),
                    timePeriodsPerfectlySynchronized);
            }
        }
        //trajectory currently not involved in this

        boolean timePeriodsDisjoint = startEndCount[2] >= 2 && //at least 2 data sets will be plotted
            startEndCount[1] < startEndCount[0]; //end time < start time    (not =, since pass data will be =
        //special case: if total time range <=59 minutes,
        //  the time periods are considered the same
        if (startEndCount[2] >= 2 && //at least 2 data sets will be plotted
            startEndCount[4] - startEndCount[3] <= 59 * Calendar2.MILLIS_PER_MINUTE) {
            timePeriodsDisjoint = false;
            timePeriodsPerfectlySynchronized = true;
            String2.log("timePeriods forced to be not 'different'");
        }
        String tWarning = null;
        if (timePeriodsDisjoint) 
            tWarning = oneOf.warningTimePeriodsDisjoint();
        else if (!timePeriodsPerfectlySynchronized)
            tWarning = oneOf.warningTimePeriodsDifferent();

        //diagnostic
        if (oneOf.verbose()) {
            StringBuilder sb = new StringBuilder("synchronizedDates=" + mapScreen.synchronizeTimesValue +
                " synchCenteredTime="  + synchCenteredTime + 
                "\n  gridTime="        + gridScreen.centeredTime.getValue(session) +
                " contourTime="        + contourScreen.centeredTime.getValue(session) +
                "\n  vectorTime="      + vectorScreen.centeredTime.getValue(session) +
                " pointVectorTime="    + pointVectorScreen.centeredTime.getValue(session));
            for (int i = 0; i < nPointScreens; i++)
                sb.append("\n  point" + i + "Date="  + pointScreen(i).centeredTime.getValue(session));
            //trajectoryScreens currently not involved
            String2.log(sb.toString());
        }

        return tWarning;
    }

    /**
     * This makes the imageFileName from the info from each screen.
     * I had trouble with long file names (>126 char). ImageMagick would just return a black .gif. E.g.
     * LATsstaS1day_20050425_x-135_X-113_y30_Y50_PRainbow_Linear_8_28_CLATsstaS1day20050425_4.0_8.0_13.0_16.0_0xFF0000_VOQNux10S1day20050415.gif
     * So I replaced the less important (and potentially long) palette info with hashcodes
     * (which return the same code for the same data -- so images can be cached).  
     * And I removed nice but not necessary underscores. Example of new file name:
     * LATsstaS1day_20050425_x-135_X-113_y30_Y50P1468940094B-1743676600COQNuxS101day20050426_-118101458VOQNux10S1day20050426.gif
     *  4/26/05 
     *
     * @return the imageFileName  doesn't include directory; does include extension
     */
    private String makeImageFileName() {
        boolean thisVerbose = false;
        //hashCodes are good because they will be same for same info
        String tImageFileName = gridScreen.startImageFileName;
        if (thisVerbose) String2.log("CWUser.makeImageFileName after grid = " + tImageFileName);
        tImageFileName += contourScreen.addToImageFileName;
        if (thisVerbose) String2.log("  after contour = " + tImageFileName);
        if (vectorScreen.plotData) {
            tImageFileName += vectorScreen.addToImageFileName;
            if (thisVerbose) String2.log("  after vector = " + tImageFileName);
        }
        if (pointVectorScreen.plotData) {
            tImageFileName += pointVectorScreen.addToImageFileName; 
            if (thisVerbose) String2.log("  after pointVector = " + tImageFileName);
        }
        for (int ps = 0; ps < nPointScreens; ps++) {
            if (pointScreen(ps).plotData) {
                tImageFileName += pointScreen(ps).addToImageFileName;  
            if (thisVerbose) String2.log("  after point " + ps + " = " + tImageFileName);
            }
        }
        for (int ts = 0; ts < nTrajectoryScreens; ts++) {
            if (trajectoryScreen(ts).plotData) {
                tImageFileName += trajectoryScreen(ts).addToImageFileName;  
            if (thisVerbose) String2.log("  after trajectory " + ts + " = " + tImageFileName);
            }
        }
        tImageFileName += OneOf.imageExtension;
        if (thisVerbose) String2.log("CWUser.makeImageFileName final = " + tImageFileName);
        return tImageFileName;
    }

    /**
     * This adjusts latestStartEarliestEndCount.
     *
     * @param latestStartEarliestEndCount the long[5] with the 
     *    0=latestStart, 1=earliestEnd, 2=count, 3=earliestStart, 4=latestEnd
     * @param tStart in millis since epoch
     * @param tEnd in millis since epoch
     * @param timePeriodsPerfectlySynchronized
     * @return false if timePeriodsPerfectlySychronized was false or tStart!=latestStart || tEnd!=earliestEnd.
     */
    private boolean adjustStartEndCount(long[] latestStartEarliestEndCount,
        long tStart, long tEnd, boolean timePeriodsPerfectlySynchronized) {
        latestStartEarliestEndCount[0] = Math.max(latestStartEarliestEndCount[0], tStart);
        latestStartEarliestEndCount[1] = Math.min(latestStartEarliestEndCount[1], tEnd);
        latestStartEarliestEndCount[2]++;
        latestStartEarliestEndCount[3] = Math.min(latestStartEarliestEndCount[3], tStart);
        latestStartEarliestEndCount[4] = Math.max(latestStartEarliestEndCount[4], tEnd);
        if (latestStartEarliestEndCount[2] > 1 && 
            (tStart != latestStartEarliestEndCount[0] || tEnd != latestStartEarliestEndCount[1]))
            timePeriodsPerfectlySynchronized = false;
        //String2.log("adjustStartEndCount tStart=" + Calendar2.epochSecondsToIsoStringTZ(tStart/1000) +
        //    " tEnd=" + Calendar2.epochSecondsToIsoStringTZ(tEnd/1000) +
        //    "\n latestStart=" + Calendar2.epochSecondsToIsoStringTZ(latestStartEarliestEndCount[0]/1000) +
        //    " earliestEnd=" + Calendar2.epochSecondsToIsoStringTZ(latestStartEarliestEndCount[1]/1000) +
        //    " perfect=" + timePeriodsPerfectlySynchronized +
        //    "\n earliestStart=" + Calendar2.epochSecondsToIsoStringTZ(latestStartEarliestEndCount[3]/1000) +
        //    " latestEnd=" + Calendar2.epochSecondsToIsoStringTZ(latestStartEarliestEndCount[4]/1000));
        return timePeriodsPerfectlySynchronized;
    }


    /**
     * This creates an animated gif.
     *
     * @param session
     * @param htmlSB
     * @return the name of the animated gif file.
     *    Name doesn't include directory; file is in oneOf.fullPublicDirectory().
     *    Name does include .gif extension (not OneOf.imageExtension).
     *    This returns null if silent failure.
     * @throws Exception if trouble
     */
    private String createAnimatedGif(HttpSession session, StringBuilder htmlSB) throws Exception {

        String2.log("createAnimatedGif");
        String errorInMethod = String2.ERROR + " in createAnimatedGif:\n";
        long time = System.currentTimeMillis();
        int animationN = String2.parseInt(mapScreen.animationNValue);
        String timePeriod = mapScreen.animationTimePeriodValue;
        double animationFPS = String2.parseDouble(mapScreen.animationFPSValue);
        int delay = Math2.roundToInt(100.0/animationFPS); //in 1/100th's second   
        int show = -1;
        if (doTally) {
            oneOf.tally().add("Animation N:", "" + animationN);
            oneOf.tally().add("Animation Time Period:", timePeriod);
        }

        //generate the animatedGifFileName
        String animatedGifFileName = "Animate" + animationN + timePeriod + animationFPS + "_" + 
             File2.getNameNoExtension(makeImageFileName()) + 
            ".gif"; //this is always .gif, not OneOf.imageExtension

        //already exists?       
        if (File2.touch(oneOf.fullPublicDirectory() + animatedGifFileName)) { 
            if (oneOf.verbose()) 
                String2.log("\nANIMATED GIF reusing: " + animatedGifFileName); 
            return animatedGifFileName; 
        } else {
            if (oneOf.verbose())
                String2.log("\nANIMATED GIF creating: " + animatedGifFileName);
        }

        //generate the magic number  so ImageMagick regex can find the files
        int randomInt = Math2.random(Integer.MAX_VALUE);

        //get the end date (the latest of dates in use e.g., ymdh not just ymd)
        String centeredTimeValue = "";
        centeredTimeValue = String2.max(centeredTimeValue, gridScreen.plotData?    gridScreen.centeredTimeValue : "");
        centeredTimeValue = String2.max(centeredTimeValue, contourScreen.plotData? contourScreen.timeValue : "");
        centeredTimeValue = String2.max(centeredTimeValue, vectorScreen.plotData?  vectorScreen.timeValue : "");
        centeredTimeValue = String2.max(centeredTimeValue, pointVectorScreen.plotData?  pointVectorScreen.centeredTimeValue : "");
        for (int i = 0; i < nPointScreens; i++)
            centeredTimeValue = String2.max(centeredTimeValue, pointScreen[i].plotData? pointScreen[i].centeredTimeValue : "");
        //trajectoryScreen currently not involved
        if (centeredTimeValue.equals(""))  //no end date  because no data
            return null; //silent failure

        //make currentCalendar
        GregorianCalendar currentCalendar = Calendar2.parseISODateTimeZulu(centeredTimeValue); //throws Exception if trouble
        String ttp = null;   //must be one of TimePeriod.OPTIONS
        int timePeriodConstant = -1;
        int timePeriodFactor = 1;
        if      (timePeriod.equals("hours"))   {ttp = "pass";    timePeriodConstant = Calendar2.HOUR_OF_DAY;}
        else if (timePeriod.equals("days"))    {ttp = "1 day";   timePeriodConstant = Calendar2.DATE;}
        else if (timePeriod.equals("weeks"))   {ttp = "8 day";   timePeriodConstant = Calendar2.DATE; timePeriodFactor = 7;}
        else if (timePeriod.equals("months"))  {ttp = "1 month"; timePeriodConstant = Calendar2.MONTH;}
        else if (timePeriod.equals("years"))   {ttp = "1 year";  timePeriodConstant = Calendar2.YEAR;} 
        else Test.error(errorInMethod + "invalid timePeriod: " + timePeriod);
        
        currentCalendar.add(timePeriodConstant, -(animationN-1) * timePeriodFactor);

        //ensure the screen time periods are <= ttp
        int ttpIndex = TimePeriods.exactTimePeriod(ttp);
        if (TimePeriods.exactTimePeriod(        gridScreen.timePeriod.getValue(session)) > ttpIndex)
               gridScreen.timePeriod.setValue(session, ttp);
        if (TimePeriods.exactTimePeriod(     contourScreen.timePeriod.getValue(session)) > ttpIndex)
            contourScreen.timePeriod.setValue(session, ttp);
        if (TimePeriods.exactTimePeriod(      vectorScreen.timePeriod.getValue(session)) > ttpIndex)
             vectorScreen.timePeriod.setValue(session, ttp);
        if (TimePeriods.exactTimePeriod( pointVectorScreen.timePeriod.getValue(session)) > ttpIndex)
             pointVectorScreen.timePeriod.setValue(session, ttp);
        for (int i = 0; i < nPointScreens; i++)
            if (TimePeriods.exactTimePeriod(pointScreen[i].timePeriod.getValue(session)) > ttpIndex)
                pointScreen[i].timePeriod.setValue(session, ttp);
        //trajectoryScreens aren't involved

        //generate special imageFileNames's for last nFrames time periods (including current)
        StringArray specialImageNames = new StringArray();
        try { //so can clean up if exception thrown
            for (int frame = 0; frame < animationN; frame++) {

                //set desired timeValue   //map and bathymetry screens unchanged
                String currentDateString = Calendar2.formatAsISODateTimeSpace(currentCalendar);
                setScreenDates(session, currentDateString);
                String2.log("createAnimatedGif frame#" + frame + " for " + currentDateString);

                //validate the screens (to get closest date, set imageFileName, etc.)
                validateScreensWithDates(session, show, htmlSB);

                //generate the frame's gif
                ArrayList imageReturn = makeImage(currentDateString);
                String imageFileName                = (String)imageReturn.get(0); 
                Table sgtMapResultsTable            = (Table)imageReturn.get(1);
                int gifWidth2                       = ((Integer)imageReturn.get(2)).intValue();
                ArrayList mapGDLs    = (ArrayList)imageReturn.get(3);
                ArrayList graphGDLs = (ArrayList)imageReturn.get(4);
                String warning                      = (String)imageReturn.get(5);

                //make a copy of the gif under a special name  (so ImageMagick regex can find them in order)
                String specialImageName = oneOf.fullPrivateDirectory() + 
                    randomInt + String2.zeroPad("" + frame, 3) + //3 zeroPadded digits so they will be sorted by frame number
                    imageFileName;
                File2.copy(
                    oneOf.fullPublicDirectory() + imageFileName,
                    specialImageName);
                specialImageNames.add(specialImageName);

                //advance currentCalendar
                currentCalendar.add(timePeriodConstant, timePeriodFactor);  //e.g., Calendar.DATE, 1
            }

            //generate the animated Gif with ImageMagick
            long imTime = System.currentTimeMillis();
            SSR.dosOrCShell("convert -delay " + delay + " " +
                "-loop 1 " + //defines how many times the animation should loop
                oneOf.fullPrivateDirectory() + randomInt + "*" + OneOf.imageExtension + " " + 
                oneOf.fullPublicDirectory() + animatedGifFileName, 120);  
            if (oneOf.verbose()) 
                String2.log("  animated gif generated in TIME=" + (System.currentTimeMillis() - imTime) + " ms");

            //delete the specially named image files
            for (int i = 0; i < specialImageNames.size(); i++)
                File2.delete(specialImageNames.get(i));

            //done
            if (oneOf.verbose()) 
                String2.log("  createAnimatedGif done. TOTAL TIME=" + (System.currentTimeMillis() - time) + " ms");
        } catch (Exception e) {
            String2.log(errorInMethod + MustBe.throwableToString(e));

            //delete the specially named image files
            for (int i = 0; i < specialImageNames.size(); i++)
                File2.delete(specialImageNames.get(i));

            //delete the animatedGif file
            File2.delete(oneOf.fullPublicDirectory() + animatedGifFileName); 

            //go back to the original values
            setScreenDates(session, centeredTimeValue);

            //validate the screens (to get closest date, set imageFileName, etc.)
            validateScreensWithDates(session, show, htmlSB);

            throw e;
        }

        return animatedGifFileName;
    }

    /**
     * This sets the date on all of the screens.
     *
     * @param session
     * @param currentIsoSpaceString
     */
    private void setScreenDates(HttpSession session, String currentIsoSpaceString) {
        //map and bathymetry screens don't have a date
        gridScreen.centeredTime.setValue(session, currentIsoSpaceString);
        contourScreen.centeredTime.setValue(session, currentIsoSpaceString);
        vectorScreen.centeredTime.setValue(session, currentIsoSpaceString);
        pointVectorScreen.centeredTime.setValue(session, currentIsoSpaceString);
        for (int i = 0; i < nPointScreens; i++)
            pointScreen[i].centeredTime.setValue(session, currentIsoSpaceString);
        //trajectoryScreens currently aren't involved
    }

    /**
     * This validates all of the screens.
     *
     * @param session
     * @param show
     * @param htmlSB
     * @param throws Exception if trouble
     */
    private void validateScreensWithDates(HttpSession session, int show, StringBuilder htmlSB) 
            throws Exception {
        IntObject step = new IntObject(1);
        IntObject rowNumber = new IntObject(0);
        double tMinX = mapScreen.minX.getDouble(session);
        double tMaxX = mapScreen.maxX.getDouble(session);
        double tMinY = mapScreen.minY.getDouble(session);
        double tMaxY = mapScreen.maxY.getDouble(session);
        String cWESNString = FileNameUtility.makeWESNString(tMinX, tMaxX, tMinY, tMaxY);  

        gridScreen.validate(       session, show, step, rowNumber, htmlSB,
            tMinX, tMaxX, tMinY, tMaxY, cWESNString, mapScreen.imageSizeIndex);
        contourScreen.validate(    session, show, step, rowNumber, htmlSB,
            tMinX, tMaxX, tMinY, tMaxY, cWESNString, mapScreen.imageSizeIndex);
        vectorScreen.validate(     session, show, step, rowNumber, htmlSB, 
            tMinX, tMaxX, tMinY, tMaxY, cWESNString, mapScreen.imageSizeIndex);
        pointVectorScreen.validate(session, show, step, rowNumber, htmlSB, 
            tMinX, tMaxX, tMinY, tMaxY, cWESNString, mapScreen.imageSizeIndex);
        for (int i = 0; i < nPointScreens; i++)
            pointScreen[i].validate(     
                session, show, step, rowNumber, htmlSB, 
                tMinX, tMaxX, tMinY, tMaxY, cWESNString);
        //trajectoryScreens currently don't have dates

    }

    /**
     * This uses SgtMap to make a map.
     *
     * @param logoFileName
     * @param g2D
     * @param ulx the upperleft x pixel for the active area
     * @param uly the upperleft y pixel for the active area
     * @param areaWidth the width of the active area (graph, labels, legend)
     * @param areaHeight the height of the active area (graph, labels, legend)
     * @param fontScale the relative font scale (1 = normal)
     * @param graphDataLayers the active graphDataLayers
     * @return ArrayList with info about where the GraphDataLayer markers were plotted 
     *   for generating the user map on the image
     *   [0]=IntArray minX, [1]=IntArray maxX, [2]=IntArray minY, [3]=IntArray maxY,
     *   [4]=IntArray rowNumber [5]=ByteArray pointLayer(0,1,2,...)
     *   This will return null if no stations plotted.
     * @throws Exception if trouble
     */
    public ArrayList makeMap(String logoFileName, Graphics2D g2D, int ulx, int uly, 
        int areaWidth, int areaHeight, double fontScale, ArrayList graphDataLayers) 
        throws Exception {

        if (oneOf.verbose()) String2.log("\n////******** CWUser.makeMap");
        long time = System.currentTimeMillis();
        double minX = mapScreen.minX.getDouble(session);
        double maxX = mapScreen.maxX.getDouble(session);
        double minY = mapScreen.minY.getDouble(session); 
        double maxY = mapScreen.maxY.getDouble(session);

        int graphArea[] = SgtMap.predictGraphSize(fontScale, areaWidth, areaHeight,
            minX, maxX, minY, maxY);
        int graphWidth = graphArea[0];
        int graphHeight = graphArea[1];

        //note that getGrid works automatically with image or pdf or ...
        //because getGrid uses areaWidth and areaHeight
        //** gather imageResFileNames before call makeMap,
        //   because a failure (e.g., no data in range) changes 
        //   (e.g., gridScreen.)plotData to false
        Grid gridGrid =       gridScreen.getGrid(minX, maxX, minY, maxY, graphWidth, graphHeight);
        Grid contourGrid = contourScreen.getGrid(minX, maxX, minY, maxY, graphWidth, graphHeight);
        //if (oneOf.verbose() && gridScreen.plotData) String2.log("gridGrid=" + gridGrid.toString(false));

        //add the vector graphDataLayer
        if (vectorScreen.plotData) {
            //Further reduce graphWidth and Height by /5 (nice round number)
            //  since vectors are always sparse.
            //This reduces nPoints significantly (1/25), 
            //but leaves flexibility of decimation process in sgtMap.makeMap(),
            //which typically wants ~1/20.
            Grid xGrid = vectorScreen.getXGrid(minX, maxX, minY, maxY, graphWidth/5, graphHeight/5);
            Grid yGrid = vectorScreen.getYGrid(minX, maxX, minY, maxY, graphWidth/5, graphHeight/5);
            if (xGrid != null && yGrid != null) {
                String tUnits = oneOf.vectorInfo()[vectorScreen.absoluteDataSetIndex][OneOf.VIUnits];
                graphDataLayers.add(new GraphDataLayer(
                    vectorScreen.editOption(), 
                    -1, -1, -1, -1, -1, GraphDataLayer.DRAW_GRID_VECTORS, false, false,
                    "", tUnits, //x,y axis title
                    oneOf.vectorInfo()[vectorScreen.absoluteDataSetIndex][OneOf.VIBoldTitle],
                    "(" + tUnits + ") " + vectorScreen.legendTime,
                    vectorScreen.xGridDataSet.courtesy.length() == 0? 
                        null : "Data courtesy of " + vectorScreen.xGridDataSet.courtesy, //Vector data
                    null, //title4
                    null, xGrid, yGrid, 
                    null, new Color(String2.parseInt("0x" + vectorScreen.colorValue)),
                    GraphDataLayer.MARKER_TYPE_NONE, 0, 
                    String2.parseDouble(oneOf.vectorInfo()[vectorScreen.absoluteDataSetIndex][OneOf.VISize]), // standardVector=e.g. 10m/s 
                    -1));
            }
        }

        //add the pointVector graphDataLayer
        if (pointVectorScreen.plotData) {
            Table averageTable = new Table();
            averageTable.readFlatNc(pointVectorScreen.fullAverageFileName, null, 1); //standardizeWhat=1
            String tUnits = oneOf.pointVectorInfo()[pointVectorScreen.absoluteDataSetIndex][OneOf.PVIUnits];
            graphDataLayers.add(new GraphDataLayer(
                pointVectorScreen.editOption(), 
                0, 1, 5, 6, -1, //x,y,u,v
                GraphDataLayer.DRAW_POINT_VECTORS, false, false,
                "", tUnits, //x,y axis title
                oneOf.pointVectorInfo()[pointVectorScreen.absoluteDataSetIndex][OneOf.PVIBoldTitle],
                "(" + tUnits + ") " + pointVectorScreen.legendTime,
                "Data courtesy of " + pointVectorScreen.legendCourtesy, //Point vector data
                null, //title4
                averageTable, null, null,
                null, new Color(String2.parseInt("0x" + pointVectorScreen.colorValue)),
                GraphDataLayer.MARKER_TYPE_NONE, 0, 
                String2.parseDouble(oneOf.pointVectorInfo()[pointVectorScreen.absoluteDataSetIndex][OneOf.PVISize]), // standardVector=e.g. 10m/s 
                -1));
        }


        ArrayList results = SgtMap.makeMap(false, 
            SgtUtil.LEGEND_BELOW,
            oneOf.legendTitle1(),
            oneOf.legendTitle2(),
            oneOf.fullContextDirectory() + "images/", 
            logoFileName,
            minX, maxX, minY, maxY,
            Browser.drawLandAsMask? "over" : "under",

            gridGrid != null, 
            gridGrid, 
            1, //scaleFactor,
            gridScreen.altScaleFactor,
            gridScreen.altOffset,
            gridScreen.fullDataSetCptName, 
            gridScreen.boldTitle,
            SgtUtil.getNewTitle2(
                gridScreen.unitsValue,
                gridScreen.legendTime, 
                ""),  
            gridScreen.courtesy == null || gridScreen.courtesy.length() == 0?
                null : "Data courtesy of " + gridScreen.courtesy,
            "",

            SgtMap.NO_LAKES_AND_RIVERS,

            contourGrid != null,
            contourGrid,
            1, //scaleFactor,
            contourScreen.altScaleFactor,
            contourScreen.altOffset,
            contourScreen.drawLinesAtValue, 
            new Color(String2.parseInt("0x" + contourScreen.colorValue)),
            contourScreen.boldTitle,
            contourScreen.unitsValue,
            "",  
            contourScreen.legendTime, 
            contourScreen.courtesy == null || contourScreen.courtesy.length() == 0? 
                null : "Data courtesy of " + contourScreen.courtesy, //Contour data

            graphDataLayers,
            g2D,
            ulx, uly, areaWidth, areaHeight,
            0,   //no coastline adjustment
            fontScale); 

        if (oneOf.verbose()) String2.log("\\\\\\\\******** CWUser.makeMap done. TOTAL TIME=" + 
            (System.currentTimeMillis() - time) + "\n");
        return results;
    }

    /**
     * This uses SgtGraph to make time series graphs.
     *
     * @param logoFileName
     * @param g2D
     * @param ulx the upperleft x pixel for the active area
     * @param uly the upperleft y pixel for the active area
     * @param areaWidth the width of the active area (graph, labels, legend)
     * @param areaHeight the height of the active area (graph, labels, legend)
     * @param fontScale the relative font scale (1 = normal)
     * @param graphDataLayers the active graphDataLayers
     * @throws Exception if trouble
     */
    public void makeGraphs(
        String logoFileName, Graphics2D g2D, 
        int ulx, int uly, int areaWidth, int areaHeight, double fontScale,
        ArrayList graphGDLs) throws Exception {

        if (oneOf.verbose()) String2.log("\n////******** CWUser.makeGraphs");
        long time = System.currentTimeMillis();
        int n = graphGDLs.size();
        if (n == 0) return;

        //if all the x and y units the same, plot on one graph
        GraphDataLayer gpl0 = (GraphDataLayer)graphGDLs.get(0);
        boolean allSame = gpl0.draw != GraphDataLayer.DRAW_STICKS; //if draw_sticks, allSame is false
        String xUnits = gpl0.table.columnAttributes(gpl0.v1).getString("units");
        String yUnits = gpl0.table.columnAttributes(gpl0.v2).getString("units");
        if (xUnits == null || yUnits == null)
            allSame = false;
        if (allSame) {
            for (int i = 1; i < n; i++) { //1 since comparing to 0
                GraphDataLayer gdl = (GraphDataLayer)graphGDLs.get(i);
                if (gdl.draw == GraphDataLayer.DRAW_STICKS ||
                    !xUnits.equals(gdl.table.columnAttributes(gdl.v1).getString("units")) ||
                    !yUnits.equals(gdl.table.columnAttributes(gdl.v2).getString("units"))) {
                    allSame = false;
                    break;
                }
            }
        }
        if (allSame) {
            oneOf.sgtGraph().makeGraph(false,
                gpl0.xAxisTitle, gpl0.yAxisTitle,
                SgtUtil.LEGEND_BELOW,
                oneOf.legendTitle1(),
                oneOf.legendTitle2(),
                oneOf.fullContextDirectory() + "images/", 
                logoFileName,
                Double.NaN, Double.NaN, true, gpl0.xIsTimeAxis, false, //isAscending, isTimeAxis, isLog
                Double.NaN, Double.NaN, true, gpl0.yIsTimeAxis, false, 
                graphGDLs,
                g2D,
                ulx, uly, areaWidth, areaHeight, 2, //graph width/height
                SgtGraph.DefaultBackgroundColor, fontScale); 
            return;
        }

        //for xAxis=time graphs, force all to have same x axis range
        double minTime = Double.MAX_VALUE;
        double maxTime = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            GraphDataLayer gdl = (GraphDataLayer)graphGDLs.get(i);
            if (gdl.xIsTimeAxis) {
                PrimitiveArray timeCol = gdl.table.getColumn(gdl.v1);
                double tStats[] = timeCol.calculateStats(); 
                if (tStats[PrimitiveArray.STATS_N] > 0) {
                    minTime = Math.min(minTime, tStats[PrimitiveArray.STATS_MIN]);
                    maxTime = Math.max(maxTime, tStats[PrimitiveArray.STATS_MAX]);
                }
            }
        }
        if (minTime == Double.MAX_VALUE) minTime = Double.NaN; 
        if (maxTime == -Double.MAX_VALUE) maxTime = Double.NaN;
        if (minTime == maxTime) {
            double suggest[] = Math2.suggestLowHigh(minTime, maxTime);
            minTime = suggest[0];
            maxTime = suggest[1];
        }

        //plot separate graphs
        //divide the vertical area into n sections, one for each GraphDataLayer
        int tUly = uly;
        int tAreaHeight = areaHeight / n;
        for (int i = 0; i < n; i++) {
            GraphDataLayer gdl = (GraphDataLayer)graphGDLs.get(i);
            ArrayList tArrayList = new ArrayList();
            tArrayList.add(gdl);
            oneOf.sgtGraph().makeGraph(false,
                gdl.xAxisTitle, gdl.yAxisTitle,
                SgtUtil.LEGEND_BELOW,
                oneOf.legendTitle1(),
                oneOf.legendTitle2(),
                oneOf.fullContextDirectory() + "images/", 
                logoFileName,
                gdl.xIsTimeAxis? minTime : Double.NaN, 
                gdl.xIsTimeAxis? maxTime : Double.NaN, true, gdl.xIsTimeAxis, false, //isAscending, isTime, isLog
                Double.NaN, Double.NaN,                true, gdl.yIsTimeAxis, false,
                tArrayList,
                g2D,
                ulx, tUly, areaWidth, tAreaHeight, 2, //graph width/height
                SgtGraph.DefaultBackgroundColor, fontScale); 
            tUly += tAreaHeight;
        }

        if (oneOf.verbose()) String2.log("\\\\\\\\******** CWUser.makeGraphs done. TOTAL TIME=" + 
            (System.currentTimeMillis() - time) + "\n");
    }

    /**
     * This generates the regionsImageUrl image which is 
     * a copy of regionsImage with a rectangle drawn
     * on it (based on the currend x/y/Min/Max values).
     * This also does some checking/validating of x/y/Min/Max.
     *
     * <p>This won't throw exception.
     * If trouble, it returns the unhighlighted regions image.
     *
     * @param session is the session associated with a user
     * @return the url of the resulting image (regionsImageUrl if successful
     *    or regionsImageDefaultUrl if not)
     */
    public String createCustomRegionsImage(HttpSession session) {
        try {
            //the already validated x/y/Min/Max values
            double minXValue = mapScreen.minX.getDouble(session);
            double maxXValue = mapScreen.maxX.getDouble(session);
            double minYValue = mapScreen.minY.getDouble(session);
            double maxYValue = mapScreen.maxY.getDouble(session);
            if (oneOf.verbose()) String2.log("\ncreateCustomRegionsImage " + 
                minXValue + "," + maxXValue + "," + minYValue + "," + maxYValue);  
            long tTime = System.currentTimeMillis();
            double xRange2 = (maxXValue - minXValue) / 2;
            double yRange2 = (maxYValue - minYValue) / 2;

            //calculate x/y/width/height
            //currently, x calculations seem incorrect but come out perfect (except for full area); 
            //  y calculations seem correct but are sometimes off by one (hard to get perfect)
            int x = Math2.truncToInt(oneOf.regionsImageMinXPixel() +
                ((minXValue - oneOf.regionsImageMinXDegrees()) / 
                 (oneOf.regionsImageMaxXDegrees() - oneOf.regionsImageMinXDegrees())) *
                (oneOf.regionsImageMaxXPixel() - oneOf.regionsImageMinXPixel()));
            int x2 = Math2.truncToInt(oneOf.regionsImageMinXPixel() +
                ((maxXValue - oneOf.regionsImageMinXDegrees()) / 
                 (oneOf.regionsImageMaxXDegrees() - oneOf.regionsImageMinXDegrees())) *
                (oneOf.regionsImageMaxXPixel() - oneOf.regionsImageMinXPixel()));
            if (minXValue == oneOf.regionsImageMinXDegrees()) {x--;  /*String2.log("Region x--"); */} //fudge
            if (maxXValue == oneOf.regionsImageMaxXDegrees()) {x2--; /*String2.log("Region x2--");*/} //fudge 
            int y = Math2.roundToInt(oneOf.regionsImageMinYPixel() + 
                ((minYValue - oneOf.regionsImageMinYDegrees()) / 
                 (oneOf.regionsImageMaxYDegrees() - oneOf.regionsImageMinYDegrees())) *
                (oneOf.regionsImageMaxYPixel() - oneOf.regionsImageMinYPixel() + 1));
            int y2 = Math2.roundToInt(oneOf.regionsImageMinYPixel() + 
                ((maxYValue - oneOf.regionsImageMinYDegrees()) / 
                 (oneOf.regionsImageMaxYDegrees() - oneOf.regionsImageMinYDegrees())) *
                (oneOf.regionsImageMaxYPixel() - oneOf.regionsImageMinYPixel() + 1));
            int width = x2 - x;
            int height = y2 - y; 
            if (width < 0) { //drawRect doesn't like negative width or height
                x += width;
                width = -width;
            }
            if (height < 0) {
                y += height;
                height = -height;
            }

            //does the file already exist?
            String newName  = File2.getNameNoExtension(oneOf.regionsImageFileName()) + 
                "x" + x + "y" + y + "w" + width + "h" + height + OneOf.imageExtension;
            String newFullDirName = oneOf.fullPublicDirectory() + newName;
            if (File2.touch(newFullDirName)) {
                if (oneOf.verbose()) String2.log("  createCustomRegionsImage done. " + 
                    "reuse: " + newName);
                return OneOf.PUBLIC_DIRECTORY + newName;
            }
            if (oneOf.verbose()) 
                String2.log("  new region image: " + newName);

            //POLICY: because this procedure may be used in more than one thread,
            //do work on unique temp files names using randomInt, then rename to proper file name.
            //If procedure fails half way through, there won't be a half-finished file.
            int randomInt = Math2.random(Integer.MAX_VALUE);

            //generate image file with selected region highlighted 
            //do via Java
            //read the file
            BufferedImage bi = SgtUtil.readImage(oneOf.regionsImageFullFileName());
            Graphics g = bi.getGraphics();
            g.setColor(Color.RED);
            g.drawRect(x, y, width, height);
            SgtUtil.saveImage(bi, newFullDirName);

            /*old approach -- do via imageMagick
            //generate to a tempName then rename (quickly) in case process is interrupted
            String tempName = randomInt + OneOf.imageExtension;
            String cmd = "convert " + //+antialias " + //+antialias turns off (!) antialiasing
                "-fill transparent " +
                "-stroke red " +
                "-draw 'rectangle " + x + "," + y + " " + (x + width) + "," + (y + height) + "' " +
                " " + oneOf.regionsImageFullFileName() + 
                " " + oneOf.fullPublicDirectory() + tempName;
            SSR.dosOrCShell(cmd, 10);

            //last step: rename it to desired name
            File2.rename(oneOf.fullPublicDirectory(), tempName, newName); 
            */

            if (oneOf.verbose())
                if (oneOf.verbose()) String2.log("  createCustomRegionsImage done. TIME=" + 
                    (System.currentTimeMillis() - tTime) + "\n"); 
            return OneOf.PUBLIC_DIRECTORY + newName;

        } catch (Exception e) {
            String2.log(MustBe.throwable("BrowserCW.createCustomRegionsImage", e));
            return oneOf.regionsImageDefaultUrl();
        }
    }


}
