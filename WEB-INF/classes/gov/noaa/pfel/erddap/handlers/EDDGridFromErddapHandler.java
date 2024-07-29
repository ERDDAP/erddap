package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGridFromErddap;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import static gov.noaa.pfel.erddap.dataset.EDD.DEFAULT_RELOAD_EVERY_N_MINUTES;

public class EDDGridFromErddapHandler extends State {
    private StringBuilder content = new StringBuilder();
    private String datasetID;
    private State completeState;

    public EDDGridFromErddapHandler(SaxHandler saxHandler, String datasetID, State completeState) {
        super(saxHandler);
        this.datasetID = datasetID;
        this.completeState = completeState;
    }

    public int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
    public int tUpdateEveryNMillis = 0;
    public String tAccessibleTo = null;
    public String tGraphsAccessibleTo = null;
    public boolean tAccessibleViaWMS = true;
    public boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
    public boolean tSubscribeToRemoteErddapDataset = EDStatic.subscribeToRemoteErddapDataset;
    public boolean tRedirect = true;
    public StringArray tOnChange = new StringArray();
    public String tFgdcFile = null;
    public String tIso19115File = null;
    public String tLocalSourceUrl = null;
    public String tDefaultDataQuery = null;
    public String tDefaultGraphQuery = null;
    public int tnThreads = -1; // interpret invalid values (like -1) as EDStatic.nGridThreads
    public boolean tDimensionValuesInMemory = true;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {}

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
            case "subscribeToRemoteErddapDataset" -> tSubscribeToRemoteErddapDataset = String2.parseBoolean(contentStr);
            case "sourceUrl" -> tLocalSourceUrl = contentStr;
            case "onChange" -> tOnChange.add(contentStr);
            case "fgdcFile" -> tFgdcFile = contentStr;
            case "iso19115File" -> tIso19115File = contentStr;
            case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
            case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
            case "nThreads" -> tnThreads = String2.parseInt(contentStr);
            case "dimensionValuesInMemory" -> tDimensionValuesInMemory = String2.parseBoolean(contentStr);
            case "redirect" -> tRedirect = String2.parseBoolean(contentStr);
            case "dataset" -> {
                EDD dataset = new EDDGridFromErddap(
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
                        tLocalSourceUrl,
                        tSubscribeToRemoteErddapDataset,
                        tRedirect,
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

}
