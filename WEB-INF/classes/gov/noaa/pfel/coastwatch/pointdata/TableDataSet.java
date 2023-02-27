/* 
 * TableDataSet Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.ema.EmaColor;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.TimePeriods;
import gov.noaa.pfel.coastwatch.griddata.DataHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

/** 
 * This class represents a 4D Table-based dataset for CWBrowser.
 * <ul>
 * <li>It can generate a table (lon,lat,depth,time,id,[data variables]) 
 *   with a subset of the data.
 * <li>'Individuals' are the representation of an external table
 *   (perhaps a data file, a relational database, or an opendap url).
 * <li>'dataVariables' are the non-coordinate variables stored for
 *   each individual. It is best if all individuals have exactly the
 *   same dataVariables, but it isn't required.   
 * </ul>
 * 
 * The constructor searches for available data.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-03-06
 */
public abstract class TableDataSet implements Comparable { 

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 
    public static boolean reallyVerbose = false;

    /** 
     * The standard colors. 
     * These correspond to EmaColor.palette17 options.
     */
    public final static Color COLORS[] = {
        new Color(0xFFFFFF), new Color(0xCCCCCC), new Color(0x999999), new Color(0x666666), new Color(0x000000), 
        new Color(0xFF0000), new Color(0xFF9900), new Color(0xFFFF00), new Color(0x99FF00), new Color(0x00FF00), new Color(0x00FF99), 
        new Color(0x00FFFF), new Color(0x0099FF), new Color(0x0000FF), new Color(0x9900FF), new Color(0xFF00FF), new Color(0xFF99FF)};

    /**
     * The names for COLORS (which are alternatives to variable names 
     * when the user chooses a ColorBarVariable (e.g., RED).
     * These correspond to EmaColor.palette17 options.
     * Names from http://www.utoronto.ca/ian/books/html4ed/appf/color2.html.
     */
    public final static String[] COLOR_NAMES = {
        //"FFFFFF", "CCCCCC", "999999", "666666", "000000", 
        "WHITE", "LIGHT GRAY", "GRAY", "DARK GRAY", "BLACK", 
        //"FF0000", "FF9900", "FFFF00", "99FF00", "00FF00", "00FF99", 
        "RED", "ORANGE", "YELLOW", "CHARTREUSE", "GREEN", "SPRING GREEN", 
        //"00FFFF", "0099FF", "0000FF", "9900FF", "FF00FF", "FF99FF"};
        "CYAN", "SKY BLUE", "BLUE", "INDIGO", "MAGENTA", "VIOLET"};


    /** Set by the constructor. */
    protected String internalName;
    protected String datasetName;
    protected String courtesy;

    /** Set by the constructor.  */
    protected String[] individuals;     

    /** Set by the constructor: dataVariables information set by each constructor */
    protected String dataVariableNames[]; //the names in the file (and in results tables)
    protected ArrayList<Attributes> dataAttributes;   //returns Attributes         
    protected ArrayList<PAType>     dataElementType;  //returns a PAType, e.g., PAType.FLOAT */ 


    /** dataVariables information set by extractDataVariableInfo */
    protected String dataLongNames[];     //the longer names for the variables (for user selection, not necessarily long_name in metadata)
    protected String units[];           
    protected String missingValues[]; 
    protected String fillValues[];                      
    protected double scaleFactors[];
    protected double addOffsets[];

    /** Set by the constructor: coordinate variable information.  */
    protected Attributes globalAttributes, lonAttributes, latAttributes, 
        depthAttributes, timeAttributes, idAttributes;

    /** For simplistic daysTillDataAccessAllowed. */
    protected int daysTillDataAccessAllowed = -1;

    /** Set by ensureValid. */
    protected String[] longNames;
    protected String[] variableNames;
    protected String[] longNamesAndColors;

    /**
     * Constructors usually call this to make the standard idAttributes.
     */
    protected void makeStandardIDAttributes() {
        idAttributes = new Attributes();
        idAttributes.set("long_name", "Identifier"); 
        idAttributes.set("units", DataHelper.UNITLESS);
    }

    /**
     * Constructors usually call this after dataAttributes has been set to
     * extract units, missingValues, fillValues, scaleFactors, and addOffsets
     * information.
     * If any dataAttributes[v] is null, the default values are set.
     */
    protected void extractDataVariableInfo() {
        int ndv = dataVariableNames.length;
        dataLongNames = new String[ndv];           
        units = new String[ndv];           
        missingValues = new String[ndv]; 
        fillValues = new String[ndv];                      
        scaleFactors = new double[ndv];
        addOffsets = new double[ndv];
        for (int v = 0; v < ndv; v++) {
            Attributes attributes = dataAttributes.get(v);
            if (attributes == null) {
                units[v] = DataHelper.UNITLESS;
                fillValues[v] = null;
                missingValues[v] = null;
                scaleFactors[v] = 1;
                addOffsets[v] = 0;
                continue;
            }
            dataLongNames[v] = attributes.getString("long_name");
            if (dataLongNames[v] == null || dataLongNames[v].length() == 0) {
                dataLongNames[v] = attributes.getString("standard_name");
                if (dataLongNames[v] != null)
                    dataLongNames[v] = String2.replaceAll(dataLongNames[v], '_', ' ');
            }
            if (dataLongNames[v] == null || dataLongNames[v].length() == 0)
                dataLongNames[v] = dataVariableNames[v];
            String tUnits = attributes.getString("units");
            units[v] = tUnits == null? DataHelper.UNITLESS : tUnits;
            fillValues[v] = attributes.getString("_FillValue");
            missingValues[v] = attributes.getString("missing_value");
            double tScale = attributes.getNiceDouble("scale_factor");
            scaleFactors[v] = Double.isNaN(tScale)? 1 : tScale;
            double tAdd = attributes.getNiceDouble("add_offset");
            addOffsets[v] = Double.isNaN(tAdd)? 0 : tAdd;
            attributes.remove("scale_factor");
            attributes.remove("add_offset");
        }
    }

    /**
     * Constructors should call this when almost finished to ensure that 
     * required values are present, constrained values are valid,
     * and to make some predefined arrays.
     *
     * @throws Exception if invalid
     */
    public void ensureValid() throws Exception {
        String errorInMethod = String2.ERROR + " in TableDataSet.ensureValid for " + internalName + ":\n";

        Test.ensureNotNull(internalName,      errorInMethod + "internalName is null!");
        Test.ensureNotNull(datasetName,       errorInMethod + "datasetName is null!");
        Test.ensureNotNull(courtesy,          errorInMethod + "courtesy is null!");
        Test.ensureNotNull(individuals,       errorInMethod + "individuals is null!");
        Test.ensureNotNull(dataVariableNames, errorInMethod + "dataVariableNames is null!");
        Test.ensureNotNull(dataElementType,   errorInMethod + "dataElementType is null!");
        Test.ensureNotNull(dataAttributes,    errorInMethod + "dataAttributes is null!");
        Test.ensureNotNull(dataLongNames,     errorInMethod + "dataLongNames is null!");
        Test.ensureNotNull(units,             errorInMethod + "units is null!");
        Test.ensureNotNull(missingValues,     errorInMethod + "missingValues is null!");
        Test.ensureNotNull(fillValues,        errorInMethod + "fillValues is null!");
        Test.ensureNotNull(scaleFactors,      errorInMethod + "scaleFactors is null!");
        Test.ensureNotNull(addOffsets,        errorInMethod + "addOffsets is null!");
        Test.ensureNotNull(globalAttributes,  errorInMethod + "globalAttributes is null!");
        Test.ensureNotNull(lonAttributes,     errorInMethod + "lonAttributes is null!");
        Test.ensureNotNull(latAttributes,     errorInMethod + "latAttributes is null!");
        Test.ensureNotNull(depthAttributes,   errorInMethod + "depthAttributes is null!");
        Test.ensureNotNull(timeAttributes,    errorInMethod + "timeAttributes is null!");
        Test.ensureNotNull(idAttributes,      errorInMethod + "idAttributes is null!");

        int ni = individuals.length;
        Test.ensureTrue(ni > 0,               errorInMethod + "individuals.length is 0!");
        for (int i = 0; i < ni; i++) {
            Test.ensureNotNull(individuals[i],           errorInMethod + "individuals[" + i + "] is null!");
            Test.ensureTrue(individuals[i].length() > 0, errorInMethod + "individuals[" + i + "].length is 0!");
        }

        int ndv = dataVariableNames.length;
        Test.ensureTrue(ndv > 0,                     errorInMethod + "dataVariables.length is 0!");
        Test.ensureEqual(ndv, dataElementType.size(),errorInMethod + "dataVariables.length != dataElementType.size().");
        Test.ensureEqual(ndv, dataAttributes.size(), errorInMethod + "dataVariables.length != dataAttributes.size().");
        Test.ensureEqual(ndv, dataLongNames.length,  errorInMethod + "dataVariables.length != dataLongNames.length.");
        Test.ensureEqual(ndv, units.length,          errorInMethod + "dataVariables.length != units.length.");
        Test.ensureEqual(ndv, missingValues.length,  errorInMethod + "dataVariables.length != missingValues.length.");
        Test.ensureEqual(ndv, fillValues.length,     errorInMethod + "dataVariables.length != fillValues.length.");
        Test.ensureEqual(ndv, scaleFactors.length,   errorInMethod + "dataVariables.length != scaleFactors.length.");
        Test.ensureEqual(ndv, addOffsets.length,     errorInMethod + "dataVariables.length != addOffsets.length.");

        for (int i = 0; i < ndv; i++) {
            if (dataVariableNames[i] == null ||
                dataVariableNames[i].length() == 0) 
                                                Test.error(errorInMethod + "dataVariableNames[" + i + "] is null or ''.");
            if (dataElementType.get(i) == null) Test.error(errorInMethod + "dataElementType[" + i + "] is null.");
            if (dataAttributes.get(i) == null)  Test.error(errorInMethod + "dataAttributes[" + i + "] is null.");
            if (dataLongNames[i] == null)   Test.error(errorInMethod + "dataLongNames[" + i + "] is null.");            
            if (units[i] == null) units[i] = "";
        }

        //premake some commonly requested arrays
        variableNames = new String[4 + dataVariableNames.length];
        System.arraycopy(DataHelper.TABLE_VARIABLE_NAMES, 0, variableNames, 0, 4);
        System.arraycopy(dataVariableNames, 0, variableNames, 4, dataVariableNames.length);

        longNames = new String[4 + dataLongNames.length];
        System.arraycopy(DataHelper.TABLE_LONG_NAMES, 0, longNames, 0, 4);
        System.arraycopy(dataLongNames, 0, longNames, 4, dataLongNames.length);

        longNamesAndColors = new String[longNames.length + COLOR_NAMES.length];
        System.arraycopy(longNames, 0, longNamesAndColors, 0, longNames.length);
        System.arraycopy(COLOR_NAMES, 0, longNamesAndColors, longNames.length, COLOR_NAMES.length);
    } 

    /** 
     * This finds the corresponding individual number.
     *
     * @param individual an individual's name
     * @return the corresponding individual number 
     * @throws Exception if trouble
     */
    protected int whichIndividual(String individual) {
        //protected, not public, because data may be stored differently in future (hashmap?)
        int whichIndividual = String2.indexOf(individuals, individual);
        if (whichIndividual < 0) 
            Test.error(String2.ERROR + " in TableDataSet4DNc.whichIndividual: invalid individual (" + 
                individual + ").");
        return whichIndividual;
    }

    /** 
     * This finds the dataVariable number corresponding to a data variable name.
     *
     * @param dataVariableName a dataVariable's name
     * @return the corresponding dataVariable number 
     * @throws Exception if trouble
     */
    protected int whichDataVariableName(String dataVariableName) {
        //protected, not public, because data may be stored differently in future (hashmap?)
        int which = String2.indexOf(dataVariableNames, dataVariableName);
        if (which < 0) 
            Test.error(String2.ERROR + " in TableDataSet4DNc.whichDataVariableName():\n" +
                "invalid dataVariableName (" + dataVariableName + ").");
        return which;
    }

    /** 
     * This finds the dataVariable number corresponding to a data long name.
     *
     * @param dataLongName a dataVariable's name
     * @return the corresponding dataVariable number 
     * @throws Exception if trouble
     */
    protected int whichDataLongName(String dataLongName) {
        //protected, not public, because data may be stored differently in future (hashmap?)
        int which = String2.indexOf(dataLongNames, dataLongName);
        if (which < 0) 
            Test.error(String2.ERROR + " in TableDataSet4DNc.whichDataLongName():\n" +
                "invalid dataLongName (" + dataLongName + ").");
        return which;
    }


    /** 
     * This finds the variableName (coordinate or data variable) corresponds 
     * longName.
     *
     * @param longName a long name of a coordinate or data variable
     * @return the corresponding (coordinate or data) variable name 
     * @throws Exception if trouble
     */
    public String convertLongNameToVariableName(String longName) {
        //is coordinate name?
        int which = String2.indexOf(DataHelper.TABLE_LONG_NAMES, longName);
        if (which >= 0 && which <= 3) return DataHelper.TABLE_VARIABLE_NAMES[which];

        //is data name?
        which = String2.indexOf(dataLongNames, longName);
        if (which < 0) 
            Test.error(String2.ERROR + " in TableDataSet4DNc.convertLongNameToVariableName():\n" +
                "invalid longName (" + longName + ").");
        return dataVariableNames[which];
    }


    /** 
     * The internal name for the dataset in the form 4<2 letter><4 letter id>, ???
     * e.g., 4NBwspd. ???  (7 characters meshes with info/infog system and other data types)
     * It needs format limitations so created data files have names which
     * won't conflict with names from other types of DataSets.
     *
     * @return the internal name for the dataset (always length=7).
     */ 
    public String internalName() {
         return internalName;
     } 

    /** 
     * The main title for the dataset (e.g., Calcofi Biological).
     * Try really hard to keep less than 20 characters.
     *
     * @return the main title for the dataset (e.g., Calcofi Biological) (always length &gt; 0).
     */
    public String datasetName() {
        return datasetName;
    }

    /** 
     * The courtesy line for the legend, e.g., NOAA NESDIS OSDPD. 
     * Try really hard to keep less than 50 characters.
     * Set by the constructor ("" if unused).
     * From standard file: dataset's "acknowledgement" (ACDD) or
     *   "creator_name" (ACDD) or "project" (ACDD).
     *
     * @return the courtesy line for the legend, e.g., NOAA NESDIS OSDPD.
     *    May be "".
     */
    public String courtesy() {
        return courtesy;
    }

    /** 
     * The names of the individuals (e.g., {"QZ5983", ...}). 
     * An individual usually corresponds to one data file or source url,
     * which has data that can be represented by a table
     * (lon,lat,depth,time,id,[data variables]).
     *
     * @return the names of the individuals in a group (e.g., {"QZ5983", ...}). 
     *   This is the internal list. So don't change it.
     * @throws Exception if group is invalid
     */ 
    public String[] individuals() { 
        return individuals;
    }

    /** 
     * The names of the data variables (e.g., Salinity) (no coordinate variables or color names).
     * All are numeric variables.
     *
     * @return the names of the data variables (e.g., Salinity) for a given individual.
     *   This is the internal list. So don't change it.
     * @throws Exception if trouble
     */
    public String[] dataVariableNames() {
        return dataVariableNames;
    }

    /** 
     * The long names of the data variables (e.g., Salinity) (no coordinate variables or color names).
     * All are numeric variables.
     *
     * @return the long names of the data variables (e.g., Salinity) for a given individual.
     *   This is the internal list. So don't change it.
     * @throws Exception if trouble
     */
    public String[] dataLongNames() {
        return dataLongNames;
    }


    /** 
     * The in file names of the coordinate variables (e.g., LON, LAT, DEPTH, TIME)
     * and the data variables (e.g., SAL) for a given individual,
     * but not the colors.
     * All are numeric variables.
     *
     * @return the in file variable names of the coordinate variables 
     *   (e.g., LON, LAT, DEPTH, TIME)
     *   and the data variables (e.g., SAL),
     *   but not the colors.
     *   This is an internal array, so don't change it.
     * @throws Exception if trouble
     */
    public String[] variableNames() {
        return variableNames;
    }

    /** 
     * The long names of the coordinate variables 
     * (e.g., Longitude, Latitude, Depth, Time)
     * and the data variables (e.g., Salinity),
     * but not the colors.
     * All are numeric variables.
     *
     * @return the long names of the coordinate variables 
     *   (Longitude, Latitude, Depth, Time)
     *   and the data variables (e.g., Salinity).
     *   This is an internal array, so don't change it.
     * @throws Exception if trouble
     */
    public String[] longNames() {
        return longNames;
    }

    /** 
     * The names of the coordinate variable names (e.g., Longitude, Latitude, Depth, Time),
     * the data variable names (e.g., Salinity),
     * and the colorNames.
     *
     * @return the names of the coordinate variables (Longitude, Latitude, Depth, Time)
     *   and the data variables (e.g., Salinity) for a given individual.
     *   This is an internal array, so don't change it.
     * @throws Exception if trouble
     */
    public String[] longNamesAndColors() {
        return longNamesAndColors;
    };

    /**
     * The color names.
     * @return the color names
     */
    public String[] colorNames() {return COLOR_NAMES;}

    /**
     * The colors.
     * @return the colors
     */
    public Color[] colors() {return COLORS;}

    /** 
     * The first time for which data is available. 
     *
     * @param individual must be a value from individuals(group)
     * @return the first time for which data is available. 
     * @throws Exception if group or individual is invalid
     */
//try to do without this
//public abstract GregorianCalendar firstTime(String individual);

    /** 
     * The last time for which data is available. 
     *
     * @return the last time for which data is available. 
     * @throws Exception if individual is invalid
     */
//try to do without this
//    public abstract GregorianCalendar lastTime(String individual); 

    /** 
     * The value of daysTillDataAccessAllowed, e.g., 0 or 14.
     * -1 is common (to avoid roundoff trouble).
     *
     * @param individual must be a value from individuals
     * @return the value of daysTillDataAccessAllowed, e.g., 0 or 14.
     * @throws Exception if group or individual is invalid
     */
    public int daysTillDataAccessAllowed(String individual) {
        return daysTillDataAccessAllowed;
    }

    /**
     * The units corresponding to one of longNames() or variableNames().
     * For coordinate variables, these are UD units.
     * For data variables, these are straight from the data source.
     * 
     * @param which must be 0 ... longNames().length-1
     * @return the units for a variable (may be DataHelper.UNITLESS, won't be "" or null).
     * @throws Exception if 'which' is invalid
     */
    public String units(int which) {
        //is it one of first 4 DataHelper.TABLE_VARIABLE_NAMEs?
        if (which >= 0 && which <= 3)
            return DataHelper.TABLE_UNITS[which];

        //it must be a dataVariable
        return units[which - 4];
    }

    /**
     * The formatted units corresponding to one of longNames() or variableNames().
     * 
     * @param which must be 0 ... longNames().length-1
     * @return the formatted units string for a variable:
     *     "([units]) " or "".
     * @throws Exception if 'which' is invalid
     */
    public String formattedUnits(int which) {
        if (which < 0) //most likely error
            Test.error(String2.ERROR + " in TableDataSet.formattedUnits: which=" + which + " is less than 0.");
        String tUnits = units(which);
        if (tUnits.length() == 0 || tUnits.equals(DataHelper.UNITLESS))
            return "";
        return tUnits = "(" + tUnits + ") ";
    }


    /**
     * This prints the most important information about this class to a string.
     *
     * @return a string describing the dataset.
     */
    public String toString() {
        return "TableDataSet " + internalName + " datasetName=" + datasetName + 
            "\n  individuals[0]=" + individuals[0] +
            "\n  dataVariableNames=" + String2.toCSSVString(dataVariableNames); 
    }

    /**
     * This is used by makeSubset to make an empty table with the crude attributes.
     * This throws Exception if trouble
     *
     * @param desiredDataVariableNames the names of the desired dataVariables
     */
    protected Table makeEmptyTable(String desiredDataVariableNames[]) {
        Table table = new Table();
        globalAttributes.copyTo(table.globalAttributes());
        table.addColumn(0, DataHelper.TABLE_VARIABLE_NAMES[0], new DoubleArray(), (Attributes)lonAttributes.clone());
        table.addColumn(1, DataHelper.TABLE_VARIABLE_NAMES[1], new DoubleArray(), (Attributes)latAttributes.clone());
        table.addColumn(2, DataHelper.TABLE_VARIABLE_NAMES[2], new DoubleArray(), (Attributes)depthAttributes.clone());
        table.addColumn(3, DataHelper.TABLE_VARIABLE_NAMES[3], new DoubleArray(), (Attributes)timeAttributes.clone());
        table.addColumn(4, DataHelper.TABLE_VARIABLE_NAMES[4], new StringArray(), (Attributes)idAttributes.clone());
        for (int v = 0; v < desiredDataVariableNames.length; v++) {
            int whichDV = whichDataVariableName(desiredDataVariableNames[v]);
            table.addColumn(5 + v, dataVariableNames[whichDV], 
                PrimitiveArray.factory(dataElementType.get(whichDV), 8, false), 
                (Attributes)(dataAttributes.get(whichDV).clone()));
        }
        return table;
    }

    /** 
     * This is used by makeSubset to clean up the data crudely added to a makeSubset table:
     * converting missingValues and fillValues to NaNs, and scaling and adding offsets.
     *
     * @param table the makeSubset table with the raw data,
     *    with the desiredDataVariables starting in column 5.
     * @param desiredDataVariableNames must be a subset of dataVariableNames().
     */
    protected void cleanUpDataVariablesData(Table table, String desiredDataVariableNames[]) {

        for (int v = 0; v < desiredDataVariableNames.length; v++) {
            PrimitiveArray column = table.getColumn(5 + v);
            int whichDV = whichDataVariableName(desiredDataVariableNames[v]);

            //convert missingValues and fillValues
            column.switchFromTo(missingValues[whichDV], "");
            if (missingValues[whichDV] != null &&
                fillValues[whichDV] != null &&
                !missingValues[whichDV].equals(fillValues[whichDV]))
                column.switchFromTo(fillValues[whichDV], "");

            //scale and addOffset
            column.scaleAddOffset(scaleFactors[whichDV], addOffsets[whichDV]);
        }
    }

    /**
     * This is used by makeSubset to clean up the attributes in a populated table 
     * (e.g., from makeEmptyTable, then adding rows of data).
     */
    public void setAttributes(Table table) {

        //set Attributes    ('null' says make no changes  (don't use ""))
        table.setAttributes(0, 1, 2, 3, datasetName, 
            null, //cdmDataType,   
            DataHelper.CW_CREATOR_EMAIL, //who is creating this file...
            DataHelper.CW_CREATOR_NAME,
            DataHelper.CW_CREATOR_URL,
            DataHelper.CW_PROJECT,
            null, //id,                    !!!!!?????
            null, //keywordsVocabulary,
            null, //keywords, 
            null, //references, 
            null, //summary, 
            courtesy, //who is source of data...
            "Time");

        table.columnAttributes(3).remove("point_spacing");

        //MBARI sets this. I don't know where else to remove it.
        for (int v = 5; v < table.nColumns(); v++)
            table.columnAttributes(v).remove("_coordinateSystem"); 
    }    

    /**
     * Make a Table with a specific subset of the data.
     *
     * @param isoMinTime an ISO format date/time for the minimum ok time,
     *    or use null or "" to indicate no limit.
     *    This rounds to the closest time point in the file (but not if it is
     *    an hour or more away).
     * @param isoMaxTime an ISO format date/time for the maximum ok time,
     *    or use null or "" to indicate no limit.
     *    This rounds to the closest time point in the file (but not if it is
     *    an hour or more away).
     * @param desiredIndividuals must be a subset of individuals
     * @param desiredDataVariables must be a subset of dataVariables().
     *    This may be length=0.
     *    If a dataVariableName is a valid dataVariableName, but isn't 
     *    available for this individual, a column of NaN's will be created.
     * @return a Table with 6 or more columns: 
     *    <br>0) "LON" (units=degrees_east, with values made relevant to the desired minX maxX), 
     *    <br>1) "LAT" (units=degrees_north), 
     *    <br>2) "DEPTH" (units=meters, positive=down), 
     *    <br>3) "TIME" (units=seconds since 1970-01-01T00:00:00Z), 
     *    <br>4) "ID" (String data), 
     *    <br>5) a column for each dataVariable with data (unpacked, in same units as data source).
     *          The columns will be in the same order as listed in the 
     *          desiredDataVariableNames parameter.
     *   <br>LON, LAT, DEPTH and TIME will be DoubleArrays; ID will be a StringArray; 
     *     the others are numeric PrimitiveArrays (not necessarily DoubleArray).  
     *   <br>Rows with missing values are NOT removed.
     *   <br>The metadata (e.g., actual_range) will be correct (as correct as I can make it). 
     *   <br>The table will have the proper columns but may have 0 rows.
     * @throws Exception if trouble (e.g., ill-formed isoMinT, or minX > maxX,
     *    or individual or a dataVariable is invalid)
     */
    public abstract Table makeSubset(String isoMinTime, String isoMaxTime, 
        String desiredIndividuals[], 
        String[] desiredDataVariables) throws Exception;


    /**
     * Make a Table with a specific subset of the data.
     *
     * @param minX the minimum acceptable longitude (degrees_east, may be  -180 to 180 or 0 to 360),
     *    or use Double.NaN to indicate no limit.
     * @param maxX the maximum acceptable longitude (degrees_east, may be  -180 to 180 or 0 to 360),
     *    or use Double.NaN to indicate no limit.
     * @param minY the minimum acceptable latitude (degrees_north),
     *    or use Double.NaN to indicate no limit.
     * @param maxY the maximum acceptable latitude (degrees_north),
     *    or use Double.NaN to indicate no limit.
     * @param minZ the minimum acceptable altitude (meters, down is positive),
     *    or use Double.NaN to indicate no limit.
     * @param maxZ the maximum acceptable altitude (meters, down is positive),
     *    or use Double.NaN to indicate no limit.
     * @param isoMinT an ISO format date/time for the minimum ok time,
     *    or use null to indicate no limit.
     *    isoMinT and isoMaxT are rounded to be a multiple of the frequency 
     *    of the data's collection.  For example, if the data is hourly, 
     *    they are rounded to the nearest hour.
     * @param isoMaxT an ISO format date/time for the maximum ok time,
     *    or use null to indicate no limit.
     * @param individual must be a value from individuals
     * @param dataVariables must be a subset of dataVariables(individual)
     * @return a Table with 6 or more columns: 
     *    <br>0) "LON" (units=degrees_east, with values made relevant to the desired minX maxX), 
     *    <br>1) "LAT" (units=degrees_north), 
     *    <br>2) "DEPTH" (units=meters, positive=down), 
     *    <br>3) "TIME" (units=seconds since 1970-01-01T00:00:00Z), 
     *    <br>4) "ID" (String data), 
     *    <br>5) a column for each dataVariable with data (unpacked, in standard units).
     *          The columns will be in the same order as listed in the dataVariables parameter.
     *   <br>LON, LAT, DEPTH and TIME will be DoubleArrays; ID will be a StringArray; 
     *     the others are numeric PrimitiveArrays (not necessarily DoubleArray).  
     *   <br>Rows with missing values are NOT removed.
     *   <br>The metadata (e.g., actual_range) will be correct (as correct as I can make it). 
     *   <br>The table will have the proper columns but may have 0 rows.
     * @throws Exception if trouble (e.g., ill-formed isoMinT, or minX > maxX,
     *    or individual or a dataVariable is invalid)
     */
/*    public abstract Table makeSubset(double minX, double maxX,
        double minY, double maxY, double minZ, double maxZ,
        String isoMinT, String isoMaxT, 
        String individual, String[] dataVariables) throws Exception;

*/

    /**
     * This calculates the average data value returned by makeSubset.
     */
/*    public double calculateAverage(double minX, double maxX,
        double minY, double maxY, double minZ, double maxZ,
        String isoMinT, String isoMaxT) throws Exception {

        Table table = makeSubset(minX, maxX, minY, maxY, minZ, maxZ,
            isoMinT, isoMaxT);
        PrimitiveArray pa = table.getColumn(5);
        double stats[] = pa.calculateStats();
        double n = stats[PrimitiveArray.STATS_N];
        double average = n == 0? Double.NaN : stats[PrimitiveArray.STATS_SUM]/n;
        String2.log("TableDataSet.calculateAverage" +
            " minT=" + Calendar2.epochSecondsToIsoStringTZ(table.getColumn(3).getDouble(0)) +
            " maxT=" + Calendar2.epochSecondsToIsoStringTZ(table.getColumn(3).getDouble(table.nRows() - 1)) +
            "\n  average=" + average);
        return average;
    }
*/
    /**
     * This makes a table with the requested time period averages (e.g.,
     * daily 8-day averages centered on Jan 1, 1999 to Jan 7, 1999.
     * An average is made for each unique x,y,z,stationID combination in the 
     * relevant raw data.
     *
     * @param minX the minimum acceptable longitude (degrees_east, may be  -180 to 180 or 0 to 360)
     * @param maxX the maximum acceptable longitude (degrees_east, may be  -180 to 180 or 0 to 360)
     * @param minY the minimum acceptable latitude (degrees_north)
     * @param maxY the maximum acceptable latitude (degrees_north)
     * @param minZ the minimum acceptable altitude (meters, up is positive)
     * @param maxZ the maximum acceptable altitude (meters, up is positive)
     * @param isoMinT an ISO format date/time for the first average's centered date/time.
     * @param isoMaxT an ISO format date/time for the last average's centered date/time.
     * @param timePeriod one of the TimePeriods.timePeriodOptions (with a max length of "1 month").
     *    For timePeriod="1 observation", this method returns raw data.
     *    For nDay timePeriods, this method returns daily timePeriod-long averages.
     *    For timePeriod="1 month", this returns monthly averages of the data.
     * @param individual must be a value from individuals
     * @param dataVariables must be a subset of dataVariables(individual)
     * @return a Table with 6 columns: 
     *    <br>1) "LON" (units=degrees_east, with values made relevant to the desired minX maxX), 
     *    <br>2) "LAT" (units=degrees_north), 
     *    <br>3) "DEPTH" (units=meters, positive=down), 
     *    <br>4) "TIME" (units=seconds since 1970-01-01T00:00:00Z), 
     *    <br>5) "ID" (String data), 
     *    <br>6) inFileVarName with data (unpacked, in standard units).
     *   <br>Id will be a StringArray; the other are numeric PrimitiveArrays 
     *   (not necessarily DoubleArray).
     *   <br>Rows with missing values are NOT removed.
     *   <br>The metadata (e.g., actual_range) will be correct (as correct as I can make it). 
     *   <br>The table will have the proper columns but may have 0 rows.
     * @throws Exception (e.g., if invalid timePeriod, isoMinT, or isoMaxT)
     */
/*    public Table makeAveragedTimeSeries(double minX, double maxX,
        double minY, double maxY, double minZ, double maxZ,
        String isoMinT, String isoMaxT, String timePeriod) throws Exception {

        String info = "//**** TableDataSet.makeAveragedTimeSeries " + boldTitle + 
            "\n  mATS timePeriod=" + timePeriod + " isoMinT=" + isoMinT + " isoMaxT=" + isoMaxT;
        if (verbose) String2.log(info);
        long time = System.currentTimeMillis();

        //validate input
        //makeSubset (below) does validation of all of input data except timePeriod

        //get timePeriodNHours
        int timePeriodNHours = TimePeriods.getNHours(timePeriod); //throws Exception if not found
        int timePeriodNSeconds = timePeriodNHours * Calendar2.SECONDS_PER_HOUR;
        int oneMonthsHours = TimePeriods.getNHours("1 month");
        Test.ensureTrue(timePeriodNHours <= oneMonthsHours, 
            String2.ERROR + " in " + info + ":\n timePeriod (" + timePeriod + ") is longer than 1 month.");
        boolean timePeriodIs1Month = timePeriodNHours == oneMonthsHours;

        //calculate the clean  isoMinT and maxT
        isoMinT = TimePeriods.getCleanCenteredTime(timePeriod, isoMinT);
        isoMaxT = TimePeriods.getCleanCenteredTime(timePeriod, isoMaxT);

        //calculate the minT and maxT (back 1 second) for getting the raw data   
        double rawMinT, rawMaxT;  //the range of data needed
        if (timePeriodIs1Month) {
            //convert start to start of min month
            GregorianCalendar gc = Calendar2.parseISODateTimeZulu(isoMinT); //throws Exception if trouble
            gc.set(Calendar.MILLISECOND, 0);
            gc.set(Calendar.SECOND, 0);
            gc.set(Calendar.MINUTE, 0);
            gc.set(Calendar.HOUR_OF_DAY, 0);
            gc.set(Calendar.DATE, 1);
            rawMinT = Calendar2.gcToEpochSeconds(gc);

            //convert end time to just before end of max month
            gc = Calendar2.parseISODateTimeZulu(isoMaxT); //throws Exception if trouble
            gc.set(Calendar.MILLISECOND, 0);
            gc.set(Calendar.SECOND, 0);
            gc.set(Calendar.MINUTE, 0);
            gc.set(Calendar.HOUR_OF_DAY, 0);
            gc.set(Calendar.DATE, 1);
            gc.add(Calendar.MONTH, 1);  //beginning of next month
            gc.add(Calendar.SECOND, -1);  //back 1 second
            rawMaxT = Calendar2.gcToEpochSeconds(gc);

        } else if (timePeriodNHours == 0) {
            //for one observation, no change to minT and maxT
            rawMinT = Calendar2.isoStringToEpochSeconds(isoMinT); //isoMinT cleaned above
            rawMaxT = Calendar2.isoStringToEpochSeconds(isoMaxT); //isoMaxT cleaned above

        } else { 
            //25hour, 33hour, and nDays  -- 1/2 time back, 1/2 time forward
            rawMinT = Calendar2.isoStringToEpochSeconds(isoMinT) - timePeriodNSeconds / 2;     //isoMinT cleaned above
            rawMaxT = Calendar2.isoStringToEpochSeconds(isoMaxT) + timePeriodNSeconds / 2 - 1; //isoMaxT cleaned above
        }
        double minT = Calendar2.isoStringToEpochSeconds(isoMinT); //isoMinT cleaned above
        double maxT = Calendar2.isoStringToEpochSeconds(isoMaxT); //isoMaxT cleaned above
        if (reallyVerbose) String2.log(
            "  mATS clean minT=" + isoMinT + " maxT=" + isoMaxT + 
            " rawMinT=" + Calendar2.epochSecondsToIsoStringTZ(rawMinT) +
            " rawMaxT=" + Calendar2.epochSecondsToIsoStringTZ(rawMaxT)); 

        //get the raw data
        Table rawTable = makeSubset(minX, maxX,
            minY, maxY, minZ, maxZ, 
            Calendar2.epochSecondsToIsoStringT(rawMinT), 
            Calendar2.epochSecondsToIsoStringT(rawMaxT));
        //!!!Note that the rawTable may have some data values beyond the end of the top time period
        //  because makeSubset rounds the times to find the boundaries. 
        //  But these extra data points are ignored by the strictly defined
        //  timePeriods.
        //String2.log("rawTable=" + rawTable);
        int rawNRows = rawTable.nRows();
        PrimitiveArray xPA    = rawTable.getColumn(0);
        PrimitiveArray yPA    = rawTable.getColumn(1);
        PrimitiveArray zPA    = rawTable.getColumn(2);
        PrimitiveArray timePA = rawTable.getColumn(3);
        PrimitiveArray idPA   = rawTable.getColumn(4);
        PrimitiveArray dataPA = rawTable.getColumn(5);

        //String2.log("TableDataSet rawTable=" + rawTable);
        //String2.log("  after makeSubset, rawTable data stats: " + dataPA.statsString());

        //for timePeriodNHours == 0, just return the raw data  (no averaging)
        if (timePeriodNHours == 0) {
            if (verbose) String2.log("\\\\**** TableDataSet.makeAveragedTimeSeries done. nRows=" + 
                rawTable.nRows() + " TIME=" + (System.currentTimeMillis() - time) + "ms");
            return rawTable;
        }

        //make the resultTable  (like rawTable, but no data)
        Table resultTable = new Table();
        PrimitiveArray xResultPA    = PrimitiveArray.factory(xPA.elementType(), 4, false);
        PrimitiveArray yResultPA    = PrimitiveArray.factory(yPA.elementType(), 4, false);
        PrimitiveArray zResultPA    = PrimitiveArray.factory(zPA.elementType(), 4, false);
        PrimitiveArray tResultPA    = PrimitiveArray.factory(timePA.elementType(), 4, false);
        StringArray    idResultPA   = new StringArray();
        //but make sure data column is double or float, since it will hold floating point averages
        Class tEt = dataPA.elementType();
        if (tEt != PAType.DOUBLE && tEt != PAType.FLOAT) 
            tEt = PAType.FLOAT; //arbitrary: should it be double? 
        PrimitiveArray dataResultPA = PrimitiveArray.factory(tEt, 4, false);
        resultTable.addColumn(rawTable.getColumnName(0), xResultPA);
        resultTable.addColumn(rawTable.getColumnName(1), yResultPA);
        resultTable.addColumn(rawTable.getColumnName(2), zResultPA);
        resultTable.addColumn(rawTable.getColumnName(3), tResultPA);
        resultTable.addColumn(rawTable.getColumnName(4), idResultPA);
        resultTable.addColumn(rawTable.getColumnName(5), dataResultPA);
        rawTable.globalAttributes().copyTo(resultTable.globalAttributes());
        for (int col = 0; col < 6; col++)
            rawTable.columnAttributes(col).copyTo(resultTable.columnAttributes(col));

        //calculate the timePeriod begin and end times
        DoubleArray tBeginTime    = new DoubleArray(); //exact begin time for data inclusion
        DoubleArray tEndTime      = new DoubleArray(); //exact end   time (1 second back) for data inclusion
        DoubleArray tCenteredTime = new DoubleArray(); //time reported to user
        if (timePeriodIs1Month) {
            //monthly        
            GregorianCalendar centerGc = Calendar2.epochSecondsToGc(minT); //center of month
            GregorianCalendar beginGc = (GregorianCalendar)centerGc.clone(); //begin of month
            beginGc.set(Calendar2.MINUTE, 0);
            beginGc.set(Calendar2.HOUR_OF_DAY, 0);
            beginGc.set(Calendar2.DATE, 1);
            while (Calendar2.gcToEpochSeconds(centerGc) <= maxT) {
                //beginTime is begin of month
                tBeginTime.add(Calendar2.gcToEpochSeconds(beginGc));
                tCenteredTime.add(Calendar2.gcToEpochSeconds(centerGc));
                beginGc.add(Calendar.MONTH, 1); //advance 1 month
                centerGc.add(Calendar.MONTH, 1); //advance 1 month
                Calendar2.centerOfMonth(centerGc);
                tEndTime.add(Calendar2.gcToEpochSeconds(beginGc) - 1); //end is 1 second before start of next month
            }
        } else {     
            //25,33hour and nDay time periods  
            double centerT = minT; 
            double timePeriodIncrementSeconds = 
                timePeriodNHours % 24 == 0?  Calendar2.SECONDS_PER_DAY : //nDays every day
                    Calendar2.SECONDS_PER_HOUR;  //25 or 33 hour every hour                
            while (centerT <= maxT) {
                tBeginTime.add(centerT - timePeriodNSeconds / 2);
                tCenteredTime.add(centerT);
                tEndTime.add(centerT + timePeriodNSeconds / 2 - 1);
                centerT += timePeriodIncrementSeconds;
            }
        }
        int nTimePeriods = tBeginTime.size();
        double timePeriodBeginTime[]    = tBeginTime.toArray();    tBeginTime = null;
        double timePeriodEndTime[]      = tEndTime.toArray();      tEndTime = null;
        double timePeriodCenteredTime[] = tCenteredTime.toArray(); tCenteredTime = null;
        if (nTimePeriods == 0) {
            if (verbose) String2.log("\\\\**** TableDataSet.makeAveragedTimeSeries done. nRows=" + 
                resultTable.nRows() + " TIME=" + (System.currentTimeMillis() - time) + "ms");
            return resultTable;
        }

        //interesting tests, but don't leave on all the time
        //Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(minT), 
        //                 Calendar2.epochSecondsToIsoStringTZ(timePeriodCenteredTime[0]), 
        //                 "minT!=center[0]");
        //Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(maxT), 
        //                 Calendar2.epochSecondsToIsoStringTZ(timePeriodCenteredTime[nTimePeriods - 1]), 
        //                 "maxT!=center[last]");
        double timePeriodSum[]   = new double[nTimePeriods];
        int    timePeriodCount[] = new int[nTimePeriods];
        if (reallyVerbose) {
            String2.log("  mATS nTimePeriods=" + nTimePeriods);
            for (int i = 0; i < Math.min(3, nTimePeriods); i++)
                String2.log("    mATS timePeriod " + i + ": " +
                    Calendar2.epochSecondsToIsoStringTZ(timePeriodBeginTime[i]) + " to " + 
                    Calendar2.epochSecondsToIsoStringTZ(timePeriodEndTime[i]) + " center=" +
                    Calendar2.epochSecondsToIsoStringTZ(timePeriodCenteredTime[i]));
            String2.log("    mATS timePeriod " + (nTimePeriods - 1) + ": " +
                Calendar2.epochSecondsToIsoStringTZ(timePeriodBeginTime[nTimePeriods - 1]) + " to " + 
                Calendar2.epochSecondsToIsoStringTZ(timePeriodEndTime[nTimePeriods - 1]) + " center=" +
                Calendar2.epochSecondsToIsoStringTZ(timePeriodCenteredTime[nTimePeriods - 1]));
        }

        //make a new table with the averaged data
        int row = 0; //the next row to be read
        while (row < rawNRows) {
            //set up to read one x,y,z,id's data;  different time periods
            double stationX = xPA.getDouble(row);
            double stationY = yPA.getDouble(row);
            double stationZ = zPA.getDouble(row);
            String stationID = idPA.getString(row);
            Arrays.fill(timePeriodSum, 0);
            Arrays.fill(timePeriodCount, 0);

            //process this row's data (till next station or end of file)
            while (row < rawNRows) {
                //if x,y,z, or id is different, we're done
                if (xPA.getDouble(row) != stationX ||
                    yPA.getDouble(row) != stationY ||
                    zPA.getDouble(row) != stationZ ||
                    !idPA.getString(row).equals(stationID)) {
                    //don't advance row, so this row included in next group
                    break;
                }
                
                //is data not of interest? 
                double tValue = dataPA.getDouble(row);
                if (Double.isNaN(tValue)) {
                    row++;
                    continue;        
                }
                //String2.log("  TableDataSet.getAveragedTimeSeries datum=" + tValue);
                
                //include this data in relevant time periods
                //firstGE and lastLE identify the section of relevant time periods
                //this is tricky; draw a diagram
                double tTime = timePA.getDouble(row);
                int first = Math2.binaryFindFirstGAE9(timePeriodEndTime, tTime);  //first of interest
                int last  = Math2.binaryFindLastLAE9(timePeriodBeginTime, tTime); //last of interest
                if (first > last) 
                    String2.log(String2.ERROR + " in " + info + ":\n  firstGE or lastLE error:\n " +
                        " firstGE=" + first + 
                        " lastLE=" + last + " n=" + nTimePeriods + 
                        " time=" + tTime + "=" + Calendar2.epochSecondsToIsoStringTZ(tTime));
                        //+ "\n\nbeginTimes=" + timePeriodBeginTime +
                        //"\n\nendTimes=" + timePeriodEndTime);

                for (int i = first; i <= last; i++) {
                    //time periods overlap. if time specifically ok for this timePeriod, tally it
                    if (tTime >= timePeriodBeginTime[i] &&
                        tTime <= timePeriodEndTime[i]) {
                        timePeriodSum[i] += tValue;
                        timePeriodCount[i]++;
                    }
                }
                row++;
            }

            //add a row to result table (even if count==0)
            //String2.log("TableDataSet break for next group at row=" + row + " stationID=" + stationID);
            for (int i = 0; i < nTimePeriods; i++) {
                xResultPA.addDouble(stationX);
                yResultPA.addDouble(stationY);
                zResultPA.addDouble(stationZ);
                tResultPA.addDouble(timePeriodCenteredTime[i]);
                idResultPA.addString(stationID);
                dataResultPA.addDouble(timePeriodCount[i] == 0?
                    Double.NaN :
                    timePeriodSum[i] / timePeriodCount[i]);
                //String2.log("  TableDataSet.getAveragedTimeSeries average=" + 
                //   dataResultPA.get(dataResultPA.size() - 1) + " n=" + timePeriodCount[i]);
            }        

        }

        //set Attributes    ('null' says make no changes  (don't use ""))
        resultTable.setAttributes(0, 1, 2, 3, boldTitle, 
            null, //cdmDataType,   
            null, //creatorEmail set by makeSubset //who is creating this file...
            null, //creatorName  set by makeSubset 
            null, //creatorUrl   set by makeSubset 
            null, //project      set by makeSubset
            null, //id, 
            null, //keywordsVocabulary,
            null, //keywords, 
            null, //references, 
            null, //summary, 
            courtesy,
            "Centered Time" + 
                (TimePeriods.getNHours(timePeriod) > 0? " of " + timePeriod + " Averages" : ""));
        resultTable.globalAttributes().remove("time_coverage_resolution"); //e.g., my ndbc files have this set to P1H

        //sort by ID, t, z, y, x
        resultTable.sort(new int[]{4, 3, 2, 1, 0}, //keys
            new boolean[]{true, true, true, true, true}); //ascending

        //return the resultTable
        //String2.log("TableDataSet.makeAveragedTS resultsTable=" + resultsTable);
        //String2.log("TableDataSet.makeAveragedTS resultsTable data stats=" + resultsTable.getColumn(5).statsString());
        if (verbose) String2.log("\\\\**** TableDataSet.makeAveragedTimeSeries done. nRows=" + 
            resultTable.nRows() + " TIME=" + (System.currentTimeMillis() - time) + "ms\n");
        return resultTable;

    }
*/
    /**
     * This appends data about stations 
     * (which have data within an x,y,z,t bounding box)
     * to a table of stations.
     * Future: for trajectories, this could be the x,y,z of the first
     *   matching point in the trajectory.
     *
     * <p>Typical use of this is:
     * <ol>
     * <li> Table stations = TableDataSet.getEmptyStationTable(
     *     0, 360, -90, 90, -100, 100, "1900-01-01", "3000-01-01");
     * <li> tableDataSets[i].addStationsToTable(
            0, 360, -90, 90, -100, 100, "1900-01-01", "3000-01-01", stations);
     * <li> stations.setActualRangeAndBoundingBox(0,1,2,-1,3);
     * </ol>
     *
     * @param minX  the minimum acceptable longitude (degrees_east).
     *    minX and maxX may be -180 to 180, or 0 to 360.
     * @param maxX  the maximum acceptable longitude (degrees_east)
     * @param minY  the minimum acceptable latitude (degrees_north)
     * @param maxY  the maximum acceptable latitude (degrees_north)
     * @param minZ  the minumum acceptable altitude (meters, positive=up)
     * @param maxZ  the maxumum acceptable altitude (meters, positive=up)
     * @param isoMinT an ISO format date/time for the minimum acceptable time  
     * @param isoMaxT an ISO format date/time for the maximum acceptable time  
     * @param stations a Table with 4 columns (LON, LAT, DEPTH, ID),
     *    where lat is in degrees_east adjusted to be in the minX maxX range,
     *    lon is in degrees_north, depth is in meters down,
     *    and ID is a string suitable for sorting (e.g., MBARI MO).
     * @throws Exception if trouble (e.g., isoMinT is invalid)
     */
/*    public abstract void addStationsToTable(double minX, double maxX,
        double minY, double maxY, double minZ, double maxZ, 
        String isoMinT, String isoMaxT, Table stations) throws Exception;
*/
    /**
     * This returns an empty table suitable for getStations.
     *
     * @param minX  the minimum acceptable longitude (degrees_east).
     *    minX and maxX may be -180 to 180, or 0 to 360.
     * @param maxX  the maximum acceptable longitude (degrees_east)
     * @param minY  the minimum acceptable latitude (degrees_north)
     * @param maxY  the maximum acceptable latitude (degrees_north)
     * @param minZ  the minumum acceptable altitude (meters, positive=up)
     * @param maxZ  the maxumum acceptable altitude (meters, positive=up)
     * @param isoMinT an ISO format date/time for the minimum acceptable time  
     * @param isoMaxT an ISO format date/time for the maximum acceptable time  
     * @return an empty table suitable for getStations: 4 columns (LON, LAT, DEPTH, ID)
     *    where lat is in degrees_east adjusted to be in the minX maxX range,
     *    lon is in degrees_north, depth is in meters down,
     *    and ID is a string suitable for sorting (e.g., MBARI MO).
     *   and prelimary metadata. Use table.setActualRangeAndBoundingBox
     *   after adding data to add the rest of the metadata.
     */
/*    public static Table getEmptyStationTable(double minX, double maxX,
        double minY, double maxY, double minZ, double maxZ, 
        String isoMinT, String isoMaxT) {

        //create the table
        Table stations = new Table();
        stations.addColumn("LON", new DoubleArray());
        stations.addColumn("LAT",  new DoubleArray());
        stations.addColumn("DEPTH",  new DoubleArray());
        stations.addColumn("ID",  new StringArray());

        //add metadata from conventions
        //see gov/noaa/pfel/coastwatch/data/MetaMetadata.txt
        //see NdbcMetStation for comments about metadatastandards requirements
        Attributes lonAttributes = stations.columnAttributes(0);
        lonAttributes.set("_CoordinateAxisType", "Lon");
        lonAttributes.set("axis", "X");
        lonAttributes.set("long_name", "Longitude");
        lonAttributes.set("standard_name", "longitude");
        lonAttributes.set("units", "degrees_east");

        Attributes latAttributes = stations.columnAttributes(1);
        latAttributes.set("_CoordinateAxisType", "Lat");
        latAttributes.set("axis", "Y");
        latAttributes.set("long_name", "Latitude");
        latAttributes.set("standard_name", "latitude");
        latAttributes.set("units", "degrees_north");

        Attributes zAttributes = stations.columnAttributes(2);
        zAttributes.set("_CoordinateAxisType", "Height");
        zAttributes.set("axis", "Z");
        zAttributes.set("long_name", "Depth");
        zAttributes.set("standard_name", "depth");
        zAttributes.set("units", "m");

        Attributes idAttributes = stations.columnAttributes(3);
        idAttributes.set("long_name", "Station Identifier");
        idAttributes.set("units", DataHelper.UNITLESS);
        
        Attributes globalAttributes = stations.globalAttributes();
        globalAttributes.set("Conventions", "CF-1.6");
        String title = "Station Locations" +
            " (minX=" + minX + 
            ", maxX=" + maxX + 
            ", minY=" + minY + 
            ", maxY=" + maxY + 
            ", minZ=" + minZ + 
            ", maxZ=" + maxZ + 
            ", minT=" + isoMinT + 
            ", maxT=" + isoMaxT + ")";
        globalAttributes.set("title",  title);
        globalAttributes.set("keywords", "Oceans"); //part of line from http://gcmd.gsfc.nasa.gov/Resources/valids/gcmd_parameters.html
        //skip keywords vocabulary since not using it strictly
        globalAttributes.set("id", title); //2019-05-07 not right, but inactive, so not used by ERDDAP. Should be datasetID
        globalAttributes.set("naming_authority", "gov.noaa.pfel.coastwatch");
        globalAttributes.set("cdm_data_type", "Station");
        //skip 'history'
        String todaysDate = Calendar2.getCurrentISODateTimeStringLocalTZ().substring(0, 10);
        globalAttributes.set("date_created", todaysDate); 
        globalAttributes.set("creator_name", DataHelper.CW_CREATOR_NAME);
        globalAttributes.set("creator_url", "https://coastwatch.pfeg.noaa.gov");
        globalAttributes.set("creator_email", "bob.simons@noaa.gov");
        //globalAttributes.set("institution", DataHelper.CW_CREATOR_NAME);
        globalAttributes.set("project", "NOAA NESDIS CoastWatch");
        globalAttributes.set("acknowledgement", "Data is from many sources.");
        //globalAttributes.set("geospatial_lat_min",  lat);
        //globalAttributes.set("geospatial_lat_max",  lat);
        globalAttributes.set("geospatial_lat_units","degrees_north");
        //globalAttributes.set("geospatial_lon_min",  lon);
        //globalAttributes.set("geospatial_lon_max",  lon);
        globalAttributes.set("geospatial_lon_units","degrees_east");
        //globalAttributes.set("geospatial_vertical_min",  0.0); //actually, sensors are -1 ..~+5 meters
        //globalAttributes.set("geospatial_vertical_max",  0.0);
        globalAttributes.set("geospatial_vertical_units","meters");
        globalAttributes.set("geospatial_vertical_positive", "up"); //since some readings are above and some are below sea level
        //globalAttributes.set("time_coverage_start", Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(timeIndex, 0)));
        //double timeStats[] = table.getColumn(timeIndex).calculateStats();
        //globalAttributes.set("time_coverage_end",   Calendar2.epochSecondsToIsoStringTZ(timeStats[PrimitiveArray.STATS_MAX]));
        //globalAttributes.set("time_coverage_resolution", "P1H");
        globalAttributes.set("standard_name_vocabulary", FileNameUtility.getStandardNameVocabulary());
        globalAttributes.set("license", "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither NOAA, NDBC, CoastWatch, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.");
        globalAttributes.set("date_issued", todaysDate);
        //'comment' could include other information about the station, its history 
        globalAttributes.set("source", "station observation");
        //attributes unique in their opendap files
        //globalAttributes.set("quality", "Automated QC checks with periodic manual QC"); //an ndbc attribute?
        //globalAttributes.set("", ""}));
        //attributes for Google Earth
        //globalAttributes.set("Southernmost_Northing", lat);
        //globalAttributes.set("Northernmost_Northing", lat);
        //globalAttributes.set("Westernmost_Easting", lon);
        //globalAttributes.set("Easternmost_Easting", lon);

        return stations;    
    }

*/

    /**
     * This implements Comparable so that these can be sorted (based on 
     * 'option').
     *
     * @param o another TableDataSet object
     */
    public int compareTo(Object o) {
        TableDataSet tds = (TableDataSet)o;
        return datasetName().compareTo(tds.datasetName());
    }


    /**
     * Find the closest time period.
     *
     * @param timePeriodValue  best if a timePeriodOption from above. But may also 
     *   be a grid-style time period (see GridDataSetCWLocal).
     * @return the index of the timePeriodOption from above for the closest
     *   time period (or "1 day"'s index if trouble).
     */
/*    public static int closestTimePeriod(String timePeriodValue) {
        return TimePeriods.closestTimePeriod(timePeriodValue, timePeriodOptions);
    }
  */  

}
