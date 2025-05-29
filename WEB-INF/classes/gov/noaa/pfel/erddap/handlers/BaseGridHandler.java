package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.variable.AxisVariableInfo;
import java.util.ArrayList;

public abstract class BaseGridHandler extends BaseDatasetHandler {

  protected int tnThreads = -1;
  protected boolean tAccessibleViaWMS = true;
  protected boolean tDimensionValuesInMemory = true;
  protected final ArrayList<AxisVariableInfo> tAxisVariables = new ArrayList<>();

  public BaseGridHandler(SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, datasetID, completeState);
  }

  protected void handleAxisVariable(String localName) {
    if ("axisVariable".equals(localName)) {
      State state = new AxisVariableHandler(saxHandler, tAxisVariables, this);
      saxHandler.setState(state);
    }
  }

  @Override
  protected boolean handleEndElement(String contentStr, String localName) {
    switch (localName) {
      case "accessibleViaWMS" -> tAccessibleViaWMS = String2.parseBoolean(contentStr);
      case "nThreads" -> tnThreads = String2.parseInt(contentStr);
      case "dimensionValuesInMemory" -> tDimensionValuesInMemory = String2.parseBoolean(contentStr);
      default -> {
        return false;
      }
    }
    return true;
  }
}
