package gov.noaa.pfel.erddap.util;

import java.net.URL;

/**
 * Translations are now done in python with an open source machine translation library.
 * /translation/translate.py
 */
public class TranslateMessages {
  /** This is the order the languages will appear in the list shown to users in ERRDDAP */
  public static String[] languageList = {
    // arbitrarily selected from most commonly used:
    // https://www.visualcapitalist.com/100-most-spoken-languages/
    // Chinese-CN=Simplified  Chinese-TW=Traditional -- I wanted shorter names
    // I got main Indian languages, but not all.
    // I got few of the Eastern European languages.
    // Google supports Tagalog but not Filipino (the official language)
    // Arabic is hard. (right to left etc)  Translated bits seemed out-of-order.
    // in English:
    // Language names taken from https://en.wikipedia.org/wiki/List_of_language_names
    // where a language is available in multiple scripts, I selected the one that is
    // used in the translation file (typically the one that is default for the language code).

    // The commented out languages are because the current translation approach does not
    // include support for them.
    "English - English",
    "বাংলা - Bengali",
    "汉语 - Chinese (Simplified)",
    "漢語 - Chinese (Transitional)",
    "Čeština - Czech",
    "Dansk - Danish",
    "Nederlands - Dutch",
    "Suomi - Finnish",
    "Français - French",
    "Deutsch - German",
    "Ελληνικά - Greek",
    // "ગુજરાતી - Gujarati",
    "हिन्दी - Hindi",
    "Magyar - Hungarian",
    "Bahasa Indonesia - Indonesian",
    "Gaeilge - Irish",
    "Italiano - Italian",
    "日本語 - Japanese",
    "한국어 - Korean",
    // "मराठी - Marathi",
    "Norsk - Norwegian",
    "Polski - Polish",
    "Português - Portuguese",
    // "ਪੰਜਾਬੀ - Punjabi",
    "Română - Romanian",
    "Русский - Russian",
    "Español - Spanish",
    // "Kiswahili - Swahili",
    "Svenska - Swedish",
    "Wikang Tagalog - Tagalog",
    "ภาษาไทย - Thai",
    "Türkçe - Turkish",
    "Українська - Ukrainian",
    "اُردُو - Urdu",
    // "Tiếng Việt Nam - Vietnamese"
  };

  public static String[] languageCodeList = {
    // The commented out languages are because the current translation approach does not
    // include support for them.
    "en",
    "bn",
    "zh-CN",
    "zh-TW",
    "cs",
    "da",
    "nl",
    "fi",
    "fr",
    "de",
    "el",
    // "gu",
    "hi",
    "hu",
    "id",
    "ga",
    "it",
    "ja",
    "ko",
    // "mr",
    "no",
    "pl",
    "pt",
    // "pa",
    "ro",
    "ru",
    "es",
    // "sw",
    "sv",
    "tl",
    "th",
    "tr",
    "uk",
    "ur",
    // "vi"
  };

  public static String utilDir = "/gov/noaa/pfel/erddap/util/";
  public static URL translatedMessagesDir =
      TranslateMessages.class.getResource(utilDir + "translatedMessages/");

  protected static String[] HTMLEntities = {
    "<p>", "<br>", "</a>", "<kbd>", "<strong>", "<li>"
  }; // 2021-12-21 was also "&lt;", "&gt;", "&quot;", "&amp;", but they are used in plain text in
  // xml
}
