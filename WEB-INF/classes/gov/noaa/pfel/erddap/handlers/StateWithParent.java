package gov.noaa.pfel.erddap.handlers;

public abstract class StateWithParent extends State {
  protected State completeState;

  public StateWithParent(SaxHandler saxHandler, State completeState) {
    super(saxHandler);
    this.completeState = completeState;
  }

  @Override
  public void popState() {
    saxHandler.setState(this.completeState);
  }
}
