package gov.noaa.pfel.erddap.jte;

import com.cohort.util.Calendar2;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.util.HtmlWidgets;
import gov.noaa.pfel.erddap.util.EDConfig;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import jakarta.servlet.http.HttpServletRequest;
import java.text.MessageFormat;

public class TemplateHelper {
  public static String getFormated(String value, String val) {
    return MessageFormat.format(value, val);
  }

  public static String getMessage(Message message, int language) {
    return EDStatic.messages.get(message, language);
  }

  public static EDConfig getConfig() {
    return EDStatic.config;
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

  public static String getLoginHtml(HttpServletRequest request, int language, String loggedInAs) {
    return EDStatic.getLoginHtml(request, language, loggedInAs);
  }

  // Prepare tooltip text for Tip('...') by escaping characters similarly to HtmlWidgets.htmlTooltip
  public static String escapeTooltipForTip(String html) {
    if (html == null) html = "";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < html.length(); i++) {
      char ch = html.charAt(i);
      if (ch == '\\') sb.append("\\\\");
      else if (ch == '"') sb.append("&quot;");
      else if (ch == '\'') sb.append("&#39;");
      else if (ch == '\n') sb.append(' ');
      else sb.append(ch);
    }
    String s = sb.toString();
    s = s.replace("&#39;", "\\'");
    s = s.replace("  ", "&nbsp;&nbsp;");
    return s;
  }

  // Return the question-mark image URL used in templates
  public static String questionMarkImageUrl(
      HttpServletRequest request, int language, String loggedInAs) {
    return EDStatic.imageDirUrl(request, loggedInAs, language)
        + EDStatic.messages.questionMarkImageFile;
  }
}
