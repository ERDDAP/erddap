/* 
 * Directory Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.MustBe;
import com.cohort.util.String2;

import java.io.File;
import java.util.Arrays;
import java.util.Vector; 

/**
 * THIS ISN'T CURRENTLY BEING USED.
 * This class holds a list of files and subdirectories (not including parent 
 * or itself) as vectors (so thread-safe).
 *
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-02-10
 *
 * <p>Changes:
 * <ul>
 * </ul>
 * 
 */
public class Directory {
    private String parent;
    private String name;
    private Vector files;          //of strings, thread-safe
    private Vector subdirectories; //of Directories, thread-safe
    private boolean recursiveHasFiles = false;

    /**
     * This returns the parent name.
     * @return the parent name
     */
    public String getParent() {return parent;}

    /**
     * This returns the directory's short name.
     * @return the directory's short name
     */
    public String getName() {return name;}

    /**
     * This returns the vector of file names. Don't change the contents.
     * @return the vector of file names
     */
    public Vector getFiles() {return files;}

    /**
     * This returns the vector of subdirectory names. Don't change the contents.
     * @return the vector of subdirectory names
     */
    public Vector getSubdirectories() {return subdirectories;}

    /**
     * This returns true if this directory or an subdirectory has any actual files.
     * @return true if this directory or an subdirectory has any actual files.
     */
    public boolean getRecursiveHasFiles() {return recursiveHasFiles;}


    /**
     * Given a directory name, this populates this directory object.
     * and (recursively) all subdirectories.
     *
     * @param parent the absolute name of the parent (e.g., c:\programs\tomcat)
     * @param name the name of this directory (e.g., logs)
     * @return true if this directory or an subdirectory has any actual files.
     */
    public boolean reset(String parent, String name) {
        try {
            this.parent = parent;
            this.name = name;
            files = new Vector();
            subdirectories = new Vector();
            recursiveHasFiles = false;

            //get a list of files and dirs
            String absoluteName = parent + File.separator + name;
            String[] names = (new File(absoluteName)).list();
            Arrays.sort(names);

            //for each, determine if it is a file or a dir
            int n = names.length;
            for (int i = 0; i < n; i++) {
                String tName = names[i];
                File tFile = new File(absoluteName + File.separator + tName);
                if (tName.equals(".") || tName.equals("..")) { //ignore parent and itself
                } else if (tFile.isFile()) {
                    files.add(tName);
                    recursiveHasFiles = true;
                } else if (tFile.isDirectory()) {
                    Directory d = new Directory();
                    if (d.reset(absoluteName, tName))
                        recursiveHasFiles = true;
                    subdirectories.add(d);
                } else String2.log(
                    "Error in Directory.reset: \"" + 
                    absoluteName + File.separator + tName + "\" isn't a file or a directory.");
            }
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
        }
        return recursiveHasFiles;
    }

    /**
     * Returns true if this directory has actual files.
     *
     * @return true if this directory has any actual files.
     */
    public boolean hasFiles() {
        return files.size() > 0;
    }

}
