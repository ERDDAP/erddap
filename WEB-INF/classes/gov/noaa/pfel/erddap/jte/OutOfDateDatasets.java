package gov.noaa.pfel.erddap.jte;

import com.cohort.util.Calendar2;
import java.text.MessageFormat;

public class OutOfDateDatasets {
  public static String getCurrentTimeZulu() {
    return Calendar2.getCurrentISODateTimeStringZulu() + "Z";
  }

  public static String getFormated(String value, String val) {
    return MessageFormat.format(value, val);
  }
}
