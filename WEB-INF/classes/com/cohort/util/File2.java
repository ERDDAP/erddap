/* This file is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * File2 has useful static methods for working with files.
 *
 */
public class File2 {

    public static final String UNABLE_TO_DELETE = "unable to delete"; //so consistent in log.txt

    //define the file types for the purpose of assigning icon in Table.directoryListing
    //compressed and image ext from wikipedia
    //many ext from http://www.fileinfo.com/filetypes/common
    public static final String BINARY_EXT[] = {
        ".accdb", ".bin", ".bufr", ".cab", ".cdf", ".cer", ".class", ".cpi", ".csr",
        ".db", ".dbf", ".dll", ".dmp", ".drv", ".dwg", ".dxf", ".fnt", ".fon", 
        ".grb", ".grib", ".grib2", ".ini", ".keychain", 
        ".lnk", ".mat", ".mdb", ".mim", ".nc", 
        ".otf", ".pdb", ".prf", ".sys", ".ttf"};
    public static final String COMPRESSED_EXT[] = {
        ".7z", ".a", ".ace", ".afa", ".alz", ".apk", 
        ".ar", ".arc", ".arj", ".ba", ".bh", ".bz2", 
        ".cab", ".cfs", ".cpio", ".dar", ".dd", ".deb", ".dgc", ".dmg", ".f",
        ".gca", ".gho", ".gz", 
        ".gzip", ".ha", ".hki", ".hqx", ".infl", ".iso", 
        ".j", ".jar", ".kgb", ".kmz", 
        ".lbr", ".lha", ".lz", ".lzh", ".lzma", ".lzo", ".lzx", 
        ".mar", ".msi", ".partimg", ".paq6", ".paq7", ".paq8", ".pea", ".pim", ".pit",
        ".pkg", ".qda", ".rar", ".rk", ".rpm", ".rz", 
        ".s7z", ".sda", ".sea", ".sen", ".sfark", ".sfx", ".shar", ".sit", ".sitx", ".sqx",
        ".tar", ".tbz2", ".tgz", ".tlz", ".toast", ".torrent",
        ".uca", ".uha", ".uue", ".vcd", ".war", ".wim", ".xar", ".xp3", ".xz", ".yz1", 
        ".z", ".zip", ".zipx", ".zoo"};
    public static final String IMAGE_EXT[] = {
        ".ai", ".bmp", ".cgm", ".draw", ".drw", ".gif", 
        ".ico", ".jfif", ".jpeg", ".jpg", 
        ".pbm", ".pgm", ".png", ".pnm", ".ppm", ".pspimage", 
        ".raw", ".svg", ".thm", ".tif", ".tiff", ".webp", ".yuv"};
    public static final String COMPRESSED_IMAGE_EXT[] = { //done quickly, the obvious compressed
        ".gif", ".jpeg", ".jpg", ".png", ".tif", ".tiff", ".webp"};
    public static final String LAYOUT_EXT[] = {
        ".doc", ".docx", ".indd", ".key", ".pct",
        ".pps", ".ppt", ".pptx",
        ".psd", ".qxd", ".qxp", ".rels", ".rtf", ".wpd", ".wps",
        ".xlr", ".xls", ".xlsx"};  
    public static final String MOVIE_EXT[] = {
        ".3g2", ".3gp", ".asf", ".asx", ".avi", ".fla", ".flv", 
        ".mov", ".mp4", ".mpg", ".ogv", ".rm", ".swf", ".vob", ".wmv", ".webm"};
    public static final String PDF_EXT[] = {".pdf"};
    public static final String PS_EXT[] = {".eps", ".ps"};
    public static final String SCRIPT_EXT[] = {  //or executable  
        ".app", ".asp", ".bat", ".cgi", ".com", ".csh", ".exe", ".gadget", ".js", ".jsp", 
        ".ksh", ".php", ".pif", ".pl", ".py", ".sh", ".tcsh", ".vb", ".wsf"};
    public static final String SOUND_EXT[] = {
        ".aif", ".aiff", ".aifc", ".au",
        ".flac", ".iff", ".m3u", ".m4a", ".mid", 
        ".mp3", ".mpa", ".ogg", ".wav", ".wave", ".wma"};
    public static final String COMPRESSED_SOUND_EXT[] = {  //done quickly, the obvious compressed
        ".flac", ".iff", ".m3u", ".m4a",  
        ".mp3", ".mpa", ".ogg", ".wma"};
    public static final String TEXT_EXT[] = {
        ".asc", ".c", ".cpp", ".cs", ".csv", ".das", ".dat", ".dds", 
        ".java", ".json", ".log", ".m", 
        ".sdf", ".sql", ".tsv", ".txt", ".vcf"};
    public static final String WORLD_EXT[] = {".css", ".htm", ".html", ".xhtml"};
    public static final String XML_EXT[] = {".dtd", ".gpx", ".kml", ".xml", ".rss"};

    /** The image file names in ERDDAP's /images/ for the different file types.
     * These may change periodically.
     * These parallel ICON_ALT. */
    public static final String ICON_FILENAME[] = {
        "generic.gif", "index.gif", "binary.gif", "compressed.gif", "image2.gif", 
        "layout.gif", "movie.gif", "pdf.gif", "ps.gif", "script.gif", 
        "sound1.gif", "text.gif", "world1.gif", "xml.gif"};

    /** The 3 character string used for <img alt=...> for the different file types.
     * I hope these won't change (other than adding new types).
     * These are from Apache directory listings.
     * These parallel ICON_FILENAME */
    public static final String ICON_ALT[] = {
        "UNK", "IDX", "BIN", "ZIP", "IMG", 
        "DOC", "MOV", "PDF", "PS", "EXE", 
        "SND", "TXT", "WWW", "XML"};

    /** These are the extensions of file types that netcdf-java makes 
     * additional index files for. This is not a perfect list.
     * I think GRIB and BUFR files can have other or no extension. */
    public static final String NETCDF_INDEX_EXT[] = {
        ".grb", ".grib", ".grib2", ".bufr"};


    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;
    public static boolean reallyVerbose = false;

    private static String tempDirectory; //lazy creation by getSystemTempDirectory

    /**
     * This indicates if the named file is indeed an existing file.
     * If dir="", it just says it isn't a file.
     *
     * @param fullName the full name of the file
     * @return true if the file exists
     */
    public static boolean isFile(String fullName) {
        try {
            //String2.log("File2.isFile: " + fullName);
            File file = new File(fullName);
            return file.isFile();
        } catch (Exception e) {
            if (verbose) String2.log(MustBe.throwable("File2.isFile(" + fullName + ")", e));
            return false;
        }
    }

    /**
     * This returns an index into ICON_FILENAME and ICON_ALT suitable for the fileName. 
     * The fallback is "UNK".
     *
     * @param fileName
     * @return an index into ICON_FILENAME and ICON_ALT suitable for the fileName. 
     */
    public static int whichIcon(String fileName) {
        String fileNameLC = fileName.toLowerCase();
        String extLC = getExtension(fileNameLC);

        if (fileNameLC.equals("index.html") ||
            fileNameLC.equals("index.htm"))              return String2.indexOf(ICON_ALT, "IDX");
        if (String2.indexOf(BINARY_EXT,     extLC) >= 0) return String2.indexOf(ICON_ALT, "BIN");
        if (String2.indexOf(COMPRESSED_EXT, extLC) >= 0) return String2.indexOf(ICON_ALT, "ZIP");
        if (String2.indexOf(IMAGE_EXT,      extLC) >= 0) return String2.indexOf(ICON_ALT, "IMG");
        if (String2.indexOf(LAYOUT_EXT,     extLC) >= 0) return String2.indexOf(ICON_ALT, "DOC");
        if (String2.indexOf(MOVIE_EXT,      extLC) >= 0) return String2.indexOf(ICON_ALT, "MOV");
        if (String2.indexOf(PDF_EXT,        extLC) >= 0) return String2.indexOf(ICON_ALT, "PDF");
        if (String2.indexOf(PS_EXT,         extLC) >= 0) return String2.indexOf(ICON_ALT, "PS ");
        if (String2.indexOf(SCRIPT_EXT,     extLC) >= 0) return String2.indexOf(ICON_ALT, "EXE");
        if (String2.indexOf(SOUND_EXT,      extLC) >= 0) return String2.indexOf(ICON_ALT, "SND");
        if (String2.indexOf(TEXT_EXT,       extLC) >= 0) return String2.indexOf(ICON_ALT, "TXT");
        if (String2.indexOf(WORLD_EXT,      extLC) >= 0) return String2.indexOf(ICON_ALT, "WWW");
        if (String2.indexOf(XML_EXT,        extLC) >= 0) return String2.indexOf(ICON_ALT, "XML");
        
        return String2.indexOf(ICON_ALT, "UNK");
    }

    public static boolean isCompressedExtension(String ext) {
        if (!String2.isSomething(ext))
            return false;
        ext = ext.toLowerCase();
        return String2.indexOf(File2.COMPRESSED_EXT, ext) >= 0 || 
               String2.indexOf(File2.COMPRESSED_IMAGE_EXT, ext) >= 0 ||
               String2.indexOf(File2.MOVIE_EXT, ext) >= 0 ||
               String2.indexOf(File2.COMPRESSED_SOUND_EXT, ext) >= 0;
    }



    /**
     * For newly created files, this tries a few times to wait for the file
     * to be accessible via the operating system.
     *
     * @param fullName the full name of the file
     * @param nTimes the number of times to try (e.g., 5) with 200ms sleep between tries
     * @return true if the file exists
     */
    public static boolean isFile(String fullName, int nTimes) {
        try {
            //String2.log("File2.isFile: " + fullName);
            File file = new File(fullName);
            boolean b = false;
            nTimes = Math.max(1, nTimes);

            for (int i = 0; i < nTimes; i++) {
                b = file.isFile();
                if (b) {
                    return true;
                } else if (i < nTimes - 1) {
                    Math2.sleep(200);
                }
            }
            return b;
        } catch (Exception e) {
            if (verbose) String2.log(MustBe.throwable("File2.isFile(" + fullName + ")", e));
            return false;
        }
    }

    /**
     * This indicates if the named directory is indeed an existing directory.
     *
     * @param dir the full name of the directory
     * @return true if the file exists
     */
    public static boolean isDirectory(String dir) {
        try {
            //String2.log("File2.isFile: " + dir);
            File d = new File(dir);
            return d.isDirectory();
        } catch (Exception e) {
            if (verbose) String2.log(MustBe.throwable("File2.isDirectory(" + dir + ")", e));
            return false;
        }
    }

    /**
     * This deletes the specified file or directory (must be empty).
     *
     * @param fullName the full name of the file
     * @return true if the file existed and was successfully deleted; 
     *    otherwise returns false.
     */
    public static boolean delete(String fullName) {
        //This can have problems if another thread is reading the file, so try repeatedly.
        //Unlike other places, this is often part of delete/rename, 
        //  so we want to know when it is done ASAP.
        int maxAttempts = String2.OSIsWindows? 11 : 4;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                File file = new File(fullName);
                if (!file.exists())
                    return attempt > 1; //if attempt=1, nothing to delete; if attempt>1, there was something
                //I think Linux deletes no matter what.
                //I think Windows won't delete file if in use by another thread or pending action(?).
                boolean result = file.delete();  
                file = null; //don't hang on to it
                if (result) {
                    //take it at its word (file has been or will soon be deleted)
                    //In Windows, file may continue to be isFile() for a short time.
                    return true;
                } else {
                    //2009-02-16 I had wierd problems on Windows with File2.delete not deleting when it should be able to.
                    //2011-02-18 problem comes and goes (varies from run to run), but still around.
                    //Windows often sets result=false on first attempt in some of my unit tests.
                    //Notably, when creating cwwcNDBCMet on local erddap.
                    //I note (from http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4045014)
                    //  that Win (95?) won't delete an open file, but solaris will.
                    //  But I think the files I'm working with have been closed.
                    //Solution? call Math2.gc instead of Math2.sleep
                    if (attempt == maxAttempts) {
                        String2.log(String2.ERROR + ": File2.delete was " + 
                            UNABLE_TO_DELETE + " " + fullName +
                            "\n" + MustBe.getStackTrace());
                        return result;
                    }
                    String2.log("WARNING #" + attempt + 
                        ": File2.delete is having trouble. It will try again to delete " + fullName 
                        //+ "\n" + MustBe.stackTrace()
                        );
                    if (attempt % 4 == 1)
                        Math2.gcAndWait(); //wait before retry delete. By experiment, gc works better than sleep.
                    else Math2.sleep(1000);
                }

            } catch (Exception e) {
                if (verbose) String2.log(MustBe.throwable("File2.delete(" + fullName + ")", e));
                return false;
            }
        }
        return false; //won't get here
    }

    /**
     * This just tries once to delete the file or directory (must be empty).
     *
     * @param fullName the full name of the file
     * @return true if the file existed and was successfully deleted; 
     *    otherwise returns false.
     */
    public static boolean simpleDelete(String fullName) {
        //This can have problems if another thread is reading the file, so try repeatedly.
        //Unlike other places, this is often part of delete/rename, 
        //  so we want to know when it is done ASAP.
        try {
            File file = new File(fullName);
            if (!file.exists())
                return false; //it didn't exist
            return file.delete();  
        } catch (Exception e) {
            if (verbose) String2.log(MustBe.throwable("File2.simpleDelete(" + fullName + ")", e));
            return false;
        }
    }

    /**
     * This deletes all files in the specified directory (not subdirectories).
     * If the dir isn't a directory, nothing happens.
     *
     * @param dir the full name of the directory
     * @return the return value from the underlying deleteIfOld
     */
    public static int deleteAllFiles(String dir) {
        return deleteIfOld(dir, Long.MAX_VALUE, false, false);
    }

    /**
     * This deletes all files in the specified directory.
     * See also gov.noaa.pfel.coastwatch.util.RegexFilenameFilter.recursiveDelete().
     * If the dir isn't a directory, nothing happens.
     *
     * @param dir the full name of the directory
     * @param recursive if true, subdirectories are searched, too
     * @param deleteEmptySubdirectories  this is only used if recursive is true
     * @return the return value from the underlying deleteIfOld
     */
    public static int deleteAllFiles(String dir, 
            boolean recursive, boolean deleteEmptySubdirectories) {
        return deleteIfOld(dir, Long.MAX_VALUE, recursive, deleteEmptySubdirectories);
    }

    /**
     * This deletes the files in the specified directory if the
     * last modified time is older than the specified time.
     *
     * @param dir the full name of the main directory
     * @param time System.currentTimeMillis of oldest file to be kept.
     *     Files will a smaller lastModified will be deleted.
     * @param recursive if true, subdirectories are searched, too
     * @param deleteEmptySubdirectories  this is only used if recursive is true
     * @return number of files that remain (or -1 if trouble). 
     *     This won't throw an exception if trouble.
     */
    public static int deleteIfOld(String dir, long time, 
            boolean recursive, boolean deleteEmptySubdirectories) {
        try {
            String msg = String2.ERROR + ": File2.deleteIfOld is " + UNABLE_TO_DELETE + " ";
            File file = new File(dir);

            //make sure it is an existing directory
            if (!file.isDirectory()) {
                String2.log(String2.ERROR + " in File2.deleteIfOld: dir=" + 
                    dir + " isn't a directory.");
                return -1;
            }

            //go through the files and delete old ones
            File files[] = file.listFiles();
            //String2.log(">> File2.deleteIfOld dir=" + dir + " nFiles=" + files.length);
            int nRemain = 0;
            int nDir = 0;
            for (int i = 0; i < files.length; i++) {
                //String2.log(">> File2.deleteIfOld files[" + i + "]=" + files[i].getAbsolutePath());
                try {
                    if (files[i].isFile()) {
                        if (files[i].lastModified() < time) {
                            if (!files[i].delete()) {
                                //unable to delete
                                String2.log(msg + files[i].getCanonicalPath());
                                nRemain = -1;                            
                            }
                        } else if (nRemain != -1) {   //once nRemain is -1, it isn't changed
                            nRemain++;
                        }
                    } else if (recursive && files[i].isDirectory()) {
                        nDir++;
                        int tnRemain = deleteIfOld(files[i].getAbsolutePath(), time, 
                            recursive, deleteEmptySubdirectories);
                        //String2.log(">> File2.deleteIfOld might delete this dir. tnRemain=" + tnRemain);
                        if (tnRemain == -1)
                            nRemain = -1;
                        else {
                            if (nRemain != -1)  //once nRemain is -1, it isn't changed
                                nRemain += tnRemain;
                            if (tnRemain == 0 && deleteEmptySubdirectories) {
                                files[i].delete();
                            }
                        }
                    }
                } catch (Exception e) {
                    try {
                        nRemain = -1;
                        String2.log(msg + files[i].getCanonicalPath());
                    } catch (Exception e2) {
                    }
                }
            }
            int nDeleted = files.length - nDir + nRemain;
            if (nDir != 0 || nDeleted != 0 || nRemain != 0) 
                String2.log("File2.deleteIfOld(" + 
                    String2.left(dir, 55) +
                    (time == Long.MAX_VALUE? "" : 
                            ", " + Calendar2.safeEpochSecondsToIsoStringTZ(time / 1000.0, "" + time)) +
                    ") nDir=" +    String2.right("" + nDir, 4) + 
                    " nDeleted=" + String2.right("" + nDeleted, 4) + 
                    " nRemain=" +  String2.right(nRemain < 0? String2.ERROR : "" + nRemain, 4));
            return nRemain;
        } catch (Exception e3) {
            String2.log(MustBe.throwable(String2.ERROR + " in File2.deleteIfOld(" + 
                dir + ", " + time + ")", e3));
            return -1;
        }
    }

    /**
     * This renames the specified file.
     * If the dir+newName file already exists, it will be deleted.
     *
     * @param dir the directory containing the file (with a trailing slash)
     * @param oldName the old name of the file
     * @param newName the new name of the file
     * @throws RuntimeException if trouble
     */
    public static void rename(String dir, String oldName, String newName) 
            throws RuntimeException {
        rename(dir + oldName, dir + newName);
    }

    /**
     * This renames the specified file.
     * If the fullNewName file already exists, it will be deleted before the renaming.
     * The files must be in the same directory.
     *
     * @param fullOldName the complete old name of the file
     * @param fullNewName the complete new name of the file
     * @throws RuntimeException if trouble
     */
    public static void rename(String fullOldName, String fullNewName) 
            throws RuntimeException {
        File oldFile = new File(fullOldName);
        if (!oldFile.isFile())
            throw new RuntimeException(
                "Unable to rename\n" + fullOldName + " to\n" + fullNewName + 
                "\nbecause source file doesn't exist.");

        //delete any existing file with destination name
        File newFile = new File(fullNewName);
        if (newFile.isFile()) {
            //It may try a few times.
            //Since we know file exists, !delete really means it couldn't be deleted; take result at its word.
            if (!delete(fullNewName))  
                throw new RuntimeException(
                    "Unable to rename\n" + fullOldName + " to\n" + fullNewName +
                    "\nbecause " + UNABLE_TO_DELETE + " an existing file with destinationName.");

            //In Windows, file may be isFile() for a short time. Give it time to delete.
            if (String2.OSIsWindows)
                Math2.sleep(Math2.shortSleep);  //if Windows: encourage successful file deletion
        }

        //rename
        if (oldFile.renameTo(newFile))
            return;

        //failed? give it a second try. This fixed a problem in a test on Windows.
        Math2.gcAndWait();  //wait before giving it a second try      
        if (oldFile.renameTo(newFile))
            return;
        throw new RuntimeException("Unable to rename\n" + fullOldName + " to\n" + fullNewName);
    }

    /**
     * This is like rename(), but won't throw an exception if trouble.
     *
     * @param fullOldName the complete old name of the file
     * @param fullNewName the complete new name of the file
     */
    public static void safeRename(String fullOldName, String fullNewName) {
        try {
            rename(fullOldName, fullNewName);
        } catch (Throwable t) {
        }
    }


    /**
     * This renames fullOldName to fullNewName if fullNewName doesn't exist.
     * <br>If fullNewName does exist, this just deletes fullOldName (and doesn't touch() fullNewName.
     * <br>The files must be in the same directory.
     * <br>This is used when more than one thread may be creating the same fullNewName 
     *   with same content (and where last step is rename tempFile to newName).
     *
     * @param fullOldName the complete old name of the file
     * @param fullNewName the complete new name of the file
     * @throws RuntimeException if trouble
     */
    public static void renameIfNewDoesntExist(String fullOldName, String fullNewName) 
            throws RuntimeException {
        if (isFile(fullNewName)) 
            //in these cases, fullNewName may be in use, so rename will fail because can't delete
            delete(fullOldName);  
        else rename(fullOldName, fullNewName);
    } 

    
    /**
     * If the directory or file exists, 
     * this changes its lastModification date/time to the current
     * date and time.  
     * (The name comes from the Unix "touch" program.)
     *
     * @param fullName the full name of the file
     * @return true if the directory or file exists 
     *    and if the modification was successful
     */
    public static boolean touch(String fullName) {
        return touch(fullName, 0);
    }



    /**
     * If the directory or file exists, 
     * this changes its lastModification date/time to the current
     * date and time minus millisInPast.  
     * (The name comes from the Unix "touch" program.)
     *
     * @param fullName the full name of the file
     * @param millisInPast
     * @return true if the directory or file exists 
     *    and if the modification was successful
     */
    public static boolean touch(String fullName, long millisInPast) {
        try {
            File file = new File(fullName);
            //The Java documentation for setLastModified doesn't state
            //if the method returns false if the file doesn't exist 
            //or if the method creates a 0 byte file (as does Unix's touch).
            //But tests show that it returns false if !exists, so no need to test. 
            //if (!file.exists()) return false;
            return file.setLastModified(System.currentTimeMillis() - millisInPast);
        } catch (Exception e) {
            if (verbose) String2.log(MustBe.throwable("File2.touch(" + fullName + ")", e));
            return false;
        }
    }

    /**
     * If the directory or file exists, 
     * this changes its lastModification date/time to millis  
     *
     * @param fullName the full name of the file
     * @param millis
     * @return true if the directory or file exists 
     *    and if the modification was successful
     */
    public static boolean setLastModified(String fullName, long millis) {
        try {
            File file = new File(fullName);
            //The Java documentation for setLastModified doesn't state
            //if the method returns false if the file doesn't exist 
            //or if the method creates a 0 byte file (as does Unix's touch).
            //But tests show that it returns false if !exists, so no need to test. 
            //if (!file.exists()) return false;
            return file.setLastModified(millis);
        } catch (Exception e) {
            if (verbose) String2.log(MustBe.throwable("File2.setLastModified(" + fullName + ")", e));
            return false;
        }
    }

    /**
     * This returns the length of the named file (or -1 if trouble).
     *
     * @param fullName the full name of the file
     * @return the length of the named file (or -1 if trouble).
     */
    public static long length(String fullName) {
        try {
            //String2.log("File2.isFile: " + fullName);
            File file = new File(fullName);
            if (!file.isFile()) return -1;
            return file.length();
        } catch (Exception e) {
            if (verbose) String2.log(MustBe.throwable("File2.length(" + fullName + ")", e));
            return -1;
        }
    }

    /**
     * Get the number of millis since the start of the Unix epoch
     * when the file was last modified.
     *
     * @param fullName the full name of the file
     * @return the time (millis since the start of the Unix epoch) 
     *    the file was last modified 
     *    (or 0 if trouble)
     */
    public static long getLastModified(String fullName) {
        try {
            File file = new File(fullName);
            return file.lastModified();
        } catch (Exception e) {
            //pause and try again
            try {
                Math2.gcAndWait(); //if trouble getting lastModified: gc encourages success
                File file = new File(fullName);
                return file.lastModified();
            } catch (Exception e2) {
                if (verbose) String2.log(MustBe.throwable("File2.getLastModified(" + fullName + ")", e2));
                return 0;
            }
        }
    }

    /** This returns the name of the oldest file in the list. 
     * Error if fullNames.length == 0.
     */
    public static String getOldest(String fullNames[]) {
        int ti = 0;
        long tm = getLastModified(fullNames[0]);
        for (int i = 1; i < fullNames.length; i++) {
            long ttm = getLastModified(fullNames[i]);
            if (ttm != 0 && ttm < tm) {
                ti = i;
                tm = ttm;
            }
        }
        return fullNames[ti];
    }

    /** This returns the name of the youngest file in the list. 
     * Error if fullNames.length == 0.
     */
    public static String getYoungest(String fullNames[]) {
        int ti = 0;
        long tm = getLastModified(fullNames[0]);
        for (int i = 1; i < fullNames.length; i++) {
            long ttm = getLastModified(fullNames[i]);
            if (ttm != 0 && ttm > tm) {
                ti = i;
                tm = ttm;
            }
        }
        return fullNames[ti];
    }

    /**
     * This returns the protocol+domain (without a trailing slash) from a URL.
     *
     * @param url a full or partial URL.
     * @return the protocol+domain (without a trailing slash).
     *    If no protocol in URL, that's okay.
     *    null returns null.  "" returns "".
     */
    public static String getProtocolDomain(String url) {
        if (url == null)
            return url;

        int urlLength = url.length();
        int po = url.indexOf('/');
        if (po < 0)
            return url;
        if (po == urlLength - 1 || po > 7) 
            //protocol names are short e.g., http:// .  Perhaps it's coastwatch.pfeg.noaa.gov/
            return url.substring(0, po);
        if (url.charAt(po + 1) == '/') { 
            if (po == urlLength - 2)
                //e.g., http://
                return url;
            //e.g., http://a.com/
            po = url.indexOf('/', po + 2);
            return po >= 0? url.substring(0, po) : url;
        } else {
            //e.g., www.a.com/...
            return url.substring(0, po);            
        }
    }

    /**
     * This returns the directory info (with a trailing slash) from the fullName).
     *
     * @param fullName the full name of the file.
     *   It can have forward or backslashes.
     * @return the directory (or "" if none; 2020-01-10 was currentDirectory)
     */
    public static String getDirectory(String fullName) {
        int po = fullName.lastIndexOf('/');
        if (po < 0)
            po = fullName.lastIndexOf('\\');
        return po > 0? fullName.substring(0, po + 1) : "";
    }

    /**
     * This returns the current directory (with the proper separator at
     *   the end).
     *
     * @return the current directory (with the proper separator at
     *   the end)
     */
    public static String getCurrentDirectory() {
        String dir = System.getProperty("user.dir");

        if (!dir.endsWith(File.separator))
            dir += File.separator;

        return dir;
    }

    /**
     * This removes the directory info (if any) from the fullName,
     * and so returns just the name and extension.
     *
     * @param fullName the full name of the file.
     *   It can have forward or backslashes.
     * @return the name and extension of the file  (may be "")
     */
    public static String getNameAndExtension(String fullName) {
        int po = fullName.lastIndexOf('/');
        if (po >= 0)
            return fullName.substring(po + 1);

        po = fullName.lastIndexOf('\\');
        return po >= 0? fullName.substring(po + 1) : fullName;
    }

    /**
     * This returns just the extension from the file's name 
     * (the last "." and anything after, e.g., ".asc").
     *
     * @param fullName the full name or just name of the file.
     *   It can have forward or backslashes.
     * @return the extension of the file (perhaps "")
     */
    public static String getExtension(String fullName) {
        String name = getNameAndExtension(fullName);
        int po = name.lastIndexOf('.');
        return po >= 0? name.substring(po) : "";
    }

    /**
     * This removes the extension (if there is one) from the file's name
     * (the last "." and anything after, e.g., ".asc").
     *
     * @param fullName the full name or just name of the file.
     *   It can have forward or backslashes.
     * @return the extension of the file (perhaps "")
     */
    public static String removeExtension(String fullName) {
        String ext = getExtension(fullName);
        return fullName.substring(0, fullName.length() - ext.length());
    }

    /**
     * This replaces the existing extension (if any) with ext.
     *
     * @param fullName the full name or just name of the file.
     *   It can have forward or backslashes.
     * @param ext the new extension (e.g., ".das")
     * @return the fullName with the new ext
     */
    public static String forceExtension(String fullName, String ext) {
        String oldExt = getExtension(fullName);
        return fullName.substring(0, fullName.length() - oldExt.length()) + ext;
    }

    /**
     * This removes the directory info (if any) and extension (after the last ".", if any) 
     * from the fullName, and so returns just the name.
     *
     * @param fullName the full name of the file.
     *   It can have forward or backslashes.
     * @return the name of the file
     */
    public static String getNameNoExtension(String fullName) {
        String name = getNameAndExtension(fullName);
        String extension = getExtension(fullName);
        return name.substring(0, name.length() - extension.length());
    }


    /**
     * This returns true if the file is compressed and decompressible via
     * getDecompressedInputStream.
     *
     * @param ext The file's extension, e.g., .gz
     */
    public static boolean isDecompressible(String ext) {

        //this exactly parallels getDecompressedInputStream
        return 
            ext.equals(".Z") ||
            (ext.indexOf('z') >=0 && 
                (ext.equals(".tgz") || 
                 ext.equals(".gz")  ||   //includes .tar.gz
                 ext.equals(".gzip") ||  //includes .tar.gzip
                 ext.equals(".zip") || 
                 ext.equals(".bz2")));
    }



    /**
     * This gets a decompressed, buffered InputStream from a file. 
     * If the file is compressed, it is assumed to be the only file (entry) in the archive.
     * 
     * @param fullFileName The full file name. If it ends in
     *   .tgz, .tar.gz, .tar.gzip, .gz, .gzip, .zip, or .bz2,
     *   this returns a decompressed, buffered InputStream.
     * @return a decompressed, buffered InputStream from a file. 
     */
    public static InputStream getDecompressedBufferedInputStream(String fullFileName) throws Exception {
        String ext = getExtension(fullFileName); //if e.g., .tar.gz, this returns .gz

        InputStream is = new BufferedInputStream( //recommended by https://commons.apache.org/proper/commons-compress/examples.html
            new FileInputStream(fullFileName));  //1MB buffer makes no difference

        // !!!!! IF CHANGE SUPPORTED COMPRESSION TYPES, CHANGE isDecompressible ABOVE !!!

        //handle .Z (capital Z) specially first
        if (ext.equals(".Z")) {
            try {
                return new ZCompressorInputStream(is);
            } catch (Exception e) {
                is.close();
                throw e;
            }
        }

        //everything caught below has a z in ext
        if (ext.indexOf('z') < 0) 
            return is;

        if (ext.equals(".tgz") || 
            fullFileName.endsWith(".tar.gz") || 
            fullFileName.endsWith(".tar.gzip")) {
            //modified from https://stackoverflow.com/questions/7128171/how-to-compress-decompress-tar-gz-files-in-java
            GzipCompressorInputStream gzipIn = null;
            TarArchiveInputStream tarIn = null;
            try {
                gzipIn = new GzipCompressorInputStream(is);
                tarIn = new TarArchiveInputStream(gzipIn);
                TarArchiveEntry entry = tarIn.getNextTarEntry();
                while (entry != null && entry.isDirectory()) 
                    entry = tarIn.getNextTarEntry();
                if (entry == null) 
                    throw new IOException(String2.ERROR + " while reading " + fullFileName + 
                        ": no file found in archive.");
                is = tarIn;
            } catch (Exception e) {
                if      (tarIn  != null) tarIn.close();
                else if (gzipIn != null) gzipIn.close();
                else is.close();
                throw e;
            }

        } else if (ext.equals(".gz") || ext.equals(".gzip")) {
            try {
                is = new GzipCompressorInputStream(is);
            } catch (Exception e) {
                is.close();
                throw e;
            }

        } else if (ext.equals(".zip")) {
            ZipInputStream zis = null;
            try {
                zis = new ZipInputStream(is);
                ZipEntry entry = zis.getNextEntry();
                while (entry != null && entry.isDirectory()) 
                    entry = zis.getNextEntry();
                if (entry == null) 
                    throw new IOException(String2.ERROR + " while reading " + fullFileName + 
                        ": no file found in archive.");
                is = zis;
            } catch (Exception e) {
                if (zis != null) zis.close();
                else is.close();
                throw e;
            }

        } else if (ext.equals(".bz2")) {
            try {
                is = new BZip2CompressorInputStream(is);
            } catch (Exception e) {
                is.close();
                throw e;
            }
        }
        //.7z is possible but different and harder

        // !!!!! IF CHANGE SUPPORTED COMPRESSION TYPES, CHANGE isDecompressible ABOVE !!!

        return is;
    }

    /** 
     * This creates a buffered, decompressed (e.g., from .gz file) FileReader. 
     *
     * @param fullFileName the full file name
     * @return a buffered FileReader
     * @throws Exception if trouble  
     */
    public static BufferedReader getDecompressedBufferedFileReader(String fullFileName, 
        String charset) throws Exception {

        /* new method (no evidence it is better) */
        if (isDecompressible(getExtension(fullFileName))) { //if e.g., .tar.gz, this returns .gz
            InputStream is = getDecompressedBufferedInputStream(fullFileName);
            try {
                return new BufferedReader(new InputStreamReader(is,  
                    String2.isSomething(charset)? charset : String2.ISO_8859_1)); //invalid charset throws exception
            } catch (Exception e) {
                try {if (is != null) is.close();} catch (Exception e2) {}
                throw e;
            }
        } else {
            return Files.newBufferedReader(FileSystems.getDefault().getPath(fullFileName), 
                String2.isSomething(charset)? Charset.forName(charset) : String2.ISO_8859_1_CHARSET);
        }
        /* */

        /* old method 
        InputStream is = getDecompressedBufferedInputStream(fullFileName);
        try {
            return new BufferedReader(new InputStreamReader(is,  
                String2.isSomething(charset)? charset : String2.ISO_8859_1)); //invalid charset throws exception
        } catch (Exception e) {
            try {if (is != null) is.close();} catch (Exception e2) {}
            throw e;
        }
        /* */
    }

    /**
     * This generates a hex dump of the first nBytes of the file.
     * 
     * @param fullFileName
     * @param nBytes 
     * @return a String with a hex dump of the first nBytes of the file.
     * @throws Exception if trouble
     */
    public static String hexDump(String fullFileName, int nBytes) throws Exception {
        InputStream fis = getDecompressedBufferedInputStream(fullFileName);
        try {
            nBytes = Math.min(nBytes, Math2.narrowToInt(length(fullFileName))); //max 2GB
            byte ba[] = new byte[nBytes];
            int bytesRead = 0;
            while (bytesRead < nBytes)
                bytesRead += fis.read(ba, bytesRead, nBytes - bytesRead);
            return String2.hexDump(ba);
        } finally {
            fis.close();
        }
    }

    /**
     * This returns the byte# at which the two files are different
     * (or -1 if same).
     *
     * @param fullFileName1
     * @param fullFileName2
     * @return byte# at which the two files are different (or -1 if same).
     */
    public static long whereDifferent(String fullFileName1, String fullFileName2) {

        long length1 = length(fullFileName1);
        long length2 = length(fullFileName2);
        long length = Math.min(length1, length2);
        InputStream bis1 = null, bis2 = null;
        long po = 0; 
        try {
            bis1 = File2.getDecompressedBufferedInputStream(fullFileName1);
            bis2 = File2.getDecompressedBufferedInputStream(fullFileName2);
            for (po = 0; po < length; po++) {
                if (bis1.read() != bis2.read())
                    break;
            }
        } catch (Exception e) {
            String2.log(String2.ERROR + " in whereDifferent(\n1:" + fullFileName1 + 
                "\n2:" + fullFileName2 + 
                "\n" + MustBe.throwableToString(e));
        }
        try {if (bis1 != null) bis1.close(); } catch (Exception e) {}
        try {if (bis2 != null) bis2.close(); } catch (Exception e) {}

        if (po < length) return po;
        if (length1 != length2) return length;
        return -1;
    }

    /**
     * This makes a directory (and any necessary parent directories) (if it doesn't already exist).
     *
     * @param name
     * @throws RuntimeException if unable to comply.
     */
    public static void makeDirectory(String name) throws RuntimeException {
        File dir = new File(name);
        if (dir.isFile()) {
            throw new RuntimeException("Unable to make directory=" + name + ". There is a file by that name!");
        } else if (!dir.isDirectory()) {
            if (!dir.mkdirs())
                throw new RuntimeException("Unable to make directory=" + name + ".");
        }
    }
    
    /** A variant that copies the entire source. */
    public static boolean copy(String source, String destination) {
        return copy(source, destination, 0, -1);
    }

    /**
     * This makes a copy of a file.
     * !!!If the source might be remote, use SSR.downloadFile(source, dest, false) instead.
     *
     * @param source the full file name of the source file.
     *   If compressed, this doesn't decompress!
     * @param destination the full file name of the destination file.
     *   If the directory doesn't exist, it will be created.
     *   It is closed at the end.
     * @param first the first byte to be transferred (0..)
     * @param last  the last byte to be transferred (inclusive),
     *    or -1 to transfer to the end.
     * @return true if successful. If not successful, the destination file
     *   won't exist.
     */
    public static boolean copy(String source, String destination, long first, long last) {

        if (source.equals(destination)) return false;
        OutputStream out = null;
        boolean success = false;
        try {
            File dir = new File(getDirectory(destination));
            if (!dir.isDirectory())
                 dir.mkdirs();
            out = new BufferedOutputStream(new FileOutputStream(destination));
            success = copy(source, out, first, last);
        } catch (Exception e) {
            String2.log(String2.ERROR + " in File2.copy source=" + source + "\n" + 
                e.toString());
        }
        try { 
            if (out != null) out.close();
        } catch (Exception e) {}

        if (!success) 
            delete(destination);

        return success;
    }


    /** A variant that copies the entire source. */
    public static boolean copy(String source, OutputStream out) {
        return copy(source, out, 0, -1);
    }

    /**
     * This is like copy(), but decompresses if the source is compressed
     *
     * @param source the full file name of the source file.
     *   If compressed, this does decompress!
     * @param destination the full file name of the destination file.
     *   If the directory doesn't exist, it will be created.
     *   It is closed at the end.
     * @return true if successful. If not successful, the destination file
     *   won't exist.
     */
    public static boolean decompress(String source, String destination) {

        if (source.equals(destination)) return false;
        InputStream in = null;
        OutputStream out = null;
        boolean success = false;
        try {
            File dir = new File(getDirectory(destination));
            if (!dir.isDirectory())
                 dir.mkdirs();
            in = getDecompressedBufferedInputStream(source);
            out = new BufferedOutputStream(new FileOutputStream(destination));
            success = copy(in, out, 0, -1);
        } catch (Exception e) {
            String2.log(String2.ERROR + " in File2.copy source=" + source + "\n" + 
                e.toString());
        }
        try { 
            if (in != null) in.close();
        } catch (Exception e) {}
        try { 
            if (out != null) out.close();
        } catch (Exception e) {}

        if (!success) 
            delete(destination);

        return success;
    }

    /**
     * This makes a copy of a file to an outputStream.
     *
     * @param source the full file name of the source. 
     *   If compressed, this doesn't decompress!
     * @param out Best if buffered. It is flushed, but not closed, at the end
     * @param first the first byte to be transferred (0..)
     * @param last  the last byte to be transferred (inclusive),
     *    or -1 to transfer to the end.
     * @return true if successful. 
     */
    public static boolean copy(String source, OutputStream out, long first, long last) {

        InputStream in = null;
        try {
            File file = new File(source);            
            if (!file.isFile()) {
                String2.log(String2.ERROR + " in File2.copy: source=" + source +
                    " doesn't exist.");
                return false;
            }
            if (last < 0) {
                last = file.length() - 1;
                if (reallyVerbose)
                    String2.log("  File2.copy(first=" + first + ", last was=-1 now=" + last + ")");
            }
            in = new BufferedInputStream(new FileInputStream(file)); //File2.getDecompressedBufferedInputStream(). Read file as is.
            return copy(in, out, first, last);
        } catch (Exception e) {
            String2.log(MustBe.throwable(String2.ERROR + " in File2.copy.", e));
            return false;
        } finally {
            try {
                if (in != null) 
                    in.close(); 
            } catch (Exception e2) {}
        }
    }


    /** A variant that copies the entire source. */
    public static boolean copy(InputStream in, OutputStream out) {
        return copy(in, out, 0, -1);
    }

    /**
     * This copies a range from an inputStream to an outputStream.
     *
     * @param in   Best if buffered. At the end, this is NOT closed.
     * @param out  Best if buffered. It is flushed, but not closed, at the end
     * @param first the first byte to be transferred (0..)
     * @param last  the last byte to be transferred (inclusive),
     *    or -1 to transfer to the end.
     * @return true if successful. 
     */
    public static boolean copy(InputStream in, OutputStream out, long first, long last) {

        int bufferSize = 32768;
        byte buffer[] = new byte[bufferSize];
        long remain = last < 0? Long.MAX_VALUE : 1 + last - first;
        try {
            skipFully(in, first);
            int nRead;
            while ((nRead = in.read(buffer, 0, (int)Math.min(remain, bufferSize))) >= 0) {  //0 shouldn't happen. -1=end of file
                out.write(buffer, 0, nRead);
                remain -= nRead;
                //String2.log(">> nRead=" + nRead + " remain=" + remain);
                if (remain == 0) 
                    break;
            }
            out.flush(); //always flush
            if (last >= 0 && nRead == -1)
                throw new IOException("Unexpected end-of-file.");
            //String2.log(">> found eof");
            return true;
        } catch (Exception e) {
            String2.log(MustBe.throwable(String2.ERROR + " in File2.copy at first=" + 
                first + ", last=" + last + ", remain=" + remain + ".", e));
            return false;
        }
    }

    /**
     * This fully skips the specified number of bytes from the inputstream
     * (unlike InputStream.skip, which may not skip all of the bytes).
     *
     * @param inputStream Best if buffered.
     * @param nToSkip the number of bytes to be read
     * @throws IOException if trouble
     */
    public static void skipFully(InputStream inputStream, long nToSkip) throws IOException {

        long remain = nToSkip;
        int zeroCount = 0; //consecutive 
        while (remain > 0) {
            long skipped = inputStream.skip(remain); //may not skip all requested
            if (skipped == 0) {
                if (++zeroCount == 3)
                    throw new IOException("Unable to skip within the inputStream.");
            } else {
                zeroCount = 0;
                remain -= skipped;
            }
        }
    }

    /**
     * This reads the specified number of bytes from the inputstream
     * (unlike InputStream.read, which may not read all of the bytes).
     *
     * @param inputStream Best if buffered.
     * @param byteArray
     * @param offset the first position of byteArray to be written to
     * @param length the number of bytes to be read
     * @throws Exception if trouble
     */
    public static void readFully(InputStream inputStream, byte[] byteArray,
        int offset, int length) throws Exception {

        int po = offset;
        int remain = length;
        while (remain > 0) {
            int read = inputStream.read(byteArray, po, remain);
            po += read;
            remain -= read;
        }
    }

    /**
     * This creates, reads, and returns a byte array of the specified length.
     *
     * @param inputStream Best if buffered.
     * @param length the number of bytes to be read
     * @throws Exception if trouble
     */
    public static byte[] readFully(InputStream inputStream, int length) throws Exception {

        byte[] byteArray = new byte[length];
        readFully(inputStream, byteArray, 0, length);
        return byteArray;
    }

    /**
     * This returns a temporary directory 
     * (with forward slashes and a trailing slash, 
     * e.g., c:/Users/Bob.Simons/AppData/Local/Temp/).
     *
     * @return the temporary directory 
     * @throws Exception if trouble
     */
    public static String getSystemTempDirectory() throws Exception {
        if (tempDirectory == null) {
            File file = File.createTempFile("File2.getSystemTempDirectory", ".tmp");
            tempDirectory = file.getCanonicalPath();
            tempDirectory = String2.replaceAll(tempDirectory, "\\", "/");
            //String2.log("tempDir=" + tempDirectory);
            int po = tempDirectory.lastIndexOf('/');
            tempDirectory = tempDirectory.substring(0, po + 1);
            file.delete();
        }

        return tempDirectory;
    }

    /**
     * This converts (if not already done) tDir to use trailing / and with / separator.
     *
     * @param tDir The directory, with or without trailing / .  
     *    With / or \\ .
     * @return The directory with trailing / and with / separator.
     */
    public static String forwardSlashDir(String tDir) {
        StringBuilder sb = new StringBuilder(tDir);
        String2.replaceAll(sb, '\\', '/');
        if (sb.length() == 0 || sb.charAt(0) != '/' )
            sb.append('/');
        return sb.toString();
    }



    /**
     * This adds a slash (matching the other slashes in the dir) 
     * to the end of the dir (if one isn't there already).
     *
     * @param dir with or without a slash at the end
     * @return dir with a slash (matching the other slashes) at the end
     */
    public static String addSlash(String dir) {
        if ("\\/".indexOf(dir.charAt(dir.length() - 1)) >= 0)
            return dir;
        int po = dir.indexOf('\\');
        if (po < 0) 
            po = dir.indexOf('/');
        char slash = po < 0? '/' : dir.charAt(po);
        return dir + slash;
    }

}

