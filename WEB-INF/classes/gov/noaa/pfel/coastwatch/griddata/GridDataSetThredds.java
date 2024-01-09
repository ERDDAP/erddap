/* 
 * GridDataSetThredds Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.Attributes;
import com.cohort.array.FloatArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.TimePeriods;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Vector;

import javax.xml.xpath.XPath;   //requires java 1.5
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

// from netcdfAll-x.jar
import ucar.nc2.*;

/** The Java DAP classes.  */
import dods.dap.*;

/** 
 * This class represents a gridDataSet which gets data and metadata from 
 * THREDDS/Opendap.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-05-16
 */
public class GridDataSetThredds extends GridDataSet { 

    /** The active time period Opendaps related to an activeTimePeriodOption. */
    public Vector activeTimePeriodOpendaps = new Vector(); //set by the constructor

    public static boolean acceptDeflate = true;

    //the constructor sets
    public String directory;
    public String fileNameRegex;
    public String gridName;

    //the constructor also sets
    public long failTime = 0;
    public long newOpendapTime = 0;
    public long getIndexInfoTime = 0;
    public int opendapTimesCount = 0;


    /**
     * The constructor searches for available data and sets
     * activeTimePeriodOptions, activeTimePeriodTitles, activeTimePeriodTimes,
     * and activeTimePeriodFileNames.
     * This prints diagnostics to String2.log.
     *
     * @param fileNameUtility
     * @param internalName  TMOk490
     * @param baseUrl e.g., http://192.168.31.13:8081/thredds/Satellite/aggregsatMO/k490/
     * @param tPalette e.g., "Rainbow"
     * @param tPaletteScale if null, "Linear" will be used
     * @param tPaletteMin if null or NaN, it will be determined from 'actual_range' metadata
     * @param tPaletteMax if null or NaN, it will be determined from 'actual_range' metadata
     * @param tDaysTillDataAccessAllowed  e.g., -1 (for margin of safety) or 14 for seawifs.
     * @param tAnomalyDataSet  or "" if none
     * @param fgdc several lines of substitutions, 
     *    stored in one String with newline separators
     *    (null if unused)
     * @param flagDirectory  If flagDirectory is not null and this dataset needs to be
     *    reindexed (because the time values have changed), a file will
     *    be put in flagDirectory (by lowMakeGrid or another method in this class) 
     *    to signal the need for a new index.
     * @param tDefaultUnits 'S' for standard, 'A' for Alternate
     * @param tAltScaleFactor 1st step to get data from standard units to alt units
     * @param tAltOffset 2nd step to get data from standard units to alt units
     * @param tAltUnits "" if alt units not used
     * @param tAltMin the default paletteMin for the alt units
     * @param tAltMax the default paletteMax for the alt units
     * @throws Exception if trouble
     */
    public GridDataSetThredds(FileNameUtility fileNameUtility, String internalName, String baseUrl, 
        String tPalette, String tPaletteScale, String tPaletteMin, String tPaletteMax, 
        int tDaysTillDataAccessAllowed, String tAnomalyDataSet, String fgdc,
        String flagDirectory,
        String tDefaultUnits, double tAltScaleFactor, double tAltOffset,
        String tAltUnits, double tAltMin, double tAltMax) throws Exception {

        this.fileNameUtility = fileNameUtility;
        this.internalName    = internalName;
        this.directory       = baseUrl; 
        anomalyDataSet = tAnomalyDataSet == null? "" : tAnomalyDataSet;

        //this.fileNameRegex = fileNameRegex;
        if (verbose)
            String2.log("\n* GridDataSetThredds constructor (" + directory + ")");
        long time = System.currentTimeMillis();
        String errorInMethod = String2.ERROR + " in GridDataSetThredds.constructor for\n  " + baseUrl + ":  \n";

        //create vectors to hold info for available data
        Vector tActiveTimePeriodOptions     = new Vector();//holds the active timePeriodOptions for this dataset
        Vector tActiveTimePeriodTitles      = new Vector();//holds the active timePeriodTitles  for this dataset
        Vector tActiveTimePeriodOpendapUrls = new Vector();//holds the active timePeriodOpendapUrls for this dataset
        Vector tActiveTimePeriodNHours      = new Vector();//holds the active timePeriodNHours  for this dataset
        tActiveTimePeriodTitles.add(""); //always add a blank main title

        //make baseBaseUrl
        //from https://thredds1.pfeg.noaa.gov/thredds/Satellite/aggregsatMO/k490/
        //make https://thredds1.pfeg.noaa.gov
        int threddsPo = baseUrl.indexOf("/thredds/");
        Test.ensureTrue(threddsPo != -1, errorInMethod + "\"/thredds/\" not found in baseUrl.");
        String baseBaseUrl = baseUrl.substring(0, threddsPo);

/* 
top level catalog is at https://oceanwatch.pfeg.noaa.gov/thredds/catalog.xml
excerpt from https://oceanwatch.pfeg.noaa.gov/thredds/Satellite/aggregsatMO/k490/catalog.xml
to see opendap server e.g., https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MO/k490/1day.html
oceanwatch numerical ip from coastwatch computer is 192.168.31.13)

Most datasets have 2 levels: <dataset><dataset>
<?xml version="1.0" encoding="UTF-8"?>
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0" xmlns:xlink="https://www.w3.org/1999/xlink" name="Satellite Data Server" version="1.0.1">
  <service name="all" serviceType="Compound" base="">
    <service name="wcs" serviceType="WCS" base="/thredds/wcs/" suffix="?request=GetCapabilities&amp;version=1.0.0&amp;service=WCS" />
    <service name="ncdods" serviceType="OPENDAP" base="/thredds/dodsC/" />
  </service>
  <dataset name="Diffuse Attenuation coefficient at 490nm (Turbidity)">
    <dataset name="Single Scans" ID="satellite/MO/k490/hday" urlPath="satellite/MO/k490/hday">
      <serviceName>all</serviceName>
    </dataset>
    <dataset name="1-day" ID="satellite/MO/k490/1day" urlPath="satellite/MO/k490/1day">
      <serviceName>all</serviceName>
    </dataset>
    <dataset name="3-day" ID="satellite/MO/k490/3day" urlPath="satellite/MO/k490/3day">
      <serviceName>all</serviceName>
    </dataset>
    <dataset name="8-day" ID="satellite/MO/k490/8day" urlPath="satellite/MO/k490/8day">
      <serviceName>all</serviceName>
    </dataset>
    <dataset name="14-day" ID="satellite/MO/k490/14day" urlPath="satellite/MO/k490/14day">
      <serviceName>all</serviceName>
    </dataset>
  </dataset>
</catalog>

J1ugeo has just one level: <dataset>   *** So look for <dataset> with urlPath attribute
<?xml version="1.0" encoding="UTF-8"?>
<catalog xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0" xmlns:xlink="https://www.w3.org/1999/xlink" name="GLOBEC Data Server" version="1.0.1">
  <service name="all" serviceType="Compound" base="">
    <service name="wcs" serviceType="WCS" base="/thredds/wcs/" suffix="?request=GetCapabilities&amp;version=1.0.0&amp;service=WCS" />
    <service name="ncdods" serviceType="OPENDAP" base="/thredds/dodsC/" />
  </service>
  <dataset name="10-day zonal geostrophic current" ID="satellite/J1/ugeo/10day" urlPath="satellite/J1/ugeo/10day">
    <serviceName>all</serviceName>
  </dataset>
</catalog>
*/

        //open the thredds catalog xml file
        //String2.log("getUrlString for " + baseUrl + "catalog.xml");
        //String2.log(XML.encodeAsTerminal(SSR.getURLResponseStringUnchanged(baseUrl + "catalog.xml")));
        Document document = XML.parseXml(SSR.getBufferedUrlReader(baseUrl + "catalog.xml"), false);
        XPath xPath = XML.getXPath();

        //get the opendapServiceBase, e.g., "/thredds/dodsC/"
        //find a service tag with serviceType='OPENDAP' and a base attribute
        NodeList nodeList = XML.getNodeList(document, xPath, 
            "/catalog/service/service[@serviceType='OPENDAP' and @base]"); //an xPath query
        Test.ensureNotEqual(nodeList.getLength(), 0, errorInMethod + 
            "catalog.xml has no /catalog/service/service tag with serviceType='OPENDAP' and a base attribute.");
        String opendapServiceBase = XML.getAttribute(nodeList.item(0), "base");
        if (verbose) String2.log("  opendapServiceBase=" + opendapServiceBase);

        //get all the dataset tags (at whatever level) with urlPath attribute
        nodeList = XML.getNodeList(document, xPath, "//dataset[@urlPath]");  //an xPath query
        for (int i = 0; i < nodeList.getLength(); i++) {

            //get the urlPath
            String urlPath = XML.getAttribute(nodeList.item(i), "urlPath"); //"satellite/MO/k490/8day"
            if (verbose) String2.log("  urlPath=" + urlPath);

//***This relies on end of directory being one of TimePeriods.IN_FILE_NAMES, e.g., 8day
            int po = urlPath.lastIndexOf('/');
            if (po < 0)
                po = urlPath.lastIndexOf('\\');
            String timePeriodInFileName = urlPath.substring(po + 1);
            int whichTimePeriod = String2.indexOf(TimePeriods.IN_FILE_NAMES, timePeriodInFileName);
            if (whichTimePeriod < 0) {
                String2.log(errorInMethod + "unrecognized timePeriod for urlPath: " + urlPath);
                continue; //continue to next while() loop 
            } else {
                String2.log("  timePeriodInFileName=" + timePeriodInFileName);
            }
            String urlPathNoTimePeriod = urlPath.substring(0, po + 1);

            String dataSetUrl = "";
            long tFailTime = System.currentTimeMillis();
            try {
                //try to create the opendap object
                long tTime = System.currentTimeMillis();

                //need to convert 
                //  baseUrl "https://oceanwatch.pfeg.noaa.gov/thredds/Satellite/aggregsatMO/k490/" + "catalog.xml", 
                //  to      "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/" + "satellite/MO/k490/hday",
                //  just add  ".html" to make user-friendly link
                dataSetUrl = baseBaseUrl + opendapServiceBase + urlPath;  

                Opendap opendap = new Opendap(dataSetUrl, acceptDeflate,
                    flagDirectory); //throws Exception if trouble
                newOpendapTime += System.currentTimeMillis() - tTime;

                //get the DAS and DDS
                DConnect dConnect = new DConnect(dataSetUrl, acceptDeflate, 1, 1);
                DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
                DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);

                //get the grid
                tTime = System.currentTimeMillis();
                StringArray gridNames = opendap.getGridNames(dds);
                if (gridNames.size() == 0)
                    Test.error(errorInMethod + "no grids found.");
                if (gridNames.size() > 1)
                    String2.log(errorInMethod + gridNames.size() + " grids found: " + gridNames.toString());
                gridName = gridNames.get(0);
                
                opendap.getGridInfo(das, dds, gridName, "-1.0e34");
                if (opendap.gridTimeDimension < 0 || 
                    opendap.gridDepthDimension < 0 || 
                    opendap.gridLatDimension < 0 || 
                    opendap.gridLonDimension < 0)
                    throw new RuntimeException(errorInMethod + "dataset (" + dataSetUrl + ")\n" +
                        "  grid (" + gridName + ") wasn't found.\n" +
                        "  timeDim=" + opendap.gridTimeDimension + 
                        "  depthDim=" + opendap.gridDepthDimension + 
                        "  latDim=" + opendap.gridLatDimension + 
                        "  lonDim=" + opendap.gridLonDimension);

               //get numberOfObservations (do before getTimeOptions)
               //This is inactive because the information is stored as metadata in each file.
               //But thredds aggregates files and only shows metadata from the penultimate file.
               //So this information is no longer available.
                /*try {
                    opendap.numberOfObservations = 
                        opendap.getDoubleArrayFromDArray(
                            "?numberOfObservations.numberOfObservations");
                } catch (Exception e) {
                    //no numberOfObservations, but dataset is still valid
                    String2.log("For url=" + dataSetUrl + " " + timePeriodInFileName + 
                        ":\n  numberOfObservations wasn't found!");
                }*/
                
                //get the attributes 
                String2.log("  getAttributes?  globalAtt size=0?  =" + globalAttributes.size());
                if (globalAttributes.size() == 0) { 
                    //global
String2.log("trying dataSetUrl=" + dataSetUrl);
                    NetcdfFile netcdfFile = NcHelper.openOpendapAsNetcdfDataset(dataSetUrl);
                    try {
                        //FUTURE: THIS COULD NOW BE DONE VIA OPENDAPHELPER,
                        //but not time critical, since just done once per GridDataSetThredds
                        Group rootGroup = netcdfFile.getRootGroup();

                        NcHelper.getGroupAttributes(rootGroup, globalAttributes);
                        //if (verbose) String2.log("\n// global attributes:\n" +
                        //    globalAttributes.toNcString("\t\t:", " ;"));

                        //lon
                        String tName = opendap.gridDimensionNames[opendap.gridLonDimension];
                        Variable variable = netcdfFile.findVariable(tName);
                        Test.ensureNotNull(variable, errorInMethod + "Lon variable '" + tName + "' not found."); 
                        NcHelper.getVariableAttributes(variable, lonAttributes);
                        //if (verbose) String2.log(lonAttributes.toNcString("GridDataSetThredds attributes: " + tName + ":", " ;"));

                        //lat
                        tName = opendap.gridDimensionNames[opendap.gridLatDimension];
                        variable = netcdfFile.findVariable(tName);
                        Test.ensureNotNull(variable, errorInMethod + "Lat variable '" + tName + "' not found."); 
                        NcHelper.getVariableAttributes(variable, latAttributes);
                        //if (verbose) String2.log(latAttributes.toNcString("GridDataSetThredds attributes: " + tName + ":", " ;"));

                        //depth
                        tName = opendap.gridDimensionNames[opendap.gridDepthDimension];
                        variable = netcdfFile.findVariable(tName);
                        Test.ensureNotNull(variable, errorInMethod + "Depth variable '" + tName + "' not found."); 
                        NcHelper.getVariableAttributes(variable, depthAttributes);
                        //if (verbose) String2.log(latAttributes.toNcString("GridDataSetThredds attributes: " + tName + ":", " ;"));

                        //time
                        tName = opendap.gridDimensionNames[opendap.gridTimeDimension];
                        variable = netcdfFile.findVariable(tName);
                        Test.ensureNotNull(variable, errorInMethod + "Time variable '" + tName + "' not found."); 
                        NcHelper.getVariableAttributes(variable, timeAttributes);
                        //if (verbose) String2.log(latAttributes.toNcString("GridDataSetThredds attributes: " + tName + ":", " ;"));
                        //fix for old ERD THREDDS datasets; actual time adjustment is made in Opendap getTimeOptions
                        timeAttributes.set("long_name", "Centered Time");

                        //data 
                        tName = opendap.gridName;
                        variable = netcdfFile.findVariable(tName);
                        Test.ensureNotNull(variable, errorInMethod + "Data variable '" + tName + "' not found."); 
                        NcHelper.getVariableAttributes(variable, dataAttributes);
                        //if (verbose) String2.log(dataAttributes.toNcString("GridDataSetThredds attributes: " + tName + ":", " ;"));

                    } finally {
                        try {if (netcdfFile != null) netcdfFile.close(); } catch (Exception e9) {}
                    }


                    //populate the gridDataSet variables
                    String title = globalAttributes.getString("title").trim();
                    String2.log("internalName=" + internalName + " title=" + String2.annotatedString(title));

                    double dPaletteMin = String2.parseDouble(tPaletteMin);
                    double dPaletteMax = String2.parseDouble(tPaletteMax);
                    if (Double.isNaN(dPaletteMin) || Double.isNaN(dPaletteMax)) {
                        PrimitiveArray actualRange = dataAttributes.get("actual_range"); 
                        Test.ensureNotNull(actualRange, errorInMethod + "No 'actual_range' metadata.");
                        Test.ensureEqual(actualRange.size(), 2, errorInMethod + "'actual_range' length must be 2.");
                        double lowHigh[] = Math2.suggestLowHigh(
                            actualRange.getDouble(0), 
                            actualRange.getDouble(1));
                        dPaletteMin = lowHigh[0];
                        dPaletteMax = lowHigh[1];

                        //center range on 0?  palette will be set to blueWhiteRed below
                        if (dPaletteMin < 0 && dPaletteMax > 0 &&  //range spans 0
                            dPaletteMax/-dPaletteMin >= 0.5 && dPaletteMax/-dPaletteMin <= 2) { //magnitudes are similar
                            dPaletteMax = Math.max(-dPaletteMin, dPaletteMax);
                            dPaletteMin = -dPaletteMax;
                        }
                    } 

                    Test.ensureTrue(tDefaultUnits.equals("S") || tDefaultUnits.equals("A"), 
                        errorInMethod + "defaultUnits=" + tDefaultUnits + " must be 'S' or 'A'.");
                    defaultUnits        = tDefaultUnits.charAt(0);
                    altUdUnits          = tAltUnits; //i.e., "" if alt system not used
                    altScaleFactor      = tAltScaleFactor;
                    altOffset           = tAltOffset;
                    altPaletteMin       = String2.genEFormat6(tAltMin); 
                    altPaletteMax       = String2.genEFormat6(tAltMax); 
                    if (altUdUnits.length() > 0)
                        Test.ensureTrue(!Double.isNaN(altScaleFactor) && !Double.isNaN(altOffset) ||
                                        !Double.isNaN(tAltMin)        && !Double.isNaN(tAltMax),
                            errorInMethod + "altUdUnits=" + altUdUnits + 
                                ", but altScaleFactor=" + altScaleFactor +
                                ", altOffset=" + altOffset +
                                ", altPaletteMin=" + altPaletteMin +
                                ", altPaletteMax=" + altPaletteMax);
                    if (defaultUnits == 'A' && altUdUnits.length() == 0)
                        Test.error(errorInMethod + "defaultUnits='A', but altUdUnits is ''.");
                    boldTitle           = title;
                    contourLinesAt  = String2.genEFormat6(
                        Math2.suggestDivisions(dPaletteMax - dPaletteMin)[0]); //[0]=primary division distance
                    if (altUdUnits.length() > 0) 
                        altContourLinesAt  = String2.genEFormat6(
                            Math2.suggestDivisions(tAltMax - tAltMin)[0]); //[0]=primary division distance
                    else altContourLinesAt = "1";
                    courtesy            = globalAttributes.getString("contributor_name");
                    if (courtesy == null)
                        courtesy        = globalAttributes.getString("creator_name");
                    if (courtesy == null) courtesy = "";
                    if (fgdc != null) 
                        fgdcSubstitutions = String2.splitNoTrim(fgdc, '\n');
                    keywords            = globalAttributes.getString("keywords");
                    keywordsVocabulary  = globalAttributes.getString("keywords_vocabulary");
                    daysTillDataAccessAllowed = tDaysTillDataAccessAllowed;
                    option              = title + "*"; 
                    palette             = tPalette == null?
                        (Math2.almostEqual(5, dPaletteMax, -dPaletteMin)? "BlueWhiteRed" : 
                         dPaletteMin == 0? "WhiteRedBlack" : "Rainbow") :
                        tPalette; 
                    paletteMin          = String2.genEFormat6(dPaletteMin);
                    paletteMax          = String2.genEFormat6(dPaletteMax);
                    paletteScale        = tPaletteScale == null? "Linear" : tPaletteScale; 
                    references          = globalAttributes.getString("references");
                    summary             = globalAttributes.getString("summary");
                    if (summary != null) {
                        //remove units info in summary -- too hard to maintain
                        int po1 = summary.indexOf(" The units of the data are ");
                        if (po1 >= 0) {
                            int po2 = summary.indexOf(".", po1);
                            if (po2 > 0)
                                summary = summary.substring(0, po1) + summary.substring(po2 + 1);
                        } else {
                            po1 = summary.indexOf(" Units are ");
                            if (po1 >= 0) {
                                int po2 = summary.indexOf(".", po1);
                                if (po2 > 0)
                                    summary = summary.substring(0, po1) + summary.substring(po2 + 1);
                            }
                        }
                    }
                    udUnits = dataAttributes.getString("units"); 
                    if (udUnits == null) udUnits = "(unknown units)";
                    if (altUdUnits.length() > 0)
                        unitsOptions = new String[]{DataHelper.makeUdUnitsReadable(udUnits),
                            DataHelper.makeUdUnitsReadable(altUdUnits)}; 
                    else unitsOptions = new String[]{DataHelper.makeUdUnitsReadable(udUnits)}; 
                }

                //kludge to treat 25 and 33 hour datasets correctly
                //ERD's thredds stores them in "hday" directories
                //  so without a kludge, they are treated as hday datasets 
                //  (e.g., EndTime not adjusted to be centered)
                int nHours = TimePeriods.N_HOURS[whichTimePeriod];
                if (nHours == 0 && 
                    (internalName.endsWith("24h") || internalName.endsWith("25h"))) {
                    whichTimePeriod = TimePeriods._25HOUR_INDEX;
                } else if (nHours == 0 && internalName.endsWith("33h")) {
                    whichTimePeriod = TimePeriods._33HOUR_INDEX;
                }
                nHours = TimePeriods.N_HOURS[whichTimePeriod]; //recalculate it
                
                //getTimeOptions (generate times[] as ISO 8601 strings)
                opendap.getTimeOptions(
                    TimePeriods.PICK_FROM[whichTimePeriod].indexOf('h') < 0, //true = show times; false = date only
                    opendap.gridTimeFactorToGetSeconds, opendap.gridTimeBaseSeconds, 
                    nHours); 
                String times[] = opendap.timeOptions;
                getIndexInfoTime += System.currentTimeMillis() - tTime;

                //if this sub dataset has data, save it
                if (times != null && times.length > 0) {

                    //store the information
                    int nTimes = times.length;
                    opendapTimesCount += nTimes;
                    if (verbose)  
                        String2.log(nTimes + " times found in " + dataSetUrl);
                    if (reallyVerbose) {
                        for (int locali = 0; locali < Math.min(7, nTimes); locali++)
                            String2.log(times[locali]);
                        if (nTimes > 7)
                            String2.log("...");
                    }

                    //save the info
                    tActiveTimePeriodOptions.add(TimePeriods.OPTIONS[whichTimePeriod]);
                    tActiveTimePeriodTitles.add(TimePeriods.TITLES[whichTimePeriod]);
                    tActiveTimePeriodOpendapUrls.add(dataSetUrl);
                    tActiveTimePeriodNHours.add("" + nHours);
                    activeTimePeriodTimes.add(times);
                    activeTimePeriodOpendaps.add(opendap);
                }
                
            } catch (Exception e) {
                tFailTime = System.currentTimeMillis() - tFailTime;
                failTime += tFailTime;
                String2.log(MustBe.throwable(
                    Opendap.WAIT_THEN_TRY_AGAIN + 
                    "\n(GridDataSetThredds can't open Opendap " + baseUrl + "\n" +                        
                    " failTime=" + tFailTime + ")", e));
            }

        } 

        //convert vectors to string[]'s
        activeTimePeriodOptions     = String2.toStringArray(tActiveTimePeriodOptions.toArray());
        activeTimePeriodTitles      = String2.toStringArray(tActiveTimePeriodTitles.toArray());
        activeTimePeriodOpendapUrls = String2.toStringArray(tActiveTimePeriodOpendapUrls.toArray());
        activeTimePeriodNHours      = String2.toIntArray(tActiveTimePeriodNHours.toArray());

        //check validity
        checkValidity();
        if (verbose) String2.log(
            "  Options: "     + String2.toCSSVString(activeTimePeriodOptions) + "\n" +
            "  Titles: "      + String2.toCSSVString(activeTimePeriodTitles) + "\n" +
            "  NHours: "      + String2.toCSSVString(activeTimePeriodNHours) + "\n" +
            "  OpendapUrls: " + String2.toCSSVString(activeTimePeriodOpendapUrls) + "\n" +
            "  GridDataSetThredds constructor " + internalName + " done. TIME=" + 
                (System.currentTimeMillis() - time));
    }        

    /**
     * This makes the specified grid as best it can.
     * See the superclass' documentation.
     */
    public Grid makeGrid(String timePeriodValue, String timeValue,         
        double minX, double maxX, 
        double minY, double maxY,
        int desiredNWide, int desiredNHigh)  throws Exception {

        long time = System.currentTimeMillis();
        String msg = "GridDataSetThredds.makeGrid(timePeriod=" + timePeriodValue +
               " date=" + timeValue +
            "\n  minX=" + minX +
               " maxX=" + maxX +
               " minY=" + minY +
               " maxY=" + maxY +
               " nWide=" + desiredNWide +
               " nHigh=" + desiredNHigh + ")";
        if (verbose) String2.log("/* " + msg); 
        String errorInMethod = String2.ERROR + " in " + msg + ":\n";

        //get indexes
        int timePeriodIndex = String2.indexOf(activeTimePeriodOptions, timePeriodValue);
        Test.ensureNotEqual(timePeriodIndex, -1, 
            errorInMethod + "timePeriod not found: " + timePeriodValue +
            "\ntimePeriodOptions=" + String2.toCSSVString(activeTimePeriodOptions));
        String tActiveTimePeriodTimes[] = (String[])activeTimePeriodTimes.get(timePeriodIndex);
        int timeIndex = String2.indexOf(tActiveTimePeriodTimes, timeValue);
        Test.ensureNotEqual(timeIndex, -1, 
            errorInMethod + "time (" + timeValue + ") must be one of\n" + 
            String2.toCSSVString(tActiveTimePeriodTimes));

        //get the data
        Opendap opendap = (Opendap)activeTimePeriodOpendaps.get(timePeriodIndex); 
        Grid grid = opendap.makeGrid(
            ((String[])activeTimePeriodTimes.get(timePeriodIndex))[timeIndex],  
            minX, maxX, minY, maxY,
            desiredNWide, desiredNHigh);  

        if (verbose) String2.log("\\* GridDataSetThredds.makeGrid " + 
            internalName + " done. TIME=" + 
            (System.currentTimeMillis() - time) + "\n");

        return grid;

    }


    /**
     * Make a Table with time series data for the grid point nearest to x,y.
     * If x,y is outside the range of the grid, an empty table will be
     * returned.
     *
     * <p> This is a THREDDS-specific implementation. 
     *
     * @param newDir  the directory for the intermediate grd file (if any)
     * @param x the desired latitude
     * @param y the desired longitude
     * @param isoMinTime an ISO format date/time for the minimum ok time.
     *    The searches are done based on the activeTimePeriodTimes.
     *    So, for example, searches for 1 day files are based on just date (2006-06-16),
     *    not dateTtime (e.g., 2006-06-16T23:59:59). 
     *    If isoMinTime and isoMaxTime are between two adjacent available times,
     *    the datum for the single closest time is returned.
     * @param isoMaxTime an ISO format date/time for the maximum ok time
     * @param timePeriod one of the activeTimePeriodOptions 
     * @return a Table with 6 columns: lon, lat, depth (meters, positive=down, currently always 0), 
     *     centered time (seconds since 1970-01-01), id (internalName), data).
     *   The data will be unpacked, in the standard units.
     *   Numeric column types may vary.
     *   Rows with missing values are NOT removed.
     *   The metadata (e.g., actual_range) will be correct (as correct as I can make it). 
     *   If there are no matching time points, this returns a table with all the
     *   usual columns but 0 rows.
     * @throws Exception if trouble (e.g., invalid isoMinTime or isoMaxtime)
     */
    public Table getTimeSeries(String newDir, double x, double y,
        String isoMinTime, String isoMaxTime, String timePeriod) throws Exception {

        if (verbose) String2.log("GridDataSetThredds.getTimeSeries x=" + x + " y=" + y + 
            "\n  isoMinTime=" + isoMinTime + " isoMaxTime=" + isoMaxTime);
        String errorInMethod = String2.ERROR + " in GridDataSetThredds.getTimeSeries:  x=" + x + " y=" + y + 
            "\n  isoMinTime=" + isoMinTime + " isoMaxTime=" + isoMaxTime;
        long time = System.currentTimeMillis();

        //ensure times have space connector (since all options have space connector)
        isoMinTime = String2.replaceAll(isoMinTime, "T", " ");
        isoMaxTime = String2.replaceAll(isoMaxTime, "T", " ");
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

        //!!! If 'L'ocal dataset   (stored in .zipped .grd files),
        //  if timePeriodNHours == 0 or  %24 !=0 (for 25 and 33 hours), limit access to 3 months
        //  if timePeriodNHours >= 1 month, no limit
        //  else (nDays) limit to 1 year
        String dataAccessLimit = null;
        double originalMinSeconds = Calendar2.isoStringToEpochSeconds(isoMinTime); //throws Exception if trouble

        //thredds has no need for dataAccessLimit (like GridDataSet)
        if (verbose) String2.log("  origMinSeconds=" + originalMinSeconds + " isoMinTime=" + isoMinTime + 
            "\n  dataAccessLimit=" + dataAccessLimit + " timePeriodNHours=" + timePeriodNHours);

        //find the timePeriod
        int timePeriodIndex = String2.indexOf(activeTimePeriodOptions, timePeriod);
        Test.ensureNotEqual(timePeriodIndex, -1, 
            errorInMethod + "unrecognized timePeriod: " + timePeriod);
        String timePeriodInFileName = TimePeriods.getInFileName(timePeriod);

        //get the opendap object for this timePeriod
        Opendap opendap = (Opendap)activeTimePeriodOpendaps.get(timePeriodIndex); 

        //get the times for that timePeriod
        String[] activeTimes = (String[])activeTimePeriodTimes.get(timePeriodIndex);   

        //find the first and last time indexes (use activeTimes, since they have been adjusted to be centeredTime)
//should this (and in GridDataSet) be binaryFindClosest?
        int firstActiveTimeIndex = Calendar2.binaryFindFirstGE(activeTimes, isoMinTime);
        int lastActiveTimeIndex  = Calendar2.binaryFindLastLE( activeTimes, isoMaxTime);

        boolean hasData = true; 
        if (firstActiveTimeIndex == activeTimes.length ||
            lastActiveTimeIndex == -1) {
            String2.log("GridDataSetThredds.getTimeSeries no relevant times!\n" +
                " activeTimes[0]=" + activeTimes[0] + 
                " activeTimes[n-1]=" + activeTimes[activeTimes.length - 1]);
            hasData = false;
        }


        double lonValues[] = null;
        double latValues[] = null;
        double xAdjust = Double.NaN; //value needed to put x into opendap lon range 
        int firstTimeIndex = -1;  
        int lastTimeIndex  = -1;  
        if (hasData) {
            //range is between two adjacent times? return datum for single closest day
            if (firstActiveTimeIndex == lastActiveTimeIndex + 1) {
                firstActiveTimeIndex = Calendar2.binaryFindClosest(activeTimes, isoMinTime);
                lastActiveTimeIndex = firstActiveTimeIndex;
            }

            //convert activeTimeIndexes back to index in time dimension (since some times are skipped)
            firstTimeIndex = opendap.timeOptionsIndex[firstActiveTimeIndex];  
            lastTimeIndex  = opendap.timeOptionsIndex[lastActiveTimeIndex];  
            if (verbose) String2.log(
                "  firstActiveTime=" + activeTimes[firstActiveTimeIndex] + " >= isoMinTime=" + isoMinTime + " index=" + firstTimeIndex + "\n" +
                "  lastActiveTime="  + activeTimes[lastActiveTimeIndex]  + " <= isoMaxTime=" + isoMaxTime + " index=" + lastTimeIndex);

            //find the closest opendap lon, lat indexes
            lonValues = opendap.gridDimensionData[opendap.gridLonDimension];
            latValues = opendap.gridDimensionData[opendap.gridLatDimension];
            int nLonValues = lonValues.length;
            int nLatValues = latValues.length;
            if      (x       >= lonValues[0] && x       <= lonValues[nLonValues - 1]) xAdjust = 0;
            else if (x + 360 >= lonValues[0] && x + 360 <= lonValues[nLonValues - 1]) xAdjust = 360;
            else if (x - 360 >= lonValues[0] && x - 360 <= lonValues[nLonValues - 1]) xAdjust = -360;
            if (Double.isNaN(xAdjust)) {
                String2.log("  x=" + x + " is out of range: min=" + lonValues[0] + " max=" + lonValues[nLonValues - 1]);
                hasData = false;
            }
            if (hasData && (y < latValues[0] || y > latValues[nLatValues - 1])) {
                String2.log("  y=" + y + " is out of range: min=" + latValues[0] + " max=" + latValues[nLatValues - 1]);
                hasData = false;
            }
        }

        if (hasData) {
            int lonIndex = Math2.binaryFindClosest(lonValues, x + xAdjust);
            int latIndex = Math2.binaryFindClosest(latValues, y);
            double resultLon = lonValues[lonIndex] - xAdjust;
            double resultLat = latValues[latIndex];
            String2.log("  xAdjust=" + xAdjust + 
                " lonIndex=" + lonIndex + " latIndex=" + latIndex + 
                " resultLon=" + resultLon + " resultLat=" + resultLat);

            //prepare opendap query   
            int nDimensions = opendap.gridDimensionData.length;
            int minIndex[] = new int[nDimensions];
            int maxIndex[] = new int[nDimensions];
            int stride[]   = new int[nDimensions];
            Arrays.fill(minIndex, 0); //altitide dimension uses the defaults
            Arrays.fill(maxIndex, 0);
            Arrays.fill(stride,   1);
            minIndex[opendap.gridLonDimension] = lonIndex;
            maxIndex[opendap.gridLonDimension] = lonIndex;
            minIndex[opendap.gridLatDimension] = latIndex;
            maxIndex[opendap.gridLatDimension] = latIndex;
            minIndex[opendap.gridTimeDimension] = firstTimeIndex;
            maxIndex[opendap.gridTimeDimension] = lastTimeIndex;
            StringBuilder sb = new StringBuilder("?" + opendap.gridName);
            for (int index = 0; index < minIndex.length; index++) 
                sb.append("[" + minIndex[index] + ":" + stride[index] + ":" + maxIndex[index] + "]");
            String query = sb.toString();
            if (verbose) String2.log("  query=" + query);
            long getTime = System.currentTimeMillis();

            //get the data from opendap 
            PrimitiveArray pa[] = null;
            try {
                //throw new Exception("test opendap exception"); //normally this line is commented out
                pa = OpendapHelper.getPrimitiveArrays(new DConnect(opendap.url, opendap.acceptDeflate, 1, 1), 
                    query); //query already percentEncoded as needed //throws Exception if trouble
            } catch (Exception e) {
                Test.error(Opendap.WAIT_THEN_TRY_AGAIN + //this message encourages getting new Shared in CWBrowser.java
                    "\n(Opendap dataset not available:\n  " + query + 
                    "\n" + e + ")");
            }
            getTime = System.currentTimeMillis() - getTime;

            //verify return values are as expected
            PrimitiveArray dataPA = pa[0]; 
            PrimitiveArray timePA = pa[opendap.gridTimeDimension + 1]; //+1 since data array is [0]
            PrimitiveArray lonPA  = pa[opendap.gridLonDimension  + 1]; 
            PrimitiveArray latPA  = pa[opendap.gridLatDimension  + 1]; 
            int expectedSize = lastTimeIndex - firstTimeIndex + 1;
            Test.ensureEqual(dataPA.size(), expectedSize, "pa[0] != expectedSize.");
            Test.ensureEqual(timePA.size(), expectedSize, "pa[timeDimension] != expectedSize.");
            Test.ensureEqual(lonPA.size(), 1, "pa[lonDimension] != 1.");
            Test.ensureEqual(latPA.size(), 1, "pa[latDimension] != 1.");
            Test.ensureEqual(lonPA.getNiceDouble(0) - xAdjust, resultLon, "observedLon != expectedLon");
            Test.ensureEqual(latPA.getNiceDouble(0),           resultLat, "observedLat != expectedLat");
            //use the time values directly

            //put the data in the table
            //find the data for each relevant time point
            double lastTime = 0;
            double depth = 0;  //currently, all grid depths treated as 0 !!!!!
            float missingValue = String2.parseFloat(opendap.gridMissingValue); //round to float is needed
            for (int timeIndex = 0; timeIndex < timePA.size(); timeIndex++) {

                //get raw time
                double thisTime = timePA.getDouble(timeIndex);

                //need to adjust to centeredTime?
                if (opendap.timeLongName != null && opendap.timeLongName.equals("End Time")) {
                    GregorianCalendar gc = Calendar2.epochSecondsToGc(thisTime);
                    //fix old-style (pre-Dec 2006) nDay and 1 month end times  so 00:00
                    if (timePeriodNHours > 1 && timePeriodNHours % 24 == 0)
                        gc.add(Calendar2.SECOND, 1);
                    TimePeriods.endCalendarToCenteredTime(timePeriodNHours, gc, errorInMethod);
                    thisTime = Calendar2.gcToEpochSeconds(gc);
                }

                //if gap since data is too long, add mv row
                String thisIsoEndTime = Calendar2.epochSecondsToIsoStringT(thisTime);
                if (timeIndex > 0 && thisTime - lastTime > expectedGapSeconds) {
                    xColumn.addDouble(resultLon);
                    yColumn.addDouble(resultLat);
                    depthColumn.addDouble(depth);  
                    tColumn.add(thisTime - expectedGapSeconds); //crude but sufficient to cause line break
                    idColumn.addString(internalName);
                    dataColumn.addFloat(Float.NaN);
                }

                //if first timeIndex and there is a dataAccessLimit, add row to be a place holder 1 hour back
                if (dataAccessLimit != null) {
                    xColumn.addDouble(resultLon);
                    yColumn.addDouble(resultLat);
                    depthColumn.addDouble(depth); 
                    tColumn.add(thisTime - Calendar2.SECONDS_PER_HOUR); //really crude, but ok: 1 hour back (if there is only 1 real datum, sgtGraph can't handle <1hr on x axis
                    idColumn.addString(internalName);
                    dataColumn.addFloat(Float.NaN);
                    dataAccessLimit = null; //signal that it has been dealt with
                }

                //add the row for this timeIndex's datum
                xColumn.addDouble(resultLon);
                yColumn.addDouble(resultLat);
                depthColumn.addDouble(depth); 
                tColumn.add(thisTime); 
                idColumn.addString(internalName);
                float datum = dataPA.getFloat(timeIndex);
                dataColumn.add(datum == missingValue? Float.NaN : datum);
                lastTime = thisTime;
            }            
        }

        //add metadata
        //for params, if null, change to "" so existing values not removed
        results.setAttributes(0, 1, 2, 3, boldTitle, 
            "",  //data was Grid, now not,   Arrgggh. Just remove it.
            DataHelper.CW_CREATOR_EMAIL, //who is creating this file...
            DataHelper.CW_CREATOR_NAME,
            DataHelper.CW_CREATOR_URL,
            DataHelper.CW_PROJECT,
            FileNameUtility.makeAveragedGridTimeSeriesName(internalName, 
                'S', x, y, isoMinTime, isoMaxTime, timePeriod), //id
            keywordsVocabulary == null? "" : keywordsVocabulary, 
            keywords           == null? "" : keywords, 
            references         == null? "" : references, 
            summary            == null? "" : summary, 
            courtesy           == null? "" : courtesy,  //who is source of data
            "Centered Time" + 
                (TimePeriods.getNHours(timePeriod) > 0? " of " + timePeriod + " Composites" : ""));
        results.columnAttributes(0).remove("coordsys");
        results.columnAttributes(0).remove("point_spacing");
        results.columnAttributes(1).remove("coordsys");
        results.columnAttributes(1).remove("point_spacing");
        Attributes tDataAttributes = results.columnAttributes(5);
        tDataAttributes.set("long_name", boldTitle);
        tDataAttributes.set("units", udUnits);
        tDataAttributes.remove("_coordinateSystem");
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

        if (verbose) String2.log("  getTimeSeries " + internalName + 
            " done. nRows=" + results.nRows() + 
            " TIME=" + (System.currentTimeMillis() - time) + "\n");
        return results;

    }

    /* this is just for test purposes */
    protected Table getSuperTimeSeries(String newDir, double x, double y,
        String isoMinTime, String isoMaxTime, String timePeriod) throws Exception {
        return super.getTimeSeries(newDir, x, y, isoMinTime, isoMaxTime, timePeriod);
    }

   /**
    * This performs a quick test of a dataset (e.g. to see why it isn't loading in 
    * a CWBrowser).
    *
    * @param twoName e.g., GA
    * @param fourName e.g., ssta
    */
   public static void quickTest(String twoName, String fourName) throws Exception {
        FileNameUtility.verbose = true;
        FileNameUtility fnu = new FileNameUtility("gov.noaa.pfel.coastwatch.CWBrowser");

        String sixName = twoName + fourName;
        String internalName = "T" + sixName;
        String2.log("\n*** GridDataSetThredds.quickTest of " + internalName);
        GridDataSetThredds.verbose = true;
        Opendap.verbose = true;
        String dir = File2.getClassPath() + //with / separator and / at the end
            "gov/noaa/pfel/coastwatch/griddata/";
        GridDataSetThredds gridDataSet = new GridDataSetThredds(fnu, 
            internalName,
            //was "https://oceanwatch.pfeg.noaa.gov/thredds/Satellite/aggregsat" + 
            "https://thredds1.pfeg.noaa.gov/thredds/Satellite/aggregsat" +
                twoName + "/" + fourName + "/", 
            "BlueWhiteRed", "Linear", "-10", "10", -1, "", null, null,
            "S", 1, 0, "", 1, 1);
        Test.ensureNotEqual(gridDataSet.activeTimePeriodOptions.length, 0, 
            "activeTimePeriodOptions.length");
        String2.log("  activeTimePeriodOptions=" + 
            String2.toCSSVString(gridDataSet.activeTimePeriodOptions));
        String2.log("  GridDataSetThredds.quickTest finished successfully");
   }
}
