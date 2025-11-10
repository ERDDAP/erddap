package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.Attributes;
import com.cohort.array.PAType;

/**
 * Holds the necessary metadata for a single variable from a file, extracted to avoid loading the
 * entire file's data table.
 */
public class FileVariableMetadata {
  public final boolean existsInFile;
  public final Attributes sourceAttributes;
  public final PAType paType;
  public final int n; // Number of valid (non-missing) values
  public final int size; // Total size of the variable array (to check for hasNaN)

  // For String/Char/Long/ULong
  public final String stringMin;
  public final String stringMax;

  // For Numeric
  public final double numericMin;
  public final double numericMax;

  // For sorted numeric column
  public final boolean isSortedColumn;
  public final String ascendingCheckResult; // "" if ascending, error message otherwise
  public final String evenlySpacedCheckResult; // "" if evenly spaced, error otherwise

  /** Creates a metadata object representing no data (n=0, size=0). */
  public static FileVariableMetadata createNoData(PAType paType) {
    Attributes atts = new Attributes();
    if (treatAsString(paType)) {
      return new FileVariableMetadata(atts, paType, 0, 0, null, null);
    } else {
      return new FileVariableMetadata(
          atts, paType, 0, 0, Double.NaN, Double.NaN, false, "n/a", "n/a");
    }
  }

  public static boolean treatAsString(PAType paType) {
    return paType == PAType.STRING
        || paType == PAType.CHAR
        || paType == PAType.LONG
        || paType == PAType.ULONG;
  }

  /** Constructor for a variable that doesn't exist in the file. */
  public FileVariableMetadata() {
    this.existsInFile = false;
    this.sourceAttributes = null;
    this.paType = null;
    this.n = 0;
    this.size = 0;
    this.stringMin = null;
    this.stringMax = null;
    this.numericMin = Double.NaN;
    this.numericMax = Double.NaN;
    this.isSortedColumn = false;
    this.ascendingCheckResult = "n/a";
    this.evenlySpacedCheckResult = "n/a";
  }

  /** Constructor for String/Long types. */
  public FileVariableMetadata(
      Attributes atts, PAType type, int n, int size, String min, String max) {
    this.existsInFile = true;
    this.sourceAttributes = atts;
    this.paType = type;
    this.n = n;
    this.size = size;
    this.stringMin = min;
    this.stringMax = max;
    this.numericMin = Double.NaN;
    this.numericMax = Double.NaN;
    this.isSortedColumn = false;
    this.ascendingCheckResult = "n/a";
    this.evenlySpacedCheckResult = "n/a";
  }

  /** Constructor for Numeric types. */
  public FileVariableMetadata(
      Attributes atts,
      PAType type,
      int n,
      int size,
      double min,
      double max,
      boolean isSorted,
      String ascendingCheck,
      String evenlySpacedCheck) {
    this.existsInFile = true;
    this.sourceAttributes = atts;
    this.paType = type;
    this.n = n;
    this.size = size;
    this.stringMin = null;
    this.stringMax = null;
    this.numericMin = min;
    this.numericMax = max;
    this.isSortedColumn = isSorted;
    this.ascendingCheckResult = ascendingCheck;
    this.evenlySpacedCheckResult = evenlySpacedCheck;
  }

  public boolean hasNaN() {
    return n < size;
  }
}
