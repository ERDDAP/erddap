package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.String2;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;

public class AxisVariableHandler extends State {
    private StringBuilder content = new StringBuilder();
    private ArrayList tAxisVariables;
    private String tSourceName = null, tDestinationName = null;
    private com.cohort.array.Attributes tAttributes = null;
    private PrimitiveArray tValuesPA = null;
    private State completeState;

    public AxisVariableHandler(SaxHandler saxHandler, ArrayList tAxisVariables, State completeState) {
        super(saxHandler);
        this.tAxisVariables = tAxisVariables;
        this.completeState = completeState;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (localName) {
            case "addAttributes" -> {
                State state = new AddAttributesHandler(saxHandler, this, tAttributes);
                saxHandler.setState(state);
            }
            case "values" -> {
                String type = attributes.getValue("type");
                if (type == null)
                    type = "";
                if (type.endsWith("List"))
                    type = type.substring(0, type.length() - 4);
                if (type.equals("unsignedShort")) //the xml name
                    type = "char"; //the PrimitiveArray name
                else if (type.equals("string")) //the xml name
                    type = "String"; //the PrimitiveArray name
                PAType elementPAType = PAType.fromCohortString(type); //throws Throwable if trouble
                double start      = String2.parseDouble(attributes.getValue("start"));
                double increment  = String2.parseDouble(attributes.getValue("increment"));
                int n             = String2.parseInt(attributes.getValue("n"));
                if (!Double.isNaN(start) &&
                        increment > 0 && //this could change to !NaN and !0
                        n > 0 && n < Integer.MAX_VALUE) {
                    //make PA with 1+ evenly spaced values
                    tValuesPA = PrimitiveArray.factory(elementPAType, n, false);
                    for (int i = 0; i < n; i++)
                        tValuesPA.addDouble(start + i * increment);
                } else {
                    //make PA with correct type, but size=0
                    tValuesPA = PrimitiveArray.factory(elementPAType, 0, "");
                }
            }
            default -> String2.log("Unexpected start tag: " + localName);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        content.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws Throwable {
        String contentStr = content.toString().trim();

        switch (localName) {
            case "sourceName" -> tSourceName = contentStr;
            case "destinationName" -> tDestinationName = contentStr;
            case "values" -> {
                if (tValuesPA.size() == 0) {
                    tValuesPA = PrimitiveArray.csvFactory(tValuesPA.elementType(), contentStr);
                }
            }
            case "axisVariable" -> {
                tAxisVariables.add(new Object[]{tSourceName, tDestinationName, tAttributes, tValuesPA});
                saxHandler.setState(this.completeState);
            }
            default -> String2.log("Unexpected end tag: " + localName);
        }
    }
}
