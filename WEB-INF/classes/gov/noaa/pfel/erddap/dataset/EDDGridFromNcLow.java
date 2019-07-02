/* 
 * EDDGridFromNcLow Copyright 2015, NOAA.
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
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.text.MessageFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Get netcdfAll-......jar from ftp://ftp.unidata.ucar.edu/pub
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Put it in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
//import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;

/** 
 * This class represents gridded data aggregated from a collection of 
 * NetCDF .nc (https://www.unidata.ucar.edu/software/netcdf/),
 * GRIB .grb (https://en.wikipedia.org/wiki/GRIB),
 * (and related) data files.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2009-01-05
 */
public abstract class EDDGridFromNcLow extends EDDGridFromFiles { 

    /** subclasses have different subClassNames. */
    public String subClassName() {
        return null;
    }

    /** 
     * Subclasses overwrite this: 
     * EDDGridFromNcFilesUnpacked applies scale_factor and add_offset and
     * converts times variables to epochSeconds at a low level (when it reads each file). 
     * Also the &lt;dataType&gt; is applied. */
    public boolean unpack() {
        return false;
    } 


    /** Used by Bob only. Don't set this to true here -- do it in the calling code. */
    public static boolean generateDatasetsXmlCoastwatchErdMode = false;

    /** The constructor just calls the super constructor. */
    public EDDGridFromNcLow(String subclassname, String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo, boolean tAccessibleViaWMS,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tAxisVariables,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes, int tUpdateEveryNMillis,
        String tFileDir, String tFileNameRegex, 
        boolean tRecursive, String tPathRegex, String tMetadataFrom,
        int tMatchAxisNDigits, boolean tFileTableInMemory,
        boolean tAccessibleViaFiles, int tnThreads, boolean tDimensionValuesInMemory, 
        String tCacheFromUrl, int tCacheSizeGB, String tCachePartialPathRegex) 
        throws Throwable {

        super(subclassname, tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo, tAccessibleViaWMS, 
            tOnChange, tFgdcFile, tIso19115File, 
            tDefaultDataQuery, tDefaultGraphQuery, 
            tAddGlobalAttributes,
            tAxisVariables,
            tDataVariables,
            tReloadEveryNMinutes, tUpdateEveryNMillis,
            tFileDir, tFileNameRegex, tRecursive, tPathRegex, tMetadataFrom,
            tMatchAxisNDigits, tFileTableInMemory,
            tAccessibleViaFiles, 
            tnThreads, tDimensionValuesInMemory,
            tCacheFromUrl, tCacheSizeGB, tCachePartialPathRegex);
    }

    /**
     * This gets sourceGlobalAttributes and sourceDataAttributes from the specified 
     * source file.
     *
     * @param tFullName the name of the decompressed data file
     * @param sourceAxisNames If there is a special axis0, this list will be the instances list[1 ... n-1].
     * @param sourceDataNames the names of the desired source data columns.
     * @param sourceDataTypes the data types of the desired source columns 
     *    (e.g., "String" or "float") 
     * @param sourceGlobalAttributes should be an empty Attributes. It will be populated by this method
     * @param sourceAxisAttributes should be an array of empty Attributes. It will be populated by this method
     * @param sourceDataAttributes should be an array of empty Attributes. It will be populated by this method
     * @throws Throwable if trouble (e.g., invalid file, or a sourceAxisName or sourceDataName not found).
     *   If there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public void lowGetSourceMetadata(String tFullName, 
        StringArray sourceAxisNames,
        StringArray sourceDataNames, String sourceDataTypes[],
        Attributes sourceGlobalAttributes, 
        Attributes sourceAxisAttributes[],
        Attributes sourceDataAttributes[]) throws Throwable {

        String getWhat = "globalAttributes";
        NetcdfFile ncFile = NcHelper.openFile(tFullName); //may throw exception
        try {
            NcHelper.getGlobalAttributes(ncFile, sourceGlobalAttributes);

            //This is cognizant of special axis0         
            for (int avi = 0; avi < sourceAxisNames.size(); avi++) {
                getWhat = "axisAttributes for avi=" + avi + " name=" + sourceAxisNames.get(avi);
                Variable var = ncFile.findVariable(sourceAxisNames.get(avi));  
                Attributes tAtts = sourceAxisAttributes[avi];
                if (var == null) {
                    //it will be null for dimensions without corresponding coordinate axis variable
                    tAtts.add("units", "count"); //"count" is udunits;  "index" isn't, but better?
                } else {
                    NcHelper.getVariableAttributes(var, tAtts);

                    //unpack?
                    if (unpack()) {
                        tAtts.unpackVariableAttributes(var.getFullName(), NcHelper.getElementClass(var.getDataType())); 
                        //shouldn't be any mv or fv
                    }
                }
            }

            for (int dvi = 0; dvi < sourceDataNames.size(); dvi++) {
                getWhat = "dataAttributes for dvi=" + dvi + " name=" + sourceDataNames.get(dvi);
                Variable var = ncFile.findVariable(sourceDataNames.get(dvi));  //null if not found
                if (var == null) {
                    String2.log("  var not in file: " + getWhat);
                } else {
                    Attributes tAtts = sourceDataAttributes[dvi];
                    NcHelper.getVariableAttributes(var, tAtts);

                    //unpack?
                    if (unpack()) 
                        tAtts.unpackVariableAttributes(var.getFullName(), NcHelper.getElementClass(var.getDataType()));
                }
            }

            //I care about this exception
            ncFile.close();

        } catch (Throwable t) {
            try {
                ncFile.close(); //make sure it is explicitly closed
            } catch (Throwable t2) {
                //don't care
            }
            throw new RuntimeException("Error in " + subClassName() + ".getSourceMetadata" +
                "\nwhile getting " + getWhat + 
                "\nfrom " + tFullName + 
                "\nCause: " + MustBe.throwableToShortString(t),
                t);
        }
    }


    /**
     * This gets source axis values from one file.
     *
     * @param tFullName
     * @param sourceAxisNames the names of the desired source axis variables.
     *    If there is a special axis0, this will not include axis0's name.
     * @return a PrimitiveArray[] with the results (with the requested sourceDataTypes).
     *   It needn't set sourceGlobalAttributes or sourceDataAttributes
     *   (but see getSourceMetadata).
     * @throws Throwable if trouble (e.g., invalid file).
     *   If there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public PrimitiveArray[] lowGetSourceAxisValues(String tFullName, 
        StringArray sourceAxisNames) throws Throwable {

        String getWhat = "?";
        NetcdfFile ncFile = NcHelper.openFile(tFullName); //may throw exception
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
                    if (unpack()) 
                        avPa[avi] = NcHelper.unpackPA(var, avPa[avi], 
                            true, true); //lookForStringTime, lookForUnsigned
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
            throw new RuntimeException("Error in " + subClassName() + ".getSourceAxisValues" +
                "\nwhile getting " + getWhat + 
                "\nfrom " + tFullName + 
                "\nCause: " + MustBe.throwableToShortString(t),
                t);
        }
    }



    /**
     * This gets source data from one file.
     *
     * @param tFullName
     * @param tDataVariables the desired data variables
     * @param tConstraints 
     *   For each axis variable, there will be 3 numbers (startIndex, stride, stopIndex).
     *   !!! If there is a special axis0, this will not include constraints for axis0.
     * @return a PrimitiveArray[] with an element for each tDataVariable with the dataValues.
     *   <br>The dataValues are straight from the source, not modified.
     *   <br>The primitiveArray dataTypes are usually the sourceDataTypeClass,
     *     but can be any type. EDDGridFromFiles will convert to the sourceDataTypeClass.
     *   <br>Note the lack of axisVariable values!
     * @throws Throwable if trouble (notably, WaitThenTryAgainException).
     *   If there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public PrimitiveArray[] lowGetSourceDataFromFile(String tFullName, 
        EDV tDataVariables[], IntArray tConstraints) throws Throwable {

        //make the selection spec  and get the axis values
        int nav = tConstraints.size() / 3; //deals with special axis0
        int ndv = tDataVariables.length;
        PrimitiveArray[] paa = new PrimitiveArray[ndv];
        StringBuilder selectionSB = new StringBuilder();
        for (int avi = 0; avi < nav; avi++) {
            selectionSB.append((avi == 0? "" : ",") +
                tConstraints.get(avi*3  ) + ":" + 
                tConstraints.get(avi*3+2) + ":" + 
                tConstraints.get(avi*3+1)); //start:STOP:stride !
        }
        String selection = selectionSB.toString();
        int nValues = -1; //not yet calculated
        EDV edv = null;

        NetcdfFile ncFile = NcHelper.openFile(tFullName); //may throw exception
        try {

            for (int dvi = 0; dvi < ndv; dvi++) {
                edv = tDataVariables[dvi];
                Variable var = ncFile.findVariable(edv.sourceName());  
                if (var == null) {
                    //this var isn't in this file: return array of missing_values
                    if (nValues == -1) {
                        nValues = 1;
                        for (int avi = 0; avi < nav; avi++) {
                            nValues *= OpendapHelper.calculateNValues(
                                tConstraints.get(avi*3  ), 
                                tConstraints.get(avi*3+1), //stride
                                tConstraints.get(avi*3+2));
                        }
                    }
                    Class tClass = edv.sourceDataTypeClass(); //appropriate even if unpacked
                    if (tClass == null) {
                        String2.log("source file=" + tFullName);
                        throw new RuntimeException("ERROR: The destinationName=" + 
                            edv.destinationName() + " variable isn't in one of the source files and " +
                            " the variable's sourceDataType wasn't specified.");
                    }
                    paa[dvi] = PrimitiveArray.factory(tClass, nValues, false); //active?
                    paa[dvi].addNDoubles(nValues, 
                        !Double.isNaN(edv.sourceFillValue())? edv.sourceFillValue():
                        edv.sourceMissingValue());
                } else {
                    String tSel = selection;
                    if (edv.sourceDataTypeClass() == String.class) 
                        tSel += ",0:" + (var.getShape(var.getRank() - 1) - 1);
                    Array array = var.read(tSel);
                    Object object = NcHelper.getArray(array);
                    paa[dvi] = PrimitiveArray.factory(object); 
                    //String2.log("!EDDGridFrimNcFiles.getSourceDataFromFile " + tDataVariables[dvi].sourceName() +
                    //    "[" + selection + "]\n" + paa[dvi].toString());

                    if (unpack()) 
                        paa[dvi] = NcHelper.unpackPA(var, paa[dvi], 
                            true, true); //lookForStringTime, lookForUnsigned
                    nValues = paa[dvi].size();
                }
            }

            //I care about this exception
            ncFile.close();
            return paa;

        } catch (Throwable t) {
            //make sure it is explicitly closed
            try {   
                ncFile.close();    
            } catch (Throwable t2) {
                String2.log("Error while trying to close " + tFullName +
                    "\n" + MustBe.throwableToShortString(t2));
            }  

            String2.log("ERROR: while reading sourceName=" +
                (edv == null? "null" : edv.sourceName()) + 
                "[" + selection + "] (start:STOP:stride).");                
            throw t;
        }
    }

    /**
     * This makes a sibling dataset, based on the new sourceUrl.
     *
     * @throws Throwable always (since this class doesn't support sibling())
     */
    public EDDGrid sibling(String tLocalSourceUrl, int firstAxisToMatch, 
        int matchAxisNDigits, boolean shareInfo) throws Throwable {
        throw new SimpleException("Error: " + 
            subClassName() + " doesn't support method=\"sibling\".");

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
    public static String generateDatasetsXml(String subclassname,
        String tFileDir, String tFileNameRegex, String sampleFileName, 
        int tReloadEveryNMinutes, String tCacheFromUrl,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("\n*** " + subclassname + ".generateDatasetsXml" +
            "\nfileDir=" + tFileDir + " fileNameRegex=" + tFileNameRegex +
            " sampleFileName=" + sampleFileName + 
            "\nreloadEveryNMinutes=" + tReloadEveryNMinutes + 
            "\nexternalAddGlobalAttributes=" + externalAddGlobalAttributes);
        boolean tUnpack = "EDDGridFromNcFilesUnpacked".equals(subclassname);
        if (!String2.isSomething(tFileDir))
            throw new IllegalArgumentException("fileDir wasn't specified.");
        if (tFileDir.endsWith("/catalog.html")) //thredds catalog
            tFileDir = tFileDir.substring(0, tFileDir.length() - 12);
        else if (tFileDir.endsWith("/catalog.xml")) //thredds catalog
            tFileDir = tFileDir.substring(0, tFileDir.length() - 11);
        else if (tFileDir.endsWith("/contents.html")) //hyrax catalog
            tFileDir = tFileDir.substring(0, tFileDir.length() - 13);
        else tFileDir = File2.addSlash(tFileDir); //otherwise, assume tFileDir is missing final slash
        tFileNameRegex = String2.isSomething(tFileNameRegex)? 
            tFileNameRegex.trim() : ".*";
        if (String2.isRemote(tCacheFromUrl)) 
            FileVisitorDNLS.sync(tCacheFromUrl, tFileDir, tFileNameRegex,
                true, ".*", false); //not fullSync, so just get 1

        if (!String2.isSomething(sampleFileName)) 
            String2.log("Found/using sampleFileName=" +
                (sampleFileName = FileVisitorDNLS.getSampleFileName(
                    tFileDir, tFileNameRegex, true, ".*"))); //recursive, pathRegex

        String decomSampleFileName = FileVisitorDNLS.decompressIfNeeded(sampleFileName, 
            tFileDir, EDStatic.fullDecompressedGenerateDatasetsXmlDirectory, 
            EDStatic.decompressedCacheMaxGB, false); //reuseExisting
        String2.log("Let's see if netcdf-java can tell us the structure of the sample file:");
        String2.log(NcHelper.ncdump(decomSampleFileName, "-h"));

        StringBuilder sb = new StringBuilder();
        NetcdfFile ncFile = NcHelper.openFile(decomSampleFileName);
        try {

            //make table to hold info
            Table axisSourceTable = new Table();  
            Table dataSourceTable = new Table();  
            Table axisAddTable = new Table();
            Table dataAddTable = new Table();
            double maxTimeES = Double.NaN; //epoch seconds

            //get source global Attributes
            Attributes globalSourceAtts = axisSourceTable.globalAttributes();
            NcHelper.getGlobalAttributes(ncFile, globalSourceAtts);

            //look at all variables with dimensions, find ones which share same max nDim
            List allVariables = ncFile.getVariables(); 
            int maxDim = 0;
            int nGridsAtSource = 0;
            for (int v = 0; v < allVariables.size(); v++) {
                Variable var = (Variable)allVariables.get(v);
                String varName = var.getFullName();
                int slashPo = varName.lastIndexOf('/');
                String groupName = slashPo < 0? "" : varName.substring(0, slashPo + 1);
                List dimensions = var.getDimensions();
                if (dimensions == null || dimensions.size() < 1) 
                    continue;
                nGridsAtSource++;
                Class tClass = NcHelper.getElementClass(var.getDataType());
                if      (tClass == char.class)    tClass = String.class;
                else if (tClass == boolean.class) tClass = byte.class; 
                PrimitiveArray sourcePA = PrimitiveArray.factory(tClass, 1, false);
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
                    maxTimeES = Double.NaN;

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
                            Variable axisVar = ncFile.findVariable(axisName);
                            if (axisVar != null) {//it will be null for dimension without same-named coordinate axis variable
                                NcHelper.getVariableAttributes(axisVar, sourceAtts);
                                if (tUnpack)  
                                    sourceAtts.unpackVariableAttributes(axisVar.getFullName(), NcHelper.getElementClass(axisVar.getDataType()));

                                //if time, try to get maxTimeES
                                String tUnits = sourceAtts.getString("units");
                                if (Calendar2.isNumericTimeUnits(tUnits)) {
                                    try {
                                        double tbf[] = Calendar2.getTimeBaseAndFactor(tUnits); //throws exception
                                        PrimitiveArray tpa = NcHelper.getPrimitiveArray(axisVar);
                                        maxTimeES = Calendar2.unitsSinceToEpochSeconds(
                                            tbf[0], tbf[1], tpa.getDouble(tpa.size() - 1));
                                    } catch (Throwable t) {
                                        String2.log("caught while trying to get maxTimeES: " + 
                                            MustBe.throwableToString(t));
                                    }
                                }
                            }
                        }
                        axisSourceTable.addColumn(avi, axisName, new DoubleArray(), //type doesn't matter
                            sourceAtts); 
                        axisAddTable.addColumn(   avi, axisName, new DoubleArray(), //type doesn't matter
                            makeReadyToUseAddVariableAttributesForDatasetsXml(
                                globalSourceAtts,
                                sourceAtts, null, axisName, 
                                true, //tryToAddStandardName
                                false, true)); //addColorBarMinMax, tryToFindLLAT
                    }

                } else { 
                    //nDim == maxDim
                    //if axes are different, reject this var
                    boolean ok = true;
                    for (int avi = 0; avi < maxDim; avi++) {
                        String axisName = ((Dimension)dimensions.get(avi)).getFullName();
                        String expectedName = axisSourceTable.getColumnName(avi);
                        if (axisName == null || !axisName.equals(expectedName)) {
                            if (verbose) String2.log("variable=" + varName + 
                                " has the right nDimensions=" + nDim + 
                                ", but axis#=" + avi + "=" + axisName + 
                                " != " + expectedName);
                            ok = false;
                            break;
                        }
                    }
                }

                //add the dataVariable
                Attributes sourceAtts = new Attributes();
                NcHelper.getVariableAttributes(var, sourceAtts);
                if (tUnpack) {
                    sourcePA = sourceAtts.unpackPA(var.getFullName(), sourcePA, 
                        true, true); //lookForStringTime, lookForUnsigned
                    sourceAtts.unpackVariableAttributes(  //after unpackPA
                        var.getFullName(), NcHelper.getElementClass(var.getDataType()));
                }
                dataSourceTable.addColumn(dataSourceTable.nColumns(), varName, sourcePA, 
                    sourceAtts);
                PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts);
                Attributes destAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                    globalSourceAtts, sourceAtts, null, varName, 
                    destPA.elementClass() != String.class, //tryToAddStandardName
                    destPA.elementClass() != String.class, //addColorBarMinMax
                    false); //tryToFindLLAT
                dataAddTable.addColumn(   dataAddTable.nColumns(),    varName, destPA, destAtts);

                //add missing_value and/or _FillValue if needed
                addMvFvAttsIfNeeded(varName, sourcePA, sourceAtts, destAtts); //sourcePA since strongly typed

            }

            if (dataAddTable.nColumns() == 0)
                throw new RuntimeException("No dataVariables found.");

            //after dataVariables known, add global attributes in the axisAddTable
            Attributes globalAddAtts = axisAddTable.globalAttributes();
            globalAddAtts.set(
                makeReadyToUseAddGlobalAttributesForDatasetsXml(
                    globalSourceAtts, 
                    "Grid",  //another cdm type could be better; this is ok
                    tFileDir, externalAddGlobalAttributes, 
                    EDD.chopUpCsvAndAdd(axisAddTable.getColumnNamesCSVString(),
                        suggestKeywords(dataSourceTable, dataAddTable))));

            //gather the results 
            String tDatasetID = suggestDatasetID(tFileDir + tFileNameRegex);
            boolean accViaFiles = false;
            int tMatchNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;

            if (generateDatasetsXmlCoastwatchErdMode) {
                accViaFiles = true;
                tMatchNDigits = 15;
                //  /u00/satellite/AT/ssta/1day/
                Pattern pattern = Pattern.compile("/u00/satellite/([^/]+)/([^/]+)/([^/]+)day/");
                Matcher matcher = pattern.matcher(tFileDir); 
                String m1, m12, m1_2; //ATssta  AT_ssta
                String cl; //composite length
                if (matcher.matches()) {
                    m1 = matcher.group(1);
                    m12 = matcher.group(1) + matcher.group(2);
                    m1_2 = matcher.group(1) + "_" + matcher.group(2);
                    cl = matcher.group(3);
                } else {
                    //  /u00/satellite/MPIC/1day/
                    pattern = Pattern.compile("/u00/satellite/([^/]+)/([^/]+)day/");
                    matcher = pattern.matcher(tFileDir); 
                    if (matcher.matches()) {
                        m1 = matcher.group(1);
                        m12 = matcher.group(1);
                        m1_2 = m12;
                        cl = matcher.group(2);
                    } else {
                        throw new RuntimeException(tFileDir + " doesn't match the pattern!");
                    }
                }

                tDatasetID = "erd" + m12 + cl + "day";
                if (!"MH1".equals(m1)) {
                    globalAddAtts.set("creator_name", "NOAA NMFS SWFSC ERD");
                    globalAddAtts.set("creator_email", "erd.data@noaa.gov");
                    globalAddAtts.set("creator_url", "https://www.pfeg.noaa.gov");
                    globalAddAtts.set("institution", "NOAA NMFS SWFSC ERD");
                }
                globalAddAtts.set("publisher_name", "NOAA NMFS SWFSC ERD");
                globalAddAtts.set("publisher_email", "erd.data@noaa.gov");
                globalAddAtts.set("publisher_url", "https://www.pfeg.noaa.gov");
                globalAddAtts.set("id", tDatasetID); //2019-05-07 was "null");
                globalAddAtts.set("infoUrl", "https://coastwatch.pfeg.noaa.gov/infog/" +
                    m1_2 + "_las.html");
                globalAddAtts.set("license", "[standard]");
                globalAddAtts.remove("summary");
                globalAddAtts.set("title", 
                    globalSourceAtts.getString("title") + " (" + 
                    (cl.equals("h")? "Single Scan" : 
                     cl.equals("m")? "Monthly Composite" : 
                                     cl + " Day Composite") + 
                    ")");

                for (int dv = 0; dv < dataSourceTable.nColumns(); dv++) {
                    dataAddTable.columnAttributes(dv).set("long_name", "!!! FIX THIS !!!");
                    if (dataSourceTable.columnAttributes(dv).get("actual_range") != null)
                           dataAddTable.columnAttributes(dv).set("actual_range", "null");
                }

            }

            if (nGridsAtSource > dataAddTable.nColumns())
                sb.append(generateDatasetsXmlCoastwatchErdMode? "":
                    "<!-- NOTE! The source for " + tDatasetID + " has nGridVariables=" + nGridsAtSource + ",\n" +
                    "  but this dataset will only serve " + dataAddTable.nColumns() + 
                    " because the others use different dimensions. -->\n");

            //tryToFindLLAT 
            tryToFindLLAT(   axisSourceTable, axisAddTable); //just axisTables
            ensureValidNames(dataSourceTable, dataAddTable);

            //use maxTimeES
            if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
                tReloadEveryNMinutes = 1440; //1440 works well with suggestedUpdateEveryNMillis

            String tTestOutOfDate = EDD.getAddOrSourceAtt(
                globalAddAtts, globalSourceAtts, "testOutOfDate", null);
            if (Double.isFinite(maxTimeES) && !String2.isSomething(tTestOutOfDate)) {
                tTestOutOfDate = suggestTestOutOfDate(maxTimeES);
                if (String2.isSomething(tTestOutOfDate))
                    globalAddAtts.set("testOutOfDate", tTestOutOfDate);
            }

            //write results
            sb.append(
                "<dataset type=\"" + subclassname + "\" datasetID=\"" + tDatasetID +                      
                    "\" active=\"true\">\n" +
                "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
                (String2.isUrl(tCacheFromUrl)? 
                  "    <cacheFromUrl>" + XML.encodeAsXML(tCacheFromUrl) + "</cacheFromUrl>\n" :
                  "    <updateEveryNMillis>" + suggestUpdateEveryNMillis(tFileDir) + "</updateEveryNMillis>\n") +  
                "    <fileDir>" + XML.encodeAsXML(tFileDir) + "</fileDir>\n" +
                "    <fileNameRegex>" + XML.encodeAsXML(tFileNameRegex) + "</fileNameRegex>\n" +
                "    <recursive>true</recursive>\n" +
                "    <pathRegex>.*</pathRegex>\n" +
                "    <metadataFrom>last</metadataFrom>\n" +
                "    <matchAxisNDigits>" + tMatchNDigits + "</matchAxisNDigits>\n" +
                "    <fileTableInMemory>false</fileTableInMemory>\n" +
                "    <accessibleViaFiles>" + accViaFiles + "</accessibleViaFiles>\n");

            sb.append(writeAttsForDatasetsXml(false, globalSourceAtts, "    "));
            sb.append(writeAttsForDatasetsXml(true,  globalAddAtts,    "    "));
            
            //last 2 params: includeDataType, questionDestinationName
            sb.append(writeVariablesForDatasetsXml(axisSourceTable, axisAddTable, "axisVariable", false, false));
            sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, "dataVariable", true,  false));
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


}

