package gov.noaa.pfel.erddap.dataset.metadata;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.array.ULongArray;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDMessages;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.util.TranslateMessages;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class LocalizedAttributes {
  private Map<String, PrimitiveArray> map = new HashMap<>();
  private List<Map<String, PrimitiveArray>> localized =
      new ArrayList<Map<String, PrimitiveArray>>(TranslateMessages.languageCodeList.size());

  public LocalizedAttributes() {}

  public LocalizedAttributes(Attributes sourceAttributes) {
    String[] sourceNames = sourceAttributes.getNames();
    for (String name : sourceNames) {
      addIfNotNull(map, name, sourceAttributes.get(name));
    }
  }

  public LocalizedAttributes(LocalizedAttributes baseAttributes) {
    copyMapTo(baseAttributes.map, map);
    for (int i = 0; i < TranslateMessages.languageCodeList.size(); i++) {
      Map<String, PrimitiveArray> langBaseMap = baseAttributes.safeGetLangMap(i);
      if (langBaseMap != null) {
        Map<String, PrimitiveArray> newLangMap = new HashMap<>();
        safeSetLangMap(i, newLangMap);
        copyMapTo(langBaseMap, newLangMap);
      }
    }
  }

  public LocalizedAttributes(LocalizedAttributes baseAttributes, Attributes sourceAttributes) {
    String[] sourceNames = sourceAttributes.getNames();
    for (String name : sourceNames) {
      addIfNotNull(map, name, sourceAttributes.get(name));
    }
    copyMapTo(baseAttributes.map, map);
    for (int i = 0; i < TranslateMessages.languageCodeList.size(); i++) {
      Map<String, PrimitiveArray> langBaseMap = baseAttributes.safeGetLangMap(i);
      if (langBaseMap != null) {
        Map<String, PrimitiveArray> newLangMap = new HashMap<>();
        safeSetLangMap(i, newLangMap);
        copyMapTo(langBaseMap, newLangMap);
      }
    }
  }

  public Map<String, PrimitiveArray> toMap(int language) {
    Map<String, PrimitiveArray> attr = new HashMap<>();
    Set<Entry<String, PrimitiveArray>> entries = map.entrySet();
    for (Entry<String, PrimitiveArray> entry : entries) {
      if (entry.getValue() != null) {
        attr.put(entry.getKey(), entry.getValue());
      }
    }
    Map<String, PrimitiveArray> langMap = safeGetLangMap(language);
    if (langMap != null) {
      Set<Entry<String, PrimitiveArray>> langEntries = langMap.entrySet();
      for (Entry<String, PrimitiveArray> entry : langEntries) {
        if (entry.getValue() != null) {
          attr.put(entry.getKey(), entry.getValue());
        }
      }
    }

    return attr;
  }

  public Attributes toAttributes(int language) {
    Attributes attr = new Attributes();
    Set<Entry<String, PrimitiveArray>> entries = map.entrySet();
    for (Entry<String, PrimitiveArray> entry : entries) {
      if (entry.getValue() != null) {
        attr.set(entry.getKey(), entry.getValue());
      }
    }
    Map<String, PrimitiveArray> langMap = safeGetLangMap(language);
    if (langMap != null) {
      Set<Entry<String, PrimitiveArray>> langEntries = langMap.entrySet();
      for (Entry<String, PrimitiveArray> entry : langEntries) {
        if (entry.getValue() != null) {
          attr.set(entry.getKey(), entry.getValue());
        }
      }
    }

    return attr;
  }

  private void addIfNotNull(Map<String, PrimitiveArray> toMap, String key, PrimitiveArray value) {
    if (value != null) {
      toMap.put(String2.canonical(key), (PrimitiveArray) value.clone());
    }
  }

  private void copyMapTo(Map<String, PrimitiveArray> fromMap, Map<String, PrimitiveArray> toMap) {
    Set<Entry<String, PrimitiveArray>> entries = fromMap.entrySet();
    for (Entry<String, PrimitiveArray> entry : entries) {
      addIfNotNull(toMap, entry.getKey(), entry.getValue());
    }
  }

  public PrimitiveArray get(int language, String name) {
    Map<String, PrimitiveArray> langMap = safeGetLangMap(language);
    if (langMap != null) {
      PrimitiveArray value = langMap.get(name);
      if (value != null) {
        return value;
      }
    }
    return map.get(name);
  }

  public int getInt(int language, String name) {
    try {
      PrimitiveArray pa = get(language, name);
      if (pa == null || pa.size() == 0) return Integer.MAX_VALUE;
      return pa.getInt(0);
    } catch (Exception e) {
      return Integer.MAX_VALUE;
    }
  }

  public double getDouble(int language, String name) {
    try {
      PrimitiveArray pa = get(language, name);
      if (pa == null || pa.size() == 0) return Double.NaN;
      return pa.getDouble(0);
    } catch (Exception e) {
      return Double.NaN;
    }
  }

  public float getFloat(int language, String name) {
    try {
      PrimitiveArray pa = get(language, name);
      if (pa == null || pa.size() == 0) return Float.NaN;
      return pa.getFloat(0);
    } catch (Exception e) {
      return Float.NaN;
    }
  }

  /**
   * A convenience method which returns the first element of the attribute's value PrimitiveArray as
   * a long, regardless of the type used to store it.
   *
   * @param name the name of an attribute
   * @return the attribute as a long or Long.MAX_VALUE if trouble (e.g., not found)
   */
  public long getLong(int language, String name) {
    try {
      PrimitiveArray pa = get(language, name);
      if (pa == null || pa.size() == 0) return Long.MAX_VALUE;
      return pa.getLong(0);
    } catch (Exception e) {
      return Long.MAX_VALUE;
    }
  }

  /**
   * A convenience method which returns the first element of the attribute's value PrimitiveArray as
   * a ulong, regardless of the type used to store it.
   *
   * @param name the name of an attribute
   * @return the attribute as a ulong or null if trouble (e.g., not found). This acts like
   *     maxIsMV=true and returns MAX_VALUE.
   */
  public BigInteger getULong(int language, String name) {
    try {
      PrimitiveArray pa = get(language, name);
      if (pa == null || pa.size() == 0) return ULongArray.MAX_VALUE;
      BigInteger bi = pa.getULong(0);
      return bi == null ? ULongArray.MAX_VALUE : bi;
    } catch (Exception e) {
      return ULongArray.MAX_VALUE;
    }
  }

  public String getString(int language, String name) {
    PrimitiveArray value = get(language, name);
    if (value != null) {
      return value.getRawString(0);
    }
    return null;
  }

  public LocalizedAttributes set(int language, String name, PrimitiveArray value) {
    // Historical behavior, if set to null, then remove.
    // This could be something like:
    // <att name=\"starting_julian_day_number\" />
    // in the datasets.xml file.
    if (value == null
        || value.size() == 0
        || (value.size() == 1
            && value.elementType() == PAType.STRING
            && value.getRawString(0).trim().length() == 0)) {
      return remove(name);
    }
    if (language == EDMessages.DEFAULT_LANGUAGE || language < 0) {
      map.put(String2.canonical(name), value);
    } else {
      setLocalized(language, name, value);
    }
    return this;
  }

  private void setLocalized(int language, String name, PrimitiveArray value) {
    Map<String, PrimitiveArray> langMap = safeGetLangMap(language);
    if (langMap == null) {
      langMap = new HashMap<>();
      safeSetLangMap(language, langMap);
    }
    langMap.put(String2.canonical(name), value);
  }

  public LocalizedAttributes set(int language, String name, String value) {
    if (value == null || value.trim().length() == 0) {
      return remove(name);
    }
    set(language, name, new StringArray(new String[] {value}));
    return this;
  }

  public LocalizedAttributes set(int language, String name, double value) {
    return set(language, name, new DoubleArray(new double[] {value}));
  }

  public PrimitiveArray removeAndGetDefault(String name) {
    PrimitiveArray value = get(EDMessages.DEFAULT_LANGUAGE, name);
    remove(name);
    return value;
  }

  public LocalizedAttributes remove(String name) {
    map.remove(name);
    for (Map<String, PrimitiveArray> langMap : localized) {
      if (langMap != null) {
        langMap.remove(name);
      }
    }
    return this;
  }

  public LocalizedAttributes removeValue(String value) {
    removeValue(map, value);
    for (Map<String, PrimitiveArray> langMap : localized) {
      if (langMap != null) {
        removeValue(langMap, value);
      }
    }
    return this;
  }

  private void removeValue(Map<String, PrimitiveArray> map, String value) {
    Iterator<Entry<String, PrimitiveArray>> it =
        map.entrySet().iterator(); // iterator (not enumeration) since I use it.remove() below
    while (it.hasNext()) {
      Entry<String, PrimitiveArray> entry = it.next();
      if (entry.getValue().toString().equals(value)) {
        it.remove();
      }
    }
  }

  public String[] getStringsFromCSV(int language, String name) {
    try {
      String csv = getString(language, name);
      return StringArray.arrayFromCSV(csv);
    } catch (Exception e) {
      return null;
    }
  }

  public EDDInternationalString getInternationalString(String name) {
    Map<Locale, String> international = new HashMap<>();

    for (int i = 0; i < TranslateMessages.languageCodeList.size(); i++) {
      Map<String, PrimitiveArray> langMap = safeGetLangMap(i);
      if (langMap != null) {
        PrimitiveArray value = langMap.get(name);
        if (value != null) {
          international.put(
              Locale.forLanguageTag(TranslateMessages.languageCodeList.get(i)),
              value.getRawString(0));
        }
      }
    }
    PrimitiveArray value = map.get(name);
    String defaultValue = value != null ? value.getRawString(0) : "";

    return new EDDInternationalString(defaultValue, international);
  }

  /**
   * A convenience method which returns the first element of the attribute's value PrimitiveArray as
   * a PAOne.
   *
   * @param name the name of an attribute
   * @return the attribute as a PAOne (or null if trouble (e.g., not found))
   */
  public PAOne getPAOne(int language, String name) {
    try {
      PrimitiveArray pa = get(language, name);
      if (pa == null || pa.size() == 0) return null;
      return new PAOne(pa, 0);
    } catch (Exception e) {
      return null;
    }
  }

  public void validateAttributes(String message) {
    Set<String> keys = map.keySet();
    for (int i = 0; i < TranslateMessages.languageCodeList.size(); i++) {
      Map<String, PrimitiveArray> langMap = safeGetLangMap(i);
      if (langMap != null) {
        keys.addAll(langMap.keySet());
      }
    }
    for (String key : keys) {
      Test.ensureSomethingUnicode(key, message + ": an attribute name");
      String defaultResult = get(EDMessages.DEFAULT_LANGUAGE, key).toString();
      Test.ensureSomethingUnicode(defaultResult, message + ": the attribute value for name=" + key);
      for (int i = 0; i < TranslateMessages.languageCodeList.size(); i++) {
        String langResult = get(i, key).toString();
        if (!defaultResult.equals(langResult)) {
          Test.ensureSomethingUnicode(
              defaultResult,
              message
                  + ": the attribute value for name="
                  + key
                  + " for lang="
                  + TranslateMessages.languageCodeList.get(i));
        }
      }
    }
  }

  public void ensureSomethingUnicode(String message) {
    ensureSomethingUnicode(map, message);
    for (int i = 0; i < TranslateMessages.languageCodeList.size(); i++) {
      Map<String, PrimitiveArray> langMap = safeGetLangMap(i);
      if (langMap != null) {
        ensureSomethingUnicode(langMap, message);
      }
    }
  }

  private void ensureSomethingUnicode(Map<String, PrimitiveArray> map, String message) {
    Set<String> names = map.keySet();
    for (String name : names) {
      Test.ensureSomethingUnicode(name, message + ": an attribute name");
      Test.ensureSomethingUnicode(
          map.get(name).toString(), message + ": the attribute value for name=" + name);
    }
  }

  /**
   * This throws a RuntimeException if any attribute name is !String2.isVariableNameSafe(attName).
   *
   * @param sourceDescripton e.g., "In the combined attributes for the variable with
   *     destinationName="sst"". This is just used in the error message.
   */
  public void ensureNamesAreVariableNameSafe(String sourceDescription) {
    ensureNamesAreVariableNameSafe(map, sourceDescription);
    for (int i = 0; i < TranslateMessages.languageCodeList.size(); i++) {
      Map<String, PrimitiveArray> langMap = safeGetLangMap(i);
      if (langMap != null) {
        ensureNamesAreVariableNameSafe(langMap, sourceDescription);
      }
    }
  }

  private void ensureNamesAreVariableNameSafe(
      Map<String, PrimitiveArray> map, String sourceDescription) {
    Set<String> names = map.keySet();
    for (String name : names) {
      if (!String2.isVariableNameSafe(name))
        throw new RuntimeException(
            sourceDescription
                + ", attributeName="
                + String2.toJson(name)
                + " isn't variableNameSafe. It must start with iso8859Letter|_ and contain only iso8859Letter|_|0-9 .");
    }
  }

  public void updateUrls() {
    // updateUrls in all attributes
    updateUrls(map);
    for (int i = 0; i < TranslateMessages.languageCodeList.size(); i++) {
      Map<String, PrimitiveArray> langMap = safeGetLangMap(i);
      if (langMap != null) {
        updateUrls(langMap);
      }
    }
  }

  private void updateUrls(Map<String, PrimitiveArray> map) {
    map.replaceAll(
        (k, v) -> {
          if (v == null) {
            return v;
          }
          if (v != null && v.elementType() != PAType.STRING) {
            return v;
          }
          if (String2.indexOf(EDStatic.messages.updateUrlsSkipAttributes, k) >= 0) {
            return v;
          }
          if (v.size() == 0) {
            return v;
          }
          String value = EDStatic.updateUrls(v.getString(0));
          if (!value.equals(v.getString(0))) {
            return new StringArray(new String[] {value});
          }
          return v;
        });
  }

  private Map<String, PrimitiveArray> safeGetLangMap(int language) {
    if (localized.size() > language) {
      return localized.get(language);
    }
    return null;
  }

  private void safeSetLangMap(int language, Map<String, PrimitiveArray> map) {
    if (localized.size() > language) {
      localized.set(language, map);
    }
    for (int i = 0; i < language; i++) {
      if (localized.size() <= i) {
        localized.add(null);
      }
    }
    localized.add(map);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    Set<String> names = map.keySet();
    for (String name : names) {
      sb.append(
          "    "
              + name
              + "="
              + get(EDMessages.DEFAULT_LANGUAGE, name).toNccsv127AttString()
              + "\n");
    }
    return sb.toString();
  }

  /**
   * If _Unsigned=true, change tSourceType. This does not call
   * tSourceAttributes.convertSomeSignedToUnsigned().
   *
   * @param tSourceType the CoHort String name for the type. If !something, this returns current
   *     value. If invalid type, this throws exception.
   * @param tSourceAtts the source attributes.
   * @param tAddAtts the add attributes. If _Unsigned existed, _Unsigned=null will be added here.
   * @return the original tSourceType or the adjusted tSourceType.
   * @throws Exception if tSourceAtts or tAddAtts is null
   */
  public String adjustSourceType(String tSourceType, Attributes tSourceAtts) {
    if (!String2.isSomething(tSourceType)) return tSourceType;
    PAType paType = PAType.fromCohortStringCaseInsensitive(tSourceType); // throws exception

    PrimitiveArray us = removeAndGetDefault("_Unsigned");
    if (us == null) us = tSourceAtts.remove("_Unsigned");
    if (us == null) return tSourceType;

    if ("true".equals(us.toString())) paType = PAType.makeUnsigned(paType);
    else if ("false".equals(us.toString())) paType = PAType.makeSigned(paType);

    set(EDMessages.DEFAULT_LANGUAGE, "_Unsigned", "null");

    return PAType.toCohortString(paType);
  }

  /**
   * This variant of adjustSourceType works with a pa, not sourceType string.
   *
   * @param pa the PrimitiveArray of source values. If null, this throws exception.
   * @param tSourceAtts the source attributes
   * @param tAddAtts the add attributes. If _Unsigned existed, _Unsigned=null will be added here.
   * @return the original tSourceType or the adjusted tSourceType.
   */
  public PrimitiveArray adjustSourceType(PrimitiveArray pa, Attributes tSourceAtts) {

    String tSourceType = adjustSourceType(pa.elementTypeString(), tSourceAtts);
    return PrimitiveArray.factory(PAType.fromCohortStringCaseInsensitive(tSourceType), pa);
  }

  public LocalizedAttributes add(Attributes attr) {
    String[] sourceNames = attr.getNames();
    for (String name : sourceNames) {
      addIfNotNull(map, name, attr.get(name));
    }
    return this;
  }
}
