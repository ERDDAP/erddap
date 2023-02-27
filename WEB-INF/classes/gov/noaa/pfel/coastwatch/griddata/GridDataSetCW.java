/* 
 * GridDataSetCW Copyright 2005, NOAA.
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
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.OneOf;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.TimePeriods;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

/** 
 * This class represents GridDataSets where the matadata is available 
 * from DataSet.properties.
 * 
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-06-23
 */
public abstract class GridDataSetCW extends GridDataSet {  

    /**
     * The number of digits to the right of the decimal for data display
     * of alternate units.
     * Set by the constructor.
     */
    public int altDataFractionDigits = Integer.MAX_VALUE; 

    /**
     * The number of digits to the right of the decimal for data display.
     * Set by the constructor.
     */
    public int dataFractionDigits = Integer.MAX_VALUE;  

    /**
     * The number of digits to the right of the decimal for lat and lon display.
     * Set by the constructor.
     */
    public int latLonFractionDigits = Integer.MAX_VALUE;

    /** 
     * The satellite source of the data, e.g., NOAA GOES-10. 
     * Set by the constructor.   "" if not from a satellite.
     */
    public String satellite = null;
    
    /** 
     * The sensor on the satellite which is the source of the data, e.g., Imager,
     * or the instrument (if not from a satellite). 
     * Set by the constructor.
     */
    public String sensor = null;

    /**
     * The standard name for the variable.
     */
    public String standardName = null;
   

    /**
     * The sets fields from info in DataSet.properties; 
     * this is used by the Coastwatch subclasses:
     * GridDataSetCWLocal and GridDataSetCWOpendap.
     *
     * @param fileNameUtility
     * @param internalName
     * @throws Exception if trouble
     */
    public void dataSetPropertiesSetup(FileNameUtility fileNameUtility, 
          String internalName) throws Exception {

        //store the info
        this.fileNameUtility = fileNameUtility;
        this.internalName = internalName;
        String sixName = internalName.substring(1);
        ResourceBundle2 dataSetRB2 = fileNameUtility.dataSetRB2();
        String otherInfo[] = String2.split(dataSetRB2.getString(sixName + "Info",  null), '`');

        Test.ensureEqual(otherInfo.length, 20, "otherInfo for " + internalName + ".");
        
        altContourLinesAt   = otherInfo[16]; //a single value or a comma-separated list of values       
        altDataFractionDigits = String2.parseInt(otherInfo[18]);
        altPaletteMax       = otherInfo[15]; 
        altPaletteMin       = otherInfo[14]; 
        altOffset           = String2.parseDouble(otherInfo[12]);
        altScaleFactor      = String2.parseDouble(otherInfo[11]);
        altUdUnits          = otherInfo[13]; 
        boldTitle           = dataSetRB2.getString(sixName + "BoldTitle", null); 
        contourLinesAt      = otherInfo[7]; //a single value or a comma-separated list of values
        courtesy            = dataSetRB2.getString(sixName + "Courtesy", null); 
        dataFractionDigits  = String2.parseInt(otherInfo[9]);
        defaultUnits        = otherInfo[10].charAt(0);
        String fgdc         = dataSetRB2.getString(sixName + "FGDC", null);
        if (fgdc != null) {
            fgdcSubstitutions = String2.splitNoTrim(fgdc, '\n');
            //String2.log(internalName + " fgdcSubstitutions.length=" + fgdcSubstitutions.length + 
            //    "\n" + XNL.encodeAsTerminal(String2.toNewlineString(fgdc)));
        }
        keywords            = dataSetRB2.getString(sixName + "Keywords", null);  //null will be caught; it must be present for CW datasets
        keywordsVocabulary  = FileNameUtility.getKeywordsVocabulary();  
        latLonFractionDigits= String2.parseInt(otherInfo[8]);
        daysTillDataAccessAllowed = String2.parseInt(otherInfo[19]);
        option              = boldTitle + (internalName.charAt(0) == 'O'? "*" : "");  //was otherInfo[0] 
        palette             = otherInfo[1]; 
        paletteMax          = otherInfo[6]; 
        paletteMin          = otherInfo[5]; 
        paletteScale        = otherInfo[2]; 
        references          = dataSetRB2.getString(sixName + "References", null);
        satellite           = dataSetRB2.getString(sixName + "Satellite", null);
        sensor              = dataSetRB2.getString(sixName + "Sensor", null);
        standardName        = dataSetRB2.getString(sixName + "StandardName", null);
        if (fgdcSubstitutions != null) {
            String fgdcSearchFor = "<metadata><idinfo><descript><abstract>";
            for (int i = 0; i < fgdcSubstitutions.length; i++) 
               if (fgdcSubstitutions[i].startsWith(fgdcSearchFor))
                   summary = fgdcSubstitutions[i].substring(fgdcSearchFor.length());
        }
        udUnits             = otherInfo[4]; 

        if (altUdUnits.length() > 0) {
            unitsOptions = new String[]{
                DataHelper.makeUdUnitsReadable(udUnits), 
                DataHelper.makeUdUnitsReadable(altUdUnits)};
        } else {
            unitsOptions = new String[]{DataHelper.makeUdUnitsReadable(udUnits)};
        }

    }

    /** 
     * This is used by the constructor to ensure the fields have valid values.
     *
     * @throws Exception if not valid
     */
    public void checkValidity() {
        //super
        super.checkValidity();

        //things specific to GridDataSetCW
        String errorInMethod = String2.ERROR + " in GridDataSetCW.checkValidity for " + 
            internalName + ":\n ";
        Test.ensureNotEqual(latLonFractionDigits, Integer.MAX_VALUE,           errorInMethod + "'latLonFractionDigits' wasn't set.");
        if (altUdUnits.length() > 0) {
            Test.ensureNotEqual(altDataFractionDigits, Integer.MAX_VALUE,      errorInMethod + "'altDataFractionDigits' wasn't set.");
        }
        Test.ensureNotEqual(dataFractionDigits, Integer.MAX_VALUE,             errorInMethod + "'dataFractionDigits' wasn't set.");
        Test.ensureNotNull(keywords,                                           errorInMethod + "'keywords' wasn't set.");
        Test.ensureNotNull(keywordsVocabulary,                                 errorInMethod + "'keywordsVocabulary' wasn't set.");
        Test.ensureNotNull(references,                                         errorInMethod + "'references' wasn't set.");
        Test.ensureNotNull(satellite,                                          errorInMethod + "'satellite' wasn't set.");
        Test.ensureNotNull(sensor,                                             errorInMethod + "'sensor' wasn't set.");
        Test.ensureNotNull(standardName,                                       errorInMethod + "'standardName' wasn't set.");
    }

    /**
     * The string representation of this gridDataSet (for diagnostic purposes).
     * @return the string representation of this gridDataSet.
     */
    public String toString() {
        return super.toString() +
            "  GridDataSetCW specific attributes:" +
            "\n  altDataFractionDigits=" + altDataFractionDigits +
            "\n  dataFractionDigits=" + dataFractionDigits +
            "\n  latLonFractionDigits=" + latLonFractionDigits +
            "\n  satellite=" + satellite +
            "\n  sensor=" + sensor +
            "\n";
    }

    /**
     * This calls the super.getTimeSeries and then adds a few attributes.
     */
    public Table getTimeSeries(String newDir, double x, double y,
        String isoMinDate, String isoMaxDate, String timePeriod) throws Exception {

        Table results = super.getTimeSeries(newDir, x, y, isoMinDate, isoMaxDate, timePeriod);
        results.globalAttributes().set("source", FileNameUtility.getSource(satellite, sensor));

        results.columnAttributes(5).set("standard_name", standardName);
        return results;

    }

    /**
     * This sets globalAttributes, latAttributes, lonAttributes, and
     * dataAttributes so that the attributes have 
     * COARDS, CF, THREDDS ACDD, and CWHDF-compliant metadata attributes.
     * This does not call calculateStats; see grid.setStatsAttributes().
     * See MetaMetadata.txt for more information.
     *
     * @param grid
     * @param fileName A CWBrowser-style file name (so that fileNameUtility
     *    can generate the information.
     */
    public void setAttributes(Grid grid, String fileName) throws Exception {
//should this clear existing attributes?
       
        //let super.setAttributes do most of the work
        super.setAttributes(grid, fileName);

        Attributes gridGlobalAttributes = grid.globalAttributes();
        Attributes gridLatAttributes = grid.latAttributes();
        Attributes gridLonAttributes = grid.lonAttributes();
        Attributes gridDataAttributes = grid.dataAttributes();

        //assemble the global metadata attributes
        gridGlobalAttributes.set("Conventions",               FileNameUtility.getConventions());

        //super is timid about these. be more forceful...
        //gridGlobalAttributes.set("keywords",                  keywords);
        //gridGlobalAttributes.set("keywords_vocabulary",       keywordsVocabulary);
        //gridGlobalAttributes.set("history",                   fileNameUtility.getHistory(fileName));
        //gridGlobalAttributes.set("institution",               fileNameUtility.getCreatorName(fileName));
        //gridGlobalAttributes.set("project",                   FileNameUtility.getProject());
        //gridGlobalAttributes.set("processing_level",          FileNameUtility.getProcessingLevel());
        //gridGlobalAttributes.set("acknowledgement",           FileNameUtility.getAcknowledgement());
        //gridGlobalAttributes.set("geospatial_vertical_min",   0.0); //0.0 (not 0, which is int)
        //gridGlobalAttributes.set("geospatial_vertical_max",   0.0); //0.0 (not 0, which is int)
        //gridGlobalAttributes.set("geospatial_vertical_units", "m");
        //gridGlobalAttributes.set("geospatial_vertical_positive","up");
        ////gridGlobalAttributes.set("time_coverage_resolution", "P12H"));
        gridGlobalAttributes.set("standard_name_vocabulary",  FileNameUtility.getStandardNameVocabulary());
        //gridGlobalAttributes.set("license",                   FileNameUtility.getLicense());
        //gridGlobalAttributes.set("contributor_name",          fileNameUtility.getContributorName(fileName));
        //gridGlobalAttributes.set("contributor_role",          FileNameUtility.getContributorRole());
        gridGlobalAttributes.set("references",                fileNameUtility.getReferences(fileName));
        gridGlobalAttributes.set("source",                    fileNameUtility.getSource(fileName));

        //gridGlobalAttributes for HDF files using CoastWatch Metadata Specifications  
        //required unless noted otherwise
        gridGlobalAttributes.set("cwhdf_version",      "3.4");          //string
        if (satellite.length() > 0) {
            gridGlobalAttributes.set("satellite",      satellite); //string
            gridGlobalAttributes.set("sensor",         sensor); //string
        } else {
            gridGlobalAttributes.set("data_source",    sensor); //string
        }
 
        //CWHDF metadata attributes for Latitude
        gridLatAttributes.set("fraction_digits",       latLonFractionDigits); //int32
        gridLonAttributes.set("fraction_digits",       latLonFractionDigits); //int32

        //COARDS, CF, ACDD metadata attributes for data
        gridDataAttributes.set("standard_name",        standardName);
        //see grid.setStatsAttributes for
        //  gridDataAttributes.set("_FillValue",           mvAr); //must be same type as data
        //  gridDataAttributes.set("missing_value",        mvAr); //must be same type as data
        //  gridDataAttributes.set("numberOfObservations", nValidPoints);
        //  gridDataAttributes.set("actual_range",         rangeAr);

        //CWHDF metadata attributes for the data: varName
        boolean useAlternateUnits = FileNameUtility.getAlternateUnits(fileName);
        gridDataAttributes.set("fraction_digits",      useAlternateUnits? altDataFractionDigits : dataFractionDigits); //int32

    }


}
