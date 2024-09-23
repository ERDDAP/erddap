/*
 * EDDGridFromEtopo Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.FileNameUtility;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDGridFromEtopoHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class represents a grid dataset with Etopo bathymetry data. <br>
 * 2011-03-14 I switched from etopo2v2 to etopo1.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2008-02-20
 */
@SaxHandlerClass(EDDGridFromEtopoHandler.class)
public class EDDGridFromEtopo extends EDDGrid {

  /** Properties of the datafile */
  protected static String fileName =
      EDStatic.getWebInfParentDirectory() + "WEB-INF/ref/etopo1_ice_g_i2.bin";

  protected static final double fileMinLon = -180, fileMaxLon = 180;
  protected static final double fileMinLat = -90, fileMaxLat = 90;
  protected static final int fileNLons = 21601, fileNLats = 10801;
  protected static final int bytesPerValue = 2;
  protected static double fileLonSpacing = (fileMaxLon - fileMinLon) / (fileNLons - 1);
  protected static double fileLatSpacing = (fileMaxLat - fileMinLat) / (fileNLats - 1);
  protected static double fileLons[] =
      DataHelper.getRegularArray(fileNLons, fileMinLon, fileLonSpacing);
  protected static double fileLats[] =
      DataHelper.getRegularArray(fileNLats, fileMinLat, fileLatSpacing);

  /** Set by the constructor */
  protected boolean is180;

  private int nCoarse = 0, nReadFromCache = 0, nWrittenToCache = 0, nFailed = 0;

  /**
   * This constructs an EDDGridFromEtopo based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDGridFromEtopo"&gt; having
   *     just been read.
   * @return an EDDGridFromEtopo. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDGridFromEtopo fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {
    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDGridFromEtopo(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    boolean tAccessibleViaWMS = true;
    boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
    int tnThreads = -1; // interpret invalid values (like -1) as EDStatic.nGridThreads
    boolean tDimensionValuesInMemory = true;

    // process the tags
    int startOfTagsN = xmlReader.stackSize();
    String startOfTags = xmlReader.allTags();
    int startOfTagsLength = startOfTags.length();
    while (true) {
      xmlReader.nextTag();
      if (xmlReader.stackSize() == startOfTagsN) break; // the </dataset> tag
      String tags = xmlReader.allTags();
      String content = xmlReader.content();
      String localTags = tags.substring(startOfTagsLength);

      // try to make the tag names as consistent, descriptive and readable as possible

      // no support for active, since always active
      // no support for accessibleTo, since accessible to all
      // no support for onChange since dataset never changes
      if (localTags.equals("<accessibleViaWMS>")) {
      } else if (localTags.equals("</accessibleViaWMS>"))
        tAccessibleViaWMS = String2.parseBoolean(content);
      else if (localTags.equals("<accessibleViaFiles>")) {
      } else if (localTags.equals("</accessibleViaFiles>"))
        tAccessibleViaFiles = String2.parseBoolean(content);
      else if (localTags.equals("<nThreads>")) {
      } else if (localTags.equals("</nThreads>")) tnThreads = String2.parseInt(content);
      else if (localTags.equals("<dimensionValuesInMemory>")) {
      } else if (localTags.equals("</dimensionValuesInMemory>"))
        tDimensionValuesInMemory = String2.parseBoolean(content);
      else xmlReader.unexpectedTagException();
    }

    return new EDDGridFromEtopo(
        tDatasetID, tAccessibleViaWMS, tAccessibleViaFiles, tnThreads, tDimensionValuesInMemory);
  }

  /**
   * The constructor.
   *
   * @throws Throwable if trouble
   */
  public EDDGridFromEtopo(
      String tDatasetID,
      boolean tAccessibleViaWMS,
      boolean tAccessibleViaFiles,
      int tnThreads,
      boolean tDimensionValuesInMemory)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDGridFromEtopo " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDGridFromEtopo(" + tDatasetID + ") constructor:\n";

    className = "EDDGridFromEtopo";
    datasetID = tDatasetID;
    is180 = datasetID.equals("etopo180");
    Test.ensureTrue(
        is180 || datasetID.equals("etopo360"),
        errorInMethod + "datasetID must be \"etopo180\" or \"etopo360\".");
    if (!tAccessibleViaWMS)
      accessibleViaWMS = String2.canonical(MessageFormat.format(EDStatic.noXxxAr[0], "WMS"));
    accessibleViaFiles = EDStatic.filesActive && tAccessibleViaFiles;
    nThreads = tnThreads; // interpret invalid values (like -1) as EDStatic.nGridThreads
    dimensionValuesInMemory = tDimensionValuesInMemory;

    sourceGlobalAttributes = new Attributes();
    sourceGlobalAttributes.add("acknowledgement", "NOAA NGDC");
    sourceGlobalAttributes.add("cdm_data_type", "Grid");
    sourceGlobalAttributes.add(
        "contributor_name",
        "GLOBE, SRTM30, Baltic Sea Bathymetry, Caspian Sea Bathymetry, "
            + "Great Lakes Bathymetry, Gulf of California Bathymetry, IBCAO, JODC Bathymetry, "
            + "Mediterranean Sea Bathymetry, U.S. Coastal Relief Model (CRM), "
            + "Antarctica RAMP Topography, Antarctic Digital Database, GSHHS");
    sourceGlobalAttributes.add("contributor_role", "source data");
    sourceGlobalAttributes.add("Conventions", "COARDS, CF-1.6, ACDD-1.3");
    sourceGlobalAttributes.add("creator_email", "Barry.Eakins@noaa.gov ");
    sourceGlobalAttributes.add("creator_name", "NOAA NGDC");
    sourceGlobalAttributes.add("creator_url", "https://www.ngdc.noaa.gov/mgg/global/global.html");
    sourceGlobalAttributes.add("data_source", "NOAA NGDC ETOPO1");
    sourceGlobalAttributes.add("drawLandMask", "under");
    sourceGlobalAttributes.add("history", "2011-03-14 Downloaded " + SgtMap.BATHYMETRY_SOURCE_URL);
    sourceGlobalAttributes.add("id", tDatasetID); // 2019-05-07 was "SampledFromETOPO1_ice_g_i2");
    sourceGlobalAttributes.add("infoUrl", "https://www.ngdc.noaa.gov/mgg/global/global.html");
    sourceGlobalAttributes.add("institution", "NOAA NGDC");
    sourceGlobalAttributes.add("keywords", "Oceans > Bathymetry/Seafloor Topography > Bathymetry");
    sourceGlobalAttributes.add("keywords_vocabulary", "GCMD Science Keywords");
    sourceGlobalAttributes.add("license", EDStatic.standardLicense);
    sourceGlobalAttributes.add("naming_authority", "gov.noaa.pfeg.coastwatch");
    sourceGlobalAttributes.add("project", "NOAA NGDC ETOPO");
    sourceGlobalAttributes.add("projection", "geographic");
    sourceGlobalAttributes.add("projection_type", "mapped");
    sourceGlobalAttributes.add("references", SgtMap.BATHYMETRY_CITE);
    sourceGlobalAttributes.add("sourceUrl", "(local file)");
    sourceGlobalAttributes.add(
        "standard_name_vocabulary", FileNameUtility.getStandardNameVocabulary());
    sourceGlobalAttributes.add("summary", SgtMap.BATHYMETRY_SUMMARY);
    sourceGlobalAttributes.add(
        "title",
        "Topography, ETOPO1, 0.0166667 degrees, Global (longitude "
            + (is180 ? "-180 to 180)" : "0 to 360)")
            + ", (Ice Sheet Surface)");

    addGlobalAttributes = new Attributes();
    combinedGlobalAttributes =
        new Attributes(addGlobalAttributes, sourceGlobalAttributes); // order is important
    combinedGlobalAttributes.removeValue("\"null\"");

    // make the axisVariables
    axisVariables = new EDVGridAxis[2];
    latIndex = 0;
    axisVariables[latIndex] =
        new EDVLatGridAxis(
            tDatasetID,
            EDV.LAT_NAME,
            new Attributes(),
            new Attributes(),
            new DoubleArray(DataHelper.getRegularArray(fileNLats, -90, 1 / 60.0)));
    lonIndex = 1;
    axisVariables[lonIndex] =
        new EDVLonGridAxis(
            tDatasetID,
            EDV.LON_NAME,
            new Attributes(),
            new Attributes(),
            new DoubleArray(DataHelper.getRegularArray(fileNLons, is180 ? -180 : 0, 1 / 60.0)));

    // make the dataVariable
    Attributes dAtt = new Attributes();
    // dAtt.set("_CoordinateAxisType", "Height"); //don't treat as axisVariable
    // dAtt.set("_CoordinateZisPositive", "up");
    // dAtt.set("axis", "Z");
    dAtt.set("_FillValue", -9999999);
    dAtt.set(
        "actual_range", new ShortArray(new short[] {-10898, 8271})); // from etopo1_ice_g_i2.hdr
    dAtt.set("coordsys", "geographic");
    dAtt.set("ioos_category", "Location");
    dAtt.set("long_name", "Altitude");
    dAtt.set("missing_value", -9999999);
    dAtt.set("positive", "up");
    dAtt.set("standard_name", "altitude");
    dAtt.set("colorBarMinimum", -8000.0); // .0 makes it a double
    dAtt.set("colorBarMaximum", 8000.0);
    dAtt.set("colorBarPalette", "Topography");
    dAtt.set("units", "m");
    dataVariables = new EDV[1];
    dataVariables[0] = new EDV(datasetID, "altitude", "", dAtt, new Attributes(), "short");
    dataVariables[0].setActualRangeFromDestinationMinMax();

    // ensure the setup is valid
    ensureValid();

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDGridFromEtopo "
              + datasetID
              + " constructor finished. TIME="
              + cTime
              + "ms"
              + (cTime >= 600000 ? "  (>10m!)" : cTime >= 10000 ? "  (>10s!)" : "")
              + "\n");

    // very last thing: saveDimensionValuesInFile
    if (!dimensionValuesInMemory) saveDimensionValuesInFile();
  }

  /**
   * This makes a sibling dataset, based on the new sourceUrl.
   *
   * @throws Throwable always (since this class doesn't support sibling())
   */
  @Override
  public EDDGrid sibling(
      String tLocalSourceUrl, int firstAxisToMatch, int matchAxisNDigits, boolean shareInfo)
      throws Throwable {
    throw new SimpleException("Error: " + "EDDGridFromEtopo doesn't support method=\"sibling\".");
  }

  /**
   * This gets data (not yet standardized) from the data source for this EDDGrid. Because this is
   * called by GridDataAccessor, the request won't be the full user's request, but will be a partial
   * request (for less than EDStatic.partialRequestMaxBytes).
   *
   * @param language the index of the selected language
   * @param tDirTable If EDDGridFromFiles, this MAY be the dirTable, else null.
   * @param tFileTable If EDDGridFromFiles, this MAY be the fileTable, else null.
   * @param tDataVariables EDV[] with just the requested data variables
   * @param tConstraints int[nAxisVariables*3] where av*3+0=startIndex, av*3+1=stride,
   *     av*3+2=stopIndex. AxisVariables are counted left to right, e.g., sst[0=time][1=lat][2=lon].
   * @return a PrimitiveArray[] where the first axisVariables.length elements are the axisValues and
   *     the next tDataVariables.length elements are the dataValues. Both the axisValues and
   *     dataValues are straight from the source, not modified.
   * @throws Throwable if trouble (notably, WaitThenTryAgainException)
   */
  @Override
  public PrimitiveArray[] getSourceData(
      int language, Table tDirTable, Table tFileTable, EDV tDataVariables[], IntArray tConstraints)
      throws Throwable {

    // Currently ETOPO1
    // etopo1_ice_g_i2.bin (grid centered, LSB 16 bit signed integers)
    // 10801 rows by 21601 columns.
    // Data is stored row by row, starting at 90, going down to -90,
    // with lon -180 to 180 on each row (the first and last points on each row are duplicates).
    // The data is grid centered, so the data associated with a given lon,lat represents
    // a cell which extends 1/2 minute N, S, E, and W of the lon, lat.
    // I verified this interpretation with Lynn.

    long eTime = System.currentTimeMillis();
    DoubleArray lats =
        (DoubleArray)
            axisVariables[0]
                .sourceValues()
                .subset(tConstraints.get(0), tConstraints.get(1), tConstraints.get(2));
    DoubleArray lons =
        (DoubleArray)
            axisVariables[1]
                .sourceValues()
                .subset(tConstraints.get(3), tConstraints.get(4), tConstraints.get(5));
    int nLats = lats.size();
    int nLons = lons.size();
    int nLatsLons = nLats * nLons;
    short data[] = new short[nLatsLons];

    PrimitiveArray results[] = new PrimitiveArray[3];
    results[0] = lats;
    results[1] = lons;
    ShortArray sa = new ShortArray(data); // it sets size to data.length
    results[2] = sa;

    // use the cache system?
    if (tConstraints.get(1) > 10
        && tConstraints.get(4) > 10
        && // not compact
        nLats > 20
        && nLats < 600
        && // not tiny or huge
        nLons > 20
        && nLons < 600) {

      // thread-safe creation of etopo grid.
      // If there are almost simultaneous requests for the same one,
      // only one thread will make it.
      // canonical is important for proper functioning of canonicalLock().
      String cacheName =
          String2.canonical(
              cacheDirectory() + String2.replaceAll(tConstraints.toString(), ", ", "_") + ".short");
      ReentrantLock lock = String2.canonicalLock(cacheName);
      if (!lock.tryLock(String2.longTimeoutSeconds, TimeUnit.SECONDS))
        throw new TimeoutException("Timeout waiting for lock on EDDGridFromEtopo cacheName.");
      try {

        // read from cache?
        if (File2.isFile(cacheName)) {
          // this dataset doesn't change, so keep files that are recently used
          File2.touch(cacheName);
          DataInputStream dis = null;
          try {
            dis = new DataInputStream(File2.getDecompressedBufferedInputStream(cacheName));
            for (int i = 0; i < nLatsLons; i++) {
              data[i] = dis.readShort();
              // if (i < 10) String2.log(i + "=" + data[i]);
            }
            dis.close();
            dis = null;
            nReadFromCache++;
            if (verbose)
              String2.log(
                  datasetID
                      + " readFromCache.  time="
                      + (System.currentTimeMillis() - eTime)
                      + "ms");
            return results;
          } catch (Throwable t) {
            if (dis != null) {
              try {
                dis.close();
              } catch (Throwable t2) {
              }
            }
            nFailed++;
            File2.delete(cacheName);
            String2.log(
                String2.ERROR
                    + " while reading "
                    + datasetID
                    + " cache file="
                    + cacheName
                    + "\n"
                    + MustBe.throwableToString(t));
          }
        }

        // get the data
        rawGetSourceData(lons, lats, data);

        // save in cache
        DataOutputStream dos = null;
        int random = Math2.random(Integer.MAX_VALUE);
        try {
          dos =
              new DataOutputStream(
                  new BufferedOutputStream(new FileOutputStream(cacheName + random)));
          for (int i = 0; i < nLatsLons; i++) {
            dos.writeShort(data[i]);
            // if (i < 10) String2.log(i + "=" + data[i]);
          }
          dos.close();
          dos = null;
          File2.rename(cacheName + random, cacheName);
          nWrittenToCache++;
          if (verbose)
            String2.log(
                datasetID + " writeToCache.  totalTime=" + (System.currentTimeMillis() - eTime));
        } catch (Throwable t) {
          if (dos != null) {
            try {
              dos.close();
            } catch (Throwable t2) {
            }
          }
          File2.delete(cacheName + random);
          nFailed++;
          String2.log(
              String2.ERROR
                  + " while writing "
                  + datasetID
                  + " cache file="
                  + cacheName
                  + random
                  + "\n"
                  + MustBe.throwableToString(t));
        }

        return results;
      } finally {
        lock.unlock();
      }

    } else {

      // Don't use cahce system. Coarse source files are small, so gain would be minimal.
      nCoarse++;
      rawGetSourceData(lons, lats, data);
      if (verbose)
        String2.log(
            datasetID + " coarse, not cached.  totalTime=" + (System.currentTimeMillis() - eTime));
      return results;
    }
  }

  /**
   * This is the low level helper for getSourceData. It doesn't cache.
   *
   * @param lons the desired lons
   * @param lats the desired lats
   * @param data will receive the results.
   */
  public void rawGetSourceData(DoubleArray lons, DoubleArray lats, short data[]) throws Throwable {

    // Currently ETOPO1
    // etopo1_ice_g_i2.bin (grid centered, LSB 16 bit signed integers)
    // 10801 rows by 21601 columns.
    // Data is stored row by row, starting at 90, going down to -90,
    // with lon -180 to 180 on each row (the first and last points on each row are duplicates).
    // The data is grid centered, so the data associated with a given lon,lat represents
    // a cell which extends 1/2 minute N, S, E, and W of the lon, lat.
    // I verified this interpretation with Lynn.

    int nLons = lons.size();
    int nLats = lats.size();

    // find the offsets for the start of the rows for the resulting lon values
    int lonOffsets[] = new int[nLons];
    for (int i = 0; i < nLons; i++) {
      double tLon = lons.get(i);
      while (tLon < fileMinLon) tLon += 360;
      while (tLon > fileMaxLon) tLon -= 360;
      // findClosest since may differ by roundoff error
      int closestLon =
          Math2.binaryFindClosest(
              fileLons, tLon); // never any ties, so no need to findFirst or findLast
      // never any ties, so no need to findFirst or findLast
      lonOffsets[i] = bytesPerValue * closestLon;
      // String2.log("tLon=" + tLon + " closestLon=" + closestLon + " offset=" + offsetLon[i]);
    }

    // find the offsets for the start of the columns closest to the desiredLon values
    int latOffsets[] = new int[nLats];
    for (int i = 0; i < nLats; i++) {
      double tLat = lats.get(i);
      while (tLat < fileMinLat) tLat += 90;
      while (tLat > fileMaxLat) tLat -= 90;
      int closestLat =
          Math2.binaryFindClosest(
              fileLats, tLat); // never any ties, so no need to findFirst or findLast
      // adjust lat, since fileLat is ascending, but file stores data top row at start of file
      closestLat = fileNLats - 1 - closestLat;
      latOffsets[i] = bytesPerValue * closestLat * fileNLons;
      // String2.log("tLat=" + tLat + " closestLat=" + closestLat + " offset=" + offsetLat[i]);
    }

    // open the file  (reading is thread safe)
    RandomAccessFile raf = new RandomAccessFile(fileName, "r");
    try {
      // fill data array
      // lat is outer loop because file is lat major
      // and loop is backwards since stored top to bottom
      // (goal is to read basically from start to end of file,
      // but if 0 - 360, lons are read out of order)
      for (int lati = nLats - 1; lati >= 0; lati--) {
        int po = lati * nLons;
        for (int loni = 0; loni < nLons; loni++) {
          raf.seek(latOffsets[lati] + lonOffsets[loni]);
          data[po++] = Short.reverseBytes(raf.readShort());
        }
      }
    } finally {
      raf.close();
    }
  }

  /** This returns the cache statistics String for this dataset. */
  public String statsString() {
    return datasetID
        + ": nCoarse="
        + nCoarse
        + ", nWrittenToCache="
        + nWrittenToCache
        + ", nReadFromCache="
        + nReadFromCache
        + ", nFailed="
        + nFailed;
  }

  /**
   * This returns a fileTable with valid files (or null if unavailable or any trouble). This is a
   * copy of any internal data, so client can modify the contents.
   *
   * @param language the index of the selected language
   * @param nextPath is the partial path (with trailing slash) to be appended onto the local fileDir
   *     (or wherever files are, even url).
   * @return null if trouble, or Object[3] where [0] is a sorted table with file "Name" (String),
   *     "Last modified" (long millis), "Size" (long), and "Description" (String, but usually no
   *     content), [1] is a sorted String[] with the short names of directories that are 1 level
   *     lower, and [2] is the local directory corresponding to this (or null, if not a local dir).
   */
  @Override
  public Object[] accessibleViaFilesFileTable(int language, String nextPath) {
    if (!accessibleViaFiles) return null;
    try {
      // if nextPath is something, there is no such dir
      if (nextPath.length() != 0) return null;

      // return the one file
      Table table = new Table();
      table.addColumn("Name", new StringArray(new String[] {File2.getNameAndExtension(fileName)}));
      table.addColumn(
          "Last modified",
          new LongArray(new long[] {File2.getLastModified(fileName)}).setMaxIsMV(true));
      table.addColumn("Size", new LongArray(new long[] {File2.length(fileName)}).setMaxIsMV(true));
      table.addColumn("Description", new StringArray(new String[] {""}));
      StringArray subDirs = new StringArray();
      return new Object[] {table, subDirs.toStringArray(), null};

    } catch (Exception e) {
      String2.log(MustBe.throwableToString(e));
      return null;
    }
  }

  /**
   * This converts a relativeFileName into a full localFileName (which may be a url).
   *
   * @param language the index of the selected language
   * @param relativeFileName (for most EDDTypes, just offset by fileDir)
   * @return full localFileName or null if any error (including, file isn't in list of valid files
   *     for this dataset)
   */
  @Override
  public String accessibleViaFilesGetLocal(int language, String relativeFileName) {
    if (!accessibleViaFiles) return null;

    if (relativeFileName.equals(File2.getNameAndExtension(fileName))) return fileName;
    return null;
  }
}
