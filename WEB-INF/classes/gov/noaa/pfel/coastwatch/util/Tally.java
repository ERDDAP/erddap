/*
 * Tally Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class tallies events (which are identified by a category name and an attribute name).
 * Basically, you create the Tally object; call add() repeatedly; call toString().
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-05-01
 */
public class Tally {
  // thread-safe; use default nConcurrent
  protected ConcurrentHashMap mainHashMap = new ConcurrentHashMap();

  /** This adds 1 tally mark. */
  public void add(String categoryName, String attributeName) {
    add(categoryName, attributeName, 1);
  }

  /**
   * This adds n tally marks. If categoryName or attributeName is null, it is treated as "".
   *
   * @param categoryName If no such category exists, one will be created. Case sensitive. If
   *     categoryName is null or "", nothing is done.
   * @param attributeName If no such attribute exists, one will be created. Case sensitive. If
   *     attributeName is null, it is logged as "(null)". AttributeName="" is valid.
   * @param nMarks The number of tally marks to add for this category, almost always 1.
   */
  public void add(String categoryName, String attributeName, int nTimes) {
    if (categoryName == null || categoryName.length() == 0) {
      String2.log(String2.ERROR + " in Tally.add: categoryName not specified.");
      return;
    }
    if (attributeName == null) attributeName = "(null)";

    // get the category's hashMap
    ConcurrentHashMap hashMap = (ConcurrentHashMap) mainHashMap.get(categoryName);
    if (hashMap == null) {
      hashMap = new ConcurrentHashMap(); // use default nConcurrent
      mainHashMap.put(categoryName, hashMap);
    }

    // get the attribute's intObject
    IntObject intObject = (IntObject) hashMap.get(attributeName);
    if (intObject == null) hashMap.put(attributeName, new IntObject(nTimes));
    else intObject.i += nTimes;
  }

  /**
   * This removes a category.
   *
   * @param categoryName It isn't an error if it doesn't exist. Case sensitive.
   */
  public void remove(String categoryName) {
    if (categoryName == null || categoryName.length() == 0) {
      String2.log(String2.ERROR + " in Tally.remove: categoryName not specified.");
      return;
    }

    // get the category's hashMap
    mainHashMap.remove(categoryName);
  }

  /**
   * Returns the string representation of the tallies (with no limitation on the max number
   * displayed).
   *
   * @return the string representation of the tallies.
   */
  @Override
  public String toString() {
    return toString(Integer.MAX_VALUE);
  }

  /**
   * Returns the string representation of the tallies (with a limitation on the max number
   * displayed).
   *
   * @param maxAttributeNames the maximum number of attribute names printed per category
   * @return the string representation of the tallies.
   */
  public String toString(int maxAttributeNames) {
    // get the categoryNames
    Set categorySet = mainHashMap.keySet();
    if (categorySet == null || categorySet.size() == 0) return "Tally system has no entries.\n\n";
    Object categoryArray[] = categorySet.toArray();

    // sort the categoryNames
    Arrays.sort(categoryArray, String2.STRING_COMPARATOR_IGNORE_CASE);

    // for each category
    StringBuilder results = new StringBuilder();
    for (int cat = 0; cat < categoryArray.length; cat++)
      results.append(toString((String) categoryArray[cat], maxAttributeNames));

    return results.toString();
  }

  /**
   * This returns a table-like ArrayList with 2 items: attributeNames (a StringArray) and counts (an
   * IntArray), sorted by counts (descending) then attributeNames (ascending).
   *
   * @return null if no items for categoryName
   */
  public ArrayList getSortedNamesAndCounts(String categoryName) {

    ConcurrentHashMap hashMap = (ConcurrentHashMap) mainHashMap.get(categoryName);
    if (hashMap == null) return null;

    // make a StringArray of attributeNames and IntArray of counts
    StringArray attributeNames = new StringArray();
    IntArray counts = new IntArray();
    Iterator it = hashMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry me = (Map.Entry) it.next();
      attributeNames.add((String) me.getKey());
      counts.add(((IntObject) me.getValue()).i);
    }

    // sort by counts
    ArrayList arrayList = new ArrayList();
    arrayList.add(attributeNames);
    arrayList.add(counts);
    PrimitiveArray.sortIgnoreCase(arrayList, new int[] {1, 0}, new boolean[] {false, true});

    return arrayList;
  }

  /**
   * This returns a String with the information for one category (or "" if not found).
   *
   * @param categoryName
   * @param maxAttributeNames the maximum number of attribute names printed per category
   */
  public String toString(String categoryName, int maxAttributeNames) {
    ArrayList arrayList = getSortedNamesAndCounts(categoryName);
    if (arrayList == null) return "";
    StringArray attributeNames = (StringArray) arrayList.get(0);
    IntArray counts = (IntArray) arrayList.get(1);

    // print
    StringBuilder results = new StringBuilder();
    results.append(categoryName + "\n");
    int countsSize = counts.size();
    int nRows = Math.min(maxAttributeNames, countsSize);
    int countsSum = Math2.roundToInt(counts.calculateStats()[PrimitiveArray.STATS_SUM]);
    for (int row = 0; row < nRows; row++) {
      String tName = attributeNames.get(row);
      boolean needToEncode = false;
      int tNameLength = tName.length();
      for (int i = 0; i < tNameLength; i++) {
        if (tName.charAt(i) < 32) {
          needToEncode = true;
          break;
        }
      }
      if (needToEncode) tName = String2.toJson(tName);
      results.append(
          "    "
              + tName
              + ": "
              + counts.get(row)
              + "  ("
              + Math2.roundToInt(counts.get(row) * 100.0 / countsSum)
              + "%)\n");
    }
    if (countsSize > nRows) {
      int countOfNotShown = 0;
      for (int row = nRows; row < countsSize; row++) countOfNotShown += counts.get(row);
      results.append(
          "    ("
              + (counts.size() - nRows)
              + " not shown): "
              + countOfNotShown
              + "  ("
              + Math2.roundToInt(countOfNotShown * 100.0 / countsSum)
              + "%)\n");
    }
    results.append("\n");
    return results.toString();
  }
}
