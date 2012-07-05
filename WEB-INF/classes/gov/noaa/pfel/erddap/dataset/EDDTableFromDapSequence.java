/* 
 * EDDTableFromDapSequence Copyright 2007, NOAA.
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

/** The Java DAP classes.  */
import dods.dap.*;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.GregorianCalendar;

/**
 * NcHelper and ucar classes only used for testing netcdf-java.
 * Get netcdf-X.X.XX.jar from http://www.unidata.ucar.edu/software/netcdf-java/index.htm
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Get slf4j-jdk14.jar from 
 * ftp://ftp.unidata.ucar.edu/pub/netcdf-java/slf4j-jdk14.jar
 * and copy it to <context>/WEB-INF/lib.
 * Put both of these .jar files in the classpath for the compiler and for Java.
 */
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;  

/** 
 * This class represents a table of data from an opendap sequence source.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-08
 */
public class EDDTableFromDapSequence extends EDDTable{ 

    protected String outerSequenceName, innerSequenceName;
    protected boolean sourceCanConstrainStringEQNE, sourceCanConstrainStringGTLT, 
        skipDapperSpacerRows;
    protected boolean isOuterVar[];

    /** Indicates if data can be transmitted in a compressed form.
     * It is unlikely anyone would want to change this. */
    public static boolean acceptDeflate = true;


    /**
     * This constructs an EDDTableFromDapSequence based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromDapSequence"&gt; 
     *    having just been read.  
     * @return an EDDTableFromDapSequence.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromDapSequence fromXml(SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFromDapSequence(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        Attributes tGlobalAttributes = null;
        double tAltitudeMetersPerSourceUnit = 1; 
        ArrayList tDataVariables = new ArrayList();
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tLocalSourceUrl = null;
        String tOuterSequenceName = null;
        String tInnerSequenceName = null; 
        boolean tSourceNeedsExpandedFP_EQ = true;
        boolean tSourceCanConstrainStringEQNE = true;
        boolean tSourceCanConstrainStringGTLT = true;
        String tSourceCanConstrainStringRegex = null;
        boolean tSkipDapperSpacerRows = false;

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
            if      (localTags.equals("<addAttributes>"))
                tGlobalAttributes = getAttributesFromXml(xmlReader);
            else if (localTags.equals( "<altitudeMetersPerSourceUnit>")) {}
            else if (localTags.equals("</altitudeMetersPerSourceUnit>")) 
                tAltitudeMetersPerSourceUnit = String2.parseDouble(content); 
            else if (localTags.equals( "<dataVariable>")) 
                tDataVariables.add(getSDADVariableFromXml(xmlReader));           
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<sourceUrl>")) {}
            else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content; 
            else if (localTags.equals( "<outerSequenceName>")) {}
            else if (localTags.equals("</outerSequenceName>")) tOuterSequenceName = content; 
            else if (localTags.equals( "<innerSequenceName>")) {}
            else if (localTags.equals("</innerSequenceName>")) tInnerSequenceName = content; 
            else if (localTags.equals( "<sourceNeedsExpandedFP_EQ>")) {}
            else if (localTags.equals("</sourceNeedsExpandedFP_EQ>")) tSourceNeedsExpandedFP_EQ = String2.parseBoolean(content); 
            else if (localTags.equals( "<sourceCanConstrainStringEQNE>")) {}
            else if (localTags.equals("</sourceCanConstrainStringEQNE>")) tSourceCanConstrainStringEQNE = String2.parseBoolean(content); 
            else if (localTags.equals( "<sourceCanConstrainStringGTLT>")) {}
            else if (localTags.equals("</sourceCanConstrainStringGTLT>")) tSourceCanConstrainStringGTLT = String2.parseBoolean(content); 
            else if (localTags.equals( "<sourceCanConstrainStringRegex>")) {}
            else if (localTags.equals("</sourceCanConstrainStringRegex>")) tSourceCanConstrainStringRegex = content; 
            else if (localTags.equals( "<skipDapperSpacerRows>")) {}
            else if (localTags.equals("</skipDapperSpacerRows>")) tSkipDapperSpacerRows = String2.parseBoolean(content); 

            //onChange
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) 
                tOnChange.add(content); 

            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 

            else xmlReader.unexpectedTagException();
        }
        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++)
            ttDataVariables[i] = (Object[])tDataVariables.get(i);

        return new EDDTableFromDapSequence(tDatasetID, tAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File, tGlobalAttributes,
            tAltitudeMetersPerSourceUnit,
            ttDataVariables,
            tReloadEveryNMinutes, tLocalSourceUrl, 
            tOuterSequenceName, tInnerSequenceName, 
            tSourceNeedsExpandedFP_EQ,
            tSourceCanConstrainStringEQNE, 
            tSourceCanConstrainStringGTLT, 
            tSourceCanConstrainStringRegex,
            tSkipDapperSpacerRows);
    }

    /**
     * The constructor.
     *
     * <p>Assumptions about what constraints the source can handle:
     * <br>Numeric variables: outer sequence (any operator except regex), inner sequence (no constraints).
     * <br>String variables:
     * <br>    outer sequence 
     * <br>        support for = and != is set by sourceCanConstrainStringEQNE (default=true)
     * <br>        support for &lt; &lt;= &gt; &gt;= is set by sourceCanConstrainStringGTLT (default=true), 
     * <br>        regex support set by sourceCanConstrainStringRegex (default ""), 
     * <br>    inner sequence (no constraints ever). 
     *
     * <p>Yes, lots of detailed information must be supplied here
     * that is sometimes available in metadata. If it is in metadata,
     * make a subclass that extracts info from metadata and calls this 
     * constructor.
     *
     * @param tDatasetID is a very short string identifier 
     *   (required: just safe characters: A-Z, a-z, 0-9, _, -, or .)
     *   for this dataset. See EDD.datasetID().
     * @param tAccessibleTo is a comma separated list of 0 or more
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
     * @param tAddGlobalAttributes are global attributes which will
     *   be added to (and take precedence over) the data source's global attributes.
     *   This may be null if you have nothing to add.
     *   The combined global attributes must include:
     *   <ul>
     *   <li> "title" - the short (&lt; 80 characters) description of the dataset 
     *   <li> "summary" - the longer description of the dataset.
     *      It may have newline characters (usually at &lt;= 72 chars per line). 
     *   <li> "institution" - the source of the data 
     *      (best if &lt; 50 characters so it fits in a graph's legend).
     *   <li> "infoUrl" - the url with information about this data set 
     *   <li> "cdm_data_type" - one of the EDD.CDM_xxx options
     *   </ul>
     *   Special case: value="null" causes that item to be removed from combinedGlobalAttributes.
     *   Special case: if combinedGlobalAttributes name="license", any instance of value="[standard]"
     *     will be converted to the EDStatic.standardLicense.
     * @param tAltMetersPerSourceUnit the factor needed to convert the source
     *    alt values to/from meters above sea level.
     * @param tDataVariables is an Object[nDataVariables][3]: 
     *    <br>[0]=String sourceName (the name of the data variable in the dataset source, 
     *         without the outer or inner sequence name),
     *    <br>[1]=String destinationName (the name to be presented to the ERDDAP user, 
     *        or null to use the sourceName),
     *    <br>[2]=Attributes addAttributes (at ERD, this must have "ioos_category" - 
     *        a category from EDV.ioosCategories). 
     *        Special case: value="null" causes that item to be removed from combinedAttributes.
     *    <br>The order of variables you define doesn't have to match the
     *       order in the source.
     *    <p>If there is a time variable,  
     *      either tAddAttributes (read first) or tSourceAttributes must have "units"
     *      which is either <ul>
     *      <li> a UDUunits string (containing " since ")
     *        describing how to interpret source time values 
     *        (which should always be numeric since they are a dimension of a grid)
     *        (e.g., "seconds since 1970-01-01T00:00:00").
     *      <li> a org.joda.time.format.DateTimeFormat string
     *        (which is compatible with java.text.SimpleDateFormat) describing how to interpret 
     *        string times  (e.g., the ISO8601TZ_FORMAT "yyyy-MM-dd'T'HH:mm:ssZ", see 
     *        http://joda-time.sourceforge.net/api-release/index.html or 
     *        http://download.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html),
     *      </ul>
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data.
     * @param tLocalSourceUrl the url to which .das or .dds or ... can be added
     * @param tOuterSequenceName
     * @param tInnerSequenceName or null or "" if only 1-level sequence
     * @param tSourceNeedsExpandedFP_EQ
     * @param tSourceCanConstrainStringEQNE  
     *    if true, the source accepts constraints on outer sequence String variables with the 
     *    = and != operators.
     * @param tSourceCanConstrainStringGTLT  
     *    if true, the source accepts constraints on outer sequence String variables with the 
     *    &lt;, &lt;=, &gt;, and &gt;= operators.
     * @param tSourceCanConstrainStringRegex "=~" (the standard), "~=" (mistakenly
     *    supported by some servers), or null or "" (indicates not supported).
     * @param tSkipDapperSpacerRows if true, this skips the last row of each 
     *     innerSequence other than the last innerSequence (because Dapper
     *     puts NaNs in the row to act as a spacer).
     * @throws Throwable if trouble
     */
    public EDDTableFromDapSequence(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        Attributes tAddGlobalAttributes,
        double tAltMetersPerSourceUnit, 
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tLocalSourceUrl,
        String tOuterSequenceName, String tInnerSequenceName,
        boolean tSourceNeedsExpandedFP_EQ,
        boolean tSourceCanConstrainStringEQNE,
        boolean tSourceCanConstrainStringGTLT,
        String tSourceCanConstrainStringRegex,
        boolean tSkipDapperSpacerRows) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromDapSequence " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromDapSequence(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDTableFromDapSequence"; 
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        if (tAddGlobalAttributes == null)
            tAddGlobalAttributes = new Attributes();
        addGlobalAttributes = tAddGlobalAttributes;
        addGlobalAttributes.set("sourceUrl", convertToPublicSourceUrl(tLocalSourceUrl));
        localSourceUrl = tLocalSourceUrl;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        outerSequenceName = tOuterSequenceName;
        innerSequenceName = tInnerSequenceName == null || tInnerSequenceName.length() == 0? null : 
            tInnerSequenceName;
        skipDapperSpacerRows = tSkipDapperSpacerRows;
        sourceNeedsExpandedFP_EQ = tSourceNeedsExpandedFP_EQ;
        sourceCanConstrainStringEQNE = tSourceCanConstrainStringEQNE;
        sourceCanConstrainStringGTLT = tSourceCanConstrainStringGTLT;

        //in general, opendap sequence support:
        sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //outer vars, yes; inner, no
        sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; //outer vars, varies; inner, no
        sourceCanConstrainStringRegex = tSourceCanConstrainStringRegex == null? "" : 
            tSourceCanConstrainStringRegex;  //but only outer vars

      
        //open the connection to the opendap source
        //Design decision: this doesn't use e.g., ucar.nc2.dt.StationDataSet 
        //  because it determines axes via _CoordinateAxisType (or similar) metadata
        //  which most datasets we use don't have yet.
        //  One could certainly write another class that did use ucar.nc2.dt.StationDataSet.
        DConnect dConnect = new DConnect(localSourceUrl, acceptDeflate, 1, 1);
        DAS das;
        try {
            das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
        } catch (Throwable t) {
            throw new SimpleException("Error while getting DAS from " + localSourceUrl + ".das .\n" +
                t.getMessage());
        }
        DDS dds;
        try {
            dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
        } catch (Throwable t) {
            throw new SimpleException("Error while getting DDS from " + localSourceUrl + ".dds .\n" +
                t.getMessage());
        }

        //get global attributes
        sourceGlobalAttributes = new Attributes();
        OpendapHelper.getAttributes(das, "GLOBAL", sourceGlobalAttributes);
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("null");

        //create structures to hold the sourceAttributes temporarily
        int ndv = tDataVariables.length;
        Attributes tDataSourceAttributes[] = new Attributes[ndv];
        String tDataSourceTypes[] = new String[ndv];
        String tDataSourceNames[] = new String[ndv];
        isOuterVar = new boolean[ndv];  //default is all false
        for (int dv = 0; dv < ndv; dv++) {
            tDataSourceNames[dv] = (String)tDataVariables[dv][0];
        }

        //delve into the outerSequence 
        BaseType outerVariable = (BaseType)dds.getVariable(outerSequenceName);
        if (!(outerVariable instanceof DSequence)) 
            throw new RuntimeException(errorInMethod + "outerVariable not a DSequence: name=" + 
                outerVariable.getName() + " type=" + outerVariable.getTypeName());
        DSequence outerSequence = (DSequence)outerVariable;
        int nOuterColumns = outerSequence.elementCount();
        AttributeTable outerAttributeTable = das.getAttributeTable(outerSequenceName);
        for (int outerCol = 0; outerCol < nOuterColumns; outerCol++) {

            //look at the variables in the outer sequence
            BaseType obt = (BaseType)outerSequence.getVar(outerCol);
            String oName = obt.getName();
            if (innerSequenceName != null && oName.equals(innerSequenceName)) {

                //look at the variables in the inner sequence
                DSequence innerSequence = (DSequence)obt;
                AttributeTable innerAttributeTable = das.getAttributeTable(innerSequence.getName());
                Enumeration ien = innerSequence.getVariables();
                while (ien.hasMoreElements()) {
                    BaseType ibt = (BaseType)ien.nextElement();
                    String iName = ibt.getName();

                    //is iName in tDataVariableNames?  i.e., are we interested in this variable?
                    int dv = String2.indexOf(tDataSourceNames, iName);
                    if (dv < 0) {
                        if (reallyVerbose) String2.log("  ignoring source iName=" + iName);
                        continue;
                    }

                    //get the sourceType
                    tDataSourceTypes[dv] = PrimitiveArray.elementClassToString( 
                        OpendapHelper.getElementClass(ibt.newPrimitiveVector()));

                    //get the ibt attributes  
                    //(some servers return innerAttributeTable, some don't -- see test cases)
                    Attributes tAtt = new Attributes();
                    if (innerAttributeTable == null) {
                        //Dapper needs this approach
                        //note use of getLongName here
                        OpendapHelper.getAttributes(das, ibt.getLongName(), tAtt);
                        //drds needs this approach
                        if (tAtt.size() == 0)
                            OpendapHelper.getAttributes(das, iName, tAtt);
                    } else {
                        //note use of getName in this section
                        //if (reallyVerbose) String2.log("try getting attributes for inner " + iName);
                        dods.dap.Attribute attribute = innerAttributeTable.getAttribute(iName);
                        //it should be a container with the attributes for this column
                        if (attribute == null) {
                            String2.log("WARNING!!! Unexpected: no attribute for innerVar=" + 
                                iName + ".");
                        } else if (attribute.isContainer()) { 
                            OpendapHelper.getAttributes(attribute.getContainer(), tAtt);
                        } else {
                            String2.log("WARNING!!! Unexpected: attribute for innerVar=" + 
                                iName + " not a container: " + 
                                attribute.getName() + "=" + attribute.getValueAt(0));
                        }
                    }
                    //tAtt.set("source_sequence", "inner");
                    tDataSourceAttributes[dv] = tAtt;  //may be empty, that's ok

                }  //inner elements loop
                
            } else {
                //deal with an outer column
                //is oName in tDataVariableNames?  i.e., are we interested in this variable?
                int dv = String2.indexOf(tDataSourceNames, oName);
                if (dv < 0) {
                      //for testing only:  throw new RuntimeException("  ignoring source oName=" + oName);
                    if (verbose) String2.log("  ignoring source outer variable name=" + oName);
                    continue;
                }
                isOuterVar[dv] = true;

                //get the sourceDataType
                tDataSourceTypes[dv] = PrimitiveArray.elementClassToString( 
                    OpendapHelper.getElementClass(obt.newPrimitiveVector()));

                //get the attributes
                Attributes tAtt = new Attributes();
                if (outerAttributeTable == null) {
                    //Dapper needs this approach
                    //note use of getLongName here
                    OpendapHelper.getAttributes(das, obt.getLongName(), tAtt);
                    //drds needs this approach
                    if (tAtt.size() == 0)
                        OpendapHelper.getAttributes(das, oName, tAtt);
                } else {            
                    //note use of getName in this section
                    //if (reallyVerbose) String2.log("try getting attributes for outer " + oName);
                    dods.dap.Attribute attribute = outerAttributeTable.getAttribute(oName);
                    //it should be a container with the attributes for this column
                    if (attribute == null) {
                        String2.log("WARNING!!! Unexpected: no attribute for outerVar=" + 
                            oName + ".");
                    } else if (attribute.isContainer()) { 
                        OpendapHelper.getAttributes(attribute.getContainer(), tAtt);
                    } else {
                        String2.log("WARNING!!! Unexpected: attribute for outerVar=" + 
                            oName + " not a container: " + 
                            attribute.getName() + "=" + attribute.getValueAt(0));
                    }
                }
                //tAtt.set("source_sequence", "outer"); //just mark inner
                tDataSourceAttributes[dv] = tAtt;
            }
        }

        //create dataVariables[]
        dataVariables = new EDV[ndv];
        for (int dv = 0; dv < ndv; dv++) {
            String tSourceName = (String)tDataVariables[dv][0];
            String tDestName = (String)tDataVariables[dv][1];
            if (tDestName == null || tDestName.trim().length() == 0)
                tDestName = tSourceName;
            Attributes tSourceAtt = tDataSourceAttributes[dv];
            Attributes tAddAtt = (Attributes)tDataVariables[dv][2];
            String tSourceType = tDataSourceTypes[dv];
            //if (reallyVerbose) String2.log("  dv=" + dv + " sourceName=" + tSourceName + " sourceType=" + tSourceType);

            //ensure the variable was found
            if (tSourceName.startsWith("=")) {
                //if isFixedValue, sourceType can be inferred
            } else if (tSourceType == null) {
                throw new IllegalArgumentException(errorInMethod + "dataVariable#" + dv + " name=" + 
                    tSourceName + " not found in data source.");
            }

            if (EDV.LON_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVLon(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, Double.NaN, Double.NaN); 
                lonIndex = dv;
            } else if (EDV.LAT_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVLat(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, Double.NaN, Double.NaN); 
                latIndex = dv;
            } else if (EDV.ALT_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVAlt(tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, Double.NaN, Double.NaN, tAltMetersPerSourceUnit);
                altIndex = dv;
            } else if (EDV.TIME_NAME.equals(tDestName)) {  //look for TIME_NAME before check hasTimeUnits (next)
                dataVariables[dv] = new EDVTime(tSourceName,
                    tSourceAtt, tAddAtt, tSourceType);
                timeIndex = dv;
            } else if (EDVTimeStamp.hasTimeUnits(tSourceAtt, tAddAtt)) {
                dataVariables[dv] = new EDVTimeStamp(tSourceName, tDestName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //the constructor that reads actual_range
                dataVariables[dv].setActualRangeFromDestinationMinMax();
            } else {
                dataVariables[dv] = new EDV(tSourceName, tDestName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //the constructor that reads actual_range
                dataVariables[dv].setActualRangeFromDestinationMinMax();
            }
        }


        //ensure the setup is valid
        ensureValid();

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDTableFromDapSequence " + datasetID + " constructor finished. TIME=" + 
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
            constraintVariables, constraintOps, constraintValues);

        //further prune constraints 
        //sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //outer vars, yes; inner, no
        //sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; //outer vars, varies; inner, no
        //sourceCanConstrainStringRegex = dataset dependent, but only outer vars
        //work backwards since deleting some
        for (int c = constraintVariables.size() - 1; c >= 0; c--) { 
            String constraintVariable = constraintVariables.get(c);
            int dv = String2.indexOf(dataVariableSourceNames(), constraintVariable);
            if (!isOuterVar[dv]) {
                //just remove constraints for all inner vars (string and numeric)
                constraintVariables.remove(c);
                constraintOps.remove(c);
                constraintValues.remove(c);
                continue;
            }

            //for string constraints
            EDV edv = dataVariables[dv];
            String op = constraintOps.get(c);
            if (edv.sourceDataTypeClass() == String.class) {

                //remove EQNE constraints
                if (!sourceCanConstrainStringEQNE && 
                    (op.equals("=") || op.equals("!="))) {
                    constraintVariables.remove(c);
                    constraintOps.remove(c);
                    constraintValues.remove(c);
                    continue;
                }

                //remove GTLT constraints
                if (!sourceCanConstrainStringGTLT && 
                    String2.indexOf(GTLT_OPERATORS, op) >= 0) {
                    constraintVariables.remove(c);
                    constraintOps.remove(c);
                    constraintValues.remove(c);
                    continue;
                }

                //convert time constraints (epochSeconds) to source String format
                if ((edv instanceof EDVTimeStamp) && !op.equals(PrimitiveArray.REGEX_OP)) { //but if regex, leave as string
                    constraintValues.set(c, 
                        ((EDVTimeStamp)edv).epochSecondsToSourceTimeString(
                            String2.parseDouble(constraintValues.get(c))));
                }

                //finally: put quotes around String constraint "value"
                constraintValues.set(c, "\"" + constraintValues.get(c) + "\"");

            } else {
                //convert time constraints (epochSeconds) to source units
                if ((edv instanceof EDVTimeStamp) && !op.equals(PrimitiveArray.REGEX_OP)) { //but if regex, leave as string
                    constraintValues.set(c, 
                        ((EDVTimeStamp)edv).epochSecondsToSourceTimeString(
                            String2.parseDouble(constraintValues.get(c))));
                }

            }

            //convert REGEX_OP to sourceCanConstrainStringRegex
            if (op.equals(PrimitiveArray.REGEX_OP)) {
                constraintOps.set(c, sourceCanConstrainStringRegex);
            }
        }

        //It is very easy for this class since the sourceDapQuery can be 
        //sent directly to the source after minimal processing.
        String encodedSourceDapQuery = formatAsPercentEncodedDapQuery(resultsVariables.toArray(),
            constraintVariables.toArray(), constraintOps.toArray(), 
            constraintValues.toArray());

        //Read all data, then write to tableWriter.
        //Very unfortunate: This is NOT memory efficient.
        //JDAP reads all rows when it deserializes (see java docs for DSequence)
        //(that's why it can return getRowCount)
        //so there is no easy way to read an opendapSequence in chunks (or row by row).
        //I can't split into subsets because I don't know which variable 
        //  to constrain or how to constrain it (it would change with different
        //  userDapQuery's).
        //I could write my own procedure to read DSequence (eek!).
        Table table = new Table();
        try {
            table.readOpendapSequence(localSourceUrl + "?" + encodedSourceDapQuery,  
                skipDapperSpacerRows);
        } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t);  //first thing in catch{}

            //if no data, convert to ERDDAP standard message
            String tToString = t.toString();
            if (tToString.indexOf("Your Query Produced No Matching Results.") >= 0) //the DAP standard
                throw new SimpleException(MustBe.THERE_IS_NO_DATA);

            //if too much data, rethrow t
            if (tToString.indexOf(Math2.memoryTooMuchData) >= 0)
                throw t;

            //any other error is real trouble
            requestReloadASAP();
            throw new WaitThenTryAgainException(EDStatic.waitThenTryAgain + 
                "\n(" + EDStatic.errorFromDataSource + tToString + ")", 
                t); 
        }
        //String2.log(table.toString());
        standardizeResultsTable(requestUrl, userDapQuery, table);
        tableWriter.writeAllAndFinish(table);
    }


    /** 
     * This does its best to generate a read-to-use datasets.xml entry for an
     * EDDTableFromDapSequence.
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
    public static String generateDatasetsXml(String tLocalSourceUrl, 
        int tReloadEveryNMinutes, Attributes externalGlobalAttributes) 
        //String outerSequenceName, String innerSequenceName, boolean sortColumnsByName) 
        throws Throwable {

        String2.log("EDDTableFromDapSequence.generateDatasetsXml" +
            "\n  tLocalSourceUrl=" + tLocalSourceUrl);
        String tPublicSourceUrl = convertToPublicSourceUrl(tLocalSourceUrl);

        //get DConnect
        DConnect dConnect = new DConnect(tLocalSourceUrl, acceptDeflate, 1, 1);
        DAS das;
        try {
            das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
        } catch (Throwable t) {
            throw new SimpleException("Error while getting DAS from " + tLocalSourceUrl + ".das .\n" +
                t.getMessage());
        }
//String2.log("das.getNames=" + String2.toCSSVString(das.getNames()));
//AttributeTable att = OpendapHelper.getAttributeTable(das, outerSequenceName);
//Attributes atts2 = new Attributes();
//OpendapHelper.getAttributes(att, atts2);
//String2.log("outer attributes=" + (att == null? "null" : atts2.toString()));

        DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();

        //get all of the vars
        String outerSequenceName = null;
        String innerSequenceName = null;
        StringArray tSubsetVariables = new StringArray();
        Enumeration datasetVars = dds.getVariables();
        int nOuterVars = 0; //so outerVars are first in dataAddTable
        while (datasetVars.hasMoreElements()) {
            BaseType datasetVar = (BaseType)datasetVars.nextElement();
            if (outerSequenceName == null && datasetVar instanceof DSequence) {
                DSequence outerSequence = (DSequence)datasetVar;
                outerSequenceName = outerSequence.getName();

                //get list of outerSequence variables
                Enumeration outerVars = outerSequence.getVariables();
                while (outerVars.hasMoreElements()) {
                    BaseType outerVar = (BaseType)outerVars.nextElement();

                    //catch innerSequence
                    if (outerVar instanceof DSequence) {
                        if (innerSequenceName == null) {
                            innerSequenceName = outerVar.getName();
                            DSequence innerSequence = (DSequence)outerVar;
                            Enumeration innerVars = innerSequence.getVariables();
                            while (innerVars.hasMoreElements()) {
                                //inner variable
                                BaseType innerVar = (BaseType)innerVars.nextElement();
                                if (innerVar instanceof DConstructor ||
                                    innerVar instanceof DVector) {
                                } else {
                                    String varName = innerVar.getName();
                                    Attributes sourceAtts = new Attributes();
                                    OpendapHelper.getAttributes(das, varName, sourceAtts);
                                    if (sourceAtts.size() == 0)
                                        OpendapHelper.getAttributes(das, 
                                            outerSequenceName + "." + innerSequenceName + "." + varName, 
                                            sourceAtts);
                                    dataSourceTable.addColumn(dataSourceTable.nColumns(), 
                                        varName, new StringArray(), sourceAtts);
                                    dataAddTable.addColumn(dataAddTable.nColumns(), 
                                        varName, new StringArray(), 
                                        makeReadyToUseAddVariableAttributesForDatasetsXml(
                                            sourceAtts, varName, true, true)); //addColorBarMinMax, tryToFindLLAT
                                }
                            }
                        } else {
                            if (verbose) String2.log("Skipping the other innerSequence: " + outerVar.getName());
                        }
                    } else if (outerVar instanceof DConstructor) {
                        //skip it
                    } else {
                        //outer variable
                        String varName = outerVar.getName();
                        Attributes sourceAtts = new Attributes();
                        OpendapHelper.getAttributes(das, varName, sourceAtts);
                        if (sourceAtts.size() == 0)
                            OpendapHelper.getAttributes(das, 
                                outerSequenceName + "." + varName, sourceAtts);
                        //just outer vars get added to subsetVariables
                        tSubsetVariables.add(suggestDestinationName(
                            varName, sourceAtts.getString("units"), 
                            sourceAtts.getFloat("scale_factor"), true)); 
                        dataSourceTable.addColumn(nOuterVars, varName, new StringArray(), 
                            sourceAtts);
                        dataAddTable.addColumn(   nOuterVars, varName, new StringArray(), 
                            makeReadyToUseAddVariableAttributesForDatasetsXml(
                                sourceAtts, varName, true, true)); //addColorBarMinMax, tryToFindLLAT
                        nOuterVars++;
                    }
                }
            }
        }

        //get global attributes and ensure required entries are present 
        //after dataVariables known, add global attributes in the axisAddTable
        OpendapHelper.getAttributes(das, "GLOBAL", dataSourceTable.globalAttributes());
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                probablyHasLonLatTime(dataAddTable)? "Point" : "Other",
                tLocalSourceUrl, externalGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));
        if (outerSequenceName == null)
            throw new SimpleException("No Sequence variable was found for " + tLocalSourceUrl + ".dds.");
        dataAddTable.globalAttributes().add("subsetVariables", tSubsetVariables.toString());

        //write the information
        boolean isDapper = tLocalSourceUrl.indexOf("dapper") > 0;
        StringBuilder sb = new StringBuilder();
        sb.append(
            directionsForGenerateDatasetsXml() +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromDapSequence\" datasetID=\"" + 
                suggestDatasetID(tPublicSourceUrl) + 
                "\" active=\"true\">\n" +
            "    <sourceUrl>" + tLocalSourceUrl + "</sourceUrl>\n" +
            "    <outerSequenceName>" + outerSequenceName + "</outerSequenceName>\n" +

                (innerSequenceName == null? "" : 
            "    <innerSequenceName>" + innerSequenceName + "</innerSequenceName>\n") +

            "    <skipDapperSpacerRows>" + isDapper + "</skipDapperSpacerRows>\n" +
            "    <sourceCanConstrainStringEQNE>" + !isDapper + "</sourceCanConstrainStringEQNE>\n" + //DAPPER doesn't support string constraints
            "    <sourceCanConstrainStringGTLT>" + !isDapper + "</sourceCanConstrainStringGTLT>\n" + //see email from Joe Sirott 1/21/2009
            "    <sourceCanConstrainStringRegex></sourceCanConstrainStringRegex>\n" + //was ~=, now ""; see notes.txt for 2009-01-16
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +
            //altitude is only relevant if var is already called "altitude".
            //(new GenerateDatasetsXml doesn't change depth to altitude) so units probably already meters up
            "    <altitudeMetersPerSourceUnit>1</altitudeMetersPerSourceUnit>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        sb.append(cdmSuggestion());
        sb.append(writeAttsForDatasetsXml(true,     dataAddTable.globalAttributes(), "    "));

        //last 3 params: includeDataType, tryToFindLLAT, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, 
            "dataVariable", false, true, false));
        sb.append(
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
    }

    /**
     * The basic tests of this class (testGlobecBottle).
     */
    public static void testBasic() throws Throwable {
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        int po, epo;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //*** test things that should throw exceptions
        StringArray rv = new StringArray();
        StringArray cv = new StringArray();
        StringArray co = new StringArray();
        StringArray cv2 = new StringArray();

        String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&altitude>-5&time>=2002-08-03";
        userDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-03";
        String regexDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-03" +
            "&longitude" + PrimitiveArray.REGEX_OP + "\".*11.*\"";

        //testGlobecBottle is like erdGlobecBottle, but with the addition
        //  of a fixed value altitude=0 variable
        EDDTable globecBottle = (EDDTableFromDapSequence)oneFromDatasetXml("testGlobecBottle"); //should work

 
        //getEmpiricalMinMax just do once
        //globecBottle.getEmpiricalMinMax("2002-07-01", "2002-09-01", false, true);
        //if (true) System.exit(1);

        //*** test valid queries
        String2.log("\n****************** EDDTableFromDapSequence.test valid queries \n");
        globecBottle.parseUserDapQuery("longitude,NO3", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), "longitude, NO3", "");
        Test.ensureEqual(cv.toString(), "", "");
        Test.ensureEqual(co.toString(), "", "");
        Test.ensureEqual(cv2.toString(), "", "");

        globecBottle.parseUserDapQuery("longitude,NO3&altitude=0", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), "longitude, NO3", "");
        Test.ensureEqual(cv.toString(), "altitude", "");
        Test.ensureEqual(co.toString(), "=", "");
        Test.ensureEqual(cv2.toString(), "0", "");

        //test: no resultsVariables interpreted as all resultsVariables
        String allVars = "longitude, latitude, altitude, time, " +
            "ship, cruise_id, cast, bottle_posn, chl_a_total, chl_a_10um, phaeo_total, " +
            "phaeo_10um, sal00, sal11, temperature0, temperature1, fluor_v, xmiss_v, " +
            "PO4, N_N, NO3, Si, NO2, NH4, oxygen, par";

        globecBottle.parseUserDapQuery("", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), allVars, "");
        Test.ensureEqual(cv.toString(), "", "");
        Test.ensureEqual(co.toString(), "", "");
        Test.ensureEqual(cv2.toString(), "", "");

        globecBottle.parseUserDapQuery("&altitude%3E=0", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), allVars, "");
        Test.ensureEqual(cv.toString(), "altitude", "");
        Test.ensureEqual(co.toString(), ">=", "");
        Test.ensureEqual(cv2.toString(), "0", "");

        globecBottle.parseUserDapQuery("s", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), allVars, "");
        Test.ensureEqual(cv.toString(), "", "");
        Test.ensureEqual(co.toString(), "", "");
        Test.ensureEqual(cv2.toString(), "", "");

        globecBottle.parseUserDapQuery("s&s.altitude=0", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), allVars, "");
        Test.ensureEqual(cv.toString(), "altitude", "");
        Test.ensureEqual(co.toString(), "=", "");
        Test.ensureEqual(cv2.toString(), "0", "");

        globecBottle.parseUserDapQuery("s.longitude,s.altitude&s.altitude=0", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), "longitude, altitude", "");
        Test.ensureEqual(cv.toString(), "altitude", "");
        Test.ensureEqual(co.toString(), "=", "");
        Test.ensureEqual(cv2.toString(), "0", "");

        //e.g., now-5days
        GregorianCalendar gc = Calendar2.newGCalendarZulu();
        gc.set(Calendar2.MILLISECOND, 0);
        gc.add(Calendar2.SECOND, 1);  //now it is "now"
        long nowMillis = gc.getTimeInMillis();
        String s;

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        String2.log("now          = " + Calendar2.formatAsISODateTimeT3(gc));
        globecBottle.parseUserDapQuery("time&time=now", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), "time", "");
        Test.ensureEqual(cv.toString(), "time", "");
        Test.ensureEqual(co.toString(), "=", "");        
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.SECOND, -1);
        String2.log("now-1second  = " + Calendar2.formatAsISODateTimeT3(gc));
        globecBottle.parseUserDapQuery("time&time=now-1second", rv, cv, co, cv2, false);  
        Test.ensureEqual(rv.toString(), "time", "");
        Test.ensureEqual(cv.toString(), "time", "");
        Test.ensureEqual(co.toString(), "=", "");        
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.SECOND, 2);
        String2.log("now+2seconds = " + Calendar2.formatAsISODateTimeT3(gc));
        globecBottle.parseUserDapQuery("time&time=now%2B2seconds", rv, cv, co, cv2, false);  
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        //non-%encoded '+' will be decoded as ' ', so treat ' ' as equal to '+' 
        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.SECOND, 2);
        String2.log("now 2seconds = " + Calendar2.formatAsISODateTimeT3(gc));
        globecBottle.parseUserDapQuery("time&time=now 2seconds", rv, cv, co, cv2, false);  
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.MINUTE, -3);
        String2.log("now-3minutes = " + Calendar2.formatAsISODateTimeT3(gc));
        globecBottle.parseUserDapQuery("time&time=now-3minutes", rv, cv, co, cv2, false);  
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.HOUR, -4);
        String2.log("now-4hours   = " + Calendar2.formatAsISODateTimeT3(gc));
        globecBottle.parseUserDapQuery("time&time=now-4hours", rv, cv, co, cv2, false);  
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.DATE, -5);
        String2.log("now-5days    = " + Calendar2.formatAsISODateTimeT3(gc));
        globecBottle.parseUserDapQuery("time&time=now-5days", rv, cv, co, cv2, false);  
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.MONTH, -6);
        String2.log("now-6months  = " + Calendar2.formatAsISODateTimeT3(gc));
        globecBottle.parseUserDapQuery("time&time=now-6months", rv, cv, co, cv2, false);  
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");

        gc = Calendar2.newGCalendarZulu(nowMillis); 
        gc.add(Calendar2.YEAR, -7);
        String2.log("now-7years   = " + Calendar2.formatAsISODateTimeT3(gc));
        globecBottle.parseUserDapQuery("time&time=now-7years", rv, cv, co, cv2, false);  
        Test.ensureEqual(cv2.toString(), "" + Calendar2.gcToEpochSeconds(gc), "");
        //if (true) throw new RuntimeException("stop here");

        //longitude converted to lon
        //altitude is fixed value, so test done by getSourceQueryFromDapQuery
        //time test deferred till later, so 'datetime_epoch' included in sourceQuery
        //also, lat is added to var list because it is used in constraints
        globecBottle.getSourceQueryFromDapQuery(userDapQuery, rv, cv, co, cv2);        
        Test.ensureEqual(formatAsDapQuery(rv.toArray(), cv.toArray(), co.toArray(), cv2.toArray()),
            "lon100,NO3,datetime_epoch,ship,lat100&lat100>0&datetime_epoch>=1.0283328E9",  
            "Unexpected sourceDapQuery from userDapQuery=" + userDapQuery);
 
        //test invalid queries
        try {
            //lon is the source name
            globecBottle.getSourceQueryFromDapQuery("lon,cast", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: Unrecognized variable=lon", 
            "error=" + error);

        error = "";
        try {
            //a variable can't be listed twice
            globecBottle.getSourceQueryFromDapQuery("cast,longitude,cast,latitude", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: variable=cast is listed twice in the results variables list.", 
            "error=" + error);

        error = "";
        try {
            //if s is used, it must be the only request var
            globecBottle.getSourceQueryFromDapQuery("s.latitude,s", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: If s is requested, it must be the only requested variable.", 
            "error=" + error);

        error = "";
        try {
            //zztop isn't valid variable
            globecBottle.getSourceQueryFromDapQuery("cast,zztop", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: Unrecognized variable=zztop", "error=" + error);

        error = "";
        try {
            //alt is fixedValue=0 so will always return NO_DATA
            globecBottle.getSourceQueryFromDapQuery("cast&altitude<-1", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Your query produced no matching results. Fixed value variable=altitude failed the test 0<-1.0.", 
            "error=" + error);

        error = "";
        try {
            //lon isn't a valid var
            globecBottle.getSourceQueryFromDapQuery("NO3, Si&lon=0", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: Unrecognized constraint variable=\"lon\"", "error=" + error);

        error = "";
        try {
            //should be ==, not =
            globecBottle.getSourceQueryFromDapQuery("NO3,Si&altitude==0", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: Use '=' instead of '==' in constraints.", 
            "error=" + error);

        error = "";
        try {
            //regex operator should be =~, not ~=
            globecBottle.getSourceQueryFromDapQuery("NO3,Si&altitude~=(0|1.*)", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: Use '=~' instead of '~=' in constraints.", 
            "error=" + error);

        error = "";
        try {
            //string regex values must be in quotes
            globecBottle.getSourceQueryFromDapQuery("NO3,Si&ship=New_Horizon", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: For constraints of String variables, the right-hand-side value must be surrounded by double quotes.",
            "error=" + error);
        Test.ensureEqual(String2.split(error, '\n')[1], 
            "Bad constraint: ship=New_Horizon", 
            "error=" + error);

        error = "";
        try {
            //numeric variable regex values must be in quotes
            globecBottle.getSourceQueryFromDapQuery("NO3,Si&altitude=~(0|1.*)", rv, cv, co, cv2);  
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: For =~ constraints of numeric variables, " +
            "the right-hand-side value must be surrounded by double quotes.",
            "error=" + error);
        Test.ensureEqual(String2.split(error, '\n')[1], 
            "Bad constraint: altitude=~(0|1.*)", 
            "error=" + error);

        error = "";
        try {
            globecBottle.getSourceQueryFromDapQuery("NO3,Si&altitude=0|longitude>-180", 
                rv, cv, co, cv2);  //invalid query format caught as invalid NaN
        } catch (Throwable t) {
            error = MustBe.throwableToString(t);
        }
        Test.ensureEqual(String2.split(error, '\n')[0], 
            "SimpleException: Query error: Numeric tests of NaN must use \"NaN\", " +
            "not value=\"0|longitude>-180\".", "error=" + error);

        error = "";
        String nowQ[] = {"nowa", "now-day", "now-", "now-4", "now-5date", "now-9dayss"};
        for (int i = 0; i < nowQ.length; i++) {
            try {
                globecBottle.getSourceQueryFromDapQuery("time&time=" + nowQ[i], 
                    rv, cv, co, cv2);  //invalid query format caught as invalid NaN
            } catch (Throwable t) {
                error = MustBe.throwableToString(t);
            }
            Test.ensureEqual(String2.split(error, '\n')[0], 
                "SimpleException: Query error: Timestamp constraints with \"now\" must be in " +
                "the form \"now(+|-)[integer](seconds|minutes|hours|days|months|years)\"" +
                ".  \"" + nowQ[i] + "\" is invalid.", "error=" + error);
        }

        //impossible queries
        String impossibleQuery[] = new String[]{
            "&longitude!=NaN&longitude<=NaN",
            "&longitude!=NaN&longitude>=NaN",
            "&longitude!=NaN&longitude=NaN",
            "&longitude=NaN&longitude!=NaN",
            "&longitude=NaN&longitude<3.0",
            "&longitude=NaN&longitude<=3.0",
            "&longitude=NaN&longitude>3.0",
            "&longitude=NaN&longitude>=3.0",
            "&longitude=NaN&longitude=3.0",
            "&longitude<2.0&longitude=NaN",
            "&longitude<=2.0&longitude=NaN",
            "&longitude<=2.0&longitude<=NaN",
            "&longitude<2.0&longitude>3.0",
            "&longitude<=2.0&longitude>=3.0",
            "&longitude<=2.0&longitude=3.0",
            "&longitude>2.0&longitude=NaN",
            "&longitude>=2.0&longitude=NaN",
            "&longitude>=2.0&longitude<=NaN",
            "&longitude>3.0&longitude=2.0",
            "&longitude>=3.0&longitude=2.0",
            "&longitude>=3.0&longitude<=2.0",
            "&longitude=2.0&longitude<=NaN",
            "&longitude=2.0&longitude>=NaN",
            "&longitude=2.0&longitude=NaN",
            "&longitude=3.0&longitude>4.0",
            "&longitude=3.0&longitude>=4.0",
            "&longitude=3.0&longitude<2.0",
            "&longitude=3.0&longitude<=2.0",
            "&longitude=2.0&longitude=3.0",
            "&longitude=2.0&longitude!=2.0"};
        for (int i = 0; i < impossibleQuery.length; i++) {
            error = "";
            try {globecBottle.getSourceQueryFromDapQuery(impossibleQuery[i], rv, cv, co, cv2);  
            } catch (Throwable t) {
                error = MustBe.throwableToString(t); 
            }
            Test.ensureEqual(String2.split(error, '\n')[0], 
                "SimpleException: Query error: " + 
                String2.replaceAll(impossibleQuery[i].substring(1), "&", " and ") + 
                " will never both be true.", 
                "error=" + error);
        }
        //possible queries
        globecBottle.getSourceQueryFromDapQuery("&longitude<2&longitude<=3", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude>=2&longitude>3", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude=2&longitude<3", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude>2&longitude=3", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude>=NaN", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude<=NaN", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude!=NaN", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude<2&longitude!=NaN", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude!=NaN&longitude=3", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude=NaN&longitude<=NaN", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude<=NaN&longitude=NaN", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude=NaN&longitude!=3", rv, cv, co, cv2);  
        globecBottle.getSourceQueryFromDapQuery("&longitude!=3&longitude=NaN", rv, cv, co, cv2);  


        //*** test dapInstructions
        //StringWriter sw = new StringWriter();
        //writeGeneralDapHtmlDocument(EDStatic.erddapUrl, sw); //for testing, use the non-https url
        //results = sw.toString();
        //expected = "Requests for Tabular Data in ";
        //Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        //expected = "In ERDDAP, time variables always have the name \"" + EDV.TIME_NAME + "\"";
        //Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

        //test sliderCsvValues
        edv = globecBottle.findDataVariableByDestinationName("longitude");
        results = edv.sliderCsvValues();
        expected = "-126.2, -126.19, -126.18, -126.17, -126.16,";  
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
        expected = ".13, -124.12, -124.11, -124.1";  
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "results=\n" + results);
        Test.ensureEqual(edv.sliderNCsvValues(), 211, "");

        edv = globecBottle.findDataVariableByDestinationName("latitude");
        results = edv.sliderCsvValues();
        expected = "41.9, 41.92, 41.94, 41.96, 41.98"; 
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
        expected = "44.56, 44.58, 44.6, 44.62, 44.64, 44.65";  
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "results=\n" + results);
        Test.ensureEqual(edv.sliderNCsvValues(), 139, "");

        edv = globecBottle.findDataVariableByDestinationName("altitude");
        results = edv.sliderCsvValues();
        expected = "0"; //0
        Test.ensureEqual(results, expected, "results=\n" + results);
        Test.ensureEqual(edv.sliderNCsvValues(), 1, "");

        edv = globecBottle.findDataVariableByDestinationName("time");
        results = edv.sliderCsvValues();
        //2002-05-30T03:21:00Z	   2002-08-19T20:18:00Z
        expected = "\"2002-05-30T03:21:00Z\", \"2002-05-30T12:00:00Z\", \"2002-05-31\", \"2002-05-31T12:00:00Z\",";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
        expected = "\"2002-08-19\", \"2002-08-19T12:00:00Z\", \"2002-08-19T20:18:00Z\"";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "results=\n" + results);
        Test.ensureEqual(edv.sliderNCsvValues(), 165, "");

        edv = globecBottle.findDataVariableByDestinationName("phaeo_total");
        results = edv.sliderCsvValues();
        expected = "-1.705, -1.7, -1.6, -1.5, -1.4, -1.3,"; //-1.705
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
        expected = "15.5, 15.6, 15.7, 15.8, 15.9, 16.047"; //16.047
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "results=\n" + results);
        Test.ensureEqual(edv.sliderNCsvValues(), 179, "");

        edv = globecBottle.findDataVariableByDestinationName("cruise_id");
        results = edv.sliderCsvValues();
        expected = null;
        Test.ensureEqual(results, expected, "results=\n" + results);
        Test.ensureEqual(edv.sliderNCsvValues(), -1, "");


        //*** test getting das for entire dataset
        String2.log("\n****************** EDDTableFromDapSequence.test das dds for entire dataset\n");
        tName = globecBottle.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Entire", ".das"); 
        results = String2.annotatedString(new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = //see OpendapHelper.EOL for comments
"Attributes {[10]\n" +
" s {[10]\n" +
"  longitude {[10]\n" +
"    String _CoordinateAxisType \"Lon\";[10]\n" +
"    Float32 actual_range -126.2, -124.1;[10]\n" +
"    String axis \"X\";[10]\n" +
"    Float64 colorBarMaximum -115.0;[10]\n" +
"    Float64 colorBarMinimum -135.0;[10]\n" +
"    String ioos_category \"Location\";[10]\n" +
"    String long_name \"Longitude\";[10]\n" +
"    String standard_name \"longitude\";[10]\n" +
"    String units \"degrees_east\";[10]\n" +
"  }[10]\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = 
"chl_a_total {[10]\n" +
"    Float32 actual_range -2.602, 40.17;[10]\n" + //2009-06-12 was 0.0070, 36.256;[10]\n" +
"    Float64 colorBarMaximum 30.0;[10]\n" +
"    Float64 colorBarMinimum 0.03;[10]\n" +
"    String colorBarScale \"Log\";[10]\n" +
"    String ioos_category \"Ocean Color\";[10]\n" +
"    String long_name \"Chlorophyll-a\";[10]\n" +
"    Float32 missing_value -9999.0;[10]\n" +
"    String standard_name \"concentration_of_chlorophyll_in_sea_water\";[10]\n" +
"    String units \"ug L-1\";[10]\n" +
"  }[10]\n";
        po = results.indexOf("chl_a_total");
        Test.ensureEqual(results.substring(po, po + expected.length()), expected, "\nresults=\n" + results);
        
        //*** test getting dds for entire dataset
        tName = globecBottle.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Entire", ".dds"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    Float32 longitude;[10]\n" +
"    Float32 latitude;[10]\n" +
"    Int32 altitude;[10]\n" +
"    Float64 time;[10]\n" +
"    String ship;[10]\n" +
"    String cruise_id;[10]\n" +
"    Int16 cast;[10]\n" +
"    Int16 bottle_posn;[10]\n" +
"    Float32 chl_a_total;[10]\n" +
"    Float32 chl_a_10um;[10]\n" +
"    Float32 phaeo_total;[10]\n" +
"    Float32 phaeo_10um;[10]\n" +
"    Float32 sal00;[10]\n" +
"    Float32 sal11;[10]\n" +
"    Float32 temperature0;[10]\n" +
"    Float32 temperature1;[10]\n" +
"    Float32 fluor_v;[10]\n" +
"    Float32 xmiss_v;[10]\n" +
"    Float32 PO4;[10]\n" +
"    Float32 N_N;[10]\n" +
"    Float32 NO3;[10]\n" +
"    Float32 Si;[10]\n" +
"    Float32 NO2;[10]\n" +
"    Float32 NH4;[10]\n" +
"    Float32 oxygen;[10]\n" +
"    Float32 par;[10]\n" +
"  } s;[10]\n" +
"} s;[10]\n" +
"[end]";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //*** test DAP data access form
        tName = globecBottle.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Entire", ".html"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
 

        //*** test make data files
        String2.log("\n****************** EDDTableFromDapSequence.test make DATA FILES\n");       

        //.asc
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".asc"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    Float32 longitude;[10]\n" +
"    Float32 NO3;[10]\n" +
"    Float64 time;[10]\n" +
"    String ship;[10]\n" +
"  } s;[10]\n" +
"} s;[10]\n" +
"---------------------------------------------[10]\n" +
"s.longitude, s.NO3, s.time, s.ship[10]\n" +
"-124.4, 35.7, 1.02833814E9, \"New_Horizon\"[10]\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = "-124.8, -9999.0, 1.02835902E9, \"New_Horizon\"[10]\n"; //row with missing value  has source missing value
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = "-124.1, 24.45, 1.02978828E9, \"New_Horizon\"[10]\n[end]"; //last row
        Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

        //.csv
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
            EDStatic.fullTestCacheDirectory, globecBottle.className() + "_Data", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,NO3,time,ship\n" +
"degrees_east,micromoles L-1,UTC,\n" +
"-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n"; 
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = "-124.82,NaN,2002-08-17T00:49:00Z,New_Horizon\n"; //row with missing value  has "NaN" missing value
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = "-124.1,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; //last row
        Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

        //.csvp  and &units("UCUM")
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery + "&units(\"UCUM\")", 
            EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".csvp"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude (deg{east}),NO3 (umol.L-1),time (UTC),ship\n" +
"-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n"; 
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = "-124.82,NaN,2002-08-17T00:49:00Z,New_Horizon\n"; //row with missing value  has "NaN" missing value
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = "-124.1,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; //last row
        Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

        //.csv   test of datasetName.dataVarName notation
        String dotDapQuery = "s.longitude,altitude,NO3,s.time,ship" +
            "&s.latitude>0&altitude>-5&s.time>=2002-08-03";
        tName = globecBottle.makeNewFileForDapQuery(null, null, dotDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_DotNotation", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,altitude,NO3,time,ship\n" +
"degrees_east,m,micromoles L-1,UTC,\n" +
"-124.4,0,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,0,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
"-124.4,0,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
        Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
        expected = "-124.1,0,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; //last row
        Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

        //.csv  test of regex on numeric variable
        tName = globecBottle.makeNewFileForDapQuery(null, null, regexDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_NumRegex", ".csv"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,NO3,time,ship\n" +
"degrees_east,micromoles L-1,UTC,\n" +
"-125.11,33.91,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,26.61,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,10.8,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,8.42,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,6.34,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,1.29,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,0.02,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,0.0,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,10.81,2002-08-09T05:03:00Z,New_Horizon\n" +
"-125.11,42.39,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,33.84,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,27.67,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,15.93,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,8.69,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,4.6,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,2.17,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,8.61,2002-08-18T23:49:00Z,New_Horizon\n" +
"-125.11,0.64,2002-08-18T23:49:00Z,New_Horizon\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv  test of String=
        try {
            String tDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5" +
                "&time>=2002-08-07T00&time<=2002-08-07T06&ship=\"New_Horizon\"";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tDapQuery, EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_StrEq", ".csv"); 
            //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"longitude,NO3,time,ship\n" +
"degrees_east,micromoles L-1,UTC,\n" +
"-124.8,34.54,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,29.98,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,17.24,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,12.74,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,11.43,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,NaN,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,9.74,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,5.62,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,4.4,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,4.21,2002-08-07T01:52:00Z,New_Horizon\n" +
"-125.0,35.28,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,30.87,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,25.2,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,20.66,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,10.85,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,5.44,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,4.69,2002-08-07T03:43:00Z,New_Horizon\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }

        //.csv  test of String< >
        try {
            String tDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5" +
                "&time>=2002-08-07T00&time<=2002-08-07T06&ship>\"Nev\"&ship<\"Nex\"";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tDapQuery, EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_GTLT", ".csv"); 
            //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"longitude,NO3,time,ship\n" +
"degrees_east,micromoles L-1,UTC,\n" +
"-124.8,34.54,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,29.98,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,17.24,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,12.74,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,11.43,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,NaN,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,9.74,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,5.62,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,4.4,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,4.21,2002-08-07T01:52:00Z,New_Horizon\n" +
"-125.0,35.28,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,30.87,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,25.2,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,20.66,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,10.85,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,5.44,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,4.69,2002-08-07T03:43:00Z,New_Horizon\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }

        //.csv    test of String regex
        //!!! I also tested this with 
        //            <sourceCanConstrainStringRegex>~=</sourceCanConstrainStringRegex>
        //but it fails: 
        //Exception in thread "main" dods.dap.DODSException: "Your Query Produced No Matching Results."
        //and it isn't an encoding problem, opera encodes unencoded request as
        //http://192.168.31.18/opendap/GLOBEC/GLOBEC_bottle.dods?lon,NO3,datetime_epoch,ship,lat&lat%3E0&datetime_epoch%3E=1.0286784E9&datetime_epoch%3C=1.0287E9&ship~=%22(zztop|.*Horiz.*)%22
        //which fails the same way
        //Other simpler regex tests succeed.
        //It seems that the regex syntax is different for drds than erddap/java.
        //So recommend that people not say that drds servers can constrain String regex
        try {
            String tDapQuery = "longitude,NO3,time,ship&latitude>0" +
                "&time>=2002-08-07T00&time<=2002-08-07T06&ship=~\"(zztop|.*Horiz.*)\""; //source fails with this
                //"&time>=2002-08-07T00&time<=2002-08-07T06&ship=~\".*Horiz.*\"";       //source works with this
            tName = globecBottle.makeNewFileForDapQuery(null, null, tDapQuery, EDStatic.fullTestCacheDirectory, 
                globecBottle.className() + "_regex", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"longitude,NO3,time,ship\n" +
"degrees_east,micromoles L-1,UTC,\n" +
"-124.8,34.54,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,29.98,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,17.24,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,12.74,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,11.43,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,NaN,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,9.74,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,5.62,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,4.4,2002-08-07T01:52:00Z,New_Horizon\n" +
"-124.8,4.21,2002-08-07T01:52:00Z,New_Horizon\n" +
"-125.0,35.28,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,30.87,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,25.2,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,20.66,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,10.85,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,5.44,2002-08-07T03:43:00Z,New_Horizon\n" +
"-125.0,4.69,2002-08-07T03:43:00Z,New_Horizon\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }


        //.das     das isn't affected by userDapQuery
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".das"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"Attributes {[10]\n" +
" s {[10]\n" +
"  longitude {[10]\n" +
"    String _CoordinateAxisType \"Lon\";[10]\n" +
"    Float32 actual_range -126.2, -124.1;[10]\n" +
"    String axis \"X\";[10]\n" +
"    Float64 colorBarMaximum -115.0;[10]\n" +
"    Float64 colorBarMinimum -135.0;[10]\n" +
"    String ioos_category \"Location\";[10]\n" +
"    String long_name \"Longitude\";[10]\n" +
"    String standard_name \"longitude\";[10]\n" +
"    String units \"degrees_east\";[10]\n" +
"  }[10]\n" +
"  latitude {[10]\n";
        Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
        expected = 
"  par {[10]\n" +
"    Float32 actual_range 0.1515, 3.261;[10]\n" +
"    Float64 colorBarMaximum 3.0;[10]\n" +
"    Float64 colorBarMinimum 0.0;[10]\n" +
"    String ioos_category \"Ocean Color\";[10]\n" +
"    String long_name \"Photosynthetically Active Radiation\";[10]\n" +
"    Float32 missing_value -9999.0;[10]\n" +    //note destinationMissingValue
"    String units \"volts\";[10]\n" +
"  }[10]\n" +
" }[10]\n" +
"  NC_GLOBAL {[10]\n" +
"    String cdm_data_type \"TrajectoryProfile\";[10]\n" +
"    String cdm_profile_variables \"time, cast, longitude, latitude\";[10]\n" +
"    String cdm_trajectory_variables \"cruise_id, ship\";[10]\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";[10]\n" +
"    Float64 Easternmost_Easting -124.1;[10]\n" +
"    String featureType \"TrajectoryProfile\";[10]\n" +
"    Float64 geospatial_lat_max 44.65;[10]\n" +
"    Float64 geospatial_lat_min 41.9;[10]\n" +
"    String geospatial_lat_units \"degrees_north\";[10]\n" +
"    Float64 geospatial_lon_max -124.1;[10]\n" +
"    Float64 geospatial_lon_min -126.2;[10]\n" +
"    String geospatial_lon_units \"degrees_east\";[10]\n" +
"    Float64 geospatial_vertical_max 0.0;[10]\n" +
"    Float64 geospatial_vertical_min 0.0;[10]\n" +
"    String geospatial_vertical_positive \"up\";[10]\n" +
"    String geospatial_vertical_units \"m\";[10]\n" +
"    String history \"" + today;
        int tpo = results.indexOf(expected.substring(0, 17));
        Test.ensureEqual(results.substring(tpo, tpo + expected.length()), expected, 
            "\nresults=\n" + results);

//+ " http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle[10]\n" +
//today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
expected = "/tabledap/testGlobecBottle.das\";[10]\n" +
"    String infoUrl \"http://oceanwatch.pfeg.noaa.gov/thredds/PaCOOS/GLOBEC/catalog.html?dataset=GLOBEC_Bottle_data\";[10]\n" +
"    String institution \"GLOBEC\";[10]\n" +
"    String keywords \"Oceans > Salinity/Density > Salinity\";[10]\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";[10]\n" +
"    String license \"The data may be used and redistributed for free but is not intended[10]\n" +
"for legal use, since it may contain inaccuracies. Neither the data[10]\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any[10]\n" +
"of their employees or contractors, makes any warranty, express or[10]\n" +
"implied, including warranties of merchantability and fitness for a[10]\n" +
"particular purpose, or assumes any legal liability for the accuracy,[10]\n" +
"completeness, or usefulness, of this information.\";[10]\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";[10]\n" +
"    Float64 Northernmost_Northing 44.65;[10]\n" +
"    String sourceUrl \"http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\";[10]\n" +
"    Float64 Southernmost_Northing 41.9;[10]\n" +
"    String standard_name_vocabulary \"CF-12\";[10]\n" +
"    String subsetVariables \"ship, cruise_id, cast, longitude, latitude\";[10]\n" +
"    String summary \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)[10]\n" +
"Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).[10]\n" +
"Notes:[10]\n" +
"Physical data processed by Jane Fleischbein (OSU).[10]\n" +
"Chlorophyll readings done by Leah Feinberg (OSU).[10]\n" +
"Nutrient analysis done by Burke Hales (OSU).[10]\n" +
"Sal00 - salinity calculated from primary sensors (C0,T0).[10]\n" +
"Sal11 - salinity calc. from secondary sensors (C1,T1).[10]\n" +
"secondary sensor pair was used in final processing of CTD data for[10]\n" +
"most stations because the primary had more noise and spikes. The[10]\n" +
"primary pair were used for cast #9, 24, 48, 111 and 150 due to[10]\n" +
"multiple spikes or offsets in the secondary pair.[10]\n" +
"Nutrient samples were collected from most bottles; all nutrient data[10]\n" +
"developed from samples frozen during the cruise and analyzed ashore;[10]\n" +
"data developed by Burke Hales (OSU).[10]\n" +
"Operation Detection Limits for Nutrient Concentrations[10]\n" +
"Nutrient  Range         Mean    Variable         Units[10]\n" +
"PO4       0.003-0.004   0.004   Phosphate        micromoles per liter[10]\n" +
"N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter[10]\n" +
"Si        0.13-0.24     0.16    Silicate         micromoles per liter[10]\n" +
"NO2       0.003-0.004   0.003   Nitrite          micromoles per liter[10]\n" +
"Dates and Times are UTC.[10]\n" +
"[10]\n" +
"For more information, see[10]\n" +
"http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view[10]\n" +
"[10]\n" +
"Inquiries about how to access this data should be directed to[10]\n" +
"Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";[10]\n" +
"    String time_coverage_end \"2002-08-19T20:18:00Z\";[10]\n" +
"    String time_coverage_start \"2002-05-30T03:21:00Z\";[10]\n" +
"    String title \"GLOBEC NEP Rosette Bottle Data (2002)\";[10]\n" +
"    Float64 Westernmost_Easting -126.2;[10]\n" +
"  }[10]\n" +
"}[10]\n" +
"[end]";
        tpo = results.indexOf(expected.substring(0, 17));
        Test.ensureEqual(results.substring(tpo, tpo + expected.length()), expected, 
            "\nresults=\n" + results);

        //.dds 
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".dds"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"Dataset {[10]\n" +
"  Sequence {[10]\n" +
"    Float32 longitude;[10]\n" +
"    Float32 NO3;[10]\n" +
"    Float64 time;[10]\n" +
"    String ship;[10]\n" +
"  } s;[10]\n" +
"} s;[10]\n" +
"[end]";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.dods
        //tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
        //    globecBottle.className() + "_Data", ".dods"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        try {
            String2.log("\ndo .dods test");
            String tUrl = EDStatic.erddapUrl + //in tests, always use non-https url
                "/tabledap/" + globecBottle.datasetID;
            //for diagnosing during development:
            //String2.log(String2.annotatedString(SSR.getUrlResponseString(
            //    "http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt.dods?stn_id&unique()")));
            //String2.log("\nDAS RESPONSE=" + SSR.getUrlResponseString(tUrl + ".das?" + userDapQuery));
            //String2.log("\nDODS RESPONSE=" + String2.annotatedString(SSR.getUrlResponseString(tUrl + ".dods?" + userDapQuery)));

            //test if table.readOpendapSequence works with Erddap opendap server
            //!!!THIS READS DATA FROM ERDDAP SERVER RUNNING ON EDStatic.erddapUrl!!! //in tests, always use non-https url                
            //!!!THIS IS NOT JUST A LOCAL TEST!!!
            Table tTable = new Table();
            tTable.readOpendapSequence(tUrl + "?" + userDapQuery, false);
            Test.ensureEqual(tTable.globalAttributes().getString("title"), "GLOBEC NEP Rosette Bottle Data (2002)", "");
            Test.ensureEqual(tTable.columnAttributes(2).getString("units"), EDV.TIME_UNITS, "");
            Test.ensureEqual(tTable.getColumnNames(), new String[]{"longitude", "NO3", "time", "ship"}, "");
            Test.ensureEqual(tTable.getFloatData(0, 0), -124.4f, "");
            Test.ensureEqual(tTable.getFloatData(1, 0), 35.7f, "");
            Test.ensureEqual(tTable.getDoubleData(2, 0), 1.02833814E9, "");
            Test.ensureEqual(tTable.getStringData(3, 0), "New_Horizon", "");
            String2.log("  .dods test succeeded");
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError accessing " + EDStatic.erddapUrl + //in tests, always use non-https url
                " and reading erddap as a data source." +
                "\nPress ^C to stop or Enter to continue..."); 
        }


        //.esriCsv
        tName = globecBottle.makeNewFileForDapQuery(null, null, 
            "&time>=2002-08-03", 
            EDStatic.fullTestCacheDirectory, 
            "testEsri5", ".esriCsv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"X,Y,altitude,date,time,ship,cruise_id,cast,bottle_pos,chl_a_tota,chl_a_10um,phaeo_tota,phaeo_10um,sal00,sal11,temperatur,temperatuA,fluor_v,xmiss_v,PO4,N_N,NO3,Si,NO2,NH4,oxygen,par\n" +
"-124.4,44.0,0,2002-08-03,1:29:00 am,New_Horizon,nh0207,20,1,-9999.0,-9999.0,-9999.0,-9999.0,33.9939,33.9908,7.085,7.085,0.256,0.518,2.794,35.8,35.7,71.11,0.093,0.037,-9999.0,0.1545\n" +
"-124.4,44.0,0,2002-08-03,1:29:00 am,New_Horizon,nh0207,20,2,-9999.0,-9999.0,-9999.0,-9999.0,33.8154,33.8111,7.528,7.53,0.551,0.518,2.726,35.87,35.48,57.59,0.385,0.018,-9999.0,0.1767\n" +
"-124.4,44.0,0,2002-08-03,1:29:00 am,New_Horizon,nh0207,20,3,1.463,-9999.0,1.074,-9999.0,33.5858,33.5834,7.572,7.573,0.533,0.518,2.483,31.92,31.61,48.54,0.307,0.504,-9999.0,0.3875\n" +
"-124.4,44.0,0,2002-08-03,1:29:00 am,New_Horizon,nh0207,20,4,2.678,-9999.0,1.64,-9999.0,33.2905,33.2865,8.093,8.098,1.244,0.518,2.262,27.83,27.44,42.59,0.391,0.893,-9999.0,0.7674\n"; 
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);


        //.geoJson    mapDapQuery so lon and lat are in query
        tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_DataGJ", ".geoJson"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"{\n" +
"  \"type\": \"FeatureCollection\",\n" +
"  \"propertyNames\": [\"NO3\", \"time\"],\n" +
"  \"propertyUnits\": [\"micromoles L-1\", \"UTC\"],\n" +
"  \"features\": [\n" +
"\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.4, 44.0] },\n" +
"  \"properties\": {\n" +
"    \"NO3\": 35.7,\n" +
"    \"time\": \"2002-08-03T01:29:00Z\" }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.4, 44.0] },\n" +
"  \"properties\": {\n" +
"    \"NO3\": 35.48,\n" +
"    \"time\": \"2002-08-03T01:29:00Z\" }\n" +
"},\n";
        tResults = results.substring(0, expected.length());
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

expected = 
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [-124.1, 44.65] },\n" +
"  \"properties\": {\n" +
"    \"NO3\": 24.45,\n" +
"    \"time\": \"2002-08-19T20:18:00Z\" }\n" +
"}\n" +
"\n" +
"  ],\n" +
"  \"bbox\": [-126.0, 41.9, -124.1, 44.65]\n" +
"}\n";
        tResults = results.substring(results.length() - expected.length());
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

        //.geoJson    just lon and lat in response
        tName = globecBottle.makeNewFileForDapQuery(null, null, 
            "longitude,latitude&latitude>0&altitude>-5&time>=2002-08-03",
            EDStatic.fullTestCacheDirectory, globecBottle.className() + "_DataGJLL", ".geoJson"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"{\n" +
"  \"type\": \"MultiPoint\",\n" +
"  \"coordinates\": [\n" +
"\n" +
"[-124.4, 44.0],\n" +
"[-124.4, 44.0],\n";
        tResults = results.substring(0, expected.length());
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

expected = 
"[-124.1, 44.65]\n" +
"\n" +
"  ],\n" +
"  \"bbox\": [-126.0, 41.9, -124.1, 44.65]\n" +
"}\n";
        tResults = results.substring(results.length() - expected.length());
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

        //.geoJson   with jsonp
        String jsonp = "Some encoded {}\n() ! text";
        tName = globecBottle.makeNewFileForDapQuery(null, null, 
            "longitude,latitude&latitude>0&altitude>-5&time>=2002-08-03" + "&.jsonp=" + SSR.percentEncode(jsonp),
            EDStatic.fullTestCacheDirectory, globecBottle.className() + "_DataGJLL", ".geoJson"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = jsonp + "(" +
"{\n" +
"  \"type\": \"MultiPoint\",\n" +
"  \"coordinates\": [\n" +
"\n" +
"[-124.4, 44.0],\n" +
"[-124.4, 44.0],\n";
        tResults = results.substring(0, expected.length());
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

expected = 
"[-124.1, 44.65]\n" +
"\n" +
"  ],\n" +
"  \"bbox\": [-126.0, 41.9, -124.1, 44.65]\n" +
"}\n" +
")"; 
        tResults = results.substring(results.length() - expected.length());
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);


        //.htmlTable
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".htmlTable"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
EDStatic.startHeadHtml(EDStatic.erddapUrl((String)null), "EDDTableFromDapSequence_Data") + "\n" +
"</head>\n" +
EDStatic.startBodyHtml(null) + "\n" +
"&nbsp;\n" +
"<form action=\"\">\n" +
"<input type=\"button\" value=\"Back\" onClick=\"history.go(-1);return true;\">\n" +
"</form>\n" +
"\n" +
"&nbsp;\n" +
"<table class=\"erd commonBGColor\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>longitude\n" +
"<th>NO3\n" +
"<th>time\n" +
"<th>ship\n" +
"</tr>\n" +
"<tr>\n" +
"<th>degrees_east\n" +
"<th>micromoles L-1\n" +
"<th>UTC\n" +
"<th>&nbsp;\n" + //note &nbsp;
"</tr>\n" +
"<tr>\n" +
"<td nowrap align=\"right\">-124.4\n" +
"<td align=\"right\">35.7\n" +
"<td nowrap>2002-08-03T01:29:00Z\n" +
"<td nowrap>New_Horizon\n" +
"</tr>\n";
        tResults = results.substring(0, expected.length());
        Test.ensureEqual(tResults, expected,  "\ntResults=\n" + tResults);
        expected =  //row with missing value  has "&nbsp;" missing value
"<tr>\n" +
"<td nowrap align=\"right\">-124.1\n" +
"<td align=\"right\">24.45\n" +
"<td nowrap>2002-08-19T20:18:00Z\n" +
"<td nowrap>New_Horizon\n" +
"</tr>\n" +
"</table>\n" +
EDStatic.endBodyHtml(EDStatic.erddapUrl((String)null)) + "\n" +
"</html>\n";
        tResults = results.substring(results.length() - expected.length());
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

        //.json
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".json"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"longitude\", \"NO3\", \"time\", \"ship\"],\n" +
"    \"columnTypes\": [\"float\", \"float\", \"String\", \"String\"],\n" +
"    \"columnUnits\": [\"degrees_east\", \"micromoles L-1\", \"UTC\", null],\n" +
"    \"rows\": [\n" +
"      [-124.4, 35.7, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n" +
"      [-124.4, 35.48, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = 
"      [-125.0, null, \"2002-08-18T13:03:00Z\", \"New_Horizon\"],\n"; //row with missing value  has "null"
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = 
        "      [-124.1, 24.45, \"2002-08-19T20:18:00Z\", \"New_Horizon\"]\n" +
        "    ]\n" +
        "  }\n" +
        "}\n"; //last rows
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

        //.json  with jsonp query
        tName = globecBottle.makeNewFileForDapQuery(null, null, 
            userDapQuery + "&.jsonp=" + SSR.percentEncode(jsonp), 
            EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".json"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = jsonp + "(" +
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"longitude\", \"NO3\", \"time\", \"ship\"],\n" +
"    \"columnTypes\": [\"float\", \"float\", \"String\", \"String\"],\n" +
"    \"columnUnits\": [\"degrees_east\", \"micromoles L-1\", \"UTC\", null],\n" +
"    \"rows\": [\n" +
"      [-124.4, 35.7, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n" +
"      [-124.4, 35.48, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = 
        "      [-124.1, 24.45, \"2002-08-19T20:18:00Z\", \"New_Horizon\"]\n" +
        "    ]\n" +
        "  }\n" +
        "}\n" +
        ")"; //last rows  
        Test.ensureEqual(results.substring(results.length() - expected.length()), 
            expected, "\nresults=\n" + results);
 
        //.mat     [I can't test that missing value is NaN.]
        //octave> load('c:/temp/tabledap/EDDTableFromDapSequence_Data.mat');
        //octave> testGlobecBottle
        //2010-07-14 Roy can read this file in Matlab, previously. text didn't show up.
        tName = globecBottle.makeNewFileForDapQuery(null, null, regexDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".mat"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = File2.hexDump(EDStatic.fullTestCacheDirectory + tName, 1000000);
        //String2.log(results);
        Test.ensureEqual(
            results.substring(0, 71 * 4) + results.substring(71 * 7), //remove the creation dateTime
"4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n" +
"69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n" +
"20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n" +
"6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n" +
//"2c 20 43 72 65 61 74 65   64 20 6f 6e 3a 20 4d 6f   , Created on: Mo |\n" +
//"6e 20 44 65 63 20 38 20   31 32 3a 34 35 3a 34 36   n Dec 8 12:45:46 |\n" +
//"20 32 30 30 38 20 20 20   20 20 20 20 20 20 20 20    2008            |\n" +
"20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n" +
"00 00 00 0e 00 00 04 58   00 00 00 06 00 00 00 08          X         |\n" +
"00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 10                    |\n" +
"74 65 73 74 47 6c 6f 62   65 63 42 6f 74 74 6c 65   testGlobecBottle |\n" +
"00 04 00 05 00 00 00 20   00 00 00 01 00 00 00 80                    |\n" +
"6c 6f 6e 67 69 74 75 64   65 00 00 00 00 00 00 00   longitude        |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"4e 4f 33 00 00 00 00 00   00 00 00 00 00 00 00 00   NO3              |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"74 69 6d 65 00 00 00 00   00 00 00 00 00 00 00 00   time             |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"73 68 69 70 00 00 00 00   00 00 00 00 00 00 00 00   ship             |\n" +
"00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" +
"00 00 00 0e 00 00 00 78   00 00 00 06 00 00 00 08          x         |\n" +
"00 00 00 07 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 12 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 07 00 00 00 48   c2 fa 38 52 c2 fa 38 52          H  8R  8R |\n" +
"c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n" +
"c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n" +
"c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n" +
"c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n" +
"00 00 00 0e 00 00 00 78   00 00 00 06 00 00 00 08          x         |\n" +
"00 00 00 07 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 12 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 07 00 00 00 48   42 07 a3 d7 41 d4 e1 48          HB   A  H |\n" +
"41 2c cc cd 41 06 b8 52   40 ca e1 48 3f a5 1e b8   A,  A  R@  H?    |\n" +
"3c a3 d7 0a 00 00 00 00   41 2c f5 c3 42 29 8f 5c   <       A,  B) \\ |\n" +
"42 07 5c 29 41 dd 5c 29   41 7e e1 48 41 0b 0a 3d   B \\)A \\)A~ HA  = |\n" +
"40 93 33 33 40 0a e1 48   41 09 c2 8f 3f 23 d7 0a   @ 33@  HA   ?#   |\n" +
"00 00 00 0e 00 00 00 c0   00 00 00 06 00 00 00 08                    |\n" +
"00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" +
"00 00 00 12 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" +
"00 00 00 09 00 00 00 90   41 ce a9 a6 82 00 00 00           A        |\n" +
"41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n" +
"41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n" +
"41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n" +
"41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n" +
"41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n" +
"41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n" +
"41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n" +
"41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n" +
"41 ce b0 19 36 00 00 00   00 00 00 0e 00 00 01 c0   A   6            |\n" +
"00 00 00 06 00 00 00 08   00 00 00 04 00 00 00 00                    |\n" +
"00 00 00 05 00 00 00 08   00 00 00 12 00 00 00 0b                    |\n" +
"00 00 00 01 00 00 00 00   00 00 00 04 00 00 01 8c                    |\n" +
"00 4e 00 4e 00 4e 00 4e   00 4e 00 4e 00 4e 00 4e    N N N N N N N N |\n" +
"00 4e 00 4e 00 4e 00 4e   00 4e 00 4e 00 4e 00 4e    N N N N N N N N |\n" +
"00 4e 00 4e 00 65 00 65   00 65 00 65 00 65 00 65    N N e e e e e e |\n" +
"00 65 00 65 00 65 00 65   00 65 00 65 00 65 00 65    e e e e e e e e |\n" +
"00 65 00 65 00 65 00 65   00 77 00 77 00 77 00 77    e e e e w w w w |\n" +
"00 77 00 77 00 77 00 77   00 77 00 77 00 77 00 77    w w w w w w w w |\n" +
"00 77 00 77 00 77 00 77   00 77 00 77 00 5f 00 5f    w w w w w w _ _ |\n" +
"00 5f 00 5f 00 5f 00 5f   00 5f 00 5f 00 5f 00 5f    _ _ _ _ _ _ _ _ |\n" +
"00 5f 00 5f 00 5f 00 5f   00 5f 00 5f 00 5f 00 5f    _ _ _ _ _ _ _ _ |\n" +
"00 48 00 48 00 48 00 48   00 48 00 48 00 48 00 48    H H H H H H H H |\n" +
"00 48 00 48 00 48 00 48   00 48 00 48 00 48 00 48    H H H H H H H H |\n" +
"00 48 00 48 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    H H o o o o o o |\n" +
"00 6f 00 6f 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    o o o o o o o o |\n" +
"00 6f 00 6f 00 6f 00 6f   00 72 00 72 00 72 00 72    o o o o r r r r |\n" +
"00 72 00 72 00 72 00 72   00 72 00 72 00 72 00 72    r r r r r r r r |\n" +
"00 72 00 72 00 72 00 72   00 72 00 72 00 69 00 69    r r r r r r i i |\n" +
"00 69 00 69 00 69 00 69   00 69 00 69 00 69 00 69    i i i i i i i i |\n" +
"00 69 00 69 00 69 00 69   00 69 00 69 00 69 00 69    i i i i i i i i |\n" +
"00 7a 00 7a 00 7a 00 7a   00 7a 00 7a 00 7a 00 7a    z z z z z z z z |\n" +
"00 7a 00 7a 00 7a 00 7a   00 7a 00 7a 00 7a 00 7a    z z z z z z z z |\n" +
"00 7a 00 7a 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    z z o o o o o o |\n" +
"00 6f 00 6f 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    o o o o o o o o |\n" +
"00 6f 00 6f 00 6f 00 6f   00 6e 00 6e 00 6e 00 6e    o o o o n n n n |\n" +
"00 6e 00 6e 00 6e 00 6e   00 6e 00 6e 00 6e 00 6e    n n n n n n n n |\n" +
"00 6e 00 6e 00 6e 00 6e   00 6e 00 6e 00 00 00 00    n n n n n n     |\n",
"\nresults=\n" + results);

        //.nc    
        //!!! This is also a test of missing_value and _FillValue both active
        String tUserDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-14&time<=2002-08-15";
        tName = globecBottle.makeNewFileForDapQuery(null, null, tUserDapQuery, 
            EDStatic.fullTestCacheDirectory, globecBottle.className() + "_Data", ".nc"); 
        results = NcHelper.dumpString(EDStatic.fullTestCacheDirectory + tName, true);
        String tHeader1 = 
"netcdf EDDTableFromDapSequence_Data.nc {\n" +
" dimensions:\n" +
"   row = 100;\n" +
"   ship_strlen = 11;\n" +
" variables:\n" +
"   float longitude(row=100);\n" +
"     :_CoordinateAxisType = \"Lon\";\n" +
"     :actual_range = -125.67f, -124.8f; // float\n" +
"     :axis = \"X\";\n" +
"     :colorBarMaximum = -115.0; // double\n" +
"     :colorBarMinimum = -135.0; // double\n" +
"     :ioos_category = \"Location\";\n" +
"     :long_name = \"Longitude\";\n" +
"     :standard_name = \"longitude\";\n" +
"     :units = \"degrees_east\";\n" +
"   float NO3(row=100);\n" +
"     :_FillValue = -99.0f; // float\n" +
"     :actual_range = 0.46f, 34.09f; // float\n" +
"     :colorBarMaximum = 50.0; // double\n" +
"     :colorBarMinimum = 0.0; // double\n" +
"     :ioos_category = \"Dissolved Nutrients\";\n" +
"     :long_name = \"Nitrate\";\n" +
"     :missing_value = -9999.0f; // float\n" +
"     :standard_name = \"mole_concentration_of_nitrate_in_sea_water\";\n" +
"     :units = \"micromoles L-1\";\n" +
"   double time(row=100);\n" +
"     :_CoordinateAxisType = \"Time\";\n" +
"     :actual_range = 1.02928674E9, 1.02936804E9; // double\n" +
"     :axis = \"T\";\n" +
"     :cf_role = \"profile_id\";\n" +
"     :ioos_category = \"Time\";\n" +
"     :long_name = \"Time\";\n" +
"     :standard_name = \"time\";\n" +  
"     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"   char ship(row=100, ship_strlen=11);\n" +
"     :ioos_category = \"Identifier\";\n" +
"     :long_name = \"Ship\";\n" +
"\n" +
" :cdm_data_type = \"TrajectoryProfile\";\n" +
" :cdm_profile_variables = \"time, cast, longitude, latitude\";\n" +
" :cdm_trajectory_variables = \"cruise_id, ship\";\n" +
" :Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :Easternmost_Easting = -124.8f; // float\n" +
" :featureType = \"TrajectoryProfile\";\n" +
" :geospatial_lat_units = \"degrees_north\";\n" +
" :geospatial_lon_max = -124.8f; // float\n" +
" :geospatial_lon_min = -125.67f; // float\n" +
" :geospatial_lon_units = \"degrees_east\";\n" +
" :geospatial_vertical_positive = \"up\";\n" +
" :geospatial_vertical_units = \"m\";\n" +
" :history = \"" + today;
        tResults = results.substring(0, tHeader1.length());
        Test.ensureEqual(tResults, tHeader1, "\nresults=\n" + results);
        
//        + " http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
//today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
String tHeader2 = 
"/tabledap/testGlobecBottle.nc?longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-14&time<=2002-08-15\";\n" +
" :id = \"EDDTableFromDapSequence_Data\";\n" +
" :infoUrl = \"http://oceanwatch.pfeg.noaa.gov/thredds/PaCOOS/GLOBEC/catalog.html?dataset=GLOBEC_Bottle_data\";\n" +
" :institution = \"GLOBEC\";\n" +
" :keywords = \"Oceans > Salinity/Density > Salinity\";\n" +
" :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
" :license = \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
" :Metadata_Conventions = \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
" :observationDimension = \"row\";\n" +
" :sourceUrl = \"http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\";\n" +
" :standard_name_vocabulary = \"CF-12\";\n" +
" :subsetVariables = \"ship, cruise_id, cast, longitude, latitude\";\n" +
" :summary = \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" +
"Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" +
"Notes:\n" +
"Physical data processed by Jane Fleischbein (OSU).\n" +
"Chlorophyll readings done by Leah Feinberg (OSU).\n" +
"Nutrient analysis done by Burke Hales (OSU).\n" +
"Sal00 - salinity calculated from primary sensors (C0,T0).\n" +
"Sal11 - salinity calc. from secondary sensors (C1,T1).\n" +
"secondary sensor pair was used in final processing of CTD data for\n" +
"most stations because the primary had more noise and spikes. The\n" +
"primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" +
"multiple spikes or offsets in the secondary pair.\n" +
"Nutrient samples were collected from most bottles; all nutrient data\n" +
"developed from samples frozen during the cruise and analyzed ashore;\n" +
"data developed by Burke Hales (OSU).\n" +
"Operation Detection Limits for Nutrient Concentrations\n" +
"Nutrient  Range         Mean    Variable         Units\n" +
"PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" +
"N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" +
"Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" +
"NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" +
"Dates and Times are UTC.\n" +
"\n" +
"For more information, see\n" +
"http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\n" +
"\n" +
"Inquiries about how to access this data should be directed to\n" +
"Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n" +
" :time_coverage_end = \"2002-08-14T23:34:00Z\";\n" +
" :time_coverage_start = \"2002-08-14T00:59:00Z\";\n" +
" :title = \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
" :Westernmost_Easting = -125.67f; // float\n" +
" data:\n";
        tpo = results.indexOf(tHeader2.substring(0, 17));
        if (tpo < 0) String2.log("results=\n" + results);
        Test.ensureEqual(results.substring(tpo, tpo + tHeader2.length()), tHeader2, 
            "results=\n" + results);

expected = 
"longitude =\n" +
"  {-124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2}\n" +
"NO3 =\n" +
"  {33.66, 30.43, 28.22, 26.4, 25.63, 23.54, 22.38, 20.15, 33.55, 31.48, 24.93, -99.0, 21.21, 20.54, 17.87, -9999.0, 16.32, 33.61, 33.48, 30.7, 27.05, 25.13, 24.5, 23.95, 16.0, 14.42, 33.28, 28.3, 26.74, 24.96, 23.78, 20.76, 17.72, 16.01, 31.22, 27.47, 13.28, 10.66, 9.61, 8.36, 6.53, 2.86, 0.96, 34.05, 29.47, 18.87, 15.17, 13.84, 9.61, 4.95, 3.46, 34.09, 23.29, 16.01, 10.35, 7.72, 4.37, 2.97, 27.25, 29.98, 22.56, 9.82, 9.19, 6.57, 5.23, 3.81, 0.96, 30.08, 19.88, 8.44, 4.59, 2.67, 1.53, 0.94, 0.47, 30.73, 20.28, 10.61, 7.48, 6.53, 4.51, 3.04, 1.36, 0.89, 32.21, 23.75, 12.04, 7.67, 5.73, 1.14, 1.02, 0.46, 33.16, 27.33, 15.16, 9.7, 9.47, 8.66, 7.65, 4.84}\n" +
"time =\n" +
"  {1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9}\n" +
"ship =\"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\", \"New_Horizon\"\n" +
"}\n";
        tpo = results.indexOf(expected.substring(0, 17));
        if (tpo < 0) String2.log("results=\n" + results);
        Test.ensureEqual(results.substring(tpo, tpo + expected.length()), expected, 
            "results=\n" + results);

        //.ncHeader
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".ncHeader"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);

        tResults = results.substring(0, tHeader1.length());
        Test.ensureEqual(tResults, tHeader1, "\nresults=\n" + results);

        expected = tHeader2 + "}\n";
        tpo = results.indexOf(expected.substring(0, 17));
        if (tpo < 0) String2.log("results=\n" + results);
        Test.ensureEqual(results.substring(tpo, tpo + expected.length()), expected, 
            "results=\n" + results);


        //.odvTxt
        try {
            tName = globecBottle.makeNewFileForDapQuery(null, null, 
                "&latitude>0&time>=2002-08-03", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_ODV", ".odvTxt"); 
            String2.log("ODV fileName=" + EDStatic.fullTestCacheDirectory + tName);
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
    "//<Creator>http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle</Creator>\n" +
    "//<CreateTime>" + today;
            Test.ensureEqual(results.substring(0, expected.length()), expected, 
                "\nresults=\n" + String2.annotatedString(results));


    //T21:09:15
            expected = 
    "</CreateTime>\n" +
    "//<Software>ERDDAP - Version 1.39</Software>\n" +
    "//<Source>http://127.0.0.1:8080/cwexperimental/tabledap/testGlobecBottle.html</Source>\n" +
    "//<Version>ODV Spreadsheet V4.0</Version>\n" +
    "//<DataField>GeneralField</DataField>\n" +
    "//<DataType>GeneralType</DataType>\n" +
    "Type:METAVAR:TEXT:2\tStation:METAVAR:TEXT:2\tLongitude [degrees_east]:METAVAR:FLOAT\tLatitude [degrees_north]:METAVAR:FLOAT\taltitude [m]:PRIMARYVAR:INTEGER\tyyyy-mm-ddThh:mm:ss.sss\tship:METAVAR:TEXT:12\tCruise:METAVAR:TEXT:7\tcast:SHORT\tbottle_posn:SHORT\tchl_a_total [ug L-1]:FLOAT\tchl_a_10um [ug L-1]:FLOAT\tphaeo_total [ug L-1]:FLOAT\tphaeo_10um [ug L-1]:FLOAT\tsal00 [PSU]:FLOAT\tsal11 [PSU]:FLOAT\ttemperature0 [degree_C]:FLOAT\ttemperature1 [degree_C]:FLOAT\tfluor_v [volts]:FLOAT\txmiss_v [volts]:FLOAT\tPO4 [micromoles L-1]:FLOAT\tN_N [micromoles L-1]:FLOAT\tNO3 [micromoles L-1]:FLOAT\tSi [micromoles L-1]:FLOAT\tNO2 [micromoles L-1]:FLOAT\tNH4 [micromoles L-1]:FLOAT\toxygen [mL L-1]:FLOAT\tpar [volts]:FLOAT\n" +
    "*\t\t-124.4\t44.0\t0\t2002-08-03T01:29:00\tNew_Horizon\tnh0207\t20\t1\t\t\t\t\t33.9939\t33.9908\t7.085\t7.085\t0.256\t0.518\t2.794\t35.8\t35.7\t71.11\t0.093\t0.037\t\t0.1545\n" +
    "*\t\t-124.4\t44.0\t0\t2002-08-03T01:29:00\tNew_Horizon\tnh0207\t20\t2\t\t\t\t\t33.8154\t33.8111\t7.528\t7.53\t0.551\t0.518\t2.726\t35.87\t35.48\t57.59\t0.385\t0.018\t\t0.1767\n" +
    "*\t\t-124.4\t44.0\t0\t2002-08-03T01:29:00\tNew_Horizon\tnh0207\t20\t3\t1.463\t\t1.074\t\t33.5858\t33.5834\t7.572\t7.573\t0.533\t0.518\t2.483\t31.92\t31.61\t48.54\t0.307\t0.504\t\t0.3875\n" +
    "*\t\t-124.4\t44.0\t0\t2002-08-03T01:29:00\tNew_Horizon\tnh0207\t20\t4\t2.678\t\t1.64\t\t33.2905\t33.2865\t8.093\t8.098\t1.244\t0.518\t2.262\t27.83\t27.44\t42.59\t0.391\t0.893\t\t0.7674\n";
            po = results.indexOf(expected.substring(0, 13));
            Test.ensureEqual(results.substring(po, po + expected.length()), expected, 
                "\nresults=\n" + String2.annotatedString(results));
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error\nPress ^C to stop or Enter to continue..."); 
        }


        //.tsv
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".tsv"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude\tNO3\ttime\tship\n" +
"degrees_east\tmicromoles L-1\tUTC\t\n" +
"-124.4\t35.7\t2002-08-03T01:29:00Z\tNew_Horizon\n";
        Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
        expected = "-124.8\tNaN\t2002-08-03T07:17:00Z\tNew_Horizon\n"; //row with missing value  has "NaN" missing value
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = "-124.1\t24.45\t2002-08-19T20:18:00Z\tNew_Horizon\n"; //last row
        Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

        //.tsvp
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".tsvp"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude (degrees_east)\tNO3 (micromoles L-1)\ttime (UTC)\tship\n" +
"-124.4\t35.7\t2002-08-03T01:29:00Z\tNew_Horizon\n";
        Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
        expected = "-124.8\tNaN\t2002-08-03T07:17:00Z\tNew_Horizon\n"; //row with missing value  has "NaN" missing value
        Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
        expected = "-124.1\t24.45\t2002-08-19T20:18:00Z\tNew_Horizon\n"; //last row
        Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

        //.xhtml
        tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "_Data", ".xhtml"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>EDDTableFromDapSequence_Data</title>\n" +
"</head>\n" +
"<body style=\"color:black; background:white; font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n" +
"\n" +
"&nbsp;\n" +
"<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>longitude</th>\n" +
"<th>NO3</th>\n" +
"<th>time</th>\n" +
"<th>ship</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>degrees_east</th>\n" +
"<th>micromoles L-1</th>\n" +
"<th>UTC</th>\n" +
"<th></th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\" align=\"right\">-124.4</td>\n" +
"<td align=\"right\">35.7</td>\n" +
"<td nowrap=\"nowrap\">2002-08-03T01:29:00Z</td>\n" +
"<td nowrap=\"nowrap\">New_Horizon</td>\n" +
"</tr>\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected =  //row with missing value  has "" missing value
"<tr>\n" +
"<td nowrap=\"nowrap\" align=\"right\">-124.1</td>\n" +
"<td align=\"right\">24.45</td>\n" +
"<td nowrap=\"nowrap\">2002-08-19T20:18:00Z</td>\n" +
"<td nowrap=\"nowrap\">New_Horizon</td>\n" +
"</tr>\n" +
"</table>\n" +
"</body>\n" +
"</html>\n";
        tResults = results.substring(results.length() - expected.length());
        Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

        //data for mapExample
        tName = globecBottle.makeNewFileForDapQuery(null, null, 
            "longitude,latitude&time>=2002-08-03&time<=2002-08-04", 
            EDStatic.fullTestCacheDirectory, 
            globecBottle.className() + "Map", ".csv");
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude,latitude\n" +
"degrees_east,degrees_north\n" +
"-124.4,44.0\n" +
"-124.4,44.0\n" +
"-124.4,44.0\n" +
"-124.4,44.0\n" +
"-124.4,44.0\n" +
"-124.4,44.0\n" +
"-124.4,44.0\n" +
"-124.4,44.0\n" +
"-124.6,44.0\n" +
"-124.6,44.0\n" +
"-124.6,44.0\n" +
"-124.6,44.0\n" +
"-124.6,44.0\n" +
"-124.6,44.0\n" +
"-124.6,44.0\n" +
"-124.6,44.0\n" +
"-124.6,44.0\n" +
"-124.8,44.0\n" +
"-124.8,44.0\n" +
"-124.8,44.0\n" +
"-124.8,44.0\n" +
"-124.8,44.0\n" +
"-124.8,44.0\n" +
"-124.8,44.0\n" +
"-124.8,44.0\n" +
"-125.0,44.0\n" +
"-125.0,44.0\n" +
"-125.0,44.0\n" +
"-125.0,44.0\n" +
"-125.0,44.0\n" +
"-125.0,44.0\n" +
"-125.0,44.0\n" +
"-125.0,44.0\n" +
"-125.2,44.0\n" +
"-125.2,44.0\n" +
"-125.2,44.0\n" +
"-125.2,44.0\n" +
"-125.2,44.0\n" +
"-125.2,44.0\n" +
"-125.2,44.0\n" +
"-125.2,44.0\n" +
"-125.2,44.0\n" +
"-125.4,44.0\n" +
"-125.4,44.0\n" +
"-125.4,44.0\n" +
"-125.4,44.0\n" +
"-125.4,44.0\n" +
"-125.4,44.0\n" +
"-125.4,44.0\n" +
"-125.4,44.0\n" +
"-125.6,43.8\n" +
"-125.6,43.8\n" +
"-125.6,43.8\n" +
"-125.6,43.8\n" +
"-125.6,43.8\n" +
"-125.6,43.8\n" +
"-125.6,43.8\n" +
"-125.6,43.8\n" +
"-125.86,43.5\n" +
"-125.86,43.5\n" +
"-125.86,43.5\n" +
"-125.86,43.5\n" +
"-125.86,43.5\n" +
"-125.86,43.5\n" +
"-125.86,43.5\n" +
"-125.86,43.5\n" +
"-125.86,43.5\n" +
"-125.86,43.5\n" +
"-125.63,43.5\n" +
"-125.63,43.5\n" +
"-125.63,43.5\n" +
"-125.63,43.5\n" +
"-125.63,43.5\n" +
"-125.63,43.5\n" +
"-125.63,43.5\n" +
"-125.63,43.5\n" +
"-125.63,43.5\n" +
"-125.63,43.5\n" +
"-125.33,43.5\n" +
"-125.33,43.5\n" +
"-125.33,43.5\n" +
"-125.33,43.5\n" +
"-125.33,43.5\n" +
"-125.33,43.5\n" +
"-125.33,43.5\n" +
"-125.33,43.5\n" +
"-125.33,43.5\n" +
"-125.33,43.5\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);  


        try {

            //test treat itself as a dataset
            EDDTable eddTable2 = new EDDTableFromDapSequence(
                "erddapGlobecBottle", //String tDatasetID, 
                null, null, null, null, null,
                1, //double tAltMetersPerSourceUnit, 
                new Object[][]{  //dataVariables: sourceName, addAttributes
                    {"longitude", null, null},
                    {"latitude", null, null},
                    {"altitude", null, null},
                    {"time", null, null},
                    {"ship", null, null},
                    {"cruise_id", null, null},
                    {"cast", null, null},
                    {"bottle_posn", null, null},
                    {"chl_a_total", null, null},
                    {"chl_a_10um", null, null},
                    {"phaeo_total", null, null},
                    {"phaeo_10um", null, null},
                    {"sal00", null, null},
                    {"sal11", null, null}, 
                    {"temperature0", null, null},  
                    {"temperature1", null, null}, 
                    {"fluor_v", null, null},
                    {"xmiss_v", null, null},
                    {"PO4", null, null},
                    {"N_N", null, null},
                    {"NO3", null, null},
                    {"Si", null, null},
                    {"NO2", null, null},
                    {"NH4", null, null},
                    {"oxygen", null, null},
                    {"par", null, null}},
                60, //int tReloadEveryNMinutes,
                EDStatic.erddapUrl + //in tests, always use non-https url
                    "/tabledap/testGlobecBottle", //sourceUrl);
                "s", null, //outerSequenceName innerSequenceName
                true, //NeedsExpandedFP_EQ
                true, //sourceCanConstrainStringEQNE
                true, //sourceCanConstrainStringGTLT
                PrimitiveArray.REGEX_OP,
                false); 

            //.xhtml from local dataset made from Erddap
            tName = eddTable2.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
                eddTable2.className() + "_Itself", ".xhtml"); 
            //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
"  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
"<head>\n" +
"  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
"  <title>EDDTableFromDapSequence_Itself</title>\n" +
"</head>\n" +
"<body style=\"color:black; background:white; font-family:Arial,Helvetica,sans-serif; font-size:85%; line-height:130%;\">\n" +
"\n" +
"&nbsp;\n" +
"<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>longitude</th>\n" +
"<th>NO3</th>\n" +
"<th>time</th>\n" +
"<th>ship</th>\n" +
"</tr>\n" +
"<tr>\n" +
"<th>degrees_east</th>\n" +
"<th>micromoles L-1</th>\n" +
"<th>UTC</th>\n" +
"<th></th>\n" +
"</tr>\n" +
"<tr>\n" +
"<td nowrap=\"nowrap\" align=\"right\">-124.4</td>\n" +
"<td align=\"right\">35.7</td>\n" +
"<td nowrap=\"nowrap\">2002-08-03T01:29:00Z</td>\n" +
"<td nowrap=\"nowrap\">New_Horizon</td>\n" +
"</tr>\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError creating a dataset from the dataset at " + 
                EDStatic.erddapUrl + //in tests, always use non-https url                
                "\nPress ^C to stop or Enter to continue..."); 
        }
        // */

    } //end of testBasic

    /**
     * Test saveAsKml.
     */
    public static void testKml() throws Throwable {
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&altitude>-5&time>=2002-08-03";

        String2.log("\n****************** EDDTableFromDapSequence.testKml\n");
        EDDTable globecBottle = (EDDTableFromDapSequence)oneFromDatasetXml("testGlobecBottle"); //should work

        //kml
        tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
            EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapKml", ".kml"); 
        //String2.log(String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1]);
        SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    }


    /**
     * The basic graphics tests of this class (testGlobecBottle).
     */
    public static void testGraphics(boolean doAll) throws Throwable {
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&altitude>-5&time>=2002-08-03";
        userDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-03";

        String2.log("\n****************** EDDTableFromDapSequence.testGraphics\n");
        EDDTable globecBottle = (EDDTableFromDapSequence)oneFromDatasetXml("testGlobecBottle"); //should work

            //kml
            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapKml", ".kml"); 
            //String2.log(String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1]);
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

        if (doAll) {

            //*** test make graphs
            //there is no .transparentPng for EDDTable

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery + "&.size=128|256&.font=.75", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphTiny", 
                ".largePng"); //to show it is irrelevant
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphS", ".smallPng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphM", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphL", ".largePng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery + "&.size=1700|1800", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphHuge", 
                ".smallPng"); //to show it is irrelevant
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphPdfSmall", ".smallPdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphPdf", ".pdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphPdfLarge", ".largePdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);


            //*** test make MAP
            String2.log("\n******************* EDDTableFromDapSequence.test make MAP\n");
            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapS", ".smallPng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapM", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapL", ".largePng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapS", ".smallPdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapM", ".pdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapL", ".largePdf"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
  

            //kml
            tName = globecBottle.makeNewFileForDapQuery(null, null, mapDapQuery, 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapKml", ".kml"); 
            //String2.log(String2.readFromFile(EDStatic.fullTestCacheDirectory + tName)[1]);
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);


            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery + "&.legend=Off&.trim=10", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphMLegendOff", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery + "&.legend=Only", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphSLegendOnly", ".smallPng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery + "&.legend=Only", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphMLegendOnly", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery + "&.legend=Only", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphLLegendOnly", ".largePng"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);


        }


         
        //test of .graphics commands
        if (true) {
            tQuery = "NO3,NH4&altitude>-5&time>=2002-08-03&NO3>=0";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                //I specify colorBar, but it isn't used
                "&.draw=markers&.marker=1|5&.color=0x0000FF&.colorBar=Rainbow|C|Linear", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphWithMarkersNoColorBar", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "NO3,NH4,sal00&altitude>-5&time>=2002-08-03&NO3>=0";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=lines&.marker=9|7", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphWithLines", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "NO3,NH4,sal00&altitude>-5&time>=2002-08-03&NO3>=0";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=linesAndMarkers&.marker=9|7&.colorBar=Rainbow|C|Linear", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphWithLinesAndMarkers", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "NO3,NH4,sal00&altitude>-5&time>=2002-08-03&NO3>=0";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                //color and colorBar aren't specified; default is used
                "&.draw=markers&.marker=9|7", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphWithMarkers", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "longitude,latitude,sal00&altitude>-5&time>=2002-08-03";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=lines&.color=0xFF8800", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapWithLines", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "longitude,latitude,sal00&altitude>-5&time>=2002-08-03";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=linesAndMarkers&.marker=5|5&.colorBar=Rainbow|D|Linear", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapWithLinesAndMarkers", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "longitude,latitude,sal00&altitude>-5&time>=2002-08-03";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=markers&.marker=5|5&.colorBar=Rainbow|D|Linear", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapWithMarkers", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "time,sal00,sal11&altitude>-5&time>=2002-08-03&NO3>=0";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=sticks&.color=0xFF8800", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_GraphWithSticks", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "longitude,latitude,sal00,sal11&altitude>-5&time>=2002-08-03";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=vectors&.color=0xFF0088&.vec=30", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapWithVectors", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tQuery = "longitude,latitude,sal00,sal11&altitude>-5&time>=2002-08-03&cast>200";
            tName = globecBottle.makeNewFileForDapQuery(null, null, tQuery + 
                "&.draw=vectors&.color=0xFF0088&.vec=30", 
                EDStatic.fullTestCacheDirectory, globecBottle.className() + "_MapWithVectorsNoData", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        }

    }

    public static void testNetcdf() throws Throwable {

        //use testGlobecBottle which has fixed altitude=0, not erdGlobecBottle
        EDDTable globecBottle = (EDDTableFromDapSequence)oneFromDatasetXml("testGlobecBottle"); //should work
        String tUrl = EDStatic.erddapUrl + //in tests, always use non-https url
            "/tabledap/" + globecBottle.datasetID;
        String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&altitude>-5&time>=2002-08-03";
        String results, expected;

        //TEST READING AS OPENDAP SERVER -- how specify constraint expression?
        //test reading via netcdf-java    similar to .dods test
        //tName = globecBottle.makeNewFileForDapQuery(null, null, userDapQuery, 
        //    EDStatic.fullTestCacheDirectory, globecBottle.className() + "_Data", ".dods"); 
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        try {
            String2.log("\n*** do netcdf-java opendap test");
            //!!!THIS READS DATA FROM ERDDAP SERVER RUNNING ON EDStatic.erddapUrl!!! //in tests, always use non-https url                
            //!!!THIS IS NOT JUST A LOCAL TEST!!!
            NetcdfFile nc = NetcdfDataset.openFile(tUrl, null);
            try {
                results = nc.toString();
                expected = 
"netcdf " + String2.replaceAll(EDStatic.erddapUrl, "http:" , "dods:") + //in tests, always use non-https url
        "/tabledap/testGlobecBottle {\n" +
" variables:\n" +
"\n" + //2009-02-26 this line was added with switch to netcdf-java 4.0
"   Structure {\n" +
"     float longitude;\n" +
"       :_CoordinateAxisType = \"Lon\";\n" +
"       :actual_range = -126.2f, -124.1f; // float\n" +
"       :axis = \"X\";\n" +
"       :colorBarMaximum = -115.0; // double\n" +
"       :colorBarMinimum = -135.0; // double\n" +
"       :ioos_category = \"Location\";\n" +
"       :long_name = \"Longitude\";\n" +
"       :standard_name = \"longitude\";\n" +
"       :units = \"degrees_east\";\n" +
"     float latitude;\n" +
"       :_CoordinateAxisType = \"Lat\";\n" +
"       :actual_range = 41.9f, 44.65f; // float\n" +
"       :axis = \"Y\";\n" +
"       :colorBarMaximum = 55.0; // double\n" +
"       :colorBarMinimum = 30.0; // double\n" +
"       :ioos_category = \"Location\";\n" +
"       :long_name = \"Latitude\";\n" +
"       :standard_name = \"latitude\";\n" +
"       :units = \"degrees_north\";\n" +
"     int altitude;\n" +
"       :_CoordinateAxisType = \"Height\";\n" +
"       :_CoordinateZisPositive = \"up\";\n" +
"       :actual_range = 0, 0; // int\n" +
"       :axis = \"Z\";\n" +
"       :ioos_category = \"Location\";\n" +
"       :long_name = \"Altitude\";\n" +
"       :positive = \"up\";\n" +
"       :standard_name = \"altitude\";\n" +
"       :units = \"m\";\n" +
"     double time;\n" +
"       :_CoordinateAxisType = \"Time\";\n";
                //odd: the spaces don't show up in console window
                Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

                expected = 
" :time_coverage_start = \"2002-05-30T03:21:00Z\";\n" +
" :title = \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
" :Westernmost_Easting = -126.2; // double\n" +
"}\n";
                Test.ensureEqual(results.substring(results.indexOf(" :time_coverage_start")), expected, "RESULTS=\n" + results);

                Attributes attributes = new Attributes();
                NcHelper.getGlobalAttributes(nc, attributes);
                Test.ensureEqual(attributes.getString("title"), "GLOBEC NEP Rosette Bottle Data (2002)", "");

                //get attributes for a dimension 
                Variable ncLat = nc.findVariable("s.latitude");
                attributes.clear();
                NcHelper.getVariableAttributes(ncLat, attributes);
                Test.ensureEqual(attributes.getString("units"), "degrees_north", "");

                //get attributes for grid variable
                Variable ncChl = nc.findVariable("s.chl_a_total");
                attributes.clear();
                NcHelper.getVariableAttributes(ncChl, attributes);
                Test.ensureEqual(attributes.getString("standard_name"),"concentration_of_chlorophyll_in_sea_water", "");

                //get sequence data
                //it's awkward. Do later if needed.
                //???How specify constraint expression?

            } finally {
                nc.close();
            }

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError accessing " + EDStatic.erddapUrl + //in tests, always use non-https url
                " via netcdf-java." +
                "\nPress ^C to stop or Enter to continue..."); 
        }

        //OTHER APPROACH: GET .NC FILE  -- HOW SPECIFY CONSTRAINT EXPRESSION???
        //SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        if (false) {
            try {
                String2.log("\n*** do netcdf-java .nc test");
                //!!!THIS READS DATA FROM ERDDAP SERVER RUNNING ON COASTWATCH CWEXPERIMENTAL!!!
                //!!!THIS IS NOT JUST A LOCAL TEST!!!
                NetcdfFile nc = NetcdfDataset.openFile(tUrl + ".nc?" + mapDapQuery, null);
                try {
                    results = nc.toString();
                    expected = "zz";
                    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

                } finally {
                    nc.close();
                }

            } catch (Throwable t) {
                String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                    "\nError accessing " + EDStatic.erddapUrl + //in tests, always use non-https url
                " via netcdf-java." +
                    "\nPress ^C to stop or Enter to continue..."); 
            }
        }       
    }

    /**
     * Test dapper (pmelTao).
     */
    public static void testDapper(boolean doGraphicsTests) throws Throwable {
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        int epo;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        try {

        //I note that if you test for a station at a specific lon,lat location
        //  (as returned by dapper), e.g., &longitude=180.124&latitude=8.003,
        //  you get no data.
        //You have to look for a range of lon and lat
        //!!! NOTE: the lon and lat results (even for a historical datum) change slightly every week or so.
        //  Apparently, they always serve the latest lon lat for historical data!
        EDDTable dapper = (EDDTableFromDapSequence)oneFromDatasetXml("pmelTao"); //should work
        String baseName = dapper.className() + "dapper";
        String dapperDapQuery = "longitude,latitude,altitude,time,sea_surface_temperature,air_temperature,\n" +
            "wind_to_direction,wind_speed\n" +
            "&longitude>164&longitude<200&latitude>-9&latitude<0" +
            "&time>=2007-08-03T00:00:00&time<=2007-08-05T00:00:00";
        String2.log("breadcrumb1");

        //min max
        edv = dapper.findDataVariableByDestinationName("longitude");
        Test.ensureEqual(edv.destinationMin(), 0, "");
        Test.ensureEqual(edv.destinationMax(), 360, "");
        edv = dapper.findDataVariableByDestinationName("latitude");
        Test.ensureEqual(edv.destinationMin(), -21, "");
        Test.ensureEqual(edv.destinationMax(), 15, "");
        edv = dapper.findDataVariableByDestinationName("altitude");
        Test.ensureEqual(edv.destinationMin(), 0, "");
        Test.ensureEqual(edv.destinationMax(), 0, "");
        edv = dapper.findDataVariableByDestinationName("time");
        Test.ensureEqual(edv.destinationMin(), 2.8404E8, "");
        Test.ensureEqual(edv.destinationMax(), Double.NaN, "");

        //test sliderCsvValues
        //longitude
        edv = dapper.findDataVariableByDestinationName("longitude");
        results = edv.sliderCsvValues();
        expected = 
"0, 2, 4, 6, 8, 10, 12, 14, 16, 18,";  
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
        expected = 
"352, 354, 356, 358, 360";  
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "results=\n" + results);
        Test.ensureEqual(edv.sliderNCsvValues(), 181, "");

        //latitude
        edv = dapper.findDataVariableByDestinationName("latitude");
        results = edv.sliderCsvValues();
        expected = 
"-21, -20.8, -20.6, -20.4, -20.2,";  
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
        expected = 
"14, 14.2, 14.4, 14.6, 14.8, 15"; 
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "results=\n" + results);
        Test.ensureEqual(edv.sliderNCsvValues(), 181, "");

        //altitude
        edv = dapper.findDataVariableByDestinationName("altitude");
        results = edv.sliderCsvValues();
        expected = "0"; //0
        Test.ensureEqual(results, expected, "results=\n" + results);
        Test.ensureEqual(edv.sliderNCsvValues(), 1, "");

        //time
        edv = dapper.findDataVariableByDestinationName("time");
        results = edv.sliderCsvValues();
        expected = 
"\"1979-01-01T12:00:00Z\", \"1979-03-01\", \"1979-05-01\", \"1979-07-01\",";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
        //last date is beginning of tomorrow
        GregorianCalendar gc = Calendar2.newGCalendarZulu();
        Calendar2.clearSmallerFields(gc, Calendar2.DATE);
        gc.add(Calendar2.DATE, 1);
        expected = ", \"" + Calendar2.formatAsISODate(gc) + "\"";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "results=\n" + results);
        Test.ensureEqual(edv.sliderNCsvValues(), 197, "");  //changes every 2 months

        //.das     das isn't affected by userDapQuery
        String2.log("breadcrumb2");
        tName = dapper.makeNewFileForDapQuery(null, null, dapperDapQuery, EDStatic.fullTestCacheDirectory, baseName, ".das"); 
        results = String2.annotatedString(new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray()));
        //String2.log(results);
        expected = 
"Attributes {[10]\n" +
" s {[10]\n" +
"  station_id {[10]\n" +
"    Int32 actual_range 1, 105;[10]\n" +
"    String cf_role \"timeseries_id\";[10]\n" +
"    Float64 colorBarMaximum 120.0;[10]\n" +
"    Float64 colorBarMinimum 0.0;[10]\n" +
"    String ioos_category \"Identifier\";[10]\n" +
"    String long_name \"Station ID\";[10]\n" +
"    Int32 missing_value 2147483647;[10]\n" +
"  }[10]\n" +
"  longitude {[10]\n" +
"    String _CoordinateAxisType \"Lon\";[10]\n" +
"    Float32 actual_range 0.0, 360.0;[10]\n" +
"    String axis \"X\";[10]\n" +
"    Float64 colorBarMaximum 360.0;[10]\n" +
"    Float64 colorBarMinimum 0.0;[10]\n" +
"    String ioos_category \"Location\";[10]\n" +
"    String long_name \"Longitude\";[10]\n" +
"    Float32 missing_value NaN;[10]\n" +
"    String standard_name \"longitude\";[10]\n" +
"    String units \"degrees_east\";[10]\n" +
"  }[10]\n" +
"  latitude {[10]\n" +
"    String _CoordinateAxisType \"Lat\";[10]\n" +
"    Float32 actual_range -21.0, 15.0;[10]\n" +
"    String axis \"Y\";[10]\n" +
"    Float64 colorBarMaximum 20.0;[10]\n" +
"    Float64 colorBarMinimum -20.0;[10]\n" +
"    String ioos_category \"Location\";[10]\n" +
"    String long_name \"Latitude\";[10]\n" +
"    Float32 missing_value NaN;[10]\n" +
"    String standard_name \"latitude\";[10]\n" +
"    String units \"degrees_north\";[10]\n" +
"  }[10]\n" +
"  altitude {[10]\n" +
"    String _CoordinateAxisType \"Height\";[10]\n" +
"    String _CoordinateZisPositive \"up\";[10]\n" +
"    Float32 actual_range 0.0, 0.0;[10]\n" +
"    String axis \"Z\";[10]\n" +
"    String ioos_category \"Location\";[10]\n" +
"    String long_name \"Altitude\";[10]\n" +
"    Float32 missing_value NaN;[10]\n" +
"    String positive \"up\";[10]\n" +
"    String standard_name \"altitude\";[10]\n" +
"    String units \"m\";[10]\n" +
"  }[10]\n" +
"  time {[10]\n" +
"    String _CoordinateAxisType \"Time\";[10]\n" +
"    Float64 actual_range 2.8404e+8, NaN;[10]\n" +  
"    String axis \"T\";[10]\n" +
"    String ioos_category \"Time\";[10]\n" +
"    String long_name \"Time\";[10]\n" +
"    Float64 missing_value NaN;[10]\n" +
//"    String source_sequence \"inner\";[10]\n" +
"    String standard_name \"time\";[10]\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";[10]\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";[10]\n" +
"  }[10]\n" +
"  sea_surface_temperature {[10]\n" +
"    Float64 colorBarMaximum 32.0;[10]\n" +
"    Float64 colorBarMinimum 0.0;[10]\n" +
"    String ioos_category \"Temperature\";[10]\n" +
"    String long_name \"Sea Surface Temperature\";[10]\n" +
"    Float32 missing_value NaN;[10]\n" +
//"    String source_sequence \"inner\";[10]\n" +
"    String standard_name \"sea_surface_temperature\";[10]\n" +
"    String units \"degree_C\";[10]\n" +
"  }[10]\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = 
"  northward_wind {[10]\n" +
"    Float64 colorBarMaximum 15.0;[10]\n" +
"    Float64 colorBarMinimum -15.0;[10]\n" +
"    String ioos_category \"Wind\";[10]\n" +
"    String long_name \"Northward Wind\";[10]\n" +
"    Float32 missing_value NaN;[10]\n" +
//"    String source_sequence \"inner\";[10]\n" +
"    String standard_name \"northward_wind\";[10]\n" +
"    String units \"m s-1\";[10]\n" +
"  }[10]\n" +
" }[10]\n" +
"  NC_GLOBAL {[10]\n" +
"    String cdm_data_type \"TimeSeries\";[10]\n" +
"    String cdm_timeseries_variables \"station_id, longitude, latitude, altitude\";[10]\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";[10]\n" +
"    Float64 Easternmost_Easting 360.0;[10]\n" +
"    Float64 geospatial_lat_max 15.0;[10]\n" +
"    Float64 geospatial_lat_min -21.0;[10]\n" +
"    String geospatial_lat_units \"degrees_north\";[10]\n" +
"    Float64 geospatial_lon_max 360.0;[10]\n" +
"    Float64 geospatial_lon_min 0.0;[10]\n" +
"    String geospatial_lon_units \"degrees_east\";[10]\n" +
"    Float64 geospatial_vertical_max 0.0;[10]\n" +
"    Float64 geospatial_vertical_min 0.0;[10]\n" +
"    String geospatial_vertical_positive \"up\";[10]\n" +
"    String geospatial_vertical_units \"m\";[10]\n" +
"    String history \"" + today + " http://dapper.pmel.noaa.gov/dapper/epic/tao_time_series.cdp[10]\n" +
today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
                "/tabledap/pmelTao.das\";[10]\n" +  //note no '?' after .das
"    String infoUrl \"http://www.pmel.noaa.gov/tao/\";[10]\n" +
"    String institution \"NOAA PMEL\";[10]\n" +
"    String license \"The data may be used and redistributed for free but is not intended[10]\n" +
"for legal use, since it may contain inaccuracies. Neither the data[10]\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any[10]\n" +
"of their employees or contractors, makes any warranty, express or[10]\n" +
"implied, including warranties of merchantability and fitness for a[10]\n" +
"particular purpose, or assumes any legal liability for the accuracy,[10]\n" +
"completeness, or usefulness, of this information.\";[10]\n" +
"    Int32 max_profiles_per_request 15000;[10]\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";[10]\n" +
"    Float64 Northernmost_Northing 15.0;[10]\n" +
"    String sourceUrl \"http://dapper.pmel.noaa.gov/dapper/epic/tao_time_series.cdp\";[10]\n" +
"    Float64 Southernmost_Northing -21.0;[10]\n" +
"    String standard_name_vocabulary \"CF-12\";[10]\n" +
"    String subsetVariables \"station_id, longitude, latitude, altitude\";[10]\n" +
"    String summary \"The TAO/TRITON array consists of approximately 100 moorings in the[10]\n" +
"Tropical Pacific Ocean, telemetering oceanographic and[10]\n" +
"meteorological data to shore in real-time via the Argos satellite[10]\n" +
"system.  The array is a major component of the El Ni[241]o/Southern[10]\n" +
"Oscillation (ENSO) Observing System, the Global Climate Observing[10]\n" +
"System (GCOS) and the Global Ocean Observing System (GOOS).[10]\n" +
"Support is provided primarily by the United States (National[10]\n" +
"Oceanic and Atmospheric Administration) and Japan (Japan Agency[10]\n" +
"for Marine-earth Science and TEChnology) with additional[10]\n" +
"contributions from France (Institut de recherche pour le[10]\n" +
"developpement).\";[10]\n" +
"    String time_coverage_start \"1979-01-01T12:00:00Z\";[10]\n" +
"    String title \"TAO Array Data from the PMEL DAPPER Server\";[10]\n" +
"    Int32 total_profiles_in_dataset 107;[10]\n" +
"    String version \"1.1.0\";[10]\n" +
"    Float64 Westernmost_Easting 0.0;[10]\n" +
"  }[10]\n" +
"}[10]\n" +
"[end]";
        epo = results.indexOf("  northward_wind");
        Test.ensureEqual(results.substring(epo), expected, 
            "\nresults=\n" + results + "\nexpected=\n" + expected);

        //examples related to EDDTable examples 

//    public static String idExample          = "pmel_dapper/tao"; 
//    public static String variablesExample   = "longitude,latitude,time,sea_surface_temperature";
//    public static String constraintsExample = "&longitude>264&latitude>0&time>=2007-08-01&time<=2007-08-08";
//    public static String dataValueExample   = "longitude,latitude,time,sea_surface_temperature&longitude>180.1&longitude<180.2&latitude>8.0&latitude<8.1&time>1167609600";
//    public static String dataTimeExample    = "longitude,latitude,time,sea_surface_temperature&longitude>180.1&longitude<180.2&latitude>8.0&latitude<8.1&time>2007-01-01";
//    //future: better to have graphExample be a time series example
//    public static String graphExample       = "time,sea_surface_temperature&longitude>180.1&longitude<180.2&latitude>8.0&latitude<8.1&time>2007-01-01";
//    public static String mapExample         = "longitude,latitude,time,sea_surface_temperature&time>=2006-12-01T12&time<2007-01-01";  
 
//"longitude, latitude, time, sea_surface_temperature\n" +
//"degrees_east, degrees_north, UTC, degree_C\n" +
//"264.997, 4.95, 2007-08-01T12:00:00Z, 27.02\n" +
//"264.997, 4.95, 2007-08-02T12:00:00Z, 27.03\n" +
//"264.997, 4.95, 2007-08-03T12:00:00Z, 26.98\n" +
//"264.997, 4.95, 2007-08-04T12:00:00Z, 26.92\n" +
//"264.997, 4.95, 2007-08-05T12:00:00Z, 26.84\n" +
//"264.997, 4.95, 2007-08-06T12:00:00Z, 26.83\n" +
//"264.997, 4.95, 2007-08-07T12:00:00Z, 26.82\n" +
//was, "264.997, 4.95, , NaN\n" +   but now supressed
//"265.065, 8.065, 2007-08-01T12:00:00Z, 27.75\n" +
//"265.065, 8.065, 2007-08-02T12:00:00Z, 27.56\n" +
//"265.065, 8.065, 2007-08-03T12:00:00Z, 27.56\n" +
//"265.065, 8.065, 2007-08-04T12:00:00Z, 27.92\n" +
//"265.065, 8.065, 2007-08-05T12:00:00Z, 28.2\n" +
//"265.065, 8.065, 2007-08-06T12:00:00Z, 27.71\n" +
//"265.065, 8.065, 2007-08-07T12:00:00Z, 27.83\n";

        //variablesExample + constraintsExample
        //because lon and lat change every week or so, I just test for pieces of info, not whole response
        String2.log("breadcrumb3, slow but should work...");
        tQuery = EDStatic.EDDTableVariablesExample + EDStatic.EDDTableConstraintsExample;
        tName = dapper.makeNewFileForDapQuery(null, null, tQuery, 
            EDStatic.fullTestCacheDirectory, baseName+"std1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        Test.ensureTrue(results.indexOf("longitude, latitude, time, sea_surface_temperature\n") >= 0, "\nresults=\n" + results);
        Test.ensureTrue(results.indexOf("degrees_east, degrees_north, UTC, degree_C\n") >= 0, "\nresults=\n" + results);
        Test.ensureTrue(results.indexOf(", 2007-08-01T12:00:00Z, 27.02\n") >= 0, "\nresults=\n" + results);
        Test.ensureTrue(results.indexOf(" , NaN\n") < 0, "\nresults=\n" + results); //skipDapperSpacerRows
        Test.ensureTrue(results.indexOf(", 2007-08-07T12:00:00Z, 24.51\n") >= 0, "\nresults=\n" + results);
        Test.ensureTrue(results.indexOf("") >= 0, "\nresults=\n" + results);
        
        //small variation to see if constraints can include !=NaN .
        //This works because data var constraints are handled by EDDTable.
        //  If handled by source, maybe it wouldn't work.
        //  ???So, always test all constraints in standardizeResultsTable???
        //  ???And change sourceCanConstrain to true/false, e.g., it wants to see it or not.???
        String2.log("\nbreadcrumb4, slow but should work...");
        tQuery = EDStatic.EDDTableVariablesExample + EDStatic.EDDTableConstraintsExample + "&sea_surface_temperature!=NaN";
        tName = dapper.makeNewFileForDapQuery(null, null, tQuery, 
            EDStatic.fullTestCacheDirectory, baseName+"std1+NaN", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        Test.ensureTrue(results.indexOf("longitude, latitude, time, sea_surface_temperature\n") >= 0, "\nresults=\n" + results);
        Test.ensureTrue(results.indexOf("degrees_east, degrees_north, UTC, degree_C\n") >= 0, "\nresults=\n" + results);
        Test.ensureTrue(results.indexOf(", 2007-08-01T12:00:00Z, 27.02\n") >= 0, "\nresults=\n" + results);
        Test.ensureTrue(results.indexOf(" , NaN\n") < 0, "\nresults=\n" + results);  //not present
        Test.ensureTrue(results.indexOf(", 2007-08-07T12:00:00Z, 24.51\n") >= 0, "\nresults=\n" + results);
        Test.ensureTrue(results.indexOf("") >= 0, "\nresults=\n" + results);

        //missing values in .nc files  (source has NaN; destination has NaN)
        String2.log("breadcrumb5");
        String lasQuery = "longitude,latitude,altitude,time,sea_surface_temperature,air_temperature" +
            "&latitude>=4.9&latitude<=5.0&time>=2007-06-01&time<2007-12-01&longitude>=264.9&longitude<=265.1";
        tName = dapper.makeNewFileForDapQuery(null, null, lasQuery, EDStatic.fullTestCacheDirectory, baseName+"NcNaN", ".ncHeader"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"double time(row=183);\n" +
"     :_CoordinateAxisType = \"Time\";\n" +
"     :actual_range = 1.1806992E9, 1.196424E9; // double\n" +
"     :axis = \"T\";\n" +
"     :ioos_category = \"Time\";\n" +
"     :long_name = \"Time\";\n" +
"     :missing_value = NaN; // double\n" +
//"     :source_sequence = \"inner\";\n" +
"     :standard_name = \"time\";\n" +
"     :time_origin = \"01-JAN-1970 00:00:00\";\n" +
"     :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"   float sea_surface_temperature(row=183);\n" +
"     :actual_range = 26.21f, 27.94f; // float\n" + //was 26.28
"     :colorBarMaximum = 32.0; // double\n" +
"     :colorBarMinimum = 0.0; // double\n" +
"     :ioos_category = \"Temperature\";\n" +
"     :long_name = \"Sea Surface Temperature\";\n" +
"     :missing_value = NaNf; // float\n" +
//"     :source_sequence = \"inner\";\n" +
"     :standard_name = \"sea_surface_temperature\";\n" +
"     :units = \"degree_C\";\n" +
"   float air_temperature(row=183);\n" +
"     :actual_range = 23.74f, 26.57f; // float\n" +
"     :colorBarMaximum = 40.0; // double\n" +
"     :colorBarMinimum = -10.0; // double\n" +
"     :ioos_category = \"Temperature\";\n" +
"     :long_name = \"Air Temperature\";\n" +
"     :missing_value = NaNf; // float\n" +
//"     :source_sequence = \"inner\";\n" +
"     :standard_name = \"air_temperature\";\n" +
"     :units = \"degree_C\";\n";
        int dtpo = results.indexOf("double time");
        Test.ensureEqual(results.substring(dtpo, dtpo + expected.length()), expected, "\nresults=\n" + results);

        //data for graphExample
        try {
            String2.log("breadcrumb6");
            tName = dapper.makeNewFileForDapQuery(null, null, EDStatic.EDDTableGraphExample, EDStatic.fullTestCacheDirectory, baseName+"graph", ".csv");
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"time, sea_surface_temperature\n" +
"UTC, degree_C\n" +
"2008-04-01T12:00:00Z, 27.09\n" +
"2008-04-02T12:00:00Z, 27.06\n" +
"2008-04-03T12:00:00Z, 26.96\n" +
"2008-04-04T12:00:00Z, 26.65\n" +
"2008-04-05T12:00:00Z, 26.38\n" +
"2008-04-06T12:00:00Z, 26.3\n" +
"2008-04-07T12:00:00Z, 26.37\n" +
"2008-04-08T12:00:00Z, 26.45\n" +
"2008-04-09T12:00:00Z, 26.5\n" +
"2008-04-10T12:00:00Z, 26.51\n" +
"2008-04-11T12:00:00Z, 26.38\n" +
"2008-04-12T12:00:00Z, 26.31\n" +
"2008-04-13T12:00:00Z, 26.36\n";
            results = results.substring(0, Math.min(results.length(), expected.length()));
            Test.ensureEqual(results, expected, "\nresults=\n" + results);  


        } catch (Throwable t) {
            String2.getStringFromSystemIn("Unexpected error: " + MustBe.throwableToString(t) + "\n" +
                "Press ^C to stop or Enter to continue..."); 
        } 


        String2.log("breadcrumb7");
        if (doGraphicsTests) {

            //mapExample
            tName = dapper.makeNewFileForDapQuery(null, null, EDStatic.EDDTableMapExample, EDStatic.fullTestCacheDirectory, baseName+"map", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            //graph
            tName = dapper.makeNewFileForDapQuery(null, null, EDStatic.EDDTableGraphExample, EDStatic.fullTestCacheDirectory, baseName+"graph", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            //kmlExample
            tName = dapper.makeNewFileForDapQuery(null, null, EDStatic.EDDTableMapExample, EDStatic.fullTestCacheDirectory, baseName+"map", ".kml"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            String2.log("\nresults=\n");
            String2.log(results);        
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        }

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected Dapper error. Press ^C to stop or Enter to continue..."); 
        }


    } //end of testDapper






    /**
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        testVerboseOn();

        //don't test local dataset because of dns/numericIP problems
        String tUrl = "http://dapper.pmel.noaa.gov/dapper/epic/tao_time_series.cdp";

String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDTableFromDapSequence\" datasetID=\"noaa_pmel_2688_61c6_5fbb\" active=\"true\">\n" +
"    <sourceUrl>http://dapper.pmel.noaa.gov/dapper/epic/tao_time_series.cdp</sourceUrl>\n" +
"    <outerSequenceName>location</outerSequenceName>\n" +
"    <innerSequenceName>time_series</innerSequenceName>\n" +
"    <skipDapperSpacerRows>true</skipDapperSpacerRows>\n" +
"    <sourceCanConstrainStringEQNE>false</sourceCanConstrainStringEQNE>\n" +
"    <sourceCanConstrainStringGTLT>false</sourceCanConstrainStringGTLT>\n" +
"    <sourceCanConstrainStringRegex></sourceCanConstrainStringRegex>\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <altitudeMetersPerSourceUnit>1</altitudeMetersPerSourceUnit>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"Conventions\">epic-insitu-1.0</att>\n" +
"        <att name=\"depth_range\" type=\"doubleList\">0.0 0.0</att>\n" +
"        <att name=\"lat_range\" type=\"doubleList\">";

//-22.7250003814697 20.4619998931885</att>\n" +
//"        <att name=\"lon_range\" type=\"doubleList\">66.943000793457 7.99200439453125</att>\n" +
//"        <att name=\"max_profiles_per_request\" type=\"int\">15000</att>\n" +
//"        <att name=\"time_range\" type=\"doubleList\">2.8404E11 1.2658896E12</att>\n" +
//"        <att name=\"total_profiles_in_dataset\" type=\"int\">107</att>\n" +

String expected2 = 
"        <att name=\"version\">1.1.0</att>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Point</att>\n" +
"        <att name=\"Conventions\">epic-insitu-1.0, COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"infoUrl\">http://dapper.pmel.noaa.gov/dapper/epic/tao_time_series.cdp.das</att>\n" +
"        <att name=\"institution\">NOAA PMEL</att>\n" +
"        <att name=\"keywords\">\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Air Temperature,\n" +
"Atmosphere &gt; Atmospheric Temperature &gt; Surface Air Temperature,\n" +
"Atmosphere &gt; Atmospheric Water Vapor &gt; Humidity,\n" +
"Atmosphere &gt; Atmospheric Winds &gt; Surface Winds,\n" +
"air, air_temperature, atmosphere, atmospheric, depth, direction, eastward, eastward_wind, epic, from, humidity, identifier, meteorology, noaa, northward, northward_wind, percent, pmel, quality, relative, relative_humidity, sea, seawater, sequence, series, source, speed, surface, tao, temperature, time, time series, vapor, water, wind, wind_from_direction, wind_speed, winds</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"Metadata_Conventions\">epic-insitu-1.0, COARDS, CF-1.6, Unidata Dataset Discovery v1.0</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF-12</att>\n" +
"        <att name=\"subsetVariables\">longitude, depth, latitude, id</att>\n" +
"        <att name=\"summary\">NOAA PMEL data from http://dapper.pmel.noaa.gov/dapper/epic/tao_time_series.cdp.das .</att>\n" +
"        <att name=\"title\">Epic tao time series</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>lon</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"long_name\">LONGITUDE</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">degree_west</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>depth</sourceName>\n" +
"        <destinationName>depth</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"axis\">Z</att>\n" +
"            <att name=\"long_name\">depth</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"colorBarPalette\">OceanDepth</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"standard_name\">depth</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>lat</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"long_name\">LATITUDE</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">degree_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>_id</sourceName>\n" +
"        <destinationName>id</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">sequence id</att>\n" +
"            <att name=\"missing_value\" type=\"int\">2147483647</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WD_410</sourceName>\n" +
"        <destinationName>WD_410</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">wind direction</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">degrees</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"standard_name\">wind_from_direction</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>QRH_5910</sourceName>\n" +
"        <destinationName>QRH_5910</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">relative humidity quality</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">128.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>RH_910</sourceName>\n" +
"        <destinationName>RH_910</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">relative humidity (%)</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">%</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"            <att name=\"standard_name\">relative_humidity</att>\n" +
"            <att name=\"units\">percent</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>T_20</sourceName>\n" +
"        <destinationName>T_20</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">sea surface temperature (c)</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">c</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>QT_5020</sourceName>\n" +
"        <destinationName>QT_5020</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">temperature quality</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">128.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>SWD_6410</sourceName>\n" +
"        <destinationName>SWD_6410</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">wind direction source</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">360.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"standard_name\">wind_from_direction</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>SRH_6910</sourceName>\n" +
"        <destinationName>SRH_6910</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">relative humidity source</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">100.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">20.0</att>\n" +
"            <att name=\"ioos_category\">Meteorology</att>\n" +
"            <att name=\"standard_name\">relative_humidity</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>QAT_5021</sourceName>\n" +
"        <destinationName>QAT_5021</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">air temperature quality</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">128.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>SWS_6401</sourceName>\n" +
"        <destinationName>SWS_6401</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">wind speed source</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"standard_name\">wind_speed</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WS_401</sourceName>\n" +
"        <destinationName>WS_401</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">wind speed (m/s)</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"standard_name\">wind_speed</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"long_name\">time</att>\n" +
"            <att name=\"missing_value\" type=\"double\">NaN</att>\n" +
"            <att name=\"units\">msec since 1970-01-01 00:00:00 GMT</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WV_423</sourceName>\n" +
"        <destinationName>WV_423</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">wind v (m/s)</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"standard_name\">northward_wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>QWS_5401</sourceName>\n" +
"        <destinationName>QWS_5401</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">wind speed quality</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">128.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>QWD_5410</sourceName>\n" +
"        <destinationName>QWD_5410</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">wind direction quality</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">128.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Quality</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>AT_21</sourceName>\n" +
"        <destinationName>AT_21</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">air temperature (c)</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">c</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"standard_name\">air_temperature</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>WU_422</sourceName>\n" +
"        <destinationName>WU_422</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"long_name\">wind u (m/s)</att>\n" +
"            <att name=\"missing_value\" type=\"float\">NaN</att>\n" +
"            <att name=\"units\">m s-1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">15.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-15.0</att>\n" +
"            <att name=\"ioos_category\">Wind</att>\n" +
"            <att name=\"standard_name\">eastward_wind</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n";


        try {
            String results = generateDatasetsXml(tUrl, 1440, null);
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

            int po = results.indexOf(expected2.substring(0, 40));
            Test.ensureEqual(results.substring(po), expected2, "results=\n" + results);

            //GenerateDatasetsXml
            GenerateDatasetsXml.doIt(new String[]{"-verbose", 
                "EDDTableFromDapSequence",
                tUrl, "1440"},
                false); //doIt loop?
            String gdxResults = String2.getClipboardString();
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");


            //EDDGridFromDap should fail and try EDDTableFromDapSequence and generate same result
            results = EDDGridFromDap.generateDatasetsXml(true, tUrl, null, null, null, 1440, null);
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

            Test.ensureEqual(results.substring(po), expected2, "results=\n" + results);

            //ensure it is ready-to-use by making a dataset from it
            EDD edd = oneFromXmlFragment(results);
            Test.ensureEqual(edd.datasetID(), "noaa_pmel_2688_61c6_5fbb", "");
            Test.ensureEqual(edd.title(), "Epic tao time series", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "longitude, depth, latitude, id, WD_410, QRH_5910, RH_910, T_20, QT_5020, SWD_6410, " +
                "SRH_6910, QAT_5021, SWS_6401, WS_401, time, WV_423, QWS_5401, QWD_5410, AT_21, WU_422",
                "");


        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nStarting 2011-07, this is failing sometimes because dapper is abandoned.\n" +
                "\nError using generateDatasetsXml." +
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }


    
    public static void testOneTime() throws Throwable {
        testVerboseOn();
        String tName;

        if (true) {
            //get empiricalMinMax
            EDDTable tedd = (EDDTable)oneFromDatasetXml("pmelArgoAll"); 
            tedd.getEmpiricalMinMax(null, "2007-08-01", "2007-08-10", false, true);
            String tq = "longitude,latitude,id&time>=2008-06-17T16:04:12Z&time<=2008-06-24T16:04:12Z" +
                "&.draw=markers&.marker=5|5&.color=0x000000&.colorBar=|C|Linear|||";
            tName = tedd.makeNewFileForDapQuery(null, null, tq, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_GraphArgo", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        }
    
        if (false) {
            //get summary string
            EDDTable tedd = (EDDTable)oneFromDatasetXml("nwioosGroundfish"); 
            String2.log(String2.annotatedString(tedd.combinedGlobalAttributes().getString("summary")));
        }

        if (false) { 
            //graph colorbar range
            EDDTable tedd = (EDDTable)oneFromDatasetXml("pmelArgoAll"); 
            String tq = "longitude,latitude,temp_adjusted&time>=2008-06-27T00:00:00Z" +
                "&.draw=markers&.marker=5|5&.color=0x000000&.colorBar=|C|Linear|0|30|30";
            tName = tedd.makeNewFileForDapQuery(null, null, tq, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_GraphArgo30", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        }

    }

    /**
     * Try to isolate trouble with Argo.
     */
    public static void testArgo() throws Throwable {
        testVerboseOn();
        String sourceUrl = "http://dapper.pmel.noaa.gov/dapper/argo/argo_all.cdp";
        String2.log("\n*** testArgo " + sourceUrl);
        DConnect dConnect = new DConnect(sourceUrl, acceptDeflate, 1, 1);
        String2.log("getDAS");
        DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
        String2.log("getDDS");
        DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);

        EDDTable tedd = (EDDTable)oneFromDatasetXml("pmelArgoAll"); 
        String tq = "longitude,latitude,id&id<=1000000&.draw=markers&.marker=4|5&.color=0x000000&.colorBar=|C|Linear|||";
        String tName = tedd.makeNewFileForDapQuery(null, null, tq, EDStatic.fullTestCacheDirectory, 
            tedd.className() + "_Argo", ".png"); 
        SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        tName = tedd.makeNewFileForDapQuery(null, null, tq, EDStatic.fullTestCacheDirectory, 
            tedd.className() + "_Argo", ".csv"); 
        String results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
//        String expected = 
//"";
//        Test.ensureEqual(results, expected, "results=\n" + results);      
        
    }


    /**
     * Try to isolate trouble with psdac for Peter Piatko.
     * Trouble is with source time that has internal spaces -- 
     *    erddap needs to percentEncode the request.
     */
    public static void testPsdac() throws Throwable {
        testVerboseOn();
        try {
            String results, query, tName;
            String baseQuery = "time,longitude,latitude,altitude,station,waterTemperature,salinity" +
                "&latitude=36.692"; 
            EDDTable tedd = (EDDTable)oneFromDatasetXml("cimtPsdac");
            String expected = 
    "time,longitude,latitude,altitude,station,waterTemperature,salinity\n" +
    "UTC,degrees_east,degrees_north,m,,degrees_Celsius,Presumed Salinity Units\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-1.0,T402,12.8887,33.8966\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-2.0,T402,12.8272,33.8937\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-3.0,T402,12.8125,33.8898\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-4.0,T402,12.7125,33.8487\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-5.0,T402,12.4326,33.8241\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-6.0,T402,12.1666,33.8349\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-7.0,T402,11.9364,33.8159\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-8.0,T402,11.7206,33.8039\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-9.0,T402,11.511,33.8271\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-10.0,T402,11.4064,33.853\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-11.0,T402,11.3552,33.8502\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-12.0,T402,11.2519,33.8607\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-13.0,T402,11.1777,33.8655\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-14.0,T402,11.1381,33.8785\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-15.0,T402,11.0643,33.8768\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-16.0,T402,10.9416,33.8537\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-17.0,T402,10.809,33.8379\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-18.0,T402,10.7034,33.8593\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-19.0,T402,10.6502,33.8476\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-20.0,T402,10.5257,33.8174\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-21.0,T402,10.2857,33.831\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-22.0,T402,10.0717,33.8511\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-23.0,T402,9.9577,33.8557\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-24.0,T402,9.8876,33.8614\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-25.0,T402,9.842,33.8757\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-26.0,T402,9.7788,33.8904\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-27.0,T402,9.7224,33.8982\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-28.0,T402,9.695,33.9038\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-29.0,T402,9.6751,33.9013\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-30.0,T402,9.6462,33.9061\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-31.0,T402,9.6088,33.9069\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-32.0,T402,9.5447,33.9145\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-33.0,T402,9.4887,33.9263\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-34.0,T402,9.4514,33.9333\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-35.0,T402,9.4253,33.9358\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-36.0,T402,9.397,33.9387\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-37.0,T402,9.3795,33.9479\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-38.0,T402,9.3437,33.9475\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-39.0,T402,9.2946,33.9494\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-40.0,T402,9.2339,33.9458\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-41.0,T402,9.1812,33.9468\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-42.0,T402,9.153,33.9548\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-43.0,T402,9.1294,33.9615\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-44.0,T402,9.1048,33.9652\n" +
    "2002-06-25T14:55:00Z,-121.845,36.692,-45.0,T402,9.0566,33.9762\n";

            //the basicQuery
            try {
                tName = tedd.makeNewFileForDapQuery(null, null, baseQuery, EDStatic.fullTestCacheDirectory, 
                    tedd.className() + "_psdac", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                //String2.log(results);
                Test.ensureEqual(results, expected, "results=\n" + results);      
            } catch (Throwable t) {
                String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                    "\nUnexpected error for psdac numeric constraint." +
                    "\nPress ^C to stop or Enter to continue..."); 
            }

            //basicQuery + String= constraint that shouldn't change the results
            try {            
                tName = tedd.makeNewFileForDapQuery(null, null, baseQuery + "&station=\"T402\"", 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_psdacNonTime", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                Test.ensureEqual(results, expected, "results=\n" + results);      
            } catch (Throwable t) {
                String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                    "\nUnexpected error for psdac with non-time String= constraint." +
                    "\nPress ^C to stop or Enter to continue..."); 
            }
            
            //basicQuery + String> String< constraints that shouldn't change the results
            try {            
                tName = tedd.makeNewFileForDapQuery(null, null, baseQuery + "&station>\"T3\"&station<\"T5\"", 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_psdacGTLT", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                Test.ensureEqual(results, expected, "results=\n" + results);      
            } catch (Throwable t) {
                String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                    "\nUnexpected error for psdac with non-time String> String< constraints." +
                    "\nPress ^C to stop or Enter to continue..."); 
            }
           
            //REGEX: If dataset is setup with sourceCanConstraintStringRegex ~=, THIS WORKS SO SOURCE REGEX PARTLY WORKS 
            //basicQuery + String regex constraint (ERDDAP handles it) that shouldn't change the results
            //This succeeds with source not handling regex, so leave test active.
            try {              //always =~ (regardless of what source needs) because this is an erddap request
                tName = tedd.makeNewFileForDapQuery(null, null, baseQuery + "&station=~\"T40.\"", 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_psdacRegex", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                Test.ensureEqual(results, expected, "results=\n" + results);      
            } catch (Throwable t) {
                String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                    "\nUnexpected error for psdac with non-time String regex constraints." +
                    "\nPress ^C to stop or Enter to continue..."); 
            }

            //REGEX: If dataset is setup with sourceCanConstraintStringRegex ~=, THIS DOESN'T WORK.
            //SO SOURCE REGEX SUPPORT IS LIMITED, SO DON'T RELY ON SOURCE HANDLING REGEX
            //basicQuery + String regex constraint (ERDDAP handles it) that shouldn't change the results
            //This succeeds with source not handling regex, so leave test active.
            try {              //always =~ (regardless of what source needs) because this is an erddap request
                tName = tedd.makeNewFileForDapQuery(null, null, baseQuery + "&station=~\"(T402|t403)\"", 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_psdacRegex", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                Test.ensureEqual(results, expected, "results=\n" + results);      
            } catch (Throwable t) {
                String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                    "\nUnexpected error for psdac with non-time String regex constraints." +
                    "\nPress ^C to stop or Enter to continue..."); 
            }

            //basicQuery + time= (a string= test) constraint that shouldn't change the results
            try {
                tName = tedd.makeNewFileForDapQuery(null, null, baseQuery + "&time=2002-06-25T14:55:00Z", 
                    EDStatic.fullTestCacheDirectory, tedd.className() + "_psdacTime", ".csv"); 
                results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
                Test.ensureEqual(results, expected, "results=\n" + results);      
            } catch (Throwable t) {
                String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                    "\nUnexpected error for psdac with time String= constraint." +
                    "\nPress ^C to stop or Enter to continue..."); 
            }
        } catch (Throwable t2) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t2) + 
                "\nUnexpected error for psdac." +
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }

    /**
     * erdGlobecBird flight_dir uses scale_factor=10 add_offset=0, so a good test of scaleAddOffset.
     */
    public static void testGlobecBirds() throws Throwable {
        testVerboseOn();
        String results, query, tName, expected;
        String baseQuery = "&time>=2000-08-07&time<2000-08-08"; 
        EDDTable tedd = (EDDTable)oneFromDatasetXml("erdGlobecBirds");

        //the basicQuery
        try {
            //an easy query
            tName = tedd.makeNewFileForDapQuery(null, null, baseQuery, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_bird1", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"trans_no,trans_id,longitude,latitude,time,area,behav_code,flight_dir,head_c,number,number_adj,species,wspd\n" +
",,degrees_east,degrees_north,UTC,km2,,degrees_true,degrees_true,count,count,,knots\n" +
"22001,8388607,-125.023,43.053,2000-08-07T00:00:00Z,1.3,1,180,240,1,0.448,SHSO,15\n" +
"22001,8388607,-125.023,43.053,2000-08-07T00:00:00Z,1.3,2,0,240,1,1.0,FUNO,15\n" +
"22001,8388607,-125.023,43.053,2000-08-07T00:00:00Z,1.3,3,0,240,1,1.0,SKMA,15\n" +
"22005,8388607,-125.225,43.955,2000-08-07T00:00:00Z,1.3,2,0,240,3,3.0,AKCA,15\n" +
"22009,8388607,-125.467,43.84,2000-08-07T00:00:00Z,1.3,1,200,240,2,0.928,PHRE,20\n" +
"22013,8388607,-125.648,43.768,2000-08-07T00:00:00Z,1.3,1,270,240,1,1.104,STLE,20\n" +
"22015,8388607,-125.745,43.73,2000-08-07T00:00:00Z,1.3,1,180,240,4,1.616,PHRE,20\n" +
"22018,8388607,-125.922,43.668,2000-08-07T00:00:00Z,1.3,2,0,240,1,1.0,AKCA,20\n" +
"22019,8388607,-125.935,43.662,2000-08-07T00:00:00Z,1.3,1,270,340,1,0.601,STLE,25\n" +
"22020,8388607,-125.968,43.693,2000-08-07T00:00:00Z,1.6,1,40,340,1,0.67,STLE,25\n" +
"22022,8388607,-125.978,43.727,2000-08-07T00:00:00Z,1.3,1,50,150,1,0.469,STLE,25\n" +
"22023,8388607,-125.953,43.695,2000-08-07T00:00:00Z,1.3,2,0,150,1,1.0,PHRE,25\n" +
"22025,8388607,-125.903,43.628,2000-08-07T00:00:00Z,1.3,1,50,150,1,0.469,STLE,25\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
            //unscaled flight_dir values are 0..36 so see if >=40 is properly handled 
            tName = tedd.makeNewFileForDapQuery(null, null, baseQuery + "&flight_dir>=40", 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_bird2", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"trans_no,trans_id,longitude,latitude,time,area,behav_code,flight_dir,head_c,number,number_adj,species,wspd\n" +
",,degrees_east,degrees_north,UTC,km2,,degrees_true,degrees_true,count,count,,knots\n" +
"22001,8388607,-125.023,43.053,2000-08-07T00:00:00Z,1.3,1,180,240,1,0.448,SHSO,15\n" +
"22009,8388607,-125.467,43.84,2000-08-07T00:00:00Z,1.3,1,200,240,2,0.928,PHRE,20\n" +
"22013,8388607,-125.648,43.768,2000-08-07T00:00:00Z,1.3,1,270,240,1,1.104,STLE,20\n" +
"22015,8388607,-125.745,43.73,2000-08-07T00:00:00Z,1.3,1,180,240,4,1.616,PHRE,20\n" +
"22019,8388607,-125.935,43.662,2000-08-07T00:00:00Z,1.3,1,270,340,1,0.601,STLE,25\n" +
"22020,8388607,-125.968,43.693,2000-08-07T00:00:00Z,1.6,1,40,340,1,0.67,STLE,25\n" +
"22022,8388607,-125.978,43.727,2000-08-07T00:00:00Z,1.3,1,50,150,1,0.469,STLE,25\n" +
"22025,8388607,-125.903,43.628,2000-08-07T00:00:00Z,1.3,1,50,150,1,0.469,STLE,25\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
        
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error for erdGlobecBirds." +
                "\nPress ^C to stop or Enter to continue..."); 
        }

        try {        
            //try getting no data -- exception should be MustBe.THERE_IS_NO_DATA
            tName = tedd.makeNewFileForDapQuery(null, null, baseQuery + "&flight_dir>=4000", 
                EDStatic.fullTestCacheDirectory, tedd.className() + "_bird2", ".csv"); 
            throw new Exception("Shouldn't have gotten here.");      
        } catch (Throwable t) {
            //test that this is the expected exception
            if (t.toString().indexOf(MustBe.THERE_IS_NO_DATA) < 0)
                throw new RuntimeException( 
                    "Exception should have been MustBe.THERE_IS_NO_DATA=\"" + 
                    MustBe.THERE_IS_NO_DATA + "\":\n" +
                    MustBe.throwableToString(t));
        }

    }

    /**
     * The argo data uses time units of mscec since 1970-01-01, so it is a 
     * test of other time units.
     */
    public static void testArgoTime() throws Throwable {
        testVerboseOn();
        String results, query, tName, expected;
        String baseQuery = "&time>=2000-08-07T00&time<2000-08-07T00:34"; 
        EDDTable tedd = (EDDTable)oneFromDatasetXml("pmelArgoAll");

        //the basicQuery
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, baseQuery, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_argo1", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = //2009-07-21 changed: temp_adjusted_error data went from NaNs to values
/*
"longitude,latitude,time,id,cndc,cndc_qc,cndc_adjusted,cndc_adjusted_error,cndc_adjusted_qc,doxy,doxy_qc,doxy_adjusted,doxy_adjusted_error,doxy_adjusted_qc,pres,pres_qc,pres_adjusted,pres_adjusted_error,pres_adjusted_qc,psal,psal_qc,psal_adjusted,psal_adjusted_error,psal_adjusted_qc,temp,temp_qc,temp_adjusted,temp_adjusted_error,temp_adjusted_qc,temp_doxy,temp_doxy_qc,temp_doxy_adjusted,temp_doxy_adjusted_error,temp_doxy_adjusted_qc\n" +
"degrees_east,degrees_north,UTC,,mhos m-1,,mhos m-1,mhos m-1,,micromole kg-1,,micromole kg-1,micromole kg-1,,decibar,,decibar,decibar,,psu,,psu,psu,,degree_C,,degree_C,degree_C,,degree_C,,degree_C,degree_C,\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,4.4,49.0,5.1,NaN,49.0,NaN,32.0,NaN,NaN,32.0,11.392,49.0,11.392,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,9.6,49.0,10.3,NaN,49.0,NaN,32.0,NaN,NaN,32.0,9.166,49.0,9.166,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,14.8,49.0,15.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,8.239,49.0,8.239,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,19.9,49.0,20.6,NaN,49.0,NaN,32.0,NaN,NaN,32.0,7.369,49.0,7.369,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,25.1,49.0,25.8,NaN,49.0,NaN,32.0,NaN,NaN,32.0,5.819,49.0,5.819,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,30.3,49.0,31.0,NaN,49.0,NaN,32.0,NaN,NaN,32.0,4.591,49.0,4.591,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,35.4,49.0,36.1,NaN,49.0,NaN,32.0,NaN,NaN,32.0,4.347,49.0,4.347,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,40.6,49.0,41.3,NaN,49.0,NaN,32.0,NaN,NaN,32.0,3.394,49.0,3.394,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,45.8,49.0,46.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,3.292,49.0,3.292,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,50.9,49.0,51.6,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.927,49.0,2.927,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,56.1,49.0,56.8,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.887,49.0,2.887,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,61.3,49.0,62.0,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.924,49.0,2.924,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,66.4,49.0,67.1,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.574,49.0,2.574,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,71.6,49.0,72.3,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.721,49.0,2.721,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,76.8,49.0,77.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.682,49.0,2.682,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,81.9,49.0,82.6,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.664,49.0,2.664,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,87.1,49.0,87.8,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.736,49.0,2.736,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,92.3,49.0,93.0,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.83,49.0,2.83,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,97.4,49.0,98.1,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.64,49.0,2.64,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,102.6,49.0,103.3,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.296,49.0,2.296,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,107.8,49.0,108.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.915,49.0,1.915,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,112.9,49.0,113.6,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.243,49.0,2.243,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,118.1,49.0,118.8,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.806,49.0,1.806,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,123.3,49.0,124.0,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.763,49.0,1.763,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,128.5,49.0,129.2,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.826,49.0,1.826,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,133.6,49.0,134.3,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.901,49.0,1.901,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,138.8,49.0,139.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.826,49.0,1.826,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,144.0,49.0,144.7,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.803,49.0,1.803,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,149.1,49.0,149.8,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.798,49.0,1.798,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,154.3,49.0,155.0,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.701,49.0,1.701,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,159.5,49.0,160.2,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.644,49.0,1.644,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,164.6,49.0,165.3,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.61,49.0,1.61,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,169.8,49.0,170.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.576,49.0,1.576,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,175.0,49.0,175.7,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.573,49.0,1.573,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,180.1,49.0,180.8,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.534,49.0,1.534,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,185.3,49.0,186.0,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.528,49.0,1.528,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,190.5,49.0,191.2,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.539,49.0,1.539,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,195.6,49.0,196.3,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.556,49.0,1.556,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,200.8,49.0,201.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.573,49.0,1.573,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,206.0,49.0,206.7,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.576,49.0,1.576,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,211.1,49.0,211.8,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.517,49.0,1.517,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,216.3,49.0,217.0,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.444,49.0,1.444,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,221.5,49.0,222.2,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.449,49.0,1.449,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,226.6,49.0,227.3,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.469,49.0,1.469,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,231.8,49.0,232.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.57,49.0,1.57,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,237.0,49.0,237.7,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.667,49.0,1.667,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,242.1,49.0,242.8,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.706,49.0,1.706,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,247.3,49.0,248.0,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.752,49.0,1.752,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,252.5,49.0,253.2,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.778,49.0,1.778,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,257.6,49.0,258.3,NaN,49.0,NaN,32.0,NaN,NaN,32.0,1.864,49.0,1.864,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,262.8,49.0,263.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.106,49.0,2.106,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,273.1,49.0,273.8,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.179,49.0,2.179,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,283.5,49.0,284.2,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.176,49.0,2.176,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,293.8,49.0,294.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.252,49.0,2.252,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,304.2,49.0,304.9,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.393,49.0,2.393,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,314.5,49.0,315.2,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.417,49.0,2.417,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,324.8,49.0,325.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.387,49.0,2.387,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,335.2,49.0,335.9,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.438,49.0,2.438,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,345.5,49.0,346.2,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.476,49.0,2.476,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,355.8,49.0,356.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.37,49.0,2.37,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,366.2,49.0,366.9,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.252,49.0,2.252,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,376.5,49.0,377.2,NaN,49.0,NaN,32.0,NaN,NaN,32.0,2.691,49.0,2.691,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,386.8,49.0,387.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,3.156,49.0,3.156,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,397.2,49.0,397.9,NaN,49.0,NaN,32.0,NaN,NaN,32.0,3.425,49.0,3.425,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,417.8,49.0,418.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,3.488,49.0,3.488,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,438.5,49.0,439.2,NaN,49.0,NaN,32.0,NaN,NaN,32.0,3.488,49.0,3.488,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,459.2,49.0,459.9,NaN,49.0,NaN,32.0,NaN,NaN,32.0,3.51,49.0,3.51,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,479.8,49.0,480.5,NaN,49.0,NaN,32.0,NaN,NaN,32.0,3.535,49.0,3.535,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"143.408,41.405,2000-08-07T00:33:50Z,156470,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,500.5,49.0,501.2,NaN,49.0,NaN,32.0,NaN,NaN,32.0,3.554,49.0,3.554,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n";
*/
//2010-04-16  just the id changed (from 156470 to 308050)!  I emailed Joe Sirott
//2011-02-15 I reordered the variables
"id,longitude,latitude,time,pres,pres_qc,pres_adjusted,pres_adjusted_error,pres_adjusted_qc,cndc,cndc_qc,cndc_adjusted,cndc_adjusted_error,cndc_adjusted_qc,doxy,doxy_qc,doxy_adjusted,doxy_adjusted_error,doxy_adjusted_qc,psal,psal_qc,psal_adjusted,psal_adjusted_error,psal_adjusted_qc,temp,temp_qc,temp_adjusted,temp_adjusted_error,temp_adjusted_qc,temp_doxy,temp_doxy_qc,temp_doxy_adjusted,temp_doxy_adjusted_error,temp_doxy_adjusted_qc\n" +
",degrees_east,degrees_north,UTC,decibar,,decibar,decibar,,mhos m-1,,mhos m-1,mhos m-1,,micromole kg-1,,micromole kg-1,micromole kg-1,,psu,,psu,psu,,degree_C,,degree_C,degree_C,,degree_C,,degree_C,degree_C,\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,4.4,49.0,5.1,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,11.392,49.0,11.392,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,9.6,49.0,10.3,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,9.166,49.0,9.166,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,14.8,49.0,15.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,8.239,49.0,8.239,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,19.9,49.0,20.6,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,7.369,49.0,7.369,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,25.1,49.0,25.8,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,5.819,49.0,5.819,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,30.3,49.0,31.0,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,4.591,49.0,4.591,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,35.4,49.0,36.1,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,4.347,49.0,4.347,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,40.6,49.0,41.3,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,3.394,49.0,3.394,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,45.8,49.0,46.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,3.292,49.0,3.292,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,50.9,49.0,51.6,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.927,49.0,2.927,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,56.1,49.0,56.8,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.887,49.0,2.887,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,61.3,49.0,62.0,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.924,49.0,2.924,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,66.4,49.0,67.1,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.574,49.0,2.574,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,71.6,49.0,72.3,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.721,49.0,2.721,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,76.8,49.0,77.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.682,49.0,2.682,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,81.9,49.0,82.6,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.664,49.0,2.664,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,87.1,49.0,87.8,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.736,49.0,2.736,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,92.3,49.0,93.0,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.83,49.0,2.83,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,97.4,49.0,98.1,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.64,49.0,2.64,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,102.6,49.0,103.3,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.296,49.0,2.296,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,107.8,49.0,108.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.915,49.0,1.915,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,112.9,49.0,113.6,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.243,49.0,2.243,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,118.1,49.0,118.8,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.806,49.0,1.806,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,123.3,49.0,124.0,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.763,49.0,1.763,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,128.5,49.0,129.2,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.826,49.0,1.826,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,133.6,49.0,134.3,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.901,49.0,1.901,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,138.8,49.0,139.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.826,49.0,1.826,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,144.0,49.0,144.7,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.803,49.0,1.803,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,149.1,49.0,149.8,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.798,49.0,1.798,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,154.3,49.0,155.0,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.701,49.0,1.701,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,159.5,49.0,160.2,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.644,49.0,1.644,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,164.6,49.0,165.3,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.61,49.0,1.61,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,169.8,49.0,170.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.576,49.0,1.576,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,175.0,49.0,175.7,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.573,49.0,1.573,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,180.1,49.0,180.8,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.534,49.0,1.534,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,185.3,49.0,186.0,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.528,49.0,1.528,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,190.5,49.0,191.2,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.539,49.0,1.539,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,195.6,49.0,196.3,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.556,49.0,1.556,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,200.8,49.0,201.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.573,49.0,1.573,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,206.0,49.0,206.7,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.576,49.0,1.576,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,211.1,49.0,211.8,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.517,49.0,1.517,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,216.3,49.0,217.0,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.444,49.0,1.444,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,221.5,49.0,222.2,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.449,49.0,1.449,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,226.6,49.0,227.3,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.469,49.0,1.469,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,231.8,49.0,232.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.57,49.0,1.57,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,237.0,49.0,237.7,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.667,49.0,1.667,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,242.1,49.0,242.8,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.706,49.0,1.706,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,247.3,49.0,248.0,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.752,49.0,1.752,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,252.5,49.0,253.2,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.778,49.0,1.778,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,257.6,49.0,258.3,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,1.864,49.0,1.864,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,262.8,49.0,263.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.106,49.0,2.106,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,273.1,49.0,273.8,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.179,49.0,2.179,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,283.5,49.0,284.2,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.176,49.0,2.176,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,293.8,49.0,294.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.252,49.0,2.252,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,304.2,49.0,304.9,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.393,49.0,2.393,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,314.5,49.0,315.2,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.417,49.0,2.417,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,324.8,49.0,325.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.387,49.0,2.387,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,335.2,49.0,335.9,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.438,49.0,2.438,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,345.5,49.0,346.2,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.476,49.0,2.476,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,355.8,49.0,356.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.37,49.0,2.37,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,366.2,49.0,366.9,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.252,49.0,2.252,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,376.5,49.0,377.2,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,2.691,49.0,2.691,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,386.8,49.0,387.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,3.156,49.0,3.156,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,397.2,49.0,397.9,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,3.425,49.0,3.425,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,417.8,49.0,418.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,3.488,49.0,3.488,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,438.5,49.0,439.2,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,3.488,49.0,3.488,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,459.2,49.0,459.9,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,3.51,49.0,3.51,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,479.8,49.0,480.5,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,3.535,49.0,3.535,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n" +
"308050,143.408,41.405,2000-08-07T00:33:50Z,500.5,49.0,501.2,NaN,49.0,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,32.0,NaN,NaN,32.0,3.554,49.0,3.554,0.0020,49.0,NaN,NaN,NaN,NaN,NaN\n";

Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error for pmelArgoAll." +
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }

    /**
     */
    public static void testErdlasNewportCtd() throws Throwable {
        testVerboseOn();
        String results, query, tName, expected;
        String baseQuery = "&time>=2006-08-07T00&time<2006-08-08"; 
        EDDTable tedd = (EDDTable)oneFromDatasetXml("erdlasNewportCtd");

        //the basicQuery
        try {
            tName = tedd.makeNewFileForDapQuery(null, null, baseQuery, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_newport", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error for erdlasNewportCtd." +
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }

    /**
     */
    public static void testDapErdlasNewportCtd() throws Throwable {
        testVerboseOn();

        //the basicQuery
        for (int test = 1; test < 2; test++) {
            String url = test == 0? 
                "http://oceanwatch.pfeg.noaa.gov:8080/dods/GLOBEC/GLOBEC_birds?birds.year,birds.species,birds.head_c,birds.month_local,birds.day_local&birds.year=2000&birds.month_local=8&birds.day_local=7" :
                "http://las.pfeg.noaa.gov/cgi-bin/ERDserver/northwest.sql?northwest.temperature,northwest.ctd_station_code,northwest.datetime,northwest.station,northwest.longitude,northwest.latitude&northwest.datetime%3E13821";
            System.out.println("\ntesting url=" + url);
            DConnect dConnect = new DConnect(url, true);
            DataDDS dataDds = dConnect.getData(null); //null = no statusUI

            //*** read the data (row-by-row, as it wants)
            DSequence outerSequence = (DSequence)dataDds.getVariables().nextElement();
            int nOuterRows = outerSequence.getRowCount();
            System.out.println("nRows=" + nOuterRows);
            for (int outerRow = 0; outerRow < Math.min(5, nOuterRows); outerRow++) {
                java.util.Vector outerVector = outerSequence.getRow(outerRow);
                StringBuilder sb = new StringBuilder();

                //process the other outerCol
                for (int outerCol = 0; outerCol < outerVector.size(); outerCol++) {
                    if (outerCol > 0)
                        sb.append(", ");
                    BaseType obt = (BaseType)outerVector.get(outerCol);
                    if      (obt instanceof DByte)     sb.append(((DByte)obt).getValue());
                    else if (obt instanceof DFloat32)  sb.append(((DFloat32)obt).getValue());
                    else if (obt instanceof DFloat64)  sb.append(((DFloat64)obt).getValue());
                    else if (obt instanceof DInt16)    sb.append(((DInt16)obt).getValue());
                    else if (obt instanceof DUInt16)   sb.append(((DUInt16)obt).getValue());
                    else if (obt instanceof DInt32)    sb.append(((DInt32)obt).getValue());
                    else if (obt instanceof DUInt32)   sb.append(((DUInt32)obt).getValue());
                    else if (obt instanceof DBoolean)  sb.append(((DBoolean)obt).getValue());
                    else if (obt instanceof DString)   sb.append(((DString)obt).getValue());
                    else if (obt instanceof DSequence) sb.append("DSequence)");
                    else sb.append(obt.getTypeName());
                }
                System.out.println(sb.toString());
            }
        }
    }

    /**
     */
    public static void testErdlasCalCatch() throws Throwable {
        testVerboseOn();
        String results, query, tName, expected;
        String baseQuery = "&time>=2006-01-01"; 
        EDDTable tedd = (EDDTable)oneFromDatasetXml("erdlasCalCatch");

        //the basicQuery
//http://las.pfeg.noaa.gov/cgi-bin/ERDserver/calcatch.sql?time,area,block,Comments,Description,imported,mark_cat,NominalSpecies,pounds,region,RegionName,SpeciesGroup&time>="2006-01-01%2000:00:00"
//my test in browser (with calcatch. added)
//http://las.pfeg.noaa.gov/cgi-bin/ERDserver/calcatch.sql.ascii?calcatch.time,calcatch.area,calcatch.block,calcatch.Comments,calcatch.Description,calcatch.imported,calcatch.mark_cat,calcatch.NominalSpecies,calcatch.pounds,calcatch.region,calcatch.RegionName,calcatch.SpeciesGroup&calcatch.time%3E=1978-01-01
//returns goofy results   with or without " around date constraint
//lynn test (she has more variables, and in same order as form): 
//http://las.pfeg.noaa.gov/cgi-bin/ERDserver/calcatch.sql.ascii?calcatch.mark_cat,calcatch.pounds,calcatch.imported,calcatch.Description,calcatch.area,calcatch.region,calcatch.year,calcatch.SpeciesGroup,calcatch.month,calcatch.NominalSpecies,calcatch.Comments,calcatch.time,calcatch.RegionName,calcatch.block&calcatch.time%3E1978-01-01
//try next: change pydap config: calcatch.time -> String, and all String vars missing_value=""
/*
Dataset {
    Sequence {
        String time;
        String area;
        Int32 block;
        String Comments;
        String Description;
        String imported;
        Int32 mark_cat;
        String NominalSpecies;
        String pounds;
        Int32 region;
        String RegionName;
        String SpeciesGroup;
    } calcatch;
} calcatch%2Esql;
---------------------------------------------
calcatch.time, calcatch.area, calcatch.block, calcatch.Comments, calcatch.Description, calcatch.imported, calcatch.mark_cat, calcatch.NominalSpecies, calcatch.pounds, calcatch.region, calcatch.RegionName, calcatch.SpeciesGroup
"1972-02-01", -9999, 682, -9999, "Rockfish, yelloweye", "N", 265, "YEYE", "264", -9999, -9999, "ROCK"
"1973-08-01", -9999, 200, -9999, "Smelts, true", "N", 180, "SMLT", "375", -9999, -9999, "PEL"
...
-9999, -9999, -9999, -9999, "Surfperch, unspecified", "N", 550, "PRCH", "40020", -9999, -9999, "OTH"
...
"1973-07-01", -9999, 701, -9999, "Bonito, Pacific", "N", 3, "BONI", "149", -9999, -9999, "GAME"
"1974-06-01", "Northern California", 203, "also called pointed nose sole", "Sole, English", "N", 206, "EGLS", "638", 2, "Eureka", "FLAT"
"1977-07-01", "Southern California", 652, -9999, "Shark, thresher", "N", 155, "SHRK", "22", 6, "Santa Barbara - Morro Bay", "SHRK"
"1971-02-01", "Southern California", 665, "also called southern halibut", "Halibut, California", "N", 222, "CHLB", "2383", 6, "Santa Barbara - Morro Bay", "FLAT"
"1976-11-01", "Central California", 623, -9999, "Sole, unspecified", "N", 200, "UFLT", "302", 6, "Santa Barbara - Morro Bay", "FLAT"
"1976-08-01", "Central California", 600, -9999, "Turbot", "N", 240, "UFLT", "40", 6, "Santa Barbara - Morro Bay", "FLAT"
*/
try {
            tName = tedd.makeNewFileForDapQuery(null, null, baseQuery, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_CalCaltch", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error for erdlasCalCatch" +
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }


    /** This tests catching and recovering from outOfMemory errors. 
     * Since erddap now (2009-06-24) catches Throwable (not just Exception),
     * it should be able to recover from similar errors.
     * ?But it is exception below, not throwable. 
     */
    public static void testMemory() throws Throwable {
        testVerboseOn();
        String results, query, tName, expected;
        EDDTable tedd = (EDDTable)oneFromDatasetXml("pmelWOD5np");

        try {
            tName = tedd.makeNewFileForDapQuery(null, null, "s", EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_memory", ".dods"); 
            String fullName = EDStatic.fullTestCacheDirectory + tName;
            String2.log("\nFile successfully created: " + fullName +
                "\nnBytes=" + File2.length(fullName));
            results = File2.hexDump(fullName, 1024);
            expected = "!!!Shouldn't get here!!!";  
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            results = MustBe.getShortErrorMessage(t);
            expected = "DODSException: \"java.io.IOException:Too much data -- CDP timeout\"";
            Test.ensureEqual(results, expected, "results=\n" + results);      
        }

    }

    /**
     * This tests EDVTimeStamp with argo data since it uses mscec since 1970-01-01, so it is a 
     * test of other time units.
     */
    public static void testTimeStamp() throws Throwable {
        String2.log("\n****************** EDDTableFromDapSequence.testTimeStamp\n");
        testVerboseOn();
        String results, query, tName, expected;
        EDDTable edd = (EDDTable)oneFromDatasetXml("testTimeStamp"); //based on pmelArgoAll

        //the basicQuery
        try {
            //*** test getting das for entire dataset
            tName = edd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                edd.className() + "_timestamp", ".das"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            String2.log(results);
            expected = //see OpendapHelper.EOL for comments
"  time_stamp {\n" +
"    Float64 actual_range 8.1048066e+8, NaN;\n" +
"    String description \"Source has time in millis since epoch!\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time Stamp\";\n" +
"    Float64 missing_value NaN;\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n";
            int po = results.indexOf(expected.substring(0,12));
            Test.ensureEqual(results.substring(po, po + expected.length()), expected, "\nresults=\n" + results);
            Test.ensureTrue(results.indexOf("String time_coverage_end") < 0, "\nresults=\n" + results);
            Test.ensureTrue(results.indexOf("String time_coverage_start") < 0, "\nresults=\n" + results);

            
            //*** test getting dds for entire dataset
            tName = edd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
                edd.className() + "_timestamp", ".dds"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            //String2.log(results);
            expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Float64 time_stamp;\n" +
"    Int32 id;\n" +
"    Float32 pres;\n" +
"    Float32 cndc;\n" +
"    Float32 temp;\n" +
"  } s;\n" +
"} s;\n";
            Test.ensureEqual(results, expected, "\nresults=\n" + results);


            //*** test a TableWriter that doesn't convert time to iso format
            query = "&time_stamp>=2000-08-07T00&time_stamp<2000-08-07T00:34";             
           
            //.asc
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_timestamp", ".asc"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
//2010-04-16  just the id changed (from 156470 to 308050)!  I emailed Joe Sirott
"Dataset {\n" +
"  Sequence {\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Float64 time_stamp;\n" +
"    Int32 id;\n" +
"    Float32 pres;\n" +
"    Float32 cndc;\n" +
"    Float32 temp;\n" +
"  } s;\n" +
"} s;\n" +
"---------------------------------------------\n" +
"s.longitude, s.latitude, s.time_stamp, s.id, s.pres, s.cndc, s.temp\n" +
"143.408, 41.405, 9.6560843E8, 308050, 4.4, , 11.392\n" +
"143.408, 41.405, 9.6560843E8, 308050, 9.6, , 9.166\n" +
"143.408, 41.405, 9.6560843E8, 308050, 14.8, , 8.239\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);      
           

            //*** test the TableWriters that convert time to iso format: geoJson, htmlTable, json, separatedValue
            //note that units are UTC, not "seconds since 1970-01-01..."
            query = "&time_stamp>=2000-08-07T00&time_stamp<2000-08-07T00:34";             
           
            //.csv   / separatedValue
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_timestamp", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
//2010-04-16  just the id changed (from 156470 to 308050)!  I emailed Joe Sirott
"longitude,latitude,time_stamp,id,pres,cndc,temp\n" +
"degrees_east,degrees_north,UTC,,decibar,mhos m-1,degree_C\n" +
"143.408,41.405,2000-08-07T00:33:50Z,308050,4.4,NaN,11.392\n" +
"143.408,41.405,2000-08-07T00:33:50Z,308050,9.6,NaN,9.166\n" +
"143.408,41.405,2000-08-07T00:33:50Z,308050,14.8,NaN,8.239\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);      
           
            //geoJson
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_timestamp", ".geoJson"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
//2010-04-16  just the id changed (from 156470 to 308050)!  I emailed Joe Sirott
"{\n" +
"  \"type\": \"FeatureCollection\",\n" +
"  \"propertyNames\": [\"time_stamp\", \"id\", \"pres\", \"cndc\", \"temp\"],\n" +
"  \"propertyUnits\": [\"UTC\", null, \"decibar\", \"mhos m-1\", \"degree_C\"],\n" +
"  \"features\": [\n" +
"\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [143.408, 41.405] },\n" +
"  \"properties\": {\n" +
"    \"time_stamp\": \"2000-08-07T00:33:50Z\",\n" +
"    \"id\": 308050,\n" +
"    \"pres\": 4.4,\n" +
"    \"cndc\": null,\n" +
"    \"temp\": 11.392 }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [143.408, 41.405] },\n" +
"  \"properties\": {\n" +
"    \"time_stamp\": \"2000-08-07T00:33:50Z\",\n" +
"    \"id\": 308050,\n" +
"    \"pres\": 9.6,\n" +
"    \"cndc\": null,\n" +
"    \"temp\": 9.166 }\n" +
"},\n" +
"{\"type\": \"Feature\",\n" +
"  \"geometry\": {\n" +
"    \"type\": \"Point\",\n" +
"    \"coordinates\": [143.408, 41.405] },\n" +
"  \"properties\": {\n" +
"    \"time_stamp\": \"2000-08-07T00:33:50Z\",\n" +
"    \"id\": 308050,\n" +
"    \"pres\": 14.8,\n" +
"    \"cndc\": null,\n" +
"    \"temp\": 8.239 }\n" +
"},\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);      
           
            //htmlTable
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_timestamp", ".htmlTable"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
EDStatic.startHeadHtml(EDStatic.erddapUrl((String)null), "EDDTableFromDapSequence_timestamp") + "\n" +
"</head>\n" +
EDStatic.startBodyHtml(null) + "\n" +
"&nbsp;\n" +
"<form action=\"\">\n" +
"<input type=\"button\" value=\"Back\" onClick=\"history.go(-1);return true;\">\n" +
"</form>\n" +
"\n" +
"&nbsp;\n" +
"<table class=\"erd commonBGColor\" cellspacing=\"0\">\n" +
"<tr>\n" +
"<th>longitude\n" +
"<th>latitude\n" +
"<th>time_stamp\n" +
"<th>id\n" +
"<th>pres\n" +
"<th>cndc\n" +
"<th>temp\n" +
"</tr>\n" +
"<tr>\n" +
"<th>degrees_east\n" +
"<th>degrees_north\n" +
"<th>UTC\n" +
"<th>&nbsp;\n" +
"<th>decibar\n" +
"<th>mhos m-1\n" +
"<th>degree_C\n" +
"</tr>\n" +
"<tr>\n" +
"<td align=\"right\">143.408\n" +
"<td align=\"right\">41.405\n" +
"<td nowrap>2000-08-07T00:33:50Z\n" +
"<td align=\"right\">308050\n" +
"<td align=\"right\">4.4\n" +
"<td>&nbsp;\n" +
"<td align=\"right\">11.392\n" +
"</tr>\n" +
"<tr>\n" +
"<td align=\"right\">143.408\n" +
"<td align=\"right\">41.405\n" +
"<td nowrap>2000-08-07T00:33:50Z\n" +
"<td align=\"right\">308050\n" +
"<td align=\"right\">9.6\n" +
"<td>&nbsp;\n" +
"<td align=\"right\">9.166\n" +
"</tr>\n" +
"<tr>\n" +
"<td align=\"right\">143.408\n" +
"<td align=\"right\">41.405\n" +
"<td nowrap>2000-08-07T00:33:50Z\n" +
"<td align=\"right\">308050\n" +
"<td align=\"right\">14.8\n" +
"<td>&nbsp;\n" +
"<td align=\"right\">8.239\n" +
"</tr>\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);      
expected =            
"<tr>\n" +
"<td align=\"right\">143.408\n" +
"<td align=\"right\">41.405\n" +
"<td nowrap>2000-08-07T00:33:50Z\n" +
"<td align=\"right\">308050\n" +
"<td align=\"right\">500.5\n" +
"<td>&nbsp;\n" +
"<td align=\"right\">3.554\n" +
"</tr>\n" +
"</table>\n" +
EDStatic.endBodyHtml(EDStatic.erddapUrl((String)null)) + "\n" +
"</html>\n";
            Test.ensureEqual(results.substring(results.length() - expected.length()), expected, 
                "results=\n" + results);      

            //json
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_timestamp", ".json"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
//2010-04-16  just the id changed (from 156470 to 308050)!  I emailed Joe Sirott
"{\n" +
"  \"table\": {\n" +
"    \"columnNames\": [\"longitude\", \"latitude\", \"time_stamp\", \"id\", \"pres\", \"cndc\", \"temp\"],\n" +
"    \"columnTypes\": [\"float\", \"float\", \"String\", \"int\", \"float\", \"float\", \"float\"],\n" +
"    \"columnUnits\": [\"degrees_east\", \"degrees_north\", \"UTC\", null, \"decibar\", \"mhos m-1\", \"degree_C\"],\n" +
"    \"rows\": [\n" +
"      [143.408, 41.405, \"2000-08-07T00:33:50Z\", 308050, 4.4, null, 11.392],\n" +
"      [143.408, 41.405, \"2000-08-07T00:33:50Z\", 308050, 9.6, null, 9.166],\n" +
"      [143.408, 41.405, \"2000-08-07T00:33:50Z\", 308050, 14.8, null, 8.239],\n";
            Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);      


            //***test that image uses time for colorbar
            query = "longitude,latitude,time_stamp&time_stamp%3E=2000-08-07T00:15&time_stamp%3C=2000-08-07T01&.draw=markers&.marker=5|5&.color=0x000000&.colorBar=|||||";
            tName = edd.makeNewFileForDapQuery(null, null, query,
                 EDStatic.fullTestCacheDirectory, edd.className() + "_timestamp", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            tName = edd.makeNewFileForDapQuery(null, null, query, 
                 EDStatic.fullTestCacheDirectory, edd.className() + "_timestamp", ".kml"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

            //***test that image uses time for x axis
            tName = edd.makeNewFileForDapQuery(null, null, 
                "time_stamp,pres,temp&time_stamp%3E=2000-08-07T00:00:00Z&time_stamp%3C=2000-08-07T01:00:00Z&.draw=markers&.marker=5|5&.color=0x000000&.colorBar=|||||", 
                 EDStatic.fullTestCacheDirectory, edd.className() + "_timestamp2", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nUnexpected error for testTimeStamp." +
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }

    /** This tests reading a .pngInfo file. */
    public static void testReadPngInfo() throws Throwable{
        /* needs work
        String2.log("\n* EDD.testReadPngInfo");
        Object oa[] = readPngInfo(null,
            "F:/u00/cwatch/testData/graphs/testGlobecBottle_2603962601.json");
        double graphDoubleWESN[] = (double[])oa[0];
        Test.ensureEqual(graphDoubleWESN, 
            new double[]{-126.30999908447265, -123.45999755859376, 41.9, 44.75000152587891}, "");

        int graphIntWESN[] = (int[])oa[1];
        Test.ensureEqual(graphIntWESN, 
            new int[]{29, 331, 323, 20}, "");
        */
    }

    /** This tests sourceNeedsExpandedFP_EQ. */
    public static void testSourceNeedsExpandedFP_EQ() throws Throwable {
        String2.log("\n****************** EDDTableFromDapSequence.testSourceNeedsExpandedFP_EQ\n");
        testVerboseOn();
        String results, query, tName, expected;
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nwioosGroundfish"); 

            //the basicQuery
            //[was: test a TableWriter that doesn't convert time to iso format
            // now year is converted to time.]
            query = "longitude,latitude,time,common_name&longitude=-124.348098754882&latitude=44.690254211425";             
           
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_FP_EQ", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"longitude,latitude,time,common_name\n" +
"degrees_east,degrees_north,UTC,\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,Dover sole\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,petrale sole\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,arrowtooth flounder\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,sablefish\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,English sole\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,yellowtail rockfish\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,Pacific ocean perch\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,longspine thornyhead\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,widow rockfish\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,shortspine thornyhead\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,darkblotched rockfish\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,yelloweye rockfish\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,cowcod\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,canary rockfish\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,chilipepper\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,bocaccio\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\nUnexpected error for testSourceNeedsExpandedFP_EQ." +
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }

    /** This tests lat lon requests. */
    public static void testLatLon() throws Throwable {
        String2.log("\n****************** EDDTableFromDapSequence.testLatLon\n");
        testVerboseOn();
        String results, query, tName, expected;

        //the basicQuery
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("erdGlobecMoc1"); 

            //*** test a TableWriter that doesn't convert time to iso format
            query = "cruise_id,station_id,longitude,latitude&latitude=44.6517&distinct()";             
           
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_LL", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
            expected = 
"cruise_id,station_id,longitude,latitude\n" +
",,degrees_east,degrees_north\n" +
"NH0005,NH15,-124.4117,44.6517\n" +
"NH0005,NH25,-124.65,44.6517\n" +
"NH0007,NH05,-124.175,44.6517\n" +
"NH0007,NH15,-124.4117,44.6517\n" +
"NH0007,NH25,-124.65,44.6517\n" +
"W0004B,NH05,-124.175,44.6517\n" +
"W0004B,NH15,-124.4117,44.6517\n" +
"W0004B,NH25,-124.65,44.6517\n" +
"W0004B,NH45,-125.1167,44.6517\n" +
"W0007A,NH15,-124.4117,44.6517\n" +
"W0007A,NH25,-124.65,44.6517\n" +
"W0009A,NH15,-124.4117,44.6517\n" +
"W0009A,NH25,-124.65,44.6517\n" +
"W0204A,NH25,-124.65,44.6517\n" +
"W0205A,NH15,-124.4117,44.6517\n" +
"W0205A,NH25,-124.65,44.6517\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\nUnexpected error for testLatLon." +
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }

    /** This tests nosCoopsRWL. */
    public static void testNosCoopsRWL() throws Throwable {
        String2.log("\n****************** EDDTableFromDapSequence.testNosCoopsRWL\n");
        testVerboseOn();
        String results, query, tName, expected;
        String today     = Calendar2.epochSecondsToIsoStringT(Calendar2.backNDays(1, Double.NaN));
        String yesterday = Calendar2.epochSecondsToIsoStringT(Calendar2.backNDays(2, Double.NaN));

        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nosCoopsRWL"); 

            //*** test a TableWriter that doesn't convert time to iso format
            query = "&station=\"1612340\"&datum=\"MLLW\"&beginTime=" + yesterday + "&endTime=" + today;             
//http://opendap.co-ops.nos.noaa.gov/dods/IOOS/SixMin_Verified_Water_Level.ascii?
//&WATERLEVEL_6MIN_VFD_PX._STATION_ID="1612340"&WATERLEVEL_6MIN_VFD_PX._DATUM="MLLW"
//&WATERLEVEL_6MIN_VFD_PX._BEGIN_DATE="20100825"&WATERLEVEL_6MIN_VFD_PX._END_DATE="20100826"            
            tName = edd.makeNewFileForDapQuery(null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_RWL", ".csv"); 
            results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
//trouble: Joda doesn't like space-padded hour values

            expected = 
"zztop\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\nUnexpected error for testNosCoopsRWL." +
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }

    /** Test reading .das */
    public static void testReadDas() throws Exception {
        String2.log("\n*** EDDTableFromDapSequence.testReadDas\n");
        String url = "http://192.168.31.6/erddap/tabledap/erdGtsppBest";
        try {
            DConnect dConnect = new DConnect(url, true, 1, 1);
            DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
            DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\nUnexpected error for testReadDas." +
                "\nNote: this test requires erdGtsppBest on coastwatch's ERDDAP:" +
                "\nurl=" + url +
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }

    /** Test graph made from subsetVariables data */
    public static void testSubsetVariablesGraph() throws Exception {
        String2.log("\n*** EDDTableFromDapSequence.testSubsetVariablesGraph\n");
        try {
            EDDTable edd = (EDDTable)oneFromDatasetXml("nwioosCoral"); 

            String tName = edd.makeNewFileForDapQuery(null, null, 
                "longitude,latitude,time&time=%221992-01-01T00:00:00Z%22" +
                "&longitude>=-132.0&longitude<=-112.0&latitude>=30.0&latitude<=50.0" +
                "&distinct()&.draw=markers&.colorBar=|D||||", 
                EDStatic.fullTestCacheDirectory, edd.className() + "_SVGraph", ".png"); 
            SSR.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

        } catch (Throwable t) {
            String2.getStringFromSystemIn("\n" + MustBe.throwableToString(t) + 
                "\nUnexpected error for testSubsetVariablesGraph." +
                "\nPress ^C to stop or Enter to continue..."); 
        }
    }

    /** Test that info from subsetVariables gets back to variable's ranges */
    public static void testSubsetVariablesRange() throws Throwable {
        String2.log("\n*** EDDTableFromDapSequence.testSubsetVariablesRange\n");
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //before I fixed this, time had destinationMin/Max = NaN
        EDDTable edd = (EDDTable)oneFromDatasetXml("nwioosCoral"); 
        EDV edvTime = edd.dataVariables()[edd.timeIndex];
        Test.ensureEqual(edvTime.destinationMin(), 3.155328E8,  "");
        Test.ensureEqual(edvTime.destinationMax(), 1.1045376E9, "");

        String tName = edd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            edd.className() + "_Entire", ".das"); 
        String results = String2.annotatedString(new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray()));
        String expected = 
"Attributes {[10]\n" +
" s {[10]\n" +
"  longitude {[10]\n" +
"    String _CoordinateAxisType \"Lon\";[10]\n" +
"    Float64 actual_range -125.98999786376953, -117.27667236328125;[10]\n" +
"    String axis \"X\";[10]\n" +
"    String ioos_category \"Location\";[10]\n" +
"    String long_name \"Longitude\";[10]\n" +
"    String standard_name \"longitude\";[10]\n" +
"    String units \"degrees_east\";[10]\n" +
"  }[10]\n" +
"  latitude {[10]\n" +
"    String _CoordinateAxisType \"Lat\";[10]\n" +
"    Float64 actual_range 32.570838928222656, 48.969085693359375;[10]\n" +
"    String axis \"Y\";[10]\n" +
"    String ioos_category \"Location\";[10]\n" +
"    String long_name \"Latitude\";[10]\n" +
"    String standard_name \"latitude\";[10]\n" +
"    String units \"degrees_north\";[10]\n" +
"  }[10]\n" +
"  altitude {[10]\n" +
"    String _CoordinateAxisType \"Height\";[10]\n" +
"    String _CoordinateZisPositive \"up\";[10]\n" +
"    Float64 actual_range 11.0, 1543.0;[10]\n" +
"    String axis \"Z\";[10]\n" +
"    Float64 colorBarMaximum 0.0;[10]\n" +
"    Float64 colorBarMinimum -1500.0;[10]\n" +
"    String ioos_category \"Location\";[10]\n" +
"    String long_name \"Altitude\";[10]\n" +
"    String positive \"up\";[10]\n" +
"    String standard_name \"altitude\";[10]\n" +
"    String units \"m\";[10]\n" +
"  }[10]\n" +
"  time {[10]\n" +
"    String _CoordinateAxisType \"Time\";[10]\n" +
"    Float64 actual_range 3.155328e+8, 1.1045376e+9;[10]\n" +
"    String axis \"T\";[10]\n" +
"    String Description \"Year of Survey.\";[10]\n" +
"    String ioos_category \"Time\";[10]\n" +
"    String long_name \"Time (Beginning of Survey Year)\";[10]\n" +
"    String standard_name \"time\";[10]\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";[10]\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";[10]\n" +
"  }[10]\n" +
"  institution {[10]\n" +
"    String Description \"Institution is either: Northwest Fisheries Science Center (FRAM Division) or Alaska Fisheries Science Center (RACE Division)\";[10]\n" +
"    String ioos_category \"Identifier\";[10]\n" +
"    String long_name \"Institution\";[10]\n" +
"  }[10]\n" +
"  institution_id {[10]\n" +
"    Float64 actual_range 38807.0, 2.00503017472e+11;[10]\n" +
"    String Description \"Unique ID from Institution.\";[10]\n" +
"    String ioos_category \"Identifier\";[10]\n" +
"    String long_name \"Institution ID\";[10]\n" +
"  }[10]\n" +
"  species_code {[10]\n" +
"    Float64 actual_range 41000.0, 144115.0;[10]\n" +
"    String Description \"Unique identifier for species.\";[10]\n" +
"    String ioos_category \"Taxonomy\";[10]\n" +
"    String long_name \"Species Code\";[10]\n" +
"  }[10]\n" +
"  taxa_scientific {[10]\n" +
"    String Description \"Scientific name of taxa\";[10]\n" +
"    String ioos_category \"Taxonomy\";[10]\n" +
"    String long_name \"Taxa Scientific\";[10]\n" +
"  }[10]\n" +
"  taxonomic_order {[10]\n" +
"    String ioos_category \"Taxonomy\";[10]\n" +
"    String long_name \"Taxonomic Order\";[10]\n" +
"  }[10]\n" +
"  order_abbreviation {[10]\n" +
"    String ioos_category \"Taxonomy\";[10]\n" +
"    String long_name \"Order Abbreviation\";[10]\n" +
"  }[10]\n" +
"  taxonomic_family {[10]\n" +
"    String ioos_category \"Taxonomy\";[10]\n" +
"    String long_name \"Taxonomic Family\";[10]\n" +
"  }[10]\n" +
"  family_abbreviation {[10]\n" +
"    String ioos_category \"Taxonomy\";[10]\n" +
"    String long_name \"Family Abbreviation\";[10]\n" +
"  }[10]\n" +
"  taxonomic_genus {[10]\n" +
"    String Description \"Taxonomic Genus.\";[10]\n" +
"    String ioos_category \"Taxonomy\";[10]\n" +
"    String long_name \"Taxonomic Genus\";[10]\n" +
"  }[10]\n" +
" }[10]\n" +
"  NC_GLOBAL {[10]\n" +
"    String cdm_data_type \"Point\";[10]\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";[10]\n" +
"    Float64 Easternmost_Easting -117.27667236328125;[10]\n" +
"    String featureType \"Point\";[10]\n" +
"    Float64 geospatial_lat_max 48.969085693359375;[10]\n" +
"    Float64 geospatial_lat_min 32.570838928222656;[10]\n" +
"    String geospatial_lat_units \"degrees_north\";[10]\n" +
"    Float64 geospatial_lon_max -117.27667236328125;[10]\n" +
"    Float64 geospatial_lon_min -125.98999786376953;[10]\n" +
"    String geospatial_lon_units \"degrees_east\";[10]\n" +
"    Float64 geospatial_vertical_max 1543.0;[10]\n" +
"    Float64 geospatial_vertical_min 11.0;[10]\n" +
"    String geospatial_vertical_positive \"up\";[10]\n" +
"    String geospatial_vertical_units \"m\";[10]\n" +
"    String history \"" + today;
        String tResults = results.substring(0, expected.length());
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
//+ " http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005[10]\n" +
//today + " http://127.0.0.1:8080/cwexperimental/
expected = 
"tabledap/nwioosCoral.das\";[10]\n" +
"    String infoUrl \"http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005.das.info\";[10]\n" +
"    String institution \"NOAA NWFSC\";[10]\n" +
"    String keywords \"Biosphere > Aquatic Ecosystems > Coastal Habitat,[10]\n" +
"Biosphere > Aquatic Ecosystems > Marine Habitat,[10]\n" +
"Biological Classification > Animals/Invertebrates > Cnidarians > Anthozoans/Hexacorals > Hard Or Stony Corals,[10]\n" +
"1980-2005, abbreviation, altitude, atmosphere, beginning, coast, code, collected, coral, data, family, genus, height, identifier, institution, noaa, nwfsc, off, order, scientific, species, station, survey, taxa, taxonomic, taxonomy, time, west, west coast, year\";[10]\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";[10]\n" +
"    String license \"The data may be used and redistributed for free but is not intended[10]\n" +
"for legal use, since it may contain inaccuracies. Neither the data[10]\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any[10]\n" +
"of their employees or contractors, makes any warranty, express or[10]\n" +
"implied, including warranties of merchantability and fitness for a[10]\n" +
"particular purpose, or assumes any legal liability for the accuracy,[10]\n" +
"completeness, or usefulness, of this information.\";[10]\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";[10]\n" +
"    Float64 Northernmost_Northing 48.969085693359375;[10]\n" +
"    String sourceUrl \"http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005\";[10]\n" +
"    Float64 Southernmost_Northing 32.570838928222656;[10]\n" +
"    String standard_name_vocabulary \"CF-12\";[10]\n" +
"    String subsetVariables \"longitude, latitude, altitude, time, institution, institution_id, species_code, taxa_scientific, taxonomic_order, order_abbreviation, taxonomic_family, family_abbreviation, taxonomic_genus\";[10]\n" +
"    String summary \"This data contains the locations of some observations of[10]\n" +
"cold-water/deep-sea corals off the west coast of the United States.[10]\n" +
"Records of coral catch originate from bottom trawl surveys conducted[10]\n" +
"from 1980 to 2001 by the Alaska Fisheries Science Center (AFSC) and[10]\n" +
"2001 to 2005 by the Northwest Fisheries Science Center (NWFSC).[10]\n" +
"Locational information represent the vessel mid positions (for AFSC[10]\n" +
"survey trawls) or \\\"best position\\\" (i.e., priority order: 1) gear[10]\n" +
"midpoint 2) vessel midpoint, 3) vessel start point, 4) vessel end[10]\n" +
"point, 5) station coordinates for NWFSC survey trawls) conducted as[10]\n" +
"part of regular surveys of groundfish off the coasts of Washington,[10]\n" +
"Oregon and California by NOAA Fisheries. Only records where corals[10]\n" +
"were identified in the total catch are included. Each catch sample[10]\n" +
"of coral was identified down to the most specific taxonomic level[10]\n" +
"possible by the biologists onboard, therefore identification was[10]\n" +
"dependent on their expertise. When positive identification was not[10]\n" +
"possible, samples were sometimes archived for future identification[10]\n" +
"by systematist experts. Data were compiled by the NWFSC, Fishery[10]\n" +
"Resource Analysis & Monitoring Division[10]\n" +
"[10]\n" +
"Purpose - Examination of the spatial and temporal distributions of[10]\n" +
"observations of cold-water/deep-sea corals off the west coast of the[10]\n" +
"United States, including waters off the states of Washington, Oregon,[10]\n" +
"and California. It is important to note that these records represent[10]\n" +
"only presence of corals in the area swept by the trawl gear. Since[10]\n" +
"bottom trawls used during these surveys are not designed to sample[10]\n" +
"epibenthic invertebrates, absence of corals in the catch does not[10]\n" +
"necessary mean they do not occupy the area swept by the trawl gear.[10]\n" +
"[10]\n" +
"Data Credits - NOAA Fisheries, Alaska Fisheries Science Center,[10]\n" +
"Resource Assessment & Conservation Engineering Division (RACE) NOAA[10]\n" +
"Fisheries, Northwest Fisheries Science Center, Fishery Resource[10]\n" +
"Analysis & Monitoring Division (FRAM)[10]\n" +
"[10]\n" +
"Contact: Curt Whitmire, NOAA NWFSC, Curt.Whitmire@noaa.gov\";[10]\n" +
"    String time_coverage_end \"2005-01-01T00:00:00Z\";[10]\n" +
"    String time_coverage_start \"1980-01-01T00:00:00Z\";[10]\n" +
"    String title \"NWFSC Coral Data Collected off West Coast of US (1980-2005)\";[10]\n" +
"    Float64 Westernmost_Easting -125.98999786376953;[10]\n" +
"  }[10]\n" +
"}[10]\n" +
"[end]";
        int tpo = results.indexOf(expected.substring(0, 17));
        if (tpo < 0) String2.log("results=\n" + results);
        Test.ensureEqual(results.substring(tpo, tpo + expected.length()), expected, 
            "results=\n" + results);

    }

     
    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test(boolean doAllGraphicsTests) throws Throwable {
        String2.log("\n****************** EDDTableFromDapSequence.test() *****************\n");
        testVerboseOn();

/* */ 
        //always done        
        testBasic();
        testGraphics(doAllGraphicsTests);
        testNetcdf();
        //testDapper(doAllGraphicsTests); Joe Sirott abandoned DAPPER ~2011-07
        testGenerateDatasetsXml();
        testPsdac();
        testGlobecBirds();
        testArgoTime();
        testTimeStamp();
        testSourceNeedsExpandedFP_EQ();
        testLatLon();
        testReadDas();
        testSubsetVariablesGraph();
        testSubsetVariablesRange();

   //     testErdlasNewportCtd();   //not yet working
   //     testErdlasCalCatch();     //not yet working
   //testReadPngInfo();  //needs work
   
        //not usually done
        //testOneTime();
        //testMemory();  important but very slow

    }

}
