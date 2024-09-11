/* 
 * GridDataSet Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.OneOf;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.TimePeriods;
import gov.noaa.pfel.coastwatch.util.StringObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

/** 
 * This class represents one grid dataset for CWBrowser.
 * It determine the dates for which data is available and
 * it can generate a grd file with the data for a specific date.
 * 
 * The constructor searches for available data.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-05-01
 */
public abstract class GridDataSet { 

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 
    public static boolean reallyVerbose = false; 

    /** This hold attributes from external sources (e.g., THREDDS)
     * or are unused (empty). */
    public Attributes globalAttributes = new Attributes();
    public Attributes lonAttributes = new Attributes();
    public Attributes latAttributes = new Attributes();
    public Attributes depthAttributes = new Attributes();
    public Attributes timeAttributes = new Attributes();
    public Attributes dataAttributes = new Attributes();

//THIS SECTION HAS ATTRIBUTES (which are needed for the gui part of CWBrowser) 
//IN ALPHABETIC ORDER
//All are initially set either default values or impossible values, 
//so checkValidity can check if the constructor set them.


    /** The active time period options.  */
    public String[] activeTimePeriodOptions; //set by the constructor

    /** The active time period titles. */
    public String[] activeTimePeriodTitles; //set by the constructor

    /** The number of days in each time period (30*24 = 1 month). */
    public int[]    activeTimePeriodNHours;

    /** The active time period opendapUrl (or array of nulls if none available).
     * E.g., https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MO/k490/hday
     * which just needs .html suffix to make link for user to click on.
     */
    public String[] activeTimePeriodOpendapUrls; //set by the constructor

    /** The active time period dates (String[]) related to an activeTimePeriodOption. 
     * The dates must be only valid dates; invalid/unparsable dates are thrown out.
     */
    public Vector activeTimePeriodTimes = new Vector(); //set by the constructor   


    /** 
     * The default for alternate drawContourLinesAt, e.g., 5. 
     * Set by the constructor (to "" if altUdUnits is "").
     */
    public String altContourLinesAt = "";


    //altLatLonDigits was removed  it doesn't change with alt things


    /** 
     * The default max of the alternate range for the palette scale, e.g., 28. 
     * Set by the constructor (to "" if altUdUnits is "").
     */
    public String altPaletteMax = ""; 

    /** 
     * The default min of the alternate range for the palette scale, e.g., 8. 
     * Set by the constructor (to "" if altUdUnits is "").
     */
    public String altPaletteMin = ""; 

    /**
     * altOffset is the offset to be added scale factor to convert values in standard units 
     * to altUnits: altUnits = standardUnits * altScale + altOffset.
     * Set by the constructor (to Double.NaN if altUdUnits is "").
     */
    public double altOffset = Double.NaN; 

    /**
     * altScaleFactor is the scale factor to convert values in standard units 
     * to altUnits: altUnits = standardUnits * altScale + altOffset.
     * Set by the constructor (to Double.naN if altUdUnits is "").
     */
    public double altScaleFactor = Double.NaN; 

    /** 
     * altUdUnits is the alternate units for this data 
     * set (e.g., degree_F). 
     * Set by the constructor.
     * Set this to "" if not used.
     */
    public String altUdUnits = null;
    
    /**
     * The corresponding anomaly dataset (e.g., APHtanm, or "" if none).
     */
    public String anomalyDataSet = "";

    /** 
     * The default bold title for the legend, usually less than 60 characters, 
     * e.g., SST, NOAA POES AVHRR, Day and Night, 1.25 km, Local Area Coverage. 
     * Set by the constructor.
     */
    public String boldTitle = null; 

    /** 
     * The default for drawContourLinesAt, e.g., 5. 
     * Set by the constructor.
     */
    public String contourLinesAt = null;

    /** 
     * The default courtesy line for the legend, e.g., NOAA NESDIS OSDPD. 
     * Set by the constructor ("" if not in use).
     */
    public String courtesy = null; 

    /**
     * default units determines whether the standard units or the 
     * alternate units are the default units.
     * The value is either 'S' (for the standard SI units)
     * or 'A' (for the alternate units).
     */
    public char defaultUnits = 'S';

    /** 
     * The xxxxFGDC substitution info for this dataset (null if not used). 
     * Set by the constructor.
     */
    public String fgdcSubstitutions[] = null; 

    /**
     * A fileNameUtility object which may or may not have info relevant to this dataSet.
     * Set by the constructor.   May be null.
     */
    public FileNameUtility fileNameUtility = null;

    /** The 7 character internal name for the dataset, e.g., LATssta. 
     * The first letter must be L (Local), O (OPeNDAP), or ....
     * Set by the constructor. 
     */ 
    public String internalName = null; 

    /**
     * Indicates if this gridDataSet is a climatology.
     */
    public boolean isClimatology = false;

    /** 
     * keywords e.g., EARTH SCIENCE > Oceans > Ocean Circulation > Ocean Currents. 
     * null if not available. 
     */
    public String keywords = null;

    /** 
     * keywordsVocabulary 
     * null if not available. 
     */
    public String keywordsVocabulary = null;


    /** 
     * The value of daysTillDataAccessAllowed, e.g., -1 (for margin of safety) or 
     *  14 (from end of composite) for seawifs SH-only.
     * Set by the constructor.
     */
    public int daysTillDataAccessAllowed = Integer.MAX_VALUE;

    /** 
     * The name that will appear on the drop down list on the screen.
     * It is usually the bold title (and if opendap or thredds, with a '*' appended), e.g., 
     * "SST, NOAA POES AVHRR, Day and Night, 1.25 km, Local Area Coverage". 
     * Set by the constructor.
     */ 
    public String option = null; 

    /** 
     * The name of the default palette, e.g., Rainbow. 
     * Set by the constructor.
     */
    public String palette = null; 

    /** 
     * The default max of the range for the palette scale, e.g., 28. 
     * Set by the constructor.
     */
    public String paletteMax = null; 

    /** 
     * The default min of the range for the palette scale, e.g., 8. 
     * Set by the constructor.
     */
    public String paletteMin = null; 

    /** 
     * The name of the default palette scale, e.g., Linear. 
     * Set by the constructor.
     */
    public String paletteScale = null; 

    /**
     * References information.
     * null if not used.
     */
    public String references;

    /** A long text summary of the dataset.  null if not available. */
    public String summary = null;

    /** 
     * The udUnits for the legend,
     * e.g., degree_C. 
     * Set by the constructor.
     */
    public String udUnits = null; 

    /**
     * The units options: {units} or {units, altUnits}.
     * Set by the constructor.
     */
    public String unitsOptions[] = null;



    /** 
     * This is used by the constructor to ensure the fields have valid values.
     *
     * @throws Exception if not valid
     */
    public void checkValidity() {

        String errorInMethod = String2.ERROR + " in GridDataSet.checkValidity for " + 
            internalName + ":\n ";

        Test.ensureNotNull(activeTimePeriodOptions,                            errorInMethod + "'activeTimePeriodOptions' wasn't set.");
        Test.ensureNotEqual(activeTimePeriodOptions.length, 0,                 errorInMethod + "No valid time periods!");
        Test.ensureNotNull(activeTimePeriodTitles,                             errorInMethod + "'activeTimePeriodTitles' wasn't set.");
        Test.ensureNotNull(activeTimePeriodNHours,                             errorInMethod + "'activeTimePeriodNHours' wasn't set.");
        Test.ensureNotNull(activeTimePeriodOpendapUrls,                        errorInMethod + "'activeTimePeriodOpendapUrls' wasn't set.");
//            //don't show: lots of activeTimePeriodTimes stored in a Vector()
        Test.ensureNotNull(altUdUnits,                                         errorInMethod + "'altUdUnits' wasn't set.");
        if (defaultUnits == 'A')
            Test.ensureNotEqual(altUdUnits.length(), 0,                        errorInMethod + "'altUdUnits' is \"\" even though it is the default.");
        if (altUdUnits.length() > 0) {
            Test.ensureNotEqual(String2.parseDouble(altContourLinesAt), Double.NaN, errorInMethod + "'altContourLinesAt' wasn't set.");
            Test.ensureNotEqual(String2.parseDouble(altPaletteMax), Double.NaN,errorInMethod + "'altPaletteMax' wasn't set.");
            Test.ensureNotEqual(String2.parseDouble(altPaletteMin), Double.NaN,errorInMethod + "'altPaletteMin' wasn't set.");
            Test.ensureNotEqual(altOffset, Double.NaN,                         errorInMethod + "'altOffset' wasn't set.");
            Test.ensureNotEqual(altScaleFactor, Double.NaN,                    errorInMethod + "'altScaleFactor' wasn't set.");
        }
        Test.ensureNotNull(anomalyDataSet,                                     errorInMethod + "'anomalyDataSet' is null.");
        Test.ensureNotNull(boldTitle,                                          errorInMethod + "'boldTitle' wasn't set.");
        Test.ensureNotEqual(String2.parseDouble(contourLinesAt), Double.NaN,   errorInMethod + "'contourLinesAt' wasn't set.");
        Test.ensureTrue(defaultUnits == 'S' || defaultUnits == 'A',            errorInMethod + "'defaultUnits' (" + defaultUnits + ") must be 'S' (standard units) or 'A' (alternate units).");
        //fgdcSubstitutions null if not used
        //Test.ensureNotNull(fileNameUtility,                                    errorInMethod + "'fileNameUtility' wasn't set.");
        Test.ensureNotNull(internalName,                                       errorInMethod + "'internalName' wasn't set.");
        //keywords null if not used
        //keywordsVocabulary null if not used
        Test.ensureNotEqual(daysTillDataAccessAllowed, Integer.MAX_VALUE,      errorInMethod + "'daysTillDataAccessAllowed' wasn't set.");
        Test.ensureNotNull(option,                                             errorInMethod + "'option' wasn't set.");
        Test.ensureNotNull(palette,                                            errorInMethod + "'palette' wasn't set.");
        Test.ensureNotEqual(String2.parseDouble(paletteMax), Double.NaN,       errorInMethod + "'paletteMax' wasn't set.");
        Test.ensureNotEqual(String2.parseDouble(paletteMin), Double.NaN,       errorInMethod + "'paletteMin' wasn't set.");
        //references is null if not used
        Test.ensureNotNull(paletteScale,                                       errorInMethod + "'paletteScale' wasn't set.");
        //summary is null if not used
        Test.ensureNotNull(udUnits,                                            errorInMethod + "'udUnits' wasn't set.");
        Test.ensureNotNull(unitsOptions,                                       errorInMethod + "'unitsOptions' wasn't set.");
    }


    /**
     * The string representation of this gridDataSet (for diagnostic purposes).
     * @return the string representation of this gridDataSet.
     */
    public String toString() {
        return "gridDataSet " + internalName + ": " + option + 
            "\n  activeTimePeriodOptions=" + String2.toCSSVString(activeTimePeriodOptions) +
            "\n  activeTimePeriodTitles=" + String2.toCSSVString(activeTimePeriodTitles) +
            "\n  activeTimePeriodNHours=" + String2.toCSSVString(activeTimePeriodNHours) +
            "\n  activeTimePeriodOpendapUrls=" + String2.toNewlineString(activeTimePeriodOpendapUrls) +
            //don't show: lots of activeTimePeriodTimes stored in a Vector()
            "  altContourLinesAt=" + altContourLinesAt + //gets \n from toNewlineString above
            "\n  altPaletteMin=" + altPaletteMin +
            "\n  altPaletteMax=" + altPaletteMax +
            "\n  altOffset=" + altOffset +
            "\n  altScaleFactor=" + altScaleFactor +
            "\n  altUdUnits=" + altUdUnits +
            "\n  anomalyDataSet=" + anomalyDataSet +
            "\n  boldTitle=" + boldTitle +
            "\n  contourLinesAt=" + contourLinesAt +
            "\n  courtesy=" + courtesy + 
            //dataSetRB2
            "\n  defaultUnits=" + defaultUnits +
            "\n  fgdcSubstitutions=" + String2.toCSSVString(fgdcSubstitutions) +
            //fileNameUtility
            "\n  internalName=" + internalName +
            "\n  keywords=" + keywords +
            "\n  keywordsVocabulary=" + keywordsVocabulary +
            "\n  daysTillDataAccessAllowed=" + daysTillDataAccessAllowed +
            "\n  option=" + option +
            "\n  palette=" + palette +
            "\n  paletteMax=" + paletteMax +
            "\n  paletteMin=" + paletteMin +
            "\n  paletteScale=" + paletteScale +
            "\n  references=" + references +
            "\n  summary=" + summary +
            "\n  udUnits=" + udUnits +
            "\n  unitsOptions=" + String2.toCSSVString(unitsOptions) +
            "\n"; 
    }


    /**
     *  The Time Period title for a single pass of the satellite.
     */
    public static String singlePassTitle() {
        return "The data from one observation.";
    }

    /**
     * This returns a Time Period title for a composite dataset covering 
     * the specified time period.
     *
     * @param timePeriodString  e.g., "a 1 day"
     * @return a title for a composite dataset covering nDays
     */
    public static String compositeTitle(String timePeriodString) {
        return "A composite (mean) of valid values from all observations in " + 
            timePeriodString + " time period.";
    }
  
    /**
     * This gets the timePeriodIndex associated with a timePeriodValue.
     * @param timePeriodValue
     * @return timePeriodIndex (or -1 if not found)
     */
    public int getTimePeriodIndex(String timePeriodValue) {
        return String2.indexOf(activeTimePeriodOptions, timePeriodValue);
    }

    /**
     * This gets the timeIndex associated with a timeValue (for the specified timePeriod).
     * @param timePeriodIndex
     * @param timeValue
     * @return timeIndex (or -1 if not found)
     */
    public int getTimeIndex(int timePeriodIndex, String timeValue) {
        String[] timePeriodTimes = (String[])activeTimePeriodTimes.get(timePeriodIndex);
        return String2.indexOf(timePeriodTimes, timeValue);
    }


    /**
     * This makes the specified grid as best it can
     * (including calling grid.makeLonPM180AndSubset).
     *
     * <p>This rounds min/max/X/Y to the nearest grid point. Rounding 
     * is most appropriate because the x,y grid points represent the
     * center of a box. Thus the data for a given x,y point may be from 
     * a grid point just outside of the range you request. Also, if you
     * request the data for one point (minX=maxX, minY=maxY), rounding
     * will return the appropriate grid point (the minX,minY point is in
     * a box; the procedure returns the value for the box).
     *
     * <p>This does not set the Grid's attributes, since it takes time to do so
     * and often isn't needed, and because the data may be altered before
     * it is saved (and hence need the attributes to be set again).
     * See setAttributes().
     *
     * @param timePeriodValue e.g., "1 day" must exactly match one of the timePeriod options
     * @param timeValue   e.g., "2006-01-02" must exactly match one of the time options
     *    for the timePeriod
     * @param minX the requested min longitude. 
     *    minX and maxX can be pm180 or 0..360. 
     *    makeGrid will adjust them, if needed, to get the desired data.  
     *    The pm180 nature of the resulting grd file will either pm180 or 0..360,
     *    as you request here.
     * @param maxX the requested max longitude.
     * @param minY the requested min latitude.
     * @param maxY the requested max latitude.
     * @param desiredNWide the requested minimal resolution (nPixels wide).
     *   The file may have higher resolution.
     *   The file will have lower resolution if that is all that is available.
     *   If the range of data available is smaller than requested,
     *      this will be descreased proportionally.
     *   Use Integer.MAX_VALUE for maximum available resolution.
     * @param desiredNHigh the requested minimal resolution (nPixels high).
     *   (See desiredNWide description.)
     * @return a grid object will be populated with a finished form of the 
     *   requested data (grid.makeLonPM180AndSubset will have been called).
     * @throws Exception if trouble (including no data in range, 
     *    since can't store grd/nc file with no data)
     */
    public abstract Grid makeGrid(String timePeriodValue, String timeValue,          
        double minX, double maxX, double minY, double maxY,
        int desiredNWide, int desiredNHigh) throws Exception;
    /*{
       
        //generate a grd file which has the needed data (although perhaps too much) 
        long time = System.currentTimeMillis();
        if (verbose) String2.log("\n//**** GridDataSet.makeGrid " + baseName);

        Grid grid = lowMakeGrid(timePeriodValue, timeValue,          
            minX, maxX, minY, maxY, desiredNWide, desiredNHigh, grid);

        //ensure we have the correct subset ... next several steps  
        grid.ensureThereIsData();

        //makeLonPM180Subset (throws Exception if no data)
        grid.makeLonPM180AndSubset(minX, maxX, minY, maxY, desiredNWide, desiredNHigh);

        if (verbose) String2.log("\\\\**** GridDataSet.makeGrid " + internalName + 
            " done. TOTAL TIME=" + (System.currentTimeMillis() - time) + "\n");

        return grid;
        
    }
    */


/* need to set up a grid cache system? here are some raw materials
        //newName can be based on request (even though those values may be changed)
        //since the file will represent the results of this request
        //and will match future identical request.
        String newName = baseName + 
            FileNameUtility.makeWESNString(minX, maxX, minY, maxY) +
            FileNameUtility.makeNLonNLatString(desiredNLon, desiredNLat) + ".grd";
        if (File2.touch(newDir + newName)) {
            if (verbose) String2.log("  reuse " + newName);
            if (userWantsGrid)
                grid.readGrd(newDir + newName, 
                    minX, maxX, minY, maxY, //more efficient to read just desired part; and should already match
                    desiredNLon, desiredNLat);
            if (verbose) String2.log("  Opendap.makeGrid done. TIME=" + 
                (System.currentTimeMillis() - startTime) + "\n");
            return newName;
        } 
*/


    /** 
     * This returns the start GregorianCalendar for the specified timePeriodValue 
     * and centeredTimeValue.
     * This default implementation works like TimePeriods.getStartCalendar (where
     * timeValue is a cleanIsoCenteredTime).
     * 
     * @param timePeriodValue
     * @param centeredTimeValue
     * @return the start GregorianCalendar for the specified timePeriodValue and 
     *    centeredTimeValue.
     * @throws Exception if trouble
     */
    public GregorianCalendar getStartCalendar(String timePeriodValue, 
        String centeredTimeValue) throws Exception {

        return TimePeriods.getStartCalendar(timePeriodValue, centeredTimeValue, null);
    }

    /** 
     * This returns the end GregorianCalendar for the specified timePeriodValue 
     * and centeredTimeValue.
     * This default implementation works like TimePeriods.getEndCalendar (where
     * timeValue is a cleanIsoCenteredTime).
     *
     * @param timePeriodValue
     * @param centeredTimeValue
     * @return the end GregorianCalendar for the specified timePeriodValue and 
     *    centeredTimeValue.
     * @throws Exception if trouble
     */
    public GregorianCalendar getEndCalendar(String timePeriodValue, 
        String centeredTimeValue) throws Exception {

        return TimePeriods.getEndCalendar(timePeriodValue, centeredTimeValue, null);
    }

    /**
     * Find the closest match for timeValue in timeOptions (ascending sorted).
     * This default implementation works like Calendar2.binaryFindClosest().
     *
     * @param timeOptions 
     * @param timeValue 
     * @return the index (in timeOptions) of the best match for timeValue.
     *   If timeValue is null or "", this returns timeOptions.length-1.
     */
    public int binaryFindClosestTime(String timeOptions[], String timeValue) {
        return Calendar2.binaryFindClosest(timeOptions, timeValue);
    }

    /**
     * This creates the legend time string.
     * This default implementation works like TimePeriods.getLegendTime.
     *
     * @param timePeriod must be one of the TimePeriod.OPTIONs
     * @param centeredTime an iso date time string
     * @return the dateTime string for the legend
     * @throws Exception if trouble
     */
    public String getLegendTime(String timePeriod, String centeredTime) 
        throws Exception {

        return TimePeriods.getLegendTime(timePeriod, centeredTime);
    }

    /**
     * This determines if users are allowed to download the specified data,
     * based on this dataset's daysTillDataAccessAllowed.
     * See dataSet.daysTillDataReleased.
     * This default implementation works like OneOf.dataAccessAllowed.
     *
     * @param timePeriod one of the TimePeriod.OPTIONs
     * @param centeredDate  the centered date of the composite, in iso format (e.g. "2001-02-05"). 
     * @return true if access is allowed
     * @throws RuntimeException if trouble (e.g., invalid endingDate)
     */
    public boolean dataAccessAllowedCentered(String timePeriod, String centeredDate) throws Exception {
        return OneOf.dataAccessAllowed(daysTillDataAccessAllowed, 
            Calendar2.formatAsISODateTimeT(TimePeriods.getEndCalendar(timePeriod, centeredDate, null)));
    }

    /**
     * This sets gridGlobalAttributes, gridLatAttributes, gridLonAttributes, and
     * dataAttributes based on source's attributes plus info known here (e.g, 
     * this gridDataSet's courtesy and other attributes, and the lat and lon 
     * range of the grid).
     * This does not call calculateStats; see grid.setStatsAttributes().
     * See MetaMetadata.txt for more information.
     *
     * <p>This is used in some situations. In other situations, grid.setAttributes 
     * is used.
     *
     * <p>This is the default implementation (used by e.g., GridDataSetThredds),
     * but subclasses may override this.
     *
     * <p>!!!LIMITATION: this method cleans up the attributes from the
     * metadata standards that I am familiar with 
     * (COARDS, CF-1.6, ACDD-1.3, CWHDF).
     * But there is no way it can clean up other metadata from other standards
     * that I don't know about.
     * Hence, metadata that is passed through may be incorrect (e.g., the 
     * metadata may not reflect a specific subset that the user has selected).
     *
     * @param grid
     * @param fileName A CWBrowser-style file name (so that fileNameUtility
     *    can generate the information.
     */
    public void setAttributes(Grid grid, String fileName) throws Exception {
//should this clear existing attributes?
       
        //Currently, this should always be false
        boolean useAlternateUnits = FileNameUtility.getAlternateUnits(fileName);
        Test.ensureTrue(!useAlternateUnits, String2.ERROR + " in GridDataSetThredds.setAttributes:\n" + 
            "file is using alternate units!\nfileName=" + fileName);
        double lat[] = grid.lat;
        double lon[] = grid.lon;
        int nLat = lat.length;
        int nLon = lon.length;

        Attributes gridGlobalAttributes = grid.globalAttributes();
        Attributes gridLatAttributes    = grid.latAttributes();
        Attributes gridLonAttributes    = grid.lonAttributes();
        Attributes gridDataAttributes   = grid.dataAttributes();

        //copy the attributes from this gridDataSet
        globalAttributes.copyTo( gridGlobalAttributes);
        latAttributes.copyTo(    gridLatAttributes);
        lonAttributes.copyTo(    gridLonAttributes);
        dataAttributes.copyTo(   gridDataAttributes);
        
        //assemble the global metadata attributes
        //!!! In general, set attributes that haven't already been set.
        gridGlobalAttributes.set("Conventions",               FileNameUtility.getConventions());
        gridGlobalAttributes.set("standard_name_vocabulary",  FileNameUtility.getStandardNameVocabulary());
        gridGlobalAttributes.set("title",                     boldTitle);
        if (gridGlobalAttributes.get("keywords") == null && keywords != null) {
            gridGlobalAttributes.set("keywords",              keywords);
            gridGlobalAttributes.set("keywords_vocabulary",   keywordsVocabulary);
        }
        gridGlobalAttributes.set("id",                        FileNameUtility.getID(fileName));
        gridGlobalAttributes.set("naming_authority",          FileNameUtility.getNamingAuthority());
        gridGlobalAttributes.set("summary",                   summary);  //summary was cleaned up in constructor
        gridGlobalAttributes.set("date_created",              FileNameUtility.getDateCreated());
        gridGlobalAttributes.set("date_issued",               FileNameUtility.getDateCreated());
        gridGlobalAttributes.set("cdm_data_type",             FileNameUtility.getCDMDataType());
        if (gridGlobalAttributes.get("creator_name") == null)
            gridGlobalAttributes.set("creator_name",          FileNameUtility.getCreatorName());
        if (gridGlobalAttributes.get("creator_url") == null)
            gridGlobalAttributes.set("creator_url",           FileNameUtility.getCreatorURL());
        if (gridGlobalAttributes.get("creator_email") == null)
            gridGlobalAttributes.set("creator_email",         FileNameUtility.getCreatorEmail());
        if (gridGlobalAttributes.get("institution") == null)
            gridGlobalAttributes.set("institution",           FileNameUtility.getCreatorName()); //was courtesy);
        if (gridGlobalAttributes.get("project") == null)
            gridGlobalAttributes.set("project",               FileNameUtility.getProject());
        if (gridGlobalAttributes.get("processing_level") == null)
            gridGlobalAttributes.set("processing_level",      FileNameUtility.getProcessingLevel());
        if (gridGlobalAttributes.get("acknowledgement") == null)
            gridGlobalAttributes.set("acknowledgement",       FileNameUtility.getAcknowledgement());
        gridGlobalAttributes.set("geospatial_lat_min",        Math.min(lat[0], lat[nLat-1]));
        gridGlobalAttributes.set("geospatial_lat_max",        Math.max(lat[0], lat[nLat-1]));
        gridGlobalAttributes.set("geospatial_lon_min",        Math.min(lon[0], lon[nLon-1]));
        gridGlobalAttributes.set("geospatial_lon_max",        Math.max(lon[0], lon[nLon-1]));
        gridGlobalAttributes.set("geospatial_lat_units",      FileNameUtility.getLatUnits());
        gridGlobalAttributes.set("geospatial_lat_resolution", Math.abs(grid.latSpacing));
        gridGlobalAttributes.set("geospatial_lon_units",      FileNameUtility.getLonUnits());
        gridGlobalAttributes.set("geospatial_lon_resolution", Math.abs(grid.lonSpacing));
        //!!!! all datasets ASSUMED to be at altitude = 0
        gridGlobalAttributes.set("geospatial_vertical_min",   0.0); //0.0 (not 0, which is int)
        gridGlobalAttributes.set("geospatial_vertical_max",   0.0); //0.0 (not 0, which is int)
        gridGlobalAttributes.set("geospatial_vertical_units", "m");
        gridGlobalAttributes.set("geospatial_vertical_positive","up");
        gridGlobalAttributes.set("time_coverage_start",       Calendar2.formatAsISODateTimeTZ(FileNameUtility.getStartCalendar(fileName)));
        gridGlobalAttributes.set("time_coverage_end",         Calendar2.formatAsISODateTimeTZ(FileNameUtility.getEndCalendar(fileName)));
        //gridGlobalAttributes.set("time_coverage_resolution", "P12H"));
        //if (gridGlobalAttributes.get("standard_name") == null && standardName != null) //if standardName is null, I will set below  INACTIVE
        //    gridGlobalAttributes.set("standard_name_vocabulary",  FileNameUtility.getStandardNameVocabulary());
        if (gridGlobalAttributes.get("license") == null)
            gridGlobalAttributes.set("license",               FileNameUtility.getLicense());
        if (gridGlobalAttributes.get("contributor_name") == null) {
            gridGlobalAttributes.set("contributor_role",          FileNameUtility.getContributorRole());
            gridGlobalAttributes.set("contributor_name",          courtesy);
        }
        //if (gridGlobalAttributes.get("references") == null)
        //    gridGlobalAttributes.set("references",            fileNameUtility.getReferences(fileName));
        //if (gridGlobalAttributes.get("source") == null)
        //    gridGlobalAttributes.set("source",                fileNameUtility.getSource(fileName));
        //attributes for Google Earth
        gridGlobalAttributes.set("Southernmost_Northing",     Math.min(lat[0], lat[nLat-1]));
        gridGlobalAttributes.set("Northernmost_Northing",     Math.max(lat[0], lat[nLat-1]));
        gridGlobalAttributes.set("Westernmost_Easting",       Math.min(lon[0], lon[nLon-1]));
        gridGlobalAttributes.set("Easternmost_Easting",       Math.max(lon[0], lon[nLon-1]));

        //gridGlobalAttributes for HDF files using CoastWatch Metadata Specifications  
        //required unless noted otherwise
        //gridGlobalAttributes.set("cwhdf_version",      "3.4");          //string    It is or it isn't; my things won't change that.
        //if (satellite.length() > 0) {
        //    gridGlobalAttributes.set("satellite",      satellite); //string
        //    gridGlobalAttributes.set("sensor",         sensor); //string
        //} else {
        //    gridGlobalAttributes.set("data_source",    sensor); //string
        //}
        gridGlobalAttributes.set("cwhdf_version",      "3.4");          //string
        gridGlobalAttributes.set("composite",          FileNameUtility.getComposite(fileName)); //string (not required)
 
        gridGlobalAttributes.set("pass_date",          new IntArray(FileNameUtility.getPassDate(fileName))); //int32[nDays] 
        gridGlobalAttributes.set("start_time",         new DoubleArray(FileNameUtility.getStartTime(fileName))); //float64[nDays] 
        gridGlobalAttributes.set("origin",             courtesy);  //string
        String oldHistory = gridGlobalAttributes.getString("history");
        if (oldHistory == null) oldHistory = courtesy;
        gridGlobalAttributes.set("history",            DataHelper.addBrowserToHistory(oldHistory)); //string  

        //write map projection data
        gridGlobalAttributes.set("projection_type",    "mapped");       //string
        gridGlobalAttributes.set("projection",         "geographic");   //string
        gridGlobalAttributes.set("gctp_sys",           0);   //int32
        gridGlobalAttributes.set("gctp_zone",          0);   //int32
        gridGlobalAttributes.set("gctp_parm",          new DoubleArray(new double[15])); //float64[15 0's]
        gridGlobalAttributes.set("gctp_datum",         12);  //int32 12=WGS84

        //determine et_affine transformation    
        // lon = a*row + c*col + e
        // lat = b*row + d*col + f
        double matrix[] = {0, -grid.latSpacing, grid.lonSpacing, 0, lon[0], lat[lat.length-1]}; //up side down
        gridGlobalAttributes.set("et_affine",          new DoubleArray(matrix)); //float64[] {a, b, c, d, e, f}

        //write row and column attributes
        gridGlobalAttributes.set("rows",               nLat);//int32 number of rows
        gridGlobalAttributes.set("cols",               nLon);//int32 number of columns
        gridGlobalAttributes.set("polygon_latitude",   new DoubleArray(new double[]{   //not required
            lat[0], lat[nLat - 1], lat[nLat - 1], lat[0], lat[0]}));
        gridGlobalAttributes.set("polygon_longitude",  new DoubleArray(new double[]{   //not required
            lon[0], lon[0], lon[nLon - 1], lon[nLon - 1], lon[0]}));


        //COARDS, CF, ACDD metadata attributes for latitude
        gridLatAttributes.set("_CoordinateAxisType",   "Lat");
        gridLatAttributes.set("long_name",             "Latitude");
        gridLatAttributes.set("standard_name",         "latitude");
        gridLatAttributes.set("units",                 FileNameUtility.getLatUnits());

        //Lynn's metadata attributes
        gridLatAttributes.set("point_spacing",         "even");
        gridLatAttributes.set("actual_range",          new DoubleArray(new double[]{lat[0], lat[nLat-1]}));
        gridLatAttributes.set("axis",                  "Y");

        //CWHDF metadata attributes for Latitude
        //gridLatAttributes.set("long_name",             "Latitude")); //string 
        //gridLatAttributes.set("units",                 fileNameUtility.getLatUnits(fileName))); //string 
        gridLatAttributes.set("coordsys",              "geographic");    //string
        //gridLatAttributes.set("fraction_digits",       latLonFractionDigits); //int32


        //COARDS, CF, ACDD metadata attributes for longitude
        gridLonAttributes.set("_CoordinateAxisType",   "Lon");
        gridLonAttributes.set("long_name",             "Longitude");
        gridLonAttributes.set("standard_name",         "longitude");
        gridLonAttributes.set("units",                 FileNameUtility.getLonUnits());

        //Lynn's metadata attributes
        gridLonAttributes.set("point_spacing",         "even");
        gridLonAttributes.set("actual_range",          new DoubleArray(new double[]{lon[0], lon[nLon-1]}));
        gridLonAttributes.set("axis",                  "X");
        
        //CWHDF metadata attributes for Longitude
        //gridLonAttributes.set("long_name",             "Longitude"); //string 
        //gridLonAttributes.set("units",                 fileNameUtility.getLonUnits(fileName));  //string  
        gridLonAttributes.set("coordsys",              "geographic");    //string
        //gridLonAttributes.set("fraction_digits",       latLonFractionDigits); //int32


        //COARDS, CF, ACDD metadata attributes for data
        gridDataAttributes.set("long_name",            boldTitle);

        //gridDataAttributes.set("standard_name",        standardName);  //see above
        gridDataAttributes.set("units",                useAlternateUnits? altUdUnits : udUnits);
        //see grid.setStatsAttributes for
        //  gridDataAttributes.set("_FillValue",           mvAr); //must be same type as data
        //  gridDataAttributes.set("missing_value",        mvAr); //must be same type as data
        //  gridDataAttributes.set("numberOfObservations", nValidPoints);
        //  gridDataAttributes.set("actual_range",         rangeAr);

        //CWHDF metadata attributes for the data: varName
        //gridDataAttributes.set("long_name",            fileNameUtility.getTitle(fileName))); //string
        //gridDataAttributes.set("units",                fileNameUtility.getUDUnits(fileName))); //string  
        gridDataAttributes.set("coordsys",             "geographic");    //string
        //gridDataAttributes.set("fraction_digits",      useAlternateUnits? altDataFractionDigits : dataFractionDigits); //int32
     
        gridDataAttributes.remove("_coordinateSystem"); //old (pre Jan 2007) .nc files had this (added by netcdf-java library)

    }

    /**
     * Make a Table with time series data for the grid point nearest to x,y.
     * If x,y is outside the range of the grid, an empty table will be
     * returned.
     *
     * <p> This is a generic inefficient implementation. 
     *  Subclasses are encouraged to replace this with a more efficient
     *  implementation specific to that subclass.
     *
     * @param newDir  the directory for the intermediate grd file (if any)
     * @param x the desired latitude
     * @param y the desired longitude
     * @param isoMinDate an ISO format date/time for the minimum ok time.
     *    The searches are done based on the activeTimePeriodTimes.
     *    So, for example, searches for 1 day files are based on just date (2006-06-16),
     *    not dateTtime (e.g., 2006-06-16T23:59:59). 
     *    If isoMinDate and isoMaxDate are between two adjacent available dates,
     *    the datum for the single closest date is returned.
     * @param isoMaxDate an ISO format date/time for the maximum ok time
     * @param timePeriod one of the activeTimePeriodOptions 
     * @return a Table with 6 columns: lon, lat, depth (meters, positive=down, currently always 0), 
     *     time (seconds since 1970-01-01), id (internalName), data).
     *   The data will be unpacked, in the standard units.
     *   Numeric column types may vary.
     *   Rows with missing values are NOT removed.
     *   The metadata (e.g., actual_range) will be correct (as correct as I can make it). 
     *   If there are no matching time points, this returns a table with all the
     *   usual columns but 0 rows.
     * @throws Exception if trouble (e.g., isoMaxDate is invalid)
     */
    public Table getTimeSeries(String newDir, double x, double y,
        String isoMinDate, String isoMaxDate, String timePeriod) throws Exception {

        if (verbose) String2.log("GridDataSet.getTimeSeries " + internalName + 
            " x=" + x + " y=" + y + 
            " isoMinDate=" + isoMinDate + " isoMaxDate=" + isoMaxDate);
        long time = System.currentTimeMillis();

        //this doesn't work/can't be used on climatology datasets
        if (isClimatology)
            Test.error("GridDataSet.getTimeSeries can't be used on climatology datasets like " + internalName + ".");

        isoMinDate = String2.replaceAll(isoMinDate, "T", " ");
        isoMaxDate = String2.replaceAll(isoMaxDate, "T", " ");
        int timePeriodNHours = TimePeriods.getNHours(timePeriod);
        double expectedGapSeconds; //a gap of longer than this means data is missing
        if (timePeriodNHours == 0 || timePeriodNHours % 24 != 0) { //timePeriod is 0 or 25 or 33 hours
            expectedGapSeconds = Calendar2.SECONDS_PER_HOUR; //1 hour
        } else if (timePeriodNHours < 30 * 24) { //timePeriod is nDays
            expectedGapSeconds = Calendar2.SECONDS_PER_DAY; //1 day
        } else { //timePeriod is >= 1 month               
            expectedGapSeconds = 32 * Calendar2.SECONDS_PER_DAY;  //e.g., gap is 32 days (more than 1 month)
        }

        //make the resultsTable
        DoubleArray xColumn = new DoubleArray();
        DoubleArray yColumn = new DoubleArray();
        DoubleArray depthColumn = new DoubleArray();
        DoubleArray tColumn = new DoubleArray();
        StringArray idColumn = new StringArray();
        FloatArray dataColumn = new FloatArray(); //currently, all grid datasets are floats (Future: better if dynamically chosen)
        Table results = new Table();
        globalAttributes.copyTo(results.globalAttributes());
        results.addColumn(0, "LON", xColumn, (Attributes)lonAttributes.clone());
        results.addColumn(1, "LAT", yColumn, (Attributes)latAttributes.clone());
        results.addColumn(2, "DEPTH", depthColumn, (Attributes)depthAttributes.clone());
        results.addColumn(3, "TIME", tColumn, (Attributes)timeAttributes.clone());
        results.addColumn(4, "ID", idColumn, new Attributes());
        results.addColumn(5, internalName, dataColumn, (Attributes)dataAttributes.clone());

        //!!! If 'L'ocal dataset, limit data access   (since data stored in .zipped .grd files),
        String dataAccessLimit = null;
        double originalMinSeconds = Calendar2.isoStringToEpochSeconds(isoMinDate); //throws exception if trouble
        /*
        if (internalName.charAt(0) == 'L') {
            double limit = Double.NaN;
            if (timePeriodNHours == 0 || timePeriodNHours % 24 != 0) { //timePeriod is 0 or 25 or 33 hours; should be hourly data
                dataAccessLimit = "1 day";
                limit = Calendar2.SECONDS_PER_DAY; 
            } else if (timePeriodNHours < 30 * 24) { //timePeriod is nDays
                dataAccessLimit = "1 month";
                limit = Calendar2.SECONDS_PER_DAY * 31;  
            } else { //timePeriod is >= 1 month   
                dataAccessLimit = "2 year";
                limit = 2 * Calendar2.SECONDS_PER_DAY * 366;  
            }

            if (dataAccessLimit != null) {
                double maxSeconds = Calendar2.isoStringToEpochSeconds(isoMaxDate);  //throws exception if trouble
                //String2.log("  maxSeconds=" + maxSeconds + " limit=" + limit + " dif=" + (originalMinSeconds > maxSeconds - limit)); 
                if (originalMinSeconds < maxSeconds - limit) {
                    //apply limit   and put a blank row in the file
                    isoMinDate = Calendar2.epochSecondsToIsoStringSpace(maxSeconds - limit);
                } else {
                    //limit doesn't apply
                    dataAccessLimit = null;
                }
            }
        }
        */
        if (verbose) String2.log("  origMinSeconds=" + originalMinSeconds + " isoMinDate=" + isoMinDate + 
            " dataAccessLimit=" + dataAccessLimit + " timePeriodNHours=" + timePeriodNHours);

        //find the timePeriod
        int timePeriodIndex = String2.indexOf(activeTimePeriodOptions, timePeriod);
        Test.ensureNotEqual(timePeriodIndex, -1, 
            String2.ERROR + " in GridDataSet.getTimeSeries:\n" +
                "unrecognized timePeriod: " + timePeriod);
        String timePeriodInFileName = TimePeriods.getInFileName(timePeriod);

        //get the dates for that timePeriod
        String[] activeDates = (String[])activeTimePeriodTimes.get(timePeriodIndex);   

        //find the first and last date indexes (use activeDates, since they have been adjusted (e.g., centeredTime vs endTime))
        int firstDateIndex = Calendar2.binaryFindFirstGE(activeDates, isoMinDate);
        int lastDateIndex  = Calendar2.binaryFindLastLE( activeDates, isoMaxDate);

        if (firstDateIndex == activeDates.length ||
            lastDateIndex == -1)
            return results;
        //range is between two adjacent dates? return datum for single closest day
        if (firstDateIndex == lastDateIndex + 1) {
            firstDateIndex = Calendar2.binaryFindClosest(activeDates, isoMinDate);
            lastDateIndex = firstDateIndex;
        }
        if (verbose) String2.log("  firstDate=" + activeDates[firstDateIndex] + " >= isoMinDate=" + isoMinDate);
        if (verbose) String2.log("  lastDate="  + activeDates[lastDateIndex]  + " <= isoMaxDate=" + isoMaxDate);

        //find the data for each relevant time point
        StringObject resultingFileName = new StringObject(null);
        double lastTime = 0;
        double lastLon = Double.NaN, lastLat = Double.NaN;
        double depth = 0;  //currently, all grid depths treated as 0 !!!!!
        for (int dateIndex = firstDateIndex; dateIndex <= lastDateIndex; dateIndex++) {

            //if gap since data is too long, add mv row
            double thisTime = Calendar2.isoStringToEpochSeconds(activeDates[dateIndex]); //activeDates should be valid iso dates
            if (dateIndex > firstDateIndex && thisTime - lastTime > expectedGapSeconds) {
                xColumn.addDouble(lastLon);
                yColumn.addDouble(lastLat);
                depthColumn.addDouble(depth);  
                tColumn.add(thisTime - expectedGapSeconds); //crude but sufficient to cause line break
                idColumn.addString(internalName);
                dataColumn.addFloat(Float.NaN);
            }

            //get the datum for this dateIndex
            //e.g., LATsstaS1day_20030304
            String baseName = internalName + "S" +
                timePeriodInFileName + "_" +
                Calendar2.removeSpacesDashesColons(activeDates[dateIndex]);
            if (verbose && dateIndex == firstDateIndex) String2.log("  first BaseName=" + baseName);
            Grid grid = makeGrid(timePeriod, activeDates[dateIndex],          
                x, x, y, y, 1, 1);

            //if first dateIndex and there is a dataAccessLimit, add row to be a place holder 1 hour back
            if (dataAccessLimit != null) {
                xColumn.addDouble(grid.lon[0]);
                yColumn.addDouble(grid.lat[0]);
                depthColumn.addDouble(depth); 
                tColumn.add(thisTime - Calendar2.SECONDS_PER_HOUR); //really crude, but ok: 1 hour back (if there is only 1 real datum, sgtGraph can't handle <1hr on x axis
                idColumn.addString(internalName);
                dataColumn.addFloat(Float.NaN);
                dataAccessLimit = null; //signal that it has been dealt with
            }

            //add the row for this datum
            String2.log("  adding date=" + activeDates[dateIndex] + " data=" + grid.data[0]);
            xColumn.addDouble(grid.lon[0]);
            yColumn.addDouble(grid.lat[0]);
            depthColumn.addDouble(depth); 
            tColumn.add(thisTime); 
            idColumn.addString(internalName);
            dataColumn.addDouble(grid.data[0]);
            lastLon = grid.lon[0];
            lastLat = grid.lat[0];
            lastTime = thisTime;
        }            

        //add metadata
        //for params, if null, change to "" so existing values not removed
        results.setAttributes(0, 1, 2, 3, boldTitle, 
            "",  //data type was Grid, now from Grid but not exactly Grid,   Arrgggh. Just remove it.
            DataHelper.CW_CREATOR_EMAIL, //who is creating this file...
            DataHelper.CW_CREATOR_NAME,
            DataHelper.CW_CREATOR_URL,
            DataHelper.CW_PROJECT,
            FileNameUtility.makeAveragedGridTimeSeriesName(internalName, 'S', 
                x, y, isoMinDate, isoMaxDate, timePeriod), //id
            keywordsVocabulary == null? "" : keywordsVocabulary, 
            keywords           == null? "" : keywords, 
            references         == null? "" : references, 
            summary            == null? "" : summary, 
            courtesy           == null? "" : courtesy, //who is source of the data...
            "Centered Time" + 
                (TimePeriods.getNHours(timePeriod) > 0? " of " + timePeriod + " Composites" : ""));
        results.columnAttributes(0).remove("coordsys");
        results.columnAttributes(0).remove("point_spacing");
        results.columnAttributes(0).remove("_CoordinateAxisType");
        results.columnAttributes(1).remove("coordsys");
        results.columnAttributes(1).remove("point_spacing");
        results.columnAttributes(1).remove("_CoordinateAxisType");
        Attributes tDataAttributes = results.columnAttributes(5);
        tDataAttributes.set("long_name", boldTitle);
        tDataAttributes.set("units", udUnits);
        tDataAttributes.remove("_coordinateSystem"); //old ndbc files have this; mbari has this
        tDataAttributes.remove("_CoordinateAxes");  //new ndbc files
        tDataAttributes.remove("coordsys");
        tDataAttributes.remove("numberOfObservations");
        tDataAttributes.remove("percentCoverage");
        //if (verbose) String2.log(results.toString());

        //remove known global attributes no longer appropriate  (mostly CWHDF metadata)
        Attributes tGlobalAttributes = results.globalAttributes();
        tGlobalAttributes.remove("cols");
        tGlobalAttributes.remove("composite");
        tGlobalAttributes.remove("cwhdf_version");
        tGlobalAttributes.remove("et_affine");
        tGlobalAttributes.remove("gctp_datum");
        tGlobalAttributes.remove("gctp_parm");
        tGlobalAttributes.remove("gctp_sys");
        tGlobalAttributes.remove("gctp_zone");
        tGlobalAttributes.remove("geospatial_lat_resolution");
        tGlobalAttributes.remove("geospatial_lon_resolution");
        tGlobalAttributes.remove("pass_date");
        tGlobalAttributes.remove("polygon_latitude");
        tGlobalAttributes.remove("polygon_longitude");
        tGlobalAttributes.remove("processing_level");
        tGlobalAttributes.remove("geographic");
        tGlobalAttributes.remove("projection_type");
        tGlobalAttributes.remove("rows");
        tGlobalAttributes.remove("start_time");

        if (verbose) String2.log("  GridDataSet.getTimeSeries " + internalName + 
            " done. nRows=" + results.nRows() + 
            " TIME=" + (System.currentTimeMillis() - time) + "\n");
        return results;

    }

    /**
     * Find the appropriate matching time option.
     * This is loose (Dave said to do it this way 12/18/2006) -- 
     * it is a match if the modernTime is anywhere within
     * the startCalendar and endCalendar of the closest climatologyCenteredTime.
     * (Alternative would be to insist in exact match.)
     *
     * @param timePeriod  e.g., "monthly", from either the modern or the climatology GridDataSet
     *   (presumably nHours is same for either)
     * @param climatologyCenteredTimes  an array of climatology centered time options (in year 0001)
     * @param modernTime a modern day centered iso time (e.g., in year 2006)
     *     or a climatology time (e.g., 0001-07-16 12:00:00)
     * @return the index of the appropriate climatologyCenteredTime 
     *    (or -1 if none is appropriate)
     * @throws Exception if trouble
     */
    public static int findAppropriateClimatologyTime(String timePeriod, 
        String climatologyCenteredTimes[], String modernTime) throws Exception {

        if (modernTime == null || modernTime.length() < 4)
            return -1;

        //find closest climatologyCenteredTime 
        String newTime = "0001" + modernTime.substring(4);
        int closest = Calendar2.binaryFindClosest(climatologyCenteredTimes, newTime);
        String closestTime = climatologyCenteredTimes[closest];
        GregorianCalendar startCalendar = TimePeriods.getStartCalendar(timePeriod, closestTime, ""); 
        GregorianCalendar endCalendar   = TimePeriods.getEndCalendar(timePeriod, closestTime, "");
        double startSeconds = Calendar2.gcToEpochSeconds(startCalendar);
        double endSeconds = Calendar2.gcToEpochSeconds(endCalendar);
        double newSeconds = Calendar2.isoStringToEpochSeconds(newTime);  //throws exception if trouble
        //String2.log(
        //    "findAppropriateClimatologyTime start=" + Calendar2.epochSecondsToIsoStringTZ(startSeconds) +
        //    "\n  new=" + Calendar2.epochSecondsToIsoStringTZ(newSeconds) +
        //    "        end=" + Calendar2.epochSecondsToIsoStringTZ(endSeconds));
        if (startSeconds == endSeconds) {
            return newSeconds == startSeconds? closest : -1;
        } else {
            return newSeconds >= startSeconds && newSeconds < endSeconds? closest : -1;
        }
    }

    /**
     * This changes all the activeTimePeriodTimes (the time options) to be canonical strings
     *  (so duplicates in different time periods and even data sets point to same string).
     *  This is called by shared right after the GridDataSet is successfully created.
     */
    public void makeTimeOptionsCanonical() {
        int matched = 0;
        int notMatched = 0;
        for (int timePeriod = 0; timePeriod < activeTimePeriodTimes.size(); timePeriod++) {
            String sar[] = (String[])activeTimePeriodTimes.get(timePeriod);
            for (int i = 0; i < sar.length; i++) {
                String oldS = sar[i];
                String newS = String2.canonical(oldS);
                if (newS == oldS) {  //yes, test if they point to same object (not equals())
                    notMatched++;
                } else {
                    matched++;
                    sar[i] = newS;
                }
            }
        }
        String2.log(internalName + " makeTimeOptionsCanonical matched=" + matched + " notMatched=" + notMatched);
    }

}
