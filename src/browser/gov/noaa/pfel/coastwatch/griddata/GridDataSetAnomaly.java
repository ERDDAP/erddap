/* 
 * GridDataSetAnomaly Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;


import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.StringArray;

import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.TimePeriods;
import gov.noaa.pfel.coastwatch.util.StringObject;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.util.Vector;


/** 
 * This class combines a regular GridDataSet and a climatology GridDataSet to 
 * create a GridDataSetAnomaly.
 * 
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-12-18
 */
public class GridDataSetAnomaly extends GridDataSet { 

    GridDataSet gridDataSet;
    GridDataSet climatology;

    /**
     * This class combines a regular GridDataSet and a climatology GridDataSet to 
     * create a GridDataSetAnomaly.
     *
     * @param internalName
     * @param fileNameUtility is used if gridDataSet is a GridDataSetCWLocal to setAttributes, 
     *    and for dataSetRB2.
     * @param boldTitle 
     * @param optionAdd a String (e.g., "*") to be added to boldTitle to make 'option'
     * @param gridDataSet  the present day gridDataSet
     * @param climatology  the climatology which is related to the gridDataSet
     * @param paletteMax the max range for the colorbar.
     *     paletteMin will be -paletteMax. paletteScale will be "linear".
     *     palette will be "BlueWhiteRed".
     * @throws Exception if trouble
     */
    public GridDataSetAnomaly(String internalName, FileNameUtility fileNameUtility,
        GridDataSet gridDataSet, GridDataSet climatology, 
        String boldTitle, String optionAdd,
        String paletteMax) throws Exception {

        if (verbose)
            String2.log("\n* GridDataSetAnomaly constructor (" + internalName + ")");

        this.gridDataSet = gridDataSet;
        this.climatology = climatology;
        String sixName = internalName.substring(1);
        long time = System.currentTimeMillis();

//ensure grids are same resolution?

        gridDataSet.globalAttributes.copyTo( globalAttributes);
        gridDataSet.lonAttributes.copyTo(    lonAttributes);
        gridDataSet.latAttributes.copyTo(    latAttributes);
        gridDataSet.depthAttributes.copyTo(  depthAttributes);
        gridDataSet.timeAttributes.copyTo(   timeAttributes);
        gridDataSet.dataAttributes.copyTo(   dataAttributes);

        //make temp data structures to be filled        
        StringArray tActiveTimePeriodOptions = new StringArray();
        StringArray tActiveTimePeriodTitles = new StringArray();
        IntArray tActiveTimePeriodNHours = new IntArray();
        StringArray tActiveTimePeriodOpendapUrls = new StringArray();
        activeTimePeriodTimes = new Vector();
        tActiveTimePeriodTitles.add(""); //always add a blank main title

        //look for common time periods
        IntArray gdsTpNHours = new IntArray(gridDataSet.activeTimePeriodNHours);
        IntArray cTpNHours = new IntArray(climatology.activeTimePeriodNHours);
        for (int tp = 0; tp < gdsTpNHours.size(); tp++) {
            int whichClimatologyTimePeriod = cTpNHours.indexOf("" + gdsTpNHours.get(tp));
            if (whichClimatologyTimePeriod < 0) continue;

            //accumulate active time period dates for this activeTimePeriodOption
            //(ensure a gdsDate has an appropriate Climatology time)
            String timePeriodString = gridDataSet.activeTimePeriodOptions[tp];
            StringArray tDates = new StringArray();
            String cDates[] = (String[])climatology.activeTimePeriodTimes.get(whichClimatologyTimePeriod);
            String gdsDates[] = (String[])gridDataSet.activeTimePeriodTimes.get(tp);
            for (int td = 0; td < gdsDates.length; td++) {
                //make sure the climatology has an appropriate time
                int tWhich = findAppropriateClimatologyTime(timePeriodString, cDates, gdsDates[td]);
                if (tWhich >= 0)
                    tDates.add(gdsDates[td]);
            }

            //store the info for this time period 
            if (tDates.size() > 0) {
                tActiveTimePeriodOptions.add(timePeriodString);
                tActiveTimePeriodTitles.add(gridDataSet.activeTimePeriodTitles[tp + 1]); //+1 since titles are offset by 1 because 0 is generic
                tActiveTimePeriodNHours.add(gridDataSet.activeTimePeriodNHours[tp]);
                tActiveTimePeriodOpendapUrls.add(gridDataSet.activeTimePeriodOpendapUrls[tp]);
                activeTimePeriodTimes.add(tDates.toArray());
            }
        }

        //convert data structures to final form
        activeTimePeriodOptions = tActiveTimePeriodOptions.toArray();
        activeTimePeriodTitles = tActiveTimePeriodTitles.toArray();
        activeTimePeriodNHours = tActiveTimePeriodNHours.toArray();
        activeTimePeriodOpendapUrls = tActiveTimePeriodOpendapUrls.toArray();

        //set the attributes (of the superclass GridDataSet)
        altUdUnits = gridDataSet.altUdUnits;
        if (altUdUnits.length() > 0) {
            //!!!interesting: anomaly always centered at 0, so altOffset always 0.
            //This affects degree_C -> degree_F datasets.
            altOffset = 0; 
            altScaleFactor = gridDataSet.altScaleFactor; 
            double tpm = String2.parseDouble(paletteMax);
            //0.94 solves problem: suggestLowHigh doesn't like tpm exactly at high end (it likes buffer space)
            tpm = 0.94 * tpm * altScaleFactor; 
            double[] lowHigh = Math2.suggestLowHigh(-tpm, tpm);
            //String2.log("  alt tpm=" + tpm + " low=" + lowHigh[0] + " high=" + lowHigh[1]);
            tpm = Math.max(Math.abs(lowHigh[0]), Math.abs(lowHigh[1])); //make symmetrical
            altPaletteMax = "" + tpm; 
            altPaletteMin = "-" + altPaletteMax; 
            altContourLinesAt = "" + (tpm / 2);
        }      

        this.boldTitle = boldTitle; 
        //gridDataSet almost certainly has anomalyDataSet; "" it here since anomaly datasets don't have anomalys
        anomalyDataSet = "";  
        contourLinesAt = "" + (String2.parseDouble(paletteMax) / 2);
        courtesy = gridDataSet.courtesy; 
        defaultUnits = gridDataSet.defaultUnits;

        //create the default summary (may be replaced below)
        summary = "This is an anomaly data set, generated by subtracting \"" + 
            climatology.boldTitle + 
            "\" data from \"" + gridDataSet.boldTitle + "\" data.";

        //get FGDC made for this anomaly dataset
        String fgdcAbstract = "<metadata><idinfo><descript><abstract>";
        String fgdc = fileNameUtility.dataSetRB2().getString(sixName + "FGDC", null);
        if (fgdc != null) {
            fgdcSubstitutions = String2.splitNoTrim(fgdc, '\n');
            for (int i = 0; i < fgdcSubstitutions.length; i++) {
                if (fgdcSubstitutions[i].startsWith(fgdcAbstract)) {
                    summary = fgdcSubstitutions[i].substring(fgdcAbstract.length());
                    break;
                }
            }
        }

        //or make FGDC from gridDataSet's FGDC info
        if (fgdcSubstitutions == null && 
            gridDataSet.fgdcSubstitutions != null) { //source of info
            fgdcSubstitutions = new String[gridDataSet.fgdcSubstitutions.length];
            String fgdcTitles[] = {
                "<metadata><idinfo><citation><citeinfo><title>" ,
                "<metadata><dataqual><lineage><srcinfo><srccite><citeinfo><title>",
                "<metadata><distinfo><resdesc>"};
            for (int i = 0; i < fgdcSubstitutions.length; i++) {
                fgdcSubstitutions[i] = gridDataSet.fgdcSubstitutions[i];
                for (int ft = 0; ft < fgdcTitles.length; ft++) {
                    if (fgdcSubstitutions[i].startsWith(fgdcTitles[ft])) {
                        fgdcSubstitutions[i] = fgdcTitles[ft] + boldTitle;
                    }
                }
                if (fgdcSubstitutions[i].startsWith(fgdcAbstract)) {
                    fgdcSubstitutions[i] = fgdcAbstract + summary + 
                        " " + fgdcSubstitutions[i].substring(fgdcAbstract.length());
                }
            }
        }

        this.fileNameUtility = fileNameUtility;
        this.internalName = internalName; 
        isClimatology = false;
        keywords = gridDataSet.keywords;
        keywordsVocabulary = gridDataSet.keywordsVocabulary;
        daysTillDataAccessAllowed = gridDataSet.daysTillDataAccessAllowed;
        option = boldTitle + optionAdd;
        palette = "BlueWhiteRed"; 
        this.paletteMax = paletteMax; 
        paletteMin = "-" + paletteMax; 
        paletteScale = "Linear"; 
        references = gridDataSet.references;
        udUnits = gridDataSet.udUnits; 
        unitsOptions = gridDataSet.unitsOptions;

        //checkValidity
        checkValidity();
    
        if (verbose) String2.log(
            "  Options: "     + String2.toCSSVString(activeTimePeriodOptions) + "\n" +
            "  Titles: "      + String2.toCSSVString(activeTimePeriodTitles) + "\n" +
            "  NHours: "      + String2.toCSSVString(activeTimePeriodNHours) + "\n" +
            "  GridDataSetAnomaly constructor " + internalName + " done. TIME=" + 
                (System.currentTimeMillis() - time));
    }

    /**
     * This suggests a boldTitle (or option) based on the gridDataSet's option.
     * This works for my names but may not work for others.
     *
     * @param boldTitle the boldTitle (or option) from the gridDataSet.
     * @return the suggested boldTitle (or option)
     */
    public static String suggestBoldTitle(String boldTitle) {
        int po = boldTitle.indexOf(',');
        if (po < 0) return boldTitle + " Anomaly";
        return boldTitle.substring(0, po) + " Anomaly" + boldTitle.substring(po);
    }


    //This uses the superclasses setAttributes.

    /**
     * This makes the specified grid as best it can.
     * See the superclass' documentation.
     */
    public Grid makeGrid(String timePeriodValue, String timeValue,          
        double minX, double maxX, double minY, double maxY,
        int desiredNWide, int desiredNHigh) throws Exception {

        long time = System.currentTimeMillis();
        String msg = "//** GridDataSetAnomaly.makeGrid(" + internalName + " timePeriod=" + timePeriodValue +
               " date=" + timeValue +
            "\n  minX=" + minX +
               " maxX=" + maxX +
               " minY=" + minY +
               " maxY=" + maxY +
               " nWide=" + desiredNWide +
               " nHigh=" + desiredNHigh + ")";
        if (verbose) String2.log(msg); 
        String errorInMethod = String2.ERROR + " in " + msg + ":\n";

        //get desired gridDataSet grid
        String gdsTimePeriod = gridDataSet.activeTimePeriodOptions[
            TimePeriods.closestTimePeriod(timePeriodValue, gridDataSet.activeTimePeriodOptions)];
        Test.ensureEqual(
            TimePeriods.getNHours(timePeriodValue),
            TimePeriods.getNHours(gdsTimePeriod),
            errorInMethod + "No matching timePeriod for " + timePeriodValue + " in gridDataSet:\n" +
                String2.toCSSVString(gridDataSet.activeTimePeriodOptions));
        //timeValue should already be appropriate
        Grid gdsGrid = gridDataSet.makeGrid(gdsTimePeriod, 
            timeValue, minX, maxX, minY, maxY, desiredNWide, desiredNHigh);

        //get desired climatology grid
        int climatologyTimePeriodIndex = 
            TimePeriods.closestTimePeriod(timePeriodValue, climatology.activeTimePeriodOptions);
        String climatologyTimePeriod = climatology.activeTimePeriodOptions[climatologyTimePeriodIndex];
        Test.ensureEqual(
            TimePeriods.getNHours(timePeriodValue),
            TimePeriods.getNHours(climatologyTimePeriod),
            errorInMethod + "No matching timePeriod for " + timePeriodValue + " in climatologyDataSet:\n" +
                String2.toCSSVString(climatology.activeTimePeriodOptions));
        String[] climatologyTimes = 
            (String[])climatology.activeTimePeriodTimes.get(climatologyTimePeriodIndex);
        int timeIndex = GridDataSet.findAppropriateClimatologyTime(
            timePeriodValue, climatologyTimes, timeValue);
        Test.ensureTrue(timeIndex >= 0,
            errorInMethod + "No appropriate time for " + timeValue + " in climatologyDataSet:\n" +
                String2.toCSSVString(climatologyTimes));
        String climatologyTimeValue = climatologyTimes[timeIndex];
        //try hard to exactly match the results from gdsGrid 
        //(as opposed to the original desired values)
        Grid cGrid = climatology.makeGrid(climatologyTimePeriod, 
            climatologyTimeValue, 
            gdsGrid.lon[0], gdsGrid.lon[gdsGrid.lon.length - 1], 
            gdsGrid.lat[0], gdsGrid.lat[gdsGrid.lat.length - 1], 
            gdsGrid.lon.length, gdsGrid.lat.length);

        //deal with grid mostly same, but slightly different (e.g., extra row in one compared to other)
        double fudge = 0.001;
        double minLon = Math.max(gdsGrid.lon[0], cGrid.lon[0]) - fudge;
        double maxLon = Math.min(gdsGrid.lon[gdsGrid.lon.length - 1], cGrid.lon[cGrid.lon.length - 1]) + fudge;
        double minLat = Math.max(gdsGrid.lat[0], cGrid.lat[0]) - fudge;
        double maxLat = Math.min(gdsGrid.lat[gdsGrid.lat.length - 1], cGrid.lat[cGrid.lat.length - 1]) + fudge;
        Test.ensureTrue(gdsGrid.subset(minLon, maxLon, minLat, maxLat, 
            Integer.MAX_VALUE, Integer.MAX_VALUE), 
            errorInMethod + "No grid data in range.");
        Test.ensureTrue(cGrid.subset(minLon, maxLon, minLat, maxLat, 
            Integer.MAX_VALUE, Integer.MAX_VALUE), 
            errorInMethod + "No climatology data in range.");

        //ensure lat's and lon's are identical
        int gdsN = gdsGrid.lat.length;
        int cN = cGrid.lat.length;
        Test.ensureTrue(gdsN >= 1, errorInMethod + "The gdsGrid.lat.length (" + gdsN + ") is < 1.");
        Test.ensureTrue(cN >= 1,   errorInMethod + "The cGrid.lat.length ("   + cN + ") is < 1.");
        Test.ensureEqual(gdsGrid.lat, cGrid.lat, 
            errorInMethod + "The lat arrays are different." +
              "\n  (gds0=" + gdsGrid.lat[0] + 
              " c0="       + cGrid.lat[0] + 
              " gdsLast="  + gdsGrid.lat[gdsN - 1] + 
              " cLast="    + cGrid.lat[cN - 1] + 
              "\n  gdsN="  + gdsN + 
              " cN="       + cN +
              " gdsRes="   + gdsGrid.latSpacing+ 
              " cRes="     + cGrid.latSpacing + ")");
        gdsN = gdsGrid.lon.length;
        cN = cGrid.lon.length;
        Test.ensureTrue(gdsN >= 1, errorInMethod + "The gdsGrid.lon.length (" + gdsN + ") is < 1.");
        Test.ensureTrue(cN >= 1,   errorInMethod + "The cGrid.lon.length ("   + cN + ") is < 1.");
        Test.ensureEqual(gdsGrid.lon, cGrid.lon, 
            errorInMethod + "The lon arrays are different." +
              "\n  (gds0=" + gdsGrid.lon[0] + 
              " c0="       + cGrid.lon[0] + 
              " gdsLast="  + gdsGrid.lon[gdsN - 1] + 
              " cLast="    + cGrid.lon[cN - 1] + 
              "\n  gdsN="  + gdsN + 
              " cN="       + cN +
              " gdsRes="   + gdsGrid.lonSpacing+ 
              " cRes="     + cGrid.lonSpacing + ")");

        //subtract
        int n = gdsGrid.lat.length * gdsGrid.lon.length;
        double gdsGridData[] = gdsGrid.data;
        double cGridData[] = cGrid.data;
        for (int i = 0; i < n; i++) 
            gdsGridData[i] -= cGridData[i];

        String2.log("\\\\** GridDataSetAnomaly.makeGrid done. TOTAL TIME=" + 
            (System.currentTimeMillis() - time) + "\n");
        return gdsGrid;

    }
}
