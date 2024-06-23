package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashSet;

public class TopLevelHandler extends DefaultHandler {
    private StringBuilder data = null;

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        data.append(new String(ch, start, length));
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if(localName.equals("angularDegreeUnits")) {
            String ts = data.toString();
            if (!String2.isSomething(ts))
                ts = EDStatic.DEFAULT_ANGULAR_DEGREE_UNITS;
            EDStatic.angularDegreeUnitsSet =
                    new HashSet<String>(String2.toArrayList(StringArray.fromCSVNoBlanks(ts).toArray())); //so canonical
            String2.log("angularDegreeUnits=" + String2.toCSVString(EDStatic.angularDegreeUnitsSet));
        } else if(localName.equals("angularDegreeTrueUnits")) {
            String ts = data.toString();
            if (!String2.isSomething(ts))
                ts = EDStatic.DEFAULT_ANGULAR_DEGREE_TRUE_UNITS;
            EDStatic.angularDegreeTrueUnitsSet =
                    new HashSet<String>(String2.toArrayList(StringArray.fromCSVNoBlanks(ts).toArray())); //so canonical
            String2.log("angularDegreeTrueUnits=" + String2.toCSVString(EDStatic.angularDegreeTrueUnitsSet));
        }
    }
}
