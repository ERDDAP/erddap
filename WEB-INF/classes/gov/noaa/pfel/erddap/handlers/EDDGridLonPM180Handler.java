package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDGridLonPM180;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDGridLonPM180Handler extends State {
    private StringBuilder content = new StringBuilder();
    private SaxParsingContext context;
    private State completeState;
    private String datasetID;

    public EDDGridLonPM180Handler(SaxHandler saxHandler, String datasetID, State completeState, SaxParsingContext context) {
        super(saxHandler);
        this.datasetID = datasetID;
        this.completeState = completeState;
        this.context = context;
    }

    public EDDGrid tChildDataset = null;
    public String tAccessibleTo = null;
    public String tGraphsAccessibleTo = null;
    public boolean tAccessibleViaWMS = true;
    public boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
    public StringArray tOnChange = new StringArray();
    public String tFgdcFile = null;
    public String tIso19115File = null;
    public String tDefaultDataQuery = null;
    public String tDefaultGraphQuery = null;
    public int tReloadEveryNMinutes = Integer.MAX_VALUE;
    public int tUpdateEveryNMillis = Integer.MAX_VALUE;
    public int tnThreads = -1;
    public boolean tDimensionValuesInMemory = true;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (localName.equals("dataset")) {
            if (tChildDataset == null) {
                String tType = attributes.getValue("type");
                if (tType == null || !tType.startsWith("EDDGrid")) {
                    throw new RuntimeException(
                            "Datasets.xml error: "
                                    + "The dataset defined in an "
                                    + "EDDGridLonPM180 must be a subclass of EDDGrid.");
                }

                String active = attributes.getValue("active");
                String childDatasetID = attributes.getValue("datasetID");
                State state = HandlerFactory.getHandlerFor(tType, childDatasetID, active, this, saxHandler, context);
                saxHandler.setState(state);
            } else {
                throw new RuntimeException(
                        "Datasets.xml error: "
                                + "The dataset defined in an "
                                + "EDDGridLonPM180 must be a subclass of EDDGrid.");
            }
        } else {
            String2.log("Unexpected start tag: " + localName);
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
            case "reloadEveryNMinutes" -> tReloadEveryNMinutes = String2.parseInt(contentStr);
            case "updateEveryNMillis" -> tUpdateEveryNMillis = String2.parseInt(contentStr);
            case "accessibleTo" -> tAccessibleTo = contentStr;
            case "graphsAccessibleTo" -> tGraphsAccessibleTo = contentStr;
            case "accessibleViaWMS" -> tAccessibleViaWMS = String2.parseBoolean(contentStr);
            case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
            case "onChange" -> tOnChange.add(contentStr);
            case "fgdcFile" -> tFgdcFile = contentStr;
            case "iso19115File" -> tIso19115File = contentStr;
            case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
            case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
            case "nThreads" -> tnThreads = String2.parseInt(contentStr);
            case "dimensionValuesInMemory" -> tDimensionValuesInMemory = String2.parseBoolean(contentStr);
            case "dataset" -> {
                EDD dataset = new EDDGridLonPM180(
                        context.getErddap(),
                        datasetID,
                        tAccessibleTo,
                        tGraphsAccessibleTo,
                        tAccessibleViaWMS,
                        tAccessibleViaFiles,
                        tOnChange,
                        tFgdcFile,
                        tIso19115File,
                        tDefaultDataQuery,
                        tDefaultGraphQuery,
                        tReloadEveryNMinutes,
                        tUpdateEveryNMillis,
                        tChildDataset,
                        tnThreads,
                        tDimensionValuesInMemory
                );

                this.completeState.handleDataset(dataset);
                saxHandler.setState(this.completeState);
            }
            default -> String2.log("Unexpected end tag: " + localName);
        }
        content.setLength(0);
    }

    @Override
    public void handleDataset(EDD dataset) {
        tChildDataset = (EDDGrid) dataset;
    }
}
