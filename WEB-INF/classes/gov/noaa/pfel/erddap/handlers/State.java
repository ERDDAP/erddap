package gov.noaa.pfel.erddap.handlers;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

abstract public class State {
    protected SaxHandler saxHandler;

    public State(SaxHandler saxHandler) {
        this.saxHandler = saxHandler;
    }

    public abstract void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException;
    public abstract void characters(char[] ch, int start, int length) throws SAXException;
    public abstract void endElement(String uri, String localName, String qName) throws SAXException;
}
