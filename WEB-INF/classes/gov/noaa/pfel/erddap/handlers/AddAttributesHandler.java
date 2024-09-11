package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.CharArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.String2;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class AddAttributesHandler extends StateWithParent {
  private StringBuilder content = new StringBuilder();
  private com.cohort.array.Attributes tAttributes;
  private String tName = null, tType = null;

  public AddAttributesHandler(
      SaxHandler saxHandler, com.cohort.array.Attributes tAttributes, State completeState) {
    super(saxHandler, completeState);
    this.tAttributes = tAttributes;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    if (localName.equals("att")) {
      tName = attributes.getValue("name");
      tType = attributes.getValue("type");
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    content.append(ch, start, length);
  }

  @Override
  public void endElement(String uri, String localName, String qName) {
    String contentStr = content.toString().trim();

    switch (localName) {
      case "att" -> {
        if (!String2.isSomething(tName)) {
          throw new IllegalArgumentException(
              "datasets.xml error : An <att> tag doesn't have a \"name\" attribute.");
        }
        if (tType == null) {
          tType = "string";
        }
        PrimitiveArray pa;
        if (content.isEmpty()) {
          pa = new StringArray();
          pa.addString("null");
        } else if (tType.equalsIgnoreCase("String")) {
          pa = new StringArray();
          pa.addString(contentStr);
        } else {
          if (tType.endsWith("List")) tType = tType.substring(0, tType.length() - 4);
          if (tType.equals("string")) tType = "String";

          if (tType.equals("unsignedShort")) {
            tType = "char";
            pa = PrimitiveArray.ssvFactory(PAType.fromCohortString("int"), contentStr);
            pa = new CharArray(pa);
          } else {
            pa = PrimitiveArray.ssvFactory(PAType.fromCohortString(tType), contentStr);
          }
        }
        tAttributes.add(tName, pa);
      }
      case "addAttributes" -> saxHandler.setState(this.completeState); // return to parentHandler
      default -> String2.log("Unexpected end tag: " + localName);
    }
    content.setLength(0);
  }
}
