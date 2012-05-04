/* 
 * EDVTime Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.variable;

import com.cohort.array.Attributes;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import java.util.GregorianCalendar;

/** 
 * This class holds information about *the* main time variable,
 * which is like EDVTimeStamp, but has destinationName="time".
 * 
 * @author Bob Simons (bob.simons@noaa.gov) 2007-06-04
 */
public class EDVTime extends EDVTimeStamp { 

     /** The constructor. */
    public EDVTime(String tSourceName, 
        Attributes tSourceAttributes, Attributes tAddAttributes,
        String tSourceDataType) 
        throws Throwable {

        super(tSourceName, EDV.TIME_NAME, tSourceAttributes, tAddAttributes,
            tSourceDataType); 
    }
        


    /**
     * This is a unit test.
     */
    public static void test() throws Throwable {
        verbose = true;

        //***with Z
        String2.log("\n*** test with Z");
        EDVTime eta = new EDVTime("sourceName", 
            null, 
            (new Attributes()).add("units", ISO8601TZ_FORMAT).
                add("actual_range", new StringArray(new String[]{"1970-01-01T00:00:00Z", "2007-01-01T00:00:00Z"})),
            "String");

        //test 'Z'
        String t1 = "2007-01-02T03:04:05Z";
        double d = eta.sourceTimeToEpochSeconds(t1);
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(d)+"Z", t1, "a1");
        Test.ensureEqual(eta.epochSecondsToSourceTimeString(d), t1, "a2");

        //test -01:00
        String t2 = "2007-01-02T02:04:05-01:00";
        d = eta.sourceTimeToEpochSeconds(t2);
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(d)+"Z", t1, "b1");
        Test.ensureEqual(eta.epochSecondsToSourceTimeString(d), t1, "b2");


        //*** no Z
        String2.log("\n*** test no Z");
        eta = new EDVTime("sourceName", 
            null, (new Attributes()).add("units", ISO8601T_FORMAT).  //without Z
                add("actual_range", new StringArray(new String[]{
                    "1970-01-01T00:00:00", "2007-01-01T00:00:00"})),  //without Z
            "String");

        //test no suffix    
        String t4 = "2007-01-02T03:04:05"; //without Z
        d = eta.sourceTimeToEpochSeconds(t4);
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(d)+"Z", t1, "b1");
        Test.ensureEqual(eta.epochSecondsToSourceTimeString(d)+"Z", t1, "b2");

    }
}
