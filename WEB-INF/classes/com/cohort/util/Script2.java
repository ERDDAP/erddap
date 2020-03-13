/* This file is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.util;

import com.cohort.util.String2;

import java.util.HashSet;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;

/**
 * A class with static Jexl script methods.
 * For tests, see gov.noaa.pfel.coastwatch.pointdata.ScriptRow.testScript().
 */
public class Script2 {

    /** Use jexlEngine to instantiate and obtain this. */
    private static JexlEngine jexlEngine;

    /** This returns a JexlEngine suitable for processing rows of a table.
     * It has a sandbox to ensure only safe, pre-approved classes and methods can be used.
     * It has strict=true, so it requires variables, methods and functions to be defined
     *   (instead of treating unknowns as null, like javascript)
     * It has silent=false, so it throws exception if trouble (e.g., unknown method).
     * The silent and strict settings make it more like Java than JavaScript.
     * The jexlEngine is thread safe.
     */
    public static JexlEngine jexlEngine() {
        if (jexlEngine == null) {
            //just instantiate if needed, then reuse
            //SECURITY: need to prevent script from accessing any class
            //Solution: make a new engine with a sandbox that blocks everything except specific classes
            //Start with a blacklist that allows nothing
            JexlSandbox jsandbox = new JexlSandbox(false); 
            //then add classes to the whitelist  
            jsandbox.white("com.cohort.util.ScriptCalendar2");
            jsandbox.white("com.cohort.util.ScriptMath");
            jsandbox.white("com.cohort.util.ScriptMath2");
            jsandbox.white("gov.noaa.pfel.coastwatch.pointdata.ScriptRow");
            jsandbox.white("java.lang.String"); 
            jsandbox.white("com.cohort.util.ScriptString2");
            jexlEngine = new JexlBuilder().sandbox(jsandbox).strict(true).silent(false).create();
        }
        return jexlEngine;
    }

    private static ScriptCalendar2 scriptCalendar2;
    private static ScriptMath      scriptMath;
    private static ScriptMath2     scriptMath2;
    private static String          scriptString;
    private static ScriptString2   scriptString2;

    /**
     * This returns a new Jexl MapContext with references to 
     * Calendar2, Math, Math2, String, and String2, 
     * so all you need to do is set("row", new ScriptRow(fileName, table)).
     */
    public static MapContext jexlMapContext() {
        //this needs to be "new" because a different Row will be used by different threads
        if (scriptString2 == null) { //test last one
            //just instantiate if needed, then reuse
            scriptCalendar2 = new ScriptCalendar2();
            scriptMath      = new ScriptMath();
            scriptMath2     = new ScriptMath2();
            scriptString    = new String("");
            scriptString2   = new ScriptString2();
        }
        MapContext jcontext = new MapContext();
        jcontext.set("Calendar2", scriptCalendar2);
        jcontext.set("Math",      scriptMath);
        jcontext.set("Math2",     scriptMath2);
        jcontext.set("String",    scriptString);
        jcontext.set("String2",   scriptString2);
        return jcontext;
    }

    public final static String  SCRIPT_COLUMN_REFERENCE_REGEX = "row\\.column(Int|Long|Float|Double|String)\\(\"(.+?)\"\\)";
    public final static Pattern SCRIPT_COLUMN_REFERENCE_PATTERN = Pattern.compile(SCRIPT_COLUMN_REFERENCE_REGEX);

    /** 
     * Given a script starting with '=', this extracts the names of the 
     * columns referenced by the script (e.g., "longitude" in row.columnDouble("longitude") ).
     *
     * @param script The script with or without the starting '='.
     * @return a HashSet with the referenced column names (may be size=0).
     */    
    public static HashSet<String> jexlScriptNeedsColumns(String script) {

        return String2.extractAllCaptureGroupsAsHashSet(script, SCRIPT_COLUMN_REFERENCE_PATTERN, 2);
     }


} //End of Script2 class.
