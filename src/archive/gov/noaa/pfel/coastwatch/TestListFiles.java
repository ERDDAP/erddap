package gov.noaa.pfel.coastwatch;

import java.io.File;

/** This is a tiny program used for test purposes.*/
public class TestListFiles {

    /** This method times file.listFiles, where 'file' is created from args[0]. */
    public static void main(String args[]) throws Exception {
        File file = new File(args[0]);
        long time = System.currentTimeMillis();
        File files[] = file.listFiles();
        System.out.println("TestListFiles found " + files.length + " files in " + args[0] +
            " in " + (System.currentTimeMillis() - time) + " ms.");
    }

}