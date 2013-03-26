/* 
 * EDDTableFromEDDGrid Copyright 2013, NOAA.
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
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.ByteArrayInputStream;
import java.util.Enumeration;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;

/** 
 * This class creates an EDDTable from an EDDGrid.
 *
 * <p>This doesn't have generateDatasetsXml.  Use the appropriate
 * EDDGridFromX.generateDatasetsXml, then wrap it with EDDTableFromEDDGrid.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2013-03-08
 */
public class EDDTableFromEDDGrid extends EDDTable{ 

    protected EDDGrid eddGrid;
    protected boolean sourceCanConstrainStringEQNE, sourceCanConstrainStringGTLT;
    protected int eddGridNAV, eddGridNDV;

    /**
     * This constructs an EDDTableFromEDDGrid based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromEDDGrid"&gt; 
     *    having just been read.  
     * @return an EDDTableFromEDDGrid.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromEDDGrid fromXml(SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFromEDDGrid(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        EDDGrid tEDDGrid = null;
        Attributes tGlobalAttributes = null;

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
            if (localTags.equals("<dataset>")) {
                String tType = ("type");
                if (tType == null || !tType.startsWith("EDDGrid"))
                    throw new SimpleException(
                        "type=\"" + tType + "\" is not allowed for the dataset within the EDDTableFromEDDGrid. " +
                        "The type MUST start with \"EDDGrid\".");
                tEDDGrid = (EDDGrid)EDD.fromXml(tType, xmlReader);

            } else if (localTags.equals("<addAttributes>")) {
                tGlobalAttributes = getAttributesFromXml(xmlReader);
            } else {
                xmlReader.unexpectedTagException();
            }
        }

        return new EDDTableFromEDDGrid(tDatasetID, tGlobalAttributes, tEDDGrid);
    }

    /**
     * The constructor.
     *
     * @param tDatasetID is a very short string identifier 
     *   (required: just safe characters: A-Z, a-z, 0-9, _, -, or .)
     *   for this dataset. See EDD.datasetID().
     * @param tGlobalAttributes global addAttributes. This must include
     *   a new cdmDataType.
     * @param tEDDGrid
     * @throws Throwable if trouble
     */
    public EDDTableFromEDDGrid(String tDatasetID, Attributes tGlobalAttributes, 
        EDDGrid tEDDGrid) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromEDDGrid " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromEDDGrid(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDTableFromEDDGrid"; 
        datasetID = tDatasetID;
        if (datasetID == null || datasetID.length() == 0)
            throw new SimpleException(errorInMethod + "The datasetID wasn't specified.");
        if (datasetID.equals(tEDDGrid.datasetID())) 
            throw new SimpleException(errorInMethod + "The EDDTableFromEDDGrid's datasetID " +
                "must be different from the contained EDDGrid's datasetID.");
        if (tEDDGrid == null)
            throw new SimpleException(errorInMethod + "The contained EDDGrid dataset is missing.");
        eddGrid = tEDDGrid;

        //gather info from the eddGrid
        accessibleTo          = eddGrid.accessibleTo;
        onChange              = eddGrid.onChange;
        fgdcFile              = eddGrid.fgdcFile;
        iso19115File          = eddGrid.iso19115File;
        localSourceUrl        = eddGrid.localSourceUrl;
        setReloadEveryNMinutes( eddGrid.getReloadEveryNMinutes());

        //global attributes
        //cdmDataType     must be changed in this dataset's addAtts
        sourceGlobalAttributes   = eddGrid.combinedGlobalAttributes;
        addGlobalAttributes      = tGlobalAttributes == null? new Attributes() : tGlobalAttributes;
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("null");

        //???
        sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //axisVars yes, dataVars no
        sourceCanConstrainStringData  = CONSTRAIN_NO; 
        sourceCanConstrainStringRegex = ""; //no    ???

        //quickRestart is handled by contained eddGrid


        //dataVariables  (axisVars in reverse order, then dataVars)
        eddGridNAV = eddGrid.axisVariables.length;
        eddGridNDV = eddGrid.dataVariables.length;
        dataVariables = new EDV[eddGridNAV + eddGridNDV];
        for (int dv = 0; dv < dataVariables.length; dv++) {
            //variables in this class just see the destination name/type/... of the eddGrid varaible
            EDV gridVar = dv < eddGridNAV?  eddGrid.axisVariables[eddGridNAV - 1 - dv] :
                                            eddGrid.dataVariables[dv - eddGridNAV];
            String tSourceName     = gridVar.destinationName();     
            Attributes tSourceAtts = gridVar.combinedAttributes(); 
            Attributes tAddAtts    = new Attributes();
            String tDataType       = gridVar.destinationDataType();
            double tMin            = gridVar.destinationMin(); 
            double tMax            = gridVar.destinationMax();
            EDV newVar = null;
            if (tSourceName.equals(EDV.LON_NAME)) {
                newVar = new EDVLon(tSourceName, tSourceAtts, tAddAtts, tDataType, tMin, tMax);
                lonIndex = dv;
            } else if (tSourceName.equals(EDV.LAT_NAME)) {
                newVar = new EDVLat(tSourceName, tSourceAtts, tAddAtts, tDataType, tMin, tMax);
                latIndex = dv;
            } else if (tSourceName.equals(EDV.ALT_NAME)) {
                newVar = new EDVAlt(tSourceName, tSourceAtts, tAddAtts, tDataType, tMin, tMax);
                altIndex = dv;
            } else if (tSourceName.equals(EDV.DEPTH_NAME)) {
                newVar = new EDVDepth(tSourceName, tSourceAtts, tAddAtts, tDataType, tMin, tMax);
                depthIndex = dv;
            } else if (tSourceName.equals(EDV.TIME_NAME)) {
                //tMin tMax are epochSeconds
                tAddAtts.add("data_min", "" + tMin);
                if (Math.abs(tMax - System.currentTimeMillis()/1000.0) > 3 * Calendar2.SECONDS_PER_DAY)
                    tAddAtts.add("data_max", "" + tMax);
                newVar = new EDVTime(tSourceName, tSourceAtts, tAddAtts, tDataType);
                timeIndex = dv;
            //currently, there is no EDVTimeStampGridAxis
            } else newVar = new EDV(tSourceName, "", tSourceAtts, tAddAtts, tDataType, tMin, tMax);

            dataVariables[dv] = newVar;
        }

        //ensure the setup is valid
        ensureValid();

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDTableFromEDDGrid " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 

    }


    /** 
     * This gets the data (chunk by chunk) from this EDDTable for the 
     * OPeNDAP DAP-style query and writes it to the TableWriter. 
     * See the EDDTable method documentation.
     *
     * @param loggedInAs the user's login name if logged in (or null if not logged in).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be null.
     * @param tableWriter
     */
    public void getDataForDapQuery(String loggedInAs, String requestUrl, 
        String userDapQuery, TableWriter tableWriter) throws Throwable {

        //get the sourceDapQuery (a query that the source can handle)
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        getSourceQueryFromDapQuery(userDapQuery,
            resultsVariables,
            constraintVariables, constraintOps, constraintValues); //timeStamp constraints other than regex are epochSeconds

        //further prune constraints 
        //work backwards since deleting some
        for (int c = constraintVariables.size() - 1; c >= 0; c--) { 

            String conVar = constraintVariables.get(c);
            String conOp = constraintOps.get(c);
            int dv = String2.indexOf(dataVariableSourceNames(), conVar);

            //remove constraints for eddGrid data vars (string and numeric)
            //and remove regex constraints
            if (dv >= eddGridNAV || conOp.equals(PrimitiveArray.REGEX_OP)) {
                constraintVariables.remove(c);
                constraintOps.remove(c);
                constraintValues.remove(c);
                continue;
            }
        }

        //generate the gridDapQuery

/*
        need to:
        see what axis indices are needed
        make a local tableWriter
            gather data as a table from eddGrid data access
              post process the table
            gather data as a table from further axis indices
              post process the table

        //get dataAccessor first, in case of error when parsing query
        boolean isAxisDapQuery = isAxisDapQuery(userDapQuery);
        String requestUrl = "/griddap/" + datasetID + ".internal"; //file extension not known here!
        if (isAxisDapQuery) {
            AxisDataAccessor ada = new AxisDataAccessor(eddGrid, 
                requestUrl, gridDapQuery);
            eddGrid.saveAsTableWriter(ada, tw);
        } else {
            GridDataAccessor gda = new GridDataAccessor(eddGrid, 
                requestUrl, gridDapQuery, true,  //rowMajor
                false);   //convertToNaN  (would be true, but TableWriterHtmlTable will do it)
            eddGrid.saveAsTableWriter(gda, tw);
        }
*/
    }



    /**
     */
    public static void testBasic() throws Throwable {
        testVerboseOn();
        String results, query, tName, expected, expected2;
        String id = "testTableFromEDDGrid";
        EDDTable tedd = (EDDTable)oneFromDatasetXml(id);

        //das
        tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            tedd.className() + "_tfdg", ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"Attributes {\n" +
" s {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Forecast time for ForecastModelRunCollection\";\n" +
"    Float64 missing_value NaN;\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  time_offset {\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"offset hour from start of run for coordinate = time\";\n" +
"    Float64 missing_value NaN;\n" +
"    String standard_name \"forecast_period\";\n" +
"    String units \"hours since 2010-01-13T00:00:00Z\";\n" +
"  }\n" +
"  time_run {\n" +
"    String _CoordinateAxisType \"RunTime\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"run times for coordinate = time\";\n" +
"    Float64 missing_value NaN;\n" +
"    String standard_name \"forecast_reference_time\";\n" +
"    String units \"hours since 2010-01-13T00:00:00Z\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String classification_authority \"not applicable\";\n" +
"    String classification_level \"UNCLASSIFIED\";\n" +
"    String comment \"...\";\n" +
"    String contact \"NAVO, N33\";\n" +
"    String Conventions \"CF-1.6, _Coordinates, COARDS, Unidata Dataset Discovery v1.0\";\n" +
"    String creator_email \"Frank.Bub@navy.mil\";\n" +
"    String creator_name \"USA/NAVY/NAVO\";\n" +
"    String creator_url \"http://www.navo.navy.mil/\";\n" +
"    String distribution_statement \"Approved for public release. Distribution unlimited.\";\n" +
"    String downgrade_date \"not applicable\";\n" +
"    String generating_model \"Global NCOM with OSU tides\";\n" +
"    String history \"created on "; //20130224 ;\n" +
//"FMRC Best Dataset\n" +
//"2013-03-07T22:08:13Z http://ecowatch.ncddc.noaa.gov/thredds/dodsC/ncom/ncom_reg7_agg/NCOM_Region_7_Aggregation_best.ncd\n" +
//"2013-03-07T22:08:13Z http://127.0.0.1:8080/cwexperimental/tabledap/testTableFromEDDGrid.das\";\n" +
expected2 = 
    "String infoUrl \"http://edac-dap2.northerngulfinstitute.org/ocean_nomads/NCOM_index_map.html\";\n" +
"    String input_data_source \"FNMOC NOGAPS, NAVO MODAS, NAVO NLOM\";\n" +
"    String institution \"Naval Oceanographic Office\";\n" +
"    String keywords \"aggregation, best, best time series, coastal, coordinate, forecast, forecast_period, forecast_reference_time, hour, model, naval, navy, navy coastal ocean model, ncom, ocean, oceanographic, office, offset, period, reference, region, run, series, start, time, time series, times\";\n" +
"    String license \"Approved for public release. Distribution unlimited.\";\n" +
"    String location \"Proto fmrc:NCOM Region 7 Aggregation\";\n" +
"    String message \"UNCLASSIFIED\";\n" +
"    String Metadata_Conventions \"CF-1.6, _Coordinates, COARDS, Unidata Dataset Discovery v1.0\";\n" +
"    String model_type \"x-curvilinear lon, y-curvilinear lat, z-sigma_z\";\n" +
"    String NCOM_Regional_Index_Map \"http://edac-dap2.northerngulfinstitute.org/ocean_nomads/NCOM_index_map.html\";\n" +
"    String operational_status \"development\";\n" +
"    String publisher_email \"Scott.Cross@noaa.gov\";\n" +
"    String publisher_name \"USA/NOAA/NESDIS/NCDDC\";\n" +
"    String publisher_url \"http://www.ncddc.noaa.gov/\";\n" +
"    String reference \"https://www.navo.navy.mil/\";\n" +
"    String sourceUrl \"http://ecowatch.ncddc.noaa.gov/thredds/dodsC/ncom/ncom_reg7_agg/NCOM_Region_7_Aggregation_best.ncd\";\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String summary \"These forecasts are from an operational, data assimilating nowcast-forecast system run by the Naval Oceanographic Office using the Navy Coastal Ocean Model (NCOM). The output files contain temperature, salinity, east and north components of current, and surface elevation, interpolated to a 1/8 degree Cartesian grid in the horizontal and to standard depth levels in the vertical, and written at three-hour intervals out to 72 hours. The Navy global atmospheric model, NOGAPS, provides atmospheric forcing. A tidal component is not included in the model output.\";\n" +
"    String time_coverage_end \"2013-03-09T00:00:00Z\";\n" +
"    String time_coverage_start \"2000-01-01T10:00:00Z\";\n" +
"    String time_origin \"2013-02-24 00:00:00\";\n" +
"    String title \"NCOM Region 7 Aggregation, Best Time Series [time]\";\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);      
        int po = results.indexOf("String infoUrl ");
        Test.ensureEqual(results.substring(Math.max(0, po)), expected2, "results=\n" + results);      

        //das
        tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            tedd.className() + "_tfdg", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float64 time;\n" +
"    Float64 time_offset;\n" +
"    Float64 time_run;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //an old query
        query = "time,time_run&time>=2006-08-07T00&time<2006-08-08"; 
        tName = tedd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
            tedd.className() + "_tfdg", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      
           

    }

     
    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        String2.log("\n****************** EDDTableFromEDDGrid.test() *****************\n");
        testVerboseOn();

/* */
        //always done        
        testBasic();
    }

}
