package gov.noaa.pfel.erddap.jte;

import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.Subscriptions;
import java.text.MessageFormat;

public class Subscription {
  public static String getFormated(String value, String val) {
    return MessageFormat.format(value, val);
  }

  public static String getMessage(Message message, int language) {
    return EDStatic.messages.get(message, language);
  }

  public static String getAddHtml() {
    return Subscriptions.ADD_HTML;
  }

  public static String getValidateHtml() {
    return Subscriptions.VALIDATE_HTML;
  }

  public static String getListHtml() {
    return Subscriptions.LIST_HTML;
  }

  public static String getRemoveHtml() {
    return Subscriptions.REMOVE_HTML;
  }
}
