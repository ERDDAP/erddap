/*
 * Units2 Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package com.cohort.util;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.google.common.io.Resources;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ucar.units.Unit;
import ucar.units.UnitFormat;

/**
 * This class has static methods to convert units from one standard to another. An old local copy of
 * UDUnits 1 info is at c:/programs/udunits-1.12.4/udunits.txt . See the UCUM validator at
 * https://ucum.nlm.nih.gov/ucum-lhc/demo.html .
 */
public class Units2 {

  /** UDUNITS and UCUM support metric prefixes. */
  public static String metricName[] = {
    "yotta", "zetta", "exa", "peta", "tera",
    "giga", "mega", "kilo", "hecto", "deka",
    "deci", "centi", "milli", "micro", "nano",
    "pico", "femto", "atto", "zepto", "yocto",
    "µ",
  };

  public static String metricAcronym[] = {
    "Y", "Z", "E", "P", "T",
    "G", "M", "k", "h", "da",
    "d", "c", "m", "u", "n",
    "p", "f", "a", "z", "y",
    "u"
  };
  public static int nMetric = metricName.length;

  /** UCUM supports power-of-two prefixes, but UDUNITS doesn't. */
  public static String twoAcronym[] = {"Ki", "Mi", "Gi", "Ti"};

  public static String twoValue[] = {"1024", "1048576", "1073741824", "1.099511627776e12"};
  public static int nTwo = twoAcronym.length;

  // these don't need to be thread-safe because they are read-only after creation
  private static Map<String, String> udHashMap =
      getHashMapStringString(
          Resources.getResource("com/cohort/util/UdunitsToUcum.properties"), File2.UTF_8);
  protected static Map<String, String> ucHashMap =
      getHashMapStringString(
          Resources.getResource("com/cohort/util/UcumToUdunits.properties"), File2.UTF_8);

  // these special cases are usually populated by EDStatic static constructor, but don't have to be
  public static Map<String, String> standardizeUdunitsHM = new HashMap();
  public static Map<String, String> ucumToUdunitsHM = new HashMap();
  public static Map<String, String> udunitsToUcumHM = new HashMap();

  /**
   * Set this to true (by calling reallyReallyVerbose=true in your program, not by changing the code
   * here) if you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean debugMode = false;

  /**
   * This is like udunitsToUcum() but won't throw an exception. If there is trouble, it returns
   * udunits.
   */
  public static String safeUdunitsToUcum(String udunits) {
    try {
      return udunitsToUcum(udunits);
    } catch (Throwable t) {
      String2.log(
          String2.ERROR
              + " while converting udunits="
              + udunits
              + " to ucum:\n"
              + MustBe.throwableToString(t));
      return udunits;
    }
  }

  /**
   * This converts UDUnits to UCUM. <br>
   * UDUnits: https://www.unidata.ucar.edu/software/udunits/ I worked with v 2.1.9 <br>
   * UCUM: https://unitsofmeasure.org/ucum.html I worked with Version: 1.8, $Revision: 28894 $
   *
   * <p>UDUnits supports lots of aliases (short and long) and plurals (usually by adding 's'). These
   * all get reduced to UCUM's short and canonical-only units.
   *
   * <p>Notes:
   *
   * <ul>
   *   <li>This method is a strictly case sensitive. The only UDUnits that should be capitalized
   *       (other than acronyms) are Btu, Gregorian..., Julian..., PI. <br>
   *       The only UDUnits that may be capitalized are Celsius, Fahrenheit, Kelvin, Rankine.
   *   <li>For "10 to the", UCUM allows 10* or 10^. This method uses 10^.
   *   <li>NTU becomes {ntu}.
   *   <li>PSU or psu becomes {psu}.
   * </ul>
   *
   * return the UDUnits converted to UCUM. null returns null. "" returns "". throws Exception if
   * trouble.
   */
  public static String udunitsToUcum(String udunits) {
    if (udunits == null) return null;
    udunits = udunits.trim();
    if (debugMode) String2.log(">> convert udunits=" + udunits);
    if (!String2.isSomething2(udunits)) return "";

    // specified in <udunitsToUcum> in messages.xml
    String t = udunitsToUcumHM.get(udunits);
    if (t != null) return t;

    // before test " since " times
    udunits = String2.replaceAll(udunits, " since analysis", "");

    // is it a point in time? e.g., seconds since 1970-01-01T00:00:00T
    int sincePo = udunits.toLowerCase().indexOf(" since ");
    if (sincePo
        > 0) { // more general than Calendar2.isNumericTimeUnits(udunits)) so allow other base
      // times, e.g., Jan 1, 2000
      try {
        // test if really appropriate
        double baf[] = Calendar2.getTimeBaseAndFactor(udunits); // throws exception if trouble

        // use 'factor', since it is more forgiving than udunitsToUcum converter
        String u =
            baf[1] == 0.001
                ? "ms"
                : baf[1] == 1
                    ? "s"
                    : baf[1] == Calendar2.SECONDS_PER_MINUTE
                        ? "min"
                        : baf[1] == Calendar2.SECONDS_PER_HOUR
                            ? "h"
                            : baf[1] == Calendar2.SECONDS_PER_DAY
                                ? "d"
                                : baf[1] == 30 * Calendar2.SECONDS_PER_DAY
                                    ? "mo"
                                    : // mo_j?
                                    baf[1] == 360 * Calendar2.SECONDS_PER_DAY
                                        ? "a"
                                        : // a_j?
                                        udunitsToUcum(
                                            udunits.substring(
                                                0,
                                                sincePo)); // shouldn't happen, but weeks? microsec?

        // make "s{since 1970-01-01T00:00:00T}
        String sinceDate = udunits.substring(sincePo + 7);
        String tryToIso = Calendar2.tryToIsoString(sinceDate);
        return u + "{since " + (tryToIso.length() > 0 ? tryToIso : sinceDate) + "}";
      } catch (Exception e) {
        if (verbose) String2.log("Caught: " + MustBe.throwableToString(e));
      }
    }

    // is it a time format string
    if (udunits.indexOf("yy") >= 0) // more forgiving than Calendar2.isStringTimeUnits(
    return "{" + udunits + "}";

    // *** SPECIAL CASES / FIX UPS
    // replace all ** with ^
    udunits = String2.replaceAll(udunits, "**", "^");

    // remove all <sup> and </sup>
    udunits = String2.replaceAll(udunits, "<sup>", "");
    udunits = String2.replaceAll(udunits, "</sup>", "");

    // change _per_ to " per "
    udunits = String2.replaceAll(udunits, "_per_", " per ");

    // these are comments, not units   set to ""???
    if (udunits.equals("8 bits, encoded")
        || udunits.equals("biomass density unit per abundance unit")
        || udunits.equals("level")
        || udunits.equals("link")
        || udunits.equals("local time")
        || udunits.equals("mean")
        || udunits.equals("mm water per 1.38m soil (0 to 7 layers)")
        || udunits.equals("sigma")
        || udunits.equals("sigma_level")
        || udunits.equals("site")
        ||
        // udunits.equals("") ||
        udunits.equals("Standardized Units of Relative Dry and Wet")
        || udunits.equals("Total Scale")
        || udunits.equals("varies by taxon/group (see description)")) return "{" + udunits + "}";

    if (udunits.equals("1-12")) return "mo";
    if (udunits.equals("1-31")) return "d";

    udunits = String2.replaceAll(udunits, "atomosphere", "atmosphere"); // misspelling

    udunits =
        String2.replaceAll(
            udunits, "Centimeter", "centimeter"); // beware centimeters (so not -> cm)

    if (udunits.equals("count per 85 centimeter^2 per day")) return "{count}.(85.cm2)-1.d-1";

    udunits = String2.replaceAll(udunits, "cubic kilometers", "km3");
    udunits = String2.replaceAll(udunits, "cubic kilometer", "km3");
    udunits = String2.replaceAll(udunits, "cubic meters", "m3");
    udunits = String2.replaceAll(udunits, "cubic meter", "m3");

    if (udunits.indexOf(" C") > 0) {
      udunits = String2.replaceAll(udunits, "deg C", "degree_C");
      udunits = String2.replaceAll(udunits, "degree C", "degree_C");
      udunits = String2.replaceAll(udunits, "degrees C", "degree_C");
    }

    if (udunits.equals("dd") || udunits.equals("dd (01 to 31)") || udunits.equals("day_of_year"))
      return "d";

    if (udunits.startsWith("decimal")) // hours, minutes, seconds, degrees
    udunits = udunits.substring(7).trim();

    if (udunits.equals("degrees (+E)") || udunits.equals("degrees-east")) return "deg{east}";
    if (udunits.equals("degrees (+N)") || udunits.equals("degrees-north")) return "deg{north}";

    if (udunits.equals("Degrees, Oceanographic Convention, 0=toward N, 90=toward E")
        || udunits.equals("degrees (clockwise from true north)")
        || udunits.equals("degrees (clockwise towards true north)")) return "deg{true}";

    if (udunits.equals("degree(azimuth)")
        || udunits.equals("Degrees(azimuth)")
        || udunits.equals("degrees(azimuth)")) return "deg{azimuth}";

    if (udunits.equals("degree(clockwise from bow)")
        || udunits.equals("degree (clockwise from bow)")
        || udunits.equals("degrees (clockwise from bow)")) return "deg{clockwise from bow}";

    if (udunits.startsWith("four digit ")) // year
    udunits = udunits.substring(11);

    if (udunits.startsWith("dyn-")) udunits = "dynamic " + udunits.substring(4);

    if (udunits.equals("(0 - 1)")
        || udunits.equals("frac.")
        || udunits.equals("fraction (between 0 and 1)")
        || udunits.equals("none (fraction)")) return "1";

    if (udunits.equals("gram per one tenth meter^2")) return "g.m-2.10-2";

    udunits = String2.replaceAll(udunits, "MM:SS", "mm:ss");
    if (udunits.startsWith("HH")) return "{" + udunits + "}";

    udunits = String2.replaceAll(udunits, "Kilo", "kilo");
    udunits = String2.replaceAll(udunits, "Kg", "kg");
    udunits = String2.replaceAll(udunits, "Km", "km");

    if (udunits.equals("mb")) // more likely mbar than m barn
    return "mbar";

    udunits = String2.replaceAll(udunits, "micro-", "micro");

    if (udunits.equals("MM/HR")) return "mm.h-1";

    if (udunits.equals("mm/mn")) // /month?
    return "mm.mo-1";

    if (udunits.equals("mm (01 to 12)")) // month
    return "mo";

    if (udunits.startsWith("molm-2")) udunits = "mol m-2 " + udunits.substring(6);

    if (udunits.equals("nominal day")) return "d";

    if (udunits.equals("number of cells")
        || udunits.equals("number of observations")
        || udunits.equals("Obs count")
        || udunits.equals("observations")) return "{count}";

    if (udunits.equals("numeric")) return "1";

    if (udunits.equals("pa")) return "Pa";

    udunits = String2.replaceAll(udunits, "parts per 1000000", "ppm");
    udunits = String2.replaceAll(udunits, "parts per million", "ppm");

    udunits = String2.replaceAll(udunits, "percentage", "%"); // avoids problems below
    udunits = String2.replaceAll(udunits, "percent", "%"); // avoids problems below

    udunits = String2.replaceAll(udunits, "photon flux", "photon_flux");

    udunits = String2.replaceAllIgnoreCase(udunits, "practical salinity units", "PSU");

    if (udunits.equals("PSS-78") || udunits.equals("PSS_78"))
      return "{PSS_78}"; // not hyphen, because it looks like exponent

    udunits = String2.replaceAll(udunits, "psu-m", "PSU m");

    if (udunits.equals("per mil relative to NIST SRM 3104a")
        || // per mil is ppm, not length?
        udunits.equals("ppm relative to NIST SRM 3104a"))
      return "[ppm].{relative to NIST SRM 3104a}"; // NIST SRM 3104a is a standard Barium solution

    udunits = String2.replaceAll(udunits, "square centimeters", "cm2");
    udunits = String2.replaceAll(udunits, "square centimeter", "cm2");
    udunits = String2.replaceAll(udunits, "square kilometers", "km2");
    udunits = String2.replaceAll(udunits, "square kilometer", "km2");
    udunits = String2.replaceAll(udunits, "square meters", "m2");
    udunits = String2.replaceAll(udunits, "square meter", "m2");

    if (udunits.startsWith("two digit ")) // e.g. month, day, hour, minute, second
    udunits = udunits.substring(10);

    if (udunits.toLowerCase().equals("unitless")) return "1";

    if (udunits.equals("volts (0-5 FSO)")) return "V";

    if (udunits.startsWith("W/M")) udunits = "W/m" + udunits.substring(3);

    /*
    if (udunits.equals(""))
        return "";
    if (udunits.equals(""))
        return "";
    if (udunits.equals(""))
        return "";
    if (udunits.equals(""))
        return "";
    */

    // parse udunits and build ucum, till done
    StringBuilder ucum = new StringBuilder();
    int udLength = udunits.length();
    boolean dividePending = false;
    int po = 0; // po is next position to be read
    while (po < udLength) {
      if (debugMode && ucum.length() > 0) String2.log("  ucum=" + ucum);
      char ch = udunits.charAt(po);

      // leave {a comment} unchanged
      if (ch == '{') {
        int po2 = po + 1;
        while (po2 < udLength && udunits.charAt(po2) != '}') po2++;
        ucum.append(udunits.substring(po, po2 + 1));
        po = po2 + 1;
        continue;
      }

      // PER
      if (po <= udLength - 3) {
        if (udunits.substring(po, po + 3).toLowerCase().equals("per")
            && (po + 3 == udLength || !String2.isLetter(udunits.charAt(po + 3)))) {
          dividePending = !dividePending;
          po += 3;
          continue;
        }
      }

      // letter
      if (isUdunitsLetter(ch)) { // includes 'µ' and '°'
        // find contiguous letters|_|digit|-|^
        int po2 = po + 1;
        while (po2 < udLength
            && (isUdunitsLetter(udunits.charAt(po2))
                || udunits.charAt(po2) == '_'
                || String2.isDigit(udunits.charAt(po2))
                || (udunits.charAt(po2) == '-'
                    && po2 + 1 < udLength
                    && // -exponent
                    String2.isDigit(udunits.charAt(po2 + 1)))
                || (udunits.charAt(po2) == '^'
                    && po2 + 1 < udLength
                    && // ^exponent
                    (udunits.charAt(po2 + 1) == '-' || String2.isDigit(udunits.charAt(po2 + 1))))))
          po2++;
        String tUdunits = udunits.substring(po, po2);
        tUdunits = String2.replaceAll(tUdunits, "^", "");
        po = po2;

        // some udunits have internal digits, but none end in digits
        // if it ends in digits, treat as exponent
        // find contiguous digits at end
        int firstDigit = tUdunits.length();
        while (firstDigit >= 1 && String2.isDigit(tUdunits.charAt(firstDigit - 1))) firstDigit--;
        if (firstDigit < tUdunits.length()
            && // there are digits
            tUdunits.charAt(firstDigit - 1) == '-') // there's a '-' too
        firstDigit--; // include it
        String exponent = tUdunits.substring(firstDigit);
        tUdunits = tUdunits.substring(0, firstDigit);
        String tUcum = oneUdunitsToUcum(tUdunits);
        if (debugMode)
          String2.log(">> tUdunits=" + tUdunits + " one=" + tUcum + " exp=" + exponent);

        if (tUcum.length() > 0 && ucum.toString().equals("1.")) ucum.setLength(0);
        ucum.append(tUcum);

        // add the exponent   (always positive here)
        if (exponent.length() > 0) {
          if (dividePending) {
            if (exponent.charAt(0) == '-') exponent = exponent.substring(1);
            else exponent = "-" + exponent;
            dividePending = false;
          }
          ucum.append(exponent);
        } else if (dividePending) {
          ucum.append("-1");
          dividePending = false;
        }

        continue;
      }

      // number
      if (ch == '-' || String2.isDigit(ch)) {
        // find contiguous digits
        int po2 = po + 1;
        while (po2 < udLength && String2.isDigit(udunits.charAt(po2))) po2++;

        // decimal place + digit (not just .=multiplication)
        boolean hasDot = false;
        if (po2 < udLength - 1
            && udunits.charAt(po2) == '.'
            && String2.isDigit(udunits.charAt(po2 + 1))) {
          hasDot = true;
          po2 += 2;
          while (po2 < udLength && String2.isDigit(udunits.charAt(po2))) po2++;
        }

        // exponent?     e-  or e{digit}
        boolean hasE = false;
        if (po2 < udLength - 1
            && Character.toLowerCase(udunits.charAt(po2)) == 'e'
            && (udunits.charAt(po2 + 1) == '-' || String2.isDigit(udunits.charAt(po2 + 1)))) {
          hasE = true;
          po2 += 2;
          while (po2 < udLength && String2.isDigit(udunits.charAt(po2))) po2++;
        }
        String num = udunits.substring(po, po2);
        po = po2;

        // convert floating point to rational number
        if (hasDot || hasE || dividePending) {
          double d = String2.parseDouble(num);
          if (dividePending) {
            d = 1 / d;
            dividePending = false;
          }
          int rational[] = String2.toRational(d);
          if (rational[1] == Integer.MAX_VALUE) ucum.append(num); // ignore the trouble !!! ???
          else if (rational[1] == 0) // includes {0, 0}
          ucum.append(rational[0]);
          else if (rational[0] == 1) ucum.append("10^" + rational[1]);
          else ucum.append(rational[0] + ".10^" + rational[1]);

        } else {
          // just copy num
          ucum.append(num);
        }

        continue;
      }

      // space,*,.,(183)    (multiplication)
      if (ch == ' ' || ch == '*' || ch == '.' || ch == 183) {
        po++;
        if (ucum.length() > 0 && ucum.charAt(ucum.length() - 1) != '.') ucum.append('.');
        continue;
      }

      // /
      if (ch == '/') {
        po++;
        dividePending = !dividePending;
        if (ucum.length() > 0 && ucum.charAt(ucum.length() - 1) != '.') ucum.append('.');
        continue;
      }

      // "
      if (ch == '\"') {
        po++;
        ucum.append("''");
        continue;
      }

      if (ch == '#') {
        po++;
        ucum.append("{count}");
        continue;
      }

      // otherwise, punctuation.   copy it
      ucum.append(ch);
      po++;
    }

    if (String2.startsWith(ucum, "m.m.")) return "m2." + ucum.substring(4);

    // if it is "a (a)" return "a"
    int ppo = ucum.indexOf(".(");
    if (ppo > 0
        && ucum.indexOf("(", ppo + 2) < 0
        && ucum.charAt(ucum.length() - 1) == ')'
        && ucum.substring(0, ppo).equals(ucum.substring(ppo + 2, ucum.length() - 1)))
      return ucum.substring(0, ppo);

    // if it is "a [a]" return "a"
    ppo = ucum.indexOf(".[");
    if (ppo > 0
        && ucum.indexOf("[", ppo + 2) < 0
        && ucum.charAt(ucum.length() - 1) == ']'
        && ucum.substring(0, ppo).equals(ucum.substring(ppo + 2, ucum.length() - 1)))
      return ucum.substring(0, ppo);

    return ucum.toString();
  }

  private static boolean isUdunitsLetter(char ch) {
    return String2.isLetter(ch) || ch == 'µ' || ch == '°';
  }

  /**
   * This converts one udunits term (perhaps with metric prefix(es)) to the corresponding ucum
   * string. If udunits is just metric prefix(es), this returns the prefix acronym(s) with "{count}"
   * as suffix (e.g., dkilo returns dk{count}). If this can't completely convert udunits, it returns
   * the original udunits as a comment (e.g., kiloBobs becomes {kiloBobs} (to avoid 'exact' becoming
   * 'ect' ). For placeholder strings ("null"), this return "".
   */
  private static String oneUdunitsToUcum(String udunits) {

    // repeatedly pull off start of udunits and build ucum, till done
    String oldUdunits = udunits;
    StringBuilder ucum = new StringBuilder();
    boolean hasPrefix = false;
    if (debugMode) String2.log(ucum.length() == 0 ? "" : "  ucum=" + ucum + " udunits=" + udunits);

    // try to find udunits in hashMap
    if (!String2.isSomething2(udunits)) return "";
    udunits = udunits.trim();
    String tUcum = udHashMap.get(udunits);
    if (tUcum != null) {
      // success! done!
      ucum.append(tUcum);
      return ucum.toString();
    }

    // try to separate out a metricName prefix (e.g., "kilo")
    for (int p = 0; p < nMetric; p++) {
      if (udunits.startsWith(metricName[p])) {
        String tUd = udunits.substring(metricName[p].length());
        if (tUd.length() == 0) {
          ucum.append(metricAcronym[p] + "{count}"); // standardize on acronym
          return ucum.toString();
        }
        String newUd = udHashMap.get(tUd);
        if (String2.isSomething(newUd)) {
          ucum.append(metricAcronym[p] + newUd); // standardize on acronym
          return ucum.toString();
        }
      }
    }

    // try to separate out a metricAcronym prefix (e.g., "k")
    for (int p = 0; p < nMetric; p++) {
      if (udunits.startsWith(metricAcronym[p])) {
        String tUd = udunits.substring(metricAcronym[p].length());
        if (tUd.length() == 0) {
          ucum.append(metricAcronym[p] + "{count}");
          return ucum.toString();
        }
        String newUd = udHashMap.get(tUd);
        if (String2.isSomething(newUd)) {
          ucum.append(metricAcronym[p] + newUd);
          return ucum.toString();
        }
      }
    }

    // no change? failure; return original udunits as a comment
    //  because I don't want to change "exact" into "ect"
    //  (i.e., seeing a metric prefix that isn't a metric prefix)
    if (debugMode)
      String2.log("Units2.oneUdunitsToUcum fail: ucum=" + ucum + " udunits=" + udunits);
    // ucum.append(udunits);
    return "{" + oldUdunits + "}"; // ucum.toString();
  }

  /**
   * This is like ucumToUdunits() but won't throw an exception. If there is trouble, it returns
   * ucum.
   */
  public static String safeUcumToUdunits(String ucum) {
    try {
      return ucumToUdunits(ucum);
    } catch (Throwable t) {
      String2.log(
          String2.ERROR
              + " while converting ucum="
              + ucum
              + " to udunits:\n"
              + MustBe.throwableToString(t));
      return ucum;
    }
  }

  /**
   * This converts UCUM to UDUnits. <br>
   * UDUnits: https://www.unidata.ucar.edu/software/udunits/ <br>
   * UCUM: https://unitsofmeasure.org/ucum.html
   *
   * <p>UCUM tends to be short, canonical-only, and strict. Many UCUM units are the same in UDUnits.
   *
   * <p>UDUnits supports lots of aliases (short and long) and plurals (usually by adding 's'). This
   * tries to convert UCUM to a short, common UDUNIT units.
   *
   * <p>Problems:
   *
   * <ul>
   *   <li>UCUM has only "deg", no concept of degree_east|north|true|true.
   * </ul>
   *
   * <p>Notes:
   *
   * <ul>
   *   <li>This method is a strictly case sensitive.
   *   <li>For "10 to the", UCUM allows 10* or 10^. This method uses 10^.
   *   <li>{ntu} becomes NTU.
   *   <li>{psu} becomes PSU.
   * </ul>
   *
   * return the UCUM converted to UDUNITS. null returns null. "" returns "".
   */
  public static String ucumToUdunits(String ucum) {
    if (ucum == null) return null;
    ucum = ucum.trim();
    if (debugMode) String2.log(">> convert ucum=" + ucum);

    StringBuilder udunits = new StringBuilder();
    int ucLength = ucum.length();
    if (ucLength == 0) return "";

    // specified in <ucumToUdunits> in messages.xml
    String t = ucumToUdunitsHM.get(ucum);
    if (t != null) return t;

    // entirely in {}
    if (ucum.charAt(0) == '{' && ucum.indexOf('}') == ucLength - 1)
      return ucum.substring(1, ucLength - 1);

    if (ucum.charAt(ucLength - 1) == '}'
        && // quick reject
        ucum.indexOf('}') == ucLength - 1) { // reasonably quick reject

      // is it a time point?  e.g., s{since 1970-01-01T00:00:00T}
      int sincePo = ucum.indexOf("{since ");
      if (sincePo > 0) {
        // is first part an atomic ucum unit?
        String start = ucum.substring(0, sincePo);
        String tUdunits = ucHashMap.get(start);
        String remainder = " " + ucum.substring(sincePo + 1, ucLength - 1);
        if (start.equals("ms")) return "milliseconds" + remainder;
        else if (start.equals("s")) return "seconds" + remainder;
        else if (start.equals("min")) return "minutes" + remainder;
        else if (start.equals("h")) return "hours" + remainder;
        else if (start.equals("d")) return "days" + remainder;
        else if (start.equals("mo")) return "months" + remainder;
        else if (start.equals("a")) return "years" + remainder;
        else if (String2.isSomething(tUdunits)) return tUdunits + remainder;
        else return start + remainder;
      } // else fall through

      // is it a time format?  e.g., {ddMMMyyyy}
      if (ucum.charAt(0) == '{'
          && ucLength >= 4
          && ucum.indexOf('{', 1) == -1
          && ucum.substring(1, ucLength - 1).indexOf("yy")
              >= 0) { // more forgiving than Calendar2.isStringTimeUnits(
        return ucum.substring(1, ucLength - 1);
      }
    }

    // remove all <sup> and </sup>
    ucum = String2.replaceAll(ucum, "<sup>", "");
    ucum = String2.replaceAll(ucum, "</sup>", "");

    // remove ^ from ^- and ^[digit]
    // but not if preceded by digit
    for (int po = ucum.length() - 3; po >= 0; po--) { // work backwards
      if (!String2.isDigit(ucum.charAt(po))
          && ucum.charAt(po + 1) == '^'
          && (ucum.charAt(po + 2) == '-' || String2.isDigit(ucum.charAt(po + 2))))
        ucum = ucum.substring(0, po + 1) + ucum.substring(po + 2);
    }

    // remove ** from **- and **[digit]
    for (int po = ucum.length() - 3; po >= 0; po--) { // work backwards
      if (ucum.charAt(po) == '*'
          && ucum.charAt(po + 1) == '*'
          && (ucum.charAt(po + 2) == '-' || String2.isDigit(ucum.charAt(po + 2))))
        ucum = ucum.substring(0, po) + ucum.substring(po + 2);
      po = Math.min(po, ucum.length() - 2);
    }
    ucLength = ucum.length();

    // parse ucum and build udunits, till done
    int po = 0; // po is next position to be read
    boolean dividePending = false;
    while (po < ucLength) {
      if (debugMode && udunits.length() > 0) String2.log("  udunits=" + udunits);
      char ch = ucum.charAt(po);

      // uncomment {a comment}
      if (ch == '{') {
        int po2 = po + 1;
        while (po2 < ucLength && ucum.charAt(po2) != '}') po2++;
        udunits.append(ucum.substring(po + 1, po2)); // non-standard udunits comment or unknown term
        po = po2 + 1;
        continue;
      }

      // 5/9
      if (ch == '5'
          && po <= ucLength - 1
          && ucum.charAt(po + 1) == '/'
          && ucum.charAt(po + 2) == '9'
          && (po + 3 == ucum.length() || !String2.isDigit(ucum.charAt(po + 3)))) {
        udunits.append("0.5555555555555556");
        po += 3;
        continue;
      }

      // letter
      if (isUcumLetter(ch)) { // includes [, ], {, }, 'µ' and "'"
        // find contiguous letters|_|digit (no '-')
        int po2 = po + 1;
        while (po2 < ucLength
            && (isUcumLetter(ucum.charAt(po2))
                || ucum.charAt(po2) == '_'
                || String2.isDigit(ucum.charAt(po2)))) {
          po2++;
          if (ucum.charAt(po2 - 1) == '{') {
            // find end of comment
            while (po2 < ucLength && ucum.charAt(po2) != '}') po2++;
            if (po2 < ucLength) // should be
            po2++;
          }
        }

        String tUcum = ucum.substring(po, po2);
        po = po2;

        // some ucum have internal digits, but none end in digits
        // if it ends in digits, treat as exponent
        // find contiguous digits at end
        int firstDigit = tUcum.length();
        while (firstDigit >= 1 && String2.isDigit(tUcum.charAt(firstDigit - 1))) firstDigit--;
        String exponent = tUcum.substring(firstDigit);
        if (dividePending) {
          if (exponent.length() == 0) exponent = "-1";
          else if (exponent.startsWith("-")) exponent = exponent.substring(1);
          else exponent = "-" + exponent;
          dividePending = false;
        }
        tUcum = tUcum.substring(0, firstDigit);
        String tUdunits = oneUcumToUdunits(tUcum);

        if (udunits.toString().equals("1 ")) udunits.setLength(0);

        // deal with PER -> /
        if (tUdunits.equals("/")) {
          dividePending = !dividePending;
          if (udunits.length() > 0 && !String2.endsWith(udunits, " ")) udunits.append(' ');
          tUdunits = "";
        } else {
          udunits.append(tUdunits);
        }

        // add the exponent
        udunits.append(exponent);
        // catch -exponent as a number below

        continue;
      }

      // number
      if (ch == '-' || String2.isDigit(ch)) {
        // find contiguous digits
        int po2 = po + 1;
        while (po2 < ucLength && String2.isDigit(ucum.charAt(po2))) po2++;

        // ^-  or ^{digit}
        boolean hasE = false;
        if (po2 < ucLength - 1
            && ucum.charAt(po2) == '^'
            && (ucum.charAt(po2 + 1) == '-' || String2.isDigit(ucum.charAt(po2 + 1)))) {
          hasE = true;
          po2 += 2;
          while (po2 < ucLength && String2.isDigit(ucum.charAt(po2))) po2++;
        }
        String num = ucum.substring(po, po2);
        if (num.startsWith("10^")) num = "1.0E" + num.substring(3);
        po = po2;
        if (udunits.toString().equals("1 ")) udunits.setLength(0);

        if (dividePending) {
          if (num.startsWith("-")) {
            num = num.substring(1);
          } else if (udunits.length() == 0) {
            // || String2.isDigit(udunits.charAt(udunits.length() - 1))) { ???
            num += "-1";
          } else {
            num = "-" + num;
          }
          dividePending = false;
        }
        udunits.append(num);
        continue;
      }

      // .*[sp]
      if (".* ".indexOf(ch) >= 0) {
        po++;
        if (udunits.length() > 0 && !String2.endsWith(udunits, " ")) udunits.append(' ');
        continue;
      }

      // '  ''
      if (ch == '\'') {
        po++;
        if (po < ucLength && ucum.charAt(po) == '\'') {
          udunits.append("arc_second");
          po++;
        } else {
          udunits.append("arc_minute");
        }
        continue;
      }

      // / (division)
      if (ch == '/') {
        dividePending = !dividePending;
        if (udunits.length() > 0 && !String2.endsWith(udunits, " ")) udunits.append(' ');
        po++;
        continue;
      }

      // otherwise, punctuation.   copy it
      //  " doesn't occur,
      udunits.append(ch);
      po++;
    }

    String uds = udunits.toString().trim();
    if (uds.startsWith("m m ")) uds = "m2 " + uds.substring(4);
    return uds;
  }

  private static boolean isUcumLetter(char ch) {
    return String2.isLetter(ch)
        || ch == '['
        || ch == ']'
        || ch == '{'
        || ch == '}'
        || ch == 'µ'
        || ch == '\'';
  }

  /**
   * This converts one ucum term (perhaps with metric prefix(es)) ( to the corresponding udunits
   * string. If ucum is just metric prefix(es), this returns the metric prefix acronym(s) with
   * "{count}" as suffix (e.g., dkilo returns dk{count}). If this can't completely convert ucum, it
   * returns the original ucum (e.g., kiloBobs remains kiloBobs (to avoid 'exact' becoming 'ect' ).
   */
  private static String oneUcumToUdunits(String ucum) {

    // repeatedly pull off start of ucum and build udunits, till done
    String oldUcum = ucum;
    StringBuilder udunits = new StringBuilder();
    boolean caughtPrefix = false; // so just allow one prefix

    MAIN:
    while (true) {
      if (debugMode)
        String2.log(udunits.length() == 0 ? "" : "  udunits=" + udunits + " ucum=" + ucum);

      // try to find ucum in hashMap
      String tUdunits = ucHashMap.get(ucum);
      if (tUdunits != null) {
        // success! done!
        udunits.append(tUdunits);
        return udunits.toString();
      }

      // try to separate out one metricAcronym prefix (e.g., "k")
      if (!caughtPrefix) {
        for (int p = 0; p < nMetric; p++) {
          if (ucum.startsWith(metricAcronym[p])) {
            ucum = ucum.substring(metricAcronym[p].length());
            udunits.append(metricAcronym[p]);
            if (ucum.length() == 0) {
              udunits.append("{count}");
              return udunits.toString();
            }
            caughtPrefix = true;
            continue MAIN;
          }
        }
      }

      // try to separate out a twoAcronym prefix (e.g., "Ki")
      if (!caughtPrefix) {
        for (int p = 0; p < nTwo; p++) {
          if (ucum.startsWith(twoAcronym[p])) {
            ucum = ucum.substring(twoAcronym[p].length());
            char udch = udunits.length() > 0 ? udunits.charAt(udunits.length() - 1) : '\u0000';
            if (udch != '\u0000' && udch != '.' && udch != '/') udunits.append('.');
            if (ucum.length() == 0) {
              udunits.append("{count}");
              return udunits.toString();
            }
            udunits.append(twoValue[p] + ".");
            caughtPrefix = true;
            continue MAIN;
          }
        }
      }

      // ends in comment?  try to just convert the beginning
      int po1 = oldUcum.lastIndexOf('{');
      if (po1 > 0 && oldUcum.endsWith("}"))
        return oneUcumToUdunits(oldUcum.substring(0, po1))
            + "("
            + oldUcum.substring(po1 + 1, oldUcum.length() - 1)
            + ")";

      // no change? failure; return original ucum
      //  because I don't want to change "exact" into "ect"
      // (i.e., seeing a metric prefix that isn't a metric prefix)
      if (debugMode)
        String2.log("Units2.oneUcumToUdunits fail: udunits=" + udunits + " ucum=" + ucum);
      // udunits.append(ucum);
      return oldUcum;
    }
  }

  /**
   * This makes a HashMap&lt;String, String&gt; from the ResourceBundle-like file's keys=values. The
   * key and value strings are trim()'d.
   *
   * @param resourceFile the full file name of the key=value file.
   * @param charset e.g., ISO-8859-1; or "" or null for the default
   * @throws RuntimeException if trouble
   */
  public static Map<String, String> getHashMapStringString(URL resourceFile, String charset)
      throws RuntimeException {
    try {
      Map ht = new HashMap();
      List<String> sar = Resources.readLines(resourceFile, Charset.forName(charset));
      int n = sar.size();
      int i = 0;
      while (i < n) {
        String s = sar.get(i++);
        if (s.startsWith("#")) continue;
        while (i < n && s.endsWith("\\")) s = s.substring(0, s.length() - 1) + sar.get(i++);
        int po = s.indexOf('=');
        if (po < 0) continue;
        // new String: so not linked to big source file's text
        ht.put(new String(s.substring(0, po).trim()), new String(s.substring(po + 1).trim()));
      }
      return ht;
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /**
   * This is used by Bob a few times to find udunits2 units that aren't yet in
   * UdunitsToUcum.properties. The source files are c:/programs/udunits-2.1.9/lib/udunits2-XXX.xml
   * No one else will need to use this.
   */
  public static void checkUdunits2File(String fileName) throws Exception {
    String2.log("\n*** Units2.checkUdUnits2File " + fileName);
    StringArray sa = StringArray.fromFileUtf8(fileName);
    StringArray names = new StringArray();
    int n = sa.size();
    int nDef = 0, nPlural = 0, nSingular = 0, nSymbol = 0;
    for (int i = 0; i < n; i++) {
      String s = sa.get(i);
      int po1;

      po1 = s.indexOf("<def>");
      if (po1 >= 0) {
        po1 += 5;
        int po2 = s.indexOf("</def>", po1);
        if (po2 >= 0) {
          String term = s.substring(po1, po2);
          if (term.indexOf(' ') < 0
              && term.indexOf('/') < 0
              && term.indexOf('.') < 0
              && term.indexOf('^') < 0
              && udHashMap.get(term) == null) {
            names.add(term);
            nDef++;
          }
        }
      }

      po1 = s.indexOf("<singular>");
      if (po1 >= 0) {
        po1 += 10;
        int po2 = s.indexOf("</singular>", po1);
        if (po2 >= 0) {
          String term = s.substring(po1, po2);
          if (term.indexOf(' ') < 0
              && term.indexOf('/') < 0
              && term.indexOf('.') < 0
              && term.indexOf('^') < 0
              && udHashMap.get(term) == null) {
            names.add(term);
            nSingular++;
          }
        }
      }

      po1 = s.indexOf("<plural>");
      if (po1 >= 0) {
        po1 += 8;
        int po2 = s.indexOf("</plural>", po1);
        if (po2 >= 0) {
          String term = s.substring(po1, po2);
          if (term.indexOf(' ') < 0
              && term.indexOf('/') < 0
              && term.indexOf('.') < 0
              && term.indexOf('^') < 0
              && udHashMap.get(term) == null) {
            names.add(term);
            nPlural++;
          }
        }
      }

      po1 = s.indexOf("<symbol>");
      if (po1 >= 0) {
        po1 += 8;
        int po2 = s.indexOf("</symbol>", po1);
        if (po2 >= 0) {
          String term = s.substring(po1, po2);
          if (term.indexOf(' ') < 0
              && term.indexOf('/') < 0
              && term.indexOf('.') < 0
              && term.indexOf('^') < 0
              && udHashMap.get(term) == null) {
            names.add(term);
            nSymbol++;
          }
        }
      }
    }
    names.sortIgnoreCase();
    String2.log(names.toNewlineString());
    String2.log(
        "nDef=" + nDef + " nPlural=" + nPlural + " nSingular=" + nSingular + " nSymbol=" + nSymbol);
  }

  /**
   * This was used by Bob once to generate a crude UcumToUdunits.properties file. No one else will
   * need to use this.
   */
  public static void makeCrudeUcumToUdunits() {
    String2.log("\n*** Units2.makeCrudeUcumToUdunits");
    StringArray sa = new StringArray();
    Object keys[] = udHashMap.keySet().toArray();
    for (int i = 0; i < keys.length; i++) {
      String key = (String) keys[i];
      if (key.endsWith("s") && udHashMap.get(key.substring(0, key.length() - 1)) != null) {
        // skip this plural (a singular exists)
      } else {
        sa.add(String2.left(udHashMap.get(key) + " = ", 21) + key);
      }
    }
    sa.sortIgnoreCase();
    String2.log(sa.toNewlineString());
  }

  /**
   * This checks if two udunits are equivalent (either both are null or "", or equal each other, or
   * have same ucom units). Sometimes different but equivalent (e.g., degrees_north and
   * degree_north).
   */
  public static boolean udunitsAreEquivalent(String ud1, String ud2) {
    if (ud1 == null) ud1 = "";
    if (ud2 == null) ud2 = "";
    if (ud1.equals(ud2)) return true;
    return udunitsToUcum(ud1).equals(udunitsToUcum(ud2));
  }

  // ******************************
  public static UnitFormat unitFormat = ucar.units.StandardUnitFormat.instance();

  /**
   * This tries to return a standardized (lightly canonical) version of a UDUnits string. This is
   * mostly about standardizing syntax and converting synonyms to 1 option. This is just a standard
   * way of writing the units as given (e.g., cm stays as cm, not as 0.01 m). This doesn't convert
   * to low level canonical units as does UDUnits units.getCanonical().
   *
   * @return the standardizedUdunits. If it was null, this returns "".
   */
  public static String safeStandardizeUdunits(String udunits) {
    if (!String2.isSomething(udunits)) return "";
    udunits = udunits.trim(); // so search HM works as expected
    String t = standardizeUdunitsHM.get(udunits); // do exceptions first
    return t == null ? safeUcumToUdunits(safeUdunitsToUcum(udunits)) : t; // do a round trip
  }

  /**
   * This returns the UDUNITS canonical version of the units. It is pretty extreme, e.g., converting
   * Joules to kg m2 s-2 and treating Hz (steady frequency) and becquerel's (radioactivity) as s-1.
   *
   * @return the canonical units (or null if trouble)
   */
  public static String safeCanonicalUdunitsString(String udunits) {
    try {
      Unit units = unitFormat.parse(udunits);
      return units == null ? null : units.getCanonicalString();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * If the variable is packed with scale_factor and/or add_offset, this will unpack the packed
   * attributes of the variable: actual_range, actual_min, actual_max, data_max, data_min,
   * valid_max, valid_min, valid_range. missing_value and _FillValue will be converted to PA
   * standard mv for the the unpacked datatype (or current type if not packed). And units will be
   * standardized.
   *
   * @param atts the set of attributes that will be unpacked/revised.
   * @param varName the var's fullName, for diagnostic messages only
   * @param oPAType the var's initial elementPAType
   */
  public static void unpackVariableAttributes(Attributes atts, String varName, PAType oPAType) {

    if (debugMode) String2.log(">> unpackVariableAttributes for varName=" + varName);

    Attributes newAtts = atts; // the results, so it has a more descriptive name
    Attributes oldAtts = new Attributes(atts); // so we have an unchanged copy to refer to

    // deal with numeric time units
    String oUnits = newAtts.getString("units");
    if (oUnits == null || !String2.isSomething(oUnits)) newAtts.remove("units");
    else if (Calendar2.isNumericTimeUnits(oUnits))
      newAtts.set("units", Calendar2.SECONDS_SINCE_1970); // AKA EDV.TIME_UNITS
    // presumably, String time var doesn't have numeric time units
    else if (Calendar2.isStringTimeUnits(oUnits)) {
    } else // standardize the units
    newAtts.set("units", Units2.safeStandardizeUdunits(oUnits));

    PrimitiveArray unsignedPA = newAtts.remove("_Unsigned");
    boolean unsigned = unsignedPA != null && "true".equals(unsignedPA.toString());
    PrimitiveArray scalePA = newAtts.remove("scale_factor");
    PrimitiveArray addPA = newAtts.remove("add_offset");

    // if present, convert _FillValue and missing_value to PA standard mv
    PAType destPAType =
        scalePA != null
            ? scalePA.elementType()
            : addPA != null
                ? addPA.elementType()
                : unsigned && oPAType == PAType.BYTE
                    ? PAType.UBYTE
                    : // was PAType.SHORT :  //similar code below
                    unsigned && oPAType == PAType.SHORT
                        ? PAType.USHORT
                        : // was PAType.INT :
                        unsigned && oPAType == PAType.INT
                            ? PAType.UINT
                            : // was PAType.DOUBLE : //ints are converted to double because nc3
                            // doesn't support long
                            unsigned && oPAType == PAType.LONG
                                ? PAType.ULONG
                                : // was PAType.DOUBLE : //longs are converted to double (not ideal)
                                oPAType;
    if (newAtts.remove("_FillValue") != null)
      newAtts.set("_FillValue", PrimitiveArray.factory(destPAType, 1, ""));
    if (newAtts.remove("missing_value") != null)
      newAtts.set("missing_value", PrimitiveArray.factory(destPAType, 1, ""));

    // if var isn't packed, we're done
    if (!unsigned && scalePA == null && addPA == null) return;

    // var is packed, so unpack all packed numeric attributes
    // lookForStringTimes is false because these are all attributes of numeric variables
    if (debugMode)
      String2.log(
          ">> before unpack "
              + varName
              + " unsigned="
              + unsigned
              + " scale_factor="
              + scalePA
              + " add_offset="
              + addPA
              + " actual_max="
              + oldAtts.get("actual_max")
              + " actual_min="
              + oldAtts.get("actual_min")
              + " actual_range="
              + oldAtts.get("actual_range")
              + " data_max="
              + oldAtts.get("data_max")
              + " data_min="
              + oldAtts.get("data_min")
              + " valid_max="
              + oldAtts.get("valid_max")
              + " valid_min="
              + oldAtts.get("valid_min")
              + " valid_range="
              + oldAtts.get("valid_range"));

    // attributes in nc3 files are never unsigned

    // if scale and/or addOffset, then remove redundant related atts
    if (scalePA != null || addPA != null) {
      newAtts.remove("Intercept");
      newAtts.remove("Slope");
      String ss = newAtts.getString("Scaling");
      if ("linear".equals(ss)) {
        // remove if Scaling=linear
        newAtts.remove("Scaling");
        newAtts.remove("Scaling_Equation");
      }
    }

    // before erddap v1.82, ERDDAP said actual_range had packed values.
    // but CF 1.7 says actual_range is unpacked.
    // So look at data types and guess which to do, with preference to believing they're already
    // unpacked
    // Note that at this point in this method, we're dealing with a packed data variable
    if (destPAType != null && (destPAType == PAType.FLOAT || destPAType == PAType.DOUBLE)) {
      for (int i = 0; i < Attributes.signedToUnsignedAttNames.length; i++) {
        String name = Attributes.signedToUnsignedAttNames[i];
        PrimitiveArray pa = newAtts.get(name);
        if (pa != null && !(pa instanceof FloatArray) && !(pa instanceof DoubleArray))
          newAtts.set(name, oldAtts.unpackPA(varName, pa, false, false));
      }
    }

    if (debugMode)
      String2.log(
          ">> after  unpack "
              + varName
              + " unsigned="
              + unsigned
              + " actual_max="
              + newAtts.get("actual_max")
              + " actual_min="
              + newAtts.get("actual_min")
              + " actual_range="
              + newAtts.get("actual_range")
              + " data_max="
              + newAtts.get("data_max")
              + " data_min="
              + newAtts.get("data_min")
              + " valid_max="
              + newAtts.get("valid_max")
              + " valid_min="
              + newAtts.get("valid_min")
              + " valid_range="
              + newAtts.get("valid_range"));
  }
}
