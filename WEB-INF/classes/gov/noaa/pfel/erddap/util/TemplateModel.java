package gov.noaa.pfel.erddap.util;

public class TemplateModel {

  public final String youAreHere;
  public final String statistics;

  /**
   * The constructor.
   *
   * @param youAreHere the string to use for the "You are here" link.
   * @param statistics the string to use for the "Statistics" link.
   */
  public TemplateModel(String youAreHere, String statistics) {
    this.youAreHere = youAreHere;
    this.statistics = statistics;
  }
}
