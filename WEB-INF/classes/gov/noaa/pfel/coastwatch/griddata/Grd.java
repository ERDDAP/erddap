/* 
 * Grd Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.util.File2;
import com.cohort.util.Image2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ImageProducer;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

//import javax.imageio.ImageIO;


//directy use hdf libraries:
//import ncsa.hdf.object.*;     // the common object package
//import ncsa.hdf.object.h4.*;  // the HDF4 implementation

/**
 * THIS CODE WORKS BUT IS CURRENTLY NOT USED BECAUSE IT ONLY RUNS ON LINUX
 * AND RELIES ON GMT COMMANDS. SEE GRID.JAVA.
 * This has static methods related to .grd files.
 * @author Bob Simons (bob.simons@noaa.gov) 2005-02-10
 *
 */
public class Grd {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;


    private static java.util.Random lockForSubsampleGrdFile = new java.util.Random();

    /**
     * This uses the Linux programs in GMT to generate a new subsampled .grd file
     * from a larger .grd file.
     * 
     * <P>This requires access to the programs/script grdcut.
     *
     * @param workDir the directory to be used for temporary and result files
     *    (with '/' at end)
     * @param inName the name of the .grd data file (without the .grd extension)
     *    (in the workDir).
     *     It may span a larger area than the desired area, but not smaller.
     * @param outName the name of the desired file (without the .grd extension).
     *     Perhaps the first part of inName plus different
     *     region info ("_x" + minX + "_X" + maxX + "_y" + minY + "_Y" + maxX).
     * @param minX the desired min lon value
     * @param maxX the desired max lon value
     * @param minY the desired min lat value
     * @param maxY the desired max lat value
     * @throws Exception
     */
    public static void subsampleGrdFile(String workDir, String inName, 
        String outName, 
        double minX, double maxX, double minY, double maxY) throws Exception {

        //I'm not sure that GMT is thread safe
        //so be sure only one thread is doing this at once.
        synchronized (lockForSubsampleGrdFile) {
            String fullInNameGrd = workDir + inName + ".grd";
            String fullOutName   = workDir + outName;

            //sabsample inName
            if (File2.touch(fullOutName + ".grd")) {
                String2.log("subsampleGrdFile reusing " + fullOutName + ".grd");
                return;
            } else {
                long time = System.currentTimeMillis();

                //POLICY: because this procedure may be used in more than one thread,
                //do work on unique temp files names using randomInt, then rename to proper file name.
                //If procedure fails half way through, there won't be a half-finished file.
                int randomInt = Math2.random(Integer.MAX_VALUE);

                SSR.cShell("grdcut " + fullInNameGrd + " -G" + fullOutName + randomInt + ".grd" +
                    " -R" + minX + "/" + maxX + "/" + minY + "/" + maxY);
                File2.rename(workDir, outName + randomInt + ".grd", outName + ".grd");

                String2.log( 
                    //String2.toCSSVString(cShell("grdinfo " + workDir + outName + ".grd -C").toArray()) + "\n" + //dianostic 
                    "subsampleGrdFile " + outName + " time=" + 
                    (System.currentTimeMillis() - time) + " ms");
            }
        }
    }

    private static java.util.Random lockForMakeDataFile = new java.util.Random();
    public static final int MakeGrd = 0;
    public static final int MakeAsc = 1;
    public static final int MakeHdf = 2;
    public static final int MakeMat = 3;
    public static final int MakeXyz = 4;
    public static final int MakeGeoTIFF = 5;
    public static final String MakeExtensions[] = {
        ".grd", ".asc", ".hdf", ".mat", ".xyz", ".tif"};

    /* //for using hdf libraries directly
    private static H4Datatype hdfCharType = new H4Datatype(
        Datatype.CLASS_CHAR, 1, Datatype.NATIVE, Datatype.NATIVE);
    private static H4Datatype hdfIntType = new H4Datatype(
        Datatype.CLASS_INTEGER, 4, Datatype.NATIVE, Datatype.NATIVE);
    private static H4Datatype hdfFloatType = new H4Datatype(
        Datatype.CLASS_FLOAT, 4, Datatype.NATIVE, Datatype.NATIVE);
    private static H4Datatype hdfDoubleType = new H4Datatype(
        Datatype.CLASS_FLOAT, 8, Datatype.NATIVE, Datatype.NATIVE);
    */

    /**
     * This uses the Linux programs in GMT to convert a .grd file to another 
     * file type.
     * 
     * <P>This requires access to the programs/scripts:
     *    grd2asc, grd2xyz, zip, and hdfView.
     * HdfView must be further installed so that Java can find the
     * Java libraries (put jhdf4obj.jar jhdf.jar, jhdfobj.jar, jhdf5obj.jar,
     * and jhdf5.jar in the class path for the Java compiler and Java) and
     * so the Java libraries can find the hdf binary libraries
     * (add to Windows path "C:\programs\ncsa\hdfview2.2\lib\win\;" 
     * or add to Linux path "/usr/local/hdfview/lib/linux", but
     * adjust as needed for the actual file locations).
     * See http://hdf.ncsa.uiuc.edu/hdf-java-html/hdf-object/use.html#libpath .
     *
     * <p>The first few lines of one of Dave's .asc files are
     * (my comments added after '#'):
     * <pre>
     * ncols 1761
     * nrows 1601
     * xllcenter 225           # min lon degrees; Dave had xllcorner, 3/07 I changed to xllcenter
     * yllcenter 30            # min lat degrees
     * cellsize 0.0125         # degrees, applies to lat and lon
     * nodata_value 99999      # this procedure uses "NaN" for the nodata_value
     * 99999                   # one value per line
     * 99999
     * </pre>
     * 
     * <p>The first few lines of one of Dave's .xyz files are:
     * <pre>
     * 225<tab>50<tab>NaN
     * 225.012<tab>50<tab>NaN
     * 225.025<tab>50<tab>NaN
     * 225.037<tab>50<tab>NaN
     * 225.05<tab>50<tab>NaN
     * </pre>
     *
     * @param grdDir the directory with the source .grd file
     *    (with '/' at end)
     * @param resultDir the directory to be used for temporary and result files
     *    (with '/' at end)
     * @param name the name 
     *    of the .grd data file (without the .grd extension, in the grdDir)
     *    and the name for the new file(s) (in the resultDir).
     * @param outType is one of the MakeXxx constants
     * @param varName is the name of the data variable (for use in the output file)
     * @param makeOutType create the outType file (vs. delete it)
     * @param makeZip     create the outType.zip file (vs. don't create it)
     * @throws Exception
     */
    public static void makeDataFile(String grdDir, String resultDir, String name, 
        int outType, String varName, boolean makeOutType, boolean makeZip)

        //Attribute hdfGlobalAttributes[], Attribute hdfDataAttributes[], //for hdf libraries direct
        //Attribute hdfLonAttributes[], Attribute hdfLatAttributes[]) 
        throws Exception {
//     * @param hdfGlobalAttributes an array of global Attributes for the HDF file
//     * @param hdfDataAttributes an array of data hdf Attributes for the HDF file
//     * @param hdfLonAttributes an array of lon hdf Attributes for the HDF file
//     * @param hdfLatAttributes an array of lat hdf Attributes for the HDF file

        //future: rewrite grd2xyz in Java to eliminate extra file reads
        //but they seem very efficient, so speed gains may be offset by Java speed losses

        //Dave's scripts are not thread safe (they often use one fixed temp file name)
        //so be sure only one thread is doing this at once.
        synchronized (lockForMakeDataFile) {
            String fullGrdName    = grdDir + name + ".grd";
            String fullResultName = resultDir + name; //but without extension
            String ext = MakeExtensions[outType];
            String2.log("makeDataFile for " + name + " " +
                (makeOutType? ext : "") + " " + 
                (makeZip? ext + ".zip" : ""));

            //do the desired files already exist?
            if ((!makeOutType || File2.touch(fullResultName + ext)) &&
                (!makeZip     || File2.touch(fullResultName + ext + ".zip"))) {
                String2.log("  reuse existing file(s)");
                return;
            }

            //POLICY: because this procedure may be used in more than one thread,
            //do work on unique temp files names using randomInt, then rename to proper file name.
            //If procedure fails half way through, there won't be a half-finished file.
            int randomInt = Math2.random(Integer.MAX_VALUE);

            //always make outType file (if it doesn't already exist)
            if (File2.touch(fullResultName + ext)) { 
                String2.log("  reusing " + fullResultName + ext);
            } else {
                long time = System.currentTimeMillis();

                //no option to make .grd from .grd; it would have the same name

                //MakeAsc (with missingValue=99999)  (and MakeMat makes .asc with missingValue=NaN)
                if (outType == MakeAsc || outType == MakeMat) {

                    //using GDAL
                    //cShell("gdal_translate -ot Float32 -of AAIGrid " + fullGrdName + " " + fullResultName + randomInt + ".asc");

                    //This replaces a perl script: /u00/chump/grd2ascii.prl .
                    //which generates a very specific file format (including, no "NaN"'s).
                    //I want to reduce dependencies on outside programs.
                    //This was the only use of Perl. So it is good to replace it.
                    //Its documentation says it generates:
                    //# an ArcInfo format ASCII grid that can be imported to ArcView 3.x
                    //# using Spatial Analysts "Import Data Source" -> "ASCII Raster"
                    //# OR
                    //# imported using ArcInfo 8.x ArcToolBox "Import to Raster" ->
                    //# "ASCII to Grid" Command.
                    //# WARNING:  For some reason the ESRI tools don't like certain characters
                    //# in the imported file name, i.e. "."
                    //That script generated lon values of 0...360.
                    //But that was troublesome for ArcView users, so I made it +-180 (Dave agreed).

                    //grdInfo creates tab-separated info string
                    //field: 0    1 2 3 4 5  6  7  8  9  10
                    //info:  name w e s n z0 z1 dx dy nx ny 
                    ArrayList al = SSR.cShell("grdinfo " + fullGrdName + " -C"); 
                    String[] info = String2.split((String)al.get(0), '\t'); 
                    //String2.log("grdinfo = " + String2.toCSSVString(info));

                    //write the header and the data
                    String error = String2.writeToFile(fullResultName + randomInt + ".asc", 
                        "ncols " + info[9] + "\n" +
                        "nrows " + info[10] + "\n" +
                        "xllcenter " + Math2.anglePM180(String2.parseDouble(info[1])) + "\n" +
                        "yllcenter " + info[3] + "\n" +
                        "cellsize " + info[7] + "\n" +
                        "nodata_value NaN\n");
                    if (error.length() > 0)
                        throw new Exception(String2.ERROR + ":\n" + error);
                    SSR.cShell("grd2xyz " + fullGrdName + " -Z" + 
                        (outType == MakeAsc? 'T' : 'B') + //Top vs Bottom
                        "La >> " + fullResultName + randomInt + ".asc"); 

                    //convert all "NaN" to "99999"
                    //and rename to <name>.asc
                    if (outType == MakeAsc) {
                        String2.simpleSearchAndReplace(
                            fullResultName + randomInt + ".asc",
                            fullResultName + randomInt + "b.asc",
                            "NaN",
                            "99999");
                        File2.rename(resultDir, name + randomInt + "b.asc", name + ".asc");
                    } else                         
                        File2.rename(resultDir, name + randomInt + ".asc", name + ".asc");

                    File2.delete(resultDir + name + randomInt + ".asc"); 

                    //diagnostic
                    if (true) {
                        String[] rff = String2.readFromFile(fullResultName + ".asc");
                        if (rff[0].length() > 0)
                            throw new Exception(String2.ERROR + ":\n" + rff[0]);
                        String2.log(fullResultName + ".asc contains:\n" +
                            String2.annotatedString(
                            rff[1].substring(0, Math.min(rff[1].length(), 200))));
                    }
                }

                //MakeGeoTIFF
                if (outType == MakeGeoTIFF) {
                    //make the temp tiff file   (gdal is part of FWTools)
                    SSR.cShell("gdal_translate -ot Float32 -of GTiff " + fullGrdName + " " + fullResultName + randomInt + ".tif");

                    //and rename to <name>.tif
                    File2.rename(resultDir, name + randomInt + ".tif", name + ".tif");
                }

                //MakeGrd
                if (outType == MakeGrd) {
                    //if different directories, copy the .grd file to the result directory
                    if (!fullGrdName.equals(fullResultName + ".grd"))
                        SSR.cShell("cp " + fullGrdName + " " + fullResultName + ".grd"); 
                }

                //MakeHdf via HDF libraries (this relies on MakeAsc above making .asc with missingValue=NaN)
                //INACTIVE: this throws errors related to inability to link native HDF libraries.
                //See alternative below.
                /*
                if (outType == MakeHdf) {
                    //Old way relies on script which calls Matlab.
                    //This relies on a proprietary program, so good to remove it.
                    //cShell("/u00/chump/grdtohdf " + fullGrdName + " " + 
                    //    fullResultName + randomInt + ".hdf " + varName); 
                    //File2.rename(resultDir, name + randomInt + ".hdf", name + ".hdf");


                    //read the data from the ASCII file
                    BufferedReader reader = new BufferedReader(new FileReader(
                        resultDir + name + ".asc"));
                    String s;
                    s = reader.readLine(); int nCols = String2.parseInt(s.substring(s.indexOf(' ') + 1));
                    s = reader.readLine(); int nRows = String2.parseInt(s.substring(s.indexOf(' ') + 1));
                    s = reader.readLine(); double lon0 = String2.parseDouble(s.substring(s.indexOf(' ') + 1));
                    s = reader.readLine(); double lat0 = String2.parseDouble(s.substring(s.indexOf(' ') + 1));
                    s = reader.readLine(); double incr = String2.parseDouble(s.substring(s.indexOf(' ') + 1));
                    s = reader.readLine(); String mv = s.substring(s.indexOf(' ') + 1);
                    //String2.log("grdinfo = " + String2.toCSSVString(info));

                    //retrieve an instance of an H4File
                    FileFormat fileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4);
                    Test.ensureNotNull(fileFormat, "Cannot find HDF4 FileFormat.");

                    //create a new file with the temp file name
                    String tFileName = resultDir + name + randomInt + ".hdf";
                    H4File hdfFile = null;
                    try {
                        //if this fails, the libraries need to be loaded
                        hdfFile = (H4File)fileFormat.create(tFileName);

                    } catch (Exception e) {
                        //ensure native library is loaded (but try/catch ensures it isn't done a second time)
                        System.load("/usr/local/hdfview/lib/linux/libjhdf5.so");
                        System.load("/usr/local/hdfview/lib/linux/libjhdf.so");
                        //String libPath = fullContextDirectory + "WEB-INF/lib/";
                        //System.load(libPath + "libjhdf5.so"); 
                        //System.load(libPath + "libjhdf.so");

                        //then try it again
                        hdfFile = (H4File)fileFormat.create(tFileName);
                    }
                    Test.ensureNotNull(hdfFile, "Failed to create file: " + tFileName);

                    //open the file and retrieve the root group
                    hdfFile.open();
                    Group root = (Group)((javax.swing.tree.DefaultMutableTreeNode)
                        hdfFile.getRootNode()).getUserObject();

                    //write the global metadata
                    if (hdfGlobalAttributes != null)
                        for (int i = 0; i < hdfGlobalAttributes.length; i++)
                            root.writeMetadata(hdfGlobalAttributes[i]);

                    //make the array of the data and write it
                    long[] dims = {nRows, nCols};
                    float ar[] = new float[nRows * nCols];
                    int po = 0;
                    for (int row = 0; row < nRows; row++)
                        for (int col = 0; col < nCols; col++) 
                            ar[po++] = String2.parseFloat(reader.readLine());
                    Dataset dataset = hdfFile.createScalarDS
                        (varName, root, hdfFloatType, dims, null, null, 0, ar);
                    if (hdfDataAttributes != null)
                        for (int i = 0; i < hdfDataAttributes.length; i++)
                            dataset.writeMetadata(hdfDataAttributes[i]);
                    ar = null;

                    //make the lon array and write it
                    dims = new long[] {nCols};
                    float lon[] = new float[nCols];
                    for (int col = 0; col < nCols; col++)
                        lon[col] = (float)(lon0 + col * incr);
                    dataset = hdfFile.createScalarDS
                        ("lon", root, hdfFloatType, dims, null, null, 0, lon);
                    if (hdfLonAttributes != null)
                        for (int i = 0; i < hdfLonAttributes.length; i++)
                            dataset.writeMetadata(hdfLonAttributes[i]);

                    //make the lat array and write it
                    dims = new long[] {nRows};
                    float lat[] = new float[nCols];
                    for (int row = 0; row < nRows; row++)
                        lat[row] = (float)(lat0 + row * incr);
                    dataset = hdfFile.createScalarDS
                        ("lat", root, hdfFloatType, dims, null, null, 0, lat);
                    if (hdfLatAttributes != null)
                        for (int i = 0; i < hdfLatAttributes.length; i++)
                            dataset.writeMetadata(hdfLatAttributes[i]);

                    // close file resource
                    hdfFile.close();

                    //delete the ascii file
                    File2.delete(resultDir + name + ".asc");
                    
                    //rename temp file to final name
                    File2.rename(resultDir, name + randomInt + ".hdf", name + ".hdf");

                } 
                */

                //MakeHdf via Matlab script
                if (outType == MakeHdf) {

                    //This way relies on a script which calls Matlab.
                    SSR.cShell("/u00/chump/grd2hdf " + 
                        fullGrdName + " " + 
                        fullResultName + randomInt + ".hdf " + 
                        varName); 

                    File2.rename(resultDir, name + randomInt + ".hdf", name + ".hdf");
                }
            
                //MakeMat (this relies on MakeAsc above making .asc with missingValue=NaN)
                if (outType == MakeMat) {
                    //read the data from the ASCII file
                    BufferedReader reader = new BufferedReader(new FileReader(
                        resultDir + name + ".asc"));
                    String s;
                    s = reader.readLine(); int nCols = String2.parseInt(s.substring(s.indexOf(' ') + 1));
                    s = reader.readLine(); int nRows = String2.parseInt(s.substring(s.indexOf(' ') + 1));
                    s = reader.readLine(); double lon0 = String2.parseDouble(s.substring(s.indexOf(' ') + 1));
                    s = reader.readLine(); double lat0 = String2.parseDouble(s.substring(s.indexOf(' ') + 1));
                    s = reader.readLine(); double incr = String2.parseDouble(s.substring(s.indexOf(' ') + 1));
                    s = reader.readLine(); String mv = s.substring(s.indexOf(' ') + 1);
                    //String2.log("grdinfo = " + String2.toCSSVString(info));

                    //open a dataOutputStream 
                    DataOutputStream dos = DataStream.getDataOutputStream(resultDir + name + randomInt + ".mat");

                    //write the header
                    Matlab.writeMatlabHeader(dos);

                    //first: make the lon array and write it to dos
                    double lon[] = new double[nCols];
                    for (int col = 0; col < nCols; col++)
                        lon[col] = lon0 + col * incr;
                    Matlab.writeDoubleArray(dos, "lon", lon);

                    //second: make the lat array and write it to dos
                    double lat[] = new double[nRows];
                    for (int row = 0; row < nRows; row++)
                        lat[row] = lat0 + row * incr;
                    Matlab.writeDoubleArray(dos, "lat", lat);

                    //make an array of the data
                    double ar[][] = new double[nRows][nCols];
                    for (int row = 0; row < nRows; row++)
                        for (int col = 0; col < nCols; col++) 
                            ar[row][col] = String2.parseDouble(reader.readLine());
                    Matlab.write2DDoubleArray(dos, varName, ar);

                    //close dos 
                    dos.close();
                        
                    //delete the ascii file
                    File2.delete(resultDir + name + ".asc");
                    
                    //Old way relies on script which calls Matlab.
                    //This relies on a proprietary program, so good to remove it.
                    //SSR.cShell("/u00/chump/grdtomatlab " + fullGrdName + " " + 
                    //    fullResultName + randomInt + ".mat " + varName); 

                    File2.rename(resultDir, name + randomInt + ".mat", name + ".mat");
                }

                //MakeXyz
                if (outType == MakeXyz) {
                    SSR.cShell("grd2xyz " + fullGrdName + " >! " + fullResultName + randomInt + ".xyz"); 
                    File2.rename(resultDir, name + randomInt + ".xyz", name + ".xyz");

                    //diagnostic
                    if (false) { // verbose) {
                        String[] rff = String2.readFromFile(fullResultName + ".xyz");
                        if (rff[0].length() > 0)
                            throw new Exception(String2.ERROR + ":\n" + rff[0]);
                        String2.log(fullResultName + ".xyz contains:\n" +
                            rff[1].substring(0, Math.min(rff[1].length(), 200)));
                    }

                    /* //Alternative: unix pipe: 10.5s for westus AT 1km BUT file name in .zip is "-"
                    SSR.cShell("grd2xyz " + fullGrdName + " | zip -j " + fullResultName + ".xyz.zip -"); 
                    // */ 

                    /* //Alternative: java pipe: 14.6s for westus AT 1km with correct file name in .zip
                    ZipOutputStream zos = startZipOutputStream(fullResultName + ".xyz.zip", 
                        File2.nameAndExtension(fullResultName + ".xyz"));
                    SSR.cShell("grd2xyz " + fullGrdName + ".grd", zos, null);
                    zos.close();        
                    // */
                }

                //print the time used
                String2.log("  make " + ext + " TIME=" + 
                    (System.currentTimeMillis() - time) + " ms");
            }

            //makeZip
            if (!makeZip) {
            } else if (File2.touch(fullResultName + ext + ".zip")) {
                String2.log("  reusing " + fullResultName + ext + ".zip");
            } else {
                long time = System.currentTimeMillis();
                SSR.zip(             fullResultName + randomInt + ".zip",
                    new String[]{fullResultName + ext}); 
                File2.rename(resultDir,  name + randomInt + ".zip", name + ext + ".zip");
                String2.log("  zip TIME=" + 
                    (System.currentTimeMillis() - time) + " ms");
            }

            //delete the outType file (it was just for temporary use) 
            if (!makeOutType) {
                File2.delete(fullResultName + ext);    
            }
        }
    }
 
    /**
     * This makes an hdf Char8 attribute.
     * @param attName
     * @param attValue
     * @return the corresponding Attribute
     */
/*    public static Attribute hdfChar8Attribute(String attName, String attValue) {
        Attribute att = new Attribute(attName, hdfCharType, new long[] {attValue.length()});
        att.setValue(attValue.toCharArray()); 
        return att;
    }
*/
    /**
     * This makes an hdf int attribute.
     * @param attName
     * @param attValue
     * @return the corresponding Attribute
     */
/*    public static Attribute hdfIntAttribute(String attName, int attValue[]) {
        Attribute att = new Attribute(attName, hdfIntType, new long[] {attValue.length});
        att.setValue(attValue); 
        return att;
    }
*/
    /**
     * This makes an hdf float attribute.
     * @param attName
     * @param attValue
     * @return the corresponding Attribute
     */
/*    public static Attribute hdfFloatAttribute(String attName, float attValue[]) {
        Attribute att = new Attribute(attName, hdfFloatType, new long[] {attValue.length});
        att.setValue(attValue); 
        return att;
    }
*/
    /**
     * This makes an hdf double attribute.
     * @param attName
     * @param attValue
     * @return the corresponding Attribute
     */
/*    public static Attribute hdfDoubleAttribute(String attName, double attValue[]) {
        Attribute att = new Attribute(attName, hdfDoubleType, new long[] {attValue.length});
        att.setValue(attValue); 
        return att;
    }
*/
    
    private static java.util.Random lockForMakeGMTMap = new java.util.Random();

    /**
     * This uses the Linux programs GMT and 'convert' to plot data on a map
     * and generate a .ps.zip and .gif file with a map.
     * Strings should be "" if not needed.
     * 
     * @param resultDir the directory to be used for temporary and result files
     *    (with '/' at end)
     * @param grdDir the directory with the .grd files (with '/' at end)
     *    (For security, all grd and temp grd files are all kept in grdDir.)
     * @param refDir the directory with psheader script file (with '/' at end)
     *    (e.g. "/u00/ref/")
     * @param imageDir the directory (with '/' at end) 
     * @param logoImageFile (must be in the imageDir) 
     *    (must be .ras file -- "convert" can make them) (currently noaa-420.ras)
     * @param minX the min lon value on the map and appropriate for the data
     * @param maxX the max lon value on the map and appropriate for the data
     * @param minY the min lat value on the map and appropriate for the data
     * @param maxY the max lat value on the map and appropriate for the data
     * @param plotMainData is true if the main dataset should be plotted
     *     (if false, other mainData parameters are ignored)
     * @param grdName the name of the .grd data file (in the grdDir).
     *     It may span a larger area than the desired map.
     * @param scale is a scale factor to be applied via grdmath (e.g., "1000000 MUL") (use "1" if none)
     * @param paletteFileName is the full name of the palette .cpt file to be used
     * @param boldTitle
     * @param units
     * @param info a String[] with info about the main data
     *    to be displayed in the legend with a plain font
     * @param plotBathymetryData is true if bathymetry lines should be plotted
     * @param fullBathymetryFileName is the full name of the GMT .grd file  
     *   (e.g., "<context>images/etopo_<region>.grd")
     * @param bathymetryDrawLinesAt is a single value or a comma-separated list
     *    of values -- depths (m) at which bathymetry lines should be drawn
     * @param bathymetryColor is an int with the rgb color value 
     *    for the bathymetry contour lines
     * @param plotContourData is true if the contour dataset should be plotted
     *   (if false, other contour parameters are ignored)
     * @param contourGrdName the name of the .grd data file (in the grdDir).
     *     It may span a larger area than the desired map.
     * @param contourScale is a scale factor to be applied via grdmath (e.g., "1000000 MUL") (use "1" if none)
     * @param contourDrawLinesAt is a single value or a comma-separated list
     *    of values at which contour lines should be drawn
     //contourPaletteFileName is the complete name of the palette file to be used
     * @param contourColor is an int with the rgb color value
     *    for the contour lines
     * @param contourBoldTitle
     * @param contourUnits
     * @param contourInfo a String[] with info about the contour data
     *    to be displayed in the legend with a plain font
     * @param plotVectorData (if false, other vector parameters are ignored)
     * @param vectorXGrdName e.g., vectorX.grd.  Must be in grdDir. 
     * @param vectorYGrdName e.g., vectorY.grd.  Must be in grdDir. 
     * @param vectorXScale is a scale factor to be applied via grdmath (e.g., "1000000 MUL") (use "1" if none)
     * @param vectorYScale is a scale factor to be applied via grdmath (e.g., "1000000 MUL") (use "1" if none)
     * @param vectorSize 
     * @param vectorBoldTitle
     * @param vectorUnits
     * @param vectorInfo a String[] with info about the vector data
     *    to be displayed in the legend with a plain font
     * @param pushColorBarLeft pushes the colorBar left (use when the numbers are long)
     * @param gifWidth in pixels
     * @param gifHeight in pixels
     * @param customFileName is the file name (without the extension)
     *  of the .ps and .gif file to be made in resultDir
     *  (if the .ps.zip and .gif versions of the file already exist, 
     *  they are over-written)
     * @throws Exception
     */
    public static void makeGMTMap(String resultDir, String grdDir, 
        String refDir, String imageDir,
        String logoImageFile,
        double minX, double maxX, double minY, double maxY, 
        boolean plotMainData,
        String grdName, 
        double scale,
        String paletteFileName,
        String boldTitle, 
        String units, 
        String info[], 
        boolean plotBathymetryData,
        String fullBathymetryFileName,
        String bathymetryDrawLinesAt, //a single value (interval) or comma-separated list (in meters)
        int bathymetryColor,
        boolean plotContourData,
        String contourGrdName, 
        double contourScale,
        String contourDrawLinesAt, //contourPaletteFileName,
        int contourColor,
        String contourBoldTitle, 
        String contourUnits, 
        String contourInfo[], 
        boolean plotVectorData,
        String vectorXGrdName, String vectorYGrdName, 
        double vectorXScale, double vectorYScale,
        double vectorSize, 
        String vectorBoldTitle, 
        String vectorUnits, 
        String vectorInfo[], 
        boolean pushColorBarLeft,
        int gifWidth, int gifHeight,
        String customFileName) throws Exception {

        //synchronized (lockForMakeGMTMap) 
        {
            //POLICY: because this procedure may be used in more than one thread,
            //do work on unique temp files names using randomInt, then rename to proper file name.
            //If procedure fails half way through, there won't be a half-finished file.
            int randomInt = Math2.random(Integer.MAX_VALUE);

            //figure out the params needed to make the map  
            String error = "";
            long startTime = System.currentTimeMillis();
            String gifFileName = customFileName + ".gif"; 
            String psFileName  = customFileName + ".ps";  
            String fullPsFileName = resultDir + customFileName + ".ps";  
            File2.delete(resultDir + gifFileName); //old versions? delete them
            File2.delete(fullPsFileName);
            String tmpFullPsFileName  = resultDir + randomInt + "psgmt.ps";
            String tmpGifFile         = randomInt + ".gif";
//            String tmpGifFile         = randomInt + ".png";
            String tmpTxtFile         = randomInt + ".txt";
            String fullTmpGifFile     = resultDir + tmpGifFile;
            String fullTmpTxtFile     = resultDir + tmpTxtFile;
            String tempGrdName        = randomInt + ".grd";
            if (minX > maxX) {double d = minX; minX = maxX; maxX = d;}
            if (minY > maxY) {double d = minY; minY = maxY; maxY = d;}
            String range = minX + "/" + maxX + "/" + minY + "/" + maxY; 
            String range0360 = Math2.looserAngle0360(minX) + "/" + Math2.looserAngle0360(maxX) + "/" + minY + "/" + maxY; 
            String xpos = ".7";
            String ypos = ".7";
            double xRange = maxX - minX; 
            double yRange = maxY - minY; 
            double maxRange = Math.max(xRange, yRange);
            //these are different from Dave's choices (see "annotation" in his GMT script files)
            //max labels in x axis (if deg/min) is 10
            double majorIncrement, minorIncrement, vecIncrement;   
            if      (maxRange >= 200) {majorIncrement =  50; minorIncrement = 10;   vecIncrement = 10; }   
            else if (maxRange >= 100) {majorIncrement =  20; minorIncrement = 10;   vecIncrement = 10; } //pacrim = 160 (xRange)  
            else if (maxRange >=  50) {majorIncrement =  10; minorIncrement = 5;    vecIncrement = 5; } //nepac  = 110 //vec was 3
            else if (maxRange >=  20) {majorIncrement =   5; minorIncrement = 1;    vecIncrement = 1; } //westus = 22 
            else if (maxRange >=  10) {majorIncrement =   2; minorIncrement = 0.5;  vecIncrement = 0.5; } 
            else if (maxRange >=   5) {majorIncrement =   1; minorIncrement = 0.5;  vecIncrement = 0.5; } //nanoos = 9
            else if (maxRange >=   2) {majorIncrement = 0.5; minorIncrement = 0.25; vecIncrement = 0.25; } //nw01   = 3.5
            else                      {majorIncrement =0.25; minorIncrement = 0.25; vecIncrement = 0.25; } 

            //determine appropriate scale
            //originally, westus map filled the space best: xRange=22 yRange=20 scale=x0.37d  
            double xScale = 22 / xRange;
            double yScale = 20 / yRange;
            double standardScale = 0.335;
            double relativeScale = Math.min(xScale, yScale);
            String graphScale  = "x" + (standardScale * relativeScale) + "d"; 

            //center the map; new scale=.335 area for map is 7.378" wide and 6.699" high
            //was scale=.3428 area for map is 7.55" wide and 6.8554" high
            //was .37 scale  8.15" wide and 7.4" high
            if (xScale > yScale) //right justify     
                xpos = "" + (String2.parseDouble(xpos) + 7.378 - standardScale * yScale * xRange); 
            else                 //vertically center
                ypos = "" + (String2.parseDouble(ypos) + (6.699 - standardScale * xScale * yRange) / 2); 
            //String2.printError("makeGMTMap xScale=" + xScale + " yScale=" + yScale + 
            //                             " xRange=" + xRange + " yRange=" + yRange +
            //                             " xpos="   + xpos   + " ypos="   + ypos);

            String increment = vecIncrement + "/" + vecIncrement;  // X/Y degrees between vectors
            String annotation = "Bf" + minorIncrement + "a" + majorIncrement +  //major/minor ticks
                                "/f" + minorIncrement + "a" + majorIncrement + "WeSn";  //WeSn=grid/labels or not
            //unused below  String dataSetMin[absoluteDataSetIndex];
            //unused below  String dataSetMax[absoluteDataSetIndex];

            //make the .ps version of the map
            long gmtTime = System.currentTimeMillis();
            SSR.cShell("gmtset DEGREE_FORMAT 1"); //so lat labels are -180 to 180; gmt ver 3.x
            //SSR.cShell("gmtset PLOT_DEGREE_FORMAT ddd:mm"); //gmt ver 4
            SSR.cShell("gmtset GRID_PEN 0"); //so no grid lines drawn (1 doesn't look good on .gif

            //call GMT to generate the .ps and .gif files
            //this is from PlotQS2dNew.java
            // psheader to define absolute original point for each page
            SSR.cShell(refDir + "psheader 0.3 0.3 >! " + tmpFullPsFileName); // Empty header

            // plot basemap in GMT
            SSR.cShell("psbasemap" + 
                " -R" + range + " -J" +graphScale + 
                " -O -K -" + annotation + " -X" + xpos + " -Y" + ypos + 
                //" --PLOT_DEGREE_FORMAT=ddd:mm" + // so lon shown as -degrees
                " >> " + tmpFullPsFileName);

            //plotMainData   [for security, grd files are all kept in grdDir]
            if (plotMainData) {
                //handle the case where a grdMath has been requested
                if (Math2.almostEqual(5, scale, 1)) {
                    SSR.cShell("cp " + grdDir + grdName + " " + grdDir  + tempGrdName);
                } else {
                    SSR.cShell("grdmath " + grdDir + grdName + " " + 
                        scale + " MUL = " + grdDir + tempGrdName);
                }

                //plot the main data
                SSR.cShell("grdimage " + grdDir + tempGrdName + " -R" + range + 
                    " -Jx -O -K -C" + paletteFileName +" >> " + tmpFullPsFileName);
                File2.delete(grdDir + tempGrdName);
            }

            //plotBathymetryData
            if (plotBathymetryData) {
                //plot the bathymetry contour lines
                String AString = " -A" + Math.abs(String2.parseDouble(bathymetryDrawLinesAt)); //annotation interval   
                String bathymetryConFile = null; 
                if (bathymetryDrawLinesAt.indexOf(',') > 0) {
                    //>1 value: make a .Con file (made-up extension) with contour levels
                    bathymetryConFile = grdDir + randomInt + "b.Con";
                    AString = " -C" + bathymetryConFile;
                    StringBuilder bathSB = new StringBuilder();
                    double dar[] = String2.csvToDoubleArray(bathymetryDrawLinesAt);
                    for (int i = 0; i < dar.length; i++)
                        bathSB.append(dar[i] + "\tA\n"); //annotation levels
                    //make cpt file
                    //for (int i = 1; i < dar.length; i++) //yes 1: since use i-1 below
                    //    bathSB.append(dar[i-1] + "\t"+i+"\t"+i+"\t"+i+"\t" + dar[i] + "\t"+i+"\t"+i+"\t"+i+"\n"); 
                    //String2.log("AString=" + AString + "\nbathSB=" + bathSB);
                    error = String2.writeToFile(bathymetryConFile, bathSB.toString());
                    if (error.length() > 0)
                        throw new RuntimeException(error);
                }
                String ts;
                SSR.cShell(ts = "grdcontour " + fullBathymetryFileName + " -R" + range + 
                    AString +  
                    " -G4i/10" + //4 inches between labels,  average 10 points to determine label angle
                    " -Wa4/" + getSlashColor(bathymetryColor) + //draw annotated bathymetry lines as width=...
                    //" -Wc2/" + getSlashColor(bathymetryColor) + //draw other bathymetry lines as width=...
                    //" -S4" + //smoothing level; if turned on, make it relative to the data xy increment
                    " -Q10" + //don't draw bathymetry lines with less than n points; etopo1 has 1 minute grid
                    " -Jx -O -K >> " + tmpFullPsFileName);
                String2.log("SSR.cShell cmd: " + ts);
                if (bathymetryConFile != null)
                    File2.delete(bathymetryConFile);
            }

            //plotContourData [for security, grd files are all kept in grdDir]
            if (plotContourData) {
                //handle the case where a grdMath has been requested
                if (Math2.almostEqual(5, contourScale, 1)) {
                    SSR.cShell("cp " + grdDir + contourGrdName + " " + grdDir  + tempGrdName);
                } else {
                    SSR.cShell("grdmath " + grdDir + contourGrdName + " " + 
                        contourScale + " MUL = " + grdDir + tempGrdName);
                }

                //plot the contour lines
                String2.log("grdcontour: " + grdDir + tempGrdName); 
                String AString = " -A" + Math.abs(String2.parseDouble(contourDrawLinesAt)); //annotation interval   
                String contourConFile = null; 
                if (contourDrawLinesAt.indexOf(',') > 0) {
                    //>1 value: make a .Con file (made-up extension) with contour levels
                    contourConFile = grdDir + randomInt + "c.Con";
                    AString = " -C" + contourConFile;
                    StringBuilder contSB = new StringBuilder();
                    double dar[] = String2.csvToDoubleArray(contourDrawLinesAt);
                    for (int i = 0; i < dar.length; i++)
                        contSB.append(dar[i] + "\tA\n"); 
                    //make cpt file
                    //for (int i = 1; i < dar.length; i++) //yes 1: since use i-1 below
                    //    contSB.append(dar[i-1] + "\t"+i+"\t"+i+"\t"+i+"\t" + dar[i] + "\t"+i+"\t"+i+"\t"+i+"\n"); 
                    //String2.log("AString=" + AString + "\ncontSB=" + contSB);
                    error = String2.writeToFile(contourConFile, contSB.toString());
                    if (error.length() > 0)
                        throw new RuntimeException(error);
                }
                SSR.cShell("grdcontour " + grdDir + tempGrdName + " -R" + range + 
                    AString +
                    //" -C" + contourPaletteFileName +
                    //" -A" + //annotationInterval f=fontSize
                    " -G3i/10" + //3 inches between labels,  average 10 points to determine label angle
                    " -Wa4/" + getSlashColor(contourColor) + //draw annotated contour lines as width=...
                    " -Wc2/" + getSlashColor(contourColor) + //draw other contour lines as width=...
                    //" -S4" + //smoothing level; if turned on, make it relative to the data xy increment
                    " -Q10" + //don't draw contour lines with less than n points
                    " -Jx -O -K >> " + tmpFullPsFileName);
                File2.delete(grdDir + tempGrdName);
                if (contourConFile != null)
                    File2.delete(contourConFile);
            }

            //plotVectorData [for security, grd files are all kept in grdDir]
            if (plotVectorData) {
                //subsample vector files to reduce number of arrows displayed
                SSR.cShell("grdsample " + grdDir + vectorXGrdName + 
                    " -G" + grdDir + randomInt + "vectorX.grd -R" + range0360 + " -I" + increment);
                SSR.cShell("grdsample " + grdDir + vectorYGrdName + 
                    " -G" + grdDir + randomInt + "vectorY.grd -R" + range0360 + " -I" + increment);

                //handle the case where a grdMath has been requested
                if (Math2.almostEqual(5, vectorXScale, 1)) {
                    File2.rename(grdDir, randomInt + "vectorX.grd", randomInt + "vectorXM.grd");
                } else {
                    SSR.cShell("grdmath " + grdDir + randomInt + "vectorX.grd " + 
                        (vectorXScale) + " MUL = " + 
                        grdDir + randomInt + "vectorXM.grd");
                    File2.delete(       grdDir + randomInt + "vectorX.grd");  
                }
                if (Math2.almostEqual(5, vectorYScale, 1)) {
                    File2.rename(grdDir, randomInt + "vectorY.grd", randomInt + "vectorYM.grd");
                } else {
                    SSR.cShell("grdmath " + grdDir + randomInt + "vectorY.grd " + 
                        (vectorYScale) + " = " + 
                        grdDir + randomInt + "vectorYM.grd");
                    File2.delete(       grdDir + randomInt + "vectorY.grd");  
                }

                //plot the vectors
                SSR.cShell("grdvector " + 
                    grdDir + randomInt + "vectorXM.grd " + 
                    grdDir + randomInt + "vectorYM.grd -R" + range + 
                    " -G0/0/0 -S" + vectorSize + " -Jx -Q0.01/0.05/0.025 -O -K >> " + 
                    tmpFullPsFileName);

                //delete the temp files
                File2.delete(grdDir + randomInt + "vectorXM.grd");  
                File2.delete(grdDir + randomInt + "vectorYM.grd");
            }

            // add coastline
            String resolution = "h";   //'f'ull 'h'igh 'i'ntermediate 'l'ow 'c'rude
            String2.log("pscoast resolution=" + resolution);
            SSR.cShell("pscoast -D" + resolution + 
                " -W " +  //-W=coastline
                " -N1 -N2" + //-N1=international -N2=state boundaries
                " -G204" + //was 204 grayscale color of land  (a web palette color); was 200
                " -Jx -R -O -K >> " +  
                tmpFullPsFileName);

            // return to relative original point
            SSR.cShell("psxy /dev/null -R -Jx -O -K -X-" + xpos + 
                " -Y-" + ypos + " >> " + tmpFullPsFileName);

            // make metadata box 
            error = String2.writeToFile(fullTmpTxtFile,
                "8.4 0.25\n" +
                "10.3 0.25\n" +
                "10.3 7.75\n" +
                "8.4 7.75\n" +
                "8.4 0.25\n");
            if (error.length() > 0)
                throw new RuntimeException(error);
            SSR.cShell("psxy " + fullTmpTxtFile + " -R0/11/0/8.5 -G255/255/204 " + // was -G248/227/194 
                "-W2/0/0/0 -N -Jx1 -O -K >> " + tmpFullPsFileName);

            //add logo in upper right corner
            SSR.cShell("psimage " + 
                //refDir + "noaa_logo.sun " +
                imageDir + "noaa-420.ras " +
                //"-E420 -C9.55/6.9 -O -K >> " + tmpFullPsFileName); //-E=image's dpi, was 72
                "-E630 -C9.55/7.0 -O -K >> " + tmpFullPsFileName); //-E=image's dpi, 
            
            /*
            //not quite working: embed an .eps noaa_logo file in 
            String [] epsfContents = String2.readFromFile(imageDir + "noaa_logo2.eps");
            if (epsfContents[0].length() > 0) 
                throw new RuntimeException(epsfContents[0]);
            String2.appendFile(tmpFullPsFileName, 
                embedEPSF(8.73 * 72, 5.7 * 72, 0,   //left, bottom, angle
                    1, 1, 0, 0, //xScale, yScale, bBoxLLX, bBoxLLY,
                    epsfContents[1])); 
            */
        
            //accumulate commands to write legend text: spacecraft, time info, units, courtesy info, 
            //If a String won't entirely fit in the box, GMT will remove words from the end.
            StringBuilder sb = new StringBuilder();
            double x = 9.36;
            double y = 6.95;
            double lineHeight = 0.17;

            //titles
            //x, y, size, angle, fontno (3=boldOblique), justify (LeftCenterRight TopMiddleBottom), text
            sb.append(
                "8.5 7.45 12 0 3 LT NOAA\n" +
                "8.5 7.25 12 0 3 LT CoastWatch\n");

            //legend for mainData
            if (plotMainData) {
                //color bar
                double scalePoXOffset = pushColorBarLeft? -0.4 : 0; 
                double barHeight = 1.35 + 
                    (plotBathymetryData? 0 : 0.3) + 
                    (plotContourData?    0 : 1.6) + 
                    (plotVectorData?     0 : 1.6);
                SSR.cShell("psscale -C" + paletteFileName +  
                    " -D" + (9 + scalePoXOffset) +    //centerX
                        "/" + (y - 0.1 - barHeight/2) + //centerY;  0.1 adjusts within padding
                        "/" + barHeight + "/.25 " +     //height/width
                    " -B:: -L -O -K >> " + tmpFullPsFileName);
                y = y - 0.3 - barHeight;  //0.3 is padding
                y = addBoldText(sb, x, y, boldTitle);
                y = addText(sb, x, y, "(" + units + ")");
                for (int i = 0; i < info.length; i++)
                    y = addText(sb, x, y, info[i]);
                y -= lineHeight/4; //gap
            }

            //legend for bathymetryData
            if (plotBathymetryData) {
                //draw a line
                y -= lineHeight;
                SSR.cShell("echo " + (x - .17) + " " + (y + 0.08) + " 90 .333333" +  
                    " | psxy -W2/" + getSlashColor(bathymetryColor) + //no -B = no axes; -Wpenwidth/r/g/b 
                    " -N -Jx1" + //-N plot even if outside boundaries -J is projection
                    " -R0/11/0/8.5 -G" + getSlashColor(bathymetryColor) + " -O -K" + //-GfillR/G/B 
                    " -SV0.01/0/0 " + //arrowwidth/headlength/headwidth
                    " >> " + tmpFullPsFileName);
                y = addBoldText(sb, x, y, "Bathymetry");
                y -= lineHeight/4; //gap
            }

            //legend for contourData
            if (plotContourData) {
                //draw a line
                y -= lineHeight;
                SSR.cShell("echo " + (x - .17) + " " + (y + 0.08) + " 90 .333333" +  
                    " | psxy -W2/" + getSlashColor(contourColor) + //no -B = no axes; -Wpenwidth/r/g/b 
                    " -N -Jx1" + //-N plot even if outside boundaries -J is projection
                    " -R0/11/0/8.5 -G" + getSlashColor(contourColor) + " -O -K" + //-GfillR/G/B 
                    " -SV0.01/0/0 " + //arrowwidth/headlength/headwidth
                    " >> " + tmpFullPsFileName);
                y = addBoldText(sb, x, y, contourBoldTitle);
                y = addText(sb, x, y, "(" + contourUnits + ")");
                for (int i = 0; i < contourInfo.length; i++)
                    y = addText(sb, x, y, contourInfo[i]);
                y -= lineHeight/4; //gap
            }

            //legend for vectorData
            if (plotVectorData) {
                //draw a standard vector
                y -= lineHeight;   
                //.4 is the standard size of a vector: don't change (vector are scaled relative to this size)
                SSR.cShell("echo " + (x - .2) + " " + (y + 0.08) + " 90 .4" +  
                    " | psxy -W2/0/0/0" + //no -B = no axes; -Wpenwidth/r/g/b 
                    " -N -Jx1" + //-N plot even if outside boundaries -J is projection
                    " -R0/11/0/8.5 -G0/0/0 -O -K" + //-GfillR/G/B 
                    " -SV0.01/0.05/0.025 " + //arrowwidth/headlength/headwidth
                    " >> " + tmpFullPsFileName);
                y = addBoldText(sb, x, y, vectorBoldTitle); 
                y = addText(sb, x, y, "(" + 
                    //(Math2.almostEqual(5, relativeScale, 1)? "" : "x" + Math2.roundTo(relativeScale, 2)) + 
                    vectorUnits + ")");
                for (int i = 0; i < vectorInfo.length; i++)
                    y = addText(sb, x, y, vectorInfo[i]);
                y -= lineHeight/4; //gap
            }

            //draw the text
            error = String2.writeToFile(fullTmpTxtFile, sb.toString());
            if (error.length() > 0)
                throw new RuntimeException(error);
            SSR.cShell("pstext " + fullTmpTxtFile + 
                " -R0/11/0/8.5 -Jx1 -O " + //no -K = last thing plotted
                " >> " + //was -M which necessitates headers
                tmpFullPsFileName);

            gmtTime = System.currentTimeMillis() - gmtTime;

            // correction to extract nan's from ps files
            // Don't delete this code. It may be needed. see use of fullPsFileName below
            File2.rename(tmpFullPsFileName, fullPsFileName);
            /*long oldFileLength = (new File(tmpFullPsFileName)).length();
            long sedTime = System.currentTimeMillis();
            SSR.cShell("cat " + tmpFullPsFileName + " | sed '/nan/ d' >! " + fullPsFileName);  //I added !
            sedTime = System.currentTimeMillis() - sedTime;
            long newFileLength = (new File(fullPsFileName)).length();
            if (oldFileLength != newFileLength)
                throw new RuntimeException("SSR.makeGMTMap " + tmpFullPsFileName + " has a 'nan'!");
            */

            // generate GIF file using IMAGICK routines
            //I turned off antialiasing: it was the cause of faint lines at 1 degree
            //increments on US map (the seams of regions used by GMT).
            //It also led to minor artifacts at borders of invalid(axisLine)/valid values (important).
            //It also may have led to more colors in the .gif file.
            //It also may have slowed down rendering (by not by much).
            //It also made the coastline look less crisp.
            //It also made the colorbars excessively dithered.
            //It also made the fonts look a little better (too bad).
            long convertTime = System.currentTimeMillis();
            String gifSizeString = gifWidth + "x" + gifHeight;
            SSR.cShell("convert +antialias" + //+antialias turns off (!) antialiasing
                " -size " + gifSizeString + " -rotate +90 ps:" +   //size is advisory; result may not be as requested
                fullPsFileName + " -resize " + gifSizeString + " gif:" + fullTmpGifFile);
//                fullPsFileName + " -resize " + gifSizeString + " png:" + fullTmpGifFile);
            convertTime = System.currentTimeMillis() - convertTime;

            //zip the ps file
            long zipTime = System.currentTimeMillis();
            SSR.zip(             fullPsFileName + ".zip",
                new String[]{fullPsFileName}); 
            zipTime = System.currentTimeMillis() - zipTime;

            //cleanup
            //File2.delete(tmpFullPsFileName);
            File2.delete(fullTmpTxtFile);
            File2.delete(fullPsFileName);

            //last step, rename fullTmpGifFile to 
            //This is done quickly. So if process above is interrupted, 
            //this file (the end product of this procedure) 
            //won't have been created half way.
/* // */    File2.rename(resultDir, tmpGifFile, gifFileName);

            String2.log("makeGMTMap done. (gmtTime=" + gmtTime +
                //" sedTime=" + sedTime +
                " convertTime=" + convertTime +
                " zipTime=" + zipTime + ") TOTAL TIME=" + 
                (System.currentTimeMillis() - startTime) + " ms");

            long imagickTime = System.currentTimeMillis();
            /*
            //test imagick's speed
            SSR.cShell("convert " +
                " -region '100x100 30 40'" +  //take this region of the incoming image
                " -scale '640x500 0 0'" + //and scale it (simple algorithm)
                " -fill   \"red\"" +
                " -stroke \"blue\"" +
                " -draw 'rectangle 250,250 600,400'" +
                " -clip '100,100 500,500'" +
                " -filter 'Point'" +
                " -draw 'image Over 0,0 0,0 " + imageDir + gifFileName + "'" + //location  size(0,0 for actual)
                " gif:" + imageDir + "TestMap.gif" +  //input
                " gif:" + resultDir + gifFileName);  //output
             */

 /*            //test ImageIO
            //***** IMPORTANT
            //  This requires the following line be added to beginning of startup.sh:
            //    export JAVA_OPTS=-Djava.awt.headless=true
            //System.getProperties().setProperty("java.awt.headless", "true");    //getProperties() not soon enough
            BufferedImage bi = ImageIO.read(new File(resultDir + tmpGifFile)); //a png!  was imageDir + "TestMap.gif"));
            BufferedImage bi2 = ImageIO.read(new File(imageDir + "noaa-420.gif"));
            Graphics g = bi.getGraphics(); 
            g.drawImage(bi2, 120,120, 240,240, //dest x1,y1,x2,y2
                 80,80, 400,400, //source x1,y1,x2,y2
                 null); //ImageObserver 
            //ImageIO.write(bi, "png", new File(imageDir + "temp.png"));
            Image2.saveAsGif216(bi, resultDir + gifFileName);
                       
            String2.log("imagick test TIME=" + 
                (System.currentTimeMillis() - imagickTime) + " ms" +
                "\nImageReaders: " + String2.toCSSVString(ImageIO.getReaderFormatNames()) +
                "\nImageWriters: " + String2.toCSSVString(ImageIO.getWriterFormatNames())
                );
//*/

        }
    }

    /**
     * Add a bold line to the current StringBuilder if text.length()>0.
     *
     * @param StringBuilder
     * @param x
     * @param y
     * @param text
     * @return newY
     */
    private static double addBoldText(StringBuilder StringBuilder, 
        double x, double y, String text) {

        if (text.length() == 0)
            return y;
        //x, y, size, angle, fontno, justify (LeftCenterRight TopMiddleBottom), text
        StringBuilder.append(x + " " + y + " 12 0 1 CT " + text + "\n");
        return y - 0.2;  //lines separated by .2  
    }

    /**
     * Add a regular line to the current StringBuilder if text.length()>0.
     *
     * @param StringBuilder
     * @param x
     * @param y
     * @param text
     * @return newY
     */
    private static double addText(StringBuilder sb, 
        double x, double y, String text) {

        if (text.length() == 0)
            return y;
        //x, y, size, angle, fontno, justify (LeftCenterRight TopMiddleBottom), text
        sb.append(x + " " + y + " 10 0 0 CT " + text + "\n");
        return y - 0.17;  //lines separated by .17  
    }

    /**
     * Make a <region>RegionPs.png (with coast from GMT) and 
     * <region>RegionRect.png (with rectangles) (usually in .../images dir) from scratch.
     * See one-time use of this in CWBrowser.java.
     *
     * @param resultDir the directory for the files (e.g., <context>/images/ with a slash at the end)
     * @param fileName the output file name (e.g., "<region>Region", without Ps.png or Rect.png at end)
     * @throws Exception
     */
    public static void makeGMTPlainRegionsMap(String resultDir, String fileName) 
            throws Exception {
        String2.log("\n**** makePlainRegionsMap(" + resultDir + ", " + fileName + ")");
        String2.log("These can be recombined with /images/" + fileName + ".draw in CoPlot");
        String2.log("then save as " + fileName + ".jpg (JPG high quality) and " + fileName + ".png.");

        /*
        //*******  westus
        //pixels from upper left 0,0
        int minXPixel = 37;  double minXDegrees = -135;  
        int maxXPixel = 230; double maxXDegrees = -113;
        int minYPixel = 194; double minYDegrees = 50;
        int maxYPixel = 368; double maxYDegrees = 30;
        //range: minX + "/" + maxX + "/" + minY + "/" + maxY; 
        String range     = "-135/-113/30/50"; 
        String refDir    = "c:/u00/ref/";
        String scale     = "x0.185d"; 
        String imageSize = "520x420";
        String xpos      = ".6";
        String ypos      = ".3";
        String areas[][]= {
            //areas is used to draw lat, lon, and region labels
            //  {0xRRGGBBAA color, minX, maxX, minY, maxY, textX, textY}
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-136",   "28",    "-135"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-131",   "28",    "-130"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-126",   "28",    "-125"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-121",   "28",    "-120"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-116",   "28",    "-115"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-137",   "29.5",  "30"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-137",   "34.5",  "35"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-137",   "39.5",  "40"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-137",   "44.5",  "45"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-137",   "49.5",  "50"},   
            {"#ccffccFFL", "-135",   "-113",   "30",   "50",   "-134.5", "39",    "West US"},   
            {"#66ff66c0L", "-131",   "-122",   "41",   "50",   "-129",   "47",    "N"},      
            {"#3399ffc0L", "-126",   "-122.5", "46.5", "50",   "-126.25","47.75", "N1"},   
            {"#3399ffc0L", "-126.5", "-123.5", "44.5", "47.5", "-126",   "45.75", "N2"},
            {"#3399ffc0L", "-127.5", "-124",   "42",   "45.5", "-126.5", "43",    "N3"},
            {"#ffff00c0L", "-129.5", "-120.5", "33.5", "42.5", "-127.5", "37",    "C"}, 
            {"#ff3333c0L", "-126.5", "-123",   "38.5", "42",   "-126",   "39.5",  "C1"},   
            {"#ff3333c0L", "-125",   "-121",   "35",   "39",   "-124.5", "37",    "C2"},  
            {"#ff3333c0L", "-123",   "-120",   "34",   "37",   "-122.75","35",    "C3"},   
            {"#cc33ccc0L", "-125",   "-116.5", "30",   "35",   "-123.25","30.75", "S"},  
            {"#cc3399c0L", "-122",   "-119",   "32",   "35",   "-121.5", "32.5",  "S1"},   
            {"#cc3399c0L", "-119.5", "-116.5", "31.5", "34.5", "-118.75","31.5",  "S2"}};
        */

        //*******  usmexico
        //pixels from upper left 0,0
        int minXPixel = 38;  double minXDegrees = -135;  
        int maxXPixel = 222; double maxXDegrees = -105; 
        int minYPixel = 195; double minYDegrees = 50;
        int maxYPixel = 367; double maxYDegrees = 22; 
        //range: minX + "/" + maxX + "/" + minY + "/" + maxY; 
        String range     = "-135/-105/22/50";  
        String refDir    = "c:/u00/ref/";
        String scale     = "x0.13d"; 
        String imageSize = "520x420";
        String xpos      = ".6";
        String ypos      = ".3";
        String areas[][]= {
            //areas is used to draw lat, lon, and region labels
            //  {0xRRGGBBAA rect color, minX, maxX, minY, maxY, textX, textY, text}
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-136.75","19.25", "-135"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-131.75","19.25", "-130"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-126.75","19.25", "-125"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-121.75","19.25", "-120"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-116.75","19.25", "-115"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-111.75","19.25", "-110"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-106.75","19.25", "-105"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-138.5", "24.25", "25"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-138.5", "29.25", "30"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-138.5", "34.25", "35"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-138.5", "39.25", "40"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-138.5", "44.25", "45"},   
            {"#ccffccFFL", "0",      "0",      "0",    "0",    "-138.5", "49.25", "50"},   
            {"#ccccccFFL", "-135",   "-105",   "22",   "50",   "-131",   "25",    "US+Mexico"},   
            {"#6666ccc0L", "-135",   "-113",   "29",   "50",   "-133",   "31",    "West US"},   
            {"#3333ccc0L", "-131",   "-122",   "41",   "50",   "-129.75","46",    "N"},      
            {"#3333ccc0L", "-126",   "-122.5", "46.5", "50",   "-127.5", "47.75", "N1"},   
            {"#3333ccc0L", "-126.5", "-123.5", "44.5", "47.5", "-126.5", "45.75", "N2"},
            {"#3333ccc0L", "-127.5", "-124",   "42",   "45.5", "-127",   "43",    "N3"},
            {"#33cc33c0L", "-129.5", "-120.5", "33.5", "42.5", "-128",   "37",    "C"}, 
            {"#33cc33c0L", "-126.5", "-123",   "38.5", "42",   "-126.5", "39.5",  "C1"},   
            {"#33cc33c0L", "-125",   "-121",   "35",   "39",   "-125.25","36.75", "C2"},  
            {"#33cc33c0L", "-123",   "-120",   "34",   "37",   "-124",   "35",    "C3"},   
            {"#cc9933c0L", "-125",   "-116.5", "29",   "35",   "-123.25","30",    "S"},  
            {"#cc9933c0L", "-122",   "-119",   "32",   "35",   "-122",   "32.5",  "S1"},   
            {"#cc9933c0L", "-119.5", "-116.5", "31.5", "34.5", "-119.75","31.5",  "S2"},
            {"#cc3333c0L", "-120",   "-105",   "22",   "33",   "-118.75","23",    "M"},  
            {"#cc3333c0L", "-118",   "-110",   "27",   "32",   "-118",   "27.5",  "M1"},   
            {"#cc3333c0L", "-116",   "-105",   "22",   "28",   "-115",   "23",    "M2"}};
        String majorIncrement = "5";
        String minorIncrement = "1";
        String annotation = "Bf" + minorIncrement + "a" + majorIncrement +  //major/minor ticks
                            "/f" + minorIncrement + "a" + majorIncrement + "wesn";  //WeSn=grid/labels or not
        String res = "f";
        String fullTmpPsName     = resultDir + "tmp.ps";
        String fullBlankName     = resultDir + "blank.png";
        String fullPsName        = resultDir + fileName + ".ps";
        String fullPngName       = resultDir + fileName + ".png";
        String fullPsPngName     = resultDir + fileName + "Ps.png";
        String fullRectPngName   = resultDir + fileName + "Rect.png";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        out.write(82);
        err.write(83);
        //make the .ps version of the map
        SSR.cShell("gmtset DEGREE_FORMAT 1", out, err); //so lat labels are -180 to 180; gmt ver 3.x
        //SSR.cShell("gmtset PLOT_DEGREE_FORMAT ddd:mm"); //gmt ver 4
        SSR.cShell("gmtset ANOT_FONT 0", out, err);

        //PART II
        //Generate image with coast
        //call GMT to generate the .ps and .gif files
        //this is from PlotQS2dNew.java
        // psheader to define absolute original point for each page
        SSR.cShell(refDir + "psheader 0.2 0.4 >! " + fullTmpPsName,
            out, err); // Empty header

        // plot basemap in GMT
        SSR.cShell("psbasemap" + 
            " -R" + range + " -J" +scale + 
            " -O -K -" + annotation + " -X" + xpos + " -Y" + ypos + 
            //" --PLOT_DEGREE_FORMAT=ddd:mm" + // so lon shown as -degrees
            " >> " + fullTmpPsName,
            out, err);

        // add coastline
        SSR.cShell("pscoast -D" + res + 
            " -N1 -N2" + //international and state boundaries
            " -Jx -R" +
            " -G204" + //was 204 grayscale color of land  (a web palette color); was 200
            " -W -O >> " + //no -K, this is end
            fullTmpPsName,
            out, err);

        //correction to extract nan's from ps files
        //SSR.cShell("cat " + fullTmpPsName + " | sed '/nan/ d' >! " + fullPsName,
        //    out, err);  //I added !
        //File2.delete(fullTmpPsName);
        
         int xPixelRange = maxXPixel - minXPixel;
         int yPixelRange = maxYPixel - minYPixel;
         double xDegreesRange = maxXDegrees - minXDegrees;
         double yDegreesRange = maxYDegrees - minYDegrees;


/*        //test create gif from ps and drawing things
        StringBuilder cmd = new StringBuilder();
        cmd.append(
            "convert -size " + imageSize + 
            " -rotate +90" +
            " ps:" + fullPsName + 
            " -resize " + imageSize + 
            " -font \"ps:helvetica\"" +
            " -pointsize 10");
        for (int area = 0; area < areas.length; area++) {
            cmd.append(
                " -fill   \"" + areas[area][0] + "\"" +
                " -stroke \"" + areas[area][0] + "\"" +
                " -draw 'rectangle " + 
                   Math2.roundToInt(minXPixel + xPixelRange*(String2.parseDouble(areas[area][1])-minXDegrees)/xDegreesRange) + "," + 
                   Math2.roundToInt(minYPixel + yPixelRange*(String2.parseDouble(areas[area][3])-minYDegrees)/yDegreesRange) + " " + 
                   Math2.roundToInt(minXPixel + xPixelRange*(String2.parseDouble(areas[area][2])-minXDegrees)/xDegreesRange) + "," + 
                   Math2.roundToInt(minYPixel + yPixelRange*(String2.parseDouble(areas[area][4])-minYDegrees)/yDegreesRange) + "'");
        }
        cmd.append(
            " -stroke \"transparent\"" +
            " -fill \"black\"");
        for (int area = 0; area < areas.length; area++) {
            cmd.append(
                " -draw 'text " + 
                   Math2.roundToInt(minXPixel + xPixelRange*(String2.parseDouble(areas[area][5])-minXDegrees)/xDegreesRange) + "," + 
                   Math2.roundToInt(minYPixel + yPixelRange*(String2.parseDouble(areas[area][6])-minYDegrees)/yDegreesRange) + " " + 
                   "\"" + areas[area][7] + "\"'");
        }
        cmd.append(
            " png:" + fullPngName);
        SSR.cShell(cmd.toString(), out, err);
*/
        //generate GIF of ps file using IMAGICK routines
        StringBuilder cmd = new StringBuilder();
        cmd.append(
            "convert +antialias" + //+antialias turns off (!) antialiasing
            " -size " + imageSize + 
            " -rotate +90" +
            " ps:" + fullTmpPsName + 
            " -resize " + imageSize + 
            " png:" + fullPsPngName);
        SSR.cShell(cmd.toString(), out, err);
        File2.delete(fullTmpPsName);

        //PART II
        //generate GIF file using IMAGICK routines
        cmd.setLength(0);
        cmd.append(
            "convert -antialias" + //-antialias: leave antialising on (for text)
            " -size " + imageSize + 
            " -font \"ps:helvetica\"" +
            " -pointsize 10");
        String2.log("areas minX,minY,maxX,maxY,text pixels:");
        for (int area = 0; area < areas.length; area++) {
            if (!areas[area][1].equals("0")) {
                int minX = Math2.roundToInt(minXPixel + xPixelRange*(String2.parseDouble(areas[area][1])-minXDegrees)/xDegreesRange); 
                int minY = Math2.roundToInt(minYPixel + yPixelRange*(String2.parseDouble(areas[area][3])-minYDegrees)/yDegreesRange); 
                int maxX = Math2.roundToInt(minXPixel + xPixelRange*(String2.parseDouble(areas[area][2])-minXDegrees)/xDegreesRange); 
                int maxY = Math2.roundToInt(minYPixel + yPixelRange*(String2.parseDouble(areas[area][4])-minYDegrees)/yDegreesRange);
                String2.log("    " + minX + "," + minY + "," + maxX + "," + maxY + "\\f\\" + "  text=" + areas[area][7]); 
                cmd.append(
                    " -fill   \"" + areas[area][0] + "\"" +
                    " -stroke \"" + areas[area][0] + "\"" +
                    " -draw 'rectangle " + minX + "," + minY + " " + maxX + "," + maxY + "'");
            }
        }
        cmd.append(
            " -stroke \"transparent\"" +
            " -fill \"black\"");
        for (int area = 0; area < areas.length; area++) {
            cmd.append(
                " -draw 'text " + 
                   Math2.roundToInt(minXPixel + xPixelRange*(String2.parseDouble(areas[area][5])-minXDegrees)/xDegreesRange) + "," + 
                   Math2.roundToInt(minYPixel + yPixelRange*(String2.parseDouble(areas[area][6])-minYDegrees)/yDegreesRange) + " " + 
                   "\"" + areas[area][7] + "\"'");
        }
        cmd.append(
            " png:" + fullBlankName +
            " png:" + fullRectPngName);
        SSR.cShell(cmd.toString(), out, err);

        String2.log(out.toString());
        String2.log(err.toString());
    }



    /**
     * This converts an int into a slash color (in the form that GMT likes).
     *
     * @param color
     * @return a string in the form r/g/b where r,g,b are 0 - 255
     */
    public static String getSlashColor(int color) {
        return 
            ((color >> 16) & 255) + "/" +
            ((color >>  8) & 255) + "/" +
            (color         & 255);
    }


}


