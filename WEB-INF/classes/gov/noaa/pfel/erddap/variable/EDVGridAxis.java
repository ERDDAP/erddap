/*
 * EDVGridAxis Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;

/**
 * This class (or a subclass like EDVTimeGridAxis) holds information about a grid axis variable.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
public class EDVGridAxis extends EDV {

  protected String parentDatasetID;
  private PrimitiveArray sourceValues;
  protected boolean isAscending = false; // for the sourceValues (dest may be flipped)
  protected boolean isEvenlySpaced = false;
  protected double averageSpacing = Double.NaN;

  /**
   * The destination coarse minimum and maximum values (in standardized destination units) of this
   * variable. destinationCoarseMin/Max defines the slightly larger range (by averageSpacing/2 on
   * each end) of valid requests.
   */
  protected double destinationCoarseMin = Double.NaN;

  protected double destinationCoarseMax = Double.NaN;

  /**
   * The constructor. The "units" attribute must be in tSourceAttributes or tAddAttributes.
   *
   * <p>Call setActualRangeFromDestinationMinMax() sometime after this returns.
   *
   * @param tParentDatasetID This is needed if dimensionValuesInMemory is false, so sourceValues
   *     sometimes need to be read from [cacheDirectory(tParentDatasetID)]/dimensionSourceValues.nc
   * @param tSourceName the name of the axis variable in the dataset source (usually with no
   *     spaces). Currently, this doesn't support fixedValue-style names.
   * @param tDestinationName is the name to be used in the results. If null or "", tSourceName will
   *     be used.
   * @param tSourceAttributes are the attributes for the variable in the source
   * @param tAddAttributes the attributes which will be added when data is extracted and which have
   *     precedence over sourceAttributes. Special case: value="null" causes that item to be removed
   *     from combinedAttributes. If this is null, an empty addAttributes will be created.
   * @param tSourceValues has the values from the source. This can't be a StringArray. There must be
   *     at least one element. They must be sorted in ascending (recommended) or descending order.
   *     Unsorted is not allowed. There can't be any missing values (or NaN).
   * @throws Throwable if trouble
   */
  public EDVGridAxis(
      String tParentDatasetID,
      String tSourceName,
      String tDestinationName,
      Attributes tSourceAttributes,
      Attributes tAddAttributes,
      PrimitiveArray tSourceValues)
      throws Throwable {

    super(
        tParentDatasetID,
        tSourceName,
        tDestinationName,
        tSourceAttributes,
        tAddAttributes,
        tSourceValues.elementTypeString(),
        new PAOne(tSourceValues, 0).min(new PAOne(tSourceValues, tSourceValues.size() - 1)),
        new PAOne(tSourceValues, 0).max(new PAOne(tSourceValues, tSourceValues.size() - 1)));

    parentDatasetID = tParentDatasetID;
    sourceValues = tSourceValues; // but continue to work with stable tSourceValues
    setActualRangeFromDestinationMinMax();

    // test if ascending
    // Note that e.g., altitude might be flipped, so destination might be descending. That's ok.
    String error = tSourceValues.isAscending();
    if (verbose && error.length() > 0) String2.log("  " + destinationName + ": " + error);
    isAscending = error.length() == 0;

    // if !isAscending, test that it is descending sorted
    if (!isAscending) {
      String error2 = tSourceValues.isDescending();
      if (error2.length() > 0)
        throw new RuntimeException(
            "AxisVariable=" + destinationName + " isn't sorted.  " + error + "  " + error2);
    }

    // test for ties (after isAscending and isDescending)
    StringBuilder sb = new StringBuilder();
    if (tSourceValues.removeDuplicates(false, sb) > 0)
      throw new RuntimeException(
          "AxisVariable=" + destinationName + " has tied values:\n" + sb.toString());

    // test if evenly spaced
    resetIsEvenlySpaced();

    initializeAverageSpacingAndCoarseMinMax();
  }

  /** This resets isEvenlySpaced. */
  public void resetIsEvenlySpaced() {
    PrimitiveArray tSourceValues = sourceValues(); // work with stable local reference
    String error = tSourceValues.isEvenlySpaced();
    if (verbose && error.length() > 0)
      String2.log(
          "  " + destinationName + ": " + error + "\n" + tSourceValues.smallestBiggestSpacing());
    isEvenlySpaced = error.length() == 0;
  }

  /**
   * Some constructors call this to set destinationCoarseMin/Max based on destinationMin/Max and
   * averageSpacing.
   */
  public void initializeAverageSpacingAndCoarseMinMax() {
    PrimitiveArray tSourceValues = sourceValues(); // work with stable local reference
    int n = tSourceValues.size();
    double rough;
    if (n >= 2) { // averageSpacing may be negative (if axis is high to low)
      averageSpacing = (lastDestinationValue() - firstDestinationValue()) / (n - 1);
      rough = Math.abs(averageSpacing) / 2;
    } else {
      // avoid single value e.g., .01, fails to match .01000000001
      rough = Math.max(Math.abs(destinationMinDouble()) / 100, 0.01); // very arbitrary
    }
    destinationCoarseMin = destinationMinDouble() - rough;
    destinationCoarseMax = destinationMaxDouble() + rough;
  }

  /**
   * This overwrites EDV superclass method to use firstDestinationValue and lastDestinationValue.
   * This is now defined in CF-1.7, with unpacked values, smallest and largest.
   */
  @Override
  public void setActualRangeFromDestinationMinMax() {

    // actual_range is useful information for .das and will be replaced by actual_range of data
    // subset.
    combinedAttributes.remove("actual_min");
    combinedAttributes.remove("actual_max");
    combinedAttributes.remove("data_min");
    combinedAttributes.remove("data_max");
    PrimitiveArray pa = PrimitiveArray.factory(destinationDataPAType(), 2, false);
    pa.addDouble(Math.min(firstDestinationValue(), lastDestinationValue()));
    pa.addDouble(Math.max(firstDestinationValue(), lastDestinationValue()));
    combinedAttributes.set("actual_range", pa);
  }

  /**
   * This returns a string representation of this EDVGridAxis.
   *
   * @return a string representation of this EDVGridAxis.
   */
  @Override
  public String toString() {
    PrimitiveArray tSourceValues = sourceValues(); // work with stable local reference
    return "EDVGridAxis/"
        + super.toString()
        + // has trailing newline
        "  nValues="
        + tSourceValues.size()
        + "\n  isAscending="
        + isAscending
        + "\n  isEvenlySpaced="
        + isEvenlySpaced
        + "\n  averageSpacing="
        + averageSpacing
        + "\n  destinationCoarseMin="
        + destinationCoarseMin
        + "\n  destinationCoarseMax="
        + destinationCoarseMax
        + "\n";
    // show sourceValues?
  }

  /**
   * This is used by the EDVGridAxis constructor to determine if this EDV is valid.
   *
   * @param errorInMethod the start string for an error message
   * @throws Throwable if this EDV is not valid
   */
  @Override
  public void ensureValid(String errorInMethod) throws Throwable {
    super.ensureValid(errorInMethod);
    PrimitiveArray tSourceValues = sourceValues(); // work with stable local reference
    Test.ensureTrue(
        String2.isSomething(parentDatasetID),
        errorInMethod + "'parentDatasetID' wasn't specified.");
    Test.ensureTrue(
        tSourceValues != null && tSourceValues.size() > 0,
        errorInMethod + "'sourceValues' is null or has 0 values.");
    // ensure no null values???
  }

  /**
   * This is used by the EDVGridAxis constructor to determine if this EDVGridAxis is valid.
   *
   * @throws Throwable if this EDVGridAxis is not valid
   */
  public void ensureValid() throws Throwable {
    String errorInMethod =
        "datasets.xml/EDVGridAxis.ensureValid error for sourceName=" + sourceName + ":\n";
    ensureValid(errorInMethod);
  }

  /**
   * This returns the PrimitiveArray with the values for this axis as stored in the source. Don't
   * change these values.
   */
  public PrimitiveArray sourceValues() {
    PrimitiveArray tSourceValues = sourceValues; // get stable reference, as is (may be null)
    if (tSourceValues != null) return tSourceValues;

    // <dimensionValuesInMemory> is false, so read from file
    if (debugMode)
      String2.log(
          ">> Reading stored dimension sourceValues for datasetID="
              + parentDatasetID
              + " variable="
              + destinationName);
    StringArray varsRead = new StringArray();
    try {
      PrimitiveArray pas[] =
          NcHelper.readPAsInNc3(
              EDD.datasetDir(parentDatasetID) + EDD.DIMENSION_VALUES_FILENAME,
              new String[] {destinationName},
              varsRead);
      if (varsRead.size() != 1 || !varsRead.get(0).equals(destinationName))
        throw new RuntimeException(String2.ERROR + ": unexpected varsRead=" + varsRead.toString());
      sourceValues = pas[0];
      return pas[0];
    } catch (Exception e) {
      String2.log(e.toString());
      throw new RuntimeException(
          "Couldn't read stored dimension sourceValues for datasetID="
              + parentDatasetID
              + " variable="
              + destinationName);
    }
  }

  /**
   * When the dataset's dimensionValuesInMemory=false, the dataset calls this to set the
   * sourceValues to null. Currently, this ignores the request if sourceValues.length < 100.
   */
  public void setSourceValuesToNull() {
    PrimitiveArray tSourceValues =
        sourceValues; // get stable reference, as is (may already be null)
    if (tSourceValues != null && tSourceValues.size() >= 100) sourceValues = null;
  }

  /**
   * This returns the PrimitiveArray with the destination values for this axis. Don't change these
   * values. This returns the sourceValues (with scaleFactor and addOffset if active; alt is
   * special; time is special). This doesn't change the order of the values (even if source is depth
   * and dest is altitude).
   */
  public PrimitiveArray destinationValues() {
    // alt and time may modify the values, so use sourceValues.clone()
    return toDestination((PrimitiveArray) sourceValues().clone());
  }

  /**
   * This returns one of the destination values for this axis (with scaleFactor and addOffset if
   * active; alt is special; time is special). This returns a number. This relies on alt and time
   * overriding toDestination().
   */
  public PrimitiveArray destinationValue(int which) {
    PrimitiveArray sourceVal = PrimitiveArray.factory(destinationDataPAType, 1, false);

    sourceVal.addDouble(sourceValues().getNiceDouble(which));
    return toDestination(sourceVal);
  }

  /**
   * This returns one of this axis' source values as a nice double destination value.
   * EDVTimeStampGridAxis subclass overwrites this.
   */
  public double destinationDouble(int which) {
    PrimitiveArray tSourceValues = sourceValues(); // work with stable local reference
    if (scaleAddOffset) {
      double d = tSourceValues.getNiceDouble(which) * scaleFactor + addOffset;
      if (destinationDataPAType == PAType.DOUBLE) return d;
      if (destinationDataPAType == PAType.FLOAT) return Math2.doubleToFloatNaN(d);
      // int type
      return Math2.roundToInt(d);
    } else {
      return tSourceValues.getDouble(which);
    }
  }

  /**
   * This returns one of this axis' source values as a nice String destination value. For most
   * EDVGridAxis, this returns destinationValues (which equal the String destination values). The
   * Time subclass overwrites this.
   */
  public String destinationString(int which) {
    PrimitiveArray tSourceValues = sourceValues(); // work with stable local reference
    if (scaleAddOffset) {
      double d = tSourceValues.getNiceDouble(which) * scaleFactor + addOffset;
      if (destinationDataPAType == PAType.DOUBLE) return "" + d;
      if (destinationDataPAType == PAType.FLOAT) return "" + Math2.doubleToFloatNaN(d);
      // int type
      return "" + Math2.roundToInt(d);
    } else {
      return tSourceValues.getString(which);
    }
  }

  /**
   * This returns the PrimitiveArray with the destination values for this axis which will return
   * nice Strings if you call pa.getString(i). Don't change these values. For most EDVGridAxis, this
   * returns destinationValues (which equal the String destination values). The Time subclass
   * overwrites this. !!!For time, if lots of values (e.g., 10^6), this is SLOW (e.g., 30
   * seconds)!!!
   */
  public PrimitiveArray destinationStringValues() {
    return destinationValues();
  }

  /**
   * This returns a JSON-style csv String with a subset of destinationStringValues suitable for use
   * on a slider with SLIDER_PIXELS. This overwrites the superclass version so that it just presents
   * valid values.
   *
   * <p>Because there are always numbers for EDVGridAxes, this always returns a valid list.
   * <strong>If the values range from high to low, this returns a high to low list.
   */
  @Override
  public String sliderCsvValues() throws Throwable {
    byte bar[] = sliderCsvValues; // local pointer to avoid concurrency problems
    if (bar != null) return String2.utf8BytesToString(bar);

    // one time: generate the sliderCsvValues
    try {
      long eTime = System.currentTimeMillis();
      PrimitiveArray tSourceValues = sourceValues(); // work with stable local reference
      int nSourceValues = tSourceValues.size();
      boolean isTimeStamp = this instanceof EDVTimeStampGridAxis;
      IntArray sliderIndices = new IntArray();
      sliderIndices.add(0); // add first index

      if (nSourceValues <= SLIDER_MAX_NVALUES) {
        for (int i = 1; i < nSourceValues; i++) sliderIndices.add(i);

      } else if (isTimeStamp) {
        // make evenly spaced nice numbers (like EDV.sliderCsvValues()),
        //  then find closest actual values.
        // Dealing with indices (later sorted) works regardless of isAscending.
        double values[] =
            Calendar2.getNEvenlySpaced(
                destinationMinDouble(), destinationMaxDouble(), SLIDER_MAX_NVALUES);
        for (int i = 0; i < values.length; i++)
          sliderIndices.add(destinationToClosestIndex(values[i]));

        // add last index
        sliderIndices.add(nSourceValues - 1);

      } else {
        // make evenly spaced nice numbers (like EDV.sliderCsvValues()),
        //  then find closest actual values.
        // Work from destMin to destMax.
        //  Dealing with indices (later sorted) works regardless of isAscending.
        double stride =
            Math2.suggestMaxDivisions(
                destinationMaxDouble() - destinationMinDouble(), SLIDER_MAX_NVALUES);
        int nDiv =
            Math2.roundToInt(Math.abs((destinationMaxDouble() - destinationMinDouble()) / stride));
        double base = Math.floor(destinationMinDouble() / stride) * stride;
        for (int i = 0; i < nDiv; i++)
          sliderIndices.add(destinationToClosestIndex(base + i * stride));

        // add last index
        sliderIndices.add(nSourceValues - 1);
      }

      // sort and remove duplicates
      // sorting indices means: if axis is high->low, values will be in that order
      sliderIndices.sort();
      sliderIndices.removeDuplicates();

      // convert to csv string
      int nValues = sliderIndices.size();
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < nValues; i++) {
        if (i > 0) sb.append(", ");
        sb.append(toSliderString(destinationString(sliderIndices.get(i)), isTimeStamp));
      }

      // store in compact utf8 format
      String csv = sb.toString();
      // String2.log(">>EDVGridAxis.sliderCsvValues nSourceValues=" +
      //    nSourceValues + " nSliderValues=" + nValues +
      //    " time=" + (System.currentTimeMillis() - eTime) + "ms");
      sliderCsvValues = String2.stringToUtf8Bytes(csv); // do last
      return csv;
    } catch (Throwable t) {
      EDStatic.rethrowClientAbortException(t); // first thing in catch{}
      String2.log(MustBe.throwableToString(t));
      return null;
    }
  }

  /** This sets sliderCsvValues to null so it will be recreated the next time it is needed. */
  public void clearSliderCsvValues() {
    sliderCsvValues = null;
  }

  /**
   * This converts a value to the nearest slider position (0 .. EDV.SLIDER_PIXELS-1). Out-of-range
   * values (even far off) are converted to nearest, but NaN -> -1. If only one destination value,
   * this returns 0.
   *
   * <p>For EDVGridAxis (which overwrites the superclass version), this works whether isAscending or
   * not.
   *
   * @param destinationValue
   * @return the nearest slider position (0 .. EDV.SLIDER_PIXELS-1) (or -1 if trouble, e.g.,
   *     sliderCsvValues can't be constructed (e.g., no min + max values)).
   */
  @Override
  public int closestSliderPosition(double destinationValue) {
    int index = destinationToClosestIndex(destinationValue);
    if (index == -1) return index;

    // it's a valid index
    int safeSourceSize1 = Math.max(1, sourceValues().size() - 1);
    return Math2.roundToInt((index * (EDV.SLIDER_PIXELS - 1.0)) / safeSourceSize1);
  }

  /**
   * This converts a destination double value to a string (time variable overwrite this to make an
   * iso string). NaN returns "";
   *
   * @param destD
   * @return destination String
   */
  public String destinationToString(double destD) {
    if (Double.isNaN(destD)) return "";
    // destinationDataPAType won't be PAType.STRING
    if (destinationDataPAType == PAType.DOUBLE) return "" + destD;
    if (destinationDataPAType == PAType.FLOAT) return "" + (float) destD;
    return "" + Math.rint(destD); // ints are nicer without trailing ".0"
  }

  /**
   * This converts a destination String value to a destination double (time variable overwrites this
   * to catch iso 8601 strings). "" or null returns NaN.
   *
   * @param destS
   * @return destination double
   */
  public double destinationToDouble(String destS) {
    return String2.parseDouble(destS);
  }

  /** This returns the nice double representation of the first destination value for this axis. */
  public double firstDestinationValue() {
    return destinationValue(0).getNiceDouble(0);
  }

  /** This returns the nice double representation of the last destination value for this axis. */
  public double lastDestinationValue() {
    return destinationValue(sourceValues().size() - 1).getNiceDouble(0);
  }

  /**
   * This returns the destinationCoarseMin value (in standardized units) for this axis (e.g.,
   * altitude values are in meters, positive=up and time values are in seconds since
   * 1970-01-01T00:00:00Z). destinationCoarseMin/Max defines the slightly larger range of valid
   * requests.
   *
   * @return the cleaned up destinationCoarseMin value for this axis.
   */
  public double destinationCoarseMin() {
    return destinationCoarseMin;
  }

  /**
   * This returns the destinationCoarseMax value (in standardized units) for this axis (e.g.,
   * altitude values are in meters, positive=up and time values are in seconds since 1970-01-01).
   * destinationCoarseMin/Max defines the slightly larger range of valid requests.
   *
   * @return the cleaned up destinationCoarseMax value for this axis.
   */
  public double destinationCoarseMax() {
    return destinationCoarseMax;
  }

  public void setDestinationCoarseMin(double tMin) {
    destinationCoarseMin = tMin;
  }

  public void setDestinationCoarseMax(double tMax) {
    destinationCoarseMax = tMax;
  }

  /**
   * This returns true if the values are ascending (tied is ok); otherwise, it returns false
   * (descending or unordered).
   *
   * @return true if the values are ascending (tied is ok); otherwise, it returns false (descending
   *     or unordered).
   */
  public boolean isAscending() {
    return isAscending;
  }

  /**
   * If there are 2 or more values and the values are evenly spaced, this returns true; else it
   * returns false.
   *
   * @return If there are 2 or more values and the values are evenly spaced, this returns true; else
   *     it returns false.
   */
  public boolean isEvenlySpaced() {
    return isEvenlySpaced;
  }

  /**
   * This sets isEvenlySpaced.
   *
   * @return If there are 2 or more values and the values are evenly spaced, this returns true; else
   *     it returns false.
   */
  public void setIsEvenlySpaced(boolean tIsEvenlySpaced) {
    isEvenlySpaced = tIsEvenlySpaced;
  }

  /**
   * If there are 2 or more values, this returns the average spacing between values (will be
   * negative if axis is descending!). If isEvenlySpaced, then these are evenly spaced. For
   * EDVTimeStampGridAxis, this is in epochSeconds.
   *
   * @return If there are 2 or more values, this returns the average spacing between values (in
   *     destination units).
   */
  public double averageSpacing() {
    return averageSpacing;
  }

  /**
   * This returns a human-oriented description of the spacing of this EDVGridAxis. (May be
   * negative.)
   *
   * @param language the index of the selected language
   */
  public String spacingDescription(int language) {
    boolean isTimeStamp = this instanceof EDVTimeStampGridAxis;
    if (sourceValues().size() == 1) return "(" + EDStatic.EDDGridJustOneValueAr[language] + ")";
    String s =
        isTimeStamp
            ? Calendar2.elapsedTimeString(Math.rint(averageSpacing()) * 1000)
            : "" + Math2.floatToDouble(averageSpacing());
    return s
        + " ("
        + (isEvenlySpaced() ? EDStatic.EDDGridEvenAr[language] : EDStatic.EDDGridUnevenAr[language])
        + ")";
  }

  /**
   * This returns HTML suitable for a tooltip for this dimension. The range will be from
   * firstDestinationValue to lastDestinationValue (which is different from min to max if
   * !ascending).
   *
   * @param language the index of the selected language
   */
  public String htmlRangeTooltip(int language) {
    String tUnits = units();
    PrimitiveArray tSourceValues = sourceValues(); // work with stable local reference
    boolean isTimeStamp = this instanceof EDVTimeStampGridAxis;
    if (tUnits == null || isTimeStamp) tUnits = "";
    if (tSourceValues.size() == 1)
      return destinationName
          + " has 1 value: "
          + destinationToString(firstDestinationValue())
          + " "
          + tUnits;

    String tSpacing =
        isTimeStamp
            ? Calendar2.elapsedTimeString(Math.rint(averageSpacing()) * 1000)
            : "" + Math2.floatToDouble(averageSpacing()) + " " + tUnits;
    return destinationName
        + " has "
        + tSourceValues.size()
        + " values<br>"
        + "ranging from "
        + destinationToString(firstDestinationValue())
        + " to "
        + destinationToString(lastDestinationValue())
        + " "
        + tUnits
        + "<br>"
        + "with "
        + (isEvenlySpaced() ? EDStatic.EDDGridEvenAr[language] : EDStatic.EDDGridUnevenAr[language])
        + " spacing "
        + (isEvenlySpaced() ? "" : "~")
        + "= "
        + tSpacing;
  }

  /**
   * This converts a destination value (time must be in epoch seconds) to the closest source index.
   * Out of range values are converted to closest source index (even if way off). NaN returns -1.
   * This works whether isAscending or not. !!!If there are ties, this doesn't specify which of the
   * tied values will be found (which is part of why EDVGridAxis doesn't allow ties).
   *
   * @param destinationD
   * @return the closest source index
   */
  public int destinationToClosestIndex(double destinationD) {
    if (Double.isNaN(destinationD)) return -1;

    DoubleArray destDA = new DoubleArray(new double[] {destinationD});
    PrimitiveArray sourcePA = toSource(destDA);
    return sourceToClosestIndex(sourcePA.getNiceDouble(0)); // all grid sources are numeric
  }

  /**
   * This is like destinationToClosestIndex but returns a double as if the index numbers where a
   * continuous line, e.g., 2.25 indicates the dest value is 1/4 of the way between the data values
   * for [2] and [3].
   *
   * <p>Out of range values are converted to closest source index, first or last (even if way off).
   * NaN returns -1. This works whether isAscending or not. (!!!If there are ties, this wouldn't
   * specify which of the tied values would be found, which is part of why EDVGridAxis doesn't allow
   * ties).
   *
   * @param destinationD
   * @return the closest source index as a continuous value
   */
  public double destinationToDoubleIndex(double destinationD) {
    // thankfully, there can't be any tied values
    int closest = sourceToClosestIndex(destinationD);

    // is it an exact match?
    double dClosest = destinationDouble(closest);
    if (dClosest == destinationD) return closest;
    boolean isLess = destinationD < dClosest;

    // there are more concise ways to code this (^), but this is the easiest to read
    double tdClosest;
    if (isAscending) {
      if (isLess) {
        // look at closest - 1
        if (closest == 0) {
          return closest;
        } else {
          tdClosest = destinationDouble(closest - 1);
          return (closest - 1) + (destinationD - tdClosest) / (dClosest - tdClosest);
        }
      } else {
        // look at closest + 1
        if (closest == sourceValues.size() - 1) {
          return closest;
        } else {
          tdClosest = destinationDouble(closest + 1);
          return closest + (destinationD - dClosest) / (tdClosest - dClosest);
        }
      }
    } else { // descending
      if (isLess) {
        // look at closest + 1
        if (closest == sourceValues.size() - 1) {
          return closest;
        } else {
          tdClosest = destinationDouble(closest + 1);
          return closest + (destinationD - dClosest) / (tdClosest - dClosest);
        }
      } else {
        // look at closest - 1
        if (closest == 0) {
          return closest;
        } else {
          tdClosest = destinationDouble(closest - 1);
          return (closest - 1) + (destinationD - tdClosest) / (dClosest - tdClosest);
        }
      }
    }
  }

  /**
   * NaN returns -1. This works whether isAscending or not. !!!If there are ties, this doesn't
   * specify which of the tied values will be found (which is part of why EDVGridAxis doesn't allow
   * ties).
   *
   * @param sourceD A number that is way out of range will catch one of the end indices.
   * @return the closest source index
   */
  public int sourceToClosestIndex(double sourceD) {
    if (Double.isNaN(sourceD)) return -1;

    if (isAscending) return sourceValues().binaryFindClosest(sourceD);
    return sourceValues().linearFindClosest(sourceD);
  }
}
