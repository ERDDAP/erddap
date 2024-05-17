package gov.noaa.pfel.coastwatch.util;

import java.util.ArrayList;

import com.cohort.util.Test;

class ParseJSONTests {

  /**
   * This test the methods of this class.
   * 
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    String error;
    // ParseJSON.verbose = true;
    // ParseJSON.reallyVerbose = true;

    // ensure it reads correctly don't test \\b
    ParseJSON pj = new ParseJSON("\"a String \\u0050\\t\\r\\n\\\" \",-1.2e+3,true,false,null]");
    ArrayList al = pj.readPrimitiveArray('[');
    Test.ensureEqual((String) al.get(0), "a String P\t\r\n\" ", "");
    Test.ensureEqual(((Double) al.get(1)).doubleValue(), -1.2e3, "");
    Test.ensureEqual((Boolean) al.get(2), Boolean.TRUE, "");
    Test.ensureEqual((Boolean) al.get(3), Boolean.FALSE, "");
    Test.ensureEqual(al.get(4), null, "");

    pj = new ParseJSON(" \"a String \\u0050\\t\\r\\n\\\" \" , -1.2e+3 , true , false , null ] ");
    al = pj.readPrimitiveArray('[');
    Test.ensureEqual((String) al.get(0), "a String P\t\r\n\" ", "");
    Test.ensureEqual(((Double) al.get(1)).doubleValue(), -1.2e3, "");
    Test.ensureEqual((Boolean) al.get(2), Boolean.TRUE, "");
    Test.ensureEqual((Boolean) al.get(3), Boolean.FALSE, "");
    Test.ensureEqual(al.get(4), null, "");

    // test intentional failures
    error = "";
    try {
      pj = new ParseJSON("\"a\nb]\"]");
      al = pj.readPrimitiveArray('['); // control chars in string must be escaped
    } catch (Exception e) {
      error = e.toString() + pj.onLine();
    }
    Test.ensureEqual(error,
        "java.lang.Exception: ParseJSON: Control character (#10) in String should have been " +
            "escaped on line #2 at character #0.",
        "");

    error = "";
    try {
      pj = new ParseJSON("1.25.3");
      al = pj.readPrimitiveArray('[');
    } catch (Exception e) {
      error = e.toString() + pj.onLine();
    }
    Test.ensureEqual(error,
        "java.lang.Exception: ParseJSON: ',' or ']' expected on line #1 at character #5.", "");

    error = "";
    try {
      pj = new ParseJSON("truue]");
      al = pj.readPrimitiveArray('[');
    } catch (Exception e) {
      error = e.toString() + pj.onLine();
    }
    Test.ensureEqual(error,
        "java.lang.Exception: ParseJSON: \"true\" expected on line #1 at character #4.", "");

    error = "";
    try {
      pj = new ParseJSON("nulf]");
      al = pj.readPrimitiveArray('[');
    } catch (Exception e) {
      error = e.toString() + pj.onLine();
    }
    Test.ensureEqual(error,
        "java.lang.Exception: ParseJSON: \"null\" expected on line #1 at character #4.", "");

    error = "";
    try {
      pj = new ParseJSON("null");
      al = pj.readPrimitiveArray('[');
    } catch (Exception e) {
      error = e.toString() + pj.onLine();
    }
    Test.ensureEqual(error,
        "java.lang.Exception: ParseJSON: ',' or ']' expected on line #1 at character #5.", "");

    error = "";
    try {
      pj = new ParseJSON("bob");
      String s = pj.readString('"');
    } catch (Exception e) {
      error = e.toString() + pj.onLine();
    }
    Test.ensureEqual(error,
        "java.lang.Exception: ParseJSON: No closing '\"' found for String starting at " +
            "line #1 character #0, and ending on line #1 at character #4.",
        "");

  }

}
