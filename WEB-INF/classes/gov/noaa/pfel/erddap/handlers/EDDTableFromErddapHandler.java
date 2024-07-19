package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromErddap;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


public class EDDTableFromErddapHandler extends State {
    private StringBuilder content = new StringBuilder();
    private String datasetID;
    private State completeState;
    private SaxParsingContext context;

    public EDDTableFromErddapHandler(SaxHandler saxHandler, String datasetID, State completeState, SaxParsingContext context) {
        super(saxHandler);
        this.datasetID = datasetID;
        this.completeState = completeState;
        this.context = context;
    }

    public int tReloadEveryNMinutes = Integer.MAX_VALUE;
    public String tAccessibleTo = null;
    public String tGraphsAccessibleTo = null;
    public boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
    public StringArray tOnChange = new StringArray();
    public boolean tSubscribeToRemoteErddapDataset = EDStatic.subscribeToRemoteErddapDataset;
    public boolean tRedirect = true;
    public String tFgdcFile = null;
    public String tIso19115File = null;
    public String tSosOfferingPrefix = null;
    public String tLocalSourceUrl = null;
    public String tDefaultDataQuery = null;
    public String tDefaultGraphQuery = null;
    public String tAddVariablesWhere = null;


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
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
            case "accessibleTo" -> tAccessibleTo = contentStr;
            case "graphsAccessibleTo" -> tGraphsAccessibleTo = contentStr;
            case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
            case "sourceUrl" -> tLocalSourceUrl = contentStr;
            case "onChange" -> tOnChange.add(contentStr);
            case "fgdcFile" -> tFgdcFile = contentStr;
            case "iso19115File" -> tIso19115File = contentStr;
            case "sosOfferingPrefix" -> tSosOfferingPrefix = contentStr;
            case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
            case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
            case "addVariablesWhere" -> tAddVariablesWhere = contentStr;
            case "subscribeToRemoteErddapDataset" -> tSubscribeToRemoteErddapDataset = String2.parseBoolean(contentStr);
            case "redirect" -> tRedirect = String2.parseBoolean(contentStr);
            case "dataset" -> {
                EDD dataset = new EDDTableFromErddap(datasetID,
                        tAccessibleTo, tGraphsAccessibleTo, tAccessibleViaFiles,
                        tOnChange, tFgdcFile, tIso19115File, tSosOfferingPrefix,
                        tDefaultDataQuery, tDefaultGraphQuery, tAddVariablesWhere,
                        tReloadEveryNMinutes,
                        tLocalSourceUrl, tSubscribeToRemoteErddapDataset, tRedirect
                );

                context.getErddap().processDataset(dataset, context);
                saxHandler.setState(this.completeState);
            }
            default -> String2.log("Unexpected end tag: " + localName);
        }
        content.setLength(0);
    }
}
