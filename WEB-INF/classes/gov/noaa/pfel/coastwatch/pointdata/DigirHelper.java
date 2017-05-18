/* 
 * DigirHelper Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.util.*;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.util.SSR;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.xml.xpath.XPath;   //requires java 1.5

import ucar.nc2.dataset.NetcdfDataset;

/**
 * This class has methods to help dealing with DiGIR (and Darwin and OBIS).
 * DiGIR is an engine which takes XML requests for data from a (usually database) 
 * table and returns a subset table stored as XML data (as defined in a schema).
 *
 * <p>Digir information: http://digir.sourceforge.net/
 * Most useful info about the whole Digir system: 
 *   http://diveintodigir.ecoforge.net/draft/digirdive.html
 *   and http://digir.net/prov/prov_manual.html .
 * A list of Digir providers: http://bigdig.ecoforge.net/wiki/SchemaStatus .
 *
 * <p>Darwin is the original schema for use with the Digir engine.
 *
 * <p>OBIS is an oceanography-related schema which extends Darwin.
 * Obis info: http://www.iobis.org .
 * Obis schema info: http://www.iobis.org/tech/provider/questions .
 *
 * <p>BMDE is an avian-related schema which extends Darwin.
 * BMDE info: http://www.avianknowledge.net/content/
 * BMDE schema: http://akn.ornith.cornell.edu/Schemas/bmde/BMDE-Bandingv1.38.08.xsd
 *
 * <p>The XML namespace uri's (xxx_XMLNS) and XML schemas xxx_XSD) are stored as 
 * constants.
 *
 * <p>!!!In addition to the various classes this uses, this needs
 * two properties files: gov/noaa/pfel/coastwatch/pointdata/DigirDarwin2.properties
 * and gov/noaa/pfel/coastwatch/pointdata/DigirObis.properties.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-05-07
 */
public class DigirHelper  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;
    public static boolean reallyVerbose = false;

    public static ResourceBundle2 digirDarwin2Properties = new ResourceBundle2(
        "gov.noaa.pfel.coastwatch.pointdata.DigirDarwin2"); 
    public static ResourceBundle2 digirObisProperties = new ResourceBundle2(
        "gov.noaa.pfel.coastwatch.pointdata.DigirObis"); 
    public static ResourceBundle2 digirBmdeProperties = new ResourceBundle2(
        "gov.noaa.pfel.coastwatch.pointdata.DigirBmde"); 
    private static String[] darwin2Variables, obisVariables, darwin2ObisVariables, 
        bmdeVariables;

    /** The DIGIR_VERSION is used in the header of digir requests.
        It is always "1.0" and diveIntoDigir says it may be removed in the future. */
    public final static String OBIS_VERSION     = "1.0";
    public final static String BMDE_VERSION     = "0.95";
    public final static String DIGIR_XMLNS      = "http://digir.net/schema/protocol/2003/1.0";
    public final static String DARWIN2_XMLNS    = "http://digir.net/schema/conceptual/darwin/2003/1.0";
    public final static String OBIS_XMLNS       = "http://www.iobis.org/obis"; 
    public final static String BMDE_XMLNS       = "http://www.bsc-eoc.org/AKNS/schema"; //listed at start of schema 
    public final static String DIGIR_XSD        = "http://digir.sourceforge.net/schema/protocol/2003/1.0/digir.xsd";
    public final static String DARWIN2_XSD      = "http://digir.sourceforge.net/schema/conceptual/darwin/2003/1.0/darwin2.xsd";
    public final static String OBIS_XSD         = "http://iobis.org/obis/obis.xsd";
    public final static String BMDE_XSD         = "http://akn.ornith.cornell.edu/Schemas/bmde/BMDE-Bandingv1.38.08.xsd";
    public final static String DARWIN_PREFIX    = "darwin";
    public final static String OBIS_PREFIX      = "obis";
    public final static String BMDE_PREFIX      = "bmde";
    public final static String OBIS_PREFIXES[]  = {"",          DARWIN_PREFIX, OBIS_PREFIX};
    public final static String OBIS_XMLNSES[]   = {DIGIR_XMLNS, DARWIN2_XMLNS, OBIS_XMLNS};
    public final static String OBIS_XSDES[]     = {DIGIR_XSD,   DARWIN2_XSD,   OBIS_XSD};
    public final static String BMDE_PREFIXES[]  = {"",          BMDE_PREFIX};
    public final static String BMDE_XMLNSES[]   = {DIGIR_XMLNS, BMDE_XMLNS};
    public final static String BMDE_XSDES[]     = {DIGIR_XSD,   BMDE_XSD};
    public final static String RUTGERS_OBIS_URL = "http://iobis.marine.rutgers.edu/digir2/DiGIR.php"; //often not working
    public final static String CSIRO_OBIS_URL   = "http://www.obis.org.au/digir/DiGIR.php"; //no longer valid
    public final static String IND_OBIS_URL     = "http://digir.indobis.org:80/digir/DiGIR.php";  //lat/lon constraints not numeric
    public final static String BURKE_URL        = "http://biology.burke.washington.edu:80/digir/burke/DiGIR.php"; //no longer valid
    public final static String FLANDERS_OBIS_URL= "http://www.vliz.be/digir/DiGIR.php";
    public final static String PRBO_BMDE_URL    = "http://digir.prbo.org/digir/DiGIR.php";



    public final static String STRING_COPS[] = {"equals", "notEquals", "like", "in"};
    public final static String NUMERIC_COPS[] = {"equals", "notEquals", "in", //no "like"
         "lessThan", "lessThanOrEquals", "greaterThan", "greaterThanOrEquals"};

    /** A list of all comparative operator symbols (for my convenience in parseQuery: 
       2 letter ops are first). */
    public final static String COP_SYMBOLS[] = {
        "!=", "~=", "<=", ">=",   
        "=", "<", ">",
        " in "};  //eeek! handle specially   spaces separate it from variable and value

    /** A list of all comparative operator names (corresponding to the COP_SYMBOLS). */
    public final static String COP_NAMES[] = {
        "notEquals", "like", "lessThanOrEquals", "greaterThanOrEquals", 
        "equals", "lessThan", "greaterThan", 
        "in"}; 


    /* SOURCE_IP is the reference ip used for digir requests. 
       It isn't actually used for ip addressing.
       "65.219.21.6" is upwell here at pfeg.noaa.gov */
    public static String SOURCE_IP = "65.219.21.6"; //upwell

//LOP - logical operator
//how are lops used?  as a tree form of a SQL WHERE clause
//  "and", "andNot", "and not", "or", "orNot", "or not"

//darwin required: 
// "darwin:DateLastModified", "darwin:InstitutionCode", "darwin:CollectionCode",
// "darwin:CatalogNumber", "darwin:ScientificName",
//obis adds required:  "darwin:Longitude", "darwin:Latitude" 

    /**
     * This returns a list of darwin2 variables (with the darwin: prefix).
     *
     * @return the a list of darwin2 variables (with the darwin: prefix).
     */
    public static String[] getDarwin2Variables() {
        if (darwin2Variables == null) {
            darwin2Variables = digirDarwin2Properties.getKeys();
            for (int i = 0; i < darwin2Variables.length; i++)
                darwin2Variables[i] = DARWIN_PREFIX + ":" + darwin2Variables[i];
        }
        return darwin2Variables;
    }

    /**
     * This returns a list of obis variables (with the obis: prefix).
     *
     * @return the a list of obis variables (with the obis: prefix).
     */
    public static String[] getObisVariables() {
        if (obisVariables == null) {
            obisVariables = digirObisProperties.getKeys();
            for (int i = 0; i < obisVariables.length; i++)
                obisVariables[i] = OBIS_PREFIX + ":" + obisVariables[i];
        }
        return obisVariables;
    }

    /**
     * This returns a list of BMDE variables (with the bmde: prefix).
     *
     * @return the a list of BMDE variables (with the bmde: prefix).
     */
    public static String[] getBmdeVariables() {
        if (bmdeVariables == null) {
            bmdeVariables = digirBmdeProperties.getKeys();
            for (int i = 0; i < bmdeVariables.length; i++)
                bmdeVariables[i] = BMDE_PREFIX + ":" + bmdeVariables[i];
        }
        return bmdeVariables;
    }

    /**
     * This returns a list of darwin2 and obis variables (with the darwin: or obis: prefix).
     *
     * @return the a list of darwin2 and obis variables (with the darwin: or obis: prefix).
     */
    public static String[] getDarwin2ObisVariables() {
        if (darwin2ObisVariables == null) {
            StringArray sa = new StringArray();
            sa.add(getDarwin2Variables());
            sa.add(getObisVariables());
            darwin2ObisVariables = sa.toArray();
        }
        return darwin2ObisVariables;
    }

    
    /**
     * This makes the pre-destination part of a request.
     * See searchDigir for parameter descriptions.
     *
     * @throws Exception if trouble
     */
    private static String getPreDestinationRequest(String version,
        String xmlnsPrefix[], String xmlnsNS[], String xmlnsXSD[]) throws Exception {

        //validate the xml info
        String errorInMethod = String2.ERROR + " in DigirHelper.makePreDestinationRequest: \n";
        Test.ensureNotNull(xmlnsPrefix, errorInMethod + "xmlnsPrefix is null.");
        Test.ensureNotNull(xmlnsNS,     errorInMethod + "xmlnsNS is null.");
        Test.ensureNotNull(xmlnsXSD,    errorInMethod + "xmlnsXSD is null.");
        Test.ensureTrue(xmlnsPrefix.length >= 1, 
            errorInMethod + "xmlnsPrefix.length is less than 1.");
        Test.ensureEqual(xmlnsPrefix.length, xmlnsNS.length, 
            errorInMethod + "xmlnsPrefix.length != xmlnsNS.length.");
        Test.ensureEqual(xmlnsPrefix.length, xmlnsXSD.length, 
            errorInMethod + "xmlnsPrefix.length != xmlnsXSD.length.");

        /* example 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<request \n" +
        "  xmlns=\"http://digir.net/schema/protocol/2003/1.0\" \n" + //default namespace
        "  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" \n" +
        "  xmlns:darwin=\"http://digir.net/schema/conceptual/darwin/2003/1.0\" \n" +
        "  xmlns:obis=\"http://www.iobis.org/obis\" \n" +
        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
        "  xsi:schemaLocation=\"http://digir.net/schema/protocol/2003/1.0 \n" + //if not provided, waits, then returns provider info
        "    http://digir.sourceforge.net/schema/protocol/2003/1.0/digir.xsd \n" +  //these are pars of xmlns+xsd
        "    http://digir.net/schema/conceptual/darwin/2003/1.0 \n" +
        "    http://digir.sourceforge.net/schema/conceptual/darwin/2003/1.0/darwin2.xsd \n" +
        "    http://www.iobis.org/obis\" \n" +
        "    http://www.iobis.org/obis/obis.xsd\" >\n" +
        "  <header>\n" +
        "    <version>1.0</version>\n" +
        "    <sendTime>&CurrentDateTTime;</sendTime>\n" +
        "    <source>localhost</source>\n" +
        */

        //make the request
        StringBuilder request = new StringBuilder();
        request.append(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<request \n" +
            "  xmlns=\"" + xmlnsNS[0] + "\" \n" + //default namespace
            "  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" \n" +
            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n");
        for (int i = 1; i < xmlnsNS.length; i++)  //1 because 0=default handled above
            request.append(
                "  xmlns:" + xmlnsPrefix[i] + "=\"" + xmlnsNS[i] + "\" \n");

        //schema info: pairs of xmlns and xsd
        request.append("  xsi:schemaLocation=\"");
        for (int i = 0; i < xmlnsNS.length; i++)  
            request.append((i == 0? "" : "\n    ") + 
                xmlnsNS[i] + " \n      " + xmlnsXSD[i]);  
        request.append("\" >\n" +
            "  <header>\n" +
            "    <version>" + version + "</version>\n" +
            "    <sendTime>" + Calendar2.getCurrentISODateTimeStringZulu() + "Z</sendTime>\n" +
            "    <source>" + SOURCE_IP + "</source>\n"); 

        return request.toString();
    }


    /**
     * This makes the filter part of a request.
     * See searchDigir for parameter descriptions.
     *
     * @param filterVariables
     * @param filterCops
     * @param filterValues
     * @throws Exception if trouble (e.g., filterVariables, filterCops, and/or
     *    filterValues lengths are different)
     */
    private static String getFilterRequest(String filterVariables[], 
        String filterCops[], String filterValues[])
        throws Exception {

        //validate the input
        String errorInMethod = String2.ERROR + " in DigirHelper.getFilterRequest: \n";
        if (filterVariables == null || filterVariables.length == 0) 
            return //a simple filter that is always true
                "    <filter>\n" +
                "     <greaterThanOrEquals>\n" +
                "        <darwin:Latitude>-90</darwin:Latitude>\n" +
                "      </greaterThanOrEquals>\n" +
                "    </filter>\n";
        Test.ensureTrue(filterCops != null && filterCops.length == filterVariables.length, 
            errorInMethod + "filterCops.length != filterVariables.length.");
        Test.ensureTrue(filterValues != null && filterCops.length == filterVariables.length, 
            errorInMethod + "filterCops.length != filterVariables.length.");

        /*
        "    <filter>\n" +
        "      <equals>\n" +
        "        <darwin:Genus>Macrocystis</darwin:Genus>\n" +
        "      </equals>\n" +
        "    </filter>\n" +
        */

        //make the filter xml
        //Digir schema says <and> always contains exactly two elements
        //  so I have to construct a tree of <and>'s.
        StringBuilder request = new StringBuilder();
        request.append("    <filter>\n");
        for (int i = 0; i < filterCops.length; i++) {
            //I can't check against darwin/obis var lists here, since other schemas may be in play
            if (filterVariables[i] == null || filterVariables[i].length() == 0)
                Test.error(errorInMethod + "filterVariable#" + i + "=" + filterVariables[i]);
            if (String2.indexOf(NUMERIC_COPS, filterCops[i]) < 0 &&
                String2.indexOf(STRING_COPS, filterCops[i]) < 0)
                Test.error(errorInMethod + "Invalid filterCOP#" + i + "=" + filterCops[i]);
            if (filterValues[i] == null)  //allow ""?
                Test.error(errorInMethod + "filterValue#" + i + "=" + filterValues[i]);

            String spacer = String2.makeString(' ', i*2);
            if (i < filterCops.length - 1) 
                request.append(spacer + "      <and>\n");
            request.append(
                spacer + "        <" + filterCops[i] + "><" + filterVariables[i] + ">" + 
                    filterValues[i] + "</" + filterVariables[i] + "></" + filterCops[i] + ">\n");
        }
        for (int i = filterCops.length - 2; i >= 0; i--) 
            request.append(String2.makeString(' ', i*2) + "      </and>\n");
        request.append("    </filter>\n");
        return request.toString();

    }
        
    /**
     * This gets a Digir provider/portal's metadata as an XML String.
     * See examples at http://diveintodigir.ecoforge.net/draft/digirdive.html
     * and http://digir.net/prov/prov_manual.html .
     * See parameters for searchDigir.
     *
     * <p>Informally: it appears that you can get the metadataXml from a provider/portal
     * just by going to the url (even in a browser). But this may be just an 
     * undocumented feature of the standard portal software.
     *
     * @throws Exception if trouble
     */
    public static String getMetadataXml(String url, String version) throws Exception {

        /* example from diveintodigir
        <?xml version="1.0" encoding="UTF-8"?>
        <request xmlns="http://digir.net/schema/protocol/2003/1.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://digir.net/schema/protocol/2003/1.0
                  http://digir.sourceforge.net/schema/protocol/2003/1.0/digir.xsd">
          <header>
            <version>1.0</version>
            <sendTime>2003-03-09T19:30:04-05:00</sendTime>
            <source>216.91.87.102</source>
            <destination>http://digir.net:80/testprov/DiGIR.php</destination>
            <type>metadata</type>                                                   
          </header>
        </request> 
        */

        //make the request
        String errorInMethod = String2.ERROR + " in DigirHelper.getMetadataXml: \n";
        StringBuilder requestSB = new StringBuilder();
        requestSB.append(getPreDestinationRequest(version,
            //only digir (not darwin or obis or ...) namespace and schema is needed
            new String[]{""},     
            new String[]{DIGIR_XMLNS},
            new String[]{DIGIR_XSD}));
        requestSB.append(
            "    <destination>" + url + "</destination>\n" +  
            "    <type>metadata</type>\n" +
            "  </header>\n" +
            "</request>\n");
        String request = requestSB.toString();
        long time = System.currentTimeMillis();
        if (reallyVerbose) String2.log("\nDigirHelper.getMetadataXml request=\n" + request +
            "\nGetting the response takes a long time...");

        //get the response
        //[This is the official way to do it. 
        //In practice, digir servers also return the metadata in response to the url alone.]
        request = String2.toSVString(String2.split(request, '\n'),  //split trims each string
            " ", true); //(white)space is necessary to separate schemalocation names and locations
        //        if (reallyVerbose) String2.log("\ncompactRequest=" + request + "\n");
        String response = SSR.getUrlResponseString(url + "?request=" + 
            SSR.percentEncode(request));
        if (verbose) String2.log("DigirHelper.getMetadataXml done. TIME=" +
            (System.currentTimeMillis() - time));
        if (reallyVerbose) String2.log(
            "start of response=\n" + response.substring(0, Math.min(response.length(), 3000)));
        return response;
    }

    /**
     * This gets a Digir provider's metadata as a table.
     * See examples at http://diveintodigir.ecoforge.net/draft/digirdive.html
     * and http://digir.net/prov/prov_manual.html .
     * See parameters for searchDigir.
     * Note that the xml contains some information before the resource list --
     * so that information doesn't make it into the metadataTable.
     *
     * 
     * @return a table with a row for each resource. 
     *   The "code" column has the codes for the 
     *   resources (which are used for inventory and search requests).   
     *   You can get a String[] of the codes for the resources available
     *   from this provider via <tt>((StringArray)table.findColumn("code")).toArray());</tt>
     * @throws Exception if trouble
     */
    public static Table getMetadataTable(String url, String version) throws Exception {

        //useful info from obis metadata:
        //   Institute of Marine and Coastal Sciences, Rutgers University</name>
        //        <name>Phoebe Zhang</name>
        //        <title>OBIS Portal Manager</title>
        //        <emailAddress>phoebe@imcs.rutgers.edu</emailAddress>
        //        <phone>001-732-932-6555 ext. 503</phone>

        //get the metadata xml and StringReader
        String xml = getMetadataXml(url, version);
        StringReader reader = new StringReader(xml);
        //for testing:
        //Test.ensureTrue(String2.writeToFile("c:/temp/ObisMetadata.xml", xml).equals(""), 
        //    "Unable to save c:/temp/Obis.Metadata.xml.");
        //FileReader reader = new FileReader("c:/programs/digir/ObisMetadata.xml");

        //read the resource data
        Table table = new Table();
        boolean validate = false; //since no .dtd specified by DOCTYPE in the file
        table.readXml(new BufferedReader(reader), 
            validate, "/response/content/metadata/provider/resource", null,
            true); //simplify
        if (reallyVerbose) String2.log("DigirHelper.getMetadataTable, first 3 rows:\n" + 
            table.toString(3));

        return table;

    }

    /**
     * This tests getMetadata.
     *
     * @throws Exception if trouble
     */
    public static void testGetMetadata() throws Exception {
        verbose = true;
        reallyVerbose = true;
        Table.verbose = true;
        Table.reallyVerbose = true;
        String2.log("\n***  DigirHelper.testGetMetadata");
        Table table;

        //test rutgers_obis  
        if (false) {
            //this used to work and probably still does; but I have stopped testing rutgers because it is often down.
            table = getMetadataTable(RUTGERS_OBIS_URL, OBIS_VERSION);
            String2.log("metadata table=" + table.toString(10));
            Test.ensureTrue(table.nRows() >= 142, "nRows=" + table.nRows());
            Test.ensureEqual(table.getColumnName(0), "name", "");
            Test.ensureEqual(table.getColumnName(1), "code", "");
            Test.ensureEqual(table.getColumnName(2), "relatedInformation", "");
            Test.ensureEqual(table.getColumnName(3), "contact/emailAddress", "");
            Test.ensureEqual(table.getStringData(0, 0), "EPA'S EMAP Database", "");
            Test.ensureEqual(table.getStringData(1, 0), "EMAP", ""); 
            Test.ensureEqual(table.getStringData(1, 1), "HMAP", ""); 
            Test.ensureEqual(table.getStringData(1, 2), "NODC", ""); 
            Test.ensureEqual(table.getStringData(2, 0), "URL that provides more information about this resource", "");
            Test.ensureEqual(table.getStringData(3, 0), "hale.stephen@epa.gov", "");   
            Test.ensureEqual(table.getStringData(4, 0), "401-782-3048", "");

            //test getting code column (mentioned in getMetadataTable docs
            String codes[] = ((StringArray)table.findColumn("code")).toArray();
            String2.log("codes=" + String2.toCSSVString(codes));
            Test.ensureTrue(codes.length >= 142, "codes.length=" + codes.length);
            Test.ensureTrue(String2.indexOf(codes, "GHMP") >= 0, "GHMP not found.");
            Test.ensureTrue(String2.indexOf(codes, "EMAP") >= 0, "EMAP not found.");
            Test.ensureTrue(String2.indexOf(codes, "iziko Fish") >= 0, "iziko Fish not found.");
        }

        //test ind_obis
        if (false) {
//    Row            name           code   contact/name  contact/title contact/emailA  contact/phone  contact2/name contact2/title contact2/email contact2/phone
//     abstract       keywords       citation conceptualSche recordIdentifi numberOfRecord dateLastUpdate minQueryTermLe maxSearchRespo maxInventoryRe
//      0  IndOBIS, India        indobis Vishwas Chavan      Scientist vs.chavan@ncl. 91 20 2590 248 Asavari Navlak Technical Offi ar.navlakhe@nc 91 20 2590 248 IndOBIS (India Indian Ocean,  Chavan, VIshwa http://digir.n        sciname   41880 2007-06-21T02:              3            100          10000
//      1  Biological Col  NIOCOLLECTION Achuthankutty, Coordinator, B   achu@nio.org    http://digir.n        sciname     803     2006-11-03              3          10000          10000
            table = getMetadataTable(IND_OBIS_URL, OBIS_VERSION);
            String2.log("metadata table=" + table.toString(10));
            Test.ensureTrue(table.nRows() >= 2, "nRows=" + table.nRows());
            Test.ensureEqual(table.getColumnName(0), "name", "");
            Test.ensureEqual(table.getColumnName(1), "code", "");
            Test.ensureEqual(table.getColumnName(2), "contact/name", "");
            Test.ensureEqual(table.getColumnName(3), "contact/title", "");
            Test.ensureEqual(table.getStringData(0, 0), "IndOBIS, Indian Ocean Node of OBIS", "");
            Test.ensureEqual(table.getStringData(1, 0), "indobis", ""); 
            Test.ensureEqual(table.getStringData(2, 0), "Vishwas Chavan", ""); 
            Test.ensureEqual(table.getStringData(1, 1), "NIOCOLLECTION", "");

        }

        //test flanders
        if (true) {
//    Row            name           code relatedInforma   contact/name  contact/title contact/emailA  contact/phone  contact2/name contact2/title contact2/email contact2/phone       abstract       citation useRestriction conceptualSche conceptualSche recordIdentifi    recordBasis numberOfRecord dateLastUpdate minQueryTermLe maxSearchRespo maxInventoryRe  contact3/name contact3/email       keywords conceptualSche contact3/title contact3/phone
//      0  Taxonomic Info          tisbe http://www.vli Edward Vanden    Manager VMDC wardvdb@vliz.b  +32 59 342130 Bart Vanhoorne IT Staff Membe bartv_at_vliz_+32 59 342130 Biogeographica Vanden Berghe, Data are freel http://www.iob http://digir.n          Tisbe              O          24622 2007-06-12 10:  0           1000          10000
//      1  Benthic fauna      pechorasea http://www.mar   Dahle, Salve     Data owner sd@akvaplan.ni +47-(0)77-75 0 Cochrane, Sabi Coordinator Bi sc@akvaplan.ni+47-777-50327 Quantitative s                Release with p http://www.iob http://digir.n     PechoraSea              O           1324 2004-09-02 18:  0           1000          10000 Denisenko, Sta dest@unitel.sp  Benthic fauna
//      2  N3 data of Kie         n3data http://www.mar   Rumohr, Heye    hrumohr@ifm-ge +49-(0)431-600    Release with p http://www.iob http://digir.n         N3Data              O           8944 2005-11-22 17:  0           1000          10000                               Benthic fauna,
            table = getMetadataTable(FLANDERS_OBIS_URL, OBIS_VERSION);
            String2.log("metadata table=" + table.toString(10));
            Test.ensureTrue(table.nRows() >= 37, "nRows=" + table.nRows());
            Test.ensureEqual(table.getColumnName(0), "name", "");
            Test.ensureEqual(table.getColumnName(1), "code", "");
            Test.ensureEqual(table.getColumnName(2), "relatedInformation", "");
            Test.ensureEqual(table.getColumnName(3), "contact/name", "");
            Test.ensureEqual(table.getStringData(0, 0), "Taxonomic Information Sytem for the Belgian coastal area", "");
            Test.ensureEqual(table.getStringData(1, 0), "tisbe", ""); 
            Test.ensureEqual(table.getStringData(2, 0), "http://www.vliz.be/vmdcdata/tisbe", "");
            Test.ensureEqual(table.getStringData(1, 1), "pechorasea", "");
        }
    
    }

    /**
     * This gets a Digir provider's inventory as an XML string.
     * "Inventory" is described in the digir schema as 
     * "which is to seek a count of all unique occurrences of a single concept."
     * See example at http://diveintodigir.ecoforge.net/draft/digirdive.html#id803800 
     * and http://digir.net/prov/prov_manual.html .
     * See parameters for searchDigir.
     *
     * @param resource  This must be a value from the "code" column
     *    from getMetadataTable.
     * @param filterVariables and filterCops and filterValues should
     *    have matching lengths (0 is ok)
     * @param resultsVariable The schema variable that you want to search for
     *   and get a count of the unique values of it, e.g., "darwin:ScientificName". 
     *   There can be only one.   The results indicate
     *   the number of occurrences (the count attribute) of each unique 
     *   value of the resultsRariable.
     * @throws Exception if trouble, including a diagnostic message with
     *    severity="fatal" or severity="error".
     */
    public static String getInventoryXml(String version,
        String xmlnsPrefix[], String xmlnsNS[], String xmlnsXSD[],
        String resource, String url, 
        String filterVariables[], String filterCops[], String filterValues[],
        String resultsVariable) throws Exception {

        /* example from diveintodigir
        <?xml version="1.0" encoding="UTF-8"?>
        <request xmlns="http://www.namespaceTBD.org/digir"
                 xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                 xmlns:darwin="http://digir.net/schema/conceptual/darwin/2003/1.0">
          <header>
            <version>1.0.0</version>
            <sendTime>2003-06-05T11:57:00-03:00</sendTime>
            <source>localhost</source>
            <destination resource="MYRES">
              http://some_provider.org/provider/DiGIR.php</destination>
            <type>inventory</type>
          </header>
          <inventory>
            <filter>
             <equals>
              <darwin:Genus>Agrobacterium</darwin:Genus>
             </equals>
            </filter>
            <darwin:ScientificName />
            <count>true</count>
          </inventory>
        </request>
        */

        //create the request
        String errorInMethod = String2.ERROR + " in DigirHelper.getInventory: \n";
        String filterRequest = filterVariables == null || filterVariables.length == 0? "" :
            getFilterRequest(filterVariables, filterCops, filterValues);
        StringBuilder requestSB = new StringBuilder();
        requestSB.append(getPreDestinationRequest(version, xmlnsPrefix, xmlnsNS, xmlnsXSD));
        requestSB.append(
            "    <destination resource=\"" + resource + "\">" + url + "</destination>\n" +  
            "    <type>inventory</type>\n" +
            "  </header>\n" +
            "  <inventory>\n" +
            filterRequest +
            "    <" + resultsVariable + " />\n" +
            "  </inventory>\n" +
            "</request>\n");
        String request = requestSB.toString();
        if (reallyVerbose) String2.log("\nDigirHelper.getInventory request=\n" + request);

        //get the response
        request = String2.toSVString(String2.split(request, '\n'),  //split trims each string
            " ", true); //(white)space is necessary to separate schemalocation names and locations
        //if (reallyVerbose) String2.log("\ncompactRequest=" + request + "\n");
        String response = SSR.getUrlResponseString(url + "?request=" + 
            SSR.percentEncode(request));

        //look for error message
        int errorPo = response.indexOf("severity=\"fatal\"");
        if (errorPo < 0)
            errorPo = response.indexOf("severity=\"error\"");
        if (errorPo > 0) {
            int dCodePo = response.lastIndexOf("<diagnostic code", errorPo);
            errorPo = dCodePo >= 0? dCodePo : Math.max(0, errorPo - 33);
            Test.error(errorInMethod + "Error message from resource=" + 
                resource + ":\n" + response.substring(errorPo));
        }

        if (reallyVerbose) String2.log("\nstart of response=\n" + 
            response.substring(0, Math.min(5000, response.length())));
        return response;
    }

    /**
     * This gets a Digir provider's inventory as a Table.
     * "Inventory" is described in the digir schema as 
     * "which is to seek a count of all unique occurrences of a single concept."
     * See example at http://diveintodigir.ecoforge.net/draft/digirdive.html#id803800 
     * and http://digir.net/prov/prov_manual.html .
     * See parameters for searchDigir.
     *
     * @param resource  These must be values from the "code" column
     *    from getMetadataTable.
     *    Note that this method allows for more than 1 resource
     *    to be queried (unlike getInventoryXml). 
     * @param filterVariables and filterCops and filterValues should
     *    have matching lengths (0 is ok)
     * @param resultsVariable The schema variable that you want to search for
     *   and get a count of the unique values of it, e.g., "darwin:ScientificName". 
     *   There can be only one.  
     * @return table with three columns: "Resource", resultsVariable, and "Count".
     *   The results indicate the number of occurrences of each unique 
     *   value of the resultsRariable at each resource.
     * @throws Exception if trouble
     */
    public static Table getInventoryTable(String version, 
        String xmlnsPrefix[], String xmlnsNS[], String xmlnsXSD[],
        String resource[], String url,
        String filterVariables[], String filterCops[], String filterValues[],
        String resultsVariable) throws Exception {

String2.log("DigirHelper.getInventoryTable(resource[0]=" + resource[0] + " var=" + resultsVariable +
    "\n  url=" + url);

        //make the results table
        Table table = new Table();
        StringArray resourceSA = new StringArray();
        StringArray resultsSA = new StringArray();
        IntArray countIA = new IntArray();
        table.addColumn("Resource",      resourceSA);
        table.addColumn(resultsVariable, resultsSA);
        table.addColumn("Count",         countIA);

        //get the actualVariable e.g., "ScientificName"
        int colonPo = resultsVariable.indexOf(':');
        String resultsActualVariable = colonPo >= 0? resultsVariable.substring(colonPo + 1) : 
            resultsVariable;

        //get the inventory for each resource
        XPath xPath = XML.getXPath();
        boolean validate = false; //since no .dtd specified by DOCTYPE in the file
        for (int res = 0; res < resource.length; res++) {
            String xml = getInventoryXml(version, xmlnsPrefix, xmlnsNS, xmlnsXSD,
                resource[res], url,
                filterVariables, filterCops, filterValues,
                resultsVariable);
            //used for testing:
            //String fileName = "c:/programs/digir/ObisInventory" + res + ".xml";
            //Test.ensureTrue(String2.writeToFile(fileName, xml).equals(""), 
            //    "Unable to save " + fileName);
            //String xml = String2.readFromFile(fileName)[1];
            //String2.log("xml=" + xml);

            //sample xml response snippet:
            //<response><content><record>
            //  <darwin:ScientificName count='248'>Macrocystis integrifolia</darwin:ScientificName>
            //</record></content></response>
            Document document = XML.parseXml(new StringReader(xml), validate);
            NodeList nodeList = XML.getNodeList(document, xPath, 
                "/response/content/record/" + resultsActualVariable);
            String2.log("DigirHelper.getInventoryTable nodeList length=" + nodeList.getLength());
            if (nodeList.getLength() == 0)
                String2.log("xml=\n" + xml);
            for (int nodeI = 0; nodeI < nodeList.getLength(); nodeI++) {
                Element element = (Element)nodeList.item(nodeI);
                resourceSA.add(resource[res]);
                resultsSA.add(element.getTextContent());
                countIA.add(String2.parseInt(element.getAttribute("count")));
            }
        }
String2.log("inventoryTable:\n" + table.toString());
        return table;
    }

    /**
     * This returns a string with OBIS Genus inventory information.
     * 
     * @param url
     * @param code  e.g. "GHMP"
     * @param field e.g., "darwin:Genus"
     * @return a string with Genus inventory information.
     * @throws Exception if trouble
     */
    public static String getObisInventoryString(String url, String code, String field) throws Exception {
        //one time things
        Table table = new Table();
        table = getInventoryTable(OBIS_VERSION, OBIS_PREFIXES, OBIS_XMLNSES, OBIS_XSDES,
            new String[]{code}, //GHMP
            url,
            new String[]{}, new String[]{}, new String[]{}, //filter
            //try goofy restrictions in case something is required
            //new String[]{"darwin:ScientificName", "darwin:ScientificName"}, //"darwin:Genus"
            //new String[]{"greaterThan",           "lessThan"             }, //"equals"
            //new String[]{"A",                     "z"                    }, //"Carcharias"
            field);
        StringBuilder sb = new StringBuilder();
        int nRows = table.nRows();
        for (int row = 0; row < nRows; row++) {
            sb.append(table.getStringData(1, row) + " (" + table.getIntData(2, row) + ")");            
            if (row < nRows - 1) sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This returns a string with BMDE Genus inventory information.
     * 
     * @param url
     * @param code  e.g. "prbo05"
     * @param field e.g., "darwin:Genus"
     * @return a string with Genus inventory information.
     * @throws Exception if trouble
     */
    public static String getBmdeInventoryString(String url, String code, String field) throws Exception {
        //one time things
        Table table = new Table();
        table = getInventoryTable(BMDE_VERSION, BMDE_PREFIXES, BMDE_XMLNSES, BMDE_XSDES,
            new String[]{code}, //prbo05
            url,
            new String[]{}, //"Genus"
            new String[]{}, //"="
            new String[]{}, //spp=carcharias
            field);
        StringBuilder sb = new StringBuilder();
        int nRows = table.nRows();
        for (int row = 0; row < nRows; row++) {
            sb.append(table.getStringData(1, row) + " (" + table.getIntData(2, row) + ")");            
            if (row < nRows - 1) sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This tests getInventory.
     * @throws Exception if trouble
     */
    public static void testGetInventory() throws Exception {
        verbose = true;
        reallyVerbose = true;
        Table table;

        //Test that "diagnostic" exception is thrown if error in request
        if (false) {
            //this worked, but I have stopped testing rutgers_obis and 
            //stopped doing tests of things that don't work -- don't stress the servers
            String2.log("\n***  DigirHelper.testGetInventory ensure diagnostic error is caught");
            try {
                String xml = getInventoryXml(OBIS_VERSION, OBIS_PREFIXES, OBIS_XMLNSES, OBIS_XSDES,
                    "NONE", //!!! not available
                    RUTGERS_OBIS_URL,
                    new String[]{"darwin:Genus"},
                    new String[]{"equals"},
                    new String[]{"Carcharodon"}, 
                    "darwin:ScientificName");            
                String2.log(xml); 
                String2.log("Shouldn't get here."); Math2.sleep(60000);
            } catch (Exception e) {
                String2.log(e.toString());
                if (e.toString().indexOf("<diagnostic") < 0) throw e;
                String2.log("\n*** diagnostic error (above) correctly caught\n");
            }
        }
    
        if (false) {
            //this worked, but I have stopped testing rutgers_obis and 
            //stopped doing tests of things that don't work -- don't stress the servers
            String2.log("\n***  DigirHelper.testGetInventory valid test...");
            table = getInventoryTable(OBIS_VERSION, OBIS_PREFIXES, OBIS_XMLNSES, OBIS_XSDES,
                //"GHMP", //or 
                //"aims_biotech", 
                //"IMCS", not available
                new String[]{"iziko Fish"},
                RUTGERS_OBIS_URL,
                new String[]{"darwin:Genus"},
                new String[]{"equals"},
                new String[]{
                    //"Macrocystis"},
                    "Carcharodon"}, //spp=carcharias
                "darwin:ScientificName");
            String2.log("inventory table=" + table);
            Test.ensureTrue(table.nRows() >= 1, "nRows=" + table.nRows());
            String colNames = String2.toCSSVString(table.getColumnNames());
            Test.ensureEqual(colNames, "Resource, darwin:ScientificName, Count", "");
            Test.ensureEqual(table.getStringData(0,0), "iziko Fish", "");
            Test.ensureEqual(table.getStringData(1,0), "Carcharodon carcharias", "");
            Test.ensureEqual(table.getStringData(2,0), "18", "");
        }

        if (false) {
            //experiment
            String2.log("\n***  DigirHelper.testGetInventory experiment...");
            table = getInventoryTable(OBIS_VERSION, OBIS_PREFIXES, OBIS_XMLNSES, OBIS_XSDES,
                new String[]{"OBIS-SEAMAP"},
                RUTGERS_OBIS_URL,
                new String[]{"darwin:YearCollected"},
                new String[]{"equals"},
                new String[]{"2005"}, 
                "darwin:ScientificName");
            String2.log("inventory table=" + table);
            Test.ensureTrue(table.nRows() >= 1, "nRows=" + table.nRows());
            String colNames = String2.toCSSVString(table.getColumnNames());
            Test.ensureEqual(colNames, "Resource, darwin:ScientificName, Count", "");
            Test.ensureEqual(table.getStringData(0,0), "OBIS-SEAMAP", "");
            //Test.ensureEqual(table.getStringData(1,0), "Carcharodon carcharias", "");
            //Test.ensureEqual(table.getStringData(2,0), "18", "");
        }

        if (false) {
            String2.log("\n***  DigirHelper.testGetInventory valid test of indobis...");
            table = getInventoryTable(OBIS_VERSION, OBIS_PREFIXES, OBIS_XMLNSES, OBIS_XSDES,
                new String[]{"indobis"},
                IND_OBIS_URL,
                new String[]{"darwin:Genus"},
                new String[]{"equals"},
                new String[]{
                    //"Macrocystis"},
                    "Carcharodon"}, //spp=carcharias
                "darwin:ScientificName");
            String2.log("inventory table=" + table);
            Test.ensureTrue(table.nRows() >= 1, "nRows=" + table.nRows());
            String colNames = String2.toCSSVString(table.getColumnNames());
            Test.ensureEqual(colNames, "Resource, darwin:ScientificName, Count", "");
            Test.ensureEqual(table.getStringData(0,0), "indobis", "");
            Test.ensureEqual(table.getStringData(1,0), "Carcharodon carcharias", "");
            Test.ensureEqual(table.getStringData(2,0), "7", "");
        }

        //This is the test I normally run because Flanders is reliable.
        if (true) {
            String2.log("\n***  DigirHelper.testGetInventory valid test of flanders...");
            table = getInventoryTable(OBIS_VERSION, OBIS_PREFIXES, OBIS_XMLNSES, OBIS_XSDES,
                new String[]{"tisbe"},
                FLANDERS_OBIS_URL,
                new String[]{"darwin:Genus"}, 
                new String[]{"equals"},
                new String[]{"Abietinaria"},
                //"darwin:Genus"); 
                "darwin:ScientificName");
            String2.log("inventory table=" + table);
//11 Abietinaria abietina
//3  Abietinaria filicula
            String2.log("darwin:ScientificNames: " + table.getColumn(1).toString());
            Test.ensureTrue(table.nRows() >= 2, "nRows=" + table.nRows());
            String colNames = String2.toCSSVString(table.getColumnNames());
            Test.ensureEqual(colNames, "Resource, darwin:ScientificName, Count", "");
            Test.ensureEqual(table.getStringData(0,0), "tisbe", "");
            Test.ensureEqual(table.getStringData(1,0), "Abietinaria abietina", "");
            //pre 2009-02-12 was 11; then 10; post 2009-03-12 is 11; post 2010-07-19 is 10
            Test.ensureEqual(table.getIntData(   2,0), 10, "");  
            Test.ensureEqual(table.getStringData(0,1), "tisbe", "");
            Test.ensureEqual(table.getStringData(1,1), "Abietinaria filicula", "");
            //pre 2009-02-12 was 3; then 2; post 2009-03-12 is 3; post 2010-07-19 is 2
            Test.ensureEqual(table.getIntData(   2,1), 2, ""); 
        }


    }


    /**
     * This gets data from Digir providers using various schemas.
     * This calls setAttributes().
     *
     * @param xmlnsPrefix The prefixes for the namespaces
     *    e.g., {"", "darwin", "obis"} .  
     *    The array length must be at least 1.
     *    0th entry is ignored (it is default namespace).
     *    xsd (http://www.w3.org/2001/XMLSchema) and 
     *    xsi (http://www.w3.org/2001/XMLSchema-instance) are automatically added.        
     * @param xmlnsNS the xmlns values (pseudo URLs, not actual documents)
     *    e.g., {DIGIR_XMLNS, DARWIN_XMLNS, OBIS_XMLNS}
     *    These must correspond to the xmlnsPrefix values.
     * @param xmlnsXSD The url's of the schema documents
     *    e.g., {DIGIR_XSD, DARWIN_XSD, OBIS_XSD}
     *    These must correspond to the xmlnsPrefix values.
     * @param resources the codes for the resources to be searched (e.g., 
     *    {"GHMP", "aims_biotech", "IndOBIS"}).
     *    You can get a list of resources from the darwin:CollectionCode column 
     *    in the metadata from the provider/portal;
     *    see getMetadataXml and getMetadataTable above.   
     *    [To my knowledge, there is no code to search all resources with one call.]
     * @param url the url of the provider, usually ending in "DiGIR.php", 
     *    e.g., http://iobis.marine.rutgers.edu/digir2/DiGIR.php.
     * @param filterVariables the variables for each of the filter tests,
     *    e.g., {"darwin:Genus", "darwin:Latitude"}.
     *    These must correspond to the filterCops.
     *    Note that filterVariables can include variables that are not in
     *    resultsVariables.
     * @param filterCops an array of COP's (comparitive operators) for filter tests, selected from: 
     *    String variable COPs 
     *        ("equals", "notEquals", "like", "in") or
     *    numeric variable COPs 
     *        ("equals", "notEquals", "in", "lessThan", "lessThanOrEquals",  
     *        "greaterThan", "greaterThanOrEquals").
     *    For "like", a "%" sign at the beginning or end of the value is a wildcard
     *      (e.g., "Op%" will match all values starting with "Op").
     *    For "in", the value can be a comma-separated list of values
     *      that can be matched.
     *    There is one filterCop for each test (1 or more, 
     *      according to the DiGIR schema) to be done.    
     *    All tests must pass (there is an implied AND between all tests).
     *    [This is a limitation of this method, not Digir.]
     *    Tests on fields with no value in the database return false.
     * @param filterValues the values for the filter tests,
     *    e.g, {"Macrocystis", "53"} .
     *    These must correspond to the filterCops (it may be null).
     * @param resultsTable data is appended to resultsTable.
     *    If resultsTable has data, it must have the same resultsVariables.
     * @param resultsVariables e.g., {"darwin:Latitude", "obis:Temperature"}.
     *    This must not be null or length 0.
     *    You can't use e.g., "obis" in place of "darwin", even though the
     *    obis schema includes darwin.
     *    The resulting table will always include just these variables 
     *    and in the requested order.
     * @throws Exception if trouble.
     *    If a resource doesn't respond appropriately, it is just skipped 
     *    (no Exception is thrown).  But if there is no data from any resource
     *    and there is a diagnostic severity="fatal" or severity="error" message, 
     *    an Exception if thrown and that message is used.    
     */
    public static void searchDigir(String version, 
        String xmlnsPrefix[], String xmlnsNS[], String xmlnsXSD[],
        String resources[], String url, 
        String filterVariables[], String filterCops[], String filterValues[],
        Table resultsTable, String resultsVariables[]) throws Exception {
 
        long time = System.currentTimeMillis();
        String msg = "DigirHelper.searchDigir: " +
            "\n  resources=" + String2.toCSSVString(resources) +
            "\n  url=" + url +
            "\n  resultsVariables=" + String2.toCSSVString(resultsVariables) +
            "\n  filterVars=" + String2.toCSSVString(filterVariables) +
            "\n  filterCops=" + String2.toCSSVString(filterCops) +
            "\n  filterValues=" + String2.toCSSVString(filterValues);
        if (verbose) String2.log("\n" + msg);
        String errorInMethod = String2.ERROR + " in " + msg +
            "\n  error message: ";

        /* my by-hand test was:
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<request \n" +
        "  xmlns=\"http://digir.net/schema/protocol/2003/1.0\" \n" + //default namespace
        "  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" \n" +
        "  xmlns:darwin=\"http://digir.net/schema/conceptual/darwin/2003/1.0\" \n" +
        "  xmlns:obis=\"http://www.iobis.org/obis\" \n" +
        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
        "  xsi:schemaLocation=\"http://digir.net/schema/protocol/2003/1.0 \n" + //if not provided, waits, then returns provider info
        "    http://digir.sourceforge.net/schema/protocol/2003/1.0/digir.xsd \n" +  //these are pars of xmlns+xsd
        "    http://digir.net/schema/conceptual/darwin/2003/1.0 \n" +
        "    http://digir.sourceforge.net/schema/conceptual/darwin/2003/1.0/darwin2.xsd \n" +
        "    http://www.iobis.org/obis \n" +
        "    http://www.iobis.org/obis/obis.xsd\" >\n" +
        "  <header>\n" +
        "    <version>1.0</version>\n" +
        "    <sendTime>&CurrentDateTTime;</sendTime>\n" +
        "    <source>localhost</source>\n" +
        "    <destination resource=\"GHMP\">&url;</destination>\n" +  //is there an 'any' option? or more than 1?
        "    <type>search</type>\n" +
        "  </header>\n" +
        "  <search>\n" +
        "    <filter>\n" +
        "      <equals>\n" +
        "        <darwin:Genus>Macrocystis</darwin:Genus>\n" +
        "      </equals>\n" +
        "    </filter>\n" +
        "    <records limit=\"3\" start=\"0\">\n" + //never any limit, always start at 0
        "      <structure>\n" +
        "        <xsd:element name=\"record\">\n" +
        "          <xsd:complexType>\n" +
        "            <xsd:sequence>\n" +
        //you can't use e.g., obis, in place of darwin
        "              <xsd:element ref=\"darwin:InstitutionCode\"/>\n" +
        "              <xsd:element ref=\"darwin:CollectionCode\"/>\n" +
        "              <xsd:element ref=\"darwin:CatalogNumber\"/>\n" +
        "              <xsd:element ref=\"darwin:ScientificName\"/>\n" +
        "              <xsd:element ref=\"darwin:Latitude\"/>\n" +
        "              <xsd:element ref=\"darwin:Longitude\"/>\n" +
        "              <xsd:element ref=\"obis:Temperature\"/>\n" +
        "            </xsd:sequence>\n" +
        "          </xsd:complexType>\n" +
        "        </xsd:element>\n" +
        "      </structure>\n" +
        "    </records>\n" +
        "    <count>true</count>\n" +
        "  </search>\n" +
        "</request>\n";
        */

        //*** validate input
        //xml and filter arrays are tested by makeXxxRequest methods
        Test.ensureNotNull(resources, errorInMethod + "resources is null.");
        Test.ensureTrue(resources.length >= 1, 
            errorInMethod + "resources.length is less than 1.");
        Test.ensureNotNull(url, errorInMethod + "url is null.");

        Test.ensureNotNull(resultsVariables, errorInMethod + "resultsVariables is null.");
        Test.ensureTrue(resultsVariables.length >= 1, 
            errorInMethod + "resultsVariables.length is less than 1.");
        

        //*** create the request before the resource
        //NO LEADING SPACES, TO SAVE SPACE, TO AVOID HTTP REQUEST-TOO-LONG ERROR
        String request1 = getPreDestinationRequest(version, xmlnsPrefix, xmlnsNS, xmlnsXSD) +
            "    <destination"; // resource="resource[i]" //is there an 'any' option? or more than 1?


        //*** create the request after the resource
        //NO LEADING SPACES, TO SAVE SPACE, TO AVOID HTTP REQUEST-TOO-LONG ERROR
        StringBuilder request2sb = new StringBuilder();
        request2sb.append(">" + url + "</destination>\n" +  
            "    <type>search</type>\n" +
            "  </header>\n" +
            "  <search>\n" +
            getFilterRequest(filterVariables, filterCops, filterValues));


        //specify the results records
        //NO LEADING SPACES, TO SAVE SPACE, TO AVOID HTTP REQUEST-TOO-LONG ERROR
        request2sb.append(
            //for releases: 10000
            "    <records limit=\"10000\" start=\"0\" >\n" +  //always specify to avoid defaults (e.g., default limit=10)
            "      <structure>\n" +
            "        <xsd:element name=\"record\">\n" +
            "          <xsd:complexType>\n" +
            "            <xsd:sequence>\n");
        for (int i = 0; i < resultsVariables.length; i++) 
            request2sb.append(          
                "              <xsd:element ref=\"" + resultsVariables[i] + "\"/>\n");
        request2sb.append(
            "            </xsd:sequence>\n" +
            "          </xsd:complexType>\n" +
            "        </xsd:element>\n" +
            "      </structure>\n");

        //NO LEADING SPACES, TO SAVE SPACE, TO AVOID HTTP REQUEST-TOO-LONG ERROR
        request2sb.append(
            "    </records>\n" +
            "    <count>false</count>\n" + //count=true may be trouble for BMDE
            "  </search>\n" +
            "</request>\n");

        String request2 = request2sb.toString();
        if (reallyVerbose) String2.log("\nrequest1=" + request1 + "\n\nrequest2=" + request2);

        //if resultsTable has column, set it aside
        Table tTable = resultsTable;
        if (resultsTable.nColumns() > 0) 
            tTable = new Table();
       
        //TableXmlHandler.verbose = true;         
        XMLReader xmlReader = TableXmlHandler.getXmlReader(tTable, 
            false, //don't validate since no .dtd specified by DOCTYPE in the file
            "/response/content/record", null);

        //*** get data from each resource    
        //apparently, there is no "all" recourse option
        String diagnosticError = "";
        for (int resource = 0; resource < resources.length; resource++) {
            try {
                //get the xml for 1 resource from the provider
                long readTime = System.currentTimeMillis();
                String request = request1 + 
                    " resource=\"" + resources[resource] + "\"" + 
                    request2;

                //for testing: test that it is well-formed
                //can't validate, because no .dtd specified by DOCTYPE in file
                //?how validate against schema?
                //XML.parseXml(new StringReader(request), false); 
                //String2.pressEnterToContinue(request);
                request = String2.toSVString(String2.split(request, '\n'),  //split trims each string
                    " ", true); //(white)space is necessary to separate schemalocation names and locations
                //if (reallyVerbose) String2.log("\ncompactRequest=" + request + "\n");
                String response = SSR.getUrlResponseString(url + "?request=" + 
                    SSR.percentEncode(request));
                //for testing:
                //String2.writeToFile("c:/temp/SearchDigirResponse" + resource + ".xml", response);
                //String response = String2.readFromFile("c:/temp/SearchDigirResponse" + resource + ".xml")[1];

                if (verbose) String2.log(resources[resource] + 
                    " readTime=" + (System.currentTimeMillis() - readTime));
                if (reallyVerbose) 
                    String2.log(resources[resource] + " start of response=\n" + 
                        response.substring(0, Math.min(5000, response.length())));

                //xml is often incomplete (nothing below <content>), so xmlReader.parse often throws exception.
                //But if it doesn't throw exception, it was successful.
                //String2.log("Calling xmlReader.parse...");
                long parseTime = System.currentTimeMillis();
                int preNRows = tTable.nRows();

                xmlReader.parse(new InputSource(new StringReader(response)));
                if (verbose) String2.log("After " + resources[resource] + ", nRows=" + tTable.nRows() +
                    " parseTime=" + (System.currentTimeMillis() - parseTime));

                //look for error message
                if (preNRows == tTable.nRows()) {
                    int errorPo = response.indexOf("severity=\"fatal\"");
                    if (errorPo < 0)
                        errorPo = response.indexOf("severity=\"error\"");
                    if (errorPo > 0) {
                        int dCodePo = response.lastIndexOf("<diagnostic code", errorPo);
                        errorPo = dCodePo >= 0? dCodePo : Math.max(0, errorPo - 33);
                        String tError = String2.ERROR + " message from resource=" + 
                            resources[resource] + ":\n" + 
                            response.substring(errorPo) + "\n";
                        diagnosticError += tError + "\n";
                        String2.log(tError);
                    }
                }


            } catch (Exception e) {
                String tError = "EXCEPTION thrown by request to resource=" + resources[resource] + ":\n" + 
                    MustBe.throwableToString(e) + "\n"; 
                diagnosticError += tError + "\n";
                if (verbose) String2.log(tError);
            }

            //make all columns the same size 
            tTable.makeColumnsSameSize();
        }

        //throw exception because nRows = 0?
        int nr = tTable.nRows();
        if (nr == 0 && diagnosticError.length() > 0)
            Test.error(errorInMethod + "\n" + diagnosticError);

        //*** move the columns into place
        //insert columns that don't exist
        for (int col = 0; col < resultsVariables.length; col++) {
            String name = resultsVariables[col];
            int tCol = tTable.findColumnNumber(name);
            //String2.log("moving columns col=" + col + " name=" + name + " tCol=" + tCol);
            if (tCol < 0) {
                //insert an empty column
                tTable.addColumn(col, name, new StringArray(nr, true));
            } else if (tCol < col) {
                Test.error(errorInMethod + "Duplicate column name in " + 
                    String2.toCSSVString(tTable.getColumnNames()));
            } else if (tCol == col) {
                //do nothing
            } else {
                //move the column into place
                PrimitiveArray pa = tTable.getColumn(tCol);
                tTable.removeColumn(tCol);
                tTable.addColumn(col, name, pa);
            }
        }

        //remove columns not requested
        for (int col = tTable.nColumns() - 1; col >= resultsVariables.length; col--) {
            if (reallyVerbose) String2.log("  !!Removing extra column: " + tTable.getColumnName(col));
            tTable.removeColumn(col);
        }

        //merge with original table
        if (resultsTable != tTable) //yes, simple "!=" is appropriate
            resultsTable.append(tTable); //they will have same columns

        //done
        if (verbose) String2.log("  DigirHelper.searchDigir done. nColumns=" + resultsTable.nColumns() +
            " nRows=" + resultsTable.nRows() + " TIME=" + (System.currentTimeMillis() - time) + "\n");

    }

    /** 
     * This is like searchDigir, but customized for OBIS (which uses the DiGIR
     * engine and the Darwin2 and OBIS XML schemas). 
     * Since OBIS is a superset of Darwin2, you can use this for 
     *   Darwin2-based resources as well, as long as you only refer
     *   to Darwin2 (not obis) variables.
     * <p>Obis info: http://www.iobis.org .
     * <br>Obis schema info: http://www.iobis.org/tech/provider/questions .
     * <br>This sets column types and attributes based on info in 
     *   DigirDarwin2.properties and DigirObis.properties (in this directory).
     * <br>See searchDigir for the parameter descriptions.
     * <br>Valid variables (for filters and results) are listed in 
     *   DigirDarwin2.properties and DigirObis.properties.
     *
     * @param includeXYZT if true, this always includes columns:
     *   <ul>
     *   <li> 0) LON
     *   <li> 1) LAT
     *   <li> 2) DEPTH which has the values from darwin:MinimumDepth
     *   <li> 3) TIME (epochSeconds) created from YearCollected-MonthCollected-DayCollected and TimeOfDay.
     *   <li> 4) ID created from [darwin:InstitutionCode]:[darwin:CollectionCode]:[darwin:CatalogNumber]
     *   </ul>
     *   A problem is that the x,y,z,t columns may have missing values!
     *   <p>If false, this just includes the resultsVariables.
     * @param table data is appended to table.
     *    If table has data, it must have the same includeXYZT and resultsVariables.
     */
    public static void searchObis(String resources[], String url,  
        String filterVariables[], String filterCops[], String filterValues[],
        Table table, boolean includeXYZT, String resultsVariables[]) throws Exception {

        String errorInMethod = String2.ERROR + " in DigirHelper.searchObis: ";

        //pre check that filterVariables and resultsVariables are valid darwin or obis variables?
        String validVars[] = getDarwin2ObisVariables(); 
        for (int i = 0; i < resultsVariables.length; i++)
            if (String2.indexOf(validVars, resultsVariables[i]) < 0)
                Test.error(errorInMethod + "Unsupported resultsVariable=" + resultsVariables[i] + "\nValid=" + String2.toCSSVString(validVars));
        for (int i = 0; i < filterVariables.length; i++)
            if (String2.indexOf(validVars, filterVariables[i]) < 0)
                Test.error(errorInMethod + "Unsupported filterVariable=" + filterVariables[i] + "\nValid=" + String2.toCSSVString(validVars));

        //add the xyzt variables
        StringArray getVariables = new StringArray(resultsVariables);
        if (includeXYZT) {
            String needed[] = {DARWIN_PREFIX + ":Longitude", DARWIN_PREFIX + ":Latitude", DARWIN_PREFIX + ":MinimumDepth", 
                DARWIN_PREFIX + ":YearCollected", DARWIN_PREFIX + ":MonthCollected", DARWIN_PREFIX + ":DayCollected", 
                DARWIN_PREFIX + ":TimeOfDay", //DARWIN_PREFIX + ":Timezone"); //deal with???
                DARWIN_PREFIX + ":InstitutionCode", DARWIN_PREFIX + ":CollectionCode", DARWIN_PREFIX + ":CatalogNumber"};
            for (int i = 0; i < needed.length; i++)
                if (getVariables.indexOf(needed[i]) < 0) 
                    getVariables.add(needed[i]);
        }

        //if table already has data, set that table aside
        Table tTable = table;
        if (table.nColumns() > 0) 
            tTable = new Table();

        //get data from the provider
        searchDigir(OBIS_VERSION, OBIS_PREFIXES, OBIS_XMLNSES, OBIS_XSDES,
            resources, 
            url,  
            filterVariables, filterCops, filterValues,
            tTable, getVariables.toArray()); 

        //for testing:
        //tTable.saveAsFlatNc("c:/temp/searchObis" + includeXYZT + ".nc", "row");
        /* */
        //Table tTable = new Table();
        //tTable.readFlatNc("c:/temp/searchObis" + includeXYZT + ".nc", null, 1);

        int nRows = tTable.nRows();
        if (includeXYZT) {
            //create and add x,y,z,t,id columns    (numeric cols forced to be doubles)
            tTable.addColumn(0, DataHelper.TABLE_VARIABLE_NAMES[0], new DoubleArray(tTable.findColumn(DARWIN_PREFIX + ":Longitude")));
            tTable.addColumn(1, DataHelper.TABLE_VARIABLE_NAMES[1], new DoubleArray(tTable.findColumn(DARWIN_PREFIX + ":Latitude")));
            tTable.addColumn(2, DataHelper.TABLE_VARIABLE_NAMES[2], new DoubleArray(tTable.findColumn(DARWIN_PREFIX + ":MinimumDepth")));
            DoubleArray tPA = new DoubleArray(nRows, false);
            tTable.addColumn(3, DataHelper.TABLE_VARIABLE_NAMES[3], tPA);
            StringArray idPA = new StringArray(nRows, false);
            tTable.addColumn(4, DataHelper.TABLE_VARIABLE_NAMES[4], idPA);
            PrimitiveArray yearPA      = tTable.findColumn(DARWIN_PREFIX + ":YearCollected");
            PrimitiveArray monthPA     = tTable.findColumn(DARWIN_PREFIX + ":MonthCollected");
            PrimitiveArray dayPA       = tTable.findColumn(DARWIN_PREFIX + ":DayCollected");
            PrimitiveArray timeOfDayPA = tTable.findColumn(DARWIN_PREFIX + ":TimeOfDay");
//          PrimitiveArray timeZonePA = findColumn("Timezone"); //deal with ???
            //obis schema says to construct id as
            //"URN:catalog:[InstitutionCode]:[CollectionCode]:[CatalogNumber]"  but their example is more terse than values I see
            PrimitiveArray insPA       = tTable.findColumn(DARWIN_PREFIX + ":InstitutionCode");
            PrimitiveArray colPA       = tTable.findColumn(DARWIN_PREFIX + ":CollectionCode");
            PrimitiveArray catPA       = tTable.findColumn(DARWIN_PREFIX + ":CatalogNumber");
            for (int row = 0; row < nRows; row++) {
                //make the t value
                double seconds = Double.NaN;
                StringBuilder sb = new StringBuilder(yearPA.getString(row));
                if (sb.length() > 0) {
                    String tMonth = monthPA.getString(row);
                    if (tMonth.length() > 0) {
                        sb.append("-" + tMonth);  //month is 01 - 12 
                        String tDay = dayPA.getString(row);
                        if (tDay.length() > 0) {
                            sb.append("-" + tDay);
                        }
                    }
                    try { 
                        seconds = Calendar2.isoStringToEpochSeconds(sb.toString()); 
                        String tTime = timeOfDayPA.getString(row);  //decimal hours since midnight
                        int tSeconds = Math2.roundToInt(
                            String2.parseDouble(tTime) * Calendar2.SECONDS_PER_HOUR);
                        if (tSeconds < Integer.MAX_VALUE) seconds += tSeconds;
                    } catch (Exception e) {
                        if (reallyVerbose) String2.log("DigirHelper.searchObis unable to parse date=" + sb);
                    }
                }
                tPA.add(seconds);

                //make the id value
                idPA.add(insPA.getString(row) + ":" + colPA.getString(row) + ":" + catPA.getString(row));

            }

            //remove unneeded columns (related to x,y,z,t,id) at end
            for (int col = tTable.nColumns() - 1; col >= 5 + resultsVariables.length; col--)
                tTable.removeColumn(col);
        }

        //simplify the columns and add column metadata
        for (int col = includeXYZT? 5 : 0; col < tTable.nColumns(); col++) {
            String colName = tTable.getColumnName(col);
            String info =      
                colName.startsWith(DARWIN_PREFIX + ":")?    
                    digirDarwin2Properties.getString(colName.substring(DARWIN_PREFIX.length() + 1), null) : 
                colName.startsWith(OBIS_PREFIX + ":")?   
                    digirObisProperties.getString(colName.substring(OBIS_PREFIX.length() + 1), null) :
                null;
            Test.ensureNotNull(info, errorInMethod + "No info found for variable=" + colName);
            String infoArray[] = String2.split(info, '\f');

            //change column type from String to ?
            String type = infoArray[0];
            if (!type.equals("String") &&
                !type.equals("dateTime")) {
                //it's a numeric column
                PrimitiveArray pa = PrimitiveArray.factory(
                    PrimitiveArray.elementStringToClass(type), 8, false);
                pa.append(tTable.getColumn(col));
                tTable.setColumn(col, pa);

                //set actual_range?
            }

            //set column metadata
            String metadata[] = String2.split(infoArray[1], '`'); 
            for (int i = 0; i < metadata.length; i++) { 
                int eqPo = metadata[i].indexOf('=');  //first instance of '='
                Test.ensureTrue(eqPo > 0, errorInMethod + 
                    "Invalid metadata for colName=" + colName + ": " + metadata[i]);
                tTable.columnAttributes(col).set(metadata[i].substring(0, eqPo),
                    metadata[i].substring(eqPo + 1));
            }
        }

        //append tTable to table (columns are identical)
        if (tTable != table) { //yes, a simple "!=" test
            table.append(tTable); 
            tTable = null; //garbage collect it
        }

        //setAttributes  (this sets the coordinate variables' axis, long_name, standard_name, and units)
        String opendapQuery = getOpendapConstraint(new String[]{},
            filterVariables, filterCops, filterValues);
        int depthCol = table.findColumnNumber(DARWIN_PREFIX + ":MinimumDepth"); 
        if (depthCol == -1)
            depthCol = table.findColumnNumber(DARWIN_PREFIX + ":MaximumDepth"); 

        table.setObisAttributes(
            includeXYZT? 0 : table.findColumnNumber(DARWIN_PREFIX + ":Longitude"), 
            includeXYZT? 1 : table.findColumnNumber(DARWIN_PREFIX + ":Latitude"), 
            includeXYZT? 2 : depthCol, 
            includeXYZT? 3 : -1, //time column 
            url, resources, opendapQuery);
    }

    /** 
     * This is like searchDigir, but customized for BMDE (which uses the DiGIR
     * engine and the BMDE XML schema). 
     * Since BMDE is not a superset of Darwin, you can't use this for 
     *   Darwin-based resources as well.
     * <br>This works differently from searchObis -- 
     *   This doesn't make artificial xyzt variables.
     * <br>This sets column types and a few attributes, based on info in 
     *   DigirBmde.properties (in this directory).
     * <br>See searchDigir for the parameter descriptions.
     * <br>Valid variables (for filters and results) are listed in 
     *   DigirBmde.properties.
     *
     */
    public static Table searchBmde(String resources[], String url,  
        String filterVariables[], String filterCops[], String filterValues[],
        String resultsVariables[]) throws Exception {

        if (reallyVerbose) String2.log(
            "\n*** digirHelper.searchBmde resources=" + String2.toCSSVString(resources) +
            "\n  url=" + url +
            "\n  filterVars=" + String2.toCSSVString(filterVariables) +
            "\n  filterCops=" + String2.toCSSVString(filterCops) +
            "\n  filterVals=" + String2.toCSSVString(filterValues) +
            "\n  resultsVars=" + String2.toCSSVString(resultsVariables));

        String errorInMethod = String2.ERROR + " in DigirHelper.searchBmde: ";

        //pre check that filterVariables and resultsVariables are valid bmde variables?
        String validVars[] = getBmdeVariables(); 
        for (int i = 0; i < resultsVariables.length; i++)
            if (String2.indexOf(validVars, resultsVariables[i]) < 0)
                Test.error(errorInMethod + "Unsupported resultsVariable=" + resultsVariables[i] + "\nValid=" + String2.toCSSVString(validVars));
        for (int i = 0; i < filterVariables.length; i++)
            if (String2.indexOf(validVars, filterVariables[i]) < 0)
                Test.error(errorInMethod + "Unsupported filterVariable=" + filterVariables[i] + "\nValid=" + String2.toCSSVString(validVars));

        //get data from the provider
        Table table = new Table();
        searchDigir(BMDE_VERSION, BMDE_PREFIXES, BMDE_XMLNSES, BMDE_XSDES,
            resources, 
            url,  
            filterVariables, filterCops, filterValues,
            table, resultsVariables); 

        //simplify the columns and add column metadata
        int nCols = table.nColumns();
        int nRows = table.nRows();
        for (int col = 0; col < nCols; col++) {
            String colName = table.getColumnName(col);
            String info =      
                colName.startsWith(BMDE_PREFIX + ":")?   
                    digirBmdeProperties.getString(colName.substring(BMDE_PREFIX.length() + 1), null) :
                null;
            Test.ensureNotNull(info, errorInMethod + "No info found for variable=" + colName);
            String infoArray[] = String2.split(info, '\f');

            //change column type from String to ?
            String type = infoArray[0];
            if (!type.equals("String") &&
                !type.equals("dateTime")) {
                //it's a numeric column
                PrimitiveArray pa = PrimitiveArray.factory(
                    PrimitiveArray.elementStringToClass(type), nRows, false);
                pa.append(table.getColumn(col));
                table.setColumn(col, pa);

                //set actual_range?
            }

            //set column metadata
            String metadata[] = String2.split(infoArray[1], '`'); 
            for (int i = 0; i < metadata.length; i++) { 
                int eqPo = metadata[i].indexOf('=');  //first instance of '='
                Test.ensureTrue(eqPo > 0, errorInMethod + 
                    "Invalid metadata for colName=" + colName + ": " + metadata[i]);
                table.columnAttributes(col).set(metadata[i].substring(0, eqPo),
                    metadata[i].substring(eqPo + 1));
            }
        }

        table.globalAttributes().set("keywords", "Biological Classification > Animals/Vertebrates > Birds");
        table.globalAttributes().set("keywords_vocabulary", "GCMD Science Keywords");
        return table;
    }
        
        
    /**
     * This tests searchBmde.
     */
    public static void testBmde() throws Exception {
        verbose = true;
        reallyVerbose = true;
        Table.verbose = true;
        Table.reallyVerbose = true;
        try {

            Table table = searchBmde(new String[]{"prbo05"},
                "http://digir.prbo.org/digir/DiGIR.php",
                new String[]{"bmde:Family", "bmde:Genus", "bmde:ObservationDate", "bmde:ObservationDate"}, //"bmde:Class", 
                new String[]{"equals",      "equals",     "greaterThan",          "lessThan"}, //"equals"
                new String[]{"Laridae",     "Uria",       "2007-06-01",           "2007-06-05"}, //"Aves", 
                new String[]{"bmde:DecimalLongitude", "bmde:DecimalLatitude", 
                    "bmde:ObservationDate",
                    "bmde:GlobalUniqueIdentifier", "bmde:Genus", "bmde:ScientificName"});
            String fileName = "c:/temp/DigirHelperTestBmde.csv";
            table.saveAsCsvASCII(fileName);
            String results = String2.readFromFile(fileName)[1];
            String expected = 
"\"bmde:DecimalLongitude\",\"bmde:DecimalLatitude\",\"bmde:ObservationDate\",\"bmde:GlobalUniqueIdentifier\",\"bmde:Genus\",\"bmde:ScientificName\"\n" +
"\"degrees_east\",\"degrees_north\",\"\",\"\",\"\",\"\"\n" +
"-123.002737,37.698771,\"2007-06-01T17:00:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1171.1\",\"Uria\",\"Uria aalge\"\n" +
"-123.002737,37.698771,\"2007-06-02T17:00:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1172.1\",\"Uria\",\"Uria aalge\"\n" +
"-123.002737,37.698771,\"2007-06-03T17:49:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1173.1\",\"Uria\",\"Uria aalge\"\n" +
"-123.002737,37.698771,\"2007-06-04T17:00:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17156.SHORE1.COMUSubcolonyCount.1174.1\",\"Uria\",\"Uria aalge\"\n" +
"-123.002737,37.698771,\"2007-06-01T17:00:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1191.1\",\"Uria\",\"Uria aalge\"\n" +
"-123.002737,37.698771,\"2007-06-02T17:55:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1192.1\",\"Uria\",\"Uria aalge\"\n" +
"-123.002737,37.698771,\"2007-06-03T17:00:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1193.1\",\"Uria\",\"Uria aalge\"\n" +
"-123.002737,37.698771,\"2007-06-04T17:25:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17163.SHORE1.COMUSubcolonyCount.1194.1\",\"Uria\",\"Uria aalge\"\n" +
"-123.002737,37.698771,\"2007-06-01T17:10:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1616.1\",\"Uria\",\"Uria aalge\"\n" +
"-123.002737,37.698771,\"2007-06-02T17:49:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1617.1\",\"Uria\",\"Uria aalge\"\n" +
"-123.002737,37.698771,\"2007-06-03T17:00:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1618.1\",\"Uria\",\"Uria aalge\"\n" +
"-123.002737,37.698771,\"2007-06-04T17:00:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17164.SHORE1.COMUSubcolonyCount.1619.1\",\"Uria\",\"Uria aalge\"\n" +
"-123.002737,37.698771,\"2007-06-03T17:49:00Z\",\"URN:catalog:PRBO:prbo05:SEFI.17167.SHORE1.COMUSubcolonyCount.1184.1\",\"Uria\",\"Uria aalge\"\n";
             Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
                 expected, "results=" + results);

        //expected error didn't occur!
        String2.pressEnterToContinue("\n" + 
            MustBe.getStackTrace() + 
            "An expected error didn't occur at the above location."); 

        } catch (Exception e) {
            String2.log(
                "THIS STOPPED WORKING ~JAN 2009: " + MustBe.throwableToString(e) + 
                "\nI think Digir is dead."); 
            Math2.gc(5000);  //in a test, after displaying a message
        }

    }

    /**
     * This tests searchObis.
     */
    public static void testObis() throws Exception {
        verbose = true;
        reallyVerbose = true;
        Table.verbose = true;
        Table.reallyVerbose = true;

        try {
            String url = RUTGERS_OBIS_URL;
            //String url = CSIRO_OBIS_URL;

            //test ghmp for Macrocystis -- this used to work
            Table table;
            if (false) {  
                //THIS WORKED ON 2007-05-03
                table = new Table();
                searchObis(
                    //IMCS is the code for "Institute of Marine and Coastal Sciences, Rutgers University"
                    //It is listed for the "host", not as a resouce.
                    //but url response is it isn't available.
                    new String[]{"GHMP"},  //has lots       
                      //"aims_biotech","IndOBIS","EMAP"}, //"EMAP" has no data
                    url,
                    new String[]{"darwin:Genus"},
                    new String[]{"equals"},
                    new String[]{
                        //"Pterosiphonia"}, 
                        "Macrocystis"},
                        //"Carcharodon"}, // sp=carcharias
                    table, false,
                    new String[]{"darwin:InstitutionCode", "darwin:CollectionCode", "darwin:CatalogNumber",
                        "darwin:ScientificName", "darwin:Longitude", "darwin:Latitude",
                        "obis:Temperature"});
                String2.log("\nresulting table is: " + table);
                Test.ensureEqual(table.nRows(), 248, "");
            }

            if (false) {
                //THIS WORKED ON 2007-05-03
                table = new Table();
                searchObis(
                    new String[]{"GHMP"}, 
                    url,
                    new String[]{"darwin:Genus", "darwin:Latitude"}, //, "darwin:Latitude"},
                    new String[]{"equals",       "greaterThan"}, //,     "lessThan"},
                    new String[]{"Macrocystis",  "53"}, //,              "54"},
                    table, false,
                    new String[]{"darwin:InstitutionCode", "darwin:CollectionCode", "darwin:CatalogNumber",
                        "darwin:ScientificName", "darwin:Longitude", "darwin:Latitude",
                        "obis:Temperature"});
                String2.log("\nresulting table is: " + table);
                Test.ensureEqual(table.nRows(), 72, "");
            }

            if (false) {
                //this worked on 6/25/07, but stop doing tests that don't work
                String2.log("\n*** DigirHelper.testObis test of invalid filter variable");
                try {
                    table = new Table();
                    searchObis(
                        new String[]{"GHMP"},  
                        url,
                        new String[]{"darwin:Genus", "darwin:XYZ"},
                        new String[]{"equals",       "greaterThan"},
                        new String[]{"Macrocystis",  "53"},
                        table, false,
                        new String[]{"darwin:InstitutionCode"});
                    String2.log("Shouldn't get here."); Math2.sleep(60000);
                } catch (Exception e) {
                    String2.log(e.toString());
                    if (e.toString().indexOf("darwin:XYZ") < 0) throw e;
                    String2.log("\n*** diagnostic error (above) correctly caught\n");
                }

                String2.log("\n*** DigirHelper.testObis test of invalid COP");
                try {
                    table = new Table();
                    searchObis(
                        new String[]{"GHMP"},  
                        url,
                        new String[]{"darwin:Genus", "darwin:Latitude"},
                        new String[]{"equals",       "greaterTheen"},
                        new String[]{"Macrocystis",  "53"},
                        table, false,
                        new String[]{"darwin:InstitutionCode"});
                    String2.log("Shouldn't get here."); Math2.sleep(60000);
                } catch (Exception e) {
                    String2.log(e.toString());
                    if (e.toString().indexOf("greaterTheen") < 0) throw e;
                    String2.log("\n*** diagnostic error (above) correctly caught\n");
                }

                String2.log("\n*** DigirHelper.testObis test of invalid resultsVariable");
                try {
                    table = new Table();
                    searchObis(
                        new String[]{"GHMP"},  
                        url,
                        new String[]{"darwin:Genus", "darwin:Latitude"},
                        new String[]{"equals",       "greaterThan"},
                        new String[]{"Macrocystis",  "53"},
                        table, false,
                        new String[]{"darwin:InstitutionCoode"});
                    String2.log("Shouldn't get here."); Math2.sleep(60000);
                } catch (Exception e) {
                    String2.log(e.toString());
                    if (e.toString().indexOf("darwin:InstitutionCoode") < 0) throw e;
                    String2.log("\n*** diagnostic error (above) correctly caught\n");
                }

                String2.log("\n*** DigirHelper.testObis test of invalid resource");
                try {
                    table = new Table();
                    searchObis(
                        new String[]{"NONE"},  
                        url,
                        new String[]{"darwin:Genus", "darwin:Latitude"},
                        new String[]{"equals",       "greaterThan"},
                        new String[]{"Macrocystis",  "53"},
                        table, false,
                        new String[]{"darwin:InstitutionCode"});
                    String2.log("Shouldn't get here."); Math2.sleep(60000);
                } catch (Exception e) {
                    String2.log(e.toString());
                    if (e.toString().indexOf("<diagnostic") < 0) throw e;
                    String2.log("\n*** diagnostic error (above) correctly caught\n");
                }
            }

            if (false) {
                String2.log("\n*** DigirHelper.testObis test of includeXYZT=false");
                //THIS WORKED ON 2007-05-03 but doesn't anymore because of 
                //failure on obis server. 
                table = new Table();
                searchObis(
                    new String[]{"GHMP",  //note that requests for "IMCS" (which I hoped would mean "all") fail
                        "NONE"},    //"NONE" tests not failing if one resource fails
                    url,
                    new String[]{"darwin:Genus", "darwin:Latitude", "darwin:Latitude", "darwin:YearCollected", "darwin:YearCollected"},
                    new String[]{"equals",       "greaterThan",     "lessThan",        "greaterThanOrEquals",   "lessThanOrEquals"},
                    new String[]{"Macrocystis",  "53",              "54",              "1970",                  "2100"},
                    table, false,
                    new String[]{"darwin:InstitutionCode", "darwin:CollectionCode", "darwin:CatalogNumber",
                        "darwin:ScientificName", "darwin:Longitude", "darwin:Latitude",
                        "obis:Temperature"});
                String2.log("\nresulting table is: " + table);
                Test.ensureTrue(table.nRows() >= 60, "nRows=" + table.nRows());
                Test.ensureEqual(table.nColumns(), 7, "");
                Test.ensureEqual(String2.toCSSVString(table.getColumnNames()),
                    "darwin:InstitutionCode, darwin:CollectionCode, darwin:CatalogNumber, " +
                    "darwin:ScientificName, darwin:Longitude, darwin:Latitude, " +
                    "obis:Temperature",
                    "");
                //!!!note that rows of data are in pairs of almost duplicates
                //and CollectionCode includes 2 sources -- 1 I requested and another one (both served by GHMP?)
                //and Lat and Lon can be slightly different (e.g., row 60/61 lat)
                DoubleArray latCol = (DoubleArray)table.getColumn(5);
                double stats[] = latCol.calculateStats();
                Test.ensureTrue(stats[PrimitiveArray.STATS_MIN] >= 53, "min=" + stats[PrimitiveArray.STATS_MIN]);
                Test.ensureTrue(stats[PrimitiveArray.STATS_MAX] <= 54, "max=" + stats[PrimitiveArray.STATS_MAX]);
                Test.ensureEqual(stats[PrimitiveArray.STATS_N], table.nRows(), "");

                //CollectionYear not in results
                //DoubleArray timeCol = (DoubleArray)table.getColumn(3);
                //stats = timeCol.calculateStats();
                //Test.ensureTrue(stats[PrimitiveArray.STATS_MIN] >= 0, "min=" + stats[PrimitiveArray.STATS_MIN]);

                StringArray catCol = (StringArray)table.getColumn(2);
                int row = catCol.indexOf("10036-MACRINT");  //==0
                Test.ensureEqual(table.getStringData(0, row  ), "Marine Fish Division, Fisheries and Oceans Canada", "");
                Test.ensureEqual(table.getStringData(1, row  ), "Gwaii Haanas Marine Algae", "");
                Test.ensureEqual(table.getStringData(2, row  ), "10036-MACRINT", "");
                Test.ensureEqual(table.getStringData(3, row  ), "Macrocystis integrifolia", "");
                Test.ensureEqual(table.getDoubleData(4, row  ), -132.4223, "");
                Test.ensureEqual(table.getDoubleData(5, row  ), 53.292, "");
                Test.ensureEqual(table.getDoubleData(6, row  ), Double.NaN, "");

                Test.ensureEqual(table.getStringData(0, row+1), "BIO", "");
                Test.ensureEqual(table.getStringData(1, row+1), "GHMP", "");
                Test.ensureEqual(table.getStringData(2, row+1), "10036-MACRINT", "");
                Test.ensureEqual(table.getStringData(3, row+1), "Macrocystis integrifolia", "");
                Test.ensureEqual(table.getDoubleData(4, row+1), -132.4223, "");
                Test.ensureEqual(table.getDoubleData(5, row+1), 53.292, "");
                Test.ensureEqual(table.getDoubleData(6, row+1), Double.NaN, "");

                row = catCol.indexOf("198-MACRINT"); 
                Test.ensureEqual(table.getStringData(0, row  ), "Marine Fish Division, Fisheries and Oceans Canada", "");
                Test.ensureEqual(table.getStringData(1, row  ), "Gwaii Haanas Marine Algae", "");
                Test.ensureEqual(table.getStringData(2, row  ), "198-MACRINT", "");
                Test.ensureEqual(table.getStringData(3, row  ), "Macrocystis integrifolia", "");
                Test.ensureEqual(table.getDoubleData(4, row  ), -132.08171, "");
                Test.ensureEqual(table.getDoubleData(5, row  ), 53.225193, "");
                Test.ensureEqual(table.getDoubleData(6, row  ), Double.NaN, "");

                Test.ensureEqual(table.getStringData(0, row+1), "BIO", "");
                Test.ensureEqual(table.getStringData(1, row+1), "GHMP", "");
                Test.ensureEqual(table.getStringData(2, row+1), "198-MACRINT", "");
                Test.ensureEqual(table.getStringData(3, row+1), "Macrocystis integrifolia", "");
                Test.ensureEqual(table.getDoubleData(4, row+1), -132.08171, "");
                Test.ensureEqual(table.getDoubleData(5, row+1), 53.22519, "");
                Test.ensureEqual(table.getDoubleData(6, row+1), Double.NaN, "");

            }
            
            if (false) {
                String2.log("\n*** DigirHelper.testObis test of includeXYZT=true");
                //THIS WORKED ON 2007-05-04 but has failed most of the time since:
                //  <diagnostic code="Unknown PHP Error [2]" severity="DIAG_WARNING">odbc_pconnect()
                //  : SQL error: [Microsoft][ODBC Driver Manager] Data source name not found and no
                //  default driver specified, SQL state IM002 in SQLConnect (D:\DiGIR_Phoebe\DiGIRpr
                //  ov\lib\adodb\drivers\adodb-odbc.inc.php:173)</diagnostic>
                //  <diagnostic code="INTERNAL_DATABASE_ERROR" severity="fatal">A connection to the
                //  database could not be created.</diagnostic>
                //  </diagnostics></response>
                String testName = "c:/temp/ObisMac5354.nc";
                table = new Table();
                searchObis(
                    new String[]{"GHMP"},   
                        //note that requests for "IMCS" (which I hoped would mean "all") fail
                        //ARC throws no error, but returns 0 records
                        //not avail: "Gwaii Haanas Marine Algae"
                    url,
                    new String[]{"darwin:Genus", "darwin:Latitude", "darwin:Latitude", "darwin:YearCollected", "darwin:YearCollected"},
                    new String[]{"equals",       "greaterThan",     "lessThan",        "greaterThanOrEquals",  "lessThanOrEquals"},
                    new String[]{"Macrocystis",  "53",              "54",              "1970",                 "2100"},
                    table, true,
                    new String[]{"darwin:InstitutionCode", "darwin:CollectionCode", 
                        "darwin:ScientificName", "obis:Temperature"});
                String2.log("\nresulting table is: " + table);

                //test contents
                table.testObis5354Table();

                //test reading as NetcdfDataset
                table.saveAsFlatNc(testName, "row");
                NetcdfDataset ncd = NetcdfDataset.openDataset(testName);
                try {
                    Object o[] = ncd.getCoordinateAxes().toArray();
                    String so = String2.toCSSVString(o);
                    String2.log("axes=" + so);
                    Test.ensureEqual(o.length, 4, "");
                    Test.ensureTrue(so.indexOf("_CoordinateAxisType = \"Lon\"") >= 0, "");
                    Test.ensureTrue(so.indexOf("_CoordinateAxisType = \"Lat\"") >= 0, "");
                    Test.ensureTrue(so.indexOf("_CoordinateAxisType = \"Height\"") >= 0, "");
                    Test.ensureTrue(so.indexOf("_CoordinateAxisType = \"Time\"") >= 0, "");

                } finally {           
                    ncd.close();
                }
            }

            //test iziko Fish for Carcharodon
            if (false) {
                table = new Table();
                searchObis(
                    //IMCS is the code for "Institute of Marine and Coastal Sciences, Rutgers University"
                    //It is listed for the "host", not as a resouce.
                    //but url response is it isn't available.
                    new String[]{"iziko Fish"}, 
                    url,
                    new String[]{"darwin:Genus"},
                    new String[]{"equals"},
                    new String[]{
                        //"Pterosiphonia"}, 
                        //"Macrocystis"},
                        "Carcharodon"}, // sp=carcharias
                    table, false,
                    new String[]{"darwin:InstitutionCode", "darwin:CollectionCode", "darwin:CatalogNumber",
                        "darwin:ScientificName", "darwin:Longitude", "darwin:Latitude", 
                        "obis:Temperature"});
                String2.log("\nresulting table is: " + table);
                Test.ensureEqual(table.nRows(), 248, "");
            }

            //indobis is more reliable, but doesn't support numeric tests of lat lon
            if (false) {  
                url = IND_OBIS_URL;
                table = new Table();
                searchObis(
                    new String[]{"indobis"}, 
                    url,
                    new String[]{"darwin:Genus"},
                    new String[]{"equals"},
                    new String[]{"Carcharodon"}, // sp=carcharias
                    table, false,
                    new String[]{"darwin:InstitutionCode", "darwin:CollectionCode", "darwin:CatalogNumber",
                        "darwin:ScientificName", "darwin:Longitude", "darwin:Latitude",
                        "obis:Temperature"});
                String2.log("\nresulting table is: " + table);
    //    Row  darwin:Institu darwin:Collect darwin:Catalog darwin:Scienti darwin:Longitu darwin:Latitud obis:Temperatu
    //      0             NCL INDOBIS-DATASE         101652 Carcharodon ca      55.666667      -4.583333            NaN
    //      1             NCL INDOBIS-DATASE         101652 Carcharodon ca      18.583334     -34.133335            NaN
    //      2             NCL INDOBIS-DATASE         101652 Carcharodon ca              0              0            NaN
                Test.ensureEqual(table.nColumns(), 7, "");
                Test.ensureEqual(table.nRows(), 7, "");
                Test.ensureEqual(String2.toCSSVString(table.getColumnNames()), 
                    "darwin:InstitutionCode, darwin:CollectionCode, darwin:CatalogNumber, " +
                    "darwin:ScientificName, darwin:Longitude, darwin:Latitude, obis:Temperature", 
                    "");
                Test.ensureEqual(table.getStringData(0, 0), "NCL", "");
                Test.ensureEqual(table.getStringData(1, 0), "INDOBIS-DATASET1", "");
                Test.ensureEqual(table.getStringData(2, 0), "101652", "");
                Test.ensureEqual(table.getStringData(3, 0), "Carcharodon carcharias", "");
                Test.ensureEqual(table.getFloatData( 4, 0), 55.666667f, "");
                Test.ensureEqual(table.getFloatData( 5, 0), -4.583333f, "");
                Test.ensureEqual(table.getFloatData( 6, 0), Float.NaN, "");
                Test.ensureEqual(table.getStringData(0, 1), "NCL", "");
                Test.ensureEqual(table.getStringData(1, 1), "INDOBIS-DATASET1", "");
                Test.ensureEqual(table.getStringData(2, 1), "101652", "");
                Test.ensureEqual(table.getStringData(3, 1), "Carcharodon carcharias", "");
                Test.ensureEqual(table.getFloatData( 4, 1), 18.583334f, "");
                Test.ensureEqual(table.getFloatData( 5, 1), -34.133335f, "");
                Test.ensureEqual(table.getFloatData( 6, 1), Float.NaN, "");
                Test.ensureEqual(table.getStringData(0, 2), "NCL", "");
                Test.ensureEqual(table.getStringData(1, 2), "INDOBIS-DATASET1", "");
                Test.ensureEqual(table.getStringData(2, 2), "101652", "");
                Test.ensureEqual(table.getStringData(3, 2), "Carcharodon carcharias", "");
                Test.ensureEqual(table.getFloatData( 4, 2), 0f, "");  //missing value?!
                Test.ensureEqual(table.getFloatData( 5, 2), 0f, "");
                Test.ensureEqual(table.getFloatData( 6, 2), Float.NaN, "");
            }

            //flanders  -- This works reliably. It is my main test.
            if (true) {  
                url = FLANDERS_OBIS_URL;
                table = new Table();
                searchObis(
                    new String[]{"tisbe"}, 
                    url,
                    new String[]{"darwin:ScientificName", "darwin:Longitude"},
                    new String[]{"equals", "lessThan"},
                    new String[]{"Abietinaria abietina", "2"}, 
                    table, false,
                    new String[]{"darwin:InstitutionCode", "darwin:CollectionCode", "darwin:CatalogNumber",
                        "darwin:ScientificName", "darwin:Longitude", "darwin:Latitude",
                        "obis:Temperature"});
//pre 2010-07-27 was 4 rows:               
//Row  darwin:Institu darwin:Collect darwin:Catalog darwin:Scienti darwin:Longitu darwin:Latitud obis:Temperatu
//  0            VLIZ          Tisbe         405003 Abietinaria ab           1.57      50.849998            NaN
//  1            VLIZ          Tisbe         405183 Abietinaria ab          -20             40              NaN
//  2            VLIZ          Tisbe         415428 Abietinaria ab           1.95          51.23            NaN
//  3            VLIZ          Tisbe         562956 Abietinaria ab           1.62          50.77            NaN

                String results = table.dataToString();
                String expected =
"darwin:InstitutionCode,darwin:CollectionCode,darwin:CatalogNumber,darwin:ScientificName,darwin:Longitude,darwin:Latitude,obis:Temperature\n" +
"VLIZ,Tisbe,405183,Abietinaria abietina,-20.0,46.0,\n" +
"VLIZ,Tisbe,415428,Abietinaria abietina,1.95,51.229,\n" +
"VLIZ,Tisbe,562956,Abietinaria abietina,1.615055,50.77295,\n";
                Test.ensureEqual(results, expected, "results=\n" + results);
            }

        } catch (Exception e) {
            String2.pressEnterToContinue(MustBe.throwableToString(e) + 
                "\nUNEXPECTED DigirHelper " + String2.ERROR); 

        }


    }


    /** 
     * This is like the other searchObis, but processes an opendap-style query.
     *  
     * <p>The first 5 columns in the results table are automatically LON, LAT, DEPTH, TIME, and ID
     *
     * @param resources see searchDigir's resources parameter
     * @param url see searchDigir's url parameter
     * @param query is the opendap-style query, e.g.,
     *    <tt>var1,var2,var3&amp;var4=value4&amp;var5&amp;gt;=value5</tt> .
     *    Note that the query must be in its unencoded form, with
     *    ampersand, greaterThan and lessThan characters as single characters.
     *    A more specific example is 
     *    <tt>darwin:Genus,darwin:Species&amp;darwin:Genus=Macrocystis&amp;darwin:Latitude&gt;=53&amp;darwin:Latitude&lt;=54</tt> .
     *    Note that each constraint's left hand side must be a variable
     *      and its right hand side must be a value.
     *    See searchDigir's parameter descriptions for filterVariables, filterCops, and filterValues,
     *      except there is currently no support for "in" here.
     *    The valid string variable COPs are "=", "!=", "~=".
     *    The valid numeric variable COPs are "=", "!=", "&lt;", "&lt;=",  
     *        "&gt;", "&gt;=").
     *    "~=" (which would normally match a regular expression on the right hand side)
     *      is translated to "like".
     *    "like" supports "%" (a wildcard) at the beginning and/or end of the value.
     *    Although you can put constraints on any Darwin variable (see DigirDarwin.properties)
     *      or OBIS variable (see DigirObis.properties), most variables have little or no data,
     *      so extensive requests will generate few or no results rows.
     * @param table the results are appended to table (and metadata is updated).
     */
    public static void searchObisOpendapStyle(String resources[], String url,  
        String query, Table table) throws Exception {

        StringArray filterVariables  = new StringArray();
        StringArray filterCops       = new StringArray();
        StringArray filterValues     = new StringArray();
        StringArray resultsVariables = new StringArray();
        parseQuery(query, resultsVariables, filterVariables, filterCops, filterValues);

        searchObis(resources, url,  
            filterVariables.toArray(), filterCops.toArray(), filterValues.toArray(),
            table, true, resultsVariables.toArray());
    }


    /**
     * This parses the query for searchOpendapStyleObis.
     *
     * @param query see searchOpendapStyleObis's query
     * @param resultsVariables to be appended with the results variables
     * @param filterVariables to be appended with the filter variables
     * @param filterCops to be appended with the filter comparative operators
     * @param filterValues to be appended with the filter values
     * @throws Exception if invalid query
     *     (0 resultsVariables is a valid query)
     */
    public static void parseQuery(String query, StringArray resultsVariables,
        StringArray filterVariables, StringArray filterCops, StringArray filterValues) {

        String errorInMethod = String2.ERROR + " in DigirHelper.parseQuery:\n(query=" + query + ")\n";
        if (query.charAt(query.length() - 1) == '&')
            Test.error(errorInMethod + "query ends with ampersand.");

        //get the comma-separated vars    before & or end-of-query
        int ampPo = query.indexOf('&');
        if (ampPo < 0) ampPo = query.length();
        int startPo = 0;
        int stopPo = query.indexOf(',');
        if (stopPo < 0 || stopPo > ampPo) stopPo = ampPo;
        while (startPo < ampPo) {
            if (stopPo == startPo) //catch ",," in query
                Test.error(errorInMethod + 
                    "Missing results variable at startPo=" + startPo + ".");
            resultsVariables.add(query.substring(startPo, stopPo).trim());
            startPo = stopPo + 1;
            stopPo = startPo >= ampPo? ampPo : query.indexOf(',', startPo);
            if (stopPo < 0 || stopPo > ampPo) stopPo = ampPo;
        }
        //String2.log("resultsVariables=" + resultsVariables);

        //get the constraints 
        //and convert to ("equals", "notEquals", "like", "lessThan", "lessThanOrEquals",  
        //  "greaterThan", "greaterThanOrEquals").
        ampPo = query.indexOf('&', startPo);
        if (ampPo < 0) ampPo = query.length();
        while (startPo < query.length()) {
            String filter = query.substring(startPo, ampPo);
            //String2.log("filter=" + filter);

            //find the op
            int op = 0;
            int opPo = -1;
            while (op < COP_SYMBOLS.length && 
                (opPo = filter.indexOf(COP_SYMBOLS[op])) < 0)
                op++;
            if (opPo < 0) 
                Test.error(errorInMethod + "No operator found in filter at startPo=" + 
                    startPo + " filter=" + filter + ".");
            filterVariables.add(filter.substring(0, opPo).trim());
            filterCops.add(COP_NAMES[op]);
            filterValues.add(filter.substring(opPo + COP_SYMBOLS[op].length()).trim());

            //remove start/end quotes from filterValues
            for (int i = 0; i < filterValues.size(); i++) {
                String fv = filterValues.get(i);
                if (fv.startsWith("\"") && fv.endsWith("\""))
                    filterValues.set(i, fv.substring(1, fv.length() - 2).trim());
                else if (fv.startsWith("'") && fv.endsWith("'"))
                    filterValues.set(i, fv.substring(1, fv.length() - 2).trim());
            }

            startPo = ampPo + 1;
            ampPo = startPo >= query.length()? query.length() : query.indexOf('&', startPo);
            if (ampPo < 0) ampPo = query.length();
        }

        if (reallyVerbose) {
            String2.log("Output from parseQuery:" +
                "\n  resultsVariables=" + resultsVariables +
                "\n  filterVariables=" + filterVariables +
                "\n  filterCops=" + filterCops +
                "\n  filterValues=" + filterValues);
        }
    }

    /**
     * This formats the filters as an opendap constraint.
     *
     * @param resultsVariables
     * @param filterVariables 
     * @param filterCops   all should be valid COP_NAMES
     * @param filterValues 
     * @return e.g., obis:Temperature,darwin:ScientificName&darwin:Genus=Macrocystis&darwin:Species=integrifolia
     */
    public static String getOpendapConstraint(String resultsVariables[],
        String filterVariables[], String filterCops[], String filterValues[]) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < resultsVariables.length; i++) {
            if (i > 0) 
                sb.append(',');
            sb.append(resultsVariables[i]);
        }

        for (int i = 0; i < filterVariables.length; i++) {
            int op = String2.indexOf(COP_NAMES, filterCops[i]);
            if (op < 0) 
                Test.error(String2.ERROR + " in DigirHelper.getOpendapConstraint:\n" +
                    "Invalid operator=" + filterCops[i] + ".");
            sb.append("&" + filterVariables[i] + COP_SYMBOLS[op] + filterValues[i]);
        }
        
        return sb.toString();
    }



    /** 
     * This tests searchOpendapStyleObis().
     */
    public static void testOpendapStyleObis() throws Exception {
        Table table = new Table();
        Table.verbose = true;
        Table.reallyVerbose = true;
        verbose = true;
        reallyVerbose = true;
        try {

            //these invalid queries are caught locally
            String2.log("\n*** DigirHelper.testOpendapStyleObis test of unknown op");
            try {
                searchObisOpendapStyle(
                    new String[]{"GHMP"},
                    RUTGERS_OBIS_URL,
                    "darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature" +
                    "&darwin:Genus!Macrocystis&darwin:Latitude>53&darwin:Latitude<54",
                    table);
                String2.log("Shouldn't get here."); Math2.sleep(60000);
            } catch (Exception e) {
                String2.log(e.toString());
                String2.log("\n*** diagnostic error (above) correctly caught\n");
            }
            
            String2.log("\n*** DigirHelper.testOpendapStyleObis test of empty filter at beginning");
            try {
                searchObisOpendapStyle(
                    new String[]{"GHMP"},
                    RUTGERS_OBIS_URL,
                    "darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature" +
                    "&&darwin:Genus=Macrocystis&darwin:Latitude>53&darwin:Latitude<54",
                    table);
                String2.log("Shouldn't get here."); Math2.sleep(60000);
            } catch (Exception e) {
                String2.log(e.toString());
                String2.log("\n*** diagnostic error (above) correctly caught\n");
            }
            
            String2.log("\n*** DigirHelper.testOpendapStyleObis test of empty filter at end");
            try {
                searchObisOpendapStyle(
                    new String[]{"GHMP"},
                    RUTGERS_OBIS_URL,
                    "darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature" +
                    "&darwin:Genus=Macrocystis&darwin:Latitude>53&darwin:Latitude<54&",
                    table);
                String2.log("Shouldn't get here."); Math2.sleep(60000);
            } catch (Exception e) {
                String2.log(e.toString());
                String2.log("\n*** diagnostic error (above) correctly caught\n");
            }
            
            String2.log("\n*** DigirHelper.testOpendapStyleObis test of invalid var name");
            try {
                searchObisOpendapStyle(
                    new String[]{"GHMP"},
                    RUTGERS_OBIS_URL,
                    "darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature" +
                    "&darwin:Genus=Macrocystis&darwin:Latitude>53&darwin:Laatitude<54",
                    table);
                String2.log("Shouldn't get here."); Math2.sleep(60000);
            } catch (Exception e) {
                String2.log(e.toString());
                String2.log("\n*** diagnostic error (above) correctly caught\n");
            }
            
            //experiment (not normally run)
            if (false) {
                table.clear();
                String2.log("\n*** DigirHelper.testOpendapStyleObis test of experiment");
                searchObisOpendapStyle(
                    new String[]{"OBIS-SEAMAP"},
                    RUTGERS_OBIS_URL,
                    "obis:Temperature,darwin:ScientificName" +
                    "&darwin:ScientificName=Caretta caretta" +
                    "&darwin:YearCollected=2005",
                    table);

                String2.log("\nresulting table is: " + table);
                table.testObis5354Table();
            }

            //valid request,   standard test
            //but I have stopped testing rutgers because it is down so often
            if (false) {
                table.clear();
                String2.log("\n*** DigirHelper.testOpendapStyleObis test of valid request");
                searchObisOpendapStyle(
                    new String[]{"GHMP"},
                    RUTGERS_OBIS_URL,
                    "darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature" +
                    "&darwin:Genus=Macrocystis&darwin:Latitude>53&darwin:Latitude<54" +
                    "&darwin:YearCollected>=1970&darwin:YearCollected<=2100",
                    table);

                String2.log("\nresulting table is: " + table);
                table.testObis5354Table();
            }

            //valid request of indobis   but indobis treats lat lon queries as strings!
            if (false) {
                table.clear();
                String2.log("\n*** DigirHelper.testOpendapStyleObis test of valid request");
                searchObisOpendapStyle(
                    new String[]{"indobis"},
                    IND_OBIS_URL,
                    "darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature" +
                    "&darwin:Genus=Carcharodon"
                    + "&darwin:Latitude>=-10&darwin:Latitude<=0" //ERROR: QUERY_TERM_TOO_SHORT
                    ,
                    table);

                String2.log("\nresulting table is: " + table);
                //testObisCarcharodonTable(table);
            }

            //test flanders   This is a reliable test that I normally use.
            try {
                table.clear();
                String2.log("\n*** DigirHelper.testOpendapStyleObis test flanders");
                searchObisOpendapStyle(
                    new String[]{"tisbe"},
                    FLANDERS_OBIS_URL,
                    "darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature" +
                    "&darwin:Genus=Abietinaria&darwin:Longitude<2",
                    table);

                testObisAbietinariaTable(table);
            } catch (Exception e) {
                String2.pressEnterToContinue(MustBe.throwableToString(e) + 
                    "\nUnexpected DigirHelper error"); 

            }
        } catch (Exception e) {
            String2.pressEnterToContinue(MustBe.throwableToString(e) + 
                "\nUnexpected error."); 
        }
    }

    /**
     * This ensures that the table is the standard result from 
     * indobis request for genus=Carcharodon from lat -10 to 0.
     *
     * @throws Exception if unexpected value found in table.
     */
    public static void testObisAbietinariaTable(Table table) {
        String2.log("\nresulting table is: " + table);
        Test.ensureEqual(String2.toCSSVString(table.getColumnNames()), 
            "LON, LAT, DEPTH, TIME, ID, darwin:InstitutionCode, darwin:CollectionCode, "+
            "darwin:ScientificName, obis:Temperature", 
            "");

        String results = table.dataToString();
        String expected =
//pre 2010-07-27 was 7 rows
"LON,LAT,DEPTH,TIME,ID,darwin:InstitutionCode,darwin:CollectionCode,darwin:ScientificName,obis:Temperature\n" +
"-20.0,46.0,,,VLIZ:Tisbe:405183,VLIZ,Tisbe,Abietinaria abietina,\n" +
"1.606355,50.73067,,,VLIZ:Tisbe:407878,VLIZ,Tisbe,Abietinaria filicula,\n" +
"-4.54935,54.2399,,,VLIZ:Tisbe:411870,VLIZ,Tisbe,Abietinaria filicula,\n" +
"1.95,51.229,,,VLIZ:Tisbe:415428,VLIZ,Tisbe,Abietinaria abietina,\n" +
"1.615055,50.77295,,,VLIZ:Tisbe:562956,VLIZ,Tisbe,Abietinaria abietina,\n";
        Test.ensureEqual(results, expected, "results=\n" + results);

    }

/*
 future: higher level method for obis.
   and higher level for obis with
     *    [Note that the procedure always makes columns for x,y,z,t,id:  should it???
     *    z is from Minimumdepth
     *    t is generated from Yearcollected|Monthcollected|Daycollected|???.
     *    id is Res_name:Catalognumber ???.
     
*/


    /**
     * This processes a Digir search request and returns XML response.
     * 
     * @param request a Digir search request xml string
     * @return a Digir response xml string
     * @throws Exception if trouble
     */
    public static String processDigirSearchRequest(String request) throws Exception {
        //parse the request

        //get the data from opendap

        //format as Digir response xml
        StringBuilder response = new StringBuilder();

        return response.toString();
    }


    /**
     * This tests the methods in this class.
     *
     * @throws Exception if trouble
     */
    public static void test() throws Exception {

        String2.log("\n***** DigirHelper.test");
        Table.verbose = true;
        Table.reallyVerbose = true;
        verbose = true;
        reallyVerbose = true;

/*        //one time things
        String2.log(String2.noLongLinesAtSpace(
            "Available genera (and number of records): " + getObisInventoryString(
                "http://aadc-maps.aad.gov.au/digir/digir.php",
                "argos_tracking",
                "darwin:ScientificName"), //"darwin:Genus"), 
            72, ""));
        if (true) System.exit(0);
/* */

        //test parseQuery
        StringArray resultsVariables = new StringArray();
        StringArray filterVariables = new StringArray();
        StringArray filterCops = new StringArray();
        StringArray filterValues = new StringArray();
        String query = "darwin:Longitude,darwin:Latitude&darwin:Genus!=Bob" +
            "&darwin:Genus~=%rocystis&darwin:Latitude<=54&darwin:Latitude>=53" +
            "&darwin:Longitude=0&darwin:Latitude<78&darwin:Latitude>77" +
            "&darwin:Species in option1,option2,option3"; 
        parseQuery(query, resultsVariables, filterVariables, filterCops, filterValues);
        Test.ensureEqual(resultsVariables.toString(), "darwin:Longitude, darwin:Latitude", "");
        Test.ensureEqual(filterVariables.toString(), 
            "darwin:Genus, darwin:Genus, darwin:Latitude, darwin:Latitude, " +
            "darwin:Longitude, darwin:Latitude, darwin:Latitude, " +
            "darwin:Species", "");
        Test.ensureEqual(filterCops.toString(), 
            String2.toCSSVString(COP_NAMES), "");
        Test.ensureEqual(filterValues.toString(), 
            "Bob, %rocystis, 54, 53, 0, 78, 77, \"option1,option2,option3\"", "");

        //test getOpendapConstraint
        filterCops.set(7, "in");
        Test.ensureEqual(getOpendapConstraint(resultsVariables.toArray(), 
            filterVariables.toArray(), COP_NAMES, filterValues.toArray()),
            query, "");

        //This works, but takes a long time and isn't directly used by
        //  the methods which get obis data, so don't test all the time.
        //testGetMetadata(); 

        //2014-08-06 REMOVED dataset no longer available: testGetInventory();
        //2014-08-06 REMOVED dataset no longer available: testObis();
        //2014-08-06 REMOVED dataset no longer available: testOpendapStyleObis();
        testBmde(); 

        //done
        String2.log("\n***** DigirHelper.test finished successfully");

    }


}
