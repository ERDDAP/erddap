package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.StringArray;
import com.cohort.util.Script2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;

class ScriptRowTests {
  /**
   * This tests Jexl scripts, including working with a table. Only the controller (ERDDAP) should
   * call this. Scripts shouldn't call this.
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    String2.log("\n*** ScriptRow.basicTest()");

    // silent=false, strict=true is most like Java (not e.g., JavaScript).
    // if !silent, it throws exception if trouble
    // if strict, it requires variables, methods and functions to be defined
    // (instead of treating unknowns as null, like javascript)
    JexlEngine jengine =
        new JexlBuilder()
            .permissions(Script2.permissions)
            .silent(false)
            .strict(true)
            .create(); // see better/safer constructor below
    JexlScript jscript; // multiple statements. Has support for if/for/while/var/{} etc.
    JexlContext jcontext;
    Object o;
    long time;
    String results, expected;

    // numeric with integers
    jscript = jengine.createScript("x+y");
    jcontext = new MapContext();
    jcontext.set("x", Integer.valueOf(2));
    jcontext.set("y", Integer.valueOf(3));
    o = jscript.execute(jcontext);
    Test.ensureEqual(o.getClass().getSimpleName(), "Integer", "");
    Test.ensureEqual(((Integer) o).intValue(), 5, "");

    // numeric with integers
    jscript = jengine.createScript("x +=1; x+y;");
    jcontext = new MapContext();
    jcontext.set("x", Integer.valueOf(2));
    jcontext.set("y", Double.valueOf(2.5));
    o = jscript.execute(jcontext);
    Test.ensureEqual(o.getClass().getSimpleName(), "Double", "");
    Test.ensureEqual(((Double) o).doubleValue(), 5.5, "");

    // reuse jscript. Set new mapContext values
    jcontext.set("x", Integer.valueOf(20));
    o = jscript.execute(jcontext);
    Test.ensureEqual(o.getClass().getSimpleName(), "Double", "");
    Test.ensureEqual(((Double) o).doubleValue(), 23.5, "");

    // parse that jscript and get list of variables
    Test.ensureEqual(String2.toCSVString(jscript.getVariables()), "[x],[y]", "");

    // String concat
    // By default, a script can access any method of any object.
    jscript = jengine.createScript("x.substring(0,3)+\" \" + y");
    jcontext = new MapContext();
    jcontext.set("x", new String("George"));
    jcontext.set("y", new String("Washington"));
    o = jscript.execute(jcontext);
    Test.ensureEqual(o.getClass().getSimpleName(), "String", "");
    Test.ensureEqual(o.toString(), "Geo Washington", "");

    // java stuff with Strings
    jscript = jengine.createScript("x.substring(0,3)+\" \" + y.getClass().getSimpleName()");
    String2.log("parsed=" + jscript.getParsedText());
    jcontext = new MapContext();
    jcontext.set("x", new String("George"));
    jcontext.set("y", new String("Washington"));
    try {
      results = jscript.execute(jcontext).toString();
    } catch (Exception e) {
      results = "Caught: " + e.toString();
    }
    expected =
        "Caught: org.apache.commons.jexl3.JexlException: gov.noaa.pfel.coastwatch.pointdata.ScriptRowTests.basicTest:ERROR_LOCATION JEXL error : + error caused by null operand";
    results = results.replaceAll("[0-9]+@[0-9]+:[0-9]+", "ERROR_LOCATION");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // ensure script can't easily access other classes unless explicitly added
    jscript = jengine.createScript("String2.zeroPad(\"a\", 3)");
    jcontext = new MapContext();
    try {
      results = jscript.execute(jcontext).toString();
    } catch (Exception e) {
      results = "Caught: " + e.toString();
    }
    Test.ensureEqual(
        results,
        "Caught: java.lang.NullPointerException: Cannot invoke \"Object.toString()\" because the return value of \"org.apache.commons.jexl3.JexlScript.execute(org.apache.commons.jexl3.JexlContext)\" is null",
        // 2021-07-02 was "org.apache.commons.jexl3.JexlException$Variable:
        // gov.noaa.pfel.coastwatch.pointdata.ScriptRow.basicTest@1:1 undefined variable
        // String2",
        "results=\n" + results);

    // Verify Jexl permissions restrict sensitive access.
    jscript =
        jengine.createScript("\"\".class.forName(\"java.lang.System\").getProperty(\"os.name\")");
    jcontext = new MapContext();
    o = jscript.execute(jcontext);
    Test.ensureEqual(o, null, "Jexl should return null due to permission restrictions");

    // a user can instantiate ANY class...
    jscript =
        jengine.createScript("new('" + StringBuilder.class.getName() + "', 'contents of sb')");
    jcontext = new MapContext();
    o = jscript.execute(jcontext);
    Test.ensureEqual(o.getClass().getSimpleName(), "StringBuilder", "");
    Test.ensureEqual(o.toString(), "contents of sb", "");

    // **** test jexlScriptNeedsColumns
    results = String2.toCSSVString(Script2.jexlScriptNeedsColumns("=\"WTDL\""));
    expected = "";
    Test.ensureEqual(results, expected, "");

    results =
        String2.toCSSVString(
            Script2.jexlScriptNeedsColumns(
                "=var wt=row.columnFloat(\"Water Temperature (Celsius)\"); return wt&lt;-10? NaN : wt*9/5+32;"));
    expected = "Water Temperature (Celsius)";
    Test.ensureEqual(results, expected, "");

    results =
        String2.toCSSVString(
            Script2.jexlScriptNeedsColumns(
                "=row.columnFloat(\"abc\"); row.columnString(\"def\"); row.columnInt(\"g h\")"));
    expected = "abc, def, g h";
    Test.ensureEqual(results, expected, "");

    // *******
    // SECURITY SOLUTION: make a new engine with a sandbox that blocks everything
    // except specific classes
    jengine = Script2.jexlEngine();

    // then retry accessing other classes: now it fails (as desired)
    // With sandbox, user can't call System class' methods.
    // https://issues.apache.org/jira/browse/JEXL-140
    try {
      jscript =
          jengine.createScript("\"\".class.forName(\"java.lang.System\").getProperty(\"os.name\")");
      jcontext = Script2.jexlMapContext();
      o = jscript.execute(jcontext);
      results = o.getClass().getSimpleName();
    } catch (Exception e) {
      results = "Caught: " + e.toString();
    }
    Test.ensureEqual(
        results,
        "Caught: java.lang.NullPointerException: Cannot invoke \"Object.getClass()\" because \"o\" is null",
        // 2021-07-02 was "org.apache.commons.jexl3.JexlException$Method:
        // gov.noaa.pfel.coastwatch.pointdata.ScriptRow.basicTest@1:37 unsolvable
        // function/method 'getProperty'",
        "");

    // test a user can instantiate any class: now it fails (as desired)
    // With sandbox, user can't create StringBuilder object.
    try {
      jscript =
          jengine.createScript("new('" + StringBuilder.class.getName() + "', 'contents of sb')");
      jcontext = Script2.jexlMapContext();
      o = jscript.execute(jcontext);
      results = o.getClass().getSimpleName();
    } catch (Exception e) {
      results = e.toString();
    }
    results = results.replaceAll("[0-9]+@[0-9]+:[0-9]+", "ERROR_LOCATION");
    Test.ensureEqual(
        results,
        "org.apache.commons.jexl3.JexlException$Method: "
            + "gov.noaa.pfel.coastwatch.pointdata.ScriptRowTests.basicTest:ERROR_LOCATION unsolvable function/method 'java.lang.StringBuilder(String)'",
        "");

    // work with static functions by making a shell class (ScriptMath) that can be
    // instantiated
    // https://issues.apache.org/jira/browse/JEXL-140
    // but this way is easier, gives me more control (e.g., subset of Math2) and has
    // Java-like notation
    jscript = jengine.createScript("Math.PI + Math.log(23.0)");
    jcontext = Script2.jexlMapContext();
    o = jscript.execute(jcontext);
    Test.ensureEqual(o.getClass().getSimpleName(), "Double", "");
    Test.ensureEqual(((Double) o).doubleValue(), Math.PI + Math.log(23.0), "");

    // Math2
    jscript = jengine.createScript("Math2.mantissa(34567.8)");
    jcontext = Script2.jexlMapContext();
    o = jscript.execute(jcontext);
    Test.ensureEqual(o.getClass().getSimpleName(), "Double", "");
    Test.ensureEqual(((Double) o).doubleValue(), 3.45678, "");

    // Math2.random is another class, held within Math2.
    // It is accesible because the script only refers to the allowed Math2 methods.
    jscript = jengine.createScript("Math2.random(5)");
    jcontext = Script2.jexlMapContext();
    o = jscript.execute(jcontext);
    Test.ensureEqual(o.getClass().getSimpleName(), "Integer", "");
    int ti = ((Integer) o).intValue();
    String2.log("random(5)=" + ti);
    Test.ensureTrue(ti >= 0 && ti < 5, "ti=" + ti);

    // String methods and concat are accesible without jcontext.set("String")
    // but not String static methods
    jscript = jengine.createScript("x.substring(0,3)+\" \" + y");
    jcontext = Script2.jexlMapContext();
    jcontext.set("x", new String("George"));
    jcontext.set("y", new String("Washington"));
    o = jscript.execute(jcontext);
    Test.ensureEqual(o.getClass().getSimpleName(), "String", "");
    Test.ensureEqual(o.toString(), "Geo Washington", "");

    // String static methods are not accessible if String not in MapContext
    try {
      jscript = jengine.createScript("x.substring(0,3)+\" \" + y + \" \" + String.valueOf(17)");
      jcontext = new MapContext(); // not Script2.jexlMapContext();
      jcontext.set("x", new String("George"));
      jcontext.set("y", new String("Washington"));
      o = jscript.execute(jcontext);
      results = o.getClass().getSimpleName();
    } catch (Exception e) {
      results = e.toString();
    }
    results = results.replaceAll("[0-9]+@[0-9]+:[0-9]+", "ERROR_LOCATION");
    Test.ensureEqual(
        results,
        "org.apache.commons.jexl3.JexlException: gov.noaa.pfel.coastwatch.pointdata.ScriptRowTests.basicTest:ERROR_LOCATION JEXL error : + error caused by null operand",
        "");

    // String static methods are accessible if String in MapContext
    jscript = jengine.createScript("x.substring(0,3)+\" \" + y + \" \" + String.valueOf(17)");
    jcontext = Script2.jexlMapContext();
    jcontext.set("x", new String("George"));
    jcontext.set("y", new String("Washington"));
    o = jscript.execute(jcontext);
    Test.ensureEqual(o.getClass().getSimpleName(), "String", "");
    Test.ensureEqual(o.toString(), "Geo Washington 17", "");

    // ensure script still can't easily access other classes unless explicitly added
    // to context
    jscript = jengine.createScript("String2.zeroPad(\"a\", 3)");
    jcontext = new MapContext(); // not Script2.jexlMapContext();
    try {
      results = jscript.execute(jcontext).toString();
    } catch (Exception e) {
      results = "Caught: " + e.toString();
    }
    Test.ensureEqual(
        results,
        "Caught: java.lang.NullPointerException: Cannot invoke \"Object.toString()\" because the return value of \"org.apache.commons.jexl3.JexlScript.execute(org.apache.commons.jexl3.JexlContext)\" is null",
        // 2021-07-02 was "org.apache.commons.jexl3.JexlException$Variable:
        // gov.noaa.pfel.coastwatch.pointdata.ScriptRow.basicTest@1:1 undefined variable
        // String2",
        "results=\n" + results);

    // but succeeds when added to context
    jscript = jengine.createScript("String2.zeroPad(\"a\", 3)");
    String2.log("parsed=" + jscript.getParsedText());
    jcontext = Script2.jexlMapContext();
    o = jscript.execute(jcontext);
    Test.ensureEqual(o.getClass().getSimpleName(), "String", "");
    Test.ensureEqual(o.toString(), "00a", "");

    // work with row (in a table)
    Table table = new Table();
    table.addColumn("ints", new IntArray(new int[] {0, 10, 20}));
    table.addColumn("doubles", new DoubleArray(new double[] {0, 1.1, 2.2}));
    table.addColumn("Strings", new StringArray(new String[] {"a", "bb", "ccc"}));
    ScriptRow row = new ScriptRow("/dir1/dir2/fileName.ext", table);
    Test.ensureEqual(row.getFullFileName(), "/dir1/dir2/fileName.ext", "");
    Test.ensureEqual(row.getFileName(), "fileName.ext", "");

    // test native java code time
    time = System.currentTimeMillis();
    for (int it = 0; it < 333000000; it++)
      ;
    {
      for (int i = 0; i < 3; i++) {
        row.setRow(i);
        double d = row.columnInt("ints") + row.columnDouble("doubles");
        Test.ensureEqual(d, i * 11.1, "");
      }
    }
    String2.log(
        "native time for 1,000,000,000 evaluations="
            + (System.currentTimeMillis() - time)
            + "ms (expected: 34)");

    // test jexl time
    time = System.currentTimeMillis();
    jscript = jengine.createScript("row.columnInt(\"ints\") + row.columnDouble(\"doubles\")");
    jcontext = Script2.jexlMapContext();
    jcontext.set("row", row);
    for (int it = 0; it < 333000000; it++)
      ;
    {
      for (int i = 0; i < 3; i++) {
        row.setRow(i);
        o = jscript.execute(jcontext);
        Test.ensureEqual(o.getClass().getSimpleName(), "Double", "");
        Test.ensureEqual(((Double) o).doubleValue(), i * 11.1, "");
      }
    }
    String2.log(
        "jexl   time for 1,000,000,000 evaluations="
            + (System.currentTimeMillis() - time)
            + "ms (expected: 137)");
  }
}
