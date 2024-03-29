/* 
 * EDDTableFromDapSequence Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
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

/** The Java DAP classes.  */
import dods.dap.*;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.GregorianCalendar;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;

/** 
 * This class represents a table of data from an opendap sequence source.
 * 
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-08
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
     * @param erddap if known in this context, else null
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromDapSequence"&gt; 
     *    having just been read.  
     * @return an EDDTableFromDapSequence.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromDapSequence fromXml(Erddap erddap, 
        SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFromDapSequence(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        Attributes tGlobalAttributes = null;
        ArrayList tDataVariables = new ArrayList();
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
        String tGraphsAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tSosOfferingPrefix = null;
        String tLocalSourceUrl = null;
        String tOuterSequenceName = null;
        String tInnerSequenceName = null; 
        boolean tSourceNeedsExpandedFP_EQ = true;
        boolean tSourceCanConstrainStringEQNE = true;
        boolean tSourceCanConstrainStringGTLT = true;
        String tSourceCanConstrainStringRegex = null;
        boolean tSkipDapperSpacerRows = false;
        String tDefaultDataQuery = null;
        String tDefaultGraphQuery = null;
        String tAddVariablesWhere = null;

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
            else if (localTags.equals( "<altitudeMetersPerSourceUnit>")) 
                throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
            else if (localTags.equals( "<dataVariable>")) 
                tDataVariables.add(getSDADVariableFromXml(xmlReader));           
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<graphsAccessibleTo>")) {}
            else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
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
            else if (localTags.equals( "<onChange>")) {}
            else if (localTags.equals("</onChange>")) tOnChange.add(content); 
            else if (localTags.equals( "<fgdcFile>")) {}
            else if (localTags.equals("</fgdcFile>"))     tFgdcFile = content; 
            else if (localTags.equals( "<iso19115File>")) {}
            else if (localTags.equals("</iso19115File>")) tIso19115File = content; 
            else if (localTags.equals( "<sosOfferingPrefix>")) {}
            else if (localTags.equals("</sosOfferingPrefix>")) tSosOfferingPrefix = content; 
            else if (localTags.equals( "<defaultDataQuery>")) {}
            else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content; 
            else if (localTags.equals( "<defaultGraphQuery>")) {}
            else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content; 
            else if (localTags.equals( "<addVariablesWhere>")) {}
            else if (localTags.equals("</addVariablesWhere>")) tAddVariablesWhere = content; 

            else xmlReader.unexpectedTagException();
        }
        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++)
            ttDataVariables[i] = (Object[])tDataVariables.get(i);

        return new EDDTableFromDapSequence(tDatasetID, 
            tAccessibleTo, tGraphsAccessibleTo,
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery, tAddVariablesWhere,
            tGlobalAttributes, ttDataVariables,
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
     *  (recommended: [A-Za-z][A-Za-z0-9_]* )
     *   for this dataset. See EDD.datasetID().
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tOnChange 0 or more actions (starting with http://, https://, or mailto: )
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
     *      <li> a java.time.format.DateTimeFormatter string
     *        (which is compatible with java.text.SimpleDateFormat) describing how to interpret 
     *        string times  (e.g., the ISO8601TZ_FORMAT "yyyy-MM-dd'T'HH:mm:ssZ", see 
     *        https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html or 
     *        https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/text/SimpleDateFormat.html)).
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
    public EDDTableFromDapSequence(String tDatasetID, 
        String tAccessibleTo, String tGraphsAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix,
        String tDefaultDataQuery, String tDefaultGraphQuery, String tAddVariablesWhere, 
        Attributes tAddGlobalAttributes,
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
        setGraphsAccessibleTo(tGraphsAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        sosOfferingPrefix= tSosOfferingPrefix;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
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

        //Design decision: this doesn't use e.g., ucar.nc2.dt.StationDataSet 
        //  because it determines axes via _CoordinateAxisType (or similar) metadata
        //  which most datasets we use don't have yet.
        //  One could certainly write another class that did use ucar.nc2.dt.StationDataSet.

        //quickRestart
        Attributes quickRestartAttributes = null;       
        if (EDStatic.quickRestart && 
            EDStatic.initialLoadDatasets() && 
            File2.isFile(quickRestartFullFileName())) {
            //try to do quick initialLoadDatasets()
            //If this fails anytime during construction, the dataset will be loaded 
            //  during the next major loadDatasets,
            //  which is good because it allows quick loading of other datasets to continue.
            //This will fail (good) if dataset has changed significantly and
            //  quickRestart file has outdated information.
            quickRestartAttributes = NcHelper.readAttributesFromNc3(quickRestartFullFileName());

            if (verbose)
                String2.log("  using info from quickRestartFile");

            //set creationTimeMillis to time of previous creation, so next time
            //to be reloaded will be same as if ERDDAP hadn't been restarted.
            creationTimeMillis = quickRestartAttributes.getLong("creationTimeMillis");
        }
      
        //DAS
        byte dasBytes[] = quickRestartAttributes == null?
            SSR.getUrlResponseBytes(localSourceUrl + ".das") : //has timeout and descriptive error 
            ((ByteArray)quickRestartAttributes.get("dasBytes")).toArray();
        DAS das = new DAS();
        das.parse(new ByteArrayInputStream(dasBytes));

        //DDS
        byte ddsBytes[] = quickRestartAttributes == null?
            SSR.getUrlResponseBytes(localSourceUrl + ".dds") : //has timeout and descriptive error 
            ((ByteArray)quickRestartAttributes.get("ddsBytes")).toArray();
        DDS dds = new DDS();
        dds.parse(new ByteArrayInputStream(ddsBytes));

        //get global attributes
        sourceGlobalAttributes = new Attributes();
        OpendapHelper.getAttributes(das, "GLOBAL", sourceGlobalAttributes);
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("\"null\"");

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
                    tDataSourceTypes[dv] = PAType.toCohortString( 
                        OpendapHelper.getElementPAType(ibt.newPrimitiveVector()));

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
                tDataSourceTypes[dv] = PAType.toCohortString( 
                    OpendapHelper.getElementPAType(obt.newPrimitiveVector()));

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

            //if _Unsigned=true or false, change tSourceType
            if (tSourceAtt == null)
                tSourceAtt = new Attributes();
            if (tAddAtt == null)
                tAddAtt = new Attributes();
            tSourceType = Attributes.adjustSourceType(tSourceType, tSourceAtt, tAddAtt);
            
            //ensure the variable was found
            if (tSourceName.startsWith("=")) {
                //if isFixedValue, sourceType can be inferred
            } else if (tSourceType == null) {
                throw new IllegalArgumentException(errorInMethod + "dataVariable#" + dv + " name=" + 
                    tSourceName + " not found in data source.");
            }

            if (EDV.LON_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVLon(datasetID, tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, PAOne.fromDouble(Double.NaN), PAOne.fromDouble(Double.NaN)); 
                lonIndex = dv;
            } else if (EDV.LAT_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVLat(datasetID, tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, PAOne.fromDouble(Double.NaN), PAOne.fromDouble(Double.NaN)); 
                latIndex = dv;
            } else if (EDV.ALT_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVAlt(datasetID, tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, PAOne.fromDouble(Double.NaN), PAOne.fromDouble(Double.NaN));
                altIndex = dv;
            } else if (EDV.DEPTH_NAME.equals(tDestName)) {
                dataVariables[dv] = new EDVDepth(datasetID, tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType, PAOne.fromDouble(Double.NaN), PAOne.fromDouble(Double.NaN));
                depthIndex = dv;
            } else if (EDV.TIME_NAME.equals(tDestName)) {  //look for TIME_NAME before check hasTimeUnits (next)
                dataVariables[dv] = new EDVTime(datasetID, tSourceName,
                    tSourceAtt, tAddAtt, 
                    tSourceType); //this constructor gets source / sets destination actual_range
                timeIndex = dv;
            } else if (EDVTimeStamp.hasTimeUnits(tSourceAtt, tAddAtt)) {
                dataVariables[dv] = new EDVTimeStamp(datasetID, tSourceName, tDestName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //this constructor gets source / sets destination actual_range
            } else {
                dataVariables[dv] = new EDV(datasetID, tSourceName, tDestName, 
                    tSourceAtt, tAddAtt,
                    tSourceType); //the constructor that reads actual_range
                dataVariables[dv].setActualRangeFromDestinationMinMax();
            }
        }


        //make addVariablesWhereAttNames and addVariablesWhereAttValues
        makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);

        //ensure the setup is valid
        ensureValid();

        //save quickRestart info
        if (quickRestartAttributes == null) { //i.e., there is new info
            try {
                quickRestartAttributes = new Attributes();
                quickRestartAttributes.set("creationTimeMillis", "" + creationTimeMillis);
                quickRestartAttributes.set("dasBytes", new ByteArray(dasBytes));
                quickRestartAttributes.set("ddsBytes", new ByteArray(ddsBytes));
                File2.makeDirectory(File2.getDirectory(quickRestartFullFileName()));
                NcHelper.writeAttributesToNc3(quickRestartFullFileName(), 
                    quickRestartAttributes);
            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }
        }

        //finally
        long cTime = System.currentTimeMillis() - constructionStartMillis;
        if (verbose) String2.log(
            (debugMode? "\n" + toString() : "") +
            "\n*** EDDTableFromDapSequence " + datasetID + " constructor finished. TIME=" + 
            cTime + "ms" + (cTime >= 600000? "  (>10m!)" : cTime >= 10000? "  (>10s!)" : "") + "\n"); 

    }

    /**
     * This returns true if this EDDTable knows each variable's actual_range (e.g., 
     * EDDTableFromFiles) or false if it doesn't (e.g., EDDTableFromDatabase).
     *
     * @returns true if this EDDTable knows each variable's actual_range (e.g., 
     * EDDTableFromFiles) or false if it doesn't (e.g., EDDTableFromDatabase).
     */
    public boolean knowsActualRange() {return false; } //because this gets info from a remote service


    /** 
     * This gets the data (chunk by chunk) from this EDDTable for the 
     * OPeNDAP DAP-style query and writes it to the TableWriter. 
     * See the EDDTable method documentation.
     *
     * @param language the index of the selected language
     * @param loggedInAs the user's login name if logged in (or null if not logged in).
     * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
     * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be null.
     * @param tableWriter
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
     */
    public void getDataForDapQuery(int language, String loggedInAs, String requestUrl, 
        String userDapQuery, TableWriter tableWriter) throws Throwable {

        //get the sourceDapQuery (a query that the source can handle)
        StringArray resultsVariables    = new StringArray();
        StringArray constraintVariables = new StringArray();
        StringArray constraintOps       = new StringArray();
        StringArray constraintValues    = new StringArray();
        getSourceQueryFromDapQuery(language, userDapQuery,
            resultsVariables,
            constraintVariables, constraintOps, constraintValues); //timeStamp constraints other than regex are epochSeconds

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
            if (edv.sourceDataPAType() == PAType.STRING) {

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
                if ((edv instanceof EDVTimeStamp ets) && !op.equals(PrimitiveArray.REGEX_OP)) { //but if regex, leave as string
                    constraintValues.set(c, 
                        ets.epochSecondsToSourceTimeString(
                            String2.parseDouble(constraintValues.get(c))));
                }

                //finally: put quotes around String constraint "value"
                constraintValues.set(c, "\"" + constraintValues.get(c) + "\"");

            } else {
                //convert time constraints (epochSeconds) to source units
                if ((edv instanceof EDVTimeStamp ets) && !op.equals(PrimitiveArray.REGEX_OP)) { //but if regex, leave as string
                    constraintValues.set(c, 
                        ets.epochSecondsToSourceTimeString(
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
                throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (says DAP)", t);

            //if OutOfMemoryError or too much data, rethrow t
            if (Thread.currentThread().isInterrupted() ||
                t instanceof InterruptedException ||
                t instanceof OutOfMemoryError ||
                tToString.indexOf(Math2.memoryTooMuchData) >= 0 ||
                tToString.indexOf(Math2.TooManyOpenFiles) >= 0)
                throw t;

            //any other error is real trouble
            String2.log(MustBe.throwableToString(t));
            throw t instanceof WaitThenTryAgainException? t : 
                new WaitThenTryAgainException(
                    EDStatic.simpleBilingual(language, EDStatic.waitThenTryAgainAr) + 
                    "\n(" + EDStatic.errorFromDataSource + tToString + ")", t); 
        }
        //String2.log(table.toString());
        standardizeResultsTable(language, requestUrl, userDapQuery, table);
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
     * @return a suggested chunk of xml for this dataset for use in datasets.xml 
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
     *    If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(String tLocalSourceUrl, 
        int tReloadEveryNMinutes, Attributes externalGlobalAttributes) 
        //String outerSequenceName, String innerSequenceName, boolean sortColumnsByName) 
        throws Throwable {

        tLocalSourceUrl = EDStatic.updateUrls(tLocalSourceUrl); //http: to https:
        String2.log("EDDTableFromDapSequence.generateDatasetsXml" +
            "\ntLocalSourceUrl=" + tLocalSourceUrl +
            "\nreloadEveryNMinutes=" + tReloadEveryNMinutes +
            "\nexternalGlobalAttributes=" + externalGlobalAttributes);
        String tPublicSourceUrl = convertToPublicSourceUrl(tLocalSourceUrl);

        //get DConnect
        if (tLocalSourceUrl.endsWith(".html"))
            tLocalSourceUrl = tLocalSourceUrl.substring(0, tLocalSourceUrl.length() - 5);
        DConnect dConnect = new DConnect(tLocalSourceUrl, acceptDeflate, 1, 1);
        DAS das = null;
        try {
            das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
        } catch (Throwable t) {
            throw new RuntimeException("Error while getting DAS from " + tLocalSourceUrl + ".das .", t);
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

        //get source global attributes
        OpendapHelper.getAttributes(das, "GLOBAL", dataSourceTable.globalAttributes());

        //get all of the vars
        String outerSequenceName = null;
        String innerSequenceName = null;
        Enumeration datasetVars = dds.getVariables();
        int nOuterVars = 0; //so outerVars are first in dataAddTable
        Attributes gridMappingAtts = null;
        while (datasetVars.hasMoreElements()) {
            BaseType datasetVar = (BaseType)datasetVars.nextElement();

            //is this the pseudo-data grid_mapping variable?
            if (gridMappingAtts == null) {
                Attributes tSourceAtts = new Attributes();
                OpendapHelper.getAttributes(das, datasetVar.getName(), tSourceAtts);
                gridMappingAtts = NcHelper.getGridMappingAtts(tSourceAtts);
            }

            if (outerSequenceName == null && datasetVar instanceof DSequence outerSequence) {
                outerSequenceName = outerSequence.getName();

                //get list of outerSequence variables
                Enumeration outerVars = outerSequence.getVariables();
                while (outerVars.hasMoreElements()) {
                    BaseType outerVar = (BaseType)outerVars.nextElement();

                    //catch innerSequence
                    if (outerVar instanceof DSequence innerSequence) {
                        if (innerSequenceName == null) {
                            innerSequenceName = outerVar.getName();
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

                                    PrimitiveArray sourcePA = PrimitiveArray.factory(OpendapHelper.getElementPAType(innerVar), 1, false);
                                    if ("true".equals(sourceAtts.getString("_Unsigned")))
                                        sourcePA = sourcePA.makeUnsignedPA();
                                    PrimitiveArray destPA   = (PrimitiveArray)sourcePA.clone();  //!This doesn't handle change in type from scale_factor, add_offset
                                    dataSourceTable.addColumn(dataSourceTable.nColumns(), 
                                        varName, sourcePA, sourceAtts);
                                    dataAddTable.addColumn(dataAddTable.nColumns(), 
                                        varName, destPA, 
                                        makeReadyToUseAddVariableAttributesForDatasetsXml(
                                            dataSourceTable.globalAttributes(),
                                            sourceAtts, null, varName, 
                                            destPA.elementType() != PAType.STRING, //tryToAddStandardName
                                            destPA.elementType() != PAType.STRING, //addColorBarMinMax
                                            true)); //tryToFindLLAT
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

                        PrimitiveArray sourcePA = PrimitiveArray.factory(OpendapHelper.getElementPAType(outerVar), 1, false);
                        if ("true".equals(sourceAtts.getString("_Unsigned")))
                            sourcePA = sourcePA.makeUnsignedPA();
                        PrimitiveArray destPA   = (PrimitiveArray)sourcePA.clone();  //!This doesn't handle change in type from scale_factor, add_offset
                        Attributes addAtts = makeReadyToUseAddVariableAttributesForDatasetsXml(
                            dataSourceTable.globalAttributes(),
                            sourceAtts, null, varName, 
                            destPA.elementType() != PAType.STRING, //tryToAddStandardName
                            destPA.elementType() != PAType.STRING, //addColorBarMinMax
                            true); //tryToFindLLAT
                        dataSourceTable.addColumn(nOuterVars, varName, sourcePA, sourceAtts);
                        dataAddTable.addColumn(   nOuterVars, varName, destPA,   addAtts);
                        nOuterVars++;
                    }
                }
            }
        }

        //add missing_value and/or _FillValue if needed
        addMvFvAttsIfNeeded(dataSourceTable, dataAddTable);

        //tryToFindLLAT
        tryToFindLLAT(dataSourceTable, dataAddTable);

        //don't suggestSubsetVariables(), instead:
        //subset vars are nOuterVars (which are the first nOuterVar columns)
        StringArray tSubsetVariables = new StringArray();
        for (int col = 0; col < nOuterVars; col++)
            tSubsetVariables.add(dataAddTable.getColumnName(col));
        dataAddTable.globalAttributes().add("subsetVariables", tSubsetVariables.toString());

        //get global attributes and ensure required entries are present 
        //after dataVariables known, add global attributes in the dataAddTable
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                hasLonLatTime(dataAddTable)? "Point" : "Other",
                tLocalSourceUrl, externalGlobalAttributes, 
                suggestKeywords(dataSourceTable, dataAddTable)));
        if (outerSequenceName == null)
            throw new SimpleException("No Sequence variable was found for " + tLocalSourceUrl + ".dds.");
        if (gridMappingAtts != null)
            dataAddTable.globalAttributes().add(gridMappingAtts);

        //write the information
        boolean isDapper = tLocalSourceUrl.indexOf("dapper") > 0;
        StringBuilder sb = new StringBuilder();
        sb.append(
            "<dataset type=\"EDDTableFromDapSequence\" datasetID=\"" + 
                suggestDatasetID(tPublicSourceUrl) + 
                "\" active=\"true\">\n" +
            "    <sourceUrl>" + XML.encodeAsXML(tLocalSourceUrl) + "</sourceUrl>\n" +
            "    <outerSequenceName>" + XML.encodeAsXML(outerSequenceName) + "</outerSequenceName>\n" +

                (innerSequenceName == null? "" : 
            "    <innerSequenceName>" + XML.encodeAsXML(innerSequenceName) + "</innerSequenceName>\n") +

            "    <skipDapperSpacerRows>" + isDapper + "</skipDapperSpacerRows>\n" +
            "    <sourceCanConstrainStringEQNE>" + !isDapper + "</sourceCanConstrainStringEQNE>\n" + //DAPPER doesn't support string constraints
            "    <sourceCanConstrainStringGTLT>" + !isDapper + "</sourceCanConstrainStringGTLT>\n" + //see email from Joe Sirott 1/21/2009
            "    <sourceCanConstrainStringRegex></sourceCanConstrainStringRegex>\n" + //was ~=, now ""; see notes.txt for 2009-01-16
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        sb.append(cdmSuggestion());
        sb.append(writeAttsForDatasetsXml(true,     dataAddTable.globalAttributes(), "    "));

        //last 2 params: includeDataType, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, 
            "dataVariable", false, false));
        sb.append(
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
    }



    /**
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        String2.log("\n*** EDDTableFromDapSequence.testGenerateDatasetsXml\n" +
            "This requires testNccsvScalar in localhost ERDDAP.\n");
        testVerboseOn();

        String tUrl = "http://cimt.dyndns.org:8080/dods/drds/vCTD";

String expected = 
"<dataset type=\"EDDTableFromDapSequence\" datasetID=\"dyndns_cimt_8cad_5f3b_717e\" active=\"true\">\n" +
"    <sourceUrl>http://cimt.dyndns.org:8080/dods/drds/vCTD</sourceUrl>\n" +
"    <outerSequenceName>vCTD</outerSequenceName>\n" +
"    <skipDapperSpacerRows>false</skipDapperSpacerRows>\n" +
"    <sourceCanConstrainStringEQNE>true</sourceCanConstrainStringEQNE>\n" +
"    <sourceCanConstrainStringGTLT>true</sourceCanConstrainStringGTLT>\n" +
"    <sourceCanConstrainStringRegex></sourceCanConstrainStringRegex>\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <!-- sourceAttributes>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Other</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n" +
"        <att name=\"creator_name\">DYNDNS CIMT</att>\n" +
"        <att name=\"creator_type\">institution</att>\n" +
"        <att name=\"creator_url\">http://cimt.dyndns.org:8080/dods/drds/vCTD</att>\n" +
"        <att name=\"infoUrl\">http://cimt.dyndns.org:8080/dods/drds/vCTD</att>\n" +
"        <att name=\"institution\">DYNDNS CIMT</att>\n" +
"        <att name=\"keywords\">acceleration, anomaly, average, avg_sound_velocity, center, cimt, cimt.dyndns.org, currents, data, density, depth, dods, drds, dyndns, earth, Earth Science &gt; Oceans &gt; Salinity/Density &gt; Salinity, fluorescence, geopotential, geopotential_anomaly, identifier, integrated, latitude, longitude, marine, ocean, oceans, optical, optical properties, practical, properties, salinity, science, sea, sea_water_practical_salinity, seawater, sigma, sigma_t, sound, station, technology, temperature, time, time2, vctd, vctd.das, velocity, water</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n" +
"        <att name=\"subsetVariables\">time2, latitude, longitude, station, depth, temperature, salinity, fluorescence, avg_sound_velocity, sigma_t, acceleration, geopotential_anomaly</att>\n" +
"        <att name=\"summary\">vCTD. DYNDNS Center for Integrated Marine Technology (CIMT) data from http://cimt.dyndns.org:8080/dods/drds/vCTD.das .</att>\n" +
"        <att name=\"title\">vCTD. DYNDNS CIMT data from http://cimt.dyndns.org:8080/dods/drds/vCTD.das .</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time2</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Description\">A date-time string</att>\n" +
"            <att name=\"Timezone\">GMT</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>latitude</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Description\">Latitude as recorded by GPS</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>longitude</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Description\">Longitude as recorded by GPS</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>station</sourceName>\n" +
"        <destinationName>station</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Description\">CIMT Station ID</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Station</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>depth</sourceName>\n" +
"        <destinationName>depth</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Constants\">salt water, lat=36.9</att>\n" +
"            <att name=\"Description\">Binned depth from the CTD</att>\n" +
"            <att name=\"units\">meters</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-8000.0</att>\n" +
"            <att name=\"colorBarPalette\">TopographyDepth</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Depth</att>\n" +
"            <att name=\"standard_name\">depth</att>\n" +
"            <att name=\"units\">m</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>temperature</sourceName>\n" +
"        <destinationName>temperature</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Description\">Temperature at depth, ITS-90</att>\n" +
"            <att name=\"units\">degrees_Celsius</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">40.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Temperature</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>salinity</sourceName>\n" +
"        <destinationName>salinity</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Description\">Salinity at depth derived from conductivity</att>\n" +
"            <att name=\"units\">Presumed Salinity Units</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">37.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">32.0</att>\n" +
"            <att name=\"ioos_category\">Salinity</att>\n" +
"            <att name=\"long_name\">Sea Water Practical Salinity</att>\n" +
"            <att name=\"standard_name\">sea_water_practical_salinity</att>\n" +
"            <att name=\"units\">PSU</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>fluorescence</sourceName>\n" +
"        <destinationName>fluorescence</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Description\">Fluorescence at depth from the WETStar</att>\n" +
"            <att name=\"units\">nominal mg Chl m/^-3</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Optical Properties</att>\n" +
"            <att name=\"long_name\">Fluorescence</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>avg_sound_velocity</sourceName>\n" +
"        <destinationName>avg_sound_velocity</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Constants\">Chen-Millero, minP=20, minS=20, pWS=20, tWS=60</att>\n" +
"            <att name=\"Description\">Average sound velocity at depth derived from temperature and pressure</att>\n" +
"            <att name=\"units\">m/s</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Currents</att>\n" +
"            <att name=\"long_name\">Avg Sound Velocity</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sigma_t</sourceName>\n" +
"        <destinationName>sigma_t</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Description\">Density (sigma-theta)</att>\n" +
"            <att name=\"units\">Kg/m^-3</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Sigma T</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>acceleration</sourceName>\n" +
"        <destinationName>acceleration</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Constants\">WS=2</att>\n" +
"            <att name=\"Description\">Acceleration</att>\n" +
"            <att name=\"units\">m/s^2</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Acceleration</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>geopotential_anomaly</sourceName>\n" +
"        <destinationName>geopotential_anomaly</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"Description\">Geopotential Anomaly</att>\n" +
"            <att name=\"units\">J/Kg</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">10.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-10.0</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Geopotential Anomaly</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
            String results = generateDatasetsXml(tUrl, 1440, null) + "\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //GenerateDatasetsXml
            String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
                "EDDTableFromDapSequence",
                tUrl, "1440", "-1"}, //defaultStandardizeWhat
                false); //doIt loop?
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

            //EDDGridFromDap should fail and try EDDTableFromDapSequence and generate same result
            results = EDDGridFromDap.generateDatasetsXml(tUrl, null, null, null, 1440, null) + "\n";
            Test.ensureEqual(results, expected, "results=\n" + results);

            //ensure it is ready-to-use by making a dataset from it
            /* This fails because time variable has no units.
            String tDatasetID = "dyndns_cimt_8cad_5f3b_717e";
            EDD.deleteCachedDatasetInfo(tDatasetID);
            EDD edd = oneFromXmlFragment(null, results);
            Test.ensureEqual(edd.datasetID(), tDatasetID, "");
            Test.ensureEqual(edd.title(), "zztop", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "zztop",
                "");
            */
    }

    /**
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml2() throws Throwable {
        String2.log("\n*** EDDTableFromDapSequence.testGenerateDatasetsXml2\n" +
            "This requires testNccsvScalar11 in localhost ERDDAP.\n");
        testVerboseOn();

        try {

        String tUrl = "http://localhost:8080/cwexperimental/tabledap/testNccsvScalar11.html"; //test that it removes .html
        String results = generateDatasetsXml(tUrl, 1440, null) + "\n";

String expected = 
"<dataset type=\"EDDTableFromDapSequence\" datasetID=\"localhost_6f85_6209_a3de\" active=\"true\">\n" +
"    <sourceUrl>http://localhost:8080/cwexperimental/tabledap/testNccsvScalar11</sourceUrl>\n" +
"    <outerSequenceName>s</outerSequenceName>\n" +
"    <skipDapperSpacerRows>false</skipDapperSpacerRows>\n" +
"    <sourceCanConstrainStringEQNE>true</sourceCanConstrainStringEQNE>\n" +
"    <sourceCanConstrainStringGTLT>true</sourceCanConstrainStringGTLT>\n" +
"    <sourceCanConstrainStringRegex></sourceCanConstrainStringRegex>\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <!-- sourceAttributes>\n" +
"        <att name=\"cdm_data_type\">Trajectory</att>\n" +
"        <att name=\"cdm_trajectory_variables\">ship</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n" +
"        <att name=\"creator_email\">bob.simons@noaa.gov</att>\n" +
"        <att name=\"creator_name\">Bob Simons</att>\n" +
"        <att name=\"creator_type\">person</att>\n" +
"        <att name=\"creator_url\">https://www.pfeg.noaa.gov</att>\n" +
"        <att name=\"Easternmost_Easting\" type=\"double\">-130.2576</att>\n" +
"        <att name=\"featureType\">Trajectory</att>\n" +
"        <att name=\"geospatial_lat_max\" type=\"double\">28.0003</att>\n" +
"        <att name=\"geospatial_lat_min\" type=\"double\">27.9998</att>\n" +
"        <att name=\"geospatial_lat_units\">degrees_north</att>\n" +
"        <att name=\"geospatial_lon_max\" type=\"double\">-130.2576</att>\n" +
"        <att name=\"geospatial_lon_min\" type=\"double\">-132.1591</att>\n" +
"        <att name=\"geospatial_lon_units\">degrees_east</att>\n" +
"        <att name=\"history\">";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
        
//        2017-05-05T16:27:08Z (local files)
//2017-05-05T16:27:08Z 
//"http://localhost:8080/cwexperimental/tabledap/testNccsvScalar.das</att>\n" +
expected = 
"        <att name=\"infoUrl\">https://erddap.github.io/NCCSV.html</att>\n" +
"        <att name=\"institution\">NOAA NMFS SWFSC ERD, NOAA PMEL</att>\n" +
"        <att name=\"keywords\">center, data, demonstration, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testLong, time, trajectory</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">&quot;NCCSV Demonstration&quot; by Bob Simons and Steve Hankin is licensed under CC BY 4.0, https://creativecommons.org/licenses/by/4.0/ .</att>\n" +
"        <att name=\"Northernmost_Northing\" type=\"double\">28.0003</att>\n" +
"        <att name=\"sourceUrl\">(local files)</att>\n" +
"        <att name=\"Southernmost_Northing\" type=\"double\">27.9998</att>\n" +
"        <att name=\"standard_name_vocabulary\">CF Standard Name Table v55</att>\n" +
"        <att name=\"subsetVariables\">ship, status, testLong</att>\n" +
"        <att name=\"summary\">This is a paragraph or two describing the dataset.</att>\n" +
"        <att name=\"time_coverage_end\">2017-03-23T23:45:00Z</att>\n" +
"        <att name=\"time_coverage_start\">2017-03-23T00:45:00Z</att>\n" +
"        <att name=\"title\">NCCSV Demonstration</att>\n" +
"        <att name=\"Westernmost_Easting\" type=\"double\">-132.1591</att>\n" +
"    </sourceAttributes -->\n" +
"    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n" +
"        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n" +
"        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n" +
"    -->\n" +
"    <addAttributes>\n" +
"        <att name=\"keywords\">byte, center, data, demonstration, earth, Earth Science &gt; Oceans &gt; Ocean Temperature &gt; Sea Surface Temperature, environmental, erd, fisheries, identifier, laboratory, latitude, long, longitude, longs, marine, national, nccsv, nmfs, noaa, ocean, oceans, pacific, pmel, science, sea, sea_surface_temperature, service, ship, southwest, sst, status, surface, swfsc, temperature, test, testByte, testlong, testnccsvscalar11, testUByte, testULong, time, trajectory, ubyte, ulong</att>\n" +
"        <att name=\"subsetVariables\">ship, time, latitude, longitude, status, testByte, testUByte, testLong, testULong, sst</att>\n" +
"        <att name=\"title\">NCCSV Demonstration (testNccsvScalar11)</att>\n" +
"    </addAttributes>\n" +
"    <dataVariable>\n" +
"        <sourceName>ship</sourceName>\n" +
"        <destinationName>ship</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"cf_role\">trajectory_id</att>\n" +
"            <att name=\"ioos_category\">Identifier</att>\n" +
"            <att name=\"long_name\">Ship</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Time</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">1.4902299E9 1.4903127E9</att>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"ioos_category\">Time</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"time_origin\">01-JAN-1970 00:00:00</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1.49032E9</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">1.49022E9</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>latitude</sourceName>\n" +
"        <destinationName>latitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lat</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">27.9998 28.0003</att>\n" +
"            <att name=\"axis\">Y</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">90.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-90.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Latitude</att>\n" +
"            <att name=\"standard_name\">latitude</att>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>longitude</sourceName>\n" +
"        <destinationName>longitude</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_CoordinateAxisType\">Lon</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">-132.1591 -130.2576</att>\n" +
"            <att name=\"axis\">X</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">180.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-180.0</att>\n" +
"            <att name=\"ioos_category\">Location</att>\n" +
"            <att name=\"long_name\">Longitude</att>\n" +
"            <att name=\"standard_name\">longitude</att>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>status</sourceName>\n" +
"        <destinationName>status</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\">?</att>\n" + //info not transmitted correctly by DAP
"            <att name=\"comment\">From http://some.url.gov/someProjectDocument , Table C</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Status</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"actual_range\">null</att>\n" + 
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>testByte</sourceName>\n" +
"        <destinationName>testByte</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"byte\">127</att>\n" +
"            <att name=\"_Unsigned\">false</att>\n" +        //erddap puts this to say "actually it is signed"
"            <att name=\"actual_range\" type=\"byteList\">-128 126</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"units\">1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">200.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-200.0</att>\n" +
"            <att name=\"long_name\">Test Byte</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>testUByte</sourceName>\n" +
"        <destinationName>testUByte</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
//"            <att name=\"_FillValue\" type=\"byte\">-1</att>\n" +  //source is (erd)dap, which treats bytes as signed
//"            <att name=\"actual_range\" type=\"byteList\">0 -2</att>\n" +
             //but ERDDAP converts source when read
"            <att name=\"_FillValue\" type=\"ubyte\">255</att>\n" +  
"            <att name=\"_Unsigned\">true</att>\n" +                 //erddap puts this to say "no, actually it is unsigned"
"            <att name=\"actual_range\" type=\"ubyteList\">0 254</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"units\">1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">300.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"long_name\">Test UByte</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>testLong</sourceName>\n" +
"        <destinationName>testLong</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">9.223372036854776E18</att>\n" + //long vars appear as double vars in DAP
            //these are the largest longs, converted to doubles
"            <att name=\"actual_range\" type=\"doubleList\">-9.223372036854776E18 9.223372036854776E18</att>\n" + //trouble
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Test of Longs</att>\n" +
"            <att name=\"units\">1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">1.0E19</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">-1.0E19</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>testULong</sourceName>\n" +
"        <destinationName>testULong</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"_FillValue\" type=\"double\">1.8446744073709552E19</att>\n" +
"            <att name=\"actual_range\" type=\"doubleList\">0.0 1.8446744073709552E19</att>\n" +
"            <att name=\"ioos_category\">Unknown</att>\n" +
"            <att name=\"long_name\">Test ULong</att>\n" +
"            <att name=\"units\">1</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">2.0E19</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>sst</sourceName>\n" +
"        <destinationName>sst</destinationName>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"actual_range\" type=\"floatList\">10.0 10.9</att>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">32.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_category\">Temperature</att>\n" +
"            <att name=\"long_name\">Sea Surface Temperature</att>\n" +
"            <att name=\"missing_value\" type=\"float\">99.0</att>\n" +
"            <att name=\"standard_name\">sea_surface_temperature</att>\n" +
"            <att name=\"testBytes\" type=\"byteList\">-128 0 127</att>\n" +
"            <att name=\"testChars\">,\n" +
"&quot;\n" +
"?</att>\n" +   //test of \\u20ac 
"            <att name=\"testDoubles\" type=\"doubleList\">-1.7976931348623157E308 0.0 1.7976931348623157E308</att>\n" +
//??? !!! Unlike Java parseFloat, JDAP reads+/-3.40282345E38 as NaN. !!!
//Hence NaNs here.  This is an unfixed bug (hopefully won't ever affect anyone).
"            <att name=\"testFloats\" type=\"floatList\">NaN 0.0 NaN</att>\n" + 
"            <att name=\"testInts\" type=\"intList\">-2147483648 0 2147483647</att>\n" +
"            <att name=\"testLongs\" type=\"doubleList\">-9.223372036854776E18 -9.007199254740992E15 9.007199254740992E15 9.223372036854776E18 9.223372036854776E18</att>\n" +
"            <att name=\"testShorts\" type=\"shortList\">-32768 0 32767</att>\n" +
"            <att name=\"testStrings\">a&#9;~\u00fc,\n" +
"&#39;z&quot;?</att>\n" +
"            <att name=\"testUBytes\" type=\"byteList\">0 127 -1</att>\n" +  //var is _Unsigned=true and isn't common un/signed att name, so no way to know it should be ubytes
"            <att name=\"testUInts\" type=\"uintList\">0 2147483647 4294967295</att>\n" +
"            <att name=\"testULongs\" type=\"doubleList\">0.0 9.223372036854776E18 1.8446744073709552E19</att>\n" + //long atts appear as double atts
"            <att name=\"testUShorts\" type=\"ushortList\">0 32767 65535</att>\n" +
"            <att name=\"units\">degree_C</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
            int po = results.indexOf(expected.substring(0, 60));
            Test.ensureEqual(results.substring(po), expected, "results=\n" + results);

            //ensure it is ready-to-use by making a dataset from it
            String tDatasetID = "localhost_6f85_6209_a3de";
            EDD.deleteCachedDatasetInfo(tDatasetID);
            EDD edd = oneFromXmlFragment(null, results);
            Test.ensureEqual(edd.datasetID(), tDatasetID, "");
            Test.ensureEqual(edd.title(), "NCCSV Demonstration (testNccsvScalar11)", "");
            Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
                "ship, time, latitude, longitude, status, testByte, testUByte, testLong, testULong, sst",
                "");
        } catch (Throwable t) {
            throw new RuntimeException("This test requires datasetID=testNccsvScalar in localhost ERDDAP.", t); 
        }

    }


    
    public static void testOneTime() throws Throwable {
        testVerboseOn();
        String tName;
        int language = 0;

        if (true) {
            //get empiricalMinMax
            EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "pmelArgoAll"); 
            tedd.getEmpiricalMinMax(language, null, "2007-08-01", "2007-08-10", false, true);
            String tq = "longitude,latitude,id&time>=2008-06-17T16:04:12Z&time<=2008-06-24T16:04:12Z" +
                "&.draw=markers&.marker=5|5&.color=0x000000&.colorBar=|C|Linear|||";
            tName = tedd.makeNewFileForDapQuery(language, null, null, tq, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_GraphArgo", ".png"); 
            Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        }
    
        if (false) {
            //get summary string
            EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "nwioosGroundfish"); 
            String2.log(String2.annotatedString(tedd.combinedGlobalAttributes().getString("summary")));
        }

        if (false) { 
            //graph colorbar range
            EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "pmelArgoAll"); 
            String tq = "longitude,latitude,temp_adjusted&time>=2008-06-27T00:00:00Z" +
                "&.draw=markers&.marker=5|5&.color=0x000000&.colorBar=|C|Linear|0|30|30";
            tName = tedd.makeNewFileForDapQuery(language, null, null, tq, EDStatic.fullTestCacheDirectory, 
                tedd.className() + "_GraphArgo30", ".png"); 
            Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        }

    }

    /**
     * Try to isolate trouble with Argo.
     */
    public static void testArgo() throws Throwable {
        testVerboseOn();
        int language = 0;
        String sourceUrl = "http://dapper.pmel.noaa.gov/dapper/argo/argo_all.cdp"; //no longer running
        String2.log("\n*** EDDTableFromDapSequence.testArgo " + sourceUrl);
        DConnect dConnect = new DConnect(sourceUrl, acceptDeflate, 1, 1);
        String2.log("getDAS");
        DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
        String2.log("getDDS");
        DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);

        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "pmelArgoAll"); 
        String tq = "longitude,latitude,id&id<=1000000&.draw=markers&.marker=4|5&.color=0x000000&.colorBar=|C|Linear|||";
        String tName = tedd.makeNewFileForDapQuery(language, null, null, tq, EDStatic.fullTestCacheDirectory, 
            tedd.className() + "_Argo", ".png"); 
        Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
        tName = tedd.makeNewFileForDapQuery(language, null, null, tq, EDStatic.fullTestCacheDirectory, 
            tedd.className() + "_Argo", ".csv"); 
        String results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
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
        int language = 0;
        String results, query, tName;
        String baseQuery = "time,longitude,latitude,depth,station,waterTemperature,salinity" +
            "&latitude=36.692"; 
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "cimtPsdac");
        String expected = 
"time,longitude,latitude,depth,station,waterTemperature,salinity\n" +
"UTC,degrees_east,degrees_north,m,,degree_C,Presumed Salinity Units\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,1.0,T402,12.8887,33.8966\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,2.0,T402,12.8272,33.8937\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,3.0,T402,12.8125,33.8898\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,4.0,T402,12.7125,33.8487\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,5.0,T402,12.4326,33.8241\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,6.0,T402,12.1666,33.8349\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,7.0,T402,11.9364,33.8159\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,8.0,T402,11.7206,33.8039\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,9.0,T402,11.511,33.8271\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,10.0,T402,11.4064,33.853\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,11.0,T402,11.3552,33.8502\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,12.0,T402,11.2519,33.8607\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,13.0,T402,11.1777,33.8655\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,14.0,T402,11.1381,33.8785\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,15.0,T402,11.0643,33.8768\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,16.0,T402,10.9416,33.8537\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,17.0,T402,10.809,33.8379\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,18.0,T402,10.7034,33.8593\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,19.0,T402,10.6502,33.8476\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,20.0,T402,10.5257,33.8174\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,21.0,T402,10.2857,33.831\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,22.0,T402,10.0717,33.8511\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,23.0,T402,9.9577,33.8557\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,24.0,T402,9.8876,33.8614\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,25.0,T402,9.842,33.8757\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,26.0,T402,9.7788,33.8904\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,27.0,T402,9.7224,33.8982\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,28.0,T402,9.695,33.9038\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,29.0,T402,9.6751,33.9013\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,30.0,T402,9.6462,33.9061\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,31.0,T402,9.6088,33.9069\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,32.0,T402,9.5447,33.9145\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,33.0,T402,9.4887,33.9263\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,34.0,T402,9.4514,33.9333\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,35.0,T402,9.4253,33.9358\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,36.0,T402,9.397,33.9387\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,37.0,T402,9.3795,33.9479\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,38.0,T402,9.3437,33.9475\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,39.0,T402,9.2946,33.9494\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,40.0,T402,9.2339,33.9458\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,41.0,T402,9.1812,33.9468\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,42.0,T402,9.153,33.9548\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,43.0,T402,9.1294,33.9615\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,44.0,T402,9.1048,33.9652\n" +
"2002-06-25T14:55:00Z,-121.845,36.692,45.0,T402,9.0566,33.9762\n";

        //the basicQuery
        tName = tedd.makeNewFileForDapQuery(language, null, null, baseQuery, EDStatic.fullTestCacheDirectory, 
            tedd.className() + "_psdac", ".csv"); 
        results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        //String2.log(results);
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //basicQuery + String= constraint that shouldn't change the results
        tName = tedd.makeNewFileForDapQuery(language, null, null, baseQuery + "&station=\"T402\"", 
            EDStatic.fullTestCacheDirectory, tedd.className() + "_psdacNonTime", ".csv"); 
        results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        Test.ensureEqual(results, expected, "results=\n" + results);      
        
        //basicQuery + String> String< constraints that shouldn't change the results
        tName = tedd.makeNewFileForDapQuery(language, null, null, baseQuery + "&station>\"T3\"&station<\"T5\"", 
            EDStatic.fullTestCacheDirectory, tedd.className() + "_psdacGTLT", ".csv"); 
        results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        Test.ensureEqual(results, expected, "results=\n" + results);      
       
        //REGEX: If dataset is setup with sourceCanConstraintStringRegex ~=, THIS WORKS SO SOURCE REGEX PARTLY WORKS 
        //basicQuery + String regex constraint (ERDDAP handles it) that shouldn't change the results
        //This succeeds with source not handling regex, so leave test active.
            //always =~ (regardless of what source needs) because this is an erddap request
        tName = tedd.makeNewFileForDapQuery(language, null, null, baseQuery + "&station=~\"T40.\"", 
            EDStatic.fullTestCacheDirectory, tedd.className() + "_psdacRegex", ".csv"); 
        results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //REGEX: If dataset is setup with sourceCanConstraintStringRegex ~=, THIS DOESN'T WORK.
        //SO SOURCE REGEX SUPPORT IS LIMITED, SO DON'T RELY ON SOURCE HANDLING REGEX
        //basicQuery + String regex constraint (ERDDAP handles it) that shouldn't change the results
        //This succeeds with source not handling regex, so leave test active.
            //always =~ (regardless of what source needs) because this is an erddap request
        tName = tedd.makeNewFileForDapQuery(language, null, null, baseQuery + "&station=~\"(T402|t403)\"", 
            EDStatic.fullTestCacheDirectory, tedd.className() + "_psdacRegex", ".csv"); 
        results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        Test.ensureEqual(results, expected, "results=\n" + results);      

        //basicQuery + time= (a string= test) constraint that shouldn't change the results
        tName = tedd.makeNewFileForDapQuery(language, null, null, baseQuery + "&time=2002-06-25T14:55:00Z", 
            EDStatic.fullTestCacheDirectory, tedd.className() + "_psdacTime", ".csv"); 
        results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        Test.ensureEqual(results, expected, "results=\n" + results);      
    }


    /**
     */
    public static void testErdlasNewportCtd() throws Throwable {
        testVerboseOn();
        int language = 0;
        String results, query, tName, expected;
        String baseQuery = "&time>=2006-08-07T00&time<2006-08-08"; 
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "erdlasNewportCtd");

        //the basicQuery
        tName = tedd.makeNewFileForDapQuery(language, null, null, baseQuery, EDStatic.fullTestCacheDirectory, 
            tedd.className() + "_newport", ".csv"); 
        results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

    }

    /**
     * NOT FINISHED.
     */
    public static void testDapErdlasNewportCtd() throws Throwable {
        testVerboseOn();
        int language = 0;

        //the basicQuery
        for (int test = 1; test < 2; test++) {
            String url = test == 0? 
                "https://oceanwatch.pfeg.noaa.gov:8080/dods/GLOBEC/GLOBEC_birds?birds.year,birds.species,birds.head_c,birds.month_local,birds.day_local&birds.year=2000&birds.month_local=8&birds.day_local=7" :
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
                    if      (obt instanceof DByte     t) sb.append(t.getValue());
                    else if (obt instanceof DFloat32  t) sb.append(t.getValue());
                    else if (obt instanceof DFloat64  t) sb.append(t.getValue());
                    else if (obt instanceof DInt16    t) sb.append(t.getValue());
                    else if (obt instanceof DUInt16   t) sb.append(t.getValue());
                    else if (obt instanceof DInt32    t) sb.append(t.getValue());
                    else if (obt instanceof DUInt32   t) sb.append(t.getValue());
                    else if (obt instanceof DBoolean  t) sb.append(t.getValue());
                    else if (obt instanceof DString   t) sb.append(t.getValue());
                    else if (obt instanceof DSequence t) sb.append("DSequence)");
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
        int language = 0;
        String results, query, tName, expected;
        String baseQuery = "&time>=2006-01-01"; 
        EDDTable tedd = (EDDTable)oneFromDatasetsXml(null, "erdlasCalCatch");

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
        tName = tedd.makeNewFileForDapQuery(language, null, null, baseQuery, EDStatic.fullTestCacheDirectory, 
            tedd.className() + "_CalCaltch", ".csv"); 
        results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
        expected = 
"\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      

    }


    /** This tests sourceNeedsExpandedFP_EQ. 
     * 2016-01-16 SOURCE IS GONE.
     */
    public static void testSourceNeedsExpandedFP_EQ() throws Throwable {
        String2.log("\n*** EDDTableFromDapSequence.testSourceNeedsExpandedFP_EQ\n");
        testVerboseOn();
        int language = 0;
        String results, query, tName, expected;
        try {
            EDDTable edd = (EDDTable)oneFromDatasetsXml(null, "nwioosGroundfish"); 

            //the basicQuery
            //[was: test a TableWriter that doesn't convert time to iso format
            // now year is converted to time.]
            query = "longitude,latitude,time,common_name&longitude=-124.348098754882&latitude=44.690254211425";             
           
            tName = edd.makeNewFileForDapQuery(language, null, null, query, EDStatic.fullTestCacheDirectory, 
                edd.className() + "_FP_EQ", ".csv"); 
            results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
            expected = //pre 2015-12-28 was sorted lexically, now case insensitive. pre 2013-05-28 wasn't sorted
"longitude,latitude,time,common_name\n" +
"degrees_east,degrees_north,UTC,\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,arrowtooth flounder\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,bocaccio\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,canary rockfish\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,chilipepper\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,cowcod\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,darkblotched rockfish\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,Dover sole\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,English sole\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,longspine thornyhead\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,Pacific ocean perch\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,petrale sole\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,sablefish\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,shortspine thornyhead\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,widow rockfish\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,yelloweye rockfish\n" +
"-124.34809875488281,44.69025421142578,2005-01-01T00:00:00Z,yellowtail rockfish\n";
            Test.ensureEqual(results, expected, "results=\n" + results);      
           
        } catch (Throwable t) {
            throw new RuntimeException("2016-01-16 SOURCE IS GONE.", t);
        }
    }

    /** NOT FINISHED.  This tests nosCoopsRWL. */
    public static void testNosCoopsRWL() throws Throwable {
        String2.log("\n*** EDDTableFromDapSequence.testNosCoopsRWL\n");
        testVerboseOn();
        int language = 0;
        String results, query, tName, expected;
        String today     = Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(1, Double.NaN));
        String yesterday = Calendar2.epochSecondsToIsoStringTZ(Calendar2.backNDays(2, Double.NaN));

        EDDTable edd = (EDDTable)oneFromDatasetsXml(null, "nosCoopsRWL"); 

        //*** test a TableWriter that doesn't convert time to iso format
        query = "&station=\"1612340\"&datum=\"MLLW\"&beginTime=" + yesterday + "&endTime=" + today;             
//https://opendap.co-ops.nos.noaa.gov/dods/IOOS/SixMin_Verified_Water_Level.ascii?
//&WATERLEVEL_6MIN_VFD_PX._STATION_ID="1612340"&WATERLEVEL_6MIN_VFD_PX._DATUM="MLLW"
//&WATERLEVEL_6MIN_VFD_PX._BEGIN_DATE="20100825"&WATERLEVEL_6MIN_VFD_PX._END_DATE="20100826"            
        tName = edd.makeNewFileForDapQuery(language, null, null, query, EDStatic.fullTestCacheDirectory, 
            edd.className() + "_RWL", ".csv"); 
        results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
//trouble: java.time (was Joda) doesn't like space-padded hour values

        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "results=\n" + results);      
       
    }

    /** Test reading .das */
    public static void testReadDas() throws Exception {
        String2.log("\n*** EDDTableFromDapSequence.testReadDas\n");
        String url = "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGtsppBest";
        int language = 0;
        try {
            DConnect dConnect = new DConnect(url, true, 1, 1);
            DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
            DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
        } catch (Throwable t) {
            throw new RuntimeException("Unexpected error for testReadDas." +
                "\nNote: this test requires erdGtsppBest on coastwatch's ERDDAP:" +
                "\nurl=" + url, t); 
        }
    }

    /** Test graph made from subsetVariables data */
    public static void testSubsetVariablesGraph() throws Exception {
        String2.log("\n*** EDDTableFromDapSequence.testSubsetVariablesGraph\n");
        int language = 0;
        try {
            EDDTable edd = (EDDTable)oneFromDatasetsXml(null, "nwioosCoral"); 

            String tName = edd.makeNewFileForDapQuery(language, null, null, 
                "longitude,latitude,time&time=%221992-01-01T00:00:00Z%22" +
                "&longitude>=-132.0&longitude<=-112.0&latitude>=30.0&latitude<=50.0" +
                "&distinct()&.draw=markers&.colorBar=|D||||", 
                EDStatic.fullTestCacheDirectory, edd.className() + "_SVGraph", ".png"); 
            Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);

        } catch (Throwable t) {
            throw new RuntimeException("2016-01-16 THE DATA SOURCE IS GONE.", t); 
        }
    }

    /** Test that info from subsetVariables gets back to variable's ranges */
    public static void testSubsetVariablesRange() throws Throwable {
        String2.log("\n*** EDDTableFromDapSequence.testSubsetVariablesRange\n");
        int language = 0;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //before I fixed this, time had destinationMin/Max = NaN
        try {
        EDDTable edd = (EDDTable)oneFromDatasetsXml(null, "nwioosCoral"); 
        EDV edvTime = edd.dataVariables()[edd.timeIndex];
        Test.ensureEqual(edvTime.destinationMinDouble(), 3.155328E8,  "");
        Test.ensureEqual(edvTime.destinationMaxDouble(), 1.1045376E9, "");

        String tName = edd.makeNewFileForDapQuery(language, null, null, "", EDStatic.fullTestCacheDirectory, 
            edd.className() + "_Entire", ".das"); 
        String results = String2.annotatedString(File2.directReadFrom88591File(
            EDStatic.fullTestCacheDirectory + tName));
        String tResults;
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
"  depth {[10]\n" +
"    String _CoordinateAxisType \"Height\";[10]\n" +
"    String _CoordinateZisPositive \"down\";[10]\n" +
"    Float64 actual_range 11.0, 1543.0;[10]\n" +
"    String axis \"Z\";[10]\n" +
"    Float64 colorBarMaximum 1500.0;[10]\n" +
"    Float64 colorBarMinimum 0.0;[10]\n" +
"    String ioos_category \"Location\";[10]\n" +
"    String long_name \"Depth\";[10]\n" +
"    String positive \"down\";[10]\n" +
"    String standard_name \"depth\";[10]\n" +
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
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";[10]\n" +
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
"    String geospatial_vertical_positive \"down\";[10]\n" +
"    String geospatial_vertical_units \"m\";[10]\n" +
"    String history \"" + today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        
//+ " http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005[10]\n" +
//today + " http://localhost:8080/cwexperimental/
expected = 
"tabledap/nwioosCoral.das\";[10]\n" +
"    String infoUrl \"http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005.info\";[10]\n" +
"    String institution \"NOAA NWFSC\";[10]\n" +
"    String keywords \"1980-2005, abbreviation, atmosphere, beginning, coast, code, collected, coral, data, depth, Earth Science > Biological Classification > Animals/Invertebrates > Cnidarians > Anthozoans/Hexacorals > Hard Or Stony Corals, Earth Science > Biosphere > Aquatic Ecosystems > Coastal Habitat, Earth Science > Biosphere > Aquatic Ecosystems > Marine Habitat, family, genus, height, identifier, institution, noaa, nwfsc, off, order, scientific, species, station, survey, taxa, taxonomic, taxonomy, time, west, west coast, year\";[10]\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";[10]\n" +
"    String license \"The data may be used and redistributed for free but is not intended[10]\n" +
"for legal use, since it may contain inaccuracies. Neither the data[10]\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any[10]\n" +
"of their employees or contractors, makes any warranty, express or[10]\n" +
"implied, including warranties of merchantability and fitness for a[10]\n" +
"particular purpose, or assumes any legal liability for the accuracy,[10]\n" +
"completeness, or usefulness, of this information.\";[10]\n" +
"    Float64 Northernmost_Northing 48.969085693359375;[10]\n" +
"    String sourceUrl \"http://nwioos.coas.oregonstate.edu:8080/dods/drds/Coral%201980-2005\";[10]\n" +
"    Float64 Southernmost_Northing 32.570838928222656;[10]\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v55\";[10]\n" +
"    String subsetVariables \"longitude, latitude, depth, time, institution, institution_id, species_code, taxa_scientific, taxonomic_order, order_abbreviation, taxonomic_family, family_abbreviation, taxonomic_genus\";[10]\n" +
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
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);

        } catch (Throwable t) {
            throw new RuntimeException("2016-01-16 THE DATA SOURCE IS GONE.", t); 
        }
    }

    /**
     * This runs all of the interactive or not interactive tests for this class.
     *
     * @param errorSB all caught exceptions are logged to this.
     * @param interactive  If true, this runs all of the interactive tests; 
     *   otherwise, this runs all of the non-interactive tests.
     * @param doSlowTestsToo If true, this runs the slow tests, too.
     * @param firstTest The first test to be run (0...).  Test numbers may change.
     * @param lastTest The last test to be run, inclusive (0..., or -1 for the last test). 
     *   Test numbers may change.
     */
    public static void test(StringBuilder errorSB, boolean interactive, 
        boolean doSlowTestsToo, int firstTest, int lastTest) {
        if (lastTest < 0)
            lastTest = interactive? -1 : 3;
        String msg = "\n^^^ EDDTableFromDapSequence.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    //if (test ==  0) testGenerateDatasetsXml(); 2020-08-27 source is gone
                    if (test ==  1) testGenerateDatasetsXml2();  //trouble: unsigned: needs work
                    //if (test ==  2) testPsdac(); 2020-08-27 source is gone
                    if (test ==  3) testReadDas();

                    //if (test ==  7) testSourceNeedsExpandedFP_EQ(); 2016-01-16 source is gone
                    //if (test ==  8) testSubsetVariablesGraph(); 2016-01-16 source is gone
                    //if (test ==  9) testSubsetVariablesRange();  2016-01-16 source is gone

                    //if (test == 10) testErdlasNewportCtd();   //not yet working
                    //if (test == 11) testErdlasCalCatch();     //not yet working
                    //if (test == 12) testReadPngInfo();        //needs work
               

                    //not usually done
                    //if (test == 1000) testOneTime();

                    //not done
                    //Tests of DAPPER were removed 2012-10-10. DAPPER was shut down recently.
                    //  https://www.pmel.noaa.gov/epic/dapper_dchart/unsupported.html
                }

                String2.log(msg + test + " finished successfully in " + (System.currentTimeMillis() - time) + " ms.");
            } catch (Throwable testThrowable) {
                String eMsg = msg + test + " caught throwable:\n" + 
                    MustBe.throwableToString(testThrowable);
                errorSB.append(eMsg);
                String2.log(eMsg);
                if (interactive) 
                    String2.pressEnterToContinue("");
            }
        }
    }

}
