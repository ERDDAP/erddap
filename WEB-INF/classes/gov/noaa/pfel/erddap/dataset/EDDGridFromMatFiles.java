/* 
 * THIS CLASS IS UNFINISHED AND INACTIVE. EDDGridFromMatFiles Copyright 2014, NOAA.
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

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.text.MessageFormat;
import java.util.List;



/** 
 * THIS CLASS IS UNFINISHED AND INACTIVE.  
 * This class represents gridded data aggregated from a collection of 
 * Matlab .mat data files
 * (as read by JMatIO http://sourceforge.net/projects/jmatio/).
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2014-01-15
 */
public class EDDGridFromMatFiles extends EDDGridFromFiles { 


    /** The constructor just calls the super constructor. */
    public EDDGridFromMatFiles(String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo, boolean tAccessibleViaWMS,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tAxisVariables,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, String tFileNameRegex, boolean tRecursive, String tPathRegex, 
        String tMetadataFrom, boolean tEnsureAxisValuesAreExactlyEqual, 
        boolean tFileTableInMemory, boolean tAccessibleViaFiles) 
        throws Throwable {

        super("EDDGridFromMatFiles", tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, tAccessibleViaWMS,
            tOnChange, tFgdcFile, tIso19115File, 
            tDefaultDataQuery, tDefaultGraphQuery, 
            tAddGlobalAttributes,
            tAxisVariables,
            tDataVariables,
            tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tEnsureAxisValuesAreExactlyEqual, 
            tFileTableInMemory, tAccessibleViaFiles);
    }


    /**
     * This gets sourceGlobalAttributes and sourceDataAttributes from the specified 
     * source file.
     *
     * @param fileDir
     * @param fileName
     * @param sourceAxisNames If special axis0, this list will be the instances list[1 ... n-1].
     * @param sourceDataNames the names of the desired source data columns.
     * @param sourceDataTypes the data types of the desired source columns 
     *    (e.g., "String" or "float") 
     * @param sourceGlobalAttributes should be an empty Attributes. It will be populated by this method
     * @param sourceAxisAttributes should be an array of empty Attributes. It will be populated by this method
     * @param sourceDataAttributes should be an array of empty Attributes. It will be populated by this method
     * @throws Throwable if trouble (e.g., invalid file, or a sourceAxisName or sourceDataName not found).
     *   If there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public void lowGetSourceMetadata(String fileDir, String fileName, 
        StringArray sourceAxisNames,
        StringArray sourceDataNames, String sourceDataTypes[],
        Attributes sourceGlobalAttributes, 
        Attributes sourceAxisAttributes[],
        Attributes sourceDataAttributes[]) throws Throwable {

/*      REMEMBER to deal with special axis0 

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
            throw new RuntimeException("Error in EDDGridFromMatFiles.getSourceMetadata" +
                "\nwhile getting " + getWhat + 
                "\nfrom " + fileDir + fileName + 
                "\nCause: " + MustBe.throwableToShortString(t),
                t);
        }
        */
    }


    /**
     * This gets source axis values from one file.
     *
     * @param fileDir
     * @param fileName
     * @param sourceAxisNames the names of the desired source axis variables.
     *    If special axis0, this will not include axis0's name.
     * @return a PrimitiveArray[] with the results (with the requested sourceDataTypes).
     *   It needn't set sourceGlobalAttributes or sourceDataAttributes
     *   (but see getSourceMetadata).
     * @throws Throwable if trouble (e.g., invalid file).
     *   If there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public PrimitiveArray[] lowGetSourceAxisValues(String fileDir, String fileName, 
        StringArray sourceAxisNames) throws Throwable {
/*
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
            throw new RuntimeException("Error in EDDGridFromMatFiles.getSourceAxisValues" +
                "\nwhile getting " + getWhat + 
                "\nfrom " + fileDir + fileName + 
                "\nCause: " + MustBe.throwableToShortString(t),
                t);
        }
        */
        return null;
    }

    /**
     * This gets source data from one file.
     *
     * @param fileDir
     * @param fileName
     * @param tDataVariables the desired data variables
     * @param tConstraints  where the first axis variable's constraints
     *   have been customized for this file.
     *   !!! If special axis0, then will not include constraints for axis0.
     * @return a PrimitiveArray[] with an element for each tDataVariable with the dataValues.
     *   <br>The dataValues are straight from the source, not modified.
     *   <br>The primitiveArray dataTypes are usually the sourceDataTypeClass,
     *     but can be any type. EDDGridFromFiles will convert to the sourceDataTypeClass.
     *   <br>Note the lack of axisVariable values!
     * @throws Throwable if trouble (notably, WaitThenTryAgainException).
     *   If there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public PrimitiveArray[] lowGetSourceDataFromFile(String fileDir, String fileName, 
        EDV tDataVariables[], IntArray tConstraints) throws Throwable {

/*
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
                Array array = var.read(selection);
                Object object = NcHelper.getArray(array);
                paa[dvi] = PrimitiveArray.factory(object); 
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
        } */
        return null;
    }

    /**
     * This makes a sibling dataset, based on the new sourceUrl.
     *
     * @throws Throwable always (since this class doesn't support sibling())
     */
    public EDDGrid sibling(String tLocalSourceUrl, int firstAxisToMatch, 
        int matchAxisNDigits, boolean shareInfo) throws Throwable {
        throw new SimpleException("Error: " + 
            "EDDGridFromMatFiles doesn't support method=\"sibling\".");

    }

    /** 
     * This does its best to generate a clean, ready-to-use datasets.xml entry 
     * for an EDDGridFromMatFiles.
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
        int tReloadEveryNMinutes, Attributes externalAddGlobalAttributes)
        throws Throwable {

        String2.log("\n*** EDDGridFromMatFiles.generateDatasetsXml" +
            "\nfileDir=" + tFileDir + " fileNameRegex=" + tfileNameRegex +
            " sampleFileName=" + sampleFileName +
            "\nreloadEveryNMinutes=" + tReloadEveryNMinutes + 
            "\nexternalAddGlobalAttributes=" + externalAddGlobalAttributes);
        if (!String2.isSomething(tFileDir))
            throw new IllegalArgumentException("fileDir wasn't specified.");
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash
        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; //1440 works well with suggestedUpdateEveryNMillis

        if (!String2.isSomething(sampleFileName)) 
            String2.log("Found/using sampleFileName=" +
                (sampleFileName = FileVisitorDNLS.getSampleFileName(
                    tFileDir, tFileNameRegex, true, ".*"))); //recursive, pathRegex

        //make tables to hold variables
        NetcdfFile ncFile = NcHelper.openFile(sampleFileName); //may throw exception
        Table axisSourceTable = new Table();  
        Table dataSourceTable = new Table();  
        Table axisAddTable = new Table();
        Table dataAddTable = new Table();
        StringBuilder sb = new StringBuilder();

        //get source global attributes
        NcHelper.getGlobalAttributes(ncFile, axisSourceTable.globalAttributes());

        try {
            //look at all variables with dimensions, find ones which share same max nDim
            List allVariables = ncFile.getVariables(); 
            int maxDim = 0;
            int nGridsAtSource = 0;
            for (int v = 0; v < allVariables.size(); v++) {
                Variable var = (Variable)allVariables.get(v);
                String varName = var.getShortName();
                List dimensions = var.getDimensions();
                if (dimensions == null || dimensions.size() <= 1) 
                    continue;
                nGridsAtSource++;
                Class tClass = NcHelper.getElementClass(var.getDataType());
                if      (tClass == char.class)    tClass = String.class;
                else if (tClass == boolean.class) tClass = byte.class; 
                PrimitiveArray pa = PrimitiveArray.factory(tClass, 1, false);
                int tDim = dimensions.size() - (tClass == String.class? 1 : 0);
                if (tDim < maxDim) {
                    continue;
                } else if (tDim > maxDim) {
                    //clear previous vars 
                    axisSourceTable.removeAllColumns();
                    dataSourceTable.removeAllColumns();
                    axisAddTable.removeAllColumns();
                    dataAddTable.removeAllColumns();
                    maxDim = tDim;

                    //store the axis vars
                    for (int avi = 0; avi < maxDim; avi++) {
                        String axisName = ((Dimension)dimensions.get(avi)).getName();
                        Variable axisVariable = ncFile.findVariable(axisName);
                        Attributes sourceAtts = new Attributes();
                        if (axisVariable != null) //it will be null for dimension without same-named coordinate axis variable
                            NcHelper.getVariableAttributes(axisVariable, sourceAtts);
                        axisSourceTable.addColumn(avi, axisName, new DoubleArray(), //type doesn't matter
                            sourceAtts); 
                        axisAddTable.addColumn(   avi, axisName, new DoubleArray(), //type doesn't matter
                            makeReadyToUseAddVariableAttributesForDatasetsXml(
                                axisSourceTable.globalAttributes(),
                                sourceAtts, axisName, false, true)); //addColorBarMinMax, tryToFindLLAT

                    }

                } else { 
                    //tDim == maxDim
                    //if axes are different, reject this var
                    boolean ok = true;
                    for (int avi = 0; avi < maxDim; avi++) {
                        String axisName = ((Dimension)dimensions.get(avi)).getName();
                        String expectedName = axisSourceTable.getColumnName(avi);
                        if (!axisName.equals(expectedName)) {
                            if (verbose) String2.log("variable=" + varName + 
                                " has the right nDimensions=" + tDim + 
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
                dataSourceTable.addColumn(dataSourceTable.nColumns(), varName, pa, sourceAtts);
                dataAddTable.addColumn(   dataAddTable.nColumns(),    varName, pa, 
                    makeReadyToUseAddVariableAttributesForDatasetsXml(
                        axisSourceTable.globalAttributes(),
                        sourceAtts, varName, true, false)); //addColorBarMinMax, tryToFindLLAT
            }

            //after dataVariables known, add global attributes in the axisAddTable
            axisAddTable.globalAttributes().set(
                makeReadyToUseAddGlobalAttributesForDatasetsXml(
                    axisSourceTable.globalAttributes(), 
                    "Grid",  //another cdm type could be better; this is ok
                    tFileDir, externalAddGlobalAttributes, 
                    EDD.chopUpCsvAndAdd(axisAddTable.getColumnNamesCSVString(),
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
                "<dataset type=\"EDDGridFromMatFiles\" datasetID=\"" + tDatasetID +                      
                    "\" active=\"true\">\n" +
                "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
                "    <updateEveryNMillis>" + suggestUpdateEveryNMillis(tFileDir) + 
                "</updateEveryNMillis>\n" +  
                "    <fileDir>" + XML.encodeAsXML(tFileDir) + "</fileDir>\n" +
                "    <fileNameRegex>" + XML.encodeAsXML(tFileNameRegex) + "</fileNameRegex>\n" +
                "    <recursive>true</recursive>\n" +
                "    <pathRegex>.*</pathRegex>\n" +
                "    <metadataFrom>last</metadataFrom>\n" +
                "    <fileTableInMemory>false</fileTableInMemory>\n");

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

        String2.log("\n*** EDDGridFromMatFiles.testGenerateDatasetsXml");
        String results = generateDatasetsXml(
            EDStatic.unitTestDataDir + "erdQSwind1day/", ".*_03\\.nc", 
            EDStatic.unitTestDataDir + "erdQSwind1day/erdQSwind1day_20080101_03.nc",
            DEFAULT_RELOAD_EVERY_N_MINUTES, null);

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromMatFiles",
            EDStatic.unitTestDataDir + "erdQSwind1day/", ".*_03\\.nc", 
            EDStatic.unitTestDataDir + "erdQSwind1day/erdQSwind1day_20080101_03.nc",
            "" + DEFAULT_RELOAD_EVERY_N_MINUTES},
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");


        String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDGridFromNcFiles\" datasetID=\"erdQSwind1day_52db_1ed3_22ce\" active=\"true\">\n" +
"    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n" +
"    <fileDir>" + EDStatic.unitTestDataDir + "erdQSwind1day/</fileDir>\n" +
"    <recursive>true</recursive>\n" +
"    <fileNameRegex>.*_03\\.nc</fileNameRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
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
"2009-01-07 http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\n" +
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
"        <att name=\"sourceUrl\">http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day</att>\n" +
"        <att name=\"Southernmost_Northing\" type=\"double\">-89.875</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-1.0</att>\n" +
"        <att name=\"summary\">Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA&#39;s QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meriodonal, and modulus sets. The reference height for all wind velocities is 10 meters.</att>\n" +
"        <att name=\"time_coverage_end\">2008-01-03T12:00:00Z</att>\n" +
"        <att name=\"time_coverage_start\">2008-01-01T12:00:00Z</att>\n" +
"        <att name=\"title\">Wind, QuikSCAT, Global, Science Quality (1 Day Composite)</att>\n" +
"        <att name=\"Westernmost_Easting\" type=\"double\">0.125</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"institution\">NOAA CoastWatch WCN</att>\n" +
"        <att name=\"keywords\">atmosphere,\n" +
"Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" +
"atmospheric, coastwatch, composite, day, global, meridional, modulus, noaa, ocean, oceans,\n" +
"Oceans &gt; Ocean Winds &gt; Surface Winds,\n" +
"quality, quikscat, science, science quality, surface, wcn, wind, winds, x_wind, y_wind, zonal</att>\n" +
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
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //ensure it is ready-to-use by making a dataset from it
        EDD edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.datasetID(), "erdQSwind1day_52db_1ed3_22ce", "");
        Test.ensureEqual(edd.title(), "Wind, QuikSCAT, Global, Science Quality (1 Day Composite)", "");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "x_wind, y_wind, mod", "");

        String2.log("\nEDDGridFromMatFiles.testGenerateDatasetsXml passed the test.");
    }


    /**
     * This tests reading Matlab .mat files with this class.
     *
     * @throws Throwable if trouble
     */
    public static void testMat(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDGridFromMatFiles.testMat() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //  /*
        String id = "testGriddedMatFiles";
        if (deleteCachedDatasetInfo) 
            deleteCachedDatasetInfo(id);
        EDDGrid eddGrid = (EDDGrid)oneFromDatasetsXml(null, id); 

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
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
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

expected = " http://localhost:8080/cwexperimental/griddap/testGriddedNcFiles.das\";\n" +
"    String infoUrl \"http://coastwatch.pfel.noaa.gov/infog/QS_ux10_las.html\";\n" +
"    String institution \"NOAA CoastWatch, West Coast Node\";\n" +
"    String keywords \"EARTH SCIENCE > Oceans > Ocean Winds > Surface Winds\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";\n" +
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
"    String sourceUrl \"http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/1day\";\n" +
"    Float64 Southernmost_Northing -89.875;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v29\";\n" +
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
     * This tests this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean deleteCachedDatasetInfo) throws Throwable {
        /* THIS CLASS IS UNFINISHED AND INACTIVE.
        testNc(deleteCachedDatasetInfo);
        testCwHdf(deleteCachedDatasetInfo);
        testHdf();
        testNcml();
        testGrib_43(deleteCachedDatasetInfo);  //42 or 43 for netcdfAll 4.2- or 4.3+
        testGrib2_43(deleteCachedDatasetInfo); //42 or 43 for netcdfAll 4.2- or 4.3+
        testGenerateDatasetsXml();
        testGenerateDatasetsXml2();
        testSpeed(-1);  //-1 = all
        /* */


    }


}

