/* 
 * DecimalDegreeFormatter Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

/**
 * This formats numbers as decimal degrees. 
 */
public class DecimalDegreeFormatter implements NumberFormatter  {
 
    /**
     * This formats a decimal degree value formatted as "degree.dddddd°".
     *
     * @param d a decimal degree value
     * @return the formatted value.
     *    NaN returns "NaN".
     */
    public String format(double d) {
        if (Double.isNaN(d))
            return "NaN";

        return String2.genEFormat6(d) + "°";
    }

    /**
     * This formats a degree value formatted as "degree°".
     * There is no "NaN" test in this method.
     *
     * @param d a decimal degree value
     * @return the formatted value.
     */
    public String format(long l) {
        return l + "°";
    }

    /**
     * This tests the methods in this class.
     *
     * @param args is ignored
     */
    public static void basicTest() {
        DecimalDegreeFormatter ddf = new DecimalDegreeFormatter();
        Test.ensureEqual(ddf.format(4),           "4°",        "a");
        Test.ensureEqual(ddf.format(4.500000001), "4.5°",      "b");
        Test.ensureEqual(ddf.format(4.499999999), "4.5°",      "c");
        Test.ensureEqual(ddf.format(0.251),       "0.251°",    "d");
        Test.ensureEqual(ddf.format(0.00125),     "1.25E-3°",  "e");

        Test.ensureEqual(ddf.format(-4),           "-4°",        "a");
        Test.ensureEqual(ddf.format(-4.500000001), "-4.5°",      "b");
        Test.ensureEqual(ddf.format(-4.499999999), "-4.5°",      "c");
        Test.ensureEqual(ddf.format(-0.251),       "-0.251°",    "d");
        Test.ensureEqual(ddf.format(-0.00125),     "-1.25E-3°",  "e");
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
        String msg = "\n^^^ DecimalDegreeFormatter.test(" + interactive + ") test=";

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