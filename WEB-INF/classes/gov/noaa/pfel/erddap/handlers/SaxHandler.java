package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.String2;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SaxHandler extends DefaultHandler {
  private State state;
  SaxParsingContext context;

  public SaxHandler(SaxParsingContext context) {
    this.context = context;
  }

  public void setState(State state) {
    this.state = state;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    try {
      this.state.startElement(uri, localName, qName, attributes);
    } catch (Throwable e) {
      context.getWarningsFromLoadDatasets().append(e.getMessage());
      String2.log(e.getMessage());
      this.state.popState();
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    try {
      this.state.characters(ch, start, length);
    } catch (Throwable e) {
      context.getWarningsFromLoadDatasets().append(e.getMessage());
      String2.log(e.getMessage());
      this.state.popState();
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) {
    try {
      this.state.endElement(uri, localName, qName);
    } catch (Throwable e) {
      context.getWarningsFromLoadDatasets().append(e.getMessage());
      String2.log(e.getMessage());
      this.state.popState();
    }
  }
}
