/*
 * DigirHelper Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.util.*;
import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.util.SSR;
import java.io.BufferedReader;
import java.io.StringReader;
import javax.xml.xpath.XPath; // requires java 1.5
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * This class has methods to help dealing with DiGIR (and Darwin and OBIS). DiGIR is an engine which
 * takes XML requests for data from a (usually database) table and returns a subset table stored as
 * XML data (as defined in a schema).
 *
 * <p>Digir information: http://digir.sourceforge.net/ Most useful info about the whole Digir
 * system: http://diveintodigir.ecoforge.net/draft/digirdive.html and
 * http://digir.net/prov/prov_manual.html . A list of Digir providers:
 * http://bigdig.ecoforge.net/wiki/SchemaStatus .
 *
 * <p>Darwin is the original schema for use with the Digir engine.
 *
 * <p>OBIS is an oceanography-related schema which extends Darwin. Obis info: http://www.iobis.org .
 * Obis schema info: http://www.iobis.org/tech/provider/questions .
 *
 * <p>BMDE is an avian-related schema which extends Darwin. BMDE info:
 * http://www.avianknowledge.net/content/ BMDE schema:
 * http://akn.ornith.cornell.edu/Schemas/bmde/BMDE-Bandingv1.38.08.xsd
 *
 * <p>The XML namespace uri's (xxx_XMLNS) and XML schemas xxx_XSD) are stored as constants.
 *
 * <p>!!!In addition to the various classes this uses, this needs two properties files:
 * gov/noaa/pfel/coastwatch/pointdata/DigirDarwin2.properties and
 * gov/noaa/pfel/coastwatch/pointdata/DigirObis.properties.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-05-07
 */
public class DigirHelper {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;

  public static ResourceBundle2 digirDarwin2Properties =
      new ResourceBundle2("gov.noaa.pfel.coastwatch.pointdata.DigirDarwin2");
  public static ResourceBundle2 digirObisProperties =
      new ResourceBundle2("gov.noaa.pfel.coastwatch.pointdata.DigirObis");
  public static ResourceBundle2 digirBmdeProperties =
      new ResourceBundle2("gov.noaa.pfel.coastwatch.pointdata.DigirBmde");
  private static String[] darwin2Variables, obisVariables, darwin2ObisVariables, bmdeVariables;

  /**
   * The DIGIR_VERSION is used in the header of digir requests. It is always "1.0" and diveIntoDigir
   * says it may be removed in the future.
   */
  public static final String OBIS_VERSION = "1.0";

  public static final String BMDE_VERSION = "0.95";
  public static final String DIGIR_XMLNS = "http://digir.net/schema/protocol/2003/1.0";
  public static final String DARWIN2_XMLNS = "http://digir.net/schema/conceptual/darwin/2003/1.0";
  public static final String OBIS_XMLNS = "http://www.iobis.org/obis";
  public static final String BMDE_XMLNS =
      "http://www.bsc-eoc.org/AKNS/schema"; // listed at start of schema
  public static final String DIGIR_XSD =
      "http://digir.sourceforge.net/schema/protocol/2003/1.0/digir.xsd";
  public static final String DARWIN2_XSD =
      "http://digir.sourceforge.net/schema/conceptual/darwin/2003/1.0/darwin2.xsd";
  public static final String OBIS_XSD = "http://iobis.org/obis/obis.xsd";
  public static final String BMDE_XSD =
      "http://akn.ornith.cornell.edu/Schemas/bmde/BMDE-Bandingv1.38.08.xsd";
  public static final String DARWIN_PREFIX = "darwin";
  public static final String OBIS_PREFIX = "obis";
  public static final String BMDE_PREFIX = "bmde";
  public static final String OBIS_PREFIXES[] = {"", DARWIN_PREFIX, OBIS_PREFIX};
  public static final String OBIS_XMLNSES[] = {DIGIR_XMLNS, DARWIN2_XMLNS, OBIS_XMLNS};
  public static final String OBIS_XSDES[] = {DIGIR_XSD, DARWIN2_XSD, OBIS_XSD};
  public static final String BMDE_PREFIXES[] = {"", BMDE_PREFIX};
  public static final String BMDE_XMLNSES[] = {DIGIR_XMLNS, BMDE_XMLNS};
  public static final String BMDE_XSDES[] = {DIGIR_XSD, BMDE_XSD};
  public static final String RUTGERS_OBIS_URL =
      "http://iobis.marine.rutgers.edu/digir2/DiGIR.php"; // often not working
  public static final String CSIRO_OBIS_URL =
      "http://www.obis.org.au/digir/DiGIR.php"; // no longer valid
  public static final String IND_OBIS_URL =
      "http://digir.indobis.org:80/digir/DiGIR.php"; // lat/lon constraints not numeric
  public static final String BURKE_URL =
      "http://biology.burke.washington.edu:80/digir/burke/DiGIR.php"; // no longer valid
  public static final String FLANDERS_OBIS_URL =
      "https://ipt.vliz.be"; // "https://www.vliz.be/digir/DiGIR.php";
  public static final String PRBO_BMDE_URL = "http://digir.prbo.org/digir/DiGIR.php";

  public static final String STRING_COPS[] = {"equals", "notEquals", "like", "in"};
  public static final String NUMERIC_COPS[] = {
    "equals",
    "notEquals",
    "in", // no "like"
    "lessThan",
    "lessThanOrEquals",
    "greaterThan",
    "greaterThanOrEquals"
  };

  /**
   * A list of all comparative operator symbols (for my convenience in parseQuery: 2 letter ops are
   * first).
   */
  public static final String COP_SYMBOLS[] = {
    "!=", "~=", "<=", ">=", "=", "<", ">", " in "
  }; // eeek! handle specially   spaces separate it from variable and value

  /** A list of all comparative operator names (corresponding to the COP_SYMBOLS). */
  public static final String COP_NAMES[] = {
    "notEquals",
    "like",
    "lessThanOrEquals",
    "greaterThanOrEquals",
    "equals",
    "lessThan",
    "greaterThan",
    "in"
  };

  /* SOURCE_IP is the reference ip used for digir requests.
  It isn't actually used for ip addressing.
  "65.219.21.6" is upwell here at pfeg.noaa.gov */
  public static String SOURCE_IP = "65.219.21.6"; // upwell

  // LOP - logical operator
  // how are lops used?  as a tree form of a SQL WHERE clause
  //  "and", "andNot", "and not", "or", "orNot", "or not"

  // darwin required:
  // "darwin:DateLastModified", "darwin:InstitutionCode", "darwin:CollectionCode",
  // "darwin:CatalogNumber", "darwin:ScientificName",
  // obis adds required:  "darwin:Longitude", "darwin:Latitude"

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
   * This makes the pre-destination part of a request. See searchDigir for parameter descriptions.
   *
   * @throws Exception if trouble
   */
  private static String getPreDestinationRequest(
      String version, String xmlnsPrefix[], String xmlnsNS[], String xmlnsXSD[]) throws Exception {

    // validate the xml info
    String errorInMethod = String2.ERROR + " in DigirHelper.makePreDestinationRequest: \n";
    Test.ensureNotNull(xmlnsPrefix, errorInMethod + "xmlnsPrefix is null.");
    Test.ensureNotNull(xmlnsNS, errorInMethod + "xmlnsNS is null.");
    Test.ensureNotNull(xmlnsXSD, errorInMethod + "xmlnsXSD is null.");
    Test.ensureTrue(xmlnsPrefix.length >= 1, errorInMethod + "xmlnsPrefix.length is less than 1.");
    Test.ensureEqual(
        xmlnsPrefix.length,
        xmlnsNS.length,
        errorInMethod + "xmlnsPrefix.length != xmlnsNS.length.");
    Test.ensureEqual(
        xmlnsPrefix.length,
        xmlnsXSD.length,
        errorInMethod + "xmlnsPrefix.length != xmlnsXSD.length.");

    /* example
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<request \n" +
    "  xmlns=\"http://digir.net/schema/protocol/2003/1.0\" \n" + //default namespace
    "  xmlns:xsd=\"https://www.w3.org/2001/XMLSchema\" \n" +
    "  xmlns:darwin=\"http://digir.net/schema/conceptual/darwin/2003/1.0\" \n" +
    "  xmlns:obis=\"http://www.iobis.org/obis\" \n" +
    "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\" \n" +
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

    // make the request
    StringBuilder request = new StringBuilder();
    request.append(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<request \n"
            + "  xmlns=\""
            + xmlnsNS[0]
            + "\" \n"
            + // default namespace
            "  xmlns:xsd=\"https://www.w3.org/2001/XMLSchema\" \n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\" \n");
    for (int i = 1; i < xmlnsNS.length; i++) // 1 because 0=default handled above
    request.append("  xmlns:" + xmlnsPrefix[i] + "=\"" + xmlnsNS[i] + "\" \n");

    // schema info: pairs of xmlns and xsd
    request.append("  xsi:schemaLocation=\"");
    for (int i = 0; i < xmlnsNS.length; i++)
      request.append((i == 0 ? "" : "\n    ") + xmlnsNS[i] + " \n      " + xmlnsXSD[i]);
    request.append(
        "\" >\n"
            + "  <header>\n"
            + "    <version>"
            + version
            + "</version>\n"
            + "    <sendTime>"
            + Calendar2.getCurrentISODateTimeStringZulu()
            + "Z</sendTime>\n"
            + "    <source>"
            + SOURCE_IP
            + "</source>\n");

    return request.toString();
  }

  /**
   * This makes the filter part of a request. See searchDigir for parameter descriptions.
   *
   * @param filterVariables
   * @param filterCops
   * @param filterValues
   * @throws Exception if trouble (e.g., filterVariables, filterCops, and/or filterValues lengths
   *     are different)
   */
  private static String getFilterRequest(
      String filterVariables[], String filterCops[], String filterValues[]) throws Exception {

    // validate the input
    String errorInMethod = String2.ERROR + " in DigirHelper.getFilterRequest: \n";
    if (filterVariables == null || filterVariables.length == 0)
      return // a simple filter that is always true
      "    <filter>\n"
          + "     <greaterThanOrEquals>\n"
          + "        <darwin:Latitude>-90</darwin:Latitude>\n"
          + "      </greaterThanOrEquals>\n"
          + "    </filter>\n";
    Test.ensureTrue(
        filterCops != null && filterCops.length == filterVariables.length,
        errorInMethod + "filterCops.length != filterVariables.length.");
    Test.ensureTrue(
        filterValues != null && filterCops.length == filterVariables.length,
        errorInMethod + "filterCops.length != filterVariables.length.");

    /*
    "    <filter>\n" +
    "      <equals>\n" +
    "        <darwin:Genus>Macrocystis</darwin:Genus>\n" +
    "      </equals>\n" +
    "    </filter>\n" +
    */

    // make the filter xml
    // Digir schema says <and> always contains exactly two elements
    //  so I have to construct a tree of <and>'s.
    StringBuilder request = new StringBuilder();
    request.append("    <filter>\n");
    for (int i = 0; i < filterCops.length; i++) {
      // I can't check against darwin/obis var lists here, since other schemas may be in play
      if (filterVariables[i] == null || filterVariables[i].length() == 0)
        Test.error(errorInMethod + "filterVariable#" + i + "=" + filterVariables[i]);
      if (String2.indexOf(NUMERIC_COPS, filterCops[i]) < 0
          && String2.indexOf(STRING_COPS, filterCops[i]) < 0)
        Test.error(errorInMethod + "Invalid filterCOP#" + i + "=" + filterCops[i]);
      if (filterValues[i] == null) // allow ""?
      Test.error(errorInMethod + "filterValue#" + i + "=" + filterValues[i]);

      String spacer = String2.makeString(' ', i * 2);
      if (i < filterCops.length - 1) request.append(spacer + "      <and>\n");
      request.append(
          spacer
              + "        <"
              + filterCops[i]
              + "><"
              + filterVariables[i]
              + ">"
              + filterValues[i]
              + "</"
              + filterVariables[i]
              + "></"
              + filterCops[i]
              + ">\n");
    }
    for (int i = filterCops.length - 2; i >= 0; i--)
      request.append(String2.makeString(' ', i * 2) + "      </and>\n");
    request.append("    </filter>\n");
    return request.toString();
  }

  /**
   * This gets a Digir provider/portal's metadata as an XML String. See examples at
   * http://diveintodigir.ecoforge.net/draft/digirdive.html and
   * http://digir.net/prov/prov_manual.html . See parameters for searchDigir.
   *
   * <p>Informally: it appears that you can get the metadataXml from a provider/portal just by going
   * to the url (even in a browser). But this may be just an undocumented feature of the standard
   * portal software.
   *
   * @throws Exception if trouble
   */
  public static String getMetadataXml(String url, String version) throws Exception {

    /* example from diveintodigir
    <?xml version="1.0" encoding="UTF-8"?>
    <request xmlns="http://digir.net/schema/protocol/2003/1.0"
            xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance"
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

    // make the request
    String errorInMethod = String2.ERROR + " in DigirHelper.getMetadataXml: \n";
    StringBuilder requestSB = new StringBuilder();
    requestSB.append(
        getPreDestinationRequest(
            version,
            // only digir (not darwin or obis or ...) namespace and schema is needed
            new String[] {""},
            new String[] {DIGIR_XMLNS},
            new String[] {DIGIR_XSD}));
    requestSB.append(
        "    <destination>"
            + url
            + "</destination>\n"
            + "    <type>metadata</type>\n"
            + "  </header>\n"
            + "</request>\n");
    String request = requestSB.toString();
    long time = System.currentTimeMillis();
    if (reallyVerbose)
      String2.log(
          "\nDigirHelper.getMetadataXml request=\n"
              + request
              + "\nGetting the response takes a long time...");

    // get the response
    // [This is the official way to do it.
    // In practice, digir servers also return the metadata in response to the url alone.]
    request =
        String2.toSVString(
            String2.split(request, '\n'), // split trims each string
            " ",
            true); // (white)space is necessary to separate schemalocation names and locations
    //        if (reallyVerbose) String2.log("\ncompactRequest=" + request + "\n");
    String response =
        SSR.getUrlResponseStringUnchanged(url + "?request=" + SSR.percentEncode(request));
    if (verbose)
      String2.log(
          "DigirHelper.getMetadataXml done. TIME=" + (System.currentTimeMillis() - time) + "ms");
    if (reallyVerbose)
      String2.log(
          "start of response=\n" + response.substring(0, Math.min(response.length(), 3000)));
    return response;
  }

  /**
   * This gets a Digir provider's metadata as a table. See examples at
   * http://diveintodigir.ecoforge.net/draft/digirdive.html and
   * http://digir.net/prov/prov_manual.html . See parameters for searchDigir. Note that the xml
   * contains some information before the resource list -- so that information doesn't make it into
   * the metadataTable.
   *
   * @return a table with a row for each resource. The "code" column has the codes for the resources
   *     (which are used for inventory and search requests). You can get a String[] of the codes for
   *     the resources available from this provider via
   *     <tt>((StringArray)table.findColumn("code")).toArray());</tt>
   * @throws Exception if trouble
   */
  public static Table getMetadataTable(String url, String version) throws Exception {

    // useful info from obis metadata:
    //   Institute of Marine and Coastal Sciences, Rutgers University</name>
    //        <name>Phoebe Zhang</name>
    //        <title>OBIS Portal Manager</title>
    //        <emailAddress>phoebe@imcs.rutgers.edu</emailAddress>
    //        <phone>001-732-932-6555 ext. 503</phone>

    // get the metadata xml and StringReader
    String xml = getMetadataXml(url, version);
    BufferedReader reader = new BufferedReader(new StringReader(xml));
    // for testing:
    // Test.ensureTrue(File2.writeToFileUtf8("c:/temp/ObisMetadata.xml", xml).equals(""),
    //    "Unable to save c:/temp/Obis.Metadata.xml.");
    // Reader reader = File2.getDecompressFileReaderUtf8("c:/programs/digir/ObisMetadata.xml");
    try {

      // read the resource data
      Table table = new Table();
      boolean validate = false; // since no .dtd specified by DOCTYPE in the file
      table.readXml(
          reader, validate, "/response/content/metadata/provider/resource", null, true); // simplify
      if (reallyVerbose)
        String2.log("DigirHelper.getMetadataTable, first 3 rows:\n" + table.toString(3));
      return table;
    } finally {
      reader.close();
    }
  }

  /**
   * This gets a Digir provider's inventory as an XML string. "Inventory" is described in the digir
   * schema as "which is to seek a count of all unique occurrences of a single concept." See example
   * at http://diveintodigir.ecoforge.net/draft/digirdive.html#id803800 and
   * http://digir.net/prov/prov_manual.html . See parameters for searchDigir.
   *
   * @param resource This must be a value from the "code" column from getMetadataTable.
   * @param filterVariables and filterCops and filterValues should have matching lengths (0 is ok)
   * @param resultsVariable The schema variable that you want to search for and get a count of the
   *     unique values of it, e.g., "darwin:ScientificName". There can be only one. The results
   *     indicate the number of occurrences (the count attribute) of each unique value of the
   *     resultsRariable.
   * @throws Exception if trouble, including a diagnostic message with severity="fatal" or
   *     severity="error".
   */
  public static String getInventoryXml(
      String version,
      String xmlnsPrefix[],
      String xmlnsNS[],
      String xmlnsXSD[],
      String resource,
      String url,
      String filterVariables[],
      String filterCops[],
      String filterValues[],
      String resultsVariable)
      throws Exception {

    /* example from diveintodigir
    <?xml version="1.0" encoding="UTF-8"?>
    <request xmlns="http://www.namespaceTBD.org/digir"
             xmlns:xsd="https://www.w3.org/2001/XMLSchema"
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

    // create the request
    String errorInMethod = String2.ERROR + " in DigirHelper.getInventory: \n";
    String filterRequest =
        filterVariables == null || filterVariables.length == 0
            ? ""
            : getFilterRequest(filterVariables, filterCops, filterValues);
    StringBuilder requestSB = new StringBuilder();
    requestSB.append(getPreDestinationRequest(version, xmlnsPrefix, xmlnsNS, xmlnsXSD));
    requestSB.append(
        "    <destination resource=\""
            + resource
            + "\">"
            + url
            + "</destination>\n"
            + "    <type>inventory</type>\n"
            + "  </header>\n"
            + "  <inventory>\n"
            + filterRequest
            + "    <"
            + resultsVariable
            + " />\n"
            + "  </inventory>\n"
            + "</request>\n");
    String request = requestSB.toString();
    if (reallyVerbose) String2.log("\nDigirHelper.getInventory request=\n" + request);

    // get the response
    request =
        String2.toSVString(
            String2.split(request, '\n'), // split trims each string
            " ",
            true); // (white)space is necessary to separate schemalocation names and locations
    // if (reallyVerbose) String2.log("\ncompactRequest=" + request + "\n");
    String response =
        SSR.getUrlResponseStringUnchanged(url + "?request=" + SSR.percentEncode(request));

    // look for error message
    int errorPo = response.indexOf("severity=\"fatal\"");
    if (errorPo < 0) errorPo = response.indexOf("severity=\"error\"");
    if (errorPo > 0) {
      int dCodePo = response.lastIndexOf("<diagnostic code", errorPo);
      errorPo = dCodePo >= 0 ? dCodePo : Math.max(0, errorPo - 33);
      Test.error(
          errorInMethod
              + "Error message from resource="
              + resource
              + ":\n"
              + response.substring(errorPo));
    }

    if (reallyVerbose)
      String2.log(
          "\nstart of response=\n" + response.substring(0, Math.min(5000, response.length())));
    return response;
  }

  /**
   * This gets a Digir provider's inventory as a Table. "Inventory" is described in the digir schema
   * as "which is to seek a count of all unique occurrences of a single concept." See example at
   * http://diveintodigir.ecoforge.net/draft/digirdive.html#id803800 and
   * http://digir.net/prov/prov_manual.html . See parameters for searchDigir.
   *
   * @param resource These must be values from the "code" column from getMetadataTable. Note that
   *     this method allows for more than 1 resource to be queried (unlike getInventoryXml).
   * @param filterVariables and filterCops and filterValues should have matching lengths (0 is ok)
   * @param resultsVariable The schema variable that you want to search for and get a count of the
   *     unique values of it, e.g., "darwin:ScientificName". There can be only one.
   * @return table with three columns: "Resource", resultsVariable, and "Count". The results
   *     indicate the number of occurrences of each unique value of the resultsRariable at each
   *     resource.
   * @throws Exception if trouble
   */
  public static Table getInventoryTable(
      String version,
      String xmlnsPrefix[],
      String xmlnsNS[],
      String xmlnsXSD[],
      String resource[],
      String url,
      String filterVariables[],
      String filterCops[],
      String filterValues[],
      String resultsVariable)
      throws Exception {

    String2.log(
        "DigirHelper.getInventoryTable(resource[0]="
            + resource[0]
            + " var="
            + resultsVariable
            + "\n  url="
            + url);

    // make the results table
    Table table = new Table();
    StringArray resourceSA = new StringArray();
    StringArray resultsSA = new StringArray();
    IntArray countIA = new IntArray();
    table.addColumn("Resource", resourceSA);
    table.addColumn(resultsVariable, resultsSA);
    table.addColumn("Count", countIA);

    // get the actualVariable e.g., "ScientificName"
    int colonPo = resultsVariable.indexOf(':');
    String resultsActualVariable =
        colonPo >= 0 ? resultsVariable.substring(colonPo + 1) : resultsVariable;

    // get the inventory for each resource
    XPath xPath = XML.getXPath();
    boolean validate = false; // since no .dtd specified by DOCTYPE in the file
    for (int res = 0; res < resource.length; res++) {
      String xml =
          getInventoryXml(
              version,
              xmlnsPrefix,
              xmlnsNS,
              xmlnsXSD,
              resource[res],
              url,
              filterVariables,
              filterCops,
              filterValues,
              resultsVariable);
      // used for testing:
      // String fileName = "c:/programs/digir/ObisInventory" + res + ".xml";
      // Test.ensureTrue(File2.writeToFileUtf8(fileName, xml).equals(""),
      //    "Unable to save " + fileName);
      // String xml = File2.readFromFile(fileName)[1];
      // String2.log("xml=" + xml);

      // sample xml response snippet:
      // <response><content><record>
      //  <darwin:ScientificName count='248'>Macrocystis integrifolia</darwin:ScientificName>
      // </record></content></response>
      Document document = XML.parseXml(new StringReader(xml), validate);
      NodeList nodeList =
          XML.getNodeList(document, xPath, "/response/content/record/" + resultsActualVariable);
      String2.log("DigirHelper.getInventoryTable nodeList length=" + nodeList.getLength());
      if (nodeList.getLength() == 0) String2.log("xml=\n" + xml);
      for (int nodeI = 0; nodeI < nodeList.getLength(); nodeI++) {
        Element element = (Element) nodeList.item(nodeI);
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
   * @param code e.g. "GHMP"
   * @param field e.g., "darwin:Genus"
   * @return a string with Genus inventory information.
   * @throws Exception if trouble
   */
  public static String getObisInventoryString(String url, String code, String field)
      throws Exception {
    // one time things
    Table table = new Table();
    table =
        getInventoryTable(
            OBIS_VERSION,
            OBIS_PREFIXES,
            OBIS_XMLNSES,
            OBIS_XSDES,
            new String[] {code}, // GHMP
            url,
            new String[] {},
            new String[] {},
            new String[] {}, // filter
            // try goofy restrictions in case something is required
            // new String[]{"darwin:ScientificName", "darwin:ScientificName"}, //"darwin:Genus"
            // new String[]{"greaterThan",           "lessThan"             }, //"equals"
            // new String[]{"A",                     "z"                    }, //"Carcharias"
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
   * @param code e.g. "prbo05"
   * @param field e.g., "darwin:Genus"
   * @return a string with Genus inventory information.
   * @throws Exception if trouble
   */
  public static String getBmdeInventoryString(String url, String code, String field)
      throws Exception {
    // one time things
    Table table = new Table();
    table =
        getInventoryTable(
            BMDE_VERSION,
            BMDE_PREFIXES,
            BMDE_XMLNSES,
            BMDE_XSDES,
            new String[] {code}, // prbo05
            url,
            new String[] {}, // "Genus"
            new String[] {}, // "="
            new String[] {}, // spp=carcharias
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
   * This gets data from Digir providers using various schemas. This calls setAttributes().
   *
   * @param xmlnsPrefix The prefixes for the namespaces e.g., {"", "darwin", "obis"} . The array
   *     length must be at least 1. 0th entry is ignored (it is default namespace). xsd
   *     (https://www.w3.org/2001/XMLSchema) and xsi (https://www.w3.org/2001/XMLSchema-instance)
   *     are automatically added.
   * @param xmlnsNS the xmlns values (pseudo URLs, not actual documents) e.g., {DIGIR_XMLNS,
   *     DARWIN_XMLNS, OBIS_XMLNS} These must correspond to the xmlnsPrefix values.
   * @param xmlnsXSD The url's of the schema documents e.g., {DIGIR_XSD, DARWIN_XSD, OBIS_XSD} These
   *     must correspond to the xmlnsPrefix values.
   * @param resources the codes for the resources to be searched (e.g., {"GHMP", "aims_biotech",
   *     "IndOBIS"}). You can get a list of resources from the darwin:CollectionCode column in the
   *     metadata from the provider/portal; see getMetadataXml and getMetadataTable above. [To my
   *     knowledge, there is no code to search all resources with one call.]
   * @param url the url of the provider, usually ending in "DiGIR.php", e.g.,
   *     http://iobis.marine.rutgers.edu/digir2/DiGIR.php.
   * @param filterVariables the variables for each of the filter tests, e.g., {"darwin:Genus",
   *     "darwin:Latitude"}. These must correspond to the filterCops. Note that filterVariables can
   *     include variables that are not in resultsVariables.
   * @param filterCops an array of COP's (comparitive operators) for filter tests, selected from:
   *     String variable COPs ("equals", "notEquals", "like", "in") or numeric variable COPs
   *     ("equals", "notEquals", "in", "lessThan", "lessThanOrEquals", "greaterThan",
   *     "greaterThanOrEquals"). For "like", a "%" sign at the beginning or end of the value is a
   *     wildcard (e.g., "Op%" will match all values starting with "Op"). For "in", the value can be
   *     a comma-separated list of values that can be matched. There is one filterCop for each test
   *     (1 or more, according to the DiGIR schema) to be done. All tests must pass (there is an
   *     implied AND between all tests). [This is a limitation of this method, not Digir.] Tests on
   *     fields with no value in the database return false.
   * @param filterValues the values for the filter tests, e.g, {"Macrocystis", "53"} . These must
   *     correspond to the filterCops (it may be null).
   * @param resultsTable data is appended to resultsTable. If resultsTable has data, it must have
   *     the same resultsVariables.
   * @param resultsVariables e.g., {"darwin:Latitude", "obis:Temperature"}. This must not be null or
   *     length 0. You can't use e.g., "obis" in place of "darwin", even though the obis schema
   *     includes darwin. The resulting table will always include just these variables and in the
   *     requested order.
   * @throws Exception if trouble. If a resource doesn't respond appropriately, it is just skipped
   *     (no Exception is thrown). But if there is no data from any resource and there is a
   *     diagnostic severity="fatal" or severity="error" message, an Exception if thrown and that
   *     message is used.
   */
  public static void searchDigir(
      String version,
      String xmlnsPrefix[],
      String xmlnsNS[],
      String xmlnsXSD[],
      String resources[],
      String url,
      String filterVariables[],
      String filterCops[],
      String filterValues[],
      Table resultsTable,
      String resultsVariables[])
      throws Exception {

    long time = System.currentTimeMillis();
    String msg =
        "DigirHelper.searchDigir: "
            + "\n  resources="
            + String2.toCSSVString(resources)
            + "\n  url="
            + url
            + "\n  resultsVariables="
            + String2.toCSSVString(resultsVariables)
            + "\n  filterVars="
            + String2.toCSSVString(filterVariables)
            + "\n  filterCops="
            + String2.toCSSVString(filterCops)
            + "\n  filterValues="
            + String2.toCSSVString(filterValues);
    if (verbose) String2.log("\n" + msg);
    String errorInMethod = String2.ERROR + " in " + msg + "\n  error message: ";

    /* my by-hand test was:
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<request \n" +
    "  xmlns=\"http://digir.net/schema/protocol/2003/1.0\" \n" + //default namespace
    "  xmlns:xsd=\"https://www.w3.org/2001/XMLSchema\" \n" +
    "  xmlns:darwin=\"http://digir.net/schema/conceptual/darwin/2003/1.0\" \n" +
    "  xmlns:obis=\"http://www.iobis.org/obis\" \n" +
    "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\" \n" +
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

    // *** validate input
    // xml and filter arrays are tested by makeXxxRequest methods
    Test.ensureNotNull(resources, errorInMethod + "resources is null.");
    Test.ensureTrue(resources.length >= 1, errorInMethod + "resources.length is less than 1.");
    Test.ensureNotNull(url, errorInMethod + "url is null.");

    Test.ensureNotNull(resultsVariables, errorInMethod + "resultsVariables is null.");
    Test.ensureTrue(
        resultsVariables.length >= 1, errorInMethod + "resultsVariables.length is less than 1.");

    // *** create the request before the resource
    // NO LEADING SPACES, TO SAVE SPACE, TO AVOID HTTP REQUEST-TOO-LONG ERROR
    String request1 =
        getPreDestinationRequest(version, xmlnsPrefix, xmlnsNS, xmlnsXSD)
            + "    <destination"; // resource="resource[i]" //is there an 'any' option? or more than
    // 1?

    // *** create the request after the resource
    // NO LEADING SPACES, TO SAVE SPACE, TO AVOID HTTP REQUEST-TOO-LONG ERROR
    StringBuilder request2sb = new StringBuilder();
    request2sb.append(
        ">"
            + url
            + "</destination>\n"
            + "    <type>search</type>\n"
            + "  </header>\n"
            + "  <search>\n"
            + getFilterRequest(filterVariables, filterCops, filterValues));

    // specify the results records
    // NO LEADING SPACES, TO SAVE SPACE, TO AVOID HTTP REQUEST-TOO-LONG ERROR
    request2sb.append(
        // for releases: 10000
        "    <records limit=\"10000\" start=\"0\" >\n"
            + // always specify to avoid defaults (e.g., default limit=10)
            "      <structure>\n"
            + "        <xsd:element name=\"record\">\n"
            + "          <xsd:complexType>\n"
            + "            <xsd:sequence>\n");
    for (int i = 0; i < resultsVariables.length; i++)
      request2sb.append("              <xsd:element ref=\"" + resultsVariables[i] + "\"/>\n");
    request2sb.append(
        "            </xsd:sequence>\n"
            + "          </xsd:complexType>\n"
            + "        </xsd:element>\n"
            + "      </structure>\n");

    // NO LEADING SPACES, TO SAVE SPACE, TO AVOID HTTP REQUEST-TOO-LONG ERROR
    request2sb.append(
        "    </records>\n"
            + "    <count>false</count>\n"
            + // count=true may be trouble for BMDE
            "  </search>\n"
            + "</request>\n");

    String request2 = request2sb.toString();
    if (reallyVerbose) String2.log("\nrequest1=" + request1 + "\n\nrequest2=" + request2);

    // if resultsTable has column, set it aside
    Table tTable = resultsTable;
    if (resultsTable.nColumns() > 0) tTable = new Table();

    // TableXmlHandler.verbose = true;
    XMLReader xmlReader =
        TableXmlHandler.getXmlReader(
            tTable,
            false, // don't validate since no .dtd specified by DOCTYPE in the file
            "/response/content/record",
            null);

    // *** get data from each resource
    // apparently, there is no "all" recourse option
    String diagnosticError = "";
    for (int resource = 0; resource < resources.length; resource++) {
      try {
        // get the xml for 1 resource from the provider
        long readTime = System.currentTimeMillis();
        String request = request1 + " resource=\"" + resources[resource] + "\"" + request2;

        // for testing: test that it is well-formed
        // can't validate, because no .dtd specified by DOCTYPE in file
        // ?how validate against schema?
        // XML.parseXml(new StringReader(request), false);
        // String2.pressEnterToContinue(request);
        request =
            String2.toSVString(
                String2.split(request, '\n'), // split trims each string
                " ",
                true); // (white)space is necessary to separate schemalocation names and locations
        // if (reallyVerbose) String2.log("\ncompactRequest=" + request + "\n");
        String response =
            SSR.getUrlResponseStringUnchanged(url + "?request=" + SSR.percentEncode(request));
        // for testing:
        // File2.writeToFileUtf8("c:/temp/SearchDigirResponse" + resource + ".xml", response);
        // String response = File2.readFromFile("c:/temp/SearchDigirResponse" + resource +
        // ".xml")[1];

        if (verbose)
          String2.log(resources[resource] + " readTime=" + (System.currentTimeMillis() - readTime));
        if (reallyVerbose)
          String2.log(
              resources[resource]
                  + " start of response=\n"
                  + response.substring(0, Math.min(5000, response.length())));

        // xml is often incomplete (nothing below <content>), so xmlReader.parse often throws
        // exception.
        // But if it doesn't throw exception, it was successful.
        // String2.log("Calling xmlReader.parse...");
        long parseTime = System.currentTimeMillis();
        int preNRows = tTable.nRows();

        xmlReader.parse(new InputSource(new StringReader(response)));
        if (verbose)
          String2.log(
              "After "
                  + resources[resource]
                  + ", nRows="
                  + tTable.nRows()
                  + " parseTime="
                  + (System.currentTimeMillis() - parseTime));

        // look for error message
        if (preNRows == tTable.nRows()) {
          int errorPo = response.indexOf("severity=\"fatal\"");
          if (errorPo < 0) errorPo = response.indexOf("severity=\"error\"");
          if (errorPo > 0) {
            int dCodePo = response.lastIndexOf("<diagnostic code", errorPo);
            errorPo = dCodePo >= 0 ? dCodePo : Math.max(0, errorPo - 33);
            String tError =
                String2.ERROR
                    + " message from resource="
                    + resources[resource]
                    + ":\n"
                    + response.substring(errorPo)
                    + "\n";
            diagnosticError += tError + "\n";
            String2.log(tError);
          }
        }

      } catch (Exception e) {
        String tError =
            "EXCEPTION thrown by request to resource="
                + resources[resource]
                + ":\n"
                + MustBe.throwableToString(e)
                + "\n";
        diagnosticError += tError + "\n";
        if (verbose) String2.log(tError);
      }

      // make all columns the same size
      tTable.makeColumnsSameSize();
    }

    // throw exception because nRows = 0?
    int nr = tTable.nRows();
    if (nr == 0 && diagnosticError.length() > 0) Test.error(errorInMethod + "\n" + diagnosticError);

    // *** move the columns into place
    // insert columns that don't exist
    for (int col = 0; col < resultsVariables.length; col++) {
      String name = resultsVariables[col];
      int tCol = tTable.findColumnNumber(name);
      // String2.log("moving columns col=" + col + " name=" + name + " tCol=" + tCol);
      if (tCol < 0) {
        // insert an empty column
        tTable.addColumn(col, name, new StringArray(nr, true));
      } else if (tCol < col) {
        Test.error(
            errorInMethod
                + "Duplicate column name in "
                + String2.toCSSVString(tTable.getColumnNames()));
      } else if (tCol == col) {
        // do nothing
      } else {
        // move the column into place
        PrimitiveArray pa = tTable.getColumn(tCol);
        tTable.removeColumn(tCol);
        tTable.addColumn(col, name, pa);
      }
    }

    // remove columns not requested
    for (int col = tTable.nColumns() - 1; col >= resultsVariables.length; col--) {
      if (reallyVerbose) String2.log("  !!Removing extra column: " + tTable.getColumnName(col));
      tTable.removeColumn(col);
    }

    // merge with original table
    if (resultsTable != tTable) // yes, simple "!=" is appropriate
    resultsTable.append(tTable); // they will have same columns

    // done
    if (verbose)
      String2.log(
          "  DigirHelper.searchDigir done. nColumns="
              + resultsTable.nColumns()
              + " nRows="
              + resultsTable.nRows()
              + " TIME="
              + (System.currentTimeMillis() - time)
              + "ms\n");
  }

  /**
   * This is like searchDigir, but customized for OBIS (which uses the DiGIR engine and the Darwin2
   * and OBIS XML schemas). Since OBIS is a superset of Darwin2, you can use this for Darwin2-based
   * resources as well, as long as you only refer to Darwin2 (not obis) variables.
   *
   * <p>Obis info: http://www.iobis.org . <br>
   * Obis schema info: http://www.iobis.org/tech/provider/questions . <br>
   * This sets column types and attributes based on info in DigirDarwin2.properties and
   * DigirObis.properties (in this directory). <br>
   * See searchDigir for the parameter descriptions. <br>
   * Valid variables (for filters and results) are listed in DigirDarwin2.properties and
   * DigirObis.properties.
   *
   * @param includeXYZT if true, this always includes columns:
   *     <ul>
   *       <li>0) LON
   *       <li>1) LAT
   *       <li>2) DEPTH which has the values from darwin:MinimumDepth
   *       <li>3) TIME (epochSeconds) created from YearCollected-MonthCollected-DayCollected and
   *           TimeOfDay.
   *       <li>4) ID created from
   *           [darwin:InstitutionCode]:[darwin:CollectionCode]:[darwin:CatalogNumber]
   *     </ul>
   *     A problem is that the x,y,z,t columns may have missing values!
   *     <p>If false, this just includes the resultsVariables.
   * @param table data is appended to table. If table has data, it must have the same includeXYZT
   *     and resultsVariables.
   */
  public static void searchObis(
      String resources[],
      String url,
      String filterVariables[],
      String filterCops[],
      String filterValues[],
      Table table,
      boolean includeXYZT,
      String resultsVariables[])
      throws Exception {

    String errorInMethod = String2.ERROR + " in DigirHelper.searchObis: ";

    // pre check that filterVariables and resultsVariables are valid darwin or obis variables?
    String validVars[] = getDarwin2ObisVariables();
    for (int i = 0; i < resultsVariables.length; i++)
      if (String2.indexOf(validVars, resultsVariables[i]) < 0)
        Test.error(
            errorInMethod
                + "Unsupported resultsVariable="
                + resultsVariables[i]
                + "\nValid="
                + String2.toCSSVString(validVars));
    for (int i = 0; i < filterVariables.length; i++)
      if (String2.indexOf(validVars, filterVariables[i]) < 0)
        Test.error(
            errorInMethod
                + "Unsupported filterVariable="
                + filterVariables[i]
                + "\nValid="
                + String2.toCSSVString(validVars));

    // add the xyzt variables
    StringArray getVariables = new StringArray(resultsVariables);
    if (includeXYZT) {
      String needed[] = {
        DARWIN_PREFIX + ":Longitude",
        DARWIN_PREFIX + ":Latitude",
        DARWIN_PREFIX + ":MinimumDepth",
        DARWIN_PREFIX + ":YearCollected",
        DARWIN_PREFIX + ":MonthCollected",
        DARWIN_PREFIX + ":DayCollected",
        DARWIN_PREFIX + ":TimeOfDay", // DARWIN_PREFIX + ":Timezone"); //deal with???
        DARWIN_PREFIX + ":InstitutionCode",
        DARWIN_PREFIX + ":CollectionCode",
        DARWIN_PREFIX + ":CatalogNumber"
      };
      for (int i = 0; i < needed.length; i++)
        if (getVariables.indexOf(needed[i]) < 0) getVariables.add(needed[i]);
    }

    // if table already has data, set that table aside
    Table tTable = table;
    if (table.nColumns() > 0) tTable = new Table();

    // get data from the provider
    searchDigir(
        OBIS_VERSION,
        OBIS_PREFIXES,
        OBIS_XMLNSES,
        OBIS_XSDES,
        resources,
        url,
        filterVariables,
        filterCops,
        filterValues,
        tTable,
        getVariables.toArray());

    // for testing:
    // tTable.saveAsFlatNc("c:/temp/searchObis" + includeXYZT + ".nc", "row");
    /* */
    // Table tTable = new Table();
    // tTable.readFlatNc("c:/temp/searchObis" + includeXYZT + ".nc", null, 1); //standardizeWhat=1

    int nRows = tTable.nRows();
    if (includeXYZT) {
      // create and add x,y,z,t,id columns    (numeric cols forced to be doubles)
      tTable.addColumn(
          0,
          DataHelper.TABLE_VARIABLE_NAMES[0],
          new DoubleArray(tTable.findColumn(DARWIN_PREFIX + ":Longitude")));
      tTable.addColumn(
          1,
          DataHelper.TABLE_VARIABLE_NAMES[1],
          new DoubleArray(tTable.findColumn(DARWIN_PREFIX + ":Latitude")));
      tTable.addColumn(
          2,
          DataHelper.TABLE_VARIABLE_NAMES[2],
          new DoubleArray(tTable.findColumn(DARWIN_PREFIX + ":MinimumDepth")));
      DoubleArray tPA = new DoubleArray(nRows, false);
      tTable.addColumn(3, DataHelper.TABLE_VARIABLE_NAMES[3], tPA);
      StringArray idPA = new StringArray(nRows, false);
      tTable.addColumn(4, DataHelper.TABLE_VARIABLE_NAMES[4], idPA);
      PrimitiveArray yearPA = tTable.findColumn(DARWIN_PREFIX + ":YearCollected");
      PrimitiveArray monthPA = tTable.findColumn(DARWIN_PREFIX + ":MonthCollected");
      PrimitiveArray dayPA = tTable.findColumn(DARWIN_PREFIX + ":DayCollected");
      PrimitiveArray timeOfDayPA = tTable.findColumn(DARWIN_PREFIX + ":TimeOfDay");
      //          PrimitiveArray timeZonePA = findColumn("Timezone"); //deal with ???
      // obis schema says to construct id as
      // "URN:catalog:[InstitutionCode]:[CollectionCode]:[CatalogNumber]"  but their example is more
      // terse than values I see
      PrimitiveArray insPA = tTable.findColumn(DARWIN_PREFIX + ":InstitutionCode");
      PrimitiveArray colPA = tTable.findColumn(DARWIN_PREFIX + ":CollectionCode");
      PrimitiveArray catPA = tTable.findColumn(DARWIN_PREFIX + ":CatalogNumber");
      for (int row = 0; row < nRows; row++) {
        // make the t value
        double seconds = Double.NaN;
        StringBuilder sb = new StringBuilder(yearPA.getString(row));
        if (sb.length() > 0) {
          String tMonth = monthPA.getString(row);
          if (tMonth.length() > 0) {
            sb.append("-" + tMonth); // month is 01 - 12
            String tDay = dayPA.getString(row);
            if (tDay.length() > 0) {
              sb.append("-" + tDay);
            }
          }
          try {
            seconds = Calendar2.isoStringToEpochSeconds(sb.toString());
            String tTime = timeOfDayPA.getString(row); // decimal hours since midnight
            int tSeconds =
                Math2.roundToInt(String2.parseDouble(tTime) * Calendar2.SECONDS_PER_HOUR);
            if (tSeconds < Integer.MAX_VALUE) seconds += tSeconds;
          } catch (Exception e) {
            if (reallyVerbose) String2.log("DigirHelper.searchObis unable to parse date=" + sb);
          }
        }
        tPA.add(seconds);

        // make the id value
        idPA.add(insPA.getString(row) + ":" + colPA.getString(row) + ":" + catPA.getString(row));
      }

      // remove unneeded columns (related to x,y,z,t,id) at end
      for (int col = tTable.nColumns() - 1; col >= 5 + resultsVariables.length; col--)
        tTable.removeColumn(col);
    }

    // simplify the columns and add column metadata
    for (int col = includeXYZT ? 5 : 0; col < tTable.nColumns(); col++) {
      String colName = tTable.getColumnName(col);
      String info =
          colName.startsWith(DARWIN_PREFIX + ":")
              ? digirDarwin2Properties.getString(
                  colName.substring(DARWIN_PREFIX.length() + 1), null)
              : colName.startsWith(OBIS_PREFIX + ":")
                  ? digirObisProperties.getString(colName.substring(OBIS_PREFIX.length() + 1), null)
                  : null;
      Test.ensureNotNull(info, errorInMethod + "No info found for variable=" + colName);
      String infoArray[] = String2.split(info, '\f');

      // change column type from String to ?
      String type = infoArray[0];
      if (!type.equals("String") && !type.equals("dateTime")) {
        // it's a numeric column
        PrimitiveArray pa = PrimitiveArray.factory(PAType.fromCohortString(type), 8, false);
        pa.append(tTable.getColumn(col));
        tTable.setColumn(col, pa);

        // set actual_range?
      }

      // set column metadata
      String metadata[] = String2.split(infoArray[1], '`');
      for (int i = 0; i < metadata.length; i++) {
        int eqPo = metadata[i].indexOf('='); // first instance of '='
        Test.ensureTrue(
            eqPo > 0,
            errorInMethod + "Invalid metadata for colName=" + colName + ": " + metadata[i]);
        tTable
            .columnAttributes(col)
            .set(metadata[i].substring(0, eqPo), metadata[i].substring(eqPo + 1));
      }
    }

    // append tTable to table (columns are identical)
    if (tTable != table) { // yes, a simple "!=" test
      table.append(tTable);
      tTable = null; // garbage collect it
    }

    // setAttributes  (this sets the coordinate variables' axis, long_name, standard_name, and
    // units)
    String opendapQuery =
        getOpendapConstraint(new String[] {}, filterVariables, filterCops, filterValues);
    int depthCol = table.findColumnNumber(DARWIN_PREFIX + ":MinimumDepth");
    if (depthCol == -1) depthCol = table.findColumnNumber(DARWIN_PREFIX + ":MaximumDepth");

    table.setObisAttributes(
        includeXYZT ? 0 : table.findColumnNumber(DARWIN_PREFIX + ":Longitude"),
        includeXYZT ? 1 : table.findColumnNumber(DARWIN_PREFIX + ":Latitude"),
        includeXYZT ? 2 : depthCol,
        includeXYZT ? 3 : -1, // time column
        url,
        resources,
        opendapQuery);
  }

  /**
   * This is like searchDigir, but customized for BMDE (which uses the DiGIR engine and the BMDE XML
   * schema). Since BMDE is not a superset of Darwin, you can't use this for Darwin-based resources
   * as well. <br>
   * This works differently from searchObis -- This doesn't make artificial xyzt variables. <br>
   * This sets column types and a few attributes, based on info in DigirBmde.properties (in this
   * directory). <br>
   * See searchDigir for the parameter descriptions. <br>
   * Valid variables (for filters and results) are listed in DigirBmde.properties.
   */
  public static Table searchBmde(
      String resources[],
      String url,
      String filterVariables[],
      String filterCops[],
      String filterValues[],
      String resultsVariables[])
      throws Exception {

    if (reallyVerbose)
      String2.log(
          "\n*** digirHelper.searchBmde resources="
              + String2.toCSSVString(resources)
              + "\n  url="
              + url
              + "\n  filterVars="
              + String2.toCSSVString(filterVariables)
              + "\n  filterCops="
              + String2.toCSSVString(filterCops)
              + "\n  filterVals="
              + String2.toCSSVString(filterValues)
              + "\n  resultsVars="
              + String2.toCSSVString(resultsVariables));

    String errorInMethod = String2.ERROR + " in DigirHelper.searchBmde: ";

    // pre check that filterVariables and resultsVariables are valid bmde variables?
    String validVars[] = getBmdeVariables();
    for (int i = 0; i < resultsVariables.length; i++)
      if (String2.indexOf(validVars, resultsVariables[i]) < 0)
        Test.error(
            errorInMethod
                + "Unsupported resultsVariable="
                + resultsVariables[i]
                + "\nValid="
                + String2.toCSSVString(validVars));
    for (int i = 0; i < filterVariables.length; i++)
      if (String2.indexOf(validVars, filterVariables[i]) < 0)
        Test.error(
            errorInMethod
                + "Unsupported filterVariable="
                + filterVariables[i]
                + "\nValid="
                + String2.toCSSVString(validVars));

    // get data from the provider
    Table table = new Table();
    searchDigir(
        BMDE_VERSION,
        BMDE_PREFIXES,
        BMDE_XMLNSES,
        BMDE_XSDES,
        resources,
        url,
        filterVariables,
        filterCops,
        filterValues,
        table,
        resultsVariables);

    // simplify the columns and add column metadata
    int nCols = table.nColumns();
    int nRows = table.nRows();
    for (int col = 0; col < nCols; col++) {
      String colName = table.getColumnName(col);
      String info =
          colName.startsWith(BMDE_PREFIX + ":")
              ? digirBmdeProperties.getString(colName.substring(BMDE_PREFIX.length() + 1), null)
              : null;
      Test.ensureNotNull(info, errorInMethod + "No info found for variable=" + colName);
      String infoArray[] = String2.split(info, '\f');

      // change column type from String to ?
      String type = infoArray[0];
      if (!type.equals("String") && !type.equals("dateTime")) {
        // it's a numeric column
        PrimitiveArray pa = PrimitiveArray.factory(PAType.fromCohortString(type), nRows, false);
        pa.append(table.getColumn(col));
        table.setColumn(col, pa);

        // set actual_range?
      }

      // set column metadata
      String metadata[] = String2.split(infoArray[1], '`');
      for (int i = 0; i < metadata.length; i++) {
        int eqPo = metadata[i].indexOf('='); // first instance of '='
        Test.ensureTrue(
            eqPo > 0,
            errorInMethod + "Invalid metadata for colName=" + colName + ": " + metadata[i]);
        table
            .columnAttributes(col)
            .set(metadata[i].substring(0, eqPo), metadata[i].substring(eqPo + 1));
      }
    }

    table
        .globalAttributes()
        .set("keywords", "Biological Classification > Animals/Vertebrates > Birds");
    table.globalAttributes().set("keywords_vocabulary", "GCMD Science Keywords");
    return table;
  }

  /**
   * This is like the other searchObis, but processes an opendap-style query.
   *
   * <p>The first 5 columns in the results table are automatically LON, LAT, DEPTH, TIME, and ID
   *
   * @param resources see searchDigir's resources parameter
   * @param url see searchDigir's url parameter
   * @param query is the opendap-style query, e.g.,
   *     <tt>var1,var2,var3&amp;var4=value4&amp;var5&amp;gt;=value5</tt> . Note that the query must
   *     be in its unencoded form, with ampersand, greaterThan and lessThan characters as single
   *     characters. A more specific example is
   *     <tt>darwin:Genus,darwin:Species&amp;darwin:Genus=Macrocystis&amp;darwin:Latitude&gt;=53&amp;darwin:Latitude&lt;=54</tt>
   *     . Note that each constraint's left hand side must be a variable and its right hand side
   *     must be a value. See searchDigir's parameter descriptions for filterVariables, filterCops,
   *     and filterValues, except there is currently no support for "in" here. The valid string
   *     variable COPs are "=", "!=", "~=". The valid numeric variable COPs are "=", "!=", "&lt;",
   *     "&lt;=", "&gt;", "&gt;="). "~=" (which would normally match a regular expression on the
   *     right hand side) is translated to "like". "like" supports "%" (a wildcard) at the beginning
   *     and/or end of the value. Although you can put constraints on any Darwin variable (see
   *     DigirDarwin.properties) or OBIS variable (see DigirObis.properties), most variables have
   *     little or no data, so extensive requests will generate few or no results rows.
   * @param table the results are appended to table (and metadata is updated).
   */
  public static void searchObisOpendapStyle(
      String resources[], String url, String query, Table table) throws Exception {

    StringArray filterVariables = new StringArray();
    StringArray filterCops = new StringArray();
    StringArray filterValues = new StringArray();
    StringArray resultsVariables = new StringArray();
    parseQuery(query, resultsVariables, filterVariables, filterCops, filterValues);

    searchObis(
        resources,
        url,
        filterVariables.toArray(),
        filterCops.toArray(),
        filterValues.toArray(),
        table,
        true,
        resultsVariables.toArray());
  }

  /**
   * This parses the query for searchOpendapStyleObis.
   *
   * @param query see searchOpendapStyleObis's query
   * @param resultsVariables to be appended with the results variables
   * @param filterVariables to be appended with the filter variables
   * @param filterCops to be appended with the filter comparative operators
   * @param filterValues to be appended with the filter values
   * @throws Exception if invalid query (0 resultsVariables is a valid query)
   */
  public static void parseQuery(
      String query,
      StringArray resultsVariables,
      StringArray filterVariables,
      StringArray filterCops,
      StringArray filterValues) {

    String errorInMethod = String2.ERROR + " in DigirHelper.parseQuery:\n(query=" + query + ")\n";
    if (query.charAt(query.length() - 1) == '&')
      Test.error(errorInMethod + "query ends with ampersand.");

    // get the comma-separated vars    before & or end-of-query
    int ampPo = query.indexOf('&');
    if (ampPo < 0) ampPo = query.length();
    int startPo = 0;
    int stopPo = query.indexOf(',');
    if (stopPo < 0 || stopPo > ampPo) stopPo = ampPo;
    while (startPo < ampPo) {
      if (stopPo == startPo) // catch ",," in query
      Test.error(errorInMethod + "Missing results variable at startPo=" + startPo + ".");
      resultsVariables.add(query.substring(startPo, stopPo).trim());
      startPo = stopPo + 1;
      stopPo = startPo >= ampPo ? ampPo : query.indexOf(',', startPo);
      if (stopPo < 0 || stopPo > ampPo) stopPo = ampPo;
    }
    // String2.log("resultsVariables=" + resultsVariables);

    // get the constraints
    // and convert to ("equals", "notEquals", "like", "lessThan", "lessThanOrEquals",
    //  "greaterThan", "greaterThanOrEquals").
    ampPo = query.indexOf('&', startPo);
    if (ampPo < 0) ampPo = query.length();
    while (startPo < query.length()) {
      String filter = query.substring(startPo, ampPo);
      // String2.log("filter=" + filter);

      // find the op
      int op = 0;
      int opPo = -1;
      while (op < COP_SYMBOLS.length && (opPo = filter.indexOf(COP_SYMBOLS[op])) < 0) op++;
      if (opPo < 0)
        Test.error(
            errorInMethod
                + "No operator found in filter at startPo="
                + startPo
                + " filter="
                + filter
                + ".");
      filterVariables.add(filter.substring(0, opPo).trim());
      filterCops.add(COP_NAMES[op]);
      filterValues.add(filter.substring(opPo + COP_SYMBOLS[op].length()).trim());

      // remove start/end quotes from filterValues
      for (int i = 0; i < filterValues.size(); i++) {
        String fv = filterValues.get(i);
        if (fv.startsWith("\"") && fv.endsWith("\""))
          filterValues.set(i, fv.substring(1, fv.length() - 2).trim());
        else if (fv.startsWith("'") && fv.endsWith("'"))
          filterValues.set(i, fv.substring(1, fv.length() - 2).trim());
      }

      startPo = ampPo + 1;
      ampPo = startPo >= query.length() ? query.length() : query.indexOf('&', startPo);
      if (ampPo < 0) ampPo = query.length();
    }

    if (reallyVerbose) {
      String2.log(
          "Output from parseQuery:"
              + "\n  resultsVariables="
              + resultsVariables
              + "\n  filterVariables="
              + filterVariables
              + "\n  filterCops="
              + filterCops
              + "\n  filterValues="
              + filterValues);
    }
  }

  /**
   * This formats the filters as an opendap constraint.
   *
   * @param resultsVariables
   * @param filterVariables
   * @param filterCops all should be valid COP_NAMES
   * @param filterValues
   * @return e.g.,
   *     obis:Temperature,darwin:ScientificName&darwin:Genus=Macrocystis&darwin:Species=integrifolia
   */
  public static String getOpendapConstraint(
      String resultsVariables[],
      String filterVariables[],
      String filterCops[],
      String filterValues[]) {

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < resultsVariables.length; i++) {
      if (i > 0) sb.append(',');
      sb.append(resultsVariables[i]);
    }

    for (int i = 0; i < filterVariables.length; i++) {
      int op = String2.indexOf(COP_NAMES, filterCops[i]);
      if (op < 0)
        Test.error(
            String2.ERROR
                + " in DigirHelper.getOpendapConstraint:\n"
                + "Invalid operator="
                + filterCops[i]
                + ".");
      sb.append("&" + filterVariables[i] + COP_SYMBOLS[op] + filterValues[i]);
    }

    return sb.toString();
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
    // parse the request

    // get the data from opendap

    // format as Digir response xml
    StringBuilder response = new StringBuilder();

    return response.toString();
  }
}
