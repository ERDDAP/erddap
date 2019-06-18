/* This file is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.util;

import com.cohort.array.StringComparatorIgnoreCase;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.Vector;
import java.util.WeakHashMap;

import org.apache.commons.codec.binary.Base64;

/**
 * A class with static String methods that add to native String methods.
 * All are static methods. 
 */
public class String2 {

    /** the source code version number. (Obviously, this is not really used.) */
    public static final double version = 1.000;

    /**
     * ERROR is a constant so that it will be consistent, so that one can 
     * search for it in output files.
     * This is NOT final, so EDStatic can change it.
     * This is the original definition, referenced by many other classes.
     */
    public static String ERROR = "ERROR";
    public static String WARNING = "WARNING";

    //public static Logger log = Logger.getLogger("com.cohort.util");
    private static boolean logToSystemOut = true;
    private static boolean logToSystemErr = false;
    public final static StringBuilder logFileLock = new StringBuilder(); //synchronize all logFile use on this 
    private static BufferedWriter logFile;
    private static String logFileName;
    /** logFileMaxSize determines when it's time to make a new logFile */
    private static int logFileSize = 0;
    public  final static int logFileDefaultMaxSize = 20000000; //20MB
    public  static int logFileMaxSize = logFileDefaultMaxSize;

    /**
     * This returns the line separator from
     *  <code>System.getProperty("line.separator");</code>
     */
    public static String lineSeparator = System.getProperty("line.separator");

//    public final static String  CHARSET = "charset";       //the name  of the charset att
    public final static String  ISO_8859_1 = "ISO-8859-1"; //the value of the charset att and usable in Java code
    public final static String  ISO_8859_1_LC = ISO_8859_1.toLowerCase();
    public final static Charset ISO_8859_1_CHARSET = Charset.forName(ISO_8859_1);
    public final static String ENCODING = "_Encoding";    //the name  of the _Encoding att
    public final static String UTF_8 = "UTF-8";           //a   value of the _Encoding att and usable in Java code
    public final static String UTF_8_LC = UTF_8.toLowerCase();
    public final static String JSON = "JSON";          

    /** Returns true if the current Operating System is Windows. */
    public static String OSName = System.getProperty("os.name");
    public static boolean OSIsWindows = OSName.toLowerCase().indexOf("windows") >= 0;
    /** Returns true if the current Operating System is Linux. */
    public static boolean OSIsLinux = OSName.toLowerCase().indexOf("linux") >= 0;
    /** Returns true if the current Operating System is Mac OS X. 
        2014-01-09 was System.getProperty("mrj.version") != null
        https://developer.apple.com/library/mac/technotes/tn2002/tn2110.html        */
    public static boolean OSIsMacOSX = OSName.contains("OS X");
    public final static String  AWS_S3_REGEX = "https?://(\\w*)\\.s3\\.amazonaws\\.com/(.*)";
    /** If testing a "dir", url should have a trailing slash.
        Patterns are thread-safe. */
    public final static Pattern AWS_S3_PATTERN = Pattern.compile(AWS_S3_REGEX);
    /** 
     * email regex used to identify likely email addresses.
     * This is intended to accept most common valid addresses and reject most invalid addresses.
     * Modified from http://www.regular-expressions.info/email.html 
     * (was "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}" )
     * (with a-z added instead of using case-insensitive regex)
     * and http://emailregex.com/ 
     *   (wow! Mine is far simpler and more restrictive. Don't want too many false positives.)
     * and https://en.wikipedia.org/wiki/Email_address
     * This isn't perfect, but it is probably good enough.
     */
    public final static String EMAIL_REGEX = // \\p{L} is any Unicode letter
        "\\p{L}[\\p{L}0-9'._%+-]{0,127}@[\\p{L}0-9.-]{1,127}\\.[A-Za-z]{2,4}";
    public final static Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);


    public final static String ACDD_CONTACT_TYPES[] = {"person", "group", "institution", "position"}; //ACDD 1.3

    //in the order they are used...
    public final static String ACDD_PERSON_REGEX1 = "(DHYRENBACH|JFPIOLLE|JLH|ZHIJIN)"; 
    public final static String ACDD_GROUP_REGEX = 
        ".*(Center|CMEMS|GHRSST|Group|ICOADS|MEaSUREs|OBPG|Office|project|Project|SSALTO|Team).*";
    public final static String ACDD_INSTITUTION_REGEX1 = 
        ".*(Environment Canada|Remote Sensing Systems|University).*";
    public final static String ACDD_INSTITUTION_REGEX2 = ".*[A-Z]{3,}.*";  //3 or more adjacent capital letters anywhere
    public final static String ACDD_INSTITUTION_REGEX3 = "([A-Za-z][a-z]+ ){3,}[A-Za-z][a-z]+.*";  //start with 4 or more words (including "of")
    /** Catch most (not all) people's names, 
        e.g., Dr. Kenneth R. Jones, Ken R. Jones, Ken R Jones, Ken Jones, K Jones, Mary McCarthy       
        Don't match other things (e.g., institutions with 3 word names). 
        See tests in TestUtil.testString2. */
    public final static String ACDD_PERSON_REGEX2 = 
        "(Dr\\.? |Prof\\.? |)[A-Z](\\.|[a-z]*) ([A-Z]\\.? |)(Ma?c|)[A-Z][a-z]+(, .+|)"; 

    public final static String NCCSV_VERSION        = "NCCSV-1.0";
    public final static String NCCSV_BINARY_VERSION = "NCCSV_BINARY-1.0";
    public final static String NCCSV_GLOBAL         = "*GLOBAL*";
    public final static String NCCSV_DATATYPE       = "*DATA_TYPE*";
    public final static String NCCSV_SCALAR         = "*SCALAR*";
    public final static String NCCSV_END_METADATA   = "*END_METADATA*";
    public final static String NCCSV_END_DATA       = "*END_DATA*";

    public final static Pattern NCCSV_BYTE_ATT_PATTERN   = Pattern.compile("-?\\d{1,3}b");
    public final static Pattern NCCSV_SHORT_ATT_PATTERN  = Pattern.compile("-?\\d{1,5}s");
    public final static Pattern NCCSV_INT_ATT_PATTERN    = Pattern.compile("-?\\d{1,10}i");
    public final static Pattern NCCSV_LONG_ATT_PATTERN   = Pattern.compile("-?\\d{1,19}L");
    public final static Pattern NCCSV_FLOAT_ATT_PATTERN  = Pattern.compile(
        // -(    1      .? 1?      |  .1        )  e    -      10    |NaN f
        "(-?(\\d{1,15}\\.?\\d{0,15}|\\.\\d{1,15})([eE][+-]?\\d{1,2})?|NaN)f");
    public final static Pattern NCCSV_DOUBLE_ATT_PATTERN = Pattern.compile(
        // -(    1      .? 1?      |  .1        )  e   +-      100   |NaN  d  
        "(-?(\\d{1,25}\\.?\\d{0,25}|\\.\\d{1,25})([eE][+-]?\\d{1,3})?|NaN)d");
    public final static Pattern NCCSV_CHAR_ATT_PATTERN   = Pattern.compile(
        //  ' char    |  \special              | "" | \uffff             '
        "\"?'([ -~^\"]|\\\\[bfnrt/\\\'\\\"\\\\]|\"\"|\\\\u[0-9a-fA-F]{4})'\"?"); 

    /* This is to test: does an NCSV string generated by Java Look Like A (LLA) number? */
    public final static Pattern NCCSV_LLA_NUMBER_PATTERN = Pattern.compile(
        "(-?\\d[0-9.eE+-]*|NaN)(b|s|L|f|)"); //Java always writes a leading digit, e.g., 0.1, not .1

    /** These are NOT thread-safe.  Always use them in synchronized blocks ("synchronized(gen....) {}").*/
    private static DecimalFormat genStdFormat6 = new DecimalFormat("0.######");
    private static DecimalFormat genEngFormat6 = new DecimalFormat("##0.#####E0");
    private static DecimalFormat genExpFormat6 = new DecimalFormat("0.######E0");
    private static DecimalFormat genStdFormat10 = new DecimalFormat("0.##########");
    private static DecimalFormat genEngFormat10 = new DecimalFormat("##0.#########E0");
    private static DecimalFormat genExpFormat10 = new DecimalFormat("0.##########E0");

    private static String classPath; //lazy creation by getClassPath

    //splitting canonicalMap into 4 maps allows each to be bigger
    //and makes synchronized contention less common.
    private static Map canonicalMap[] = new Map[6];
    static {
        for (int i = 0; i < canonicalMap.length; i++)
            canonicalMap[i] = new WeakHashMap();
    }

    //EDStatic may change this
    public static String unitTestDataDir    = "/erddapTest/";
    public static String unitTestBigDataDir = "/erddapTestBig/";

    /**
     * This returns the string which sorts higher.
     * null sorts low.
     *
     * @param s1
     * @param s2
     * @return the string which sorts higher.
     */
    public static String max(String s1, String s2) {
        if (s1 == null)
            return s2;
        if (s2 == null)
            return s1;
        return s1.compareTo(s2) >= 0? s1 : s2;
    }

    /**
     * This returns the string which sorts lower.
     * null sorts low.
     *
     * @param s1
     * @param s2
     * @return the string which sorts lower.
     */
    public static String min(String s1, String s2) {
        if (s1 == null)
            return s1;
        if (s2 == null)
            return s2;
        return s1.compareTo(s2) < 0? s1 : s2;
    }

    /**
     * This makes a new String of specified length, filled with ch.
     * For safety, if length&gt;=1000000, it returns "".
     * 
     * @param ch the character to fill the string
     * @param length the length of the string
     * @return a String 'length' long, filled with ch.
     *    If length &lt; 0 or &gt;= 1000000, this returns "".
     */
    public static String makeString(char ch, int length) {
        if ((length < 0) || (length >= 1000000))
            return "";

        char[] car = new char[length];
        java.util.Arrays.fill(car, ch);
        return new String(car);
    }

    /**
     * Returns a String 'length' long, with 's' right-justified  
     * (using spaces as the added characters) within the resulting String.
     * If s is already longer, then there will be no change.
     * 
     * @param s is the string to be right-justified.
     * @param length is desired length of the resulting string.
     * @return 's' right-justified to make the result 'length' long.
     */
    public static String right(String s, int length) {
        int toAdd = length - s.length();

        if (toAdd <= 0)
            return s;
        else
            return makeString(' ', toAdd).concat(s);
    }

    /**
     * Returns a String 'length' long, with 's' left-justified  
     * (using spaces as the added characters) within the resulting String.  
     * If s is already longer, then there will be no change.
     * 
     * @param s is the string to be left-justified.
     * @param length is desired length of the resulting string.
     * @return 's' left-justified to make the result 'length' long.
     */
    public static String left(String s, int length) {
        int toAdd = length - s.length();

        if (toAdd <= 0)
            return s;
        else
            return s.concat(makeString(' ', toAdd));
    }

    /**
     * Returns a String 'length' long, with 's' centered  
     * (using spaces as the added characters) within the resulting String.  
     * If s is already longer, then there will be no change.
     * 
     * @param s is the string to be centered.
     * @param length is desired length of the resulting string.
     * @return 's' centered to make the result 'length' long.
     */
    public static String center(String s, int length) {
        int toAdd = length - s.length();

        if (toAdd <= 0)
            return s;
        else
            return makeString(' ', toAdd / 2) + s
            + makeString(' ', toAdd - (toAdd / 2));
    }

    /**
     * This returns a string no more than max characters long, throwing away the excess.
     * If you want to keep the whole string and just insert newlines periodically, 
     * use noLongLines() instead.
     *
     * @param s
     * @param max
     * @return s (if it is short) or the first max characters of s.
     *   If s==null, this returns "".
     */
    public static String noLongerThan(String s, int max) {
        if (s == null)
            return "";
        if (s.length() <= max)  
            return s;
        return s.substring(0, Math.max(0, max));
    }

    /**
     * This is like noLongerThan, but if truncated, s.substring(0, max-3) + "..." is returned.
     *
     * @param s
     * @param max
     * @return s (if it is short) or the first max characters of s
     */
    public static String noLongerThanDots(String s, int max) {
        if (s == null)
            return "";
        if (s.length() <= max)  
            return s;
        return s.substring(0, Math.max(0, max-3)) + "...";
    }

    /**
     * This converts non-isPrintable characters to "[#]".
     * \\n generates both [10] and a newline character.
     *
     * @param s the string
     * @return s, but with non-32..126 characters replaced by [#].
     *    The result ends with "[end]".
     *    null returns "[null][end]".
     */
    public static String annotatedString(String s) {
        if (s == null) 
            return "[null][end]";
        int sLength = s.length();
        StringBuilder buffer = new StringBuilder(sLength / 5 * 6);

        for (int i = 0; i < sLength; i++) {
            char ch = s.charAt(i);

            if (ch >= 32 && ch <= 126) {
                buffer.append(ch);
            } else {
                buffer.append("[" + ((int) ch) + "]");  //safe char to int type conversion
                if (ch == '\n') 
                    buffer.append('\n');
            }
        }

        buffer.append("[end]");
        return buffer.toString();
    }


    /**
     * This determines the number of initial characters that match.
     * 
     * @param s1
     * @param s2
     * @return the number of characters that are the same at the start
     *   of both strings.
     */
    public static int getNMatchingCharacters(String s1, String s2) {
        int minLength = Math.min(s1.length(), s2.length());
        for (int i = 0; i < minLength; i++)
            if (s1.charAt(i) != s2.charAt(i))
                return i;
        return minLength;
    }
   
    /**
     * Finds the first instance of 'find' at or after fromIndex (0..), ignoring case.
     *
     * @param s
     * @param find
     * @return the first instance of 'find' at or after fromIndex (0..), ignoring case.
     */
    public static int indexOfIgnoreCase(String s, String find) {
        return indexOfIgnoreCase(s, find, 0);
    }

    /**
     * Finds the first instance of 'find' at or after fromIndex (0..), ignoring case.
     *
     * @param s
     * @param find
     * @param fromIndex
     * @return the first instance of 'find' at or after fromIndex (0..), ignoring case.
     */
    public static int indexOfIgnoreCase(String s, String find, int fromIndex) {
        if (s == null) 
            return -1;
        int sLength = s.length();
        if (sLength == 0)
            return -1;
        find = find.toLowerCase();
        int findLength = find.length();
        if (findLength == 0)
            return fromIndex;
        int maxPo = sLength - findLength;
        char ch0 = find.charAt(0);

        int po = fromIndex;
        while (po <= maxPo) {
            if (Character.toLowerCase(s.charAt(po)) == ch0) {
                int f2 = 1;
                while (f2 < findLength && 
                    Character.toLowerCase(s.charAt(po + f2)) == find.charAt(f2))
                    f2++;
                if (f2 == findLength)
                    return po;
            }
            po++;
        }
        return -1;
    }

    /**
     * This goes beyond indexOfIgnoreCase by looking after punctuation removed.
     *
     * @param s
     * @param find
     * @return true if find is loosely in s. Return false if s or find !isSomething.
     */
    public static boolean looselyContains(String s, String find) {
        if (s == null || find == null) 
            return false;

        int sLength = s.length();
        StringBuilder ssb = new StringBuilder();
        for (int i = 0; i < sLength; i++) {
            char ch = s.charAt(i);
            if (Character.isLetterOrDigit(ch))
                ssb.append(Character.toLowerCase(ch));
        }
        if (ssb.length() == 0)
            return false;

        int fLength = find.length();
        StringBuilder fsb = new StringBuilder();
        for (int i = 0; i < fLength; i++) {
            char ch = find.charAt(i);
            if (Character.isLetterOrDigit(ch))
                fsb.append(Character.toLowerCase(ch));
        }
        if (fsb.length() == 0)
            return false;

        return ssb.indexOf(fsb.toString()) >= 0;
    }

    /**
     * This goes beyond equalsIgnoreCase by looking after punctuation removed.
     *
     * @param s1
     * @param s2
     * @return true if find is loosely in s. Return false if s or find !isSomething.
     */
    public static boolean looselyEquals(String s1, String s2) {
        if (s1 == null || s2 == null) 
            return false;

        int s1Length = s1.length();
        StringBuilder s1sb = new StringBuilder();
        for (int i = 0; i < s1Length; i++) {
            char ch = s1.charAt(i);
            if (Character.isLetterOrDigit(ch))
                s1sb.append(Character.toLowerCase(ch));
        }
        if (s1sb.length() == 0)
            return false;

        int s2Length = s2.length();
        StringBuilder s2sb = new StringBuilder();
        for (int i = 0; i < s2Length; i++) {
            char ch = s2.charAt(i);
            if (Character.isLetterOrDigit(ch))
                s2sb.append(Character.toLowerCase(ch));
        }
        if (s2sb.length() == 0)
            return false;

        return s1sb.toString().equals(s2sb.toString());
    }

    /**
     * Finds the first instance of s at or after fromIndex (0.. ) in sb.
     *
     * @param sb a StringBuilder
     * @param s the String you want to find
     * @param fromIndex the index number of the position to start the search
     * @return The starting position of s. If s is null or not found, it returns -1.
     */
    public static int indexOf(StringBuilder sb, String s, int fromIndex) {
        if (s == null) 
            return -1;
        int sLength = s.length();
        if (sLength == 0)
            return -1;

        char ch = s.charAt(0);
        int index = Math.max(fromIndex, 0);
        int tSize = sb.length() - sLength + 1; //no point in searching last few char
        while (index < tSize) {
            if (sb.charAt(index) == ch) {
                int nCharsMatched = 1;
                while ((nCharsMatched < sLength)
                        && (sb.charAt(index + nCharsMatched) == s.charAt(nCharsMatched)))
                    nCharsMatched++;
                if (nCharsMatched == sLength)
                    return index;
            }

            index++;
        }

        return -1;
    }

    /**
     * This finds (case-sensitive) the first whole word instance of 'word' in s.
     * It will find 'word' at the start or end of s.
     * I.e., the character before and after (if any) mustn't be a letter or digit or '_'.
     *
     * @param word must be a simple word (without regex special characters)
     * @return -1 if not found (or trouble, e.g., find=null)
     */
    public static int findWholeWord(String s, String word) {
        if (s == null || s.length() == 0 ||
            word == null || word.length() == 0)
            return -1;

        if (s.equals(word) || 
            s.matches(word + "\\b.*"))
            return 0;
        if (s.matches(".*\\b" + word))
            return s.length() - word.length();
        Pattern pattern = Pattern.compile("\\b(" + word + ")\\b");
        Matcher matcher = pattern.matcher(s);
        return matcher.find()? matcher.start(1) : -1;
    }

    /** 
     * This creates a hashset of the unique acronyms in a string.
     * An acronym here is defined by the regular expression:
     * [^a-zA-Z0-9][A-Z]{2,}[^a-zA-Z0-9]
     *
     * @param text
     * @return hashset of the unique acronyms in text.
     */
    public static HashSet<String> findAcronyms(String text) {
        HashSet<String> hs = new HashSet();
        if (text == null || text.length() < 2)
            return hs;
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9]([A-Z]{2,})[^a-zA-Z0-9]");
        Matcher matcher = pattern.matcher(text);
        int po = 0;
        while (po < text.length()) {
            if (matcher.find(po)) {
                hs.add(matcher.group(1));
                po = matcher.end();
            } else {
                return hs;
            }
        }
        return hs;
    }


    /**
     * This returns the first section of s (starting at fromIndex) 
     * which matches regex.
     * !!! Note that . in the regex doesn't match line terminators in s !!!
     *
     * @param s the source String.
     * @param regex the regular expression, see java.util.regex.Pattern.
     * @param fromIndex the starting index in s
     * @return the section of s which matches regex, or null if not found
     * @throws RuntimeException if trouble
     */
    public static String extractRegex(String s, String regex, int fromIndex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(s);
        if (m.find(fromIndex)) 
            return s.substring(m.start(), m.end());
        return null; 
    }

    /**
     * This returns all the sections of s that match regex.
     * It assumes that the extracted parts don't overlap.
     * !!! Note that . in the regex doesn't match line terminators in s !!!
     *
     * @param s the source String
     * @param regex the regular expression, see java.util.regex.Pattern.
     *    Note that you often want to use the "reluctant" qualifiers
     *    which match as few chars as possible (e.g., ??, *?, +?)
     *    not the "greedy"  qualifiers
     *    which match as many chars as possible (e.g., ?, *, +).
     * @return a String[] with all the matching sections of s (or String[0] if none)
     * @throws RuntimeException if trouble
     */
    public static String[] extractAllRegexes(String s, String regex) {
        ArrayList<String> al = new ArrayList();
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(s);
        int fromIndex = 0;
        while (m.find(fromIndex)) {
            al.add(s.substring(m.start(), m.end()));
            fromIndex = m.end();
        }
        return al.toArray(new String[0]);
    }

    /**
     * This returns the specified capture group from s. 
     *
     * @param s the source String
     * @param regex the regular expression, see java.util.regex.Pattern.
     * @param captureGroupNumber the number of the capture group (0 for entire regex,
     *    1 for first capture group, 2 for second, etc.)
     * @return the value of the specified capture group,
          or null if the s doesn't match the regex
     * @throws RuntimeException if trouble, e.g., invalid regex syntax
     */
    public static String extractCaptureGroup(String s, String regex, int captureGroupNumber) {
        return extractCaptureGroup(s, Pattern.compile(regex), captureGroupNumber);
    }

    /**
     * This returns the specified capture group from s. 
     *
     * @param s the source String
     * @param regexPattern the regexPattern must match the entire string
     * @param captureGroupNumber the number of the capture group (0 for entire regex,
     *    1 for first capture group, 2 for second, etc.)
     * @return the value of the specified capture group,
          or null if the s doesn't match the regex
     * @throws RuntimeException if trouble, e.g., invalid regex syntax
     */
    public static String extractCaptureGroup(String s, Pattern regexPattern, int captureGroupNumber) {
        Matcher m = regexPattern.matcher(s);
        if (m.matches()) 
            return m.group(captureGroupNumber);
        else return null; 
    }

    /**
     * Finds the first instance of i at or after fromIndex (0.. ) in iArray.
     *
     * @param iArray
     * @param i the int you want to find
     * @param fromIndex the index number of the position to start the search
     * @return The first instance of i. If not found, it returns -1.
     */
    public static int indexOf(int[] iArray, int i, int fromIndex) {
        int iArrayLength = iArray.length;
        for (int index = Math.max(fromIndex, 0); index < iArrayLength; index++) {
            if (iArray[index] == i) 
                return index;
        }
        return -1;
    }

    /**
     * Finds the first instance of i in iArray.
     *
     * @param iArray
     * @param i the int you want to find
     * @return The first instance of i. If not found, it returns -1.
     */
    public static int indexOf(int[] iArray, int i) {
        return indexOf(iArray, i, 0);
    }

    /**
     * Finds the first instance of c at or after fromIndex (0.. ) in cArray.
     *
     * @param cArray
     * @param c the char you want to find
     * @param fromIndex the index number of the position to start the search
     * @return The first instance of c. If not found, it returns -1.
     */
    public static int indexOf(char[] cArray, char c, int fromIndex) {
        int cArrayLength = cArray.length;
        for (int index = Math.max(fromIndex, 0); index < cArrayLength; index++) {
            if (cArray[index] == c) 
                return index;
        }
        return -1;
    }

    /**
     * Finds the first instance of c in cArray.
     *
     * @param cArray
     * @param c the char you want to find
     * @return The first instance of c. If not found, it returns -1.
     */
    public static int indexOf(char[] cArray, char c) {
        return indexOf(cArray, c, 0);
    }

    /**
     * This indexOf is a little different: it finds the first instance in s of any char in car.
     *
     * @param s a string 
     * @param car the chars you want to find any of 
     * @param fromIndex the index number of the position to start the search
     * @return The first instance in s of any char in car. If not found, it returns -1.
     */
    public static int indexOf(String s, char[] car, int fromIndex) {
        int sLength = s.length();
        for (int index = Math.max(fromIndex, 0); index < sLength; index++) {
            if (indexOf(car, s.charAt(index)) >= 0)
                return index;
        }
        return -1;
    }
    
    /**
     * This indexOf is a little different: it finds the first instance in s of any char in car.
     *
     * @param s a string 
     * @param car the chars you want to find any of 
     * @param fromIndex the index number of the position to start the search
     * @return The first instance in s of any char in car. If not found, it returns -1.
     */
    public static int indexOf(String s, String car, int fromIndex) {
        int sLength = s.length();
        for (int index = Math.max(fromIndex, 0); index < sLength; index++) {
            if (car.indexOf(s.charAt(index)) >= 0)
                return index;
        }
        return -1;
    }
    


    /**
     * Finds the first instance of d at or after fromIndex (0.. ) in dArray
     * (tested with Math2.almostEqual5).
     *
     * @param dArray
     * @param d the double you want to find
     * @param fromIndex the index number of the position to start the search
     * @return The first instance of d. If not found, it returns -1.
     */
    public static int indexOf(double[] dArray, double d, int fromIndex) {
        int dArrayLength = dArray.length;
        for (int index = Math.max(fromIndex, 0); index < dArrayLength; index++) {
            if (Math2.almostEqual(5, dArray[index], d)) 
                return index;
        }
        return -1;
    }

    /** 
     * This tries to find the first one of the words in the longerString.
     * This is case-sensitive.
     *
     * @return index of the matching word (or -1 if no match or other trouble)
     */
    public static int whichWord(String longerString, String words[]) {
        if (longerString == null || longerString.length() == 0 || words == null)
            return -1;
        int n = words.length;
        for (int i = 0; i < n; i++) 
            if (longerString.indexOf(words[i]) >= 0)
                return i;
        return -1;
    }

    /**
     * Finds the first instance of d in dArray
     * (tested with Math2.almostEqual5).
     *
     * @param dArray
     * @param d the double you want to find
     * @return The first instance of d. If not found, it returns -1.
     */
    public static int indexOf(double[] dArray, double d) {
        return indexOf(dArray, d, 0);
    }

    /** This calls directReadFromFile with charset= UTF_8 */
    public static String directReadFromUtf8File(String fileName) 
        throws Exception {
        return directReadFromFile(fileName, UTF_8);
    }

    /** This calls directReadFromFile with charset= ISO_8859_1 */
    public static String directReadFrom88591File(String fileName) 
        throws Exception {
        return directReadFromFile(fileName, ISO_8859_1);
    }

    /**
     * This reads the bytes of the file with the specified charset.
     * This does not alter the characters (e.g., the line endings).
     * 
     * <P>This method is generally appropriate for small and medium-sized
     * files. For very large files or files that need additional processing,
     * it may be better to write a custom method to
     * read the file line-by-line, processing as it goes.
     *
     * @param fileName is the (usually canonical) path (dir+name) for the file
     * @param charset e.g., ISO-8859-1, UTF-8, or "" or null for the default (ISO-8859-1)
     * @return a String with the decoded contents of the file.
     * @throws Exception if trouble
     */
    public static String directReadFromFile(String fileName, String charset) 
        throws Exception {

        //declare the BufferedReader variable
        //declare the results variable: String results[] = {"", ""}; 
        //BufferedReader and results are declared outside try/catch so 
        //that they can be accessed from within either try/catch block.
        long time = System.currentTimeMillis();
        InputStream fis = null;
        Reader isr = null;
        StringBuilder sb = new StringBuilder(8192);
        try {

            fis = File2.getDecompressedBufferedInputStream(fileName);
            isr = new BufferedReader(new InputStreamReader(fis, 
                charset == null || charset.length() == 0? ISO_8859_1 : charset));

            //get the text from the file
            char buffer[] = new char[8192];
            int nRead;
            while ((nRead = isr.read(buffer)) >= 0)  //-1 = end-of-file
                sb.append(buffer, 0, nRead);
            return sb.toString();
        } finally {
            try {
                if (isr != null)
                    isr.close();
                else if (fis != null)
                    fis.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * This is a variant of readFromFile that uses the default character set 
     * and 3 tries (1 second apart) to read the file. 
     */
    public static String[] readFromFile(String fileName) {
        return readFromFile(fileName, null, 3);
    }

    /**
     * This is a variant of readFromFile that uses the specified character set
     * and 3 tries (1 second apart) to read the file.
     */
    public static String[] readFromFile(String fileName, String charset) {
        return readFromFile(fileName, charset, 3);
    }

    /**
     * This reads the text contents of the specified text file.
     * 
     * <P>This method uses try/catch to ensure that all possible
     * exceptions are caught and returned as the error String
     * (throwable.toString()).
     * 
     * <P>This method is generally appropriate for small and medium-sized
     * files. For very large files or files that need additional processing,
     * it may be better to write a custom method to
     * read the file line-by-line, processing as it goes.
     *
     * @param fileName is the (usually canonical) path (dir+name) for the file
     * @param charset e.g., ISO-8859-1, UTF-8, or "" or null for the default (ISO-8859-1)
     * @param maxAttempt e.g. 3   (the tries are 1 second apart)
     * @return a String array with two strings.
     *     Using a String array gets around Java's limitation of
     *         only returning one value from a method.
     *     String #0 is an error String (or "" if no error).
     *     String #1 has the contents of the file as one big string
     *         (with any end-of-line characters converted to \n).
     *     If the error String is not "", String #1
     *         may not have all the contents of the file.
     *     ***This ensures that the last character in the file (if any) is \n.
     *     This behavior varies from other implementations of readFromFile.
     */
    public static String[] readFromFile(String fileName, String charset, int maxAttempt) {

        //declare the BufferedReader variable
        //declare the results variable: String results[] = {"", ""}; 
        //BufferedReader and results are declared outside try/catch so 
        //that they can be accessed from within either try/catch block.
        long time = System.currentTimeMillis();
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader bufferedReader = null;
        String results[] = {"", ""};
        int errorIndex = 0;
        int contentsIndex = 1;

        try {
            //open the file
            //To deal with problems in multithreaded apps 
            //(when deleting and renaming files, for an instant no file with that name exists),
            maxAttempt = Math.max(1, maxAttempt);
            for (int attempt = 1; attempt <= maxAttempt; attempt++) {
                try {
                    is = File2.getDecompressedBufferedInputStream(fileName);
                    isr = new InputStreamReader(is, 
                        charset == null || charset.length() == 0? ISO_8859_1 : charset);
                } catch (Exception e) {
                    if (attempt == maxAttempt) {
                        log(ERROR + ": String2.readFromFile was unable to read " + fileName);
                        throw e;
                    } else {
                        log("WARNING #" + attempt + 
                            ": String2.readFromFile is having trouble. It will try again to read " + 
                            fileName);
                        if (attempt == 1) Math2.gcAndWait(); //trouble! Give OS/Java a time and gc to deal with trouble
                        else Math2.sleep(1000);
                    }
                }
            }
            bufferedReader = new BufferedReader(isr);
                         
            //get the text from the file
            //This uses bufferedReader.readLine() to repeatedly
            //read lines from the file and thus can handle various 
            //end-of-line characters.
            //The lines (with \n added at the end) are added to a 
            //StringBuilder.
            StringBuilder sb = new StringBuilder(8192);
            String s = bufferedReader.readLine();
            while (s != null) { //null = end-of-file
                sb.append(s);
                sb.append('\n');
                s = bufferedReader.readLine();
            }

            //save the contents as results[1]
            results[contentsIndex] = sb.toString();

        } catch (Exception e) {
            results[errorIndex] = MustBe.throwable("fileName=" + fileName, e);
        }

        //close whatever got opened
        try {
            //close the highest level file object available
            if (bufferedReader != null) bufferedReader.close();
            else if (isr       != null) isr.close();
            else if (is        != null) is.close();

        } catch (Exception e) {
            if (results[errorIndex].length() == 0)
                results[errorIndex] = e.toString(); 
            //else ignore the error (the first one is more important)
        }

        //return results
        //log("  String2.readFromFile " + fileName + " time=" + 
        //    (System.currentTimeMillis() - time) + "ms");
        return results;
    }

    /**
     * This is like the other readFromFile, but returns ArrayList of Strings
     * and throws Exception is trouble.
     * The strings in the ArrayList are not canonical! So this is useful
     * for reading, processing, and throwing away.
     * 
     * <P>This method is generally appropriate for small and medium-sized
     * files. For very large files or files that need additional processing,
     * it may be more efficient to write a custom method to
     * read the file line-by-line, processing as it goes.
     *
     * @param fileName is the (usually canonical) path (dir+name) for the file
     * @param charset e.g., ISO-8859-1, UTF-8, or "" or null for the default (ISO-8859-1)
     * @param maxAttempt e.g. 3   (the tries are 1 second apart)
     * @return String[] with the lines from the file
     * @throws Exception if trouble
     */
    public static String[] readLinesFromFile(String fileName, String charset,
        int maxAttempt) throws Exception {

        long time = System.currentTimeMillis();
        InputStream is = null;
        InputStreamReader isr = null;
        try {
            for (int i = 0; i < maxAttempt; i++) {
                try {
                    is = File2.getDecompressedBufferedInputStream(fileName);
                    isr = new InputStreamReader(is, 
                        charset == null || charset.length() == 0? ISO_8859_1 : charset);
                    break; //success
                } catch (RuntimeException e) {
                    if (is != null) {
                        is.close();
                        is = null;
                    }
                    if (i == maxAttempt - 1)
                        throw e;
                    Math2.sleep(100);
                }
            }
            BufferedReader bufferedReader = new BufferedReader(isr);
            try {
                ArrayList<String> al = new ArrayList();                         
                String s = bufferedReader.readLine();
                while (s != null) { //null = end-of-file
                    al.add(s);
                    s = bufferedReader.readLine();
                }
                return al.toArray(new String[0]);
            } finally {
                bufferedReader.close();
                isr = null;
            }
        } finally {
            if (isr != null)
                isr.close();
        }
    }

    /*
    Here is a skeleton for more direct control of reading text from a file: 
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));                      
        try {
            String s;
            while ((s = bufferedReader.readLine()) != null) { //null = end-of-file
                //do something with s
                //for example, split at whitespace: String fields[] = s.split("\\s+"); //s = whitespace regex
                }

            bufferedReader.close();
        } catch (Exception e) {
            System.err.println(error + "while reading file '" + filename + "':\n" + e);
            e.printStackTrace(System.err);
            bufferedReader.close();
        }
    */

    /**
     * This saves some text in a file named fileName.
     * This uses the default character encoding.
     * 
     * <P>This method uses try/catch to ensure that all possible
     * exceptions are caught and returned as the error String
     * (throwable.toString()).
     *
     * <P>This method is generally appropriate for small and medium-sized
     * files. For very large files or files that need additional processing,
     * it may be more efficient to write a custom method to
     * read the file line-by-line, processing as it goes.
     *
     * @param fileName is the (usually canonical) path (dir+name) for the file
     * @param contents has the text that will be written to the file.
     *     contents must use \n as the end-of-line marker.
     *     Currently, this method purposely does not convert \n to the 
     *     operating-system-appropriate end-of-line characters when writing 
     *     to the file (see lineSeparator).
     * @return an error message (or "" if no error).
     */
    public static String writeToFile(String fileName, String contents) {
        return lowWriteToFile(fileName, contents, null, "\n", false);
    }

    /**
     * This is like writeToFile, but it appends the text if the file already 
     * exists. If the file doesn't exist, it makes a new file. 
     */
    public static String appendFile(String fileName, String contents) {
        return lowWriteToFile(fileName, contents, null, "\n", true);
    }

    public static String writeToFile(String fileName, String contents, String charset) {
        return lowWriteToFile(fileName, contents, charset, "\n", false);
    }

    public static String appendFile(String fileName, String contents, String charset) {
        return lowWriteToFile(fileName, contents, charset, "\n", true);
    }

    /**
     * This provides services to writeToFile and appendFile. 
     * If there is an error and !append, the partial file is deleted.
     *
     * @param fileName is the (usually canonical) path (dir+name) for the file
     * @param contents has the text that will be written to the file.
     *     contents must use \n as the end-of-line marker.
     *     Currently, this method purposely does not convert \n to the 
     *     operating-system-appropriate end-of-line characters when writing 
     *     to the file (see lineSeparator).
     * @param charset e.g., UTF-8; or null or "" for the default (ISO-8859-1 ?)
     * @param lineSeparator is the desired lineSeparator for the outgoing file.
     * @param append if you want to append any existing fileName;
     *   otherwise any existing file is deleted first.
     * @return an error message (or "" if no error).
     */
    private static String lowWriteToFile(String fileName, String contents, 
        String charset, String lineSeparator, boolean append) {
        
        //bufferedWriter and error are declared outside try/catch so 
        //that they can be accessed from within either try/catch block.
        BufferedWriter bufferedWriter = null;
        String error = "";

        try {
            //open the file
            //This uses a BufferedWriter wrapped around a FileWriter
            //to write the information to the file.
            Writer w = charset == null || charset.length() == 0?
                new FileWriter(fileName, append) :  //buffered below
                new OutputStreamWriter(
                    new BufferedOutputStream(new FileOutputStream(fileName, append)), charset);
            bufferedWriter = new BufferedWriter(w);
                         
            //convert \n to operating-system-specific lineSeparator
            if (!lineSeparator.equals("\n"))
                contents = replaceAll(contents, "\n", lineSeparator);
                //since the first String is a regex, you can use "[\\n]" too

            //write the text to the file
            bufferedWriter.write(contents);

            //test speed
            //int start = 0;
            //while (start < contents.length()) {
            //    bufferedWriter.write(contents.substring(start, Math.min(start+39, contents.length())));
            //    start += 39;
            //}

        } catch (Exception e) {
            error = e.toString();
        }

        //make sure bufferedWriter is closed
        try {
            if (bufferedWriter != null) 
                bufferedWriter.close();
        } catch (Exception e) {
            if (error.length() == 0)
                error = e.toString(); 
            //else ignore the error (the first one is more important)
        }

        //and delete partial file if error and not appending
        if (error.length() > 0 && !append)
            File2.delete(fileName);

        return error;
    }

    /**
     * A string of Java info (version, vendor).  32 bit.
     */
    public static String javaInfo() {
        String javaVersion = System.getProperty("java.version");
        String mrjVersion = System.getProperty("mrj.version");
        mrjVersion = (mrjVersion == null) ? "" : (" (mrj=" + mrjVersion + ")"); //unofficial Mac property
        return "Java " + javaVersion + mrjVersion + " (" + Math2.JavaBits + " bit, " +
            System.getProperty("java.vendor") + ") on " +
            OSName + " (" + System.getProperty("os.version") + ").";
    }

    /**
     * This returns true for A..Z, a..z.
     *
     * @param c a char
     * @return true if c is a letter
     */
    public static final boolean isAsciiLetter(int c) {
        if (c <  'A') return false;
        if (c <= 'Z') return true;
        if (c <  'a') return false;
        if (c <= 'z') return true;
        return false;
    }

    /**
     * This includes hiASCII/ISO Latin 1/ISO 8859-1, but not extensive unicode characters.
     * Letters are A..Z, a..z, and #192..#255 (except #215 and #247).
     * For unicode characters, see Java Lang Spec pg 14.
     *
     * @param c a char
     * @return true if c is a letter
     */
    public static final boolean isLetter(int c) {
        //return (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'))
        //|| ((c >= '\u00c0') && (c <= '\u00FF') && (c != '\u00d7')
        //&& (c != '\u00f7')));
        if (c <  'A') return false;
        if (c <= 'Z') return true;
        if (c <  'a') return false;
        if (c <= 'z') return true;
        if (c <  '\u00c0') return false;
        if (c == '\u00d7') return false;
        if (c <= '\u00FF') return true;
        return false;
    }

    /**
     * First letters for identifiers (e.g., variable names, method names) can be
     * all isLetter()'s plus $ and _.
     *
     * @param c a char
     * @return true if c is a valid character for the first character if a Java ID
     */
    public static final boolean isIDFirstLetter(int c) {
        if (c == '_') return true;
        if (c == '$') return true;
        return isLetter(c);
    }

    /**
     * 0..9, a..f, A..F
     * Hex numbers are 0x followed by hexDigits.
     *
     * @param c a char
     * @return true if c is a valid hex digit
     */
    public static final boolean isHexDigit(int c) {
        //return (((c >= '0') && (c <= '9')) || ((c >= 'a') && (c <= 'f'))
        //|| ((c >= 'A') && (c <= 'F')));
        if (c <  '0') return false;
        if (c <= '9') return true;
        if (c <  'A') return false;
        if (c <= 'F') return true;
        if (c <  'a') return false;
        if (c <= 'f') return true;
        return false;
    }

    /** Returns true if all of the characters in s are hex digits.
     * A 0-length string returns false.
     */
    public static final boolean isHexString(String s) {
        if (s == null)
            return false;
        int sLength = s.length();
        if (sLength == 0)
            return false;
        for (int i = 0; i < sLength; i++)
            if (!isHexDigit(s.charAt(i)))
                return false;
        return true;
    }

    /**
     * 0..9.
     * Non-Latin numeric characters are not included (see Java Lang Spec pg 14).
     *
     * @param c a char
     * @return true if c is a digit
     */
    public static final boolean isDigit(int c) {
        return (c >= '0') && (c <= '9');
    }

    /**
     * 0..9.
     * Non-Latin numeric characters are not included (see Java Lang Spec pg 14).
     *
     * @param s a string
     * @return true if c is a digit
     */
    public static final boolean allDigits(String s) {
        if (s == null)
            return false;
        int n = s.length();
        if (n == 0)
            return false;
        for (int po = 0; po < n; po++)
            if (!isDigit(s.charAt(po)))
                return false;
        return true;
    }

    /**
     * Determines if the character is a digit or a letter.
     *
     * @param c a char
     * @return true if c is a letter or a digit
     */
    public static final boolean isDigitLetter(int c) {
        return isLetter(c) || isDigit(c);
    }

    /**
     * This tries to quickly determine if the string is a correctly
     * formatted number (including decimal, hexadecimal, octal, and "NaN" (any case)).
     *
     * <p>This may not be perfect.  In the future, this may be changed to be perfect.
     * That shouldn't affect its use.
     *
     * @param s  usually already trimmed, since any space in s will return false.
     * @return true if s is *probably* a number.
     *     This returns false if s is *definitely* not a number.
     *     "NaN" (case insensitive) returns true.  (It is a numeric value of sorts.)
     *     null and "" return false.
     */
    public static final boolean isNumber(String s) {
        if (s == null)
            return false;
        int sLength = s.length();
        if (sLength == 0)
            return false;
        char ch0 = s.charAt(0);

        //hexadecimal? e.g., 0x2AFF            //octal not supported
        if (ch0 == '0' && sLength >= 3 && Character.toUpperCase(s.charAt(1)) == 'X') {
            //ensure all remaining chars are hexadecimal
            for (int po = 2; po < sLength; po++) {
                if ("0123456789abcdefABCDEF".indexOf(s.charAt(po)) < 0) 
                    return false;
            }
            return true;
        }

        //NaN?
        if (ch0 == 'n' || ch0 == 'N') 
            return s.toUpperCase().equals("NAN");

        //*** rest of method: test if floating point
        //is 1st char .+-?
        int po = 0;
        char ch = s.charAt(po);
        boolean hasPeriod = ch == '.';
        if (hasPeriod || ch == '-') {
            if (sLength == 1)  //must be another char
                return false;
            ch = s.charAt(++po);

            //is 2nd char .?
            if (ch == '.') {
                if (hasPeriod || sLength == 2)  // 2nd period or . after e are not allowed
                    return false;
                hasPeriod = true;
                ch = s.charAt(++po);
            }
        }
        
        //initial digit
        if (ch < '0' || ch > '9')  //there must be a digit 
            return false;  

        //subsequent chars
        boolean hasE = false;
        while (++po < sLength) {
            ch = s.charAt(po);
            if (ch == '.') {
                if (hasPeriod || hasE || po == sLength - 1)  // 2nd period or . after e are not allowed
                    return false;
                hasPeriod = true;
            } else if (ch == 'e' || ch == 'E') {
                if (hasE || po == sLength - 1) //e as last char is not allowed
                    return false;
                hasE = true;
                ch = s.charAt(++po);
                if (ch == '-' || ch == '+') {
                    if (po == sLength-1)
                        return false;
                    ch = s.charAt(++po);
                }
                if (ch < '0' || ch > '9') //there must be a digit after e     
                    return false;  
            } else if (ch < '0' || ch > '9') { 
                return false;  
            }
        }
        return true;  //probably a number
    }


    /**
     * Whitespace characters are u0001 .. ' '.
     * Java just considers a few of these (sp HT FF) as white space,
     *  see the Java Lang Specification.
     * u0000 is not whitespace.  Some methods count on this fact.
     *
     * @param c a char
     * @return true if c is a whitespace character
     */
    public static final boolean isWhite(int c) {
        return ((c >= '\u0001') && (c <= ' ')) || c == '\u00a0'; //nbsp
    }

    /**
     * This indicates if ch is printable with System.err.println() and
     *   Graphics.drawString(); hence, it is a subset of 0..255.
     * <UL>
     * <LI> This is used, for example, to limit characters entering CoText.
     * <LI> Currently, this accepts the ch if
     *   <tt>(ch&gt;=32 &amp;&amp; ch&lt;127) || (ch&gt;=161 &amp;&amp; ch&lt;=255)</tt>.
     * <LI> tab(#9) is not included.  It should be caught separately
     *   and dealt with (expand to spaces?).  The problem is that
     *   tabs are printed with a wide box (non-character symbol)
     *   in Windows Courier font.
     *   Thus, they mess up the positioning of characters in CoText.
     * <LI> newline is not included.  It should be caught separately
     *   and dealt with.
     * <LI> This requires further study into all standard fonts on all
     *   platforms to see if other characters can be accepted.
     * </UL>
     *
     * @param ch a char
     * @return true if ch is a printable character
     */
    public static final boolean isPrintable(int ch) {
        //return (ch&gt;=32 &amp;&amp; ch<127) || (ch&gt;=161 &amp;&amp; ch&lt;=255);  //was 160
        if (ch <   32) return false;
        if (ch <= 126) return true;  //was 127 
        if (ch <  161) return false; //was 160
        if (ch <= 255) return true;
        return false;
    }

    /** Returns true if all of the characters in s are printable */
    public static final boolean isPrintable(String s) {
        if (s == null)
            return false;
        int sLength = s.length();
        for (int i = 0; i < sLength; i++)
            if (!isPrintable(s.charAt(i)))
                return false;
        return true;
    }

    /** returns true if ch is 32..126. */
    public static final boolean isAsciiPrintable(int ch) {
        if (ch <   32) return false;
        if (ch <= 126) return true; 
        return false;
    }

    /** Returns true if all of the characters in s are 32..126 */
    public static final boolean isAsciiPrintable(String s) {
        if (s == null)
            return false;
        int sLength = s.length();
        for (int i = 0; i < sLength; i++)
            if (!isAsciiPrintable(s.charAt(i)))
                return false;
        return true;
    }
    /**
     * This returns the string with all non-isPrintable characters removed.
     *
     * @param s
     * @return s with all the non-isPrintable characters removed.
     *   If s is null, this throws null pointer exception.
     */
    public static String justPrintable(String s) {
        int n = s.length();
        StringBuilder sb = new StringBuilder(n);
        int start = 0;
        for (int i = 0; i < n; i++) {
            if (!isPrintable(s.charAt(i))) {
                sb.append(s.substring(start, i));
                start = i + 1;
            }
        }
        sb.append(s.substring(start));
        return sb.toString();
    }

    /** This crudely converts 160.. 255 to plainASCII characters which 
     * hava a similar meaning or look similar (or '?' if no good substitute). 
     */
    public final static String plainASCII = 
        " !cLoY|%:Ca<--R-" +
        "o???'uP.,'o>????" +
        "AAAAAAACEEEEIIII" +
        "DNOOOOOxOUUUUYpB" +
        "aaaaaaaceeeeiiii" +
        "onooooo/ouuuuypy";

    /**
     * This converts the string to plain ascii (0..127).
     * Diacritics are stripped off high ASCII characters.
     * Some high ASCII characters are crudely converted to similar characters
     * (the conversion is always character-for-character, 
     * so the string length will be unchanged).
     * Other characters become '?'.
     * The result will be the same length as s.
     *
     * @param s
     * @return the string converted to plain ascii (0..127).
     */
    public static String modifyToBeASCII(String s) {
        StringBuilder sb = new StringBuilder(s);
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char ch = sb.charAt(i);
            if (ch <= 127) {}
            else if (ch >= 160 && ch <= 255) sb.setCharAt(i, plainASCII.charAt(ch - 160));
            else sb.setCharAt(i, '?');
        }
        return sb.toString();
    }

    /**
     * A description of file-name-safe characters.
     */
    public final static String fileNameSafeDescription = "(A-Z, a-z, 0-9, _, -, or .)";

    /**
     * This indicates if ch is a file-name-safe character (A-Z, a-z, 0-9, _, -, or .).
     *
     * @param ch
     * @return true if ch is a file-name-safe character (A-Z, a-z, 0-9, _, -, .).
     */
    public static boolean isFileNameSafe(char ch) {
        //return (ch >= 'A' &amp;&amp; ch <= 'Z') ||                
        //       (ch >= 'a' &amp;&amp; ch <= 'z') ||                
        //       (ch >= '0' &amp;&amp; ch <= '9') ||
        //        ch == '-' || ch == '_' || ch == '.';
        if (ch == '.' || ch == '-') return true;
        if (ch <  '0') return false;
        if (ch <= '9') return true;
        if (ch <  'A') return false;
        if (ch <= 'Z') return true;
        if (ch == '_') return true;
        if (ch <  'a') return false;
        if (ch <= 'z') return true;
        return false;
    }

    /**
     * This indicates if 'email' is a valid email address.
     *
     * @param email a possible email address
     * @return true if 'email' is a valid email address.
     */
    public static boolean isEmailAddress(String email) {
        if (email == null || email.length() == 0)
            return false;

        return EMAIL_PATTERN.matcher(email).matches(); 
    }

    /**
     * This indicates if 'url' is probably a valid url.
     * This is like isRemote, but returns true for "file://...".
     *
     * @param url a possible url
     * @return true if 'url' is probably a valid url.
     *    false if 'url' is not a valid url.
     *    Note that "file://..." is a url.
     */
    public static boolean isUrl(String url) {
        if (url == null)
            return false;
        int po = url.indexOf("://");
        if (po == -1 ||
            !isPrintable(url))
            return false;

        String protocol = url.substring(0, po);
        return 
            protocol.equals("file") ||
            protocol.equals("ftp") ||
            protocol.equals("http") ||
            protocol.equals("https") ||
            protocol.equals("sftp") ||
            protocol.equals("smb");
    }

    /** 
     * This returns true if the dir starts with http://, https://, ftp://, sftp://,
     * or smb://.
     * This is like isRemote, but returns false for "file://...".
     * 
     * @return true if the dir is remote (e.g., a URL other than file://)
     *   If dir is null or "", this returns false.
     */
    public static boolean isRemote(String dir) {
        if (isUrl(dir))
            return dir.startsWith("file://")? false : true;
        return false;
    }



    /**
     * This indicates if s has length &gt;= 1 and 
     *   has just file-name-safe characters (0-9, A-Z, a-z, _, -, .).
     * Note, this does not check for filenames that are too long
     * (Windows has a path+fileName max length of 255 chars).
     *
     * @param s a string, usually a file name
     * @return true if s has just file-name-safe characters (0-9, A-Z, a-z, _, -, .).
     *    It returns false if s is null or "".
     */
    public static boolean isFileNameSafe(String s) {
        if (s == null || s.length() == 0)
            return false;
        int sLength = s.length();
        for (int i = 0; i < sLength; i++)
            if (!isFileNameSafe(s.charAt(i)))
                return false;
        return true;
    }

    /**
     * This returns the string with just file-name-safe characters (0-9, A-Z, a-z, _, -, .).
     * This is different from String2.encodeFileNameSafe --
     *   this emphasizes readability, not avoiding losing information.
     * Non-safe characters are converted to '_'.
     * Adjacent '_' are collapsed into '_'.
     * See posix fully portable file names at https://en.wikipedia.org/wiki/Filename .
     * See javadocs for java.net.URLEncoder, which describes valid characters
     *  (but deals with encoding, whereas this method alters or removes).
     * The result may be shorter than s.
     * Note, this does not check for filenames that are too long
     * (Windows has a path+fileName max length of 255 chars).
     *
     * @param s  If s is null, this returns "_null".
     *    If s is "", this returns "_".
     * @return s with all of the non-fileNameSafe characters removed or changed
     */
    public static String modifyToBeFileNameSafe(String s) {
        if (s == null)
            return "_null";
        s = modifyToBeASCII(s);
        int n = s.length();
        if (n == 0)
            return "_";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            sb.append(isFileNameSafe(ch)? ch : '_');
        }
        while (sb.indexOf("__") >= 0)
            replaceAll(sb, "__", "_");

        return sb.toString();
    }

    /**
     * This tests if s is a valid variableName:
     * <ul>
     * <li>first character must be (iso8859Letter|_).
     * <li>optional subsequent characters must be (iso8859Letter|_|0-9).
     * </ul>
     * Note that Java allows Unicode characters, but this does not.
     *
     * @param s a possible variable name
     * @return true if s is a valid variableName.
     */
    public static boolean isVariableNameSafe(String s) {
        if (s == null)
            return false;
        int n = s.length();
        if (n == 0)
            return false;

        //first character must be (iso8859Letter|_)
        char ch = s.charAt(0);
        if (isLetter(ch) || ch == '_') ;
        else return false;

        //subsequent characters must be (iso8859Letter|_|0-9)
        for (int i = 1; i < n; i++) {
            ch = s.charAt(i);
            if (isDigitLetter(ch) || ch == '_') ;
            else return false;    
        }
        return true;
    }

    /**
     * This tests if s is a valid jsonp function name.
     * The functionName MUST be a series of 1 or more (period-separated) words.
     * For each word:
     * <ul>
     * <li>The first character must be (iso8859Letter|_).
     * <li>The optional subsequent characters must be (iso8859Letter|_|0-9).
     * <li>s must not be longer than 255 characters.
     * </ul>
     * Note that JavaScript allows Unicode characters, but this does not.
     *
     * @param s a possible jsonp function name
     * @return true if s is a valid jsonp function name.
     */
    public static boolean isJsonpNameSafe(String s) {
        if (s == null)
            return false;
        int n = s.length();
        if (n == 0 || n > 255)
            return false;

        //last (or only) character can't be .
        if (s.charAt(n - 1) == '.')
            return false;

        ArrayList<String> al = splitToArrayList(s, '.', false); //trim=false
        int nal = al.size();

        //test each word
        for (int part = 0; part < nal; part++) {
            String ts = al.get(part);
            int tn = ts.length();
            if (tn == 0)
                return false;

            //first character must be (iso8859Letter|_)
            char ch = ts.charAt(0);
            if (isLetter(ch) || ch == '_') ;
            else return false;

            //subsequent characters must be (iso8859Letter|_|0-9)
            for (int i = 1; i < tn; i++) {
                ch = ts.charAt(i);
                if (isDigitLetter(ch) || ch == '_') ;
                else return false;    
            }
        }

        return true;
    }


    /**
     * This is like modifyToBeFileNameSafe, but restricts the name to:
     * <ul>
     * <li>first character must be (iso8859Letter|_).
     * <li>subsequent characters must be (iso8859Letter|_|0-9).
     * </ul>
     * Note that Java allows Unicode characters, but this does not.
     * See also the safer encodeMatlabNameSafe(String s).
     * Note, this does not check for names that are too long
     * (many system have an 80 or 255 char limit).
     *
     * @param s
     * @return a safe variable name (but perhaps two s's lead to the same result)
     */
    public static String modifyToBeVariableNameSafe(String s) {
        if (isVariableNameSafe(s))
            return s;
        if (s == null)
            return "_null";
        s = replaceAll(s, "%20", "_");
        if (s.indexOf("%3a") >= 0) {
            s = replaceAll(s, "CF%3afeature_type", "featureType"); //CF:feature_type
            s = replaceAll(s, "CF%3a", ""); //CF:
            s = replaceAll(s, "%3a", "_");
        }
        int n = s.length();
        if (n == 0)
            return "_";

        StringBuilder sb = new StringBuilder(n + 1);

        //first character must be (iso8859Letter|_)
        char ch = s.charAt(0);
        sb.append(isLetter(ch)? ch : isDigit(ch)? "_" + ch : "_");

        //subsequent characters must be (iso8859Letter|_|0-9)
        for (int i = 1; i < n; i++) {
            ch = s.charAt(i);
            if (isDigitLetter(ch))
                sb.append(ch);
            else if (sb.charAt(sb.length() - 1) != '_')
                sb.append('_');    
        }

        //remove trailing _
        if (sb.length() > 1 && sb.charAt(sb.length() - 1) == '_')
            sb.setLength(sb.length() - 1);

        return sb.toString();
    }


    /**
     * This counts all occurrences of <tt>findS</tt> in sb.
     * if (sb == null || findS == null || findS.length() == 0) return 0;
     * 
     * @param sb the source StringBuilder
     * @param findS the string to be searched for
     */
    public static int countAll(StringBuilder sb, String findS) {
        if (sb == null || findS == null || findS.length() == 0) return 0;
        int n = 0;
        int sLength = findS.length();
        int po = sb.indexOf(findS, 0);
        while (po >= 0) {
            n++;
            po = sb.indexOf(findS, po + sLength);
        }
        return n;
    }

    /**
     * This counts all occurrences of <tt>findS</tt> in s.
     * if (s == null || findS == null || findS.length() == 0) return 0;
     * 
     * @param s the source string
     * @param findS the string to be searched for
     */
    public static int countAll(String s, String findS) {
        if (s == null || findS == null || findS.length() == 0) return 0;
        int n = 0;
        int sLength = findS.length();
        int po = s.indexOf(findS, 0);
        while (po >= 0) {
            n++;
            po = s.indexOf(findS, po + sLength);
        }
        return n;
    }

    /**
     * Replaces all occurences of <tt>oldS</tt> in sb with <tt>newS</tt>.
     * If <tt>oldS</tt> occurs inside <tt>newS</tt>, it won't be replaced
     *   recursively (obviously).
     * 
     * @param sb the source StringBuilder
     * @param oldS the string to be searched for
     * @param newS the string to replace oldS
     */
    public static void replaceAll(StringBuilder sb, String oldS, String newS) {
        replaceAll(sb, oldS, newS, false);
    }

    /**
     * Replaces all occurences of <tt>oldS</tt> in sb with <tt>newS</tt>.
     * If <tt>oldS</tt> occurs inside <tt>newS</tt>, it won't be replaced
     *   recursively (obviously).
     * When searching sb for oldS, this ignores the case of sb and oldS.
     * 
     * @param sb the StringBuilder
     * @param oldS the string to be searched for
     * @param newS the string to replace oldS
     */
    public static void replaceAllIgnoreCase(StringBuilder sb, String oldS, String newS) {
        replaceAll(sb, oldS, newS, true);
    }

    /**
     * Replaces all occurences of <tt>oldS</tt> in sb with <tt>newS</tt>.
     * If <tt>oldS</tt> occurs inside <tt>newS</tt>, it won't be replaced
     *   recursively (obviously).
     * 
     * @param sb the StringBuilder
     * @param oldS the string to be searched for
     * @param newS the string to replace oldS
     * @param ignoreCase   If true, when searching sb for oldS, this ignores the case of sb and oldS.
     * @return the number of replacements made.
     */
    public static int replaceAll(StringBuilder sb, String oldS, String newS, boolean ignoreCase) {
        int sbL = sb.length();
        int oldSL = oldS.length();
        if (oldSL == 0)
            return 0;
        int newSL = newS.length();
        StringBuilder testSB = sb;
        String testOldS = oldS;
        if (ignoreCase) {
            testSB = new StringBuilder(sbL);
            for (int i = 0; i < sbL; i++)
                testSB.append(Character.toLowerCase(sb.charAt(i)));
            testOldS = oldS.toLowerCase();
        }
        int po = testSB.indexOf(testOldS);
        //System.out.println("testSB=" + testSB.toString() + " testOldS=" + testOldS + " po=" + po); //not String2.log
        if (po < 0) return 0;
        StringBuilder sb2 = new StringBuilder(sbL / 5 * 6); //a little bigger
        int base = 0;
        int n = 0;
        while (po >= 0) {
            n++;
            sb2.append(sb.substring(base, po));
            sb2.append(newS);
            base = po + oldSL;
            po = testSB.indexOf(testOldS, base);
            //System.out.println("testSB=" + testSB.toString() + " testOldS=" + testOldS + " po=" + po + 
            //    " sb2=" + sb2.toString()); //not String2.log
        }
        sb2.append(sb.substring(base));
        sb.setLength(0);
        sb.append(sb2);
        return n;
    }

    /**
     * This repeatedly replaces all oldS with newS.
     * e.g., replace "++" with "+" in "++++" will yield "+".
     *
     * @return sb for convenience
     */
    public static StringBuilder repeatedlyReplaceAll(StringBuilder sb, String oldS, String newS, boolean ignoreCase) {        
        while (replaceAll(sb, oldS, newS, ignoreCase) > 0) {}
        return sb;
    }

    /**
     * This repeatedly replaces all oldS with newS.
     * e.g., replace "++" with "+" in "++++" will yield "+".
     *
     */
    public static String repeatedlyReplaceAll(String s, String oldS, String newS, boolean ignoreCase) {        
        StringBuilder sb = new StringBuilder(s);
        while (replaceAll(sb, oldS, newS, ignoreCase) > 0) {}
        return sb.toString();
    }



    /**
     * Returns a string where all occurences of <tt>oldS</tt> have
     *   been replaced with <tt>newS</tt>.
     * If <tt>oldS</tt> occurs inside <tt>newS</tt>, it won't be replaced
     *   recursively (obviously).
     *
     * @param s the main string
     * @param oldS the string to be searched for
     * @param newS the string to replace oldS
     * @return a modified version of s, with newS in place of all the olds.
     * @throws RuntimeException if s is null.
     */
    public static String replaceAll(String s, String oldS, String newS) {
        StringBuilder sb = new StringBuilder(s);
        replaceAll(sb, oldS, newS, false);
        return sb.toString();
    }

    /**
     * Returns a string where all occurences of <tt>oldS</tt> have
     *   been replaced with <tt>newS</tt>.
     * If <tt>oldS</tt> occurs inside <tt>newS</tt>, it won't be replaced
     *   recursively (obviously).
     * When finding oldS in s, their case is irrelevant.
     *
     * @param s the main string
     * @param oldS the string to be searched for
     * @param newS the string to replace oldS
     * @return a modified version of s, with newS in place of all the olds.
     *   throws RuntimeException if s is null.
     */
    public static String replaceAllIgnoreCase(String s, String oldS, String newS) {
        StringBuilder sb = new StringBuilder(s);
        replaceAll(sb, oldS, newS, true);
        return sb.toString();
    }

    /**
     * Returns a string where all cases of more than one space are 
     * replaced by one space.  The string is also trim'd to remove
     * leading and trailing spaces.
     * Also, spaces after { or ( and before ) or } will be removed.
     *
     * @param s 
     * @return s, but with the spaces combined
     *    (or null if s is null)
     */
    public static String combineSpaces(String s) {
        if (s == null)
            return null;
        s = s.trim();
        int sLength = s.length();
        if (sLength <= 2) //first and last chars must be non-space
            return s; 
        StringBuilder sb = new StringBuilder(sLength);
        sb.append(s.charAt(0));
        for (int po = 1; po < sLength; po++) {
            char ch = s.charAt(po);
            if (ch == ' ') {
                if ("{( ".indexOf(sb.charAt(sb.length() - 1)) < 0) //prev isn't {( or ' '
                    sb.append(ch);
            } else if (")}".indexOf(ch) >= 0 && sb.charAt(sb.length() - 1) == ' ') {
                sb.setCharAt(sb.length() - 1, ch); // ) overwrite previous ' '
            } else { 
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * This is like combineSpaces, but converts any sequence of whitespace
     * to be one space.  The string is also trim'd to remove
     * leading and trailing whitespace.
     * Also, spaces after { or ( and before ) or } will be removed.
     *
     * @param sb 
     */
    public static void whitespacesToSpace(StringBuilder sb) {
        if (sb == null)
            return;
        String s = sb.toString().trim(); //this removes whitespace, not just ' '
        int sLength = s.length();
        sb.setLength(0);
        if (sLength == 0)
            return;
        sb.append(s.charAt(0));
        for (int po = 1; po < sLength; po++) {
            char ch = s.charAt(po);
            if (Character.isWhitespace(ch)) {
                char ch2 = sb.charAt(sb.length() - 1);
                if (!(ch2 == '{' || ch2 =='(' || ch2 == ' ')) //prev isn't {( or ' '
                    sb.append(' ');
            } else if ((ch == ')' || ch == '}') && 
                sb.charAt(sb.length() - 1) == ' ') {
                sb.setCharAt(sb.length() - 1, ch); // ) overwrite previous ' '
            } else { 
                sb.append(ch);
            }
        }
    }

    /* A variant that takes and returns a string */
    public static String whitespacesToSpace(String s) {
        if (s == null)
            return null;
        StringBuilder sb = new StringBuilder(s);
        whitespacesToSpace(sb);
        return sb.toString();
    }

    /**
     * Returns a string where all occurences of <tt>oldCh</tt> have
     *   been replaced with <tt>newCh</tt>.
     * This doesn't throw exceptions if bad values.
     */
    public static String replaceAll(String s, char oldCh, char newCh) {
        int po = s.indexOf(oldCh);
        if (po < 0)
            return s;

        StringBuilder buffer = new StringBuilder(s);
        while (po >= 0) {
            buffer.setCharAt(po, newCh);
            po = s.indexOf(oldCh, po + 1);
        }
        return buffer.toString();
    }

    /**
     * Returns the same StringBuilder, where all occurences of <tt>oldCh</tt> have
     *   been replaced with <tt>newCh</tt>.
     *
     * @return the same StringBuilder, for convenience.
     */
    public static StringBuilder replaceAll(StringBuilder sb, char oldCh, char newCh) {
        String oldS = "" + oldCh;
        int po = sb.indexOf(oldS);
        while (po >= 0) {
            sb.setCharAt(po, newCh);
            po = sb.indexOf(oldS, po + 1);
        }
        return sb;
    }

    /**
     * This converts many common &gt;255 Unicode characters to the similar
     * plain text character.
     *
     * @return sb for convenience
     */
    public static StringBuilder commonUnicodeToPlainText(StringBuilder sb) {
        replaceAll(sb, '\u2013', '-');  //endash 
        replaceAll(sb, '\u2014', '-');  //emdash   --?
        replaceAll(sb, '\u2018', '\''); //left quote
        replaceAll(sb, '\u2019', '\''); //right quote
        replaceAll(sb, '\u201c', '\"'); //left double quote
        replaceAll(sb, '\u201d', '\"'); //right double quote
        replaceAll(sb, '\u2212', '-');  //math minus sign
        replaceAll(sb, '\u03bc', '�');  //mu
        replaceAll(sb, "\u2264", "<="); 
        replaceAll(sb, "\u2265", ">="); 
        return sb;
    }

    public static String commonUnicodeToPlainText(String s) {
        return commonUnicodeToPlainText(new StringBuilder(s)).toString();
    }

    /**
     * This adds 0's to the left of the string until there are <tt>nDigits</tt>
     *   to the left of the decimal point (or nDigits total if there isn't
     *   a decimal point).
     * If the number is too big, nothing is added or taken away.
     *
     * @param number a positive number.  This doesn't handle negative numbers.
     * @param nDigits the desired number of digits to the left of the decimal point
     * (or total, if no decimal point)
     * @return the number, left-padded with 0's so there are nDigits to 
     * the left of the decimal point
     */
    public static String zeroPad(String number, int nDigits) {
        int decimal = number.indexOf(".");
        if (decimal < 0)
            decimal = number.length();

        int toAdd = nDigits - decimal;
        if (toAdd <= 0)
            return number;

        return makeString('0', toAdd).concat(number);
    }

    /**
     * The converts a string[] into a JSON array of strings.
     *
     * @param sa
     * @return e.g., ["aa", "bb", "cc"].
     *     If sa is null, this returns null (as a String).
     */
    public static String toJsonArray(String sa[]) {
        if (sa == null)
            return "null";
        int saLength = sa.length;
        StringBuilder sb = new StringBuilder(10 * saLength);
        int start = 0;
        for (int i = 0; i < saLength; i++) {
            sb.append(i == 0? "[" : ",");
            sb.append(toJson(sa[i]));
        }
        sb.append("]");
        return sb.toString();
    }



    /**
     * This makes a JSON version of a float. 
     *
     * @param f
     * @return "null" if not finite. Return an integer if it ends with ".0".
     *    Else returns the number as a string.
     */
    public static String toJson(float f) {
        if (!Float.isFinite(f))
            return "null";
        String s = "" + f;
        return s.endsWith(".0")? s.substring(0, s.length() - 2) : s;
    }

    /**
     * This makes a JSON version of a number. 
     *
     * @param d
     * @return "null" if not finite. Return an integer if it ends with ".0".
     *    Else returns the number as a string.
     */
    public static String toJson(double d) {
        if (!Double.isFinite(d))
            return "null";
        String s = "" + d;
        return s.endsWith(".0")? s.substring(0, s.length() - 2) : s;
    }

    /**
     * This makes a JSON version of a string 
     * (\\, \f, \n, \r, \t and \" are escaped with a backslash character
     * and double quotes are added before and after).
     * null is returned as null.
     * This variant encodes char #127 and above.
     *
     * @param s
     * @return the JSON-encoded string surrounded by "'s.
     */
    public static String toJson(String s) {
        return toJson(s, 127, true);
    }

    /**
     * This variant doesn't encode high characters.
     *
     * @param s
     * @return the JSON-encoded string surrounded by "'s.
     */
    public static String toJson65536(String s) {
        return toJson(s, 65536, true);
    }

    /**
     * This makes a JSON version of a string 
     * (\\, \f, \n, \r, \t and \" are escaped with a backslash character
     * and double quotes are added before and after).
     * null is returned as null.
     * This variant encodes char #127 and above as \\uhhhh.
     *
     * @param s The String to be encoded.
     * @param firstUEncodedChar The first char to be \\uhhhh encoded, 
     *   commonly 127, 256, or 65536.
     * @return the JSON-encoded string surrounded by "'s.
     */
    public static String toJson(String s, int firstUEncodedChar) {
        return toJson(s, firstUEncodedChar, true);
    }

    /** 
     * This is a variant of toJson that lets you encode newlines or not. 
     * 
     * @param s The String to be encoded.
     * @param firstUEncodedChar The first char to be \\uhhhh encoded, 
     *   commonly 127, 256, or 65536.
     * @return the JSON-encoded string surrounded by "'s.
     */
    public static String toJson(String s, int firstUEncodedChar, boolean encodeNewline) {
        if (s == null)
            return "null";
        int sLength = s.length();
        StringBuilder sb = new StringBuilder((sLength / 5 + 1) * 6);
        sb.append('\"');
        int start = 0;
        for (int i = 0; i < sLength; i++) {
            char ch = s.charAt(i);
            //using 127 (not 255) means the output is 7bit ASCII and file encoding is irrelevant
            if (ch < 32 || ch >= firstUEncodedChar) { 
                sb.append(s.substring(start, i));  
                start = i + 1;
                if      (ch == '\f') sb.append("\\f");
                else if (ch == '\n') sb.append(encodeNewline? "\\n" : "\n");
                else if (ch == '\r') sb.append("\\r");
                else if (ch == '\t') sb.append("\\t");
                else if (ch == '\b') {} //remove it
                //  / can be encoded as \/ but there is no need and it looks odd
                else sb.append("\\u" + zeroPad(Integer.toHexString(ch), 4)); 
            } else if (ch == '\\') {
                sb.append(s.substring(start, i));  
                start = i + 1;
                sb.append("\\\\");
            } else if (ch == '\"') {
                sb.append(s.substring(start, i));  
                start = i + 1;
                sb.append("\\\"");
            }  // else normal character will be appended later via s.substring
        }
        sb.append(s.substring(start));  
        sb.append('\"');
        return sb.toString();
    }

    /** This encodes one char to the Json encoding. */
    public static String charToJsonString(char ch, int firstUEncodedChar, boolean encodeNewline) {
        //using 127 (not 255) means the output is 7bit ASCII and file encoding is irrelevant
        if (ch < 32 || ch >= firstUEncodedChar) { 
            if      (ch == '\f') return "\\f";
            else if (ch == '\n') return encodeNewline? "\\n" : "\n";
            else if (ch == '\r') return "\\r";
            else if (ch == '\t') return "\\t";
            else if (ch == '\b') return ""; //remove it
            //  / can be encoded as \/ but there is no need and it looks odd
            return "\\u" + zeroPad(Integer.toHexString(ch), 4); 
        } 
        if (ch == '\\') 
            return "\\\\";
        if (ch == '\"') 
            return "\\\"";
        // else normal character
        return "" + ch;
    }

    /**
     * This is like the other fromJson, but returns "" instead of null.
     */
    public static String fromJsonNotNull(String s) {
        s = fromJson(s);
        return s == null? "" : s;
    }

    /**
     * This returns the unJSON version of a JSON string 
     * (surrounding "'s (if any) are removed and \\, \f, \n, \r, \t, \/, and \" are unescaped).
     * This is very liberal in what it accepts, including all common C escaped characters:
     * http://msdn.microsoft.com/en-us/library/h21280bw%28v=vs.80%29.aspx
     * "null" returns the String "null". null returns null.
     * This won't throw an exception.
     *
     * @param s  it may be enclosed by "'s, or not.
     * @return the decoded string
     */
    public static String fromJson(String s) {
        if (s == null)
            return null;
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
            s = s.substring(1, s.length() - 1);
        int sLength = s.length();
        StringBuilder sb = new StringBuilder(sLength);
        int po = 0;
        int start = 0;
        while (po < sLength) {
            char ch = s.charAt(po);
            if (ch == '\\') {
                sb.append(s.substring(start, po)); 
                if (po == sLength - 1) 
                    po--;  //so reread \ and treat as \\
                po++; 
                start = po + 1;
                ch = s.charAt(po);
                if      (ch == 'f')  sb.append('\f');
                else if (ch == 'n')  sb.append('\n');
                else if (ch == 'r')  sb.append('\r');
                else if (ch == 't')  sb.append('\t');
                else if (ch == '?')  sb.append('?');
                else if (ch == '\\') sb.append('\\');
                else if (ch == '/')  sb.append('/');
                else if (ch == '"')  sb.append('\"');
                else if (ch == '\'') sb.append('\'');
                else if (ch == 'a' || ch == 'b' || ch == 'v') {
                    //delete a=bell, b=backspace, v=vertTab
                } else if (isDigit(ch) && po + 2 < sLength) { 
                    //  \\ooo octal
                    String os = s.substring(po, po + 3);
                    try {
                        po += 2;
                        start = po + 1;

                        sb.append((char)Integer.parseInt(os, 8));
                    } catch (Exception e) {      
                        log("ERROR in fromJson: invalid escape sequence \\" + os);
                        //falls through
                    }
                    
                } else if (ch == 'x' && po + 2 < sLength) { 
                    //  \\xhh hex
                    String os = s.substring(po + 1, po + 3);
                    try {
                        po += 2;
                        start = po + 1;

                        sb.append((char)Integer.parseInt(os, 16));
                    } catch (Exception e) {      
                        log("ERROR in fromJson: invalid escape sequence \\x" + os);
                        //falls through
                    }
                    
                } else if (ch == 'u' && po + 4 < sLength) { 
                    //  \\uhhhh unicode 
                    String os = s.substring(po + 1, po + 5);
                    try {
                        po += 4;
                        start = po + 1;

                        sb.append((char)Integer.parseInt(os, 16));
                    } catch (Exception e) {      
                        log("ERROR in fromJson: invalid escape sequence \\u" + os);
                        //falls through
                    }                    
                   
                } else { 
                    //this shouldn't happen, but be forgiving
                    //before 2009-02-27, this tossed the \    is this the best solution?
                    log("ERROR in fromJson: invalid escape sequence \\" + ch);
                    sb.append('\\'); 
                    sb.append(ch); 
                } 
            } //else normal char will be appended by s.substring later
            po++;
        }
        sb.append(s.substring(start));
        return sb.toString();
    }
    
    /**
     * This converts an NCCSV encoded char to a true char  
     * (surrounding "'s and ''s (if any) are removed and \\, \f, \n, \r, \t, \/, and \" are unescaped).
     * This is very liberal in what it accepts, including all common C escaped characters:
     * http://msdn.microsoft.com/en-us/library/h21280bw%28v=vs.80%29.aspx
     *
     * @param s  it may be enclosed by "'s and ''s, or not.
     * @return the decoded char (or '?' if trouble) as a 1-char string.
     */
    public static char fromNccsvChar(String s) {
        if (s == null)
            return '?';
        //String2.log(">> String2.fromNccsvChar  in=" + annotatedString(s));
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') 
            s = s.substring(1, s.length() - 1);
        if (s.length() >= 2 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') 
            s = s.substring(1, s.length() - 1);
        s = s.equals("\"\"")? "\"" : fromJson(s);
        //String2.log(">> String2.fromNccsvChar out=" + annotatedString(s));
        return s.length() > 0? s.charAt(0) : '?'; 
    }

    /**
     * This converts an NCCSV string to a true string  
     * (surrounding "'s (if any) are removed and \\, \f, \n, \r, \t, \/, and \" are unescaped).
     * This is very liberal in what it accepts, including all common C escaped characters:
     * http://msdn.microsoft.com/en-us/library/h21280bw%28v=vs.80%29.aspx
     * This won't throw an exception.
     *
     * @param s  it may be enclosed by "'s, or not.
     * @return the decoded string
     */
    public static String fromNccsvString(String s) {
        return replaceAll(fromJson(s), "\"\"", "\"");
    }

    /**
     * This encodes one char for an NCCSV char or String, without surrounding quotes.
     */
    public static String toNccsvChar(char ch) {
        if (ch == '\\') return "\\\\";
        if (ch == '\b') return "\\b"; //trouble
        if (ch == '\f') return "\\f";
        if (ch == '\n') return "\\n";
        if (ch == '\r') return "\\r";
        if (ch == '\t') return "\\t";
        if (ch == '\"') return "\"\"";
        if (ch < ' ' || ch > '~') return "\\u" + zeroPad(Integer.toHexString(ch), 4); 
        return "" + ch;
    }

    /**
     * This encodes one String as an NCCSV data String, with surrounding double quotes
     * only if necessary.
     */
    public static String toNccsvDataString(String s) {
        //encode the string
        if (s == null || s.length() == 0)
            return "";
        int n = s.length();
        StringBuilder sb = new StringBuilder(n * 2);
        for (int i = 0; i < n; i++)
            sb.append(toNccsvChar(s.charAt(i)));

        //surround in "'s?
        if (s.startsWith(" ") ||
            s.endsWith(" ") ||
            s.indexOf(',') >= 0 || 
            s.indexOf('"') >= 0 ||
            s.equals("null"))
            return "\"" + sb.toString() + "\"";
        return sb.toString();
    }

    /**
     * This encodes one String as an NCCSV att String, with surrounding double quotes
     * only if necessary.
     */
    public static String toNccsvAttString(String s) {
        //encode the string
        int n = s.length();
        StringBuilder sb = new StringBuilder(n * 2);
        for (int i = 0; i < n; i++)
            sb.append(toNccsvChar(s.charAt(i)));

        //surround in "'s?
        if (s.startsWith(" ") ||
            s.endsWith(" ") ||
            s.indexOf(',') >= 0 || 
            s.indexOf('"') >= 0 ||
            s.equals("null") ||
            NCCSV_CHAR_ATT_PATTERN.matcher(s).matches() ||  //Looks Like A char
            NCCSV_LLA_NUMBER_PATTERN.matcher(s).matches())  //Looks Like A number
            return "\"" + sb.toString() + "\"";
        return sb.toString();
    }

    /**
     * This writes one string to an NCCSV DOS.
     */
/* project not finished or tested
    public static void writeNccsvDos(DataOutputStream dos, String s) throws IOException {
        byte bar[] = stringToUtf8Bytes(s);
        dos.writeInt(bar.length);
        dos.write(bar, 0, bar.length);
    }
*/
    /**
     * This encodes special characters in s if needed so that 
     * s can be stored as an item in a tsv string.
     */
    public static String toTsvString(String s) {
        if (s == null || s.length() == 0)
            return "";  //json would return "null"
        s = toJson(s);
        return s.substring(1, s.length() - 1); //remove enclosing quotes
    }

    /**
     * This takes a multi-line string (with \\r, \\n, \\r\\n line separators)
     *   and converts it into an ArrayList strings.
     * <ul>
     * <li> Only isPrintable and tab characters are saved.
     * <li> Each line separator generates another line.  So if last char
     *   is a line separator, it generates a blank line at the end.
     * <li> If s is "", this still generates 1 string ("").
     * </ul>
     *
     * @param s the string with internal line separators
     * @return an arrayList&lt;Strings&gt; (separate lines of text)
     */
    public static ArrayList<String> multiLineStringToArrayList(String s) {
        char endOfLineChar = s.indexOf('\n') >= 0? '\n' : '\r';
        int sLength = s.length();
        ArrayList<String> arrayList = new ArrayList(); //this is local, so okay if not threadsafe
        StringBuilder oneLine = new StringBuilder(512);
        char ch;
        int start = 0;
        for (int po = 0; po < sLength; po++) {
            ch = s.charAt(po);
            if (!(isPrintable(ch) || ch == '\t')) {
                //unprintable    
                //copy the accumulated printable chars
                oneLine.append(s.substring(start, po));
                start = po + 1;
                if (ch == endOfLineChar) {
                    //so it catches *the* designated eol char (e.g., \r), 
                    //and ignores any other (e.g., \n)
                    arrayList.add(oneLine.toString());                
                    oneLine.setLength(0);
                } //else:  other characters are ignored
            }
        }
        arrayList.add(oneLine.toString());
        return arrayList;
    }

    /**
     * This creates an ArrayList with the objects from the enumeration.
     * WARNING: This does not have a sychronized block: if your enumeration
     *    needs thread-safety, wrap this call in somthing like 
     *    <tt>synchronized(enum) {String2.toArrayList(enum); }</tt>.
     *
     * @param e an enumeration
     * @return arrayList with the objects from the enumeration
     */
    public static ArrayList toArrayList(Enumeration e) {
        ArrayList al = new ArrayList();
        while (e.hasMoreElements()) 
            al.add(e.nextElement());
        return al;
    }

    /**
     * This creates an ArrayList from an Object[].
     *
     * @param objectArray an Object[]
     * @return arrayList with the objects
     */
    public static ArrayList toArrayList(Object objectArray[]) {
        int n = objectArray.length;
        ArrayList al = new ArrayList(n);
        for (int i = 0; i < n; i++)
            al.add(objectArray[i]);
        return al;
    }

    /**
     * This returns the standard Help : About message.
     *
     * @return the standard Help : About message
     */
    public static String standardHelpAboutMessage() {
        return
            //"This program includes open source com.cohort classes (version " + 
            //    version + "),\n" +
            //"Copyright(c) 2004 - 2007, CoHort Software.\n" +
            //"For more information, visit www.cohort.com.\n" +
            //"\n" + 
            //"This program is using\n" + 
            javaInfo() + "\n" +
            Math2.memoryString() + " " + Math2.xmxMemoryString();
    }


    /**
     * This replaces "{0}", "{1}", and "{2}" in msg with s0, s1, s2.
     *
     * @param msg a string which may contain "{0}", "{1}", and/or "{2}".
     * @param s0 the first substitution string. If null, that substitution
     *     won't be attempted.
     * @param s1 the second substitution string. If null, that substitution
     *     won't be attempted.
     * @param s2 the third substitution string. If null, that substitution
     *     won't be attempted.
     * @return the modified msg
     */
    public static String substitute(String msg, String s0, String s1, String s2) {
        StringBuilder msgSB = new StringBuilder(msg);
        if (s0 != null) 
            replaceAll(msgSB, "{0}", s0); 
        if (s1 != null) 
            replaceAll(msgSB, "{1}", s1); 
        if (s2 != null) 
            replaceAll(msgSB, "{2}", s2); 
        return msgSB.toString();
    }

    /**
     * This returns a CSV (not CSSV) String.
     */
    public static String toCSVString(Enumeration en) {
        return toSVString(toArrayList(en).toArray(), ",", false);
    }
    public static String toCSVString(ArrayList al) {
        return toSVString(al.toArray(), ",", false);
    }
    public static String toCSVString(Vector v) {
        return toSVString(v.toArray(), ",", false);
    }
    public static String toCSVString(Object ar[]) {
        return toSVString(ar, ",", false);
    }
    public static String toCSVString(Set set) {
        Object ar[] = set.toArray();
        Arrays.sort(ar, new StringComparatorIgnoreCase());
        return toCSVString(ar);
    }

    /**
     * Generates a Comma-Space-Separated-Value (CSSV) string.  
     * <p>WARNING: This does not have a sychronized block: if your enumeration
     *   needs thread-safety, wrap this call in somthing like 
     *   <tt>synchronized (enum) {String2.toArrayList(enum); }</tt>.
     * <p>CHANGED: before 2011-03-06, this didn't do anything special for 
     *   strings with internal commas or quotes. Now it uses toJson for that string.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param en an enumeration of objects
     * @return a CSSV String with the values with ", " after
     *    all but the last value.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toCSSVString(Enumeration en) {
        return toSVString(toArrayList(en).toArray(), ", ", false);
    }

    /**
     * Generates a Comma-Space-Separated-Value (CSSV) string.  
     * <p>CHANGED: before 2011-03-06, this didn't do anything special for 
     *   strings with internal commas or quotes. Now it uses toJson for that string.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param al an arrayList of objects
     * @return a CSV String with the values with ", " after
     *    all but the last value.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toCSSVString(ArrayList al) {
        return toSVString(al.toArray(), ", ", false);
    }

    /**
     * Generates a Comma-Space-Separated-Value (CSSV) string.  
     * <p>CHANGED: before 2011-03-06, this didn't do anything special for 
     *   strings with internal commas or quotes. Now it uses toJson for that string.
     *
     * @param v a vector of objects
     * @return a CSSV String with the values with ", " after
     *    all but the last value.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toCSSVString(Vector v) {
        return toSVString(v.toArray(), ", ", false);
    }

    /**
     * Generates a Comma-Space-Separated-Value (CSSV) string.  
     * <p>CHANGED: before 2011-03-06, this didn't do anything special for 
     *   strings with internal commas or quotes. Now it uses toJson for that string.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of objects
     * @return a CSSV String with the values with ", " after
     *    all but the last value.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toCSSVString(Object ar[]) {
        return toSVString(ar, ", ", false);
    }

    /**
     * Generates a sorted (ignoreCase) Comma-Space-Separated-Value (CSSV) string
     * with the string version of each .  
     *
     * @param set
     * @return a CSSV String with the values with ", " after
     *    all but the last value.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toCSSVString(Set set) {
        Object ar[] = set.toArray();
        Arrays.sort(ar, new StringComparatorIgnoreCase());
        return toCSSVString(ar);
    }


    /**
     * Generates a space-separated-value string.  
     * <p>WARNING: This is simplistic. It doesn't do anything special for 
     *   strings with internal spaces.
     *
     * @param ar an array of objects
     *    (for an ArrayList or Vector, use o.toArray())
     * @return a SSV String with the values with " " after
     *    all but the last value.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toSSVString(Object ar[]) {
        return toSVString(ar, " ", false);
    }

    /**
     * Generates a tab-separated-value string.  
     * <p>WARNING: This is simplistic. It doesn't do anything special for 
     *   strings with internal tabs.
     *
     * @param ar an array of objects
     *    (for an ArrayList or Vector, use o.toArray())
     * @return a TSV String with the values with "\t" after
     *    all but the last value.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toTSVString(Object ar[]) {
        return toSVString(ar, "\t", false);
    }

    /**
     * Generates a newline-separated string, with a newline at the end.
     * <p>WARNING: This is simplistic. It doesn't do anything special for 
     *   strings with internal newlines.
     *
     * @param ar an array of objects
     *    (for an ArrayList or Vector, use o.toArray())
     * @return a String with the values, 
     *    with a '\n' after each value, even the last.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toNewlineString(Object ar[]) {
        return toSVString(ar, "\n", true);
    }

    /**
     * This is used at a low level to generate a 
     * 'separator'-separated-value string (without newlines) 
     * with the element.toString()'s from the array.
     *
     * @param ar an array of objects
     *    (for an ArrayList or Vector, use o.toArray())
     * @param separator the separator string
     * @param finalSeparator if true, a separator will be added to the 
     *    end of the resulting string (if it isn't "").
     * @return a separator-separated-value String.
     *    Returns null if ar is null.
     *    null elements are represented as "[null]".
     */
    public static String toSVString(Object ar[], String separator, boolean finalSeparator) {
        if (ar == null) 
            return null;
        int n = ar.length;
        boolean csv = separator.charAt(0) == ',';
        //8 bytes is lame estimate of bytes/element
        StringBuilder sb = new StringBuilder(8 * Math.min(n, (Integer.MAX_VALUE-8192) / 8));
        for (int i = 0; i < n; i++) {
            if (i > 0)
                sb.append(separator);
            Object o = ar[i];
            if (o == null) 
                sb.append("[null]");
            else {
                String s = o.toString();
                if (csv && (s.indexOf(',') >= 0 || s.indexOf('"') >= 0))
                    s = toJson(s);
                sb.append(s);
            }
        }
        if (finalSeparator && n > 0)
            sb.append(separator);
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of boolean
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(boolean ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 7 bytes/element
        StringBuilder sb = new StringBuilder(7 * Math.min(n, (Integer.MAX_VALUE-8192) / 7));
        for (int i = 0; i < n; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(ar[i]);
        }
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of bytes
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(byte ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 5 bytes/element
        StringBuilder sb = new StringBuilder(5 * Math.min(n, (Integer.MAX_VALUE-8192) / 5));
        for (int i = 0; i < n; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(ar[i]);
        }
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * (chars are treated as unsigned shorts).
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of char
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(char ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 6 bytes/element
        StringBuilder sb = new StringBuilder(6 * Math.min(n, (Integer.MAX_VALUE-8192) / 6));
        for (int i = 0; i < n; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(toNccsvDataString("" + ar[i]));  //safe char to int type conversion
        }
        return sb.toString();
    }

    /** 
     * This returns the int formatted as a 0x hex String with at least nHexDigits, 
     * e.g., 0x00FF00.
     * Negative numbers are twos compliment, e.g., -4 -&gt; 0xfffffffc.
     */
    public static String to0xHexString(int i, int nHexDigits) {
        return "0x" + zeroPad(Integer.toHexString(i), nHexDigits);
    }

    /**
     * This generates a hexadecimal Comma-Space-Separated-Value (CSSV) String from the array.
     * Negative numbers are twos compliment, e.g., -4 -&gt; 0xfc.
     * <p>CHANGED: before 2011-09-04, this was called toHexCSVString.
     *
     * @param ar an array of bytes
     * @return a CSSV String (or null if ar is null)
     */
    public static String toHexCSSVString(byte ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 6 bytes/element
        StringBuilder sb = new StringBuilder(6 * Math.min(n, (Integer.MAX_VALUE-8192) / 6));
        for (int i = 0; i < n; i++) {
            if (i > 0)
                sb.append(", ");
            String s = Integer.toHexString(ar[i]);
            if (s.length() == 8 && s.startsWith("ffffff"))
                s = s.substring(6);
            sb.append("0x" + s);
        }
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of shorts
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(short ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 7 bytes/element
        StringBuilder sb = new StringBuilder(7 * Math.min(n, (Integer.MAX_VALUE-8192) / 7));
        for (int i = 0; i < n; i++) {
            sb.append(ar[i]);
            if (i < n - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This generates a hexadecimal Comma-Space-Separated-Value (CSSV) String from the array.
     * Negative numbers are twos compliment, e.g., -4 -&gt; 0xfffc.
     * <p>CHANGED: before 2011-09-04, this was called toHexCSVString.
     *
     * @param ar an array of short
     * @return a CSSV String (or null if ar is null)
     */
    public static String toHexCSSVString(short ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 8 bytes/element
        StringBuilder sb = new StringBuilder(8 * Math.min(n, (Integer.MAX_VALUE-8192) / 8));
        for (int i = 0; i < n; i++) {
            String s = Integer.toHexString(ar[i]);
            if (s.length() == 8 && s.startsWith("ffff"))
                s = s.substring(4);
            sb.append("0x" + s);
            if (i < n - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of ints
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(int ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 8 bytes/element
        StringBuilder sb = new StringBuilder(8 * Math.min(n, (Integer.MAX_VALUE-8192) / 8));
        for (int i = 0; i < n; i++) {
            sb.append(ar[i]);
            if (i < n - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This generates a hexadecimal Comma-Space-Separated-Value (CSSV) String from the array.
     * Negative numbers are twos compliment, e.g., -4 -&gt; 0xfffffffc.
     * <p>CHANGED: before 2011-09-04, this was called toHexCSVString.
     *
     * @param ar an array of ints
     * @return a CSSV String (or null if ar is null)
     */
    public static String toHexCSSVString(int ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 12 bytes/element
        StringBuilder sb = new StringBuilder(12 * Math.min(n, (Integer.MAX_VALUE-8192) / 12));
        for (int i = 0; i < n; i++) {
            sb.append("0x" + Integer.toHexString(ar[i]));
            if (i < n - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of longs
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(long ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 12 bytes/element
        StringBuilder sb = new StringBuilder(12 * Math.min(n, (Integer.MAX_VALUE-8192) / 12));
        for (int i = 0; i < n; i++) {
            sb.append(ar[i]);
            if (i < n - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of float
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(float ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 12 bytes/element
        StringBuilder sb = new StringBuilder(12 * Math.min(n, (Integer.MAX_VALUE-8192) / 12));
        for (int i = 0; i < n; i++) {
            sb.append(ar[i]);
            if (i < n - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This generates a Comma-Space-Separated-Value (CSSV) String from the array.
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param ar an array of double
     * @return a CSSV String (or null if ar is null)
     */
    public static String toCSSVString(double ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 12 bytes/element
        StringBuilder sb = new StringBuilder(12 * Math.min(n, (Integer.MAX_VALUE-8192) / 12));
        for (int i = 0; i < n; i++) {
            sb.append(ar[i]);
            if (i < n - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * This generates a newline-separated (always '\n') String from the array.
     *
     * @param ar an array of ints
     * @return a newline-separated String (or null if ar is null)
     */
    public static String toNewlineString(int ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 12 bytes/element
        StringBuilder sb = new StringBuilder(12 * Math.min(n, (Integer.MAX_VALUE-8192) / 12));
        for (int i = 0; i < n; i++) {
            sb.append(ar[i]);
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * This generates a newline-separated (always '\n') String from the array.
     *
     * @param ar an array of double
     * @return a newline-separated String (or null if ar is null)
     */
    public static String toNewlineString(double ar[]) {
        if (ar == null) 
            return null;
        int n = ar.length;
        //estimate 12 bytes/element
        StringBuilder sb = new StringBuilder(12 * Math.min(n, (Integer.MAX_VALUE-8192) / 12));
        for (int i = 0; i < n; i++) {
            sb.append(ar[i]);
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * This converts an Object[] into a String[].
     * If you have an ArrayList or a Vector, use arrayList.toArray().
     *
     * @param aa
     * @return the corresponding String[] by calling toString() for each object
     */
    public static String[] toStringArray(Object aa[]) {
        if (aa == null)
            return null;
        int n = aa.length;
        Math2.ensureMemoryAvailable(8L * n, "String2.toStringArray"); //8L is lame estimate of bytes/element
        String sa[] = new String[n];
        for (int i = 0; i < n; i++) {
            Object o = aa[i];
            sa[i] = o == null? (String)o : o.toString();
        }
        return sa;
    }

    /**
     * Add the items in the array (if any) to the arrayList.
     *
     * @param arrayList
     * @param ar the items to be added
     */
    public static void add(ArrayList arrayList, Object ar[]) {
        if (arrayList == null || ar == null) 
            return;
        int n = ar.length;
        for (int i = 0; i < n; i++)
            arrayList.add(ar[i]);
    }

    /**
     * This displays the contents of a bitSet as a String.
     *
     * @param bitSet
     * @return the corresponding String (the 'true' bits, comma separated)
     */
    public static String toString(BitSet bitSet) {
        if (bitSet == null)
            return null;
        StringBuilder sb = new StringBuilder(1024);

        String separator = "";
        int i = bitSet.nextSetBit(0);
        while (i >= 0) {
            sb.append(separator + i);
            separator = ", ";
            i = bitSet.nextSetBit(i + 1);
        }
        return sb.toString();
    }

    /**
     * This displays the contents of a map as a String.
     * See also StringArray(Map)
     *
     * @param map  if it needs to be thread-safe, use ConcurrentHashMap
     * @return the corresponding String, with one entry on each line 
     *    (key = value) sorted (case insensitive) by key
     */
    public static String toString(Map map) {
        if (map == null)
            return null;
        StringBuilder sb = new StringBuilder(1024);

        Set entrySet = map.entrySet();
        Iterator it = entrySet.iterator();
        while (it.hasNext()) {
            Map.Entry me = (Map.Entry)it.next();
            sb.append(me.getKey().toString() + " = " + me.getValue().toString() + "\n");
        }
        return sb.toString();
    }

    /**
     * From an arrayList which alternates attributeName (a String) and 
     * attributeValue (an object), this generates a String with 
     * "    name=value" on each line.
     * If arrayList == null, this returns "    [null]\n".
     *
     * @param arrayList 
     * @return the desired string representation
     */
    public static String alternateToString(ArrayList arrayList) {
        if (arrayList == null)
            return "    [null]\n";
        int n = arrayList.size();
        //estimate 32 bytes/element
        StringBuilder sb = new StringBuilder(32 * Math.min(n, (Integer.MAX_VALUE-8192) / 32));
        for (int i = 0; i < n; i += 2) {
            sb.append("    ");
            sb.append(arrayList.get(i).toString());
            sb.append('=');
            sb.append(arrayToCSSVString(arrayList.get(i+1)));
            sb.append('\n');
        }
        return sb.toString();
    }


    /**
     * From an arrayList which alternates attributeName (a String) and 
     * attributeValue (an object), this an array of attributeNames.
     * If arrayList == null, this returns "    [null]\n".
     *
     * @param arrayList 
     * @return the attributeNames in the arrayList
     */
    public static String[] alternateGetNames(ArrayList arrayList) {
        if (arrayList == null)
            return null;
        int n = arrayList.size();
        String[] sar = new String[n / 2];
        int i2 = 0;
        for (int i = 0; i < n / 2; i++) {
            sar[i] = arrayList.get(i2).toString();
            i2 += 2;
        }
        return sar;
    }
    /**
     * From an arrayList which alternates attributeName (a String) and 
     * attributeValue (an object), this returns the attributeValue
     * associated with the supplied attributeName.
     * If array == null or there is no matching value, this returns null.
     *
     * @param arrayList 
     * @param attributeName
     * @return the associated value
     */
    public static Object alternateGetValue(ArrayList arrayList, String attributeName) {
        if (arrayList == null)
            return null;
        int n = arrayList.size();
        for (int i = 0; i < n; i += 2) {
            if (arrayList.get(i).toString().equals(attributeName))
                return arrayList.get(i + 1);
        }
        return null;
    }

    /**
     * Given an arrayList which alternates attributeName (a String) and 
     * attributeValue (an object), this either removes the attribute
     * (if value == null), adds the attribute and value (if it isn't in the list),
     * or changes the value (if the attriubte is in the list).
     *
     * @param arrayList 
     * @param attributeName
     * @param value the value associated with the attributeName
     * @return the previous value for the attribute (or null)
     * @throws RuntimeException of trouble (e.g., if arrayList is null)
     */
    public static Object alternateSetValue(ArrayList arrayList, 
            String attributeName, Object value) {
        if (arrayList == null)
            throw new SimpleException(ERROR + " in String2.alternateSetValue: arrayList is null.");
        int n = arrayList.size();
        for (int i = 0; i < n; i += 2) {
            if (arrayList.get(i).toString().equals(attributeName)) {
                Object oldValue = arrayList.get(i + 1);
                if (value == null) {
                    arrayList.remove(i + 1); //order of removal is important
                    arrayList.remove(i);
                }
                else arrayList.set(i+1, value);
                return oldValue;
            }
        }

        //attributeName not found? 
        if (value == null)
            return null;
        else {
            //add it
            arrayList.add(attributeName);
            arrayList.add(value);
            return null;
        }
    }

    /**
     * This returns a nice String representation of the attribute value
     * (which should be a String or an array of primitives).
     * <p>CHANGED: before 2011-09-04, this was called toCSVString.
     *
     * @param value
     * @return a nice String representation
     */
    public static String arrayToCSSVString(Object value) {
        if (value instanceof byte[])   return toCSSVString((byte[])value);
        if (value instanceof char[])   return toCSSVString((char[])value);
        if (value instanceof short[])  return toCSSVString((short[])value);
        if (value instanceof int[])    return toCSSVString((int[])value);
        if (value instanceof long[])   return toCSSVString((long[])value);
        if (value instanceof float[])  return toCSSVString((float[])value);
        if (value instanceof double[]) return toCSSVString((double[])value);
        if (value instanceof String[]) return toCSSVString((String[])value);
        if (value instanceof Object[]) return toCSSVString((Object[])value);
        return value.toString();
    }

    /**
     * This extracts the lower 8 bits of each char to form a
     * byte array.
     *
     * @param s a String
     * @return the corresponding byte[] (or null if s is null)
     */
    public static byte[] toByteArray(String s) {
        if (s == null)
            return null;
        int sLength = s.length();
        byte[] ba = new byte[sLength];
        for (int i = 0; i < sLength; i++) {
            char c = s.charAt(i);  //2016-11-29 I added: char>255 -> '?', it's better than just low 8 bits
            ba[i] = (byte)(c < 256? c : '?'); 
        }
        return ba;
    }

    /**
     * This extracts the lower 8 bits of each char to form a
     * byte array.
     *
     * @param sb a StringBuilder
     * @return the corresponding byte[] (or null if s is null)
     */
    public static byte[] toByteArray(StringBuilder sb) {
        if (sb == null)
            return null;
        int sbLength = sb.length();
        byte[] ba = new byte[sbLength];
        for (int i = 0; i < sbLength; i++) {
            char c = sb.charAt(i);  //2016-11-29 I added: char>255 -> 255, it's better than just low 8 bits
            ba[i] = (byte)(c < 256? c : 255); 
        }
        return ba;
    }

    /**
     * This creates a String which displays the bytes in hex, 16 per line.
     *
     * @param byteArray   perhaps from toByteArray(s)
     * @return the hex dump of the bytes (or null if byteArray is null).
     *    Each line will be 71 chars long (char#71 will be newline).
     */
    public static String hexDump(byte[] byteArray) {
        int bal = byteArray.length;
        StringBuilder printable = new StringBuilder(32);
        //~5 bytes/element
        StringBuilder sb = new StringBuilder(5 * Math.min(bal, (Integer.MAX_VALUE-8192) / 5));
        int i;
        for (i = 0; i < bal; i++) {
            int data = byteArray[i] & 255;
            sb.append(zeroPad(Integer.toHexString(data), 2) + " ");
            printable.append(data >= 32 && data <= 126? (char)data: ' '); 
            if (i % 8 == 7) 
                sb.append("  ");
            if (i % 16 == 15) {
                sb.append(printable + " |\n");
                printable.setLength(0);
            }

        }
        if (byteArray.length % 16 != 0) {
            sb.append(printable);
            sb.append(makeString(' ', 69 - sb.length() % 71));
            sb.append("|\n");
        }

        return sb.toString();
    }

    /**
     * This finds the first element in Object[]  (starting at element startAt)
     * where ar[i]==o.
     *
     * @param ar the array of Objects
     * @param o the String to be found
     * @param startAt the first element of ar to be checked.
     *    If startAt &lt; 0, this starts with startAt = 0.
     *    If startAt &gt;= ar.length, this returns -1.
     * @return the element number of ar which is equal to s (or -1 if ar is null, or s is null or not found)
     */
    public static int indexOfObject(Object[] ar, Object o, int startAt) {
        if (ar == null || o == null)
            return -1;
        int n = ar.length;
        for (int i = Math.max(0, startAt); i < n; i++)
            if (ar[i] != null && ar[i] == o)  
                return i;
        return -1;
    }

    /** A variant of indexOfObject() that uses startAt=0. */
    public static int indexOfObject(Object[] ar, Object o) {
        return indexOfObject(ar, o, 0);
    }


    /**
     * This finds the first element in Object[] 
     * where the ar[i].toString value equals to s.
     *
     * @param ar the array of Objects (Strings?)
     * @param s the String to be found
     * @return the element number of ar which is equal to s (or -1 if ar is null, or s is null or not found)
     */
    public static int indexOf(Object[] ar, String s) {
        return indexOf(ar, s, 0);
    }

    /**
     * This finds the first element in Object[]  (starting at element startAt)
     * where the ar[i].toString value equals s.
     *
     * @param ar the array of Objects
     * @param s the String to be found
     * @param startAt the first element of ar to be checked.
     *    If startAt &lt; 0, this starts with startAt = 0.
     *    If startAt &gt;= ar.length, this returns -1.
     * @return the element number of ar which is equal to s (or -1 if ar is null, or s is null or not found)
     */
    public static int indexOf(Object[] ar, String s, int startAt) {
        if (ar == null || s == null)
            return -1;
        int n = ar.length;
        for (int i = Math.max(0, startAt); i < n; i++)
            if (ar[i] != null && s.equals(ar[i].toString()))  
                return i;
        return -1;
    }

    /**
     * This finds the first element in Object[] 
     * where ar[i].toString().toLowerCase() equals to s.toLowerCase().
     *
     * @param ar the array of Objects
     * @param s the String to be found
     * @return the element number of ar which is equal to s (or -1 if s is null or not found)
     */
    public static int caseInsensitiveIndexOf(Object[] ar, String s) {
        if (ar == null || s == null)
            return -1;
        int n = ar.length;
        s = s.toLowerCase();
        for (int i = 0; i < n; i++)
            if (ar[i] != null && s.equals(ar[i].toString().toLowerCase()))  
                return i;
        return -1;
    }

    /**
     * This finds the first element in Object[] 
     * where the ar[i].toString value contains the substring s.
     *
     * @param ar the array of objects
     * @param s the String to be found
     * @return the element number of ar which is equal to s (or -1 if not found)
     */
    public static int lineContaining(Object[] ar, String s) {
        return lineContaining(ar, s, 0);
    }

    /**
     * This finds the first element in Object[] (starting at element startAt)
     * where the ar[i].toString value contains the substring s.
     *
     * @param ar the array of objects
     * @param s the String to be found
     * @param startAt the first element of ar to be checked.
     *    If startAt &lt; 0, this starts with startAt = 0.
     * @return the element number of ar which is equal to s (or -1 if not found)
     */
    public static int lineContaining(Object[] ar, String s, int startAt) {
        if (ar == null || s == null)
            return -1;
        int n = ar.length;
        for (int i = Math.max(0, startAt); i < n; i++)
            if (ar[i] != null && ar[i].toString().indexOf(s) >= 0) 
                return i;
        return -1;
    }

    /**
     * This finds the first element in Object[] (starting at element startAt)
     * where the ar[i].toString value contains the substring s (ignoring
     * the case of ar and s).
     *
     * @param ar the array of objects
     * @param s the String to be found
     * @param startAt the first element of ar to be checked.
     *    If startAt &lt; 0, this starts with startAt = 0.
     * @return the element number of ar which is equal to s (or -1 if not found)
     */
    public static int lineContainingIgnoreCase(Object[] ar, String s, int startAt) {
        if (ar == null || s == null)
            return -1;
        int n = ar.length;
        s = s.toLowerCase();
        for (int i = Math.max(0, startAt); i < n; i++)
            if (ar[i] != null && ar[i].toString().toLowerCase().indexOf(s) >= 0) 
                return i;
        return -1;
    }

    /**
     * This returns the first element in Object[] (starting at element 0)
     * where the ar[i].toString value starts with s.
     *
     * @param ar the array of objects
     * @param s the String to be found
     * @return the first element ar (as a String) which starts with s (or null if not found)
     */
    public static String stringStartsWith(Object[] ar, String s) {
        int i = lineStartsWith(ar, s, 0);
        return i < 0? null : ar[i].toString();
    }

    /**
     * This finds the first element in Object[] (starting at element 0)
     * where the ar[i].toString value starts with s.
     *
     * @param ar the array of objects
     * @param s the String to be found
     * @return the element number of ar which starts with s (or -1 if not found)
     */
    public static int lineStartsWith(Object[] ar, String s) {
        return lineStartsWith(ar, s, 0);
    }

    /**
     * This finds the first element in Object[] (starting at element startAt)
     * where the ar[i].toString value starts with s.
     *
     * @param ar the array of objects
     * @param s the String to be found
     * @param startAt the first element of ar to be checked.
     *    If startAt &lt; 0, this starts with startAt = 0.
     * @return the element number of ar which starts with s (or -1 if not found)
     */
    public static int lineStartsWith(Object[] ar, String s, int startAt) {
        if (ar == null || s == null)
            return -1;
        int n = ar.length;
        for (int i = Math.max(0, startAt); i < n; i++)
            if (ar[i] != null && ar[i].toString().startsWith(s)) 
                return i;
        return -1;
    }

    /**
     * This variant of lineStartsWith startsAt index=0.
     *
     * @param ar the array of objects, e.g., including LATITUDE
     * @param s the String to be found, e.g., Lat
     * @return the element number of ar which starts with s (or -1 if not found)
     */
    public static int lineStartsWithIgnoreCase(Object[] ar, String s) {
        return lineStartsWithIgnoreCase(ar, s, 0);
    }

    /**
     * This is like lineStartsWith, but ignores case.
     *
     * @param ar the array of objects, e.g., including LATITUDE
     * @param s the String to be found, e.g., Lat
     * @param startAt the first element of ar to be checked.
     *    If startAt &lt; 0, this starts with startAt = 0.
     * @return the element number of ar which starts with s (or -1 if not found)
     */
    public static int lineStartsWithIgnoreCase(Object[] ar, String s, int startAt) {
        if (ar == null || s == null)
            return -1;
        s = s.toLowerCase();
        int n = ar.length;
        for (int i = Math.max(0, startAt); i < n; i++)
            if (ar[i] != null && ar[i].toString().toLowerCase().startsWith(s)) 
                return i;
        return -1;
    }

    /**
     * This finds the first element in prefixes (starting at element startAt)
     * where the longerString starts with prefixes[i].
     *
     * @param prefixes the array of prefixes
     * @param longerString the String that might start with one of the prefixes
     * @param startAt the first element of ar to be checked.
     *    If startAt &lt; 0, this starts with startAt = 0.
     * @return the element number of prefixes which longerString starts with (or -1 if not found)
     */
    public static int whichPrefix(String[] prefixes, String longerString, int startAt) {
        if (prefixes == null || longerString == null || longerString.length() == 0)
            return -1;
        int n = prefixes.length;
        for (int i = Math.max(0, startAt); i < n; i++)
            if (prefixes[i] != null && longerString.startsWith(prefixes[i])) 
                return i;
        return -1;
    }

    /**
     * This is like whichPrefix, but returns the found prefix (or null).
     *
     * @param prefixes the array of prefixes
     * @param longerString the String that might start with one of the prefixes
     * @param startAt the first element of ar to be checked.
     *    If startAt &lt; 0, this starts with startAt = 0.
     * @return the prefixes[i] which longerString starts with (or null if not found)
     */
    public static String findPrefix(String[] prefixes, String longerString, int startAt) {
        int i = whichPrefix(prefixes, longerString, startAt);
        return i < 0? null : prefixes[i];
    }

    /**
     * This finds the first element in suffixes (starting at element startAt)
     * where the longerString ends with suffixes[i].
     *
     * @param suffixes the array of suffixes
     * @param longerString the String that might end with one of the suffixes
     * @param startAt the first element of ar to be checked.
     *    If startAt &lt; 0, this starts with startAt = 0.
     * @return the element number of suffixes which longerString ends with (or -1 if not found)
     */
    public static int whichSuffix(String[] suffixes, String longerString, int startAt) {
        if (suffixes == null || longerString == null || longerString.length() == 0)
            return -1;
        int n = suffixes.length;
        for (int i = Math.max(0, startAt); i < n; i++)
            if (suffixes[i] != null && longerString.endsWith(suffixes[i])) 
                return i;
        return -1;
    }

    /**
     * This tells Commons Logging to use com.cohort.util.String2Log.
     * I use this at the beginning of my programs 
     * (TestAll, NetCheck, Browser, ConvertTable, DoubleCenterGrids)
     * to route Commons Logging requests through String2Log.
     * !!!Don't use this in lower level methods as it will hijack the 
     * parent program's (e.g., Armstrong's) logger setup.
     *
     * param level a String2Log.XXX_LEVEL constant (or -1 to leave unchanged,
     *     default=String2Log.WARN_LEVEL)
     */
    public static void setupCommonsLogging(int level) {
        //By setting this property, I specify that String2LogFactory
        //  will be used to generate logFactories.
        //  (It makes one String2Log, which sends all messages to String2.log.)
        System.setProperty("org.apache.commons.logging.LogFactory", 
            "com.cohort.util.String2LogFactory");
        if (level >= 0) {
            System.setProperty("com.cohort.util.String2Log.level", "" + level);
        } else {
            if (System.getProperty("com.cohort.util.String2Log.level") == null)
                System.setProperty("com.cohort.util.String2Log.level", 
                    "" + String2Log.WARN_LEVEL);
        }

        //this dummy variable ensures String2LogFactory gets compiled 
        String2LogFactory string2LogFactory;

    }

    /**
     * This changes the log system set up.
     * The default log prints to System.err.
     * Use the logger by calling String2.log(msg); 
     *
     * @param tLogToSystemOut indicates if info should be printed to System.out (default = true).
     * @param tLogToSystemErr indicates if info should be printed to System.err (default = false).
     * @param fullFileName the name for the log file (or "" for none).
     * @param append If a previous log file of the same name exists,
     *   this determines whether a new log file should be created
     *   or whether info should be appended to the old file.
     * @param tLogFileMaxSize determines the approximate max size (in bytes) of the log file.
     *   When maxSize is reached, the current log file is copied to 
     *   fullFileName.previous, and a new fullFileName is created. 
     *   Specify 0 for no limit to the size.    
     * @throws Exception if trouble
     */
    public static synchronized void setupLog(
        boolean tLogToSystemOut, boolean tLogToSystemErr,
        String fullFileName, //boolean logToStringBuilder, 
        boolean append, int tLogFileMaxSize) 
        throws Exception {

        String oLogFileName = logFileName;
        logToSystemOut = tLogToSystemOut;
        logToSystemErr = tLogToSystemErr;

        if (!append)
            logFileSize = 0;
        logFileMaxSize = Math2.minMax(Math2.BytesPerMB, 2000000000, tLogFileMaxSize);

        //close the old file
        closeLogFile(); //it synchronizes on logFileBuilder

        //if no file name, return
        if (fullFileName.length() == 0) {
            if (oLogFileName != null && oLogFileName.length() > 0)
                log("*** closed logFile=" + oLogFileName + " at " + 
                    Calendar2.getCurrentISODateTimeStringLocalTZ());
            return;
        }

        //logFile: open the file
        //always synchronize on logFileLock
        synchronized(logFileLock) { 
            try {
                logFile = new BufferedWriter(new FileWriter(fullFileName, append)); //default charset
                logFileSize = Math2.narrowToInt((new File(fullFileName)).length());
                logFileName = fullFileName; //log file created, so assign logFileName
            } catch (Throwable t) {
                System.out.println(Calendar2.getCurrentISODateTimeStringZulu() +
                    " ERROR: while creating new logFile=" + fullFileName + "\n" +
                    MustBe.throwableToString(t));
            }
        }

        log("logFileMaxSize=" + logFileMaxSize);
    }

    /**
     * This closes the log file (if it exists and is open).
     * It is best if a crashing program calls this to ensure logFile is closed.
     */
    public static void closeLogFile() {
        try {
            if (logFile == null) {
                logFileName = null;
            } else {
                //always synchronize on logFileLock
                synchronized(logFileLock) {  
                    logFile.flush(); //be extra sure it is flushed
                    logFile.close();
                    logFile = null;
                    logFileName = null;
                }
            }
        } catch (Exception e) {
            logFile = null;
            logFileName = null;
        }
    }

    /**
     * This returns logging to just System.out.
     */
    public static void returnLoggingToSystemOut() {
        try {
            setupLog(true, false, "", false, logFileDefaultMaxSize);
        } catch (Throwable t2) {
            System.out.println(MustBe.throwableToString(t2));
        }
    }

    /**
     * This writes the specified message (with \n as line separator) to the log file,
     * appending \n at the end.
     * This will not throw an exception.
     *
     * @param message the message
     */
    public static void log(String message) {
        lowLog(message, true);
    }

    /**
     * This writes the specified message (with \n as line separator) to the log file
     * without appending \n at the end.
     * This will not throw an exception.
     *
     * @param message the message
     */
    public static void logNoNewline(String message) {
        lowLog(message, false);
    }

    /**
     * This writes the specified message (with \n as line separator) to the log file
     * without appending \n at the end.
     * This will not throw an exception.
     *
     * @param message the message
     */
    public static void lowLog(String message, boolean addNewline) {
        //Now each part is thread safe.
/*To test thread safety: 
1) In Erddap.java, set log file size to 1 byte (so default of 10K will be used)
2) run local erddap with standard test datasets
3) in a browser, in separate tabs, simultaneously load
http://localhost/cwexperimental/tabledap/erdGtsppBest.htmlTable?&depth=200&temperature=10
http://localhost/cwexperimental/tabledap/cwwcNDBCMet.htmlTable?&atmp=10&wtmp=10
http://localhost/cwexperimental/tabledap/pmelTaoDySst.htmlTable?&T_25=25
and zoom and pan with controls in 
  http://localhost/cwexperimental/wms/erdBAssta5day/index.html
*/
        try {
            //write to system.out and/or logFile 
            if (!lineSeparator.equals("\n"))
                message = replaceAll(message, "\n", lineSeparator);

            if (logToSystemOut) {
                if (addNewline) 
                     System.out.println(message); //it's synchronized
                else System.out.print(  message); //it's synchronized
            }

            if (logToSystemErr) {
                if (addNewline)
                     System.err.println(message); //it's synchronized
                else System.err.print(  message); //it's synchronized
            }

            if (logFile != null) {
                long ctm = System.currentTimeMillis();
                //always synchronize on logFileLock
                synchronized(logFileLock) {
                    //write the message to the logFile (common, fast)
                    logFile.write(message); //non-blocking
                    if (addNewline) 
                        logFile.write(lineSeparator); //non-blocking
                    logFileSize += message.length() + (addNewline? 1 : 0); //not crucial: true for Linux; underestimate if Windows

                    if (logFileSize >= logFileMaxSize) {
                        //is the file too big?
                        //time to roll over log file to .previous
                        //rare, slow
                        logFile.close();
                        logFile = null; //was: otherwise, infinite loop if File2.rename calls String2.log
                        logFileSize = 0;
                        File2.safeRename(logFileName, logFileName + ".previous"); //won't throw exception
                        try {
                            logFile = new BufferedWriter(new FileWriter(logFileName));
                        } catch (Throwable t) {
                            //try again: really bad if unable to create a new logFile
                            Math2.gc(1000);
                            try {
                                logFile = new BufferedWriter(new FileWriter(logFileName));
                            } catch (Throwable t2) {
                                System.out.println(Calendar2.getCurrentISODateTimeStringZulu() +
                                    " ERROR: while creating new logFile=" + logFileName + "\n" +
                                    MustBe.throwableToString(t));
                                logFileName = null; //logFile disabled
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            //eek! what should I do?
        }
    }

    /**
     * This flushes the log system.
     * In practice, this just flushes the logFile (if any), 
     * since other log destinations (e.g., System.out) are flushed automatically every time.
     * This will not throw an exception.
     */
    public static void flushLog() {
        if (logFile != null) {
            try {
                //always synchronize on logFileLock
                synchronized(logFileLock) {
                    logFile.flush(); 
                }
            } catch (Exception e) {
                //do nothing
            }
        }
    }

    /**
     * This returns the logFileName (or null if none).
     *
     * @return the logFileName (or null if none)
     */
    public static String logFileName() {
        return logFileName;
    }

    /**
     * This splits the string at the specified character.
     * Leading and trailing whitespace is removed.
     * A missing final strings is treated as "" (not discarded as with String.split).
     * 
     * @param s a string with 0 or more separator chatacters
     * @param separator
     * @return an ArrayList of strings.
     *   s=null returns null.
     *   s="" returns ArrayList with one value: "".
     */
    public static ArrayList<String> splitToArrayList(String s, char separator) {
        return splitToArrayList(s, separator, true);
    }

    /**
     * This splits the string at the specified character.
     * A missing final string is treated as "" (not discarded as with String.split).
     * 
     * @param s a string with 0 or more separator chatacters
     * @param separator
     * @param trim  trim the substrings, or don't
     * @return an ArrayList of strings (not canonical).
     *   s=null returns null.
     *   s="" returns ArrayList with one value: "".
     */
    public static ArrayList<String> splitToArrayList(String s, char separator, boolean trim) {
        if (s == null) 
            return null;

        //go through the string looking for separators
        ArrayList al = new ArrayList();
        int sLength = s.length();
        int start = 0;
        //log("split line=" + annotatedString(s));
        for (int index = 0; index < sLength; index++) {
            if (s.charAt(index) == separator) {
                String ts = s.substring(start, index);
                if (trim) ts = ts.trim();
                al.add(ts);
                start = index + 1;
            }
        }

        //add the final substring
        String ts = s.substring(start, sLength); //start == sLength? "" : s.substring(start, sLength);
        if (trim) ts = ts.trim();
        al.add(ts);
        //log("al.size=" + al.size() + "\n");
        return al;
    }

    /**
     * This splits the string at the specified character.
     * The substrings are trim'd.
     * A missing final string is treated as "" (not discarded as with String.split).
     * 
     * @param s a string with 0 or more separator chatacters
     * @param separator
     * @return a String[] with the strings (not canonical).
     *   s=null returns null.
     *   s="" returns String[1]{""}.
     */
    public static String[] split(String s, char separator) {
        ArrayList<String> al = splitToArrayList(s, separator, true);
        if (al == null)
            return null;
        return al.toArray(new String[0]);
    }

    /**
     * This splits the string at the specified character.
     * A missing final string is treated as "" (not discarded as with String.split).
     * 
     * @param s a string with 0 or more separator chatacters
     * @param separator
     * @return a String[] with the strings (not canonical).
     *   s=null returns null.
     *   s="" returns String[1]{""}.
     */
    public static String[] splitNoTrim(String s, char separator) {
        ArrayList<String> al = splitToArrayList(s, separator, false);
        if (al == null)
            return null;
        return al.toArray(new String[0]);
    }

    /**
     * This converts an Object[] (for example, where objects are Strings or 
     *  Integers) into an int[].
     *
     * @param oar an Object[]
     * @return the corresponding int[]  (invalid values are converted to Integer.MAX_VALUE).
     *   oar=null returns null.
     */
    public static int[] toIntArray(Object oar[]) {
        if (oar == null)
            return null;
        int n = oar.length;
        Math2.ensureMemoryAvailable(4L * n, "String2.toIntArray"); 
        int ia[] = new int[n];
        for (int i = 0; i < n; i++)
            ia[i] = parseInt(oar[i].toString());
        return ia;
    }

    /**
     * This converts an Object[] (for example, where objects are Strings or 
     *  Floats) into a float[].
     *
     * @param oar an Object[]
     * @return the corresponding float[] (invalid values are converted to Float.NaN).
     *   oar=null returns null.
     */
    public static float[] toFloatArray(Object oar[]) {
        if (oar == null)
            return null;
        int n = oar.length;
        Math2.ensureMemoryAvailable(4L * n, "String2.toFloatArray"); 
        float fa[] = new float[n];
        for (int i = 0; i < n; i++)
            fa[i] = parseFloat(oar[i].toString());
        return fa;
    }

    /**
     * This converts an Object[] (for example, where objects are Strings or 
     *  Doubles) into a double[].
     *
     * @param oar an Object[]
     * @return the corresponding double[] (invalid values are converted to Double.NaN).
     *   oar=null returns null.
     */
    public static double[] toDoubleArray(Object oar[]) {
        if (oar == null)
            return null;
        int n = oar.length;
        Math2.ensureMemoryAvailable(8L * n, "String2.toDoubleArray"); 
        double da[] = new double[n];
        for (int i = 0; i < n; i++)
            da[i] = parseDouble(oar[i].toString());
        return da;
    }

    /**
     * This converts an ArrayList with Integers into an int[].
     *
     * @param al an Object[]
     * @return the corresponding int[]  (invalid values are converted to Integer.MAX_VALUE).
     *   al=null returns null.
     */
    public static int[] toIntArray(ArrayList al) {
        if (al == null)
            return null;
        int n = al.size();
        Math2.ensureMemoryAvailable(4L * n, "String2.toIntArray"); 
        int ia[] = new int[n];
        for (int i = 0; i < n; i++)
            ia[i] = ((Integer)al.get(i)).intValue();
        return ia;
    }

    /**
     * This converts an ArrayList with Floats into a float[].
     *
     * @param al an Object[]
     * @return the corresponding float[] (invalid values are converted to Float.NaN).
     *   al=null returns null.
     */
    public static float[] toFloatArray(ArrayList al) {
        if (al == null)
            return null;
        int n = al.size();
        Math2.ensureMemoryAvailable(4L * n, "String2.toFloatArray"); 
        float fa[] = new float[n];
        for (int i = 0; i < n; i++)
            fa[i] = ((Float)al.get(i)).floatValue();
        return fa;
    }

    /**
     * This converts an ArrayList with Doubles into a double[].
     *
     * @param al an Object[]
     * @return the corresponding double[] (invalid values are converted to Double.NaN).
     *   al=null returns null.
     */
    public static double[] toDoubleArray(ArrayList al) {
        if (al == null)
            return null;
        int n = al.size();
        Math2.ensureMemoryAvailable(4L * n, "String2.toDoubleArray"); 
        double da[] = new double[n];
        for (int i = 0; i < n; i++)
            da[i] = ((Double)al.get(i)).doubleValue();
        return da;
    }

    /**
     * This returns an int[] with just the non-Integer.MAX_VALUE values from the original 
     * array.
     *
     * @param iar is an int[]
     * @return a new int[] with just the non-Integer.MAX_VALUE values.
     *   iar=null returns null.
     */
    public static int[] justFiniteValues(int iar[]) {
        if (iar == null)
            return null;
        int n = iar.length;
        int nFinite = 0;
        int ia[] = new int[n];
        for (int i = 0; i < n; i++)
            if (iar[i] < Integer.MAX_VALUE)
                ia[nFinite++] = iar[i];

        //copy to a new array
        int iaf[] = new int[nFinite];
        System.arraycopy(ia, 0, iaf, 0, nFinite);
        return iaf;
    }

    /**
     * This returns a double[] with just the finite values from the original 
     * array.
     *
     * @param dar is a double[]
     * @return a new double[] with just finite values.
     *   dar=null returns null.
     */
    public static double[] justFiniteValues(double dar[]) {
        if (dar == null)
            return null;
        int n = dar.length;
        int nFinite = 0;
        double da[] = new double[n];
        for (int i = 0; i < n; i++)
            if (Double.isFinite(dar[i]))
                da[nFinite++] = dar[i];

        //copy to a new array
        double daf[] = new double[nFinite];
        System.arraycopy(da, 0, daf, 0, nFinite);
        return daf;
    }

    /** 
     * This returns a String[] with just non-null strings
     * from the original array.
     *
     * @param sar is a String[]
     * @return a new String[] with just non-null strings.
     *   sar=null returns null.
     */
    public static String[] removeNull(String sar[]) {
        if (sar == null)
            return null;
        int n = sar.length;
        int nValid = 0;
        String sa[] = new String[n];
        for (int i = 0; i < n; i++)
            if (sar[i] != null)
                sa[nValid++] = sar[i];

        //copy to a new array
        String sa2[] = new String[nValid];
        System.arraycopy(sa, 0, sa2, 0, nValid);
        return sa2;
    }

    /** 
     * This returns a String[] with just non-null and non-"" strings
     * from the original array.
     *
     * @param sar is a String[]
     * @return a new String[] with just non-null and non-"" strings.
     *   sar=null returns null.
     */
    public static String[] removeNullOrEmpty(String sar[]) {
        if (sar == null)
            return null;
        int n = sar.length;
        int nValid = 0;
        String sa[] = new String[n];
        for (int i = 0; i < n; i++)
            if (sar[i] != null && sar[i].length() > 0)
                sa[nValid++] = sar[i];

        //copy to a new array
        String sa2[] = new String[nValid];
        System.arraycopy(sa, 0, sa2, 0, nValid);
        return sa2;
    }

    /**
     * This converts a comma-separated-value String into an int[].
     * Invalid values are converted to Integer.MAX_VALUE.
     *
     * @param csv the comma-separated-value String.
     * @return the corresponding int[].
     *    csv=null returns null.
     *    csv="" is converted to int[1]{Integer.MAX_VALUE}.
     */
    public static int[] csvToIntArray(String csv) {
        return toIntArray(split(csv, ','));      
    }

    /**
     * This converts a comma-separated-value String into a double[].
     * Invalid values are converted to Double.NaN.
     *
     * @param csv the comma-separated-value String
     * @return the corresponding double[].
     *    csv=null returns null.
     *    csv="" is converted to double[1]{Double.NAN}.
     */
    public static double[] csvToDoubleArray(String csv) {
        return toDoubleArray(split(csv, ','));      
    }

    /**
     * This converts a string to a boolean.
     * 
     * @param s the string
     * @return false if s is "false", "f", or "0". Case and leading/trailing
     *   spaces don't matter.  All other values (and null) are treated as true.
     */
    public static boolean parseBoolean(String s) {
        if (s == null)
            return true;
        s = s.toLowerCase().trim();
        return !(s.equals("false") || s.equals("f") || s.equals("0"));
    }

    /**
     * This converts a string to a boolean and then a byte.
     * 
     * @param s the string
     * @return Byte.MAX_VALUE (i.e., missing value) if s is null or s is "". 
     *   Return 0 if s is "false", "f", or "0".   
     *   Return 1 if for all other values.
     *   Case and leading/trailing spaces don't matter.
     */
    public static byte parseBooleanToByte(String s) {
        if (s == null)
            return Byte.MAX_VALUE;
        s = s.toLowerCase().trim();
        if (s.length() == 0)
            return Byte.MAX_VALUE;
        return (s.equals("false") || s.equals("f") || s.equals("0"))? (byte)0 : (byte)1;
    }

    /** This removes leading ch's.
     * @param s
     * @param ch
     * @return s or a new string without leading ch's.
     *   null returns null.
     */
    public static String removeLeading(String s, char ch) {
        if (s == null)
            return s;
        int sLength = s.length();
        int start = 0;
        while (start < sLength && s.charAt(start) == ch)
            start++;
        return start == 0? s : s.substring(start);
    }


    /** Like parseInt(s), but returns def if error). */
    public static int parseInt(String s, int def) {
        int i = parseInt(s);
        return i == Integer.MAX_VALUE? def : i;
    }

    /**
     * Convert a string to an int.
     * Leading or trailing spaces are automatically removed.
     * This accepts hexadecimal integers starting with "0x".
     * Leading 0's (e.g., 0012) are ignored; number is treated as decimal (not octal as Java would).
     * Floating point numbers are rounded.
     * This won't throw an exception if the number isn't formatted right.
     * To make a string from an int, use ""+i, Integer.toHexString, or Integer.toString(i,radix).
     *
     * @param s is the String representation of a number.
     * @return the int value from the String 
     *    (or Integer.MAX_VALUE if error).
     */
    public static int parseInt(String s) {
        //*** XML.decodeEntities relies on leading 0's being ignored 
        //    and number treated as decimal (not octal)

        //quickly reject most non-numbers
        //This is a huge speed improvement when parsing ASCII data files
        //  because Java is very slow at filling in the stack trace when an exception is thrown.
        if (s == null)
            return Integer.MAX_VALUE;
        s = s.trim();
        if (s.length() == 0)
            return Integer.MAX_VALUE;
        char ch = s.charAt(0);
        if ((ch < '0' || ch > '9') && ch != '-' && ch != '+' && ch != '.')
            return Integer.MAX_VALUE;

        //try to parse hex or regular int        
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) 
                return (int)Long.parseLong(s.substring(2), 16); //for >7fffffff, returns signed int
            return Integer.parseInt(s);
        } catch (Exception e) {      
            //falls through
        }

        //round from double?
        try {
            //2011-02-09 Bob Simons added to avoid Java hang bug.
            //But now, latest version of Java is fixed.
            //if (isDoubleTrouble(s)) return 0;  

            return Math2.roundToInt(Double.parseDouble(s));
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Convert a string to a double.
     * Leading or trailing spaces are automatically removed.
     * This accepts hexadecimal integers starting with "0x".
     * Whole number starting with '0' (e.g., 012) is treated as decimal (not octal as Java would).
     * This won't throw an exception if the number isn't formatted right.
     *
     * @param s is the String representation of a number.
     * @return the double value from the String (a finite value,
     *   Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 
     *   or Double.NaN if error).
     */
    public static double parseDouble(String s) {
        //quickly reject most non-numbers
        //This is a huge speed improvement when parsing ASCII data files
        //  because Java is very slow at filling in the stack trace when an exception is thrown.
        if (s == null)
            return Double.NaN;
        s = s.trim();
        if (s.length() == 0)
            return Double.NaN;
        char ch = s.charAt(0);
        if ((ch < '0' || ch > '9') && ch != '-' && ch != '+' && ch != '.')
            return Double.NaN;

        try {
            if (s.startsWith("0x") || s.startsWith("0X")) 
                return Long.parseLong(s.substring(2), 16); //for >7fffffff, returns signed int

            //2011-02-09 Bob Simons added to avoid Java hang bug.
            //But now, latest version of Java is fixed.
            //if (isDoubleTrouble(s)) return 0;  
            
            return Double.parseDouble(s);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /** 
     * DON'T USE THIS; RELY ON THE FIXES AVAILABLE FOR JAVA: 
     * EITHER THE LATEST VERSION OF JAVA OR THE 
     * JAVA UPDATER TO FIX THE BUG ON EXISTING OLDER JAVA INSTALLATIONS
     * https://www.oracle.com/technetwork/java/javase/fpupdater-tool-readme-305936.html
     *
     * <p>This returns true if s is a value that causes Java to hang. 
     * Avoid java hang.     2011-02-09 
     * http://www.exploringbinary.com/java-hangs-when-converting-2-2250738585072012e-308
     * This was Bob's work-around to avoid the Java bug.
     *
     * @param s a string representing a double value
     * @return true if the value is the troublesome value.
     *    If true, the value can be interpreted as either +/-Double.MIN_VALUE (not sure which) 
     *    or (crudely) 0.
     */
    public static boolean isDoubleTrouble(String s) {
        if (s == null || s.length() < 22)  //this is a good quick reject
            return false;

        //all variants are relevant, so look for the mantissa
        return replaceAll(s, ".", "").indexOf("2225073858507201") >= 0;
    }

    /**
     * Convert a string to an int, with rounding.
     * Leading or trailing spaces are automatically removed.
     * This won't throw an exception if the number isn't formatted right.
     *
     * @param s is the String representation of a number.
     * @return the int value from the String (or Double.NaN if error).
     */
    public static double roundingParseInt(String s) {
        return Math2.roundToInt(parseDouble(s));
    }

    /** 
     * This converts String representation of a long. 
     * Leading or trailing spaces are automatically removed.
     * THIS DOESN'T ROUND! So floating point values lead to Long.MAX_VALUE.
     *
     * @param s a valid String representation of a long value
     * @return a long (or Long.MAX_VALUE if trouble).
     */
    public static long parseLong(String s) {
        //quickly reject most non-numbers
        //This is a huge speed improvement when parsing ASCII data files
        //  because Java is very slow at filling in the stack trace when an exception is thrown.
        if (s == null)
            return Long.MAX_VALUE;
        s = s.trim();
        if (s.length() == 0)
            return Long.MAX_VALUE;
        char ch = s.charAt(0);
        if ((ch < '0' || ch > '9') && ch != '-' && ch != '+')
            return Long.MAX_VALUE;

        try {
            if (s.startsWith("0x") || s.startsWith("0X"))
                return Long.parseLong(s.substring(2), 16);
            return Long.parseLong(s);
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Parse as a float with either "." or "," as the decimal point.
     * Leading or trailing spaces are automatically removed.
     *
     * @param s a String representing a float value (e.g., 1234.5 or 1234,5 or 
     *     1.234e3 1,234e3)
     * @return the corresponding float (or Float.NaN if not properly formatted)
     */
    public static float parseFloat(String s) {
        //quickly reject most non-numbers
        //This is a huge speed improvement when parsing ASCII data files
        //  because Java is very slow at filling in the stack trace when an exception is thrown.
        if (s == null)
            return Float.NaN;
        s = s.trim();
        if (s.length() == 0)
            return Float.NaN;
        char ch = s.charAt(0);
        if ((ch < '0' || ch > '9') && ch != '-' && ch != '+' && ch != '.')
            return Float.NaN;

        try {
            s = s.replace(',', '.');   //!!! this is inconsistent with parseDouble
            float f = Float.parseFloat(s);
            //String2.log(">> parseFloat " + s + " -> " + f);
            return f;
        } catch (Exception e) {
            log(">> parseFloat exception: " + s);
            return Float.NaN;
        }
    }

    /**
     * This converts a multiple-space-separated string into a String[] of separate tokens.
     * Double quoted tokens may have internal spaces.
     *
     * @param s the space-separated string
     * @return String[] of tokens (or null if s is null)
     */
    public static String[] tokenize(String s) {
        if (s == null)
            return null;

        ArrayList<String> arrayList = new ArrayList();
        int sLength = s.length();
        int index = 0; //next char to be read
        //eat spaces
        while (index < sLength && s.charAt(index) == ' ') 
            index++;
        //repeatedly get tokens
        while (index < sLength) {
            //grab a token
            int start = index;
            int stop;
            //does it start with quotes?
            if (s.charAt(index) == '"') {
                index++; //skip the quotes
                start++;
                while (index < sLength && s.charAt(index) != '"') 
                    index++;
                stop = index; //if end of string and no closing quotes, it's a silent error
                index++; //skip the quotes
            } else {
                while (index < sLength && s.charAt(index) != ' ') 
                    index++;
                stop = index;
            }
            arrayList.add(s.substring(start, stop));

            //eat spaces
            while (index < sLength && s.charAt(index) == ' ') 
                index++;
        }

        return arrayList.toArray(new String[0]);
    }

    /** The size of the int[] needed for distribute() and getDistributionStatistics(). */
    public static int DistributionSize = 22;

    private static int BinMax[] = new int[]{0, 1, 2, 5, 10, 20, 50, 100, 200, 500,
        1000, 2000, 5000, 10000, 20000, //1,2,5,10,20 seconds
        60000, 120000, 300000, 600000, 1200000, 3600000, //1,2,5,10,20,60 minutes
        Integer.MAX_VALUE};

    /**
     * Put aTime into one of the distribution bins.
     * @param aTime 
     * @param distribution an int[DistributionSize] holding the counts of aTimes in 
     *   different categories
     */
    public static void distribute(long aTime, int[] distribution) {
        //catch really long times (greater than Integer.MAX_VALUE)
        if (aTime < 0)
            aTime = 0;
        if (aTime > 3600000) { distribution[21]++; return; }   //1hr   
        
        int iTime = (int)aTime; //safe since extreme values caught above
        for (int bin = 0; bin < DistributionSize; bin++) {
            if (iTime <= BinMax[bin]) {
                distribution[bin]++;
                return;
            }
        }
    }

    /**
     * Get the number of values in the distribution.
     *
     * @param distribution an int[DistributionSize] holding the counts of aTimes in 
     *   different categories
     * @return the number of values in the distribution.
     */
    public static int getDistributionN(int[] distribution) {
        //calculate n
        int n = 0;
        for (int bin = 0; bin < DistributionSize; bin++)
            n += distribution[bin];
        return n;
    }

    /**
     * Get the approximate median of the distribution.
     * See Sokal and Rohlf, Biometry, Box 4.1, pg 45.
     *
     * @param distribution an int[DistributionSize] holding the counts of aTimes in 
     *   different categories
     * @param n from getDistributionN
     * @return the approximate median of the distribution.
     *    If trouble or n&lt;=0, this returns -1.
     */
    public static int getDistributionMedian(int[] distribution, int n) {
        double n2 = n / 2.0;

        if (n > 0) {
            //handle bin 0
            int cum = distribution[0];
            if (cum >= n2)
                return 0;

            for (int bin = 1; bin < DistributionSize; bin++) {  //bin 0 handled above
                if (distribution[bin] > 0) {
                    int tCum = cum + distribution[bin];
                    if (cum <= n2 && tCum >= n2) {
                        int tBinMax = bin == DistributionSize - 1? BinMax[bin-1] * 3 : BinMax[bin];
                        return Math2.roundToInt(
                            BinMax[bin-1] + ((n2-cum+0.0)/distribution[bin]) * (tBinMax - BinMax[bin-1]));
                    }
                    cum = tCum;
                }
            }
        }
        return -1; //trouble
    }

    /**
     * Generate brief statistics for a distribution.
     * @param distribution an int[DistributionSize] holding the counts of aTimes in 
     *   different categories
     * @return the statistics
     */
    public static String getBriefDistributionStatistics(int[] distribution) {
        int n = getDistributionN(distribution);
        String s = "n =" + right("" + n, 9);
        if (n == 0) 
            return s;
        int median = getDistributionMedian(distribution, n);
        return s + ",  median ~=" + right("" + median, 9) + " ms";
    }

    /**
     * Generate statistics for a distribution.
     * @param distribution an int[DistributionSize] holding the counts of aTimes in 
     *   different categories
     * @return the statistics
     */
    public static String getDistributionStatistics(int[] distribution) {
        int n = getDistributionN(distribution);
        String s = 
            "    " + getBriefDistributionStatistics(distribution) + "\n";

        if (n == 0)
            return s;

        return 
            s + 
            "    0 ms:      " + right("" + distribution[0], 10) + "\n" +
            "    1 ms:      " + right("" + distribution[1], 10) + "\n" +
            "    2 ms:      " + right("" + distribution[2], 10) + "\n" +
            "    <= 5 ms:   " + right("" + distribution[3], 10) + "\n" +
            "    <= 10 ms:  " + right("" + distribution[4], 10) + "\n" +
            "    <= 20 ms:  " + right("" + distribution[5], 10) + "\n" +
            "    <= 50 ms:  " + right("" + distribution[6], 10) + "\n" +
            "    <= 100 ms: " + right("" + distribution[7], 10) + "\n" +
            "    <= 200 ms: " + right("" + distribution[8], 10) + "\n" +
            "    <= 500 ms: " + right("" + distribution[9], 10) + "\n" +
            "    <= 1 s:    " + right("" + distribution[10], 10) + "\n" +
            "    <= 2 s:    " + right("" + distribution[11], 10) + "\n" +
            "    <= 5 s:    " + right("" + distribution[12], 10) + "\n" +
            "    <= 10 s:   " + right("" + distribution[13], 10) + "\n" +
            "    <= 20 s:   " + right("" + distribution[14], 10) + "\n" +
            "    <= 1 min:  " + right("" + distribution[15], 10) + "\n" +
            "    <= 2 min:  " + right("" + distribution[16], 10) + "\n" +
            "    <= 5 min:  " + right("" + distribution[17], 10) + "\n" +
            "    <= 10 min: " + right("" + distribution[18], 10) + "\n" +
            "    <= 20 min: " + right("" + distribution[19], 10) + "\n" +
            "    <= 1 hr:   " + right("" + distribution[20], 10) + "\n" +
            "    >  1 hr:   " + right("" + distribution[21], 10) + "\n";
    }


    /**
     * If lines in s are &gt;=maxLength characters, this inserts "\n"+spaces at the
     * previous non-DigitLetter + DigitLetter; or if none, this inserts "\n"+spaces at maxLength.
     * Useful keywords for searching for this method: longer, longest, noLongerThan.
     *
     * @param s a String with multiple lines, separated by \n's
     * @param maxLength the maximum line length allowed
     * @param spaces the string to be inserted after the inserted newline, e.g., "&lt;br&gt;    " 
     * @return s (perhaps the same, perhaps different), but with no long lines
     */
    public static String noLongLines(String s, int maxLength, String spaces) {
        int maxLength2 = maxLength / 2;
        int spacesLength = spaces.length();
        int start = 0, count = 0;
        int sLength = s.length();
        StringBuilder sb = new StringBuilder(sLength / 5 * 6);
        for (int i = 0; i < sLength; i++) { 
           if (s.charAt(i) == '\n') {
               count = 0;
           } else {
               count++; 
               if (count >= maxLength) {
                   int oi = i;
                   char ch, ch1 = s.charAt(i);
                   while (count > maxLength2) {
                       ch = ch1;
                       ch1 = s.charAt(i - 1);
                       //work backwards from maxLength to maxLength/2 to find a break point  (i)
                       if (!isDigitLetter(ch1) && ch1 != '(' && isDigitLetter(ch)) {
                           count = 0; //signal success
                           break;
                       }
                       count--;
                       i--;
                   }
                   if (count > 0) { 
                       //newline not inserted above; insert it at oi                    
                       i = oi;
                   }
                   sb.append(s.substring(start, i));
                   sb.append("\n" + spaces);
                   start = i;
                   count = spacesLength;
               }
           }
        }
        if (start == 0)
            return s;

        sb.append(s.substring(start));
        return sb.toString();
    }


    /**
     * This is like noLongLines, but will only break (add newlines) at spaces.
     * If there is no reasonable break before maxLength, it will break after maxLength.
     *
     * @param sb a StringBuilder with multiple lines, separated by \n's
     * @param maxLength the maximum line length allowed
     * @param spaces the string to be inserted after the inserted newline, e.g., "&lt;br&gt;    " 
     * @return the same or a different StringBuilder, but with no long lines
     */
    public static StringBuilder noLongLinesAtSpace(StringBuilder sb, int maxLength, String spaces) {
        int sbLength = sb.length();
        if (sbLength <= maxLength)
            return sb;
        StringBuilder newSB = new StringBuilder(sbLength / 5 * 6);         
        int minCount = maxLength / 2; //try hard

        int startAt = 0;  //start for next copy chunk
        int count = 0; //don't jump ahead because there may be an internal \n
        int lastSpaceAt = -1;
        
        for (int sbi = 0; sbi < sbLength; sbi++) { 
            char ch = sb.charAt(sbi);
            if (ch == '\n') {
                newSB.append(sb, startAt, sbi + 1);
                startAt = sbi + 1;
                count = 0;
                lastSpaceAt = -1;
            } else {
                if (ch == ' ' && count >= minCount)
                    lastSpaceAt = sbi;
                count++; 
                if (count >= maxLength && lastSpaceAt >= 0) {

                    //use lastSpaceAt
                    newSB.append(sb, startAt, lastSpaceAt);
                    newSB.append('\n');
                    newSB.append(spaces);
                    sbi = lastSpaceAt;
                    lastSpaceAt = -1;
                    count = spaces.length();

                    //maybe next char is a space, too; skip to last space in a series
                    while (sbi < sbLength - 1 && sb.charAt(sbi + 1) == ' ')
                        sbi++;
                    startAt = sbi + 1;
                }
            }
        }
        //copy remainder of sb
        if (startAt < sbLength)
            newSB.append(sb, startAt, sbLength);
        return newSB;
    }

    /**
     * This is like noLongLines, but will only break at spaces.
     *
     * @param s a String with multiple lines, separated by \n's
     * @param maxLength the maximum line length allowed
     * @param spaces the string to be inserted after the inserted newline, e.g., "    " 
     * @return the content of s, but with no long lines
     */
    public static String noLongLinesAtSpace(String s, int maxLength, String spaces) {
        if (s.length() <= maxLength)
            return s;
        return noLongLinesAtSpace(new StringBuilder(s), maxLength, spaces).toString();
    }

    /**
     * This reads an ASCII file line by line (with any common end-of-line characters), 
     * does a simple (not regex) search and replace on each line, 
     * and saves the lines in another file (with String2.lineSeparator's).
     *
     * @param fullInFileName the full name of the input file
     * @param fullOutFileName the full name of the output file 
        (if same as fullInFileName, fullInFileName will be renamed +.original)
     * @param search  a plain text string to search for
     * @param replace  a plain text string to replace any instances of 'search'
     * @throws Exception if any trouble
     */
    public static void simpleSearchAndReplace(String fullInFileName,
        String fullOutFileName, String search, String replace) 
        throws Exception {       
             
        log("simpleSearchAndReplace in=" + fullInFileName +
            " out=" + fullOutFileName + " search=" + search + " replace=" + replace);
        String tOutFileName = fullOutFileName + Math2.random(Integer.MAX_VALUE);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fullInFileName));
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(tOutFileName));
            try {
                                 
                //convert the text, line by line
                //This uses bufferedReader.readLine() to repeatedly
                //read lines from the file and thus can handle various 
                //end-of-line characters.
                String s = bufferedReader.readLine();
                while (s != null) { //null = end-of-file
                    bufferedWriter.write(replaceAll(s, search, replace));
                    bufferedWriter.write(lineSeparator);
                    s = bufferedReader.readLine();
                }

                bufferedReader.close();
                bufferedReader = null;
                bufferedWriter.close();
                bufferedWriter = null;

                if (fullInFileName.equals(fullOutFileName))
                    File2.rename(fullInFileName, fullInFileName + ".original");
                File2.rename(tOutFileName, fullOutFileName);
                if (fullInFileName.equals(fullOutFileName))
                    File2.delete(fullInFileName + ".original");

            } catch (Exception e) {
                try {
                    if (bufferedWriter != null) {
                        bufferedWriter.close();
                        bufferedWriter = null;
                    }
                } catch (Exception e2) {
                }
                try {
                    if (bufferedReader != null) {
                        bufferedReader.close();
                        bufferedReader = null;
                    }
                } catch (Exception e2) {
                }
                File2.delete(tOutFileName);
                throw e;
            }
        } catch (Exception e3) {
            try {
                if (bufferedReader != null) 
                    bufferedReader.close();
            } catch (Exception e4) {
            }
            File2.delete(tOutFileName);
            throw e3;
        }
    }

    /**
     * This reads an ASCII file line by line (with any common end-of-line characters), 
     * does a regex search and replace on each line, 
     * and saves the lines in another file (with String2.lineSeparator's).
     *
     * @param fullInFileName the full name of the input file
     * @param fullOutFileName the full name of the output file
     * @param search  a regex to search for
     * @param replace  a plain text string to replace any instances of 'search'
     * @throws Exception if any trouble
     */
    public static void regexSearchAndReplace(String fullInFileName,
        String fullOutFileName, String search, String replace) 
        throws Exception {
         
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fullInFileName));
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fullOutFileName));
            try {                   
                //get the text from the file
                //This uses bufferedReader.readLine() to repeatedly
                //read lines from the file and thus can handle various 
                //end-of-line characters.
                String s = bufferedReader.readLine();
                while (s != null) { //null = end-of-file
                    bufferedWriter.write(s.replaceAll(search, replace));
                    bufferedWriter.write(lineSeparator);
                    s = bufferedReader.readLine();
                }
            } finally {
                bufferedWriter.close();
            }
        } finally {
            bufferedReader.close();
        }
    }


    /**
     * This returns a string with the keys and values of the Map (sorted by the keys, ignoreCase).
     *
     * @param map (keys and values are objects with good toString methods).
     *   If it needs to be thead-safe, use ConcurrentHashMap.
     * @return a string with the sorted (ignoreCase) keys and their values ("key1: value1\nkey2: value2\n")
     */
    public static String getKeysAndValuesString(Map map) {
        ArrayList al = new ArrayList();

        //synchronize so protected from changes in other threads
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            al.add(key.toString() + ": " + map.get(key).toString());
        }
        Collections.sort(al, new StringComparatorIgnoreCase());
        return toNewlineString(al.toArray());
    }

    /**
     * This returns the number formatted with up to 6 digits to the left and right of
     * the decimal and trailing decimal 0's removed.  
     * If abs(d) &lt; 0.0999995 or abs(d) &gt;= 999999.9999995, the number is displayed
     * in scientific notation (e.g., 8.954321E-5).
     * Thus the maximum length should be 14 characters (-123456.123456).
     * 0 returns "0"
     * NaN returns "NaN".
     * Double.POSITIVE_INFINITY returns "Infinity".
     * Double.NEGATIVE_INFINITY returns "-Infinity".
     *
     * @param d a number
     * @return the number converted to a string
     */
    public static String genEFormat6(double d) {

        //!finite
        if (!Double.isFinite(d))
            return "" + d;

        //almost 0
        if (Math2.almost0(d))
            return "0";

        //close to 0 
        //String2.log("genEFormat test " + (d*1000) + " " + Math.rint(d*1000));
        if (Math.abs(d) < 0.0999995 &&  
            !Math2.almostEqual(6, d * 10000, Math.rint(d * 10000))) {     //leave .0021 as .0021, but display .00023 as 2.3e-4
            synchronized(genExpFormat6) {
                return genExpFormat6.format(d);
            }
        }

        //large int
        if (Math.abs(d) < 1e13 && d == Math.rint(d))
            return "" + Math2.roundToLong(d);

        //>10e6
        if (Math.abs(d) >= 999999.9999995) {
            synchronized(genExpFormat6) {
                return genExpFormat6.format(d);
            }
        }

        synchronized(genStdFormat6) {
            return genStdFormat6.format(d);
        }
    }

    /**
     * This returns the number formatted with up to 10 digits to the left and right of
     * the decimal and trailing decimal 0's removed.  
     * If abs(d) &lt; 0.09999999995 or abs(d) &gt;= 999999.99999999995, the number is displayed
     * in scientific notation (e.g., 8.9544680321E-5).
     * Thus the maximum length should be 18 characters (-123456.1234567898).
     * 0 returns "0"
     * NaN returns "NaN".
     * Double.POSITIVE_INFINITY returns "Infinity".
     * Double.NEGATIVE_INFINITY returns "-Infinity".
     *
     * @param d a number
     * @return the number converted to a string
     */
    public static String genEFormat10(double d) {

        //!finite
        if (!Double.isFinite(d))
            return "" + d;

        //almost 0
        if (Math2.almost0(d))
            return "0";

        //close to 0 and many sig digits
        //String2.log("genEFormat test " + (d*1000) + " " + Math.rint(d*1000));
        if (Math.abs(d) < 0.09999999995 &&     
            !Math2.almostEqual(9, d * 1000000, Math.rint(d * 1000000))) {     //leave .0021 as .0021, but display .00023 as 2.3e-4
            synchronized(genExpFormat10) {
                return genExpFormat10.format(d);
            }
        }

        //large int
        if (Math.abs(d) < 1e13 && d == Math.rint(d)) //rint only catches 9 digits(?)
            return "" + Math2.roundToLong(d);

        //>10e6
        if (Math.abs(d) >= 999999.99999999995) {
            synchronized(genExpFormat10) {
                return genExpFormat10.format(d);
            }
        }

        synchronized(genStdFormat10) {
            return genStdFormat10.format(d);
        }
    }

    /**
     * This is like genEFormat6, but the scientific notation format
     * is, e.g., 8.954321x10^-5.
     *
     * @param d a number
     * @return the number converted to a string
     */
    public static String genX10Format6(double d) {
        return replaceAll(genEFormat6(d), "E", "x10^");
    }

    /**
     * This is like genEFormat10, but the scientific notation format
     * is, e.g., 8.9509484321x10^-5.
     *
     * @param d a number
     * @return the number converted to a string
     */
    public static String genX10Format10(double d) {
        return replaceAll(genEFormat10(d), "E", "x10^");
    }

    /**
     * This is like genEFormat6, but the scientific notation format
     * is, e.g., 8.954321x10<sup>-5</sup>.
     *
     * @param d a number
     * @return the number converted to a string
     */
    public static String genHTMLFormat6(double d) {
        String s = genEFormat6(d);
        int po = s.indexOf('E');
        if (po >= 0) 
            s = replaceAll(genEFormat6(d), "E", "x10<sup>") + "</sup>";
        return s;
    }

    /**
     * This is like genEFormat10, but the scientific notation format
     * is, e.g., 8.9509244321x10<sup>-5</sup>.
     *
     * @param d a number
     * @return the number converted to a string
     */
    public static String genHTMLFormat10(double d) {
        String s = genEFormat10(d);
        int po = s.indexOf('E');
        if (po >= 0) 
            s = replaceAll(genEFormat10(d), "E", "x10<sup>") + "</sup>";
        return s;
    }

    /**
     * This removes white space characters at the beginning and end of a StringBuilder.
     *
     * @param sb a StringBuilder
     * @return the same pointer to the StringBuilder for convenience
     */
    public static StringBuilder trim(StringBuilder sb) {
        int po = 0;
        while (po < sb.length() && isWhite(sb.charAt(po))) po++;
        sb.delete(0, po);

        po = sb.length();
        while (po > 0 && isWhite(sb.charAt(po - 1))) po--;
        sb.delete(po, sb.length());
        return sb;
    }

    /**
     * This trims just the start of the string.
     * 
     * @param s
     * @return s with just the start of the string trim'd.
     *    If s == null, this returns null.
     */
    public static String trimStart(String s) {
        if (s == null)
            return s;
        int sLength = s.length();
        int po = 0;
        while (po < sLength && isWhite(s.charAt(po))) 
            po++;
        return po > 0? s.substring(po) : s;
    }

    /**
     * This trims just the end of the string.
     * 
     * @param s
     * @return s with just the end of the string trim'd.
     *    If s == null, this returns null.
     */
    public static String trimEnd(String s) {
        if (s == null)
            return s;
        int sLength = s.length();
        int po = sLength;
        while (po > 0 && isWhite(s.charAt(po - 1))) 
            po--;
        return po < sLength? s.substring(0, po) : s;
    }

    /**
     * This returns the directory that is the classpath for the source
     * code files (with forward slashes and a trailing slash, 
     * e.g., c:/programs/_tomcat/webapps/cwexperimental/WEB-INF/classes/.
     *
     * @return directory that is the classpath for the source
     *     code files (with / separator and / at the end)
     * @throws RuntimeException if trouble
     */
    public static String getClassPath() {
        if (classPath == null) {
            String find = "/com/cohort/util/String2.class";
            //use this.getClass(), not ClassLoader.getSystemResource (which fails in Tomcat)
            classPath = String2.class.getResource(find).getFile();
            classPath = replaceAll(classPath, '\\', '/');
            int po = classPath.indexOf(find);
            classPath = classPath.substring(0, po + 1);

            //on windows, remove the troublesome leading "/"
            if (OSIsWindows && classPath.length() > 2 && 
                classPath.charAt(0) == '/' && classPath.charAt(2) == ':')
                classPath = classPath.substring(1);

            //classPath is a URL! so spaces are encoded as %20 on Windows!
            //UTF-8: see https://en.wikipedia.org/wiki/Percent-encoding#Current_standard
            try {
                classPath = URLDecoder.decode(classPath, UTF_8);  
            } catch (Throwable t) {
                log(MustBe.throwableToString(t));
            }
        }

        return classPath;
    }

    /**
     * On the command line, this prompts the user a String.
     *
     * @param prompt
     * @return the String the user entered
     * @throws RuntimeException if trouble
     */
    public static String getStringFromSystemIn(String prompt) {
        try {
            flushLog();
            System.out.print(prompt);
            BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
            return inReader.readLine();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 
     * A variant of getStringFromSystemIn that adds "\nPress ^C to stop or Enter to continue..."
     * to the prompt.
     *
     * @throws RuntimeException if trouble
     */
    public static String pressEnterToContinue(String prompt) {
        if (prompt == null)
            prompt = "";
        return getStringFromSystemIn(prompt + 
                (prompt.length() == 0 || prompt.endsWith("\n")? "" : "\n") +
                "Press ^C to stop or Enter to continue...");
    }

   
    /** 
     * A variant of pressEnterToContinue with "Press ^C to stop or Enter to continue..."
     * as the prompt.
     *
     * @throws RuntimeException if trouble
     */
    public static String pressEnterToContinue() {
        return pressEnterToContinue("");
    }
     


/*  OLD VERSION
    public static String getPasswordFromSystemIn(String prompt) throws Exception {
        System.out.print(prompt);
        StringBuilder sb = new StringBuilder();
        while (true) {
            while (System.in.available() == 0) {
                Math2.sleep(1);
                System.out.print("\b*");
            }                    
            int ch = System.in.read();
            if (ch <= 0) 
                continue;
            if (ch == '\n') 
                return sb.toString();
            sb.append((char)ch);
        }
    }
*/

    /**
     * On the command line, this prompts the user a String (which is
     * not echoed to the screen, so is suitable for passwords).
     * This is slighly modified from 
     * http://java.sun.com/developer/technicalArticles/Security/pwordmask/ .
     *
     * @param prompt
     * @return the String the user entered
     * @throws Exception if trouble
     */
    public static final String getPasswordFromSystemIn(String prompt) throws Exception {
        InputStream in = System.in; //bob added, instead of parameter

        MaskingThread maskingthread = new MaskingThread(prompt);
        Thread thread = new Thread(maskingthread);
        thread.start();
        

        char[] lineBuffer;
        char[] buf;
        int i;

        buf = lineBuffer = new char[128];

        int room = buf.length;
        int offset = 0;
        int c;
        
        try { //bob added
            loop:   while (true) {
                c = in.read();
                if (c == -1 || c == '\n')
                    break loop;
                if (c == '\r') {
                    int c2 = in.read();
                    if ((c2 != '\n') && (c2 != -1)) {
                        if (!(in instanceof PushbackInputStream)) {
                            in = new PushbackInputStream(in);
                        }
                        ((PushbackInputStream)in).unread(c2);
                    } else {
                        break loop;
                    }
                }

                //if not caught and 'break loop' above...
                if (--room < 0) {
                    buf = new char[offset + 128];
                    room = buf.length - offset - 1;
                    System.arraycopy(lineBuffer, 0, buf, 0, offset);
                    Arrays.fill(lineBuffer, ' ');
                    lineBuffer = buf;
                }
                buf[offset++] = (char) c;
            }
        } catch (Exception e) {
        }
        maskingthread.stopMasking();
        if (offset == 0) {
           return ""; //bob changed from null
        }
        char[] ret = new char[offset];
        System.arraycopy(buf, 0, ret, 0, offset);
        Arrays.fill(buf, ' ');
        return new String(ret); //bob added; originally it returned char[]
    }

    /**
     * Find the last element which is &lt;= s in an ascending sorted array.
     *
     * @param sar an ascending sorted String[] which may have duplicate values
     * @param s
     * @return the index of the last element which is &lt;= s in an ascending sorted array.
     *   If s is null or s &lt; the smallest element, this returns -1  (no element is appropriate).
     *   If s &gt; the largest element, this returns sar.length-1.
     */
    public static int binaryFindLastLE(String[] sar, String s) {
        if (s == null) 
            return -1;
        int i = Arrays.binarySearch(sar, s);

        //an exact match; look for duplicates
        if (i >= 0) {
            while (i < sar.length - 1 && sar[i + 1].compareTo(s) <= 0)
                i++;
            return i; 
        }

        int insertionPoint = -i - 1;  //0.. sar.length
        return insertionPoint - 1;
    }

    /**
     * Find the first element which is &gt;= s in an ascending sorted array.
     *
     * @param sar an ascending sorted String[] which currently may not have duplicate values
     * @param s
     * @return the index of the first element which is &gt;= s in an ascending sorted array.
     *   If s &lt; the smallest element, this returns 0.
     *   If s is null or s &gt; the largest element, this returns sar.length (no element is appropriate).
     */
    public static int binaryFindFirstGE(String[] sar, String s) {
        if (s == null) 
            return sar.length;
        int i = Arrays.binarySearch(sar, s);

        //an exact match; look for duplicates
        if (i >= 0) {
            while (i > 0 && sar[i - 1].compareTo(s) >= 0)
                i--;
            return i; 
        }

        return -i - 1;  //the insertion point,  0.. sar.length
    }

    /**
     * Find the closest element to s in an ascending sorted array.
     *
     * @param sar an ascending sorted String[].
     *   It the array has duplicates and s equals one of them,
     *   it isn't specified which duplicate's index will be returned.
     * @param s
     * @return the index of the element closest to s.
     *   If s is null, this returns -1.
     */
    public static int binaryFindClosest(String[] sar, String s) {
        if (s == null)
            return -1;
        int i = Arrays.binarySearch(sar, s);
        if (i >= 0)
            return i; //success

        //insertionPoint at end point?
        int insertionPoint = -i - 1;  //0.. sar.length
        if (insertionPoint == 0) 
            return 0;
        if (insertionPoint >= sar.length)
            return sar.length - 1;

        //insertionPoint between 2 points 
        //do they differ at a different position?
        //make all the same length
        int preIndex = insertionPoint - 1;
        int postIndex = insertionPoint;
        String pre  = sar[preIndex];
        String post = sar[postIndex];
        int longest = Math.max(s.length(), Math.max(pre.length(), post.length()));
        String ts = s + makeString(' ', longest - s.length());
        pre  += makeString(' ', longest - pre.length());
        post += makeString(' ', longest - post.length());
        for (i = 0; i < longest; i++) {
            char ch = ts.charAt(i);
            char preCh = pre.charAt(i);
            char postCh = post.charAt(i);
            if (preCh == ch && postCh != ch) return preIndex;
            if (preCh != ch && postCh == ch) return postIndex;
            if (preCh != ch && postCh != ch) {
                //which one is closer
                return Math.abs(preCh - ch) < Math.abs(postCh - ch)?
                    preIndex : postIndex;
            }
        }
        //shouldn't all be equal
        return preIndex;
    }

    /**
     * This returns the index of the first non-Unicode character.
     * Currently, valid characters are #32 - #126, #160+.
     *
     * @param s
     * @param alsoOK a string with characters (e.g., \r, \n, \t) which are also valid
     * @return the index of the first non-utf-8 character, or -1 if all valid.
     */
    public static int findInvalidUnicode(String s, String alsoOK) {
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            if (alsoOK.indexOf(ch) >= 0)
                continue;
            if (ch < 32) 
                return i;
            if (ch <= 126)
                continue;
            if (ch <= 159)
                return i;
            //160+ is valid
        }
        return -1;
    }

    /**
     * This makes s valid Unicode by converting invalid characters (e.g., #128)
     * with \\uhhhh (literally 2 backslashes, so no info is lost). 
     * The invalid characters are often Windows charset characters #127 - 159.
     *
     * @param s
     * @param alsoOK a string with characters (e.g., \r, \n, \t) which are also valid
     * @return the valid Unicode string.
     */
    public static String makeValidUnicode(String s, String alsoOK) {
        if (s == null)
            return "";
        int n = s.length();
        StringBuilder sb = new StringBuilder(n + 128);
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            if (alsoOK.indexOf(ch) >= 0) 
                sb.append(ch);
            else if (ch < 32) 
                sb.append("\\u" + zeroPad(Integer.toHexString(ch), 4));
            else if (ch < 127) 
                sb.append(ch);
            else if (ch < 160)
                sb.append("\\u" + zeroPad(Integer.toHexString(ch), 4));
            else sb.append(ch);   //160+ is valid                         
        }
        return sb.toString();
    }

    /**
     * This converts the char to an ISO-8859-1 (ISO_8859_1) char. 
     * This converts any char in 127-159 and &gt;255 into '?'.
     *
     * @param ch the char to be converted
     * @return an ISO_8859_1-only char.
     */
    public static char toIso88591Char(char ch) {  
        if (ch < 127) return ch;
        if (ch < 160) return '?';
        if (ch < 256) return ch;
        return               '?';
    }

    /**
     * This converts the chars to ISO-8859-1 (ISO_8859_1) chars. 
     * This converts any char in 127-159 and &gt;255 into '?'.
     *
     * @param car[] the char[] to be converted
     * @return car for convenience.
     */
    public static char[] toIso88591Chars(char car[]) {  
        int n = car.length;
        for (int i = 0; i < n; i++) {            
            char ch = car[i];
            if      (ch < 127) {              }
            else if (ch < 160) {car[i] = '?'; }
            else if (ch < 256) {              }
            else               {car[i] = '?'; }
        }
        return car;
    }

    /**
     * A little weird: This returns the ISO-8859-1 (ISO_8859_1) encoding of the 
     * string as a String (using only the lower byte of each 2-byte char),
     * so a unicode string can be stored in a 1-byte/char string. 
     * This converts any char in 127-159 and &gt;255 into '?'.
     *
     * @param s the string to be converted
     * @return an ISO_8859_1-only string (perhaps the same string).
     *   If s==null, this returns "".
     */
    public static String toIso88591String(String s) {  
        //return ISO_8859_1_CHARSET.decode( //makes a CharBuffer
        //    ISO_8859_1_CHARSET.encode(s)).toString(); //makes a ByteBuffer

        if (s == null)
            return "";
        boolean returnS = true;
        int n = s.length();
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            if      (ch < 127) { sb.append(ch);                   }
            else if (ch < 160) { sb.append('?'); returnS = false; }
            else if (ch < 256) { sb.append(ch);                   }
            else               { sb.append('?'); returnS = false; }
        }
        return returnS? s : sb.toString();
    }

    /** This converts all of the Strings to ISO_8859_1 encoding. 
     *
     * @return sar for convenience
     */
    public static String[] toIso88591Strings(String sar[]) {
        int n = sar.length;
        for (int i = 0; i < n; i++) 
            sar[i] = toIso88591String(sar[i]);
        return sar;
    }

    /**
     * This returns the UTF-8 encoding of the string (or null if trouble).
     * The inverse of this is utf8BytesToString.
     * This won't throw an exception and returns ERROR (as bytes) if trouble.
     */
    public static byte[] stringToUtf8Bytes(String s) {
        try {
            return s.getBytes(UTF_8);   
        } catch (Exception e) {  //danger is invalid encoding name -- won't happen
            log("Caught " + ERROR + " in String2.stringToUtf8Bytes(" + s + "): " + 
                MustBe.throwableToString(e));
            return new byte[]{59, 92, 92, 79, 92}; //ERROR
        }
    }

    /**
     * This returns a string from the UTF-8 encoded byte[] (or ERROR if trouble).
     * The inverse of this is stringToUtf8Bytes.
     * This won't throw an exception and returns ERROR if trouble.
     */
    public static String utf8BytesToString(byte[] bar) {
        try {
            return new String(bar, UTF_8); 
            //alternatively, you could use (char)bar[i]
        } catch (Exception e) { //danger is invalid UTF-8
            log("Caught " + ERROR + " in String2.utf8BytesToString(" + toCSSVString(bar) + "): " + 
                MustBe.throwableToString(e));
            return ERROR; 
        }
    }

    /**
     * A little weird: This returns the UTF-8 encoding of the string as a String 
     * (using only the lower byte of each 2-byte char),
     * so a unicode string can be stored in a 1-byte/char string. 
     * This won't throw an exception and returns ERROR (as bytes) if trouble.
     */
    public static String stringToUtf8String(String s) {
        try {
            return new String(s.getBytes(UTF_8), 0);  //0=highByte  this is deprecated but useful
        } catch (Exception e) {
            log("Caught " + ERROR + " in String2.stringToUtf8String(" + s + "): " + 
                MustBe.throwableToString(e));
            return ERROR; 
        }
    }

    /**
     * A little weird: This returns the unicode string from a UTF-8 encoded String.
     * This won't throw an exception and returns ERROR (as bytes) if trouble.
     */
    public static String utf8StringToString(String s) {
        try {
            return new String(toByteArray(s), UTF_8);             
        } catch (Exception e) {
            log("Caught " + ERROR + " in String2.utf8StringToString(" + s + "): " + 
                MustBe.throwableToString(e));
            return ERROR; 
        }
    }


    /**
     * This creates the jump table (int[256]) for a given 'find' stringUtf8
     * of use by indexOf(byte[], byte[], jumpTable[]) below.
     * Each entry in the result is: how far for indexOf to jump endPo forward for any given s[endPo] byte.
     *
     * @param find the byte array to be found
     * @return jump table (int[256]) for a given 'find' stringUtf8
     */
    public static int[] makeJumpTable(byte[] find) {
        //work forwards so last found instance of a letter is most important
        //   s = Two times nine.
        //find = nine
        //First test will compare find's 'e' and s's ' '
        //Not a match, so jump jump[' '] positions forward.
        int findLength = find.length;
        int jump[] = new int[256];
        Arrays.fill(jump, findLength);
        for (int po = 0; po < findLength; po++)
            jump[find[po] & 0xFF] = findLength - 1 - po;   //make b 0..255
        return jump;
    }


    /**
     * Return the first index of 'find' in s (or -1 if not found).
     * Idea: since a full text search entails looking for a few 'find' strings
     *   inside any of nDatasets long searchStrings (that don't change often),
     *   and since we don't really care about exact index, just relative index,
     *   it would be nice to store searchStrings as byte[]
     *   (1/2 the memory use and simpler search).
     *   So encode 'find' and searchString as UTF-8 via byte[] find.getBytes(utf8Charset).
     *   Then we can do Boyer-Moore-like search for first indexOf.
     *   This can speed up the searches ~3.5X in good conditions (assuming setup is amortized).
     *
     * @param s the long string to be search, stored utf8.
     * @param find the short string to be found, stored utf 8.
     * @param jumpTable from makeJumpTable
     * @return the first index of 'find' in s (or -1 if not found).
     */
    public static int indexOf(byte[] s, byte[] find, int jumpTable[]) {
        //future: is jump table for second character jumpTable[s[endPo]]-1 IFF that value isn't <=0?

        //see algorithm in makeJumpTable
        int findLength = find.length;
        int sLength = s.length;
        if (findLength == 0)  return 0;
        if (sLength == 0)     return -1;
        int findLength1 = findLength - 1;
        int endPo = findLength1;
        byte lastFindByte = find[findLength1]; 

        //if findLength is 1, do simple search
        if (findLength == 1) {
            int po = -1;
            while (++po < sLength) {
                if (s[po] == lastFindByte)
                    return po;  
            }
            return -1;
        }

        //Boyer-Moore-like search
        whileBlock:
        while (endPo < sLength) {
            byte b = s[endPo];

            //last bytes don't match? jump
            if (b != lastFindByte) {
                endPo += jumpTable[b & 0xFF]; //make b 0..255
                continue;
            }

            //last bytes do match: try to match all of 'find'
            int countBack = 1;
            do {  //we know find is at least 2 long
                if (s[endPo - countBack] == find[findLength1 - countBack]) {
                   countBack++;
                } else {
                    endPo += 1;
                    continue whileBlock;
                }
            } while (countBack < findLength); 

            //found it!
            return endPo - findLength1;
        }
        return -1;
    } 

    /**
     * This returns the MD5 hash digest of stringToUtf8Bytes(password) as a String of 32 lowercase hex digits.
     * Lowercase because the digest authentication standard uses lower case; so mimic them.
     * And lowercase is easier to type.
     * 
     * @param password  the text to be digested
     * @return the MD5 hash digest of the password (32 lowercase hex digits, as a String),
     *   or null if password is null or there is trouble.
     */
    public static String md5Hex(String password) {
        return passwordDigest("MD5", password);
    }

    /**
     * This returns the hash digest of stringToUtf8Bytes(password) as a String of lowercase hex digits.
     * Lowercase because the digest authentication standard uses lower case; so mimic them.
     * And lowercase is easier to type.
     * 
     * @param algorithm one of the FILE_DIGEST_OPTIONS
     * @param password  the text to be digested
     * @return the algorithm's hash digest of the password (many lowercase hex digits, as a String),
     *   or null if password is null or there is trouble.
     */
    public static String passwordDigest(String algorithm, String password) {
        try {
            if (password == null) return null;
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(stringToUtf8Bytes(password));
            byte bytes[] = md.digest();
            int nBytes = bytes.length;
            StringBuilder sb = new StringBuilder(nBytes * 2);
            for (int i = 0; i < nBytes; i++)
                sb.append(zeroPad(Integer.toHexString(
                    (int)bytes[i] & 0xFF), 2));   //safe, (int) and 0xFF make it unsigned byte
            return sb.toString();
        } catch (Throwable t) {
            log(MustBe.throwableToString(t));
            return null;
        }
    }

    /** Java only guarantees that the first 3 of these will be supported. */
    public static final String FILE_DIGEST_OPTIONS[]    = {"MD5",  "SHA-1", "SHA-256", "SHA-384", "SHA-512" };
    public static final String FILE_DIGEST_EXTENSIONS[] = {".md5", ".sha1", ".sha256", ".sha384", ".sha512"}; //Bagit likes these (after the '.')

    /**
     * This returns a hash digest of fullFileName (read as bytes)
     * as a String of lowercase hex digits.
     * Lowercase because the digest authentication standard uses lower case; so mimic them.
     * And lowercase is easier to type.
     * 
     * @param useBase64 If true, this returns the digest as a base64 string.
     *   If false, this returns the digest as a hex string.
     * @param algorithm one of the FILE_DIGEST_OPTIONS ("MD5", "SHA-1", "SHA-256", ...).
     * @param fullFileName  the name of the file to be digested
     * @return the hash digest of the file
     *   (for MD5, 32 lowercase hex digits as a String)
     * @throws Exception if real trouble
     */
    public static String fileDigest(boolean useBase64, String algorithm, String fullFileName) 
        throws Exception {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        //below not File2.getDecompressedBufferedInputStream() because want file digest of archive
        InputStream fis = new BufferedInputStream(new FileInputStream(fullFileName));
        try {
            byte buffer[] = new byte[8192];
            int nBytes;
            while ((nBytes = fis.read(buffer)) >= 0) 
                md.update(buffer, 0, nBytes);
        } finally {
            fis.close();
        }
        byte bytes[] = md.digest();
        if (useBase64) {
            return new String(Base64.encodeBase64(bytes));
        } else {
            int nBytes = bytes.length;
            StringBuilder sb = new StringBuilder(nBytes * 2);
            for (int i = 0; i < nBytes; i++)
                sb.append(zeroPad(Integer.toHexString(
                    (int)bytes[i] & 0xFF), 2));   //safe, (int) and 0xFF make it unsigned byte
            return sb.toString();
        }
    }

    /* This variant returns the digest as a hex string. */
    public static String fileDigest(String algorithm, String fullFileName) throws Exception {
        return fileDigest(false, algorithm, fullFileName);
    }

    /** 
     * This returns the last 12 hex digits from md5Hex (or null if md5 is null),
     * broken into 3 blocks of 4 digits, separated by '_'.
     * I use this as a short, easy to type, repeatable, representation of 
     * long strings (e.g., an ERDDAP query URL), sort of like the idea of tinyURL.
     * It performs much better than hashcode or CRC32 when a large number of passwords
     * (or filenames) are encoded and you don't want any collisions.
     * See Projects.testHashFunctions.
     */
    public static String md5Hex12(String password) {
        String s = md5Hex(password);
        return s == null? null : 
            s.substring(20, 24) + "_" + s.substring(24, 28) + "_" + s.substring(28, 32);
    }

    /**
     * Given two strings with internal newlines, oldS and newS, this a message
     * indicating where they differ.
     * 
     * @param oldS  
     * @param newS
     * @return a message indicating where they differ, or "" if there is no difference.
     */
    public static String differentLine(String oldS, String newS) {
        if (oldS == null) return "(There is no old version.)";
        if (newS == null) return "(There is no new version.)";
        int oldLength = oldS.length();
        int newLength = newS.length();
        int newlinePo = -1;
        int line = 1;
        int n = Math.min(oldLength, newLength);
        int po = 0;
        while (po < n && oldS.charAt(po) == newS.charAt(po)) {
            if (oldS.charAt(po) == '\n') {newlinePo = po; line++;}
            po++;
        }
        if (po == oldLength && po == newLength)
            return "";
        int oldEnd = newlinePo + 1;
        int newEnd = newlinePo + 1;
        while (oldEnd < oldLength && oldS.charAt(oldEnd) != '\n') oldEnd++;
        while (newEnd < newLength && newS.charAt(newEnd) != '\n') newEnd++;
        return 
            "  old line #" + line + "=" + toJson(oldS.substring(newlinePo + 1, oldEnd)) + ",\n" +
            "  new line #" + line + "=" + toJson(newS.substring(newlinePo + 1, newEnd)) + ".";
    }

    /** 
     * This converts a double to a rational number (m * 10^t).
     * This is similar to Math2.mantissa and Math2.intExponent, but works via string manipulation
     * to avoid roundoff problems (e.g., with 6.6260755e-24).
     * 
     * @param d
     * @return int[2]: [0]=m, [1]=t.
     *  (or {0, 0} if d=0, or {1, Integer.MAX_VALUE} if !finite(d))
     */
    public static int[] toRational(double d) {        
        if (d == 0)
            return new int[]{0, 0};

        if (!Double.isFinite(d))
            return new int[]{1, Integer.MAX_VALUE};

        String s = "" + d; //-12.0 or 6.6260755E-24
        //String2.log("\nd=" + d + "\ns=" + s);
        int ten = 0;

        //remove the e
        int epo = s.indexOf('E');
        if (epo > 0) {
            ten = parseInt(s.substring(epo + 1));
            s = s.substring(0, epo);
            //String2.log("remove E s=" + s + " ten=" + ten);
        }

        //remove .0; remove decimal point
        if (s.endsWith(".0"))
            s = s.substring(0, s.length() - 2);
        int dpo = s.indexOf('.');
        if (dpo > 0) {
            ten -= s.length() - dpo - 1;
            s = s.substring(0, dpo) + s.substring(dpo + 1);
            //String2.log("remove . s=" + s + " ten=" + ten);
        }

        //convert s to long
        //need to lose some precision?
        long tl = parseLong(s);    
        //String2.log("tl=" + tl + " s=" + s);
        while (Math.abs(tl) > 1000000000) {
            tl = Math.round(tl / 10.0);
            ten++;
            //String2.log("tl=" + tl + " ten=" + ten);
        }
        //remove trailing 0's
        while (tl != 0 && tl / 10 == tl / 10.0) {
            tl /= 10;
            ten++;
            //String2.log("remove 0 tl=" + tl + " ten=" + ten);
        }
        //add up to 3 0's?
        if (tl < 100000 && ten >= 1 && ten <= 3) {
            while (ten > 0) {
                tl *= 10;
                ten--;
            }
        }

        return new int[]{(int)tl, ten}; //safe since large values handled above
    }

    /**
     * This is different from String2.modifyToBeFileNameSafe --
     *   this encodes non-fileNameSafe characters so little or no information is lost.
     * <br>This returns the string with just file-name-safe characters (0-9, A-Z, a-z, _, -, .).
     * <br>'x' and non-safe characters are CONVERTED to 'x' plus their 
     *   2 lowercase hexadecimalDigit number or "xx" + their 4 hexadecimalDigit number.
     * <br>See posix fully portable file names at https://en.wikipedia.org/wiki/Filename .
     * <br>When the encoding is more than 25 characters, this stops encoding and 
     *   adds "xh" and the hash code for the entire original string,
     *   so the result will always be less than ~41 characters.
     *
     * <p>THIS WON'T BE CHANGED. FILE NAMES CREATED FOR EDDGridCopy and EDDTableCopy 
     *  DEPEND ON SAME ENCODING OVER TIME.
     *
     * @param s  
     * @return s with all of the non-fileNameSafe characters changed.
     *    <br>If s is null, this returns "x-1".
     *    <br>If s is "", this returns "x-0".
     */
    public static String encodeFileNameSafe(String s) {
        if (s == null)
            return "x-1";
        int n = s.length();
        if (n == 0)
            return "x-0";
        StringBuilder sb = new StringBuilder(n / 3 * 4);
        for (int i = 0; i < n; i++) {
            if (sb.length() >= 25) {
                sb.append("xh" + md5Hex12(s)); //was Math2.reduceHashCode(s.hashCode()));
                break;
            }
            char ch = s.charAt(i);

            if (ch != 'x' && isFileNameSafe(ch)) {
                sb.append(ch);
            } else if (ch <= 255) {
                sb.append("x" + zeroPad(Integer.toHexString(ch), 2));
            } else {
                sb.append("xx" + zeroPad(Integer.toHexString(ch), 4));
            }
        }

        return sb.toString();
    }

    /**
     * This is like encodeFileNameSafe, but further restricts the name to
     * <ul>
     * <li>first character must be A-Z, a-z, _.
     * <li>subsequent characters must be A-Z, a-z, _, 0-9.
     * </ul>
     * 2016-06-16: DEPRECATED.
     *   I THINK THAT RESTRICTION CLAIM ISN'T TRUE. BUT LEAVE THIS AS IS.
     *   RECOMMEND: USE NEW encodeMatlabNameSafe FOR NEW USES.
     * <br>'x' and non-safe characters are CONVERTED to 'x' plus their 
     *   2 lowercase hexadecimalDigit number or "xx" + their 4 hexadecimalDigit number.
     * <br>See posix fully portable file names at https://en.wikipedia.org/wiki/Filename .
     * <br>When the encoding is more than 25 characters, this stops encoding and 
     *   adds "xh" and the hash code for the entire original string,
     *   so the result will always be less than ~41 characters.
     *
     * <p>THIS WON'T BE CHANGED. FILE NAMES CREATED FOR EDDGridFromFile and EDDTableFromFile 
     *  DEPEND ON SAME ENCODING OVER TIME.
     *
     * @param s  
     * @return s with all of the non-variableNameSafe characters changed.
     *    <br>If s is null, this returns "x_1".
     *    <br>If s is "", this returns "x_0".
     */
    public static String encodeVariableNameSafe(String s) {
        if (s == null)
            return "x_1";
        int n = s.length();
        if (n == 0)
            return "x_0";
        StringBuilder sb = new StringBuilder(n / 3 * 4);
        for (int i = 0; i < n; i++) {
            if (sb.length() >= 25) {
                sb.append("xh" + md5Hex12(s)); //was Math2.reduceHashCode(s.hashCode()));
                break;
            }
            char ch = s.charAt(i);

            //THIS ISN'T RIGHT because isFileNameSafe now allows high ASCII letters! BUT LEAVE IT AS IS.
            if (ch != 'x' && isFileNameSafe(ch) && ch != '-' && ch != '.' &&
                (i > 0 || ((ch >= 'A' && ch <= 'Z') || (ch >='a' && ch <='z') || (ch == '_')))) {
                sb.append(ch);
            } else if (ch <= 255) {
                sb.append("x" + zeroPad(Integer.toHexString(ch), 2));
            } else {
                sb.append("xx" + zeroPad(Integer.toHexString(ch), 4));
            }
        }

        return sb.toString();
    }

    /**
     * This is like encodeFileNameSafe, but further restricts the name to
     * <ul>
     * <li>first character must be A-Z, a-z.
     * <li>subsequent characters must be A-Z, a-z, _, 0-9.
     * </ul>
     * <br>'x' and non-safe characters are CONVERTED to 'x' plus their 
     *   2 lowercase hexadecimalDigit number or "xx" + their 4 hexadecimalDigit number.
     * <br>See posix fully portable file names at https://en.wikipedia.org/wiki/Filename .
     * <br>When the encoding is more than 25 characters, this stops encoding and 
     *   adds "xh" and the hash code for the entire original string,
     *   so the result will always be less than ~41 characters.
     * <br>This meets MatLab restrictions:
     *   https://www.mathworks.com/help/matlab/ref/matlab.lang.makevalidname.html
     *
     * <p>THIS WON'T BE CHANGED. FILE NAMES CREATED FOR EDDGridFromFile and EDDTableFromFile 
     *  DEPEND ON SAME ENCODING OVER TIME.
     *
     * @param s  
     * @return s with all of the non-variableNameSafe characters changed.
     *    <br>If s is null, this returns "x_1".
     *    <br>If s is "", this returns "x_0".
     */
    public static String encodeMatlabNameSafe(String s) {
        if (s == null)
            return "x_1";
        int n = s.length();
        if (n == 0)
            return "x_0";
        StringBuilder sb = new StringBuilder(4 * Math.min(50, n) / 3);
        for (int i = 0; i < n; i++) {
            if (sb.length() >= 25) {
                sb.append("xh" + md5Hex12(s)); //was Math2.reduceHashCode(s.hashCode()));
                break;
            }
            char ch = s.charAt(i);

            if (ch == 'x') {
                sb.append("x" + zeroPad(Integer.toHexString(ch), 2));
            } else if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) { //1st chars
                sb.append(ch);
            } else if (i > 0 && ((ch >= '0' && ch <= '9') || ch == '_')) { //subsequent chars
                sb.append(ch);
            } else if (ch <= 255) {  //others
                sb.append("x" + zeroPad(Integer.toHexString(ch), 2));
            } else { //others
                sb.append("xx" + zeroPad(Integer.toHexString(ch), 4));
            }
        }

        return sb.toString();
    }

    /**
     * This is like encodeMatlabNameSafe, but simpler and won't always retain all the info.
     * <ul>
     * <li>first character must be A-Z, a-z.
     * <li>subsequent characters must be A-Z, a-z, _, 0-9.
     * </ul>
     * <br>non-safe characters are some safe variant.
     * <br>See posix fully portable file names at https://en.wikipedia.org/wiki/Filename .
     * <br>When the encoding is more than 25 characters, this stops encoding and 
     *   adds "xh" and the hash code for the entire original string,
     *   so the result will always be less than ~41 characters.
     * <br>This meets MatLab restrictions:
     *   https://www.mathworks.com/help/matlab/ref/matlab.lang.makevalidname.html
     *
     * <p>THIS WON'T BE CHANGED. SOME datasetIDs DEPEND ON SAME ENCODING OVER TIME.
     *
     * @param s  
     * @return s with all of the non-variableNameSafe characters changed.
     *    <br>If s is null, this returns "null_".
     *    <br>If s is "", this returns "nothing_".
     */
    public static String simpleMatlabNameSafe(String s) {
        if (s == null)
            return "null_";
        int n = s.length();
        if (n == 0)
            return "nothing_";
        s = modifyToBeASCII(s);
        StringBuilder sb = new StringBuilder(4 * Math.min(50, n) / 3);
        for (int i = 0; i < n; i++) {
            if (sb.length() >= 25) {
                sb.append("_" + md5Hex12(s)); //was Math2.reduceHashCode(s.hashCode()));
                break;
            }
            char ch = s.charAt(i);

            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) { //1st chars
                sb.append(ch);
            } else if (sb.length() > 0 && ch >= '0' && ch <= '9') { //subsequent chars
                sb.append(ch);
            //all other chars get converted to '_'
            //but '_' can't be first char and no two '_' in a row
            } else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_') { 
                sb.append('_');
            }
        }
        if (sb.length() == 0)
            sb.append("a_");

        return sb.toString();
    }


    /** 
     * Get gets the String from the system clipboard
     * (or null if none).
     * This works in a standalone Java program, not an applet.
     * From Java Developers Almanac.
     * This won't throw an exception.
     */
    public static String getClipboardString() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable t = clipboard.getContents(null);    
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) 
                return (String)t.getTransferData(DataFlavor.stringFlavor);
        } catch (Throwable th) {
            log(ERROR + " while getting the string from the clipboard:\n" +
                MustBe.throwableToString(th));
        }
        return null;
    }
    
    /** This method writes a string to the system clipboard.
     * This works in a standalone Java program, not an applet.
     * From Java Developers Almanac.
     * This won't throw an exception.
     */
    public static void setClipboardString(String s) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(s), null);
        } catch (Throwable t) {
            log(ERROR + " while putting the string on the clipboard:\n" +
                MustBe.throwableToString(t));
        }
    }



    /**
     * This replaces each of the substrings with the canonical String.
     * If any is null, it is left as null.
     *
     * @return the same array, for convenience. null returns null.
     */
    public static String[] canonical(String sar[]) {
        if (sar == null)
            return null;
        int n = sar.length;
        for (int i = 0; i < n; i++)
            sar[i] = canonical(sar[i]);
        return sar;
    }

    /** 
     * This is like String.intern(), but uses a WeakHashMap so the canonical strings 
     * can be garbage collected.
     * <br>This is thread safe.
     * <br>It is fast: ~0.002ms per call.
     * <br>See TestUtil.testString2canonical().
     *
     * <p>Using this increases memory use by ~6 bytes per canonical string
     * (4 for pointer * ~.5 hashMap load factor).
     * <br>So it only saves memory if many strings would otherwise be duplicated.
     * <br>But if lots of strings are originally duplicates, it saves *lots* of memory.
     *
     * @param s  the string   (may be null)  (may be from s2.substring(start, stop))
     * @return a canonical string with the same characters as s.
     */
    public static String canonical(String s) {
        if (s == null)
            return null;
        if (s.length() == 0)
            return "";
        //generally, it slows things down to see if same as last canonical String.
        char ch0 = s.charAt(0);
        Map tCanonicalMap = canonicalMap[
            ch0 < 'A'? 0 : ch0 < 'N'? 1 : //divide uppercase into 2 parts
            ch0 < 'a'? 2 : ch0 < 'j'? 3 : //divide lowercase into 3 parts
            ch0 < 'r'? 4 : 5];
       
        //faster and logically better to synchronized(canonicalMap) once 
        //  (and use a few times in consistent state)
        //than to synchronize canonicalMap and lock/unlock twice
        synchronized (tCanonicalMap) {
            WeakReference wr = (WeakReference)tCanonicalMap.get(s);
            //wr won't be garbage collected, but reference might (making wr.get() return null)
            String canonical = wr == null? null : (String)(wr.get());
            if (canonical == null) {
                //For proof that new String(s.substring(,)) is just storing relevant chars,
                //not a reference to the parent string, see TestUtil.testString2canonical2()
                canonical = new String(s); //in case s is from s2.substring, copy to be just the characters
                tCanonicalMap.put(canonical, new WeakReference(canonical));
                //log("new canonical string: " + canonical);
            }
            return canonical;
        }
    }

    /** This is only used to test canonical. */
    public static int canonicalSize() {
        int sum = 0;
        for (int i = 0; i < canonicalMap.length; i++)
            sum += canonicalMap[i].size();
        return sum;
    }

    /** 
     * For command line parameters, this returns toJson(s) if the string is empty or contains 
     * special characters or single or double quotes or backslash; otherwise it return s.
     */
    public static String quoteParameterIfNeeded(String s) {
        return s.length() > 0 && isPrintable(s) && 
            s.indexOf('\"') < 0 && s.indexOf('\'') < 0 && 
            s.indexOf('\\') < 0? s : 
            toJson(s);
    }



    /* *
     * This makes a medium-deep clone of an ArrayList by calling clone() of
     * each element of the ArrayList.
     *
     * @param oldArrayList
     * @param newArrayList  If oldArrayList is null, this returns null.
     *    Elements of oldArrayList can be null.
     */
    /* I couldn't make this compile. clone throws an odd exception.
    public ArrayList clone(ArrayList oldArrayList) {
        if (oldArrayList == null)
            return (ArrayList)null;

        ArrayList newArrayList = new ArrayList();
        int n = oldArrayList.size();
        for (int i = 0; i < n; i++) {
            Object o = oldArrayList.get(i);
            try {
                if (o != null) o = o.clone();
            } catch (Exception e) {
            }
            newArrayList.add(o);
        }
        return newArrayList;
    } */

    /** This changes the characters case to title case (only letters after non-letters are
     * capitalized).  This is simplistic (it doesn't know about acronyms or pH or ...).
     */
    public static String toTitleCase(String s) {
        if (s == null)
            return null;
        int sLength = s.length();
        StringBuilder sb = new StringBuilder(s);
        char c = ' ', oc = ' ';
        for (int i = 0; i < sLength; i++) {
            oc = c;
            c = sb.charAt(i);
            if (isLetter(c)) 
                sb.setCharAt(i, isLetter(oc)? Character.toLowerCase(c): Character.toUpperCase(c));
        }
        return sb.toString();
    }

    /** This changes the character's case to sentence case 
     * (first letter and first letter after each period capitalized).  This is simplistic.
     */
    public static String toSentenceCase(String s) {
        if (s == null)
            return null;
        int sLength = s.length();
        StringBuilder sb = new StringBuilder(s);
        boolean capNext = true;
        for (int i = 0; i < sLength; i++) {
            char c = sb.charAt(i);
            if (isLetter(c)) {
                if (capNext) {
                    sb.setCharAt(i, Character.toUpperCase(c));
                    capNext = false;
                } else {
                    sb.setCharAt(i, Character.toLowerCase(c));
                }
            } else if (c == '.') {
                capNext = true;
            }
        }
        return sb.toString();
    }

    /** This suggests a camel-case variable name.
     * 
     * @param s the starting string for the variable name.
     * @return a valid variable name asciiLowerCaseLetter+asciiDigitLetter*, using camel case.
     *   This is a simplistic suggestion. Different strings may return the same variable name.
     *   null returns "null".
     *   "" returns "a".
     */
    public static String toVariableName(String s) {
        if (s == null)
            return "null";
        int sLength = s.length();
        if (sLength == 0)
            return "a";
        s = modifyToBeASCII(s);
        s = toTitleCase(s);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sLength; i++) {
            char c = s.charAt(i);
            if (isDigitLetter(c))
                sb.append(c);
        }
        if (sb.length() == 0)
            return "a";
        char c = sb.charAt(0);
        sb.setCharAt(0, Character.toLowerCase(c));
        if (c >= '0' && c <= '9')
            sb.insert(0, 'a');
        return sb.toString();
    }

    /** 
     * This converts "camelCase99String" to "Camel Case 99 String"
     * 
     * @param s the camel case string.
     * @return the string with spaces before capital letters.
     *   null returns null.
     *   "" returns "".
     */
    public static String camelCaseToTitleCase(String s) {

        //change 
        //  but don't space out an acronym, e.g., E T O P O
        //  and don't split hyphenated words, e.g.,   Real-Time
        if (s == null)
            return null;
        int n = s.length();
        if (n <= 1)
            return s;
        StringBuilder sb = new StringBuilder(n + 10);
        sb.append(Character.toUpperCase(s.charAt(0)));
        for (int i = 1; i < n; i++) {
            char chi1 = s.charAt(i - 1);
            char chi  = s.charAt(i);
            if (Character.isLetter(chi)) {
                if (chi1 == Character.toLowerCase(chi1) && Character.isLetterOrDigit(chi1) &&
                    chi  != Character.toLowerCase(chi )) 
                    sb.append(' ');
            } else if (Character.isDigit(chi)) {
                if (chi1 == Character.toLowerCase(chi1) && Character.isLetter(chi1)) 
                    sb.append(' ');
            }
            sb.append(chi);
        }
        return sb.toString();
    }

    /**
     * This returns true if the string contains only ISO 8859-1 characters (i.e., 0 - 255).
     */
    public static boolean isIso8859(String s) {
        int sLength = s.length();
        for (int i = 0; i < sLength; i++) 
            if (s.charAt(i) > 255) return false;
        return true;
    }

    /** This returns true if s isn't null and s.trim().length() &gt; 0. */
    public static boolean isSomething(String s) {
        return s != null && s.trim().length() > 0;
    }

    /** This returns true if s isn't null, "", "-", "null", "nd", "N/A", "...", "???", etc. */
    public static boolean isSomething2(String s) {
        //Some datasets have "" for an attribute.

        //Some datasets have comment="..." ,e.g.,
        //http://edac-dap.northerngulfinstitute.org/thredds/dodsC/ncom/region1/ncom_glb_reg1_2010013000.nc.das
        //which then prevents title from being generated

        //some have "-", e.g.,
        //http://dm1.caricoos.org/thredds/dodsC/content/wrf_archive/wrfout_d01_2009-09-25_12_00_00.nc.das
        if (s == null || s.length() == 0)
            return false;
        s = s.trim().toLowerCase();
        if (s.length() == 0) //may be now (not before) because of trim()
            return false;

        //"nd" (used by BCO-DMO) and "other" (since it is often part of a vocabulary) 
        //are purposely not on the list.

        //if lots of words, switch to hash set.
        char ch = s.charAt(0);
        if (s.length() == 1) {
            return ".-?".indexOf(ch) < 0;
        } else if (ch == 'n') {
            return !(s.equals("n/a")  || s.equals("na") || 
                     s.equals("nd") ||
                     s.equals("none") || s.equals("none.") || 
                     s.equals("not applicable") || 
                     s.equals("null"));
        } else if (ch == 'u') {
            return !(s.equals("unknown") || s.equals("unspecified"));
        } else {
            return !(s.equals("...") || s.equals("???"));
        }
    }


    /**
     * This returns the text to make n system beeps if printed to the console.
     */
    public static String beep(int n) {
        return makeString('\u0007', n);
    }

    /**
     * This cleverly concatenates the 2 strings (with "", ". ", or " ", as appropriate.
     */
    public static String periodSpaceConcat(String a, String b) {
        if (!isSomething(a)) 
            return isSomething(b)? b : "";
        //we know 'a' isSomething
        a = a.trim();
        if (!isSomething(b)) 
            return "";
        //we know 'b' isSomething
        b = b.trim();
        return a + 
            (".!?;,".indexOf(a.charAt(a.length() - 1)) >= 0? " " : ". ") +
            b;
    }

    /**
     * This cleverly concatenates the 2 strings (with separator, as appropriate).
     * 
     * @param a may be null or "" or something
     * @param separator will only be used if a and b are something.
     * @param b may be null or "" or something
     * @return a.trim(), a.trim()+separator+b.trim(), b.trim(), or ""
     */
    public static String ifSomethingConcat(String a, String separator, String b) {
        if (isSomething(a)) 
             return isSomething(b)? a.trim() + separator + b.trim() : a.trim();
        else return isSomething(b)? b.trim() : "";
    }

    /**
     * This cleverly concatenates the 2 strings (with separator, as appropriate).
     * Afterwards, a will have a, a+separator+b.trim(), b.trim(), or ""
     * 
     * @param a may be null or "" or something
     * @param separator will only be used if a and b are something.
     * @param b may be null or "" or something
     */
    public static void ifSomethingConcat(StringBuilder a, String separator, String b) {
        if (a.length() > 0) 
             a.append(isSomething(b)? separator + b.trim() : "");
        else a.append(isSomething(b)? b.trim() : "");
    }

    /** 
     * Given an Amazon AWS S3 URL, this returns the bucketName.
     * http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGET.html
     * If files have file-system-like names, e.g., 
     *   https?://(bucketName).s3.amazonaws.com/(prefix)
     *   where the prefix is usually in the form dir1/dir2/fileName.ext
     *   http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_NorESM1-M_209601-209912.nc
     *
     * @param url
     * @return the bucketName or null if not an s3 URL
     */
    public static String getAwsS3BucketName(String url) {
        if (url == null)
            return null;
        if (url.endsWith(".s3.amazonaws.com"))
            url = File2.addSlash(url);
        Matcher matcher = AWS_S3_PATTERN.matcher(url); 
        if (matcher.matches()) 
            return matcher.group(1); //bucketName
        return null;
    }

    /** 
     * Given an Amazon AWS S3 URL, this returns the objectName or prefix.
     * http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGET.html
     * If files have file-system-like names, e.g., 
     *   https?://(bucketName).s3.amazonaws.com/(prefix)
     *   where a prefix is usually in the form dir1/dir2/ 
     *   where an objectName is usually in the form dir1/dir2/fileName.ext
     *   http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/tasmin_amon_BCSD_rcp26_r1i1p1_CONUS_NorESM1-M_209601-209912.nc
     *
     * @param url  If the url is supposed to be for a prefix, use 
     *    getAwsS3Prefix(File2.addSlash(url))
     * @return the prefix or null if not an s3 URL
     */
    public static String getAwsS3Prefix(String url) {
        if (url == null)
            return null;
        if (url.endsWith(".s3.amazonaws.com"))
            url = File2.addSlash(url);
        Matcher matcher = AWS_S3_PATTERN.matcher(url); 
        if (matcher.matches()) 
            return matcher.group(2); //prefix
        return null;
    }

    /** 
     * This provides an startsWith() method for StringBuilder, which has none!
     *
     * @return true if sb starts with pre (including if pre=""), otherwise returns false (including
     *    if sb or pre is null).
     */
    public static boolean startsWith(StringBuilder sb, String pre) {
        if (sb == null || pre == null || pre.length() > sb.length())
            return false;
        return sb.substring(0, pre.length()).equals(pre);
    }

    /** 
     * This provides an endsWith() method for StringBuilder, which has none!
     *
     * @return true if sb ends with suffix (including if suffix=""), otherwise returns false (including
     *    if sb or suffix is null).
     */
    public static boolean endsWith(StringBuilder sb, String suffix) {
        if (sb == null || suffix == null || suffix.length() > sb.length())
            return false;
        return sb.substring(sb.length() - suffix.length()).equals(suffix);
    }

    /** 
     * This adds a newline to sb if sb.length() &gt; 0 and !endsWith("\n").
     * @return sb for convenience
     */
    public static StringBuilder addNewlineIfNone(StringBuilder sb) {
        if (sb == null || sb.length() == 0)
            return sb;
        if (!endsWith(sb, "\n"))
            sb.append('\n');
        return sb;
    }

    /**
     * Validate ACDD contact type (case insensitive search, case sensitive return),
     * or return null no match.
     */
     public static String validateAcddContactType(String value) {
        int which = caseInsensitiveIndexOf(ACDD_CONTACT_TYPES, value); 
        return which < 0? null : ACDD_CONTACT_TYPES[which];
     }


    /**
     * Guess the ACDD contact type, or return null if new pretty sure.
     */
     public static String guessAcddContactType(String name) {
        //guess publisher_type        
        //order of tests is important
        //position is rare
        if (name.matches(ACDD_PERSON_REGEX1)) 
            return "person";
        if (name.matches(ACDD_GROUP_REGEX)) 
            return "group";
        if (name.matches(ACDD_INSTITUTION_REGEX1)) 
            return "institution";
        if (name.matches(ACDD_INSTITUTION_REGEX2))
            return "institution";
        if (name.matches(ACDD_PERSON_REGEX2)) 
            return "person";
        //if not pretty sure, don't specify
        return null;
     }

     /**
      * Convert plain text to simple regex by backslash encoding
      * all special regex chars.
      */
     public static String plainTextToRegex(String s) {
         int n = s.length();
         StringBuilder sb = new StringBuilder(n + 32);
         for (int i = 0; i < n; i++) {
             char ch = s.charAt(i);
             if ("\\^$.?*+|{}[]()".indexOf(ch) >= 0)  // , -
                 sb.append('\\');
             sb.append(ch);
         }
         return sb.toString();
     }

     /**
      * This lists the methods for a given object's class.
      */
     public static void listMethods(Object v) {
        Class tClass = v.getClass();
        Method[] methods = tClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            String2.log("public method #" + i + ": " + methods[i]);
        }
     }


} //End of String2 class.
