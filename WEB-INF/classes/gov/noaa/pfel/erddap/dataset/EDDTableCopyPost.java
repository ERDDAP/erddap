/* 
 * EDDTableCopyPost Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
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
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.TaskThread;
import gov.noaa.pfel.erddap.variable.*;

import java.util.ArrayList;
import java.util.Enumeration;


/** 
 * This class makes and maintains a local copy of the data from a remote source.
 * This class serves data from the local copy.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2009-05-19
 */
public class EDDTableCopyPost extends EDDTableCopy { 


    //inactive: protected String variableDescriptionTable = null;

    /**
     * This constructs an EDDTableCopyPost based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableCopy"&gt; 
     *    having just been read.  
     * @return an EDDTableCopy.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableCopyPost fromXml(SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableCopyPost(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        EDDTable tSourceEdd = null;
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tSosOfferingPrefix = null;
        String tExtractDestinationNames = "";
        String tOrderExtractBy = "";
        boolean checkSourceData = defaultCheckSourceData;
        boolean tSourceNeedsExpandedFP_EQ = true;
        boolean tFileTableInMemory = false;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;

        //process the tags
        String startOfTags = xmlReader.allTags();
        int startOfTagsN = xmlReader.stackSize();
        int startOfTagsLength = startOfTags.length();
        while (true) {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            String content = xmlReader.content();
            //if (reallyVerbose) String2.log("  tags=" + tags + content);
            if (xmlReader.stackSize() == startOfTagsN) 
                break; //the </dataset> tag
            String localTags = tags.substring(startOfTagsLength);

            //try to make the tag names as consistent, descriptive and readable as possible
            if      (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) tOnChange.add(content); 
            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 
            else if (localTags.equals( "<sosOfferingPrefix>")) {}
            else if (localTags.equals("</sosOfferingPrefix>")) tSosOfferingPrefix = content; 
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<extractDestinationNames>")) {}
            else if (localTags.equals("</extractDestinationNames>")) tExtractDestinationNames = content; 
            else if (localTags.equals( "<orderExtractBy>")) {}
            else if (localTags.equals("</orderExtractBy>")) tOrderExtractBy = content; 
            else if (localTags.equals( "<checkSourceData>")) {}
            else if (localTags.equals("</checkSourceData>")) checkSourceData = String2.parseBoolean(content); 
            else if (localTags.equals( "<sourceNeedsExpandedFP_EQ>")) {}
            else if (localTags.equals("</sourceNeedsExpandedFP_EQ>")) tSourceNeedsExpandedFP_EQ = String2.parseBoolean(content); 
            else if (localTags.equals( "<fileTableInMemory>")) {}
            else if (localTags.equals("</fileTableInMemory>")) tFileTableInMemory = String2.parseBoolean(content); 
            else if (localTags.equals( "<defaultDataQuery>")) {}
            else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content; 
            else if (localTags.equals( "<defaultGraphQuery>")) {}
            else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content; 
            else if (localTags.equals("<dataset>")) {
                try {
                    if (checkSourceData) {
                        //after first time, it's ok if source dataset isn't available
                        tSourceEdd = (EDDTable)EDD.fromXml(xmlReader.attributeValue("type"), xmlReader);
                    } else {
                        String2.log("WARNING!!! checkSourceData is false, so EDDTableCopy datasetID=" + 
                            tDatasetID + " is not checking the source dataset!");
                        int stackSize = xmlReader.stackSize();
                        do {  //will throw Exception if trouble (e.g., unexpected end-of-file
                            xmlReader.nextTag();
                        } while (xmlReader.stackSize() != stackSize); 
                        tSourceEdd = null;
                    }
                } catch (Throwable t) {
                    String2.log(MustBe.throwableToString(t));
                }
            } 
            else xmlReader.unexpectedTagException();
        }

        return new EDDTableCopyPost(tDatasetID, 
            tAccessibleTo, tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery, tReloadEveryNMinutes, 
            tExtractDestinationNames, tOrderExtractBy, tSourceNeedsExpandedFP_EQ,
            tSourceEdd, tFileTableInMemory);
    }

    /**
     * The constructor.
     * See the documentation for the superclass.
     *
     * @throws Throwable if trouble
     */
    public EDDTableCopyPost(String tDatasetID, 
        String tAccessibleTo, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File, String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        int tReloadEveryNMinutes,
        String tExtractDestinationNames, String tOrderExtractBy, 
        Boolean tSourceNeedsExpandedFP_EQ,
        EDDTable tSourceEdd, boolean tFileTableInMemory) throws Throwable {

        super(tDatasetID, tAccessibleTo, tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery,
            tReloadEveryNMinutes,
            tExtractDestinationNames, tOrderExtractBy, tSourceNeedsExpandedFP_EQ, 
            tSourceEdd, tFileTableInMemory);

    }

    /**
     * This is used by the constructor to make localEdd.
     * This subclass version makes an EDDTableFromPostNcFiles
     *
     * <p>It will fail if 0 local files -- that's okay, TaskThread will continue to work 
     *  and constructor will try again in 15 min.
     */
    EDDTableFromFiles makeLocalEdd(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, String tSosOfferingPrefix,
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tFileDir, boolean tRecursive, String tFileNameRegex, String tMetadataFrom,
        String tCharset, int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory) 
        throws Throwable {

        return new EDDTableFromPostNcFiles(tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            "", "", //tDefaultDataQuery, tDefaultGraphQuery
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes, 
            tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, 
            tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames, tSourceNeedsExpandedFP_EQ,
            tFileTableInMemory); 
    }

    /**
     * This returns a suggested fileName (no dir or extension).
     * It doesn't add a random number, so will return the same results 
     * if the inputs are the same.
     *
     * @param loggedInAs is only used for POST datasets (which override EDD.suggestFileName)
     *    since loggedInAs is used by POST for row-by-row authorization
     * @param userDapQuery
     * @param fileTypeName
     * @return a suggested fileName (no dir or extension)
     * @throws Exception if trouble (in practice, it shouldn't)
     */
    public String suggestFileName(String loggedInAs, String userDapQuery, String fileTypeName) {

        //decode userDapQuery to a canonical form to avoid slight differences in percent-encoding 
        try {
            userDapQuery = SSR.percentDecode(userDapQuery);
        } catch (Exception e) {
            //shouldn't happen
        }

        //include fileTypeName in hash so, e.g., different sized .png 
        //  have different file names
        String name = datasetID + "_" + 
            String2.md5Hex12(loggedInAs + userDapQuery + fileTypeName); 
        //String2.log("%% POST suggestFileName=" + name + "\n  for loggedInAs=" + loggedInAs + " from query=" + userDapQuery + "\n  from type=" + fileTypeName);
        return name;
    }

    /** 
     * This returns the name of the file in datasetDir()
     * which has all of the distinct data combinations for the current subsetVariables.
     * The file is deleted by setSubsetVariablesCSV(), which is called whenever
     * the dataset is reloaded.
     * See also EDDTable, which deletes these files.
     *
     * @param loggedInAs POST datasets overwrite this method and use loggedInAs 
     *    since data for each loggedInAs is different; others don't use this
     * @throws Exception if trouble
     */
    public String subsetVariablesFileName(String loggedInAs) throws Exception {
        return (loggedInAs == null? "" : String2.encodeFileNameSafe(loggedInAs) + "_") + 
            SUBSET_FILENAME; 
    }

    /** 
     * This returns the name of the file in datasetDir()
     * which has all of the distinct values for the current subsetVariables.
     * The file is deleted by setSubsetVariablesCSV(), which is called whenever
     * the dataset is reloaded.
     * See also EDDTable, which deletes these files.
     *
     * @param loggedInAs POST datasets overwrite this method and use loggedInAs 
     *    since data for each loggedInAs is different; others don't use this
     * @throws Exception if trouble
     */
    public String distinctSubsetVariablesFileName(String loggedInAs) throws Exception {
        return (loggedInAs == null? "" : String2.encodeFileNameSafe(loggedInAs) + "_") + 
            DISTINCT_SUBSET_FILENAME; 
    }


    /** Access to the variableDescriptionTable. */
    //public String variableDescriptionTable() {
    //    return variableDescriptionTable;
    //}


    /** Use this two times to download and make a local copy of the POST cPostSurg3 data. */
    public static void copyPostSurg3() throws Throwable {
        testCopyPostSurg3(true);
    }

    /**
     * This tests the copy of post surgery3 dataset.
     *
     * @throws Throwable if trouble
     */
    public static void testCopyPostSurg3(boolean checkSourceData) throws Throwable {
        String2.log("\n*** testCopyPostSurg3");
        testVerboseOn();
        defaultCheckSourceData = checkSourceData;
        long eTime;
        int po;
        String tQuery, tName, results, expected;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        try {
            EDDTable tedd = (EDDTable)oneFromDatasetXml("cPostSurg3"); 
/* */
            //das
            tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_cps3_Data", ".das"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
//data changes so it is hard to test all
"Attributes {\n" +
" s {\n" +
"  unique_tag_id {\n" +
"    String description \"Unique identifier for a tagging.\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Unique Tag Identifier\";\n" +
"  }\n" +
"  PI {\n" +
"    String description \"Firstname and Lastname of PI.  All capital letters.  Example: CEDAR CHITTENDEN\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Principal Investigator\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "\nresults=\n" + results);

            //.dds 
            tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_cps3_Data", ".dds"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String unique_tag_id;\n" +
"    String PI;\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    Float64 time;\n" +
"    Float64 activation_time;\n" +
//"    Float64 anaesthetic_conc_recirc;\n" +
//"    Float64 anaesthetic_conc_surgery;\n" +
//"    Float64 buffer_conc_anaesthetic;\n" +
//"    Float64 buffer_conc_recirc;\n" +
//"    Float64 buffer_conc_surgery;\n" +
"    String channel;\n" +
"    String code_label;\n" +
"    String comments;\n" +
"    String common_name;\n" +
"    Float64 date_public;\n" +
"    Int32 delay_max;\n" +
"    Int32 delay_min;\n" +
"    Int32 delay_start;\n" +
"    String delay_type;\n" +
"    Byte dna_sampled;\n" +
"    Float64 est_tag_life;\n" +
//"    Float64 fork_length;\n" +
"    Float64 frequency;\n" +
"    String implant_type;\n" +
//"    Float64 length_hood;\n" +
//"    Float64 length_pcl;\n" +
//"    Float64 length_standard;\n" +
"    String project;\n" +
"    String provenance;\n" +
"    String role;\n" +
"    String scientific_name;\n" +
//"    Float64 sedative_conc_surgery;\n" +
"    String stock;\n" +
"    Int32 surgery_id;\n" +
"    String surgery_location;\n" +
"    Float64 surgery_longitude;\n" +
"    Float64 surgery_latitude;\n" +
"    Float64 surgery_time;\n" +
"    String tag_id_code;\n" +
"    String tagger;\n" +
"    String treatment_type;\n" +
"    Float64 water_temp;\n" +
//"    Float64 weight;\n" +
"  } s;\n" +
"} s;\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);


            //test 1 role, public data   not loggedIn
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE\"";
            tName = tedd.makeNewFileForDapQuery(null, null, tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_1role", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =   
"unique_tag_id, pi, longitude, latitude, time, activation_time, anaesthetic_conc_recirc, anaesthetic_conc_surgery, buffer_conc_anaesthetic, buffer_conc_recirc, buffer_conc_surgery, channel, code_label, comments, common_name, date_public, delay_max, delay_min, delay_start, delay_type, dna_sampled, est_tag_life, fork_length, frequency, implant_type, length_hood, length_pcl, length_standard, project, provenance, role, scientific_name, sedative_conc_surgery, stock, surgery_id, surgery_location, surgery_longitude, surgery_latitude, surgery_time, tagger, treatment_type, water_temp, weight\n" +
", , degrees_east, degrees_north, UTC, UTC, ppm, ppm, ppm, ppm, ppm, , , , , UTC, ms, ms, days, , , days, mm, kHz, , mm, mm, mm, , , , , ppm, , , , degrees_east, degrees_north, UTC, , , degree_C, grams\n" +
"1154_A69-1005-AE_9326D, DAVID WELCH, -121.96733, 49.08505, 2004-05-04T03:30:00Z, 2004-04-11T07:00:00Z, NaN, 70.0, 140.0, NaN, NaN, , A69-1005-AE, \"LOCATED IN TANK # 16, NOT CLIPPED\", \"SOCKEYE, KOKANEE\", 2008-09-29T23:28:25Z, NaN, NaN, NaN, , 0, NaN, 165.0, NaN, INTERNAL, NaN, NaN, NaN, KINTAMA RESEARCH, Hatchery, WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE, ONCORHYNCHUS NERKA, 1.0, CULTUS LAKE, 276, ROSEWALL HATCHERY, -124.77528, 49.46028, 2004-04-28T07:00:00Z, MELINDA JACOBS, NOT ENTERED, 9.1, NaN\n" +
"1155_A69-1005-AE_9327D, DAVID WELCH, -121.96733, 49.08505, 2004-05-04T03:30:00Z, 2004-04-11T07:00:00Z, NaN, 70.0, 140.0, NaN, NaN, , A69-1005-AE, \"LOCATED IN TANK # 16, NOT CLIPPED\", \"SOCKEYE, KOKANEE\", 2008-09-29T23:28:25Z, NaN, NaN, NaN, , 0, NaN, 167.0, NaN, INTERNAL, NaN, NaN, NaN, KINTAMA RESEARCH, Hatchery, WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE, ONCORHYNCHUS NERKA, 1.0, CULTUS LAKE, 277, ROSEWALL HATCHERY, -124.77528, 49.46028, 2004-04-28T07:00:00Z, MELINDA JACOBS, NOT ENTERED, 9.2, NaN\n" +
"1156_A69-1005-AE_9328D, DAVID WELCH, -121.96733, 49.08505, 2004-05-04T03:30:00Z, 2004-04-11T07:00:00Z, NaN, 70.0, 140.0, NaN, NaN, , A69-1005-AE, \"LOCATED IN TANK # 8, DORSAL FIN CLIPPED\", \"SOCKEYE, KOKANEE\", 2008-09-29T23:28:25Z, NaN, NaN, NaN, , 0, NaN, 171.0, NaN, INTERNAL, NaN, NaN, NaN, KINTAMA RESEARCH, Hatchery, WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE, ONCORHYNCHUS NERKA, 1.0, CULTUS LAKE, 278, ROSEWALL HATCHERY, -124.77528, 49.46028, 2004-04-28T07:00:00Z, MELINDA JACOBS, NOT ENTERED, 9.1, NaN\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "\nresults=\n" + results.substring(0, 3000));
            String2.log("*** testPostSurg3 c1role FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //test 1 role, public data   loggedIn for other data
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE\"";
            tName = tedd.makeNewFileForDapQuery(null, "ROBICHAUD_DAVID", tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_1roleLIOD", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //same expected
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "\nresults=\n" + results.substring(0, 3000));
            String2.log("*** testCopyPostSurg3 c1roleLIOD FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 


            //test 1 role, private data, not loggedIn
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER\"";
            try {
                tName = tedd.makeNewFileForDapQuery(null, null, tQuery, 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_1roleP", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                String2.log("\nUnexpected results:\n" + results);
                throw new Exception("Shouldn't get here.");
            } catch (Throwable t) {
                if (t.toString().indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                    throw new Exception(
"*** I think the problem is that this test looks for data that was not-yet-public,\n" +
"but now it is public.\n" +
                        "Should have been THERE_IS_NO_DATA: " +
                        MustBe.throwableToString(t));
            }
            String2.log("*** testCopyPostSurg3 c1roleP FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //test 1 role, wrong log in
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER\"";
            try {
                tName = tedd.makeNewFileForDapQuery(null, "WELCH_DAVID", tQuery, 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_1roleWP", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                throw new Exception("Shouldn't get here.");
            } catch (Throwable t) {
                if (t.toString().indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                    throw new Exception("Should have been THERE_IS_NO_DATA: " +
                        MustBe.throwableToString(t));
            }
            String2.log("*** testCopyPostSurg3 c1roleWP FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //test 1 role, public data, logged in
            eTime = System.currentTimeMillis();
            tQuery = "&role=\"ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER\"";
            tName = tedd.makeNewFileForDapQuery(null, "ROBICHAUD_DAVID", tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_1roleL", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =   
"unique_tag_id, pi, longitude, latitude, time, activation_time, anaesthetic_conc_recirc, anaesthetic_conc_surgery, buffer_conc_anaesthetic, buffer_conc_recirc, buffer_conc_surgery, channel, code_label, comments, common_name, date_public, delay_max, delay_min, delay_start, delay_type, dna_sampled, est_tag_life, fork_length, frequency, implant_type, length_hood, length_pcl, length_standard, project, provenance, role, scientific_name, sedative_conc_surgery, stock, surgery_id, surgery_location, surgery_longitude, surgery_latitude, surgery_time, tagger, treatment_type, water_temp, weight\n" +
", , degrees_east, degrees_north, UTC, UTC, ppm, ppm, ppm, ppm, ppm, , , , , UTC, ms, ms, days, , , days, mm, kHz, , mm, mm, mm, , , , , ppm, , , , degrees_east, degrees_north, UTC, , , degree_C, grams\n" +
"23669_A69-1303-AC_1058600, DAVID ROBICHAUD, -122.816286671098, 49.2235994918324, 2008-08-24T14:50:00Z, 2008-08-23T07:00:00Z, NaN, NaN, NaN, NaN, NaN, , A69-1303-AC, Missing L pec fin, WHITE STURGEON, 2010-01-23T21:25:29Z, NaN, NaN, NaN, , 0, NaN, 850.0, NaN, INTERNAL, NaN, NaN, NaN, LGL LIMITED, Wild, ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER, ACIPENSER TRANSMONTANUS, NaN, Lower Fraser, 7992, FRASER RIVER JUST UPSTREAM OF PORT MANN BRIDGE, -122.816286671098, 49.2235994918324, 2008-08-23T07:00:00Z, Lucia Ferreira, SUMMER, NaN, NaN\n" +
"23670_A69-1303-AC_1058601, DAVID ROBICHAUD, -122.816286671098, 49.2235994918324, 2008-08-24T14:50:00Z, 2008-08-23T07:00:00Z, NaN, NaN, NaN, NaN, NaN, , A69-1303-AC, def in lower lobe of caudal fin, WHITE STURGEON, 2010-01-23T21:25:29Z, NaN, NaN, NaN, , 0, NaN, 1070.0, NaN, INTERNAL, NaN, NaN, NaN, LGL LIMITED, Wild, ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER, ACIPENSER TRANSMONTANUS, NaN, Lower Fraser, 7993, FRASER RIVER JUST UPSTREAM OF PORT MANN BRIDGE, -122.816286671098, 49.2235994918324, 2008-08-23T07:00:00Z, Lucia Ferreira, SUMMER, NaN, NaN\n" +
"23671_A69-1303-AC_1058602, DAVID ROBICHAUD, -122.788286819502, 49.2229398886514, 2008-08-30T15:56:00Z, 2008-08-30T07:00:00Z, NaN, NaN, NaN, NaN, NaN, , A69-1303-AC, small scrape near incision, WHITE STURGEON, 2010-01-23T21:25:29Z, NaN, NaN, NaN, , 0, NaN, 715.0, NaN, INTERNAL, NaN, NaN, NaN, LGL LIMITED, Wild, ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER, ACIPENSER TRANSMONTANUS, NaN, Lower Fraser, 7994, FRASER RIVER JUST UPSTREAM OF PORT MANN BRIDGE, -122.788286819502, 49.2229398886514, 2008-08-30T07:00:00Z, Lucia Ferreira, SUMMER, NaN, NaN\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "\nresults=\n" + results.substring(0, 3000));
            String2.log("*** testCopyPostSurg3 c1roleL FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //test public data + logged in
            eTime = System.currentTimeMillis();
            tQuery = "&orderBy(\"unique_tag_id\")"; //without this, data is ordered by pi, so different order
            tName = tedd.makeNewFileForDapQuery(null, "ROBICHAUD_DAVID", tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_1rolePDLI", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =   
"unique_tag_id, pi, longitude, latitude, time, activation_time, anaesthetic_conc_recirc, anaesthetic_conc_surgery, buffer_conc_anaesthetic, buffer_conc_recirc, buffer_conc_surgery, channel, code_label, comments, common_name, date_public, delay_max, delay_min, delay_start, delay_type, dna_sampled, est_tag_life, fork_length, frequency, implant_type, length_hood, length_pcl, length_standard, project, provenance, role, scientific_name, sedative_conc_surgery, stock, surgery_id, surgery_location, surgery_longitude, surgery_latitude, surgery_time, tagger, treatment_type, water_temp, weight\n" +
", , degrees_east, degrees_north, UTC, UTC, ppm, ppm, ppm, ppm, ppm, , , , , UTC, ms, ms, days, , , days, mm, kHz, , mm, mm, mm, , , , , ppm, , , , degrees_east, degrees_north, UTC, , , degree_C, grams\n" +
"1000_A69-1005-AE_1030201, DAVID WELCH, -121.0529, 49.8615, 2005-05-19T18:15:00Z, 2005-05-05T07:00:00Z, NaN, 70.0, 140.0, NaN, NaN, , A69-1005-AE, ADIPOSE FIN CLIPPED, COHO, 2008-09-29T23:28:33Z, NaN, NaN, NaN, , 0, NaN, 128.0, NaN, INTERNAL, NaN, NaN, NaN, KINTAMA RESEARCH, Hatchery, WELCH_DAVID_ONCORHYNCHUSKISUTCH_SPIUSCREEK, ONCORHYNCHUS KISUTCH, 1.0, SPIUS CREEK, 1148, SPIUS CREEK HATCHERY, -121.0253, 50.1415, 2005-05-17T07:00:00Z, MELINDA JACOBS, NOT ENTERED, 7.5, NaN\n" +
"1000_A69-1005-AE_1032813, DAVID WELCH, -115.93472, 46.12972, 2006-05-08T18:00:00Z, 2006-04-11T07:00:00Z, NaN, 80.0, 160.0, NaN, NaN, , A69-1005-AE, VERY TIGHT FIT, CHINOOK, 2008-09-29T23:28:58Z, NaN, NaN, NaN, , 0, NaN, 142.0, NaN, INTERNAL, NaN, NaN, NaN, KINTAMA RESEARCH, Hatchery, WELCH_DAVID_ONCORHYNCHUSTSHAWYTSCHA_DWORSHAK, ONCORHYNCHUS TSHAWYTSCHA, 30.0, DWORSHAK, 3808, \"KOOSKIA NATIONAL FISH HATCHERY, KOOSKIA, ID\", -115.93472, 46.12972, 2006-04-11T07:00:00Z, ADRIAN LADOUCEUR, ROR, 12.9, 31.4\n" +
"1001_A69-1005-AE_1030202, DAVID WELCH, -121.0529, 49.8615, 2005-05-19T18:15:00Z, 2005-05-05T07:00:00Z, NaN, 70.0, 140.0, NaN, NaN, , A69-1005-AE, \"ADDED 10 ML MS222, ADIPOSE FIN CLIPPED\", COHO, 2008-09-29T23:28:33Z, NaN, NaN, NaN, , 0, NaN, 125.0, NaN, INTERNAL, NaN, NaN, NaN, KINTAMA RESEARCH, Hatchery, WELCH_DAVID_ONCORHYNCHUSKISUTCH_SPIUSCREEK, ONCORHYNCHUS KISUTCH, 1.0, SPIUS CREEK, 1149, SPIUS CREEK HATCHERY, -121.0253, 50.1415, 2005-05-17T07:00:00Z, MELINDA JACOBS, NOT ENTERED, 7.6, NaN\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "\nresults=\n" + results.substring(0, 3000));
            String2.log("*** testCopyPostSurg3 c1rolePDLI FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 


            //test public data all have lat/lon/time?
            eTime = System.currentTimeMillis();
            tQuery = "longitude,latitude,time&time>=2007-05-01&time<2007-09-01";
            tName = tedd.makeNewFileForDapQuery(null, null, tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_LLT", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log("results=\n" + results.substring(0, Math.min(results.length(), 10000)));
            po = results.indexOf("NaN");
            String2.log("\nresults.length()=" + results.length() + "  NaN po=" + po);
            Test.ensureEqual(po, -1, "\nresults=\n" + results.substring(0, Math.min(results.length(), 10000)));
            String2.log("*** testCopyPostSurg3 cLLT FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

            //distinct roles
            eTime = System.currentTimeMillis();
            tQuery = "role&distinct()";
            tName = tedd.makeNewFileForDapQuery(null, EDStatic.loggedInAsSuperuser, tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_roles", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =   
"role\n" +
"\n" +
"ANDREWS_KELLY_HYDROLAGUSCOLLIEI_PUGETSOUND\n" +
"ANDREWS_KELLY_SQUALUSACANTHIAS_PUGETSOUND\n" +
"BEAMISH_RICHARD_ONCORHYNCHUSTSHAWYTSCHA_STRAITOFGEORGIA\n" +
"BEREJIKIAN_BARRY_ONCORHYNCHUSMYKISS_BIGBEEFCREEK\n" +
"BEREJIKIAN_BARRY_ONCORHYNCHUSMYKISS_DEWATTORIVER\n" +
"BEREJIKIAN_BARRY_ONCORHYNCHUSMYKISS_DUCKABUSH\n"; 
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "\nresults=\n" + results.substring(0, expected.length()));
            String2.log("*** testCopyPostSurg3 roles FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 



            //test tagger=""
            eTime = System.currentTimeMillis();
            tQuery = "unique_tag_id,pi,longitude,latitude,time,tagger&tagger=\"\"";
            tName = tedd.makeNewFileForDapQuery(null, "noTagger", tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_noTagger", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =   
"unique_tag_id, pi, longitude, latitude, time, tagger, date_public, role\n" +
", , degrees_east, degrees_north, UTC, , UTC, \n" +
"3090_A69-1005-AE_, FRED GOETZ, NaN, NaN, 2005-08-24T00:00:00Z, , 2008-09-29T23:28:42Z, GOETZ_FRED_ONCORHYNCHUSTSHAWYTSCHA_LAKEWASHINGTON\n" +
"3091_A69-1005-AE_, FRED GOETZ, NaN, NaN, 2005-08-24T00:00:00Z, , 2008-09-29T23:28:42Z, GOETZ_FRED_ONCORHYNCHUSTSHAWYTSCHA_LAKEWASHINGTON\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            String2.log("*** testCopyPostSurg3 noTagger FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 


            //test tagger=%22ADRIAN%20LADOUCEUR%22   (was a problem since tagger has a missing value)
            eTime = System.currentTimeMillis();
            Test.ensureTrue(String2.max("", "ADRIAN LADOUCEUR").equals("ADRIAN LADOUCEUR"), "\"\" doesn't sort first!");
            tQuery = "unique_tag_id,pi,longitude,latitude,time,tagger&tagger=%22ADRIAN%20LADOUCEUR%22";
            tName = tedd.makeNewFileForDapQuery(null, "tagAL", tQuery, 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_ps3_tagAL", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =   
"unique_tag_id, pi, longitude, latitude, time, tagger, date_public, role\n" +
", , degrees_east, degrees_north, UTC, , UTC, \n" +
"1874_A69-1005-AE_1032641, CHRIS WOOD, -123.9744, 49.69628, 2005-11-12T18:15:00Z, ADRIAN LADOUCEUR, 2008-09-29T23:28:37Z, WOOD_CHRIS_ONCORHYNCHUSNERKA_SAKINAWLAKE\n" +
"1875_A69-1005-AE_1032642, CHRIS WOOD, -123.9744, 49.69628, 2005-11-12T18:15:00Z, ADRIAN LADOUCEUR, 2008-09-29T23:28:37Z, WOOD_CHRIS_ONCORHYNCHUSNERKA_SAKINAWLAKE\n" +
"1876_A69-1005-AE_1032643, CHRIS WOOD, -123.9744, 49.69628, 2005-11-12T18:15:00Z, ADRIAN LADOUCEUR, 2008-09-29T23:28:37Z, WOOD_CHRIS_ONCORHYNCHUSNERKA_SAKINAWLAKE\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
            String2.log("*** testCopyPostSurg3 noTagger FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 



/* */
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "Unexpected EDDTableFromPostDatabase.testCopyPostSurg3 error:\n" +
                "Press ^C to stop or Enter to continue..."); 
        }
        defaultCheckSourceData = true;

    }

    /**
     * This downloads the POST Detection3 data in chunks via tcPostDet3.
     *
     * @throws Throwable if trouble
     */
    public static void testCopyChunkPostDet3() throws Throwable {
        String2.log("\n*** testCopyChunkPostDet3");
        testVerboseOn();
        long eTime;
        String tQuery, tName, results, expected;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        try {
            EDDTable tedd = (EDDTable)oneFromDatasetXml("chunkPostDet3"); 
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "Unexpected EDDTableFromPostDatabase.testCopyPostSurg3 error:\n" +
                "Press ^C to stop or Enter to continue..."); 
        }
    }

    /**
     * This makes cPostDet3 by breaking up the chunk files from tcPostDet3.
     *
     * @throws Throwable if trouble
     */
    public static void testBreakUpDet3Chunks() throws Throwable {
        String2.log("\n*** testBreakUpDet3Chunks");
        String2.getStringFromSystemIn(
            "\nMake sure that the datasets2.xml has the temporary version of cPostDet3\n" +
            "that gets data from chunkPostDet3 files.\n" +
            "Press ^C to stop or Enter to continue..."); 
        testVerboseOn();
        long eTime;
        String tQuery, tName, results, expected;
        try {
            EDDTable tedd = (EDDTable)oneFromDatasetXml("cPostDet3"); 
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "Unexpected EDDTableFromPostDatabase.testBreakUpDet3Chunks error:\n" +
                "Press ^C to stop or Enter to continue..."); 
        }
    }

    
    /** Use this two times to download and make a local copy of the POST cPostDet3 data. */
    public static void copyPostDet3() throws Throwable {
        testCopyPostDet3(true, false);
    }

    /**
     * This tests the copy of post detection3 dataset.
     *
     * @throws Throwable if trouble
     */
    public static void testCopyPostDet3(boolean checkSourceData, boolean runOtherTests) throws Throwable {
        String2.log("\n*** testCopyPostDet3");
        defaultCheckSourceData = checkSourceData;
        long eTime;
        String tQuery, tName, results, expected;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        try {
            EDDTable tedd = (EDDTable)oneFromDatasetXml("cPostDet3"); 
/* 
            //das
            tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_cpd3_Data", ".das"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"Attributes {\n" +
" s {\n" +
"  unique_tag_id {\n" +
"    String description \"Unique identifier for a tagging.\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Unique Tag Identifier\";\n" +
"  }\n" +
"  pi {\n" +
"    String description \"Firstname and Lastname of PI.  All capital letters.  Example: CEDAR CHITTENDEN\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Principal Investigator\";\n" +
"  }\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
  
            //.dds 
            tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_cpd3_Data", ".dds"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String unique_tag_id;\n" +
"    String pi;\n" +
"    Float64 longitude;\n" +
"    Float64 latitude;\n" +
"    Float64 time;\n" +
"    Float64 bottom_depth;\n" +
"    String common_name;\n" +
"    Float64 date_public;\n" +
"    String position_on_subarray;\n" +
"    String project;\n" +
"    Float64 riser_height;\n" +
"    String role;\n" +
"    String scientific_name;\n" +
"    String stock;\n" +
"    Float64 surgery_time;\n" +
"    String surgery_location;\n" +
"    String tagger;\n" +
"  } s;\n" +
"} s;\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
*/

            if (runOtherTests) {
                reallyVerbose = false;

                //test 1 tag, public data   not loggedIn
                eTime = System.currentTimeMillis();
                tQuery = "&unique_tag_id=\"224_A69-1206-AF_4003\"";
                tName = tedd.makeNewFileForDapQuery(null, null, tQuery, 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_pd3_1tag", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                expected =   
    "unique_tag_id, pi, longitude, latitude, time, bottom_depth, common_name, date_public, position_on_subarray, project, riser_height, role, scientific_name, stock, surgery_time, surgery_location, tagger\n" +
    ", , degrees_east, degrees_north, UTC, m, , UTC, , , m, , , , UTC, , \n" +
    "19835, DAVID WELCH, -121.97974, 49.07988, 2007-05-16T22:00:00Z, NaN, \"SOCKEYE, KOKANEE\", 2008-09-29T23:29:37Z, , KINTAMA RESEARCH, NaN, WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE, ONCORHYNCHUS NERKA, CULTUS LAKE, 2007-05-13T07:00:00Z, INCH CREEK HATCHERY, ADRIAN LADOUCEUR\n" +
    "19835, DAVID WELCH, -122.59574, 49.20158, 2007-05-20T23:34:31Z, 1.3, \"SOCKEYE, KOKANEE\", 2008-09-29T23:29:37Z, , KINTAMA RESEARCH, NaN, WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE, ONCORHYNCHUS NERKA, CULTUS LAKE, 2007-05-13T07:00:00Z, INCH CREEK HATCHERY, ADRIAN LADOUCEUR\n" +
    "19835, DAVID WELCH, -122.59574, 49.20158, 2007-05-20T23:35:49Z, 1.3, \"SOCKEYE, KOKANEE\", 2008-09-29T23:29:37Z, , KINTAMA RESEARCH, NaN, WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE, ONCORHYNCHUS NERKA, CULTUS LAKE, 2007-05-13T07:00:00Z, INCH CREEK HATCHERY, ADRIAN LADOUCEUR\n";
                Test.ensureEqual(results, expected, "\nresults=\n" + results);
                String2.log("*** testCopyPostDet3 c1tag FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 


                //test 1 role, public data   not loggedIn
                eTime = System.currentTimeMillis();
                tQuery = "&role=\"WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE\"";
                tName = tedd.makeNewFileForDapQuery(null, null, tQuery, 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_pd3_1role", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                //same expected
                Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
                String2.log("*** testCopyPostDet3 c1role FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

                //test 1 role, public data   loggedIn for other data
                eTime = System.currentTimeMillis();
                tQuery = "&role=\"WELCH_DAVID_ONCORHYNCHUSNERKA_CULTUSLAKE\"";
                tName = tedd.makeNewFileForDapQuery(null, "ROBICHAUD_DAVID", tQuery, 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_pd3_1roleLIOD", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                //same expected
                Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
                String2.log("*** testCopyPostDet3 c1roleLIOD FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 


                //test 1 role, private data, not loggedIn
                eTime = System.currentTimeMillis();
                tQuery = "&role=\"ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER\"";
                try {
                    tName = tedd.makeNewFileForDapQuery(null, null, tQuery, 
                        EDStatic.fullTestCacheDirectory, tedd.className() + "_pd3_1roleP", ".csv"); 
                    results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                    throw new Exception("Shouldn't get here.");
                } catch (Throwable t) {
                    if (t.toString().indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                        throw new Exception("Should have been THERE_IS_NO_DATA: " +
                            MustBe.throwableToString(t));
                }
                String2.log("*** testCopyPostDet3 c1roleP FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

                //test 1 role, wrong log in
                eTime = System.currentTimeMillis();
                tQuery = "&role=\"ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER\"";
                try {
                    tName = tedd.makeNewFileForDapQuery(null, "WELCH_DAVID", tQuery, 
                        EDStatic.fullTestCacheDirectory, tedd.className() + "_pd3_1roleWP", ".csv"); 
                    results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                    throw new Exception("Shouldn't get here.");
                } catch (Throwable t) {
                    if (t.toString().indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                        throw new Exception("Should have been THERE_IS_NO_DATA: " +
                            MustBe.throwableToString(t));
                }
                String2.log("*** testCoypPostDet3 c1roleWP FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 

                //test 1 role, public data, logged in
                eTime = System.currentTimeMillis();
                tQuery = "&role=\"ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER\"";
                tName = tedd.makeNewFileForDapQuery(null, "ROBICHAUD_DAVID", tQuery, 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_pd3_1roleL", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
    String2.log("\n" + results.substring(0, 4000) + "\n");
                expected =   
    "unique_tag_id, pi, longitude, latitude, time, bottom_depth, common_name, date_public, position_on_subarray, project, riser_height, role, scientific_name, stock, surgery_time, surgery_location, tagger\n" +
    ", , degrees_east, degrees_north, UTC, m, , UTC, , , m, , , , UTC, , \n" +
    "23669, DAVID ROBICHAUD, -122.816287004559, 49.2235095396314, 2008-08-24T03:17:40Z, 3.5, WHITE STURGEON, 2010-01-23T21:25:29Z, 1, LGL LIMITED, NaN, ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER, ACIPENSER TRANSMONTANUS, Lower Fraser, 2008-08-23T07:00:00Z, FRASER RIVER JUST UPSTREAM OF PORT MANN BRIDGE, Lucia Ferreira\n" +
    "23669, DAVID ROBICHAUD, -122.816287004559, 49.2235095396314, 2008-08-24T03:23:59Z, 3.5, WHITE STURGEON, 2010-01-23T21:25:29Z, 1, LGL LIMITED, NaN, ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER, ACIPENSER TRANSMONTANUS, Lower Fraser, 2008-08-23T07:00:00Z, FRASER RIVER JUST UPSTREAM OF PORT MANN BRIDGE, Lucia Ferreira\n" +
    "23669, DAVID ROBICHAUD, -122.816287004559, 49.2235095396314, 2008-08-24T03:29:33Z, 3.5, WHITE STURGEON, 2010-01-23T21:25:29Z, 1, LGL LIMITED, NaN, ROBICHAUD_DAVID_ACIPENSERTRANSMONTANUS_LOWERFRASER, ACIPENSER TRANSMONTANUS, Lower Fraser, 2008-08-23T07:00:00Z, FRASER RIVER JUST UPSTREAM OF PORT MANN BRIDGE, Lucia Ferreira\n"; 
                Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results.substring(0, 4000));
                String2.log("*** testCopyPostDet3 c1roleL FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 


                //distinct roles
                eTime = System.currentTimeMillis();
                tQuery = "role&distinct()";
                tName = tedd.makeNewFileForDapQuery(null, EDStatic.loggedInAsSuperuser, tQuery, 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_pd3_roles", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                expected =   
    "role\n" +
    "\n" +
    "BEAMISH_RICHARD_ONCORHYNCHUSTSHAWYTSCHA_STRAITOFGEORGIA\n" +
    "BEREJIKIAN_BARRY_ONCORHYNCHUSMYKISS_BIGBEEFCREEK\n" +
    "BEREJIKIAN_BARRY_ONCORHYNCHUSMYKISS_DEWATTORIVER\n" +
    "BEREJIKIAN_BARRY_ONCORHYNCHUSMYKISS_HAMMAHAMMA\n" +
    "BEREJIKIAN_BARRY_ONCORHYNCHUSMYKISS_SKOKOMISH\n" +
    "BISHOP_MARYANNE_OPHIODONELONGATUS_PRINCEWILLIAMSOUND\n" +
    "BLOCK_BARBARA_LAMNADITROPIS_N/A\n"; 
                Test.ensureEqual(results.substring(0, expected.length()), expected, 
                    "\nresults=\n" + results);
                String2.log("*** testCopyPostDet3 roles FINISHED.  TIME=" + (System.currentTimeMillis() - eTime)); 
            }

/* */
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
"*** THE DATA HAS CHANGED. THESE TESTS NEED WORK.\n" +
                "Expected EDDTableFromPostDatabase.testCopyPostDet3 error:\n" +
                "Press ^C to stop or Enter to continue..."); 
        }
        defaultCheckSourceData = true;

    }


    /** This is a custom little program to find tags with travelled &gt;degreesPerDay
     * or &gt; longDegreesTraveled or &gt; longYearsTraveled. */
    public static String findFastSwimmers(String loggedInAs, double degreesPerDay,
        double longDegreesTraveled, double longYearsTraveled) throws Throwable {
        String2.log("\n*** EDDTableCopyPost findFastSwimmers");
        //verbose = false;
        reallyVerbose = false;
        defaultCheckSourceData = false;
        StringArray tags = findTags(loggedInAs, false);
        EDDTable eddDet = (EDDTable)oneFromDatasetXml("cPostDet3"); 
        String dir = "c:/temp/temp/";
        int nFast = 0, nYoungFast = 0, 
            nTagsLookedAt = 0, 
            nLongDegreesDid = 0, nLongDegreesDidnt = 0, 
            nLongYearsDid   = 0, nLongYearsDidnt   = 0,
            nLongDegreesYearsDid = 0, nLongDegreesYearsDidnt = 0;

        Table fastSwimmerResults = new Table();
        StringArray fastSwimmerResultsPiPa = new StringArray();
        StringArray fastSwimmerResultsTagPa = new StringArray();
        StringArray fastSwimmerResultsErrorPa = new StringArray();
        int resultsPiPaI    = fastSwimmerResults.addColumn(String2.left("PI", 16),            fastSwimmerResultsPiPa);
        int resultsTagPaI   = fastSwimmerResults.addColumn(String2.left("unique_tag_id", 23), fastSwimmerResultsTagPa);
        int resultsErrorPaI = fastSwimmerResults.addColumn("error",                           fastSwimmerResultsErrorPa);

        Table longResults = new Table();
        StringArray longResultsPiPa = new StringArray();
        StringArray longResultsTagPa = new StringArray();
        StringArray longResultsErrorPa = new StringArray();
        longResults.addColumn(String2.left("PI", 16),            longResultsPiPa);
        longResults.addColumn(String2.left("unique_tag_id", 23), longResultsTagPa);
        longResults.addColumn("error",                           longResultsErrorPa);

        for (int pt = 0; pt < tags.size(); pt++) { 
            TableWriterAll twa = new TableWriterAll(dir, "findFastSwimmer");
            if (pt % 100 == 0) String2.log("" + pt);
            try {
                String tag = tags.get(pt);
                //if (tag.compareTo("1397F")<0) continue;
                nTagsLookedAt++;
                reallyVerbose = false;
                eddDet.getDataForDapQuery(loggedInAs, "", 
                    "longitude,latitude,time,PI&unique_tag_id=\"" + tag + "\"", twa);
                Table detTable = twa.cumulativeTable();
                Test.ensureEqual(detTable.getColumnName(0), "longitude", "");
                Test.ensureEqual(detTable.getColumnName(1), "latitude", "");
                Test.ensureEqual(detTable.getColumnName(2), "time", "");
                Test.ensureEqual(detTable.getColumnName(3), "PI", "");
                
                PrimitiveArray lonPA  = detTable.getColumn(0);  
                PrimitiveArray latPA  = detTable.getColumn(1);  
                PrimitiveArray timePA = detTable.getColumn(2);  
                String pi             = detTable.getColumn(3).getString(0);  
                double lon0, lat0, time0;
                double lon1  = lonPA.getDouble(0);
                double lat1  = latPA.getDouble(0);
                double time1 = timePA.getDouble(0); 
                double oLon = lon1, oLat = lat1, oTime = time1; 
                int nRows = lonPA.size();
                boolean isFast = false;
                double maxDegrees = 0;
                double youngLatDegrees = 0;
                double youngTime1 = 0;
                double youngDays = 0;
                for (int row = 1; row < nRows; row++) {
                    lon0  = lon1;
                    lat0  = lat1;
                    time0 = time1;
                    lon1  = lonPA.getDouble(row);
                    lat1  = latPA.getDouble(row);
                    time1 = timePA.getDouble(row);
                    double dist = Math.sqrt((lat1 - lat0) * (lat1 - lat0) + (lon1 - lon0) * (lon1 - lon0));
                    double days = (time1 - time0) / Calendar2.SECONDS_PER_DAY;
                    double elapsedDays = (time1 - oTime) / Calendar2.SECONDS_PER_DAY;
                    if (dist > 0.5 &&   //because not interested in quick jump between receivers in a line
                        dist / days > degreesPerDay) {
                        isFast = true;
                        fastSwimmerResultsPiPa.add(String2.left(pi, 16));
                        fastSwimmerResultsTagPa.add(String2.left(tag, 23));
                        fastSwimmerResultsErrorPa.add( 
                            " FS!" +
                            " at " + Calendar2.epochSecondsToIsoStringT(time1) +
                            " deg="       + String2.left("" + (float)dist, 13) + 
                            " days="      + String2.left("" + (float)days, 13) +
                            " deg/day="   + (float)(dist/days)); 
                    }

                    //degrees traveled (from release)
                    dist = Math.sqrt((lat1 - oLat) * (lat1 - oLat) + (lon1 - oLon) * (lon1 - oLon));
                    if (dist > maxDegrees) 
                        maxDegrees = dist;

                    //young fast swimmers    (use days for this detection)
                    if (elapsedDays < 90) {
                        double tYoungLatDegrees = Math.abs(lat1 - oLat);
                        if (tYoungLatDegrees > 1 && tYoungLatDegrees > youngLatDegrees) {
                            youngLatDegrees = tYoungLatDegrees;
                            youngTime1 = time1;
                            youngDays = elapsedDays;
                        }
                    }
                }
                if (isFast) nFast++;


                double firstToLastDegrees = 
                    Math.sqrt((lat1 - oLat) * (lat1 - oLat) + (lon1 - oLon) * (lon1 - oLon));
                double years = (time1 - oTime) / (365.25 * Calendar2.SECONDS_PER_DAY);

                boolean returned = firstToLastDegrees < 2;
                boolean longDegrees = maxDegrees > longDegreesTraveled;
                boolean longYears = years > longYearsTraveled;
                if (longDegrees || longYears) {
                    if (returned) {
                        if (longDegrees) nLongDegreesDid++;
                        if (longYears)   nLongYearsDid++;
                        if (longDegrees && longYears) nLongDegreesYearsDid++;
                    } else {
                        if (longDegrees) nLongDegreesDidnt++;
                        if (longYears)   nLongYearsDidnt++;
                        if (longDegrees && longYears) nLongDegreesYearsDidnt++;
                    }
                
                    longResultsPiPa.add(String2.left(pi, 16));
                    longResultsTagPa.add(String2.left(tag, 23));
                    longResultsErrorPa.add((isFast? "FS!" : "   ") +
                        " Long " +
                        (longDegrees? "Deg "  : "    ") +
                        (longYears?   "Years. "   : "     . ") +
                        (returned?    "Returned. " : "          ") +
                        " deg=" + String2.left("" + (float)maxDegrees, 13) + 
                        " years="   + String2.left("" + (float)years, 13) +
                        " FTL deg=" + (float)firstToLastDegrees);
                }

                //youngFastSwimmers
                if (youngLatDegrees > 0) {
                    nYoungFast++;
                    fastSwimmerResultsPiPa.add(String2.left(pi, 16));
                    fastSwimmerResultsTagPa.add(String2.left(tag, 23));
                    fastSwimmerResultsErrorPa.add( 
                        "YFS!" +
                        " at " + Calendar2.epochSecondsToIsoStringT(youngTime1) +
                        " deg=" + String2.left("" + (float)youngLatDegrees, 13) +
                        " days=" + String2.left("" + (float)youngDays, 13) +
                        " deg/day=" + (float)(youngLatDegrees/youngDays));
                    //testOneDetectionMap(eddDet, tag, false); Math2.sleep(2000);
                }

            } catch (Throwable t) {
                String2.log(t.toString());
            }

            twa.releaseResources();

        }
        defaultCheckSourceData = true;

        StringBuilder resultsSB = new StringBuilder(
            "\n\n****** Issue #8: Fast Swimmers and Issue #17: Young Fast Swimmers\n");

        //Issue #8 Fast Swimmers
        resultsSB.append(
            "\n\n****** Issue #8: Fast Swimmers ( >0.5 degree and >" + degreesPerDay + " degrees/day)\n\n" +
            "This test checks for adjacent detections where\n" +
            "1) the animal traveled > 0.5 degrees (because I'm not\n" +
            "   interested in a move to a nearby receiver), and\n" +
            "2) the animal traveled > " + degreesPerDay + " degrees/day\n" +
            "   (since 1 degree Lat ~= 69 miles and at 45N, 1 degree Lon ~= 49 miles,\n" +
            "   this corresponds to ~" + (degreesPerDay * 45) + " miles/day).\n" +
            "   *** Is this possible for an adult riding a strong current???\n\n" + 
            "number of unique_tag_id's looked at = " + nTagsLookedAt + "\n" +
            "number of unique_tag_id's that are fastSwimmers = " + nFast + "\n\n");

        //Issue #17 Young Fast Swimmers
        resultsSB.append("\n\n****** Issue #17: Young Fast Swimmers\n\n" +
            "This is a different issue than Issue #8 Fast Swimmers.\n" +
            "This test checks for detections in the first 90 days after release where\n" +
            "the animal traveled > 1 latitude degrees (ignoring longitude movement).\n" +
            "Assumptions:\n" +
            "* Only young animals are tagged.\n" +
            "* The first detection time is the release_time (not always true!).\n" +
            "* Young animals can't swim far on their own, but they can float downstream.\n" + 
            "* The rivers run East-West, so travel North-South is active swimming\n" +
            "  and/or riding currents.\n" +
            "So young animals that travel > 1 latitude degree are suspicious.\n" +
            "Certainly, many/most of these are valid detection data,\n" +
            "but I think it is worth looking at them to search for incorrect data.\n" +
            "Some are obviously incorrect.\n" +
            "  number of unique_tag_id's looked at = " + nTagsLookedAt + "\n" +
            "  number of unique_tag_id's that are youngFastSwimmers = " + nYoungFast + "\n" +
            "\n" +
            "***\n" +
            "Here are the Fast Swimmers (\"FS!\") and their lat,lon distance, and the\n" +
            "Young Fast Swimmers (\"YFS!\") and their record lat-only distance <= 90 days.\n" +
            "(View this in a wide window so the lines don't wrap around.)\n\n");
        fastSwimmerResults.sort(new int[]{resultsPiPaI, resultsTagPaI, resultsErrorPaI}, 
                            new boolean[]{true, true, true});
        resultsSB.append(fastSwimmerResults.dataToCSVString());
        resultsSB.append("\n");

        //long   (This was Issue #9 and Issue #10. Now just Of Interest.)
        resultsSB.append(
            "\n\n*** Of Interest: Traveled > " + longDegreesTraveled + 
            " degrees (from release_lat/lon)\n" +
            "    AND/OR Traveled > " + longYearsTraveled + " years\n" +            
            "    AND may have returned (First To Last Degrees < 2)\n" +
            "    (These aren't necessarily a problem, but sometimes worth investigating.)\n" +
            "    (The PI's might like it if you emailed these to the PI's.)\n" +
            "\n" +
            "number of unique_tag_id's looked at = " + nTagsLookedAt + "\n" +
            "number of unique_tag_id's that traveled a long distance and DIDN'T return = " + nLongDegreesDidnt + "\n" +
            "number of unique_tag_id's that traveled a long distance and DID    return = " + nLongDegreesDid   + "\n" +
            "number of unique_tag_id's that traveled a long time     and DIDN'T return = " + nLongYearsDidnt   + "\n" +
            "number of unique_tag_id's that traveled a long time     and DID    return = " + nLongYearsDid     + "\n" +
            "number of unique_tag_id's that traveled a long distance and time and DIDN'T return = " + nLongDegreesYearsDidnt + "\n" +
            "number of unique_tag_id's that traveled a long distance and time and DID    return = " + nLongDegreesYearsDid   + "\n" +
            "(\"deg\" = degrees)\n" +
            "(\"FTL deg\" = First To Last degrees)\n" +
            "(\"FS!\" = Fast Swimmer!)\n" +
            "View this in a wide window so the lines don't wrap around.\n\n");
        longResults.sort(new int[]{resultsPiPaI, resultsTagPaI, resultsErrorPaI}, 
                     new boolean[]{true, true, true});
        resultsSB.append(longResults.dataToCSVString());
        resultsSB.append("\n");

        String2.log(resultsSB.toString());
        return resultsSB.toString();
    }

    
    /** This is a custom little program to find tags with specific data,
     * e.g., which traveled atLeastDegrees in latitude and longitude
     * or persisted atLeastDays. */
    public static void findLongest(String loggedInAs, double atLeastDegrees,
          int atLeastYears) throws Throwable {
        //verbose = false;
        reallyVerbose = false;
        defaultCheckSourceData = false;
        StringArray tags = findTags(loggedInAs, false);
        EDDTable eddDet = (EDDTable)oneFromDatasetXml("cPostDet3"); 
        String dir = "c:/temp/temp/";
        float maxDegrees = 0;
        float maxYears = 0;
        StringBuilder results = new StringBuilder();
        for (int pt = 0; pt < tags.size(); pt++) { 
            TableWriterAll twa = new TableWriterAll(dir, "findLongest");
            String2.log("" + pt);
            try {
                String tag = tags.get(pt);
//if (tag.compareTo("103") < 0) continue;
                reallyVerbose = false;
                eddDet.getDataForDapQuery(loggedInAs, "", 
                    "longitude,latitude,time&unique_tag_id=\"" + tag + "\"", twa);
                
                PrimitiveArray pa = twa.column(0);  
                double stats[] = pa.calculateStats();
                double m0 = stats[PrimitiveArray.STATS_MAX] - stats[PrimitiveArray.STATS_MIN];
                
                pa = twa.column(1);  
                stats = pa.calculateStats();
                double m1 = stats[PrimitiveArray.STATS_MAX] - stats[PrimitiveArray.STATS_MIN];
                float degrees = Math2.doubleToFloatNaN(Math.sqrt(m0 * m1));

                pa = twa.column(2);  
                stats = pa.calculateStats();
                float years = Math2.doubleToFloatNaN(
                    (stats[PrimitiveArray.STATS_MAX] - stats[PrimitiveArray.STATS_MIN]) /
                    (365.25 * Calendar2.SECONDS_PER_DAY));

                String s = String2.left(tag, 15) + 
                    " degrees=" + String2.right("" + degrees, 10) +
                    " years=" + String2.right("" + years, 8);
                //String2.log(s);

                if (degrees > atLeastDegrees || years > atLeastYears) {
                    String2.log(s);
                    results.append(s + "\n");
                    maxDegrees = Math.max(degrees, maxDegrees);
                    maxYears   = Math.max(years, maxYears);

                    testOneDetectionMap(eddDet, tag, false); Math2.sleep(1000);
                }
            } catch (Throwable t) {
                String2.log(t.toString());
            }

            twa.releaseResources();

        }
        defaultCheckSourceData = true;
        String2.log("\nfindLongest results for atLeastDegrees=" + atLeastDegrees + 
            " atLeastYears=" + atLeastYears + ":");
        String2.log(results.toString());
        String2.log("\nmaxDegrees=" + maxDegrees + " maxYears=" + maxYears);
    }

    /** This is a custom little program to find tags which swam upstream. */
    public static void findUpstream(String loggedInAs) throws Throwable {
        String2.getStringFromSystemIn(
            "Running this again? Modify to have hashmap of lat+lon->elevation.\n" +
            "Press Enter to continue-> ");
        verbose = false;
        reallyVerbose = false;
        defaultCheckSourceData = false;
        StringArray tags = findTags(loggedInAs, false);
        EDDTable eddDet = (EDDTable)oneFromDatasetXml("cPostDet3"); 
        EDDGrid etopo180 = (EDDGrid)oneFromDatasetXml("etopo180");
        String dir = "c:/temp/temp/";
        StringBuilder results = new StringBuilder();
        int nTagsChecked = 0, nOdd = 0;
        GridDataAccessor.verbose = false;
        GridDataAccessor.reallyVerbose = false;
        for (int pt = 0; pt < tags.size(); pt++) { 
            TableWriterAll twa = new TableWriterAll(dir, "findUpstream");
            String2.log("" + pt);
            try {
                String tag = tags.get(pt);
                //if (tag.compareTo("2246F") < 0) continue;
                nTagsChecked++;
                verbose = false;
                reallyVerbose = false;
                eddDet.getDataForDapQuery(loggedInAs, "", "longitude,latitude,time&unique_tag_id=\"" + tag + "\"", twa);

                PrimitiveArray lon = twa.column(0);  
                PrimitiveArray lat = twa.column(1);
                PrimitiveArray time = twa.column(2);
                int nRows = lon.size();
                int elevation = 10000, oElevation;
                int nTimesUpstream = 0, nTimesDownstream = 0;
                double tLon = 0, tLat = 0, oLon, oLat;
                for (int row = 0; row < nRows; row++) {
                    oLon = tLon;
                    oLat = tLat;
                    oElevation = elevation;
                    tLon = lon.getDouble(row);
                    tLat = lat.getDouble(row);
                    if (Double.isNaN(tLon) || Double.isNaN(tLat) ||
                        (oLon == tLon && oLat == tLat)) //speedup: if same receiver, no need to check
                        continue;
                    //get nearest elevation value
                    GridDataAccessor gda = new GridDataAccessor(etopo180, "",
                        "altitude[(" + tLat + ")][(" + tLon + ")]", 
                        true, true);   //rowMajor, convertToNaN
                    gda.increment();
                    elevation = gda.getDataValueAsInt(0);
                    elevation = Math.max(0, elevation); //don't care about ocean depth
                    //String2.log("lon=" + tLon + " lat=" + tLat + " elev=" + elevation);
                    if (row == 0)
                        continue;
                    boolean tUpstream   = oElevation <= 10 && elevation >  10;
                    boolean tDownstream = oElevation >  10 && elevation <= 10;
                    if (tUpstream)   nTimesUpstream++;
                    if (tDownstream) nTimesDownstream++;
                    if ((tUpstream   && nTimesUpstream   > 1) ||
                        (tDownstream && nTimesDownstream > 1)) {
                        String msg = String2.left(tag, 15) + " row=" + 
                            String2.right("" + row, 5) + " elev from=" +
                            String2.right("" + oElevation, 6) + " to=" +
                            String2.right("" + elevation, 6)         + " at lon=" +
                            String2.right("" + (float)tLon, 10) + " lat=" +
                            String2.right("" + (float)tLat, 10) + " time=" +
                            Calendar2.epochSecondsToIsoStringT(time.getDouble(row)) + "Z";
                        String2.log(msg);
                        results.append(msg + "\n");
                        //break;
                    }
                }
                if (nTimesUpstream > 1 || nTimesDownstream > 1) {
                    nOdd++;
                    testOneDetectionMap(eddDet, tag, false); Math2.sleep(1000);
                }
            } catch (Throwable t) {
                String2.log(t.toString());
            }

            twa.releaseResources();

        }
        defaultCheckSourceData = true;
        String2.log("\nfindUpstream results:");
        String2.log(results.toString());
        String2.log("\nTagsChecked=" + nTagsChecked + " nOdd=" + nOdd);
    }

    /** This is a custom little program to find tags with detections before surgery. */
    public static String findDetectionBeforeSurgery(String loggedInAs) throws Throwable {
        //verbose = false;
        String2.log("\n*** EDDTableCopyPost.findDetectionBeforeSurgery\n");
        reallyVerbose = false;
        defaultCheckSourceData = false;
        StringArray tags = findTags(loggedInAs, false);
        EDDTable eddDet = (EDDTable)oneFromDatasetXml("cPostDet3"); 
        String dir = "c:/temp/temp/";

        Table resultsTable = new Table();
        StringArray resultsPiPa = new StringArray();
        StringArray resultsTagPa = new StringArray();
        StringArray resultsErrorPa = new StringArray();
        int resultsPiPaI    = resultsTable.addColumn(String2.left("PI", 16),            resultsPiPa);
        int resultsTagPaI   = resultsTable.addColumn(String2.left("unique_tag_id", 23), resultsTagPa);
        int resultsErrorPaI = resultsTable.addColumn("error",                           resultsErrorPa);

        int nMultiple = 0;
        int nBefore = 0;
        int nNaN = 0;
        for (int pt = 0; pt < tags.size(); pt++) { 
            TableWriterAll twa = new TableWriterAll(dir, "findDetBeforeSurg");
            String2.log("" + pt);
            try {
                String tag = tags.get(pt);
                reallyVerbose = false;
                eddDet.getDataForDapQuery(loggedInAs, "", "unique_tag_id,time,surgery_time,PI&unique_tag_id=\"" + tag + "\"", twa);
                Table table = twa.cumulativeTable();
                Test.ensureEqual(table.getColumnName(0), "unique_tag_id", "");
                Test.ensureEqual(table.getColumnName(1), "time", "");
                Test.ensureEqual(table.getColumnName(2), "surgery_time", "");
                Test.ensureEqual(table.getColumnName(3), "PI", "");

                PrimitiveArray timePA = twa.column(1);  
                double timeStats[] = timePA.calculateStats();
                PrimitiveArray surgPA = twa.column(2);  
                PrimitiveArray piPA = twa.column(3);  
                surgPA.sort();
                surgPA.removeDuplicates(false);
                String error = null;
                if (surgPA.size() != 1) {
                    nMultiple++;
                    error = "More than 1 (" + surgPA.size() + ") surgery times!";
                } 
                
                double surgStats[] = null;
                if (error == null) {
                    surgStats = surgPA.calculateStats();
                    if (surgPA.size() != surgStats[PrimitiveArray.STATS_N]) {
                        nNaN++;
                        error = ""; //error, but not displayed.  was "surgery_time=NaN";
                    }
                }

                if (error == null) {
                    double minTime  = timeStats[PrimitiveArray.STATS_MIN];
                    double surgTime = surgStats[PrimitiveArray.STATS_MAX];
                    if (minTime < surgTime) {
                        nBefore++;
                        error = "detectionTime=" + Calendar2.epochSecondsToIsoStringT(minTime) + 
                            " before surgeryTime=" + Calendar2.epochSecondsToIsoStringT(surgTime);
                    }
                }

                if (error != null && error.length() > 0) {
                    String pi = piPA.getString(0);
                    String2.log(pi + ", " + tag + ", " + error);
                    resultsPiPa.add(String2.left(pi, 16));
                    resultsTagPa.add(String2.left(tag, 23));
                    resultsErrorPa.add(error);
                }


            } catch (Throwable t) {
                String2.log(t.toString());
            }
            twa.releaseResources();

        }
        defaultCheckSourceData = true;
        StringBuilder resultsSB = new StringBuilder(
            "****** Issue #6: Detections Before Surgery in detection3\n\n" +
            "Looking at the data in the detection3 table,\n" +
            "the following tags have a detection timestamp before the\n" +
            "surgery_timestamp, or a surgery_timestamp of NaN.\n\n" +
            "  number of unique_tag_id's with >1 surgery_timestamp = " + nMultiple + "\n" +
            "  number of unique_tag_id's (not shown) with surgery_timestamp=NaN = " + nNaN + "\n" +
            "  number of unique_tag_id's with a detection before \n" +
            "    the surgery_timestamp = " + nBefore + "\n\n");
        resultsTable.sort(new int[]{resultsPiPaI, resultsTagPaI, resultsErrorPaI}, 
                          new boolean[]{true, true, true});
        resultsSB.append(resultsTable.dataToCSVString());
        resultsSB.append("\n\n");

        String2.log(resultsSB.toString());
        return resultsSB.toString();
    }


    /** This is a custom little program to find tags with detections where the date_public isn't uniform. */
    public static void findDifferentDatePublic(String loggedInAs) throws Throwable {
        //verbose = false;
        reallyVerbose = false;
        defaultCheckSourceData = false;
        StringArray tags = findTags(loggedInAs, false);
        EDDTable eddDet = (EDDTable)oneFromDatasetXml("cPostDet3"); 
        String dir = "c:/temp/temp/";
        StringBuilder results = new StringBuilder();
        int nNaN = 0, nG1 = 0;
        for (int pt = 0; pt < tags.size(); pt++) { 
            String2.log("" + pt);
            TableWriterAll twa = new TableWriterAll(dir, "findDetDatePublic");
            try {
                String tag = tags.get(pt);
                reallyVerbose = false;
                eddDet.getDataForDapQuery(loggedInAs, "", 
                    "unique_tag_id,date_public&unique_tag_id=\"" + tag + "\"", twa);
                Table table = twa.cumulativeTable();
                Test.ensureEqual(table.getColumnName(0), "unique_tag_id", "");
                Test.ensureEqual(table.getColumnName(1), "date_public", "");

                PrimitiveArray dpPA = twa.column(1);  
                dpPA.sort();
                dpPA.removeDuplicates(false);
                if (dpPA.getString(dpPA.size() - 1).length() == 0) {
                    nNaN++;
                    String s = "unique_tag_id=" + tag + " has date_public=NaN!";
                    String2.log(s);
                    results.append(s + "\n");
                }

                if (dpPA.size() > 1) {
                    nG1++;
                    StringBuilder sb = new StringBuilder("unique_tag_id=" + tag + " has " + 
                        dpPA.size() + " date_public times: " +
                        Calendar2.safeEpochSecondsToIsoStringTZ(dpPA.getDouble(0), "NaN"));
                    for (int i = 1; i < dpPA.size(); i++) 
                        sb.append(", " + Calendar2.safeEpochSecondsToIsoStringTZ(dpPA.getDouble(i), "NaN"));
                    String2.log(sb.toString());
                    results.append(sb.toString() + "\n");
                }

            } catch (Throwable t) {
                String2.log("\nfindDifferentDatePublic results:");
                String2.log(t.toString());
            }

            twa.releaseResources();

        }
        defaultCheckSourceData = true;
        String2.log("\n*** Tags in detection3 with missing or different date_public:\n" + 
            "  number of unique_tag_id's with date_public=NaN = " + nNaN + "\n" +
            "  number of unique_tag_id's with >1 date_public = " + nG1 + "\n");
        String2.log(results.toString());
    }

    /**
     * In cPostSurg3 table, this finds unique_tag_id where ....
     *
     * @throws Throwable if trouble
     */
    public static String findSurgeryBeforeActivation(String loggedInAs) throws Throwable {
        String2.log("\n*** findSurgeries");
        defaultCheckSourceData = false;
        EDDTable tedd = (EDDTable)oneFromDatasetXml("cPostSurg3"); 

        Table resultsTable = new Table();
        StringArray resultsPiPa = new StringArray();
        StringArray resultsTagPa = new StringArray();
        StringArray resultsErrorPa = new StringArray();
        int resultsPiPaI    = resultsTable.addColumn(String2.left("PI", 16),            resultsPiPa);
        int resultsTagPaI   = resultsTable.addColumn(String2.left("unique_tag_id", 23), resultsTagPa);
        int resultsErrorPaI = resultsTable.addColumn("error",                           resultsErrorPa);

        //find activation_time is after surgery_time
        TableWriterAll twa = new TableWriterAll(
            "c:/u00/cwatch/erddap2/cache/cPostSurg3/", "findSurgeryBeforeActivation");
        tedd.getDataForDapQuery(loggedInAs, "requestUrl", 
            "unique_tag_id,activation_time,surgery_time,PI", twa);
        Table table = twa.cumulativeTable();
        Test.ensureEqual(table.getColumnName(0), "unique_tag_id", "");
        Test.ensureEqual(table.getColumnName(1), "activation_time", "");
        Test.ensureEqual(table.getColumnName(2), "surgery_time", "");
        Test.ensureEqual(table.getColumnName(3), "PI", "");
        int nRows = table.nRows();
        int nSurgNaN = 0, nActNaN = 0, nAGS = 0;
        for (int row = 0; row < nRows; row++) {
            String tag = table.getStringData(0, row);
            double actTime = table.getDoubleData(1, row);
            double surgTime = table.getDoubleData(2, row);
            String pi = table.getStringData(3, row);
            String error = null;
            if (Double.isNaN(surgTime)) {
                error = ""; //error, but don't display it.  Was "surgery_timestamp = NaN";
                nSurgNaN++;
            }
            
            if (error == null && Double.isNaN(actTime)) {
                error = ""; //error, but don't display it.  Was "activation_timestamp = NaN";
                nActNaN++;
            } 
            
            if (error == null && actTime > surgTime) {
                error = "activation_timestamp=" +
                    Calendar2.epochSecondsToIsoStringT(actTime) + " > surgery_timestamp=" +
                    Calendar2.epochSecondsToIsoStringT(surgTime);
                nAGS++;
            }

            if (error != null && error.length() > 0) {
                //String2.log(pi + ", " + tag + ", " + error);
                resultsPiPa.add(String2.left(pi, 16));
                resultsTagPa.add(String2.left(tag, 23));
                resultsErrorPa.add(error);
            }
        
        }
        defaultCheckSourceData = true;
        StringBuilder resultsSB = new StringBuilder(
            "****** Issue #7: Activation After Surgery in surgery3\n\n" +
            "Looking at the data in the surgery3 table,\n" +
            "  number of unique_tag_id's = " + nRows + "\n" +
            "  number of unique_tag_id's (not shown) with surgery_timestamp=NaN = "  + nSurgNaN + "\n" +
            "  number of unique_tag_id's (not shown) with activation_timestamp=NaN = " + nActNaN + "\n" +
            "  number of unique_tag_id's with activation_timestamp after surgery_timestamp = " + nAGS + "\n" +
            "\n\n");
        resultsTable.sort(new int[]{resultsPiPaI, resultsTagPaI, resultsErrorPaI}, 
                          new boolean[]{true, true, true});
        resultsSB.append(resultsTable.dataToCSVString());
        resultsSB.append("\n");

        String2.log(resultsSB.toString());
        return resultsSB.toString();
    }

    /**
     * Get the unique_tag_id's.
     * This uses the current defaultCheckSourceData
     *
     * @param loggedInAs may be null or EDStatic.loggedInAsSuperuser or ...
     * @throws Throwable if trouble
     */
    public static StringArray findTags(String loggedInAs, boolean forceCreateNewFile) throws Throwable {
        String2.log("\n*** findTags");
        String dir = "c:/temp/temp/";
        String name = "postTagsFor" + String2.encodeFileNameSafe(loggedInAs);

        //need to make the file?
        if (forceCreateNewFile || !File2.isFile(dir + name)) {          
            EDDTable tedd = (EDDTable)oneFromDatasetXml("cPostSurg3"); 
            TableWriterAllWithMetadata twa = new TableWriterAllWithMetadata(dir, name);
            tedd.getDataForDapQuery(loggedInAs, "", "unique_tag_id", twa);
            tedd.saveAsFlatNc(dir + name, twa); //internally, it writes to temp file, then rename to cacheFullName
        }

        //read from file
        Table table = new Table();
        table.readFlatNc(dir + name, null, 1);
        Test.ensureEqual(table.getColumnName(0), "unique_tag_id", "");
        StringArray tags = (StringArray)table.getColumn(0);
        tags.sort();
        int removed = tags.removeDuplicates(true);
        String2.log("findTags finished. loggedInAs=" + loggedInAs + 
            " n=" + tags.size() + " nDuplicates=" + removed);
        return tags;
    }

    /** Test role. This gets the role for the tag. */
    public static void testRole(String unique_tag_id) throws Throwable {
        String2.log("\n*************** EDDTableCopyPost.testRole(" + unique_tag_id + ") **************\n");
        EDDTable surg3 = (EDDTable)oneFromDatasetXml("testPostSurg3"); 
        Table surg3Table = surg3.getTwawmForDapQuery(EDStatic.loggedInAsSuperuser, "", 
            "role&unique_tag_id=%22" + SSR.minimalPercentEncode(unique_tag_id) + 
            "%22&distinct()").cumulativeTable(); 
        String2.log(surg3Table.dataToCSVString(1000000));

        EDDTable det3 = (EDDTable)oneFromDatasetXml("testPostDet3"); 
        Table det3Table = det3.getTwawmForDapQuery(EDStatic.loggedInAsSuperuser, "",
            "role&unique_tag_id=%22" + SSR.minimalPercentEncode(unique_tag_id) + 
            "%22&distinct()").cumulativeTable(); 
        String2.log(det3Table.dataToCSVString(1000000));

    }

    /** Test one detection map */
    public static void testOneDetectionMap(String unique_tag_id) throws Throwable {
        String2.log("\n****************** EDDTableCopyPost.testOneDetectionMap() *****************\n");
        defaultCheckSourceData = false;
        EDDTable postDetections = (EDDTable)oneFromDatasetXml("cPostDet3"); 
        testOneDetectionMap(postDetections, unique_tag_id, true);
    }

    /** Test one detection map */
    public static void testOneDetectionMap(EDDTable postDetections, String unique_tag_id,
        boolean showHtmlTable) throws Throwable {
        String2.log("\n****************** EDDTableCopyPost.testOneDetectionMap() *****************\n");
        defaultCheckSourceData = false;
        String tDir = EDStatic.fullTestCacheDirectory;
        String tName = postDetections.makeNewFileForDapQuery(null, EDStatic.loggedInAsSuperuser, 
            "longitude,latitude,time&unique_tag_id=%22" + 
            SSR.minimalPercentEncode(unique_tag_id) + "%22&.draw=markers&.colorBar=BlueWhiteRed|D||||", 
            tDir, "cPostDet3_1Det3Map_" + String2.modifyToBeFileNameSafe(unique_tag_id), ".png"); 
        SSR.displayInBrowser("file://" + tDir + tName);

        if (showHtmlTable) {
            tName = postDetections.makeNewFileForDapQuery(null, EDStatic.loggedInAsSuperuser, 
                "longitude,latitude,time&unique_tag_id=%22" + 
                SSR.minimalPercentEncode(unique_tag_id) + "%22", 
                tDir, "cPostDet3_1Det3Data_" + String2.modifyToBeFileNameSafe(unique_tag_id), ".htmlTable"); 
            SSR.displayInBrowser("file://" + tDir + tName);
        }
    }

    /** print one surgery */
    public static void printOneSurgery(String unique_tag_id) throws Throwable {
        String2.log("\n****************** EDDTableCopyPost.printOneSurgery() *****************\n");
        defaultCheckSourceData = false;
        EDDTable postSurgery3 = (EDDTable)oneFromDatasetXml("cPostSurg3"); 
        String tDir = EDStatic.fullTestCacheDirectory;
        String tName = postSurgery3.makeNewFileForDapQuery(null, EDStatic.loggedInAsSuperuser, 
            "&unique_tag_id=%22" + SSR.minimalPercentEncode(unique_tag_id) + "%22", 
            tDir, "cPostSurg3_1Surgery_" + String2.modifyToBeFileNameSafe(unique_tag_id), ".csv"); 
        String2.log("\n" + String2.readFromFile(tDir + tName)[1]);
    }

    /** Test if absolute times are correct */
    public static String testAbsoluteTime() throws Throwable {
        String2.log("\n****************** EDDTableCopyPost.testAbsoluteTime() *****************\n");
        defaultCheckSourceData = false;
        String query, tDir, tName, results, expected, tag;
        Table table;
        StringBuilder resultsSB = new StringBuilder(
"\n\n****** Issue #24: Incorrect Absolute Time\n" +
"\n" +
"Unless great care is taken at every step of processing,\n" +
"time values can easily be wrong by 1 hour (daylight savings),\n" +
"or 7 or 8 hours (time zone issues).\n" +
"This test checks that the time values returned by ERDDAP\n" +
"match the official time values provided by Jose in an email\n" +
"dated 8/18/2010 12:07 PM:\n" +
"Surgery table:\n" +
"pi                     |     unique_tag_id      |   \n" +
"post_activation        |      erd_activation    | \n" +
"post_surgery_timestamp |      erd_surgery       | \n" +
"post_datepublic        |       erd_datepublic   |   \n" +
"post_release           |      erd_release\n" +
"FRED GOETZ             | 19190_A69-1303_1037589 | \n" +
"2007-04-24 00:00:00    | 2007-04-23 17:00:00-07 | \n" +
"2007-04-24 00:00:00    | 2007-04-23 17:00:00-07 | \n" +
"2010-01-30 20:30:14.818849 | 2010-01-30 12:30:14.818849-08 |\n" +
"2007-04-24 15:00:00    | 2007-04-24 08:00:00-07\n" +
"CEDAR CHITTENDEN       | 1968_A69-1204_1034135  | \n" +
"2006-09-12 00:00:00    | 2006-09-11 17:00:00-07 | \n" +
"2006-09-12 00:00:00    | 2006-09-11 17:00:00-07 | \n" +
"2008-09-29 23:29:06.436868 | 2008-09-29 16:29:06.436868-07 | \n" +
"2006-09-12 19:00:00 | 2006-09-12 12:00:00-07\n" +
"\n" +
"Detection table: \n" +
"pi                     |   unique_tag_id   | \n" +
"post_detection         |     erd_detection      | \n" +
"post_release           |      erd_release       |\n" +
"post_datepublic        |        erd_datepublic  |   \n" +
"post_deployment        |     erd_deployment     |    \n" +
"post_recovery          |      erd_recovery\n" +
"FRED GOETZ             | 791_A69-1206_1528      | \n" +
"2006-11-17 20:07:43-08 | 2006-11-17 20:07:43-08 | \n" +
"2006-11-01 09:30:00    | 2006-11-01 01:30:00-08 | \n" +
"2010-01-30 20:36:53.351176 | 2010-01-30 12:36:53.351176-08 | \n" +
"2006-10-25 19:14:00    | 2006-10-25 12:14:00-07 | \n" +
"2007-04-13 17:24:00    | 2007-04-13 10:24:00-07\n" +
"CEDAR CHITTENDEN       | 17086_A69-1303_1262H  | \n" +
"2006-06-14 00:27:47-07 | 2006-06-14 00:27:47-07 | \n" +
"2006-05-22 01:10:00    | 2006-05-21 18:10:00-07 | \n" +
"2008-09-29 23:29:17.288638 | 2008-09-29 16:29:17.288638-07 | \n" +
"2006-04-05 22:57:00    | 2006-04-05 15:57:00-07 | \n" +
"2006-07-22 01:36:00    | 2006-07-21 18:36:00-07\n" +
"\n" +
"!!! If ERDDAP is displaying the wrong times,\n" +
"the data should not be released!!!\n" +
"\n" +
"RESULTS:\n");

        //surgery  
        EDDTable postSurgery = (EDDTable)oneFromDatasetXml("cPostSurg3"); 
        tDir = EDStatic.fullTestCacheDirectory;

        tag = "19190_A69-1303_1037589";
        query = "&unique_tag_id=\"" + tag + "\"";
        resultsSB.append("cPostSurg3  ?" + query + "\n");
        table = postSurgery.getTwawmForDapQuery(null, "", query).cumulativeTable();
        resultsSB.append(table.check1(    "unique_tag_id",   0, tag) + "\n");
        resultsSB.append(table.check1Time("time",            0, "2007-04-24T15:00:00Z") + "\n");
        resultsSB.append(table.check1Time("activation_time", 0, "2007-04-24T00:00:00Z") + "\n");
        resultsSB.append(table.check1Time("date_public",     0, "2010-01-30T20:30:14Z") + "\n");
        resultsSB.append(table.check1Time("surgery_time",    0, "2007-04-24T00:00:00Z") + "\n");
        resultsSB.append("\n");
        
        tag = "1968_A69-1204_1034135";
        query = "&unique_tag_id=\"" + tag + "\"";
        resultsSB.append("cPostSurg3  ?" + query + "\n");
        table = postSurgery.getTwawmForDapQuery(null, "", query).cumulativeTable();
        resultsSB.append(table.check1(    "unique_tag_id",   0, tag) + "\n");
        resultsSB.append(table.check1Time("time",            0, "2006-09-12T19:00:00Z") + "\n");
        resultsSB.append(table.check1Time("activation_time", 0, "2006-09-12T00:00:00Z") + "\n");
        resultsSB.append(table.check1Time("date_public",     0, "2008-09-29T23:29:06Z") + "\n");
        resultsSB.append(table.check1Time("surgery_time",    0, "2006-09-12T00:00:00Z") + "\n");
        resultsSB.append("\n");
        

        //detection  
        EDDTable postDetections = (EDDTable)oneFromDatasetXml("cPostDet3"); 

        tag = "791_A69-1206_1528";
        query = "&unique_tag_id=\"" + tag + "\"&time=\"2006-11-18T04:07:43Z\"";
        resultsSB.append("cPostDet3  ?" + query + "\n");
        table = postDetections.getTwawmForDapQuery(null, "", query).cumulativeTable();
        resultsSB.append(table.check1(    "unique_tag_id",   0, tag) + "\n");
        resultsSB.append(table.check1Time("time",            0, "2006-11-18T04:07:43Z") + "\n");
        resultsSB.append(table.check1Time("date_public",     0, "2010-01-30T20:36:53Z") + "\n");
        resultsSB.append(table.check1Time("surgery_time",    0, "") + "\n");
        resultsSB.append("\n");
        
        //jose didn't provide surgery_time
        tag = "17086_A69-1303_1262H";
        query = "&unique_tag_id=\"" + tag + "\"&time=\"2006-06-14T07:27:47Z\"";
        resultsSB.append("cPostDet3  ?" + query + "\n");
        table = postDetections.getTwawmForDapQuery(null, "", query).cumulativeTable();
        resultsSB.append(table.check1(    "unique_tag_id",   0, tag) + "\n");
        resultsSB.append(table.check1Time("time",            0, "2006-06-14T07:27:47Z") + "\n");
        resultsSB.append(table.check1Time("date_public",     0, "2008-09-29T23:29:17Z") + "\n");
        resultsSB.append(table.check1Time("surgery_time",    0, "2006-05-20T00:00:00Z") + "\n");
        resultsSB.append("\n");
        

        //test surgery table's surgery time for above tag
        query = "&unique_tag_id=\"" + tag + "\"";
        resultsSB.append("cPostSurg3  ?" + query + "\n");
        table = postSurgery.getTwawmForDapQuery(null, "", query).cumulativeTable();
        resultsSB.append(table.check1(    "unique_tag_id",   0, tag) + "\n");
        resultsSB.append(table.check1Time("time",            0, "2006-05-22T01:10:00Z") + "\n");
        resultsSB.append(table.check1Time("activation_time", 0, "2006-05-20T00:00:00Z") + "\n");
        resultsSB.append(table.check1Time("date_public",     0, "2008-09-29T23:29:17Z") + "\n");
        resultsSB.append(table.check1Time("surgery_time",    0, "2006-05-20T00:00:00Z") + "\n");
        resultsSB.append("\n");

        String2.log(resultsSB.toString());
        String2.log("\nEDDTableCopyPost.testAbsoluteTime() finished successfully\n");
        return resultsSB.toString();
    }

    /** Test for inconsistencies in data */
    public static String testForInconsistencies() throws Throwable { 
        String2.log("\n****************** EDDTableCopyPost.testForInconsistencies() *****************\n");
        StringBuilder resultsSB = new StringBuilder();
        EDDTable surg3 = (EDDTable)oneFromDatasetXml("cPostSurg3All"); 
        EDDTable det3  = (EDDTable)oneFromDatasetXml("cPostDet3All"); 
        Table surgTable = surg3.getTwawmForDapQuery(EDStatic.loggedInAsSuperuser, "",
            "unique_tag_id,surgery_id,PI,common_name,stock,longitude,latitude,surgery_time,time,role,date_public" +
            "&distinct()&orderBy(\"unique_tag_id,surgery_id,surgery_time\")").cumulativeTable();
        Table detTable = det3.getTwawmForDapQuery(EDStatic.loggedInAsSuperuser, "",
            "unique_tag_id,PI,common_name,stock,surgery_time,role" +
            "&distinct()&orderBy(\"unique_tag_id,role,surgery_time\")").cumulativeTable();

        //internal test of surg3 
        String2.log("\n*** Testing cPostSurg3");
        PrimitiveArray sutPa = (PrimitiveArray)(surgTable.findColumn("unique_tag_id").clone());
        resultsSB.append(
            "\nsurg3 unique_tag_id nDuplicates=" + sutPa.removeDuplicates(true) + "\n\n" + //true = log duplicates
            "****** Issue #1: release_latitude = release_longitude in surgery3\n" +
            "I think it would be best if these tags were suppressed until they are\n" +
            "fixed. If not, they appear to have been released in Russia.\n" +
            "\n" +
            "****** Issue #1b: And for some of the tags latitude=longitude=0.\n" +
            "I think it would be best if these tags were suppressed until they are\n" +
            "fixed. If not, they appear to have been released off the coast of Ghana.\n\n");
        sutPa                = surgTable.findColumn("unique_tag_id");
        PrimitiveArray lonPa = surgTable.findColumn("longitude");
        PrimitiveArray latPa = surgTable.findColumn("latitude");
        int n = lonPa.size();
        int nLEL = 0;
        for (int i = 0; i < n; i++) {
            double tLon = lonPa.getDouble(i);
            double tLat = latPa.getDouble(i);
            if (!Math2.isFinite(tLon) && !Math2.isFinite(tLat)) {
            } else if (Math2.almostEqual(5, tLon, tLat)) {
                nLEL++;
                resultsSB.append(
                    "In surgery3, for unique_tag_id=" + sutPa.getString(i) + 
                    ", longitude=latitude (" + tLon + ")\n");
            }
        }
        resultsSB.append("The number of longitude=latitude tags = " + nLEL + "\n");
        if (false) {
            String2.log(resultsSB.toString());
            return resultsSB.toString();
        }

        //count of Private surgery tags (release_date after today)
        PrimitiveArray spiPa = surgTable.findColumn("PI");
        PrimitiveArray sdpPa = surgTable.findColumn("date_public");
        double todaySeconds = Calendar2.gcToEpochSeconds(Calendar2.newGCalendarZulu());
        if (false) {
            for (int i = 0; i < n; i++) {
                double tsdp = sdpPa.getDouble(i);
                if (tsdp > todaySeconds) 
                    EDStatic.tally.add("SurgHasPrivateTagData", spiPa.getString(i));
            }
            resultsSB.append(
                "\nSurgery Table PI's That Have Private Tag Data:\n" + 
                EDStatic.tally.toString("SurgHasPrivateTagData", 50) + "\n");

            String2.log(resultsSB.toString());
            return resultsSB.toString();
        }

        //internal test of det3
        resultsSB.append(
            "\n****** Issue #2: Unique_tag_ids That Aren't Unique.\n\n" +
            "In detection3, if I\n" +
            "  SELECT DISTINCT unique_tag_id, pi, common_name, stock, surgery_timestamp, role\n" +
            "and then look for unique_tag_id's that are associated with more than one\n" +
            "combination of\n" +
            "  pi, common_name, stock, surgery_timestamp, role\n" +
            "I find these duplicate unique_tag_ids:\n\n");
        PrimitiveArray dutPa = (PrimitiveArray)(detTable.findColumn("unique_tag_id").clone());
        resultsSB.append(
            "\ndet3 unique_tag_id nDuplicates=" + dutPa.removeDuplicates(true) + "\n"); //true = log duplicates
        //dutPa is reloaded from detTable below


        //compare surg3 and det3
        resultsSB.append(
            "\n****** Issue #4: Inconsistencies between surgery3 and detection3.\n\n");            
                       sutPa = surgTable.findColumn("unique_tag_id");
                       dutPa =  detTable.findColumn("unique_tag_id");
        //PrimitiveArray spiPa = surgTable.findColumn("PI");
        PrimitiveArray dpiPa =  detTable.findColumn("PI");
        PrimitiveArray scnPa = surgTable.findColumn("common_name");
        PrimitiveArray dcnPa =  detTable.findColumn("common_name");
        PrimitiveArray sskPa = surgTable.findColumn("stock");
        PrimitiveArray dskPa =  detTable.findColumn("stock");
        PrimitiveArray sroPa = surgTable.findColumn("role");
        PrimitiveArray droPa =  detTable.findColumn("role");
        PrimitiveArray sstPa = surgTable.findColumn("surgery_time");
        PrimitiveArray dstPa =  detTable.findColumn("surgery_time");
        n = sutPa.size();  //currently, surgTable drives tests because it has more unique tags
        for (int si = 0; si < n; si++) {
            String uti = sutPa.getString(si); //unique_tag_id
            int di     = dutPa.indexOf(uti);  //linear search will find first instance
            if (di < 0) {
                resultsSB.append(
                    "ERROR: unique_tag_id=" + uti + " in cPostSurg3 not found in cPostDet3.\n");
                continue;
            }

            String msg = "For unique_tag_id=" + uti + ", cPostSurg3 doesn't match cPostDet3 for "; 
            if (                                             !spiPa.getString(si).equals(        dpiPa.getString(di)))
                resultsSB.append(msg + "PI:\n \""           + spiPa.getString(si) + "\" != \"" + dpiPa.getString(di) + "\"\n");
            if (                                             !scnPa.getString(si).equals(        dcnPa.getString(di)))
                resultsSB.append(msg + "common_name:\n \""  + scnPa.getString(si) + "\" != \"" + dcnPa.getString(di) + "\"\n");
            if (                                             !sskPa.getString(si).equals(        dskPa.getString(di)))
                resultsSB.append(msg + "stock:\n \""        + sskPa.getString(si) + "\" != \"" + dskPa.getString(di) + "\"\n");
            if (                                             !sroPa.getString(si).equals(        droPa.getString(di)))
                resultsSB.append(msg + "role:\n \""         + sroPa.getString(si) + "\" != \"" + droPa.getString(di) + "\"\n");
            if (                                             !sstPa.getString(si).equals(        dstPa.getString(di)))
                resultsSB.append(msg + "surgery_time:\n \"" + sstPa.getString(si) + "\" != \"" + dstPa.getString(di) + "\"\n");
            if (                                             !sstPa.getString(si).equals(        dstPa.getString(di)))
                resultsSB.append(msg + "surgery_time:\n \"" + sstPa.getString(si) + "\" != \"" + dstPa.getString(di) + "\"\n");
        }        

        resultsSB.append(            
            "\n****** Issue #3: Tags with Too Much Detection Data for ERDDAP.\n\n" +
            "A few tags have too much detection data for ERDDAP (>1GB)\n" +
            "and so are present in ERDDAP's copy of surgery3,\n" +
            "but not ERDDAP's copy of detection3.\n" +
            "I have previously suggested a solution:\n" +
            "remove some of the unnecessary detections for these tags\n" +
            "(remove the middle detections when there is a long series\n" +
            "of detections in a short time at the same receiver).\n\n" +
            "Until this is fixed, it would be best to remove these\n" +
            "tags from surgery3 and detection3.\n\n");

        //compare surg3 and det3
        resultsSB.append(            
            "\n****** Issue #5: Different number of unique_tag_id's in surgery3 and detection3.\n\n" +
            "\nThe number of unique_tag_id's is different in surgery3 and detection3:" +
            "\n  surgery3   n unique unique_tag_id=" + sutPa.size() +
            "\n  detection3 n unique unique_tag_id=" + dutPa.size() +
            "\nHopefully, this is explained by the few tags that have too much data for ERDDAP" +
            "\nand so are not in ERDDAP's copy of detection3.\n\n");

        String2.log(resultsSB.toString());

        String2.log("\n*** end of EDDTableCopyPost.testForInconsistencies()");
        return resultsSB.toString();
    }

    /** Test if surgery (release)lon,lat,time is in detection3 as first row of data. */
    public static String testIfSurgeryDataIsDetectionFirstRow() throws Throwable { 
        String2.log("\n************* EDDTableCopyPost.testIfSurgeryDataIsDetectionFirstRow() ***********\n");
        EDDTable surg3 = (EDDTable)oneFromDatasetXml("cPostSurg3"); 
        EDDTable det3  = (EDDTable)oneFromDatasetXml("cPostDet3"); 
        Table surgTable = surg3.getTwawmForDapQuery(EDStatic.loggedInAsSuperuser, "",
            "unique_tag_id,longitude,latitude,time,PI" +
            "&distinct()&orderBy(\"unique_tag_id\")").cumulativeTable();
        PrimitiveArray sutiPa = surgTable.findColumn("unique_tag_id");
        PrimitiveArray slatPa = surgTable.findColumn("latitude");
        PrimitiveArray slonPa = surgTable.findColumn("longitude");
        PrimitiveArray stimPa = surgTable.findColumn("time");
        PrimitiveArray sPiPa  = surgTable.findColumn("PI");

        Table resultsTable = new Table();
        StringArray resultsPiPa = new StringArray();
        StringArray resultsTagPa = new StringArray();
        StringArray resultsErrorPa = new StringArray();
        int resultsPiPaI    = resultsTable.addColumn(String2.left("PI", 16),            resultsPiPa);
        int resultsTagPaI   = resultsTable.addColumn(String2.left("unique_tag_id", 23), resultsTagPa);
        int resultsErrorPaI = resultsTable.addColumn("error",                           resultsErrorPa);

        StringBuilder beforeTags = new StringBuilder();

        //for each surgery, ...
        int nChecked = 0, nNaNTime = 0, nSNaNTime = 0, nMissing = 0, nAfter = 0, nBefore = 0, 
            nLonDifferent = 0, nLatDifferent = 0, nOK = 0;
        int n = sutiPa.size(); 
        for (int si = 0; si < n; si++) {
            //verbose = false;
            reallyVerbose = false;
            String piInfo  = String2.left(sPiPa.getString(si), 16);
            String tag = sutiPa.getString(si);
            //if (!tag.startsWith("1030")) continue;
            String tagInfo = String2.left(tag, 23);
            nChecked++;
            String error = null;

            try {
                Table detTable = det3.getTwawmForDapQuery(EDStatic.loggedInAsSuperuser, "",
                    "unique_tag_id,longitude,latitude,time,PI" +
                    "&unique_tag_id=\"" + tag + "\"").cumulativeTable();
                PrimitiveArray dutiPa =  detTable.findColumn("unique_tag_id");
                PrimitiveArray dlatPa =  detTable.findColumn("latitude");  
                PrimitiveArray dlonPa =  detTable.findColumn("longitude"); 
                PrimitiveArray dtimPa =  detTable.findColumn("time");


                double stime = stimPa.getDouble(si);
                double dtime = dtimPa.getDouble(0);
                double slon  = slonPa.getDouble(si);
                double dlon  = dlonPa.getDouble(0);
                double slat  = slatPa.getDouble(si);
                double dlat  = dlatPa.getDouble(0);

                if (Double.isNaN(stime) && Double.isNaN(dtime)) {
                    nNaNTime++;
                    error = "Surgery release_time = first detection time = NaN";
                } 
                
                if (Double.isNaN(stime)) {
                    nSNaNTime++;
                    error = "Surgery release_time = NaN";
                } 

                if (error == null && stime != dtime) {
                    String stimes = Calendar2.epochSecondsToIsoStringT(stime);
                    String dtimes = Calendar2.epochSecondsToIsoStringT(dtime);
                    if (dtime < stime) {
                        nAfter++;
                        error = "Surgery release_time=" + stimes + 
                                " is AFTER  first detection time=" + dtimes;
                    } else if (dtime > stime) {
                        nBefore++;
                        //beforeTags.append(tag + ", ");
                        error = "Surgery release_time=" + stimes + 
                                " is BEFORE first detection time=" + dtimes;
                    }
                } 
                
                if (error == null && slat != dlat) {
                    nLatDifferent++;
                    error = "Times are same but surgery release_lat=" + (float)slat +
                            " != first detection lat=" + (float)dlat;
                } 
                
                if (error == null && slon != dlon) {
                    nLonDifferent++;
                    error = "Times & lats are same but surgery release_lon=" + (float)slon +
                            " != first detection lon=" + (float)dlon;
                } 
                
                
            } catch (Throwable t) {
                String tError = t.toString();
                if (tError.indexOf(MustBe.THERE_IS_NO_DATA) >= 0) {
                    nMissing++;
                    error = "Tag isn't in detection3.";
                } else {
                    error = "ERROR: " + tError;
                }
            }

            if (error == null) {
                nOK++;
            } else {
                String2.log(piInfo + ", " + tagInfo + ", " + error);
                resultsPiPa.add(piInfo);
                resultsTagPa.add(tagInfo);
                resultsErrorPa.add(error);
            }
        
        }
        StringBuilder resultsSB = new StringBuilder(
            "\n****** Issue #12: surgery3 release_time,lat,lon inconsistent with detection3 first detection\n" +
            "\n" +
            "Number of tags checked = " + nChecked + "\n" +
            "Trouble:\n" + 
            "  number of tags where surgery release_time and first detection time are NaN = " + nNaNTime + "\n" +
            "  number of tags where surgery release_time is NaN and first detection time isn't NaN = " + nSNaNTime + "\n" +
            "  number of tags where surgery release_time is AFTER first detection time = " + nAfter + "\n" +
            "  number of tags where surgery release_time is BEFORE first detection time = " + nBefore + "\n" +
            "  number of tags where time is same but surgery release_latitude (and lon?) differs from first detection lat = " + nLatDifferent + "\n" +
            "  number of tags where time is same but surgery release_longitude (not lat) differs from first detection lon = " + nLonDifferent + "\n" +
            "Minor trouble (probably just that the surgery release data isn't in the detection table):\n" +
            "  number of tags where there is no detection data = " + nMissing + "\n" +
            "    (some of these are because of Issue #3 -- tags with too much data for ERDDAP)\n" +
            "OK:\n" +
            "  number of tags where surgery release_time,lat,lon matches first detection info = " + nOK + "\n" +
            "\n" +
            "Here are the tags that have problems:\n\n");
        resultsTable.sort(new int[]{resultsPiPaI, resultsTagPaI, resultsErrorPaI}, 
                          new boolean[]{true, true, true});
        resultsSB.append(resultsTable.dataToCSVString());
        resultsSB.append("\n");
        //String2.log("\n\nTags where surgery release_time is BEFORE first detection time:\n");
        //String2.log(String2.noLongLinesAtSpace(beforeTags, 75, "").toString());
        String2.log(resultsSB.toString());
        String2.log("\n*** end of EDDTableCopyPost.testIfSurgeryDataIsDetectionFirstRow()");

        return resultsSB.toString();
    }

    /** Test if surgery items have lowercase letters. */
    public static String testLowerCase() throws Throwable { 
        String2.log("\n************* EDDTableCopyPost.testLowerCase ***********\n");
        EDDTable surg3 = (EDDTable)oneFromDatasetXml("cPostSurg3"); 
        Table surgTable;
        PrimitiveArray pa;
        int n;
        StringBuilder resultsSB = new StringBuilder(
            "\n\n" +
            "****** Issue #13: Leading/trailing spaces and Uncapitalized Data\n" +
            "\n" +
            "Leading/trailing spaces are a serious problem and need to be fixed.\n" +
            "\n" +
            "Most String data (e.g., for PI) is capitalized. Some isn't.\n" +
            "This doesn't matter much to me, and perhaps this isn't important,\n" +
            "but it is inconsistent and different from the data's description.\n" +
            "\n" +
            "Leading/trailing spaces and Uncapitalized values are:\n" +
            "\n");

        String varNames[] = new String[]{"PI", "common_name", "scientific_name", 
            "implant_type", "project", "provenance", "stock", "surgery_location",
            "tagger", "treatment_type"};
        for (int vn = 0; vn < varNames.length; vn++) {
            String2.log("varName=" + varNames[vn]);
            resultsSB.append(varNames[vn] + "\n");
            surgTable = surg3.getTwawmForDapQuery(EDStatic.loggedInAsSuperuser, "",
                varNames[vn] + "&distinct()").cumulativeTable();
            pa = surgTable.findColumn(varNames[vn]);
            pa.sort();
            pa.removeDuplicates();
            n = pa.size();
            for (int i = 0; i < n; i++) {
                String s = pa.getString(i);
                if (s.startsWith(" ") || s.endsWith(" "))
                    resultsSB.append("  '" + s + "'\n");
                else if (!s.equals(s.toUpperCase()))
                    resultsSB.append("  " + s + "\n");
            }
        }

        String2.log(resultsSB.toString());
        return resultsSB.toString();
    }


    /**
     * This runs the test and other methods in this class.
     *
     * @param test
     * @param reallyVerbose
     * @throws Throwable if trouble
     */
    public static void run(int which, boolean reallyVerbose) throws Throwable {
        String2.log("\n****************** EDDTableCopyPost.run(" + which + ") *****************\n");
        testVerbose(reallyVerbose);

        String resultsFileName = EDStatic.fullTestCacheDirectory + "runPOST.log";
        File2.delete(resultsFileName);
        
        int firstTest = which < 0? 0 : which;
        int lastTest  = which < 0? 6 : which;
        long tTime = System.currentTimeMillis();
        for (int tTest = firstTest; tTest <= lastTest; tTest++) {
            try {

            if (tTest < 100)
                String2.appendFile(resultsFileName, 
                    "\n\n" +
                    "#######################  EDDTableCopyPost test #" + tTest + "  #######################\n\n");

            String results = "";
            if (tTest == 0) 
                results = testForInconsistencies();

            if (tTest == 1)
                results = findDetectionBeforeSurgery(EDStatic.loggedInAsSuperuser); 

            if (tTest == 2)
                results = findSurgeryBeforeActivation(EDStatic.loggedInAsSuperuser);        

            if (tTest == 3)
                results = findFastSwimmers(EDStatic.loggedInAsSuperuser, 2.0, 5, 4);  //deg/day, longDegrees, longYears

            if (tTest == 4)
                results = testIfSurgeryDataIsDetectionFirstRow();

            if (tTest == 5)
                results = testLowerCase();

            if (tTest == 6)
                results = testAbsoluteTime();

        //always done
//        testCopyPostSurg3(true);        //checkSourceData
//        testCopyPostDet3(false, true);  //checkSourceData, runOtherTests


            //non-test methods
            if (tTest == 100)
                copyPostSurg3(); //Get the data. Run this twice! read Issue #18 messages 2nd time.      
            if (tTest == 101)
                copyPostDet3();  //Get the data. Run this twice! in case glitch in getting some tags.

            String2.appendFile(resultsFileName, results); 

            } catch (Throwable t) {
                String2.appendFile(resultsFileName, 
                    MustBe.throwableToString(t) + "\n");
            }
        }

        if (which < 100) {
            String msg = 
                "\n\n" +
                "#######################  END OF RESULTS  #######################\n\n" +
                "*** EDDTableCopyPost.test(" + which + ") finished.  time=" +
                Calendar2.elapsedTimeString(System.currentTimeMillis() - tTime) +
                "\nThe results are in " + resultsFileName + "\n";
            String2.log(msg);
            String2.appendFile(resultsFileName, msg);

            SSR.displayInBrowser("file://" + resultsFileName);    
        }
    }

}
