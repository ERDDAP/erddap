package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TopLevelDatasetCapture extends State {
  private SaxParsingContext context;
  private EDD dataset;

  public TopLevelDatasetCapture(SaxHandler saxHandler, SaxParsingContext context) {
    super(saxHandler);
    this.context = context;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {

    switch (localName) {
      case "convertToPublicSourceUrl" -> {
        String tFrom = attributes.getValue("from");
        String tTo = attributes.getValue("to");
        int spo = EDStatic.convertToPublicSourceUrlFromSlashPo(tFrom);
        if (tFrom != null && tFrom.length() > 3 && spo == tFrom.length() - 1 && tTo != null) {
          EDStatic.convertToPublicSourceUrl.put(tFrom, tTo);
        }
      }
      case "dataset" -> {
        String datasetType = attributes.getValue("type");
        String datasetID = attributes.getValue("datasetID");
        String active = attributes.getValue("active");

        State state =
            HandlerFactory.getHandlerFor(
                datasetType, datasetID, active, this, saxHandler, context, true);
        saxHandler.setState(state);
      }
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {}

  @Override
  public void endElement(String uri, String localName, String qName) {}

  @Override
  public void handleDataset(EDD dataset) {
    // Only capture the first dataset
    if (this.dataset == null) {
      this.dataset = dataset;
    }
  }

  public EDD getDataset() {
    return this.dataset;
  }

  @Override
  public void popState() {
    String2.log("Attempt to pop top level handler. Something likely went wrong.");
  }
}
