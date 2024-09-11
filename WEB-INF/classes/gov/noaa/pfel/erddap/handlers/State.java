package gov.noaa.pfel.erddap.handlers;

import gov.noaa.pfel.erddap.dataset.EDD;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;

public abstract class State {
  protected SaxHandler saxHandler;

  public State(SaxHandler saxHandler) {
    this.saxHandler = saxHandler;
  }

  public void handleDataset(EDD dataset) throws SAXNotSupportedException {
    throw new SAXNotSupportedException();
  }

  public abstract void startElement(
      String uri, String localName, String qName, Attributes attributes) throws SAXException;

  public abstract void characters(char[] ch, int start, int length) throws SAXException;

  public abstract void endElement(String uri, String localName, String qName) throws Throwable;

  public abstract void popState();
}
