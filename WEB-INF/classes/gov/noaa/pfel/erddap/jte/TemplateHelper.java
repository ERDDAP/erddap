package gov.noaa.pfel.erddap.jte;

import com.cohort.util.Calendar2;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.text.MessageFormat;

public class TemplateHelper {
  public static String getFormated(String value, String val) {
    return MessageFormat.format(value, val);
  }

  public static String getMessage(Message message, int language) {
    return EDStatic.messages.get(message, language);
  }

  public static String getCurrentTimeZulu() {
    return Calendar2.getCurrentISODateTimeStringZulu() + "Z";
  }
}
