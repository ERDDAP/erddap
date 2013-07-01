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
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tLocalSourceUrl,
        String tBeforeData[], String tAfterData, String tNoData)
        throws Throwable {

        super("EDDTableFromAsciiServiceNOS", tDatasetID, tAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File, 
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
                    String2.log(String2.ERROR + " for stationID=" + tStationID +
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
     * @throws Throwable if trouble.
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
        String results, query, tName, expected;
        String yesterday = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(1, Double.NaN)).substring(0, 11);
        String daysAgo5 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(5, Double.NaN)).substring(0, 11);
        String daysAgo70 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(70, Double.NaN)).substring(0, 11);
        String daysAgo72 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(72, Double.NaN)).substring(0, 11);
        String daysAhead5 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(-5, Double.NaN)).substring(0, 11);
        String daysAhead7 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(-7, Double.NaN)).substring(0, 11);
        String daysAgo90 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(90, Double.NaN)).substring(0, 11);
        String daysAgo92 = Calendar2.epochSecondsToIsoStringT(
            Calendar2.backNDays(92, Double.NaN)).substring(0, 11);

     
/*        //Raw 6 minute          
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
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:00:00Z,MLLW,1,N1,1.007,0.02,NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:06:00Z,MLLW,1,N1,0.945,0.03,NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:12:00Z,MLLW,1,N1,0.883,0.02,NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:18:00Z,MLLW,1,N1,0.829,0.02,NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:24:00Z,MLLW,1,N1,0.775,0.02,NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:30:00Z,MLLW,1,N1,0.72,0.03,NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:36:00Z,MLLW,1,N1,0.66,0.02,NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:42:00Z,MLLW,1,N1,0.599,0.02,NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:48:00Z,MLLW,1,N1,0.533,0.02,NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:54:00Z,MLLW,1,N1,0.476,0.03,NaN,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T22:00:00Z,MLLW,1,N1,0.422,0.02,NaN,0,0,0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsWLR6 results change every day (last night's data)." +
                "\nIs the response reasonable?" +
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
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:00:00Z,MLLW,1,U1,1.512\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:01:00Z,MLLW,1,U1,0.999\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:02:00Z,MLLW,1,U1,0.994\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:03:00Z,MLLW,1,U1,0.977\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:04:00Z,MLLW,1,U1,0.964\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:05:00Z,MLLW,1,U1,0.963\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:06:00Z,MLLW,1,U1,0.945\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:07:00Z,MLLW,1,U1,0.942\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:08:00Z,MLLW,1,U1,0.926\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:09:00Z,MLLW,1,U1,0.923\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-13T21:10:00Z,MLLW,1,U1,0.906\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsWLR1 results change every day (last night's data)." +
                "\nIs the response reasonable?" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Verified 6 minute
        if ("nosCoopsWLV6".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsWLV6"); 

            query = "&stationID=\"9414290\"&datum=\"MLLW\"&time>=" + daysAgo5 +   //2012-11-14 was daysAgo70
                "21:00&time<=" + daysAgo5 + "22:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = //changes every day
"stationID,stationName,longitude,latitude,time,datum,waterLevel,sigma,I,F,R,L\n" +
",,degrees_east,degrees_north,UTC,,m,m,,,,\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-09T21:00:00Z,MLLW,0.456,0.039,0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-09T21:06:00Z,MLLW,0.437,0.04,0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-09T21:12:00Z,MLLW,0.418,0.042,0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-09T21:18:00Z,MLLW,0.424,0.042,0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-09T21:24:00Z,MLLW,0.398,0.043,0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-09T21:30:00Z,MLLW,0.394,0.044,0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-09T21:36:00Z,MLLW,0.41,0.039,0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-09T21:42:00Z,MLLW,0.403,0.042,0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-09T21:48:00Z,MLLW,0.411,0.042,0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-09T21:54:00Z,MLLW,0.389,0.038,0,0,0,0\n" +
"9414290,San Francisco,-122.465,37.8067,2012-11-09T22:00:00Z,MLLW,0.405,0.04,0,0,0,0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsWLV6 results change every day (5 days ago)." +
                "\nIs the response reasonable?" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Verified hourly
        if ("nosCoopsWLR60".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsWLV60"); 
            //no recent data. always ask for daysAgo70
            //2012-11-14 was stationID=9044020  but that stopped working
            query = "&stationID=\"8454000\"&datum=\"MLLW\"&time>=" + daysAgo70 + 
                "14:00&time<=" + daysAgo70 + "23:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,datum,waterLevel,sigma,I,L\n" +
",,degrees_east,degrees_north,UTC,,m,m,,\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-05T14:00:00Z,MLLW,1.183,0.003,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-05T15:00:00Z,MLLW,1.368,0.002,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-05T16:00:00Z,MLLW,1.551,0.004,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-05T17:00:00Z,MLLW,1.434,0.003,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-05T18:00:00Z,MLLW,1.216,0.006,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-05T19:00:00Z,MLLW,0.708,0.007,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-05T20:00:00Z,MLLW,0.378,0.004,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-05T21:00:00Z,MLLW,0.276,0.001,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-05T22:00:00Z,MLLW,0.378,0.002,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-05T23:00:00Z,MLLW,0.572,0.002,0,0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsWLV60 results change every day (70 days ago)." +
                "\nIs the response reasonable?" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Verified high low
        if ("nosCoopsWLVHL".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsWLVHL"); 
            //no recent data.  always ask for daysAgo72 ... daysAgo70
            //2012-11-14 was stationID=9044020  but that stopped working
            query = "&stationID=\"8454000\"&datum=\"MLLW\"&time>=" + daysAgo72 + 
                "00:00&time<=" + daysAgo70 + "00:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,datum,waterLevel,type,I,L\n" +
",,degrees_east,degrees_north,UTC,,m,,,\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-03T02:00:00Z,MLLW,1.6,H,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-03T07:18:00Z,MLLW,0.068,LL,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-03T14:48:00Z,MLLW,1.632,HH,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-03T19:42:00Z,MLLW,0.176,L,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-04T03:12:00Z,MLLW,1.478,H,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-04T08:00:00Z,MLLW,-0.002,LL,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-04T15:24:00Z,MLLW,1.507,HH,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-09-04T20:30:00Z,MLLW,0.197,L,0,0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsWLVHL results change every day (72 - 70 days ago)." +
                "\nIs the response reasonable?" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }


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


        //Tide Predicted High Low 
        //I needed a different way to specify datum (just 0=MLLW or 1=STND)
        //  so always use datum=0; don't let user specify datum
        //works: http://opendap.co-ops.nos.noaa.gov/axis/webservices/highlowtidepred/plain/response.jsp?
        //unit=0&timeZone=0&metadata=yes&Submit=Submit&stationId=8454000&beginDate=20101123+00:00&endDate=20101125+00:00&datum=0
        //RESULT FORMAT IS VERY DIFFERENT  so this class processes this dataset specially
        if ("nosCoopsWLTPHL".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsWLTPHL"); 

            query = "&stationID=\"8454000\"&time>=" + daysAhead5 + 
                "00:00&time<=" + daysAhead7 + "00:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,datum,waterLevel,type\n" +
",,degrees_east,degrees_north,UTC,,m,\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T04:52:00Z,MLLW,1.496,H\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T09:46:00Z,MLLW,0.016,L\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T17:21:00Z,MLLW,1.548,H\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T22:27:00Z,MLLW,0.076,L\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-20T05:48:00Z,MLLW,1.451,H\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-20T10:42:00Z,MLLW,0.142,L\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-20T18:16:00Z,MLLW,1.437,H\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-20T23:22:00Z,MLLW,0.149,L\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsWLTPHL results change every day (5-7 days ahead)." +
                "\nIs the response reasonable?" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

*/
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

            query = "&stationID=\"8454000\"&datum=\"MLLW\"&time>=" + daysAhead5 + 
                "00:00&time<=" + daysAhead5 + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,datum,predictedWL\n" +
",,degrees_east,degrees_north,UTC,,m\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T00:00:00Z,MLLW,0.195\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T00:06:00Z,MLLW,0.201\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T00:12:00Z,MLLW,0.207\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T00:18:00Z,MLLW,0.214\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T00:24:00Z,MLLW,0.221\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T00:30:00Z,MLLW,0.229\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T00:36:00Z,MLLW,0.239\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T00:42:00Z,MLLW,0.25\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T00:48:00Z,MLLW,0.262\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T00:54:00Z,MLLW,0.276\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T01:00:00Z,MLLW,0.292\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsWLTP6 results change every day (5+ days ahead)." +
                "\nIs the response reasonable?" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }
/*
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

            query = "&stationID=\"8454000\"&time>=" + daysAhead5 + 
                "00:00&time<=" + daysAhead5 + "10:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,datum,predictedWL\n" +
",,degrees_east,degrees_north,UTC,,m\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T00:00:00Z,MLLW,0.195\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T01:00:00Z,MLLW,0.292\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T02:00:00Z,MLLW,0.563\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T03:00:00Z,MLLW,0.99\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T04:00:00Z,MLLW,1.37\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T05:00:00Z,MLLW,1.493\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T06:00:00Z,MLLW,1.288\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T07:00:00Z,MLLW,0.87\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T08:00:00Z,MLLW,0.421\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T09:00:00Z,MLLW,0.097\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-19T10:00:00Z,MLLW,0.023\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsWLTP60 results change every day (5+ days ahead)." +
                "\nIs the response reasonable?" +
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

            query = "&stationID=\"8454000\"&time>=" + yesterday + 
                "00:00&time<=" + yesterday + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,dcp,sensor,AT,X,N,R\n" +
",,degrees_east,degrees_north,UTC,,,degree_C,,,\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:00:00Z,1,D1,16.3,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:06:00Z,1,D1,16.4,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:12:00Z,1,D1,16.2,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:18:00Z,1,D1,16.0,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:24:00Z,1,D1,16.2,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:30:00Z,1,D1,16.0,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:36:00Z,1,D1,16.1,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:42:00Z,1,D1,16.0,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:48:00Z,1,D1,16.0,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:54:00Z,1,D1,16.1,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T01:00:00Z,1,D1,16.1,0,0,0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMAT results change every day (yesterday)." +
                "\nIs the response reasonable?" +
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

            query = "&stationID=\"8454000\"&time>=" + yesterday + 
                "00:00&time<=" + yesterday + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,dcp,sensor,BP,X,N,R\n" +
",,degrees_east,degrees_north,UTC,,,mbar,,,\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:00:00Z,1,F1,1026.2,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:06:00Z,1,F1,1026.1,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:12:00Z,1,F1,1026.0,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:18:00Z,1,F1,1026.0,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:24:00Z,1,F1,1025.9,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:30:00Z,1,F1,1025.9,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:36:00Z,1,F1,1025.8,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:42:00Z,1,F1,1025.8,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:48:00Z,1,F1,1025.7,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:54:00Z,1,F1,1025.6,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T01:00:00Z,1,F1,1025.6,0,0,0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMBP results change every day (yesterday)." +
                "\nIs the response reasonable?" +
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

            query = "&stationID=\"8454000\"&time>=" + yesterday + 
                "00:00&time<=" + yesterday + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,dcp,sensor,CN,X,N,R\n" +
",,degrees_east,degrees_north,UTC,,,mS/cm,,,\n" +
"8454000,Providence,-71.4012,41.8071,2010-11-21T00:00:00Z,1,G1,29.3,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2010-11-21T00:06:00Z,1,G1,29.4,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2010-11-21T00:12:00Z,1,G1,30.4,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2010-11-21T00:18:00Z,1,G1,31.3,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2010-11-21T00:24:00Z,1,G1,29.8,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2010-11-21T00:30:00Z,1,G1,29.8,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2010-11-21T00:36:00Z,1,G1,29.6,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2010-11-21T00:42:00Z,1,G1,30.5,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2010-11-21T00:48:00Z,1,G1,31.1,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2010-11-21T00:54:00Z,1,G1,31.3,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2010-11-21T01:00:00Z,1,G1,31.1,0,0,0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** 2012-11-14 their example with this station says "no data is available from this station"
                "\n*** nosCoopsMC results change every day (yesterday)." +
                "\nIs the response reasonable?" +
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

            query = "&stationID=\"9752619\"&time>=" + yesterday + 
                "00:00&time<=" + yesterday + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,dcp,sensor,RF,X,R\n" +
",,degrees_east,degrees_north,UTC,,,mm,,\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525,2012-11-13T00:00:00Z,1,J1,0.2,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525,2012-11-13T00:06:00Z,1,J1,0.0,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525,2012-11-13T00:12:00Z,1,J1,0.0,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525,2012-11-13T00:18:00Z,1,J1,0.0,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525,2012-11-13T00:24:00Z,1,J1,0.0,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525,2012-11-13T00:30:00Z,1,J1,0.0,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525,2012-11-13T00:36:00Z,1,J1,0.0,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525,2012-11-13T00:42:00Z,1,J1,0.0,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525,2012-11-13T00:48:00Z,1,J1,0.0,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525,2012-11-13T00:54:00Z,1,J1,0.0,0,0\n" +
"9752619,\"Isabel Segunda, Vieques Island\",-65.4438,18.1525,2012-11-13T01:00:00Z,1,J1,0.0,0,0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMRF results change every day (yesterday)." +
                "\nIs the response reasonable?" +
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

            query = "&stationID=\"9063063\"&time>=" + yesterday + 
                "00:00&time<=" + yesterday + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,dcp,sensor,RH,X,N,R\n" +
",,degrees_east,degrees_north,UTC,,,percent,,,\n" +
"9063063,Cleveland,-81.6355,41.5409,2012-11-13T00:00:00Z,1,R1,76.0,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409,2012-11-13T00:06:00Z,1,R1,75.0,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409,2012-11-13T00:12:00Z,1,R1,76.0,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409,2012-11-13T00:18:00Z,1,R1,76.0,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409,2012-11-13T00:24:00Z,1,R1,75.0,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409,2012-11-13T00:30:00Z,1,R1,77.0,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409,2012-11-13T00:36:00Z,1,R1,69.0,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409,2012-11-13T00:42:00Z,1,R1,73.0,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409,2012-11-13T00:48:00Z,1,R1,72.0,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409,2012-11-13T00:54:00Z,1,R1,70.0,0,0,0\n" +
"9063063,Cleveland,-81.6355,41.5409,2012-11-13T01:00:00Z,1,R1,71.0,0,0,0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMRH results change every day (yesterday)." +
                "\nIs the response reasonable?" +
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

            query = "&stationID=\"8454000\"&time>=" + yesterday + 
                "00:00&time<=" + yesterday + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,dcp,sensor,WT,X,N,R\n" +
",,degrees_east,degrees_north,UTC,,,degree_C,,,\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:00:00Z,1,E1,11.5,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:06:00Z,1,E1,11.5,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:12:00Z,1,E1,11.5,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:18:00Z,1,E1,11.4,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:24:00Z,1,E1,11.4,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:30:00Z,1,E1,11.4,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:36:00Z,1,E1,11.4,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:42:00Z,1,E1,11.4,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:48:00Z,1,E1,11.4,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:54:00Z,1,E1,11.4,0,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T01:00:00Z,1,E1,11.4,0,0,0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMWT results change every day (yesterday)." +
                "\nIs the response reasonable?" +
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

            query = "&stationID=\"8454000\"&time>=" + yesterday + 
                "00:00&time<=" + yesterday + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,dcp,sensor,WS,WD,WG,X,R\n" +
",,degrees_east,degrees_north,UTC,,,m s-1,degrees_true,m s-1,,\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:00:00Z,1,C1,4.9,193.0,8.2,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:06:00Z,1,C1,4.5,193.0,7.2,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:12:00Z,1,C1,5.1,190.0,7.1,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:18:00Z,1,C1,4.6,183.0,7.5,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:24:00Z,1,C1,6.0,185.0,7.9,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:30:00Z,1,C1,4.4,191.0,7.2,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:36:00Z,1,C1,4.6,186.0,6.7,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:42:00Z,1,C1,5.7,190.0,7.4,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:48:00Z,1,C1,5.1,193.0,7.4,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T00:54:00Z,1,C1,5.0,184.0,7.7,0,0\n" +
"8454000,Providence,-71.4012,41.8071,2012-11-13T01:00:00Z,1,C1,5.4,187.0,8.3,0,0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMW results change every day (yesterday)." +
                "\nIs the response reasonable?" +
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

            query = "&stationID=\"8737005\"&time>=" + yesterday + 
                "00:00&time<=" + yesterday + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,Vis\n" +
",,degrees_east,degrees_north,UTC,nautical_miles\n" +
"8737005,Pinto Island,-88.0311,30.6711,2012-11-13T00:00:00Z,5.4\n" +
"8737005,Pinto Island,-88.0311,30.6711,2012-11-13T00:06:00Z,5.4\n" +
"8737005,Pinto Island,-88.0311,30.6711,2012-11-13T00:12:00Z,5.4\n" +
"8737005,Pinto Island,-88.0311,30.6711,2012-11-13T00:18:00Z,5.4\n" +
"8737005,Pinto Island,-88.0311,30.6711,2012-11-13T00:24:00Z,5.4\n" +
"8737005,Pinto Island,-88.0311,30.6711,2012-11-13T00:30:00Z,5.4\n" +
"8737005,Pinto Island,-88.0311,30.6711,2012-11-13T00:36:00Z,5.4\n" +
"8737005,Pinto Island,-88.0311,30.6711,2012-11-13T00:42:00Z,5.4\n" +
"8737005,Pinto Island,-88.0311,30.6711,2012-11-13T00:48:00Z,5.4\n" +
"8737005,Pinto Island,-88.0311,30.6711,2012-11-13T00:54:00Z,5.4\n" +
"8737005,Pinto Island,-88.0311,30.6711,2012-11-13T01:00:00Z,5.4\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMV results change every day (yesterday)." +
                "\nIs the response reasonable?" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }

        //Active Currents
        //works http://opendap.co-ops.nos.noaa.gov/axis/webservices/currents/plain/response.jsp?
        //metadata=yes&Submit=Submit&stationId=db0301&beginDate=20101121+00:00&endDate=20101121+01:00
        if ("nosCoopsCA".matches(idRegex)) {
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsCA"); 

            query = "&stationID=\"db0301\"&time>=" + yesterday + 
                "00:00&time<=" + yesterday + "01:00";             
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_" + edd.datasetID(), ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"stationID,stationName,longitude,latitude,time,CS,CD\n" +
",,degrees_east,degrees_north,UTC,knots,degrees_true\n" +
"db0301,Philadelphia,-75.1397,39.9462,2012-11-13T00:03:00Z,1.291,188.0\n" +
"db0301,Philadelphia,-75.1397,39.9462,2012-11-13T00:09:00Z,1.244,189.0\n" +
"db0301,Philadelphia,-75.1397,39.9462,2012-11-13T00:15:00Z,1.139,187.0\n" +
"db0301,Philadelphia,-75.1397,39.9462,2012-11-13T00:21:00Z,1.139,188.0\n" +
"db0301,Philadelphia,-75.1397,39.9462,2012-11-13T00:27:00Z,0.851,188.0\n" +
"db0301,Philadelphia,-75.1397,39.9462,2012-11-13T00:33:00Z,0.799,189.0\n" +
"db0301,Philadelphia,-75.1397,39.9462,2012-11-13T00:39:00Z,0.657,189.0\n" +
"db0301,Philadelphia,-75.1397,39.9462,2012-11-13T00:45:00Z,0.371,186.0\n" +
"db0301,Philadelphia,-75.1397,39.9462,2012-11-13T00:51:00Z,0.196,169.0\n" +
"db0301,Philadelphia,-75.1397,39.9462,2012-11-13T00:57:00Z,0.08,200.0\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\n*** nosCoopsMV results change every day (yesterday)." +
                "\nIs the response reasonable?" +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        }
*/
        // There is no plain text service for the Survey Currents Survey dataset at
        // http://opendap.co-ops.nos.noaa.gov/axis/text.html
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
        testNosCoops(".*");

        //rarely done
        //makeNosCoopsWLSubsetFiles(true);  //re-download the stations table
        //makeNosCoopsMetSubsetFiles(false); //re-download the stations table
        //makeNosActiveCurrentsSubsetFile();
    }

}
