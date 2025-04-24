public class StringArray {
    /**
   * This compares two text files, line by line, and throws Exception indicating line where
   * different. nullString == nullString is ok.
   *
   * @param fileName1 a complete file name
   * @param fileName2 a complete file name
   * @param charset e.g., File2.ISO_8859_1 or File2.UTF_8.
   * @throws Exception if files are different
   */
  public static void diff(final String fileName1, final String fileName2, final String charset)
  throws Exception {
StringArray sa1 = fromFile(fileName1, charset);
StringArray sa2 = fromFile(fileName2, charset);
sa1.diff(sa2);
}

/**
* This repeatedly compares two text files, line by line, and throws Exception indicating line
* where different. nullString == nullString is ok.
*
* @param fileName1 a complete file name
* @param fileName2 a complete file name
* @param charset e.g., File2.ISO_8859_1 or File2.UTF_8.
* @throws Exception if files are different
*/
public static void repeatedDiff(
  final String fileName1, final String fileName2, final String charset) throws Exception {
while (true) {
  try {
    String2.log("\nComparing " + fileName1 + "\n      and " + fileName2);
    final StringArray sa1 = fromFile(fileName1, charset);
    final StringArray sa2 = fromFile(fileName2, charset);
    sa1.diff(sa2);
    String2.log("!!! The files are the same!!!");
    break;
  } catch (Exception e) {
    String2.getStringFromSystemIn(
        MustBe.throwableToString(e)
            + "\nPress ^C to stop or Enter to compare the files again...");
  }
}
}
}
