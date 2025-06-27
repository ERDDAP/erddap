public class SSR {

    /**
   * This is a variant of shell() for cShell command lines.
   *
   * @param commandLine the command line to be executed (for example, "myprogram <filename>")
   * @param outStream an outputStream to capture the results. This does not close the outStream
   *     afterwards. (Use "null" if you don't want to capture out.)
   * @param errStream an outputStream to capture the error. This does not close the errStream
   *     afterwards. (Use "null" if you don't want to capture err.)
   * @param timeOutSeconds (use 0 for no timeout)
   * @return the exitValue
   * @throws Exception but unlike the other shell commands, this doesn't throw an exception just
   *     because exitValue != 0.
   * @see #shell
   */
  public static int cShell(
    String commandLine, OutputStream outStream, OutputStream errStream, int timeOutSeconds)
    throws Exception {
  if (verbose) String2.log("cShell        in: " + commandLine);
  ByteArrayOutputStream outBAOS = null;
  ByteArrayOutputStream errBAOS = null;
  if (outStream == null) {
    outBAOS = new ByteArrayOutputStream();
    outStream = outBAOS;
  }
  if (errStream == null) {
    errBAOS = new ByteArrayOutputStream();
    errStream = errBAOS;
  }
  PipeToOutputStream outCatcher = new PipeToOutputStream(outStream);
  PipeToOutputStream errCatcher = new PipeToOutputStream(errStream);

  // call shell()
  int exitValue =
      shell(new String[] {"/bin/csh", "-c", commandLine}, outCatcher, errCatcher, timeOutSeconds);

  // if I created the streams, close them
  if (outBAOS != null)
    try {
      outBAOS.close();
    } catch (Exception e) {
    }
  if (errBAOS != null)
    try {
      errBAOS.close();
    } catch (Exception e) {
    }

  // collect and print results (or throw exception)
  String err = errBAOS == null ? "" : errBAOS.toString();
  if (verbose || err.length() > 0 || exitValue != 0) {
    String2.log(
        "cShell       cmd: "
            + commandLine
            + "\n"
            + "cShell exitValue: "
            + exitValue
            + "\n"
            + "cShell       err: "
            + (errBAOS == null ? "[unknown]" : err));
  }

  return exitValue;
}

/**
   * This returns a string with the topNMostRequested items.
   *
   * @param printTopN
   * @param header (e.g., printTopN + " Most Requested .grd Files")
   * @param requestedFilesMap (map with key=String (e.g., fileName), value=IntObject with frequency
   *     info). If it needs to be thread-safe, use ConcurrentHashMap.
   * @return a string with the topN items in a table
   */
  public static String getTopN(int printTopN, String header, Map requestedFilesMap) {
    // printTopNMostRequested
    // many of these will be artifacts: e.g., initial default file
    StringBuilder sb = new StringBuilder();
    if (printTopN > 0 && !requestedFilesMap.isEmpty()) {
      // topN will be kept as sorted ascending, so best will be at end
      String topN[] = new String[printTopN];
      Arrays.fill(topN, "\t");
      int worst = 0;
      int nActive = 0;
      Iterator it = requestedFilesMap.keySet().iterator();
      while (it.hasNext()) {
        String key = (String) it.next();
        int value = ((IntObject) requestedFilesMap.get(key)).i;
        if (value <= 0) continue;
        if (nActive < printTopN || value > worst) {
          if (nActive < printTopN) nActive++;
          if (value < worst) worst = value;
          String ts = String2.right("" + value, 9) + "  " + key;
          int where = Arrays.binarySearch(topN, ts);
          if (where >= 0)
            // it is already in the array -- shouldn't ever happen
            sb.append(
                String2.ERROR
                    + ": SSR.getTopN wants to insert \""
                    + ts
                    + "\"\n"
                    + "at "
                    + where
                    + ", where values are\n"
                    + String2.toNewlineString(topN)
                    + "\n");
          else {
            // make 'where' positively stated
            where = -where - 2; // would be -1, but worst is always thrown out

            // 'where' may be -1 if tie for worst and file name sorts it at beginning
            if (where >= 0) {
              // open up a space (worst is always thrown out)
              System.arraycopy(topN, 1, topN, 0, where);

              // insert it
              topN[where] = ts;
            }
          }
        }
      }

      // print the most requested .grd files
      sb.append(printTopN + header + "\n");
      for (int i = printTopN - 1; i >= 0; i--) {
        if (!topN[i].equals("\t"))
          sb.append(String2.right("#" + (printTopN - i), 7) + topN[i] + "\n");
      }
    }
    return sb.toString();
  }

  /**
   * This runs a series matlab commands, ending with 'exit' or 'quit'. This is set up for Linux
   * computers and uses cShell commands.
   *
   * @param fullControlName the name of the file with the commands for Matlabs command line (one per
   *     line).
   * @param fullOutputName the name for the file that will be created to hold the Matlab output.
   * @param timeOutSeconds (use -1 for no time out)
   * @throws Exception if trouble
   */
  public static void runMatlab(String fullControlName, String fullOutputName, int timeOutSeconds)
      throws Exception {
    // this is Dave Foley's trick in his /u00/chump/matcom script
    Exception e = null;
    try {
      SSR.cShell("set OLDDISPLAY = $DISPLAY", 1);
      SSR.cShell("unsetenv DISPLAY", 1);
      SSR.cShell(
          "nohup matlab < "
              + fullControlName
              + " >! "
              + fullOutputName, // ">!" writes to a new file
          timeOutSeconds);
    } catch (Exception e2) {
      e = e2;
    }

    // The purpose of try/catch above is to ensure this gets done.
    // Problem was: DISPLAY was unset, then error occurred and DISPLAY was never reset.
    SSR.cShell("setenv DISPLAY $OLDDISPLAY", 1);

    if (e != null) throw e;
  }

  /**
   * This is a one time method to change the names of chl2 files in chla .zip's to chla. This
   * unzips, renames, re-zips the files.
   *
   * @param zipDir the dir with the chla .zip files
   * @param emptyDir needs to be an empty temporary directory
   */
  public static void changeChl2ToChla(String zipDir, String emptyDir) {
    String2.log("SSR.changeChl2ToChla zipDir=" + zipDir + " emptyDir=" + emptyDir);

    // get the names of all the chla files in zipDir
    String names[] = RegexFilenameFilter.fullNameList(zipDir, ".+chla.+\\.zip");

    // for each file
    int countRenamed = 0;
    for (int i = 0; i < names.length; i++) {
      try {
        // unzip to temp dir
        unzip(names[i], emptyDir, true, 10, null);

        // if internal file was already chla, delete internal file and continue
        String tNames[] = RegexFilenameFilter.list(emptyDir, ".+");
        Test.ensureEqual(tNames.length, 1, "nFiles in .zip not 1!");
        if (tNames[0].indexOf("chla") >= 0) {
          File2.delete(emptyDir + tNames[0]);
          continue;
        }
        String2.log("changing " + tNames[0]);

        // rename internal file
        Test.ensureTrue(tNames[0].indexOf("chl2") >= 0, "tNames[0] not chl2 file!");
        String newName = String2.replaceAll(tNames[0], "chl2", "chla");
        File2.rename(emptyDir, tNames[0], newName);

        // delete old zip file
        File2.delete(names[i]);

        // make new zip file
        zip(
            names[i],
            new String[] {emptyDir + newName},
            10,
            false,
            ""); // false = don't include dir names

        // delete internal file
        File2.delete(newName);

        countRenamed++;

      } catch (Exception e) {
        String2.log(MustBe.throwableToString(e));
      }

      // empty the directory
      String tNames2[] = RegexFilenameFilter.list(emptyDir, ".+");
      for (int j = 0; j < tNames2.length; j++) File2.delete(emptyDir + tNames2[j]);
    }
    String2.log(
        "successfully changed " + countRenamed + " out of " + names.length + " chla .zip files.");
  }

  /**
   * This is a one time method to change the names of GH files in GA .zip's to GA. This unzips,
   * renames, re-zips the files into their correct original location.
   *
   * @param zipDir the dir with the chla .zip files
   * @param emptyDir needs to be an empty temporary directory
   */
  public static void changeGHToGA(String zipDir, String emptyDir) {
    String2.log("SSR.changeGHToGA zipDir=" + zipDir + " emptyDir=" + emptyDir);

    // get the names of all the GA files in zipDir
    String names[] = RegexFilenameFilter.fullNameList(zipDir, "GA.+\\.zip");

    // for each file
    int countRenamed = 0;
    for (int i = 0; i < names.length; i++) {
      try {
        // unzip to temp dir
        unzip(names[i], emptyDir, true, 10, null);

        // if internal file was already GA, delete internal file and continue
        String tNames[] = RegexFilenameFilter.list(emptyDir, ".+");
        Test.ensureEqual(tNames.length, 1, "nFiles in .zip not 1!");
        if (tNames[0].startsWith("GA")) {
          File2.delete(emptyDir + tNames[0]);
          continue;
        }
        String2.log("changing " + tNames[0]);

        // rename internal file
        Test.ensureTrue(tNames[0].startsWith("GH"), "tNames[0] not GH file!");
        String newName = "GA" + tNames[0].substring(2);
        File2.rename(emptyDir, tNames[0], newName);

        // delete old zip file
        File2.delete(names[i]);

        // make new zip file
        zip(
            names[i],
            new String[] {emptyDir + newName},
            10,
            false,
            ""); // false = don't include dir names

        // delete internal file
        File2.delete(newName);

        countRenamed++;

      } catch (Exception e) {
        String2.log(MustBe.throwableToString(e));
      }

      // empty the directory
      String tNames2[] = RegexFilenameFilter.list(emptyDir, ".+");
      for (int j = 0; j < tNames2.length; j++) File2.delete(emptyDir + tNames2[j]);
    }
    String2.log(
        "successfully changed " + countRenamed + " out of " + names.length + " GA .zip files.");
  }

  /**
   * This is a one time method to change the names of GA0 files in GA .zip's to GA20. This unzips,
   * renames, re-zips the files into their correct original location.
   *
   * @param zipDir the dir with the chla .zip files
   * @param emptyDir needs to be an empty temporary directory
   */
  public static void changeGA0ToGA20(String zipDir, String emptyDir) {
    String2.log("SSR.changeGA0ToGA20 zipDir=" + zipDir + " emptyDir=" + emptyDir);

    // get the names of all the GA files in zipDir
    String names[] = RegexFilenameFilter.fullNameList(zipDir, "GA.+\\.zip");

    // for each file
    int countRenamed = 0;
    for (int i = 0; i < names.length; i++) {
      try {
        // unzip to temp dir
        unzip(names[i], emptyDir, true, 10, null);

        // if internal file was already GA, delete internal file and continue
        String tNames[] = RegexFilenameFilter.list(emptyDir, ".+");
        Test.ensureEqual(tNames.length, 1, "nFiles in .zip not 1!");
        if (tNames[0].startsWith("GA20")) {
          File2.delete(emptyDir + tNames[0]);
          continue;
        }
        String2.log("changing " + tNames[0]);

        // rename internal file
        Test.ensureTrue(tNames[0].startsWith("GA0"), "tNames[0] not GA0 file!");
        String newName = "GA2" + tNames[0].substring(2);
        File2.rename(emptyDir, tNames[0], newName);

        // delete old zip file
        File2.delete(names[i]);

        // make new zip file
        zip(
            names[i],
            new String[] {emptyDir + newName},
            10,
            false,
            ""); // false = don't include dir names

        // delete internal file
        File2.delete(newName);

        countRenamed++;

      } catch (Exception e) {
        String2.log(MustBe.throwableToString(e));
      }

      // empty the directory
      String tNames2[] = RegexFilenameFilter.list(emptyDir, ".+");
      for (int j = 0; j < tNames2.length; j++) File2.delete(emptyDir + tNames2[j]);
    }
    String2.log(
        "successfully changed " + countRenamed + " out of " + names.length + " GA0 .zip files.");
  }

  /**
   * This is a one time method to enclose each of the files in a directory in its own zip file.
   *
   * @param dir
   */
  public static void zipEach(String dir) {
    String2.log("SSR.zipEach dir=" + dir);

    // get the names of all the files dir
    String names[] = RegexFilenameFilter.fullNameList(dir, ".+");

    // for each file
    int countRenamed = 0;
    for (int i = 0; i < names.length; i++) {
      try {
        // make new zip file
        zip(
            names[i] + ".zip",
            new String[] {names[i]},
            10,
            false,
            ""); // false = don't include dir names

        countRenamed++;

      } catch (Exception e) {
        String2.log(MustBe.throwableToString(e));
      }
    }
    String2.log("successfully zipped " + countRenamed + " out of " + names.length + " .zip files.");
  }

  /**
   * Unzip oldDir + oldName.zip (a zip containing one file: oldName) and rename it to newDir +
   * newName. If newDir + newName already exists, it is File2.'touch'ed.
   *
   * @param oldDir (with slash at end)
   * @param oldName (with .zip at end)
   * @param newDir (with slash at end)
   * @param newName
   * @param timeOutSeconds (use 0 for no timeout)
   * @throws Exception
   */
  public static void unzipRename(
      String oldDir, String oldName, String newDir, String newName, int timeOutSeconds)
      throws Exception {

    // already exists?
    if (File2.touch(newDir + newName)) {
      String2.log("SSR.unzipRename is reusing " + newName);
      return;
    }

    // unzip the file
    unzip(oldDir + oldName, newDir, true, timeOutSeconds, null);

    // rename the file
    String oldNameNoZip = oldName.substring(0, oldName.length() - 4);
    if (!oldNameNoZip.equals(newName)) File2.rename(newDir, oldNameNoZip, newName);
  }

  public static void genericTunnelTest(int nTimes, String baseUrl, String varName)
  throws Exception {
// currently, GAssta hday on otter has time dimension size is 1392
// currently, GAssta hday on oceanwatch has time dimension size is 2877
int nTimePoints = 1000;
System.out.println("\nSSR.genericTunnelTest(" + nTimes + ", " + baseUrl + ")");
long elapsedTime = System.currentTimeMillis();
java.util.Random random = new java.util.Random();

// run the test
for (int time = 0; time < nTimes; time++) {
  // String2.log("iteration #" + time);
  int tIndex = random.nextInt(nTimePoints);
  int xIndex = random.nextInt(52);
  int yIndex = random.nextInt(52);
  @SuppressWarnings("unused")
  String unusedTs =
      getUrlResponseStringUnchanged(
          baseUrl + "?" + varName + "[" + tIndex + ":1:" + tIndex + "]" + "[0:1:0]" + "["
              + yIndex + ":1:" + yIndex + "]" + "[" + xIndex + ":1:" + xIndex + "]");
  // if (time == 0) System.out.println(ts);
}
System.out.println(
    "SSR.threddsTunnelTest done.  TIME=" + (System.currentTimeMillis() - elapsedTime) + "ms\n");
}

/**
   * This gets the bytes from a file.
   *
   * @param fileName
   * @return a byte[] with the response.
   * @throws Exception if error occurs
   */
  public static String getFileString(String fileName) throws Exception {
    return new String(getFileBytes(fileName));
  }

  /**
   * This opens a ZipOutputStream with one entry (with the fileName, but no data). This is not
   * wrapped in a BufferedOutputStream, since it often doesn't need to be.
   *
   * @param zipDirName the full name for the .zip file (path + name + ".zip")
   * @param fileName the file name to be put in the zip file. Your choice: with directory info or
   *     not. Use forward directory separators [I'm pretty sure].
   * @return the ZipOutputStream (or null if trouble)
   */
  public static ZipOutputStream startZipOutputStream(String zipDirName, String fileName) {

    try {
      // Create the ZIP file
      ZipOutputStream out =
          new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipDirName)));

      // Add ZIP entry to output stream.
      out.putNextEntry(new ZipEntry(fileName));

      return out;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * This gets the bytes from a file.
   *
   * @param fileName If compressed file, this reads the decompressed, first file in the archive.
   * @return a byte[] with the response.
   * @throws Exception if error occurs
   */
  public static byte[] getFileBytes(String fileName) throws Exception {
    try (InputStream is = File2.getDecompressedBufferedInputStream(fileName)) {
      long time = System.currentTimeMillis();
      byte buffer[] = new byte[1024];
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      for (int s; (s = is.read(buffer)) != -1; ) baos.write(buffer, 0, s);
      is.close();
      if (reallyVerbose)
        String2.log(
            "  SSR.getFileBytes "
                + fileName
                + " finished. TIME="
                + (System.currentTimeMillis() - time)
                + "ms");
      return baos.toByteArray();
    } catch (Exception e) {
      // String2.log(e.toString());
      throw new Exception("ERROR while reading file=" + fileName + " : " + e, e);
    }
  }
    
}
