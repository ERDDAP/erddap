package testSupport;

/** Helper to centralize external test base URLs so tests can be pointed at mocks. */
public class ExternalTestUrls {
  public static String apdrcHawaiiBase() {
    return System.getProperty("test.apdrc.hawaiiUrl", "http://apdrc.soest.hawaii.edu");
  }
}
