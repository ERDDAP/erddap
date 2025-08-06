/* This file is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.util;

import com.cohort.array.Attributes;
import com.cohort.array.PAOne;
import com.cohort.array.PrimitiveArray;
import java.math.BigInteger;

/** This is a Java program to test all of the methods in com.cohort.util. */
public class Test {

  /**
   * This throws a runtime exception with the specified error message.
   *
   * @param message
   */
  @SuppressWarnings("DoNotCallSuggester")
  public static void error(String message) throws RuntimeException {
    throw new RuntimeException(message);
  }

  /**
   * If the two boolean values aren't equal, this throws a RuntimeException with the specified
   * message.
   *
   * @param b1
   * @param b2
   * @param message
   */
  public static void ensureEqual(boolean b1, boolean b2, String message) throws RuntimeException {
    if (b1 != b2)
      error(
          "\n"
              + String2.ERROR
              + " in Test.ensureEqual(boolean):\n"
              + message
              + "\nSpecifically: "
              + b1
              + " != "
              + b2);
  }

  /**
   * If the boolean values isn't true, this throws a RuntimeException with the specified message.
   *
   * @param b
   * @param message
   */
  public static void ensureTrue(boolean b, String message) throws RuntimeException {
    if (!b) error("\n" + String2.ERROR + " in Test.ensureTrue:\n" + message);
  }

  /**
   * If the two char values aren't equal, this throws a RuntimeException with the specified message.
   *
   * @param c1
   * @param c2
   * @param message
   */
  public static void ensureEqual(char c1, char c2, String message) throws RuntimeException {
    if (c1 != c2)
      error(
          "\n"
              + String2.ERROR
              + " in Test.ensureEqual(char):\n"
              + message
              + "\nSpecifically: "
              + c1
              + " != "
              + c2);
  }

  /**
   * If the two long values aren't equal, this throws a RuntimeException with the specified message.
   *
   * @param i1
   * @param i2
   * @param message
   */
  public static void ensureEqual(long i1, long i2, String message) throws RuntimeException {
    if (i1 != i2)
      error(
          "\n"
              + String2.ERROR
              + " in Test.ensureEqual(long):\n"
              + message
              + "\nSpecifically: "
              + i1
              + " != "
              + i2);
  }

  /**
   * If the two BigInteger values aren't equal, this throws a RuntimeException with the specified
   * message.
   *
   * @param i1
   * @param i2
   * @param message
   */
  public static void ensureEqual(BigInteger i1, BigInteger i2, String message)
      throws RuntimeException {
    if (i1 == null && i2 == null) return;
    if (i1 == null || !i1.equals(i2))
      error(
          "\n"
              + String2.ERROR
              + " in Test.ensureEqual(BigInteger):\n"
              + message
              + "\nSpecifically: "
              + i1
              + " != "
              + i2);
  }

  /**
   * If the two PAOne values aren't equal, this throws a RuntimeException with the specified
   * message.
   *
   * @param i1
   * @param i2
   * @param message
   */
  public static void ensureEqual(PAOne i1, PAOne i2, String message) throws RuntimeException {
    if (i1 == null && i2 == null) return;
    if (i1 == null || !i1.equals(i2))
      error(
          "\n"
              + String2.ERROR
              + " in Test.ensureEqual(PAOne):\n"
              + message
              + "\nSpecifically: "
              + i1
              + " != "
              + i2);
  }

  /**
   * If the two long values are equal, this throws a RuntimeException with the specified message.
   *
   * @param i1
   * @param i2
   * @param message
   */
  public static void ensureNotEqual(long i1, long i2, String message) throws RuntimeException {
    if (i1 == i2)
      error(
          "\n"
              + String2.ERROR
              + " in Test.ensureNotEqual(long):\n"
              + message
              + "\nSpecifically: "
              + i1
              + " = "
              + i2);
  }

  /**
   * This returns true if the two float values are almost equal (5 digits) or both NaN or both
   * infinite.
   *
   * @param f1
   * @param f2
   */
  public static boolean equal(float f1, float f2) {
    // special check if both are the same special value
    if (Float.isNaN(f1) && Float.isNaN(f2)) return true;
    if (Float.isInfinite(f1) && Float.isInfinite(f2)) return f1 > 0 == f2 > 0;
    return Math2.almostEqual(5, f1, f2);
  }

  /**
   * This returns true if the two double values are almost equal (9 digits) or both NaN or both
   * infinite.
   *
   * @param d1
   * @param d2
   */
  public static boolean equal(double d1, double d2) {
    // special check if both are the same special value
    if (Double.isNaN(d1) && Double.isNaN(d2)) return true;
    if (Double.isInfinite(d1) && Double.isInfinite(d2)) return d1 > 0 == d2 > 0;
    return Math2.almostEqual(9, d1, d2);
  }

  /**
   * This returns true if the two String values are equal (or both null).
   *
   * @param s1
   * @param s2
   */
  public static boolean equal(String s1, String s2) {
    if (s1 == null && s2 == null) return true;
    if (s1 == null || s2 == null) return false;
    return s1.equals(s2);
  }

  /**
   * If the two float values aren't almost equal, this throws a RuntimeException with the specified
   * message.
   *
   * @param f1
   * @param f2
   * @param message
   */
  public static void ensureEqual(float f1, float f2, String message) throws RuntimeException {
    if (!equal(f1, f2))
      error(
          "\n"
              + String2.ERROR
              + " in Test.ensureEqual(float):\n"
              + message
              + "\nSpecifically: "
              + f1
              + " != "
              + f2);
  }

  /**
   * If the two double values aren't almost equal, this throws a RuntimeException with the specified
   * message.
   *
   * @param d1
   * @param d2
   * @param message
   */
  public static void ensureEqual(double d1, double d2, String message) throws RuntimeException {
    if (!equal(d1, d2))
      error(
          "\n"
              + String2.ERROR
              + " in Test.ensureEqual(double):\n"
              + message
              + "\nSpecifically: "
              + d1
              + " != "
              + d2);
  }

  /**
   * If the two double values are equal, this throws a RuntimeException with the specified message.
   *
   * @param d1
   * @param d2
   * @param message
   */
  public static void ensureNotEqual(double d1, double d2, String message) throws RuntimeException {
    // special check if both are the same special value
    if ((Double.isNaN(d1) && Double.isNaN(d2))
        || (Double.isInfinite(d1) && Double.isInfinite(d2))
        || Math2.almostEqual(9, d1, d2))
      error(
          "\n"
              + String2.ERROR
              + " in Test.ensureNotEqual(double):\n"
              + message
              + "\nSpecifically: "
              + d1
              + " = "
              + d2);
  }

  /**
   * If d is less than min or greater than max, this throws a RuntimeException with the specified
   * message.
   *
   * @param d
   * @param minAllowed
   * @param maxAllowed
   * @param message
   */
  public static void ensureBetween(double d, double minAllowed, double maxAllowed, String message)
      throws RuntimeException {

    if (Double.isNaN(d) || d < minAllowed || d > maxAllowed)
      error(
          "\n"
              + String2.ERROR
              + " in Test.ensureBetween:\n"
              + message
              + "\nSpecifically: "
              + d
              + " isn't between "
              + minAllowed
              + " and "
              + maxAllowed
              + ".");
  }

  /**
   * If the two String values aren't equal, this throws a RuntimeException with the specified
   * message.
   *
   * @param s1
   * @param s2
   * @param message
   */
  public static void ensureEqual(String s1, String s2, String message) throws RuntimeException {
    String result = testEqual(s1, s2, message);
    if (result.length() == 0) return;
    error(result);
  }

  /**
   * This returns "" if the Strings are equal or an error message if not. This won't throw an
   * exception.
   *
   * @param s1
   * @param s2
   * @param message "" if they're non-null and equal
   */
  public static String testEqual(String s1, String s2, String message) {
    if (s1 == null && s2 == null) return "";
    if (s1 == null && s2 != null)
      return "\n"
          + String2.ERROR
          + " in Test.ensureEqual(Strings):\n"
          + message
          + "\nSpecifically: "
          + "s1=[null]\n"
          + "s2="
          + String2.noLongerThanDots(s2, 70);
    if (s1 != null && s2 == null)
      return "\n"
          + String2.ERROR
          + " in Test.ensureEqual(Strings):\n"
          + message
          + "\nSpecifically:\n"
          + "s1="
          + String2.noLongerThanDots(s1, 70)
          + "\n"
          + "s2=[null]";
    if (s1.equals(s2)) return "";

    // generate the error message
    int po = 0;
    int line = 1;
    int lastNewlinePo = -1;
    int s1length = s1.length();
    int s2length = s2.length();
    while (po < s1length && po < s2length && s1.charAt(po) == s2.charAt(po)) {
      if (s1.charAt(po) == '\n') {
        line++;
        lastNewlinePo = po;
      }
      po++;
    }
    String c1 = po >= s1length ? "" : String2.annotatedString("" + s1.charAt(po));
    String c2 = po >= s2length ? "" : String2.annotatedString("" + s2.charAt(po));
    // find end of lines
    int line1End = po;
    int line2End = po;
    while (line1End < s1length && "\r\n".indexOf(s1.charAt(line1End)) < 0) line1End++;
    while (line2End < s2length && "\r\n".indexOf(s2.charAt(line2End)) < 0) line2End++;
    String line1Sample = String2.annotatedString(s1.substring(lastNewlinePo + 1, line1End));
    String line2Sample = String2.annotatedString(s2.substring(lastNewlinePo + 1, line2End));
    String annS1 = String2.annotatedString(s1);
    String annS2 = String2.annotatedString(s2);
    int poM30 = Math.max(po - 30, 0);
    int po1P30 = Math.min(po + 30, s1length);
    int po2P30 = Math.min(po + 30, s2length);
    String local1 = s1.substring(poM30, po1P30);
    String local2 = s2.substring(poM30, po2P30);
    boolean showArrow = line1Sample.length() < 100 || line2Sample.length() < 100;
    String lineCol = "line #" + line + ", col #" + (po - lastNewlinePo);

    return "\n"
        + String2.ERROR
        + " in Test.ensureEqual(Strings) "
        + lineCol
        + " '"
        + c1
        + "'!='"
        + c2
        + "':\n"
        + (message.length() > 0 ? message + "\n" : "")
        + (line > 1 ? "\"" + annS1 + "\" != \n\"" + annS2 + "\"\n" : "")
        + "Specifically, at "
        + lineCol
        + ":\n"
        + "s1: "
        + line1Sample
        + "\n"
        + "s2: "
        + line2Sample
        + "\n"
        + (showArrow
            ? String2.makeString(' ', 3 + po - lastNewlinePo) + "^" + "\n"
            : "More specifically, at "
                + lineCol
                + ":\n"
                + "s1.substring("
                + poM30
                + ", "
                + po1P30
                + ")="
                + String2.annotatedString(local1)
                + "\n"
                + "s2.substring("
                + poM30
                + ", "
                + po2P30
                + ")="
                + String2.annotatedString(local2)
                + "\n");
  }

  /**
   * This tests each line of the source against the regex in each line of the destination to ensure
   * each matches. If not, this throws a RuntimeException with the specified message. To prepare
   * plain text for this method, you MUST add \\ before these characters {}[]()^$?+*.| . Adding \\
   * before . is optional (since it will match).
   *
   * @param tText a newline-separated block of text. Carriage returns are ignored.
   * @param tRegex a newline-separated set of regexes. Carriage returns are ignored.
   * @param message
   * @return "" if no error or an error message.
   */
  public static String testLinesMatch(String tText, String tRegex, String message) {

    tText = tText == null ? "" : String2.replaceAll(tText, "\r", "");
    tRegex = tRegex == null ? "" : String2.replaceAll(tRegex, "\r", "");
    String[] text = String2.splitNoTrim(tText, '\n');
    String[] regex = String2.splitNoTrim(tRegex, '\n');
    int n = Math.min(text.length, regex.length);
    for (int line = 0; line < n; line++) {
      // String2.log("t" + line + "=" + text[line] + "\n" +
      //            "r" + line + "=" + regex[line] + "\n\n");
      if (!text[line].matches(regex[line]))
        return "\n"
            + String2.ERROR
            + " in Test.ensureLinesMatch():\n"
            + message
            + "\n"
            + "The first line that differs is:\n"
            + "  text ["
            + line
            + "]="
            + String2.annotatedString(text[line])
            + "\n"
            + "  regex["
            + line
            + "]="
            + String2.annotatedString(regex[line])
            + "\n";
      // testEqual(text[line], regex[line], "");  //diagnostic
    }
    if (text.length != regex.length)
      return "\n"
          + String2.ERROR
          + " in Test.ensureLinesMatch():\n"
          + message
          + "\n"
          + "The number of lines differs: text.length="
          + text.length
          + " != regex.length="
          + regex.length;
    return "";
  }

  /**
   * This tests each line of the source against the regex in each line of the destination to ensure
   * each matches. If not, this throws a RuntimeException with the specified message. To prepare
   * plain text for this method, you MUST add \\ before these characters {}[]()^$ . Adding \\ before
   * . is optional (since it will match).
   *
   * @param tText a newline-separated block of text
   * @param tRegex a newline-separated set of regexes
   * @param message
   * @throws RuntimeException if a line of tText doesn't match a regex in tRegex
   */
  public static void ensureLinesMatch(String tText, String tRegex, String message)
      throws RuntimeException {

    String error = testLinesMatch(tText, tRegex, message);
    if (error.length() == 0) return;
    error(error);
  }

  /**
   * If the object is null, this throws a RuntimeException with the specified message.
   *
   * @param o
   * @param message
   */
  public static void ensureNotNull(Object o, String message) throws RuntimeException {
    if (o == null) error("\n" + String2.ERROR + " in Test.ensureNotNull:\n" + message);
  }

  /**
   * If the string is null or "", this throws a RuntimeException with the specified message.
   *
   * @param s
   * @param message
   */
  public static void ensureNotNothing(String s, String message) throws RuntimeException {
    if (s == null || s.length() == 0)
      error("\n" + String2.ERROR + " in Test.ensureNotNothing:\n" + message);
  }

  /**
   * If the toString values of the arrays aren't equal, this throws a RuntimeException with the
   * specified message.
   *
   * @param ar1
   * @param ar2
   * @param message
   */
  public static void ensureEqual(Object ar1[], Object ar2[], String message)
      throws RuntimeException {
    if (ar1 == null && ar2 == null) return;
    ensureEqual(
        ar1.length,
        ar2.length,
        String2.ERROR
            + " in Test.ensureEqual(Object[].length): "
            + message
            + "\n  ar1="
            + String2.toNewlineString(ar1)
            + "\n  ar2="
            + String2.toNewlineString(ar2));
    for (int i = 0; i < ar1.length; i++)
      ensureEqual(
          ar1[i].toString(),
          ar2[i].toString(),
          String2.ERROR + " in Test.ensureEqual(Object[" + i + "]): " + message);
  }

  private static final String errorInObjectEquals =
      "\n" + String2.ERROR + " in Test.ensureEqual(object.equals):\n";

  /**
   * If !a.equals(b), this throws a RuntimeException with the specified message.
   *
   * @param a
   * @param b
   * @param message
   */
  public static void ensureEqual(Object a, Object b, String message) {
    if (a == null && b == null) return;
    if ((a == null) && (b != null)) error(errorInObjectEquals + message + "\nSpecifically: a=null");
    if ((a != null) && (b == null)) error(errorInObjectEquals + message + "\nSpecifically: b=null");

    // test for some things that have no equals method
    if (a instanceof byte[] aar && b instanceof byte[] bar) {
      int an = aar.length;
      int bn = bar.length;
      ensureEqual(an, bn, errorInObjectEquals + message + "\na byte[] length != b byte[] length");
      for (int i = 0; i < an; i++)
        if (aar[i] != bar[i])
          Test.error(
              errorInObjectEquals
                  + message
                  + "\na byte["
                  + i
                  + "]="
                  + aar[i]
                  + " != b byte["
                  + i
                  + "]="
                  + bar[i]
                  + ".\n"
                  + "a="
                  + String2.toCSSVString(aar)
                  + "\n"
                  + "b="
                  + String2.toCSSVString(bar)
                  + "\n");
      return;
    }
    if (a instanceof char[] aar && b instanceof char[] bar) {
      int an = aar.length;
      int bn = bar.length;
      ensureEqual(an, bn, errorInObjectEquals + message + "\na char[] length != b char[] length");
      for (int i = 0; i < an; i++)
        if (aar[i] != bar[i])
          Test.error(
              errorInObjectEquals
                  + message
                  + "\na char["
                  + i
                  + "]="
                  + (int) aar[i]
                  + " != b char["
                  + i
                  + "]="
                  + (int) bar[i]
                  + ".\n"
                  + "a="
                  + String2.toCSSVString(aar)
                  + "\n"
                  + "b="
                  + String2.toCSSVString(bar)
                  + "\n");
      return;
    }
    if (a instanceof short[] aar && b instanceof short[] bar) {
      int an = aar.length;
      int bn = bar.length;
      ensureEqual(an, bn, errorInObjectEquals + message + "\na short[] length != b short[] length");
      for (int i = 0; i < an; i++)
        if (aar[i] != bar[i])
          Test.error(
              errorInObjectEquals
                  + message
                  + "\na short["
                  + i
                  + "]="
                  + aar[i]
                  + " != b short["
                  + i
                  + "]="
                  + bar[i]
                  + ".\n"
                  + "a="
                  + String2.toCSSVString(aar)
                  + "\n"
                  + "b="
                  + String2.toCSSVString(bar)
                  + "\n");
      return;
    }
    if (a instanceof int[] aar && b instanceof int[] bar) {
      int an = aar.length;
      int bn = bar.length;
      ensureEqual(an, bn, errorInObjectEquals + message + "\na int[] length != b int[] length");
      for (int i = 0; i < an; i++)
        if (aar[i] != bar[i])
          Test.error(
              errorInObjectEquals
                  + message
                  + "\na int["
                  + i
                  + "]="
                  + aar[i]
                  + " != b int["
                  + i
                  + "]="
                  + bar[i]
                  + ".\n"
                  + "a="
                  + String2.toCSSVString(aar)
                  + "\n"
                  + "b="
                  + String2.toCSSVString(bar)
                  + "\n");
      return;
    }
    if (a instanceof long[] aar && b instanceof long[] bar) {
      int an = aar.length;
      int bn = bar.length;
      ensureEqual(an, bn, errorInObjectEquals + message + "\na long[] length != b long[] length");
      for (int i = 0; i < an; i++)
        if (aar[i] != bar[i])
          Test.error(
              errorInObjectEquals
                  + message
                  + "\na long["
                  + i
                  + "]="
                  + aar[i]
                  + " != b long["
                  + i
                  + "]="
                  + bar[i]
                  + ".\n"
                  + "a="
                  + String2.toCSSVString(aar)
                  + "\n"
                  + "b="
                  + String2.toCSSVString(bar)
                  + "\n");
      return;
    }
    if (a instanceof BigInteger[] aar && b instanceof BigInteger[] bar) {
      int an = aar.length;
      int bn = bar.length;
      ensureEqual(
          an,
          bn,
          errorInObjectEquals + message + "\na BigInteger[] length != b BigInteger[] length");
      for (int i = 0; i < an; i++)
        if (!aar[i].equals(bar[i]))
          Test.error(
              errorInObjectEquals
                  + message
                  + "\na BigInteger["
                  + i
                  + "]="
                  + aar[i]
                  + " != b BigInteger["
                  + i
                  + "]="
                  + bar[i]
                  + ".\n"
                  + "a="
                  + String2.toCSSVString(aar)
                  + "\n"
                  + "b="
                  + String2.toCSSVString(bar)
                  + "\n");
      return;
    }
    if (a instanceof float[] aar && b instanceof float[] bar) {
      int an = aar.length;
      int bn = bar.length;
      ensureEqual(an, bn, errorInObjectEquals + message + "\na float[] length != b float[] length");
      for (int i = 0; i < an; i++)
        if (!equal(aar[i], bar[i]))
          Test.error(
              errorInObjectEquals
                  + message
                  + "\na float["
                  + i
                  + "]="
                  + aar[i]
                  + " != b float["
                  + i
                  + "]="
                  + bar[i]
                  + ".\n"
                  + "a="
                  + String2.toCSSVString(aar)
                  + "\n"
                  + "b="
                  + String2.toCSSVString(bar)
                  + "\n");
      return;
    }
    if (a instanceof double[] aar && b instanceof double[] bar) {
      int an = aar.length;
      int bn = bar.length;
      ensureEqual(
          an, bn, errorInObjectEquals + message + "\na double[] length != b double[] length");
      for (int i = 0; i < an; i++)
        if (!equal(aar[i], bar[i]))
          Test.error(
              errorInObjectEquals
                  + message
                  + "\na double["
                  + i
                  + "]="
                  + aar[i]
                  + " != b double["
                  + i
                  + "]="
                  + bar[i]
                  + ".\n"
                  + "a="
                  + String2.toCSSVString(aar)
                  + "\n"
                  + "b="
                  + String2.toCSSVString(bar)
                  + "\n");
      return;
    }
    if (a instanceof String[] aar && b instanceof String[] bar) {
      int an = aar.length;
      int bn = bar.length;
      ensureEqual(
          an, bn, errorInObjectEquals + message + "\na String[] length != b String[] length");
      for (int i = 0; i < an; i++)
        if (!aar[i].equals(bar[i]))
          Test.error(
              errorInObjectEquals
                  + message
                  + "\na String["
                  + i
                  + "]=\""
                  + aar[i]
                  + "\" != b String["
                  + i
                  + "]=\""
                  + bar[i]
                  + "\".\n"
                  + "a="
                  + String2.toCSSVString(aar)
                  + "\n"
                  + "b="
                  + String2.toCSSVString(bar)
                  + "\n");
      return;
    }
    if (a instanceof StringBuilder && b instanceof StringBuilder) {
      ensureEqual(a.toString(), b.toString(), message);
      return;
    }

    if (a instanceof PrimitiveArray pa) {
      String err = pa.testEquals(b);
      if (err.length() > 0) error(err);
    }
    if (a instanceof PAOne) {
      String s = a.toString();
      if (s.endsWith(".0")) // so a double can be compared to an int
      s = s.substring(0, s.length() - 2);
      a = s;
    }
    if (b instanceof PAOne) {
      String s = b.toString();
      if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
      b = s;
    }

    // fall through to most general case
    if (!a.toString().equals(b.toString()))
      error(
          errorInObjectEquals
              + message
              + "\nSpecifically:\n"
              + "a("
              + a.getClass().getName()
              + ")="
              + a
              + "\n"
              + "b("
              + b.getClass().getName()
              + ")="
              + b);
  }

  /**
   * This ensures s is something (not null or "") and is fileNameSave (see
   * String2.isFileNameSafe(s)).
   *
   * @param s
   * @param message ending with the item's name
   */
  public static void ensureFileNameSafe(String s, String message) throws RuntimeException {
    if (!String2.isFileNameSafe(s))
      error(
          "\n"
              + String2.ERROR
              + " in Test.ensureFileNameSafe():\n"
              + message
              + "=\""
              + String2.annotatedString(s)
              + "\" must have length>0 and must contain only safe characters "
              + String2.fileNameSafeDescription
              + ".");
  }

  /**
   * This ensures s is something (not null or "") and is valid Unicode (see
   * String2.findInvalidUnicode(s, "\r\n\t")).
   *
   * @param s
   * @param message ending with the item's name
   */
  public static void ensureSomethingUnicode(String s, String message) throws RuntimeException {
    if (s == null || s.trim().length() == 0)
      error(
          "\n" + String2.ERROR + " in Test.ensureSomethingUnicode():\n" + message + " wasn't set.");

    int po = String2.findInvalidUnicode(s, "\r\n\t");
    if (po >= 0) {
      int max = Math.min(po + 20, s.length());
      error(
          "\n"
              + String2.ERROR
              + " in Test.ensureSomethingUnicode():\n"
              + message
              + " has an invalid Unicode character (#"
              + (int) s.charAt(po)
              + ") at position="
              + po
              + (po > 80
                  ? "\n[#] at the center of \""
                      + String2.annotatedString(s.substring(po - 20, max))
                      + "\""
                  : "\n[#] in \"" + String2.annotatedString(s) + "\""));
    }
  }

  /**
   * This ensures atts isn't null, and the names and attributes in atts are something (not null or
   * "") and are valid Unicode (see String2.findInvalidUnicode(s, "\n\t")). 0 names+attributes is
   * valid.
   *
   * @param atts
   * @param message
   */
  public static void ensureSomethingUnicode(Attributes atts, String message)
      throws RuntimeException {

    if (atts == null)
      error(
          "\n" + String2.ERROR + " in Test.ensureSomethingUnicode():\n" + message + " wasn't set.");
    String names[] = atts.getNames();
    for (String name : names) {
      ensureSomethingUnicode(name, message + ": an attribute name");
      ensureSomethingUnicode(
          atts.get(name).toString(), message + ": the attribute value for name=" + name);
    }
  }
}
