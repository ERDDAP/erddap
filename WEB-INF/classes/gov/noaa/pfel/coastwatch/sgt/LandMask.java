/* 
 * LandMask Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.sgt;

import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.DataStream;
import gov.noaa.pfel.coastwatch.griddata.Grid;

import java.io.DataInputStream;


/**
 * THIS IS CURRENTLY NOT ACTIVE.
 * This class holds the land mask data for the entire world in a compact form.
 * The standard data uses about 19MB of memory.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2005-02-10
 *
 */
public class LandMask  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /* The pointers point to the appropriate record. 
     * [lon -180..179 stored as 0..359][lat -90..89 stored as 0..179]
     */
    private short pointers[][] = new short[360][180]; //[lon][lat]

    /* Each record holds the data for a 1x1 degree area, in 1/128 degree increment. 
     * [record number][lat 0..127][lon 0..7] where each short has 16 bits.
     */
    private short records[][][]; //

    /**
     * This reads the LandMask.dat file, stores the data, and 
     * makes it available in different formats.
     *
     * @param landMaskDatFileName
     * @throws Exception if trouble
     */
    public LandMask(String landMaskDatFileName)
            throws Exception {
        //if (verbose) 
        //    String2.log(File2.hexDump(landMaskDatFileName, 64));

        long time = System.currentTimeMillis();

        //open the file
        DataInputStream dis = DataStream.getDataInputStream(landMaskDatFileName);
        int nRecords = dis.available() / 2048;
        String2.log("bytes available = " + dis.available() + " nRecords=" + nRecords);

        //read the header  (one 2048 byte record)
        Test.ensureEqual(dis.readShort(),  128, "Unexpected value #0");
        Test.ensureEqual(dis.readShort(), 9644, "Unexpected value #1");
        Test.ensureEqual(dis.readShort(), 2048, "Unexpected value #2");
        Test.ensureEqual(dis.readShort(), -180, "Unexpected value #3");
        Test.ensureEqual(dis.readShort(),  180, "Unexpected value #4");
        Test.ensureEqual(dis.readShort(),  -90, "Unexpected value #5");
        Test.ensureEqual(dis.readShort(),   90, "Unexpected value #6");
        dis.skip(1017 * 2);

        //read the record pointers (64 2048 byte records)
        for (int tLat = 0; tLat < 180; tLat++)
            for (int tLon = 0; tLon < 360; tLon++) 
                pointers[tLon][tLat] = dis.readShort();
        dis.skip(736 * 2);

        //read the land mask records (records #65..nRecords-1
        records = new short[nRecords][][]; //[record number][lat 0..127][lon 0..7]
        for (int tRecord = 65; tRecord < nRecords; tRecord++) {
            records[tRecord] = new short[128][8];
            for (int i = 0; i < 128; i++)
                for (int j = 0; j < 8; j++)
                    records[tRecord][i][j] = dis.readShort();
        }

        //make a dummy record #0 (all water) and #1 (all land)
        records[0] = new short[128][8]; //already all zeroes
        records[1] = new short[128][8];
        for (int i = 0; i < 128; i++)
            for (int j = 0; j < 8; j++)
                records[1][i][j] = (short)0xFFFF;

        //close the file
        dis.close();

        //if (verbose) 
            String2.log("LandMask constructor time=" + (System.currentTimeMillis() - time) + "ms"); 
     
    }

    /**
     * Find out if a given location is land or not.
     *
     * @param lon128 longitude (lon -180..180 +180) * 128
     * @param lat128 latitude (lat + 90) * 128
     * @return true if this point is land
     */
    public boolean isLand(int lon128, int lat128) {
        //find relevant record number
        int recordNumber = pointers[lon128 / 128][lat128 / 128]; //floor -> degrees

        //each record has bits row by row, bottom to top, left to right
        //String2.log("isLand(" + (lon128/128.0 - 360) + ", " + (lat128/128.0 - 90) + ") record#=" + recordNumber +
        //     " [2]=" + (lat128 % 128) + " [3]=" + ((lon128 % 128) / 16));
        return (records[recordNumber][lat128 % 128][(lon128 % 128) / 16] & (0x8000 >> (lon128 % 16))) != 0;
    }


    /**
     * THIS IS NOT FINISHED. THE RESULTS MAY BE OFF POSITION BY 1/2 OF 1/128TH DEGREE.
     * Make a Grid with a water mask (water=0, other=NaN),
     * a land mask (land=1, other=NaN), or
     * a combined file (water=0, land=1).
     * 
     * @param desiredMinLon may be +/-180 or 0..360
     * @param desiredMaxLon may be +/-180 or 0..360
     * @param desiredMinLat
     * @param desiredMaxLat
     * @param desiredNLonPoints
     * @param desiredNLatPoints
     * @param maskType 0=water, 1=land, 2=combined
     * @return a grid.
     *    The new grid's minZ and maxZ are simplistic (as if the area contains 
     *    some land and some water.
     * @throws Exception if trouble
     */
    public Grid makeGrid(double desiredMinLon, double desiredMaxLon,
        double desiredMinLat, double desiredMaxLat,
        int desiredNLonPoints,
        int desiredNLatPoints,
        int maskType) throws Exception {

        Test.ensureBetween(maskType, 0, 2, "maskType");     

        //this method of convert to pm180 is better (here) than Math.anglePM180 
        //because desiredMaxLon of 360 shouldn't be converted to -180
        //!!!THIS NEEDS TO BE REPLACED BY A PROPER SYSTEM!!!  lonPM180-related
        double desiredMinLonPM180 = desiredMinLon - (desiredMaxLon > 180? 360 : 0); 
        double desiredMaxLonPM180 = desiredMaxLon - (desiredMaxLon > 180? 360 : 0);

        //figure out what is needed   
        int lon128Start  = Math.max(0,             (int)Math.floor((desiredMinLonPM180 + 180) * 128)); //safe
        int lon128End    = Math.min(360 * 128 - 1, (int)Math.ceil( (desiredMaxLonPM180 + 180) * 128)); //safe
        int lon128Stride = Grid.findStride(lon128Start, lon128End, desiredNLonPoints);
        int lat128Start  = Math.max(0,             (int)Math.floor((desiredMinLat + 90) * 128)); //safe
        int lat128End    = Math.min(180 * 128 - 1, (int)Math.ceil( (desiredMaxLat + 90) * 128)); //safe
        int lat128Stride = Grid.findStride(lat128Start, lat128End, desiredNLatPoints);
        int nLon = Math2.hiDiv(lon128End - lon128Start + 1, lon128Stride);
        int nLat = Math2.hiDiv(lat128End - lat128Start + 1, lat128Stride);
        String2.log("LandMask.makeGrid" +
            " lonStart=" + lon128Start + " lonEnd=" + lon128End + " lonStride=" + lon128Stride + 
            " latStart=" + lat128Start + " latEnd=" + lat128End + " latStride=" + lat128Stride);

        //make the new grid
        Grid newGrid = new Grid();

        //gather z in desired order (column by column, left to right, bottom to top within the column)
        newGrid.z = new double[nLon * nLat];
        int po = 0;
        for (int tLon128 = lon128Start; tLon128 <= lon128End; tLon128 += lon128Stride) {
            for (int tLat128 = lat128Start; tLat128 <= lat128End; tLat128 += lat128Stride) {
                boolean land = isLand(tLon128, tLat128);
                if      (maskType == 0) newGrid.z[po++] = land? Double.NaN : 0;
                else if (maskType == 1) newGrid.z[po++] = land? 1 : Double.NaN;
                else                    newGrid.z[po++] = land? 1 : 0;
                //String2.logNoNewline(Double.isNaN(newGrid.z[po-1])? " " : "" + (int)newGrid.z[po-1]);
            }
            //String2.logNoNewline("\n");
        }
        Test.ensureEqual(po, newGrid.z.length, "po");
        if      (maskType == 0) {newGrid.minZ = 0; newGrid.maxZ = 0;}
        else if (maskType == 1) {newGrid.minZ = 1; newGrid.maxZ = 1;}
        else                    {newGrid.minZ = 0; newGrid.maxZ = 1;}

        //set the other public values
        newGrid.lonSpacing = lon128Stride / 128.0;
        newGrid.latSpacing = lat128Stride / 128.0;
        newGrid.lon = Grid.makeRegularArray(nLon, lon128Start / 128.0 - 180, newGrid.lonSpacing);               
        newGrid.lat = Grid.makeRegularArray(nLat, lat128Start / 128.0 -  90, newGrid.latSpacing);               
        if (desiredMaxLon > 180) 
            newGrid.forceLonPM180(false);

        return newGrid;
    }



    /**
     * A main method -- used to test the methods in this class.
     *
     * @param args is ignored  (use null)
     * @throws Exception if trouble
     */
    public static void main(String args[]) throws Exception {
        LandMask.verbose = true;
        LandMask landMask = new LandMask(String2.getContextDirectory() + "WEB-INF/ref/landmask.dat");
        Test.ensureEqual(landMask.isLand((-130 + 360) * 128, (40 + 90) * 128), false, "a"); //is lon right?
        Test.ensureEqual(landMask.isLand((-120 + 360) * 128, (40 + 90) * 128), true , "b"); 

    }


}
