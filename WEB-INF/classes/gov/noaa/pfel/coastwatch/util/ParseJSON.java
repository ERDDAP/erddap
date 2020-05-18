/* 
 * ParseJSON Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * This facilitates parsing JSON information.
 * See http://json.org/ .
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2008-01-11
 *
 */
public class ParseJSON {


    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;
    public static boolean reallyVerbose = false;

    private Reader reader;
    private int lineNumber = 1;
    private int linePo = 0; //char number on current line; 0 before first char read
    protected int pushedChar = -1;

    /** The constructor. */
    public ParseJSON(Reader reader) {
        this.reader = reader;
    }
    
    /** The constructor. */
    public ParseJSON(String json) {
        this.reader = new StringReader(json);
    }

    /**
     * When done, you should close the reader (but not essential).
     */
    public void close() {
        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }
        } catch (Exception e) {
        }
    }

    /** 
     * Users of this class shouldn't call this -- use close() instead.
     * Java calls this when an object is no longer used, just before garbage collection. 
     * 
     */
    protected void finalize() throws Throwable {
        try {  //extra insurance
            close();
        } catch (Exception e) {
        }
        super.finalize();
    }

    /**
     * This returns the current line number in the reader.
     *
     * @return the current line number.
     */
    public int lineNumber() {
        return lineNumber;
    }

    /**
     * This returns the current character number on the current line in the reader.
     *
     * @return the current character number on the current line in the reader.
     */
    public int linePo() {
        return linePo;
    }

    /**
     * This returns the current position, e.g., "on line#7 at character#20.".
     * @return the current position, e.g., "on line#7 at character#20.".
     */
    public String onLine() {
        return " on line #" + lineNumber + " at character #" + linePo + ".";
    }

    /**
     * This returns the next character from the Reader (in the simplest sense).
     * If the end-of-reader is reached, this doesn't close the reader and returns -1.
     * 
     * @return the next char
     * @throws Exception if trouble
     */
    public int read() throws Exception {
        if (pushedChar == -1) {
            int tc = reader.read(); //this is the only place that calls reader.read, so keep track of lineNumber
            if (tc == 10) {
                lineNumber++;  //only increment when actually read (not if pushedChar)
                linePo = 0;
            } else {
                linePo++;
            }
            if (reallyVerbose) String2.log("" + (char)tc);
            return tc;
        } else {
            int tc = pushedChar;
            pushedChar = -1;
            if (reallyVerbose) String2.log("pushedChar=" + (char)tc);
            return tc;
        }
    }

    /**
     * This returns the next non-whitespace character (or -1).
     * 
     * @return the next non-whitespace character (or -1)
     * @throws Exception if trouble
     */
    public int readNonWhiteChar() throws Exception {
        int tc = read(); //not reader.read, so lineNumber is accurate
        while (tc >= 0 && tc <= 32) 
            tc = read(); //not reader.read, so lineNumber is accurate
        return tc;
    }


    /**
     * This reads white space then the expected character.
     * 
     * @param expected (as an int)
     * @throws Exception if trouble
     */
    public void readExpected(int expected) throws Exception {
        if (read() != expected)
            throw new Exception("ParseJSON: Expected character '" + (char)expected + "' not found");
    }


    /**
     * Used when the last char read was in [-0123456789], this reads 
     * subsequent digits.
     * This is used by internally, but rarely used directly by users.
     * The last character read will have been the last digit in the sequence.
     *
     * @param chi the last char read (usually [-0123456789]) (as an int)
     * @return the initial ch plus the 0 or more new digits
     * @throws Exception if trouble
     */
    public String readDigits(int chi) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append((char)chi);

        //read the digits
        chi = read(); //if -1, handled by pushedChar=ch below
        while (chi >= '0' && chi <= '9') {
            sb.append((char)chi);
            chi = read(); //if -1, handled by pushedChar=ch below
        }
        pushedChar = chi; 
        return sb.toString();
    }

    /**
     * Assuming the last char read was in '-0123456789', this reads 
     * the rest of a non-null double.
     * This is used internally, but rarely used directly by users.
     * The last character read will have been the final digit of the double.
     *
     * @param chi the last char read (as an int)
     * @return the double as a String
     * @throws Exception if trouble
     */
    public String readDoubleAsString(int chi) throws Exception {
        //this isn't very strict, but problems will be caught by parseDouble below

        //read start of number
        StringBuilder sb = new StringBuilder(readDigits(chi));
        chi = read(); //if -1, handled by pushedChar=ch below

        //decimal point?
        if (chi == '.') {
            sb.append(readDigits(chi));
            chi = read(); //if -1, handled by pushedChar=ch below
        }

        //exponent?
        if (Character.toLowerCase((char)chi) == 'e') {
            sb.append((char)chi);
            chi = read(); //if -1, handled by pushedChar=ch below
            if (chi == '-' || chi == '+' || (chi >= '0' && chi <= '9')) {
                sb.append(readDigits(chi));
                chi = read(); //if -1, handled by pushedChar=ch below
            } else {
                sb.append((char)chi);
                throw new NumberFormatException(sb.toString());
            }
        }

        pushedChar = chi; 
        return sb.toString();
    }

    /**
     * Assuming the last char read was in '-0123456789', this reads 
     * the rest of a non-null double.
     * The last character read will have been the final digit of the double.
     *
     * @param chi the last char read (as an int)
     * @return the double
     * @throws Exception if trouble
     */
    public double readDouble(int chi) throws Exception {
        //2011-02-09 Bob Simons added to avoid Java hang bug.
        //But now, latest version of Java is fixed.
        String s = readDoubleAsString(chi);
        return 
            //String2.isDoubleTrouble(s)? 0 : 
            Double.parseDouble(s);
    }

    /**
     * Used when the last char read was in '-0123456789', this reads 
     * the rest of an int (must be a valid Java int).
     * The last character read will have been the final digit of the int.
     *
     * @param chi the last char read (as an int)
     * @return the int
     * @throws Exception if trouble
     */
    public int readInt(int chi) throws Exception {
        //this isn't very strict, but problems will be caught by parseInt below
        return Integer.parseInt(readDigits(chi));
    }
    
    /**
     * Assuming the last char read was '"', this reads 
     * the rest of a String.
     * The last char read will have been the closing '"'.
     *
     * @param chi the last char read (almost always '"')
     * @return the String
     * @throws Exception if trouble
     */
    public String readString(int chi) throws Exception {
        StringBuilder sb = new StringBuilder();
        int startLineNumber = lineNumber;
        int startLinePo = linePo;

        //read the characters
        chi = read(); //if -1, handled by pushedChar=ch below
        while (chi != -1) {
            if (chi == '"') {
                //we're done
                return sb.toString();
            } else if (chi == '\\') {
                chi = read(); //if -1, handled by last 'else' below
                if (chi == '\\')     sb.append('\\');
                else if (chi == '"') sb.append('"');
                else if (chi == '/') sb.append('/');   //odd, since Java doesn't support \\/
                else if (chi == 'b') sb.append('\b');  
                else if (chi == 'f') sb.append('\f');
                else if (chi == 'n') sb.append('\n');
                else if (chi == 'r') sb.append('\r');
                else if (chi == 't') sb.append('\t');
                else if (chi == 'u') {
                    //4 hex digits
                    char charAr[] = new char[4];
                    for (int i = 0; i < 4; i++)
                        charAr[i] = (char)read(); //-1's will be caught by parseInt below
                    sb.append((char)Integer.parseInt(new String(charAr), 16));
                } else throw new Exception("ParseJSON: Unexpected character #" + chi + " after '\\'");
            } else if (chi < 32) {
                throw new Exception("ParseJSON: Control character (#" + chi + ") in String should have been escaped");
            } else {
                sb.append((char)chi);
            }

            chi = read(); //if -1, handled if fall through loop
        }

        //unexpected end of stream
        throw new Exception("ParseJSON: No closing '\"' found for String starting at line #" + 
            startLineNumber + " character #" + startLinePo + ", and ending"); //+ onLine
    }
    
    /**
     * Assuming the last char read was 't', this reads "rue" or throws Exception.
     * The last character read will have been the 'e'.
     *
     * @param chi the last char read (always 't')
     * @return Boolean.TRUE
     * @throws Exception if next characters not "rue"
     */
    public Boolean readTrue(int chi) throws Exception {
        if (read() == 'r' &&
            read() == 'u' &&
            read() == 'e')
            return Boolean.TRUE;
        throw new Exception("ParseJSON: \"true\" expected");

    }
    
    /**
     * Assuming the last char read was 'f', this reads "alse" or throws Exception.
     * The last character read will have been the 'e'.
     *
     * @param chi the last char read (always 'f')
     * @return Boolean.FALSE
     * @throws Exception if next characters not "alse"
     */
    public Boolean readFalse(int chi) throws Exception {
        if (read() == 'a' &&
            read() == 'l' &&
            read() == 's' &&
            read() == 'e')
            return Boolean.FALSE;
        throw new Exception("ParseJSON: \"false\" expected");
    }
    
    /**
     * Assuming the last char read was 'n', this reads "ull" or throws Exception.
     * The last character read will have been the last 'l'.
     *
     * @param chi the last char read (always 'n')
     * @return null
     * @throws Exception if next characters not "ull"
     */
    public Object readNull(int chi) throws Exception {
        if (read() == 'u' &&
            read() == 'l' &&
            read() == 'l')
            return null;
        throw new Exception("ParseJSON: \"null\" expected");
    }

    /**
     * This reads an array of primitive values: '[' + commaSeparatedValues + ']'.
     *
     * @return the ArrayList with double, String, true, false, or null
     * @throws Exception if trouble
     */
    public ArrayList readPrimitiveArray() throws Exception {
        readExpected('"');
        return readPrimitiveArray('"');
    }    

    /**
     * Assuming the last char read was '[', this reads 
     * subsequent values of an array of primitives and stores them in an ArrayList.
     * The last char read will have been the closing ']'.
     *
     * @param chi the last char read (almost always '[')
     * @return the ArrayList with double, String, true, false, or null
     * @throws Exception if trouble
     */
    public ArrayList readPrimitiveArray(int chi) throws Exception {
        int startLineNumber = lineNumber;
        int startLinePo = linePo;
        ArrayList al = new ArrayList();
        chi = readNonWhiteChar(); //if -1, handled by pushedChar=ch below
        if (chi == ']')             
            return al; //we're done

        while (true) {
            //read a value
            if (chi == '-' || (chi >= '0' && chi <= '9')) al.add(new Double(readDouble(chi)));
            else if (chi == '"')                          al.add(readString(chi));
            else if (chi == 't')                          al.add(readTrue(chi));
            else if (chi == 'f')                          al.add(readFalse(chi));
            else if (chi == 'n')                          al.add(readNull(chi));
            else throw new Exception("ParseJSON: Non-primitive value found");

            //next chi must be ] or ,
            chi = readNonWhiteChar(); //if -1, handled by loop's -1 test
            if (chi == ']') 
                return al; //we're done
            else if (chi == ',')
                chi = readNonWhiteChar(); //if -1, handled by loop's -1 test
            else throw new Exception("ParseJSON: ',' or ']' expected");

        }
    }

    /**
     * This test the methods of this class.
     * @throws Exception if trouble
     */
    public static void basicTest() throws Exception {
        String error;
        verbose = true;
        reallyVerbose = true;

        //ensure it reads correctly               don't test \\b
        ParseJSON pj = new ParseJSON("\"a String \\u0050\\t\\r\\n\\\" \",-1.2e+3,true,false,null]");
        ArrayList al = pj.readPrimitiveArray('[');
        Test.ensureEqual((String)al.get(0),                 "a String P\t\r\n\" ", "");
        Test.ensureEqual(((Double)al.get(1)).doubleValue(), -1.2e3,                  "");
        Test.ensureEqual((Boolean)al.get(2),                Boolean.TRUE,            "");
        Test.ensureEqual((Boolean)al.get(3),                Boolean.FALSE,           "");
        Test.ensureEqual(al.get(4),                         null,                    "");

        pj = new ParseJSON(" \"a String \\u0050\\t\\r\\n\\\" \" , -1.2e+3 , true , false , null ] ");
        al = pj.readPrimitiveArray('[');
        Test.ensureEqual((String)al.get(0),                 "a String P\t\r\n\" ", "");
        Test.ensureEqual(((Double)al.get(1)).doubleValue(), -1.2e3,                  "");
        Test.ensureEqual((Boolean)al.get(2),                Boolean.TRUE,            "");
        Test.ensureEqual((Boolean)al.get(3),                Boolean.FALSE,           "");
        Test.ensureEqual(al.get(4),                         null,                    "");

        //test intentional failures
        error = "";
        try { 
            pj =new ParseJSON("\"a\nb]\"]");
            al = pj.readPrimitiveArray('['); //control chars in string must be escaped
        } catch (Exception e) {error = e.toString() + pj.onLine();  }
        Test.ensureEqual(error, 
            "java.lang.Exception: ParseJSON: Control character (#10) in String should have been " +
            "escaped on line #2 at character #0.", "");

        error = "";
        try { 
            pj = new ParseJSON("1.25.3");
            al = pj.readPrimitiveArray('['); 
        } catch (Exception e) {error = e.toString() + pj.onLine();  }
        Test.ensureEqual(error, 
            "java.lang.Exception: ParseJSON: ',' or ']' expected on line #1 at character #5.", "");

        error = "";
        try { 
            pj = new ParseJSON("truue]");
            al = pj.readPrimitiveArray('['); 
        } catch (Exception e) {error = e.toString() + pj.onLine();  }
        Test.ensureEqual(error, 
            "java.lang.Exception: ParseJSON: \"true\" expected on line #1 at character #4.", "");

        error = "";
        try { 
            pj = new ParseJSON("nulf]");
            al = pj.readPrimitiveArray('['); 
        } catch (Exception e) {error = e.toString() + pj.onLine();  }
        Test.ensureEqual(error, 
            "java.lang.Exception: ParseJSON: \"null\" expected on line #1 at character #4.", "");

        error = "";
        try { 
            pj = new ParseJSON("null");
            al = pj.readPrimitiveArray('['); 
        } catch (Exception e) {error = e.toString() + pj.onLine();  }
        Test.ensureEqual(error, 
            "java.lang.Exception: ParseJSON: ',' or ']' expected on line #1 at character #5.", "");

        error = "";
        try { 
            pj = new ParseJSON("bob");
            String s = pj.readString('"'); 
        } catch (Exception e) {error = e.toString() + pj.onLine();  }
        Test.ensureEqual(error, 
            "java.lang.Exception: ParseJSON: No closing '\"' found for String starting at " +
            "line #1 character #0, and ending on line #1 at character #4.", "");

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
            lastTest = interactive? -1 : 0;
        String msg = "\n^^^ ParseJSON.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0) basicTest();
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


