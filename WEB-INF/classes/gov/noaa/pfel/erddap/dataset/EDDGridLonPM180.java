/* 
 * EDDGridLonPM180 Copyright 2015, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
//import com.cohort.array.ByteArray;
//import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
//import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;

//import gov.noaa.pfel.coastwatch.griddata.NcHelper;
//import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
//import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

//import java.util.ArrayList;


/** 
 * THIS IS NOT FINISHED.
 * This class creates an EDDGrid with longitude values in the range -180 to 180
 * from the enclosed dataset with longitude values in the range 0 to 360.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2015-08-05
 */
public class EDDGridLonPM180 extends EDDGrid { 

    protected EDDGrid childDataset;

    /**
     * This constructs an EDDGridLonPM180 based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridLonPM180"&gt; 
     *    having just been read.  
     * @return an EDDGridLonPM180.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDGridLonPM180 fromXml(SimpleXMLReader xmlReader) throws Throwable {

        if (verbose) String2.log("\n*** constructing EDDGridAggregateExistingDimension(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 

        //data to be obtained while reading xml
        EDDGrid tChildDataset = null;
        String tAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;

        //process the tags
        String startOfTags = xmlReader.allTags();
        int startOfTagsN = xmlReader.stackSize();
        int startOfTagsLength = startOfTags.length();

        while (true) {
            xmlReader.nextTag();
            String tags = xmlReader.allTags();
            String content = xmlReader.content();
            if (xmlReader.stackSize() == startOfTagsN) 
                break; //the </dataset> tag
            String localTags = tags.substring(startOfTagsLength);

            //try to make the tag names as consistent, descriptive and readable as possible
            if (localTags.equals("<dataset>")) {
                if (tChildDataset == null) {
                    EDD edd = EDD.fromXml(xmlReader.attributeValue("type"), xmlReader);
                    if (edd instanceof EDDGrid) {
                        tChildDataset = (EDDGrid)edd;
                    } else {
                        throw new RuntimeException("Datasets.xml error: " +
                            "The dataset defined in an " +
                            "EDDGridLonPM180 must be a subclass of EDDGrid.");
                    }
                } else {
                    throw new RuntimeException("Datasets.xml error: " +
                        "There can be only one <dataset> defined within an " +
                        "EDDGridLonPM180 <dataset>.");
                }

            } 
            else if (localTags.equals( "<sourceUrl>")) {}
            else if (localTags.equals("</sourceUrl>")) 
                tLocalSourceUrls.add(content);

            //<sourceUrls serverType="thredds" regex=".*\\.nc" recursive="true"
            //  >http://ourocean.jpl.nasa.gov:8080/thredds/dodsC/g1sst/catalog.xml</sourceUrl>
            else if (localTags.equals( "<sourceUrls>")) {
                tSUServerType = xmlReader.attributeValue("serverType");
                tSURegex      = xmlReader.attributeValue("regex");
                String tr     = xmlReader.attributeValue("recursive");
                tSURecursive  = tr == null? true : String2.parseBoolean(tr);
            }
            else if (localTags.equals("</sourceUrls>")) tSU = content; 
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) tOnChange.add(content); 
            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 
            else if (localTags.equals( "<defaultDataQuery>")) {}
            else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content; 
            else if (localTags.equals( "<defaultGraphQuery>")) {}
            else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content; 

            else xmlReader.unexpectedTagException();
        }

        //make the main dataset based on the information gathered
        return new EDDGridLonPM180(tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File,
            tChildDataset);

    }

    /**
     * The constructor.
     * The axisVariables must be the same and in the same
     * order for each dataVariable.
     *
     * @param tDatasetID
     * @param tAccessibleTo is a space separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tOnChange 0 or more actions (starting with "http://" or "mailto:")
     *    to be done whenever the dataset changes significantly
     * @param tFgdcFile This should be the fullname of a file with the FGDC
     *    that should be used for this dataset, or "" (to cause ERDDAP not
     *    to try to generate FGDC metadata for this dataset), or null (to allow
     *    ERDDAP to try to generate FGDC metadata for this dataset).
     * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
     * @param tChildDataset
     * @throws Throwable if trouble
     */
    public EDDGridLonPM180(String tDatasetID, 
        String tAccessibleTo, StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        EDDGrid tChildDataset) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDGridLonPM180 " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDGridGridAggregateExistingDimension(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDGridLonPM180"; 
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        childDataset = tChildDataset;

        if (childDataset == null)
            throw new RuntimeException(String2.ERROR + 
                ": The child dataset doesn't have a longitude axis variable.");
        MustBe.ensureFileNameSafe(datasetID, "datasetID");
        if (datasetID.equals(childDataset.datasetID()))
            throw new RuntimeException(String2.ERROR + 
                ": This dataset's datasetID must not be the same as the child's datasetID.");

        localSourceUrl = childDataset.localSourceUrl();
        addGlobalAttributes = childDataset.addGlobalAttributes();
        sourceGlobalAttributes = childDataset.sourceGlobalAttributes();
        combinedGlobalAttributes = childDataset.combinedGlobalAttributes();
        //and clear searchString of children?

        //make the local axisVariables
        int nAv = childDataset.axisVariables.length;
        axisVariables = new EDVGridAxis[nAv];
        System.arraycopy(childDataset.axisVariables, 0, axisVariables, 0, nAv);
        lonIndex   = childDataset.lonIndex;
        latIndex   = childDataset.latIndex;
        altIndex   = childDataset.altIndex;
        depthIndex = childDataset.depthIndex;
        timeIndex  = childDataset.timeIndex;
        if (lonIndex < 0)
            throw new RuntimeException(String2.ERROR + 
                ": The child dataset doesn't have a longitude axis variable.");

        //make the new lon axisVariable from child's destinationValues
        EDVLonGridAxis childEDVLon = axisVariables[lonIndex];
        PrimitiveArray childEDVLonDestValues = childEDVLon.destinationValues();
...
        EDVLonGridAxis newEDVLon = new EDVLonGridAxis(EDV.LON_NAME,
            new Attributes(sourceEDVLon.combinedAttributes()), new Attributes(),
            PrimitiveArray tSourceValues) throws Throwable {
        axisVariables[lonIndex] = newEDVLon; 

        //ensure the setup is valid
        ensureValid();

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDGridLonPM180 " + datasetID + " constructor finished. TIME=" + 
            (System.currentTimeMillis() - constructionStartMillis) + "\n"); 
    }

    /**
     * This makes a sibling dataset, based on the new sourceUrl.
     *
     * @throws Throwable always (since this class doesn't support sibling())
     */
    public EDDGrid sibling(String tLocalSourceUrl, int firstAxisToMatch, 
        int matchAxisNDigits, boolean shareInfo) throws Throwable {
        throw new SimpleException( 
            "Error: EDDGridLonPM180 doesn't support method=\"sibling\".");
    }

    /** 
     * This gets data (not yet standardized) from the data 
     * source for this EDDGrid.     
     * Because this is called by GridDataAccessor, the request won't be the 
     * full user's request, but will be a partial request (for less than
     * EDStatic.partialRequestMaxBytes).
     * 
     * @param tDataVariables EDV[] with just the requested data variables
     * @param tConstraints  int[nAxisVariables*3] 
     *   where av*3+0=startIndex, av*3+1=stride, av*3+2=stopIndex.
     *   AxisVariables are counted left to right, e.g., sst[0=time][1=lat][2=lon].
     * @return a PrimitiveArray[] where the first axisVariables.length elements
     *   are the axisValues and the next tDataVariables.length elements
     *   are the dataValues.
     *   Both the axisValues and dataValues are straight from the source,
     *   not modified.
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public PrimitiveArray[] getSourceData(EDV tDataVariables[], IntArray tConstraints) 
        throws Throwable {

        //parcel first dimension's constraints to appropriate datasets
        int nChildren = childStopsAt.length;
        int nAv = axisVariables.length;
        int nDv = tDataVariables.length;
        PrimitiveArray[] cumResults = null;
        int index = tConstraints.get(0);
        int stride = tConstraints.get(1);
        int stop = tConstraints.get(2);
        //find currentDataset (associated with index)
        int currentStart = index;
        int currentDataset = 0;  
        while (index > childStopsAt[currentDataset])
            currentDataset++;
        IntArray childConstraints = (IntArray)tConstraints.clone();

        //walk through the requested index values
        while (index <= stop) {
            //find nextDataset (associated with next iteration's index)
            int nextDataset = currentDataset; 
            while (nextDataset < nChildren && index + stride > childStopsAt[nextDataset])
                nextDataset++; //ok if >= nDatasets

            //get a chunk of data related to current chunk of indexes?
            if (nextDataset != currentDataset ||   //next iteration will be a different dataset
                index + stride > stop) {           //this is last iteration
                //get currentStart:stride:index
                int currentDatasetStartsAt = currentDataset == 0? 0 : childStopsAt[currentDataset - 1] + 1;
                childConstraints.set(0, currentStart - currentDatasetStartsAt);
                childConstraints.set(2, index - currentDatasetStartsAt);
                if (reallyVerbose) String2.log("  currentDataset=" + currentDataset +
                    "  datasetStartsAt=" + currentDatasetStartsAt + 
                    "  localStart=" + childConstraints.get(0) +
                    "  localStop=" + childConstraints.get(2));
                PrimitiveArray[] tResults = childDatasets[currentDataset].getSourceData(
                    tDataVariables, childConstraints);
                //childDataset has already checked that axis values are as *it* expects          
                if (cumResults == null) {
                    if (!ensureAxisValuesAreEqual  ..switch to matchAxisNDigits) {
                        //make axis values exactly as expected by aggregate dataset
                        for (int av = 1; av < nAv; av++)
                            tResults[av] = axisVariables[av].sourceValues().subset(
                                childConstraints.get(av * 3 + 0),
                                childConstraints.get(av * 3 + 1),
                                childConstraints.get(av * 3 + 2));
                    }
                    cumResults = tResults;
                } else {
                    cumResults[0].append(tResults[0]);
                    for (int dv = 0; dv < nDv; dv++)
                        cumResults[nAv + dv].append(tResults[nAv + dv]);
                }

                currentDataset = nextDataset;
                currentStart = index + stride;            
            }

            //increment index
            index += stride;
        }

        return cumResults;
    }


    /**
     * This tests the basic methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void testBasic() throws Throwable {

        String2.log("\n****************** EDDGridLonPM180.testBasic() *****************\n");
        testVerboseOn();
        String name, tName, userDapQuery, results, expected, error;

        //one time

        //*** NDBC  is also IMPORTANT UNIQUE TEST of >1 variable in a file
        EDDGrid gridDataset = (EDDGrid)oneFromDatasetXml("ndbcCWind41002");       

        //min max
        EDV edv = gridDataset.findAxisVariableByDestinationName("longitude");
        Test.ensureEqual(edv.destinationMin(), -75.42, "");
        Test.ensureEqual(edv.destinationMax(), -75.42, "");

        String ndbcDapQuery = "wind_speed[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, 
            EDStatic.fullTestCacheDirectory, gridDataset.className(), ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time, latitude, longitude, wind_speed\n" +
"UTC, degrees_north, degrees_east, m s-1\n" +
"1989-06-13T16:10:00Z, 32.27, -75.42, 15.7\n" +
"1989-06-13T16:20:00Z, 32.27, -75.42, 15.0\n" +
"1989-06-13T16:30:00Z, 32.27, -75.42, 14.4\n" +
"1989-06-13T16:40:00Z, 32.27, -75.42, 14.0\n" +
"1989-06-13T16:50:00Z, 32.27, -75.42, 13.6\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        ndbcDapQuery = "wind_speed[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className(), ".nc"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        int dataPo = results.indexOf("data:");
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        expected = 
"netcdf EDDGridLonPM180.nc {\n" +
" dimensions:\n" +
"   time = 5;\n" +   // (has coord.var)\n" +   //changed when switched to netcdf-java 4.0, 2009-02-23
"   latitude = 1;\n" +   // (has coord.var)\n" +
"   longitude = 1;\n" +   // (has coord.var)\n" +
" variables:\n" +
"   double time(time=5);\n" +
"     :_CoordinateAxisType = \"Time\";\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

//BUG???!!!
//"     :actual_range = 6.137568E8, 7.258458E8; // double\n" +  //this changes depending on how many files aggregated

expected = 
":axis = \"T\";\n" +
"     :ioos_category = \"Time\";\n" +
"     :long_name = \"Epoch Time\";\n" +
"     :short_name = \"time\";\n" +
"     :standard_name = \"time\";\n" +
"     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"   float latitude(latitude=1);\n" +
"     :_CoordinateAxisType = \"Lat\";\n" +
"     :actual_range = 32.27f, 32.27f; // float\n" +
"     :axis = \"Y\";\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Latitude\";\n" +
"     :short_name = \"latitude\";\n" +
"     :standard_name = \"latitude\";\n" +
"     :units = \"degrees_north\";\n" +
"   float longitude(longitude=1);\n" +
"     :_CoordinateAxisType = \"Lon\";\n" +
"     :actual_range = -75.42f, -75.42f; // float\n" +
"     :axis = \"X\";\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Longitude\";\n" +
"     :short_name = \"longitude\";\n" +
"     :standard_name = \"longitude\";\n" +
"     :units = \"degrees_east\";\n" +
"   float wind_speed(time=5, latitude=1, longitude=1);\n" +
"     :_FillValue = 99.0f; // float\n" +
"     :ioos_category = \"Wind\";\n" +
"     :long_name = \"Wind Speed\";\n" +
"     :missing_value = 99.0f; // float\n" +
"     :short_name = \"wspd\";\n" +
"     :standard_name = \"wind_speed\";\n" +
"     :units = \"m s-1\";\n" +
"\n" +
" :cdm_data_type = \"Grid\";\n" +
" :comment = \"S HATTERAS - 250 NM East of Charleston, SC\";\n" +
" :contributor_name = \"NOAA NDBC\";\n" +
" :contributor_role = \"Source of data.\";\n" +
" :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
" :Easternmost_Easting = -75.42f; // float\n" +
" :geospatial_lat_max = 32.27f; // float\n" +
" :geospatial_lat_min = 32.27f; // float\n" +
" :geospatial_lon_max = -75.42f; // float\n" +
" :geospatial_lon_min = -75.42f; // float\n" +
" :history = \"" + today + " http://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1989.nc\n" +
today + " " + EDStatic.erddapUrl + //in tests, always non-https url
"/griddap/ndbcCWind41002.nc?wind_speed[1:5][0][0]\";\n" +
" :infoUrl = \"http://www.ndbc.noaa.gov/cwind.shtml\";\n" +
" :institution = \"NOAA NDBC\";\n" +
" :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
" :location = \"32.27 N 75.42 W \";\n" +
" :Northernmost_Northing = 32.27f; // float\n" +
" :quality = \"Automated QC checks with manual editing and comprehensive monthly QC\";\n" +
" :sourceUrl = \"http://dods.ndbc.noaa.gov/thredds/dodsC/data/cwind/41002/41002c1989.nc\";\n" +
" :Southernmost_Northing = 32.27f; // float\n" +
" :station = \"41002\";\n" +
" :summary = \"These continuous wind measurements from the NOAA National Data Buoy Center (NDBC) stations are 10-minute average values of wind speed (in m/s) and direction (in degrees clockwise from North).\";\n" +
" :time_coverage_end = \"1989-06-13T16:50:00Z\";\n" +
" :time_coverage_start = \"1989-06-13T16:10:00Z\";\n" +
" :title = \"Wind Data from NDBC 41002\";\n" +
" :url = \"http://dods.ndbc.noaa.gov\";\n" +
" :Westernmost_Easting = -75.42f; // float\n" +
" data:\n" +
"time =\n" +
"  {6.137574E8, 6.13758E8, 6.137586E8, 6.137592E8, 6.137598E8}\n" +
"latitude =\n" +
"  {32.27}\n" +
"longitude =\n" +
"  {-75.42}\n" +
"wind_speed =\n" +
"  {\n" +
"    {\n" +
"      {15.7}\n" +
"    },\n" +
"    {\n" +
"      {15.0}\n" +
"    },\n" +
"    {\n" +
"      {14.4}\n" +
"    },\n" +
"    {\n" +
"      {14.0}\n" +
"    },\n" +
"    {\n" +
"      {13.6}\n" +
"    }\n" +
"  }\n" +
"}\n";
        int po = results.indexOf(":axis =");
        Test.ensureEqual(results.substring(po), expected, "results=\n" + results);

        ndbcDapQuery = "wind_direction[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "2", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time, latitude, longitude, wind_direction\n" +
"UTC, degrees_north, degrees_east, degrees_true\n" +
"1989-06-13T16:10:00Z, 32.27, -75.42, 234\n" +
"1989-06-13T16:20:00Z, 32.27, -75.42, 233\n" +
"1989-06-13T16:30:00Z, 32.27, -75.42, 233\n" +
"1989-06-13T16:40:00Z, 32.27, -75.42, 235\n" +
"1989-06-13T16:50:00Z, 32.27, -75.42, 235\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        ndbcDapQuery = "wind_speed[1:5][0][0],wind_direction[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "3", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time, latitude, longitude, wind_speed, wind_direction\n" +
"UTC, degrees_north, degrees_east, m s-1, degrees_true\n" +
"1989-06-13T16:10:00Z, 32.27, -75.42, 15.7, 234\n" +
"1989-06-13T16:20:00Z, 32.27, -75.42, 15.0, 233\n" +
"1989-06-13T16:30:00Z, 32.27, -75.42, 14.4, 233\n" +
"1989-06-13T16:40:00Z, 32.27, -75.42, 14.0, 235\n" +
"1989-06-13T16:50:00Z, 32.27, -75.42, 13.6, 235\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        ndbcDapQuery = "wind_direction[1:5][0][0],wind_speed[1:5][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "4", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time, latitude, longitude, wind_direction, wind_speed\n" +
"UTC, degrees_north, degrees_east, degrees_true, m s-1\n" +
"1989-06-13T16:10:00Z, 32.27, -75.42, 234, 15.7\n" +
"1989-06-13T16:20:00Z, 32.27, -75.42, 233, 15.0\n" +
"1989-06-13T16:30:00Z, 32.27, -75.42, 233, 14.4\n" +
"1989-06-13T16:40:00Z, 32.27, -75.42, 235, 14.0\n" +
"1989-06-13T16:50:00Z, 32.27, -75.42, 235, 13.6\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "4", ".nc"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        dataPo = results.indexOf("data:");
        results = results.substring(dataPo);
        expected = 
"data:\n" +
"time =\n" +
"  {6.137574E8, 6.13758E8, 6.137586E8, 6.137592E8, 6.137598E8}\n" +
"latitude =\n" +
"  {32.27}\n" +
"longitude =\n" +
"  {-75.42}\n" +
"wind_direction =\n" +
"  {\n" +
"    {\n" +
"      {234}\n" +
"    },\n" +
"    {\n" +
"      {233}\n" +
"    },\n" +
"    {\n" +
"      {233}\n" +
"    },\n" +
"    {\n" +
"      {235}\n" +
"    },\n" +
"    {\n" +
"      {235}\n" +
"    }\n" +
"  }\n" +
"wind_speed =\n" +
"  {\n" +
"    {\n" +
"      {15.7}\n" +
"    },\n" +
"    {\n" +
"      {15.0}\n" +
"    },\n" +
"    {\n" +
"      {14.4}\n" +
"    },\n" +
"    {\n" +
"      {14.0}\n" +
"    },\n" +
"    {\n" +
"      {13.6}\n" +
"    }\n" +
"  }\n" +
"}\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test seam of two datasets
        ndbcDapQuery = "wind_direction[22232:22239][0][0],wind_speed[22232:22239][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "5", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time, latitude, longitude, wind_direction, wind_speed\n" +
"UTC, degrees_north, degrees_east, degrees_true, m s-1\n" +
"1989-12-05T05:20:00Z, 32.27, -75.42, 232, 23.7\n" +
"1989-12-05T05:30:00Z, 32.27, -75.42, 230, 24.1\n" +
"1989-12-05T05:40:00Z, 32.27, -75.42, 225, 23.5\n" +
"1989-12-05T05:50:00Z, 32.27, -75.42, 233, 23.3\n" +
"1992-04-28T23:00:00Z, 32.27, -75.42, 335, 29.4\n" +
"1992-04-28T23:10:00Z, 32.27, -75.42, 335, 30.5\n" +
"1992-04-28T23:20:00Z, 32.27, -75.42, 330, 32.3\n" +
"1992-04-28T23:30:00Z, 32.27, -75.42, 331, 33.2\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

        //test seam of two datasets   with stride
        ndbcDapQuery = "wind_direction[22232:2:22239][0][0],wind_speed[22232:2:22239][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "6", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time, latitude, longitude, wind_direction, wind_speed\n" +
"UTC, degrees_north, degrees_east, degrees_true, m s-1\n" +
"1989-12-05T05:20:00Z, 32.27, -75.42, 232, 23.7\n" +
"1989-12-05T05:40:00Z, 32.27, -75.42, 225, 23.5\n" +
"1992-04-28T23:00:00Z, 32.27, -75.42, 335, 29.4\n" +
"1992-04-28T23:20:00Z, 32.27, -75.42, 330, 32.3\n";
        Test.ensureEqual(results, expected, "results=\n" + results);
        // */

        //test last data value (it causes a few problems -- including different axis values)
        ndbcDapQuery = "wind_direction[(2007-12-31T22:40:00):1:(2007-12-31T22:55:00)][0][0]," +
                           "wind_speed[(2007-12-31T22:40:00):1:(2007-12-31T22:55:00)][0][0]";
        tName = gridDataset.makeNewFileForDapQuery(null, null, ndbcDapQuery, EDStatic.fullTestCacheDirectory, 
            gridDataset.className() + "7", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"time, latitude, longitude, wind_direction, wind_speed\n" +
"UTC, degrees_north, degrees_east, degrees_true, m s-1\n" +
"2007-12-31T22:40:00Z, 32.27, -75.42, 36, 4.0\n" +
"2007-12-31T22:50:00Z, 32.27, -75.42, 37, 4.3\n";
        Test.ensureEqual(results, expected, "results=\n" + results);


        String2.log("\n*** EDDGridLonPM180.test finished.");

    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {

        String2.log("\n****************** EDDGridLonPM180.test() *****************\n");
        testBasic();

        //not usually run
    }



}
