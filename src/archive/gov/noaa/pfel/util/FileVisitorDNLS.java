public class FileVisitorDNLS {
    /**
   * This looks in all the specified files until a file with a line that matches lineRegex is found.
   *
   * @param tDir The starting directory.
   * @param tFileNameRegex The regex to specify which file names to include.
   * @param tRecursive If true, subdirectories are also searched.
   * @param tPathRegex If tRecursive, this specifies which subdirs should be searched.
   * @param lineRegex This is the main regex. We seek lines that match this regex.
   * @param tallyWhich If &gt;= 0, this tabulates the values of the tallyWhich-th capture group in
   *     the lineRegex.
   * @param interactiveNLines if &gt;0, this shows the file, the matching line and nLines
   *     thereafter, and calls pressEnterToContinue(). If false, this returns the name of the first
   *     file matching lineRegex.
   * @param showTopN If tallyWhich &gt;=0, the results will show the topN matched values.
   * @return this returns the first matching fileName, or null if none
   */
  public static String findFileWith(
    String tDir,
    String tFileNameRegex,
    boolean tRecursive,
    String tPathRegex,
    String lineRegex,
    int tallyWhich,
    int interactiveNLines,
    int showTopN)
    throws Throwable {
  String2.log("\n*** findFileWith(\"" + lineRegex + "\")");
  Table table = oneStep(tDir, tFileNameRegex, tRecursive, tPathRegex, false);
  StringArray dirs = (StringArray) table.getColumn(DIRECTORY);
  StringArray names = (StringArray) table.getColumn(NAME);
  Tally tally = tallyWhich >= 0 ? new Tally() : null;
  int nFiles = names.size();
  Pattern linePattern = Pattern.compile(lineRegex);
  String firstFullName = null;
  int lastFileiMatch = -1;
  int nFilesMatched = 0;
  int nLinesMatched = 0;
  for (int filei = 0; filei < nFiles; filei++) {
    if (filei % 100 == 0)
      String2.log(
          "file#"
              + filei
              + " nFilesMatched="
              + nFilesMatched
              + " nLinesMatched="
              + nLinesMatched);
    try {
      String fullName = dirs.get(filei) + names.get(filei);
      List<String> lines = SSR.getUrlResponseArrayList(fullName);
      int nLines = lines.size();
      for (int linei = 0; linei < nLines; linei++) {
        Matcher matcher = linePattern.matcher(lines.get(linei));
        if (matcher.matches()) {
          nLinesMatched++;
          if (lastFileiMatch != filei) {
            lastFileiMatch = filei;
            nFilesMatched++;
          }
          if (firstFullName == null) firstFullName = fullName;
          if (tallyWhich >= 0) tally.add(lineRegex, matcher.group(tallyWhich));
          if (interactiveNLines > 0) {
            String msg =
                "\n***** Found match in fileName#" + filei + "=" + fullName + " on line#" + linei;
            String2.log(msg + ". File contents=");
            String2.log(String2.toNewlineString(lines.toArray(new String[0])));
            String2.log("\n" + msg + ":\n");
            for (int tl = linei; tl < Math.min(linei + interactiveNLines + 1, nLines); tl++)
              String2.log(lines.get(tl));
            String2.pressEnterToContinue();
          } else if (tallyWhich < 0) {
            return firstFullName;
          }
        }
      }
    } catch (Exception e) {
      String2.log(MustBe.throwableToString(e));
    }
  }
  String2.log(
      "\nfindFileWhich finished successfully. nFiles="
          + nFiles
          + " nFilesMatched="
          + nFilesMatched
          + " nLinesMatched="
          + nLinesMatched);
  if (tallyWhich >= 0) String2.log(tally.toString(showTopN)); // most common n will be shown
  return firstFullName;
}

/** This finds the file names where an xml findTags matches a regex. */
public static void findMatchingContentInXml(
    String dir, String fileNameRegex, boolean recursive, String findTag, String matchRegex)
    throws Exception {

  Table table =
      oneStep(
          dir, fileNameRegex, recursive, ".*", false); // tRecursive, tPathRegex, tDirectoriesToo
  StringArray dirs = (StringArray) table.getColumn(FileVisitorDNLS.DIRECTORY);
  StringArray names = (StringArray) table.getColumn(FileVisitorDNLS.NAME);
  int nErrors = 0;
  for (int i = 0; i < names.size(); i++) {

    SimpleXMLReader xmlReader = null;
    try {
      String2.log("reading #" + i + ": " + dirs.get(i) + names.get(i));
      xmlReader =
          new SimpleXMLReader(
              File2.getDecompressedBufferedInputStream(dirs.get(i) + names.get(i)));
      while (true) {
        xmlReader.nextTag();
        String tags = xmlReader.allTags();
        if (tags.length() == 0) break;
        if (tags.equals(findTag) && xmlReader.content().matches(matchRegex))
          String2.log(dirs.get(i) + names.get(i) + " has \"" + xmlReader.content() + "\"");
      }
      xmlReader.close();

    } catch (Throwable t) {
      String2.log(MustBe.throwableToString(t));
      nErrors++;
      if (xmlReader != null) xmlReader.close();
    }
  }
  String2.log("\n*** findMatchingContentInXml() finished. nErrors=" + nErrors);
}
}
