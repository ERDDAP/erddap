/* 
 * EDDGridFromNcFiles Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.Projects;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.text.MessageFormat;
import java.util.List;


/**
 * Get netcdf-X.X.XX.jar from http://www.unidata.ucar.edu/software/netcdf-java/index.htm
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Get slf4j-jdk14.jar from 
 * ftp://ftp.unidata.ucar.edu/pub/netcdf-java/slf4j-jdk14.jar
 * and copy it to <context>/WEB-INF/lib.
 * Put both of these .jar files in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;

/** 
 * This class represents gridded data aggregated from a collection of 
 * NetCDF .nc (http://www.unidata.ucar.edu/software/netcdf/),
 * GRIB .grb (http://en.wikipedia.org/wiki/GRIB),
 * (and related) data files.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-01-05
 */
public class EDDGridFromNcFiles extends EDDGridFromFiles { 


    /** The constructor just calls the super constructor. */
    public EDDGridFromNcFiles(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tAxisVariables,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, boolean tRecursive, String tFileNameRegex, String tMetadataFrom,
        boolean tEnsureAxisValuesAreExactlyEqual, boolean tFileTableInMemory,
        boolean tAccessibleViaFiles) 
        throws Throwable {

        super("EDDGridFromNcFiles", tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, 
            tDefaultDataQuery, tDefaultGraphQuery, 
            tAddGlobalAttributes,
            tAxisVariables,
            tDataVariables,
            tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
            tEnsureAxisValuesAreExactlyEqual, tFileTableInMemory,
            tAccessibleViaFiles);
    }


    /**
     * This gets sourceGlobalAttributes and sourceDataAttributes from the specified 
     * source file.
     *
     * @param fileDir
     * @param fileName
     * @param sourceAxisNames
     * @param sourceDataNames the names of the desired source data columns.
     * @param sourceDataTypes the data types of the desired source columns 
     *    (e.g., "String" or "float") 
     * @param sourceGlobalAttributes should be an empty Attributes. It will be populated by this method
     * @param sourceAxisAttributes should be an array of empty Attributes. It will be populated by this method
     * @param sourceDataAttributes should be an array of empty Attributes. It will be populated by this method
     * @throws Throwable if trouble (e.g., invalid file, or a sourceAxisName or sourceDataName not found).
     *   If there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public void getSourceMetadata(String fileDir, String fileName, 
        StringArray sourceAxisNames,
        StringArray sourceDataNames, String sourceDataTypes[],
        Attributes sourceGlobalAttributes, 
        Attributes sourceAxisAttributes[],
        Attributes sourceDataAttributes[]) throws Throwable {

        NetcdfFile ncFile = NcHelper.openFile(fileDir + fileName); //may throw exception
        String getWhat = "globalAttributes";
        try {
            NcHelper.getGlobalAttributes(ncFile, sourceGlobalAttributes);

            for (int avi = 0; avi < sourceAxisNames.size(); avi++) {
                getWhat = "axisAttributes for avi=" + avi + " name=" + sourceAxisNames.get(avi);
                Variable var = ncFile.findVariable(sourceAxisNames.get(avi));  
                if (var == null) {
                    //it will be null for dimensions without corresponding coordinate axis variable
                    sourceAxisAttributes[avi].add("units", "count"); //"count" is udunits;  "index" isn't, but better?
                } else {
                    NcHelper.getVariableAttributes(var, sourceAxisAttributes[avi]);
                }
            }

            for (int dvi = 0; dvi < sourceDataNames.size(); dvi++) {
                getWhat = "dataAttributes for dvi=" + dvi + " name=" + sourceDataNames.get(dvi);
                Variable var = ncFile.findVariable(sourceDataNames.get(dvi));  //null if not found
                if (var == null)
                    String2.log("  var not in file: " + getWhat);
                else 
                    NcHelper.getVariableAttributes(var, sourceDataAttributes[dvi]);
            }

            //I care about this exception
            ncFile.close();

        } catch (Throwable t) {
            try {
                ncFile.close(); //make sure it is explicitly closed
            } catch (Throwable t2) {
                //don't care
            }
            throw new RuntimeException("Error in EDDGridFromNcFiles.getSourceMetadata" +
                "\nwhile getting " + getWhat + 
                "\nfrom " + fileDir + fileName + 
                "\nCause: " + MustBe.throwableToShortString(t),
                t);
        }
    }


    /**
     * This gets source axis values from one file.
     *
     * @param fileDir
     * @param fileName
     * @param sourceAxisNames the names of the desired source axis variables.
     * @return a PrimitiveArray[] with the results (with the requested sourceDataTypes).
     *   It needn't set sourceGlobalAttributes or sourceDataAttributes
     *   (but see getSourceMetadata).
     * @throws Throwable if trouble (e.g., invalid file).
     *   If there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public PrimitiveArray[] getSourceAxisValues(String fileDir, String fileName, 
        StringArray sourceAxisNames) throws Throwable {

        NetcdfFile ncFile = NcHelper.openFile(fileDir + fileName); //may throw exception
        String getWhat = "globalAttributes";
        try {
            PrimitiveArray[] avPa = new PrimitiveArray[sourceAxisNames.size()];

            for (int avi = 0; avi < sourceAxisNames.size(); avi++) {
                String avName = sourceAxisNames.get(avi);
                getWhat = "axisAttributes for variable=" + avName;
                Variable var = ncFile.findVariable(avName);  //null if not found
                if (var == null) {
                    //there is no corresponding coordinate variable; make pa of indices, 0...
                    Dimension dim = ncFile.findDimension(avName);
                    int dimSize1 = dim.getLength() - 1;
                    avPa[avi] = avi > 0 && dimSize1 < 32000? 
                        new ShortArray(0, dimSize1) :
                        new IntArray(0, dimSize1);
                } else {
                    avPa[avi] = NcHelper.getPrimitiveArray(var); 
                }
            }

            //I care about this exception
            ncFile.close();
            return avPa;

        } catch (Throwable t) {
            try {
                ncFile.close(); //make sure it is explicitly closed
            } catch (Throwable t2) {
                //don't care
            }
            throw new RuntimeException("Error in EDDGridFromNcFiles.getSourceAxisValues" +
                "\nwhile getting " + getWhat + 
                "\nfrom " + fileDir + fileName + 
                "\nCause: " + MustBe.throwableToShortString(t),
                t);
        }
    }

    /**
     * This gets source data from one file.
     *
     * @param fileDir
     * @param fileName
     * @param tDataVariables the desired data variables
     * @param tConstraints  where the first axis variable's constraints
     *   have been customized for this file.
     * @return a PrimitiveArray[] with an element for each tDataVariable with the dataValues.
     *   <br>The dataValues are straight from the source, not modified.
     *   <br>The primitiveArray dataTypes are usually the sourceDataTypeClass,
     *     but can be any type. EDDGridFromFiles will convert to the sourceDataTypeClass.
     *   <br>Note the lack of axisVariable values!
     * @throws Throwable if trouble (notably, WaitThenTryAgainException).
     *   If there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public PrimitiveArray[] getSourceDataFromFile(String fileDir, String fileName, 
        EDV tDataVariables[], IntArray tConstraints) throws Throwable {

        //make the selection spec  and get the axis values
        int nav = axisVariables.length;
        int ndv = tDataVariables.length;
        PrimitiveArray[] paa = new PrimitiveArray[ndv];
        StringBuilder selectionSB = new StringBuilder();
        for (int avi = 0; avi < nav; avi++) {
            selectionSB.append((avi == 0? "" : ",") +
                tConstraints.get(avi*3  ) + ":" + 
                tConstraints.get(avi*3+2) + ":" + 
                tConstraints.get(avi*3+1)); //start:stop:stride !
        }
        String selection = selectionSB.toString();

        NetcdfFile ncFile = NcHelper.openFile(fileDir + fileName); //may throw exception
        try {

            for (int dvi = 0; dvi < ndv; dvi++) {
                Variable var = ncFile.findVariable(tDataVariables[dvi].sourceName());  
                if (var == null) 
                    throw new RuntimeException(
                        MessageFormat.format(EDStatic.errorNotFoundIn,
                            "dataVariableSourceName=" + tDataVariables[dvi].sourceName(),
                            fileName)); //don't show directory    
                String tSel = selection;
                if (tDataVariables[dvi].sourceDataTypeClass() == String.class) 
                    tSel += ",0:" + (var.getShape(var.getRank() - 1) - 1);
                Array array = var.read(tSel);
                Object object = NcHelper.getArray(array);
                paa[dvi] = PrimitiveArray.factory(object); 
                //String2.log("!EDDGridFrimNcFiles.getSourceDataFromFile " + tDataVariables[dvi].sourceName() +
                //    "[" + selection + "]\n" + paa[dvi].toString());
            }

            //I care about this exception
            ncFile.close();
            return paa;

        } catch (Throwable t) {
            //make sure it is explicitly closed
            try {   
                ncFile.close();    
            } catch (Throwable t2) {
                String2.log("Error while trying to close " + fileDir + fileName +
                    "\n" + MustBe.throwableToShortString(t2));
            }  

            throw t;
        }
    }

    /**
     * This makes a sibling dataset, based on the new sourceUrl.
     *
     * @throws Throwable always (since this class doesn't support sibling())
     */
    public EDDGrid sibling(String tLocalSourceUrl, int ensureAxisValuesAreEqual, 
        boolean shareInfo) throws Throwable {
        throw new SimpleException("Error: " + 
            "EDDGridFromNcFiles doesn't support method=\"sibling\".");

    }

    /** 
     * This does its best to generate a clean, ready-to-use datasets.xml entry 
     * for an EDDGridFromNcFiles.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * <p>This can't be made into a web service because it would allow any user
     * to looks at (possibly) private .nc files on the server.
     *
     * @param tFileDir the starting (parent) directory for searching for files
     * @param tFileNameRegex  the regex that each filename (no directory info) must match 
     *    (e.g., ".*\\.nc")  (usually only 1 backslash; 2 here since it is Java code). 
     * @param sampleFileName full file name of one of the files in the collection
     * @param externalAddGlobalAttributes  These are given priority. Use null if none available.
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(
        String tFileDir, String tFileNameRegex, String sampleFileName, 
        int tReloadEveryNMinutes, Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("EDDGridFromNcFiles.generateDatasetsXml" +
            "\n  sampleFileName=" + sampleFileName);
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash
        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; //1440 works well with suggestedUpdateEveryNMillis

        NetcdfFile ncFile = NcHelper.openFile(sampleFileName); //may throw exception
        Table axisSourceTable = new Table();  
        Table dataSourceTable = new Table();  
        Table axisAddTable = new Table();
        Table dataAddTable = new Table();
        StringBuilder sb = new StringBuilder();

        try {
            //look at all variables with dimensions, find ones which share same max nDim
            List allVariables = ncFile.getVariables(); 
            int maxDim = 0;
            int nGridsAtSource = 0;
            for (int v = 0; v < allVariables.size(); v++) {
                Variable var = (Variable)allVariables.get(v);
                String varName = var.getFullName();
                List dimensions = var.getDimensions();
                if (dimensions == null || dimensions.size() <= 1) 
                    continue;
                nGridsAtSource++;
                Class tClass = NcHelper.getElementClass(var.getDataType());
                if      (tClass == char.class)    tClass = String.class;
                else if (tClass == boolean.class) tClass = byte.class; 
                PrimitiveArray pa = PrimitiveArray.factory(tClass, 1, false);
                int nDim = dimensions.size() - (tClass == String.class? 1 : 0);
                if (nDim < maxDim) {
                    continue;
                } else if (nDim > maxDim) {
                    //clear previous vars 
                    axisSourceTable.removeAllColumns();
                    dataSourceTable.removeAllColumns();
                    axisAddTable.removeAllColumns();
                    dataAddTable.removeAllColumns();
                    maxDim = nDim;

                    //store the axis vars
                    for (int avi = 0; avi < maxDim; avi++) {
                        Dimension tDim = ((Dimension)dimensions.get(avi));
//String2.log(">>varName=" + varName + " avi=" + avi + " dim=" + tDim.toString());
//String2.log(">>name=" + tDim.getName()); 
                        //work-around bug in netcdf-java: for anonymous dim,
                        //  getName() returns null, but getFullName() throws Exception.
                        String axisName = tDim.getName();
                        if (axisName != null) 
                            axisName = tDim.getFullName();  
                        Attributes sourceAtts = new Attributes();
                        if (axisName != null) {
                            Variable axisVariable = ncFile.findVariable(axisName);
                            if (axisVariable != null) //it will be null for dimension without same-named coordinate axis variable
                                NcHelper.getVariableAttributes(axisVariable, sourceAtts);
                        }
                        axisSourceTable.addColumn(avi, axisName, new DoubleArray(), //type doesn't matter
                            sourceAtts); 
                        String destName = String2.modifyToBeVariableNameSafe(axisName);
                        axisAddTable.addColumn(   avi, destName, new DoubleArray(), //type doesn't matter
                            makeReadyToUseAddVariableAttributesForDatasetsXml(
                                sourceAtts, destName, false, true)); //addColorBarMinMax, tryToFindLLAT

                    }

                } else { 
                    //nDim == maxDim
                    //if axes are different, reject this var
                    boolean ok = true;
                    for (int avi = 0; avi < maxDim; avi++) {
                        String axisName = ((Dimension)dimensions.get(avi)).getFullName();
                        String expectedName = axisSourceTable.getColumnName(avi);
                        if (!axisName.equals(expectedName)) {
                            if (verbose) String2.log("variable=" + varName + 
                                " has the right nDimensions=" + nDim + 
                                ", but axis#=" + avi + "=" + axisName + 
                                " != " + expectedName);
                            ok = false;
                            continue;
                        }
                    }
                }


                //add the dataVariable
                Attributes sourceAtts = new Attributes();
                NcHelper.getVariableAttributes(var, sourceAtts);
                dataSourceTable.addColumn(dataSourceTable.nColumns(), varName, pa, 
                    sourceAtts);
                String destName = String2.modifyToBeVariableNameSafe(varName);
                dataAddTable.addColumn(   dataAddTable.nColumns(),   destName, pa, 
                    makeReadyToUseAddVariableAttributesForDatasetsXml(
                        sourceAtts, destName, true, false)); //addColorBarMinMax, tryToFindLLAT
            }

            //after dataVariables known, add global attributes in the axisAddTable
            NcHelper.getGlobalAttributes(ncFile, axisSourceTable.globalAttributes());
            axisAddTable.globalAttributes().set(
                makeReadyToUseAddGlobalAttributesForDatasetsXml(
                    axisSourceTable.globalAttributes(), 
                    "Grid",  //another cdm type could be better; this is ok
                    tFileDir, externalAddGlobalAttributes, 
                    suggestKeywords(dataSourceTable, dataAddTable)));

            //gather the results 
            String tDatasetID = suggestDatasetID(tFileDir + tFileNameRegex);
            sb.append(directionsForGenerateDatasetsXml());

            if (nGridsAtSource > dataAddTable.nColumns())
                sb.append(
                    "!!! The source for " + tDatasetID + " has nGridVariables=" + nGridsAtSource + ",\n" +
                    "but this dataset will only serve " + dataAddTable.nColumns() + 
                    " because the others use different dimensions.\n");
            sb.append(
                "-->\n\n" +
                "<dataset type=\"EDDGridFromNcFiles\" datasetID=\"" + tDatasetID +                      
                    "\" active=\"true\">\n" +
                "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
                "    <updateEveryNMillis>" + suggestedUpdateEveryNMillis + "</updateEveryNMillis>\n" +  
                "    <fileDir>" + tFileDir + "</fileDir>\n" +
                "    <recursive>true</recursive>\n" +
                "    <fileNameRegex>" + XML.encodeAsXML(tFileNameRegex) + "</fileNameRegex>\n" +
                "    <metadataFrom>last</metadataFrom>\n" +
                "    <fileTableInMemory>false</fileTableInMemory>\n" +
                "    <accessibleViaFiles>false</accessibleViaFiles>\n");

            sb.append(writeAttsForDatasetsXml(false, axisSourceTable.globalAttributes(), "    "));
            sb.append(writeAttsForDatasetsXml(true,  axisAddTable.globalAttributes(),    "    "));
            
            //last 3 params: includeDataType, tryToFindLLAT, questionDestinationName
            sb.append(writeVariablesForDatasetsXml(axisSourceTable, axisAddTable, "axisVariable", false, true,  false));
            sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, "dataVariable", true,  false, false));
            sb.append(
                "</dataset>\n" +
                "\n");

            //I care about this exception
            ncFile.close();

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");

        } catch (Throwable t) {
            try {
                ncFile.close(); //make sure it is explicitly closed
            } catch (Throwable t2) {
                //don't care
            }
            throw t;
        }
        return sb.toString();        

    }



    /** This tests generateDatasetsXml. 
     * @throws Throwable if touble
     */
    public static void testGenerateDatasetsXml() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXml");

        String results = generateDatasetsXml(
            EDStatic.unitTestDataDir + "erdQSwind1day/", ".*_03\\.nc", 
            EDStatic.unitTestDataDir + "erdQSwind1day/erdQSwind1day_20080101_03.nc",
            DEFAULT_RELOAD_EVERY_N_MINUTES, null) + "\n";
        String suggDatasetID = suggestDatasetID(
            EDStatic.unitTestDataDir + "erdQSwind1day/.*_03\\.nc");

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromNcFiles",
            EDStatic.unitTestDataDir + "erdQSwind1day/", ".*_03\\.nc", 
            EDStatic.unitTestDataDir + "erdQSwind1day/erdQSwind1day_20080101_03.nc",
            "" + DEFAULT_RELOAD_EVERY_N_MINUTES},
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt. " + 
            gdxResults.length() + " " + results.length());

        String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"" + suggDatasetID + "\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + EDStatic.unitTestDataDir + "erdQSwind1day/</fileDir>\n" +
"    <recursive>true</recursive>\n" +
"    <fileNameRegex>.*_03\\.nc</fileNameRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"acknowledgement\">NOAA NESDIS COASTWATCH, NOAA SWFSC ERD</att>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"composite\">true</att>\n" +
"        <att name=\"contributor_name\">Remote Sensing Systems, Inc</att>\n" +
"        <att name=\"contributor_role\">Source of level 2 data.</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.0, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"creator_email\">dave.foley@noaa.gov</att>\n" +
"        <att name=\"creator_name\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"creator_url\">http://coastwatch.pfel.noaa.gov</att>\n" +
"        <att name=\"date_created\">2008-08-29Z</att>\n" +
"        <att name=\"date_issued\">2008-08-29Z</att>\n" +
"        <att name=\"Easternmost_Easting\" type=\"double\">359.875</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"double\">89.875</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">-89.875</att>\n" +
"        <att name=\"geospatial_lat_resolution\" type=\"double\">0.25</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">359.875</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">0.125</att>\n" +
"        <att name=\"geospatial_lon_resolution\" type=\"double\">0.25</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
"        <att name=\"geospatial_vertical_max\" type=\"double\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_min\" type=\"double\">0.0</att>\n" +
"        <att name=\"geospatial_vertical_positive\">up</att>\n" +
"        <att name=\"geospatial_vertical_units\">m</att>\n" +
"        <att name=\"history\">Remote Sensing Systems, Inc\n" +
"2008-08-29T00:31:43Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" +
             //still numeric ip because file was generated long ago
"2009-01-07 http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day\n" +
"2009-01-07 http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.nc?x_wind[(2008-01-01T12:00:00Z):1:(2008-01-03T12:00:00Z)][(0.0):1:(0.0)][(-89.875):1:(89.875)][(0.125):1:(359.875)],y_wind[(2008-01-01T12:00:00Z):1:(2008-01-03T12:00:00Z)][(0.0):1:(0.0)][(-89.875):1:(89.875)][(0.125):1:(359.875)],mod[(2008-01-01T12:00:00Z):1:(2008-01-03T12:00:00Z)][(0.0):1:(0.0)][(-89.875):1:(89.875)][(0.125):1:(359.875)]</att>\n" +
"        <att name=\"infoUrl\">http://coastwatch.pfel.noaa.gov/infog/QS_ux10_las.html</att>\n" +
"        <att name=\"institution\">NOAA CoastWatch, West Coast Node</att>\n" +
"        <att name=\"keywords\">EARTH SCIENCE &gt; Oceans &gt; Ocean Winds &gt; Surface Winds</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.</att>\n" +
"        <att name=\"naming_authority\">gov.noaa.pfel.coastwatch</att>\n" +
"        <att name=\"Northernmost_Northing\" type=\"double\">89.875</att>\n" +
"        <att name=\"origin\">Remote Sensing Systems, Inc</att>\n" +
"        <att name=\"processing_level\">3</att>\n" +
"        <att name=\"project\">CoastWatch (http://coastwatch.noaa.gov/)</att>\n" +
"        <att name=\"projection\">geographic</att>\n" +
"        <att name=\"projection_type\">mapped</att>\n" +
"        <att name=\"references\">RSS Inc. Winds: http://www.remss.com/ .</att>\n" +
"        <att name=\"satellite\">QuikSCAT</att>\n" +
"        <att name=\"sensor\">SeaWinds</att>\n" +
"        <att name=\"source\">satellite observation: QuikSCAT, SeaWinds</att>\n" +
                     //still numeric ip because file was generated long ago
"        <att name=\"sourceUrl\">http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day</att>\n" +
"        <att name=\"Southernmost_Northing\" type=\"double\">-89.875</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-1.0</att>\n" +
"        <att name=\"summary\">Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA&#39;s QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.</att>\n" +
"        <att name=\"time_coverage_end\">2008-01-03T12:00:00Z</att>\n" +
"        <att name=\"time_coverage_start\">2008-01-01T12:00:00Z</att>\n" +
"        <att name=\"title\">Wind, QuikSCAT, Global, Science Quality (1 Day Composite)</att>\n" +
"        <att name=\"Westernmost_Easting\" type=\"double\">0.125</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"institution\">NOAA CoastWatch WCN</att>\n" +
"        <att name=\"keywords\">atmosphere,\n" +
"Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" +
"atmospheric, coastwatch, composite, day, global, meridional, modulus, noaa, ocean, oceans,\n" +
"Oceans &gt; Ocean Winds &gt; Surface Winds,\n" +
"quality, quikscat, science, science quality, surface, wcn, wind, winds, x_wind, y_wind, zonal</att>\n" +
"        <att name=\"Metadata_Conventions\">COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"original_institution\">NOAA CoastWatch, West Coast Node</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Time</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">1.1991888E9 1.1993616E9</att>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">0</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Centered Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_origin\">01-JAN-1970 00:00:00</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>altitude</sourceName>\n" +
"        <destinationName>altitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Height</att>\n" +
"            <att name=\"_CoordinateZisPositive\">up</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">0.0 0.0</att>\n" +
"            <att name=\"axis\">Z</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Altitude</att>\n" +
"            <att name=\"positive\">up</att>\n" +
"            <att name=\"standard_name\">altitude</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>latitude</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lat</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">-89.875 89.875</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">2</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"point_spacing\">even</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>longitude</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lon</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">0.125 359.875</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">2</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"point_spacing\">even</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>x_wind</sourceName>\n" +
"        <destinationName>x_wind</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">1</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Zonal Wind</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">x_wind</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>y_wind</sourceName>\n" +
"        <destinationName>y_wind</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">1</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Meridional Wind</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"standard_name\">y_wind</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>mod</sourceName>\n" +
"        <destinationName>mod</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">18.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"colorBarPalette\">WhiteRedBlack</att>\n" +
"            <att name=\"coordsys\">geographic</att>\n" +
"            <att name=\"fraction_digits\" type=\"int\">1</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"long_name\">Modulus of Wind</att>\n" +
"            <att name=\"missing_value\" type=\"float\">-9999999.0</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, results.length() + " " + expected.length() + 
            "\nresults=\n" + results);

        //ensure it is ready-to-use by making a dataset from it
        EDD edd = oneFromXmlFragment(results);
        Test.ensureEqual(edd.datasetID(), suggDatasetID, "");
        Test.ensureEqual(edd.title(), "Wind, QuikSCAT, Global, Science Quality (1 Day Composite)", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "x_wind, y_wind, mod", "");

        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXml passed the test.");
    }

    /** This tests generateDatasetsXml, notably the invalid characters in global attribute
     * names created by netcdfAll-latest.jar. 
     *
     * @throws Throwable if touble
     */
    public static void testGenerateDatasetsXml2() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testGenerateDatasetsXml2");
        String results = generateDatasetsXml(
            "/erddapTestBig/geosgrib/", ".*", 
            "/erddapTestBig/geosgrib/multi_1.glo_30m.all.grb2",
            DEFAULT_RELOAD_EVERY_N_MINUTES, null);
        String suggDatasetID = suggestDatasetID(
            "/erddapTestBig/geosgrib/.*");

        String expected = //as of 2012-02-20. Will change if John Caron fixes bugs I reported.
directionsForGenerateDatasetsXml() +
"!!! The source for " + suggDatasetID + " has nGridVariables=13,\n" +
"but this dataset will only serve 3 because the others use different dimensions.\n" +
"-->\n" +
"\n" +
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"" + suggDatasetID + "\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/erddapTestBig/geosgrib/</fileDir>\n" +
"    <recursive>true</recursive>\n" +
"    <fileNameRegex>.*</fileNameRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"Analysis_or_forecast_generating_process_identifier_defined_by_originating_centre\">Global Multi-Grid Wave Model (Static Grids)</att>\n" +
"        <att name=\"Conventions\">CF-1.6</att>\n" +
"        <att name=\"featureType\">GRID</att>\n" +
"        <att name=\"file_format\">GRIB-2</att>\n" +
"        <att name=\"GRIB_table_version\">2,1</att>\n" +
"        <att name=\"history\">Read using CDM IOSP Grib2Collection</att>\n" +
"        <att name=\"Originating_or_generating_Center\">US National Weather Service, National Centres for Environmental Prediction (NCEP)</att>\n" +
"        <att name=\"Originating_or_generating_Subcenter\">0</att>\n" +
"        <att name=\"Type_of_generating_process\">Forecast</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">CF-1.6, COARDS, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"creator_name\">NCEP</att>\n" +
"        <att name=\"infoUrl\">???</att>\n" +
"        <att name=\"institution\">NCEP</att>\n" +
"        <att name=\"keywords\">data, direction, height, local, mean, ncep, ocean, oceans,\n" +
"Oceans &gt; Ocean Waves &gt; Significant Wave Height,\n" +
"Oceans &gt; Ocean Waves &gt; Swells,\n" +
"Oceans &gt; Ocean Waves &gt; Wave Period,\n" +
"ordered, period, sea, sea_surface_swell_wave_period, sea_surface_swell_wave_significant_height, sea_surface_swell_wave_to_direction, sequence, significant, source, surface, surface waves, swell, swells, wave, waves</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"Metadata_Conventions\">CF-1.6, COARDS, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-12</att>\n" +
"        <att name=\"summary\">NCEP data from a local source.</att>\n" +
"        <att name=\"title\">NCEP data from a local source.</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">Hour since 2009-06-01T06:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>ordered_sequence_of_data</sourceName>\n" +
"        <destinationName>ordered_sequence_of_data</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Grib2_level_type\" type=\"int\">241</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Taxonomy</att>\n" +
"            <att name=\"long_name\">Ordered Sequence Of Data</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Direction_of_swell_waves_ordered_sequence_of_data</sourceName>\n" +
"        <destinationName>Direction_of_swell_waves_ordered_sequence_of_data</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"abbreviation\">SWDIR</att>\n" +
"            <att name=\"Grib2_Generating_Process_Type\">Forecast</att>\n" +
"            <att name=\"Grib2_Level_Type\" type=\"int\">241</att>\n" +
"            <att name=\"Grib2_Parameter\" type=\"intList\">10 0 7</att>\n" +
"            <att name=\"Grib2_Parameter_Category\">Waves</att>\n" +
"            <att name=\"Grib2_Parameter_Discipline\">Oceanographic products</att>\n" +
"            <att name=\"Grib2_Parameter_Name\">Direction of swell waves</att>\n" +
"            <att name=\"Grib_Variable_Id\">VAR_10-0-7_L241</att>\n" +
"            <att name=\"long_name\">Direction of swell waves @ Ordered Sequence of Data</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">degree.true</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_to_direction</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Significant_height_of_swell_waves_ordered_sequence_of_data</sourceName>\n" +
"        <destinationName>Significant_height_of_swell_waves_ordered_sequence_of_data</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"abbreviation\">SWELL</att>\n" +
"            <att name=\"Grib2_Generating_Process_Type\">Forecast</att>\n" +
"            <att name=\"Grib2_Level_Type\" type=\"int\">241</att>\n" +
"            <att name=\"Grib2_Parameter\" type=\"intList\">10 0 8</att>\n" +
"            <att name=\"Grib2_Parameter_Category\">Waves</att>\n" +
"            <att name=\"Grib2_Parameter_Discipline\">Oceanographic products</att>\n" +
"            <att name=\"Grib2_Parameter_Name\">Significant height of swell waves</att>\n" +
"            <att name=\"Grib_Variable_Id\">VAR_10-0-8_L241</att>\n" +
"            <att name=\"long_name\">Significant height of swell waves @ Ordered Sequence of Data</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_significant_height</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>Mean_period_of_swell_waves_ordered_sequence_of_data</sourceName>\n" +
"        <destinationName>Mean_period_of_swell_waves_ordered_sequence_of_data</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"abbreviation\">SWPER</att>\n" +
"            <att name=\"Grib2_Generating_Process_Type\">Forecast</att>\n" +
"            <att name=\"Grib2_Level_Type\" type=\"int\">241</att>\n" +
"            <att name=\"Grib2_Parameter\" type=\"intList\">10 0 9</att>\n" +
"            <att name=\"Grib2_Parameter_Category\">Waves</att>\n" +
"            <att name=\"Grib2_Parameter_Discipline\">Oceanographic products</att>\n" +
"            <att name=\"Grib2_Parameter_Name\">Mean period of swell waves</att>\n" +
"            <att name=\"Grib_Variable_Id\">VAR_10-0-9_L241</att>\n" +
"            <att name=\"long_name\">Mean period of swell waves @ Ordered Sequence of Data</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">20.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Surface Waves</att>\n" +
"            <att name=\"standard_name\">sea_surface_swell_wave_period</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //ensure it is ready-to-use by making a dataset from it
        //EDD edd = oneFromXmlFragment(results);
        //Test.ensureEqual(edd.datasetID(), "erdQSwind1day_52db_1ed3_22ce", "");
        //Test.ensureEqual(edd.title(), "Wind, QuikSCAT, Global, Science Quality (1 Day Composite)", "");
        //Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
        //    "x_wind, y_wind, mod", "");

        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXml2 passed the test.");
    }



    /**
     * This tests reading NetCDF .nc files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testNc(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testNc() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDVGridAxis edvga;
        String id = "testGriddedNcFiles";
        String dataDir = EDStatic.unitTestDataDir + "erdQSwind1day/";
        deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml(id); 
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        //*** test getting das for entire dataset
        String2.log("\n*** .nc test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Entire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.1991888e+9, 1.1999664e+9;\n" +
"    String axis \"T\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float64 actual_range 0.0, 0.0;\n" +
"    String axis \"Z\";\n" +
"    Int32 fraction_digits 0;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range -89.875, 89.875;\n" +
"    String axis \"Y\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 0.125, 359.875;\n" +
"    String axis \"X\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String point_spacing \"even\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  x_wind {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum -15.0;\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Zonal Wind\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"x_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  y_wind {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum -15.0;\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Meridional Wind\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String standard_name \"y_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  mod {\n" +
"    Float32 _FillValue -9999999.0;\n" +
"    Float64 colorBarMaximum 18.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String colorBarPalette \"WhiteRedBlack\";\n" +
"    String coordsys \"geographic\";\n" +
"    Int32 fraction_digits 1;\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Modulus of Wind\";\n" +
"    Float32 missing_value -9999999.0;\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String acknowledgement \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" + 
"    String cdm_data_type \"Grid\";\n" +
"    String composite \"true\";\n" +
"    String contributor_name \"Remote Sensing Systems, Inc\";\n" +
"    String contributor_role \"Source of level 2 data.\";\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String creator_email \"dave.foley@noaa.gov\";\n" +
"    String creator_name \"NOAA CoastWatch, West Coast Node\";\n" +
"    String creator_url \"http://coastwatch.pfel.noaa.gov\";\n" +
"    String date_created \"2008-08-29Z\";\n" +
"    String date_issued \"2008-08-29Z\";\n" +
"    Float64 Easternmost_Easting 359.875;\n" +
"    Float64 geospatial_lat_max 89.875;\n" +
"    Float64 geospatial_lat_min -89.875;\n" +
"    Float64 geospatial_lat_resolution 0.25;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 359.875;\n" +
"    Float64 geospatial_lon_min 0.125;\n" +
"    Float64 geospatial_lon_resolution 0.25;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 0.0;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"Remote Sensing Systems, Inc\n" +
"2008-08-29T00:31:43Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" + 
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//            + " http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
//today + 

expected = " http://127.0.0.1:8080/cwexperimental/griddap/testGriddedNcFiles.das\";\n" +
"    String infoUrl \"http://coastwatch.pfel.noaa.gov/infog/QS_ux10_las.html\";\n" +
"    String institution \"NOAA CoastWatch, West Coast Node\";\n" +
"    String keywords \"EARTH SCIENCE > Oceans > Ocean Winds > Surface Winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String naming_authority \"gov.noaa.pfel.coastwatch\";\n" +
"    Float64 Northernmost_Northing 89.875;\n" +
"    String origin \"Remote Sensing Systems, Inc\";\n" +
"    String processing_level \"3\";\n" +
"    String project \"CoastWatch (http://coastwatch.noaa.gov/)\";\n" +
"    String projection \"geographic\";\n" +
"    String projection_type \"mapped\";\n" +
"    String references \"RSS Inc. Winds: http://www.remss.com/ .\";\n" +
"    String satellite \"QuikSCAT\";\n" +
"    String sensor \"SeaWinds\";\n" +
"    String source \"satellite observation: QuikSCAT, SeaWinds\";\n" +
                     //numeric IP because these are files captured long ago
"    String sourceUrl \"http://192.168.31.18/thredds/dodsC/satellite/QS/ux10/1day\";\n" +
"    Float64 Southernmost_Northing -89.875;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String summary \"Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.\";\n" +
"    String time_coverage_end \"2008-01-10T12:00:00Z\";\n" +
"    String time_coverage_start \"2008-01-01T12:00:00Z\";\n" +
"    String title \"Wind, QuikSCAT, Global, Science Quality (1 Day Composite)\";\n" +
"    Float64 Westernmost_Easting 0.125;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Entire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 10];\n" +
"  Float64 altitude[altitude = 1];\n" +
"  Float64 latitude[latitude = 720];\n" +
"  Float64 longitude[longitude = 1440];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 x_wind[time = 10][altitude = 1][latitude = 720][longitude = 1440];\n" +
"    MAPS:\n" +
"      Float64 time[time = 10];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 720];\n" +
"      Float64 longitude[longitude = 1440];\n" +
"  } x_wind;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 y_wind[time = 10][altitude = 1][latitude = 720][longitude = 1440];\n" +
"    MAPS:\n" +
"      Float64 time[time = 10];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 720];\n" +
"      Float64 longitude[longitude = 1440];\n" +
"  } y_wind;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 mod[time = 10][altitude = 1][latitude = 720][longitude = 1440];\n" +
"    MAPS:\n" +
"      Float64 time[time = 10];\n" +
"      Float64 altitude[altitude = 1];\n" +
"      Float64 latitude[latitude = 720];\n" +
"      Float64 longitude[longitude = 1440];\n" +
"  } mod;\n" +
"} testGriddedNcFiles;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);



        //.csv  with data from one file
        String2.log("\n*** .nc test read from one file\n");       
        userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
//verified with 
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
"time,altitude,latitude,longitude,y_wind\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n";

String csvExpected = 
"2008-01-10T12:00:00Z,0.0,36.625,230.125,3.555585\n" +
"2008-01-10T12:00:00Z,0.0,36.625,230.875,2.82175\n" +
"2008-01-10T12:00:00Z,0.0,36.625,231.625,4.539375\n" +
"2008-01-10T12:00:00Z,0.0,36.625,232.375,4.975015\n" +
"2008-01-10T12:00:00Z,0.0,36.625,233.125,5.643055\n" +
"2008-01-10T12:00:00Z,0.0,36.625,233.875,2.72394\n" +
"2008-01-10T12:00:00Z,0.0,36.625,234.625,1.39762\n" +
"2008-01-10T12:00:00Z,0.0,36.625,235.375,2.10711\n" +
"2008-01-10T12:00:00Z,0.0,36.625,236.125,3.019165\n" +
"2008-01-10T12:00:00Z,0.0,36.625,236.875,3.551915\n" +
"2008-01-10T12:00:00Z,0.0,36.625,237.625,NaN\n";          //test of NaN
        expected += csvExpected;

        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csvp  with data from one file
        String2.log("\n*** .nc test read from one file  .csvp\n");       
        userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".csvp"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
//verified with 
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csvp?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
"time (UTC),altitude (m),latitude (degrees_north),longitude (degrees_east),y_wind (m s-1)\n" +
csvExpected;       
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv0  with data from one file
        String2.log("\n*** .nc test read from one file  .csv0\n");       
        userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".csv0"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = csvExpected;
//verified with 
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv0?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from several files
        String2.log("\n*** .nc test read from several files\n");       
        userDapQuery = "y_wind[(1.1991888e9):3:(1.1999664e9)][0][(36.5)][(230)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
//verified with 
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv?y_wind[(1.1991888e9):3:(1.1999664e9)][0][(36.5)][(230)]
"time,altitude,latitude,longitude,y_wind\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"2008-01-01T12:00:00Z,0.0,36.625,230.125,7.6282454\n" +
"2008-01-04T12:00:00Z,0.0,36.625,230.125,-12.3\n" +
"2008-01-07T12:00:00Z,0.0,36.625,230.125,-5.974585\n" +
"2008-01-10T12:00:00Z,0.0,36.625,230.125,3.555585\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.tsv  with data from one file
        String2.log("\n*** .nc test read from one file\n");       
        userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".tsv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
//verified with 
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.tsv?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
"time\taltitude\tlatitude\tlongitude\ty_wind\n" +
"UTC\tm\tdegrees_north\tdegrees_east\tm s-1\n";

String tsvExpected = 
"2008-01-10T12:00:00Z\t0.0\t36.625\t230.125\t3.555585\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t230.875\t2.82175\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t231.625\t4.539375\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t232.375\t4.975015\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t233.125\t5.643055\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t233.875\t2.72394\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t234.625\t1.39762\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t235.375\t2.10711\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t236.125\t3.019165\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t236.875\t3.551915\n" +
"2008-01-10T12:00:00Z\t0.0\t36.625\t237.625\tNaN\n";          //test of NaN
        expected += tsvExpected;

        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.tsvp  with data from one file
        String2.log("\n*** .nc test read from one file  .tsvp\n");       
        userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".tsvp"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
//verified with 
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csvp?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
"time (UTC)\taltitude (m)\tlatitude (degrees_north)\tlongitude (degrees_east)\ty_wind (m s-1)\n" +
tsvExpected;       
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.tsv0  with data from one file
        String2.log("\n*** .nc test read from one file  .tsv0\n");       
        userDapQuery = "y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_Data1", ".tsv0"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = tsvExpected;
//verified with 
//http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwind1day.csv0?y_wind[(1.1999664e9)][0][(36.5)][(230):3:(238)]
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //  */
    }

    

    /**
     * For netcdfAll version 4.2 and below, this tests reading GRIB .grb files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testGrib_42(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testGrib_42() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        try {

        //generateDatasetsXml
        
        //  /*
        String id = "testGribFiles_42";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml(id); 

        //*** test getting das for entire dataset
        String2.log("\n*** .grb test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_42", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 3.471984e+8, 6.600528e+8;\n" +
"    String axis \"T\";\n" +
"    String GRIB2_significanceOfRTName \"Start of forecast\";\n" +
"    String GRIB_orgReferenceTime \"1981-01-01T12:00:00Z\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Forecast Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  height_above_ground {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    Float64 actual_range 10.0, 10.0;\n" +
"    String GRIB_level_type \"105\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Specified Height Level above Ground\";\n" +
"    String positive \"up\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 88.75, -88.75;\n" +
"    String axis \"Y\";\n" +
"    String grid_spacing \"-2.5 degrees_north\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 0.0, 356.25;\n" +
"    String axis \"X\";\n" +
"    String grid_spacing \"3.75 degrees_east\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  wind_speed {\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Int32 GRIB_center_id 74;\n" +
"    Int32 GRIB_level_type 105;\n" +
"    Int32 GRIB_param_id 1, 74, 1, 32;\n" +
"    String GRIB_param_name \"Wind_speed\";\n" +
"    Int32 GRIB_param_number 32;\n" +
"    String GRIB_param_short_name \"VAR32\";\n" +
"    String GRIB_product_definition_type \"Initialized analysis product\";\n" +
"    Int32 GRIB_table_id 1;\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind_speed @ height_above_ground\";\n" +
"    Float32 missing_value -9999.0;\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _CoordinateModelRunDate \"1981-01-01T12:00:00Z\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String CF:feature_type \"GRID\";\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
//"    String creator_name \"UK Meteorological Office Bracknell (RSMC) subcenter = 0\";\n" +
"    Float64 Easternmost_Easting 356.25;\n" +
"    String file_format \"GRIB-1\";\n" +
//"    String Generating_Process_or_Model \"Unknown\";\n" +
"    Float64 geospatial_lat_max 88.75;\n" +
"    Float64 geospatial_lat_min -88.75;\n" +
"    Float64 geospatial_lat_resolution 2.5;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 356.25;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    Float64 geospatial_lon_resolution 3.75;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"Direct read of GRIB-1 into NetCDF-Java 4 API\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//+ " (local files)\n" +
//today + 

expected = 
" http://127.0.0.1:8080/cwexperimental/griddap/testGribFiles_42.das\";\n" +
"    String infoUrl \"http://www.nceas.ucsb.edu/scicomp/GISSeminar/UseCases/ExtractGRIBClimateWithR/ExtractGRIBClimateWithR.html\";\n" +
"    String institution \"UK Met RSMC\";\n" +
"    String keywords \"Atmosphere > Atmospheric Winds > Surface Winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +    
"    String location \"" + EDStatic.unitTestDataDir + "grib/HADCM3_A2_wind_1981-1990.grb\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Northernmost_Northing 88.75;\n" +
"    String Originating_center \"U.K. Met Office - Exeter (RSMC) (74)\";\n" +
"    String Product_Type \"Initialized analysis product\";\n" +
"    String source \"Initialized analysis product\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -88.75;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String summary \"This is a test of EDDGridFromNcFiles with GRIB files.\";\n" +
"    String time_coverage_end \"1990-12-01T12:00:00Z\";\n" +
"    String time_coverage_start \"1981-01-01T12:00:00Z\";\n" +
"    String title \"Test of EDDGridFromNcFiles with GRIB files\";\n" +
"    Float64 Westernmost_Easting 0.0;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        tResults = results.substring(tPo, Math.min(results.length(), tPo +  expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_42", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 120];\n" +
"  Float64 height_above_ground[height_above_ground = 1];\n" +
"  Float64 latitude[latitude = 72];\n" +
"  Float64 longitude[longitude = 96];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 wind_speed[time = 120][height_above_ground = 1][latitude = 72][longitude = 96];\n" +
"    MAPS:\n" +
"      Float64 time[time = 120];\n" +
"      Float64 height_above_ground[height_above_ground = 1];\n" +
"      Float64 latitude[latitude = 72];\n" +
"      Float64 longitude[longitude = 96];\n" +
"  } wind_speed;\n" +
"} testGribFiles_42;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from one file
        String2.log("\n*** .grb test read from one file\n");       
        userDapQuery = "wind_speed[(4e8):10:(5e8)][0][(36.5)][(200):5:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribData1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time,height_above_ground,latitude,longitude,wind_speed\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"1982-09-01T12:00:00Z,10.0,36.25,198.75,8.129883\n" +
"1982-09-01T12:00:00Z,10.0,36.25,217.5,5.25\n" +
"1982-09-01T12:00:00Z,10.0,36.25,236.25,3.1298828\n" +
"1983-07-01T12:00:00Z,10.0,36.25,198.75,5.379883\n" +
"1983-07-01T12:00:00Z,10.0,36.25,217.5,5.25\n" +
"1983-07-01T12:00:00Z,10.0,36.25,236.25,2.6298828\n" +
"1984-05-01T12:00:00Z,10.0,36.25,198.75,5.38\n" +
"1984-05-01T12:00:00Z,10.0,36.25,217.5,7.7501173\n" +
"1984-05-01T12:00:00Z,10.0,36.25,236.25,3.88\n" +
"1985-03-01T12:00:00Z,10.0,36.25,198.75,8.629883\n" +
"1985-03-01T12:00:00Z,10.0,36.25,217.5,9.0\n" +
"1985-03-01T12:00:00Z,10.0,36.25,236.25,3.25\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //  */
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }

    /**
     * For netcdfAll version 4.2 and below, this tests reading GRIB2 .grb2 files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testGrib2_42(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testGrib2_42() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        String2.log(NcHelper.dumpString("/erddapTestBig/geosgrib/multi_1.glo_30m.all.grb2", false));

        //generateDatasetsXml
        try {   
        String id = "testGrib2_42";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml(id); 

        //*** test getting das for entire dataset
        String2.log("\n*** .grb2 test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_42", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.243836e+9, 1.244484e+9;\n" +
"    String axis \"T\";\n" +
//"    String GRIB2_significanceOfRTName \"Start of forecast\";\n" +
//"    String GRIB_orgReferenceTime \"2009-06-01T06:00:00Z\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Forecast Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 90.0, -77.5;\n" +
"    String axis \"Y\";\n" +
"    String grid_spacing \"-0.5 degrees_north\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range 0.0, 359.5;\n" +
"    String axis \"X\";\n" +
"    String grid_spacing \"0.5 degrees_east\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  Direction_of_swell_waves {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 241;\n" +
"    String GRIB_level_type_name \"ordered_sequence_of_data\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 7;\n" +
"    String GRIB_param_name \"Direction_of_swell_waves\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Direction_of_swell_waves @ ordered_sequence_of_data\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_swell_wave_to_direction\";\n" +
"    String units \"degrees\";\n" +
"  }\n" +
"  Direction_of_wind_waves {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 4;\n" +
"    String GRIB_param_name \"Direction_of_wind_waves\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Direction_of_wind_waves @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wind_wave_to_direction\";\n" +
"    String units \"degrees\";\n" +
"  }\n" +
"  Mean_period_of_swell_waves {\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 241;\n" +
"    String GRIB_level_type_name \"ordered_sequence_of_data\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 9;\n" +
"    String GRIB_param_name \"Mean_period_of_swell_waves\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Mean_period_of_swell_waves @ ordered_sequence_of_data\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_swell_wave_mean_period_from_variance_spectral_density_first_frequency_moment\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  Mean_period_of_wind_waves {\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 6;\n" +
"    String GRIB_param_name \"Mean_period_of_wind_waves\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Mean_period_of_wind_waves @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wind_wave_mean_period_from_variance_spectral_density_first_frequency_moment\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  Primary_wave_direction {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 10;\n" +
"    String GRIB_param_name \"Primary_wave_direction\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Primary_wave_direction @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wave_to_direction\";\n" +
"    String units \"degrees\";\n" +
"  }\n" +
"  Primary_wave_mean_period {\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 11;\n" +
"    String GRIB_param_name \"Primary_wave_mean_period\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Primary_wave_mean_period @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wave_mean_period_from_variance_spectral_density_first_frequency_moment\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  Significant_height_of_combined_wind_waves_and_swell {\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 3;\n" +
"    String GRIB_param_name \"Significant_height_of_combined_wind_waves_and_swell\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Significant_height_of_combined_wind_waves_and_swell @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  Significant_height_of_swell_waves {\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 241;\n" +
"    String GRIB_level_type_name \"ordered_sequence_of_data\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 8;\n" +
"    String GRIB_param_name \"Significant_height_of_swell_waves\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Significant_height_of_swell_waves @ ordered_sequence_of_data\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_swell_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  Significant_height_of_wind_waves {\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Waves\";\n" +
"    String GRIB_param_discipline \"Oceanographic_products\";\n" +
"    Int32 GRIB_param_id 2, 10, 0, 5;\n" +
"    String GRIB_param_name \"Significant_height_of_wind_waves\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Significant_height_of_wind_waves @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wind_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  U_component_of_wind {\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Momentum\";\n" +
"    String GRIB_param_discipline \"Meteorological_products\";\n" +
"    Int32 GRIB_param_id 2, 0, 2, 2;\n" +
"    String GRIB_param_name \"U-component_of_wind\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"U-component_of_wind @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"x_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  V_component_of_wind {\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Momentum\";\n" +
"    String GRIB_param_discipline \"Meteorological_products\";\n" +
"    Int32 GRIB_param_id 2, 0, 2, 3;\n" +
"    String GRIB_param_name \"V-component_of_wind\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"V-component_of_wind @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"y_wind\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  Wind_direction_from_which_blowing {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Momentum\";\n" +
"    String GRIB_param_discipline \"Meteorological_products\";\n" +
"    Int32 GRIB_param_id 2, 0, 2, 0;\n" +
"    String GRIB_param_name \"Wind_direction\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind_direction_from_which_blowing @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"wind_from_direction\";\n" +
"    String units \"degrees\";\n" +
"  }\n" +
"  Wind_speed {\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String GRIB_generating_process_type \"Forecast\";\n" +
"    Int32 GRIB_level_type 1;\n" +
"    String GRIB_level_type_name \"surface\";\n" +
"    String GRIB_param_category \"Momentum\";\n" +
"    String GRIB_param_discipline \"Meteorological_products\";\n" +
"    Int32 GRIB_param_id 2, 0, 2, 1;\n" +
"    String GRIB_param_name \"Wind_speed\";\n" +
"    Int32 GRIB_product_definition_template 0;\n" +
"    String GRIB_product_definition_template_desc \"Analysis/forecast at horizontal level/layer at a point in time\";\n" +
"    String GRIB_VectorComponentFlag \"easterlyNortherlyRelative\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind_speed @ surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _CoordinateModelRunDate \"2009-06-01T06:00:00Z\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String CF:feature_type \"GRID\";\n" +  //Eeeek!
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Easternmost_Easting 359.5;\n" +
"    String file_format \"GRIB-2\";\n" +
"    String Generating_Model \"Global Multi-Grid Wave Model\";\n" +
"    Float64 geospatial_lat_max 90.0;\n" +
"    Float64 geospatial_lat_min -77.5;\n" +
"    Float64 geospatial_lat_resolution 0.5;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 359.5;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    Float64 geospatial_lon_resolution 0.5;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"Direct read of GRIB-2 into NetCDF-Java 4 API\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//+ " (local files)\n" +
//today + 
expected= 
" http://127.0.0.1:8080/cwexperimental/griddap/testGrib2_42.das\";\n" +
"    String infoUrl \"???\";\n" +
"    String institution \"???\";\n" +
"    String keywords \"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"Oceans > Ocean Waves > Wave Height,\n" +
"Oceans > Ocean Waves > Wave Speed/Direction\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String location \"/erddapTestBig/geosgrib/multi_1.glo_30m.all.grb2\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Northernmost_Northing 90.0;\n" +
"    String Originating_center \"US National Weather Service - NCEP(WMC) (7)\";\n" +
"    String Product_Status \"Operational products\";\n" +
"    String Product_Type \"Forecast products\";\n" +
"    String source \"Type: Forecast products Status: Operational products\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -77.5;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String summary \"???\";\n" +
"    String time_coverage_end \"2009-06-08T18:00:00Z\";\n" +
"    String time_coverage_start \"2009-06-01T06:00:00Z\";\n" +
"    String title \"Test of Grib2\";\n" +
"    Float64 Westernmost_Easting 0.0;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        tResults = results.substring(tPo, Math.min(results.length(), tPo +  expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_42", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 61];\n" +
"  Float64 latitude[latitude = 336];\n" +
"  Float64 longitude[longitude = 720];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Direction_of_swell_waves[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Direction_of_swell_waves;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Direction_of_wind_waves[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Direction_of_wind_waves;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Mean_period_of_swell_waves[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Mean_period_of_swell_waves;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Mean_period_of_wind_waves[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Mean_period_of_wind_waves;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Primary_wave_direction[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Primary_wave_direction;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Primary_wave_mean_period[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Primary_wave_mean_period;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Significant_height_of_combined_wind_waves_and_swell[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Significant_height_of_combined_wind_waves_and_swell;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Significant_height_of_swell_waves[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Significant_height_of_swell_waves;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Significant_height_of_wind_waves[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Significant_height_of_wind_waves;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 U_component_of_wind[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } U_component_of_wind;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 V_component_of_wind[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } V_component_of_wind;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Wind_direction_from_which_blowing[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Wind_direction_from_which_blowing;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Wind_speed[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float64 latitude[latitude = 336];\n" +
"      Float64 longitude[longitude = 720];\n" +
"  } Wind_speed;\n" +
"} testGrib2_42;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from one file
        String2.log("\n*** .grb test read from one file\n");       
        userDapQuery = "Wind_speed[0][(30)][(200):5:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribData1_42", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time,latitude,longitude,Wind_speed\n" +
"UTC,degrees_north,degrees_east,m s-1\n" +
"2009-06-01T06:00:00Z,30.0,200.0,6.17\n" +
"2009-06-01T06:00:00Z,30.0,202.5,6.73\n" +
"2009-06-01T06:00:00Z,30.0,205.0,8.13\n" +
"2009-06-01T06:00:00Z,30.0,207.5,6.7\n" +
"2009-06-01T06:00:00Z,30.0,210.0,4.62\n" +
"2009-06-01T06:00:00Z,30.0,212.5,1.48\n" +
"2009-06-01T06:00:00Z,30.0,215.0,3.03\n" +
"2009-06-01T06:00:00Z,30.0,217.5,4.63\n" +
"2009-06-01T06:00:00Z,30.0,220.0,5.28\n" +
"2009-06-01T06:00:00Z,30.0,222.5,5.3\n" +
"2009-06-01T06:00:00Z,30.0,225.0,4.04\n" +
"2009-06-01T06:00:00Z,30.0,227.5,3.64\n" +
"2009-06-01T06:00:00Z,30.0,230.0,5.3\n" +
"2009-06-01T06:00:00Z,30.0,232.5,2.73\n" +
"2009-06-01T06:00:00Z,30.0,235.0,3.15\n" +
"2009-06-01T06:00:00Z,30.0,237.5,4.23\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //  */
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }

    /**
     * For netcdfAll version 4.3 and above, this tests reading GRIB .grb files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testGrib_43(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testGrib_43() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        try {

        //generateDatasetsXml
        
        //  /*
        String id = "testGribFiles_43";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml(id); 

        //*** test getting das for entire dataset
        String2.log("\n*** .grb test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_43", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = //2013-09-03 The details of the GRIB attributes change frequently!
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 3.471984e+8, 6.600528e+8;\n" +
"    String axis \"T\";\n" +
"    String GRIB2_significanceOfRTName \"Start of forecast\";\n" +
"    String GRIB_orgReferenceTime \"1981-01-01T12:00:00Z\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Forecast Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  height_above_ground {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    Float32 actual_range 10.0, 10.0;\n" +
"    String datum \"ground\";\n" +
"    Int32 Grib1_level_code 105;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Specified Height Level above Ground\";\n" +
"    String positive \"up\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range 88.75001, -88.74999;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range 0.0, 356.25;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  wind_speed {\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    Int32 Grib1_Center 74;\n" +
"    String Grib1_Level_Desc \"Specified height level above ground\";\n" +
"    Int32 Grib1_Level_Type 105;\n" +
"    Int32 Grib1_Parameter 32;\n" +
"    Int32 Grib1_Subcenter 0;\n" +
"    Int32 Grib1_TableVersion 1;\n" +
"    String Grib_Variable_Id \"VAR_74-0-1-32_L105\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind speed @ Specified height level above ground\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"m s-1\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _CoordinateModelRunDate \"1981-01-01T12:00:00Z\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Easternmost_Easting 356.25;\n" +
"    String file_format \"GRIB-1\";\n" +
"    Float64 geospatial_lat_max 88.75001;\n" +
"    Float64 geospatial_lat_min -88.74999;\n" +
"    Float64 geospatial_lat_resolution 2.5;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 356.25;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    Float64 geospatial_lon_resolution 3.75;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \"Read using CDM IOSP Grib1Collection\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//+ " (local files)\n" +
//today + 
try {
expected = 
"http://127.0.0.1:8080/cwexperimental/griddap/testGribFiles_43.das\";\n" +
"    String infoUrl \"http://www.nceas.ucsb.edu/scicomp/GISSeminar/UseCases/ExtractGRIBClimateWithR/ExtractGRIBClimateWithR.html\";\n" +
"    String institution \"UK Met RSMC\";\n" +
"    String keywords \"Atmosphere > Atmospheric Winds > Surface Winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String location \"" + EDStatic.unitTestDataDir + "grib/HADCM3_A2_wind_1981-1990.grb\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Northernmost_Northing 88.75001;\n" +
"    String Originating_or_generating_Center \"UK Meteorological Office ­ Exeter (RSMC)\";\n" + //- is #173!
"    String Originating_or_generating_Subcenter \"0\";\n" +
"    String Product_Type \"Initialized analysis product\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -88.74999;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String summary \"This is a test of EDDGridFromNcFiles with GRIB files.\";\n" +
"    String time_coverage_end \"1990-12-01T12:00:00Z\";\n" +
"    String time_coverage_start \"1981-01-01T12:00:00Z\";\n" +
"    String title \"Test of EDDGridFromNcFiles with GRIB files\";\n" +
"    Float64 Westernmost_Easting 0.0;\n" +
"  }\n" +
"}\n";
            int tPo = results.indexOf(expected.substring(0, 17));
            Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
            Test.ensureEqual(
                results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
                expected, "results=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nPress ^C to stop or Enter to continue..."); 
        }
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_43", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 120];\n" +
"  Float32 height_above_ground[height_above_ground = 1];\n" +
"  Float32 latitude[latitude = 72];\n" +
"  Float32 longitude[longitude = 96];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 wind_speed[time = 120][height_above_ground = 1][latitude = 72][longitude = 96];\n" +
"    MAPS:\n" +
"      Float64 time[time = 120];\n" +
"      Float32 height_above_ground[height_above_ground = 1];\n" +
"      Float32 latitude[latitude = 72];\n" +
"      Float32 longitude[longitude = 96];\n" +
"  } wind_speed;\n" +
"} testGribFiles_43;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from one file
        String2.log("\n*** .grb test read from one file\n");       
        userDapQuery = "wind_speed[(4e8):10:(5e8)][0][(36.5)][(200):5:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribData1_43", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time,height_above_ground,latitude,longitude,wind_speed\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"1982-09-01T12:00:00Z,10.0,36.250008,198.75002,8.129883\n" +
"1982-09-01T12:00:00Z,10.0,36.250008,217.50002,5.25\n" +
"1982-09-01T12:00:00Z,10.0,36.250008,236.25002,3.1298828\n" +
"1983-07-01T12:00:00Z,10.0,36.250008,198.75002,5.379883\n" +
"1983-07-01T12:00:00Z,10.0,36.250008,217.50002,5.25\n" +
"1983-07-01T12:00:00Z,10.0,36.250008,236.25002,2.6298828\n" +
"1984-05-01T12:00:00Z,10.0,36.250008,198.75002,5.38\n" +
"1984-05-01T12:00:00Z,10.0,36.250008,217.50002,7.7501173\n" +
"1984-05-01T12:00:00Z,10.0,36.250008,236.25002,3.88\n" +
"1985-03-01T12:00:00Z,10.0,36.250008,198.75002,8.629883\n" +
"1985-03-01T12:00:00Z,10.0,36.250008,217.50002,9.0\n" +
"1985-03-01T12:00:00Z,10.0,36.250008,236.25002,3.25\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //  */
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }

    /**
     * For netcdfAll version 4.3 and above, this tests reading GRIB2 .grb2 files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testGrib2_43(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testGrib2_43() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        //generateDatasetsXml
        //file dir is EDStatic.unitTestDataDir: /erddapTest/
        try {   
        String id = "testGrib2_43";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml(id); 

        //*** test getting das for entire dataset
        String2.log("\n*** .grb2 test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_43", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.243836e+9, 1.244484e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Forecast Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range 90.0, -77.5;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range 0.0, 359.5;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  Direction_of_swell_waves_degree_true_ordered_sequence_of_data {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String standard_name \"sea_surface_swell_wave_to_direction\";\n" +
"  }\n" +
"  Direction_of_wind_waves_degree_true_surface {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String standard_name \"sea_surface_wind_wave_to_direction\";\n" +
"  }\n" +
"  Mean_period_of_swell_waves_ordered_sequence_of_data {\n" +
"    String abbreviation \"SWPER\";\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 241;\n" +
"    Int32 Grib2_Parameter 10, 0, 9;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Mean period of swell waves\";\n" +
"    String Grib_Variable_Id \"VAR_10-0-9_L241\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Mean period of swell waves @ Ordered Sequence of Data\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_swell_wave_mean_period_from_variance_spectral_density_first_frequency_moment\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  Mean_period_of_wind_waves_surface {\n" +
"    String abbreviation \"WVPER\";\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 10, 0, 6;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Mean period of wind waves\";\n" +
"    String Grib_Variable_Id \"VAR_10-0-6_L1\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Mean period of wind waves @ Ground or water surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wind_wave_mean_period_from_variance_spectral_density_first_frequency_moment\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  Primary_wave_direction_degree_true_surface {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String standard_name \"sea_surface_wave_to_direction\";\n" +
"  }\n" +
"  Primary_wave_mean_period_surface {\n" +
"    String abbreviation \"PERPW\";\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 10, 0, 11;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Primary wave mean period\";\n" +
"    String Grib_Variable_Id \"VAR_10-0-11_L1\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Primary wave mean period @ Ground or water surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wave_mean_period_from_variance_spectral_density_first_frequency_moment\";\n" +
"    String units \"s\";\n" +
"  }\n" +
"  Significant_height_of_combined_wind_waves_and_swell_surface {\n" +
"    String abbreviation \"HTSGW\";\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 10, 0, 3;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Significant height of combined wind waves and swell\";\n" +
"    String Grib_Variable_Id \"VAR_10-0-3_L1\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Significant height of combined wind waves and swell @ Ground or water surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  Significant_height_of_swell_waves_ordered_sequence_of_data {\n" +
"    String abbreviation \"SWELL\";\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 241;\n" +
"    Int32 Grib2_Parameter 10, 0, 8;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Significant height of swell waves\";\n" +
"    String Grib_Variable_Id \"VAR_10-0-8_L241\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Significant height of swell waves @ Ordered Sequence of Data\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_swell_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  Significant_height_of_wind_waves_surface {\n" +
"    String abbreviation \"WVHGT\";\n" +
"    Float64 colorBarMaximum 15.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 10, 0, 5;\n" +
"    String Grib2_Parameter_Category \"Waves\";\n" +
"    String Grib2_Parameter_Discipline \"Oceanographic products\";\n" +
"    String Grib2_Parameter_Name \"Significant height of wind waves\";\n" +
"    String Grib_Variable_Id \"VAR_10-0-5_L1\";\n" +
"    String ioos_category \"Surface Waves\";\n" +
"    String long_name \"Significant height of wind waves @ Ground or water surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"sea_surface_wind_wave_significant_height\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  U_component_of_wind {\n" +
"    String abbreviation \"UGRD\";\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 0, 2, 2;\n" +
"    String Grib2_Parameter_Category \"Momentum\";\n" +
"    String Grib2_Parameter_Discipline \"Meteorological products\";\n" +
"    String Grib2_Parameter_Name \"u-component of wind\";\n" +
"    String Grib_Variable_Id \"VAR_0-2-2_L1\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"u-component of wind @ Ground or water surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"x_wind\";\n" +
"    String units \"m/s\";\n" +
"  }\n" +
"  V_component_of_wind {\n" +
"    String abbreviation \"VGRD\";\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 0, 2, 3;\n" +
"    String Grib2_Parameter_Category \"Momentum\";\n" +
"    String Grib2_Parameter_Discipline \"Meteorological products\";\n" +
"    String Grib2_Parameter_Name \"v-component of wind\";\n" +
"    String Grib_Variable_Id \"VAR_0-2-3_L1\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"v-component of wind @ Ground or water surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"y_wind\";\n" +
"    String units \"m/s\";\n" +
"  }\n" +
"  Wind_direction_from_which_blowing_degree_true_surface {\n" +
"    Float64 colorBarMaximum 360.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Wind\";\n" +
"    String standard_name \"wind_from_direction\";\n" +
"  }\n" +
"  Wind_speed_surface {\n" +
"    String abbreviation \"WIND\";\n" +
"    Float64 colorBarMaximum 20.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String Grib2_Generating_Process_Type \"Forecast\";\n" +
"    Int32 Grib2_Level_Type 1;\n" +
"    Int32 Grib2_Parameter 0, 2, 1;\n" +
"    String Grib2_Parameter_Category \"Momentum\";\n" +
"    String Grib2_Parameter_Discipline \"Meteorological products\";\n" +
"    String Grib2_Parameter_Name \"Wind speed\";\n" +
"    String Grib_Variable_Id \"VAR_0-2-1_L1\";\n" +
"    String ioos_category \"Wind\";\n" +
"    String long_name \"Wind speed @ Ground or water surface\";\n" +
"    Float32 missing_value NaN;\n" +
"    String standard_name \"wind_speed\";\n" +
"    String units \"m/s\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String Analysis_or_forecast_generating_process_identifier_defined_by_originating_centre \"Global Multi-Grid Wave Model (Static Grids)\";\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Easternmost_Easting 359.5;\n" +
"    String file_format \"GRIB-2\";\n" +
"    Float64 geospatial_lat_max 90.0;\n" +
"    Float64 geospatial_lat_min -77.5;\n" +
"    Float64 geospatial_lat_resolution 0.5;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 359.5;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    Float64 geospatial_lon_resolution 0.5;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String GRIB_table_version \"2,1\";\n" +
"    String GRIB_table_version_master_local \"2/1\";\n" +
"    String history \"Read using CDM IOSP Grib2Collection\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);

//+ " (local files)\n" +
//today + 
expected= 
"http://127.0.0.1:8080/cwexperimental/griddap/testGrib2_43.das\";\n" +
"    String infoUrl \"???\";\n" +
"    String institution \"???\";\n" +
"    String keywords \"Atmosphere > Atmospheric Winds > Surface Winds,\n" +
"Oceans > Ocean Waves > Wave Height,\n" +
"Oceans > Ocean Waves > Wave Speed/Direction\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String location \"/erddapTestBig/geosgrib/multi_1.glo_30m.all.grb2\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Northernmost_Northing 90.0;\n" +
"    String Originating_generating_Center \"US National Weather Service, National Centres for Environmental Prediction (NCEP)\";\n" +
"    String Originating_generating_Subcenter \"0\";\n" +
"    String Originating_or_generating_Center \"US National Weather Service, National Centres for Environmental Prediction (NCEP)\";\n" +
"    String Originating_or_generating_Subcenter \"0\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -77.5;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String summary \"???\";\n" +
"    String time_coverage_end \"2009-06-08T18:00:00Z\";\n" +
"    String time_coverage_start \"2009-06-01T06:00:00Z\";\n" +
"    String title \"Test of Grib2\";\n" +
"    String Type_of_generating_process \"Forecast\";\n" +
"    Float64 Westernmost_Easting 0.0;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribEntire_43", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Float64 time[time = 61];\n" +
"  Float32 latitude[latitude = 336];\n" +
"  Float32 longitude[longitude = 720];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Direction_of_swell_waves_degree_true_ordered_sequence_of_data[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Direction_of_swell_waves_degree_true_ordered_sequence_of_data;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Direction_of_wind_waves_degree_true_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Direction_of_wind_waves_degree_true_surface;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Mean_period_of_swell_waves_ordered_sequence_of_data[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Mean_period_of_swell_waves_ordered_sequence_of_data;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Mean_period_of_wind_waves_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Mean_period_of_wind_waves_surface;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Primary_wave_direction_degree_true_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Primary_wave_direction_degree_true_surface;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Primary_wave_mean_period_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Primary_wave_mean_period_surface;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Significant_height_of_combined_wind_waves_and_swell_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Significant_height_of_combined_wind_waves_and_swell_surface;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Significant_height_of_swell_waves_ordered_sequence_of_data[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Significant_height_of_swell_waves_ordered_sequence_of_data;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Significant_height_of_wind_waves_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Significant_height_of_wind_waves_surface;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 U_component_of_wind[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } U_component_of_wind;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 V_component_of_wind[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } V_component_of_wind;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Wind_direction_from_which_blowing_degree_true_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Wind_direction_from_which_blowing_degree_true_surface;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 Wind_speed_surface[time = 61][latitude = 336][longitude = 720];\n" +
"    MAPS:\n" +
"      Float64 time[time = 61];\n" +
"      Float32 latitude[latitude = 336];\n" +
"      Float32 longitude[longitude = 720];\n" +
"  } Wind_speed_surface;\n" +
"} testGrib2_43;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from one file
        String2.log("\n*** .grb test read from one file\n");       
        userDapQuery = "Wind_speed_surface[0][(30)][(200):5:(238)]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_GribData1_43", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time,latitude,longitude,Wind_speed_surface\n" +
"UTC,degrees_north,degrees_east,m/s\n" +
"2009-06-01T06:00:00Z,30.0,200.0,6.17\n" +
"2009-06-01T06:00:00Z,30.0,202.5,6.73\n" +
"2009-06-01T06:00:00Z,30.0,205.0,8.13\n" +
"2009-06-01T06:00:00Z,30.0,207.5,6.7\n" +
"2009-06-01T06:00:00Z,30.0,210.0,4.62\n" +
"2009-06-01T06:00:00Z,30.0,212.5,1.48\n" +
"2009-06-01T06:00:00Z,30.0,215.0,3.03\n" +
"2009-06-01T06:00:00Z,30.0,217.5,4.63\n" +
"2009-06-01T06:00:00Z,30.0,220.0,5.28\n" +
"2009-06-01T06:00:00Z,30.0,222.5,5.3\n" +
"2009-06-01T06:00:00Z,30.0,225.0,4.04\n" +
"2009-06-01T06:00:00Z,30.0,227.5,3.64\n" +
"2009-06-01T06:00:00Z,30.0,230.0,5.3\n" +
"2009-06-01T06:00:00Z,30.0,232.5,2.73\n" +
"2009-06-01T06:00:00Z,30.0,235.0,3.15\n" +
"2009-06-01T06:00:00Z,30.0,237.5,4.23\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //  */
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
"\n2012-07-12 with change to Java 4.3.8, this doesn't pass because of\n" +
"spaces and parens in attribute names. John Caron says he will fix.\n" +
"2013-02-20 better but not all fixed.\n" +
"http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#idp4775248\n" +
"says\n" +
"\"Variable, dimension and attribute names should begin with a letter and be\n" +
"composed of letters, digits, and underscores.\"\n" +
"BUT NOTHING HAS CHANGED!\n" +
"So now generatedDatasetsXml suggests setting original to null, \n" +
"and adds a variant with a valid CF attribute name.\n" +
"Press ^C to stop or Enter to continue..."); 
        }
    }



    /**
     * This tests reading CoastWatch Mercator .hdf files with this class.
     * <br>This also tests dimension without corresponding coordinate axis variable.
     * <br>This file has lots of data variables; this just tests two of them.
     *
     * @throws Throwable if trouble
     */
    public static void testCwHdf(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testCwHdf() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //generateDatasetsXml
        
        //  /*
        String id = "testCwHdf";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml(id); 

        //*** test getting das for entire dataset
        String2.log("\n*** testCwHdf test das dds for entire dataset\n");
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_CwHdfEntire", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Attributes {\n" +
"  rows {\n" +
"    Int32 actual_range 0, 1247;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Rows\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  cols {\n" +
"    Int16 actual_range 0, 1139;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Cols\";\n" +
"    String units \"count\";\n" +
"  }\n" +
"  avhrr_ch1 {\n" +
"    Float64 _FillValue -327.68;\n" +
"    Float64 add_offset_err 0.0;\n" +
"    Int32 calibrated_nt 0;\n" +
"    String coordsys \"Mercator\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"AVHRR Channel 1\";\n" +
"    Float64 missing_value -327.68;\n" +
"    Float64 scale_factor_err 0.0;\n" +
"    String standard_name \"isotropic_spectral_radiance_in_air\";\n" +
"    String units \"percent\";\n" +
"  }\n" +
"  sst {\n" +
"    Float64 _FillValue -327.68;\n" +
"    Float64 add_offset_err 0.0;\n" +
"    Int32 calibrated_nt 0;\n" +
"    Float64 colorBarMaximum 32.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String coordsys \"Mercator\";\n" +
"    Int32 fraction_digits 2;\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Sea Surface Temperature\";\n" +
"    Float64 missing_value -327.68;\n" +
"    Float64 scale_factor_err 0.0;\n" +
"    String sst_equation_day \"nonlinear split window\";\n" +
"    String sst_equation_night \"linear triple window modified\";\n" +
"    String standard_name \"sea_surface_temperature\";\n" +
"    String units \"celsius\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String _History \"Direct read of HDF4 file through CDM library\";\n" +
"    String autonav_performed \"true\";\n" +
"    Int32 autonav_quality 2;\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String history \"Direct read of HDF4 file through CDM library\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "results=\n" + results);
        
//+ " (local files)\n" +
//today + 
    
expected =
" http://127.0.0.1:8080/cwexperimental/griddap/testCwHdf.das\";\n" +
"    String infoUrl \"???\";\n" +
"    String institution \"NOAA CoastWatch\";\n" +
"    String keywords \"Oceans > Ocean Temperature > Sea Surface Temperature\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String origin \"USDOC/NOAA/NESDIS CoastWatch\";\n" +
"    String pass_type \"day\";\n" +
"    String projection \"Mercator\";\n" +
"    String projection_type \"mapped\";\n" +
"    String satellite \"noaa-18\";\n" +
"    String sensor \"avhrr\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String summary \"???\";\n" +
"    String title \"Test of CoastWatch HDF files\";\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_CwHdfEntire", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"Dataset {\n" +
"  Int32 rows[rows = 1248];\n" +
"  Int16 cols[cols = 1140];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 avhrr_ch1[rows = 1248][cols = 1140];\n" +
"    MAPS:\n" +
"      Int32 rows[rows = 1248];\n" +
"      Int16 cols[cols = 1140];\n" +
"  } avhrr_ch1;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 sst[rows = 1248][cols = 1140];\n" +
"    MAPS:\n" +
"      Int32 rows[rows = 1248];\n" +
"      Int16 cols[cols = 1140];\n" +
"  } sst;\n" +
"} testCwHdf;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv  with data from one file
        String2.log("\n*** testCwHdf test read from one file\n");       
        userDapQuery = "sst[600:2:606][500:503]";
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddGrid.className() + "_CwHdfData1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"rows,cols,sst\n" +
"count,count,celsius\n" +
"600,500,21.07\n" +
"600,501,20.96\n" +
"600,502,21.080000000000002\n" +
"600,503,20.93\n" +
"602,500,21.16\n" +
"602,501,21.150000000000002\n" +
"602,502,21.2\n" +
"602,503,20.95\n" +
"604,500,21.34\n" +
"604,501,21.13\n" +
"604,502,21.13\n" +
"604,503,21.25\n" +
"606,500,21.37\n" +
"606,501,21.11\n" +
"606,502,21.0\n" +
"606,503,21.02\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //  */
    }

    /** 
     * This test the speed of all types of responses.
     * This test is in this class because the source data is in a file, 
     * so it has reliable access speed.
     * This gets a pretty big chunk of data.
     *
     * @param whichTest -1 for all, or 0..
     */
    public static void testSpeed(int whichTest) throws Throwable {
        String2.log("\n*** EDDGridFromNcFiles.testSpeed\n" + 
            SgtUtil.isBufferedImageAccelerated() + "\n");
        boolean oReallyVerbose = reallyVerbose;
        reallyVerbose = false;
        String tName;
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("testGriddedNcFiles"); 
        String userDapQuery = "y_wind[(1.1999664e9)][0][][0:719]"; //719 avoids esriAsc cross lon=180
        String dir = EDStatic.fullTestCacheDirectory;
        String extensions[] = new String[]{
            ".asc", ".csv", ".csvp", ".csv0", 
            ".das", ".dds", ".dods", ".esriAscii", 
            ".graph", ".html", ".htmlTable",   //.help not available at this level
            ".json", ".mat", 
            ".nc", ".ncHeader", 
            ".odvTxt", ".tsv", ".tsvp", ".tsv0", ".xhtml", 
            ".geotif", ".kml", 
            ".smallPdf", ".pdf", ".largePdf", 
            ".smallPng", ".png", ".largePng", 
            ".transparentPng"};
        int expectedMs[] = new int[]  {  
            //now Java 1.7/M4700          //was Java 1.6 times            //was java 1.5 times
            187, 905, 811, 800,           //734, 6391, 6312, ?            //1250, 9750, 9562, ?                                  
            15, 15, 109, 8112,            //15, 15, 156, 16875            //15, 15, 547, 18859
            63, 47, 561,                  //63, 47, 2032,                 //93, 31, ...,
            921, 125,                     //6422, 203,                    //9621, 625,  
            121, 121,                     //2015-02 faster: 121. 2014-09 slower 163->331: java? netcdf-java? unsure //234, 250,   //500, 500, 
            1248, 811, 811, 811, 1139,    //9547, 6297, 6281, ?, 8625,    //13278, 8766, 8844, ?, 11469, 
            750, 10,                      //2015-02 kml faster: 10, 2014-09 kml slower 110->258. why?  656, 110,         //687, 94,  //Java 1.7 was 390r until change to new netcdf-Java
            444, 976, 1178,               //860, 2859, 3438,              //2188, 4063, 3797,   //small varies greatly
            160, 378, 492,                //2015-02 faster: 160, 2014-09 png slower 212,300->378. why? //438, 468, 1063,               //438, 469, 1188,     //small varies greatly
            758};                         //1703                          //2359};
        int bytes[]    = new int[]   {
            5875592, 23734053, 23734063, 23733974, 
            6006, 303, 2085486, 4701074, 
            53173, 51428, 14770799, 
            31827797, 2085800, 
            2090600, 5285, 
            24337084, 23734053, 23734063, 23733974, 90604796, 
            523113, 3601, 
            478774, 2189656, 2904880, 
            30852, 76777, 277494, 
            335307};

        //warm up
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_testSpeedw", ".csvp"); 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_testSpeedw", ".png"); 
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
            dir, eddGrid.className() + "_testSpeedw", ".pdf"); 
        
        int firstExt = whichTest < 0? 0 : whichTest;
        int lastExt  = whichTest < 0? extensions.length - 1 : whichTest;
        for (int ext = firstExt; ext <= lastExt; ext++) {
            try {
                String2.log("\n*** EDDGridFromNcFiles.testSpeed test#" + ext + ": " + 
                    extensions[ext] + " speed\n");
                long time = 0, cLength = 0;
                for (int chance = 0; chance < 3; chance++) {
                    Math2.gcAndWait(); //in a test
                    time = System.currentTimeMillis();
                    tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, 
                        dir, eddGrid.className() + 
                        "_testSpeed" + extensions[ext].substring(1) + chance + ext, 
                        extensions[ext]); 
                    time = System.currentTimeMillis() - time;
                    cLength = File2.length(dir + tName);
                    String2.log("\n*** EDDGridFromNcFiles.testSpeed test#" + ext + 
                        " chance#" + chance + ": " + extensions[ext] + " done.\n  " + 
                        cLength + " bytes (" + bytes[ext]+ 
                        ").  time=" + time + " ms (expected=" + expectedMs[ext] + ")\n");
                    Math2.sleep(3000);

                    //if not too slow or too fast, break
                    if (time > 1.5 * Math.max(50, expectedMs[ext]) ||
                        time < (expectedMs[ext] <= 50? 0 : 0.5) * expectedMs[ext]) {
                        //give it another chance
                    } else {
                        break;
                    }
                }

                //size test
                Test.ensureTrue(cLength > 0.9 * bytes[ext], 
                    "File shorter than expected.  observed=" + 
                    cLength + " expected=~" + bytes[ext] +
                    "\n" + dir + tName);
                Test.ensureTrue(cLength < 1.1 * bytes[ext], 
                    "File longer than expected.  observed=" + 
                    cLength + " expected=~" + bytes[ext] +
                    "\n" + dir + tName);

                //time test
                if (time > 1.5 * Math.max(50, expectedMs[ext]))
                    throw new SimpleException(
                        "Slower than expected. observed=" + time + 
                        " expected=~" + expectedMs[ext] + " ms.");
                if (time < (expectedMs[ext] <= 50? 0.1 : 0.5) * expectedMs[ext])
                    throw new SimpleException(
                        "Faster than expected! observed=" + time + 
                        " expected=~" + expectedMs[ext] + " ms.");
            } catch (Exception e) {
                String2.getStringFromSystemIn(
                    MustBe.throwableToString(e) +
                    "\nUnexpected ERROR for Test#" + ext + ": " + extensions[ext]+ 
                    ".  Press ^C to stop or Enter to continue..."); 
            }
        }
        reallyVerbose = oReallyVerbose;
    }

    /** test reading an .hdf file */
    public static void testHdf() throws Throwable {

    }


    /** test reading an .ncml file */
    public static void testNcml() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testNcml");

        //2013-10-25 netcdf-java's reading of the source HDF4 file changed dramatically.
        //  Attributes that had internal spaces now have underscores.
        //  So the previous m4.ncml was making changes to atts that no longer existed
        //  and making changes that no longer need to be made.
        //  So I renamed old version as m4R20131025.ncml and revised m4.ncml.
        String2.log("\nOne of the source files that will be aggregated and modified:\n" + 
            NcHelper.dumpString(
            "/u00/data/viirs/MappedMonthly4km/V20120012012031.L3m_MO_NPP_CHL_chlor_a_4km", 
            false) + "\n");

        String results = Projects.dumpTimeLatLon("/u00/data/viirs/MappedMonthly4km/m4.ncml");
        String expected = 
"ncmlName=/u00/data/viirs/MappedMonthly4km/m4.ncml\n" +
"latitude [0]=89.97916666666667 [1]=89.9375 [4319]=-89.97916666666664\n" +
"longitude [0]=-179.97916666666666 [1]=-179.9375 [8639]=179.97916666666666\n" +
"time [0]=15340 [1]=15371 [1]=15371\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
    }

    /**
     * This tests that ensureValid throws exception if an AxisVariable and 
     * a dataVariable use the same sourceName.
     *
     * @throws Throwable if trouble
     */
    public static void testAVDVSameSource() throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testAVDVSameSource() *****************\n");
        String error = "shouldn't happen";
        try {
            EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("testAVDVSameSource"); 
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            error = String2.split(MustBe.throwableToString(t), '\n')[1]; 
        }

        Test.ensureEqual(error, 
            "Two variables have the same sourceName=OB_time.", 
            "Unexpected error message:\n" + error);
    }

    /**
     * This tests that ensureValid throws exception if 2  
     * dataVariables use the same sourceName.
     *
     * @throws Throwable if trouble
     */
    public static void test2DVSameSource() throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.test2DVSameSource() *****************\n");
        String error = "shouldn't happen";
        try {
            EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("test2DVSameSource"); 
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            error = String2.split(MustBe.throwableToString(t), '\n')[1]; 
        }

        Test.ensureEqual(error, 
            "Two variables have the same sourceName=IB_time.", 
            "Unexpected error message:\n" + error);
    }

    /**
     * This tests that ensureValid throws exception if an AxisVariable and 
     * a dataVariable use the same destinationName.
     *
     * @throws Throwable if trouble
     */
    public static void testAVDVSameDestination() throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testAVDVSameDestination() *****************\n");
        String error = "shouldn't happen";
        try {
            EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("testAVDVSameDestination"); 
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            error = String2.split(MustBe.throwableToString(t), '\n')[1]; 
        }

        Test.ensureEqual(error, 
            "Two variables have the same destinationName=OB_time.", 
            "Unexpected error message:\n" + error);
    }

    /**
     * This tests that ensureValid throws exception if 2  
     * dataVariables use the same destinationName.
     *
     * @throws Throwable if trouble
     */
    public static void test2DVSameDestination() throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.test2DVSameDestination() *****************\n");
        String error = "shouldn't happen";
        try {
            EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("test2DVSameDestination"); 
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
            error = String2.split(MustBe.throwableToString(t), '\n')[1]; 
        }

        Test.ensureEqual(error, 
            "Two variables have the same destinationName=IB_time.", 
            "Unexpected error message:\n" + error);
    }

    /**
     * This tests sub-second time_precision in all output file types.
     *
     * @throws Throwable if trouble
     */
    public static void testTimePrecisionMillis() throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testTimePrecisionMillis() *****************\n");
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("testTimePrecisionMillis"); 
        String tDir = EDStatic.fullTestCacheDirectory;
        String aq = "[(1984-02-01T12:00:59.001Z):1:(1984-02-01T12:00:59.401Z)]";
        String userDapQuery = "ECEF_X" + aq + ",IB_time" + aq;
        String fName = "testTimePrecisionMillis";
        String tName, results, ts, expected;
        int po;

        //Yes. EDDGrid.parseAxisBrackets parses ISO 8601 times to millis precision.
        //I checked with this query and by turning on debug messages in parseAxisBrackets.

        //.asc  
        // !!!!! THIS IS ALSO THE ONLY TEST OF AN IMPORTANT BUG
        // In EDDGridFromFiles could cause 
        //  intermediate results to use data type from another variable,
        //   which could be lesser precision.
        //  (only occurred if vars had different precisions and not first var was requested,
        //  and if less precision mattered (e.g., double->float lost info))  
        // (SEE NOTES 2014-10-20)
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".asc"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"Dataset {\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 ECEF_X[time = 5];\n" +
"    MAPS:\n" +
"      Float64 time[time = 5];\n" +
"  } ECEF_X;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 IB_time[time = 5];\n" +
"    MAPS:\n" +
"      Float64 time[time = 5];\n" +
"  } IB_time;\n" +
"} testTimePrecisionMillis;\n" +
"---------------------------------------------\n" +
"ECEF_X.ECEF_X[5]\n" +
//missing first value is where outer dimension values would be, e.g., [0] (but none here)
", 9.96921E36, 9.96921E36, 9.96921E36, 9.96921E36, 9.96921E36\n" +
"\n" +
"ECEF_X.time[5]\n" +
"4.44484859001E8, 4.44484859101E8, 4.44484859201E8, 4.44484859301E8, 4.4448485940099996E8\n" +
"IB_time.IB_time[5]\n" +
//missing first value is where outer dimension values would be, e.g., [0] (but none here)
//THESE ARE THE VALUES THAT WERE AFFECTED BY THE BUG!!!
", 7.60017659E8, 7.600176591E8, 7.600176592E8, 7.600176593E8, 7.600176594E8\n" +
"\n" +
"IB_time.time[5]\n" +
"4.44484859001E8, 4.44484859101E8, 4.44484859201E8, 4.44484859301E8, 4.4448485940099996E8\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"time,ECEF_X,IB_time\n" +
"UTC,m,UTC\n" +
"1984-02-01T12:00:59.001Z,9.96921E36,1994-01-31T12:00:59.000Z\n" +
"1984-02-01T12:00:59.101Z,9.96921E36,1994-01-31T12:00:59.100Z\n" +
"1984-02-01T12:00:59.201Z,9.96921E36,1994-01-31T12:00:59.200Z\n" +
"1984-02-01T12:00:59.301Z,9.96921E36,1994-01-31T12:00:59.300Z\n" +
"1984-02-01T12:00:59.401Z,9.96921E36,1994-01-31T12:00:59.400Z\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.dods  hard to test

        //.htmlTable
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".htmlTable"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<table class=\"erd commonBGColor\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>time\n" +
"<th>ECEF_X\n" +
"<th>IB_time\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC\n" +
"<th>m\n" +
"<th>UTC\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1984-02-01T12:00:59.001Z\n" +
"<td align=\"right\">9.96921E36\n" +
"<td nowrap>1994-01-31T12:00:59.000Z\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1984-02-01T12:00:59.101Z\n" +
"<td align=\"right\">9.96921E36\n" +
"<td nowrap>1994-01-31T12:00:59.100Z\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1984-02-01T12:00:59.201Z\n" +
"<td align=\"right\">9.96921E36\n" +
"<td nowrap>1994-01-31T12:00:59.200Z\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1984-02-01T12:00:59.301Z\n" +
"<td align=\"right\">9.96921E36\n" +
"<td nowrap>1994-01-31T12:00:59.300Z\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1984-02-01T12:00:59.401Z\n" +
"<td align=\"right\">9.96921E36\n" +
"<td nowrap>1994-01-31T12:00:59.400Z\n" +
"</tr>\n" +
"</table>\n";
        po = results.indexOf("<table class");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.json  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".json"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"time\", \"ECEF_X\", \"IB_time\"],\n" +
"    \"columnTypes\": [\"String\", \"float\", \"String\"],\n" +
"    \"columnUnits\": [\"UTC\", \"m\", \"UTC\"],\n" +
"    \"rows\": [\n" +
"      [\"1984-02-01T12:00:59.001Z\", 9.96921E36, \"1994-01-31T12:00:59.000Z\"],\n" +
"      [\"1984-02-01T12:00:59.101Z\", 9.96921E36, \"1994-01-31T12:00:59.100Z\"],\n" +
"      [\"1984-02-01T12:00:59.201Z\", 9.96921E36, \"1994-01-31T12:00:59.200Z\"],\n" +
"      [\"1984-02-01T12:00:59.301Z\", 9.96921E36, \"1994-01-31T12:00:59.300Z\"],\n" +
"      [\"1984-02-01T12:00:59.401Z\", 9.96921E36, \"1994-01-31T12:00:59.400Z\"]\n" +
"    ]\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.mat  doesn't write strings
//!!! but need to test to ensure not rounding to the nearest second

        //.nc  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".nc"); 
        results = NcHelper.dumpString(tDir + tName, true);
        expected = 
":time_coverage_end = \"1984-02-01T12:00:59.401Z\";\n" + 
"  :time_coverage_start = \"1984-02-01T12:00:59.001Z\";\n" +  
"  :title = \"L1b Magnetometer (MAG) Geomagnetic Field Product\";\n" +
" data:\n" +
"time =\n" +
"  {4.44484859001E8, 4.44484859101E8, 4.44484859201E8, 4.44484859301E8, 4.4448485940099996E8}\n" +
"ECEF_X =\n" +
"  {9.96921E36, 9.96921E36, 9.96921E36, 9.96921E36, 9.96921E36}\n" +
"IB_time =\n" +
"  {7.60017659E8, 7.600176591E8, 7.600176592E8, 7.600176593E8, 7.600176594E8}\n" +  
"}\n";
        po = results.indexOf(":time_coverage_end");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.odvTxt
        /* can't test because it needs lon lat values
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".odvTxt"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        */

        //.xhtml  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".xhtml"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>time</th>\n" +
"<th>ECEF_X</th>\n" +
"<th>IB_time</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC</th>\n" +
"<th>m</th>\n" +
"<th>UTC</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1984-02-01T12:00:59.001Z</td>\n" +
"<td align=\"right\">9.96921E36</td>\n" +
"<td nowrap=\"nowrap\">1994-01-31T12:00:59.000Z</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1984-02-01T12:00:59.101Z</td>\n" +
"<td align=\"right\">9.96921E36</td>\n" +
"<td nowrap=\"nowrap\">1994-01-31T12:00:59.100Z</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1984-02-01T12:00:59.201Z</td>\n" +
"<td align=\"right\">9.96921E36</td>\n" +
"<td nowrap=\"nowrap\">1994-01-31T12:00:59.200Z</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1984-02-01T12:00:59.301Z</td>\n" +
"<td align=\"right\">9.96921E36</td>\n" +
"<td nowrap=\"nowrap\">1994-01-31T12:00:59.300Z</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1984-02-01T12:00:59.401Z</td>\n" +
"<td align=\"right\">9.96921E36</td>\n" +
"<td nowrap=\"nowrap\">1994-01-31T12:00:59.400Z</td>\n" +
"</tr>\n" +
"</table>\n";
        po = results.indexOf("<table ");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);


    }

    /**
     * This tests timestamps and other things.
     *
     * @throws Throwable if trouble
     */
    public static void testSimpleTestNc() throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testSimpleTestNc() *****************\n");
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("testSimpleTestNc"); 
        String tDir = EDStatic.fullTestCacheDirectory;
        String userDapQuery = "hours[1:2],minutes[1:2],seconds[1:2],millis[1:2],bytes[1:2]," +
            "shorts[1:2],ints[1:2],floats[1:2],doubles[1:2],Strings[1:2]";
        String fName = "testSimpleTestNc";
        String tName, results, ts, expected;
        int po;

        String2.log(NcHelper.dumpString(EDStatic.unitTestDataDir + "simpleTest.nc", true));

        //all  
        tName = eddGrid.makeNewFileForDapQuery(null, null, "", tDir, 
            fName, ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"days,hours,minutes,seconds,millis,bytes,shorts,ints,floats,doubles,Strings\n" +
"UTC,UTC,UTC,UTC,UTC,,,,,,\n" +
"1970-01-02T00:00:00Z,1980-01-01T05:00:00Z,1990-01-01T00:09:00Z,2000-01-01T00:00:20Z,2010-01-01T00:00:00.030Z,40,10000,1000000,0.0,1.0E12,0\n" +
"1970-01-03T00:00:00Z,1980-01-01T06:00:00Z,1990-01-01T00:10:00Z,2000-01-01T00:00:21Z,2010-01-01T00:00:00.031Z,41,10001,1000001,1.1,1.0000000000001E12,10\n" +
"1970-01-04T00:00:00Z,1980-01-01T07:00:00Z,1990-01-01T00:11:00Z,2000-01-01T00:00:22Z,2010-01-01T00:00:00.032Z,42,10002,1000002,2.2,1.0000000000002E12,20\n" +
"1970-01-05T00:00:00Z,1980-01-01T08:00:00Z,1990-01-01T00:12:00Z,2000-01-01T00:00:23Z,2010-01-01T00:00:00.033Z,43,10004,1000004,4.4,1.0000000000003E12,30\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.asc  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".asc"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"Dataset {\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 hours[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } hours;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 minutes[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } minutes;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 seconds[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } seconds;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 millis[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } millis;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte bytes[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } bytes;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Int16 shorts[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } shorts;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Int32 ints[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } ints;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float32 floats[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } floats;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 doubles[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } doubles;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      String Strings[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } Strings;\n" +
"} testSimpleTestNc;\n" +
"---------------------------------------------\n" +
"hours.hours[2]\n" +
", 3.155544E8, 3.15558E8\n" +
"\n" +
"hours.days[2]\n" +
"172800.0, 259200.0\n" +
"minutes.minutes[2]\n" +
", 6.311526E8, 6.3115266E8\n" +
"\n" +
"minutes.days[2]\n" +
"172800.0, 259200.0\n" +
"seconds.seconds[2]\n" +
", 9.46684821E8, 9.46684822E8\n" +
"\n" +
"seconds.days[2]\n" +
"172800.0, 259200.0\n" +
"millis.millis[2]\n" +
", 1.262304000031E9, 1.262304000032E9\n" +
"\n" +
"millis.days[2]\n" +
"172800.0, 259200.0\n" +
"bytes.bytes[2]\n" +
", 41, 42\n" +
"\n" +
"bytes.days[2]\n" +
"172800.0, 259200.0\n" +
"shorts.shorts[2]\n" +
", 10001, 10002\n" +
"\n" +
"shorts.days[2]\n" +
"172800.0, 259200.0\n" +
"ints.ints[2]\n" +
", 1000001, 1000002\n" +
"\n" +
"ints.days[2]\n" +
"172800.0, 259200.0\n" +
"floats.floats[2]\n" +
", 1.1, 2.2\n" +
"\n" +
"floats.days[2]\n" +
"172800.0, 259200.0\n" +
"doubles.doubles[2]\n" +
", 1.0000000000001E12, 1.0000000000002E12\n" +
"\n" +
"doubles.days[2]\n" +
"172800.0, 259200.0\n" +
"Strings.Strings[2]\n" +
", 10, 20\n" +
"\n" +
"Strings.days[2]\n" +
"172800.0, 259200.0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"days,hours,minutes,seconds,millis,bytes,shorts,ints,floats,doubles,Strings\n" +
"UTC,UTC,UTC,UTC,UTC,,,,,,\n" +
"1970-01-03T00:00:00Z,1980-01-01T06:00:00Z,1990-01-01T00:10:00Z,2000-01-01T00:00:21Z,2010-01-01T00:00:00.031Z,41,10001,1000001,1.1,1.0000000000001E12,10\n" +
"1970-01-04T00:00:00Z,1980-01-01T07:00:00Z,1990-01-01T00:11:00Z,2000-01-01T00:00:22Z,2010-01-01T00:00:00.032Z,42,10002,1000002,2.2,1.0000000000002E12,20\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.dods  hard to test

        //.htmlTable
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".htmlTable"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<table class=\"erd commonBGColor\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>days\n" +
"<th>hours\n" +
"<th>minutes\n" +
"<th>seconds\n" +
"<th>millis\n" +
"<th>bytes\n" +
"<th>shorts\n" +
"<th>ints\n" +
"<th>floats\n" +
"<th>doubles\n" +
"<th>Strings\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC\n" +
"<th>UTC\n" +
"<th>UTC\n" +
"<th>UTC\n" +
"<th>UTC\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1970-01-03\n" +
"<td nowrap>1980-01-01T06Z\n" +
"<td nowrap>1990-01-01T00:10Z\n" +
"<td nowrap>2000-01-01T00:00:21Z\n" +
"<td nowrap>2010-01-01T00:00:00.031Z\n" +
"<td align=\"right\">41\n" +
"<td align=\"right\">10001\n" +
"<td align=\"right\">1000001\n" +
"<td align=\"right\">1.1\n" +
"<td align=\"right\">1.0000000000001E12\n" +
"<td nowrap>10\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1970-01-04\n" +
"<td nowrap>1980-01-01T07Z\n" +
"<td nowrap>1990-01-01T00:11Z\n" +
"<td nowrap>2000-01-01T00:00:22Z\n" +
"<td nowrap>2010-01-01T00:00:00.032Z\n" +
"<td align=\"right\">42\n" +
"<td align=\"right\">10002\n" +
"<td align=\"right\">1000002\n" +
"<td align=\"right\">2.2\n" +
"<td align=\"right\">1.0000000000002E12\n" +
"<td nowrap>20\n" +
"</tr>\n" +
"</table>\n";
        po = results.indexOf("<table class");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.json  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".json"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"days\", \"hours\", \"minutes\", \"seconds\", \"millis\", \"bytes\", \"shorts\", \"ints\", \"floats\", \"doubles\", \"Strings\"],\n" +
"    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", \"String\", \"byte\", \"short\", \"int\", \"float\", \"double\", \"String\"],\n" +
"    \"columnUnits\": [\"UTC\", \"UTC\", \"UTC\", \"UTC\", \"UTC\", null, null, null, null, null, null],\n" +
"    \"rows\": [\n" +
"      [\"1970-01-03T00:00:00Z\", \"1980-01-01T06:00:00Z\", \"1990-01-01T00:10:00Z\", \"2000-01-01T00:00:21Z\", \"2010-01-01T00:00:00.031Z\", 41, 10001, 1000001, 1.1, 1.0000000000001E12, \"10\"],\n" +
"      [\"1970-01-04T00:00:00Z\", \"1980-01-01T07:00:00Z\", \"1990-01-01T00:11:00Z\", \"2000-01-01T00:00:22Z\", \"2010-01-01T00:00:00.032Z\", 42, 10002, 1000002, 2.2, 1.0000000000002E12, \"20\"]\n" +
"    ]\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.mat  doesn't write strings
//!!! but need to test to ensure not rounding to the nearest second

        //.nc  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".nc"); 
        results = NcHelper.dumpString(tDir + tName, true);
        expected = 
"netcdf testSimpleTestNc.nc {\n" +
"  dimensions:\n" +
"    days = 2;\n" +
"    Strings_strlen = 2;\n" +
"  variables:\n" +
"    double days(days=2);\n" +
"      :actual_range = 172800.0, 259200.0; // double\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Days\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double hours(days=2);\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Hours\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01T00Z\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double minutes(days=2);\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Minutes\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01T00:00Z\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double seconds(days=2);\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Seconds\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"not valid\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    double millis(days=2);\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Millis\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01T00:00:00.000Z\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    byte bytes(days=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    short shorts(days=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    int ints(days=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    float floats(days=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    double doubles(days=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    char Strings(days=2, Strings_strlen=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_data_type = \"Grid\";\n" +
"  :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"  :history = \"";  //2014-10-22T16:16:21Z (local files)\n";
        ts = results.substring(0, expected.length()); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

expected = 
//"2014-10-22T16:16:21Z http://127.0.0.1:8080/cwexperimental
"/griddap/testSimpleTestNc.nc?hours[1:2],minutes[1:2],seconds[1:2],millis[1:2],bytes[1:2],shorts[1:2],ints[1:2],floats[1:2],doubles[1:2],Strings[1:2]\";\n" +
"  :id = \"simpleTest\";\n" +
"  :infoUrl = \"???\";\n" +
"  :institution = \"NOAA NMFS SWFSC ERD\";\n" +
"  :keywords = \"data, local, longs, source, strings\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :Metadata_Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"  :sourceUrl = \"(local files)\";\n" +
"  :standard_name_vocabulary = \"CF-12\";\n" +
"  :summary = \"My summary.\";\n" +
"  :title = \"My Title\";\n" +
" data:\n" +
"days =\n" +
"  {172800.0, 259200.0}\n" +
"hours =\n" +
"  {3.155544E8, 3.15558E8}\n" +
"minutes =\n" +
"  {6.311526E8, 6.3115266E8}\n" +
"seconds =\n" +
"  {9.46684821E8, 9.46684822E8}\n" +
"millis =\n" +
"  {1.262304000031E9, 1.262304000032E9}\n" +
"bytes =\n" +
"  {41, 42}\n" +
"shorts =\n" +
"  {10001, 10002}\n" +
"ints =\n" +
"  {1000001, 1000002}\n" +
"floats =\n" +
"  {1.1, 2.2}\n" +
"doubles =\n" +
"  {1.0000000000001E12, 1.0000000000002E12}\n" +
"Strings =\"10\", \"20\"\n" +
"}\n";
        po = results.indexOf("/griddap/testSimpleTestNc.nc?");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.odvTxt
        /* can't test because it needs lon lat values
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".odvTxt"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        */

        //.xhtml  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".xhtml"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>testSimpleTestNc</title>\n" +
"</head>\n" +
"<body style=\"color:black; background:white; font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n" +
"\n" +
"&nbsp;\n" +
"<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>days</th>\n" +
"<th>hours</th>\n" +
"<th>minutes</th>\n" +
"<th>seconds</th>\n" +
"<th>millis</th>\n" +
"<th>bytes</th>\n" +
"<th>shorts</th>\n" +
"<th>ints</th>\n" +
"<th>floats</th>\n" +
"<th>doubles</th>\n" +
"<th>Strings</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC</th>\n" +
"<th>UTC</th>\n" +
"<th>UTC</th>\n" +
"<th>UTC</th>\n" +
"<th>UTC</th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1970-01-03T00:00:00Z</td>\n" +
"<td nowrap=\"nowrap\">1980-01-01T06:00:00Z</td>\n" +
"<td nowrap=\"nowrap\">1990-01-01T00:10:00Z</td>\n" +
"<td nowrap=\"nowrap\">2000-01-01T00:00:21Z</td>\n" +
"<td nowrap=\"nowrap\">2010-01-01T00:00:00.031Z</td>\n" +
"<td align=\"right\">41</td>\n" +
"<td align=\"right\">10001</td>\n" +
"<td align=\"right\">1000001</td>\n" +
"<td align=\"right\">1.1</td>\n" +
"<td align=\"right\">1.0000000000001E12</td>\n" +
"<td nowrap=\"nowrap\">10</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1970-01-04T00:00:00Z</td>\n" +
"<td nowrap=\"nowrap\">1980-01-01T07:00:00Z</td>\n" +
"<td nowrap=\"nowrap\">1990-01-01T00:11:00Z</td>\n" +
"<td nowrap=\"nowrap\">2000-01-01T00:00:22Z</td>\n" +
"<td nowrap=\"nowrap\">2010-01-01T00:00:00.032Z</td>\n" +
"<td align=\"right\">42</td>\n" +
"<td align=\"right\">10002</td>\n" +
"<td align=\"right\">1000002</td>\n" +
"<td align=\"right\">2.2</td>\n" +
"<td align=\"right\">1.0000000000002E12</td>\n" +
"<td nowrap=\"nowrap\">20</td>\n" +
"</tr>\n" +
"</table>\n" +
"</body>\n" +
"</html>\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //test time on x and y axis
        tName = eddGrid.makeNewFileForDapQuery(null, null, 
            "hours[(1970-01-02):(1970-01-05)]&.draw=linesAndMarkers" +
            "&.vars=days|hours|&.marker=5|5&.color=0x000000&.colorBar=|||||",
            tDir, fName,  ".png"); 
        SSR.displayInBrowser("file://" + tDir + tName);
        String2.log("\n!!!! KNOWN PROBLEM: SgtGraph DOESN'T SUPPORT TWO TIME AXES. !!!!\n" +
            "See SgtGraph \"yIsTimeAxis = false;\".\n");
        Math2.sleep(10000);       

    }

    /**
     * This tests timestamps and other things.
     *
     * @throws Throwable if trouble
     */
    public static void testSimpleTestNc2() throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testSimpleTestNc2() *****************\n");
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetXml("testSimpleTestNc"); 
        String tDir = EDStatic.fullTestCacheDirectory;
        String userDapQuery = "bytes[2:3],doubles[2:3],Strings[2:3]";
        String fName = "testSimpleTestNc2";
        String tName, results, ts, expected;
        int po;

        String2.log(NcHelper.dumpString(EDStatic.unitTestDataDir + "simpleTest.nc", true));

        //.asc  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".asc"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"Dataset {\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Byte bytes[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } bytes;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 doubles[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } doubles;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      String Strings[days = 2];\n" +
"    MAPS:\n" +
"      Float64 days[days = 2];\n" +
"  } Strings;\n" +
"} testSimpleTestNc;\n" +
"---------------------------------------------\n" +
"bytes.bytes[2]\n" +
", 42, 43\n" +
"\n" +
"bytes.days[2]\n" +
"259200.0, 345600.0\n" +
"doubles.doubles[2]\n" +
", 1.0000000000002E12, 1.0000000000003E12\n" +
"\n" +
"doubles.days[2]\n" +
"259200.0, 345600.0\n" +
"Strings.Strings[2]\n" +
", 20, 30\n" +
"\n" +
"Strings.days[2]\n" +
"259200.0, 345600.0\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"days,bytes,doubles,Strings\n" +
"UTC,,,\n" +
"1970-01-04T00:00:00Z,42,1.0000000000002E12,20\n" +
"1970-01-05T00:00:00Z,43,1.0000000000003E12,30\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.dods  doesn't write strings

        //.htmlTable
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".htmlTable"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<table class=\"erd commonBGColor\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>days\n" +
"<th>bytes\n" +
"<th>doubles\n" +
"<th>Strings\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"<th>&nbsp;\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1970-01-04\n" +
"<td align=\"right\">42\n" +
"<td align=\"right\">1.0000000000002E12\n" +
"<td nowrap>20\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap>1970-01-05\n" +
"<td align=\"right\">43\n" +
"<td align=\"right\">1.0000000000003E12\n" +
"<td nowrap>30\n" +
"</tr>\n" +
"</table>\n";
        po = results.indexOf("<table class");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.json  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".json"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"days\", \"bytes\", \"doubles\", \"Strings\"],\n" +
"    \"columnTypes\": [\"String\", \"byte\", \"double\", \"String\"],\n" +
"    \"columnUnits\": [\"UTC\", null, null, null],\n" +
"    \"rows\": [\n" +
"      [\"1970-01-04T00:00:00Z\", 42, 1.0000000000002E12, \"20\"],\n" +
"      [\"1970-01-05T00:00:00Z\", 43, 1.0000000000003E12, \"30\"]\n" +
"    ]\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.mat  doesn't write strings
//!!! but need to test to ensure not rounding to the nearest second

        //.nc  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".nc"); 
        results = NcHelper.dumpString(tDir + tName, true);
        expected = 
"netcdf testSimpleTestNc2.nc {\n" +
"  dimensions:\n" +
"    days = 2;\n" +
"    Strings_strlen = 2;\n" +
"  variables:\n" +
"    double days(days=2);\n" +
"      :actual_range = 259200.0, 345600.0; // double\n" +
"      :ioos_category = \"Time\";\n" +
"      :long_name = \"Days\";\n" +
"      :standard_name = \"time\";\n" +
"      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"      :time_precision = \"1970-01-01\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"\n" +
"    byte bytes(days=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    double doubles(days=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"    char Strings(days=2, Strings_strlen=2);\n" +
"      :ioos_category = \"Unknown\";\n" +
"\n" +
"  // global attributes:\n" +
"  :cdm_data_type = \"Grid\";\n" +
"  :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"  :history = \""; //2014-10-22T16:16:21Z (local files)\n";
        ts = results.substring(0, expected.length()); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

expected = 
//"2014-10-22T16:16:21Z http://127.0.0.1:8080/cwexperimental
"/griddap/testSimpleTestNc.nc?bytes[2:3],doubles[2:3],Strings[2:3]\";\n" +
"  :id = \"simpleTest\";\n" +
"  :infoUrl = \"???\";\n" +
"  :institution = \"NOAA NMFS SWFSC ERD\";\n" +
"  :keywords = \"data, local, longs, source, strings\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"  :Metadata_Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"  :sourceUrl = \"(local files)\";\n" +
"  :standard_name_vocabulary = \"CF-12\";\n" +
"  :summary = \"My summary.\";\n" +
"  :title = \"My Title\";\n" +
" data:\n" +
"days =\n" +
"  {259200.0, 345600.0}\n" +
"bytes =\n" +
"  {42, 43}\n" +
"doubles =\n" +
"  {1.0000000000002E12, 1.0000000000003E12}\n" +
"Strings =\"20\", \"30\"\n" +
"}\n";
        po = results.indexOf("/griddap/testSimpleTestNc.nc?");
        ts = results.substring(Math.max(0, po), Math.min(results.length(), po + expected.length())); 
        Test.ensureEqual(ts, expected, "\nresults=\n" + results);

        //.odvTxt
        /* can't test because it needs lon lat values
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".odvTxt"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        */

        //.xhtml  
        tName = eddGrid.makeNewFileForDapQuery(null, null, userDapQuery, tDir, 
            fName, ".xhtml"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>testSimpleTestNc2</title>\n" +
"</head>\n" +
"<body style=\"color:black; background:white; font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n" +
"\n" +
"&nbsp;\n" +
"<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>days</th>\n" +
"<th>bytes</th>\n" +
"<th>doubles</th>\n" +
"<th>Strings</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>UTC</th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"<th></th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1970-01-04T00:00:00Z</td>\n" +
"<td align=\"right\">42</td>\n" +
"<td align=\"right\">1.0000000000002E12</td>\n" +
"<td nowrap=\"nowrap\">20</td>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\">1970-01-05T00:00:00Z</td>\n" +
"<td align=\"right\">43</td>\n" +
"<td align=\"right\">1.0000000000003E12</td>\n" +
"<td nowrap=\"nowrap\">30</td>\n" +
"</tr>\n" +
"</table>\n" +
"</body>\n" +
"</html>\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
    }

    /** This tests working with hdf file. 
     * @throws Throwable if touble
     */
    public static void testRTechHdf() throws Throwable {

        String2.log("\n*** EDDGridFromNcFiles.testRTechHdf");
        String dir = "c:/data/rtech/";
        String regex = "MT.*\\.hdf";
        String fileName = "MT1_L2-FLUX-SCASL1A2-1.06_2015-01-01T01-42-24_V1-00.hdf";
        String2.log(NcHelper.dumpString(dir + fileName, false));

        String results = generateDatasetsXml(dir, regex, dir + fileName,
            DEFAULT_RELOAD_EVERY_N_MINUTES, null) + "\n";
        String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"zztop\n\n";
        Test.ensureEqual(results, expected, results.length() + " " + expected.length() + 
            "\nresults=\n" + results);

        //ensure it is ready-to-use by making a dataset from it
        EDD edd = oneFromXmlFragment(results);
        Test.ensureEqual(edd.datasetID(), "erdQSwind1day_52db_1ed3_22ce", "");
        Test.ensureEqual(edd.title(), "Wind, QuikSCAT, Global, Science Quality (1 Day Composite)", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "x_wind, y_wind, mod", "");

        String2.log("\nEDDGridFromNcFiles.testGenerateDatasetsXml passed the test.");
    }

    /**
     * This tests the EDDGridFromFiles.update().
     *
     * @throws Throwable if trouble
     */
    public static void testUpdate() throws Throwable {
        String2.log("\n****************** EDDGridFromNcFiles.testUpdate() *****************\n");
        EDDGridFromNcFiles eddGrid = (EDDGridFromNcFiles)oneFromDatasetXml("testGriddedNcFiles"); 
        String dataDir = eddGrid.fileDir;
        String tDir = EDStatic.fullTestCacheDirectory;
        String axisQuery = "time[]";
        String dataQuery = "x_wind[][][100][100]";
        String tName, results, expected;

        //*** read the original data
        String2.log("\n*** read original data\n");       
        tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
            eddGrid.className() + "_update_1a", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        String originalExpectedAxis = 
"time\n" +
"UTC\n" +
"2008-01-01T12:00:00Z\n" +
"2008-01-02T12:00:00Z\n" +
"2008-01-03T12:00:00Z\n" +
"2008-01-04T12:00:00Z\n" +
"2008-01-05T12:00:00Z\n" +
"2008-01-06T12:00:00Z\n" +
"2008-01-07T12:00:00Z\n" +
"2008-01-08T12:00:00Z\n" +
"2008-01-09T12:00:00Z\n" +
"2008-01-10T12:00:00Z\n";      
        Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

        //expected values
        String oldMinTime   = "2008-01-01T12:00:00Z";
        String oldMinMillis = "1.1991888E9";
        String newMinTime   = "2008-01-04T12:00:00Z";
        String newMinMillis = "1.199448E9";
        String oldMaxTime   = "2008-01-10T12:00:00Z";
        String oldMaxMillis = "1.1999664E9";

        tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddGrid.className() + "_update_1d", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        String originalExpectedData = 
"time,altitude,latitude,longitude,x_wind\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"2008-01-01T12:00:00Z,0.0,-64.875,25.125,-3.24724\n" +
"2008-01-02T12:00:00Z,0.0,-64.875,25.125,6.00287\n" +
"2008-01-03T12:00:00Z,0.0,-64.875,25.125,NaN\n" +
"2008-01-04T12:00:00Z,0.0,-64.875,25.125,-3.0695\n" +
"2008-01-05T12:00:00Z,0.0,-64.875,25.125,-7.76223\n" +
"2008-01-06T12:00:00Z,0.0,-64.875,25.125,-12.8834\n" +
"2008-01-07T12:00:00Z,0.0,-64.875,25.125,4.782275\n" +
"2008-01-08T12:00:00Z,0.0,-64.875,25.125,9.80197\n" +
"2008-01-09T12:00:00Z,0.0,-64.875,25.125,-7.5635605\n" +
"2008-01-10T12:00:00Z,0.0,-64.875,25.125,-7.974725\n";      
        Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);

        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMinString(), 
            oldMinTime, "av[0].destinationMin");
        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMaxString(), 
            oldMaxTime, "av[0].destinationMax");
        Test.ensureEqual(eddGrid.axisVariables()[0].combinedAttributes().get("actual_range").toString(),
            oldMinMillis + ", " + oldMaxMillis, "actual_range");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_start"),
            oldMinTime, "time_coverage_start");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_end"),
            oldMaxTime, "time_coverage_end");

        //*** rename a data file so it doesn't match regex
        try {
            String2.log("\n*** rename a data file so it doesn't match regex\n");       
            File2.rename(dataDir, "erdQSwind1day_20080101_03.nc", "erdQSwind1day_20080101_03.nc2");
            for (int i = 0; i < 5; i++) {
                String2.log("after rename .nc to .nc2, update #" + i + " " + eddGrid.update());
                Math2.sleep(1000);
            }

            tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
                eddGrid.className() + "_update_2a", ".csv"); 
            results = new String((new ByteArray(tDir + tName)).toArray());
            expected = 
"time\n" +
"UTC\n" +
"2008-01-04T12:00:00Z\n" +
"2008-01-05T12:00:00Z\n" +
"2008-01-06T12:00:00Z\n" +
"2008-01-07T12:00:00Z\n" +
"2008-01-08T12:00:00Z\n" +
"2008-01-09T12:00:00Z\n" +
"2008-01-10T12:00:00Z\n";      
            Test.ensureEqual(results, expected, "\nresults=\n" + results);

            tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
                eddGrid.className() + "_update_2d", ".csv"); 
            results = new String((new ByteArray(tDir + tName)).toArray());
            expected = 
"time,altitude,latitude,longitude,x_wind\n" +
"UTC,m,degrees_north,degrees_east,m s-1\n" +
"2008-01-04T12:00:00Z,0.0,-64.875,25.125,-3.0695\n" +
"2008-01-05T12:00:00Z,0.0,-64.875,25.125,-7.76223\n" +
"2008-01-06T12:00:00Z,0.0,-64.875,25.125,-12.8834\n" +
"2008-01-07T12:00:00Z,0.0,-64.875,25.125,4.782275\n" +
"2008-01-08T12:00:00Z,0.0,-64.875,25.125,9.80197\n" +
"2008-01-09T12:00:00Z,0.0,-64.875,25.125,-7.5635605\n" +
"2008-01-10T12:00:00Z,0.0,-64.875,25.125,-7.974725\n";      
            Test.ensureEqual(results, expected, "\nresults=\n" + results);

            Test.ensureEqual(eddGrid.axisVariables()[0].destinationMinString(), 
                newMinTime, "av[0].destinationMin");
            Test.ensureEqual(eddGrid.axisVariables()[0].destinationMaxString(), 
                oldMaxTime, "av[0].destinationMax");
            Test.ensureEqual(eddGrid.axisVariables()[0].combinedAttributes().get("actual_range").toString(),
                newMinMillis + ", " + oldMaxMillis, "actual_range");
            Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_start"),
                newMinTime, "time_coverage_start");
            Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_end"),
                oldMaxTime, "time_coverage_end");

        } finally {
            //rename it back to original
            String2.log("\n*** rename it back to original\n");       
            File2.rename(dataDir, "erdQSwind1day_20080101_03.nc2", "erdQSwind1day_20080101_03.nc");
            for (int i = 0; i < 5; i++) {
                String2.log("after rename .nc2 to .nc, update #" + i + " " + eddGrid.update());
                Math2.sleep(1000);
            }
        }

        //*** back to original
        String2.log("\n*** after back to original\n");       
        tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
            eddGrid.className() + "_update_3a", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddGrid.className() + "_update_3d", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);

        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMinString(), 
            oldMinTime, "av[0].destinationMin");
        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMaxString(), 
            oldMaxTime, "av[0].destinationMax");
        Test.ensureEqual(eddGrid.axisVariables()[0].combinedAttributes().get("actual_range").toString(),
            oldMinMillis + ", " + oldMaxMillis, "actual_range");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_start"),
            oldMinTime, "time_coverage_start");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_end"),
            oldMaxTime, "time_coverage_end");

        //*** rename a non-data file so it matches the regex
        try {
            String2.log("\n*** rename an invalid file to be a valid name\n");       
            File2.rename(dataDir, "bad.notnc", "bad.nc");
            for (int i = 0; i < 5; i++) {
                String2.log("after rename .notnc to .nc, update #" + i + " " + eddGrid.update());
                Math2.sleep(1000);
            }

            tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
                eddGrid.className() + "_update_4a", ".csv"); 
            results = new String((new ByteArray(tDir + tName)).toArray());
            Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

            tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
                eddGrid.className() + "_update_4d", ".csv"); 
            results = new String((new ByteArray(tDir + tName)).toArray());
            Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);

            Test.ensureEqual(eddGrid.axisVariables()[0].destinationMinString(), 
                oldMinTime, "av[0].destinationMin");
            Test.ensureEqual(eddGrid.axisVariables()[0].destinationMaxString(), 
                oldMaxTime, "av[0].destinationMax");
            Test.ensureEqual(eddGrid.axisVariables()[0].combinedAttributes().get("actual_range").toString(),
                oldMinMillis + ", " + oldMaxMillis, "actual_range");
            Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_start"),
                oldMinTime, "time_coverage_start");
            Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_end"),
                oldMaxTime, "time_coverage_end");
        } finally {
            //rename it back to original
            String2.log("\n*** rename it back to original\n");       
            File2.rename(dataDir, "bad.nc", "bad.notnc");
            for (int i = 0; i < 5; i++) {
                String2.log("after rename .nc to .notnc, update #" + i + " " + eddGrid.update());
                Math2.sleep(1000);
            }
        }

        String2.log("\n*** after back to original again\n");       
        tName = eddGrid.makeNewFileForDapQuery(null, null, axisQuery, tDir, 
            eddGrid.className() + "_update_5a", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedAxis, "\nresults=\n" + results);

        tName = eddGrid.makeNewFileForDapQuery(null, null, dataQuery, tDir, 
            eddGrid.className() + "_update_5d", ".csv"); 
        results = new String((new ByteArray(tDir + tName)).toArray());
        Test.ensureEqual(results, originalExpectedData, "\nresults=\n" + results);

        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMinString(), 
            oldMinTime, "av[0].destinationMin");
        Test.ensureEqual(eddGrid.axisVariables()[0].destinationMaxString(), 
            oldMaxTime, "av[0].destinationMax");
        Test.ensureEqual(eddGrid.axisVariables()[0].combinedAttributes().get("actual_range").toString(),
            oldMinMillis + ", " + oldMaxMillis, "actual_range");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_start"),
            oldMinTime, "time_coverage_start");
        Test.ensureEqual(eddGrid.combinedGlobalAttributes().getString("time_coverage_end"),
            oldMaxTime, "time_coverage_end");

        //test time for update if 0 events
        long cumTime = 0;
        for (int i = 0; i < 1000; i++) {
            long time = System.currentTimeMillis();
            eddGrid.update();
            cumTime += (System.currentTimeMillis() - time);
            Math2.sleep(2); //updateEveryNMillis=1, so always a valid new update()
        }
        String2.getStringFromSystemIn("time/update() = " + (cumTime/1000.0) +
            "ms (diverse results 0.001 - 11.08ms on Bob's M4700)" +
            "\nNot an error, just FYI. Press ^C to stop or Enter to continue..."); 
    }



    /**
     * This tests this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean deleteCachedDatasetInfo) throws Throwable {
        /* */
        testNc(deleteCachedDatasetInfo);
        testCwHdf(deleteCachedDatasetInfo);
        testHdf();
        testNcml();
        testGrib_43(deleteCachedDatasetInfo);  //42 or 43 for netcdfAll 4.2- or 4.3+
        testGrib2_43(deleteCachedDatasetInfo); //42 or 43 for netcdfAll 4.2- or 4.3+
        testGenerateDatasetsXml();
        testGenerateDatasetsXml2();
        testSpeed(-1);  //-1 = all
        testAVDVSameSource();
        test2DVSameSource();
        testAVDVSameDestination();
        test2DVSameDestination();
        testTimePrecisionMillis();
        testSimpleTestNc();
        testSimpleTestNc2();
//finish this        testRTechHdf();
        testUpdate();
        /* */

        //one time tests
        //String fiName = "/erddapTestBig/geosgrib/multi_1.glo_30m.all.grb2";
        //String2.log(NcHelper.dumpString(fiName, false));
        //NetcdfDataset in = NetcdfDataset.openDataset(fiName);
        //in.close();

    }


}

