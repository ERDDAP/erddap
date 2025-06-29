package gov.noaa.pfel.erddap.jte;

import gov.noaa.pfel.erddap.util.EDStatic;

public class Legal {
  public static String getLegalNoticesAr(int language) {
    return EDStatic.messages.legalNoticesAr[language];
  }

  public static String getStandardGeneralDisclaimerAr(int language) {
    return EDStatic.messages.standardGeneralDisclaimerAr[language];
  }

  public static String getLegal(int language, String tErddapUrl) {
    return EDStatic.legal(language, tErddapUrl);
  }
}
