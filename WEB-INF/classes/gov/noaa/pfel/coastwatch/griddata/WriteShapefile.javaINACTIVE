/* 
 * Matlab Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.util.String2;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Vector;


/**
 * THIS WAS NOT FINISHED AND IS NOT ACTIVE.
 * This class can convert an gridded .asc file (in CoastWatch's ArcView format)
 * into an ArcView shapefile.
 * See Shapefile documentation at https://www.esri.com/library/whitepapers/pdfs/shapefile.pdf
 *
 * This class only relies on DataStream and String2 -- no other classes(?).
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2005-05-18
 *
 */
public class WriteShapefile {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    //These constants relate to the little-endian boolean value 
    //commonly used as part of calls to DataStream methods.
    private final static boolean little = true;
    private final static boolean big = false;


    /* *
     * This writes a shapefile header to the file.
     * See Shapefile documentation, pgs 3-5. 
     *
     * @param fullAscName the full name of the ESRI-style ASCII Grid file.
     *   The first 6 lines must be in the format:
     *   <pre>
     *     ncols 1761
     *     nrows 1601
     *     xllcenter 225   //was xllcorner
     *     yllcenter 30
     *     cellsize 0.0125
     *     nodata_value 99999
     *   </pre>
     *   Subsequent lines have the data.
     * @param stream the stream for the Shapefile file
     * @throws Exception if trouble
     */
    public static void write(String fullAscName, DataOutputStream stream) throws Exception {

        //read the data from the ASCII file
        String2.log("WriteShapefile.write(" + fullAscName + ")");
        long time = System.currentTimeMillis();
        BufferedReader reader = new BufferedReader(new FileReader(fullAscName));
        try {
            String s;
            s = reader.readLine(); int nX = String2.parseInt(s.substring(s.indexOf(' ') + 1));
            s = reader.readLine(); int nY = String2.parseInt(s.substring(s.indexOf(' ') + 1));
            s = reader.readLine(); double x0 = String2.parseDouble(s.substring(s.indexOf(' ') + 1));
            s = reader.readLine(); double y0 = String2.parseDouble(s.substring(s.indexOf(' ') + 1));
            s = reader.readLine(); double incr = String2.parseDouble(s.substring(s.indexOf(' ') + 1));
            s = reader.readLine(); String mvString = s.substring(s.indexOf(' ') + 1);
            float mv = String2.parseFloat(s); 
            int x, y;

            //make the x and y arrays
            double xValues[] = DataHelper.getRegularArray(nx, x0, incr);
            double yValues[] = DataHelper.getRegularArray(ny, y0, incr);

            //make the data array
            double minM = Double.MAX_VALUE;
            double maxM = -Double.MAX_VALUE; //not Double.MIN_VALUE which ~= 0
            float m[][] = new float[nX][nY];  //M is shapefile term for "measure"
            for (y = 0; y < nY; y++) {
                for (x = 0; x < nX; x++) {
                    float f = String2.parseDouble(reader.readLine());
                    if (f == mv) {
                        f = Float.NaN;
                    } else {
                        minM = Math.min(minM, f);
                        maxM = Math.max(maxM, f);
                    }
                    data[x][y] = f; 
                }
            }
        } finally {
            reader.close();
        }
        String2.log("time to read ascii file: " + (System.currentTimeMillis() - time));

        //write the shapefile header
        DataStream.writeInt(big, stream, 9994); //file id code
        DataStream.writeInt(big, stream, 0);
        DataStream.writeInt(big, stream, 0);
        DataStream.writeInt(big, stream, 0);
        DataStream.writeInt(big, stream, 0);
        DataStream.writeInt(big, stream, 0);
        DataStream.writeInt(big, stream, 0); //file length (0 for now; see postEdit)
        DataStream.writeInt(little, stream, 1000);  //file version
        //?DataStream.writeInt(little, stream, ?);  //shapetype (only one type per file)
        DataStream.writeDouble(little, stream, x0);  //minX
        DataStream.writeDouble(little, stream, y0);  //minY
        DataStream.writeDouble(little, stream, x0 + nX * incr);  //maxX (double precision)
        DataStream.writeDouble(little, stream, y0 + nY * incr);  //maxY (double precision)
        DataStream.writeDouble(little, stream, 0);  //minZ (their Z: altitude)
        DataStream.writeDouble(little, stream, 0);  //maxZ 
        DataStream.writeDouble(little, stream, minM);  
        DataStream.writeDouble(little, stream, maxM); 


    }



    /* *
     * This tests the methods in this class.
     */
    public static void main(String args[]) throws Exception {
        /*
        readMatlabFile(SSR.getTempDirectory() + "OQNux108day20050516_x-135_X-113_y30_Y50.mat");

        readMatlabFile(SSR.getTempDirectory() + "AT2005032_2005045_ssta_westus.mat");

        //write a double matlab file
        String tempFile = "c:/temp/double.mat";       
        DataOutputStream das = getDataOutputStream(tempFile);
        try {
            writeMatlabHeader(das);
            double dLat[] = {1.1, 2.2, 3.3};
            double dLon[] = {44.4, 55.5};
            double dData[][] =  new double[3][2] {{1.11, 2.22}, {33.33, 44.44}, {555.55, 666.66}};
            writeDoubleArray(das, "lon", dLon);
            writeDoubleArray(das, "lat", dLat);
            write2DDoubleArray(das, "MyData", dData);
            das.close();
            log(com.cohort.util.File2.hexDump(tempFile, 256));  //uses an external class!

            //read it
            readMatlabFile(tempFile);

            //write a float matlab file
            tempFile = "c:/temp/float.mat";       
            das = getDataOutputStream(tempFile);
            writeMatlabHeader(das);
            float fLat[] = {1.1f, 2.2f, 3.3f};
            float fLon[] = {44.4f, 55.5f};
            float fData[][] = new float[3][2] {{1.11f, 2.22f}, {33.33f, 44.44f}, {555.55f, 666.66f}};
            writeFloatArray(das, "lon", fLon);
            writeFloatArray(das, "lat", fLat);
            write2DFloatArray(das, "MyData", fData);      
        } finally {
            das.close();
        }
        log(com.cohort.util.File2.hexDump(tempFile, 256)); //uses an external class!
        
        //read it
        readMatlabFile(tempFile);

        //in use in SSR (only works on computer with GMT)
        //SSR.makeDataFile(
        //    "c:/temp/", "OpenDAPAGssta1day20050308.W-135.E-113.S30.N50", //without .grd 
        //    SSR.MakeMat, "ssta", true, false); 
        */

    }
}
