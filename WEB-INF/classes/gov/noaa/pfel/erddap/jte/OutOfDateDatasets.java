package gov.noaa.pfel.erddap.jte;

import com.cohort.util.Calendar2;
import gov.noaa.pfel.erddap.util.EDStatic;

public class OutOfDateDatasets {
  public static String getAdvcoutOfDateAr(int language) {
    return EDStatic.messages.advc_outOfDateAr[language];
  }

  public static String getNMatchingAr(int language) {
    return EDStatic.messages.nMatchingAr[language];
  }

  public static String getGeneratedAtAr(int language) {
    return EDStatic.messages.generatedAtAr[language];
  }

  public static String getAutoRefreshAr(int language) {
    return EDStatic.messages.autoRefreshAr[language];
  }

  public static String getOptionsAr(int language) {
    return EDStatic.messages.optionsAr[language];
  }

  public static String getAddConstraintsAr(int language) {
    return EDStatic.messages.addConstraintsAr[language];
  }

  public static String getPercentEncodeAr(int language) {
    return EDStatic.messages.percentEncodeAr[language];
  }

  public static String getRestfulInformationFormatsAr(int language) {
    return EDStatic.messages.restfulInformationFormatsAr[language];
  }

  public static String getRestfulViaServiceAr(int language) {
    return EDStatic.messages.restfulViaServiceAr[language];
  }

  public static String getCurrentTimeZulu() {
    return Calendar2.getCurrentISODateTimeStringZulu() + "Z";
  }
}
