package com.cohort.util;

import java.util.EnumSet;
import org.nibor.autolink.LinkExtractor;
import org.nibor.autolink.LinkSpan;
import org.nibor.autolink.LinkType;

/** A helper class for finding and splitting links in text using autolink-java. */
public class LinkHelper {

  private static final LinkExtractor EXTRACTOR =
      LinkExtractor.builder().linkTypes(EnumSet.of(LinkType.URL, LinkType.WWW)).build();

  /**
   * A CharSequence that lazily replaces backslashes with forward slashes and masks spaces and
   * quotes within URLs to allow autolink-java to recognize URLs with quoted spaces.
   */
  private static class NormalizedCharSequence implements CharSequence {
    private final String source;
    private final char[] masked;

    NormalizedCharSequence(String source) {
      this.source = source;
      this.masked = source.toCharArray();
      boolean inDoubleQuote = false;
      for (int i = 0; i < masked.length; i++) {
        char c = masked[i];

        if (c == '\"') {
          inDoubleQuote = !inDoubleQuote;
          masked[i] = 'z'; // Mask quote as alphanumeric to ensure linker includes it
          continue;
        }

        if (c == '%' && i + 2 < masked.length && masked[i + 1] == '2' && masked[i + 2] == '2') {
          inDoubleQuote = !inDoubleQuote;
          // Mask %22 as zzz to ensure linker includes it and toggles quote state
          masked[i] = 'z';
          masked[i + 1] = 'z';
          masked[i + 2] = 'z';
          i += 2;
          continue;
        }

        if (c == '\\') {
          masked[i] = '/';
        } else if (c == ' ' && inDoubleQuote) {
          masked[i] = '_';
        }
      }
    }

    @Override
    public int length() {
      return source.length();
    }

    @Override
    public char charAt(int index) {
      return masked[index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      return new NormalizedCharSequence(source.substring(start, end));
    }

    @Override
    public String toString() {
      return new String(masked);
    }
  }

  private static boolean isValidLink(CharSequence searchIn, LinkSpan span) {
    if (span.getType() == LinkType.WWW) {
      return true;
    }
    // LinkType.URL
    int begin = span.getBeginIndex();
    int end = span.getEndIndex();

    // Work on normalized text for validation
    String linkText = searchIn.subSequence(begin, end).toString().toLowerCase();

    // Find "://" (searchIn is already normalized, so backslashes are forward slashes)
    int schemeEnd = linkText.indexOf("://");
    if (schemeEnd == -1) return false;

    // Check if it's file:
    if (schemeEnd == 4 && linkText.startsWith("file")) {
      return true;
    }

    int hostStart = schemeEnd + 3;
    // Skip extra slashes (e.g. http:////)
    while (hostStart < linkText.length() && linkText.charAt(hostStart) == '/') {
      hostStart++;
    }
    if (hostStart >= linkText.length()) return false;

    // Check for user:pass@
    int atSign = -1;
    for (int i = hostStart; i < linkText.length(); i++) {
      char c = linkText.charAt(i);
      if (c == '/' || c == '?' || c == '#') break;
      if (c == '@') {
        atSign = i;
        break;
      }
    }
    if (atSign != -1) {
      hostStart = atSign + 1;
    }
    if (hostStart >= linkText.length()) return false;

    // Check for localhost
    if (linkText.substring(hostStart).startsWith("localhost")) {
      int endOfLocalhost = hostStart + 9;
      if (endOfLocalhost == linkText.length()) return true;
      char c = linkText.charAt(endOfLocalhost);
      if (c == '/' || c == ':' || c == '?' || c == '#') return true;
    }

    // Find host end and check for dot
    boolean hasDot = false;
    for (int i = hostStart; i < linkText.length(); i++) {
      char c = linkText.charAt(i);
      if (c == '/' || c == ':' || c == '?' || c == '#') break;
      if (c == '.') hasDot = true;
    }
    return hasDot;
  }

  /** Functional interface for handling parts of a string during linkification. */
  @FunctionalInterface
  public interface LinkPartHandler {
    void handle(String text, boolean isUrl);
  }

  /**
   * Processes the input string, identifying links and plain text segments, and passes each to the
   * handler.
   *
   * @param input the text to process
   * @param handler the handler to receive text and URL segments
   * @return true if at least one link was found and handled
   */
  public static boolean linkify(String input, LinkPartHandler handler) {
    if (input == null || input.isEmpty()) {
      return false;
    }

    CharSequence searchIn = new NormalizedCharSequence(input);
    int lastEnd = 0;
    boolean found = false;
    for (LinkSpan span : EXTRACTOR.extractLinks(searchIn)) {
      if (isValidLink(searchIn, span)) {
        found = true;
        if (span.getBeginIndex() > lastEnd) {
          handler.handle(input.substring(lastEnd, span.getBeginIndex()), false);
        }
        handler.handle(input.substring(span.getBeginIndex(), span.getEndIndex()), true);
        lastEnd = span.getEndIndex();
      }
    }
    if (lastEnd < input.length()) {
      handler.handle(input.substring(lastEnd), false);
    }
    return found;
  }

  /**
   * This is used when setting href attributes in anchor tags. Specifically this is to make sure
   * browsers know this is an absolute url and not a relative url.
   */
  public static String addHttpsForWWW(final String input) {
    if (input != null && input.startsWith("www.")) {
      return "https://" + input;
    }
    return input;
  }
}
