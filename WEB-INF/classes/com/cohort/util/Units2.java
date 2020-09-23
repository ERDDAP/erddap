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
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap; 
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.ResourceBundle;

import ucar.units.Unit;
import ucar.units.UnitFormat;

/** 
 * This class has static methods to convert units from one standard to another.
 * An old local copy of UDUnits 1 info is at c:/programs/udunits-1.12.4/udunits.txt .
 * See the UCUM validator at https://ucum.nlm.nih.gov/ucum-lhc/demo.html .
 */
public class Units2 { 

    /** UDUNITS and UCUM support metric prefixes. */
    public static String metricName[] = {
        "yotta", "zetta", "exa",   "peta",  "tera", 
        "giga",  "mega",  "kilo",  "hecto", "deka", 
        "deci",  "centi", "milli", "micro", "nano", 
        "pico",  "femto", "atto",  "zepto", "yocto", 
        "µ",     };
    public static String metricAcronym[] = {
        "Y", "Z", "E", "P", "T", 
        "G", "M", "k", "h", "da",
        "d", "c", "m", "u", "n", 
        "p", "f", "a", "z", "y",
        "u"};
    public static int nMetric = metricName.length;

    /** UCUM supports power-of-two prefixes, but UDUNITS doesn't. */
    public static String twoAcronym[] = {
        "Ki", "Mi", "Gi", "Ti"};
    public static String twoValue[] = {
        "1024", "1048576", "1073741824", "1.099511627776e12"}; 
    public static int nTwo = twoAcronym.length;

    //these don't need to be thread-safe because they are read-only after creation
    private static HashMap<String,String> udHashMap = getHashMapStringString(
        String2.webInfParentDirectory() + "WEB-INF/classes/com/cohort/util/UdunitsToUcum.properties", 
        String2.ISO_8859_1);
    private static HashMap<String,String> ucHashMap = getHashMapStringString(
        String2.webInfParentDirectory() + "WEB-INF/classes/com/cohort/util/UcumToUdunits.properties", 
        String2.ISO_8859_1);

    //these special cases are usually populated by EDStatic static constructor, but don't have to be
    public static HashMap<String,String> standardizeUdunitsHM = new HashMap();
    public static HashMap<String,String> ucumToUdunitsHM = new HashMap();
    public static HashMap<String,String> udunitsToUcumHM = new HashMap();

    /**
     * Set this to true (by calling reallyReallyVerbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false; 
    public static boolean debugMode = false; 


    /** 
     * This is like udunitsToUcum() but won't throw an exception. 
     * If there is trouble, it returns udunits.
     */
    public static String safeUdunitsToUcum(String udunits) {
        try {
           return udunitsToUcum(udunits); 
        } catch (Throwable t) {
            String2.log(String2.ERROR + " while converting udunits=" + udunits + " to ucum:\n" +
                MustBe.throwableToString(t));
            return udunits;
        }
    }
        

    /** 
     * This converts UDUnits to UCUM.
     * <br>UDUnits: https://www.unidata.ucar.edu/software/udunits/
     *   I worked with v 2.1.9
     * <br>UCUM: https://unitsofmeasure.org/ucum.html
     *   I worked with Version: 1.8, $Revision: 28894 $
     *
     * <p>UDUnits supports lots of aliases (short and long)
     *   and plurals (usually by adding 's'). 
     *   These all get reduced to UCUM's short and canonical-only units.
     *
     * <p>Notes: 
     * <ul>
     * <li>This method is a strictly case sensitive.
     *   The only UDUnits that should be capitalized (other than acronyms) are
     *     Btu, Gregorian..., Julian..., PI.
     *   <br>The only UDUnits that may be capitalized are
     *     Celsius, Fahrenheit, Kelvin, Rankine.
     * <li>For "10 to the", UCUM allows 10* or 10^. This method uses 10^.
     * <li>NTU becomes {ntu}.
     * <li>PSU or psu becomes {psu}.
     * </ul>
     *
     * return the UDUnits converted to UCUM.
     *    null returns null. "" returns "".
     * throws Exception if trouble.
     */
    public static String udunitsToUcum(String udunits) {
        if (udunits == null)
            return null;
        udunits = udunits.trim();
        if (debugMode) 
            String2.log(">> convert udunits=" + udunits);
        if (!String2.isSomething2(udunits))
            return "";

        //specified in <udunitsToUcum> in messages.xml
        String t = udunitsToUcumHM.get(udunits);
        if (t != null)
            return t;

        //before test " since " times
        udunits = String2.replaceAll(udunits, " since analysis", "");

        //is it a point in time? e.g., seconds since 1970-01-01T00:00:00T
        int sincePo = udunits.toLowerCase().indexOf(" since ");
        if (sincePo > 0) {  //more general than Calendar2.isNumericTimeUnits(udunits)) so allow other base times, e.g., Jan 1, 2000
            try {
                //test if really appropriate
                double baf[] = Calendar2.getTimeBaseAndFactor(udunits); //throws exception if trouble

                //use 'factor', since it is more forgiving than udunitsToUcum converter
                String u = baf[1] == 0.001? "ms" :
                           baf[1] == 1? "s"  :
                           baf[1] == Calendar2.SECONDS_PER_MINUTE? "min" :
                           baf[1] == Calendar2.SECONDS_PER_HOUR? "h" :
                           baf[1] == Calendar2.SECONDS_PER_DAY? "d" :
                           baf[1] == 30 * Calendar2.SECONDS_PER_DAY? "mo" :  //mo_j?
                           baf[1] == 360 * Calendar2.SECONDS_PER_DAY? "a" : //a_j?
                           udunitsToUcum(udunits.substring(0, sincePo)); //shouldn't happen, but weeks? microsec?

                //make "s{since 1970-01-01T00:00:00T}
                String sinceDate = udunits.substring(sincePo + 7);
                String tryToIso = Calendar2.tryToIsoString(sinceDate);
                return u + "{since " + (tryToIso.length() > 0? tryToIso : sinceDate) + "}";                 
            } catch (Exception e) { 
                if (verbose) String2.log("Caught: " + MustBe.throwableToString(e));
            }
        }

        //is it a time format string 
        if (udunits.indexOf("yy") >= 0) //more forgiving than Calendar2.isStringTimeUnits(
            return "{" + udunits + "}";

        //*** SPECIAL CASES / FIX UPS
        //replace all ** with ^
        udunits = String2.replaceAll(udunits, "**", "^");

        //remove all <sup> and </sup>
        udunits = String2.replaceAll(udunits, "<sup>",  "");
        udunits = String2.replaceAll(udunits, "</sup>", "");

        //change _per_ to " per "
        udunits = String2.replaceAll(udunits, "_per_", " per ");

        //these are comments, not units   set to ""???
        if (udunits.equals("8 bits, encoded") ||
            udunits.equals("biomass density unit per abundance unit") ||
            udunits.equals("level") ||
            udunits.equals("link") ||
            udunits.equals("local time") ||
            udunits.equals("mean") ||            
            udunits.equals("mm water per 1.38m soil (0 to 7 layers)") ||
            udunits.equals("sigma") ||
            udunits.equals("sigma_level") ||
            udunits.equals("site") ||
            //udunits.equals("") ||
            udunits.equals("Standardized Units of Relative Dry and Wet") ||
            udunits.equals("Total Scale") ||
            udunits.equals("varies by taxon/group (see description)"))
            return "{" + udunits + "}";

        if (udunits.equals("1-12")) return "mo";
        if (udunits.equals("1-31")) return "d";

        udunits = String2.replaceAll(udunits, "atomosphere", "atmosphere"); //misspelling 

        udunits = String2.replaceAll(udunits, "Centimeter", "centimeter");  //beware centimeters (so not -> cm)

        if (udunits.equals("count per 85 centimeter^2 per day"))
            return "{count}.(85.cm2)-1.d-1";

        udunits = String2.replaceAll(udunits, "cubic kilometers", "km3");
        udunits = String2.replaceAll(udunits, "cubic kilometer", "km3");
        udunits = String2.replaceAll(udunits, "cubic meters", "m3");
        udunits = String2.replaceAll(udunits, "cubic meter", "m3");

        if (udunits.indexOf(" C") > 0) {
            udunits = String2.replaceAll(udunits, "deg C",     "degree_C");
            udunits = String2.replaceAll(udunits, "degree C",  "degree_C");
            udunits = String2.replaceAll(udunits, "degrees C", "degree_C");
        }

        if (udunits.equals("dd") ||
            udunits.equals("dd (01 to 31)") ||
            udunits.equals("day_of_year"))
            return "d";

        if (udunits.startsWith("decimal"))  //hours, minutes, seconds, degrees
            udunits = udunits.substring(7).trim();

        if (udunits.equals("degrees (+E)") ||
            udunits.equals("degrees-east"))
            return "deg{east}";
        if (udunits.equals("degrees (+N)") ||
            udunits.equals("degrees-north"))
            return "deg{north}";

        if (udunits.equals("Degrees, Oceanographic Convention, 0=toward N, 90=toward E") ||
            udunits.equals("degrees (clockwise from true north)") ||
            udunits.equals("degrees (clockwise towards true north)"))
            return "deg{true}";

        if (udunits.equals("degree(azimuth)") ||
            udunits.equals("Degrees(azimuth)") ||
            udunits.equals("degrees(azimuth)"))
            return "deg{azimuth}";

        if (udunits.equals("degree(clockwise from bow)") ||
            udunits.equals("degree (clockwise from bow)") ||
            udunits.equals("degrees (clockwise from bow)"))
            return "deg{clockwise from bow}";

        if (udunits.startsWith("four digit "))  //year
            udunits = udunits.substring(11);

        if (udunits.startsWith("dyn-"))  
            udunits = "dynamic " + udunits.substring(4);

        if (udunits.equals("(0 - 1)") ||
            udunits.equals("frac.")   ||
            udunits.equals("fraction (between 0 and 1)") ||
            udunits.equals("none (fraction)"))   
            return "1";  

        if (udunits.equals("gram per one tenth meter^2"))
            return "g.m-2.10-2";

        udunits = String2.replaceAll(udunits, "MM:SS", "mm:ss");
        if (udunits.startsWith("HH"))
            return "{" + udunits + "}";

        udunits = String2.replaceAll(udunits, "Kilo", "kilo");
        udunits = String2.replaceAll(udunits, "Kg",   "kg");
        udunits = String2.replaceAll(udunits, "Km",   "km");

        if (udunits.equals("mb")) //more likely mbar than m barn
            return "mbar";

        udunits = String2.replaceAll(udunits, "micro-", "micro");

        if (udunits.equals("MM/HR"))
            return "mm.h-1";

        if (udunits.equals("mm/mn"))  // /month?
            return "mm.mo-1";

        if (udunits.equals("mm (01 to 12)"))  //month
            return "mo";

        if (udunits.startsWith("molm-2"))
            udunits = "mol m-2 " + udunits.substring(6);

        if (udunits.equals("nominal day"))
            return "d";

        if (udunits.equals("number of cells")   ||
            udunits.equals("number of observations") ||
            udunits.equals("Obs count") ||
            udunits.equals("observations"))   
            return "{count}";

        if (udunits.equals("numeric"))
            return "1";

        if (udunits.equals("pa"))
            return "Pa";

        udunits = String2.replaceAll(udunits, "parts per 1000000", "ppm");
        udunits = String2.replaceAll(udunits, "parts per million", "ppm");

        udunits = String2.replaceAll(udunits, "percentage", "%");  //avoids problems below
        udunits = String2.replaceAll(udunits, "percent", "%");     //avoids problems below

        udunits = String2.replaceAll(udunits, "photon flux", "photon_flux");

        udunits = String2.replaceAllIgnoreCase(udunits, "practical salinity units", "PSU");

        if (udunits.equals("PSS-78") ||
            udunits.equals("PSS_78"))
            return "{PSS_78}";  //not hyphen, because it looks like exponent

        udunits = String2.replaceAll(udunits, "psu-m", "PSU m");

        if (udunits.equals("per mil relative to NIST SRM 3104a") ||  //per mil is ppm, not length?
            udunits.equals("ppm relative to NIST SRM 3104a"))  
            return "[ppm].{relative to NIST SRM 3104a}"; //NIST SRM 3104a is a standard Barium solution

        udunits = String2.replaceAll(udunits, "square centimeters", "cm2");
        udunits = String2.replaceAll(udunits, "square centimeter", "cm2");
        udunits = String2.replaceAll(udunits, "square kilometers", "km2");
        udunits = String2.replaceAll(udunits, "square kilometer", "km2");
        udunits = String2.replaceAll(udunits, "square meters", "m2");
        udunits = String2.replaceAll(udunits, "square meter", "m2");

        if (udunits.startsWith("two digit "))  //e.g. month, day, hour, minute, second
            udunits = udunits.substring(10);

        if (udunits.toLowerCase().equals("unitless"))
            return "1";

        if (udunits.equals("volts (0-5 FSO)"))
            return "V";

        if (udunits.startsWith("W/M"))
            udunits = "W/m" + udunits.substring(3);

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


        //parse udunits and build ucum, till done
        StringBuilder ucum = new StringBuilder();        
        int udLength = udunits.length();
        boolean dividePending = false;
        int po = 0;  //po is next position to be read
        while (po < udLength) {
            if (debugMode && ucum.length() > 0) String2.log("  ucum=" + ucum);
            char ch = udunits.charAt(po);

            //leave {a comment} unchanged
            if (ch == '{') {
                int po2 = po + 1;
                while (po2 < udLength && udunits.charAt(po2) != '}') 
                    po2++;
                ucum.append(udunits.substring(po, po2 + 1)); 
                po = po2 + 1;
                continue;
            }

            //PER
            if (po <= udLength - 3) {
                if (udunits.substring(po, po + 3).toLowerCase().equals("per") &&
                    (po + 3 == udLength || !String2.isLetter(udunits.charAt(po + 3)))) {
                    dividePending = !dividePending;
                    po += 3;
                    continue;
                }
            }

            //letter  
            if (isUdunitsLetter(ch)) {     //includes 'µ' and '°'
                //find contiguous letters|_|digit|-|^ 
                int po2 = po + 1;
                while (po2 < udLength && 
                    (isUdunitsLetter(udunits.charAt(po2)) || 
                     udunits.charAt(po2) == '_' ||
                     String2.isDigit(udunits.charAt(po2)) ||
                    (udunits.charAt(po2) == '-' && po2+1 < udLength &&   //-exponent
                        String2.isDigit(udunits.charAt(po2+1))) ||
                    (udunits.charAt(po2) == '^' && po2+1 < udLength &&   //^exponent
                        (udunits.charAt(po2+1) == '-' || String2.isDigit(udunits.charAt(po2+1)))))) 
                    po2++;
                String tUdunits = udunits.substring(po, po2);
                tUdunits = String2.replaceAll(tUdunits, "^", "");
                po = po2;

                //some udunits have internal digits, but none end in digits 
                //if it ends in digits, treat as exponent
                //find contiguous digits at end
                int firstDigit = tUdunits.length();
                while (firstDigit >= 1 && String2.isDigit(tUdunits.charAt(firstDigit - 1)))
                    firstDigit--;
                if (firstDigit < tUdunits.length() &&       //there are digits
                    tUdunits.charAt(firstDigit - 1) == '-') //there's a '-' too
                    firstDigit--;                           //include it
                String exponent = tUdunits.substring(firstDigit);
                tUdunits = tUdunits.substring(0, firstDigit);
                String tUcum = oneUdunitsToUcum(tUdunits);
                if (debugMode) String2.log(">> tUdunits=" + tUdunits + " one=" + tUcum + " exp=" + exponent);

                if (tUcum.length() > 0 && ucum.toString().equals("1."))
                    ucum.setLength(0);
                ucum.append(tUcum);

                //add the exponent   (always positive here)
                if (exponent.length() > 0) {
                    if (dividePending) {
                        if (exponent.charAt(0) == '-')
                            exponent = exponent.substring(1);
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

            //number
            if (ch == '-' || String2.isDigit(ch)) {
                //find contiguous digits
                int po2 = po + 1;
                while (po2 < udLength && String2.isDigit(udunits.charAt(po2))) 
                    po2++;

                //decimal place + digit (not just .=multiplication)
                boolean hasDot = false;
                if (po2 < udLength-1 && udunits.charAt(po2) == '.' && String2.isDigit(udunits.charAt(po2+1))) {
                    hasDot = true;
                    po2 += 2;
                    while (po2 < udLength && String2.isDigit(udunits.charAt(po2))) 
                        po2++;
                }

                //exponent?     e-  or e{digit}
                boolean hasE = false;
                if (po2 < udLength-1 && Character.toLowerCase(udunits.charAt(po2)) == 'e' &&
                    (udunits.charAt(po2+1) == '-' || String2.isDigit(udunits.charAt(po2+1)))) {
                    hasE = true;
                    po2 += 2;
                    while (po2 < udLength && String2.isDigit(udunits.charAt(po2))) 
                        po2++;
                }
                String num = udunits.substring(po, po2);
                po = po2;

                //convert floating point to rational number
                if (hasDot || hasE || dividePending) {
                    double d = String2.parseDouble(num);
                    if (dividePending) {
                        d = 1 / d;
                        dividePending = false;
                    }
                    int rational[] = String2.toRational(d);
                    if (rational[1] == Integer.MAX_VALUE)
                        ucum.append(num); //ignore the trouble !!! ???
                    else if (rational[1] == 0) //includes {0, 0}
                        ucum.append(rational[0]);
                    else if (rational[0] == 1)
                        ucum.append("10^" + rational[1]);
                    else ucum.append(rational[0] + ".10^" + rational[1]);

                } else {
                    //just copy num
                    ucum.append(num);
                }                

                continue;
            }

            //space,*,.,(183)    (multiplication)
            if (ch == ' ' || ch == '*' || ch == '.' || ch == 183) {
                po++;
                if (ucum.length() > 0 && ucum.charAt(ucum.length() - 1) != '.')
                    ucum.append('.');
                continue;
            }

            // /
            if (ch == '/') {
                po++;
                dividePending = !dividePending;
                if (ucum.length() > 0 && ucum.charAt(ucum.length() - 1) != '.')
                    ucum.append('.');
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

            //otherwise, punctuation.   copy it
            ucum.append(ch);
            po++;
        }

        if (String2.startsWith(ucum, "m.m."))
            return "m2." + ucum.substring(4);

        //if it is "a (a)" return "a"
        int ppo = ucum.indexOf(".(");
        if (ppo > 0 && ucum.indexOf("(", ppo + 2) < 0 &&
            ucum.charAt(ucum.length() - 1) == ')' &&
            ucum.substring(0, ppo).equals(ucum.substring(ppo + 2, ucum.length() - 1)))
            return ucum.substring(0, ppo);

        //if it is "a [a]" return "a"
        ppo = ucum.indexOf(".[");
        if (ppo > 0 && ucum.indexOf("[", ppo + 2) < 0 &&
            ucum.charAt(ucum.length() - 1) == ']' &&
            ucum.substring(0, ppo).equals(ucum.substring(ppo + 2, ucum.length() - 1)))
            return ucum.substring(0, ppo);

        return ucum.toString();
    }

    private static boolean isUdunitsLetter(char ch) {
        return String2.isLetter(ch) || ch == 'µ' || ch == '°';
    }

    /**
     * This converts one udunits term (perhaps with metric prefix(es)) to the corresponding ucum string.
     * If udunits is just metric prefix(es), this returns the prefix acronym(s) with "{count}" as suffix
     *   (e.g., dkilo returns dk{count}).
     * If this can't completely convert udunits, it returns the original udunits as a comment
     *   (e.g., kiloBobs becomes {kiloBobs}  (to avoid 'exact' becoming 'ect' ).
     * For placeholder strings ("null"), this return "".
     */
    private static String oneUdunitsToUcum(String udunits) {


        //repeatedly pull off start of udunits and build ucum, till done
        String oldUdunits = udunits;
        StringBuilder ucum = new StringBuilder();        
        boolean hasPrefix = false;
        if (debugMode) 
            String2.log(ucum.length() == 0? "" : "  ucum=" + ucum + " udunits=" + udunits);

        //try to find udunits in hashMap
        if (!String2.isSomething2(udunits))
            return "";            
        udunits = udunits.trim();
        String tUcum = udHashMap.get(udunits);
        if (tUcum != null) {
            //success! done!
            ucum.append(tUcum);
            return ucum.toString();
        }

        //try to separate out a metricName prefix (e.g., "kilo")
        for (int p = 0; p < nMetric; p++) {
            if (udunits.startsWith(metricName[p])) {
                String tUd = udunits.substring(metricName[p].length());
                if (tUd.length() == 0) {
                    ucum.append(metricAcronym[p] + "{count}");  //standardize on acronym
                    return ucum.toString();
                }
                String newUd = udHashMap.get(tUd);
                if (String2.isSomething(newUd)) {
                    ucum.append(metricAcronym[p] + newUd);  //standardize on acronym
                    return ucum.toString();
                }
            }
        }

        //try to separate out a metricAcronym prefix (e.g., "k")
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

        //no change? failure; return original udunits as a comment
        //  because I don't want to change "exact" into "ect" 
        //  (i.e., seeing a metric prefix that isn't a metric prefix)
        if (debugMode) String2.log("Units2.oneUdunitsToUcum fail: ucum=" + 
            ucum + " udunits=" + udunits);
        //ucum.append(udunits);
        return "{" + oldUdunits + "}"; //ucum.toString();
    }


    /**
     * This tests udunitsToUcum.
     * The most likely bugs are:
     * <ul>
     * <li> Excess/incorrect substitutions, e.g., "avoirdupois_pounds" to "[lb_av]" to "[[lb_av]_av]".
     *   <br>This is the only thing that this method has pretty good tests for.
     * <li> Plural vs Singular conversions
     * <li> Typos 
     * <li> Misunderstanding (e.g., use of [H2O])
     * </ul>
     *
     * @throws RuntimeException if trouble
     */
    public static void testUdunitsToUcum() {

        String2.log("\n*** Units2.testUdunitsToUcum");
        //debugMode = true;

        //time point (try odd time units)
        testUdunitsToUcum("seconds since 1970-1-1T00:00:00Z", "s{since 1970-01-01T00:00:00Z}");
        testUdunitsToUcum("millisec since 1971-02-03T01", "ms{since 1971-02-03T01:00:00Z}");
        testUdunitsToUcum("sec since 1971-02-03T00:00:00-4:00", "s{since 1971-02-03T04:00:00Z}");
        testUdunitsToUcum("min since 1971-02-03", "min{since 1971-02-03}");
        testUdunitsToUcum("hr since 1971-02-03", "h{since 1971-02-03}");
        testUdunitsToUcum("days since 1971-02-03", "d{since 1971-02-03}");
        testUdunitsToUcum("months since 1971-02-03", "mo{since 1971-02-03}");  //mo_j?
        testUdunitsToUcum("yrs since 1971-02-03", "a{since 1971-02-03}"); //a_j?
        testUdunitsToUcum("zztops since 1971-02-03", "{zztops}.{since}.1971-02-03"); //not a point in time

        testUdunitsToUcum("-", ""); //!String2.isSomething2()
        testUdunitsToUcum(".", ""); //!String2.isSomething2()
        testUdunitsToUcum("n/a", ""); //!String2.isSomething2()
        testUdunitsToUcum("N/A", ""); //!String2.isSomething2()
        testUdunitsToUcum("null", ""); //!String2.isSomething2()
        testUdunitsToUcum("Unknown", ""); //!String2.isSomething2()
        testUdunitsToUcum("?", ""); //!String2.isSomething2()


        //main alphabetical section
        testUdunitsToUcum("abampere",          "(10.A)");
        testUdunitsToUcum("abfarad",           "GF");
        testUdunitsToUcum("abhenry",           "nH");
        testUdunitsToUcum("abmho",             "GS");
        testUdunitsToUcum("abohm",             "nO");
        testUdunitsToUcum("abvolt",            "(V/10^8)");
        testUdunitsToUcum("acre_feet",         "([acr_us].ft)");
        testUdunitsToUcum("acre_foot",         "([acr_us].ft)");
        testUdunitsToUcum("acre",              "[acr_us]");
        testUdunitsToUcum("ampere",            "A");
        testUdunitsToUcum("amp",               "A");
        testUdunitsToUcum("amu",               "u");
        testUdunitsToUcum("Å",                 "Ao");
        testUdunitsToUcum("Ångström",          "Ao");  
        testUdunitsToUcum("angstrom",          "Ao");  
        testUdunitsToUcum("angular_degree",    "deg");  
        testUdunitsToUcum("angular_minute",    "'");  
        testUdunitsToUcum("angular_second",    "''");  //it doesn't look like "!
        testUdunitsToUcum("apostilb",          "(cd/[pi]/m2)");
        testUdunitsToUcum("apdram",            "[dr_ap]");
        testUdunitsToUcum("apothecary_dram",   "[dr_ap]");
        testUdunitsToUcum("apothecary_ounce",  "[oz_ap]");
        testUdunitsToUcum("apothecary_pound",  "[lb_ap]");
        testUdunitsToUcum("apounce",           "[oz_ap]");
        testUdunitsToUcum("appound",           "[lb_ap]");
        testUdunitsToUcum("arcdeg",            "deg");
        testUdunitsToUcum("arcminute",         "'");  
        testUdunitsToUcum("arcsecond",         "''");  //it doesn't look like "!
        testUdunitsToUcum("are",               "ar");  
        testUdunitsToUcum("arpentlin",         "(191835/1000.[ft_i])");   //101.835
        testUdunitsToUcum("assay_ton",         "(2916667/10^8.kg)");      //2.916667e-2
        testUdunitsToUcum("astronomical_unit", "AU");
        testUdunitsToUcum("at",                "att"); 
        testUdunitsToUcum("atmosphere",        "atm");
        testUdunitsToUcum("atomic_mass_unit",  "u");
        testUdunitsToUcum("atomicmassunit",    "u");        
        testUdunitsToUcum("avogadro_constant", "mol");  //???
        testUdunitsToUcum("avoirdupois_ounce", "[oz_av]");
        testUdunitsToUcum("avoirdupois_pound", "[lb_av]");

        testUdunitsToUcum("bag",               "(94.[lb_av])");
        testUdunitsToUcum("bakersdozen",       "13");
        testUdunitsToUcum("barie",             "(dN/m2)");
        testUdunitsToUcum("barleycorn",        "([in_i]/3)");
        testUdunitsToUcum("barn",              "b");
        testUdunitsToUcum("barrel",            "[bbl_us]");
        testUdunitsToUcum("bars",              "bar");
        testUdunitsToUcum("baud",              "Bd");
        testUdunitsToUcum("barye",             "(dN/m2)");
        testUdunitsToUcum("bbl",               "[bbl_us]");
        testUdunitsToUcum("becquerel",         "Bq");
        testUdunitsToUcum("bel",               "B");
        testUdunitsToUcum("bev",               "GeV");
        testUdunitsToUcum("big_point",         "([in_i]/72)");
        testUdunitsToUcum("biot",              "Bi");
        testUdunitsToUcum("bit",               "bit");
        testUdunitsToUcum("blondel",           "(cd/[pi]/m2)");
        testUdunitsToUcum("board_feet",        "[bf_i]");
        testUdunitsToUcum("board_foot",        "[bf_i]");
        testUdunitsToUcum("boiler_horsepower", "(98095/10.W)"); //9.80950e3
        testUdunitsToUcum("bps",               "(bit/s)");
        testUdunitsToUcum("Btu",               "[Btu_IT]");
        testUdunitsToUcum("bushel",            "[bu_us]");
        testUdunitsToUcum("bu",                "[bu_us]");
        testUdunitsToUcum("byte",              "By");

        testUdunitsToUcum("C",                 "Cel"); //Non-standard: In udunits and ucum, "C" means coulomb, but I treat as Celsius
        testUdunitsToUcum("c",                 "[c]"); //can't deal with "c" -> "[c]" (velocity of light) without parsing everything (since it is also a prefix)
        testUdunitsToUcum("C12_faraday",       "(9648531/100.C)"); //9.648531e4
        testUdunitsToUcum("cal",               "cal_IT");
        testUdunitsToUcum("calorie",           "cal_IT");
        testUdunitsToUcum("Canadian_liquid_gallon", "(454609/10^8.m3)"); //4.546090e-3
        testUdunitsToUcum("candela",           "cd");
        testUdunitsToUcum("candle",            "cd");
        testUdunitsToUcum("carat",             "[car_m]");
        testUdunitsToUcum("cc",                "cm3");
        testUdunitsToUcum("celsius",           "Cel");
        testUdunitsToUcum("Celsius",           "Cel");
        testUdunitsToUcum("chemical_faraday",  "(964957/10.C)"); //9.64957e4
        testUdunitsToUcum("circle",            "circ");
        testUdunitsToUcum("circular_mil",      "[cml_i]");
        testUdunitsToUcum("clo",               "(155/1000.K.m2/W)"); //1.55e-1
        testUdunitsToUcum("cmH2O",             "cm[H2O]");  
        testUdunitsToUcum("cmHg",              "cm[Hg]");  
        testUdunitsToUcum("common_year",       "(365.d)");
        testUdunitsToUcum("conventional_mercury", "[Hg]");  
        testUdunitsToUcum("conventional_water",   "[H2O]");  
        testUdunitsToUcum("coulomb",           "C");
        testUdunitsToUcum("count",             "{count}");    //no perfect UCUM conversion
        testUdunitsToUcum("cps",               "Hz");
        testUdunitsToUcum("cup",               "[cup_us]");
        testUdunitsToUcum("curie",             "Ci");

        testUdunitsToUcum("darcy",             "(9869233/10^19.m2)"); //9.869233e-13
        testUdunitsToUcum("day",               "d");

        testUdunitsToUcum("degree",            "deg");

        //there are no south options
        testUdunitsToUcum("degree_east",       "deg{east}");
        testUdunitsToUcum("degree_E",          "deg{east}");
        testUdunitsToUcum("degreeE",           "deg{east}");
        testUdunitsToUcum("degrees_east",      "deg{east}");
        testUdunitsToUcum("degrees_E",         "deg{east}");
        testUdunitsToUcum("degreesE",          "deg{east}");

        testUdunitsToUcum("degree_north",      "deg{north}");  
        testUdunitsToUcum("degree_N",          "deg{north}");  
        testUdunitsToUcum("degreeN",           "deg{north}");  
        testUdunitsToUcum("degrees_north",     "deg{north}");  
        testUdunitsToUcum("degrees_N",         "deg{north}");  
        testUdunitsToUcum("degreesN",          "deg{north}");  

        testUdunitsToUcum("degree_true",       "deg{true}");
        testUdunitsToUcum("degree_T",          "deg{true}");
        testUdunitsToUcum("degreeT",           "deg{true}");
        testUdunitsToUcum("degrees_true",      "deg{true}");
        testUdunitsToUcum("degrees_T",         "deg{true}");
        testUdunitsToUcum("degreesT",          "deg{true}");

        testUdunitsToUcum("degree_west",       "deg{west}");
        testUdunitsToUcum("degree_W",          "deg{west}");
        testUdunitsToUcum("degreeW",           "deg{west}");
        testUdunitsToUcum("degrees_west",      "deg{west}");
        testUdunitsToUcum("degrees_W",         "deg{west}");
        testUdunitsToUcum("degreesW",          "deg{west}");

        //most plurals are only in udunits-2
        testUdunitsToUcum("°C",                "Cel");  //udunits-2
        testUdunitsToUcum("degree_centigrade", "Cel");
        testUdunitsToUcum("degree_Celsius",    "Cel");  
        testUdunitsToUcum("degrees_Celsius",   "Cel");  
        testUdunitsToUcum("degC",              "Cel");
        testUdunitsToUcum("degreeC",           "Cel");
        testUdunitsToUcum("degreesC",          "Cel"); 
        testUdunitsToUcum("degree_C",          "Cel");
        testUdunitsToUcum("degrees_C",         "Cel"); 
        testUdunitsToUcum("degree_c",          "Cel");
        testUdunitsToUcum("degrees_c",         "Cel"); 
        testUdunitsToUcum("deg_C",             "Cel");
        testUdunitsToUcum("degs_C",            "Cel");
        testUdunitsToUcum("deg_c",             "Cel");
        testUdunitsToUcum("degs_c",            "Cel");

        //technically not valid
        testUdunitsToUcum("C",                  "Cel");
        testUdunitsToUcum("deg C",              "Cel");
        testUdunitsToUcum("degree C",           "Cel");
        testUdunitsToUcum("degrees C",          "Cel"); 

        //ucum seems to always treat degF as a unit of heat
        //  but C and K can be measures on a scale.
        //  F is a Farad.
        testUdunitsToUcum("°F",                "[degF]");  //udunits-2
        testUdunitsToUcum("degree_Fahrenheit", "[degF]");
        testUdunitsToUcum("degrees_Fahrenheit","[degF]"); //non-standard
        testUdunitsToUcum("degF",              "[degF]");  
        testUdunitsToUcum("degreeF",           "[degF]");
        testUdunitsToUcum("degreesF",          "[degF]"); //non-standard
        testUdunitsToUcum("degree_F",          "[degF]");
        testUdunitsToUcum("degrees_F",         "[degF]"); //non-standard
        testUdunitsToUcum("degree_f",          "[degF]"); 
        testUdunitsToUcum("degrees_f",         "[degF]"); //non-standard
        testUdunitsToUcum("deg_F",             "[degF]");
        testUdunitsToUcum("degs_F",            "[degF]");
        testUdunitsToUcum("deg_f",             "[degF]");
        testUdunitsToUcum("degs_f",            "[degF]");

        testUdunitsToUcum("°K",                "K");  //udunits-2
        testUdunitsToUcum("degree_Kelvin",     "K");
        testUdunitsToUcum("degrees_Kelvin",    "K"); //non-standard
        testUdunitsToUcum("degK",              "K");
        testUdunitsToUcum("degreeK",           "K");
        testUdunitsToUcum("degreesK",          "K"); //non-standard
        testUdunitsToUcum("degree_K",          "K");
        testUdunitsToUcum("degrees_K",         "K"); //non-standard
        testUdunitsToUcum("degree_k",          "K");
        testUdunitsToUcum("degrees_k",         "K"); //non-standard
        testUdunitsToUcum("deg_K",             "K");
        testUdunitsToUcum("degs_K",            "K");
        testUdunitsToUcum("deg_k",             "K");
        testUdunitsToUcum("degs_k",            "K");

        testUdunitsToUcum("°R",                "(5/9.K)");  //udunits-2
        testUdunitsToUcum("degree_Rankine",    "(5/9.K)");
        testUdunitsToUcum("degrees_Rankine",   "(5/9.K)"); //non-standard
        testUdunitsToUcum("degR",              "(5/9.K)");
        testUdunitsToUcum("degreeR",           "(5/9.K)");
        testUdunitsToUcum("degreesR",          "(5/9.K)"); //non-standard
        testUdunitsToUcum("degree_R",          "(5/9.K)");
        testUdunitsToUcum("degrees_R",         "(5/9.K)"); //non-standard
        testUdunitsToUcum("degree_r",          "(5/9.K)");
        testUdunitsToUcum("degrees_r",         "(5/9.K)"); //non-standard
        testUdunitsToUcum("deg_R",             "(5/9.K)");
        testUdunitsToUcum("degs_R",            "(5/9.K)");
        testUdunitsToUcum("deg_r",             "(5/9.K)");
        testUdunitsToUcum("degs_r",            "(5/9.K)");

        testUdunitsToUcum("denier",            "(1111111/10^13.kg/m)"); //1.111111e-7
        testUdunitsToUcum("diopter",           "[diop]");
        testUdunitsToUcum("dozen",             "12{count}");
        testUdunitsToUcum("dram",              "[fdr_us]");  //udunits dr is a volume, not a weight
        testUdunitsToUcum("dr",                "[fdr_uk]");  //in udunits, dr is a volume, not a weight
        testUdunitsToUcum("drop",              "[drp]");
        testUdunitsToUcum("dry_pint",          "[dpt_us]");
        testUdunitsToUcum("dry_quart",         "[dqt_us]");
        //see "dr" below
        testUdunitsToUcum("dynamic",           "[g]");
        testUdunitsToUcum("dyne",              "dyn");

        testUdunitsToUcum("EC_therm",          "(105506/10^13.J)"); //1.05506e8
        testUdunitsToUcum("electric_horsepower","(746.W)");  
        testUdunitsToUcum("electronvolt",      "eV");
        testUdunitsToUcum("eon",               "Ga");    //10^9.a
        testUdunitsToUcum("ergs",              "erg");

        testUdunitsToUcum("F",                 "[degF]");   //  In ucum, F means Farad.
        testUdunitsToUcum("Fahrenheit",        "[degF]");
        testUdunitsToUcum("fahrenheit",        "[degF]");
        testUdunitsToUcum("faraday",           "(9648531/100.C)");  //9.648531e4
        testUdunitsToUcum("farad",             "F");
        testUdunitsToUcum("fathom",            "[fth_us]");
        testUdunitsToUcum("feet",              "[ft_i]");
        testUdunitsToUcum("fermi",             "fm");
        testUdunitsToUcum("firkin",            "([bbl_us]/4)");
        testUdunitsToUcum("fldr",              "[dr_av]");
        testUdunitsToUcum("floz",              "[foz_us]");
        testUdunitsToUcum("fluid_ounce",       "[foz_us]");
        testUdunitsToUcum("fluid_dram",        "[fdr_us]");
        testUdunitsToUcum("footcandle",        "(1076391/10^7.lx)");  //0.1076391
        testUdunitsToUcum("footlambert",       "(3426259/10^6.cd/m2)");    //3.426259
        testUdunitsToUcum("foot_H2O",          "[ft_i'H2O]");
        testUdunitsToUcum("footH2O",           "[ft_i'H2O]");
        testUdunitsToUcum("foot",              "[ft_i]");

        testUdunitsToUcum("force_gram",        "gf");
        testUdunitsToUcum("force_ounce",       "([lbf_av]/16)");  //there is no [ozf_av]
        testUdunitsToUcum("force_pound",       "[lbf_av]");
        testUdunitsToUcum("force_kilogram",    "kgf");
        testUdunitsToUcum("force_ton",         "(2000.[lbf_av])"); //there is no ...
        testUdunitsToUcum("force",             "[g]");

        testUdunitsToUcum("fortnight",         "(14.d)");
        testUdunitsToUcum("free_fall",         "[g]");
        testUdunitsToUcum("ftH2O",             "[ft_i'H2O]");
        testUdunitsToUcum("ft",                "[ft_i]");  
        testUdunitsToUcum("furlong",           "[fur_us]");

        testUdunitsToUcum("gallon",            "[gal_us]");
        testUdunitsToUcum("gal",               "Gal");
        testUdunitsToUcum("gamma",             "nT");
        testUdunitsToUcum("gauss",             "G");
        testUdunitsToUcum("geopotential",      "[g]");
        testUdunitsToUcum("gilbert",           "Gb");
        testUdunitsToUcum("gill",              "[gil_us]");
        testUdunitsToUcum("gp",                "[g]");
        testUdunitsToUcum("gr",                "[gr]");
        testUdunitsToUcum("grade",             "gon");
        testUdunitsToUcum("grain",             "[gr]");
        testUdunitsToUcum("gram_force",        "gf");
        testUdunitsToUcum("gram",              "g");
        testUdunitsToUcum("gravity",           "[g]");
        testUdunitsToUcum("gray",              "Gy");
        testUdunitsToUcum("Gregorian_year",    "a_g");

        testUdunitsToUcum("h2o",               "[H2O]");  
        testUdunitsToUcum("H2O",               "[H2O]");  
        testUdunitsToUcum("hectare",           "Har");    //it's a hecto are
        testUdunitsToUcum("henry",             "H");
        testUdunitsToUcum("hertz",             "Hz");
        testUdunitsToUcum("hg",                "[Hg]");
        testUdunitsToUcum("Hg",                "[Hg]");  
        testUdunitsToUcum("horsepower",        "[HP]");
        testUdunitsToUcum("hour",              "h");
        testUdunitsToUcum("hr",                "h");
        testUdunitsToUcum("hundred",           "100");

        testUdunitsToUcum("inch_H2O_39F",      "[in_i'H2O]{39F}");  
        testUdunitsToUcum("inch_H2O_60F",      "[in_i'H2O]{60F}");  
        testUdunitsToUcum("inch_Hg_32F",       "[in_i'Hg]{32F}");   
        testUdunitsToUcum("inch_Hg_60F",       "[in_i'Hg]{60F}");   
        testUdunitsToUcum("inch_Hg",           "[in_i'Hg]");        
        testUdunitsToUcum("inches",            "[in_i]");
        testUdunitsToUcum("inch",              "[in_i]");

        testUdunitsToUcum("inhg",              "[in_i'Hg]");       
        testUdunitsToUcum("in",                "[in_i]"); 

        testUdunitsToUcum("international_feet",  "[ft_i]");
        testUdunitsToUcum("international_foot",  "[ft_i]");
        testUdunitsToUcum("international_inches","[in_i]");
        testUdunitsToUcum("international_inch",  "[in_i]");
        testUdunitsToUcum("international_knot",  "[kn_i]");
        testUdunitsToUcum("international_mile",  "[mi_i]");
        testUdunitsToUcum("international_yard",  "[yd_i]");
        
        testUdunitsToUcum("IT_Btu",            "[Btu_IT]"); 
        testUdunitsToUcum("IT_calorie",        "cal_IT");   

        testUdunitsToUcum("jiffies",           "cs");  
        testUdunitsToUcum("jiffy",             "cs");
        testUdunitsToUcum("joule",             "J");
        testUdunitsToUcum("Julian_year",       "a_j");

        testUdunitsToUcum("K",                 "K");
        testUdunitsToUcum("kat",               "kat");
        testUdunitsToUcum("katal",             "kat");
        testUdunitsToUcum("kayser",            "Ky");
        testUdunitsToUcum("kelvin",            "K");
        testUdunitsToUcum("Kelvin",            "K");
        testUdunitsToUcum("kip",               "(1000.[lbf_av])");
        testUdunitsToUcum("knot_international","[kn_i]");
        testUdunitsToUcum("knot",              "[kn_i]");
        testUdunitsToUcum("ksi",               "(1000.[lbf_av]/[in_i]2)");
        testUdunitsToUcum("kt",                "[kn_i]");

        testUdunitsToUcum("L",                 "l");  //both allowed in ucum, I choose l
        testUdunitsToUcum("l",                 "l");  //both allowed in ucum, I choose l
        testUdunitsToUcum("lambert",           "Lmb");
        testUdunitsToUcum("lbf",               "[lbf_av]");
        testUdunitsToUcum("lb",                "[lb_av]");
        testUdunitsToUcum("leap_year",         "(366.d)");
        testUdunitsToUcum("light_year",        "[ly]");
        testUdunitsToUcum("liquid_gallon",     "[gal_us]");  
        testUdunitsToUcum("liquid_pint",       "[pt_us]");
        testUdunitsToUcum("liquid_quart",      "[qt_us]");
        testUdunitsToUcum("liter",             "l");    //or use "L"?
        testUdunitsToUcum("litre",             "l");    //or use "L"?
        //testUdunitsToUcum("L",                 "l");  seems like: both allowed in ucum
        testUdunitsToUcum("long_hundredweight","[lcwt_av]");  
        testUdunitsToUcum("long_ton",          "t"); 
        testUdunitsToUcum("longton",           "t"); 
        testUdunitsToUcum("lumen",             "lm");
        testUdunitsToUcum("lunar_month",       "mo_s");
        testUdunitsToUcum("luxes",             "lx");
        testUdunitsToUcum("lux",               "lx");

        testUdunitsToUcum("m",                 "m");
        testUdunitsToUcum("maxwell",           "Mx");
        
        testUdunitsToUcum("mercury_0C",        "[Hg]");   
        testUdunitsToUcum("mercury_32F",       "[Hg]"); 
        testUdunitsToUcum("mercury_60F",       "[Hg]{60F}"); 
        testUdunitsToUcum("mercury",           "[Hg]");

        testUdunitsToUcum("meter",             "m");
        testUdunitsToUcum("metric_horsepower", "(735499/10^5.W)");  //7.35499,  
        testUdunitsToUcum("metric_ton",        "t");
        testUdunitsToUcum("metricton",         "t");
        testUdunitsToUcum("mhos",              "mho");  //=S
        testUdunitsToUcum("micron",            "um");
        testUdunitsToUcum("mile",              "[mi_i]");
        testUdunitsToUcum("millimeter_Hg",     "mm[Hg]");   
        testUdunitsToUcum("million",           "10^6");
        testUdunitsToUcum("mil",               "[mil_i]"); 
        testUdunitsToUcum("mins",              "min");
        testUdunitsToUcum("minute",            "min");
        testUdunitsToUcum("mi",                "[mi_i]"); //mile
        testUdunitsToUcum("mm_Hg",             "mm[Hg]");      
        testUdunitsToUcum("mm_hg",             "mm[Hg]");     
        testUdunitsToUcum("mm_Hg_0C",          "mm[Hg]{0C}");    
        testUdunitsToUcum("mmHg",              "mm[Hg]");     
        testUdunitsToUcum("mole",              "mol");
        testUdunitsToUcum("month",             "mo");

        testUdunitsToUcum("N",                 "N");
        testUdunitsToUcum("neper",             "Np");
        testUdunitsToUcum("nautical_mile",     "[nmi_i]");  
        testUdunitsToUcum("nmile",             "[nmi_i]");      
        testUdunitsToUcum("nmi",               "[nmi_i]");
        testUdunitsToUcum("newton",            "N");
        testUdunitsToUcum("nit",               "(cd/m2)");
        testUdunitsToUcum("NTU",               "{ntu}"); //???
        testUdunitsToUcum("nt",                "(cd/m2)");

        testUdunitsToUcum("O",                 "O");
        testUdunitsToUcum("oersted",           "Oe");
        testUdunitsToUcum("ohm",               "O");
        testUdunitsToUcum("osmole",            "osm"); 
        testUdunitsToUcum("ounce_force",       "([lbf_av]/16)");  //there is no [ozf_av]
        testUdunitsToUcum("ozf",               "([lbf_av]/16)");
        testUdunitsToUcum("oz",                "[foz_us]");

        testUdunitsToUcum("pair",              "2");
        testUdunitsToUcum("parsec",            "pc");
        testUdunitsToUcum("pascal",            "Pa");
        testUdunitsToUcum("peck",              "[pk_us]");
        testUdunitsToUcum("pennyweight",       "[pwt_tr]"); 
        testUdunitsToUcum("percent",           "%");
        testUdunitsToUcum("percentage",        "%");
        testUdunitsToUcum("perches",           "[rd_us]");
        testUdunitsToUcum("perch",             "[rd_us]");
        testUdunitsToUcum("perm_0C",           "(S.572135.10^-16.kg/(Pa.s.m2))");
        testUdunitsToUcum("perm_23C",          "(S.574525.10^-16.kg/(Pa.s.m2))");

        testUdunitsToUcum("phot",              "ph");  
        testUdunitsToUcum("pH",                "[pH]"); 
        testUdunitsToUcum("physical_faraday",  "(965219/10.C)"); //9.65219e4
        testUdunitsToUcum("pica",              "[pca_pr]");
        testUdunitsToUcum("pint",              "[pt_us]");
        testUdunitsToUcum("pi",                "[pi]"); 
        testUdunitsToUcum("PI",                "[pi]");   
        testUdunitsToUcum("pk",                "[pk_us]");
        testUdunitsToUcum("poise",             "P");      
        testUdunitsToUcum("pole",              "[rd_us]");
        testUdunitsToUcum("pond",              "(980665/10^5.N)"); //9.806650e-3
        testUdunitsToUcum("poundal",           "(138255/10^6.N)"); //1.382550e-1
        testUdunitsToUcum("pound_force",       "[lbf_av]");
        testUdunitsToUcum("pound",             "[lb_av]");

        testUdunitsToUcum("ppth",              "[ppth]");
        testUdunitsToUcum("ppm",               "[ppm]");
        testUdunitsToUcum("ppb",               "[ppb]");
        testUdunitsToUcum("pptr",              "[pptr]");
        
        testUdunitsToUcum("printers_pica",     "[pca_pr]");  
        testUdunitsToUcum("printers_point",    "[pnt_pr]");
        testUdunitsToUcum("psi",               "[psi]");
        testUdunitsToUcum("PSU",               "{PSU}"); //??? PSU changed to 1e-3 with CF std names 25. Crazy. Useless. Don't do it.
        testUdunitsToUcum("psu",               "{PSU}"); //??? PSU changed to 1e-3 with CF std names 25. Crazy. Useless. Don't do it.
        testUdunitsToUcum("pt",                "[pt_us]");

        testUdunitsToUcum("quart",             "[qt_us]");

        testUdunitsToUcum("R",                 "R");
        testUdunitsToUcum("radian",            "rad");
        testUdunitsToUcum("rad",               "RAD");
        testUdunitsToUcum("Rankine",           "(5/9.K)");
        testUdunitsToUcum("Rankines",          "(5/9.K)");
        testUdunitsToUcum("rankine",           "(5/9.K)");
        testUdunitsToUcum("rankines",          "(5/9.K)");
        testUdunitsToUcum("rd",                "RAD");
        testUdunitsToUcum("refrigeration_ton", "(12000.[Btu_IT]/hr)");
        testUdunitsToUcum("register_ton",      "(2831685/10^6.m3)"); //2.831685
        testUdunitsToUcum("rem",               "REM");
        testUdunitsToUcum("rhe",               "(10.Pa-1.s-1)");
        testUdunitsToUcum("rod",               "[rd_us]");
        testUdunitsToUcum("roentgen",          "R");
        testUdunitsToUcum("rps",               "Hz");

        testUdunitsToUcum("S",                 "S");
        testUdunitsToUcum("score",             "20");
        testUdunitsToUcum("scruple",           "[sc_ap]");
        testUdunitsToUcum("second",            "s");
        testUdunitsToUcum("sec",               "s");
        testUdunitsToUcum("shaft_horsepower",  "[HP]");  
        testUdunitsToUcum("shake",             "(10^-8.s)");
        testUdunitsToUcum("short_hundredweight","[scwt_av]");  
        testUdunitsToUcum("short_ton",         "[ston_av]");
        testUdunitsToUcum("shortton",          "[ston_av]");

        testUdunitsToUcum("sidereal_day",      "(8616409/10^2.s)"); //8.616409e4
        testUdunitsToUcum("sidereal_hour",     "(359017/10^2.s)");   //3.590170e3
        testUdunitsToUcum("sidereal_minute",   "(5983617/10^5.s)");   //5.983617e1
        testUdunitsToUcum("sidereal_month",    "(27321661/10^6.d)");    //27.321661
        testUdunitsToUcum("sidereal_second",   "(9972696/10^7.s)");    //0.9972696
        testUdunitsToUcum("sidereal_year",     "(31558150.s)");   //3.155815e7
        
        testUdunitsToUcum("siemens",           "S"); //always has s at end
        testUdunitsToUcum("sievert",           "Sv");
        testUdunitsToUcum("slug",              "(145939/10^4.kg)"); //14.5939
        testUdunitsToUcum("sphere",            "(4.[pi].sr)");
        testUdunitsToUcum("standard_atmosphere","atm"); 
        testUdunitsToUcum("standard_free_fall","[g]");
        
        testUdunitsToUcum("statampere",        "(333564/10^15.A)");  //3.335640e-10
        testUdunitsToUcum("statcoulomb",       "(333564/10^15.C)");  //3.335640e-10
        testUdunitsToUcum("statfarad",         "(111265/10^17.F)");  //1.112650e-12
        testUdunitsToUcum("stathenry",         "(8987554.10^5.H)");   //8.987554e11
        testUdunitsToUcum("statmho",           "(111265/10^17.S)");  //1.112650e-12 
        testUdunitsToUcum("statohm",           "(8987554.10^5.O)");   //8.987554e11
        testUdunitsToUcum("statvolt",          "(2997925/10^4.V)");    //2.997925e2
        
        testUdunitsToUcum("steradian",         "sr");    
        testUdunitsToUcum("stere",             "st");
        testUdunitsToUcum("stilb",             "sb");
        testUdunitsToUcum("stokes",            "St");
        testUdunitsToUcum("stone",             "[stone_av]");  
        testUdunitsToUcum("svedberg",          "[S]");

        testUdunitsToUcum("T",                 "T");
        testUdunitsToUcum("t",                 "t");
        testUdunitsToUcum("tablespoon",        "[tbs_us]");
        testUdunitsToUcum("Tblsp",             "[tbs_us]");
        testUdunitsToUcum("tblsp",             "[tbs_us]");
        testUdunitsToUcum("Tbl",               "[tbs_us]");
        testUdunitsToUcum("Tbsp",              "[tbs_us]");
        testUdunitsToUcum("tbsp",              "[tbs_us]");
        testUdunitsToUcum("tbs",               "[tbs_us]");
        testUdunitsToUcum("teaspoon",          "[tsp_us]");
        testUdunitsToUcum("tsp",               "[tsp_us]");
        testUdunitsToUcum("technical_atmosphere","att"); 
        testUdunitsToUcum("ten",               "10");
        testUdunitsToUcum("tesla",             "T");
        testUdunitsToUcum("tex",               "(mg/m)");
        testUdunitsToUcum("thermochemical_calorie","cal_th"); 
        testUdunitsToUcum("therm",             "(105480400.J)");  
        testUdunitsToUcum("thm",               "(105480400.J)");  
        testUdunitsToUcum("thousand",          "1000");
        testUdunitsToUcum("ton_force",         "(2000.[lbf_av])"); //there is no ...
        testUdunitsToUcum("ton_of_refrigeration","(12000.[Btu_IT]/hr)");
        testUdunitsToUcum("ton_TNT",           "(4184.10^6.J)");  //4.184e9
        testUdunitsToUcum("tonne",             "t");  
        testUdunitsToUcum("ton",               "[ston_av]"); //U.S. short ton
        testUdunitsToUcum("torr",              "mm[Hg]"); 
        testUdunitsToUcum("tropical_month",    "(27321582/10^6.d)"); //27.321582
        testUdunitsToUcum("tropical_year",     "a_t");
        testUdunitsToUcum("troy_ounce",        "[oz_tr]");
        testUdunitsToUcum("troy_pound",        "[lb_tr]");
        testUdunitsToUcum("turn",              "circ");

        testUdunitsToUcum("u",                 "u");
        testUdunitsToUcum("ua",                "AU");
        testUdunitsToUcum("UK_fluid_ounce",    "[foz_br]");
        testUdunitsToUcum("UK_horsepower",     "(7457/10.W)");  //745.7
        testUdunitsToUcum("UK_liquid_cup",     "([pt_br]/2)");   
        testUdunitsToUcum("UK_liquid_gallon",  "[gal_br]");   
        testUdunitsToUcum("UK_liquid_gill",    "[gil_br]");   
        testUdunitsToUcum("UK_liquid_ounce",   "[foz_br]");
        testUdunitsToUcum("UK_liquid_pint",    "[pt_br]");    
        testUdunitsToUcum("UK_liquid_quart",   "[qt_br]");    
        
        testUdunitsToUcum("unified_atomic_mass_unit", "u");
        testUdunitsToUcum("unit_pole",         "(1256637/10^13.Wb)"); //1.256637e-7
        
        testUdunitsToUcum("US_dry_gallon",     "[gal_wi]");  
        testUdunitsToUcum("US_dry_pint",       "[dpt_us]");  
        testUdunitsToUcum("US_dry_quart",      "[dqt_us]");  
        testUdunitsToUcum("US_fluid_ounce",    "[foz_us]");
        testUdunitsToUcum("US_liquid_cup",     "[cup_us]");  
        testUdunitsToUcum("US_liquid_gallon",  "[gal_us]");  
        testUdunitsToUcum("US_liquid_gill",    "[gil_us]");  
        testUdunitsToUcum("US_liquid_ounce",   "[foz_us]");
        testUdunitsToUcum("US_liquid_pint",    "[pt_us]");   
        testUdunitsToUcum("US_liquid_quart",   "[qt_us]");   
        testUdunitsToUcum("US_statute_mile",   "[mi_us]");
        testUdunitsToUcum("US_survey_feet",    "[ft_us]");
        testUdunitsToUcum("US_survey_foot",    "[ft_us]");
        testUdunitsToUcum("US_survey_mile",    "[mi_us]");
        testUdunitsToUcum("US_survey_yard",    "[yd_us]"); 
        testUdunitsToUcum("US_therm",          "(105480400.J)");  //1.054804e8
        
        testUdunitsToUcum("V",                 "V");
        testUdunitsToUcum("volt",              "V");

        testUdunitsToUcum("W",                 "W");    
        testUdunitsToUcum("water_32F",         "[H2O]");    
        testUdunitsToUcum("water_39F",         "[H2O]{39F}"); 
        testUdunitsToUcum("water_4C",          "[H2O]{4Cel}"); 
        testUdunitsToUcum("water_60F",         "[H2O]{60F}"); 
        testUdunitsToUcum("water",             "[H2O]");
        testUdunitsToUcum("watt meter-2",      "W.m-2");
        testUdunitsToUcum("watt/meter-2",      "W.m2"); //test -- 

        //invalid but converted
        testUdunitsToUcum("w",                 "W");    
        testUdunitsToUcum("w/m2",              "W.m-2");    


        testUdunitsToUcum("watt",              "W"); 
        testUdunitsToUcum("weber",             "Wb");
        testUdunitsToUcum("week",              "wk");
        testUdunitsToUcum("work_month",        "(2056.hr/12)");
        testUdunitsToUcum("work_year",         "(2056.hr)");

        testUdunitsToUcum("yard",              "[yd_i]");
        testUdunitsToUcum("yd",                "[yd_i]"); 
        testUdunitsToUcum("year",              "a");
        testUdunitsToUcum("yr",                "a");


        //metric prefixes
        testUdunitsToUcum("atto",              "a{count}");
        testUdunitsToUcum("centi",             "c{count}");
        testUdunitsToUcum("deci",              "d{count}");
        testUdunitsToUcum("deka",              "da{count}");
        testUdunitsToUcum("exa",               "E{count}");
        testUdunitsToUcum("femto",             "f{count}");
        testUdunitsToUcum("giga",              "G{count}");
        testUdunitsToUcum("hecto",             "h{count}");
        testUdunitsToUcum("kilo",              "k{count}");
        testUdunitsToUcum("mega",              "M{count}");
        testUdunitsToUcum("micro",             "u{count}");
        testUdunitsToUcum("µ",                 "u{count}");
        testUdunitsToUcum("milli",             "m{count}");
        testUdunitsToUcum("nano",              "n{count}");
        testUdunitsToUcum("peta",              "P{count}");
        testUdunitsToUcum("pico",              "p{count}");
        testUdunitsToUcum("tera",              "T{count}");
        testUdunitsToUcum("yocto",             "y{count}");
        testUdunitsToUcum("yotta",             "Y{count}");
        testUdunitsToUcum("zepto",             "z{count}");  
        testUdunitsToUcum("zetta",             "Z{count}");


        //multiple metric prefixes are no longer allowed
        //Wikipedia says "Although formerly in use, the SI disallows combining prefixes; "
        testUdunitsToUcum("decikilograms", "{decikilograms}");

        //per
        testUdunitsToUcum("kilograms / second2",     "kg.s-2");
        testUdunitsToUcum("kilograms PER second**2", "kg.s-2");
        testUdunitsToUcum("kilograms per second^2",  "kg.s-2");

        //dates
        testUdunitsToUcum("MM/dd/yyyy",        "{MM/dd/yyyy}");
        testUdunitsToUcum("millisecond since 1900-1-1",  "ms{since 1900-01-01}");
        testUdunitsToUcum("second since 1900-1-1",       "s{since 1900-01-01}");
        testUdunitsToUcum("minute since 1900-1-1",       "min{since 1900-01-01}");
        testUdunitsToUcum("hour since Jan 1, 2000",      "h{since 2000-01-01}");
        testUdunitsToUcum("day since 1900-1-1",          "d{since 1900-01-01}");
        testUdunitsToUcum("month since 1900-1-1",        "mo{since 1900-01-01}");
        testUdunitsToUcum("year since 1900-1-1",         "a{since 1900-01-01}");

        //numbers
        testUdunitsToUcum("-12 kg",            "-12.kg");
        testUdunitsToUcum("-0.0e14 kg",        "0.kg");
        testUdunitsToUcum("-20.0e14 kg",       "-2.10^15.kg");
        testUdunitsToUcum("-1.2e1 kg",         "-12.kg");
        testUdunitsToUcum("-12340e-1 kg",      "-1234.kg");
        testUdunitsToUcum("-12340e2 kg",       "-1234000.kg");
        testUdunitsToUcum("-12.345e2 kg",      "-12345.10^-1.kg");
        testUdunitsToUcum("-12.345e400 kg",    "-12.345e400.kg");  //trouble is passed through

        //failures
        testUdunitsToUcum("dkilo",             "{dkilo}");  //2 prefixes isn't allowed
        testUdunitsToUcum("kiloBobs",          "{kiloBobs}");   //original returned as comment, not split into k{Bobs}
        testUdunitsToUcum("some unrelated24 con33tent, really (a fact)", 
                          "{some}.{unrelated}24.{con33tent},.{really}.(a.{fact})");   //not butchered

        //punctuation
        //PER should be handled as special case to avoid ./.
        testUdunitsToUcum("m per s",            "m.s-1");
        testUdunitsToUcum("m PER s",            "m.s-1");
        testUdunitsToUcum("m**s",                "m^s");  //exponent
        testUdunitsToUcum("m*s",                "m.s");  //explicit multiplication
        testUdunitsToUcum("m·s",                 "m.s");   //explicit multiplication   middot is tiny in this font!
        testUdunitsToUcum("m s",                "m.s");  //implied multiplication
        testUdunitsToUcum("\"",                 "''");
        testUdunitsToUcum("'",                  "'");
        testUdunitsToUcum("#",                  "{count}");

        testUdunitsToUcum("°",                 "deg");
        testUdunitsToUcum("°F",                "[degF]");
        testUdunitsToUcum("°R",                "(5/9.K)");
        testUdunitsToUcum("°C",                "Cel");
        testUdunitsToUcum("°K",                "K");
        testUdunitsToUcum("°north",            "{°north}"); //invalid

        debugMode = false;
    }

    private static void testUdunitsToUcum(String udunits, String ucum) {
        Test.ensureEqual(udunitsToUcum(udunits), ucum, "original=" + udunits);
    }


    /** 
     * This is like ucumToUdunits() but won't throw an exception. 
     * If there is trouble, it returns ucum.
     */
    public static String safeUcumToUdunits(String ucum) {
        try {
           return ucumToUdunits(ucum); 
        } catch (Throwable t) {
            String2.log(String2.ERROR + " while converting ucum=" + ucum + " to udunits:\n" +
                MustBe.throwableToString(t));
            return ucum;
        }
    }

    /** 
     * This converts UCUM to UDUnits.
     * <br>UDUnits: https://www.unidata.ucar.edu/software/udunits/
     * <br>UCUM: https://unitsofmeasure.org/ucum.html
     *
     * <p>UCUM tends to be short, canonical-only, and strict.
     *    Many UCUM units are the same in UDUnits.
     *
     * <p>UDUnits supports lots of aliases (short and long)
     *   and plurals (usually by adding 's'). 
     *   This tries to convert UCUM to a short, common UDUNIT units.
     *
     * <p>Problems:
     * <ul>
     * <li> UCUM has only "deg", no concept of degree_east|north|true|true.
     * </ul>
     * 
     * <p>Notes: 
     * <ul>
     * <li>This method is a strictly case sensitive.
     * <li>For "10 to the", UCUM allows 10* or 10^. This method uses 10^.
     * <li>{ntu} becomes NTU.
     * <li>{psu} becomes PSU.
     * </ul>
     *
     * return the UCUM converted to UDUNITS.
     *    null returns null. "" returns "".
     */
    public static String ucumToUdunits(String ucum) {
        if (ucum == null)
            return null;
        ucum = ucum.trim();
        if (debugMode) 
            String2.log(">> convert ucum=" + ucum);
        
        StringBuilder udunits = new StringBuilder();        
        int ucLength = ucum.length();
        if (ucLength == 0)
            return "";

        //specified in <ucumToUdunits> in messages.xml
        String t = ucumToUdunitsHM.get(ucum);
        if (t != null)
            return t;

        //entirely in {}
        if (ucum.charAt(0) == '{' &&
            ucum.indexOf('}') == ucLength - 1)
            return ucum.substring(1, ucLength - 1);

        if (ucum.charAt(ucLength - 1) == '}' &&  //quick reject
            ucum.indexOf('}') == ucLength - 1) { //reasonably quick reject

            //is it a time point?  e.g., s{since 1970-01-01T00:00:00T}
            int sincePo = ucum.indexOf("{since ");
            if (sincePo > 0) {
                //is first part an atomic ucum unit?
                String start = ucum.substring(0, sincePo);
                String tUdunits = ucHashMap.get(start);
                String remainder = " " + ucum.substring(sincePo + 1, ucLength - 1);
                if      (start.equals("ms"))     return "milliseconds" + remainder;
                else if (start.equals("s"))      return "seconds" + remainder;
                else if (start.equals("min"))    return "minutes" + remainder;
                else if (start.equals("h"))      return "hours"   + remainder;
                else if (start.equals("d"))      return "days"    + remainder;
                else if (start.equals("mo"))     return "months"  + remainder;
                else if (start.equals("a"))      return "years"   + remainder;
                else if (String2.isSomething(tUdunits))
                                                 return tUdunits  + remainder; 
                else                             return start     + remainder; 
            } //else fall through

            //is it a time format?  e.g., {ddMMMyyyy}
            if (ucum.charAt(0) == '{' &&
                ucLength >= 4 &&
                ucum.indexOf('{', 1) == -1 &&
                ucum.substring(1, ucLength - 1).indexOf("yy") >= 0) { //more forgiving than Calendar2.isStringTimeUnits(
                return ucum.substring(1, ucLength - 1);
            }
        }

        //remove all <sup> and </sup>
        ucum = String2.replaceAll(ucum, "<sup>",  "");
        ucum = String2.replaceAll(ucum, "</sup>", "");

        //remove ^ from ^- and ^[digit] 
        // but not if preceded by digit
        for (int po = ucum.length() - 3; po >= 0; po--) { //work backwards
            if (!String2.isDigit(ucum.charAt(po)) &&
                ucum.charAt(po + 1) == '^' &&
                (ucum.charAt(po + 2) == '-' || String2.isDigit(ucum.charAt(po + 2))))
            ucum = ucum.substring(0, po + 1) +
                   ucum.substring(po + 2);
        }

        //remove ** from **- and **[digit] 
        for (int po = ucum.length() - 3; po >= 0; po--) { //work backwards
            if (ucum.charAt(po) == '*' &&
                ucum.charAt(po + 1) == '*' &&
               (ucum.charAt(po + 2) == '-' || String2.isDigit(ucum.charAt(po + 2))))
            ucum = ucum.substring(0, po) +
                   ucum.substring(po + 2);
            po = Math.min(po, ucum.length() - 2);
        }
        ucLength = ucum.length();
       
        //parse ucum and build udunits, till done        
        int po = 0;  //po is next position to be read
        boolean dividePending = false;
        while (po < ucLength) {
            if (debugMode && udunits.length() > 0) String2.log("  udunits=" + udunits);
            char ch = ucum.charAt(po);

            //uncomment {a comment}
            if (ch == '{') {
                int po2 = po + 1;
                while (po2 < ucLength && ucum.charAt(po2) != '}') 
                    po2++;
                udunits.append(ucum.substring(po + 1, po2)); //non-standard udunits comment or unknown term
                po = po2 + 1;
                continue;
            }

            //5/9
            if (ch == '5' && 
                po <= ucLength - 1 &&
                ucum.charAt(po + 1) == '/' &&
                ucum.charAt(po + 2) == '9' &&
                (po + 3 == ucum.length() || !String2.isDigit(ucum.charAt(po + 3)))) {
                udunits.append("0.5555555555555556");
                po += 3;
                continue;
            }

            //letter  
            if (isUcumLetter(ch)) {     //includes [, ], {, }, 'µ' and "'"
                //find contiguous letters|_|digit (no '-') 
                int po2 = po + 1;
                while (po2 < ucLength && 
                    (isUcumLetter(ucum.charAt(po2)) || ucum.charAt(po2) == '_' ||
                     String2.isDigit(ucum.charAt(po2)))) {
                    po2++;
                    if (ucum.charAt(po2-1) == '{') {
                        //find end of comment
                        while (po2 < ucLength && ucum.charAt(po2) != '}')
                            po2++;
                        if (po2 < ucLength) //should be
                            po2++;
                    }
                }

                String tUcum = ucum.substring(po, po2);
                po = po2;

                //some ucum have internal digits, but none end in digits 
                //if it ends in digits, treat as exponent
                //find contiguous digits at end
                int firstDigit = tUcum.length();
                while (firstDigit >= 1 && String2.isDigit(tUcum.charAt(firstDigit - 1)))
                    firstDigit--;
                String exponent = tUcum.substring(firstDigit);
                if (dividePending) {
                    if (exponent.length() == 0)
                        exponent = "-1";
                    else if (exponent.startsWith("-"))
                        exponent = exponent.substring(1);
                    else exponent = "-" + exponent;
                    dividePending = false;
                }
                tUcum = tUcum.substring(0, firstDigit);
                String tUdunits = oneUcumToUdunits(tUcum);

                if (udunits.toString().equals("1 "))
                    udunits.setLength(0);

                //deal with PER -> / 
                if (tUdunits.equals("/")) {   
                    dividePending = !dividePending;
                    if (udunits.length() > 0 && !String2.endsWith(udunits, " "))
                        udunits.append(' ');
                    tUdunits = "";
                } else {
                    udunits.append(tUdunits);
                }                

                //add the exponent
                udunits.append(exponent);
                //catch -exponent as a number below

                continue;
            }

            //number
            if (ch == '-' || String2.isDigit(ch)) {
                //find contiguous digits
                int po2 = po + 1;
                while (po2 < ucLength && String2.isDigit(ucum.charAt(po2))) 
                    po2++;

                // ^-  or ^{digit}
                boolean hasE = false;
                if (po2 < ucLength-1 && 
                    ucum.charAt(po2) == '^' &&
                    (ucum.charAt(po2+1) == '-' || String2.isDigit(ucum.charAt(po2+1)))) {
                    hasE = true;
                    po2 += 2;
                    while (po2 < ucLength && String2.isDigit(ucum.charAt(po2))) 
                        po2++;
                }
                String num = ucum.substring(po, po2);
                if (num.startsWith("10^"))
                    num = "1.0E" + num.substring(3);
                po = po2;
                if (udunits.toString().equals("1 ")) 
                    udunits.setLength(0); 

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
                if (udunits.length() > 0 && !String2.endsWith(udunits, " "))
                    udunits.append(' ');
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
                if (udunits.length() > 0 && !String2.endsWith(udunits, " "))
                    udunits.append(' ');
                po++;
                continue;
            }

            //otherwise, punctuation.   copy it
            //  " doesn't occur,
            udunits.append(ch);
            po++;
        }

        String uds = udunits.toString().trim();
        if (uds.startsWith("m m "))
            uds = "m2 " + uds.substring(4);
        return uds;
    }

    private static boolean isUcumLetter(char ch) {
        return String2.isLetter(ch) || 
            ch == '[' || ch == ']' || 
            ch == '{' || ch == '}' || 
            ch == 'µ' || ch == '\'';
    }

    /**
     * This converts one ucum term (perhaps with metric prefix(es)) 
     (   to the corresponding udunits string.
     * If ucum is just metric prefix(es), this returns the metric prefix 
     *   acronym(s) with "{count}" as suffix (e.g., dkilo returns dk{count}).
     * If this can't completely convert ucum, it returns the original ucum
     *   (e.g., kiloBobs remains kiloBobs  (to avoid 'exact' becoming 'ect' ).
     */
    private static String oneUcumToUdunits(String ucum) {


        //repeatedly pull off start of ucum and build udunits, till done
        String oldUcum = ucum;
        StringBuilder udunits = new StringBuilder();        
        boolean caughtPrefix = false; //so just allow one prefix

        MAIN:
        while (true) {
            if (debugMode) 
                String2.log(udunits.length() == 0? "" : "  udunits=" + udunits + " ucum=" + ucum);

            //try to find ucum in hashMap
            String tUdunits = ucHashMap.get(ucum);
            if (tUdunits != null) {
                //success! done!
                udunits.append(tUdunits);
                return udunits.toString();
            }

            //try to separate out one metricAcronym prefix (e.g., "k")
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

            //try to separate out a twoAcronym prefix (e.g., "Ki")
            if (!caughtPrefix) {
                for (int p = 0; p < nTwo; p++) {
                    if (ucum.startsWith(twoAcronym[p])) {
                        ucum = ucum.substring(twoAcronym[p].length());
                        char udch = udunits.length() > 0? udunits.charAt(udunits.length() - 1) : '\u0000';
                        if (udch != '\u0000' && udch != '.' && udch != '/')
                            udunits.append('.');
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

            //ends in comment?  try to just convert the beginning
            int po1 = oldUcum.lastIndexOf('{');
            if (po1 > 0 && oldUcum.endsWith("}")) 
                return oneUcumToUdunits(oldUcum.substring(0, po1)) + 
                    "(" + oldUcum.substring(po1 + 1, oldUcum.length() - 1) + ")";

            //no change? failure; return original ucum
            //  because I don't want to change "exact" into "ect" 
            // (i.e., seeing a metric prefix that isn't a metric prefix)
            if (debugMode) String2.log("Units2.oneUcumToUdunits fail: udunits=" + 
                udunits + " ucum=" + ucum);
            //udunits.append(ucum);
            return oldUcum; 
        }
    }

    /**
     * This tests UcumToUdunits.
     * The most likely bugs are:
     * <ul>
     * <li> Excess/incorrect substitutions, e.g., "avoirdupois_pounds" to "[lb_av]" to "[[lb_av]_av]".
     *   <br>This is the only thing that this method has pretty good tests for.
     * <li> Plural vs Singular conversions
     * <li> Typos 
     * <li> Misunderstanding (e.g., use of [H2O])
     * </ul>
     *
     * @throws RuntimeException if trouble
     */
    public static void testUcumToUdunits() {

        String2.log("\n*** Units2.testUcumToUdunits");
        debugMode = true;

        //main alphabetical section
        testUcumToUdunits("A",                 "A");
        testUcumToUdunits("[acr_us]",          "acre");


        testUcumToUdunits("deg",               "degree");
        testUcumToUdunits("deg{east}",         "degrees_east");
        testUcumToUdunits("deg{north}",        "degrees_north");
        testUcumToUdunits("deg{true}",         "degrees_true");
        testUcumToUdunits("deg{west}",         "degrees_west");

        //twoAcronym
        testUcumToUdunits("KiBd",              "1024.baud");

        //failures
        testUcumToUdunits("dkcount",           "dkcount"); //not ideal
        testUcumToUdunits("dk{count}",         "dk(count)"); //not ideal
        testUcumToUdunits("kiloBobs",          "kiloBobs");   //original returned, not split into kBobs
        testUcumToUdunits("deg{bob}",          "degree(bob)");  //comment left intact

        //punctuation
        //PER should be handled as special case to avoid ./.
        testUcumToUdunits("m^s",               "m^s");  //exponent
        testUcumToUdunits("m.s",               "m s");  //explicit multiplication
        testUcumToUdunits("''",                "arc_second");
        testUcumToUdunits("'",                 "arc_minute");

        testUcumToUdunits("deg",               "degree");
        testUcumToUdunits("[degF]",            "degree_F");
String2.log("5/9=" + (5/9.0));
        testUcumToUdunits("5/9.K",             "0.5555555555555556 degree_K");
        testUcumToUdunits("Cel",               "degree_C");
        testUcumToUdunits("K",                 "degree_K");

        //time point
        testUcumToUdunits("s{since 1970-01-01T00:00:00Z}", "seconds since 1970-01-01T00:00:00Z");
        testUcumToUdunits("Gb{since 1970-01-01T00}", "gilbert since 1970-01-01T00");  //absurd but okay
        testUcumToUdunits("s.m{ since 1970-01-01T00}", "s m( since 1970-01-01T00)"); //fail, so comment falls through

        debugMode = false;

    }


    private static void testUcumToUdunits(String ucum, String udunits) {
        Test.ensureEqual(ucumToUdunits(ucum), udunits, "original=" + ucum);
    }


    /**
     * This makes a HashMap&lt;String, String&gt; from the ResourceBundle-like file's keys=values.
     * The key and value strings are trim()'d.
     *
     * @param fileName  the full file name of the key=value file.
     * @param charset e.g., ISO-8859-1; or "" or null for the default
     * @throws RuntimeException if trouble
     */
    public static HashMap<String, String> getHashMapStringString(String fileName, String charset) 
        throws RuntimeException {
        try {
            HashMap ht = new HashMap();
            ArrayList<String> sar = String2.readLinesFromFile(fileName, charset, 2);
            int n = sar.size();
            int i = 0;
            while (i < n) {
                String s = sar.get(i++);
                if (s.startsWith("#"))
                    continue;
                while (i < n && s.endsWith("\\")) 
                    s = s.substring(0, s.length() - 1) + sar.get(i++);
                int po = s.indexOf('=');
                if (po < 0) 
                    continue;
                //new String: so not linked to big source file's text
                ht.put(
                    new String(s.substring(0, po).trim()), 
                    new String(s.substring(po + 1).trim())); 
            }
            return ht;
        } catch (Throwable t) {
            throw new RuntimeException(t); 
        }
    }


    
    /** 
     * This is used by Bob a few times to find udunits2 units that aren't yet in UdunitsToUcum.properties.
     * The source files are c:/programs/udunits-2.1.9/lib/udunits2-XXX.xml
     * No one else will need to use this.
     */
    public static void checkUdunits2File(String fileName) throws Exception {
        String2.log("\n*** Units2.checkUdUnits2File " + fileName);
        StringArray sa = StringArray.fromFile(fileName);
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
                    if (term.indexOf(' ') < 0 && term.indexOf('/') < 0 &&
                        term.indexOf('.') < 0 && term.indexOf('^') < 0 &&      
                        udHashMap.get(term) == null) {
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
                    if (term.indexOf(' ') < 0 && term.indexOf('/') < 0 &&
                        term.indexOf('.') < 0 && term.indexOf('^') < 0 &&      
                        udHashMap.get(term) == null) {
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
                    if (term.indexOf(' ') < 0 && term.indexOf('/') < 0 &&
                        term.indexOf('.') < 0 && term.indexOf('^') < 0 &&      
                        udHashMap.get(term) == null) {
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
                    if (term.indexOf(' ') < 0 && term.indexOf('/') < 0 &&
                        term.indexOf('.') < 0 && term.indexOf('^') < 0 &&      
                        udHashMap.get(term) == null) {
                        names.add(term);
                        nSymbol++;
                    }
                }
            }
        }
        names.sortIgnoreCase();
        String2.log(names.toNewlineString());
        String2.log("nDef=" + nDef + " nPlural=" + nPlural + 
            " nSingular=" + nSingular + " nSymbol=" + nSymbol);

    }

    /** 
     * This was used by Bob once to generate a crude UcumToUdunits.properties file.
     * No one else will need to use this.
     */
    public static void makeCrudeUcumToUdunits() {
        String2.log("\n*** Units2.makeCrudeUcumToUdunits");
        StringArray sa = new StringArray();
        Object keys[] = udHashMap.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            String key = (String)keys[i];
            if (key.endsWith("s") && udHashMap.get(key.substring(0, key.length() - 1)) != null) {
                //skip this plural (a singular exists)
            } else {
                sa.add(String2.left(udHashMap.get(key) + " = ", 21) + key);
            }
        }
        sa.sortIgnoreCase();
        String2.log(sa.toNewlineString());
    }

    /** 
     * This is used by Bob as a one time test.
     * No one else will need to use this.
     */
    public static void testRoundTripConversions() {
        //uc -> ud -> uc is more likely to work cleanly because it starts with acronym
        String2.log("\n*** Units2.testRoundTripConversions");
        Object ucKeys[] = ucHashMap.keySet().toArray();
        Arrays.sort(ucKeys);
        for (int i = 0; i < ucKeys.length; i++) {
            String uc1 = (String)ucKeys[i];
            String ud1 = ucumToUdunits(uc1);
            String uc2 = udunitsToUcum(ud1);
            if (!uc1.equals(uc2))
                String2.log(
                    "\nuc1=" + uc1 +
                    "\nud1=" + ud1 +
                    "\nuc2=" + uc2);
        }
    }

    /**
     * This checks if two udunits are equivalent (either both are null or "", 
     * or equal each other, or have same ucom units).
     * Sometimes different but equivalent (e.g., degrees_north and degree_north).
     */
    public static boolean udunitsAreEquivalent(String ud1, String ud2) {
        if (ud1 == null) ud1 = "";
        if (ud2 == null) ud2 = ""; 
        if (ud1.equals(ud2))
            return true;
        return udunitsToUcum(ud1).equals(udunitsToUcum(ud2));
    }

        
    //******************************
    private static String bobsErddapContentDir = "/programs/_tomcat/content/erddap/";
    private static String cfUnique = bobsErddapContentDir + "uniqueCFUnits.txt";
    public static UnitFormat unitFormat = ucar.units.StandardUnitFormat.instance();

    /**
     * This generates a list of unique units mentioned in datasetsUAF.xml,
     * datasetsFEDCW.xml and uniqueCFUnits.txt.
     */
    public static StringArray getTestUdunits() throws Exception {

        HashSet set = new HashSet();
        //add some additional tests
        set.add("degree_true");
        set.add("degrees_true");
        set.add("milliseconds since 1980-1-1T00:00:00Z");
        set.add("minutes since 1980-1-1");
        set.add("years since 02-JAN-1985");

        Pattern pattern = Pattern.compile("<att name=\"units\">(.*)</att>");
        for (int f = 0; f < 3; f++) {
            String fileName = bobsErddapContentDir + 
                (f == 0? "datasetsFED31UAF.xml" :
                 f == 1? "datasetsFEDCW.xml" :
                         "uniqueCFUnits.txt");

            ArrayList<String> lines = String2.readLinesFromFile(fileName, String2.ISO_8859_1, 1); //nAttempts
            int nLines = lines.size();
            for (int i = 0; i < nLines; i++) {
                String s = String2.extractCaptureGroup(lines.get(i), pattern, 1); //captureGroupNumber
                if (String2.isSomething(s)) 
                    set.add(XML.decodeEntities(s));
            }
        }
        StringArray sa = new StringArray(set.toArray());
        set = null;
        sa.sortIgnoreCase();
        //sa.toFile(cfUnique, String2.UTF_8, "\n");
        //String2.log(sa.toNewlineString());
        return sa;
    }

    /**
     * This tries to return a standardized (lightly canonical) version of a UDUnits string.
     * This is mostly about standardizing syntax and converting synonyms to 1 option.
     * This is just a standard way of writing the units as given (e.g., cm stays as cm,
     * not as 0.01 m).
     * This doesn't convert to low level canonical units as does UDUnits units.getCanonical().
     * @return the standardizedUdunits. If it was null, this returns "".
     */
    public static String safeStandardizeUdunits(String udunits) {
        if (!String2.isSomething(udunits))
            return "";
        udunits = udunits.trim(); //so search HM works as expected
        String t = standardizeUdunitsHM.get(udunits);  //do exceptions first
        return t == null? safeUcumToUdunits(safeUdunitsToUcum(udunits)) : t; //do a round trip
    }

    /**
     * This returns the UDUNITS canonical version of the units.
     * It is pretty extreme, e.g., converting Joules to kg m2 s-2
     * and treating Hz (steady frequency) and becquerel's (radioactivity) 
     * as s-1.
     * 
     * @return the canonical units (or null if trouble)
     */
    public static String safeCanonicalUdunitsString(String udunits) {
        try {
            Unit units = unitFormat.parse(udunits);
            return units == null? null : units.getCanonicalString();
        } catch (Exception e) {
            return null;
        }
    } 


    /**
     * This generates all the detailed tests.
     */
     public static void generateTests() throws Exception {
         StringArray uaf = getTestUdunits();
         for (int i = 0; i < uaf.size(); i++) 
             generateOneTest(uaf.get(i));
     }  

     public static void generateOneTest(String udunit) {
         String pad = "\n                    ";
         String ucum = udunitsToUcum(udunit);
         String udunits2 = safeStandardizeUdunits(udunit);
         String ucum2 = udunitsToUcum(udunits2);
         String a = String2.left(String2.toJson(udunit)   + ", ", 20);
         String b = String2.left(String2.toJson(ucum)     + ", ", 20);
         String c = String2.left(String2.toJson(udunits2) + ", ", 20);
         String d = String2.toJson(ucum2);

         String2.log("testToUcumToUdunits(" + 
             a + (a.length() > 20? pad : "") +
             b + (a.length() > 20? pad : "") +
             c + (a.length() > 20? pad : "") +
             d + ");");

         //String2.log(String2.left("" + udunit, 15)  + " = " + 
         //            String2.left("" + safeCanonicalString(udunit), 15) + " = " + 
         //            String2.left("" + udunitsToUcum(udunit), 15) + " = " + 
         //            standardizeUdunits(udunit));
     }

     public static void repeatedlyTestOneUdunit() throws Exception {
         while (true) {
             String udunits = String2.getStringFromSystemIn("Udunits? ");
             String2.log(
                   "ud canonical=" + safeCanonicalUdunitsString(udunits) +
                 "\n        ucum=" + safeUdunitsToUcum(udunits) +
                 "\n  toUcumToUd=" + safeStandardizeUdunits(udunits));
         }
     }


    /** 
     * This is used every year or so: 
     * Given a CF standard names XML file, this finds all canonical units 
     *   strings, reduces that to just the unique names, makes a few changes
     *   (e.g., K to degree_C) and saves that to cfUnique. 
     *
     * @param fullCFXMLFileName A CF standard names XML file downloaded from
     *    https://cfconventions.org/standard-names.html
     * @throws Exception if trouble
     */
    public static void gatherUniqueCFUnits(String fullCFXMLFileName) throws Exception {

        ArrayList<String> lines = String2.readLinesFromFile(fullCFXMLFileName, String2.UTF_8, 1); //nAttempts
        HashSet reject = new HashSet();
        HashSet set = new HashSet();
        set.add("degree_C");  //test it, too
        Pattern pattern = Pattern.compile("<canonical_units>(.*)</canonical_units>");
        int nLines = lines.size();
        for (int i = 0; i < nLines; i++) {
            String s = String2.extractCaptureGroup(lines.get(i), pattern, 1); //captureGroupNumber
            if (s != null) {
                if (s.startsWith("1e-") || //eg. "1e-3 kg m-2"  All are present and better without 1e-3.
                    s.equals("m -1") ||    //I think those aren't valid udunits
                    s.equals("J kg -1")) { //  and version without space exists 
                    reject.add(s); 
                    continue;
                }
                if (s.equals("degrees")) //makes no sense to have degree and degrees
                    s = "degree";
                s = String2.replaceAll(s, "mole", "mol");                
                set.add(s);
            }
        }
        StringArray sa = new StringArray(set.toArray());
        set = null;
        sa.sortIgnoreCase();
        sa.toFile(cfUnique, String2.UTF_8, "\n");
        String2.log(sa.toNewlineString());
        sa = new StringArray(reject.toArray());
        sa.sortIgnoreCase();
        String2.log("Rejected:\n" + sa.toNewlineString());
        String2.log(
            "* UdunitsHelper.gatherUniqueCFUnits() successfully wrote the list of unique CF units to\n" +
            cfUnique);
    }

    /**
     * If the variable is packed with scale_factor and/or add_offset, 
     *  this will unpack the packed attributes of the variable: 
     *   actual_range, actual_min, actual_max, 
     *   data_max, data_min, 
     *   valid_max, valid_min, valid_range.
     * missing_value and _FillValue will be converted to PA standard mv 
     *   for the the unpacked datatype (or current type if not packed).
     *
     * @param atts the set of attributes that will be unpacked/revised.
     * @param varName the var's fullName, for diagnostic messages only
     * @param oPAType the var's initial elementPAType
     */    
    public static void unpackVariableAttributes(Attributes atts, String varName, PAType oPAType) {

        if (debugMode)
            String2.log(">> unpackVariableAttributes for varName=" + varName);

        Attributes newAtts = atts;   //the results, so it has a more descriptive name     
        Attributes oldAtts = new Attributes(atts); //so we have an unchanged copy to refer to

        //deal with numeric time units
        String oUnits = newAtts.getString("units");
        if (oUnits == null || !String2.isSomething(oUnits))
            newAtts.remove("units"); 
        else if (Calendar2.isNumericTimeUnits(oUnits)) 
            newAtts.set("units", Calendar2.SECONDS_SINCE_1970); //AKA EDV.TIME_UNITS
            //presumably, String time var doesn't have numeric time units
        else if (Calendar2.isStringTimeUnits(oUnits)) 
            {}
        else 
            newAtts.set("units", Units2.safeStandardizeUdunits(oUnits));

        PrimitiveArray unsignedPA = newAtts.remove("_Unsigned");
        boolean unsigned = unsignedPA != null && "true".equals(unsignedPA.toString());         
        PrimitiveArray scalePA = newAtts.remove("scale_factor");
        PrimitiveArray addPA   = newAtts.remove("add_offset");

        //if present, convert _FillValue and missing_value to PA standard mv
        PAType destPAType = 
            scalePA != null? scalePA.elementType() :
            addPA   != null? addPA.elementType() : 
            unsigned && oPAType == PAType.BYTE?  PAType.UBYTE  : //was PAType.SHORT :  //similar code below
            unsigned && oPAType == PAType.SHORT? PAType.USHORT : //was PAType.INT :
            unsigned && oPAType == PAType.INT?   PAType.UINT   : //was PAType.DOUBLE : //ints are converted to double because nc3 doesn't support long
            unsigned && oPAType == PAType.LONG?  PAType.ULONG  : //was PAType.DOUBLE : //longs are converted to double (not ideal)
            oPAType;  
        if (newAtts.remove("_FillValue")    != null)
            newAtts.set(   "_FillValue",    PrimitiveArray.factory(destPAType, 1, ""));
        if (newAtts.remove("missing_value") != null)
            newAtts.set(   "missing_value", PrimitiveArray.factory(destPAType, 1, ""));

        //if var isn't packed, we're done
        if (!unsigned && scalePA == null && addPA == null)
            return; 

        //var is packed, so unpack all packed numeric attributes
        //lookForStringTimes is false because these are all attributes of numeric variables
        if (debugMode) 
            String2.log(">> before unpack " + varName + 
                " unsigned="      + unsigned +
                " scale_factor="  + scalePA + 
                " add_offset="    + addPA + 
                " actual_max="    + oldAtts.get("actual_max") + 
                " actual_min="    + oldAtts.get("actual_min") +
                " actual_range="  + oldAtts.get("actual_range") + 
                " data_max="      + oldAtts.get("data_max") + 
                " data_min="      + oldAtts.get("data_min") +
                " valid_max="     + oldAtts.get("valid_max") + 
                " valid_min="     + oldAtts.get("valid_min") +
                " valid_range="   + oldAtts.get("valid_range"));

        //attributes in nc3 files are never unsigned

        //if scale and/or addOffset, then remove redundant related atts
        if (scalePA != null || addPA != null) {
            newAtts.remove("Intercept");
            newAtts.remove("Slope");
            String ss = newAtts.getString("Scaling");
            if ("linear".equals(ss)) {
                //remove if Scaling=linear
                newAtts.remove("Scaling");
                newAtts.remove("Scaling_Equation");
            }
        }

        //before erddap v1.82, ERDDAP said actual_range had packed values.
        //but CF 1.7 says actual_range is unpacked. 
        //So look at data types and guess which to do, with preference to believing they're already unpacked
        //Note that at this point in this method, we're dealing with a packed data variable
        if (destPAType != null && (destPAType == PAType.FLOAT || destPAType == PAType.DOUBLE)) {
            for (int i = 0; i < Attributes.signedToUnsignedAttNames.length; i++) {
                String name = Attributes.signedToUnsignedAttNames[i];
                PrimitiveArray pa = newAtts.get(name);
                if (pa != null && !(pa instanceof FloatArray) && !(pa instanceof DoubleArray))
                    newAtts.set(name, oldAtts.unpackPA(varName, pa, false, false)); 
            }
        }

        if (debugMode) 
            String2.log(">> after  unpack " + varName + 
                " unsigned="      + unsigned +               
                " actual_max="    + newAtts.get("actual_max") + 
                " actual_min="    + newAtts.get("actual_min") +
                " actual_range="  + newAtts.get("actual_range") + 
                " data_max="      + newAtts.get("data_max") + 
                " data_min="      + newAtts.get("data_min") +
                " valid_max="     + newAtts.get("valid_max") + 
                " valid_min="     + newAtts.get("valid_min") +
                " valid_range="   + newAtts.get("valid_range"));
    }

    /**
     * This tests if the canonical units for each unique CF unit is unique.
     */
    public static void testIfCFCanonicalUnitsUnique() throws Throwable {
        StringArray sa = StringArray.fromFile(cfUnique, String2.UTF_8);
        Attributes atts = new Attributes(); //use it as a hashmap: canon -> source
        for (int i = 0; i < sa.size(); i++) {
            String s = sa.get(i);
            Unit units = unitFormat.parse(s);
            String canon = units.getCanonicalString();
            String already = atts.getString(canon);
            if (already != null) 
                String2.log("! " + s + " and " + already + " both have canonicalUnits=" + canon);
            atts.set(canon, s);
        }
        String2.log("\nThe canonical -> source pairs:" +
            atts.toString());
    }



/*
    UnitFormat unitFormat = ucar.units.StandardUnitFormat.instance();
    Unit unitMS = unitFormat.parse("m/s");
    String2.log("unitMS=" + unitMS.toString() + "=" + unitMS.getCanonicalString());
    Unit unitK  = unitFormat.parse("knots");
    String2.log("unitK=" + unitK.toString() + "=" + unitK.getCanonicalString());
    Unit unitZ  = unitFormat.parse("null");
    String2.log("unitZ=" + unitZ);
    String2.log("isCompatible=" + unitMS.isCompatible(unitK));
    ucar.units.Converter converter = unitK.getConverterTo(unitMS);
    String2.log("convert 1 knots to " + converter.convert(1.0) + " m/s");
    String2.log("convert 2 knots to " + converter.convert(2.0) + " m/s");

    Unit unitC = unitFormat.parse("degree_C");
    String2.log("unitC=" + unitC.toString() + "=" + unitC.getCanonicalString());
    Unit unitF  = unitFormat.parse("degree_F");
    String2.log("unitF=" + unitF.toString() + "=" + unitF.getCanonicalString());
    String2.log("isCompatible=" + unitC.isCompatible(unitF));
    ucar.units.Converter converter2 = unitF.getConverterTo(unitC);
    String2.log("convert 33 F to " + converter2.convert(33.0) + " C");

    while (true) {
        s = String2.getStringFromSystemIn("units? ");
        if (s.length() == 0) break;
        String2.log("\"" + s + "\" parses to " + unitFormat.parse(s));
    }
*/

    /**
     * This tests the udunitsToUcum and ucumToUdunits conversions.
     *
     * @param udunits the source udunits string
     * @param ucum the expected/desired, resulting ucum string from udunitsToUcum.
     * @param rt1 the sanitized udunits string created by round trip: udunits to ucum to udunits.
     * @param rt2 the sanitized ucum string created by round trip: ucum to udunits to ucum.
     */
    public static void testToUcumToUdunits(String udunits, String ucum, String rt1, String rt2) {
        String ucum2 = safeUdunitsToUcum(udunits);
        Test.ensureEqual(ucum2, ucum, "ucum2 error for udunits=" + udunits);
        String udunits2 = safeUcumToUdunits(ucum2);
        Test.ensureEqual(udunits2, rt1, "rt1 error for udunits=" + udunits + ", ucum2=" + ucum2);
        String ucum3 = safeUdunitsToUcum(udunits2);
        Test.ensureEqual(ucum3, rt2, "rt2 error for udunits2=" + udunits2);
        Test.ensureEqual(ucum, rt2, "expected ucum and rt2 should be the same");
    }

    public static void testAllToUcumToUdnits() {
//                  source udunits,     expected ucum,      round trip udunits, rount trip ucum         
testToUcumToUdunits("#",                "{count}",          "count",            "{count}");
testToUcumToUdunits("%",                "%",                "%",                "%");
testToUcumToUdunits("(0 - 1)",          "1",                "1",                "1");
testToUcumToUdunits("(kg m-3) (m s-1)", "(kg.m-3).(m.s-1)", "(kg m-3) (m s-1)", "(kg.m-3).(m.s-1)");
testToUcumToUdunits("-",                "",                 "",                 "");
testToUcumToUdunits("0.001",            "10^-3",            "1.0E-3",           "10^-3");
testToUcumToUdunits("1",                "1",                "1",                "1");
testToUcumToUdunits("1-12",             "mo",               "month",            "mo");
testToUcumToUdunits("1-31",             "d",                "day",              "d");
testToUcumToUdunits("1./s",             "s-1",              "s-1",              "s-1");
testToUcumToUdunits("1/s",              "s-1",              "s-1",              "s-1");
testToUcumToUdunits("10**10 J m-2",     "10^10.J.m-2",      "1.0E10 J m-2",     "10^10.J.m-2");
testToUcumToUdunits("10**6 kg m-2 s-1", "10^6.kg.m-2.s-1",  "1.0E6 kg m-2 s-1", "10^6.kg.m-2.s-1");
testToUcumToUdunits("1e-3",             "10^-3",            "1.0E-3",           "10^-3");
testToUcumToUdunits("1e-3 day-1",       "10^-3.d-1",        "1.0E-3 day-1",     "10^-3.d-1");
testToUcumToUdunits("1e-9",             "10^-9",            "1.0E-9",           "10^-9");
testToUcumToUdunits("8 bits, encoded",  "{8 bits, encoded}",
                    "8 bits, encoded",  "{8 bits, encoded}");
testToUcumToUdunits("?",                "",                 "",                 "");
testToUcumToUdunits("???",              "",                 "",                 "");
testToUcumToUdunits("angular_degree",   "deg",              "degree",           "deg");
testToUcumToUdunits("becquerel per liter",
                                        "Bq.l-1",           "Bq l-1",           "Bq.l-1");
testToUcumToUdunits("biomass density unit per abundance unit",
                    "{biomass density unit per abundance unit}",
                    "biomass density unit per abundance unit",
                    "{biomass density unit per abundance unit}");
testToUcumToUdunits("bytes",            "By",               "byte",             "By");
testToUcumToUdunits("C",                "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("Cel",              "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("cells per milliliter",
                                        "{cells}.ml-1",     "cells ml-1",       "{cells}.ml-1");
testToUcumToUdunits("cells/milliliter", "{cells}.ml-1",     "cells ml-1",       "{cells}.ml-1");
testToUcumToUdunits("Celsius",          "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("celsius",          "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("Celsius day-1",    "Cel.d-1",          "degree_C day-1",   "Cel.d-1");
testToUcumToUdunits("centimeter",       "cm",               "cm",               "cm");
testToUcumToUdunits("centimeter per second",
                                        "cm.s-1",           "cm s-1",           "cm.s-1");
testToUcumToUdunits("centimeter^2",     "cm2",              "cm2",              "cm2");
testToUcumToUdunits("Centimeters",      "cm",               "cm",               "cm");
testToUcumToUdunits("centimeters",      "cm",               "cm",               "cm");
testToUcumToUdunits("cm",               "cm",               "cm",               "cm");
testToUcumToUdunits("cm s-1",           "cm.s-1",           "cm s-1",           "cm.s-1");
testToUcumToUdunits("cm-2 day-1",       "cm-2.d-1",         "cm-2 day-1",       "cm-2.d-1");
testToUcumToUdunits("cm/s",             "cm.s-1",           "cm s-1",           "cm.s-1");
testToUcumToUdunits("copies/uL",        "{copies}.ul-1",    "copies ul-1",      "{copies}.ul-1");
testToUcumToUdunits("correl",           "{correl}",         "correl",           "{correl}");
testToUcumToUdunits("count",            "{count}",          "count",            "{count}");
testToUcumToUdunits("count m-3",        "{count}.m-3",      "count m-3",        "{count}.m-3");
testToUcumToUdunits("count per 85 centimeter^2 per day",
                    "{count}.(85.cm2)-1.d-1",
                    "count (85 cm2)-1 day-1",
                    "{count}.(85.cm2)-1.d-1");
testToUcumToUdunits("count per liter",  "{count}.l-1",      "count l-1",        "{count}.l-1");
testToUcumToUdunits("count per meter^2",
                                        "{count}.m-2",      "count m-2",        "{count}.m-2");
testToUcumToUdunits("count per microliter",
                                        "{count}.ul-1",     "count ul-1",       "{count}.ul-1");
testToUcumToUdunits("counts",           "{count}",          "count",            "{count}");
testToUcumToUdunits("cubic kilometers", "km3",              "km3",              "km3");
testToUcumToUdunits("cubic meter",      "m3",               "m3",               "m3");
testToUcumToUdunits("cubic meter per kilogram",
                                        "m3.kg-1",          "m3 kg-1",          "m3.kg-1");
testToUcumToUdunits("day",              "d",                "day",              "d");
testToUcumToUdunits("day since 1992-10-05 00:00:00",
                    "d{since 1992-10-05T00:00:00Z}",
                    "days since 1992-10-05T00:00:00Z",
                    "d{since 1992-10-05T00:00:00Z}");
testToUcumToUdunits("day-1",            "d-1",              "day-1",            "d-1");
testToUcumToUdunits("day_of_year",      "d",                "day",              "d");
testToUcumToUdunits("days",             "d",                "day",              "d");
testToUcumToUdunits("days since 0000-01-01T00:00:00Z",
                    "d{since 0000-01-01T00:00:00Z}",
                    "days since 0000-01-01T00:00:00Z",
                    "d{since 0000-01-01T00:00:00Z}");
testToUcumToUdunits("days since 0000-1-1",
                    "d{since 0000-01-01}",
                    "days since 0000-01-01",
                    "d{since 0000-01-01}");
testToUcumToUdunits("days since 0001-01-01 00:00:00",
                    "d{since 0001-01-01T00:00:00Z}",
                    "days since 0001-01-01T00:00:00Z",
                    "d{since 0001-01-01T00:00:00Z}");
testToUcumToUdunits("days since 0001-01-01T00:00:00",
                    "d{since 0001-01-01T00:00:00Z}",
                    "days since 0001-01-01T00:00:00Z",
                    "d{since 0001-01-01T00:00:00Z}");
testToUcumToUdunits("days since 0001-01-01T00:00:00.000Z",
                    "d{since 0001-01-01T00:00:00.000Z}",
                    "days since 0001-01-01T00:00:00.000Z",
                    "d{since 0001-01-01T00:00:00.000Z}");
testToUcumToUdunits("days since 0001-01-01T00:00:00Z",
                    "d{since 0001-01-01T00:00:00Z}",
                    "days since 0001-01-01T00:00:00Z",
                    "d{since 0001-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1-1-1 00:00:0.0",
                    "d{since 0001-01-01T00:00:00.000Z}",
                    "days since 0001-01-01T00:00:00.000Z",
                    "d{since 0001-01-01T00:00:00.000Z}");
testToUcumToUdunits("days since 1-1-1 00:00:00",
                    "d{since 0001-01-01T00:00:00Z}",
                    "days since 0001-01-01T00:00:00Z",
                    "d{since 0001-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1700-01-01 00:00:00",
                    "d{since 1700-01-01T00:00:00Z}",
                    "days since 1700-01-01T00:00:00Z",
                    "d{since 1700-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1700-01-01T00:00:00Z",
                    "d{since 1700-01-01T00:00:00Z}",
                    "days since 1700-01-01T00:00:00Z",
                    "d{since 1700-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1800-01-01 00:00:0.0",
                    "d{since 1800-01-01T00:00:00.000Z}",
                    "days since 1800-01-01T00:00:00.000Z",
                    "d{since 1800-01-01T00:00:00.000Z}");
testToUcumToUdunits("days since 1800-01-01T00:00:00.000Z",
                    "d{since 1800-01-01T00:00:00.000Z}",
                    "days since 1800-01-01T00:00:00.000Z",
                    "d{since 1800-01-01T00:00:00.000Z}");
testToUcumToUdunits("days since 1800-01-01T00:00:00Z",
                    "d{since 1800-01-01T00:00:00Z}",
                    "days since 1800-01-01T00:00:00Z",
                    "d{since 1800-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1800-1-1 00:00:0.0",
                    "d{since 1800-01-01T00:00:00.000Z}",
                    "days since 1800-01-01T00:00:00.000Z",
                    "d{since 1800-01-01T00:00:00.000Z}");
testToUcumToUdunits("days since 1800-1-1 00:00:00",
                    "d{since 1800-01-01T00:00:00Z}",
                    "days since 1800-01-01T00:00:00Z",
                    "d{since 1800-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1850-01-01 00:00:00",
                    "d{since 1850-01-01T00:00:00Z}",
                    "days since 1850-01-01T00:00:00Z",
                    "d{since 1850-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1850-01-01T00:00:00Z",
                    "d{since 1850-01-01T00:00:00Z}",
                    "days since 1850-01-01T00:00:00Z",
                    "d{since 1850-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1861-01-01 00:00:00",
                    "d{since 1861-01-01T00:00:00Z}",
                    "days since 1861-01-01T00:00:00Z",
                    "d{since 1861-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1861-01-01T00:00:00",
                    "d{since 1861-01-01T00:00:00Z}",
                    "days since 1861-01-01T00:00:00Z",
                    "d{since 1861-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1861-01-01T00:00:00Z",
                    "d{since 1861-01-01T00:00:00Z}",
                    "days since 1861-01-01T00:00:00Z",
                    "d{since 1861-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1870-1-1 0:0:0",
                    "d{since 1870-01-01T00:00:00Z}",
                    "days since 1870-01-01T00:00:00Z",
                    "d{since 1870-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1891-01-01T00:00:00Z",
                    "d{since 1891-01-01T00:00:00Z}",
                    "days since 1891-01-01T00:00:00Z",
                    "d{since 1891-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1891-1-1 00:00:00",
                    "d{since 1891-01-01T00:00:00Z}",
                    "days since 1891-01-01T00:00:00Z",
                    "d{since 1891-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1900-01-01",
                    "d{since 1900-01-01}",
                    "days since 1900-01-01",
                    "d{since 1900-01-01}");
testToUcumToUdunits("days since 1900-01-01 00:00:00",
                    "d{since 1900-01-01T00:00:00Z}",
                    "days since 1900-01-01T00:00:00Z",
                    "d{since 1900-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1900-01-01T00:00:00Z",
                    "d{since 1900-01-01T00:00:00Z}",
                    "days since 1900-01-01T00:00:00Z",
                    "d{since 1900-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1900-12-31 00:00:00",
                    "d{since 1900-12-31T00:00:00Z}",
                    "days since 1900-12-31T00:00:00Z",
                    "d{since 1900-12-31T00:00:00Z}");
testToUcumToUdunits("days since 1902-01-01 12:00:00",
                    "d{since 1902-01-01T12:00:00Z}",
                    "days since 1902-01-01T12:00:00Z",
                    "d{since 1902-01-01T12:00:00Z}");
testToUcumToUdunits("days since 1920-01-01 00:00:00",
                    "d{since 1920-01-01T00:00:00Z}",
                    "days since 1920-01-01T00:00:00Z",
                    "d{since 1920-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1920-01-01T00:00:00Z",
                    "d{since 1920-01-01T00:00:00Z}",
                    "days since 1920-01-01T00:00:00Z",
                    "d{since 1920-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1950-01-01",
                    "d{since 1950-01-01}",
                    "days since 1950-01-01",
                    "d{since 1950-01-01}");
testToUcumToUdunits("days since 1950-01-01 00:00:00",
                    "d{since 1950-01-01T00:00:00Z}",
                    "days since 1950-01-01T00:00:00Z",
                    "d{since 1950-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1950-01-01T00:00:00Z",
                    "d{since 1950-01-01T00:00:00Z}",
                    "days since 1950-01-01T00:00:00Z",
                    "d{since 1950-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1970-01-01 0:0:0",
                    "d{since 1970-01-01T00:00:00Z}",
                    "days since 1970-01-01T00:00:00Z",
                    "d{since 1970-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1970-01-01T00:00:00Z",
                    "d{since 1970-01-01T00:00:00Z}",
                    "days since 1970-01-01T00:00:00Z",
                    "d{since 1970-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1970-01-01T12:00:00Z",
                    "d{since 1970-01-01T12:00:00Z}",
                    "days since 1970-01-01T12:00:00Z",
                    "d{since 1970-01-01T12:00:00Z}");
testToUcumToUdunits("days since 1970-01-05T00:00:00Z",
                    "d{since 1970-01-05T00:00:00Z}",
                    "days since 1970-01-05T00:00:00Z",
                    "d{since 1970-01-05T00:00:00Z}");
testToUcumToUdunits("days since 1970-01-15T00:00:00Z",
                    "d{since 1970-01-15T00:00:00Z}",
                    "days since 1970-01-15T00:00:00Z",
                    "d{since 1970-01-15T00:00:00Z}");
testToUcumToUdunits("days since 1970-1-1 00:00:00",
                    "d{since 1970-01-01T00:00:00Z}",
                    "days since 1970-01-01T00:00:00Z",
                    "d{since 1970-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1976-01-01T00:00:00",
                    "d{since 1976-01-01T00:00:00Z}",
                    "days since 1976-01-01T00:00:00Z",
                    "d{since 1976-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1981-01-01",
                    "d{since 1981-01-01}",
                    "days since 1981-01-01",
                    "d{since 1981-01-01}");
testToUcumToUdunits("days since 1982-01-01 00:00:00",
                    "d{since 1982-01-01T00:00:00Z}",
                    "days since 1982-01-01T00:00:00Z",
                    "d{since 1982-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1982-01-01T00:00:00Z",
                    "d{since 1982-01-01T00:00:00Z}",
                    "days since 1982-01-01T00:00:00Z",
                    "d{since 1982-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1983-04-16 12:00:00",
                    "d{since 1983-04-16T12:00:00Z}",
                    "days since 1983-04-16T12:00:00Z",
                    "d{since 1983-04-16T12:00:00Z}");
testToUcumToUdunits("days since 1990-01-01",
                    "d{since 1990-01-01}",
                    "days since 1990-01-01",
                    "d{since 1990-01-01}");
testToUcumToUdunits("days since 1990-01-01 00:00:00",
                    "d{since 1990-01-01T00:00:00Z}",
                    "days since 1990-01-01T00:00:00Z",
                    "d{since 1990-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1991-01-01 00:00:00",
                    "d{since 1991-01-01T00:00:00Z}",
                    "days since 1991-01-01T00:00:00Z",
                    "d{since 1991-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1997-01-01",
                    "d{since 1997-01-01}",
                    "days since 1997-01-01",
                    "d{since 1997-01-01}");
testToUcumToUdunits("days since 1997-01-01 00:00:00",
                    "d{since 1997-01-01T00:00:00Z}",
                    "days since 1997-01-01T00:00:00Z",
                    "d{since 1997-01-01T00:00:00Z}");
testToUcumToUdunits("days since 1999-01-01",
                    "d{since 1999-01-01}",
                    "days since 1999-01-01",
                    "d{since 1999-01-01}");
testToUcumToUdunits("days since 2000-01-01",
                    "d{since 2000-01-01}",
                    "days since 2000-01-01",
                    "d{since 2000-01-01}");
testToUcumToUdunits("days since 2000-01-01 00:00:00",
                    "d{since 2000-01-01T00:00:00Z}",
                    "days since 2000-01-01T00:00:00Z",
                    "d{since 2000-01-01T00:00:00Z}");
testToUcumToUdunits("days since 2000-01-01 00:00:00 UTC",
                    "d{since 2000-01-01T00:00:00Z}",
                    "days since 2000-01-01T00:00:00Z",
                    "d{since 2000-01-01T00:00:00Z}");
testToUcumToUdunits("days since 2000-01-01T00:00:00Z",
                    "d{since 2000-01-01T00:00:00Z}",
                    "days since 2000-01-01T00:00:00Z",
                    "d{since 2000-01-01T00:00:00Z}");
testToUcumToUdunits("days since 2001-01-01T00:00:00",
                    "d{since 2001-01-01T00:00:00Z}",
                    "days since 2001-01-01T00:00:00Z",
                    "d{since 2001-01-01T00:00:00Z}");
testToUcumToUdunits("days since 2002-01-01",
                    "d{since 2002-01-01}",
                    "days since 2002-01-01",
                    "d{since 2002-01-01}");
testToUcumToUdunits("days since 2002-01-01 00:00:00",
                    "d{since 2002-01-01T00:00:00Z}",
                    "days since 2002-01-01T00:00:00Z",
                    "d{since 2002-01-01T00:00:00Z}");
testToUcumToUdunits("days since 2002-1-1",
                    "d{since 2002-01-01}",
                    "days since 2002-01-01",
                    "d{since 2002-01-01}");
testToUcumToUdunits("days since 2006-01-01 00:00:00",
                    "d{since 2006-01-01T00:00:00Z}",
                    "days since 2006-01-01T00:00:00Z",
                    "d{since 2006-01-01T00:00:00Z}");
testToUcumToUdunits("days since 2006-01-01T00:00:00Z",
                    "d{since 2006-01-01T00:00:00Z}",
                    "days since 2006-01-01T00:00:00Z",
                    "d{since 2006-01-01T00:00:00Z}");
testToUcumToUdunits("days since 2010-01-01 00:00:00",
                    "d{since 2010-01-01T00:00:00Z}",
                    "days since 2010-01-01T00:00:00Z",
                    "d{since 2010-01-01T00:00:00Z}");
testToUcumToUdunits("days since 2011-01-01",
                    "d{since 2011-01-01}",
                    "days since 2011-01-01",
                    "d{since 2011-01-01}");
testToUcumToUdunits("days since 2011-01-01T00:00:00Z",
                    "d{since 2011-01-01T00:00:00Z}",
                    "days since 2011-01-01T00:00:00Z",
                    "d{since 2011-01-01T00:00:00Z}");
testToUcumToUdunits("days since 2011-01-03T12:00:00Z",
                    "d{since 2011-01-03T12:00:00Z}",
                    "days since 2011-01-03T12:00:00Z",
                    "d{since 2011-01-03T12:00:00Z}");
testToUcumToUdunits("dB",               "dB",               "dB",               "dB");
testToUcumToUdunits("dBar",             "dbar",             "dbar",             "dbar");
testToUcumToUdunits("dbar",             "dbar",             "dbar",             "dbar");
testToUcumToUdunits("dd",               "d",                "day",              "d");
testToUcumToUdunits("dd (01 to 31)",    "d",                "day",              "d");
testToUcumToUdunits("dd-MMM-yyyy",      "{dd-MMM-yyyy}",    "dd-MMM-yyyy",      "{dd-MMM-yyyy}");
testToUcumToUdunits("ddMMMyyyy",        "{ddMMMyyyy}",      "ddMMMyyyy",        "{ddMMMyyyy}");
testToUcumToUdunits("decibar",          "dbar",             "dbar",             "dbar");
testToUcumToUdunits("decibars",         "dbar",             "dbar",             "dbar");
testToUcumToUdunits("decimal hours",    "h",                "hour",             "h");
testToUcumToUdunits("deg C",            "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("deg C m/s",        "Cel.m.s-1",        "degree_C m s-1",   "Cel.m.s-1");
testToUcumToUdunits("Deg_C",            "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("deg_C",            "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("degC",             "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("degC m s-1",       "Cel.m.s-1",        "degree_C m s-1",   "Cel.m.s-1");
testToUcumToUdunits("degC m/s",         "Cel.m.s-1",        "degree_C m s-1",   "Cel.m.s-1");
testToUcumToUdunits("degC-m/s",         "Cel-m.s-1",        "degree_C-m s-1",   "Cel-m.s-1");  //invalid passes through
testToUcumToUdunits("degC/day",         "Cel.d-1",          "degree_C day-1",   "Cel.d-1");
testToUcumToUdunits("degK",             "K",                "degree_K",         "K");
testToUcumToUdunits("degree",           "deg",              "degree",           "deg");
testToUcumToUdunits("degree C",         "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("degree_C",         "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("degree_C day-1",   "Cel.d-1",          "degree_C day-1",   "Cel.d-1");
testToUcumToUdunits("degree_Celsius",   "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("degree_east",      "deg{east}",        "degrees_east",     "deg{east}");
testToUcumToUdunits("degree_F",         "[degF]",           "degree_F",         "[degF]");
testToUcumToUdunits("degree_K",         "K",                "degree_K",         "K");
testToUcumToUdunits("degree_north",     "deg{north}",       "degrees_north",    "deg{north}");
testToUcumToUdunits("degree_true",      "deg{true}",        "degrees_true",     "deg{true}");
testToUcumToUdunits("degrees",          "deg",              "degree",           "deg");
testToUcumToUdunits("degrees (+E)",     "deg{east}",        "degrees_east",     "deg{east}");
testToUcumToUdunits("degrees (+N)",     "deg{north}",       "degrees_north",    "deg{north}");
testToUcumToUdunits("degrees (clockwise from bow)",
                    "deg{clockwise from bow}",
                    "degree(clockwise from bow)",
                    "deg{clockwise from bow}");
testToUcumToUdunits("degrees (clockwise from true north)",
                                        "deg{true}",        "degrees_true",      "deg{true}");
testToUcumToUdunits("degrees (clockwise towards true north)",
                                        "deg{true}",        "degrees_true",      "deg{true}");
testToUcumToUdunits("degrees C",        "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("degrees Celcius",  "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("degrees Celsius",  "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("Degrees(azimuth)", "deg{azimuth}",     "degree(azimuth)",  "deg{azimuth}");
testToUcumToUdunits("degrees(azimuth)", "deg{azimuth}",     "degree(azimuth)",  "deg{azimuth}");
testToUcumToUdunits("Degrees, Oceanographic Convention, 0=toward N, 90=toward E",
                                        "deg{true}",        "degrees_true",     "deg{true}");
testToUcumToUdunits("degrees-east",     "deg{east}",        "degrees_east",     "deg{east}");
testToUcumToUdunits("degrees-north",    "deg{north}",       "degrees_north",    "deg{north}");
testToUcumToUdunits("degrees/min",      "deg.min-1",        "degree minute-1",  "deg.min-1");
testToUcumToUdunits("degrees_celsius",  "Cel",              "degree_C",         "Cel");
testToUcumToUdunits("degrees_E",        "deg{east}",        "degrees_east",     "deg{east}");
testToUcumToUdunits("degrees_east",     "deg{east}",        "degrees_east",     "deg{east}");
testToUcumToUdunits("degrees_N",        "deg{north}",       "degrees_north",    "deg{north}");
testToUcumToUdunits("degrees_north",    "deg{north}",       "degrees_north",    "deg{north}");
testToUcumToUdunits("degrees_true",     "deg{true}",        "degrees_true",     "deg{true}");
testToUcumToUdunits("Dobsons",          "{dobson}",         "dobson",           "{dobson}");
testToUcumToUdunits("dyn-cm",           "[g].cm",           "geopotential cm",  "[g].cm");
testToUcumToUdunits("dynamic meter",    "[g].m",            "geopotential m",   "[g].m");
testToUcumToUdunits("einstein m^-2 day^-1",
                                        "einstein.m-2.d-1", "einstein m-2 day-1","einstein.m-2.d-1");
testToUcumToUdunits("Einsteins m-2 d-1","einstein.m-2.d-1", "einstein m-2 day-1","einstein.m-2.d-1");
testToUcumToUdunits("fish per cubic kilometer",
                                        "{fish}.km-3",      "fish km-3",        "{fish}.km-3");
testToUcumToUdunits("fish per square kilometer",
                                        "{fish}.km-2",      "fish km-2",        "{fish}.km-2");
testToUcumToUdunits("fish per tow",     "{fish}.{tow}-1",   "fish tow-1",       "{fish}.{tow}-1");
testToUcumToUdunits("four digit year",  "a",                "year",             "a");
testToUcumToUdunits("frac.",            "1",                "1",                "1");
testToUcumToUdunits("fraction",         "1",                "1",                "1");
testToUcumToUdunits("fraction (between 0 and 1)",
                                        "1",                "1",                "1");
testToUcumToUdunits("ft",               "[ft_i]",           "ft",               "[ft_i]");
testToUcumToUdunits("g/kg",             "g.kg-1",           "g kg-1",           "g.kg-1");
testToUcumToUdunits("gm/kg",            "g.kg-1",           "g kg-1",           "g.kg-1");
testToUcumToUdunits("gpm",              "[gal_us].min-1",   "gallon minute-1",  "[gal_us].min-1");
testToUcumToUdunits("gram",             "g",                "g",                "g");
testToUcumToUdunits("gram per gram",    "g.g-1",            "g g-1",            "g.g-1");
testToUcumToUdunits("gram per meter^2", "g.m-2",            "g m-2",            "g.m-2");
testToUcumToUdunits("gram per meter^2 per day",
                                        "g.m-2.d-1",        "g m-2 day-1",      "g.m-2.d-1");
testToUcumToUdunits("gram per one tenth meter^2",
                                        "g.m-2.10-2",       "g m-2 10-2",       "g.m-2.10-2"); //???
testToUcumToUdunits("grams/kg",         "g.kg-1",           "g kg-1",           "g.kg-1");
testToUcumToUdunits("grams/kg m/s",     "g.kg-1.m.s-1",     "g kg-1 m s-1",     "g.kg-1.m.s-1");
testToUcumToUdunits("HH:MM:SS",         "{HH:mm:ss}",       "HH:mm:ss",         "{HH:mm:ss}");
testToUcumToUdunits("HHmm.m",           "{HHmm.m}",         "HHmm.m",           "{HHmm.m}");
testToUcumToUdunits("hour",             "h",                "hour",             "h");
testToUcumToUdunits("hour since 1984-01-14 14:00:00",
                    "h{since 1984-01-14T14:00:00Z}",
                    "hours since 1984-01-14T14:00:00Z",
                    "h{since 1984-01-14T14:00:00Z}");
testToUcumToUdunits("hour since 2000-01-01 00:00 UTC",
                    "h{since 2000-01-01T00:00:00Z}",
                    "hours since 2000-01-01T00:00:00Z",
                    "h{since 2000-01-01T00:00:00Z}");
testToUcumToUdunits("Hour since 2013-02-13T00:00:00Z",
                    "h{since 2013-02-13T00:00:00Z}",
                    "hours since 2013-02-13T00:00:00Z",
                    "h{since 2013-02-13T00:00:00Z}");
testToUcumToUdunits("Hour since 2013-02-15T12:00:00Z",
                    "h{since 2013-02-15T12:00:00Z}",
                    "hours since 2013-02-15T12:00:00Z",
                    "h{since 2013-02-15T12:00:00Z}");
testToUcumToUdunits("hours",            "h",                "hour",             "h");
testToUcumToUdunits("hours since 0001-01-01T00:00:00.000Z",
                    "h{since 0001-01-01T00:00:00.000Z}",
                    "hours since 0001-01-01T00:00:00.000Z",
                    "h{since 0001-01-01T00:00:00.000Z}");
testToUcumToUdunits("hours since 1-1-1 00:00:0.0",
                    "h{since 0001-01-01T00:00:00.000Z}",
                    "hours since 0001-01-01T00:00:00.000Z",
                    "h{since 0001-01-01T00:00:00.000Z}");
testToUcumToUdunits("hours since 1800-01-01 00:00",
                    "h{since 1800-01-01T00:00:00Z}",
                    "hours since 1800-01-01T00:00:00Z",
                    "h{since 1800-01-01T00:00:00Z}");
testToUcumToUdunits("hours since 1800-01-01 00:00:0.0",
                    "h{since 1800-01-01T00:00:00.000Z}",
                    "hours since 1800-01-01T00:00:00.000Z",
                    "h{since 1800-01-01T00:00:00.000Z}");
testToUcumToUdunits("hours since 1800-01-01T00:00:00.000Z",
                    "h{since 1800-01-01T00:00:00.000Z}",
                    "hours since 1800-01-01T00:00:00.000Z",
                    "h{since 1800-01-01T00:00:00.000Z}");
testToUcumToUdunits("hours since 1800-01-01T00:00:00Z",
                    "h{since 1800-01-01T00:00:00Z}",
                    "hours since 1800-01-01T00:00:00Z",
                    "h{since 1800-01-01T00:00:00Z}");
testToUcumToUdunits("hours since 1800-1-1 00:00:0.0",
                    "h{since 1800-01-01T00:00:00.000Z}",
                    "hours since 1800-01-01T00:00:00.000Z",
                    "h{since 1800-01-01T00:00:00.000Z}");
testToUcumToUdunits("hours since 1800-1-1 00:00:00",
                    "h{since 1800-01-01T00:00:00Z}",
                    "hours since 1800-01-01T00:00:00Z",
                    "h{since 1800-01-01T00:00:00Z}");
testToUcumToUdunits("hours since 1901-01-15",
                    "h{since 1901-01-15}",
                    "hours since 1901-01-15",
                    "h{since 1901-01-15}");
testToUcumToUdunits("HOURS since 1901-01-15 00:00:00",
                    "h{since 1901-01-15T00:00:00Z}",
                    "hours since 1901-01-15T00:00:00Z",
                    "h{since 1901-01-15T00:00:00Z}");
testToUcumToUdunits("hours since 1948-01-01",
                    "h{since 1948-01-01}",
                    "hours since 1948-01-01",
                    "h{since 1948-01-01}");
testToUcumToUdunits("hours since 1950-01-01T00:00:00Z",
                    "h{since 1950-01-01T00:00:00Z}",
                    "hours since 1950-01-01T00:00:00Z",
                    "h{since 1950-01-01T00:00:00Z}");
testToUcumToUdunits("hours since 1950-1-1 0:0:0",
                    "h{since 1950-01-01T00:00:00Z}",
                    "hours since 1950-01-01T00:00:00Z",
                    "h{since 1950-01-01T00:00:00Z}");
testToUcumToUdunits("hours since 1970-01-01 00:00:00",
                    "h{since 1970-01-01T00:00:00Z}",
                    "hours since 1970-01-01T00:00:00Z",
                    "h{since 1970-01-01T00:00:00Z}");
testToUcumToUdunits("hours since 1970-01-01T00:00:00Z",
                    "h{since 1970-01-01T00:00:00Z}",
                    "hours since 1970-01-01T00:00:00Z",
                    "h{since 1970-01-01T00:00:00Z}");
testToUcumToUdunits("hours since 1987-01-01 00:00:0.0",
                    "h{since 1987-01-01T00:00:00.000Z}",
                    "hours since 1987-01-01T00:00:00.000Z",
                    "h{since 1987-01-01T00:00:00.000Z}");
testToUcumToUdunits("hours since 1987-01-01 00:00:00",
                    "h{since 1987-01-01T00:00:00Z}",
                    "hours since 1987-01-01T00:00:00Z",
                    "h{since 1987-01-01T00:00:00Z}");
testToUcumToUdunits("hours since 1992-01-01T00:00:00Z",
                    "h{since 1992-01-01T00:00:00Z}",
                    "hours since 1992-01-01T00:00:00Z",
                    "h{since 1992-01-01T00:00:00Z}");
testToUcumToUdunits("hours since 1996-01-01",
                    "h{since 1996-01-01}",
                    "hours since 1996-01-01",
                    "h{since 1996-01-01}");
testToUcumToUdunits("hours since 1998-01-01",
                    "h{since 1998-01-01}",
                    "hours since 1998-01-01",
                    "h{since 1998-01-01}");
testToUcumToUdunits("hours since 2000-01-01 00:00:00",
                    "h{since 2000-01-01T00:00:00Z}",
                    "hours since 2000-01-01T00:00:00Z",
                    "h{since 2000-01-01T00:00:00Z}");
testToUcumToUdunits("hours since 2006-01-01 00:00:00.000 UTC",
                    "h{since 2006-01-01T00:00:00.000Z}",
                    "hours since 2006-01-01T00:00:00.000Z",
                    "h{since 2006-01-01T00:00:00.000Z}");
testToUcumToUdunits("hours since 2006-01-01T00:00:00.000Z",
                    "h{since 2006-01-01T00:00:00.000Z}",
                    "hours since 2006-01-01T00:00:00.000Z",
                    "h{since 2006-01-01T00:00:00.000Z}");
testToUcumToUdunits("hours since 2006-01-01T00:00:00Z",
                    "h{since 2006-01-01T00:00:00Z}",
                    "hours since 2006-01-01T00:00:00Z",
                    "h{since 2006-01-01T00:00:00Z}");
testToUcumToUdunits("hours since 2009-02-08 00:00:00.000 UTC",
                    "h{since 2009-02-08T00:00:00.000Z}",
                    "hours since 2009-02-08T00:00:00.000Z",
                    "h{since 2009-02-08T00:00:00.000Z}");
testToUcumToUdunits("hours since 2009-02-08T00:00:00.000Z",
                    "h{since 2009-02-08T00:00:00.000Z}",
                    "hours since 2009-02-08T00:00:00.000Z",
                    "h{since 2009-02-08T00:00:00.000Z}");
testToUcumToUdunits("hours since 2009-02-08T00:00:00Z",
                    "h{since 2009-02-08T00:00:00Z}",
                    "hours since 2009-02-08T00:00:00Z",
                    "h{since 2009-02-08T00:00:00Z}");
testToUcumToUdunits("hours since 2009-05-03 00:00:00.000 UTC",
                    "h{since 2009-05-03T00:00:00.000Z}",
                    "hours since 2009-05-03T00:00:00.000Z",
                    "h{since 2009-05-03T00:00:00.000Z}");
testToUcumToUdunits("hours since 2009-05-03T00:00:00.000Z",
                    "h{since 2009-05-03T00:00:00.000Z}",
                    "hours since 2009-05-03T00:00:00.000Z",
                    "h{since 2009-05-03T00:00:00.000Z}");
testToUcumToUdunits("hours since 2009-05-03T00:00:00Z",
                    "h{since 2009-05-03T00:00:00Z}",
                    "hours since 2009-05-03T00:00:00Z",
                    "h{since 2009-05-03T00:00:00Z}");
testToUcumToUdunits("hours since 2009-11-19 00:00:00.000 UTC",
                    "h{since 2009-11-19T00:00:00.000Z}",
                    "hours since 2009-11-19T00:00:00.000Z",
                    "h{since 2009-11-19T00:00:00.000Z}");
testToUcumToUdunits("hours since 2009-11-19T00:00:00.000Z",
                    "h{since 2009-11-19T00:00:00.000Z}",
                    "hours since 2009-11-19T00:00:00.000Z",
                    "h{since 2009-11-19T00:00:00.000Z}");
testToUcumToUdunits("hours since 2009-11-19T00:00:00Z",
                    "h{since 2009-11-19T00:00:00Z}",
                    "hours since 2009-11-19T00:00:00Z",
                    "h{since 2009-11-19T00:00:00Z}");
testToUcumToUdunits("hours since 2010-01-13 00:00:00.000 UTC",
                    "h{since 2010-01-13T00:00:00.000Z}",
                    "hours since 2010-01-13T00:00:00.000Z",
                    "h{since 2010-01-13T00:00:00.000Z}");
testToUcumToUdunits("hours since 2010-01-13T00:00:00.000Z",
                    "h{since 2010-01-13T00:00:00.000Z}",
                    "hours since 2010-01-13T00:00:00.000Z",
                    "h{since 2010-01-13T00:00:00.000Z}");
testToUcumToUdunits("hours since 2010-01-13T00:00:00Z",
                    "h{since 2010-01-13T00:00:00Z}",
                    "hours since 2010-01-13T00:00:00Z",
                    "h{since 2010-01-13T00:00:00Z}");
testToUcumToUdunits("hours since 2010-01-23 00:00:00.000 UTC",
                    "h{since 2010-01-23T00:00:00.000Z}",
                    "hours since 2010-01-23T00:00:00.000Z",
                    "h{since 2010-01-23T00:00:00.000Z}");
testToUcumToUdunits("hours since 2010-01-23T00:00:00.000Z",
                    "h{since 2010-01-23T00:00:00.000Z}",
                    "hours since 2010-01-23T00:00:00.000Z",
                    "h{since 2010-01-23T00:00:00.000Z}");
testToUcumToUdunits("hours since 2010-01-23T00:00:00Z",
                    "h{since 2010-01-23T00:00:00Z}",
                    "hours since 2010-01-23T00:00:00Z",
                    "h{since 2010-01-23T00:00:00Z}");
testToUcumToUdunits("hours since 2010-02-08 00:00:00.000 UTC",
                    "h{since 2010-02-08T00:00:00.000Z}",
                    "hours since 2010-02-08T00:00:00.000Z",
                    "h{since 2010-02-08T00:00:00.000Z}");
testToUcumToUdunits("hours since 2010-02-08T00:00:00.000Z",
                    "h{since 2010-02-08T00:00:00.000Z}",
                    "hours since 2010-02-08T00:00:00.000Z",
                    "h{since 2010-02-08T00:00:00.000Z}");
testToUcumToUdunits("hours since 2010-02-08T00:00:00Z",
                    "h{since 2010-02-08T00:00:00Z}",
                    "hours since 2010-02-08T00:00:00Z",
                    "h{since 2010-02-08T00:00:00Z}");
testToUcumToUdunits("hours since 2010-02-24 00:00:00.000 UTC",
                    "h{since 2010-02-24T00:00:00.000Z}",
                    "hours since 2010-02-24T00:00:00.000Z",
                    "h{since 2010-02-24T00:00:00.000Z}");
testToUcumToUdunits("hours since 2010-02-24T00:00:00.000Z",
                    "h{since 2010-02-24T00:00:00.000Z}",
                    "hours since 2010-02-24T00:00:00.000Z",
                    "h{since 2010-02-24T00:00:00.000Z}");
testToUcumToUdunits("hours since 2010-02-24T00:00:00Z",
                    "h{since 2010-02-24T00:00:00Z}",
                    "hours since 2010-02-24T00:00:00Z",
                    "h{since 2010-02-24T00:00:00Z}");
testToUcumToUdunits("hours since 2010-05-01 00:00:00.000 UTC",
                    "h{since 2010-05-01T00:00:00.000Z}",
                    "hours since 2010-05-01T00:00:00.000Z",
                    "h{since 2010-05-01T00:00:00.000Z}");
testToUcumToUdunits("hours since 2010-05-01T00:00:00.000Z",
                    "h{since 2010-05-01T00:00:00.000Z}",
                    "hours since 2010-05-01T00:00:00.000Z",
                    "h{since 2010-05-01T00:00:00.000Z}");
testToUcumToUdunits("hours since 2010-05-01T00:00:00Z",
                    "h{since 2010-05-01T00:00:00Z}",
                    "hours since 2010-05-01T00:00:00Z",
                    "h{since 2010-05-01T00:00:00Z}");
testToUcumToUdunits("hours since 2010-05-08 00:00:00.000 UTC",
                    "h{since 2010-05-08T00:00:00.000Z}",
                    "hours since 2010-05-08T00:00:00.000Z",
                    "h{since 2010-05-08T00:00:00.000Z}");
testToUcumToUdunits("hours since 2010-05-08T00:00:00.000Z",
                    "h{since 2010-05-08T00:00:00.000Z}",
                    "hours since 2010-05-08T00:00:00.000Z",
                    "h{since 2010-05-08T00:00:00.000Z}");
testToUcumToUdunits("hours since 2010-05-08T00:00:00Z",
                    "h{since 2010-05-08T00:00:00Z}",
                    "hours since 2010-05-08T00:00:00Z",
                    "h{since 2010-05-08T00:00:00Z}");
testToUcumToUdunits("hours since 2011-03-10T00:00:00Z",
                    "h{since 2011-03-10T00:00:00Z}",
                    "hours since 2011-03-10T00:00:00Z",
                    "h{since 2011-03-10T00:00:00Z}");
testToUcumToUdunits("hours since 2011-03-28T00:00:00Z",
                    "h{since 2011-03-28T00:00:00Z}",
                    "hours since 2011-03-28T00:00:00Z",
                    "h{since 2011-03-28T00:00:00Z}");
testToUcumToUdunits("hours since 2013-01-03T01:00:00Z",
                    "h{since 2013-01-03T01:00:00Z}",
                    "hours since 2013-01-03T01:00:00Z",
                    "h{since 2013-01-03T01:00:00Z}");
testToUcumToUdunits("hours since 2013-02-27 00:00:00.000 UTC",
                    "h{since 2013-02-27T00:00:00.000Z}",
                    "hours since 2013-02-27T00:00:00.000Z",
                    "h{since 2013-02-27T00:00:00.000Z}");
testToUcumToUdunits("hours since 2013-02-27T00:00:00.000Z",
                    "h{since 2013-02-27T00:00:00.000Z}",
                    "hours since 2013-02-27T00:00:00.000Z",
                    "h{since 2013-02-27T00:00:00.000Z}");
testToUcumToUdunits("hours since 2013-02-27T00:00:00Z",
                    "h{since 2013-02-27T00:00:00Z}",
                    "hours since 2013-02-27T00:00:00Z",
                    "h{since 2013-02-27T00:00:00Z}");
testToUcumToUdunits("hours since 2013-03-05 00:00:00.000 UTC",
                    "h{since 2013-03-05T00:00:00.000Z}",
                    "hours since 2013-03-05T00:00:00.000Z",
                    "h{since 2013-03-05T00:00:00.000Z}");
testToUcumToUdunits("hours since 2013-03-05T00:00:00.000Z",
                    "h{since 2013-03-05T00:00:00.000Z}",
                    "hours since 2013-03-05T00:00:00.000Z",
                    "h{since 2013-03-05T00:00:00.000Z}");
testToUcumToUdunits("hours since 2013-03-05T00:00:00Z",
                    "h{since 2013-03-05T00:00:00Z}",
                    "hours since 2013-03-05T00:00:00Z",
                    "h{since 2013-03-05T00:00:00Z}");
testToUcumToUdunits("hours since 2013-04-05 00:00:00.000 UTC",
                    "h{since 2013-04-05T00:00:00.000Z}",
                    "hours since 2013-04-05T00:00:00.000Z",
                    "h{since 2013-04-05T00:00:00.000Z}");
testToUcumToUdunits("hours since 2013-04-05T00:00:00.000Z",
                    "h{since 2013-04-05T00:00:00.000Z}",
                    "hours since 2013-04-05T00:00:00.000Z",
                    "h{since 2013-04-05T00:00:00.000Z}");
testToUcumToUdunits("hours since 2013-04-05T00:00:00Z",
                    "h{since 2013-04-05T00:00:00Z}",
                    "hours since 2013-04-05T00:00:00Z",
                    "h{since 2013-04-05T00:00:00Z}");
testToUcumToUdunits("hours since 2013-05-18 00:00:00.000 UTC",
                    "h{since 2013-05-18T00:00:00.000Z}",
                    "hours since 2013-05-18T00:00:00.000Z",
                    "h{since 2013-05-18T00:00:00.000Z}");
testToUcumToUdunits("hours since 2013-05-18T00:00:00.000Z",
                    "h{since 2013-05-18T00:00:00.000Z}",
                    "hours since 2013-05-18T00:00:00.000Z",
                    "h{since 2013-05-18T00:00:00.000Z}");
testToUcumToUdunits("hours since 2013-05-18T00:00:00Z",
                    "h{since 2013-05-18T00:00:00Z}",
                    "hours since 2013-05-18T00:00:00Z",
                    "h{since 2013-05-18T00:00:00Z}");
testToUcumToUdunits("hours since 2017-10-19T00:00:00Z",
                    "h{since 2017-10-19T00:00:00Z}",
                    "hours since 2017-10-19T00:00:00Z",
                    "h{since 2017-10-19T00:00:00Z}");
testToUcumToUdunits("hours since 2018-01-01 00:00:00.000 UTC",
                    "h{since 2018-01-01T00:00:00.000Z}",
                    "hours since 2018-01-01T00:00:00.000Z",
                    "h{since 2018-01-01T00:00:00.000Z}");
testToUcumToUdunits("hours since 2018-01-01T00:00:00.000Z",
                    "h{since 2018-01-01T00:00:00.000Z}",
                    "hours since 2018-01-01T00:00:00.000Z",
                    "h{since 2018-01-01T00:00:00.000Z}");
testToUcumToUdunits("hours since 2018-01-01T00:00:00Z",
                    "h{since 2018-01-01T00:00:00Z}",
                    "hours since 2018-01-01T00:00:00Z",
                    "h{since 2018-01-01T00:00:00Z}");
testToUcumToUdunits("hours since analysis", "h",            "hour",             "h");
testToUcumToUdunits("hPa",              "hPa",              "hPa",              "hPa");
testToUcumToUdunits("Hz",               "Hz",               "Hz",               "Hz");
testToUcumToUdunits("index",            "{index}",          "index",            "{index}");
testToUcumToUdunits("integer",          "1",                "1",                "1");
testToUcumToUdunits("J/kg",             "J.kg-1",           "J kg-1",           "J.kg-1");
testToUcumToUdunits("Julian days since December 31, 2010",
                    "d{since 2010-12-31}",
                    "days since 2010-12-31",
                    "d{since 2010-12-31}"); 
testToUcumToUdunits("K",                "K",                "degree_K",         "K");
testToUcumToUdunits("Kelvin",           "K",                "degree_K",         "K");
testToUcumToUdunits("kelvin",           "K",                "degree_K",         "K");
testToUcumToUdunits("Kelvins",          "K",                "degree_K",         "K");
testToUcumToUdunits("kg km-2",          "kg.km-2",          "kg km-2",          "kg.km-2");
testToUcumToUdunits("kg m-1 s-1",       "kg.m-1.s-1",       "kg m-1 s-1",       "kg.m-1.s-1");
testToUcumToUdunits("kg m-2",           "kg.m-2",           "kg m-2",           "kg.m-2");
testToUcumToUdunits("kg m-2 s-1",       "kg.m-2.s-1",       "kg m-2 s-1",       "kg.m-2.s-1");
testToUcumToUdunits("kg m-3",           "kg.m-3",           "kg m-3",           "kg.m-3");
testToUcumToUdunits("kg m^-3",          "kg.m-3",           "kg m-3",           "kg.m-3");
testToUcumToUdunits("kg/kg",            "kg.kg-1",          "kg kg-1",          "kg.kg-1");
testToUcumToUdunits("kg/kg/s",          "kg.kg-1.s-1",      "kg kg-1 s-1",      "kg.kg-1.s-1");
testToUcumToUdunits("kg/m",             "kg.m-1",           "kg m-1",           "kg.m-1");
testToUcumToUdunits("kg/m2/s",          "kg.m-2.s-1",       "kg m-2 s-1",       "kg.m-2.s-1");
testToUcumToUdunits("kg/m^2",           "kg.m-2",           "kg m-2",           "kg.m-2");
testToUcumToUdunits("Kg/m^2/s",         "kg.m-2.s-1",       "kg m-2 s-1",       "kg.m-2.s-1");
testToUcumToUdunits("kg/m^2/s",         "kg.m-2.s-1",       "kg m-2 s-1",       "kg.m-2.s-1");
testToUcumToUdunits("kg/m^3",           "kg.m-3",           "kg m-3",           "kg.m-3");
testToUcumToUdunits("kilogram",         "kg",               "kg",               "kg");
testToUcumToUdunits("kilogram meter-1 s-1",
                    "kg.m-1.s-1",       "kg m-1 s-1",       "kg.m-1.s-1");
testToUcumToUdunits("kilogram meter-2", "kg.m-2",           "kg m-2",           "kg.m-2");
testToUcumToUdunits("kilogram meter-2 second-1",
                                        "kg.m-2.s-1",       "kg m-2 s-1",       "kg.m-2.s-1");
testToUcumToUdunits("kilogram meter-3", "kg.m-3",           "kg m-3",           "kg.m-3");
testToUcumToUdunits("kilogram meter3",  "kg.m3",            "kg m3",            "kg.m3");
testToUcumToUdunits("kilogram per meter^2",
                                        "kg.m-2",           "kg m-2",           "kg.m-2");
testToUcumToUdunits("kilogram per meter^2 per day",
                                        "kg.m-2.d-1",       "kg m-2 day-1",     "kg.m-2.d-1");
testToUcumToUdunits("kilogram per meter^3",
                                        "kg.m-3",           "kg m-3",           "kg.m-3");
testToUcumToUdunits("kilograms per cubic meter",
                                        "kg.m-3",           "kg m-3",           "kg.m-3");
testToUcumToUdunits("kilograms per cubic meter - 1000",     //not ideal, not valid?
                                        "kg.m-3.-.1000",    "kg m-3 - 1000",   "kg.m-3.-.1000");  
testToUcumToUdunits("kilograms/cubic meter",
                                        "kg.m-3",           "kg m-3",           "kg.m-3");
testToUcumToUdunits("kilometers",       "km",               "km",               "km");
testToUcumToUdunits("km*mm/hr",         "km.mm.h-1",        "km mm hour-1",     "km.mm.h-1");
testToUcumToUdunits("km2",              "km2",              "km2",              "km2");
testToUcumToUdunits("knots",            "[kn_i]",           "knot",             "[kn_i]");
testToUcumToUdunits("level",            "{level}",          "level",            "{level}");
testToUcumToUdunits("link",             "{link}",           "link",             "{link}");
testToUcumToUdunits("liter per second", "l.s-1",            "l s-1",            "l.s-1");
testToUcumToUdunits("local time",       "{local time}",     "local time",       "{local time}");
testToUcumToUdunits("m",                "m",                "m",                "m");
testToUcumToUdunits("m s**-1",          "m.s-1",            "m s-1",            "m.s-1");
testToUcumToUdunits("m s-1",            "m.s-1",            "m s-1",            "m.s-1");
testToUcumToUdunits("m**/s**2",         "m^.s-2",           "m^ s-2",           "m^.s-2");  //invalid
testToUcumToUdunits("m**3/s**3",        "m3.s-3",           "m3 s-3",           "m3.s-3");
testToUcumToUdunits("m*m/s",            "m2.s-1",           "m2 s-1",           "m2.s-1");
testToUcumToUdunits("m-1",              "m-1",              "m-1",              "m-1");
testToUcumToUdunits("M-d-yyyy",         "{M-d-yyyy}",       "M-d-yyyy",         "{M-d-yyyy}");
testToUcumToUdunits("m/min",            "m.min-1",          "m minute-1",       "m.min-1");
testToUcumToUdunits("m/s",              "m.s-1",            "m s-1",            "m.s-1");
testToUcumToUdunits("m/sec",            "m.s-1",            "m s-1",            "m.s-1");
testToUcumToUdunits("m2",               "m2",               "m2",               "m2");
testToUcumToUdunits("m2/s2",            "m2.s-2",           "m2 s-2",           "m2.s-2");
testToUcumToUdunits("m3",               "m3",               "m3",               "m3");
testToUcumToUdunits("m3 s-1",           "m3.s-1",           "m3 s-1",           "m3.s-1");
testToUcumToUdunits("m^-1",             "m-1",              "m-1",              "m-1");
testToUcumToUdunits("m^2/Hz",           "m2.Hz-1",          "m2 Hz-1",          "m2.Hz-1");
testToUcumToUdunits("m^2/s^2",          "m2.s-2",           "m2 s-2",           "m2.s-2");
testToUcumToUdunits("m^3 s^-1 100m^-1", "m3.s-1.100m-1",    "m3 s-1 100m-1",    "m3.s-1.100m-1"); //???
testToUcumToUdunits("mb",               "mbar",             "mbar",             "mbar");
testToUcumToUdunits("mBar",             "mbar",             "mbar",             "mbar");
testToUcumToUdunits("mean",             "{mean}",           "mean",             "{mean}");
testToUcumToUdunits("meter",            "m",                "m",                "m");
testToUcumToUdunits("meter per second", "m.s-1",            "m s-1",            "m.s-1");
testToUcumToUdunits("meter second-1",   "m.s-1",            "m s-1",            "m.s-1");
testToUcumToUdunits("meter-1",          "m-1",              "m-1",              "m-1");
testToUcumToUdunits("meter/sec",        "m.s-1",            "m s-1",            "m.s-1");
testToUcumToUdunits("meter2 second-1",  "m2.s-1",           "m2 s-1",           "m2.s-1");
testToUcumToUdunits("meter3 second-1",  "m3.s-1",           "m3 s-1",           "m3.s-1");
testToUcumToUdunits("meter^2",          "m2",               "m2",               "m2");
testToUcumToUdunits("meters",           "m",                "m",                "m");
testToUcumToUdunits("meters per second",
                                        "m.s-1",            "m s-1",            "m.s-1");
testToUcumToUdunits("meters/sec",       "m.s-1",            "m s-1",            "m.s-1");
testToUcumToUdunits("meters/second",    "m.s-1",            "m s-1",            "m.s-1");
testToUcumToUdunits("meters^3",         "m3",               "m3",               "m3");
testToUcumToUdunits("mg C m-2 day-1",   "mg.Cel.m-2.d-1",   "mg degree_C m-2 day-1",
                    "mg.Cel.m-2.d-1");
testToUcumToUdunits("mg Carbon m-2 day-1",
                    "mg.{Carbon}.m-2.d-1",
                    "mg Carbon m-2 day-1",
                    "mg.{Carbon}.m-2.d-1");
testToUcumToUdunits("mg m-3",           "mg.m-3",           "mg m-3",           "mg.m-3");
testToUcumToUdunits("mg m^-3",          "mg.m-3",           "mg m-3",           "mg.m-3");
testToUcumToUdunits("mg mg-1",          "mg.mg-1",          "mg mg-1",          "mg.mg-1");
testToUcumToUdunits("mhos m-1",         "mho.m-1",          "mho m-1",          "mho.m-1");
testToUcumToUdunits("micro-atmospheres","uatm",             "uatm",             "uatm");
testToUcumToUdunits("microatmosphere",  "uatm",             "uatm",             "uatm");
testToUcumToUdunits("microatomosphere", "uatm",             "uatm",             "uatm");
testToUcumToUdunits("microEinsteins m-2 s-1",
                                        "ueinstein.m-2.s-1","ueinstein m-2 s-1","ueinstein.m-2.s-1");
testToUcumToUdunits("microEinsteins m^-2 s-1",
                                        "ueinstein.m-2.s-1","ueinstein m-2 s-1","ueinstein.m-2.s-1");
testToUcumToUdunits("microgram",        "ug",               "ug",               "ug");
testToUcumToUdunits("micromol mol-1",   "umol.mol-1",       "umol mol-1",       "umol.mol-1");
testToUcumToUdunits("micromole kg-1",   "umol.kg-1",        "umol kg-1",        "umol.kg-1");
testToUcumToUdunits("micromole per centimeter^2 per hour",
                                        "umol.cm-2.h-1",    "umol cm-2 hour-1", "umol.cm-2.h-1");
testToUcumToUdunits("micromole per centimeter^2 per hour per photon flux",
                    "umol.cm-2.h-1.{photon_flux}-1",
                    "umol cm-2 hour-1 photon_flux-1",
                    "umol.cm-2.h-1.{photon_flux}-1");             //???
testToUcumToUdunits("micromole per kilogram",
                                        "umol.kg-1",        "umol kg-1",        "umol.kg-1");
testToUcumToUdunits("micromole per liter",
                                        "umol.l-1",         "umol l-1",         "umol.l-1");
testToUcumToUdunits("micromole per meter^2 per second",
                                        "umol.m-2.s-1",     "umol m-2 s-1",     "umol.m-2.s-1");
testToUcumToUdunits("micromoles L-1",   "umol.l-1",         "umol l-1",         "umol.l-1");
testToUcumToUdunits("micromoles per Kilogram",
                                        "umol.kg-1",        "umol kg-1",        "umol.kg-1");
testToUcumToUdunits("micromoles per liter (umoles L-1)",
                                        "umol.l-1",         "umol l-1",         "umol.l-1");
testToUcumToUdunits("micromoles/l",     "umol.l-1",         "umol l-1",         "umol.l-1");
testToUcumToUdunits("micromoles_per_liter",
                                        "umol.l-1",         "umol l-1",         "umol.l-1");
testToUcumToUdunits("microns",          "um",               "um",               "um");
testToUcumToUdunits("microwatt cm-2 s-2 nm-1 sr-1",
                    "uW.cm-2.s-2.nm-1.sr-1",
                    "uW cm-2 s-2 nm-1 sr-1",
                    "uW.cm-2.s-2.nm-1.sr-1");
testToUcumToUdunits("miles per hour",   "[mi_i].h-1",       "mile hour-1",      "[mi_i].h-1");
testToUcumToUdunits("millibar",         "mbar",             "mbar",             "mbar");
testToUcumToUdunits("milliBars",        "mbar",             "mbar",             "mbar");
testToUcumToUdunits("millibars",        "mbar",             "mbar",             "mbar");
testToUcumToUdunits("milligram",        "mg",               "mg",               "mg");
testToUcumToUdunits("milligram per gram per hour",
                                        "mg.g-1.h-1",       "mg g-1 hour-1",    "mg.g-1.h-1");
testToUcumToUdunits("milligram per gram per hour per photon flux",
                                        "mg.g-1.h-1.{photon_flux}-1",
                                        "mg g-1 hour-1 photon_flux-1",
                                        "mg.g-1.h-1.{photon_flux}-1");     //???
testToUcumToUdunits("milligram per liter",
                                        "mg.l-1",           "mg l-1",           "mg.l-1");
testToUcumToUdunits("milligram per meter^3",
                                        "mg.m-3",           "mg m-3",           "mg.m-3");
testToUcumToUdunits("milligrams per cubic meter",
                                        "mg.m-3",           "mg m-3",           "mg.m-3");
testToUcumToUdunits("milligrams per liter (mg/L)",
                                        "mg.l-1",           "mg l-1",           "mg.l-1");
testToUcumToUdunits("milliliter per liter",
                                        "ml.l-1",           "ml l-1",           "ml.l-1");
testToUcumToUdunits("milliliters per Liter",
                                        "ml.l-1",           "ml l-1",           "ml.l-1");
testToUcumToUdunits("milliliters_per_liter",
                                        "ml.l-1",           "ml l-1",           "ml.l-1");
testToUcumToUdunits("millimeter",       "mm",               "mm",               "mm");
testToUcumToUdunits("millimole per meter^2 per day",
                                        "mmol.m-2.d-1",     "mmol m-2 day-1",   "mmol.m-2.d-1");
testToUcumToUdunits("millimoles per kilogram",
                                        "mmol.kg-1",        "mmol kg-1",        "mmol.kg-1");
testToUcumToUdunits("milliseconds since 1980-1-1T00:00:00Z",
                    "ms{since 1980-01-01T00:00:00Z}",
                    "milliseconds since 1980-01-01T00:00:00Z",
                    "ms{since 1980-01-01T00:00:00Z}");
testToUcumToUdunits("milliSiemens per centimeter [mS/cm]",
                                        "mS.cm-1",          "mS cm-1",          "mS.cm-1");
testToUcumToUdunits("millivolts (mV)",  "mV",               "mV",               "mV");
//! Calendar2.tryToIsoString was unable to find a format for 1-1-1980 00:00 UTC
testToUcumToUdunits("minutes since 1-1-1980 00:00 UTC",
                    "min{since 1980-01-01T00:00:00Z}",
                    "minutes since 1980-01-01T00:00:00Z",
                    "min{since 1980-01-01T00:00:00Z}");
testToUcumToUdunits("minutes since 1980-01-01T00:00:00Z",
                    "min{since 1980-01-01T00:00:00Z}",
                    "minutes since 1980-01-01T00:00:00Z",
                    "min{since 1980-01-01T00:00:00Z}");
testToUcumToUdunits("minutes since 1980-1-1",
                    "min{since 1980-01-01}",
                    "minutes since 1980-01-01",
                    "min{since 1980-01-01}");
testToUcumToUdunits("minutes since 2017-01-01 00:00",
                    "min{since 2017-01-01T00:00:00Z}",
                    "minutes since 2017-01-01T00:00:00Z",
                    "min{since 2017-01-01T00:00:00Z}");
testToUcumToUdunits("minutes",          "min",              "minute",           "min");
testToUcumToUdunits("mL L-1",           "ml.l-1",           "ml l-1",           "ml.l-1");
testToUcumToUdunits("ml l^-1",          "ml.l-1",           "ml l-1",           "ml.l-1");
testToUcumToUdunits("ml/l",             "ml.l-1",           "ml l-1",           "ml.l-1");
testToUcumToUdunits("mm",               "mm",               "mm",               "mm");
testToUcumToUdunits("mm (01 to 12)",    "mo",               "month",            "mo");
testToUcumToUdunits("mm day-1",         "mm.d-1",           "mm day-1",         "mm.d-1");
testToUcumToUdunits("mm water per 1.38m soil (0 to 7 layers)",
                    "{mm water per 1.38m soil (0 to 7 layers)}",
                    "mm water per 1.38m soil (0 to 7 layers)",
                    "{mm water per 1.38m soil (0 to 7 layers)}");   //???
testToUcumToUdunits("MM-dd-yy",         "{MM-dd-yy}",       "MM-dd-yy",         "{MM-dd-yy}");
testToUcumToUdunits("MM-dd-yyyy",       "{MM-dd-yyyy}",     "MM-dd-yyyy",       "{MM-dd-yyyy}");
testToUcumToUdunits("mm/day",           "mm.d-1",           "mm day-1",         "mm.d-1");
testToUcumToUdunits("MM/dd/yy",         "{MM/dd/yy}",       "MM/dd/yy",         "{MM/dd/yy}");
testToUcumToUdunits("MM/dd/yy' 'HH:mm", "{MM/dd/yy' 'HH:mm}",
                    "MM/dd/yy' 'HH:mm", "{MM/dd/yy' 'HH:mm}");
testToUcumToUdunits("MM/dd/yyyy",       "{MM/dd/yyyy}",     "MM/dd/yyyy",       "{MM/dd/yyyy}");
testToUcumToUdunits("MM/dd/yyyy HH:mm", "{MM/dd/yyyy HH:mm}",
                    "MM/dd/yyyy HH:mm", "{MM/dd/yyyy HH:mm}");
testToUcumToUdunits("MM/HR",            "mm.h-1",           "mm hour-1",        "mm.h-1");
testToUcumToUdunits("mm/hr",            "mm.h-1",           "mm hour-1",        "mm.h-1");
testToUcumToUdunits("mm/mn",            "mm.mo-1",          "mm month-1",       "mm.mo-1");
testToUcumToUdunits("mm/month",         "mm.mo-1",          "mm month-1",       "mm.mo-1");
testToUcumToUdunits("mm/s",             "mm.s-1",           "mm s-1",           "mm.s-1");
testToUcumToUdunits("mm3/mm3",          "mm3.mm-3",         "mm3 mm-3",         "mm3.mm-3");
testToUcumToUdunits("mol m-2 s-1",      "mol.m-2.s-1",      "mol m-2 s-1",      "mol.m-2.s-1");
testToUcumToUdunits("mol m^-3",         "mol.m-3",          "mol m-3",          "mol.m-3");
testToUcumToUdunits("mol/cell",         "mol.{cell}-1",     "mol cell-1",       "mol.{cell}-1");
testToUcumToUdunits("mole per meter^2 per day",
                                        "mol.m-2.d-1",      "mol m-2 day-1",    "mol.m-2.d-1");
testToUcumToUdunits("molm-2Season-1",   "mol.m-2.{Season}-1","mol m-2 Season-1","mol.m-2.{Season}-1");
testToUcumToUdunits("molm-2Yr-1",       "mol.m-2.a-1",      "mol m-2 year-1",   "mol.m-2.a-1");
testToUcumToUdunits("month",            "mo",               "month",            "mo");
testToUcumToUdunits("months since 0000-01-01 00:00:00",
                    "mo{since 0000-01-01T00:00:00Z}",
                    "months since 0000-01-01T00:00:00Z",
                    "mo{since 0000-01-01T00:00:00Z}");
testToUcumToUdunits("months since 0000-01-01T00:00:00Z",
                    "mo{since 0000-01-01T00:00:00Z}",
                    "months since 0000-01-01T00:00:00Z",
                    "mo{since 0000-01-01T00:00:00Z}");
testToUcumToUdunits("months since 1400-01-15T00:00:00Z",
                    "mo{since 1400-01-15T00:00:00Z}",
                    "months since 1400-01-15T00:00:00Z",
                    "mo{since 1400-01-15T00:00:00Z}");
testToUcumToUdunits("months since 1400-1-15 00:00:00",
                    "mo{since 1400-01-15T00:00:00Z}",
                    "months since 1400-01-15T00:00:00Z",
                    "mo{since 1400-01-15T00:00:00Z}");
testToUcumToUdunits("months since 2010-01-15 00:00:00",
                    "mo{since 2010-01-15T00:00:00Z}",
                    "months since 2010-01-15T00:00:00Z",
                    "mo{since 2010-01-15T00:00:00Z}");
testToUcumToUdunits("MPa m-1",          "MPa.m-1",          "MPa m-1",          "MPa.m-1");
testToUcumToUdunits("mS cm-1",          "mS.cm-1",          "mS cm-1",          "mS.cm-1");
testToUcumToUdunits("mS/cm",            "mS.cm-1",          "mS cm-1",          "mS.cm-1");
testToUcumToUdunits("mW cm^-2 um^-1 sr^-1",
                                        "mW.cm-2.um-1.sr-1","mW cm-2 um-1 sr-1","mW.cm-2.um-1.sr-1");
testToUcumToUdunits("mWh",              "mWh",              "mWh",              "mWh");
testToUcumToUdunits("N m-2",            "N.m-2",            "N m-2",            "N.m-2");
testToUcumToUdunits("n/a",              "",                 "",                 "");
testToUcumToUdunits("N/m2",             "N.m-2",            "N m-2",            "N.m-2");
testToUcumToUdunits("N/m^2",            "N.m-2",            "N m-2",            "N.m-2");
testToUcumToUdunits("NA",               "",                 "",                 "");
testToUcumToUdunits("nanomoles per liter",
                                        "nmol.l-1",         "nmol l-1",         "nmol.l-1");
testToUcumToUdunits("nautical_miles",   "[nmi_i]",          "nautical_mile",    "[nmi_i]");
testToUcumToUdunits("Newton meter-2",   "N.m-2",            "N m-2",            "N.m-2");
testToUcumToUdunits("newton meter-2",   "N.m-2",            "N m-2",            "N.m-2");
testToUcumToUdunits("newton/meter2",    "N.m-2",            "N m-2",            "N.m-2");
testToUcumToUdunits("nm",               "nm",               "nm",               "nm");
testToUcumToUdunits("nominal day",      "d",                "day",              "d");
testToUcumToUdunits("none",             "",                 "",                 "");
testToUcumToUdunits("none (fraction)",  "1",                "1",                "1");
testToUcumToUdunits("null",             "",                 "",                 "");
testToUcumToUdunits("number of cells",  "{count}",          "count",            "{count}");
testToUcumToUdunits("number of observations",
                                        "{count}",          "count",            "{count}");
testToUcumToUdunits("numeric",          "1",                "1",                "1");
testToUcumToUdunits("Obs count",        "{count}",          "count",            "{count}");
testToUcumToUdunits("observations",     "{count}",          "count",            "{count}");
testToUcumToUdunits("okta",             "{okta}",           "okta",             "{okta}");
testToUcumToUdunits("Omega",            "{Omega}",          "Omega",            "{Omega}");
testToUcumToUdunits("Pa",               "Pa",               "Pa",               "Pa");
testToUcumToUdunits("pa",               "Pa",               "Pa",               "Pa");
testToUcumToUdunits("Pa m-1",           "Pa.m-1",           "Pa m-1",           "Pa.m-1");
testToUcumToUdunits("Pa s-1",           "Pa.s-1",           "Pa s-1",           "Pa.s-1");
testToUcumToUdunits("Pa/s",             "Pa.s-1",           "Pa s-1",           "Pa.s-1");
testToUcumToUdunits("parts per 1000000",
                                        "[ppm]",            "ppm",              "[ppm]");
testToUcumToUdunits("parts per million (ppm)",
                                        "[ppm]",            "ppm",              "[ppm]");
testToUcumToUdunits("Pascal",           "Pa",               "Pa",               "Pa");
testToUcumToUdunits("pascal",           "Pa",               "Pa",               "Pa");
testToUcumToUdunits("Pascal/s",         "Pa.s-1",           "Pa s-1",           "Pa.s-1");
testToUcumToUdunits("Pascals",          "Pa",               "Pa",               "Pa");
testToUcumToUdunits("per 1000",         "10^-3",            "1.0E-3",           "10^-3");
testToUcumToUdunits("per day",          "d-1",              "day-1",            "d-1");
testToUcumToUdunits("per meter",        "m-1",              "m-1",              "m-1");
testToUcumToUdunits("per meter per steradian",
                                        "m-1.sr-1",         "m-1 sr-1",         "m-1.sr-1");
testToUcumToUdunits("per mil relative to NIST SRM 3104a",
                    "[ppm].{relative to NIST SRM 3104a}",
                    "ppm relative to NIST SRM 3104a",
                    "[ppm].{relative to NIST SRM 3104a}");
testToUcumToUdunits("percent",          "%",                "%",                "%");
testToUcumToUdunits("percent cover",    "%.{cover}",        "% cover",          "%.{cover}");
testToUcumToUdunits("pH",               "[pH]",             "pH",               "[pH]");
testToUcumToUdunits("picomoles per liter",
                                        "pmol.l-1",         "pmol l-1",         "pmol.l-1");
testToUcumToUdunits("pounds",           "[lb_av]",          "pound",            "[lb_av]");
testToUcumToUdunits("ppm",              "[ppm]",            "ppm",              "[ppm]");
//CF standard names v25 switched to 1e-1, not PSU. Crazy. Useless. Don't do it.
testToUcumToUdunits("practical salinity units (PSU)",
                                        "{PSU}",            "PSU",              "{PSU}");
testToUcumToUdunits("PSS",              "{PSS}",            "PSS",              "{PSS}");
testToUcumToUdunits("PSS-78",           "{PSS_78}",         "PSS_78",           "{PSS_78}");
testToUcumToUdunits("PSU",              "{PSU}",            "PSU",              "{PSU}");
testToUcumToUdunits("psu",              "{PSU}",            "PSU",              "{PSU}");
testToUcumToUdunits("PSU day-1",        "{PSU}.d-1",        "PSU day-1",        "{PSU}.d-1");
testToUcumToUdunits("PSU m s-1",        "{PSU}.m.s-1",      "PSU m s-1",        "{PSU}.m.s-1");
testToUcumToUdunits("psu-m/s",          "{PSU}.m.s-1",      "PSU m s-1",        "{PSU}.m.s-1"); //invalid fixed up
testToUcumToUdunits("PSU/day",          "{PSU}.d-1",        "PSU day-1",        "{PSU}.d-1");
testToUcumToUdunits("psu/day",          "{PSU}.d-1",        "PSU day-1",        "{PSU}.d-1");
testToUcumToUdunits("radians",          "rad",              "radian",           "rad");
testToUcumToUdunits("RFU",              "{RFU}",            "RFU",              "{RFU}");
testToUcumToUdunits("s",                "s",                "s",                "s");
testToUcumToUdunits("S m-1",            "S.m-1",            "S m-1",            "S.m-1");
testToUcumToUdunits("s-1",              "s-1",              "s-1",              "s-1");
testToUcumToUdunits("S/m",              "S.m-1",            "S m-1",            "S.m-1");
testToUcumToUdunits("sec since 1970-01-01T00:00:00Z",
                    "s{since 1970-01-01T00:00:00Z}",
                    "seconds since 1970-01-01T00:00:00Z",
                    "s{since 1970-01-01T00:00:00Z}");
testToUcumToUdunits("second",           "s",                "s",                "s");
testToUcumToUdunits("second-1",         "s-1",              "s-1",              "s-1");
testToUcumToUdunits("seconds",          "s",                "s",                "s");
testToUcumToUdunits("seconds since 1970-01-01 00:00:00",
                    "s{since 1970-01-01T00:00:00Z}",
                    "seconds since 1970-01-01T00:00:00Z",
                    "s{since 1970-01-01T00:00:00Z}");
testToUcumToUdunits("seconds since 1970-01-01 00:00:00 UTC",
                    "s{since 1970-01-01T00:00:00Z}",
                    "seconds since 1970-01-01T00:00:00Z",
                    "s{since 1970-01-01T00:00:00Z}");
testToUcumToUdunits("seconds since 1970-01-01 00:00:00Z",
                    "s{since 1970-01-01T00:00:00Z}",
                    "seconds since 1970-01-01T00:00:00Z",
                    "s{since 1970-01-01T00:00:00Z}");
testToUcumToUdunits("seconds since 1970-01-01T00:00:00Z",
                    "s{since 1970-01-01T00:00:00Z}",
                    "seconds since 1970-01-01T00:00:00Z",
                    "s{since 1970-01-01T00:00:00Z}");
testToUcumToUdunits("seconds since 1970-01-01T12:00:00Z",
                    "s{since 1970-01-01T12:00:00Z}",
                    "seconds since 1970-01-01T12:00:00Z",
                    "s{since 1970-01-01T12:00:00Z}");
testToUcumToUdunits("seconds since 1970-01-05T00:00:00Z",
                    "s{since 1970-01-05T00:00:00Z}",
                    "seconds since 1970-01-05T00:00:00Z",
                    "s{since 1970-01-05T00:00:00Z}");
testToUcumToUdunits("seconds since 1970-01-15T00:00:00Z",
                    "s{since 1970-01-15T00:00:00Z}",
                    "seconds since 1970-01-15T00:00:00Z",
                    "s{since 1970-01-15T00:00:00Z}");
testToUcumToUdunits("seconds since 1970-01-16T00:00:00Z",
                    "s{since 1970-01-16T00:00:00Z}",
                    "seconds since 1970-01-16T00:00:00Z",
                    "s{since 1970-01-16T00:00:00Z}");
testToUcumToUdunits("seconds since 1981-01-01 00:00:00",
                    "s{since 1981-01-01T00:00:00Z}",
                    "seconds since 1981-01-01T00:00:00Z",
                    "s{since 1981-01-01T00:00:00Z}");
testToUcumToUdunits("seconds since 1981-01-01 00:00:00 UTC",
                    "s{since 1981-01-01T00:00:00Z}",
                    "seconds since 1981-01-01T00:00:00Z",
                    "s{since 1981-01-01T00:00:00Z}");
testToUcumToUdunits("seconds since 1993-01-01 00:00:00",
                    "s{since 1993-01-01T00:00:00Z}",
                    "seconds since 1993-01-01T00:00:00Z",
                    "s{since 1993-01-01T00:00:00Z}");
testToUcumToUdunits("seconds since 2010-01-19T00:00:00Z",
                    "s{since 2010-01-19T00:00:00Z}",
                    "seconds since 2010-01-19T00:00:00Z",
                    "s{since 2010-01-19T00:00:00Z}");
testToUcumToUdunits("seconds since 2015-1-1 00:00:00 UTC",
                    "s{since 2015-01-01T00:00:00Z}",
                    "seconds since 2015-01-01T00:00:00Z",
                    "s{since 2015-01-01T00:00:00Z}");
testToUcumToUdunits("siemens meter-1",  "S.m-1",            "S m-1",            "S.m-1");
testToUcumToUdunits("siemens per centimeter",
                                        "S.cm-1",           "S cm-1",           "S.cm-1");
testToUcumToUdunits("siemens per meter",
                                        "S.m-1",            "S m-1",            "S.m-1");
testToUcumToUdunits("sigma",            "{sigma}",          "sigma",            "{sigma}");
testToUcumToUdunits("sigma_level",      "{sigma_level}",    "sigma_level",      "{sigma_level}");
testToUcumToUdunits("site",             "{site}",           "site",             "{site}");
testToUcumToUdunits("square centimeters (cm^2)",
                                        "cm2",              "cm2",              "cm2");
testToUcumToUdunits("square kilometers",
                                        "km2",              "km2",              "km2");
testToUcumToUdunits("sr^-1",            "sr-1",             "sr-1",             "sr-1");
testToUcumToUdunits("Standardized Units of Relative Dry and Wet",
                    "{Standardized Units of Relative Dry and Wet}",
                    "Standardized Units of Relative Dry and Wet",
                    "{Standardized Units of Relative Dry and Wet}");
testToUcumToUdunits("ton",              "[ston_av]",        "ton",              "[ston_av]");
testToUcumToUdunits("Total Scale",      "{Total Scale}",    "Total Scale",      "{Total Scale}");
testToUcumToUdunits("two digit day",    "d",                "day",              "d");
testToUcumToUdunits("two digit month",  "mo",               "month",            "mo");
testToUcumToUdunits("uatm",             "uatm",             "uatm",             "uatm");
testToUcumToUdunits("uE/m<sup>2</sup>/sec",
                                        "ueinstein.m-2.s-1","ueinstein m-2 s-1","ueinstein.m-2.s-1");
testToUcumToUdunits("ug L-1",           "ug.l-1",           "ug l-1",           "ug.l-1");
testToUcumToUdunits("ug/m3",            "ug.m-3",           "ug m-3",           "ug.m-3");
testToUcumToUdunits("um^3",             "um3",              "um3",              "um3");
testToUcumToUdunits("umol/kg",          "umol.kg-1",        "umol kg-1",        "umol.kg-1");
testToUcumToUdunits("Unitless",         "1",                "1",                "1");
testToUcumToUdunits("uS/cm",            "uS.cm-1",          "uS cm-1",          "uS.cm-1");
testToUcumToUdunits("varies by taxon/group (see description)",
                    "{varies by taxon/group (see description)}",
                    "varies by taxon/group (see description)",
                    "{varies by taxon/group (see description)}");
testToUcumToUdunits("volt",             "V",                "V",                "V");
testToUcumToUdunits("volts",            "V",                "V",                "V");
testToUcumToUdunits("volts (0-5 FSO)",  "V",                "V",                "V");
testToUcumToUdunits("W m-2",            "W.m-2",            "W m-2",            "W.m-2");
testToUcumToUdunits("w m-2",            "W.m-2",            "W m-2",            "W.m-2");
testToUcumToUdunits("W m^-2 um^-1 sr^-1",
                                        "W.m-2.um-1.sr-1",  "W m-2 um-1 sr-1",  "W.m-2.um-1.sr-1");
testToUcumToUdunits("W/M**2",           "W.m-2",            "W m-2",            "W.m-2");
testToUcumToUdunits("w/m2",             "W.m-2",            "W m-2",            "W.m-2");
testToUcumToUdunits("W/m^2",            "W.m-2",            "W m-2",            "W.m-2");
testToUcumToUdunits("watt m-2",         "W.m-2",            "W m-2",            "W.m-2");
testToUcumToUdunits("watt meter-2",     "W.m-2",            "W m-2",            "W.m-2");
testToUcumToUdunits("watt per meter^2", "W.m-2",            "W m-2",            "W.m-2");
testToUcumToUdunits("watts/meters<sup>2</sup>",
                                        "W.m-2",            "W m-2",            "W.m-2");
testToUcumToUdunits("year",             "a",                "year",             "a");
testToUcumToUdunits("years",            "a",                "year",             "a");
testToUcumToUdunits("years since 0000-01-01",
                    "a{since 0000-01-01}",
                    "years since 0000-01-01",
                    "a{since 0000-01-01}");
testToUcumToUdunits("years since 0000-07-01",
                    "a{since 0000-07-01}",
                    "years since 0000-07-01",
                    "a{since 0000-07-01}");
testToUcumToUdunits("years since 02-JAN-1985",
                    "a{since 1985-01-02}",
                    "years since 1985-01-02",
                    "a{since 1985-01-02}");
testToUcumToUdunits("yyyy",             "{yyyy}",           "yyyy",             "{yyyy}");
testToUcumToUdunits("yyyy-MM-dd",       "{yyyy-MM-dd}",     "yyyy-MM-dd",       "{yyyy-MM-dd}");
testToUcumToUdunits("yyyy-MM-dd HH:mm", "{yyyy-MM-dd HH:mm}",
                    "yyyy-MM-dd HH:mm", "{yyyy-MM-dd HH:mm}");
testToUcumToUdunits("yyyy-MM-dd' 'HH:mm:ss",
                    "{yyyy-MM-dd' 'HH:mm:ss}",
                    "yyyy-MM-dd' 'HH:mm:ss",
                    "{yyyy-MM-dd' 'HH:mm:ss}");
testToUcumToUdunits("yyyy-MM-dd'T'HH:mm",
                    "{yyyy-MM-dd'T'HH:mm}",
                    "yyyy-MM-dd'T'HH:mm",
                    "{yyyy-MM-dd'T'HH:mm}");
testToUcumToUdunits("yyyy-MM-dd'T'HH:mm:ss",
                    "{yyyy-MM-dd'T'HH:mm:ss}",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "{yyyy-MM-dd'T'HH:mm:ss}");
testToUcumToUdunits("yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "{yyyy-MM-dd'T'HH:mm:ss.SSS}",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "{yyyy-MM-dd'T'HH:mm:ss.SSS}");
testToUcumToUdunits("yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                    "{yyyy-MM-dd'T'HH:mm:ss.SSSZ}",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                    "{yyyy-MM-dd'T'HH:mm:ss.SSSZ}");
testToUcumToUdunits("yyyy-MM-dd'T'HH:mm:ssZ",
                    "{yyyy-MM-dd'T'HH:mm:ssZ}",
                    "yyyy-MM-dd'T'HH:mm:ssZ",
                    "{yyyy-MM-dd'T'HH:mm:ssZ}");
testToUcumToUdunits("yyyy/M/d",         "{yyyy/M/d}",       "yyyy/M/d",         "{yyyy/M/d}");
testToUcumToUdunits("yyyyMMdd",         "{yyyyMMdd}",       "yyyyMMdd",         "{yyyyMMdd}");
testToUcumToUdunits("yyyyMMdd'_'HHmmss",
                    "{yyyyMMdd'_'HHmmss}",
                    "yyyyMMdd'_'HHmmss",
                    "{yyyyMMdd'_'HHmmss}");
testToUcumToUdunits("yyyyMMddHHmm",     "{yyyyMMddHHmm}",   "yyyyMMddHHmm",     "{yyyyMMddHHmm}");
testToUcumToUdunits("µmole/kg",         "umol.kg-1",        "umol kg-1",        "umol.kg-1");    

//more tests
debugMode = true;
Test.ensureEqual(udunitsToUcum("some {any nonsense}"),   "{some}.{any nonsense}",   "");
Test.ensureEqual(ucumToUdunits("{some}.{any nonsense}"), "some any nonsense",       "");
Test.ensureEqual(udunitsToUcum("some any nonsense"),     "{some}.{any}.{nonsense}", "");

Test.ensureEqual(udunitsToUcum("some (any nonsense)"),       "{some}.({any}.{nonsense})",   "");
Test.ensureEqual(ucumToUdunits("{some}.({any}.{nonsense})"), "some (any nonsense)",       "");

testToUcumToUdunits("some unrelated24 con33tent, really (a fact)",
                    "{some}.{unrelated}24.{con33tent},.{really}.(a.{fact})",  
                    "some unrelated24 con33tent, really (year fact)",  
                    "{some}.{unrelated}24.{con33tent},.{really}.(a.{fact})");    

//test of <ucumToUdunits> and <udunitsToUcum> in messages.xml
Test.ensureEqual(ucumToUdunits("deg north"),      "degrees_north", ""); //this also tests that last triplet is read
Test.ensureEqual(udunitsToUcum("degrees north"),  "deg{north}",    ""); //this also tests that last triplet is read 

        Test.ensureEqual(safeStandardizeUdunits("percent"),    "%", ""); 
        Test.ensureEqual(safeStandardizeUdunits("percentage"), "%", ""); 
        Test.ensureEqual(safeStandardizeUdunits("%"),          "%", "");  

debugMode = false;
}

    /**
     * This runs all of the interactive or not interactive tests for this class.
     *
     * @param errorSB all caught exceptions are logged to this.
     * @param interactive  If true, this runs all of the interactive tests; 
     *   otherwise, this runs all of the non-interactive tests.
     * @param doSlowTestsToo If true, this runs the slow tests, too.
     * @param firstTest The first test to be run (0...).  Test numbers may change.
     * @param lastTest The last test to be run, inclusive (0..., or -1 for the last test). 
     *   Test numbers may change.
     */
    public static void test(StringBuilder errorSB, boolean interactive, 
        boolean doSlowTestsToo, int firstTest, int lastTest) {
        if (lastTest < 0)
            lastTest = interactive? -1 : 2;
        String msg = "\n^^^ Units2.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0) testUdunitsToUcum();
                    if (test ==  1) testUcumToUdunits();
                    if (test ==  2) testAllToUcumToUdnits();

                    //not normally run:
                    if (test ==  1000) generateTests();
                    //tool for development and testing:
                    if (test ==  1001) repeatedlyTestOneUdunit();

                }

                String2.log(msg + test + " finished successfully in " + (System.currentTimeMillis() - time) + " ms.");
            } catch (Throwable testThrowable) {
                String eMsg = msg + test + " caught throwable:\n" + 
                    MustBe.throwableToString(testThrowable);
                errorSB.append(eMsg);
                String2.log(eMsg);
                if (interactive) 
                    String2.pressEnterToContinue("");
            }
        }
    }


}
