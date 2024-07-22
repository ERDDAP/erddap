/*
 * Table Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import com.cohort.util.Test;
import java.util.ArrayList;
import java.util.HashSet;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A class to handle SAX2 XML events for and store information records in an XML document in a
 * Table. This class usually isn't instantiated directly. See getXmlReader below for info on how to
 * use this class.
 *
 * <p>[Sorry, this is hard to word clearly...]
 *
 * <ul>
 *   <li>This makes a column for each unique type of descendant element of a 'rowElement' which has
 *       text content.
 *   <li>Thus, each descendant should be a unique element (e.g., Latitude, Longitude); this doesn't
 *       get the names from, e.g., a 'name' attribute.
 *   <li>Descendants below children are given XPath-like names with a slash to indicate a new
 *       generation, e.g., coordinate/Latitude.
 *   <li>Descendant elements with no data aren't ever made into columns.
 *   <li>!!!If there are two or more xml tags with the same name (e.g., two "contact" tags), the
 *       subsequent tags are given name+[number], where number is 2,... to make the name unique.
 *   <li>!!!Only specified rowElementAttributes are kept. Other attributes are ignored.
 *   <li>!!!Tags that differ by some attribute (e.g., "type=") are not differentiated here.
 * </ul>
 *
 * @author Bob Simons (was was bob.simons@noaa.gov, now BobSimons2.00@gmail.com, now
 *     BobSimons2.00@gmail.com) 2007-05-02
 */
public class TableXmlHandler extends DefaultHandler {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  private String errorInMethod = String2.ERROR + " in TableXmlHandler:\n";

  // set by constructor
  private Table table;
  private String rowStack[];
  private int rowStackSize;
  private String[] rowElementAttributes;

  // reset by startDocument
  private int row;
  private boolean inRow;
  private StringArray nameStack;
  private StringArray uniqueNameStack;
  private ArrayList<HashSet> hashsetStack;
  private StringBuilder characters;

  /**
   * The constructor.
   *
   * @param table is the table which will receive the results. This class/method does not call
   *     table.clear(), so data will be appended.
   * @param rowElementXPath the element (XPath style) identifying a row, e.g.,
   *     /response/content/record.
   * @param rowElementAttributes are the attributes of the row element (e.g., "name") which will be
   *     noted and stored in columns of the table. May be null if none. (Other element's attributes
   *     are ignored.)
   */
  public TableXmlHandler(Table table, String rowElementXPath, String rowElementAttributes[]) {
    super();
    this.table = table;
    if (rowElementAttributes == null) rowElementAttributes = new String[0];
    this.rowElementAttributes = rowElementAttributes;
    for (int i = 0; i < rowElementAttributes.length; i++) {
      table.addColumn(rowElementAttributes[i], new StringArray());
    }

    // set up the rowStack which identifies the start of a row
    //  (rowElementXPath split into parts)
    Test.ensureTrue(
        rowElementXPath.startsWith("/"),
        errorInMethod + "rowElementXPath=" + rowElementXPath + " must start with '/'.");
    if (rowElementXPath.length() > 1 && rowElementXPath.endsWith("/"))
      rowElementXPath = rowElementXPath.substring(0, rowElementXPath.length() - 1);
    rowStack =
        String2.split(rowElementXPath.substring(1), '/'); // substring(1) because first char is '/'
    rowStackSize = rowStack.length;
    if (verbose) String2.log("TableXmlHandler rowStack=" + String2.toCSSVString(rowStack));
  }

  /**
   * This method is called by XMLReader.parse to handle a startDocument event. This method will
   * probably never be called by any other code.
   */
  @Override
  public void startDocument() {
    if (verbose) String2.log("Start document");

    // reset things for next document
    row = table.nRows() - 1;
    inRow = false;

    // recreating the objects frees memory if the objects were previously huge
    nameStack = new StringArray();
    uniqueNameStack = new StringArray();
    hashsetStack = new ArrayList();
    characters = new StringBuilder();
  }

  /**
   * This method is called by XMLReader.parse to handle an endDocument event. This method will
   * probably never be called by any other code.
   */
  @Override
  public void endDocument() {
    if (verbose) String2.log("End document");
    characters.setLength(0);
    table.makeColumnsSameSize();
  }

  /**
   * This method is called by XMLReader.parse to handle a startElement event. This method will
   * probably never be called by any other code.
   *
   * @param uri is the full uri of the dtd
   * @param name is the simple tag name, e.g., Longitude
   * @param qName e.g., darwin:Longitude
   */
  @Override
  public void startElement(String uri, String name, String qName, Attributes atts) {

    // add name to nameStack
    nameStack.add(qName);
    if (verbose) String2.log("Start element: " + qName);

    // create the hashset for this element's children
    hashsetStack.add(new HashSet());

    // is this the start of a row?
    if (nameStack.size() == rowStackSize) {
      inRow = true;
      for (int i = 0; i < rowStackSize; i++) {
        if (!nameStack.get(i).equals(rowStack[i])) {
          inRow = false;
          break;
        }
      }
      if (inRow) {
        row++;
        if (verbose) String2.log("Start row:     " + row);
      }

      // catch the rowElementAttributes
      for (int i = 0; i < rowElementAttributes.length; i++) {
        String trimmed = atts.getValue(rowElementAttributes[i]);
        if (trimmed == null) continue;
        trimmed = trimmed.trim();
        if (trimmed.length() == 0) continue;

        // rowElementAttribute is the i'th column in the table
        StringArray sa = (StringArray) table.getColumn(i); // they are all StringArray's
        sa.addN(row - sa.size(), ""); // may be no data on previous rows

        // add the value
        sa.add(trimmed);
      }
    }

    // make the unique name for this element
    if (inRow && nameStack.size() > rowStackSize) { // a child which is inRow
      // find countString which makes the name unique
      HashSet<String> parentHashset = hashsetStack.get(nameStack.size() - 2);
      int count = 1;
      String countString = "";
      while (!parentHashset.add(
          qName + countString)) { // false= not successfully added  i.e., it was already there
        count++;
        countString = "" + count;
      }
      uniqueNameStack.add(qName + countString); // e.g., contact2
    } else uniqueNameStack.add(""); // irrelevant

    // and finally, always clear 'characters', since the characters were before this tag
    characters.setLength(0);
  }

  /**
   * This method is called by XMLReader.parse to handle an endElement event. This method will
   * probably never be called by any other code. Most of the work is here because the characters
   * before an endElement are now known.
   */
  @Override
  public void endElement(String uri, String name, String qName) {
    if (verbose) String2.log("End element:   " + qName);

    // add data to table
    int stackSize = nameStack.size();
    if (inRow
        && // if not inRow, I don't care
        stackSize > rowStackSize) { // needs to be a child inRow

      // get the text characters trimmed of leading and trailing white space (not just spaces)
      int start = 0;
      int end = characters.length();
      while (start < end && String2.isWhite(characters.charAt(start))) start++;
      while (start < end && String2.isWhite(characters.charAt(end - 1))) end--;
      String trimmed = start == end ? "" : characters.substring(start, end);
      if (verbose) String2.log("  trimmed characters: " + String2.annotatedString(trimmed));

      if (trimmed.length() > 0) { // needs to have characters
        StringBuilder colNameSB = new StringBuilder(uniqueNameStack.get(rowStackSize));
        for (int i = rowStackSize + 1; i < stackSize; i++)
          colNameSB.append("/" + uniqueNameStack.get(i)); // '/' chosen to be xpath-like
        String colName = colNameSB.toString();
        int col = table.findColumnNumber(colName);
        if (col < 0) {
          col = table.nColumns();
          table.addColumn(colName, new StringArray());
        }

        // ensure the column has the right number of rows (there may have been no data on previous
        // rows)
        StringArray sa = (StringArray) table.getColumn(col); // they are all StringArray's
        if (row < sa.size())
          // this shouldn't be possible (since hashset and count ensure uniqueness above)
          Test.error(errorInMethod + "Internal error for tag=" + colName + " row=" + row);

        sa.addN(row - sa.size(), "");

        // add the value
        sa.add(trimmed);
      }
    }

    // is this the end of a row?
    if (inRow && nameStack.size() == rowStackSize) {
      inRow = false;
      if (verbose) String2.log("End row:       " + row);
    }

    // remove items from stacks
    nameStack.remove(nameStack.size() - 1);
    uniqueNameStack.remove(uniqueNameStack.size() - 1);
    hashsetStack.remove(hashsetStack.size() - 1);

    // and finally, always clear 'characters', since the characters were before this tag
    characters.setLength(0);
  }

  /**
   * This method is called by XMLReader.parse to handle a characters event. Note that this may be
   * called a few times in a row, and the info should be concatenated. This method will probably
   * never be called by any other code. The characters are concatenated in the class variable
   * 'characters'.
   */
  @Override
  public void characters(char ch[], int start, int length) {
    characters.append(ch, start, length);
    if (verbose)
      String2.log("Characters:    " + String2.annotatedString(new String(ch, start, length)));
  }

  /**
   * This method is called by XMLReader.parse to handle a recoverable parser error. This method will
   * probably never be called by any other code.
   */
  @Override
  public void error(SAXParseException e) throws SAXParseException {
    throw e;
  }

  /**
   * This method is called by XMLReader.parse to handle fatal parser error. This method will
   * probably never be called by any other code.
   */
  @Override
  public void fatalError(SAXParseException e) throws SAXParseException {
    throw e;
  }

  /**
   * This method is called by XMLReader.parse to handle a parser warning. This method will probably
   * never be called by any other code.
   */
  @Override
  public void warning(SAXParseException e) throws SAXParseException {
    String2.log("WARNING in TableXmlHandler: " + e);
  }

  /**
   * This is a convenience method to get an XMLReader. One you have it, you can repeatedly call
   * xmlReader.parse(InputSource) to parse one or more xml documents and add their contents to the
   * table. The order of the columns is not important. New columns will be added as needed.
   *
   * <p>Internally, this method makes a TableXmlHandler. The xmlReader calls its methods to process
   * the parts of the xml document.
   *
   * @param table a Table, perhaps already containing some columns and rows. This does not call
   *     table.clear().
   * @param validate indicates if the XML parser should validate the xml in a simple sense (see
   *     https://www.w3.org/TR/REC-xml#proc-types). true or false, the XMLReader always insists that
   *     the document be well formed. true or false, the XMLReader doesn't validate against a
   *     schema. The validate parameter will be ignored if the XMLReader doesn't support validation.
   *     (It is a good sign that, on Windows, the XMLReader that comes with Java seems to support
   *     validation, or at least doesn't object to being told to validate the xml.)
   * @param rowElementXPath the element (XPath style) identifying a row, e.g.,
   *     /response/content/record.
   * @param rowElementAttributes are the attributes of the row element (e.g., "name") which will be
   *     noted and stored in columns of the table. (Other element's attributes are ignored.)
   * @throws Exception if trouble
   */
  public static XMLReader getXmlReader(
      Table table, boolean validate, String rowElementXPath, String rowElementAttributes[])
      throws Exception {

    // set up the TableXmlHandler
    // TableXmlHandler.verbose = verbose;
    TableXmlHandler handler = new TableXmlHandler(table, rowElementXPath, rowElementAttributes);

    // set up the xmlReader (and validation)
    XMLReader xr = XMLReaderFactory.createXMLReader();
    xr.setContentHandler(handler);
    xr.setErrorHandler(handler);
    try {
      if (validate) xr.setFeature("http://xml.org/sax/features/validation", true);
    } catch (Exception e) {
      String2.log("WARNING: XMLReader cannot activate validation.");
    }
    // xr.parse(new InputSource(xml));
    return xr;
  }
}
