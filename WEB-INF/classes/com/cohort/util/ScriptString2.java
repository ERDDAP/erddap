/* This file is Copyright (c) 2019 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 */
package com.cohort.util;

/**
 * This class makes about half of the static methods in com.cohort.String2 accessible to JexlScript
 * scripts as "String2.<i>name</i>()" methods. All of the constants and methods related to files,
 * system information, ArrayList, Enumeration, Vector, BitSet, Map, interactive computing, etc., are
 * not included here.
 *
 * <p>The underlying String2 class is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in com/cohort/util/LICENSE.txt.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2019-11-14
 */
public class ScriptString2 {

  /**
   * This returns the string which sorts higher. null sorts low.
   *
   * @param s1
   * @param s2
   * @return the string which sorts higher.
   */
  public static String max(String s1, String s2) {
    return String2.max(s1, s2);
  }

  /**
   * This returns the string which sorts lower. null sorts low.
   *
   * @param s1
   * @param s2
   * @return the string which sorts lower.
   */
  public static String min(String s1, String s2) {
    return String2.min(s1, s2);
  }

  /**
   * This makes a new String of specified length, filled with ch. For safety, if length&gt;=1000000,
   * it returns "".
   *
   * @param ch the character to fill the string
   * @param length the length of the string
   * @return a String 'length' long, filled with ch. If length &lt; 0 or &gt;= 1000000, this returns
   *     "".
   */
  public static String makeString(char ch, int length) {
    return String2.makeString(ch, length);
  }

  /**
   * Returns a String 'length' long, with 's' right-justified (using spaces as the added characters)
   * within the resulting String. If s is already longer, then there will be no change.
   *
   * @param s is the string to be right-justified.
   * @param length is desired length of the resulting string.
   * @return 's' right-justified to make the result 'length' long.
   */
  public static String right(String s, int length) {
    return String2.right(s, length);
  }

  /**
   * Returns a String 'length' long, with 's' left-justified (using spaces as the added characters)
   * within the resulting String. If s is already longer, then there will be no change.
   *
   * @param s is the string to be left-justified.
   * @param length is desired length of the resulting string.
   * @return 's' left-justified to make the result 'length' long.
   */
  public static String left(String s, int length) {
    return String2.left(s, length);
  }

  /**
   * Returns a String 'length' long, with 's' centered (using spaces as the added characters) within
   * the resulting String. If s is already longer, then there will be no change.
   *
   * @param s is the string to be centered.
   * @param length is desired length of the resulting string.
   * @return 's' centered to make the result 'length' long.
   */
  public static String center(String s, int length) {
    return String2.center(s, length);
  }

  /**
   * This returns a string no more than max characters long, throwing away the excess. If you want
   * to keep the whole string and just insert newlines periodically, use noLongLines() instead.
   *
   * @param s
   * @param max
   * @return s (if it is short) or the first max characters of s. If s==null, this returns "".
   */
  public static String noLongerThan(String s, int max) {
    return String2.noLongerThan(s, max);
  }

  /**
   * This is like noLongerThan, but if truncated, s.substring(0, max-3) + "..." is returned.
   *
   * @param s
   * @param max
   * @return s (if it is short) or the first max characters of s
   */
  public static String noLongerThanDots(String s, int max) {
    return String2.noLongerThanDots(s, max);
  }

  /**
   * This converts non-isPrintable characters to "[#]". \\n generates both [10] and a newline
   * character.
   *
   * @param s the string
   * @return s, but with non-32..126 characters replaced by [#]. The result ends with "[end]". null
   *     returns "[null][end]".
   */
  public static String annotatedString(String s) {
    return String2.annotatedString(s);
  }

  /**
   * This determines the number of initial characters that match.
   *
   * @param s1
   * @param s2
   * @return the number of characters that are the same at the start of both strings.
   */
  public static int getNMatchingCharacters(String s1, String s2) {
    return String2.getNMatchingCharacters(s1, s2);
  }

  /**
   * Finds the first instance of 'find' at or after fromIndex (0..), ignoring case.
   *
   * @param s
   * @param find
   * @return the first instance of 'find' at or after fromIndex (0..), ignoring case.
   */
  public static int indexOfIgnoreCase(String s, String find) {
    return String2.indexOfIgnoreCase(s, find);
  }

  /**
   * Finds the first instance of 'find' at or after fromIndex (0..), ignoring case.
   *
   * @param s
   * @param find
   * @param fromIndex
   * @return the first instance of 'find' at or after fromIndex (0..), ignoring case.
   */
  public static int indexOfIgnoreCase(String s, String find, int fromIndex) {
    return String2.indexOfIgnoreCase(s, find, fromIndex);
  }

  /**
   * This goes beyond indexOfIgnoreCase by looking after punctuation removed.
   *
   * @param s
   * @param find
   * @return true if find is loosely in s. Return false if s or find !isSomething.
   */
  public static boolean looselyContains(String s, String find) {
    return String2.looselyContains(s, find);
  }

  /**
   * This goes beyond equalsIgnoreCase by looking after punctuation removed.
   *
   * @param s1
   * @param s2
   * @return true if find is loosely in s. Return false if s or find !isSomething.
   */
  public static boolean looselyEquals(String s1, String s2) {
    return String2.looselyEquals(s1, s2);
  }

  /**
   * This finds (case-sensitive) the first whole word instance of 'word' in s. It will find 'word'
   * at the start or end of s. I.e., the character before and after (if any) mustn't be a letter or
   * digit or '_'.
   *
   * @param word must be a simple word (without regex special characters)
   * @return -1 if not found (or trouble, e.g., find=null)
   */
  public static int findWholeWord(String s, String word) {
    return String2.findWholeWord(s, word);
  }

  /**
   * This returns the first section of s (starting at fromIndex) which matches regex. !!! Note that
   * . in the regex doesn't match line terminators in s !!!
   *
   * @param s the source String.
   * @param regex the regular expression, see java.util.regex.Pattern.
   * @param fromIndex the starting index in s
   * @return the section of s which matches regex, or null if not found
   * @throws RuntimeException if trouble
   */
  public static String extractRegex(String s, String regex, int fromIndex) {
    return String2.extractRegex(s, regex, fromIndex);
  }

  /**
   * This returns all the sections of s that match regex. It assumes that the extracted parts don't
   * overlap. !!! Note that . in the regex doesn't match line terminators in s !!!
   *
   * @param s the source String
   * @param regex the regular expression, see java.util.regex.Pattern. Note that you often want to
   *     use the "reluctant" qualifiers which match as few chars as possible (e.g., ??, *?, +?) not
   *     the "greedy" qualifiers which match as many chars as possible (e.g., ?, *, +).
   * @return a String[] with all the matching sections of s (or String[0] if none)
   * @throws RuntimeException if trouble
   */
  public static String[] extractAllRegexes(String s, String regex) {
    return String2.extractAllRegexes(s, regex);
  }

  /**
   * This repeatedly finds the regex and extracts the specified captureGroup.
   *
   * @param s the source String
   * @param regex the regular expression, see java.util.regex.Pattern. Note that you often want to
   *     use the "reluctant" qualifiers which match as few chars as possible (e.g., ??, *?, +?) not
   *     the "greedy" qualifiers which match as many chars as possible (e.g., ?, *, +).
   * @return a String[] with the found strings in their original order.
   */
  public static String[] extractAllCaptureGroupsAsStringArray(
      String s, String regex, int captureGroupNumber) {
    return String2.extractAllCaptureGroupsAsStringArray(s, regex, captureGroupNumber);
  }

  /**
   * This returns the specified capture group from s.
   *
   * @param s the source String
   * @param regex the regular expression, see java.util.regex.Pattern, which matches part of s.
   * @param captureGroupNumber the number of the capture group (0 for entire regex, 1 for first
   *     capture group, 2 for second, etc.)
   * @return the value of the specified capture group in the first match of the regex, or null if
   *     the s doesn't match the regex
   * @throws RuntimeException if trouble, e.g., invalid regex syntax
   */
  public static String extractCaptureGroup(String s, String regex, int captureGroupNumber) {
    return String2.extractCaptureGroup(s, regex, captureGroupNumber);
  }

  /**
   * Finds the first instance of i at or after fromIndex (0.. ) in iArray.
   *
   * @param iArray
   * @param i the int you want to find
   * @param fromIndex the index number of the position to start the search
   * @return The first instance of i. If not found, it returns -1.
   */
  public static int indexOf(int[] iArray, int i, int fromIndex) {
    return String2.indexOf(iArray, i, fromIndex);
  }

  /**
   * Finds the first instance of i in iArray.
   *
   * @param iArray
   * @param i the int you want to find
   * @return The first instance of i. If not found, it returns -1.
   */
  public static int indexOf(int[] iArray, int i) {
    return String2.indexOf(iArray, i);
  }

  /**
   * Finds the first instance of c at or after fromIndex (0.. ) in cArray.
   *
   * @param cArray
   * @param c the char you want to find
   * @param fromIndex the index number of the position to start the search
   * @return The first instance of c. If not found, it returns -1.
   */
  public static int indexOf(char[] cArray, char c, int fromIndex) {
    return String2.indexOf(cArray, c, fromIndex);
  }

  /**
   * Finds the first instance of c in cArray.
   *
   * @param cArray
   * @param c the char you want to find
   * @return The first instance of c. If not found, it returns -1.
   */
  public static int indexOf(char[] cArray, char c) {
    return String2.indexOf(cArray, c);
  }

  /**
   * This indexOf is a little different: it finds the first instance in s of any char in car.
   *
   * @param s a string
   * @param car the chars you want to find any of
   * @param fromIndex the index number of the position to start the search
   * @return The first instance in s of any char in car. If not found, it returns -1.
   */
  public static int indexOfChar(String s, char[] car, int fromIndex) {
    return String2.indexOfChar(s, car, fromIndex);
  }

  /**
   * This indexOf is a little different: it finds the first instance in s of any char in car.
   *
   * @param s a string
   * @param car the chars you want to find any of
   * @param fromIndex the index number of the position to start the search
   * @return The first instance in s of any char in car. If not found, it returns -1.
   */
  public static int indexOf(String s, String car, int fromIndex) {
    return String2.indexOf(s, car, fromIndex);
  }

  /**
   * Finds the first instance of d at or after fromIndex (0.. ) in dArray (tested with
   * Math2.almostEqual5).
   *
   * @param dArray
   * @param d the double you want to find
   * @param fromIndex the index number of the position to start the search
   * @return The first instance of d. If not found, it returns -1.
   */
  public static int indexOf(double[] dArray, double d, int fromIndex) {
    return String2.indexOf(dArray, d, fromIndex);
  }

  /**
   * This tries to find the first one of the words in the longerString. This is case-sensitive.
   *
   * @return index of the matching word (or -1 if no match or other trouble)
   */
  public static int whichWord(String longerString, String words[]) {
    return String2.whichWord(longerString, words);
  }

  /**
   * Finds the first instance of d in dArray (tested with Math2.almostEqual5).
   *
   * @param dArray
   * @param d the double you want to find
   * @return The first instance of d. If not found, it returns -1.
   */
  public static int indexOf(double[] dArray, double d) {
    return String2.indexOf(dArray, d);
  }

  /**
   * This returns true for A..Z, a..z.
   *
   * @param c a char
   * @return true if c is a letter
   */
  public static final boolean isAsciiLetter(int c) {
    return String2.isAsciiLetter(c);
  }

  /**
   * This includes hiASCII/ISO Latin 1/ISO 8859-1, but not extensive unicode characters. Letters are
   * A..Z, a..z, and #192..#255 (except #215 and #247). For unicode characters, see Java Lang Spec
   * pg 14.
   *
   * @param c a char
   * @return true if c is a letter
   */
  public static final boolean isLetter(int c) {
    return String2.isLetter(c);
  }

  /**
   * First letters for identifiers (e.g., variable names, method names) can be all isLetter()'s plus
   * $ and _.
   *
   * @param c a char
   * @return true if c is a valid character for the first character if a Java ID
   */
  public static final boolean isIDFirstLetter(int c) {
    return String2.isIDFirstLetter(c);
  }

  /**
   * 0..9, a..f, A..F Hex numbers are 0x followed by hexDigits.
   *
   * @param c a char
   * @return true if c is a valid hex digit
   */
  public static final boolean isHexDigit(int c) {
    return String2.isHexDigit(c);
  }

  /** Returns true if all of the characters in s are hex digits. A 0-length string returns false. */
  public static final boolean isHexString(String s) {
    return String2.isHexString(s);
  }

  /**
   * 0..9. Non-Latin numeric characters are not included (see Java Lang Spec pg 14).
   *
   * @param c a char
   * @return true if c is a digit
   */
  public static final boolean isDigit(int c) {
    return String2.isDigit(c);
  }

  /**
   * 0..9. Non-Latin numeric characters are not included (see Java Lang Spec pg 14).
   *
   * @param s a string
   * @return true if c is a digit
   */
  public static final boolean allDigits(String s) {
    return String2.allDigits(s);
  }

  /**
   * Determines if the character is a digit or a letter.
   *
   * @param c a char
   * @return true if c is a letter or a digit
   */
  public static final boolean isDigitLetter(int c) {
    return String2.isDigitLetter(c);
  }

  /**
   * This tries to quickly determine if the string is a correctly formatted number (including
   * decimal, hexadecimal, octal, and "NaN" (any case)).
   *
   * <p>This may not be perfect. In the future, this may be changed to be perfect. That shouldn't
   * affect its use.
   *
   * @param s usually already trimmed, since any space in s will return false.
   * @return true if s is *probably* a number. This returns false if s is *definitely* not a number.
   *     "NaN" (case insensitive) returns true. (It is a numeric value of sorts.) null and "" return
   *     false.
   */
  public static final boolean isNumber(String s) {
    return String2.isNumber(s);
  }

  /**
   * Whitespace characters are u0001 .. ' '. Java just considers a few of these (sp HT FF) as white
   * space, see the Java Lang Specification. u0000 is not whitespace. Some methods count on this
   * fact.
   *
   * @param c a char
   * @return true if c is a whitespace character
   */
  public static final boolean isWhite(int c) {
    return String2.isWhite(c);
  }

  /**
   * This indicates if ch is printable with System.err.println() and Graphics.drawString(); hence,
   * it is a subset of 0..255.
   *
   * <UL>
   *   <LI>This is used, for example, to limit characters entering CoText.
   *   <LI>Currently, this accepts the ch if <tt>(ch&gt;=32 &amp;&amp; ch&lt;127) || (ch&gt;=161
   *       &amp;&amp; ch&lt;=255)</tt>.
   *   <LI>tab(#9) is not included. It should be caught separately and dealt with (expand to
   *       spaces?). The problem is that tabs are printed with a wide box (non-character symbol) in
   *       Windows Courier font. Thus, they mess up the positioning of characters in CoText.
   *   <LI>newline is not included. It should be caught separately and dealt with.
   *   <LI>This requires further study into all standard fonts on all platforms to see if other
   *       characters can be accepted.
   * </UL>
   *
   * @param ch a char
   * @return true if ch is a printable character
   */
  public static final boolean isPrintable(int ch) {
    return String2.isPrintable(ch);
  }

  /** Returns true if all of the characters in s are printable */
  public static final boolean isPrintable(String s) {
    return String2.isPrintable(s);
  }

  /** returns true if ch is 32..126. */
  public static final boolean isAsciiPrintable(int ch) {
    return String2.isAsciiPrintable(ch);
  }

  /** Returns true if all of the characters in s are 32..126 */
  public static final boolean isAsciiPrintable(String s) {
    return String2.isAsciiPrintable(s);
  }

  /**
   * This returns the string with all non-isPrintable characters removed.
   *
   * @param s
   * @return s with all the non-isPrintable characters removed. If s is null, this throws null
   *     pointer exception.
   */
  public static String justPrintable(String s) {
    return String2.justPrintable(s);
  }

  /**
   * This converts the string to plain ascii (0..127). Diacritics are stripped off high ASCII
   * characters. Some high ASCII characters are crudely converted to similar characters (the
   * conversion is always character-for-character, so the string length will be unchanged). Other
   * characters become '?'. The result will be the same length as s.
   *
   * @param s
   * @return the string converted to plain ascii (0..127).
   */
  public static String modifyToBeASCII(String s) {
    return String2.modifyToBeASCII(s);
  }

  /**
   * This indicates if ch is a file-name-safe character (A-Z, a-z, 0-9, _, -, or .).
   *
   * @param ch
   * @return true if ch is a file-name-safe character (A-Z, a-z, 0-9, _, -, .).
   */
  public static boolean isFileNameSafe(char ch) {
    return String2.isFileNameSafe(ch);
  }

  /**
   * This indicates if 'email' is a valid email address.
   *
   * @param email a possible email address
   * @return true if 'email' is a valid email address.
   */
  public static boolean isEmailAddress(String email) {
    return String2.isEmailAddress(email);
  }

  /**
   * This indicates if 'url' is probably a valid url. This is like isRemote, but returns true for
   * "file://...".
   *
   * @param url a possible url
   * @return true if 'url' is probably a valid url. false if 'url' is not a valid url. Note that
   *     "file://..." is a url.
   */
  public static boolean isUrl(String url) {
    return String2.isUrl(url);
  }

  /**
   * This returns true if the dir starts with http://, https://, ftp://, sftp://, or smb://. This is
   * like isRemote, but returns false for "file://...". WARNING: AWS S3 URLs are considered remote
   * here, but often they should be treated as local.
   *
   * @return true if the dir is remote (e.g., a URL other than file://) If dir is null or "", this
   *     returns false.
   */
  public static boolean isRemote(String dir) {
    return String2.isRemote(dir);
  }

  /**
   * This returns true if the dir starts with http://, https://, ftp://, sftp://, or smb://, but not
   * if it's an AWS S3 URL. This is like isRemote, but returns false for "file://...". NOTE: AWS S3
   * URLs are considered local here, but sometimes they should be treated as local.
   *
   * @return true if the dir is remote (but not an AWS S3 URL) (e.g., a URL other than file://) If
   *     dir is null or "", this returns false.
   */
  public static boolean isTrulyRemote(String dir) {
    return String2.isTrulyRemote(dir);
  }

  /**
   * This indicates if s has length &gt;= 1 and has just file-name-safe characters (0-9, A-Z, a-z,
   * _, -, .). Note, this does not check for filenames that are too long (Windows has a
   * path+fileName max length of 255 chars).
   *
   * @param s a string, usually a file name
   * @return true if s has just file-name-safe characters (0-9, A-Z, a-z, _, -, .). It returns false
   *     if s is null or "".
   */
  public static boolean isFileNameSafe(String s) {
    return String2.isFileNameSafe(s);
  }

  /**
   * This returns the string with just file-name-safe characters (0-9, A-Z, a-z, _, -, .). This is
   * different from String2.encodeFileNameSafe -- this emphasizes readability, not avoiding losing
   * information. Non-safe characters are converted to '_'. Adjacent '_' are collapsed into '_'. See
   * posix fully portable file names at https://en.wikipedia.org/wiki/Filename . See javadocs for
   * java.net.URLEncoder, which describes valid characters (but deals with encoding, whereas this
   * method alters or removes). The result may be shorter than s. Note, this does not check for
   * filenames that are too long (Windows has a path+fileName max length of 255 chars).
   *
   * @param s If s is null, this returns "_null". If s is "", this returns "_".
   * @return s with all of the non-fileNameSafe characters removed or changed
   */
  public static String modifyToBeFileNameSafe(String s) {
    return String2.modifyToBeFileNameSafe(s);
  }

  /**
   * This tests if s is a valid variableName:
   *
   * <ul>
   *   <li>first character must be (iso8859Letter|_).
   *   <li>optional subsequent characters must be (iso8859Letter|_|0-9).
   * </ul>
   *
   * Note that Java allows Unicode characters, but this does not.
   *
   * @param s a possible variable name
   * @return true if s is a valid variableName.
   */
  public static boolean isVariableNameSafe(String s) {
    return String2.isVariableNameSafe(s);
  }

  /**
   * This tests if s is a valid jsonp function name. The functionName MUST be a series of 1 or more
   * (period-separated) words. For each word:
   *
   * <ul>
   *   <li>The first character must be (iso8859Letter|_).
   *   <li>The optional subsequent characters must be (iso8859Letter|_|0-9).
   *   <li>s must not be longer than 255 characters.
   * </ul>
   *
   * Note that JavaScript allows Unicode characters, but this does not.
   *
   * @param s a possible jsonp function name
   * @return true if s is a valid jsonp function name.
   */
  public static boolean isJsonpNameSafe(String s) {
    return String2.isJsonpNameSafe(s);
  }

  /**
   * This is like modifyToBeFileNameSafe, but restricts the name to:
   *
   * <ul>
   *   <li>first character must be (iso8859Letter|_).
   *   <li>subsequent characters must be (iso8859Letter|_|0-9).
   * </ul>
   *
   * Note that Java allows Unicode characters, but this does not. See also the safer
   * encodeMatlabNameSafe(String s). Note, this does not check for names that are too long (many
   * system have an 80 or 255 char limit).
   *
   * @param s
   * @return a safe variable name (but perhaps two s's lead to the same result)
   */
  public static String modifyToBeVariableNameSafe(String s) {
    return String2.modifyToBeVariableNameSafe(s);
  }

  /**
   * This counts all occurrences of <tt>findS</tt> in s. if (s == null || findS == null ||
   * findS.length() == 0) return 0;
   *
   * @param s the source string
   * @param findS the string to be searched for
   */
  public static int countAll(String s, String findS) {
    return String2.countAll(s, findS);
  }

  /**
   * This counts all occurrences of <tt>findS</tt> in s. if (s == null || findS == null ||
   * findS.length() == 0) return 0;
   *
   * @param s the source string
   * @param findS the char to be searched for
   */
  public static int countAll(String s, char findS) {
    return String2.countAll(s, findS);
  }

  /**
   * This returns the index of the nth occurrence of <tt>findS</tt> in s. If s == null, or "", or
   * nth &lt;0, or nth occurence not found, return -1. If s.length()==0 or nth==0, return -1;
   *
   * @param s the source string
   * @param findS the char to be searched for
   * @param nth 1+
   * @return This returns the index of the nth occurrence of <tt>findS</tt> in s.
   */
  public static int findNth(String s, char findS, int nth) {
    return String2.findNth(s, findS, nth);
  }

  /**
   * This repeatedly replaces all oldS with newS. e.g., replace "++" with "+" in "++++" will yield
   * "+".
   */
  public static String repeatedlyReplaceAll(
      String s, String oldS, String newS, boolean ignoreCase) {
    return String2.repeatedlyReplaceAll(s, oldS, newS, ignoreCase);
  }

  /**
   * Returns a string where all occurences of <tt>oldS</tt> have been replaced with <tt>newS</tt>.
   * If <tt>oldS</tt> occurs inside <tt>newS</tt>, it won't be replaced recursively (obviously).
   *
   * @param s the main string
   * @param oldS the string to be searched for
   * @param newS the string to replace oldS
   * @return a modified version of s, with newS in place of all the olds.
   * @throws RuntimeException if s is null.
   */
  public static String replaceAll(String s, String oldS, String newS) {
    return String2.replaceAll(s, oldS, newS);
  }

  /**
   * Returns a string where all occurences of <tt>oldS</tt> have been replaced with <tt>newS</tt>.
   * If <tt>oldS</tt> occurs inside <tt>newS</tt>, it won't be replaced recursively (obviously).
   * When finding oldS in s, their case is irrelevant.
   *
   * @param s the main string
   * @param oldS the string to be searched for
   * @param newS the string to replace oldS
   * @return a modified version of s, with newS in place of all the olds. throws RuntimeException if
   *     s is null.
   */
  public static String replaceAllIgnoreCase(String s, String oldS, String newS) {
    return String2.replaceAllIgnoreCase(s, oldS, newS);
  }

  /**
   * Returns a string where all cases of more than one space are replaced by one space. The string
   * is also trim'd to remove leading and trailing spaces. Also, spaces after { or ( and before ) or
   * } will be removed.
   *
   * @param s
   * @return s, but with the spaces combined (or null if s is null)
   */
  public static String combineSpaces(String s) {
    return String2.combineSpaces(s);
  }

  /* A variant that takes and returns a string */
  public static String whitespacesToSpace(String s) {
    return String2.whitespacesToSpace(s);
  }

  /**
   * Returns a string where all occurences of <tt>oldCh</tt> have been replaced with <tt>newCh</tt>.
   * This doesn't throw exceptions if bad values.
   */
  public static String replaceAll(String s, char oldCh, char newCh) {
    return String2.replaceAll(s, oldCh, newCh);
  }

  public static String commonUnicodeToPlainText(String s) {
    return String2.commonUnicodeToPlainText(s);
  }

  /**
   * This adds 0's to the left of the string until there are <tt>nDigits</tt> to the left of the
   * decimal point (or nDigits total if there isn't a decimal point). If the number is too big,
   * nothing is added or taken away.
   *
   * @param number a positive number. This doesn't handle negative numbers.
   * @param nDigits the desired number of digits to the left of the decimal point (or total, if no
   *     decimal point)
   * @return the number, left-padded with 0's so there are nDigits to the left of the decimal point
   */
  public static String zeroPad(String number, int nDigits) {
    return String2.zeroPad(number, nDigits);
  }

  /**
   * The converts a string[] into a JSON array of strings.
   *
   * @param sa
   * @return e.g., ["aa", "bb", "cc"]. If sa is null, this returns null (as a String).
   */
  public static String toJsonArray(String sa[]) {
    return String2.toJsonArray(sa);
  }

  /**
   * This makes a JSON version of a float.
   *
   * @param f
   * @return "null" if not finite. Return an integer if it ends with ".0". Else returns the number
   *     as a string.
   */
  public static String toJson(float f) {
    return String2.toJson(f);
  }

  /**
   * This makes a JSON version of a number.
   *
   * @param d
   * @return "null" if not finite. Return an integer if it ends with ".0". Else returns the number
   *     as a string.
   */
  public static String toJson(double d) {
    return String2.toJson(d);
  }

  /**
   * This makes a JSON version of a string (\\, \f, \n, \r, \t and \" are escaped with a backslash
   * character and double quotes are added before and after). null is returned as null. This variant
   * encodes char #127 and above.
   *
   * @param s
   * @return the JSON-encoded string surrounded by "'s.
   */
  public static String toJson(String s) {
    return String2.toJson(s);
  }

  /**
   * This variant doesn't encode high characters.
   *
   * @param s
   * @return the JSON-encoded string surrounded by "'s.
   */
  public static String toJson65536(String s) {
    return String2.toJson65536(s);
  }

  /**
   * This makes a JSON version of a string (\\, \f, \n, \r, \t and \" are escaped with a backslash
   * character and double quotes are added before and after). null is returned as null. This variant
   * encodes char #127 and above as \\uhhhh.
   *
   * @param s The String to be encoded.
   * @param firstUEncodedChar The first char to be \\uhhhh encoded, commonly 127, 256, or 65536.
   * @return the JSON-encoded string surrounded by "'s.
   */
  public static String toJson(String s, int firstUEncodedChar) {
    return String2.toJson(s, firstUEncodedChar);
  }

  /**
   * This is a variant of toJson that lets you encode newlines or not.
   *
   * @param s The String to be encoded.
   * @param firstUEncodedChar The first char to be \\uhhhh encoded, commonly 127, 256, or 65536.
   * @return the JSON-encoded string surrounded by "'s.
   */
  public static String toJson(String s, int firstUEncodedChar, boolean encodeNewline) {
    return String2.toJson(s, firstUEncodedChar, encodeNewline);
  }

  /** This encodes one char to the Json encoding. */
  public static String charToJsonString(char ch, int firstUEncodedChar, boolean encodeNewline) {
    return String2.charToJsonString(ch, firstUEncodedChar, encodeNewline);
  }

  /** This is like the other fromJson, but returns "" instead of null. */
  public static String fromJsonNotNull(String s) {
    return String2.fromJsonNotNull(s);
  }

  /** If the String is surrounded by ", this returns fromJson(s), else it returns s. */
  public static String ifJsonFromJson(String s) {
    return String2.ifJsonFromJson(s);
  }

  /**
   * This returns the unJSON version of a JSON string (surrounding "'s (if any) are removed and \\,
   * \f, \n, \r, \t, \/, and \" are unescaped). This is very liberal in what it accepts, including
   * all common C escaped characters:
   * http://msdn.microsoft.com/en-us/library/h21280bw%28v=vs.80%29.aspx "null" returns the String
   * "null". null returns null. This won't throw an exception.
   *
   * @param s it may be enclosed by "'s, or not.
   * @return the decoded string
   */
  public static String fromJson(String s) {
    return String2.fromJson(s);
  }

  /**
   * This converts an NCCSV encoded char to a true char (surrounding "'s and ''s (if any) are
   * removed and \\, \f, \n, \r, \t, \/, and \" are unescaped). This is very liberal in what it
   * accepts, including all common C escaped characters:
   * http://msdn.microsoft.com/en-us/library/h21280bw%28v=vs.80%29.aspx
   *
   * @param s it may be enclosed by "'s and ''s, or not.
   * @return the decoded char (or '?' if trouble) as a 1-char string.
   */
  public static char fromNccsvChar(String s) {
    return String2.fromNccsvChar(s);
  }

  /**
   * This converts an NCCSV string to a true string (surrounding "'s (if any) are removed and \\,
   * \f, \n, \r, \t, \/, and \" are unescaped). This is very liberal in what it accepts, including
   * all common C escaped characters:
   * http://msdn.microsoft.com/en-us/library/h21280bw%28v=vs.80%29.aspx This won't throw an
   * exception.
   *
   * @param s it may be enclosed by "'s, or not.
   * @return the decoded string
   */
  public static String fromNccsvString(String s) {
    return String2.fromNccsvString(s);
  }

  /** This encodes one char for an NCCSV char or String, without surrounding quotes. */
  public static String toNccsvChar(char ch) {
    return String2.toNccsvChar(ch);
  }

  /**
   * This encodes one String as an NCCSV data String, with surrounding double quotes only if
   * necessary.
   */
  public static String toNccsvDataString(String s) {
    return String2.toNccsvDataString(s);
  }

  /**
   * This encodes one String as an NCCSV att String, with surrounding double quotes only if
   * necessary.
   */
  public static String toNccsvAttString(String s) {
    return String2.toNccsvAttString(s);
  }

  /**
   * This replaces "{0}", "{1}", and "{2}" in msg with s0, s1, s2.
   *
   * @param msg a string which may contain "{0}", "{1}", and/or "{2}".
   * @param s0 the first substitution string. If null, that substitution won't be attempted.
   * @param s1 the second substitution string. If null, that substitution won't be attempted.
   * @param s2 the third substitution string. If null, that substitution won't be attempted.
   * @return the modified msg
   */
  public static String substitute(String msg, String s0, String s1, String s2) {
    return String2.substitute(msg, s0, s1, s2);
  }

  /** This returns a CSV (not CSSV) String. */
  public static String toCSVString(Object ar[]) {
    return String2.toCSVString(ar);
  }

  /**
   * Generates a Comma-Space-Separated-Value (CSSV) string.
   *
   * <p>CHANGED: before 2011-03-06, this didn't do anything special for strings with internal commas
   * or quotes. Now it uses toJson for that string.
   *
   * <p>CHANGED: before 2011-09-04, this was called toCSVString.
   *
   * @param ar an array of objects
   * @return a CSSV String with the values with ", " after all but the last value. Returns null if
   *     ar is null. null elements are represented as "[null]".
   */
  public static String toCSSVString(Object ar[]) {
    return String2.toCSSVString(ar);
  }

  /**
   * Generates a space-separated-value string.
   *
   * <p>WARNING: This is simplistic. It doesn't do anything special for strings with internal
   * spaces.
   *
   * @param ar an array of objects (for an ArrayList or Vector, use o.toArray())
   * @return a SSV String with the values with " " after all but the last value. Returns null if ar
   *     is null. null elements are represented as "[null]".
   */
  public static String toSSVString(Object ar[]) {
    return String2.toSSVString(ar);
  }

  /**
   * Generates a newline-separated string, with a newline at the end.
   *
   * <p>WARNING: This is simplistic. It doesn't do anything special for strings with internal
   * newlines.
   *
   * @param ar an array of objects (for an ArrayList or Vector, use o.toArray())
   * @return a String with the values, with a '\n' after each value, even the last. Returns null if
   *     ar is null. null elements are represented as "[null]".
   */
  public static String toNewlineString(Object ar[]) {
    return String2.toNewlineString(ar);
  }

  /**
   * This is used at a low level to generate a 'separator'-separated-value string (without newlines)
   * with the element.toString()'s from the array.
   *
   * @param ar an array of objects (for an ArrayList or Vector, use o.toArray())
   * @param separator the separator string
   * @param finalSeparator if true, a separator will be added to the end of the resulting string (if
   *     it isn't "").
   * @return a separator-separated-value String. Returns null if ar is null. null elements are
   *     represented as "[null]".
   */
  public static String toSVString(Object ar[], String separator, boolean finalSeparator) {
    return String2.toSVString(ar, separator, finalSeparator);
  }

  /**
   * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
   *
   * <p>CHANGED: before 2011-09-04, this was called toCSVString.
   *
   * @param ar an array of boolean
   * @return a CSSV String (or null if ar is null)
   */
  public static String toCSSVString(boolean ar[]) {
    return String2.toCSSVString(ar);
  }

  /**
   * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
   *
   * <p>CHANGED: before 2011-09-04, this was called toCSVString.
   *
   * @param ar an array of bytes
   * @return a CSSV String (or null if ar is null)
   */
  public static String toCSSVString(byte ar[]) {
    return String2.toCSSVString(ar);
  }

  /**
   * This generates a Comma-Space-Separated-Value (CSSV) String from the array. (chars are treated
   * as unsigned shorts).
   *
   * <p>CHANGED: before 2011-09-04, this was called toCSVString.
   *
   * @param ar an array of char
   * @return a CSSV String (or null if ar is null)
   */
  public static String toCSSVString(char ar[]) {
    return String2.toCSSVString(ar);
  }

  /**
   * This returns the int formatted as a 0x hex String with at least nHexDigits, e.g., 0x00FF00.
   * Negative numbers are twos compliment, e.g., -4 -&gt; 0xfffffffc.
   */
  public static String to0xHexString(int i, int nHexDigits) {
    return String2.to0xHexString(i, nHexDigits);
  }

  /**
   * This generates a hexadecimal Comma-Space-Separated-Value (CSSV) String from the array. Negative
   * numbers are twos compliment, e.g., -4 -&gt; 0xfc.
   *
   * <p>CHANGED: before 2011-09-04, this was called toHexCSVString.
   *
   * @param ar an array of bytes
   * @return a CSSV String (or null if ar is null)
   */
  public static String toHexCSSVString(byte ar[]) {
    return String2.toHexCSSVString(ar);
  }

  /**
   * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
   *
   * <p>CHANGED: before 2011-09-04, this was called toCSVString.
   *
   * @param ar an array of shorts
   * @return a CSSV String (or null if ar is null)
   */
  public static String toCSSVString(short ar[]) {
    return String2.toCSSVString(ar);
  }

  /**
   * This generates a hexadecimal Comma-Space-Separated-Value (CSSV) String from the array. Negative
   * numbers are twos compliment, e.g., -4 -&gt; 0xfffc.
   *
   * <p>CHANGED: before 2011-09-04, this was called toHexCSVString.
   *
   * @param ar an array of short
   * @return a CSSV String (or null if ar is null)
   */
  public static String toHexCSSVString(short ar[]) {
    return String2.toHexCSSVString(ar);
  }

  /**
   * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
   *
   * <p>CHANGED: before 2011-09-04, this was called toCSVString.
   *
   * @param ar an array of ints
   * @return a CSSV String (or null if ar is null)
   */
  public static String toCSSVString(int ar[]) {
    return String2.toCSSVString(ar);
  }

  /**
   * This generates a hexadecimal Comma-Space-Separated-Value (CSSV) String from the array. Negative
   * numbers are twos compliment, e.g., -4 -&gt; 0xfffffffc.
   *
   * <p>CHANGED: before 2011-09-04, this was called toHexCSVString.
   *
   * @param ar an array of ints
   * @return a CSSV String (or null if ar is null)
   */
  public static String toHexCSSVString(int ar[]) {
    return String2.toHexCSSVString(ar);
  }

  /**
   * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
   *
   * <p>CHANGED: before 2011-09-04, this was called toCSVString.
   *
   * @param ar an array of longs
   * @return a CSSV String (or null if ar is null)
   */
  public static String toCSSVString(long ar[]) {
    return String2.toCSSVString(ar);
  }

  /**
   * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
   *
   * <p>CHANGED: before 2011-09-04, this was called toCSVString.
   *
   * @param ar an array of float
   * @return a CSSV String (or null if ar is null)
   */
  public static String toCSSVString(float ar[]) {
    return String2.toCSSVString(ar);
  }

  /**
   * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
   *
   * <p>CHANGED: before 2011-09-04, this was called toCSVString.
   *
   * @param ar an array of double
   * @return a CSSV String (or null if ar is null)
   */
  public static String toCSSVString(double ar[]) {
    return String2.toCSSVString(ar);
  }

  /**
   * This generates a newline-separated (always '\n') String from the array.
   *
   * @param ar an array of ints
   * @return a newline-separated String (or null if ar is null)
   */
  public static String toNewlineString(int ar[]) {
    return String2.toNewlineString(ar);
  }

  /**
   * This generates a newline-separated (always '\n') String from the array.
   *
   * @param ar an array of double
   * @return a newline-separated String (or null if ar is null)
   */
  public static String toNewlineString(double ar[]) {
    return String2.toNewlineString(ar);
  }

  /**
   * This converts an Object[] into a String[]. If you have an ArrayList or a Vector, use
   * arrayList.toArray().
   *
   * @param aa
   * @return the corresponding String[] by calling toString() for each object
   */
  public static String[] toStringArray(Object aa[]) {
    return String2.toStringArray(aa);
  }

  /**
   * This returns a nice String representation of the attribute value (which should be a String or
   * an array of primitives).
   *
   * <p>CHANGED: before 2011-09-04, this was called toCSVString.
   *
   * @param value
   * @return a nice String representation
   */
  public static String arrayToCSSVString(Object value) {
    return String2.arrayToCSSVString(value);
  }

  /**
   * This extracts the lower 8 bits of each char to form a byte array.
   *
   * @param s a String
   * @return the corresponding byte[] (or null if s is null)
   */
  public static byte[] toByteArray(String s) {
    return String2.toByteArray(s);
  }

  /**
   * This finds the first element in Object[] (starting at element startAt) where ar[i]==o.
   *
   * @param ar the array of Objects
   * @param o the String to be found
   * @param startAt the first element of ar to be checked. If startAt &lt; 0, this starts with
   *     startAt = 0. If startAt &gt;= ar.length, this returns -1.
   * @return the element number of ar which is equal to s (or -1 if ar is null, or s is null or not
   *     found)
   */
  public static int indexOfObject(Object[] ar, Object o, int startAt) {
    return String2.indexOfObject(ar, o, startAt);
  }

  /** A variant of indexOfObject() that uses startAt=0. */
  public static int indexOfObject(Object[] ar, Object o) {
    return String2.indexOfObject(ar, o);
  }

  /**
   * This finds the first element in Object[] where the ar[i].toString value equals to s.
   *
   * @param ar the array of Objects (Strings?)
   * @param s the String to be found
   * @return the element number of ar which is equal to s (or -1 if ar is null, or s is null or not
   *     found)
   */
  public static int indexOf(Object[] ar, String s) {
    return String2.indexOf(ar, s);
  }

  /**
   * This finds the first element in Object[] (starting at element startAt) where the ar[i].toString
   * value equals s.
   *
   * @param ar the array of Objects
   * @param s the String to be found
   * @param startAt the first element of ar to be checked. If startAt &lt; 0, this starts with
   *     startAt = 0. If startAt &gt;= ar.length, this returns -1.
   * @return the element number of ar which is equal to s (or -1 if ar is null, or s is null or not
   *     found)
   */
  public static int indexOf(Object[] ar, String s, int startAt) {
    return String2.indexOf(ar, s, startAt);
  }

  /**
   * This finds the first element in Object[] where ar[i].toString().toLowerCase() equals to
   * s.toLowerCase(). This could have been called indexOfIgnoreCase().
   *
   * @param ar the array of Objects
   * @param s the String to be found
   * @return the element number of ar which is equal to s (or -1 if s is null or not found)
   */
  public static int caseInsensitiveIndexOf(Object[] ar, String s) {
    return String2.caseInsensitiveIndexOf(ar, s);
  }

  /**
   * This finds the first element in Object[] where the ar[i].toString value contains the substring
   * s.
   *
   * @param ar the array of objects
   * @param s the String to be found
   * @return the element number of ar which is equal to s (or -1 if not found)
   */
  public static int lineContaining(Object[] ar, String s) {
    return String2.lineContaining(ar, s);
  }

  /**
   * This finds the first element in Object[] (starting at element startAt) where the ar[i].toString
   * value contains the substring s.
   *
   * @param ar the array of objects
   * @param s the String to be found
   * @param startAt the first element of ar to be checked. If startAt &lt; 0, this starts with
   *     startAt = 0.
   * @return the element number of ar which is equal to s (or -1 if not found)
   */
  public static int lineContaining(Object[] ar, String s, int startAt) {
    return String2.lineContaining(ar, s, startAt);
  }

  /**
   * This finds the first element in Object[] (starting at element startAt) where the ar[i].toString
   * value contains the substring s (ignoring the case of ar and s).
   *
   * @param ar the array of objects
   * @param s the String to be found
   * @param startAt the first element of ar to be checked. If startAt &lt; 0, this starts with
   *     startAt = 0.
   * @return the element number of ar which is equal to s (or -1 if not found)
   */
  public static int lineContainingIgnoreCase(Object[] ar, String s, int startAt) {
    return String2.lineContainingIgnoreCase(ar, s, startAt);
  }

  /**
   * This returns the first element in Object[] (starting at element 0) where the ar[i].toString
   * value starts with s.
   *
   * @param ar the array of objects
   * @param s the String to be found
   * @return the first element ar (as a String) which starts with s (or null if not found)
   */
  public static String stringStartsWith(Object[] ar, String s) {
    return String2.stringStartsWith(ar, s);
  }

  /**
   * This finds the first element in Object[] (starting at element 0) where the ar[i].toString value
   * starts with s.
   *
   * @param ar the array of objects
   * @param s the String to be found
   * @return the element number of ar which starts with s (or -1 if not found)
   */
  public static int lineStartsWith(Object[] ar, String s) {
    return String2.lineStartsWith(ar, s);
  }

  /**
   * This finds the first element in Object[] (starting at element startAt) where the ar[i].toString
   * value starts with s.
   *
   * @param ar the array of objects
   * @param s the String to be found
   * @param startAt the first element of ar to be checked. If startAt &lt; 0, this starts with
   *     startAt = 0.
   * @return the element number of ar which starts with s (or -1 if not found)
   */
  public static int lineStartsWith(Object[] ar, String s, int startAt) {
    return String2.lineStartsWith(ar, s, startAt);
  }

  /**
   * This variant of lineStartsWith startsAt index=0.
   *
   * @param ar the array of objects, e.g., including LATITUDE
   * @param s the String to be found, e.g., Lat
   * @return the element number of ar which starts with s (or -1 if not found)
   */
  public static int lineStartsWithIgnoreCase(Object[] ar, String s) {
    return String2.lineStartsWithIgnoreCase(ar, s);
  }

  /**
   * This is like lineStartsWith, but ignores case.
   *
   * @param ar the array of objects, e.g., including LATITUDE
   * @param s the String to be found, e.g., Lat
   * @param startAt the first element of ar to be checked. If startAt &lt; 0, this starts with
   *     startAt = 0.
   * @return the element number of ar which starts with s (or -1 if not found)
   */
  public static int lineStartsWithIgnoreCase(Object[] ar, String s, int startAt) {
    return String2.lineStartsWithIgnoreCase(ar, s, startAt);
  }

  /**
   * This finds the first element in prefixes (starting at element startAt) where the longerString
   * starts with prefixes[i].
   *
   * @param prefixes the array of prefixes
   * @param longerString the String that might start with one of the prefixes
   * @param startAt the first element of ar to be checked. If startAt &lt; 0, this starts with
   *     startAt = 0.
   * @return the element number of prefixes which longerString starts with (or -1 if not found)
   */
  public static int whichPrefix(String[] prefixes, String longerString, int startAt) {
    return String2.whichPrefix(prefixes, longerString, startAt);
  }

  /**
   * This is like whichPrefix, but returns the found prefix (or null).
   *
   * @param prefixes the array of prefixes
   * @param longerString the String that might start with one of the prefixes
   * @param startAt the first element of ar to be checked. If startAt &lt; 0, this starts with
   *     startAt = 0.
   * @return the prefixes[i] which longerString starts with (or null if not found)
   */
  public static String findPrefix(String[] prefixes, String longerString, int startAt) {
    return String2.findPrefix(prefixes, longerString, startAt);
  }

  /**
   * This finds the first element in suffixes (starting at element startAt) where the longerString
   * ends with suffixes[i].
   *
   * @param suffixes the array of suffixes
   * @param longerString the String that might end with one of the suffixes
   * @param startAt the first element of ar to be checked. If startAt &lt; 0, this starts with
   *     startAt = 0.
   * @return the element number of suffixes which longerString ends with (or -1 if not found)
   */
  public static int whichSuffix(String[] suffixes, String longerString, int startAt) {
    return String2.whichSuffix(suffixes, longerString, startAt);
  }

  /**
   * This splits the string at the specified character. The substrings are trim'd. A missing final
   * string is treated as "" (not discarded as with String.split).
   *
   * @param s a string with 0 or more separator chatacters
   * @param separator
   * @return a String[] with the strings (not canonical). s=null returns null. s="" returns
   *     String[1]{""}.
   */
  public static String[] split(String s, char separator) {
    return String2.split(s, separator);
  }

  /**
   * This splits the string at the specified character. A missing final string is treated as "" (not
   * discarded as with String.split).
   *
   * @param s a string with 0 or more separator chatacters
   * @param separator
   * @return a String[] with the strings (not canonical). s=null returns null. s="" returns
   *     String[1]{""}.
   */
  public static String[] splitNoTrim(String s, char separator) {
    return String2.splitNoTrim(s, separator);
  }

  /**
   * This converts an Object[] (for example, where objects are Strings or Integers) into an int[].
   *
   * @param oar an Object[]
   * @return the corresponding int[] (invalid values are converted to Integer.MAX_VALUE). oar=null
   *     returns null.
   */
  public static int[] toIntArray(Object oar[]) {
    return String2.toIntArray(oar);
  }

  /**
   * This converts an Object[] (for example, where objects are Strings or Floats) into a float[].
   *
   * @param oar an Object[]
   * @return the corresponding float[] (invalid values are converted to Float.NaN). oar=null returns
   *     null.
   */
  public static float[] toFloatArray(Object oar[]) {
    return String2.toFloatArray(oar);
  }

  /**
   * This converts an Object[] (for example, where objects are Strings or Doubles) into a double[].
   *
   * @param oar an Object[]
   * @return the corresponding double[] (invalid values are converted to Double.NaN). oar=null
   *     returns null.
   */
  public static double[] toDoubleArray(Object oar[]) {
    return String2.toDoubleArray(oar);
  }

  /**
   * This returns an int[] with just the non-Integer.MAX_VALUE values from the original array.
   *
   * @param iar is an int[]
   * @return a new int[] with just the non-Integer.MAX_VALUE values. iar=null returns null.
   */
  public static int[] justFiniteValues(int iar[]) {
    return String2.justFiniteValues(iar);
  }

  /**
   * This returns a double[] with just the finite values from the original array.
   *
   * @param dar is a double[]
   * @return a new double[] with just finite values. dar=null returns null.
   */
  public static double[] justFiniteValues(double dar[]) {
    return String2.justFiniteValues(dar);
  }

  /**
   * This returns a String[] with just non-null strings from the original array.
   *
   * @param sar is a String[]
   * @return a new String[] with just non-null strings. sar=null returns null.
   */
  public static String[] removeNull(String sar[]) {
    return String2.removeNull(sar);
  }

  /**
   * This returns a String[] with just non-null and non-"" strings from the original array.
   *
   * @param sar is a String[]
   * @return a new String[] with just non-null and non-"" strings. sar=null returns null.
   */
  public static String[] removeNullOrEmpty(String sar[]) {
    return String2.removeNullOrEmpty(sar);
  }

  /**
   * This converts a comma-separated-value String into an int[]. Invalid values are converted to
   * Integer.MAX_VALUE.
   *
   * @param csv the comma-separated-value String.
   * @return the corresponding int[]. csv=null returns null. csv="" is converted to
   *     int[1]{Integer.MAX_VALUE}.
   */
  public static int[] csvToIntArray(String csv) {
    return String2.csvToIntArray(csv);
  }

  /**
   * This converts a comma-separated-value String into a double[]. Invalid values are converted to
   * Double.NaN.
   *
   * @param csv the comma-separated-value String
   * @return the corresponding double[]. csv=null returns null. csv="" is converted to
   *     double[1]{Double.NAN}.
   */
  public static double[] csvToDoubleArray(String csv) {
    return String2.csvToDoubleArray(csv);
  }

  /**
   * This converts a string to a boolean.
   *
   * @param s the string
   * @return false if s is "false", "f", or "0". Case and leading/trailing spaces don't matter. All
   *     other values (and null) are treated as true.
   */
  public static boolean parseBoolean(String s) {
    return String2.parseBoolean(s);
  }

  /**
   * This converts a string to a boolean and then a Int.
   *
   * @param s the string
   * @return Integer.MAX_VALUE (i.e., missing value) if s is null or s is "". Return 0 if s is
   *     "false", "f", or "0". Return 1 if for all other values. Case and leading/trailing spaces
   *     don't matter.
   */
  public static int parseBooleanToInt(String s) {
    return String2.parseBooleanToInt(s);
  }

  /**
   * This removes leading ch's.
   *
   * @param s
   * @param ch
   * @return s or a new string without leading ch's. null returns null.
   */
  public static String removeLeading(String s, char ch) {
    return String2.removeLeading(s, ch);
  }

  /** Like parseInt(s), but returns def if error). */
  public static int parseInt(String s, int def) {
    return String2.parseInt(s, def);
  }

  /**
   * Convert a string to an int. Leading or trailing spaces are automatically removed. This accepts
   * hexadecimal integers starting with "0x". Leading 0's (e.g., 0012) are ignored; number is
   * treated as decimal (not octal as Java would). Floating point numbers are rounded. This won't
   * throw an exception if the number isn't formatted right. To make a string from an int, use ""+i,
   * Integer.toHexString, or Integer.toString(i,radix).
   *
   * @param s is the String representation of a number.
   * @return the int value from the String (or Integer.MAX_VALUE if error).
   */
  public static int parseInt(String s) {
    return String2.parseInt(s);
  }

  /**
   * Convert a string to a double. Leading or trailing spaces are automatically removed. This
   * accepts hexadecimal integers starting with "0x". Whole number starting with '0' (e.g., 012) is
   * treated as decimal (not octal as Java would). This won't throw an exception if the number isn't
   * formatted right.
   *
   * @param s is the String representation of a number.
   * @return the double value from the String (a finite value, Double.POSITIVE_INFINITY,
   *     Double.NEGATIVE_INFINITY, or Double.NaN if error).
   */
  public static double parseDouble(String s) {
    return String2.parseDouble(s);
  }

  /**
   * Convert a string to an int, with rounding. Leading or trailing spaces are automatically
   * removed. This won't throw an exception if the number isn't formatted right.
   *
   * @param s is the String representation of a number.
   * @return the int value from the String (or Double.NaN if error).
   */
  public static double roundingParseInt(String s) {
    return String2.roundingParseInt(s);
  }

  /**
   * This converts String representation of a long. Leading or trailing spaces are automatically
   * removed. THIS DOESN'T ROUND! So floating point values lead to Long.MAX_VALUE.
   *
   * @param s a valid String representation of a long value
   * @return a long (or Long.MAX_VALUE if trouble).
   */
  public static long parseLong(String s) {
    return String2.parseLong(s);
  }

  /**
   * Parse as a float with either "." or "," as the decimal point. Leading or trailing spaces are
   * automatically removed.
   *
   * @param s a String representing a float value (e.g., 1234.5 or 1234,5 or 1.234e3 1,234e3)
   * @return the corresponding float (or Float.NaN if not properly formatted)
   */
  public static float parseFloat(String s) {
    return String2.parseFloat(s);
  }

  /**
   * This converts a multiple-space-separated string into a String[] of separate tokens. Double
   * quoted tokens may have internal spaces.
   *
   * @param s the space-separated string
   * @return String[] of tokens (or null if s is null)
   */
  public static String[] tokenize(String s) {
    return String2.tokenize(s);
  }

  /**
   * If lines in s are &gt;=maxLength characters, this inserts "\n"+spaces at the previous
   * non-DigitLetter + DigitLetter; or if none, this inserts "\n"+spaces at maxLength. Useful
   * keywords for searching for this method: longer, longest, noLongerThan.
   *
   * @param s a String with multiple lines, separated by \n's
   * @param maxLength the maximum line length allowed
   * @param spaces the string to be inserted after the inserted newline, e.g., "&lt;br&gt; "
   * @return s (perhaps the same, perhaps different), but with no long lines
   */
  public static String noLongLines(String s, int maxLength, String spaces) {
    return String2.noLongLines(s, maxLength, spaces);
  }

  /**
   * This is like noLongLines, but will only break at spaces.
   *
   * @param s a String with multiple lines, separated by \n's
   * @param maxLength the maximum line length allowed
   * @param spaces the string to be inserted after the inserted newline, e.g., " "
   * @return the content of s, but with no long lines
   */
  public static String noLongLinesAtSpace(String s, int maxLength, String spaces) {
    return String2.noLongLinesAtSpace(s, maxLength, spaces);
  }

  /**
   * This returns the number formatted with up to 6 digits to the left and right of the decimal and
   * trailing decimal 0's removed. If abs(d) &lt; 0.0999995 or abs(d) &gt;= 999999.9999995, the
   * number is displayed in scientific notation (e.g., 8.954321E-5). Thus the maximum length should
   * be 14 characters (-123456.123456). 0 returns "0" NaN returns "NaN". Double.POSITIVE_INFINITY
   * returns "Infinity". Double.NEGATIVE_INFINITY returns "-Infinity".
   *
   * @param d a number
   * @return the number converted to a string
   */
  public static String genEFormat6(double d) {
    return String2.genEFormat6(d);
  }

  /**
   * This returns the number formatted with up to 10 digits to the left and right of the decimal and
   * trailing decimal 0's removed. If abs(d) &lt; 0.09999999995 or abs(d) &gt;= 999999.99999999995,
   * the number is displayed in scientific notation (e.g., 8.9544680321E-5). Thus the maximum length
   * should be 18 characters (-123456.1234567898). 0 returns "0" NaN returns "NaN".
   * Double.POSITIVE_INFINITY returns "Infinity". Double.NEGATIVE_INFINITY returns "-Infinity".
   *
   * @param d a number
   * @return the number converted to a string
   */
  public static String genEFormat10(double d) {
    return String2.genEFormat10(d);
  }

  /**
   * This is like genEFormat6, but the scientific notation format is, e.g., 8.954321x10^-5.
   *
   * @param d a number
   * @return the number converted to a string
   */
  public static String genX10Format6(double d) {
    return String2.genX10Format6(d);
  }

  /**
   * This is like genEFormat10, but the scientific notation format is, e.g., 8.9509484321x10^-5.
   *
   * @param d a number
   * @return the number converted to a string
   */
  public static String genX10Format10(double d) {
    return String2.genX10Format10(d);
  }

  /**
   * This is like genEFormat6, but the scientific notation format is, e.g.,
   * 8.954321x10<sup>-5</sup>.
   *
   * @param d a number
   * @return the number converted to a string
   */
  public static String genHTMLFormat6(double d) {
    return String2.genHTMLFormat6(d);
  }

  /**
   * This is like genEFormat10, but the scientific notation format is, e.g.,
   * 8.9509244321x10<sup>-5</sup>.
   *
   * @param d a number
   * @return the number converted to a string
   */
  public static String genHTMLFormat10(double d) {
    return String2.genHTMLFormat10(d);
  }

  /**
   * This trims just the start of the string.
   *
   * @param s
   * @return s with just the start of the string trim'd. If s == null, this returns null.
   */
  public static String trimStart(String s) {
    return String2.trimStart(s);
  }

  /**
   * This trims just the end of the string.
   *
   * @param s
   * @return s with just the end of the string trim'd. If s == null, this returns null.
   */
  public static String trimEnd(String s) {
    return String2.trimEnd(s);
  }

  /**
   * Find the last element which is &lt;= s in an ascending sorted array.
   *
   * @param sar an ascending sorted String[] which may have duplicate values
   * @param s
   * @return the index of the last element which is &lt;= s in an ascending sorted array. If s is
   *     null or s &lt; the smallest element, this returns -1 (no element is appropriate). If s &gt;
   *     the largest element, this returns sar.length-1.
   */
  public static int binaryFindLastLE(String[] sar, String s) {
    return String2.binaryFindLastLE(sar, s);
  }

  /**
   * Find the first element which is &gt;= s in an ascending sorted array.
   *
   * @param sar an ascending sorted String[] which currently may not have duplicate values
   * @param s
   * @return the index of the first element which is &gt;= s in an ascending sorted array. If s &lt;
   *     the smallest element, this returns 0. If s is null or s &gt; the largest element, this
   *     returns sar.length (no element is appropriate).
   */
  public static int binaryFindFirstGE(String[] sar, String s) {
    return String2.binaryFindFirstGE(sar, s);
  }

  /**
   * Find the closest element to s in an ascending sorted array.
   *
   * @param sar an ascending sorted String[]. It the array has duplicates and s equals one of them,
   *     it isn't specified which duplicate's index will be returned.
   * @param s
   * @return the index of the element closest to s. If s is null, this returns -1.
   */
  public static int binaryFindClosest(String[] sar, String s) {
    return String2.binaryFindClosest(sar, s);
  }

  /**
   * This returns the index of the first non-Unicode character. Currently, valid characters are #32
   * - #126, #160+.
   *
   * @param s
   * @param alsoOK a string with characters (e.g., \r, \n, \t) which are also valid
   * @return the index of the first non-utf-8 character, or -1 if all valid.
   */
  public static int findInvalidUnicode(String s, String alsoOK) {
    return String2.findInvalidUnicode(s, alsoOK);
  }

  /**
   * This makes s valid Unicode by converting invalid characters (e.g., #128) with \\uhhhh
   * (literally 2 backslashes, so no info is lost). The invalid characters are often Windows charset
   * characters #127 - 159.
   *
   * @param s
   * @param alsoOK a string with characters (e.g., \r, \n, \t) which are also valid
   * @return the valid Unicode string.
   */
  public static String makeValidUnicode(String s, String alsoOK) {
    return String2.makeValidUnicode(s, alsoOK);
  }

  /**
   * This converts the char to an ISO-8859-1 (ISO_8859_1) char. This converts any char in 127-159
   * and &gt;255 into '?'.
   *
   * @param ch the char to be converted
   * @return an ISO_8859_1-only char.
   */
  public static char toIso88591Char(char ch) {
    return String2.toIso88591Char(ch);
  }

  /**
   * This converts the chars to ISO-8859-1 (ISO_8859_1) chars. This converts any char in 127-159 and
   * &gt;255 into '?'.
   *
   * @param car[] the char[] to be converted
   * @return car for convenience.
   */
  public static char[] toIso88591Chars(char car[]) {
    return String2.toIso88591Chars(car);
  }

  /**
   * This returns the UTF-8 encoding of the string (or null if trouble). The inverse of this is
   * utf8BytesToString. This won't throw an exception and returns ERROR (as bytes) if trouble.
   *
   * @return the byte[]. null in returns null. length=0 returns BAR_ZERO.
   */
  public static byte[] stringToUtf8Bytes(String s) {
    return String2.stringToUtf8Bytes(s);
  }

  /**
   * This returns a string from the UTF-8 encoded byte[] (or ERROR if trouble). The inverse of this
   * is stringToUtf8Bytes. This won't throw an exception unless bar is invalid and returns ERROR if
   * trouble. null in returns null out.
   */
  public static String utf8BytesToString(byte[] bar) {
    return String2.utf8BytesToString(bar);
  }

  /**
   * This returns the MD5 hash digest of stringToUtf8Bytes(password) as a String of 32 lowercase hex
   * digits. Lowercase because the digest authentication standard uses lower case; so mimic them.
   * And lowercase is easier to type.
   *
   * @param password the text to be digested
   * @return the MD5 hash digest of the password (32 lowercase hex digits, as a String), or null if
   *     password is null or there is trouble.
   */
  public static String md5Hex(String password) {
    return String2.md5Hex(password);
  }

  /**
   * This returns the hash digest of stringToUtf8Bytes(password) as a String of lowercase hex
   * digits. Lowercase because the digest authentication standard uses lower case; so mimic them.
   * And lowercase is easier to type.
   *
   * @param algorithm one of the FILE_DIGEST_OPTIONS {"MD5", "SHA-1", "SHA-256", "SHA-384",
   *     "SHA-512" };
   * @param password the text to be digested
   * @return the algorithm's hash digest of the password (many lowercase hex digits, as a String),
   *     or null if password is null or there is trouble.
   */
  public static String passwordDigest(String algorithm, String password) {
    return String2.passwordDigest(algorithm, password);
  }

  /**
   * This returns the last 12 hex digits from md5Hex (or null if md5 is null), broken into 3 blocks
   * of 4 digits, separated by '_'. I use this as a short, easy to type, repeatable, representation
   * of long strings (e.g., an ERDDAP query URL), sort of like the idea of tinyURL. It performs much
   * better than hashcode or CRC32 when a large number of passwords (or filenames) are encoded and
   * you don't want any collisions. See Projects.testHashFunctions.
   */
  public static String md5Hex12(String password) {
    return String2.md5Hex12(password);
  }

  /**
   * Given two strings with internal newlines, oldS and newS, this a message indicating where they
   * differ.
   *
   * @param oldS
   * @param newS
   * @return a message indicating where they differ, or "" if there is no difference.
   */
  public static String differentLine(String oldS, String newS) {
    return String2.differentLine(oldS, newS);
  }

  /**
   * This converts a double to a rational number (m * 10^t). This is similar to Math2.mantissa and
   * Math2.intExponent, but works via string manipulation to avoid roundoff problems (e.g., with
   * 6.6260755e-24).
   *
   * @param d
   * @return int[2]: [0]=m, [1]=t. (or {0, 0} if d=0, or {1, Integer.MAX_VALUE} if !finite(d))
   */
  public static int[] toRational(double d) {
    return String2.toRational(d);
  }

  /**
   * This is different from String2.modifyToBeFileNameSafe -- this encodes non-fileNameSafe
   * characters so little or no information is lost. <br>
   * This returns the string with just file-name-safe characters (0-9, A-Z, a-z, _, -, .). <br>
   * 'x' and non-safe characters are CONVERTED to 'x' plus their 2 lowercase hexadecimalDigit number
   * or "xx" + their 4 hexadecimalDigit number. <br>
   * See posix fully portable file names at https://en.wikipedia.org/wiki/Filename . <br>
   * When the encoding is more than 25 characters, this stops encoding and adds "xh" and the hash
   * code for the entire original string, so the result will always be less than ~41 characters.
   *
   * <p>THIS WON'T BE CHANGED. FILE NAMES CREATED FOR EDDGridCopy and EDDTableCopy DEPEND ON SAME
   * ENCODING OVER TIME.
   *
   * @param s
   * @return s with all of the non-fileNameSafe characters changed. <br>
   *     If s is null, this returns "x-1". <br>
   *     If s is "", this returns "x-0".
   */
  public static String encodeFileNameSafe(String s) {
    return String2.encodeFileNameSafe(s);
  }

  /**
   * This is like encodeFileNameSafe, but further restricts the name to
   *
   * <ul>
   *   <li>first character must be A-Z, a-z.
   *   <li>subsequent characters must be A-Z, a-z, _, 0-9.
   * </ul>
   *
   * <br>
   * 'x' and non-safe characters are CONVERTED to 'x' plus their 2 lowercase hexadecimalDigit number
   * or "xx" + their 4 hexadecimalDigit number. <br>
   * See posix fully portable file names at https://en.wikipedia.org/wiki/Filename . <br>
   * When the encoding is more than 25 characters, this stops encoding and adds "xh" and the hash
   * code for the entire original string, so the result will always be less than ~41 characters.
   * <br>
   * This meets MatLab restrictions:
   * https://www.mathworks.com/help/matlab/ref/matlab.lang.makevalidname.html
   *
   * <p>THIS WON'T BE CHANGED. FILE NAMES CREATED FOR EDDGridFromFile and EDDTableFromFile DEPEND ON
   * SAME ENCODING OVER TIME.
   *
   * @param s
   * @return s with all of the non-variableNameSafe characters changed. <br>
   *     If s is null, this returns "x_1". <br>
   *     If s is "", this returns "x_0".
   */
  public static String encodeMatlabNameSafe(String s) {
    return String2.encodeMatlabNameSafe(s);
  }

  /**
   * This is like encodeMatlabNameSafe, but simpler and won't always retain all the info.
   *
   * <ul>
   *   <li>first character must be A-Z, a-z.
   *   <li>subsequent characters must be A-Z, a-z, _, 0-9.
   * </ul>
   *
   * <br>
   * non-safe characters are some safe variant. <br>
   * See posix fully portable file names at https://en.wikipedia.org/wiki/Filename . <br>
   * When the encoding is more than 25 characters, this stops encoding and adds "xh" and the hash
   * code for the entire original string, so the result will always be less than ~41 characters.
   * <br>
   * This meets MatLab restrictions:
   * https://www.mathworks.com/help/matlab/ref/matlab.lang.makevalidname.html
   *
   * <p>THIS WON'T BE CHANGED. SOME datasetIDs DEPEND ON SAME ENCODING OVER TIME.
   *
   * @param s
   * @return s with all of the non-variableNameSafe characters changed. <br>
   *     If s is null, this returns "null_". <br>
   *     If s is "", this returns "nothing_".
   */
  public static String simpleMatlabNameSafe(String s) {
    return String2.simpleMatlabNameSafe(s);
  }

  /**
   * For command line parameters, this returns toJson(s) if the string is empty or contains special
   * characters or single or double quotes or backslash; otherwise it return s.
   */
  public static String quoteParameterIfNeeded(String s) {
    return String2.quoteParameterIfNeeded(s);
  }

  /**
   * This changes the characters case to title case (only letters after non-letters are
   * capitalized). This is simplistic (it doesn't know about acronyms or pH or ...).
   */
  public static String toTitleCase(String s) {
    return String2.toTitleCase(s);
  }

  /**
   * This changes the character's case to sentence case (first letter and first letter after each
   * period capitalized). This is simplistic.
   */
  public static String toSentenceCase(String s) {
    return String2.toSentenceCase(s);
  }

  /**
   * This suggests a camel-case variable name.
   *
   * @param s the starting string for the variable name.
   * @return a valid variable name asciiLowerCaseLetter+asciiDigitLetter*, using camel case. This is
   *     a simplistic suggestion. Different strings may return the same variable name. null returns
   *     "null". "" returns "a".
   */
  public static String toVariableName(String s) {
    return String2.toVariableName(s);
  }

  /**
   * This converts "camelCase99String" to "Camel Case 99 String"
   *
   * @param s the camel case string.
   * @return the string with spaces before capital letters. null returns null. "" returns "".
   */
  public static String camelCaseToTitleCase(String s) {
    return String2.camelCaseToTitleCase(s);
  }

  /** This returns true if the string contains only ISO 8859-1 characters (i.e., 0 - 255). */
  public static boolean isIso8859(String s) {
    return String2.isIso8859(s);
  }

  /** This returns true if s isn't null and s.trim().length() &gt; 0. */
  public static boolean isSomething(String s) {
    return String2.isSomething(s);
  }

  /** This returns true if s isn't null, "", "-", "null", "nd", "N/A", "...", "???", etc. */
  public static boolean isSomething2(String s) {
    return String2.isSomething2(s);
  }

  /** This cleverly concatenates the 2 strings (with "", ". ", or " ", as appropriate. */
  public static String periodSpaceConcat(String a, String b) {
    return String2.periodSpaceConcat(a, b);
  }

  /**
   * This cleverly concatenates the 2 strings (with separator, as appropriate).
   *
   * @param a may be null or "" or something
   * @param separator will only be used if a and b are something.
   * @param b may be null or "" or something
   * @return a.trim(), a.trim()+separator+b.trim(), b.trim(), or ""
   */
  public static String ifSomethingConcat(String a, String separator, String b) {
    return String2.ifSomethingConcat(a, separator, b);
  }
}
