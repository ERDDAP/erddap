package gov.noaa.pfel.erddap.handlers;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class SkipDatasetHandler extends StateWithParent {

  public SkipDatasetHandler(SaxHandler saxHandler, State completeState) {
    super(saxHandler, completeState);
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {}

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {}

  @Override
  public void endElement(String uri, String localName, String qName) {
    if (localName.equals("dataset")) {
      saxHandler.setState(this.completeState);
    }
  }
}
