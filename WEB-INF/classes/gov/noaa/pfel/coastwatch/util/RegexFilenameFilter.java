/* 
 * RegexFilenameFilter Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.LongArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.array.StringComparatorIgnoreCase;

import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A filter to find files (and directories!) whose names match a specified 
 * regular expression.
 * See regEx documentation in Java Docs for java.util.regex.Pattern.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2005-02-10
 *
 */
public class RegexFilenameFilter implements FilenameFilter {
    private String regex;
    private Pattern pattern;

    //ideally, not static, but used for informational purposes only
    public static long getTime, matchTime, sortTime; 

    /**
     * The constructor.
     *
     * @param regex the regular expression.  
     * See regEx documentation in Java Docs for java.util.regex.Pattern.
     */
    public RegexFilenameFilter(String regex) {
        this.regex = regex;
        pattern = Pattern.compile(regex);
    }

    /**
     * Determines whether a file is accepted.
     * This is part of the FilenameFilter implementation.
     * 
     * <p>Note that this doesn't check if the name 
     * represents a file or a directory (and there is no way
     * I know of to specify that distinction as part of the regex).
     *
     * @param dir
     * @param name
     * @return true if the name matches the pattern
     */
    public boolean accept(File dir, String name) {
        return pattern.matcher(name).matches();
    }

    /**
     * Returns a sorted list of file names which match the regex
     * in the specified directory (e.g., "c:\\cohort\\").
     * This will return null if trouble.
     *
     * <p>Sample use: unzip all the .zip files in a directory:
       <pre>
        String dir = "c:\\programs\\GrdFiles\\";
        String[] zipFiles = RegexFilenameFilter.list(dir, ".*\\.zip");
        for (int i = 0; i < zipFiles.length; i++)
            SSR.unzip(dir + zipFiles[i], dir, true, null);
       </pre>
     * 
     * <p>Note that this doesn't check if the name 
     * represents a file or a directory (and there is no way
     * I know of no way to specify that distinction as part of the regex).
     *
     * @param dir the directory of interest (with or without a trailing slash)
     * @param regex  See regEx documentation in Java Docs for java.util.regex.Pattern.
     * @return a sorted list of the matching file names (just the names, without the dirs)
     *     or null if trouble (e.g., dir doesn't exist)
     */
    public static String[] list(String dir, String regex) {
        try {
            ArrayList<String> list = new ArrayList();
            long tTime = System.currentTimeMillis();
            File dirFile = new File(dir);
            if (!dirFile.isDirectory())
                return list.toArray(new String[0]);

            //get all names    
            String[] allNames = dirFile.list();
            if (allNames == null)
                return null;
            getTime += System.currentTimeMillis() - tTime;

            //determine which match the regex
            tTime = System.currentTimeMillis();
            RegexFilenameFilter filter = new RegexFilenameFilter(regex); 
            int n = allNames.length;
            for (int i = 0; i < n; i++) 
                if (filter.accept(null, allNames[i])) 
                    list.add(allNames[i]);
            matchTime += System.currentTimeMillis() - tTime;

            //sort
            tTime = System.currentTimeMillis();
            Collections.sort(list);
            sortTime += System.currentTimeMillis() - tTime;

            //return 
            return list.toArray(new String[0]);
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            return null;
        }
    }

    /**
     * DEPRECATED - USE FileVisitorDNLS INSTEAD!
     * This gathers information about all subdirectories (regardless of regex)
     * and all files matching the regex
     * in the specified directory (e.g., "c:/cohort");
     * 
     * @param dir the directory of interest (with or without a trailing slash)
     * @param regex  See regEx documentation in Java Docs for java.util.regex.Pattern.
     * @return PrimitiveArray[] [0]=dirNames(StringArray) with trailing / or \\, 
     *   [1]=fileNames(StringArray), 
     *   [2]=fileLastModified(LongArray), [3]=fileSize(LongArray).
     *   dirNames will not include parent ("..") or self (".").
     *   The sizes of [1], [2], [3] will be the same.
     *   [0] and [1] will each be sorted (ignoringCase).
     *   
     * @throws RuntimeException if trouble
     */
    public static PrimitiveArray[] gatherInfo(String dir, String regex) {

        //add slash to end of dir (if none)
        dir = File2.addSlash(dir);

        StringArray dirNames         = new StringArray();
        StringArray fileNames        = new StringArray(); 
        LongArray   fileLastModified = new LongArray();
        LongArray   fileSize         = new LongArray();
        PrimitiveArray paAr[] = new PrimitiveArray[]{dirNames,
            fileNames, fileLastModified, fileSize};
        
        //get a list of files and dirs
        String[] names = (new File(dir)).list();
        if (names == null)
            return paAr;

        //for each, determine if it is a file or a dir
        Arrays.sort(names, new StringComparatorIgnoreCase());
        int n = names.length;
        for (int i = 0; i < n; i++) {
            String tName = names[i];
            File tFile = new File(dir + tName);
            if (tFile.isDirectory()) {
                if (!tName.equals("."))  //ignore self
                    dirNames.add(tName);
            } else if (tFile.isFile()) {
                if (tName.matches(regex)) {
                    fileNames.add(tName);
                    fileLastModified.add(tFile.lastModified());
                    fileSize.add(tFile.length());
                }
            } else String2.log(
                String2.ERROR + " in RegexFilenameFilter.gatherInfo: \"" + 
                  dir + tName + "\" isn't a file or a directory.  (symbolic link?)");
        }
        return paAr;
    }

    /**
     * This is like list(), but returns the full file names.
     * 
     * <p>Note that this doesn't check if the name 
     * represents a file or a directory (and there is no way
     * I know of no way to specify that distinction as part of the regex).
     *
     * @param dir the directory of interest (with or without a trailing slash)
     * @param regex  See regEx documentation in Java Docs for java.util.regex.Pattern.
     * @return a sorted list of the matching dir + file names 
     *     or null if trouble (e.g., dir doesn't exist).
     *     The slashes will match the slashes in dir (\\ or /).
     */
    public static String[] fullNameList(String dir, String regex) {
        String list[] = list(dir, regex);
        if (list == null)
            return null;
        dir = File2.addSlash(dir);
        for (int i = 0; i < list.length; i++)
            list[i] = dir + list[i];
        return list;
    }


    /**
     * This adds file names which match the regex
     * in the specified directory (e.g., "c:/cohort") 
     * AND IN RECURSIVELY FOUND SUBDIRECTORIES to an arrayList.
     * 
     * <p>Note that this *does* check if the name 
     * represents a file or a directory -- see directoriesToo.
     *
     * @param arrayList to which full file names will be added.
     *    Directories sort higher than the file names in those directories.
     *    If the dir doesn't exist or is empty, this adds nothing to the arrayList.
     * @param dir the directory of interest (with or without a trailing slash)
     * @param regex  See regEx documentation in Java Docs for java.util.regex.Pattern.
     * @param directoriesToo if true, directory names are also added to the 
     *    arrayList, with "/" or "\\" (to match dir) added to the end to 
     *    identify them as directories.
     * @throws RuntimeException if trouble
     */
    public static void recursiveFullNameList(ArrayList<String> arrayList, String dir, 
        String regex, boolean directoriesToo) {

        //add slash to end of dir (if none)
        dir = File2.addSlash(dir);
        
        //get a list of files and dirs
        String[] names = (new File(dir)).list();
        if (names == null)
            return;

        //for each, determine if it is a file or a dir
        int n = names.length;
        for (int i = 0; i < n; i++) {
            String tName = names[i];
            File tFile = new File(dir + tName);
            if (tName.equals(".") || tName.equals("..")) { //ignore parent and itself
            } else if (tFile.isFile()) {
                if (tName.matches(regex))
                    arrayList.add(dir + tName);
            } else if (tFile.isDirectory()) {
                String tDir = File2.addSlash(dir + tName); 
                if (directoriesToo) arrayList.add(tDir);
                //String2.log("directory=" + tDir);
                recursiveFullNameList(arrayList, tDir, regex, directoriesToo);
            } else String2.log(
                String2.ERROR + " in RegexFilenameFilter.recursiveFullNameList: \"" + 
                  dir + tName + "\" isn't a file or a directory.  (symbolic link?)");
        }
    }


    /**
     * This returns a String[] with the file names which match the regex
     * in the specified directory (e.g., "c:\\cohort") 
     * AND IN RECURSIVELY FOUND SUBDIRECTORIES.
     *
     * <p>Note that this *does* check if the name 
     * represents a file or a directory -- see directoriesToo.
     *
     * @param dir the directory of interest
     * @param regex  See regEx documentation in Java Docs for java.util.regex.Pattern.
     * @param directoriesToo if true, directory names are also added to the 
     *    arrayList, with "/" added to the end to identify them as directories.
     * @return an array of the matching file names 
     * @throws RuntimeException if trouble
     */
    public static String[] recursiveFullNameList(String dir, String regex, 
        boolean directoriesToo) {

        ArrayList<String> arrayList = new ArrayList();
        recursiveFullNameList(arrayList, dir, regex, directoriesToo);
        String sar[] = arrayList.toArray(new String[0]);
        Arrays.sort(sar);
        return sar;
    }


    /** 
     * This deletes the specified files in a directory.
     * BEWARE: THIS IS VERY POWERFUL!!!!
     *
     * @param dir a full file directory (e.g., c:/u00/satellite/temp/)
     *    (trailing slash is optional)
     * @param regex to identify the file names to be deleted.
     * @param recursive If true, all subdirectories will also be searched.
     *    Empty directories won't be deleted.
     * @return the number of files that couldn't be deleted.
     * @throws RuntimeException if trouble.
     */
    public static int regexDelete(String dir, String regex, boolean recursive) {
        if (!File2.isDirectory(dir)) {
            String2.log("WARNING: regexDelete says: \"" + dir + "\" isn't a directory.");
            return 0;
        }
        String names[] = recursive? 
            recursiveFullNameList(dir, regex, false) :
            fullNameList(dir, regex);
        int notDeleted = 0;
        for (int i = 0; i < names.length; i++)
            if (!File2.delete(names[i]))
                notDeleted++;
        return notDeleted;
    }

    /** 
     * This deletes all the files and subdirectories in a directory.
     * BEWARE: THIS IS VERY POWERFUL!!!!
     * See also com.cohort.util.File2.deleteAllFiles().
     *
     * @param dir a full file directory (e.g., c:/u00/satellite/temp/)
     *    (trailing slash is optional)
     * @throws RuntimeException if trouble
     */
    public static void recursiveDelete(String dir) {
        if (!File2.isDirectory(dir))
            return;
        String names[] = recursiveFullNameList(dir, ".+", true);
        //work backwards, because need to delete files before delete containing directory
        for (int i = names.length - 1; i >= 0; i--) {
            File file = new File(names[i]);
            String2.log("recursiveDelete " + names[i]);
            //Math2.sleep(5000);
            Test.ensureTrue(file.delete(),
                String2.ERROR + " in RegexFilenameFilter.recursiveDelete: unable to delete " +
                names[i]);
        }
        Test.ensureTrue(File2.delete(dir), 
            String2.ERROR + " in RegexFilenameFilter.recursiveDelete: unable to delete " + 
            dir);
    }



    /**
     * This tests the methods of RegexFilenameFilter.
     *
     * @throws Exception if trouble
     */
    public static void test() throws Exception {
        String2.log("\n* testing RegexFilenameFilter ...");
        String coastwatchDir = SSR.getContextDirectory() + //with / separator and / at the end
            "WEB-INF/classes/gov/noaa/pfel/coastwatch/";

        //test list
        String[] sar = list(coastwatchDir, "S.+\\.java");
        String[] shouldBe = {
            "Screen.java",
            "Shared.java"};
        Test.ensureEqual(sar, shouldBe, "RegexFilenameFilter.list");

        //test fullNameList
        sar = fullNameList(coastwatchDir, "S.+\\.java");
        shouldBe = new String[] {
            coastwatchDir + "Screen.java",
            coastwatchDir + "Shared.java"
            };
        Test.ensureEqual(sar, shouldBe, "RegexFilenameFilter.fullNameList");

        //test recursiveList
        sar = recursiveFullNameList(coastwatchDir, "S.+\\.java", true);
        shouldBe = new String[] {
            coastwatchDir + "Screen.java",
            coastwatchDir + "Shared.java",
            coastwatchDir + "griddata/",
            coastwatchDir + "griddata/SaveOpendap.java",
            coastwatchDir + "hdf/",
            coastwatchDir + "hdf/SdsReader.java",
            coastwatchDir + "hdf/SdsWriter.java",
            coastwatchDir + "netcheck/",
            coastwatchDir + "pointdata/",
            coastwatchDir + "pointdata/StationVariableNc4D.java",
            coastwatchDir + "pointdata/StoredIndex.java",
            coastwatchDir + "sgt/",
            coastwatchDir + "sgt/SGTPointsVector.java",
            coastwatchDir + "sgt/SgtGraph.java",
            coastwatchDir + "sgt/SgtMap.java",
            coastwatchDir + "sgt/SgtUtil.java",
            coastwatchDir + "util/",
            coastwatchDir + "util/SSR.java",
            coastwatchDir + "util/SimpleXMLReader.java",
            coastwatchDir + "util/StringObject.java"             
            };
        Test.ensureEqual(sar, shouldBe, "RegexFilenameFilter.recursiveFullNameList");

        //test recursiveList   no directories
        sar = recursiveFullNameList(coastwatchDir, "S.+\\.java", false);
        shouldBe = new String[] {
            coastwatchDir + "Screen.java",
            coastwatchDir + "Shared.java",
            coastwatchDir + "griddata/SaveOpendap.java",
            coastwatchDir + "hdf/SdsReader.java",
            coastwatchDir + "hdf/SdsWriter.java",
            coastwatchDir + "pointdata/StationVariableNc4D.java",
            coastwatchDir + "pointdata/StoredIndex.java",
            coastwatchDir + "sgt/SGTPointsVector.java",
            coastwatchDir + "sgt/SgtGraph.java",
            coastwatchDir + "sgt/SgtMap.java",
            coastwatchDir + "sgt/SgtUtil.java",
            coastwatchDir + "util/SSR.java",
            coastwatchDir + "util/SimpleXMLReader.java",
            coastwatchDir + "util/StringObject.java"             
            };
        Test.ensureEqual(sar, shouldBe, "RegexFilenameFilter.recursiveFullNameList");

        //gatherInfo
        PrimitiveArray info[] = gatherInfo("C:\\programs\\digir", "obis.*");
        int tn = info[1].size();
        StringArray lastMod = new StringArray();
        for (int i = 0; i < tn; i++)
            lastMod.add(Calendar2.safeEpochSecondsToIsoStringTZ(info[2].getLong(i) / 1000.0, "ERROR"));
        Test.ensureEqual(info[0].toString(), 
            "BiotaDiGIRProvider, DiGIR_Portal_Engine, DiGIRprov", "");
        Test.ensureEqual(info[1].toString(), 
            "obis.xsd, obisInventory.txt", "");
        //lastMod and size verified by using DOS dir command
        Test.ensureEqual(lastMod.toString(),                                 
            "2007-04-23T18:24:38Z, 2007-05-02T19:18:33Z", "");
        Test.ensureEqual(info[3].toString(), 
            "21509, 3060", "");
    }

}