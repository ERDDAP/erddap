/* 
 * CfToGcmd Copyright 2011, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import java.util.Arrays; 
import java.util.HashMap; 
import java.util.HashSet; 


/** 
 * This class has static methods to convert CF Standard Names to/from GCMD Science Keywords.
 * This uses CfToGcmd.txt (7bit ASCII file) which Bob Simons generated (best effort, not perfect!)
 * ~2011-09-29 based on 
 * ./cfStdNames.txt 
 * (Bob created from CF Standard Names, Version 18, 22 July 2011
 * at http://cfconventions.org/Data/cf-standard-names/18/build/cf-standard-name-table.html 
 * Aliases are treated like other Standard Names.  File sorted by EditPlus.)
 * and 
 * ./gcmdScienceKeywords.txt 
 * (Bob created from GCMD Science Keywords  2008-02-05
 * at  http://gcmd.nasa.gov/Resources/valids/archives/keyword_list.html
 * specifically http://gcmd.nasa.gov/Resources/valids/archives/GCMD_Science_Keywords.pdf )
 */
public class CfToFromGcmd { 


    /**
     * Set this to true (by calling reallyVerbose=true in your program, 
     * not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean reallyVerbose = false; 

    /** 
     * The (simply sorted) list of CF Standard Names.  
     * <br>Other classes can use this but shouldn't change it! 
     * <br>The first value is "".
     * <br>CF Standard Names which have no corresponding
     *    GCMD Science Keyword are not in this list.
     */
    public static String cfNames[];
    /** 
     * The GCMD Science Keywords associated with each of the cfNames. 
     * <br>Other classes can use this but shouldn't change it!
     *
     * <p>!!!Note the some will be String[0] because there are no matching GCMD keywords!
     */
    public static String cfToGcmd[][];

    /** 
     * The (simply sorted) list of GCMD Science Keywords. 
     * <br>Other classes can use this but shouldn't change it! 
     * <br>The first value is "".
     * <br>GCMD Science Keywords which have no corresponding
     *     CF Standard Names not in this list.
     */
    public static String gcmdKeywords[];
    /** 
     * The CF Standard Names associated with each of the gcmdKeywords. 
     * <br>Other classes can use this but shouldn't change it!
     */
    public static String gcmdToCf[][];

    /**
     * Other classes can use this but shouldn't change it!
     */
    public static String array0[] = new String[0];

    /** 
     * This static block reads the information from [thisDirectory]/CfToGcmd.txt
     * and organizes the information into the (currently null) static data structures above.
     *
     * <p>!!! If a standardName has to matching GCMD keywords, it will point to String[0]
     *
     * @throws RuntimeException if trouble
     */
    static {
        //this is done only once so no concurrency issues
        //use String2.getClass(), not ClassLoader.getSystemResource (which fails in Tomcat)
        String fileName = String2.getClassPath() + "gov/noaa/pfel/erddap/util/CfToGcmd.txt";
        String2.log("CfToFromGcmd static loading " + fileName);
        StringArray lines;
        try {
            lines = StringArray.fromFile(fileName, "ISO-8859-1"); //actually, 7bit ASCII
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        int nLines = lines.size();
        HashMap cfHashMap = new HashMap();
        HashMap gcmdHashMap = new HashMap();
        cfHashMap.put("", new StringArray());
        gcmdHashMap.put("", new HashSet());
        boolean prevLineBlank = true;
        String prevCFs = null;
        StringArray prevCFsar = null;
        for (int i = 0; i < nLines; i++) {
            String s = String2.canonical(lines.get(i).trim());
            if (s.length() == 0) {
                //blank line
                prevLineBlank = true;
                continue;
            }
            if (prevLineBlank) {
                //this line is CF standard name
                prevCFs = s;
                prevCFsar = null;
                prevLineBlank = false;

            } else {
                //this line is GCMD keyword
                if (prevCFsar == null) {
                    //first GCMD for this CF
                    prevCFsar = new StringArray();
                    Object old = cfHashMap.put(prevCFs, prevCFsar);
                    if (old != null)
                        throw new RuntimeException("In file=" + fileName + "\n" +
                            "there should only be one section for CF=" + prevCFs);
                }
                prevCFsar.add(s);

                HashSet gcmdHashSet = (HashSet)gcmdHashMap.get(s);
                if (gcmdHashSet == null) {
                    gcmdHashSet = new HashSet();
                    gcmdHashMap.put(s, gcmdHashSet);
                }
                gcmdHashSet.add(prevCFs);
            }
        }

        //convert cfHashMap to cfNames and cfToGcmd
        cfNames = (String[])cfHashMap.keySet().toArray(new String[0]); 
        Arrays.sort(cfNames); //they are consistently capitalized, so sort works nicely
        int nCF = cfNames.length;
        cfToGcmd = new String[nCF][];
        for (int i = 0; i < nCF; i++) {
            StringArray sar = (StringArray)cfHashMap.get(cfNames[i]);
            //sort?  No, there is a general ordering of best to least good
            cfToGcmd[i] = sar.toArray();
        }
        
        //convert gcmdHashMap to gcmdKeywords and gcmdToCf
        gcmdKeywords = (String[])gcmdHashMap.keySet().toArray(new String[0]); 
        Arrays.sort(gcmdKeywords); //they are consistently capitalized, so sort works nicely
        int nGCMD = gcmdKeywords.length;
        gcmdToCf = new String[nGCMD][];
        for (int i = 0; i < nGCMD; i++) {
            HashSet hashSet = (HashSet)gcmdHashMap.get(gcmdKeywords[i]);
            gcmdToCf[i] = (String[])hashSet.toArray(new String[0]);
            Arrays.sort(gcmdToCf[i]);  //they are consistently capitalized, so sort works nicely
        }
        
    }

    /** 
     * This converts a CF Standard Name into a list of GCMD Science Keywords 
     * (currently ordered roughly as best to least good match). 
     *
     * @param cf the CF Standard Name
     * @return a String[] of matching GCMD Science Keywords
     *    (or String[0] if there are none or there is trouble).
     *    Don't make changes to the String[].
     */
    public static String[] cfToGcmd(String cf) {
        if (cf == null)
            return array0;
        cf = cf.trim();
        if (cf.length() == 0)
            return array0;

        //!!! If a standardName has to matching GCMD keywords, it will point to String[0]
        //so finding it doesn't necessarily mean there are matching GCMD keywords.
        int which = Arrays.binarySearch(cfNames, cf);
        return which < 0? array0 : cfToGcmd[which];
    }
        
    /** 
     * This converts a GCMD Science Keyword into a list of CF Standard Names
     * (sorted alphabetically). 
     *
     * @param gcmd the GCMD Science Keyword
     * @return a String[] of matching CF Standard Names
     *    (or String[0] if there are none or there is trouble).
     *    Don't make changes to the String[].
     */
    public static String[] gcmdToCf(String gcmd) {
        if (gcmd == null)
            return array0;
        gcmd = gcmd.trim();
        if (gcmd.length() == 0)
            return array0;
        int which = Arrays.binarySearch(gcmdKeywords, gcmd);
        return which < 0? array0 : gcmdToCf[which];
    }
        


    /**
     * This tests CF to GCMD.
     *
     * @throws RuntimeException if trouble
     */
    public static void testCfToGcmd() {

        String2.log("\n*** CfToFromGcmd.testCfToGcmd");
        //reallyReallyVerbose = true;

        //first
        Test.ensureEqual(cfNames[0], "", "test1");
        Test.ensureEqual(cfToGcmd(""), new String[0], "test2");

        //second
        Test.ensureEqual(
            String2.toCSSVString(cfToGcmd("aerosol_angstrom_exponent")),
            "Atmosphere > Aerosols > Aerosol Optical Depth/Thickness > Angstrom Exponent, " +
            "Atmosphere > Aerosols > Aerosol Particle Properties, " +
            "Atmosphere > Aerosols > Particulate Matter",
            "test3");

        //in middle  (with lead/trail spaces)
        Test.ensureEqual(
            String2.toCSSVString(cfToGcmd("  sea_water_temperature  ")),
            "Oceans > Ocean Temperature > Water Temperature",
            "test4");

        //no translation
        Test.ensureEqual(String2.toCSSVString(cfToGcmd("model_level_number")), "", "test5");

        //last
        Test.ensureEqual(
            String2.toCSSVString(cfToGcmd("zenith_angle")),
            "Atmosphere > Atmospheric Radiation > Incoming Solar Radiation, " +
            "Atmosphere > Atmospheric Radiation > Solar Irradiance, " +
            "Atmosphere > Atmospheric Radiation > Solar Radiation",
            "test6");

    
        String2.log("\n*** CfToFromGcmd.testCfToGcmd finished successfully.");
    }

    /**
     * This tests GCMD to CF.
     *
     * @throws RuntimeException if trouble
     */
    public static void testGcmdToCf() {

        String2.log("\n*** CfToFromGcmd.testGcmdToCf");
        //reallyReallyVerbose = true;

        //nothing
        Test.ensureEqual(gcmdToCf(""), new String[0], "test20");

        //invalid/unknown name
        Test.ensureEqual(gcmdToCf("bob"), new String[0], "test21");

        // (with lead/trail spaces)
        Test.ensureEqual(
String2.toCSSVString(gcmdToCf(
  "  Atmosphere > Atmospheric Chemistry > Carbon and Hydrocarbon Compounds > Carbon Dioxide  ")),
"atmosphere_mass_content_of_carbon_dioxide, " +
"atmosphere_mass_of_carbon_dioxide, " +
"atmosphere_moles_of_carbon_dioxide, " +
"mass_concentration_of_carbon_dioxide_in_air, " +
"mass_fraction_of_carbon_dioxide_in_air, " +
"mole_concentration_of_carbon_dioxide_in_air, " +
"mole_fraction_of_carbon_dioxide_in_air, " +
"surface_carbon_dioxide_mole_flux, " +
"surface_carbon_dioxide_partial_pressure_difference_between_air_and_sea_water, " +
"surface_carbon_dioxide_partial_pressure_difference_between_sea_water_and_air, " +
"surface_downward_mass_flux_of_carbon_dioxide_expressed_as_carbon, " +
"surface_downward_mole_flux_of_carbon_dioxide, " +
"surface_frozen_carbon_dioxide_amount, " +
"surface_net_downward_mass_flux_of_carbon_dioxide_expressed_as_carbon_due_to_all_land_processes, " +
"surface_net_downward_mass_flux_of_carbon_dioxide_expressed_as_carbon_due_to_all_land_processes_excluding_anthropogenic_land_use_change, " +
"surface_net_upward_mass_flux_of_carbon_dioxide_expressed_as_carbon_due_to_emission_from_anthropogenic_land_use_change, " +
"surface_partial_pressure_of_carbon_dioxide_in_air, " +
"surface_upward_mass_flux_of_carbon_dioxide_expressed_as_carbon_due_to_emission_from_crop_harvesting, " +
"surface_upward_mass_flux_of_carbon_dioxide_expressed_as_carbon_due_to_emission_from_fires_excluding_anthropogenic_land_use_change, " +
"surface_upward_mass_flux_of_carbon_dioxide_expressed_as_carbon_due_to_emission_from_grazing, " +
"surface_upward_mass_flux_of_carbon_dioxide_expressed_as_carbon_due_to_emission_from_natural_sources, " +
"surface_upward_mole_flux_of_carbon_dioxide, " +
"tendency_of_atmosphere_mass_content_of_carbon_dioxide_due_to_emission, " +
"tendency_of_atmosphere_mass_content_of_carbon_dioxide_expressed_as_carbon_due_to_anthropogenic_emission, " +
"tendency_of_atmosphere_mass_content_of_carbon_dioxide_expressed_as_carbon_due_to_emission_from_fossil_fuel_combustion, " +
"tendency_of_atmosphere_moles_of_carbon_dioxide",
            "test22");

        //doesn't exist
        Test.ensureEqual(String2.toCSSVString(gcmdToCf("bob")), "", "test23");

        String2.log("\n*** CfToFromGcmd.testGcmdToCf finished successfully.");
    }

    /**
     * This tests this class.
     *
     * @throws RuntimeException if trouble
     */
    public static void test() {
        testCfToGcmd();
        testGcmdToCf();
    }

}
