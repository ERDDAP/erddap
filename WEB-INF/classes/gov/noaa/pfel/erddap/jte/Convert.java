package gov.noaa.pfel.erddap.jte;

import gov.noaa.pfel.erddap.jte.Index.ConverterLink;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.util.ArrayList;
import java.util.List;

/** Helper class providing shared data for ERDDAP converter pages and navigation links. */
public class Convert {

  /**
   * Generates the canonical list of converter tool links for the home page and converter index.
   *
   * @param tErddapUrl The base ERDDAP URL
   * @param language The selected language index
   * @return A list of {@link ConverterLink} instances representing each conversion tool
   */
  public static List<ConverterLink> getConverterLinks(String tErddapUrl, int language) {
    List<ConverterLink> links = new ArrayList<>();
    links.add(
        new ConverterLink(
            tErddapUrl + "/convert/oceanicAtmosphericAcronyms.html",
            EDStatic.messages.get(Message.ACRONYMS, language),
            EDStatic.messages.get(Message.CONVERT_OA_ACRONYMS_TO_FROM, language)));
    links.add(
        new ConverterLink(
            tErddapUrl + "/convert/fipscounty.html",
            EDStatic.messages.get(Message.FIPS_COUNTY_CODES, language),
            EDStatic.messages.get(Message.CONVERT_FIPS_COUNTY, language)));
    links.add(
        new ConverterLink(
            tErddapUrl + "/convert/interpolate.html",
            EDStatic.messages.get(Message.INTERPOLATE, language),
            EDStatic.messages.get(Message.CONVERT_INTERPOLATE, language)));
    links.add(
        new ConverterLink(
            tErddapUrl + "/convert/keywords.html",
            EDStatic.messages.get(Message.KEYWORDS, language),
            EDStatic.messages.get(Message.CONVERT_KEYWORDS, language)));
    links.add(
        new ConverterLink(
            tErddapUrl + "/convert/time.html",
            EDStatic.messages.get(Message.TIME, language),
            EDStatic.messages.get(Message.CONVERT_TIME, language)));
    links.add(
        new ConverterLink(
            tErddapUrl + "/convert/units.html",
            EDStatic.messages.get(Message.UNITS, language),
            EDStatic.messages.get(Message.CONVERT_UNITS, language)));
    links.add(
        new ConverterLink(
            tErddapUrl + "/convert/color.html",
            EDStatic.messages.get(Message.CONVERT_COLORS, language),
            EDStatic.messages.get(Message.CONVERT_COLORS_MESSAGE, language)));
    links.add(
        new ConverterLink(
            tErddapUrl + "/convert/urls.html",
            "URLs",
            EDStatic.messages.get(Message.CONVERT_URLS, language)));
    links.add(
        new ConverterLink(
            tErddapUrl + "/convert/oceanicAtmosphericVariableNames.html",
            EDStatic.messages.get(Message.VARIABLE_NAMES, language),
            EDStatic.messages.get(Message.CONVERT_OA_VARIABLE_NAMES_TO_FROM, language)));
    return links;
  }
}
