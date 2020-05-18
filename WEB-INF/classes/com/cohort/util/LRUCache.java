/* This file is Copyright (c) 2010, NOAA.
 * See the MIT/X-like license in LICENSE.txt.
 * For more information, bob.simons@noaa.gov.
 */
package com.cohort.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A HashMap/cache where, when size &gt; tMaxSize and a new element is added, 
 *     the Least-Recently-Used element will be removed.
 *
 * <p>This not thread-safe. To make it thread-safe, use
 *   <tt>Map cache = Collections.synchronizedMap(new LRUCache(maxSize));</tt>
 *
 */
public class LRUCache extends LinkedHashMap {

    int maxSize;
 
    /** Constructor 
     * @param tMaxSize the maximum number of elements you want this to cache.
     *     When size &gt; tMaxSize and a new element is added, 
     *     the Least-Recently-Used element will be removed.
     */
    public LRUCache(int tMaxSize) {
        super(tMaxSize + 1, 0.75f, true); //true means 'eldest' based on when last accessed (not when inserted) 
        maxSize = tMaxSize;
    }

    /** RemoveEldestEntry is over-ridden to enforce maxSize rule. */
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > maxSize;
     }

    /** Test this class. */
    public static void basicTest() {
        String2.log("\n*** LRUCache.basicTest");
        LRUCache cache = new LRUCache(5);
        for (int i = 0; i < 5; i++)
            cache.put("" + i, "" + (11 * i));
        Test.ensureEqual(cache.size(), 5, "");
        Test.ensureNotNull(cache.get("0"), ""); //0 was eldest. Now accessed so 1 is eldest

        //knock "1" out of cache
        cache.put("6", "66");
        Test.ensureEqual(cache.size(), 5, "");
        Test.ensureTrue(cache.get("1") == null, "");
        Test.ensureNotNull(cache.get("2"), "");

        //knock "3" out of cache
        cache.put("7", "77");
        Test.ensureEqual(cache.size(), 5, "");
        Test.ensureTrue(cache.get("3") == null, "");
        Test.ensureNotNull(cache.get("4"), "");

        String2.log("LRUCache.basicTest finished");           
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
        String msg = "\n^^^ LRUCache.test(" + interactive + ") test=";

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
