package gov.noaa.pfel.erddap.handlers;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SaxHandler extends DefaultHandler {
    private State state;

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        this.state.startElement(uri, localName, qName, attributes);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        this.state.characters(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        try {
            this.state.endElement(uri, localName, qName);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
