/* 
 * EDDTableFromBMDE Copyright 2008, NOAA.
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

import gov.noaa.pfel.coastwatch.griddata.FileNameUtility;
import gov.noaa.pfel.coastwatch.pointdata.DigirHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.pointdata.TableXmlHandler;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.util.ArrayList;
import java.util.Arrays;

/** 
 * THIS USED TO WORK BUT IS NOW INACTIVE SINCE THERE ARE NO KNOWN VALID DATASETS.
 * IF YOU WANT TO USE THIS, TEST IT THOROUGHLY.
 * This class represents a table of data from a DiGIR/BMDE (Bird Monitoring Data Exchange) source.
 * This class is very very similar to EDDTableFromOBIS, since both use DiGIR servers,
 * just different schemas.

 * See http://www.avianknowledge.net for more information.
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2008-07-18
 */
public class EDDTableFromBMDE extends EDDTable{ 

    protected String sourceCode;

    public final static String STANDARD_INFO_URL = "http://www.avianknowledge.net";

    public final static String BMDE_SUMMARY = 
"DiGIR is an engine which takes XML requests for data and returns a data \n" +
"subset stored as XML data (as defined in a schema). For more DiGIR \n" +
"information, see http://digir.sourceforge.net/ , \n" +
"http://diveintodigir.ecoforge.net/draft/digirdive.html , \n" +
"and http://digir.net/prov/prov_manual.html . \n" +
"A list of Digir providers is at \n" +
"http://bigdig.ecoforge.net/wiki/SchemaStatus . \n" +
"\n" +
"The Bird Monitoring Data Exchange (BMDE) schema defines a standard \n" +
"data table with bird monitoring information. \n" +
"See the BMDE schema at " + DigirHelper.BMDE_XSD + " . \n" +
"For more BMDE info, see http://www.avianknowledge.net . \n" +
"\n" +
//"Queries: Although BMDE datasets have many variables, most variables \n" +
//"have few values.  The only queries that are likely to succeed SHOULD \n" +
//"include a constraint for some combination of ScientificName=, Class=, \n" +
//"Order=, Family=, Genus=, longitude, latitude, and time.\n" +
//"\n" +
"Most BMDE datasets return a maximum of 10000 rows of data per request.";


    /**
     * This constructs an EDDTableFromBMDE based on the information in an .xml file.
     * 
     * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromBMDE"&gt;
     *    having just been read.  
     * @return an EDDTableFromBMDE.
     *    When this returns, xmlReader will have just read &lt;erddapDatasets&gt;&lt;/dataset&gt; .
     * @throws Throwable if trouble
     */
    public static EDDTableFromBMDE fromXml(SimpleXMLReader xmlReader) throws Throwable {

        //data to be obtained (or not)
        if (verbose) String2.log("\n*** constructing EDDTableFromBMDE(xmlReader)...");
        String tDatasetID = xmlReader.attributeValue("datasetID"); 
        int tReloadEveryNMinutes = Integer.MAX_VALUE;
        String tAccessibleTo = null;
        StringArray tOnChange = new StringArray();
        String tFgdcFile = null;
        String tIso19115File = null;
        String tSosOfferingPrefix = null;
        Attributes tGlobalAttributes = null;
        String tLocalSourceUrl = null, tSourceCode = null;
        ArrayList tDataVariables = new ArrayList();
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
            if      (localTags.equals("<addAttributes>"))
                tGlobalAttributes = getAttributesFromXml(xmlReader);
            else if (localTags.equals( "<altitudeMetersPerSourceUnit>")) 
                throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
            else if (localTags.equals( "<accessibleTo>")) {}
            else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
            else if (localTags.equals( "<reloadEveryNMinutes>")) {}
            else if (localTags.equals("</reloadEveryNMinutes>")) tReloadEveryNMinutes = String2.parseInt(content); 
            else if (localTags.equals( "<sourceUrl>")) {}
            else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content; 
            else if (localTags.equals( "<sourceCode>")) {}
            else if (localTags.equals("</sourceCode>")) tSourceCode = content; 
            else if (localTags.equals( "<dataVariable>")) 
                tDataVariables.add(getSDADVariableFromXml(xmlReader));           
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

            else xmlReader.unexpectedTagException();
        }
        int ndv = tDataVariables.size();
        Object ttDataVariables[][] = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++)
            ttDataVariables[i] = (Object[])tDataVariables.get(i);

        return new EDDTableFromBMDE(tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
            tDefaultDataQuery, tDefaultGraphQuery, tGlobalAttributes,
            ttDataVariables, tReloadEveryNMinutes, tLocalSourceUrl, tSourceCode);
    }

    /**
     * The constructor.  
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
     *   <li> "institution" - the source of the data 
     *      (best if &lt; 50 characters so it fits in a graph's legend).
     *   </ul>
     *   <br>The constructor sets "cdm_data_type" to CDM_POINT.
     *   <br>If not present, the standard "infoUrl" will be added
     *      (a url with information about this data set).
     *   <br>Special case: If not present: keywords="Biological Classification > Animals/Vertebrates > Birds"
     *      will be added.
     *   <br>Special case: If not present, the standard "BMDE_SUMMARY" will be added 
     *      (a longer description of the dataset; it may have newline characters (usually at &lt;= 72 chars per line)).
     *      If summary is present, the standard BMDE_SUMMARY will be substituted for "[BMDE_SUMMARY]".
     *   <br>Special case: value="null" causes that item to be removed from combinedGlobalAttributes.
     *   <br>Special case: addGlobalAttributes must have a name="creator_email" value=AnEmailAddress
     *     for users to contact regarding publications that use the data in order 
     *     to comply with license.
     *   <br>(Currentln not true:)Special case: I manually add the list of available "Genus" values 
     *     to the "summary" metadata. I get the list from DigirHelper.getBmdeInventoryString(),
     *     specifically some one-time code in DigirHelper.test().
     * @param tDataVariables is an Object[nDataVariables][3]
     *    OR: specify with nDataVariables=0 to get all of the BMDE variables.
     *    <br>[0]=String sourceName (the name of the data variable in the dataset source, 
     *         without the "bmde:" prefix),
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
     *        http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html or 
     *        http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html).
     *      </ul>
     * @param tReloadEveryNMinutes indicates how often the source should
     *    be checked for new data.
     * @param tLocalSourceUrl the url to which requests are sent
     *    e.g., http://digir.prbo.org/digir/DiGIR.php
     * @param tSourceCode the BMDE name for the source, e.g., prbo05.
     *    If you read the xml response from the sourceUrl, this is the
     *    name from the &lt;resource&gt;&lt;code&gt; tag.
     * @throws Throwable if trouble
     */
    public EDDTableFromBMDE(String tDatasetID, String tAccessibleTo, 
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tSosOfferingPrefix;
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object tDataVariables[][],
        int tReloadEveryNMinutes,
        String tLocalSourceUrl, String tSourceCode) throws Throwable {

        if (verbose) String2.log(
            "\n*** constructing EDDTableFromBMDE " + tDatasetID); 
        long constructionStartMillis = System.currentTimeMillis();
        String errorInMethod = "Error in EDDTableFromBMDE(" + 
            tDatasetID + ") constructor:\n";
            
        //save some of the parameters
        className = "EDDTableFromBMDE"; 
        datasetID = tDatasetID;
        setAccessibleTo(tAccessibleTo);
        onChange = tOnChange;
        fgdcFile = tFgdcFile;
        iso19115File = tIso19115File;
        sosOfferingPrefix = tSosOfferingPrefix;
        defaultDataQuery = tDefaultDataQuery;
        defaultGraphQuery = tDefaultGraphQuery;
        setReloadEveryNMinutes(tReloadEveryNMinutes);
        if (tAddGlobalAttributes == null)
            tAddGlobalAttributes = new Attributes();
        if (tAddGlobalAttributes.getString("infoUrl") == null) tAddGlobalAttributes.add("infoUrl", STANDARD_INFO_URL);
        String tSummary = tAddGlobalAttributes.getString("summary");
        tAddGlobalAttributes.add("summary", 
            tSummary == null? BMDE_SUMMARY :
            String2.replaceAll(tSummary, "[BMDE_SUMMARY]", BMDE_SUMMARY));
        if (tAddGlobalAttributes.getString("keywords") == null)
            tAddGlobalAttributes.add("keywords", "Biological Classification > Animals/Vertebrates > Birds");
        //String tCreator_email = tAddGlobalAttributes.getString("creator_email");
        //Test.ensureNotNothing(tCreator_email, 
        //    "The global addAttributes must include 'creator_email' for users to contact regarding " +
        //    "publications that use the data in order to comply with license.");
        tAddGlobalAttributes.add("cdm_data_type", CDM_POINT);
        tAddGlobalAttributes.add("standard_name_vocabulary", FileNameUtility.getStandardNameVocabulary());
        addGlobalAttributes = tAddGlobalAttributes;
        addGlobalAttributes.set("sourceUrl", makePublicSourceUrl(tLocalSourceUrl));
        localSourceUrl = tLocalSourceUrl;
        sourceCode = tSourceCode;

        sourceGlobalAttributes = new Attributes();
        combinedGlobalAttributes = new Attributes(addGlobalAttributes, sourceGlobalAttributes); //order is important
        String tLicense = combinedGlobalAttributes.getString("license");
        if (tLicense != null)
            combinedGlobalAttributes.set("license", 
                String2.replaceAll(tLicense, "[standard]", EDStatic.standardLicense));
        combinedGlobalAttributes.removeValue("null");

        //sourceCanConstrain:
        sourceNeedsExpandedFP_EQ = true;
        sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //everything, partial to be safe
        sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; //only = !=
        sourceCanConstrainStringRegex = ""; //Digir has simplistic regex support

        //make list of sourceNames to be created 
        String tSourceNames[];
        String tDestNames[];
        Attributes tAddAtts[];
        String bmdePre = DigirHelper.BMDE_PREFIX + ":";
        if (tDataVariables == null || tDataVariables.length == 0) {
            //get the original BMDE variable names    (remove prefix so sortable)
            String tOrigNames[] = DigirHelper.getBmdeVariables(); //don't modify these!
            tSourceNames = new String[tOrigNames.length];
            tDestNames   = new String[tOrigNames.length];
            tAddAtts     = new Attributes[tOrigNames.length];
            for (int v = 0; v < tSourceNames.length; v++) {
                if (tOrigNames[v].startsWith(bmdePre))   
                    tSourceNames[v] = tOrigNames[v].substring(bmdePre.length());
                else throw new IllegalArgumentException("Unexpected prefix for origName=" + tOrigNames[v]);
                tDestNames[v] = null;
                tAddAtts[v] = null;
            }
            Arrays.sort(tSourceNames); //tDestNames and tAddAtts are null, so don't need to sort together

            //identify LLAT vars
            String fixedVarNames[] = {"DecimalLongitude", "DecimalLatitude", 
                "MinimumElevationInMeters", "ObservationDate"}; 
            tDestNames[String2.indexOf(tSourceNames, fixedVarNames[0])] = EDV.LON_NAME;
            tDestNames[String2.indexOf(tSourceNames, fixedVarNames[1])] = EDV.LAT_NAME;
            tDestNames[String2.indexOf(tSourceNames, fixedVarNames[2])] = EDV.ALT_NAME;
            tDestNames[String2.indexOf(tSourceNames, fixedVarNames[3])] = EDV.TIME_NAME;
        } else {
            tSourceNames = new String[tDataVariables.length];
            tDestNames   = new String[tDataVariables.length];
            tAddAtts     = new Attributes[tDataVariables.length];
            for (int v = 0; v < tSourceNames.length; v++) {
                tSourceNames[v] = (String)tDataVariables[v][0];
                tDestNames[v]   = (String)tDataVariables[v][1];
                tAddAtts[v]     = (Attributes)tDataVariables[v][2];
            }
        }

        //make the variables
        //??? !!! PROBLEM: constructor can make dataset with all bmde vars, 
        //but a data request for all variables returns HTTP error 414 - url too long.
        dataVariables = new EDV[tSourceNames.length];  
        for (int v = 0; v < tSourceNames.length; v++) {

            //get info
            String info = DigirHelper.digirBmdeProperties.getString(tSourceNames[v], null);
            Test.ensureNotNull(info, errorInMethod + "No info found for variable=" + tSourceNames[v]);
            String infoArray[] = String2.split(info, '\f');
            Test.ensureTrue(infoArray.length == 2, 
                errorInMethod + "Info for variable=" + tSourceNames[v] + 
                    " in digirBmde.properties" +
                    " should have one \\f to separate the datatype from the attributes.");

            //get tDestName
            String tDestName = tDestNames[v] == null || tDestNames[v].length() == 0?
                tSourceNames[v] : tDestNames[v];

            //get tSourceType
            String tSourceType = infoArray[0];
            boolean isTimeStamp = tSourceType.equals("dateTime");
            if (isTimeStamp) 
                tSourceType = "String";

            //get sourceAtt
            Attributes tSourceAtt = new Attributes();
            String metadata[] = String2.split(infoArray[1], '`'); 
            for (int i = 0; i < metadata.length; i++) { 
                int eqPo = metadata[i].indexOf('=');  //first instance of '='
                Test.ensureTrue(eqPo > 0, errorInMethod + 
                    "Invalid metadata for " + bmdePre + tSourceNames[v] + " : " + metadata[i]);
                tSourceAtt.set(metadata[i].substring(0, eqPo), 
                    metadata[i].substring(eqPo + 1));
            }

            //handle LLAT
            if (tDestName.equals(EDV.LON_NAME)) {
                lonIndex = v;
                dataVariables[lonIndex] = new EDVLon(bmdePre + tSourceNames[v],
                    tSourceAtt, tAddAtts[v], tSourceType, Double.NaN, Double.NaN);
            } else if (tDestName.equals(EDV.LAT_NAME)) {
                latIndex = v;
                dataVariables[latIndex] = new EDVLat(bmdePre + tSourceNames[v],
                    tSourceAtt, tAddAtts[v], tSourceType, Double.NaN, Double.NaN);
            } else if (tDestName.equals(EDV.ALT_NAME)) {
                altIndex = v;
                dataVariables[altIndex] = new EDVAlt(bmdePre + tSourceNames[v],
                    tSourceAtt, tAddAtts[v], tSourceType, Double.NaN, Double.NaN);
            } else if (tDestName.equals(EDV.DEPTH_NAME)) {
                depthIndex = v;
                dataVariables[depthIndex] = new EDVDepth(bmdePre + tSourceNames[v],
                    tSourceAtt, tAddAtts[v], tSourceType, Double.NaN, Double.NaN);
            } else if (tDestName.equals(EDV.TIME_NAME)) {  //look for TIME_NAME before check isTimeStamp (next)
                timeIndex = v;
                dataVariables[timeIndex] = new EDVTime(bmdePre + tSourceNames[v],
                    tSourceAtt, tAddAtts[v], tSourceType);  //this constructor gets source / sets destination actual_range
            } else if (isTimeStamp) {
                dataVariables[v] = new EDVTimeStamp(bmdePre + tSourceNames[v], tDestName,
                    tSourceAtt, tAddAtts[v], tSourceType);  //this constructor gets source / sets destination actual_range
            } else {
                //handle other variables            
                //if (reallyVerbose) String2.log("v=" + v + " source=" + prefix+tSourceName + 
                //    " destName=" + tDestName + " type=" + tSourceType + " sourceAtt=\n" + tSourceAtt + 
                //    " addAtt=\n" + tAddAtts[v]);

                //make the variable
                dataVariables[v] = new EDV(bmdePre + tSourceNames[v], tDestName,
                    tSourceAtt, tAddAtts[v], tSourceType); //the constructor that reads actual_range
                dataVariables[v].setActualRangeFromDestinationMinMax();
            }
        }

        //ensure the setup is valid
        ensureValid();

        //finally
        if (verbose) String2.log(
            (reallyVerbose? "\n" + toString() : "") +
            "\n*** EDDTableFromBMDE " + datasetID + " constructor finished. TIME=" + 
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
     * @throws Throwable if trouble (notably, WaitThenTryAgainException)
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
        //sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //everything, partial to be safe
        //sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; //only = !=
        //sourceCanConstrainStringRegex = ""; //Digir has simplistic regex support
        //work backwards since deleting some
        for (int c = constraintVariables.size() - 1; c >= 0; c--) { 
            String constraintVariable = constraintVariables.get(c);
            int dv = String2.indexOf(dataVariableSourceNames(), constraintVariable);
            EDV edv = dataVariables[dv];

            if (edv.sourceDataTypeClass() == String.class &&
                String2.indexOf(GTLT_OPERATORS, constraintOps.get(c)) >= 0) {
                //remove >, >=, <, <= ops for String variables
                constraintVariables.remove(c);
                constraintOps.remove(c);
                constraintValues.remove(c);
                continue;
            }

            //convert numeric time constraints (epochSeconds) to source format
            if ((edv instanceof EDVTimeStamp) && 
                !constraintOps.get(c).equals(PrimitiveArray.REGEX_OP)) { //but if regex, leave as string
                constraintValues.set(c, 
                    ((EDVTimeStamp)edv).epochSecondsToSourceTimeString(
                        String2.parseDouble(constraintValues.get(c))));
            }


        }

        //convert constraintOps to words
        for (int i = 0; i < constraintOps.size(); i++) {
            int po = String2.indexOf(DigirHelper.COP_SYMBOLS, constraintOps.get(i));
            if (po >= 0)
                constraintOps.set(i, DigirHelper.COP_NAMES[po]);
            else throw new IllegalArgumentException("Unexpected constraintOp=" + constraintOps.get(i));
        }

        //Read all data, then write to tableWriter.
        //I can't split into subsets because I don't know which variable 
        //  to constrain or how to constrain it (it would change with different
        //  userDapQuery's).
        Table table = DigirHelper.searchBmde(
            new String[]{sourceCode}, 
            sourceUrl,
            constraintVariables.toArray(), 
            constraintOps.toArray(),
            constraintValues.toArray(),
            resultsVariables.toArray());

        //if (reallyVerbose) String2.log(table.toString());

        standardizeResultsTable(requestUrl, userDapQuery, table);
        tableWriter.writeAllAndFinish(table);
    }


    /**
     * This tests the bmde with all variables -- used for getting numerical variable ranges
     * and indications of which String variables are active.
     *
     * @param sourceUrl  http://digir.prbo.org/digir/DiGIR.php
     * @param souceCode  e.g., prbo05
     * @param startDate for the test query, e.g., "2002-07-01"
     * @param stopDate for the test query, e.g., "2002-09-01"
     * @throws Throwable if trouble
     */
    public static void testAllVariables(String sourceUrl, String sourceCode, 
          String startDate, String stopDate) throws Throwable {
        String2.log("\n*** testAllVariables");
        testVerboseOn();


        try {

            //one time get inventory  
            //String2.log(DigirHelper.getBmdeInventoryString(
            //    "http://digir.prbo.org/digir/DiGIR.php", 
            //    "prbo05", "darwin:Genus"));
            //if (true) System.exit(0);

            Attributes globalAtts = new Attributes();
            globalAtts.add("creator_email", "???");
            globalAtts.add("infoUrl", "???");
            globalAtts.add("institution", "???");
            globalAtts.add("license", "[standard]");
            globalAtts.add("summary", "A test of BMDE with all variables.");
            globalAtts.add("title", "BMDE - All");

            EDDTable bmde = new EDDTableFromBMDE(
                "testBmde", null, null, globalAtts, 1, 
                new Object[0][0], 1440, sourceUrl, sourceCode); 

            //getEmpiricalMinMax 
            //this returns HTTP error 414 -- url too long
            //so I repeatedly: modify first and last in getEmpricalMinMax and run for those vars
            //??? PERHAPS NOT WORKING BECAUSE I MADE CHANGES TO CREATING WHEN NO DATAVARIABLES SPECIFIED
            bmde.getEmpiricalMinMax(null, startDate, stopDate, false, true);

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nUnexpected testAllVariables error."); 
        }
    }

    /**
     * This tests the prbo dataset.
     *
     * @throws Throwable if trouble
     */
    public static void testPrbo() throws Throwable {
        String2.log("\n*** test prbo");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery;
        String error = "";
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        //one time get inventory  
        //String2.log(DigirHelper.getBmdeInventoryString(
        //    "http://digir.prbo.org/digir/DiGIR.php", 
        //    "prbo05", "darwin:Genus"));
        //if (true) System.exit(0);

        try {
        EDDTable prbo = (EDDTable)oneFromDatasetXml("prbo05Bmde"); 

        //one time getEmpiricalMinMax 
        //prbo.getEmpiricalMinMax("2002-07-01", "2002-09-01", false, true);
        //if (true) System.exit(0);

        //.das     das isn't affected by userDapQuery
        tName = prbo.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            prbo.className(), ".das"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"Attributes {\n" +
" s {\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float64 actual_range -124.0, -122.0;\n" +
"    String axis \"X\";\n" +
"    Float64 colorBarMaximum -122.0;\n" +
"    Float64 colorBarMinimum -124.0;\n" +
"    String comment \"The longitude of the location from which the organism was collected, expressed in decimal degrees. Range = -180.0 to 180.0.\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float64 actual_range 36.5, 38.5;\n" +
"    String axis \"Y\";\n" +
"    Float64 colorBarMaximum 38.5;\n" +
"    Float64 colorBarMinimum 36.5;\n" +
"    String comment \"The latitude of the location from which the organism was collected, expressed in decimal degrees. Range = -90.0 to 90.0.\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  altitude {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"up\";\n" +
"    Float64 actual_range 0.0, 200.0;\n" +
"    String axis \"Z\";\n" +
"    Float64 colorBarMaximum 200.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"The minimum altitude in meters above (positive) or below (negative) sea level of the collecting locality.\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Altitude\";\n" +
"    String positive \"up\";\n" +
"    String standard_name \"altitude\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 8.283168e+8, NaN;\n" +
"    String axis \"T\";\n" +
"    String comment \"Full date/time of this observation event.\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  BasisOfRecord {\n" +
"    String comment \"A descriptive term indicating whether the record represents an object or observation. Examples: \\\"preserved specimen\\\", \\\"observation\\\", \\\"living organism\\\".\";\n" +
"    String ioos_category \"Identifier\";\n" +
"  }\n";
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);
        expected = 
"  TimeObservationsStarted {\n" +
"    Float64 actual_range 0.0, 24.0;\n" +
"    Float64 colorBarMaximum 24.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String comment \"The time of day the entire observation event started (this may be different from the time when the observation represented by this single record was made), expressed as decimal hours from midnight, local time (e.g., 12.0 = noon, 13.5 = 1:30pm).\";\n" +
"    String ioos_category \"Time\";\n" +
"    String units \"hours\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Point\";\n" +
"    String citation \"(none required)\";\n" +
"    String creator_email \"gballard@prbo.org\";\n" +
"    Float64 Easternmost_Easting -122.0;\n" +
"    Float64 geospatial_lat_max 38.5;\n" +
"    Float64 geospatial_lat_min 36.5;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max -122.0;\n" +
"    Float64 geospatial_lon_min -124.0;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    Float64 geospatial_vertical_max 200.0;\n" +
"    Float64 geospatial_vertical_min 0.0;\n" +
"    String geospatial_vertical_positive \"up\";\n" +
"    String geospatial_vertical_units \"m\";\n" +
"    String history \"" + today + " http://digir.prbo.org/digir/DiGIR.php\n" +
today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
            "/tabledap/prbo05Bmde.das\";\n" +
"    String infoUrl \"http://www.prbo.org\";\n" +
"    String institution \"PRBO\";\n" +
"    String keywords \"Biological Classification > Animals/Vertebrates > Birds\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 38.5;\n" +
"    String sourceUrl \"http://digir.prbo.org/digir/DiGIR.php\";\n" +
"    Float64 Southernmost_Northing 36.5;\n" +
"    String standard_name_vocabulary \"CF Standard Name Table v27\";\n" +
"    String summary \"Shorebird observation data from the Southeast Farallon Island from the PRBO Conservation Service.\n" +
"\n" +
"DiGIR is an engine which takes XML requests for data and returns a data\n" +
"subset stored as XML data (as defined in a schema). For more DiGIR\n" +
"information, see http://digir.sourceforge.net/ ,\n" +
"http://diveintodigir.ecoforge.net/draft/digirdive.html ,\n" +
"and http://digir.net/prov/prov_manual.html .\n" +
"A list of Digir providers is at\n" +
"http://bigdig.ecoforge.net/wiki/SchemaStatus .\n" +
"\n" +
"The Bird Monitoring Data Exchange (BMDE) schema defines a standard\n" +
"data table with bird monitoring information.\n" +
"See the BMDE schema at http://akn.ornith.cornell.edu/Schemas/bmde/BMDE-Bandingv1.38.08.xsd .\n" +
"For more BMDE info, see http://www.avianknowledge.net .\n" +
"\n" +
"Most BMDE datasets return a maximum of 10000 rows of data per request.\";\n" +
"    String time_coverage_start \"1996-04-01T00:00:00Z\";\n" +
"    String title \"BMDE - PRBO, SE Farallon Island Shorebirds\";\n" +
"    Float64 Westernmost_Easting -124.0;\n" +
"  }\n" +
"}\n";
        int tPo = results.indexOf(expected.substring(0, 17));
        Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
        Test.ensureEqual(
            results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
            expected, "results=\n" + results);

        //.csv        
        //from DigirHelper.testBmde()
        //    new String[]{"bmde:Family", "bmde:Genus", "bmde:ObservationDate", "bmde:ObservationDate"}, //"bmde:Class", 
        //    new String[]{"equals",      "equals",     "greaterThan",          "lessThan"}, //"equals"
        //    new String[]{"Laridae",     "Uria",       "2007-06-01",           "2007-06-05"}, //"Aves", 
        //    new String[]{"bmde:DecimalLongitude", "bmde:DecimalLatitude", 
        //        "bmde:ObservationDate",
        //        "bmde:GlobalUniqueIdentifier", "bmde:Genus", "bmde:ScientificName"});
        userDapQuery = "longitude,latitude,altitude,time,GlobalUniqueIdentifier,Genus,ScientificName,ObservationCount" +
            "&Family=\"Laridae\"&Genus=\"Uria\"&time>2007-06-01&time<2007-06-05"; 
        tName = prbo.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            prbo.className(), ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);
        expected = 
"longitude, latitude, altitude, time, GlobalUniqueIdentifier, Genus, ScientificName, ObservationCount\n" +
"degrees_east, degrees_north, m, UTC, , , , count\n" +
"-123.002737, 37.698771, NaN, 2007-06-01T17:00:00Z, URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1171.1, Uria, Uria aalge, 271\n" +
"-123.002737, 37.698771, NaN, 2007-06-02T17:00:00Z, URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1172.1, Uria, Uria aalge, 256\n" +
"-123.002737, 37.698771, NaN, 2007-06-03T17:49:00Z, URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1173.1, Uria, Uria aalge, 260\n" +
"-123.002737, 37.698771, NaN, 2007-06-04T17:00:00Z, URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1174.1, Uria, Uria aalge, 249\n" +
"-123.002737, 37.698771, NaN, 2007-06-01T17:00:00Z, URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1191.1, Uria, Uria aalge, 245\n" +
"-123.002737, 37.698771, NaN, 2007-06-02T17:55:00Z, URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1192.1, Uria, Uria aalge, 309\n" +
"-123.002737, 37.698771, NaN, 2007-06-03T17:00:00Z, URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1193.1, Uria, Uria aalge, 268\n" +
"-123.002737, 37.698771, NaN, 2007-06-04T17:25:00Z, URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1194.1, Uria, Uria aalge, 236\n" +
"-123.002737, 37.698771, NaN, 2007-06-01T17:10:00Z, URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1616.1, Uria, Uria aalge, 129\n" +
"-123.002737, 37.698771, NaN, 2007-06-02T17:49:00Z, URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1617.1, Uria, Uria aalge, 113\n" +
"-123.002737, 37.698771, NaN, 2007-06-03T17:00:00Z, URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1618.1, Uria, Uria aalge, 131\n" +
"-123.002737, 37.698771, NaN, 2007-06-04T17:00:00Z, URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1619.1, Uria, Uria aalge, 104\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv     variation to test string  < >
        userDapQuery = "longitude,latitude,altitude,time,GlobalUniqueIdentifier,Genus,ScientificName,ObservationCount" +
            "&Family=\"Laridae\"&Genus>=\"Urh\"&Genus<\"Urj\"&time>2007-06-01&time<2007-06-05"; 
        tName = prbo.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            prbo.className() + "Compare", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //.csv     variation to test string  regex
        userDapQuery = "longitude,latitude,altitude,time,GlobalUniqueIdentifier,Genus,ScientificName,ObservationCount" +
            "&Family=\"Laridae\"&Genus=~\"(zztop|Uria)\"&time>2007-06-01&time<2007-06-05"; 
        tName = prbo.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            prbo.className() + "Compare", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //.csv        test lon lat constraints
        //All I ever get for altitude is missingValue.
        userDapQuery = "longitude,latitude,altitude,time,GlobalUniqueIdentifier,Genus,ScientificName,ObservationCount" +
            "&longitude=-123.002737&latitude=37.698771&Family=\"Laridae\"&Genus=\"Uria\"&time>2007-06-01&time<2007-06-05"; 
        tName = prbo.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            prbo.className() + "LonLat", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        String2.log(results);
        expected = 
"longitude, latitude, altitude, time, GlobalUniqueIdentifier, Genus, ScientificName, ObservationCount\n" +
"degrees_east, degrees_north, m, UTC, , , , count\n" +
"-123.002737, 37.698771, NaN, 2007-06-01T17:00:00Z, URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1171.1, Uria, Uria aalge, 271\n" +
"-123.002737, 37.698771, NaN, 2007-06-02T17:00:00Z, URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1172.1, Uria, Uria aalge, 256\n" +
"-123.002737, 37.698771, NaN, 2007-06-03T17:49:00Z, URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1173.1, Uria, Uria aalge, 260\n" +
"-123.002737, 37.698771, NaN, 2007-06-04T17:00:00Z, URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1174.1, Uria, Uria aalge, 249\n" +
"-123.002737, 37.698771, NaN, 2007-06-01T17:00:00Z, URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1191.1, Uria, Uria aalge, 245\n" +
"-123.002737, 37.698771, NaN, 2007-06-02T17:55:00Z, URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1192.1, Uria, Uria aalge, 309\n" +
"-123.002737, 37.698771, NaN, 2007-06-03T17:00:00Z, URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1193.1, Uria, Uria aalge, 268\n" +
"-123.002737, 37.698771, NaN, 2007-06-04T17:25:00Z, URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1194.1, Uria, Uria aalge, 236\n" +
"-123.002737, 37.698771, NaN, 2007-06-01T17:10:00Z, URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1616.1, Uria, Uria aalge, 129\n" +
"-123.002737, 37.698771, NaN, 2007-06-02T17:49:00Z, URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1617.1, Uria, Uria aalge, 113\n" +
"-123.002737, 37.698771, NaN, 2007-06-03T17:00:00Z, URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1618.1, Uria, Uria aalge, 131\n" +
"-123.002737, 37.698771, NaN, 2007-06-04T17:00:00Z, URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1619.1, Uria, Uria aalge, 104\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

        //expected error didn't occur!
        String2.pressEnterToContinue("\n" + MustBe.getStackTrace() + 
            "An expected error didn't occur at the above location."); 

        } catch (Throwable t) {
            String2.pressEnterToContinue(MustBe.throwableToString(t) + 
                "\nprbo05Bmde STOPPED WORKING ~JAN 2009."); 
        }
    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        String2.log("\n****************** EDDTableFromBMDE.test() *****************\n");

        //usually run
        testPrbo();

        //not usually run     see comments in testAllVars
        //testAllVariables("http://digir.prbo.org/digir/DiGIR.php", "prbo05", "2002-07-01", "2002-09-01");

    }

}
