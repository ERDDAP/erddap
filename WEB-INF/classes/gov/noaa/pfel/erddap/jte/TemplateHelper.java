package gov.noaa.pfel.erddap.jte;

import com.cohort.util.Calendar2;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.util.HtmlWidgets;
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

  public static String getEndBodyHtml(String erddapUrl, int language) {
    // ported from EDStatic.endBodyHtml
    String s =
        String2.replaceAll(getMessage(Message.END_BODY_HTML, language), "&erddapUrl;", erddapUrl);
    if (language > 0) {
      s =
          s.replace(
              "<hr>",
              """
        <br><img src="%s/images/TranslatedByGoogle.png" alt="Translated by Google">
        %s
        <hr>
        """
                  .formatted(
                      erddapUrl,
                      HtmlWidgets.htmlTooltipImage(
                          erddapUrl + "/images/" + EDStatic.messages.questionMarkImageFile,
                          "?",
                          EDStatic.translationDisclaimer,
                          "")));
    }
    return s;
  }
}
