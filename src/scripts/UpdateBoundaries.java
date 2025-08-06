public class UpdateBoundaries {
    /**
   * This converts an .asc SGTLine datafile into a .double SGTLine datafile. The .double file has
   * groups of pairs of doubles: <br>
   * NaN, nPoints <br>
   * minLon, minLat <br>
   * maxLon, maxLat <br>
   * nPoints pairs of lon, lat
   *
   * <p>The end of the files has NaN, NaN.
   *
   * @throws Exception if trouble
   */
  public static void convertSgtLine(String sourceName, String destName) throws Exception {
    int format = GMT_FORMAT;

    String2.log("convertSgtLine\n in:" + sourceName + "\nout:" + destName);
    DoubleArray tempLat = new DoubleArray();
    DoubleArray tempLon = new DoubleArray();
    String startGapLine1 = format == MATLAB_FORMAT ? "nan nan" : ">";
    String startGapLine2 = format == MATLAB_FORMAT ? "nan nan" : "#";
    int nObjects = 0;

    try (BufferedReader bufferedReader = File2.getDecompressedBufferedFileReader88591(sourceName)) {
      try (DataOutputStream dos =
          new DataOutputStream(new BufferedOutputStream(new FileOutputStream(destName)))) {
        String s = bufferedReader.readLine();
        while (true) {
          if (s == null
              || // null = end-of-file
              s.startsWith(startGapLine1)
              || s.startsWith(startGapLine2)) {
            // try to add this subpath
            if (tempLat.size() > 0) {
              double lonStats[] = tempLon.calculateStats();
              double latStats[] = tempLat.calculateStats();
              dos.writeDouble(Double.NaN);
              dos.writeDouble(tempLat.size());
              dos.writeDouble(lonStats[PrimitiveArray.STATS_MIN]);
              dos.writeDouble(latStats[PrimitiveArray.STATS_MIN]);
              dos.writeDouble(lonStats[PrimitiveArray.STATS_MAX]);
              dos.writeDouble(latStats[PrimitiveArray.STATS_MAX]);
              for (int i = 0; i < tempLat.size(); i++) {
                dos.writeDouble(tempLon.get(i));
                dos.writeDouble(tempLat.get(i));
              }
              nObjects++;
            }
            tempLon.clear();
            tempLat.clear();
            if (s == null) break;
          } else {
            // each line: x\ty
            String[] items = String2.split(s, '\t');
            if (items.length == 2) {
              double tLon = String2.parseDouble(items[0]);
              double tLat = String2.parseDouble(items[1]);
              if (!Double.isNaN(tLon) && !Double.isNaN(tLat)) {
                tempLon.add(tLon);
                tempLat.add(tLat);
              } else {
                // bufferedReader and dos closed by finally{} below
                throw new RuntimeException(String2.ERROR + " in convertSGTLine, s=" + s);
              }
            }
          }
          s = bufferedReader.readLine();
        }
        // mark of file
        dos.writeDouble(Double.NaN);
        dos.writeDouble(Double.NaN);
      }
    }
    String2.log("    Boundaries.convertSgtLine nObjects=" + nObjects);
  }

  /** Bob reruns this whenever there is new data to convert all of the data files. */
  public static void bobConvertAll() throws Exception {
    String dir = "c:/programs/boundaries/";
    String types[] = {"nationalBoundaries", "stateBoundaries", "rivers"};
    String ress[] = {"c", "f", "h", "i", "l"};
    for (String string : types) {
      for (String s : ress) {
        convertSgtLine(dir + string + s + ".asc", dir + string + s + ".double");
      }
    }
  }
}
