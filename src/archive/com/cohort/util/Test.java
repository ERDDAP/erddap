public class Test {
    /**
   * This is like ensureLinesMatch, but will test all lines even if there are failures on individual
   * lines.
   *
   * @param tText a newline-separated block of text
   * @param tRegex a newline-separated set of regexes
   * @param message
   * @throws RuntimeException if a line of tText doesn't match a regex in tRegex
   */
  public static void repeatedlyTestLinesMatch(String tText, String tRegex, String message)
  throws RuntimeException {

tText = tText == null ? "" : String2.replaceAll(tText, "\r", "");
tRegex = tRegex == null ? "" : String2.replaceAll(tRegex, "\r", "");
String[] text = String2.splitNoTrim(tText, '\n');
String[] regex = String2.splitNoTrim(tRegex, '\n');
int n = Math.min(text.length, regex.length);
int nDifferences = 0;
for (int line = 0; line < n; line++) {
  // String2.log("t" + line + "=" + text[line] + "\n" +
  //            "r" + line + "=" + regex[line] + "\n\n");
  if (!text[line].matches(regex[line]))
    String2.pressEnterToContinue(
        message
            + "\n"
            + MustBe.getStackTrace()
            + "\n"
            + String2.ERROR
            + " in Test.repeatedlyEnsureLinesMatch():\n"
            + "Difference #"
            + ++nDifferences
            + " is:\n"
            + "  text ["
            + line
            + "]="
            + String2.annotatedString(text[line])
            + "\n"
            + "  regex["
            + line
            + "]="
            + String2.annotatedString(regex[line])
            + "\n"
            + "Press Enter to see the next error. ");
  // testEqual(text[line], regex[line], "");  //diagnostic
}
if (text.length != regex.length)
  String2.pressEnterToContinue(
      message
          + "\n"
          + MustBe.getStackTrace()
          + "\n"
          + String2.ERROR
          + " in Test.ensureLinesMatch():\n"
          + "The number of lines differs: text.length="
          + text.length
          + " != regex.length="
          + regex.length);
}

 /**
   * This returns true if the two BigInteger values are equal (or both null).
   *
   * @param s1
   * @param s2
   */
  public static boolean equal(BigInteger s1, BigInteger s2) {
    if (s1 == null && s2 == null) return true;
    if (s1 == null || s2 == null) return false;
    return s1.equals(s2);
  }

  /**
   * This returns true if the two PAOne values are equal (or both null).
   *
   * @param s1
   * @param s2
   */
  public static boolean equal(PAOne s1, PAOne s2) {
    if (s1 == null && s2 == null) return true;
    if (s1 == null || s2 == null) return false;
    return s1.equals(s2);
  }

  /**
   * If the two BigInteger values are equal, this throws a RuntimeException with the specified
   * message.
   *
   * @param i1
   * @param i2
   * @param message
   */
  public static void ensureNotEqual(BigInteger i1, BigInteger i2, String message)
      throws RuntimeException {
    if (i1 == null && i2 == null) return;
    if (i1 == null || i1.equals(i2))
      error(
          "\n"
              + String2.ERROR
              + " in Test.ensureNotEqual(BigInteger):\n"
              + message
              + "\nSpecifically: "
              + i1
              + " = "
              + i2);
  }
}
