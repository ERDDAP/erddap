/* 
 * EDDTableFromAsciiServiceNOS Copyright 2010, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.FloatArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;


/** 
 * This is a subclass of EDDTableFromAsciiService for the NOS Ascii services.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2010-11-12
 */
public class EDDTableFromAsciiServiceNOS extends EDDTableFromAsciiService { 

    protected Table stationTable;
    protected int stationIDCol, stationNameCol, stationLonCol, stationLatCol; 
    protected boolean datumIsFixedValue;


    /** The constructor. */
    public EDDTableFromAsciiServiceNOS(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File,
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tLocalSourceUrl,
        String tBeforeData[], String tAfterData, String tNoData)
        throws Throwable {

        super("EDDTableFromAsciiServiceNOS", tDatasetID, tAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix, 
            tDefaultDataQuery, tDefaultGraphQuery, tAddGlobalAttributes,
            tDataVariables,
            tReloadEveryNMinutes, tLocalSourceUrl,
            tBeforeData, tAfterData, tNoData);

        //datumIsFixedValue
        datumIsFixedValue = 
            tAddGlobalAttributes.getString("sourceUrl").indexOf("datum=") > 0 || //url specifies the datum
            String2.indexOf(dataVariableDestinationNames(), "datum") < 0;        //dataset doesn't use datum

        //find a user who is authorized to access this dataset
        String user = (accessibleTo == null || accessibleTo.length == 0)? null : accessibleTo[0]; 

        //make the stationTable (expected columns and no others) 
        //which is a subset of the subsetVariables table (which may have extra cols like 'datum')
        stationTable = subsetVariablesDataTable(user);  //exception if trouble
        stationTable.justKeepColumns(new String[]{"stationID", "stationName", "longitude", "latitude"},
            " It must be in <subsetVariables>.");
        stationTable.leftToRightSort(4);
        //remove duplicate rows  (e.g., perhaps caused by removing datum column)
        stationTable.removeDuplicates();
        //String2.log("\nstationTable=\n" + stationTable);

        // Not perfectly SOS-able: each offering's min/MaxTime isn't known.

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
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public void getDataForDapQuery(String loggedInAs, String requestUrl, 
        String userDapQuery, TableWriter tableWriter) throws Throwable {

        //make the sourceQuery
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        getSourceQueryFromDapQuery(userDapQuery,
            resultsVariables,
            constraintVariables, constraintOps, constraintValues); //timeStamp constraints other than regex are epochSeconds

        //go through the station table to find the stations of interest
        //and find datum= beginTime>= endTime<= 
        int nStations = stationTable.nRows();
        BitSet keep = new BitSet();
        keep.set(0, nStations, true);
        String datum = null;
        double beginSeconds = Double.NaN;
        //default endTime is an hour ahead of now (for safety)
        double endSeconds = Calendar2.gcToEpochSeconds(Calendar2.newGCalendarZulu()) + 3600;  
        int nConstraints = constraintVariables.size();
        for (int c = 0; c < nConstraints; c++) { 
            String conVar = constraintVariables.get(c);
            String conOp  = constraintOps.get(c);
            char conOp0  = conOp.charAt(0);
            String conVal = constraintValues.get(c);
            if (conVar.equals("datum")) {
                if (conOp.equals("="))
                    datum = constraintValues.get(c);
                continue;
            }
            if (conVar.equals("time")) {
                if (conOp0 == '>')
                    beginSeconds = String2.parseDouble(conVal);
                else if (conOp0 == '<')
                    endSeconds = String2.parseDouble(conVal);
                continue;
            }

            //is this constraint for a column that's in the stationTable?
            int nKeep = stationTable.tryToApplyConstraint(stationIDCol, conVar, conOp, conVal, keep);
            if (nKeep == 0)
                throw new SimpleException(MustBe.THERE_IS_NO_DATA +
                   " (There are no matching stations.)");
        }
        //ensure the required constraints were specified
        if (!datumIsFixedValue && (datum == null || datum.length() == 0))
            throw new SimpleException(
                EDStatic.queryError + "For this dataset, all queries must include a \"datum=\" constraint.");
        if (Double.isNaN(beginSeconds))
            throw new SimpleException(EDStatic.queryError + "Missing time>= constraint.");
        if (Double.isNaN(endSeconds))
            throw new SimpleException(EDStatic.queryError + "If present, the time<= constraint must be valid.");
        String beginTime = Calendar2.epochSecondsToIsoStringT(beginSeconds).substring(0, 16);  //no seconds
        String endTime   = Calendar2.epochSecondsToIsoStringT(  endSeconds).substring(0, 16);
        if (beginSeconds > endSeconds)
            throw new SimpleException("time>=" + beginTime + " must be before time<=" + endTime);
        if (endSeconds - beginSeconds > 30 * Calendar2.SECONDS_PER_DAY)
            throw new SimpleException("You must request a time range <= 30 days (begin=" + 
                beginTime + ", end=" + endTime + ").");
        beginTime = String2.replaceAll(beginTime, "-", "");
        endTime   = String2.replaceAll(  endTime, "-", "");
        beginTime = String2.replaceAll(beginTime, 'T', '+');  //percent encode T -> space -> +
        endTime   = String2.replaceAll(  endTime, 'T', '+');

        //get the data for all of the valid stations
        int stationRow = keep.nextSetBit(0);
        boolean errorPrinted = false;
        while (stationRow >= 0) {

            //format the query
            //http://opendap.co-ops.nos.noaa.gov/axis/webservices/waterlevelrawsixmin/
            //plain/response.jsp?stationId=9414290&beginDate=20101110+05:00&endDate=20101110+06:00
            //&datum=MLLW&unit=0&timeZone=0&metadata=yes&Submit=Submit
            String tStationID = stationTable.getStringData(stationIDCol, stationRow);
            String encodedSourceUrl = localSourceUrl +
                "&stationId=" + SSR.minimalPercentEncode(tStationID) + 
                "&beginDate=" + beginTime +
                "&endDate="   + endTime +
                (datumIsFixedValue? "" : "&datum=" + SSR.minimalPercentEncode(datum));

            try {
                //Open the file
                BufferedReader in = getBufferedReader(encodedSourceUrl);
                String s = in.readLine();

                //read stationName
                String stationName = null;
                while (s != null) {
                    //custom find() because two options
                    int po = s.indexOf("StationName");
                    if (po < 0) po = s.indexOf("Station Name");
                    if (po >= 0) {
                        po = s.indexOf(":", po + 11);
                        if (po < 0)
                            throw new SimpleException("':' not found after \"Station Name\"");
                        stationName = s.substring(po + 1).trim();
                        //if (reallyVerbose) String2.log("  found stationName=" + stationName);
                        s = in.readLine();
                        break;
                    }
                    s = in.readLine();
                }
                if (stationName == null)
                    throw new SimpleException("\"Station Name\" not found");

                //read latitude
                s = find(in, s, "Latitude",  "\"Latitude\" wasn't found");
                s = find(in, s, ":",         "\":\" wasn't found after \"Latitude\"");
                float tLatitude = String2.parseFloat(s);
                s = in.readLine();

                //read longitude
                s = find(in, s, "Longitude", "\"Longitude\" wasn't found");
                s = find(in, s, ":",         "\":\" wasn't found after \"Longitude\"");
                float tLongitude = String2.parseFloat(s);
                s = in.readLine();

                //read beforeData
                s = findBeforeData(in, s);

                //read the data
                Table table = getTable(in, s); //table PA's have different sizes!
                in.close();

                //table.makeColumnsSameSize();
                //String2.log("\npre table=\n" + table.dataToCSVString());

                if (datasetID.equals("nosCoopsWLTPHL")) {

                    //make newTable by breaking waterLevel into rows, 
                    //   each with time=HH:mm, WaterLevel=-0.4, type=L
                    //time has 11/24/2010   
                    //waterLevel = 02:24   -0.4  L     09:24    5.2  H     15:08    0.0  L     21:59    4.4  H  
                    Table newTable = makeEmptySourceTable(dataVariables, 32);
                    int timeCol = table.findColumnNumber("time");
                    int wlCol   = table.findColumnNumber("waterLevel");
                    int typeCol = table.findColumnNumber("type");
                    PrimitiveArray oTimePa = table.getColumn(timeCol);
                    PrimitiveArray oTypePa = table.getColumn(typeCol); //stored in 'type' because it's a string (WL isn't)
                    PrimitiveArray nTimePa = newTable.getColumn(timeCol);
                    PrimitiveArray nWLPa   = newTable.getColumn(wlCol);
                    PrimitiveArray nTypePa = newTable.getColumn(typeCol);
                    int nRows = oTimePa.size();
                    for (int row = 0; row < nRows; row++) {
                        String oTime = oTimePa.getString(row);
                        String oType = oTypePa.getString(row);
                        StringArray parts = StringArray.wordsAndQuotedPhrases(oType);
                        //part is the third value (so we know all 3 are available) 
                        for (int part = 2; part < parts.size(); part += 3) { 
                            nTimePa.addString(oTime + " " + parts.get(part - 2));
                            nWLPa.addString(parts.get(part - 1));
                            nTypePa.addString(parts.get(part));
                        }
                    }
                    //newTable.makeColumnsSameSize();
                    //String2.log("\nnewTable=\n" + newTable.dataToCSVString());

                    //swap newTable into place
                    table = newTable;
                }

                //figure out nRows (some cols have data, some don't)
                int nRows = 0;
                int nDV = table.nColumns();
                for (int dv = 0; dv < nDV; dv++) {
                    nRows = table.getColumn(dv).size();                    
                    if (nRows > 0)
                        break;
                }

                if (nRows > 0) {

                    //do standard processing
                    //fill the stationID column
                    StringArray sa = (StringArray)table.getColumn("stationID");
                    sa.clear();
                    sa.addN(nRows, tStationID);

                    //fill the stationName column
                    sa = (StringArray)table.getColumn("stationName");
                    sa.clear();
                    sa.addN(nRows, stationName);

                    //fill the longitude column
                    FloatArray fa = (FloatArray)table.getColumn("longitude");
                    fa.clear();
                    fa.addN(nRows, tLongitude);

                    //fill the latitude column
                    fa = (FloatArray)table.getColumn("latitude");
                    fa.clear();
                    fa.addN(nRows, tLatitude);

                    int datumCol = String2.indexOf(dataVariableDestinationNames(), "datum");
                    if (datumIsFixedValue) {
                        //if it exists, remove it.  standardizeResultsTable will add it
                        if (datumCol >= 0)
                            table.removeColumn(datumCol);
                    } else {
                        //fill the datum column
                        sa = (StringArray)table.getColumn(datumCol);
                        sa.clear();
                        sa.addN(nRows, datum);
                    }

                    //String2.log("\npost table=\n" + table.dataToCSVString());
                    standardizeResultsTable(requestUrl, userDapQuery, table);

                    if (table.nRows() > 0) {
                        tableWriter.writeSome(table);
                        //it isn't a loop, so no need to call makeEmptySourceTable again
                    }
                }

            } catch (Throwable t) {
                EDStatic.rethrowClientAbortException(t);  //first thing in catch{}

                //basically ignore errors?!  (Or should a single error stop the request?)
                if (verbose) {
                    String2.log(String2.ERROR + " for stationID=" + tStationID + " " +
                        (errorPrinted? "" :
                            encodedSourceUrl + "\n" + MustBe.throwableToString(t)));
                    errorPrinted = true;
                }
            }

            stationRow = keep.nextSetBit(stationRow + 1);
            if (tableWriter.noMoreDataPlease) 
                tableWriter.logCaughtNoMoreDataPlease(datasetID);
                break;
        }

        tableWriter.finish();
    }


    /* * 
     * This does its best to generate a read-to-use datasets.xml entry for an
     * EDDTableFromAsciiServiceNOS.
     * <br>The XML can then be edited by hand and added to the datasets.xml file.
     * <br>This uses the first outerSequence (and if present, first innerSequence) found.
     * <br>Other sequences are skipped.
     *
     * @param tLocalSourceUrl
     * @param tReloadEveryNMinutes  must be a valid value, e.g., 1440 for once per day. 
     *    Use, e.g., 1000000000, for never reload.
     * @param externalGlobalAttributes globalAttributes gleaned from external 
     *    sources, e.g., a THREDDS catalog.xml file.
     *    These have priority over other sourceGlobalAttributes.
     *    Okay to use null if none.
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
/*    public static String generateDatasetsXml(String tLocalSourceUrl, 
        int tReloadEveryNMinutes, Attributes externalGlobalAttributes) 
        //String outerSequenceName, String innerSequenceName, boolean sortColumnsByName) 
        throws Throwable {

        String2.log("EDDTableFromAsciiServiceNOS.generateDatasetsXml" +
            "\n  tLocalSourceUrl=" + tLocalSourceUrl);
        String tPublicSourceUrl = convertToPublicSourceUrl(tLocalSourceUrl);


        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
    } */


    
    private static String stationsFileName = "c:/programs/nos/stations.xml";

    /** Bob uses this to reload the Water Level and Meteorological Capabilities document
      * and store it in stationsFileName. */
    public static void reloadStationsFile() throws Throwable {
        String2.log("reloadStationsFile()");

        //get WaterLevel and Meteorological station list from SOAP list at
        //http://opendap.co-ops.nos.noaa.gov/axis/webservices/activestations/response.jsp
        //from Active Water Level Stations Try Me / XML at
        //http://opendap.co-ops.nos.noaa.gov/axis/
        SSR.downloadFile(
            "http://opendap.co-ops.nos.noaa.gov/axis/webservices/activestations/response.jsp",
            stationsFileName, true);
    }


    /** 
     * Bob uses this to get the lookFor (e.g., "Water Level" or "" for any)
     * stations from the stationsFileName.
     *
     * @return a table with stationID, stationName, longitude, latitude
     */
    public static Table lookForStations(String lookFor) throws Exception {
        String2.log("lookForStations(" + lookFor + ")");

//        <parameter DCP="1" name="Water Level" sensorID="V1" status="1"/>
//</station>
//<station ID="8410140" name="Eastport">
//<lat>21.9544</lat>
//<long>-159.3561</long>
        StringArray stationID = new StringArray();
        StringArray stationName = new StringArray();
        FloatArray longitude = new FloatArray();
        FloatArray latitude = new FloatArray();
        Table table = new Table();
        table.addColumn("stationID", stationID);
        table.addColumn("stationName", stationName);
        table.addColumn("longitude", longitude);
        table.addColumn("latitude", latitude);
        String s2[] = String2.readFromFile(stationsFileName);
        String stationsXML = s2[1];
        int stationIDPo = stationsXML.indexOf("<station ID=\"");
        while (stationIDPo > 0) {
            int stationIDEnd   = stationsXML.indexOf('\"',      stationIDPo + 13);
            int stationNamePo  = stationsXML.indexOf("name=\"", stationIDEnd + 1);
            int stationNameEnd = stationsXML.indexOf('\"',      stationNamePo + 6);
            int latPo          = stationsXML.indexOf("<lat>",   stationNameEnd + 1);
            int latEnd         = stationsXML.indexOf("</lat>",  latPo + 5);
            int lonPo          = stationsXML.indexOf("<long>",  latEnd + 6);
            int lonEnd         = stationsXML.indexOf("</long>", lonPo + 6);
            int lookForPo      = lookFor.length() == 0? (lonEnd + 7) :
                                 stationsXML.indexOf("name=\"" + lookFor + "\"", lonEnd + 7);
            int nextStationIDPo = stationsXML.indexOf("<station ID=\"", stationIDPo + 13);

            String tStationID = stationsXML.substring(stationIDPo + 13, stationIDEnd);

            if (stationIDEnd < 0 || stationNamePo < 0 || stationNameEnd < 0 ||
                latPo < 0 || latEnd < 0 || lonPo < 0 || lonEnd < 0 ||
                (nextStationIDPo > 0 && lonEnd > nextStationIDPo)) 
                throw new Exception("trouble: stationID=" + tStationID + 
                    " stationIDPo=" + stationIDPo + 
                    " stationIDEnd=" + stationIDEnd + 
                    " stationNamePo=" + stationNamePo +
                    " latPo=" + latPo + " latEnd=" + latEnd +
                    " lonPo=" + lonPo + " lonEnd=" + lonEnd +
                    " nextStationIDPo=" + nextStationIDPo); 

            //does lookFor occur before start of next station?
            if (lookForPo < nextStationIDPo ||
                (nextStationIDPo == -1 && lookForPo > 0)) { //last station
                stationID.add(tStationID);
                stationName.add(                 stationsXML.substring(stationNamePo + 6, stationNameEnd));
                longitude.add(String2.parseFloat(stationsXML.substring(lonPo + 6, lonEnd)));
                latitude.add( String2.parseFloat(stationsXML.substring(latPo + 5, latEnd)));
            }

            stationIDPo = nextStationIDPo;
        }
        //String2.log(stationID1.toString());
        String2.log("n " + lookFor + " Stations=" + table.dataToCSVString());
        return table;
    }



    /** 
     * Bob uses this to make the NOS COOPS Active Currents subset file.
     *
     */
    public static void makeNosActiveCurrentsSubsetFile() throws Exception {
        String2.log("makeNosActiveCurrentsSubsetTable");

        StringArray stationID = new StringArray();
        StringArray stationName = new StringArray();
        FloatArray longitude = new FloatArray();
        FloatArray latitude = new FloatArray();
        Table table = new Table();
        table.addColumn("stationID", stationID);
        table.addColumn("stationName", stationName);
        table.addColumn("longitude", longitude);
        table.addColumn("latitude", latitude);
        String tStationID = null;
        String tStationName = null;
        Float tLongitude = Float.NaN;
        Float tLatitude = Float.NaN;

        //get xml file by hand from 
        //http://opendap.co-ops.nos.noaa.gov/axis/webservices/activecurrentstations/index.jsp
        //save as  
        String fileName = "c:/programs/nos/ActiveCurrentsStations.xml";
        //last done 2010-11-23

        //read from file
        InputStream in = new FileInputStream(fileName);
        SimpleXMLReader xmlReader = new SimpleXMLReader(in, "soapenv:Envelope");
        while (true) {
            xmlReader.nextTag();
            String tag = xmlReader.topTag();
            //String2.log("  tag=" + tag);
            if (tag.equals("/soapenv:Envelope"))
                break; 

            //<station ID="aa0101" name="OSTEP1">
            //  <deployment long="-76.308327" deployed="2009-03-24 19:18:00.0" lat="36.8572"/>
            if (tag.equals("station")) {
                tStationID   = xmlReader.attributeValue("ID");
                tStationName = xmlReader.attributeValue("name");
            } else if (tag.equals("/station")) {
                String2.log(tStationID + " " + tStationName + " " + tLongitude + " " + tLatitude);
                if (tStationID != null && tStationName != null &&
                    !Float.isNaN(tLongitude) && !Float.isNaN(tLatitude))
                stationID.add(tStationID);
                stationName.add(tStationName);
                longitude.add(tLongitude);
                latitude.add(tLatitude);
            } else if (tag.equals("deployment")) {
                //there are usually several deployments, but only last is remembered
                //NOT GOOD: if service makes older data available, long lat returned will be slightly off for them
                tLongitude = String2.parseFloat(xmlReader.attributeValue("long"));
                tLatitude  = String2.parseFloat(xmlReader.attributeValue("lat"));
            }
        }
        in.close();
        table.leftToRightSort(1);
        String dir = "c:/programs/tomcat/content/erddap/subset/";
        table.saveAsJson(dir + "nosCoopsCA.json",  -1, false); //timeColumn, writeUnits
        String2.log(table.dataToCSVString());
    }

    /**
     * This makes the Meteorological station table for many nosCoops M datasets
     * on Bob's computer.
     *
     * @throws Throwable if trouble
     */
    public static void makeNosCoopsMetSubsetFiles(boolean reloadStationsFile) throws Throwable {
        String2.log("\n****************** EDDTableFromAsciiServiceNOS.makeNosCoopsMetSubsetFiles\n");


        //reload the Capabilities document
        if (reloadStationsFile)
            reloadStationsFile();
        String dir = "c:/programs/tomcat/content/erddap/subset/";

        lookForStations("Air Temp"     ).saveAsJson(dir + "nosCoopsMAT.json",  -1, false);
        lookForStations("Air Pressure" ).saveAsJson(dir + "nosCoopsMBP.json",  -1, false);
        lookForStations("Conductivity" ).saveAsJson(dir + "nosCoopsMC.json",  -1, false);
        lookForStations("Humidity"     ).saveAsJson(dir + "nosCoopsMRH.json",  -1, false);
        lookForStations("Water Temp"   ).saveAsJson(dir + "nosCoopsMWT.json",  -1, false);
        lookForStations("Winds"        ).saveAsJson(dir + "nosCoopsMW.json",  -1, false);
        lookForStations("Visibility"   ).saveAsJson(dir + "nosCoopsMV.json",  -1, false);

        //rainfall has no related property
        //so create from entire station file and just keep stations listed at
        //http://opendap.co-ops.nos.noaa.gov/axis/webservices/rainfall/index.jsp
        Table table = lookForStations(""); 
        int nRows = table.oneStepApplyConstraint(0,
            "stationID", "=~", "(9752619|9753216|9754228|9757809|9758053|9759394)");
        table.saveAsJson(dir + "nosCoopsMRF.json",  -1, false);
        Test.ensureEqual(nRows, 6, "Rain Fall");

        String2.log("\n*** Done.");
        
    }


    /**
     * This makes the WaterLevel station table for many nosCoops WL datasets
     * on Bob's computer.
     *
     * @throws Throwable if trouble
     */
    public static void makeNosCoopsWLSubsetFiles(boolean reloadStationsFile) throws Throwable {
        String2.log("\n****************** EDDTableFromAsciiServiceNOS.makeNosCoopsWLSubsetFiles\n");


        //reload the Capabilities document
        if (reloadStationsFile)
            reloadStationsFile();

        StringArray stationID1 = (StringArray)(lookForStations("Water Level").getColumn(0));       
        
        // was based on soap response
        //BufferedInputStream bis = new BufferedInputStream(
        //    SSR.getUrlInputStream(
        //    "http://opendap.co-ops.nos.noaa.gov/ioos-dif-sos/SOS?service=SOS&request=GetCapabilities"));
        //    //new FileInputStream("c:/programs/nos/stations.xml"));  //for testing
        //Table table1 = EDDTableFromSOS.getStationTable(bis, 
        //    "http://mmisw.org/ont/cf/parameter/water_level");
        //bis.close();
        //int nRows1 = table1.nRows();
        //StringArray stationID1 = (StringArray)table1.getColumn(0);

        //for each station, get available datums from the datum service
        Table table2 = new Table();
        StringArray stationID = new StringArray();
        StringArray stationName = new StringArray();
        FloatArray stationLon = new FloatArray();
        FloatArray stationLat = new FloatArray();
        StringArray stationDatum = new StringArray();
        //column names are dataset destinationNames
        table2.addColumn("stationID",   stationID);
        table2.addColumn("stationName", stationName);
        table2.addColumn("longitude",   stationLon);
        table2.addColumn("latitude",    stationLat);
        table2.addColumn("datum",       stationDatum);

        HashMap datumsHash = new HashMap();
        int nStations = stationID1.size();
        for (int row = 0; row < nStations; row++) {
            try {
                String tStationID = stationID1.get(row);
                String url = "http://opendap.co-ops.nos.noaa.gov/axis/webservices/datums/plain/response.jsp?" +
                    "epoch=A&unit=0&metadata=yes&Submit=Submit&stationId=" + tStationID;
                String2.log(url);
                String content[] = SSR.getUrlResponse(url);
/*
   Station Id         :   9414290
   Station Name       :   San Francisco
   State              :   CA
   Latitude           :   37.8067
   Longitude          :   -122.465
   Unit               :   Meters
   Status             :   Accepted
   Epoch              :   Current (1983-2001)
...
<b>Datum          Value          Description</b>
==========================================================================================================
<pre>
MHHW           3.602          Mean Higher-High Water 
MHW            3.416          Mean High Water 
*/
                //no datums? e.g., 8311062
                int po = String2.lineContaining(content, 
                    "but no Datums data is available from this station", 0);
                if (po >= 0) {String2.log("  No datums data for this station."); continue;}

                po = String2.lineContaining(content, "Station Name       :", 0);
                if (po < 0) {String2.log("Station Name not found"); continue;}
                String tStationName = content[po].substring(26).trim();

                po = String2.lineContaining(content, "Latitude", po + 1);
                if (po < 0) {String2.log("Latitude not found"); continue;}
                float tStationLat = String2.parseFloat(content[po].substring(26));

                po = String2.lineContaining(content, "Longitude", po + 1);
                if (po < 0) {String2.log("Longitude not found"); continue;}
                float tStationLon = String2.parseFloat(content[po].substring(26));

                po = String2.lineContaining(content, "<pre>", po + 1);
                if (po < 0) {String2.log("<pre> not found"); continue;}
                po++;
                boolean foundSome = false;
                while (po < content.length && content[po].length() > 1) {
                    content[po] += "                                        ";
                    String tDatum = content[po].substring(0, 10).trim();
                    String tValue = content[po].substring(15, 20).trim();
                    if (tValue.length() > 0) {
                        String description = content[po].substring(30).trim();
                        datumsHash.put(tDatum, description);
                        stationID.add(   tStationID);
                        stationName.add( tStationName);
                        stationLon.add(  tStationLon);
                        stationLat.add(  tStationLat);
                        stationDatum.add(tDatum);
                        foundSome = true;
                    }
                    po++;
                }
                //2010-11-18 email from Mohamed.Chaouchi@noaa.gov says 
                // "The majority of the station will support STND but not all of them."
                if (foundSome) {
                    stationID.add(   tStationID);
                    stationName.add( tStationName);
                    stationLon.add(  tStationLon);
                    stationLat.add(  tStationLat);
                    stationDatum.add("STND");
                }

            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }
        }

        //sort
        table2.leftToRightSort(5);

        //print a little of table2
        String2.log(table2.dataToCSVString(30));

        //print datumDescriptions
        Table datumDesc = new Table();
        datumDesc.readMap(datumsHash, "Datum", "Description");
        String2.log(datumDesc.dataToCSVString());

        String dir = "c:/programs/tomcat/content/erddap/subset/";
        table2.saveAsJson(dir + "nosCoopsWLR6.json",  -1, false);
        table2.saveAsJson(dir + "nosCoopsWLR1.json",  -1, false);
        table2.saveAsJson(dir + "nosCoopsWLV6.json",  -1, false);
        table2.saveAsJson(dir + "nosCoopsWLV60.json", -1, false);
        table2.saveAsJson(dir + "nosCoopsWLVHL.json", -1, false);
        table2.saveAsJson(dir + "nosCoopsWLVDM.json", -1, false);

        //remove datum other than MLLW
        table2.oneStepApplyConstraint(0, "datum", "=", "MLLW");
        table2.saveAsJson(dir + "nosCoopsWLTPHL.json",  -1, false);
        table2.saveAsJson(dir + "nosCoopsWLTP6.json",  -1, false);
        table2.saveAsJson(dir + "nosCoopsWLTP60.json",  -1, false);

        String2.log("\n*** Done.  nRows=" + table2.nRows());
        
    }


    /** This tests nosCoops Water Level datasets. 
These datasets are based on plain text responses from forms listed at
http://opendap.co-ops.nos.noaa.gov/axis/text.html
(from "text" link at bottom of http://opendap.co-ops.nos.noaa.gov/axis/ )
These datasets were hard to work with:
* Different services support different stations (WLVDM is very diffent).
* Different stations support different datums.
* The web service which indicates which station supports which datum
  is imperfect (see above; e.g., it never mentions STND which most, not all, support).
* For the web services, the parameters sometimes change form:
  nosCoopsWLHLTP uses datum 0=MLLW 1=STND, others use acronym
* The responses are sometimes inconsistent
  some say UTC, some GMT
  some say Feet when the return values are Meters (nosCoopsWLR1)
  some respond "Station Name", some "StationName"
* For the soap services, there is no RESTfull (URL) access. So harder to work with.
* There is a time limit of 30 day's worth of data
* Approx. when is the latest available Verified data?
* The Great Lakes stations have completely separate services:
  http://tidesandcurrents.noaa.gov/station_retrieve.shtml?type=Great+Lakes+Water+Level+Data
     *
     * @param idRegex is a regex indicating which datasetID's should be tested (".*" for all)
     */
    public static void testNosCoops(String idRegex) throws Throwable {
        String2.log("\n****************** EDDTableFromAsciiServiceNOS.testNosCoopsWL\n");
        testVerboseOn();
EDD.debugMode=true;
        String results, query, tName, expected;
        //(0, 11) causes all of these to end in 'T'
        String yesterday = Calendar2.epochSecondsToIsoStringT(  
            Calendar2.backNDays(1, Double.NaN)).substring(0, 11); 
        String daysAgo5 = Calendar2.epochSecondsToIsoStringT(   
            Calendar2.backNDays(5, Double.NaN)).substring(0, 11); 
        String daysAgo10 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(10, Double.NaN)).substring(0, 11);
        String daysAgo20 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(20, Double.NaN)).substring(0, 11);
        String daysAgo30 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(30, Double.NaN)).substring(0, 11);
        String daysAgo40 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(40, Double.NaN)).substring(0, 11);
        String daysAgo70 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(70, Double.NaN)).substring(0, 11);
        String daysAgo72 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(72, Double.NaN)).substring(0, 11);
        String daysAgo90 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(90, Double.NaN)).substring(0, 11);
        String daysAgo92 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(92, Double.NaN)).substring(0, 11);

        String daysAhead5 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(-5, Double.NaN)).substring(0, 11);
        String daysAhead7 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(-7, Double.NaN)).substring(0, 11);

/* */
        //Raw 6 minute          
        if ("nosCoopsWLR6".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsWLR6"); 

            query = "&stationID=\"9414290\"&datum=\"MLLW\"&time>=" + yesterday + 
                "21:00&time<=" + yesterday + "22:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected =   //this changes every day
"stationID,stationName,longitude,latitude,time,datum,dcp,sensor,waterLevel,sigma,O,F,R,L\n" +
",,degrees_east,degrees_north,UTC,,,,m,m,count,,,\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:00:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:06:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:12:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:18:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:24:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:30:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:36:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:42:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:48:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:54:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "22:00:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,4}|NaN),NaN,0,0,0\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Raw 1 minute
        if ("nosCoopsWLR1".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsWLR1"); 
            query = "&stationID=\"9414290\"&datum=\"MLLW\"&time>=" + yesterday + 
                "21:00&time<=" + yesterday + "21:10";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,datum,dcp,sensor,waterLevel\n" +
",,degrees_east,degrees_north,UTC,,,,m\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:00:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN)\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:01:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN)\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:02:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN)\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:03:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN)\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:04:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN)\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:05:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN)\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:06:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN)\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:07:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN)\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:08:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN)\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:09:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN)\n" +
"9414290,San Francisco,-122.465,37.8067," + yesterday + "21:10:00Z,MLLW,1,..,([\\-\\.\\d]{1,6}|NaN)\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsLR1" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Verified 6 minute
        if ("nosCoopsWLV6".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsWLV6"); 

            //2012-11-14 and 2013-11-01 Have to experiment a lot to get actual data. (Gap in Early 2013-10?)
            //list of stations: http://opendap.co-ops.nos.noaa.gov/stations/index.jsp
            //their example=8454000  SF=9414290  Honolulu=1612340
            String daysAgo = daysAgo40;
            query = "&stationID=\"9414290\"&datum=\"MLLW\"&time>=" + daysAgo + "21:00" +
                                                         "&time<=" + daysAgo + "22:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = //changes every day
"stationID,stationName,longitude,latitude,time,datum,waterLevel,sigma,I,F,R,L\n" +
",,degrees_east,degrees_north,UTC,,m,m,,,,\n" +
"9414290,San Francisco,-122.465,37.8067," + daysAgo + "21:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + daysAgo + "21:06:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + daysAgo + "21:12:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + daysAgo + "21:18:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + daysAgo + "21:24:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + daysAgo + "21:30:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + daysAgo + "21:36:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + daysAgo + "21:42:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + daysAgo + "21:48:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + daysAgo + "21:54:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067," + daysAgo + "22:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0,0,0\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsWLV6" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Verified hourly
        if ("nosCoopsWLR60".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsWLV60"); 
            //2012-11-14 and 2013-11-01 Have to experiment a lot to get actual data. (Gap in Early 2013-10?)
            //list of stations: http://opendap.co-ops.nos.noaa.gov/stations/index.jsp
            //their example=8454000  SF=9414290  Honolulu=1612340
            //2012-11-14 was stationID=9044020  but that stopped working
            String daysAgo = daysAgo40;
            query = "&stationID=\"8454000\"&datum=\"MLLW\"&time>=" + daysAgo + 
                                                    "14:00&time<=" + daysAgo + "23:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,datum,waterLevel,sigma,I,L\n" +
",,degrees_east,degrees_north,UTC,,m,m,,\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "14:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "15:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "16:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "17:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "18:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "19:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "20:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "21:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "22:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "23:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN),0,0\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsWLV60" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Verified high low
        if ("nosCoopsWLVHL".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsWLVHL"); 
            //no recent data.  always ask for daysAgo72 ... daysAgo70
            //2012-11-14 was stationID=9044020  but that stopped working
            //2012-11-14 and 2013-11-01 Have to experiment a lot to get actual data. (Gap in Early 2013-10?)
            //list of stations: http://opendap.co-ops.nos.noaa.gov/stations/index.jsp
            //their example=8454000  SF=9414290  Honolulu=1612340
            String daysAgo = daysAgo72;
            query = "&stationID=\"8454000\"&datum=\"MLLW\"&time>=" + daysAgo + 
                                                    "00:00&time<=" + daysAgo + "23:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //number of lines in results varies
            String sar[] = String2.split(results, '\n');
            expected = 
"stationID,stationName,longitude,latitude,time,datum,waterLevel,type,I,L\n" +
",,degrees_east,degrees_north,UTC,,m,,,\n";
            for (int i = 2; i < sar.length - 1; i++)
                expected += 
"8454000,Providence,-71.4012,41.8071," + daysAgo + "..:..:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),.{1,2},0,0\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsWLVHL" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }



        //Tide Predicted High Low 
        //I needed a different way to specify datum (just 0=MLLW or 1=STND)
        //  so always use datum=0; don't let user specify datum
        //works: http://opendap.co-ops.nos.noaa.gov/axis/webservices/highlowtidepred/plain/response.jsp?
        //unit=0&timeZone=0&metadata=yes&Submit=Submit&stationId=8454000&beginDate=20101123+00:00&endDate=20101125+00:00&datum=0
        //RESULT FORMAT IS VERY DIFFERENT  so this class processes this dataset specially
        if ("nosCoopsWLTPHL".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsWLTPHL"); 

            String daysAhead = daysAhead5;
            query = "&stationID=\"8454000\"&time>=" + daysAhead + 
                                     "00:00&time<=" + daysAhead + "23:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //number of lines in results varies
            String sar[] = String2.split(results, '\n');
            expected = 
"stationID,stationName,longitude,latitude,time,datum,waterLevel,type\n" +
",,degrees_east,degrees_north,UTC,,m,\n";
            for (int i = 2; i < sar.length - 1; i++)
                expected += 
"8454000,Providence,-71.4012,41.8071," + daysAhead + "..:..:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN),.{1,2}\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsWLTPHL results change every day (5-7 days ahead)." +
                "\nIs the response reasonable?" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }


        //Tide Predicted   6 minute
        //I needed a different way to specify datum (just 0=MLLW or 1=STND)
        //  so always use datum=0; don't let user specify datum
        //And always use dataInterval=6 (minutes).
        //works:http://opendap.co-ops.nos.noaa.gov/axis/webservices/predictions/plain/response.jsp?
        //unit=0&timeZone=0&datum=0&dataInterval=6&beginDate=20101122+00:00&endDate=20101122+02:00
        //&metadata=yes&Submit=Submit&stationId=1611400
        if ("nosCoopsWLTP6".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsWLTP6"); 

            String daysAhead = daysAhead5;
            query = "&stationID=\"8454000\"&datum=\"MLLW\"&time>=" + daysAhead + 
                                                    "00:00&time<=" + daysAhead + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,datum,predictedWL\n" +
",,degrees_east,degrees_north,UTC,,m\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "00:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "00:06:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "00:12:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "00:18:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "00:24:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "00:30:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "00:36:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "00:42:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "00:48:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "00:54:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "01:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsWLTP6" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }


        //Tide Predicted   60 minute
        //I needed a different way to specify datum (just 0=MLLW or 1=STND)
        //  so always use datum=0; don't let user specify datum
        //And always use dataInterval=60 (minutes).
        //works:http://opendap.co-ops.nos.noaa.gov/axis/webservices/predictions/plain/response.jsp?
        //unit=0&timeZone=0&datum=0&dataInterval=60&beginDate=20101122+00:00&endDate=20101122+02:00
        //&metadata=yes&Submit=Submit&stationId=1611400
        if ("nosCoopsWLTP60".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsWLTP60"); 

            String daysAhead = daysAhead5;
            query = "&stationID=\"8454000\"&time>=" + daysAhead + 
                                     "00:00&time<=" + daysAhead + "10:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,datum,predictedWL\n" +
",,degrees_east,degrees_north,UTC,,m\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "00:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "01:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "02:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "03:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "04:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "05:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "06:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "07:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "08:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "09:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n" +
"8454000,Providence,-71.4012,41.8071," + daysAhead + "10:00:00Z,MLLW,([\\-\\.\\d]{1,6}|NaN)\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsWLTP60" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Air Temperature
        //works http://opendap.co-ops.nos.noaa.gov/axis/webservices/airtemperature/plain/response.jsp?
        //timeZone=0&metadata=yes&Submit=Submit&stationId=8454000
        //&beginDate=20101024+00:00&endDate=20101026+00:00
        if ("nosCoopsMAT".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsMAT"); 

            String daysAgo = yesterday;
            query = "&stationID=\"8454000\"&time>=" + daysAgo + 
                                     "00:00&time<=" + daysAgo + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,dcp,sensor,AT,X,N,R\n" +
",,degrees_east,degrees_north,UTC,,,degree_C,,,\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:00:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:06:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:12:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:18:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:24:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:30:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:36:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:42:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:48:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:54:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "01:00:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMAT" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Barometric Pressure
        //works http://opendap.co-ops.nos.noaa.gov/axis/webservices/barometricpressure/plain/response.jsp?
        //timeZone=0&metadata=yes&Submit=Submit&stationId=8454000
        //&beginDate=20101024+00:00&endDate=20101026+00:00
        if ("nosCoopsMBP".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsMBP"); 

            String daysAgo = yesterday;
            query = "&stationID=\"8454000\"&time>=" + daysAgo + 
                                     "00:00&time<=" + daysAgo + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,dcp,sensor,BP,X,N,R\n" +
",,degrees_east,degrees_north,UTC,,,hPa,,,\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:00:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:06:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:12:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:18:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:24:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:30:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:36:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:42:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:48:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:54:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "01:00:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMBP" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Conductivity
        //works http://opendap.co-ops.nos.noaa.gov/axis/webservices/conductivity/plain/response.jsp?
        //timeZone=0&metadata=yes&Submit=Submit&stationId=8454000
        //&beginDate=20101024+00:00&endDate=20101026+00:00
        if ("nosCoopsMC".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsMC"); 

            String id =     "9414290"; //"8454000";
            String cityLL = ",San Francisco,-122.465,37.8067,"; // ",Providence,-71.4012,41.8071,";
            String daysAgo = daysAgo40;
            query = "&stationID=\"" + id + "\"&time>=" + daysAgo + 
                                        "00:00&time<=" + daysAgo + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,dcp,sensor,CN,X,N,R\n" +
",,degrees_east,degrees_north,UTC,,,mS/cm,,,\n" +
id + cityLL + daysAgo + "00:00:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
id + cityLL + daysAgo + "00:06:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
id + cityLL + daysAgo + "00:12:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
id + cityLL + daysAgo + "00:18:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
id + cityLL + daysAgo + "00:24:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
id + cityLL + daysAgo + "00:30:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
id + cityLL + daysAgo + "00:36:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
id + cityLL + daysAgo + "00:42:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
id + cityLL + daysAgo + "00:48:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
id + cityLL + daysAgo + "00:54:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n" +
id + cityLL + daysAgo + "01:00:00Z,1,..,([\\-\\.\\d]{1,6}|NaN),0,0,0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String msg = MustBe.throwableToString(t);
            if (msg.indexOf("no matching results") >= 0)
                Test.knownProblem(
                    "2013-11-01 After trying several variations, I couldn't find data for this dataset.",
                    msg);
            else 
                String2.getStringFromSystemIn("\n" + msg + 
                    "\n*** 2012-11-14 their example with this station says \"no data is available from this station\"" +
                    "\n*** nosCoopsMC" +
                    "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Rain Fall
        //works http://opendap.co-ops.nos.noaa.gov/axis/webservices/rainfall/plain/response.jsp?
        //timeZone=0&metadata=yes&Submit=Submit&stationId=9752619
        //&beginDate=20101024+00:00&endDate=20101026+00:00
        if ("nosCoopsMRF".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsMRF"); 

            String daysAgo = yesterday;
            query = "&stationID=\"9752619\"&time>=" + daysAgo + 
                                     "00:00&time<=" + daysAgo + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,dcp,sensor,RF,X,R\n" +
",,degrees_east,degrees_north,UTC,,,mm,,\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525," + daysAgo + "00:00:00Z,1,J1,\\d\\.\\d,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525," + daysAgo + "00:06:00Z,1,J1,\\d\\.\\d,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525," + daysAgo + "00:12:00Z,1,J1,\\d\\.\\d,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525," + daysAgo + "00:18:00Z,1,J1,\\d\\.\\d,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525," + daysAgo + "00:24:00Z,1,J1,\\d\\.\\d,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525," + daysAgo + "00:30:00Z,1,J1,\\d\\.\\d,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525," + daysAgo + "00:36:00Z,1,J1,\\d\\.\\d,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525," + daysAgo + "00:42:00Z,1,J1,\\d\\.\\d,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525," + daysAgo + "00:48:00Z,1,J1,\\d\\.\\d,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525," + daysAgo + "00:54:00Z,1,J1,\\d\\.\\d,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525," + daysAgo + "01:00:00Z,1,J1,\\d\\.\\d,0,0\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMRF" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Relative Humidity
        //works http://opendap.co-ops.nos.noaa.gov/axis/webservices/relativehumidity/plain/response.jsp?
        //timeZone=0&metadata=yes&Submit=Submit&stationId=9063063
        //&beginDate=20101024+00:00&endDate=20101026+00:00
        if ("nosCoopsMRH".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsMRH"); 

            String daysAgo = yesterday;
            query = "&stationID=\"9063063\"&time>=" + daysAgo + 
                                     "00:00&time<=" + daysAgo + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,dcp,sensor,RH,X,N,R\n" +
",,degrees_east,degrees_north,UTC,,,percent,,,\n" +
"9063063,Cleveland,-81.6355,41.5409," + daysAgo + "00:00:00Z,1,..,\\d\\d\\.\\d,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409," + daysAgo + "00:06:00Z,1,..,\\d\\d\\.\\d,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409," + daysAgo + "00:12:00Z,1,..,\\d\\d\\.\\d,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409," + daysAgo + "00:18:00Z,1,..,\\d\\d\\.\\d,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409," + daysAgo + "00:24:00Z,1,..,\\d\\d\\.\\d,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409," + daysAgo + "00:30:00Z,1,..,\\d\\d\\.\\d,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409," + daysAgo + "00:36:00Z,1,..,\\d\\d\\.\\d,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409," + daysAgo + "00:42:00Z,1,..,\\d\\d\\.\\d,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409," + daysAgo + "00:48:00Z,1,..,\\d\\d\\.\\d,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409," + daysAgo + "00:54:00Z,1,..,\\d\\d\\.\\d,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409," + daysAgo + "01:00:00Z,1,..,\\d\\d\\.\\d,0,0,0\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMRH" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Water Temperature
        //works http://opendap.co-ops.nos.noaa.gov/axis/webservices/watertemperature/plain/response.jsp?
        //timeZone=0&metadata=yes&Submit=Submit&stationId=8454000
        //&beginDate=20101024+00:00&endDate=20101026+00:00
        if ("nosCoopsMWT".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsMWT"); 

            //2014-01 "yesterday" fails, so seek a specific date
            String daysAgo = "2010-10-24T"; //yesterday;
            query = "&stationID=\"8454000\"&time>=" + daysAgo + 
                                     "00:00&time<=" + daysAgo + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,dcp,sensor,WT,X,N,R\n" +
",,degrees_east,degrees_north,UTC,,,degree_C,,,\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:00:00Z,1,..,\\d{1,2}.\\d,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:06:00Z,1,..,\\d{1,2}.\\d,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:12:00Z,1,..,\\d{1,2}.\\d,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:18:00Z,1,..,\\d{1,2}.\\d,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:24:00Z,1,..,\\d{1,2}.\\d,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:30:00Z,1,..,\\d{1,2}.\\d,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:36:00Z,1,..,\\d{1,2}.\\d,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:42:00Z,1,..,\\d{1,2}.\\d,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:48:00Z,1,..,\\d{1,2}.\\d,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:54:00Z,1,..,\\d{1,2}.\\d,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "01:00:00Z,1,..,\\d{1,2}.\\d,0,0,0\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMWT" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Wind
        //works http://opendap.co-ops.nos.noaa.gov/axis/webservices/wind/plain/response.jsp?
        //timeZone=0&metadata=yes&Submit=Submit&stationId=8454000
        //&beginDate=20101024+00:00&endDate=20101026+00:00
        if ("nosCoopsMW".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsMW"); 

            String daysAgo = yesterday;
            query = "&stationID=\"8454000\"&time>=" + daysAgo + 
                                     "00:00&time<=" + daysAgo + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,dcp,sensor,WS,WD,WG,X,R\n" +
",,degrees_east,degrees_north,UTC,,,m s-1,degrees_true,m s-1,,\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:00:00Z,1,..,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:06:00Z,1,..,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:12:00Z,1,..,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:18:00Z,1,..,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:24:00Z,1,..,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:30:00Z,1,..,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:36:00Z,1,..,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:42:00Z,1,..,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:48:00Z,1,..,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "00:54:00Z,1,..,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,0\n" +
"8454000,Providence,-71.4012,41.8071," + daysAgo + "01:00:00Z,1,..,\\d{1,2}\\.\\d{1,3},\\d{1,3}\\.\\d{1,2},\\d{1,2}\\.\\d{1,2},0,0\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMW" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Visibility
        //works http://opendap.co-ops.nos.noaa.gov/axis/webservices/visibility/plain/response.jsp?
        //timeZone=0&metadata=yes&Submit=Submit&stationId=8737005
        //&beginDate=20101024+00:00&endDate=20101026+00:00
        if ("nosCoopsMV".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsMV"); 

            //2014-01 "yesterday" fails, so seek a specific date
            String daysAgo = "2010-10-24T"; //yesterday;
            query = "&stationID=\"8737005\"&time>=" + daysAgo + 
                                     "00:00&time<=" + daysAgo + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,Vis\n" +
",,degrees_east,degrees_north,UTC,nautical_miles\n";
            //number of lines in results varies
            String sar[] = String2.split(results, '\n');
            for (int i = 2; i < sar.length - 1; i++)
                expected += 
"8737005,Pinto Island,-88.0311,30.6711," + daysAgo + "..:..:00Z,([\\-\\.\\d]{1,6}|NaN)\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMV" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }


        //Active Currents
        //works http://opendap.co-ops.nos.noaa.gov/axis/webservices/currents/plain/response.jsp?
        //metadata=yes&Submit=Submit&stationId=db0301&beginDate=20101121+00:00&endDate=20101121+01:00
        if ("nosCoopsCA".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsCA"); 

            //2014-01 "yesterday" fails, so seek a specific date
            String daysAgo = "2010-11-21T"; //yesterday;
            query = "&stationID=\"db0301\"&time>=" + daysAgo + 
                                    "00:00&time<=" + daysAgo + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,CS,CD\n" +
",,degrees_east,degrees_north,UTC,knots,degrees_true\n" +
"db0301,Philadelphia,-75.1397,39.9462," + daysAgo + "00:03:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n" +
"db0301,Philadelphia,-75.1397,39.9462," + daysAgo + "00:09:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n" +
//"db0301,Philadelphia,-75.1397,39.9462," + daysAgo + "00:15:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n" + //not on 2010-11-21
"db0301,Philadelphia,-75.1397,39.9462," + daysAgo + "00:21:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n" +
"db0301,Philadelphia,-75.1397,39.9462," + daysAgo + "00:27:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n" +
"db0301,Philadelphia,-75.1397,39.9462," + daysAgo + "00:33:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n" +
"db0301,Philadelphia,-75.1397,39.9462," + daysAgo + "00:39:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n" +
"db0301,Philadelphia,-75.1397,39.9462," + daysAgo + "00:45:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n" +
"db0301,Philadelphia,-75.1397,39.9462," + daysAgo + "00:51:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n" +
"db0301,Philadelphia,-75.1397,39.9462," + daysAgo + "00:57:00Z,([\\-\\.\\d]{1,6}|NaN),([\\-\\.\\d]{1,6}|NaN)\n";
            Test.ensureLinesMatch(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsCA" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
  
/*        
        //Verified Daily Mean   NEVER WORKED,
        //Their example at http://opendap.co-ops.nos.noaa.gov/axis/webservices/waterlevelverifieddaily/plain/
        // with stationID=9044020 works,
        // but using it here returns ERDDAP error message (not NOS service error message)
        //  "Your query produced no matching results. (There are no matching stations.)"
        if ("nosCoopsWLVDM".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsWLVDM"); 
            query = "&stationID=\"9044020\"&datum=\"IGLD\"&time>=" + daysAgo72 + 
                "00:00&time<=" + daysAgo70 + "00:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"zztop\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** THIS HAS NEVER WORKED." +
                "\nnosCoopsWLVDM results change every day (72 - 70 days ago)." +
                "\nIs the response reasonable?" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }
*/
        }

        // There is no plain text service for the Survey Currents Survey dataset at
        // http://opendap.co-ops.nos.noaa.gov/axis/text.html

        EDD.debugMode=false;

    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean doAllGraphicsTests) throws Throwable {
        String2.log("\n****************** EDDTableFromAsciiService.test() *****************\n");
        testVerboseOn();

        //always done  
        testNosCoops(".*");  //test all: ".*"

        //rarely done
        //makeNosCoopsWLSubsetFiles(true);  //re-download the stations table
        //makeNosCoopsMetSubsetFiles(false); //re-download the stations table
        //makeNosActiveCurrentsSubsetFile();
    }

}
