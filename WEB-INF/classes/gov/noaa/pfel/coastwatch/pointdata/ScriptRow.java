/* Table Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.Script2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;


/** 
 * This class makes the data on 1 row of a table accessible 
 * to JexlScript scripts via "row.<i>name</i>()" methods.
 *
 * <p>This class is Copyright 2019, NOAA.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2019-11-14
 */
public class ScriptRow  {

    private String fullFileName = "";
    private String fileName = "";
    private Table table;
    private int row = 0;

    /**
     * The constructor.
     *
     * @param fullFileName  The full name (perhaps a URL) of the current file, or "" if 
     * the source is not file-like.
     */
    public ScriptRow(String tFullFileName, Table tTable) {
        if (String2.isSomething(tFullFileName)) {
            fullFileName = tFullFileName;
            fileName = File2.getNameAndExtension(fullFileName);
        }
        table = tTable;
    }

    /**
     * This gets the full name (perhaps a URL) of the current file, or "" if 
     * the source is not file-like.
     */
    public String getFullFileName() {
        return fullFileName;
    }

    /**
     * This gets the short name of the current file, or "" if 
     * the source is not file-like.
     */
    public String getFileName() {
        return fileName;
    }

    /** 
     * Set the current row number in the table (0..). Only the controller (ERDDAP) should call this. Scripts shouldn't call this. 
     */
    public void setRow(int tRow) {
        row = tRow;
    }

    /** 
     * Get the current row in the table (0..).
     */
    public int getRow() {
        return row;
    }


    /**
     * This gets the value from a column as an int.
     *
     * @param colName the columnName 
     * @return the value as an int (or Int.MAX_VALUE if column not found or other trouble)
     */
    public int columnInt(String colName) {
        int col = table.findColumnNumber(colName);
        return col < 0? Integer.MAX_VALUE : table.getColumn(col).getInt(row);
    }

    /**
     * This gets the value from a column as a long.
     *
     * @param colName the columnName 
     * @return the value as a long (or Long.MAX_VALUE if column not found or other trouble)
     */
    public long columnLong(String colName) {
        int col = table.findColumnNumber(colName);
        return col < 0? Long.MAX_VALUE : table.getColumn(col).getLong(row);
    }

    /**
     * This gets the value from a column as a float.
     *
     * @param colName the columnName 
     * @return the value as a float (or NaN if column not found or other trouble)
     */
    public float columnFloat(String colName) {
        int col = table.findColumnNumber(colName);
        return col < 0? Float.NaN : table.getColumn(col).getFloat(row);
    }

    /**
     * This gets the value from a column as a double.
     *
     * @param colName the columnName 
     * @return the value as a double (or NaN if column not found or other trouble)
     */
    public double columnDouble(String colName) {
        int col = table.findColumnNumber(colName);
        return col < 0? Double.NaN : table.getColumn(col).getDouble(row);
    }

    /**
     * This gets the value from a column as a String.
     *
     * @param colName the columnName 
     * @return the value as a String (or "" if column not found)
     */
    public String columnString(String colName) {
        int col = table.findColumnNumber(colName);
        return col < 0? "" : table.getColumn(col).getString(row);
    }


    /**
     * This tests Jexl scripts, including working with a table.
     * Only the controller (ERDDAP) should call this. Scripts shouldn't call this. 
     */
    public static void basicTest() throws Exception {
        String2.log("\n*** ScriptRow.basicTest()");

        //silent=false, strict=true is most like Java (not e.g., JavaScript).
        //if !silent, it throws exception if trouble
        //if strict, it requires variables, methods and functions to be defined 
        //  (instead of treating unknowns as null, like javascript)
        JexlEngine jengine = new JexlBuilder().silent(false).strict(true).create(); //see better/safer constructor below
        JexlScript jscript;  //multiple statements. Has support for if/for/while/var/{} etc.
        JexlContext jcontext;
        Object o;
        long time;
        String results, expected;
        
        //numeric with integers
        jscript = jengine.createScript("x+y");
        jcontext = new MapContext();
        jcontext.set("x", new Integer(2));
        jcontext.set("y", new Integer(3));
        o = jscript.execute(jcontext);
        Test.ensureEqual(o.getClass().getSimpleName(), "Integer", "");
        Test.ensureEqual(((Integer)o).intValue(), 5, "");
        
        //numeric with integers
        jscript = jengine.createScript("x +=1; x+y;");
        jcontext = new MapContext();
        jcontext.set("x", new Integer(2));
        jcontext.set("y", new Double(2.5));
        o = jscript.execute(jcontext);
        Test.ensureEqual(o.getClass().getSimpleName(), "Double", "");
        Test.ensureEqual(((Double)o).doubleValue(), 5.5, "");

        //reuse jscript.  Set new mapContext values
        jcontext.set("x", new Integer(20));
        o = jscript.execute(jcontext);
        Test.ensureEqual(o.getClass().getSimpleName(), "Double", "");
        Test.ensureEqual(((Double)o).doubleValue(), 23.5, "");

        //parse that jscript and get list of variables
        Test.ensureEqual(String2.toCSVString(jscript.getVariables()), "[x],[y]", "");
        
        //String concat
        //By default, a script can access any method of any object.
        jscript = jengine.createScript("x.substring(0,3)+\" \" + y");
        jcontext = new MapContext();
        jcontext.set("x", new String("George"));
        jcontext.set("y", new String("Washington"));
        o = jscript.execute(jcontext);
        Test.ensureEqual(o.getClass().getSimpleName(), "String", "");
        Test.ensureEqual(o.toString(), "Geo Washington", "");

        //java stuff with Strings
        jscript = jengine.createScript("x.substring(0,3)+\" \" + y.getClass().getSimpleName()");
        String2.log("parsed=" + jscript.getParsedText());
        jcontext = new MapContext();
        jcontext.set("x", new String("George"));
        jcontext.set("y", new String("Washington"));
        o = jscript.execute(jcontext);
        Test.ensureEqual(o.getClass().getSimpleName(), "String", "");
        Test.ensureEqual(o.toString(), "Geo String", "");

        //ensure script can't easily access other classes unless explicitly added
        jscript = jengine.createScript("String2.zeroPad(\"a\", 3)");
        jcontext = new MapContext();
        try {
            results = jscript.execute(jcontext).toString();
        } catch (Exception e) {
            results = e.toString();
        }
        Test.ensureEqual(results, 
            "org.apache.commons.jexl3.JexlException$Variable: gov.noaa.pfel.coastwatch.pointdata.ScriptRow.basicTest@1:1 undefined variable String2", 
            "results=\n" + results);


        //SECURITY ISSUE: A user can call a static method in a static class and instantiate any class!
        jscript = jengine.createScript("\"\".class.forName(\"java.lang.System\").getProperty(\"os.name\")");
        jcontext = new MapContext();
        o = jscript.execute(jcontext);
        Test.ensureEqual(o.getClass().getSimpleName(), "String", "");
        try {
            Test.ensureEqual(o.toString(), "Windows 10", "(On Bob's computer)");
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.pressEnterToContinue("This will give a different answer on different OS's.");

        }

        //a user can instantiate ANY class...
        jscript = jengine.createScript("new('" + StringBuilder.class.getName() + "', 'contents of sb')");
        jcontext = new MapContext();
        o = jscript.execute(jcontext);
        Test.ensureEqual(o.getClass().getSimpleName(), "StringBuilder", "");
        Test.ensureEqual(o.toString(), "contents of sb", "");


        //**** test jexlScriptNeedsColumns
        results = String2.toCSSVString(Script2.jexlScriptNeedsColumns("=\"WTDL\""));
        expected = "";
        Test.ensureEqual(results, expected, "");

        results = String2.toCSSVString(Script2.jexlScriptNeedsColumns(
            "=var wt=row.columnFloat(\"Water Temperature (Celsius)\"); return wt&lt;-10? NaN : wt*9/5+32;"));
        expected = "Water Temperature (Celsius)";
        Test.ensureEqual(results, expected, "");

        results = String2.toCSSVString(Script2.jexlScriptNeedsColumns(
            "=row.columnFloat(\"abc\"); row.columnString(\"def\"); row.columnInt(\"g h\")"));
        expected = "abc, def, g h";
        Test.ensureEqual(results, expected, "");


        //*******
        //SECURITY SOLUTION: make a new engine with a sandbox that blocks everything except specific classes
        jengine = Script2.jexlEngine();

        //then retry accessing other classes: now it fails (as desired)
        //With sandbox, user can't call System class' methods.
        //https://issues.apache.org/jira/browse/JEXL-140
        try {
            jscript = jengine.createScript("\"\".class.forName(\"java.lang.System\").getProperty(\"os.name\")");
            jcontext = Script2.jexlMapContext();
            o = jscript.execute(jcontext);
            results = o.getClass().getSimpleName();
        } catch (Exception e) {
            results = e.toString();
        }
        Test.ensureEqual(results,
            "org.apache.commons.jexl3.JexlException$Method: " +
            "gov.noaa.pfel.coastwatch.pointdata.ScriptRow.basicTest@1:37 unsolvable function/method 'getProperty'",
            "");

        //test a user can instantiate any class: now it fails (as desired)
        //With sandbox, user can't create StringBuilder object.
        try {
            jscript = jengine.createScript("new('" + StringBuilder.class.getName() + "', 'contents of sb')");
            jcontext = Script2.jexlMapContext();
            o = jscript.execute(jcontext);
            results = o.getClass().getSimpleName();
        } catch (Exception e) {
            results = e.toString();
        }
        Test.ensureEqual(results,
            "org.apache.commons.jexl3.JexlException$Method: " +
            "gov.noaa.pfel.coastwatch.pointdata.ScriptRow.basicTest@1:1 unsolvable function/method 'java.lang.StringBuilder'",
            "");

        //work with static functions  by making a shell class (ScriptMath) that can be instantiated
        //https://issues.apache.org/jira/browse/JEXL-140 
        //but this way is easier, gives me more control (e.g., subset of Math2) and has Java-like notation
        jscript = jengine.createScript("Math.PI + Math.log(23.0)");
        jcontext = Script2.jexlMapContext();
        o = jscript.execute(jcontext);
        Test.ensureEqual(o.getClass().getSimpleName(), "Double", "");
        Test.ensureEqual(((Double)o).doubleValue(), Math.PI + Math.log(23.0), "");

        //Math2
        jscript = jengine.createScript("Math2.mantissa(34567.8)");
        jcontext = Script2.jexlMapContext();
        o = jscript.execute(jcontext);
        Test.ensureEqual(o.getClass().getSimpleName(), "Double", "");
        Test.ensureEqual(((Double)o).doubleValue(), 3.45678, "");

        //Math2.random is another class, held within Math2. 
        //It is accesible because the script only refers to the allowed Math2 methods.
        jscript = jengine.createScript("Math2.random(5)");
        jcontext = Script2.jexlMapContext();
        o = jscript.execute(jcontext);
        Test.ensureEqual(o.getClass().getSimpleName(), "Integer", "");
        int ti = ((Integer)o).intValue();
        String2.log("random(5)=" + ti);
        Test.ensureTrue(ti >= 0 && ti < 5, "ti=" + ti);

        //String methods and concat are accesible without jcontext.set("String")
        //but not String static methods
        jscript = jengine.createScript("x.substring(0,3)+\" \" + y");
        jcontext = Script2.jexlMapContext();
        jcontext.set("x", new String("George"));
        jcontext.set("y", new String("Washington"));
        o = jscript.execute(jcontext);
        Test.ensureEqual(o.getClass().getSimpleName(), "String", "");
        Test.ensureEqual(o.toString(), "Geo Washington", "");

        //String static methods are not accessible if String not in MapContext
        try {
            jscript = jengine.createScript("x.substring(0,3)+\" \" + y + \" \" + String.valueOf(17)");
            jcontext = new MapContext();  //not Script2.jexlMapContext();
            jcontext.set("x", new String("George"));
            jcontext.set("y", new String("Washington"));
            o = jscript.execute(jcontext);
            results = o.getClass().getSimpleName();
        } catch (Exception e) {
            results = e.toString();
        }
        Test.ensureEqual(results,
            "org.apache.commons.jexl3.JexlException$Variable: gov.noaa.pfel.coastwatch.pointdata.ScriptRow.basicTest@1:34 undefined variable String",
            "");

        //String static methods are accessible if String in MapContext
        jscript = jengine.createScript("x.substring(0,3)+\" \" + y + \" \" + String.valueOf(17)");
        jcontext = Script2.jexlMapContext();
        jcontext.set("x", new String("George"));
        jcontext.set("y", new String("Washington"));
        o = jscript.execute(jcontext);
        Test.ensureEqual(o.getClass().getSimpleName(), "String", "");
        Test.ensureEqual(o.toString(), "Geo Washington 17", "");

        //ensure script still can't easily access other classes unless explicitly added to context
        jscript = jengine.createScript("String2.zeroPad(\"a\", 3)");
        jcontext = new MapContext(); //not Script2.jexlMapContext();
        try {
            results = jscript.execute(jcontext).toString();
        } catch (Exception e) {
            results = e.toString();
        }
        Test.ensureEqual(results, 
            "org.apache.commons.jexl3.JexlException$Variable: gov.noaa.pfel.coastwatch.pointdata.ScriptRow.basicTest@1:1 undefined variable String2", 
            "results=\n" + results);

        //but succeeds when added to context
        jscript = jengine.createScript("String2.zeroPad(\"a\", 3)");
        String2.log("parsed=" + jscript.getParsedText());
        jcontext = Script2.jexlMapContext();
        o = jscript.execute(jcontext);
        Test.ensureEqual(o.getClass().getSimpleName(), "String", "");
        Test.ensureEqual(o.toString(), "00a", "");

        //work with row (in a table)
        Table table = new Table();
        table.addColumn("ints",    new IntArray(new int[]{0,10,20}));
        table.addColumn("doubles", new DoubleArray(new double[]{0, 1.1, 2.2}));
        table.addColumn("Strings", new StringArray(new String[]{"a", "bb", "ccc"}));
        ScriptRow row = new ScriptRow("/dir1/dir2/fileName.ext", table);
        Test.ensureEqual(row.getFullFileName(), "/dir1/dir2/fileName.ext", "");
        Test.ensureEqual(row.getFileName(), "fileName.ext", "");

        //test native java code time
        time = System.currentTimeMillis();
        for (int it = 0; it < 333000000; it++); {
            for (int i = 0; i < 3; i++) {
                row.setRow(i);
                double d = row.columnInt("ints") + row.columnDouble("doubles");
                Test.ensureEqual(d, i * 11.1, "");
            }
        }
        String2.log("native time for 1,000,000,000 evaluations=" + (System.currentTimeMillis() - time) + "ms (expected: 34)");

        //test jexl time
        time = System.currentTimeMillis();
        jscript = jengine.createScript("row.columnInt(\"ints\") + row.columnDouble(\"doubles\")");
        jcontext = Script2.jexlMapContext();
        jcontext.set("row", row);
        for (int it = 0; it < 333000000; it++); {
            for (int i = 0; i < 3; i++) {
                row.setRow(i);
                o = jscript.execute(jcontext);
                Test.ensureEqual(o.getClass().getSimpleName(), "Double", "");
                Test.ensureEqual(((Double)o).doubleValue(), i * 11.1, "");
            }
        }
        String2.log("jexl   time for 1,000,000,000 evaluations=" + (System.currentTimeMillis() - time) + "ms (expected: 137)");
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
        String msg = "\n^^^ ScriptRow.test(" + interactive + ") test=";

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
