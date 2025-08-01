package gov.noaa.pfel.erddap.jte;

import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;

public class Legal {

  public static String getMessage(Message message, int language) {
    return EDStatic.messages.get(message, language);
  }

  public static String getLegal(int language, String tErddapUrl) {
    return EDStatic.legal(language, tErddapUrl);
  }
}
