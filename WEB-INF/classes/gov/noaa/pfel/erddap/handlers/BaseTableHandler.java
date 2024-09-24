package gov.noaa.pfel.erddap.handlers;

public abstract class BaseTableHandler extends BaseDatasetHandler {

  protected String tSosOfferingPrefix = null;
  protected String tAddVariablesWhere = null;

  public BaseTableHandler(SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, datasetID, completeState);
  }

  @Override
  protected boolean handleEndElement(String contentStr, String localName) {
    switch (localName) {
      case "sosOfferingPrefix" -> tSosOfferingPrefix = contentStr;
      case "addVariablesWhere" -> tAddVariablesWhere = contentStr;
      default -> {
        return false;
      }
    }
    return true;
  }
}
