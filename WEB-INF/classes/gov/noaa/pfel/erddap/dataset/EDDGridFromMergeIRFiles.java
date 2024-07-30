/*
 * EDDGridFromMergeIRFiles Copyright 2014, R.Tech.
 * See the LICENSE.txt file in this file's directory.
 */
// JL
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.erddap.variable.*;
import java.io.InputStream;
import java.nio.file.FileSystemException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * This class represents gridded data aggregated from a collection of NCEP/CPC 4km Global (60N -
 * 60S) IR Dataset data files. Project info and file structure:
 * https://www.cpc.ncep.noaa.gov/products/global_precip/html/README Source data files:
 * ftp://disc2.nascom.nasa.gov/data/s4pa/TRMM_ANCILLARY/MERG/ (which works for Bob in a browser but
 * not yet in FileZilla)
 *
 * @author
 */
public class EDDGridFromMergeIRFiles extends EDDGridFromFiles {

  private static final int NLON = 9896;
  private static final int NLAT = 3298;

  private static final double SLON = 0.036378335;
  private static final double SLAT = 0.036383683;

  private static final double SIGMA = 5.67E-8;

  protected static final short IR_MV = (short) 330;
  protected static final double FLUX_MV = 470.0;

  /** The constructor just calls the super constructor. */
  public EDDGridFromMergeIRFiles(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      boolean tAccessibleViaWMS,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      Attributes tAddGlobalAttributes,
      Object[][] tAxisVariables,
      Object[][] tDataVariables,
      int tReloadEveryNMinutes,
      int tUpdateEveryNMillis,
      String tFileDir,
      String tFileNameRegex,
      boolean tRecursive,
      String tPathRegex,
      String tMetadataFrom,
      int tMatchAxisNDigits,
      boolean tFileTableInMemory,
      boolean tAccessibleViaFiles,
      int tnThreads,
      boolean tDimensionValuesInMemory,
      String tCacheFromUrl,
      int tCacheSizeGB,
      String tCachePartialPathRegex)
      throws Throwable {

    super(
        "EDDGridFromMergeIRFiles",
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tAccessibleViaWMS,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddGlobalAttributes,
        tAxisVariables,
        tDataVariables,
        tReloadEveryNMinutes,
        tUpdateEveryNMillis,
        tFileDir,
        tFileNameRegex,
        tRecursive,
        tPathRegex,
        tMetadataFrom,
        tMatchAxisNDigits,
        tFileTableInMemory,
        tAccessibleViaFiles,
        tnThreads,
        tDimensionValuesInMemory,
        tCacheFromUrl,
        tCacheSizeGB,
        tCachePartialPathRegex);

    if (verbose) String2.log("\n*** constructing EDDGridFromMergeIRFiles(xmlReader)...");
  }

  /**
   * This gets sourceGlobalAttributes and sourceDataAttributes from the specified source file.
   *
   * @param tFullName the name of the decompressed data file
   * @param sourceAxisNames If there is a special axis0, this list will be the instances list[1 ...
   *     n-1].
   * @param sourceDataNames the names of the desired source data columns.
   * @param sourceDataTypes the data types of the desired source columns (e.g., "String" or "float")
   * @param sourceGlobalAttributes should be an empty Attributes. It will be populated by this
   *     method
   * @param sourceAxisAttributes should be an array of empty Attributes. It will be populated by
   *     this method
   * @param sourceDataAttributes should be an array of empty Attributes. It will be populated by
   *     this method
   * @throws Throwable if trouble (e.g., invalid file, or a sourceAxisName or sourceDataName not
   *     found). If there is trouble, this doesn't call addBadFile or requestReloadASAP().
   */
  @Override
  public void lowGetSourceMetadata(
      String tFullName,
      StringArray sourceAxisNames,
      StringArray sourceDataNames,
      String sourceDataTypes[],
      Attributes sourceGlobalAttributes,
      Attributes sourceAxisAttributes[],
      Attributes sourceDataAttributes[])
      throws Throwable {

    if (reallyVerbose) String2.log("getSourceMetadata " + tFullName);

    try {
      // globalAtts
      sourceGlobalAttributes.add("Conventions", "COARDS, CF-1.6, ACDD-1.3");
      sourceGlobalAttributes.add("creator_name", "Bob Joyce");
      sourceGlobalAttributes.add("creator_email", "robert.joyce@noaa.gov");
      sourceGlobalAttributes.add("creator_type", "person");
      sourceGlobalAttributes.add("creator_url", "https://www.cpc.ncep.noaa.gov/");
      sourceGlobalAttributes.add(
          "infoUrl", "https://www.cpc.ncep.noaa.gov/products/global_precip/html/README");
      sourceGlobalAttributes.add("institution", "NOAA NWS NCEP CPC");
      sourceGlobalAttributes.add(
          "keywords",
          "4km, brightness, cpc, flux, global, ir, merge, ncep, noaa, nws, temperature");
      sourceGlobalAttributes.add("keywords_vocabulary", "GCMD Science Keywords");
      sourceGlobalAttributes.add(
          "summary",
          "The Climate Prediction Center/NCEP/NWS is now making available\n"
              + "globally-merged (60N-60S) pixel-resolution IR brightness\n"
              + "temperature data (equivalent blackbody temps), merged from all\n"
              + "available geostationary satellites (GOES-8/10, METEOSAT-7/5 and\n"
              + "GMS).  The availability of data from METEOSAT-7, which is\n"
              + "located at 57E at the present time, yields a unique opportunity\n"
              + "for total global (60N-60S) coverage.");
      sourceGlobalAttributes.add("title", "NCEP/CPC 4km Global (60N - 60S) IR Dataset");

      // This is cognizant of special axis0
      for (int avi = 0; avi < sourceAxisNames.size(); avi++) {
        if (reallyVerbose)
          String2.log("axisAttributes for avi=" + avi + " name=" + sourceAxisNames.get(avi));
        switch (sourceAxisNames.get(avi)) {
          case "longitude":
            sourceAxisAttributes[avi].add("axis", "Y");
            sourceAxisAttributes[avi].add("ioos_category", "Location");
            sourceAxisAttributes[avi].add("units", "degrees_east");
            break;
          case "latitude":
            sourceAxisAttributes[avi].add("axis", "X");
            sourceAxisAttributes[avi].add("ioos_category", "Location");
            sourceAxisAttributes[avi].add("units", "degrees_north");
            break;
          case "time":
            sourceAxisAttributes[avi].add("axis", "T");
            sourceAxisAttributes[avi].add("delta_t", "0000-00-00 00:30:00");
            sourceAxisAttributes[avi].add("long_name", "Time");
            sourceAxisAttributes[avi].add("standard_name", "time");
            sourceAxisAttributes[avi].add("units", "seconds since 1970-01-01T00:00:00Z");
            break;
          default:
            sourceAxisAttributes[avi].add(
                "units", "count"); // "count" is udunits;  "index" isn't, but better?
            break;
        }
      }
      for (int avi = 0; avi < sourceDataNames.size(); avi++) {
        if (reallyVerbose)
          String2.log("dataAttributes for avi=" + avi + " name=" + sourceDataNames.get(avi));
        switch (sourceDataNames.get(avi)) {
          case "ir":
            sourceDataAttributes[avi].add("colorBarMinimum", 170);
            sourceDataAttributes[avi].add("colorBarMaximum", 330);
            sourceDataAttributes[avi].add("ioos_category", "Heat Flux");
            sourceDataAttributes[avi].add("long_name", "IR Brightness Temperature");
            sourceDataAttributes[avi].add("missing_value", IR_MV);
            sourceDataAttributes[avi].add("standard_name", "brightness_temperature");
            sourceDataAttributes[avi].add("units", "degreeK");
            break;
          case "flux":
            sourceDataAttributes[avi].add("colorBarMinimum", 0.0);
            sourceDataAttributes[avi].add("colorBarMaximum", 500.0);
            sourceDataAttributes[avi].add("ioos_category", "Heat Flux");
            sourceDataAttributes[avi].add("long_name", "Flux");
            sourceDataAttributes[avi].add("missing_value", FLUX_MV);
            sourceDataAttributes[avi].add("standard_name", "surface_upwelling_shortwave_flux");
            sourceDataAttributes[avi].add("units", "W/m^2");
            break;
          default:
            sourceAxisAttributes[avi].add("ioos_category", "Unknown");
            break;
        }
      }

    } catch (Throwable t) {
      throw new RuntimeException(
          "Error in EDDGridFromMergeIRFiles.getSourceMetadata"
              + "\nfrom "
              + tFullName
              + "\nCause: "
              + MustBe.throwableToShortString(t),
          t);
    }
  }

  /**
   * This gets source axis values from one file.
   *
   * @param tFullName the name of the decompressed data file
   * @param sourceAxisNames the names of the desired source axis variables. If there is a special
   *     axis0, this will not include axis0's name.
   * @param sourceDataNames When there are unnamed dimensions, this is to find out the shape of the
   *     variable to make index values 0, 1, size-1.
   * @return a PrimitiveArray[] with the results (with the requested sourceDataTypes). It needn't
   *     set sourceGlobalAttributes or sourceDataAttributes (but see getSourceMetadata).
   * @throws Throwable if trouble (e.g., invalid file). If there is trouble, this doesn't call
   *     addBadFile or requestReloadASAP().
   */
  @Override
  public PrimitiveArray[] lowGetSourceAxisValues(
      String tFullName, StringArray sourceAxisNames, StringArray sourceDataNames) throws Throwable {

    String getWhat = "";

    try {
      PrimitiveArray[] avPa = new PrimitiveArray[sourceAxisNames.size()];

      for (int avi = 0; avi < sourceAxisNames.size(); avi++) {
        String avName = sourceAxisNames.get(avi);
        getWhat = "axisValues for variable=" + avName;

        switch (avName) {
          case "longitude":
            FloatArray lonBounds = new FloatArray(NLON, true);
            float lon = 0.0182f;
            for (int i = 0; i < NLON; i++) {
              lonBounds.setFloat(i, lon);
              lon = (float) (lon + SLON);
            }
            avPa[avi] = lonBounds;
            break;
          case "latitude":
            FloatArray latBounds = new FloatArray(NLAT, true);
            float lat = -59.982f;
            for (int i = 0; i < NLAT; i++) {
              latBounds.setFloat(i, lat);
              lat = (float) (lat + SLAT);
            }
            avPa[avi] = latBounds;
            break;
          case "time":
            {
              if (reallyVerbose) String2.log("case time with " + tFullName);
              String sdate =
                  File2.getNameNoExtension(tFullName)
                      .replaceAll("merg_", "")
                      .replaceAll("_4km-pixel", "");
              if (reallyVerbose) String2.log("date = " + sdate + " (" + sdate.length() + ")");

              if (sdate.length() != 10)
                throw new RuntimeException(
                    "File name (" + tFullName + ") must contain a date encoded as 10 characters.");

              // format fdjksdfljk_2014010102
              String year = sdate.substring(0, 4);
              String month = sdate.substring(4, 6);
              String day = sdate.substring(6, 8);
              String hour = sdate.substring(8, 10);

              java.text.SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
              sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
              String fileTime = month + "/" + day + "/" + year + " " + hour + ":00:00";
              Date date = sdf.parse(fileTime);

              // calculate bounds
              long d0 = date.getTime() / 1000;
              long d1 = d0 + 1800;

              // log
              getWhat +=
                  "fileTime = "
                      + fileTime
                      + " --> date = \""
                      + date.toString()
                      + "\" (d0="
                      + d0
                      + ", d1="
                      + d1
                      + ")";
              if (reallyVerbose) String2.log(getWhat);

              // set result
              DoubleArray ret = new DoubleArray(2, true);
              ret.set(0, d0);
              ret.set(1, d1);

              avPa[avi] = ret;
            }
            break;
          default:
            throw new Exception("case : " + avName);
        }
      }
      return avPa;

    } catch (Throwable t) {
      throw new RuntimeException(
          "Error in EDDGridFromMergeIRFiles.getSourceAxisValues"
              + "\nwhile getting "
              + getWhat
              + "\nfrom "
              + tFullName
              + "\nCause: "
              + MustBe.throwableToShortString(t),
          t);
    }
  }

  /**
   * This gets source data from one file.
   *
   * @param tFullName the name of the decompressed data file
   * @param tDataVariables the desired data variables
   * @param tConstraints For each axis variable, there will be 3 numbers (startIndex, stride,
   *     stopIndex). !!! If there is a special axis0, this will not include constraints for axis0.
   * @return a PrimitiveArray[] with an element for each tDataVariable with the dataValues. <br>
   *     The dataValues are straight from the source, not modified. <br>
   *     The primitiveArray dataTypes are usually the sourceDataPAType, but can be any type.
   *     EDDGridFromFiles will convert to the sourceDataPAType. <br>
   *     Note the lack of axisVariable values!
   * @throws Throwable if trouble (notably, WaitThenTryAgainException). If there is trouble, this
   *     doesn't call addBadFile or requestReloadASAP().
   */
  @Override
  public PrimitiveArray[] lowGetSourceDataFromFile(
      String tFullName, EDV tDataVariables[], IntArray tConstraints) throws Throwable {

    if (verbose)
      String2.log(
          "getSourceDataFromFile("
              + tFullName
              + ", ["
              + String2.toCSSVString(tDataVariables)
              + "], "
              + tConstraints
              + ")");

    // make the selection spec  and get the axis values
    int nbAxisVariable = axisVariables.length;
    int nbDataVariable = tDataVariables.length;
    PrimitiveArray[] paa = new PrimitiveArray[nbDataVariable];
    StringBuilder selectionSB = new StringBuilder();

    int minTime = 0, maxTime = 0, strideTime = 0;
    int minLat = 0, maxLat = 0, strideLat = 0;
    int minLon = 0, maxLon = 0, strideLon = 0;

    for (int avi = 0; avi < nbAxisVariable; avi++) {
      switch (axisVariables[avi].sourceName()) {
        case "latitude":
          minLat = tConstraints.get(avi * 3);
          strideLat = tConstraints.get(avi * 3 + 1);
          maxLat = tConstraints.get(avi * 3 + 2);
          break;
        case "longitude":
          minLon = tConstraints.get(avi * 3);
          strideLon = tConstraints.get(avi * 3 + 1);
          maxLon = tConstraints.get(avi * 3 + 2);
          break;
        case "time":
          minTime = tConstraints.get(avi * 3);
          strideTime = tConstraints.get(avi * 3 + 1);
          maxTime = tConstraints.get(avi * 3 + 2);
          break;
      }
      selectionSB.append(
          (avi == 0 ? "" : ",")
              + axisVariables[avi].sourceName()
              + "{"
              + tConstraints.get(avi * 3)
              + ":"
              + tConstraints.get(avi * 3 + 1)
              + ":"
              + tConstraints.get(avi * 3 + 2)
              + "}"); // start:stop:stride !
    }
    String selection = selectionSB.toString();

    int nbLat = DataHelper.strideWillFind(maxLat - minLat + 1, strideLat);
    int nbLon = DataHelper.strideWillFind(maxLon - minLon + 1, strideLon);
    int nbTime = DataHelper.strideWillFind(maxTime - minTime + 1, strideTime);
    int total = nbLat * nbLon * nbTime; // data length

    if (reallyVerbose)
      String2.log(
          "constraints : "
              + selection
              + "\nnb: lat="
              + nbLat
              + ", lon="
              + nbLon
              + ", time="
              + nbTime
              + ", total size="
              + total);
    int indexOut = 0; // index in data array

    InputStream inStream =
        File2.getDecompressedBufferedInputStream(tFullName); // may throw exception

    try {

      byte[] in = new byte[NLON * NLAT * 2];
      short[] out1 = new short[total];
      double[] out2 = new double[total];

      // entire read of the file
      long t0 = System.currentTimeMillis();
      int indexIn = 0;
      byte[] buff = new byte[NLAT];
      int n = -2;
      while ((n = inStream.read(buff, 0, buff.length)) != -1) {
        System.arraycopy(buff, 0, in, indexIn, n);
        indexIn += n;
      }
      if (indexIn != in.length) {
        throw new FileSystemException(
            "Merge file seems to be corrupted because size is : "
                + indexIn
                + " and should be "
                + in.length);
      }
      if (verbose) String2.log("read file in " + (System.currentTimeMillis() - t0) + "ms");
      if (reallyVerbose) String2.logNoNewline("Closing file...");
      inStream.close(); // I care about this exception
      inStream = null; // indicate it closed successfully
      if (reallyVerbose) String2.log("Done");

      if (reallyVerbose) String2.logNoNewline("Copy filtered data...");
      t0 = System.currentTimeMillis();
      for (int t = minTime; t <= maxTime; t += strideTime) { // [0 - 1]
        int offsetTime = NLON * NLAT * t;
        for (int la = minLat; la <= maxLat; la += strideLat) {
          int offsetLat = NLON * la;
          for (int lo = minLon; lo <= maxLon; lo += strideLon) {
            short value = (short) (in[offsetTime + offsetLat + lo] & 0xff);
            value = (short) (value + 75);
            out1[indexOut] = value;
            out2[indexOut] = ((double) Math.round(T2F(value) * 10.)) / 10.;
            indexOut++;
          }
        }
      }
      if (reallyVerbose) {
        String2.log("Done in " + (System.currentTimeMillis() - t0) + "ms");
        if (total >= 10) {
          String2.log("Log the 10 last values :");
          int i = 0;
          while (i != 10) {
            int j = total - 1 - i;
            String2.log("\tT[" + j + "] = " + out1[j] + "F[" + j + "] = " + out2[j]);
            i++;
          }
        }
      }

      in = null;
      if (nbDataVariable == 2) {
        paa[0] = PrimitiveArray.factory(out1);
        paa[1] = PrimitiveArray.factory(out2);
      } else if (nbDataVariable == 1) {
        if (tDataVariables[0].sourceName().equalsIgnoreCase("ir")) {
          paa[0] = PrimitiveArray.factory(out1);
        } else {
          paa[0] = PrimitiveArray.factory(out2);
        }
      } // else 0

    } catch (Throwable t) {
      // make sure it is explicitly closed
      if (inStream != null) {
        try {
          inStream.close();
        } catch (Throwable t2) {
          if (verbose)
            String2.log("2nd attempt to close also failed:\n" + MustBe.throwableToShortString(t2));
        }
      }
      if (verbose) String2.log("Error while reading " + tFullName);
      throw t;
    }
    return paa;
  }

  private static double T2F(double pT) {
    // relation to go from brightness temperature to flux
    // return (T-162.)*(300./(330.-162.));
    // return T;
    // return SIGMA*T*T*T*T;
    // return (T-120)*(325.0/(330-75.0));
    // return (T-140)*(400.0/(330-75.0));
    boolean model2007 = true;
    if (model2007) {
      double t2 = pT - 40.;
      return SIGMA * t2 * t2 * t2 * t2 + 69.;
    }
    return (pT - 160) * (500.0 / (330 - 75.0));
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
    throw new SimpleException("Error: EDDGridFromMergeIRFiles doesn't support method=\"sibling\".");
  }

  /**
   * This does its best to generate a clean, ready-to-use datasets.xml entry for an
   * EDDGridFromMergeIRFiles dataset. The XML can then be edited by hand and added to the
   * datasets.xml file.
   *
   * <p>This can't be made into a web service because it would allow any user to look at (possibly)
   * private files on the server.
   *
   * @param tFileDir the starting (parent) directory for searching for files
   * @param tFileNameRegex the regex that each filename (no directory info) must match (e.g.,
   *     ".*\\.gz") (usually only 1 backslash; 2 here since it is Java code).
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble. If no trouble, then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      String tFileDir, String tFileNameRegex, int tReloadEveryNMinutes, String tCacheFromUrl)
      throws Throwable {

    String2.log(
        "\n*** EDDGridFromMergeIRFiles.generateDatasetsXml"
            + "\nfileDir="
            + tFileDir
            + " fileNameRegex="
            + tFileNameRegex
            + " reloadEveryNMinutes="
            + tReloadEveryNMinutes);
    if (!String2.isSomething(tFileDir))
      throw new IllegalArgumentException("fileDir wasn't specified.");
    tFileDir = File2.addSlash(tFileDir); // ensure it has trailing slash
    tFileNameRegex = String2.isSomething(tFileNameRegex) ? tFileNameRegex.trim() : ".*";
    if (String2.isRemote(tCacheFromUrl))
      FileVisitorDNLS.sync(
          tCacheFromUrl, tFileDir, tFileNameRegex, true, ".*", false); // not fullSync
    if (tReloadEveryNMinutes < 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
      tReloadEveryNMinutes = 1440; // daily. More often than usual default.

    StringBuilder sb = new StringBuilder();
    // gather the results
    String tDatasetID = "mergeIR";

    // ???
    // sb.append( "<!-- NOTE! The source for " + tDatasetID + " has nGridVariables=" + 2 +".
    // -->\n");

    sb.append(
        "<dataset type=\"EDDGridFromMergeIRFiles\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n"
            + (String2.isUrl(tCacheFromUrl)
                ? "    <cacheFromUrl>" + XML.encodeAsXML(tCacheFromUrl) + "</cacheFromUrl>\n"
                : "    <updateEveryNMillis>"
                    + suggestUpdateEveryNMillis(tFileDir)
                    + "</updateEveryNMillis>\n")
            + "    <fileDir>"
            + XML.encodeAsXML(tFileDir)
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + XML.encodeAsXML(tFileNameRegex)
            + "</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n");

    sb.append(
        "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Grid</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_name\">Bob Joyce</att>\n"
            + "        <att name=\"creator_email\">robert.joyce@noaa.gov</att>\n"
            + "        <att name=\"creator_url\">https://www.cpc.ncep.noaa.gov/</att>\n"
            + "        <att name=\"drawLandMask\">under</att>\n"
            + "        <att name=\"infoUrl\">https://www.cpc.ncep.noaa.gov/products/global_precip/html/README</att>\n"
            + "        <att name=\"institution\">NOAA NWS NCEP CPC</att>\n"
            + "        <att name=\"keywords\">4km, brightness, cpc, flux, global, ir, merge, ncep, noaa, nws, temperature</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"summary\">"
            + "The Climate Prediction Center/NCEP/NWS is now making available\n"
            + "globally-merged (60N-60S) pixel-resolution IR brightness\n"
            + "temperature data (equivalent blackbody temps), merged from all\n"
            + "available geostationary satellites (GOES-8/10, METEOSAT-7/5 and\n"
            + "GMS).  The availability of data from METEOSAT-7, which is\n"
            + "located at 57E at the present time, yields a unique opportunity\n"
            + "for total global (60N-60S) coverage.</att>\n"
            + "        <att name=\"title\">NCEP/CPC 4km Global (60N - 60S) IR Dataset</att>\n"
            + "    </addAttributes>\n");

    sb.append(
        "    <axisVariable>\n"
            + "        <sourceName>time</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"axis\">T</att>\n"
            + "            <att name=\"delta_t\">0000-00-00 00:30:00</att>\n"
            + "            <att name=\"long_name\">Time</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>latitude</sourceName>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"units\">"
            + EDV.LAT_UNITS
            + "</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n"
            + "    <axisVariable>\n"
            + "        <sourceName>longitude</sourceName>\n"
            + "        <addAttributes>\n"
            + "            <att name=\"units\">"
            + EDV.LON_UNITS
            + "</att>\n"
            + "        </addAttributes>\n"
            + "    </axisVariable>\n");

    sb.append(
        "    <dataVariable>\n"
            + "        <sourceName>ir</sourceName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"int\">170</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"int\">330</att>\n"
            + "            <att name=\"ioos_cateory\">Heat Flux</att>\n"
            + "            <att name=\"long_name\">IR Brightness Temperature</att>\n"
            + "            <att name=\"missing_value\" type=\"short\">"
            + IR_MV
            + "</att>\n"
            + "            <att name=\"standard_name\">brightness_temperature</att>\n"
            + "            <att name=\"units\">degreeK</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>flux</sourceName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">500.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"ioos_cateory\">Heat Flux</att>\n"
            + "            <att name=\"long_name\">Flux</att>\n"
            + "            <att name=\"missing_value\" type=\"double\">"
            + FLUX_MV
            + "</att>\n"
            + "            <att name=\"standard_name\">surface_upwelling_shortwave_flux</att>\n"
            + "            <att name=\"units\">W/m^2</att>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n");

    sb.append("</dataset>\n" + "\n");

    return sb.toString();
  }
}
