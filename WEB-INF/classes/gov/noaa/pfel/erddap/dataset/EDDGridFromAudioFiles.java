/* 
 * EDDGridFromAudioFiles Copyright 2017, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents gridded data from an audio file
 * where all files have the same number of samples and 
 * where the leftmost dimension is created from the fileName.
 * See Table.readAudio file for info on which types of audio files can be read.
 * https://docs.oracle.com/javase/tutorial/sound/converters.html
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2017-09-11
 */
public class EDDGridFromAudioFiles extends EDDGridFromFiles {


    /**
     * The constructor just calls the super constructor.
     */
    public EDDGridFromAudioFiles(String tDatasetID, 
            String tAccessibleTo, String tGraphsAccessibleTo, boolean tAccessibleViaWMS,
            StringArray tOnChange, String tFgdcFile, String tIso19115File,
            String tDefaultDataQuery, String tDefaultGraphQuery,
            Attributes tAddGlobalAttributes,
            Object[][] tAxisVariables,
            Object[][] tDataVariables,
            int tReloadEveryNMinutes, int tUpdateEveryNMillis, String tFileDir, 
            String tFileNameRegex, boolean tRecursive, String tPathRegex, 
            String tMetadataFrom, int tMatchAxisNDigits, 
            boolean tFileTableInMemory, boolean tAccessibleViaFiles, 
            int tnThreads, boolean tDimensionValuesInMemory, 
            String tCacheFromUrl, int tCacheSizeGB, String tCachePartialPathRegex)
            throws Throwable {

        super("EDDGridFromAudioFiles", tDatasetID, 
                tAccessibleTo, tGraphsAccessibleTo, tAccessibleViaWMS,
                tOnChange, tFgdcFile, tIso19115File,
                tDefaultDataQuery, tDefaultGraphQuery,
                tAddGlobalAttributes,
                tAxisVariables,
                tDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis,
                tFileDir, tFileNameRegex, tRecursive, tPathRegex, 
                tMetadataFrom, tMatchAxisNDigits, 
                tFileTableInMemory, tAccessibleViaFiles, 
                tnThreads, tDimensionValuesInMemory, 
                tCacheFromUrl, tCacheSizeGB, tCachePartialPathRegex);

        if (verbose) String2.log("\n*** constructing EDDGridFromAudioFiles(xmlReader)...");        
    }

    /**
     * This gets sourceGlobalAttributes and sourceDataAttributes from the
     * specified source file.
     *
     * @param tFullName the name of the decompressed data file
     * @param sourceAxisNames If there is a special axis0, this list will be the instances list[1 ... n-1].
     * @param sourceDataNames the names of the desired source data columns.
     * @param sourceDataTypes the data types of the desired source data columns
     *   (e.g., "String" or "float")
     * @param sourceGlobalAttributes should be an empty Attributes. It will be
     *   populated by this method
     * @param sourceAxisAttributes should be an array of empty Attributes. It
     *   will be populated by this method
     * @param sourceDataAttributes should be an array of empty Attributes. It
     *   will be populated by this method
     * @throws Throwable if trouble (e.g., invalid file, or a sourceAxisName or
     *   sourceDataName not found). If there is trouble, this doesn't call
     *   addBadFile or requestReloadASAP().
     */
    public void lowGetSourceMetadata(String tFullName,
            StringArray sourceAxisNames,
            StringArray sourceDataNames, 
            String sourceDataTypes[],
            Attributes sourceGlobalAttributes,
            Attributes sourceAxisAttributes[],
            Attributes sourceDataAttributes[]) throws Throwable {
        
        if (reallyVerbose) String2.log("getSourceMetadata " + tFullName);
        
        Table table = new Table();
        table.readAudioFile(tFullName, false, true); //readData? addElapsedTime?

        //globalAtts
        sourceGlobalAttributes.add(table.globalAttributes());

        //axisVariables
        if (sourceAxisNames.size() != 1)
            throw new SimpleException("SourceAxisNames size=" + 
                sourceAxisNames.size() + ", but expected=1.");
        Test.ensureEqual(sourceAxisNames.get(0), Table.ELAPSED_TIME,
            "Unexpected sourceAxisName[0].");
        sourceAxisAttributes[0].add(table.columnAttributes(0));

        //dataVariables
        int ndni = sourceDataNames.size();
        for (int dni = 0; dni < ndni; dni++) {
            String name = sourceDataNames.get(dni);
            int tc = table.findColumnNumber(name);
            if (tc < 0)
                throw new SimpleException("There is no sourceDataName=" + name +
                   ". The available names are [" + table.getColumnNamesCSSVString() + "].");
            sourceDataAttributes[dni].add(table.columnAttributes(tc));
            Test.ensureEqual(table.getColumn(tc).elementClassString(), 
                sourceDataTypes[dni], "Unexpected source dataType for sourceDataName=" +
                name + ".");
        }
    }

    /**
     * This gets source axis values from one file.
     *
     * @param tFullName the name of the decompressed data file
     * @param sourceAxisNames the names of the desired source axis variables.
     *   If there is a special axis0, this will not include axis0's name.
     * @return a PrimitiveArray[] with the results (with the requested
     *   sourceDataTypes). It needn't set sourceGlobalAttributes or
     *   sourceDataAttributes (but see getSourceMetadata).
     * @throws Throwable if trouble (e.g., invalid file). If there is trouble,
     * this doesn't call addBadFile or requestReloadASAP().
     */
    public PrimitiveArray[] lowGetSourceAxisValues(String tFullName,
            StringArray sourceAxisNames) throws Throwable {

        //for this class, only elapsedTime is available
        if (sourceAxisNames.size() != 1 ||
            !sourceAxisNames.get(0).equals(Table.ELAPSED_TIME))
            throw new SimpleException("The only sourceAxisName available from audio files is " +
                Table.ELAPSED_TIME + ", not [" + sourceAxisNames.toJsonCsvString() + "].");
        
        try {
            PrimitiveArray[] avPa = new PrimitiveArray[1];

            Table table = new Table();
            table.readAudioFile(tFullName, true, true); //readData? addElapsedTime?
            avPa[0] = table.getColumn(0);
           
            return avPa;

        } catch (Throwable t) {
            throw new RuntimeException("Error in EDDGridFromAudioFiles.getSourceAxisValues"
                + "\nfrom " + tFullName
                + "\nCause: " + MustBe.throwableToShortString(t),
                t);
        }
    }

    /**
     * This gets source data from one file.
     *
     * @param fullFileName
     * @param tDataVariables the desired data variables
     * @param tConstraints 
     *   For each axis variable, there will be 3 numbers (startIndex, stride, stopIndex).
     *   !!! If there is a special axis0, this will not include constraints for axis0.
     * @return a PrimitiveArray[] with an element for each tDataVariable with
     *   the dataValues.
     *   <br>The dataValues are straight from the source, not modified.
     *   <br>The primitiveArray dataTypes are usually the sourceDataTypeClass, but
     *   can be any type. EDDGridFromFiles will convert to the
     *   sourceDataTypeClass.
     *   <br>Note the lack of axisVariable values!
     * @throws Throwable if trouble (notably, WaitThenTryAgainException). If
     *   there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public PrimitiveArray[] lowGetSourceDataFromFile(String fullFileName,
            EDV tDataVariables[], IntArray tConstraints) throws Throwable {
        
        if (verbose) String2.log("getSourceDataFromFile(" + fullFileName + 
            ", [" + String2.toCSSVString(tDataVariables) + "], " + tConstraints + ")");

        //for this class, elapsedTime must be constrained
        if (tConstraints.size() != 3)
            throw new SimpleException("The only sourceAxisName available from audio files is " +
                Table.ELAPSED_TIME + ", not [" + sourceAxisNames.toJsonCsvString() + "].");
        int start  = tConstraints.get(0);
        int stride = tConstraints.get(1);
        int stop   = tConstraints.get(2);
        int howManyRows = PrimitiveArray.strideWillFind(stop - start + 1, stride);

        Table table = new Table();
        table.readAudioFile(fullFileName, true, false); //readData? addElapsedTime?
        int ndv = tDataVariables.length;
        PrimitiveArray paa[] = new PrimitiveArray[ndv];
        for (int dvi = 0; dvi < ndv; dvi++) {
            EDV edv = tDataVariables[dvi];
            int col = table.findColumnNumber(edv.sourceName());
            if (col >= 0) {
                paa[dvi] = table.getColumn(col).subset(start, stride, stop);
                table.setColumn(col, new IntArray(1, false)); //small, to save memory
            } else {
                //make a pa with missing values
                double mv = edv.sourceFillValue();
                if (Double.isNaN(mv))
                    mv = edv.sourceMissingValue();
                paa[dvi] = PrimitiveArray.factory(
                    edv.sourceDataTypeClass(), howManyRows, "" + mv);
            }
        }            
        return paa;
    }
    
    /**
     * This makes a sibling dataset, based on the new sourceUrl.
     *
     * @throws Throwable always (since this class doesn't support sibling())
     */
    public EDDGrid sibling(String tLocalSourceUrl, int firstAxisToMatch,
        int matchAxisNDigits, boolean shareInfo) throws Throwable {
        throw new SimpleException(
            "Error: EDDGridFromAudioFiles doesn't support method=\"sibling\".");

    }

       
    /** 
     * This does its best to generate a clean, ready-to-use datasets.xml entry 
     * for an EDDGridFromAudioFiles.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * <p>This can't be made into a web service because it would allow any user
     * to looks at (possibly) private audio files on the server.
     *
     * @param tFileDir the starting (parent) directory for searching for files
     * @param tFileNameRegex  the regex that each filename (no directory info) must match 
     *    (e.g., ".*\\.nc")  (usually only 1 backslash; 2 here since it is Java code). 
     * @param sampleFileName full file name of one of the files in the collection.
     *    If "", this will pick a file.
     * @param extractFileNameRegex The regex to extract info from the fileName
     *    to create axisVariable[0].
     * @param extractDataType This may be a data type (eg, int, double, String)
     *    or a timeFormat (e.g., yyyyMMddHHmmss).
     * @param extractColumnName the name for the column created by the fileName
     *    extract, e.g., time.
     * @param tReloadEveryNMinutes, 

     * @param externalAddGlobalAttributes  These are given priority. Use null if none available.
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(
        String tFileDir, String tFileNameRegex, String sampleFileName, 
        String extractFileNameRegex,
        String extractDataType,
        String extractColumnName,
        int tReloadEveryNMinutes, String tCacheFromUrl,
        Attributes externalAddGlobalAttributes) throws Throwable {

        String2.log("\n*** EDDGridFromAudioFiles.generateDatasetsXml" +
            "\nfileDir=" + tFileDir + " fileNameRegex=" + tFileNameRegex +
            " sampleFileName=" + sampleFileName + 
            "\nreloadEveryNMinutes=" + tReloadEveryNMinutes + 
            "\nexternalAddGlobalAttributes=" + externalAddGlobalAttributes);
        if (!String2.isSomething(tFileDir))
            throw new IllegalArgumentException("fileDir wasn't specified.");
        if (!String2.isSomething(extractFileNameRegex))
            throw new IllegalArgumentException("extractFileNameRegex wasn't specified.");
        if (!String2.isSomething(extractDataType))
            throw new IllegalArgumentException("extractDataType wasn't specified.");
        if (!String2.isSomething(extractColumnName))
            throw new IllegalArgumentException("extractColumnName wasn't specified.");

        tFileDir = File2.addSlash(tFileDir); 
        tFileNameRegex = String2.isSomething(tFileNameRegex)? 
            tFileNameRegex.trim() : ".*";
        if (String2.isRemote(tCacheFromUrl)) 
            FileVisitorDNLS.sync(tCacheFromUrl, tFileDir, tFileNameRegex,
                true, ".*", false); //not fullSync

        if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; //1440 works well with suggestedUpdateEveryNMillis

        if (!String2.isSomething(sampleFileName)) 
            String2.log("Found/using sampleFileName=" +
                (sampleFileName = FileVisitorDNLS.getSampleFileName(
                    tFileDir, tFileNameRegex, true, ".*"))); //recursive, pathRegex

        String2.log("Let's see if we get the structure of the sample file:");
        Table table = new Table();
        String decomSampleFileName = FileVisitorDNLS.decompressIfNeeded(sampleFileName, 
            tFileDir, EDStatic.fullDecompressedGenerateDatasetsXmlDirectory, 
            EDStatic.decompressedCacheMaxGB, false); //reuseExisting
        table.readAudioFile(decomSampleFileName, false, true); //getData, addElapsedTimeColumn
        String2.log(table.toString(0));
        int nCols = table.nColumns();

        //make tables to hold info
        Table axisSourceTable = new Table();  
        Table dataSourceTable = new Table();  
        Table axisAddTable = new Table();
        Table dataAddTable = new Table();
        StringBuilder sb = new StringBuilder();

        //get source global Attributes
        Attributes globalSourceAtts = table.globalAttributes();

        //add axisVariables
        String varName = extractColumnName;
        Attributes sourceAtts = new Attributes();
        Attributes addAtts = (new Attributes()).
            add("units", "seconds since 1970-01-01T00:00:00Z");
        Class tClass = null;
        if (extractDataType.startsWith("timeFormat=")) {
            tClass = String.class;
        } else {
            tClass = PrimitiveArray.elementStringToClass(extractDataType);
        }
        PrimitiveArray pa = PrimitiveArray.factory(tClass, 1, false);
        axisSourceTable.addColumn(0, 
            //***fileName,dataType,extractRegex,captureGroupNumber 
            "***fileName," + String2.toJson(extractDataType) + "," + 
                String2.toJson(extractFileNameRegex) + ",1",  
            pa, sourceAtts);
        axisAddTable.addColumn(   0, varName, pa, addAtts);

        //default queries
        String et = Table.ELAPSED_TIME;
        String defGraph = "channel_1[0][(0):(1)]" +
            "&.draw=lines&.vars=" + et + "|" + extractColumnName;
        String defData  = "&time=min(time)";

        varName = table.getColumnName(0); //Table.ELAPSED_TIME
        sourceAtts = table.columnAttributes(0);
        addAtts = new Attributes();
        if (EDStatic.variablesMustHaveIoosCategory)
            addAtts.set("ioos_category", "Time");
        pa = table.getColumn(0);
        axisSourceTable.addColumn(1, varName, pa, sourceAtts);
        axisAddTable.addColumn(   1, varName, pa, addAtts);
       
        //add the dataVariables
        for (int col = 1; col < nCols; col++) {
            varName = table.getColumnName(col);
            pa = table.getColumn(col);
            sourceAtts = table.columnAttributes(col);
            addAtts = new Attributes();
            if (EDStatic.variablesMustHaveIoosCategory)
                addAtts.set("ioos_category", "Other");
            if (pa.isIntegerType()) {
                addAtts
                    .add("colorBarMinimum", Math2.niceDouble(-pa.missingValue(), 2))
                    .add("colorBarMaximum", Math2.niceDouble( pa.missingValue(), 2));
            }
            dataSourceTable.addColumn(col - 1, varName, pa, sourceAtts);
            dataAddTable.addColumn(   col - 1, varName, pa, addAtts);

            //add missing_value and/or _FillValue if needed
            addMvFvAttsIfNeeded(varName, pa, sourceAtts, addAtts);

        }

        //after dataVariables known, add global attributes in the axisAddTable
        Attributes globalAddAtts = axisAddTable.globalAttributes();
        globalAddAtts.set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                globalSourceAtts, 
                "Grid",  //another cdm type could be better; this is ok
                tFileDir, externalAddGlobalAttributes, 
                EDD.chopUpCsvAndAdd(axisAddTable.getColumnNamesCSVString(),
                    suggestKeywords(dataSourceTable, dataAddTable))));
        if ("Data from a local source.".equals(globalAddAtts.getString("summary")))
            globalAddAtts.set("summary", "Audio data from a local source.");
        if ("Data from a local source.".equals(globalAddAtts.getString("title")))
            globalAddAtts.set("title",   "Audio data from a local source.");


        //gather the results 
        String tDatasetID = suggestDatasetID(tFileDir + tFileNameRegex);
        boolean accViaFiles = false;
        int tMatchNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;

        ensureValidNames(dataSourceTable, dataAddTable);

        //write results
        sb.append(
            "<dataset type=\"EDDGridFromAudioFiles\" datasetID=\"" + tDatasetID +                      
                "\" active=\"true\">\n" +
            "    <defaultGraphQuery>" + XML.encodeAsXML(defGraph) + "</defaultGraphQuery>\n" +
            "    <defaultDataQuery>" + XML.encodeAsXML(defData) + "</defaultDataQuery>\n" +
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
            "    <dimensionValuesInMemory>false</dimensionValuesInMemory>\n" +
            "    <fileTableInMemory>false</fileTableInMemory>\n" +
            "    <accessibleViaFiles>false</accessibleViaFiles>\n");

        sb.append(writeAttsForDatasetsXml(false, globalSourceAtts, "    "));
        sb.append(writeAttsForDatasetsXml(true,  globalAddAtts,    "    "));
        
        //last 2 params: includeDataType, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(axisSourceTable, axisAddTable, "axisVariable", false, false));
        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, "dataVariable", true,  false));
        sb.append(
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");

        return sb.toString();        
    }


    /** 
     * This tests generateDatasetsXml. 
     * The test files are from Jim Potemra 
     * http://oos.soest.hawaii.edu/erddap/files/aco_acoustic/
     * Note that the first 3 files (for :00, :05, :10 minutes) have float data,
     *   seem to be have almost all ~0,
     *   whereas the :15 and :20 files have audible short data. 
     * I'm using the first 3 files here.
     */
    public static void testGenerateDatasetsXml() throws Throwable {

        String2.log("\n*** EDDGridFromAudioFiles.testGenerateDatasetsXml");

        String dir = EDStatic.unitTestDataDir + "audio/floatWav"; //test omit trailing slash
        String regex = "aco_acoustic\\.[0-9]{8}_[0-9]{6}\\.wav";
        String extractFileNameRegex = "aco_acoustic\\.([0-9]{8}_[0-9]{6})\\.wav";
        String extractDataType = "timeFormat=yyyyMMdd'_'HHmmss";
        String extractColumnName = "time";

        String results = generateDatasetsXml(
            dir, regex, "", 
            extractFileNameRegex, extractDataType, extractColumnName,
            -1, null, null) + "\n";

        String expected = 
"<dataset type=\"EDDGridFromAudioFiles\" datasetID=\"floatWav_1fc2_a353_7a36\" active=\"true\">\n" +
"    <defaultGraphQuery>channel_1[0][(0):(1)]&amp;.draw=lines&amp;.vars=elapsedTime|time</defaultGraphQuery>\n" +
"    <defaultDataQuery>&amp;time=min(time)</defaultDataQuery>\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/erddapTest/audio/floatWav/</fileDir>\n" +
"    <fileNameRegex>aco_acoustic\\.[0-9]{8}_[0-9]{6}\\.wav</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <dimensionValuesInMemory>false</dimensionValuesInMemory>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"audioBigEndian\">false</att>\n" +
"        <att name=\"audioChannels\" type=\"int\">1</att>\n" +
"        <att name=\"audioEncoding\">PCM_FLOAT</att>\n" +
"        <att name=\"audioFrameRate\" type=\"float\">24000.0</att>\n" +
"        <att name=\"audioFrameSize\" type=\"int\">4</att>\n" +
"        <att name=\"audioSampleRate\" type=\"float\">24000.0</att>\n" +
"        <att name=\"audioSampleSizeInBits\" type=\"int\">32</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"infoUrl\">???</att>\n" +
"        <att name=\"institution\">???</att>\n" +
"        <att name=\"keywords\">channel, channel_1, data, elapsedTime, local, source, time</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v55</att>\n" +
"        <att name=\"summary\">Audio data from a local source.</att>\n" +
"        <att name=\"title\">Audio data from a local source.</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>***fileName,\"timeFormat=yyyyMMdd'_'HHmmss\",\"aco_acoustic\\\\.([0-9]{8}_[0-9]{6})\\\\.wav\",1</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>elapsedTime</sourceName>\n" +
"        <destinationName>elapsedTime</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Elapsed Time</att>\n" +
"            <att name=\"units\">seconds</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>channel_1</sourceName>\n" +
"        <destinationName>channel_1</destinationName>\n" +
"        <dataType>float</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Channel 1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Other</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, 
            "results.length=" + results.length() + " expected.length=" + expected.length() + 
            "\nresults=\n" + results);

        //GenerateDatasetsXml
        results = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromAudioFiles", dir, regex, "", 
            extractFileNameRegex, extractDataType, extractColumnName,
            "-1", ""}, //default reloadEvery, cacheFromUrl
            false); //doIt loop?

        Test.ensureEqual(results, expected, 
            "Unexpected results from GenerateDatasetsXml.doIt. " + 
            "results.length=" + results.length() + " expected.length=" + expected.length());

        //ensure it is ready-to-use by making a dataset from it
        deleteCachedDatasetInfo("floatWav_1fc2_a353_7a36");
        EDDGrid eddg = (EDDGrid)oneFromXmlFragment(null, results);
        Test.ensureEqual(eddg.className(), "EDDGridFromAudioFiles", "className");
        Test.ensureEqual(eddg.title(), "Audio data from a local source.", "title");
        Test.ensureEqual(String2.toCSSVString(
            eddg.axisVariableDestinationNames()), 
            "time, elapsedTime", "axisVariableDestinationNames");
        Test.ensureEqual(String2.toCSSVString(
            eddg.dataVariableDestinationNames()), 
            "channel_1", "dataVariableDestinationNames");

        String2.log("\nEDDGridFromAudioFiles.testGenerateDatasetsXml passed the test.");
    }


    /** 
     * This tests generateDatasetsXml. 
     * The test files are from Jim Potemra 
     * http://oos.soest.hawaii.edu/erddap/files/aco_acoustic/
     * Note that the first 3 files (for :00, :05, :10 minutes) have float data,
     *   seem to be have almost all ~0,
     *   whereas the :15 and :20 files have audible short data. 
     * I'm using the latter 2 files here.
     */
    public static void testGenerateDatasetsXml2() throws Throwable {

        String2.log("\n*** EDDGridFromAudioFiles.testGenerateDatasetsXml2");

        //test files are from Jim Potemra 
        //http://oos.soest.hawaii.edu/erddap/files/aco_acoustic/
        //Note that the first 3 files (for :00, :05, :10 minutes) have float data,
        //  seem to be have almost all ~0,
        //  whereas the :15 and :20 files have audible short data. 
        //I'm using the latter 2 files here.

        String dir = EDStatic.unitTestDataDir + "audio/wav"; //test omit trailing slash
        String regex = "aco_acoustic\\.[0-9]{8}_[0-9]{6}\\.wav";
        String extractFileNameRegex = "aco_acoustic\\.([0-9]{8}_[0-9]{6})\\.wav";
        String extractDataType = "timeFormat=yyyyMMdd'_'HHmmss";
        String extractColumnName = "time";

        String results = generateDatasetsXml(
            dir, regex, "", 
            extractFileNameRegex, extractDataType, extractColumnName,
            -1, null, null) + "\n";

        String expected = 
"<dataset type=\"EDDGridFromAudioFiles\" datasetID=\"wav_b2fb_4111_6998\" active=\"true\">\n" +
"    <defaultGraphQuery>channel_1[0][(0):(1)]&amp;.draw=lines&amp;.vars=elapsedTime|time</defaultGraphQuery>\n" +
"    <defaultDataQuery>&amp;time=min(time)</defaultDataQuery>\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>/erddapTest/audio/wav/</fileDir>\n" +
"    <fileNameRegex>aco_acoustic\\.[0-9]{8}_[0-9]{6}\\.wav</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <matchAxisNDigits>20</matchAxisNDigits>\n" +
"    <dimensionValuesInMemory>false</dimensionValuesInMemory>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <accessibleViaFiles>false</accessibleViaFiles>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"audioBigEndian\">false</att>\n" +
"        <att name=\"audioChannels\" type=\"int\">1</att>\n" +
"        <att name=\"audioEncoding\">PCM_SIGNED</att>\n" +
"        <att name=\"audioFrameRate\" type=\"float\">96000.0</att>\n" +
"        <att name=\"audioFrameSize\" type=\"int\">2</att>\n" +
"        <att name=\"audioSampleRate\" type=\"float\">96000.0</att>\n" +
"        <att name=\"audioSampleSizeInBits\" type=\"int\">16</att>\n" +
"    </sourceAttributes -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"infoUrl\">???</att>\n" +
"        <att name=\"institution\">???</att>\n" +
"        <att name=\"keywords\">channel, channel_1, data, elapsedTime, local, source, time</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v55</att>\n" +
"        <att name=\"summary\">Audio data from a local source.</att>\n" +
"        <att name=\"title\">Audio data from a local source.</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>***fileName,\"timeFormat=yyyyMMdd'_'HHmmss\",\"aco_acoustic\\\\.([0-9]{8}_[0-9]{6})\\\\.wav\",1</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>elapsedTime</sourceName>\n" +
"        <destinationName>elapsedTime</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Elapsed Time</att>\n" +
"            <att name=\"units\">seconds</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>channel_1</sourceName>\n" +
"        <destinationName>channel_1</destinationName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">Channel 1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">33000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-33000.0</att>\n" +
"            <att name=\"ioos_category\">Other</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, 
            "results.length=" + results.length() + " expected.length=" + expected.length() + 
            "\nresults=\n" + results);

        //GenerateDatasetsXml
        results = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromAudioFiles", dir, regex, "", 
            extractFileNameRegex, extractDataType, extractColumnName,
            "-1", ""}, //default reloadEvery, cacheFromUrl
            false); //doIt loop?

        Test.ensureEqual(results, expected, 
            "Unexpected results from GenerateDatasetsXml.doIt. " + 
            "results.length=" + results.length() + " expected.length=" + expected.length());

        //ensure it is ready-to-use by making a dataset from it
        deleteCachedDatasetInfo("wav_b2fb_4111_6998");
        EDDGrid eddg = (EDDGrid)oneFromXmlFragment(null, results);
        Test.ensureEqual(eddg.className(), "EDDGridFromAudioFiles", "className");
        Test.ensureEqual(eddg.title(), "Audio data from a local source.", "title");
        Test.ensureEqual(String2.toCSSVString(
            eddg.axisVariableDestinationNames()), 
            "time, elapsedTime", "axisVariableDestinationNames");
        Test.ensureEqual(String2.toCSSVString(
            eddg.dataVariableDestinationNames()), 
            "channel_1", "dataVariableDestinationNames");

        String2.log("\nEDDGridFromAudioFiles.testGenerateDatasetsXml2 passed the test.");
    }


    /** This tests this class. */
    public static void testBasic(boolean deleteCachedDatasetInfo) throws Throwable {

        String2.log("\n*** EDDGridFromAudioFiles.testBasic(" + 
            deleteCachedDatasetInfo + ")\n");
        testVerboseOn();
        EDDGrid.debugMode = true;
        EDV.debugMode = true; //to see dimensionValuesInMemory diagnostic messages
        //delete cached info
        if (deleteCachedDatasetInfo)
            deleteCachedDatasetInfo("testGridWav");
        EDDGrid edd   = (EDDGrid)oneFromDatasetsXml(null, "testGridWav");
        String dir = EDStatic.fullTestCacheDirectory;
        String tName, results, expected, dapQuery;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);


        //.dds
        expected = 
"Dataset {\n" +
"  Float64 time[time = 2];\n" +
"  Float64 elapsedTime[elapsedTime = 28800000];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Int16 channel_1[time = 2][elapsedTime = 28800000];\n" +
"    MAPS:\n" +
"      Float64 time[time = 2];\n" +
"      Float64 elapsedTime[elapsedTime = 28800000];\n" +
"  } channel_1;\n" +
"} testGridWav;\n";
        tName = edd.makeNewFileForDapQuery(null, null, "", 
            dir, edd.className() + "_", ".dds"); 
        results = String2.directReadFrom88591File(dir + tName);
        Test.ensureEqual(results, expected, "results=\n" + results);

        //*** .das
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.4163561e+9, 1.4163564e+9;\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  elapsedTime {\n" +
"    Float64 actual_range 0.0, 299.99998958333333;\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Elapsed Time\";\n" +
"    String units \"seconds\";\n" +
"  }\n" +
"  channel_1 {\n" +
"    Float64 colorBarMaximum 33000.0;\n" +
"    Float64 colorBarMinimum -33000.0;\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Channel 1\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String audioBigEndian \"false\";\n" +
"    Int32 audioChannels 1;\n" +
"    String audioEncoding \"PCM_SIGNED\";\n" +
"    Float32 audioFrameRate 96000.0;\n" +
"    Int32 audioFrameSize 2;\n" +
"    Float32 audioSampleRate 96000.0;\n" +
"    Int32 audioSampleSizeInBits 16;\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String defaultDataQuery \"&time=min(time)\";\n" +
"    String defaultGraphQuery \"channel_1[0][(0):(1)]&.draw=lines&.vars=elapsedTime|time\";\n" +
"    String history \"" + today;

        tName = edd.makeNewFileForDapQuery(null, null, "", 
            dir, edd.className() + "_", ".das"); 
        results = String2.directReadFrom88591File(dir + tName);
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

expected = "http://localhost:8080/cwexperimental/griddap/testGridWav.das\";\n" +
"    String infoUrl \"???\";\n" +
"    String institution \"???\";\n" +
"    String keywords \"channel_1, data, elapsedTime, local, source, time\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v55\";\n" +
"    String summary \"Audio data from a local source.\";\n" +
"    String time_coverage_end \"2014-11-19T00:20:00Z\";\n" +
"    String time_coverage_start \"2014-11-19T00:15:00Z\";\n" +
"    String title \"Audio data from a local source.\";\n" +
"  }\n" +
"}\n"; 

        po = results.indexOf(expected.substring(0, 40));
        Test.ensureEqual(results.substring(po), expected, "results=\n" + results);


        //find some data
        /* 
        String dir2 = EDStatic.unitTestDataDir + "audio/wav/"; 
        String name = "aco_acoustic.20141119_001500.wav";
        Table table = new Table();
        table.readAudioFile(dir2 + name, true, false); //getData, addElapsedTimeColumn
        int nRows = table.nRows();
        PrimitiveArray pa = table.getColumn(0);
        int count = 0;
        for (int row = 0; row < nRows; row++) {
            if (Math.abs(pa.getInt(row)) > 10000) {                
                String2.log("[" + row + "]=" + pa.getInt(row));
                if (count++ > 100)
                    break;
            }
        }
        table = null;
        /* */
        
        
        //*** data
        dapQuery = "channel_1[1][344855:344864]";
        expected = 
"time,elapsedTime,channel_1\n" +
"UTC,seconds,\n" +
"2014-11-19T00:20:00Z,3.5922395833333334,-10026\n" +
"2014-11-19T00:20:00Z,3.59225,-9807\n" +
"2014-11-19T00:20:00Z,3.5922604166666665,-10025\n" +
"2014-11-19T00:20:00Z,3.5922708333333335,-10092\n" +
"2014-11-19T00:20:00Z,3.59228125,-10052\n" +
"2014-11-19T00:20:00Z,3.5922916666666667,-9832\n" +
"2014-11-19T00:20:00Z,3.5923020833333332,-9652\n" +
"2014-11-19T00:20:00Z,3.5923125,-9758\n" +
"2014-11-19T00:20:00Z,3.592322916666667,-9800\n" +
"2014-11-19T00:20:00Z,3.5923333333333334,-9568\n";
        tName = edd.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, edd.className() + "_", ".csv"); 
        results = String2.directReadFrom88591File(dir + tName);
        Test.ensureEqual(results, expected, "results=\n" + results);

        //*** data
        dapQuery = "channel_1[0][0:(15)]";  //15 seconds
        tName = edd.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, edd.className() + "_", ".wav"); 
        Table table = new Table();
        table.readAudioFile(dir + tName, true, true);
        results = table.toString(10);
        expected = 
"{\n" +
"dimensions:\n" +
"\trow = 1440001 ;\n" +
"variables:\n" +
"\tdouble elapsedTime(row) ;\n" +
"\t\telapsedTime:long_name = \"Elapsed Time\" ;\n" +
"\t\telapsedTime:units = \"seconds\" ;\n" +
"\tshort channel_1(row) ;\n" +
"\t\tchannel_1:long_name = \"Channel 1\" ;\n" +
"\n" +
"// global attributes:\n" +
"\t\t:audioBigEndian = \"false\" ;\n" +
"\t\t:audioChannels = 1 ;\n" +
"\t\t:audioEncoding = \"PCM_SIGNED\" ;\n" +
"\t\t:audioFrameRate = 96000.0f ;\n" +
"\t\t:audioFrameSize = 2 ;\n" +
"\t\t:audioSampleRate = 96000.0f ;\n" +
"\t\t:audioSampleSizeInBits = 16 ;\n" +
"}\n" +
"elapsedTime,channel_1\n" +
"0.0,-7217\n" +
"1.0416666666666666E-5,-7255\n" +
"2.0833333333333333E-5,-7462\n" +
"3.125E-5,-7404\n" +
"4.1666666666666665E-5,-7456\n" +
"5.208333333333334E-5,-7529\n" +
"6.25E-5,-7157\n" +
"7.291666666666667E-5,-7351\n" +
"8.333333333333333E-5,-7388\n" +
"9.375E-5,-7458\n" +
"...\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);
        SSR.displayInBrowser("file://" + dir + tName);        
        String2.pressEnterToContinue("Close audio player when done.");
        File2.delete(dir + tName);

        String2.log("\n*** EDDGridFromAudioFiles.testBasic(" + 
            deleteCachedDatasetInfo + ") finished successfully");
        EDDGrid.debugMode = true;
        EDV.debugMode = false;
    }

    /**
     * This was used one time to fix some test files.
     */
    public static void oneTimeFixFiles() throws Exception {
        Table table = new Table();
        String dir = EDStatic.unitTestDataDir + "audio/wav/"; 
        String name1 = "aco_acoustic.20141119_00";
        String name2 = "00.wav";

        for (int min = 0; min <= 10; min += 5) {
            String name = name1 + String2.zeroPad("" + min, 2) + name2;
            table.readAudioFile(dir + name, true, false); //getData, addElapsedTimeColumn
            String2.log(name + " nRows=" + table.nRows() + " time=" + (table.nRows()/24000.0));
        }
    }

    /** This tests byte range requests to /files/ */
    public static void testByteRangeRequest() throws Throwable {

        String2.log("\n*** EDDGridFromAudioFiles.testByteRangeRequest\n");
        testVerboseOn();

        String results, results2, expected;
        ArrayList al;
        List list;
        int po;
        int timeOutSeconds = 120;
        String reqBase = "curl http://localhost:8080/cwexperimental/";
        String req = reqBase + "files/testGridWav/aco_acoustic.20141119_001500.wav -i "; //-i includes header in response

        // * request no byte range
        al = SSR.dosShell(req, timeOutSeconds);
        list = al.subList(0, 8);
        results = String2.annotatedString(String2.toNewlineString(list.toArray()));
expected = 
//"[10]\n" +
//"c:\\programs\\_tomcat\\webapps\\cwexperimental\\WEB-INF>call C:\\programs\\curl7600\\I386\\curl.exe http://localhost:8080/cwexperimental/files/testGridWav/aco_acoustic.20141119_001500.wav -i        [10]\n" +
"HTTP/1.1 200 [10]\n" +  //200=SC_OK
"Content-Encoding: identity[10]\n" +
"Accept-ranges: bytes[10]\n" +
"Content-Type: audio/wav[10]\n" +
"Content-Length: 57600044[10]\n" +
"Date: "; //Wed, 27 Sep 2017 18:08:20 GMT[10]\n" +
        results2 = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(results2, expected, "results2=\n" + results2);


        // * request short byte range
        al = SSR.dosShell(req + "-H \"Range: bytes=0-30\"", timeOutSeconds);
        list = al.subList(0, 8);
        results = String2.annotatedString(String2.toNewlineString(list.toArray()));
expected = 
"HTTP/1.1 206 [10]\n" +  //206=SC_PARTIAL_CONTENT
"Content-Encoding: identity[10]\n" +
"Content-Range: bytes 0-30/57600044[10]\n" +
"Content-Type: audio/wav[10]\n" +
"Content-Length: 31[10]\n" +
"Date: "; //Wed, 27 Sep 2017 18:08:20 GMT[10]\n" +
        results2 = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(results2, expected, "results2=\n" + results2);


        // * request  bytes=0-  which is what <audio> seems to do
        al = SSR.dosShell(req + "-H \"Range: bytes=0-\"", timeOutSeconds);
        list = al.subList(0, 8);
        results = String2.annotatedString(String2.toNewlineString(list.toArray()));
expected = 
"HTTP/1.1 206 [10]\n" +  //206=SC_PARTIAL_CONTENT
"Content-Encoding: identity[10]\n" +
"Content-Range: bytes 0-57600043/57600044[10]\n" +
"Content-Type: audio/wav[10]\n" +
"Content-Length: 57600044[10]\n" +
"Date: "; //Wed, 27 Sep 2017 18:08:20 GMT[10]\n" +
        results2 = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(results2, expected, "results2=\n" + results2);

        // * request  bytes=[start]-  which is what <audio> seems to do
        al = SSR.dosShell(req + "-H \"Range: bytes=50000000-\"", timeOutSeconds);
        list = al.subList(0, 8);
        results = String2.annotatedString(String2.toNewlineString(list.toArray()));
expected = 
"HTTP/1.1 206 [10]\n" +  //206=SC_PARTIAL_CONTENT
"Content-Encoding: identity[10]\n" +
"Content-Range: bytes 50000000-57600043/57600044[10]\n" +
"Content-Type: audio/wav[10]\n" +
"Content-Length: 7600044[10]\n" +
"Date: "; //Wed, 27 Sep 2017 18:08:20 GMT[10]\n" +
        results2 = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(results2, expected, "results2=\n" + results2);

        // * request  images/wz_tooltip.js
        al = SSR.dosShell(reqBase + "images/wz_tooltip.js -i", timeOutSeconds);
        list = al.subList(0, 5);
        results = String2.annotatedString(String2.toNewlineString(list.toArray()));
expected = 
"HTTP/1.1 200 [10]\n" +
"Cache-Control: PUBLIC, max-age=604800, must-revalidate[10]\n" +
"Expires: ";
        results2 = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(results2, expected, "results2=\n" + results2);

        list = al.subList(4, 10);
        results = String2.annotatedString(String2.toNewlineString(list.toArray()));
expected = 
"Content-Encoding: identity[10]\n" +
"Accept-ranges: bytes[10]\n" +
"Content-Type: application/x-javascript;charset=UTF-8[10]\n" +
"Content-Length: 36673[10]\n" +
"Date: ";
        results2 = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(results2, expected, "results2=\n" + results2);

    }

    /** This tests this class. */
    public static void test() throws Throwable {
/* for releases, this line should have open/close comment */
        testGenerateDatasetsXml();
        testGenerateDatasetsXml2();
        //oneTimeFixFiles();  
        testBasic(true);
        testBasic(false);
        testByteRangeRequest();
        /* */
    }

}

