/* 
 * DecimalDegreeFormatter Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

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
    public static void main(String args[]) {
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

}